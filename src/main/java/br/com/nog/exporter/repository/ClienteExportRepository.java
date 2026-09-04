package br.com.nog.exporter.repository;

import br.com.nog.exporter.model.ClienteBase;
import br.com.nog.exporter.model.EnderecoExportacao;
import br.com.nog.exporter.model.OperacaoExportacao;
import br.com.nog.exporter.model.TelefoneExportacao;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClienteExportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClienteExportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Equivale ao SELECT principal da query original, mas usa keyset pagination:
     * c.ndg > :lastNdg em vez de OFFSET.
     */
    public List<ClienteBase> buscarClientes(long lote, long lastNdg, int limit) {
        String sql = """
                SELECT
                    TRIM(c.cpf_cnpj) AS cpfCnpj,
                    TRIM(c.nome) AS nomeCliente,
                    c.tipo_pessoa AS tipoPessoa,
                    c.data_nascimento AS dataNascimento,
                    c.sexo AS sexo,
                    c.estado_civil AS estadoCivil,
                    c.profissao AS profissao,
                    c.ndg AS codigo,
                    TRIM(c.email) AS email
                FROM rico.cadastros c
                WHERE c.ndg > :lastNdg
                  AND EXISTS (
                      SELECT 1
                      FROM rico.operacoes o
                      WHERE o.ndg = c.ndg
                        AND o.lote = :lote
                  )
                ORDER BY c.ndg
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lote", lote)
                .addValue("lastNdg", lastNdg)
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ClienteBase(
                rs.getString("cpfCnpj"),
                rs.getString("nomeCliente"),
                rs.getString("tipoPessoa"),
                toLocalDate(rs.getDate("dataNascimento")),
                rs.getString("sexo"),
                rs.getString("estadoCivil"),
                rs.getString("profissao"),
                rs.getLong("codigo"),
                rs.getString("email")
        ));
    }

    public Map<Long, List<TelefoneExportacao>> buscarTelefones(List<Long> ndgs) {
        if (ndgs.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT DISTINCT
                    t.ndg,
                    TRIM(t.ddd) AS ddd,
                    TRIM(t.numero) AS numero,
                    t.tipo_telefone AS tipo,
                    t.status_telefone AS status
                FROM rico.cad_telefones t
                WHERE t.ndg IN (:ndgs)
                ORDER BY t.ndg
                """;

        Map<Long, List<TelefoneExportacao>> resultado = new HashMap<>();
        jdbcTemplate.query(sql, Map.of("ndgs", ndgs), rs -> {
            long ndg = rs.getLong("ndg");
            resultado.computeIfAbsent(ndg, key -> new ArrayList<>())
                    .add(new TelefoneExportacao(
                            rs.getString("ddd"),
                            rs.getString("numero"),
                            rs.getString("tipo"),
                            rs.getString("status")
                    ));
        });
        return resultado;
    }

    public Map<Long, List<EnderecoExportacao>> buscarEnderecos(List<Long> ndgs) {
        if (ndgs.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT DISTINCT
                    e.ndg,
                    TRIM(e.cep) AS cep,
                    TRIM(e.logradouro) AS logradouro,
                    TRIM(e.numero) AS numero,
                    TRIM(e.complemento) AS complemento,
                    TRIM(e.bairro) AS bairro,
                    TRIM(e.cidade) AS cidade,
                    TRIM(e.estado) AS estado,
                    e.tipo_endereco AS tipo,
                    e.endereco_principal AS principal
                FROM rico.cad_enderecos e
                WHERE e.ndg IN (:ndgs)
                ORDER BY e.ndg
                """;

        Map<Long, List<EnderecoExportacao>> resultado = new HashMap<>();
        jdbcTemplate.query(sql, Map.of("ndgs", ndgs), rs -> {
            long ndg = rs.getLong("ndg");
            resultado.computeIfAbsent(ndg, key -> new ArrayList<>())
                    .add(new EnderecoExportacao(
                            rs.getString("cep"),
                            rs.getString("logradouro"),
                            rs.getString("numero"),
                            rs.getString("complemento"),
                            rs.getString("bairro"),
                            rs.getString("cidade"),
                            rs.getString("estado"),
                            rs.getString("tipo"),
                            rs.getBoolean("principal")
                    ));
        });
        return resultado;
    }

    public Map<Long, List<OperacaoExportacao>> buscarOperacoes(List<Long> ndgs, long lote) {
        if (ndgs.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT DISTINCT
                    o.ndg,
                    o.id_operacao AS id,
                    o.nome AS nome,
                    o.status_operacao AS situacao,
                    o.data_inclusao AS dataInclusao,
                    o.data_vencimento AS dataVencimento,
                    o.valor_aquisicao_negocial AS valorReferencia,
                    p.vl_piso AS valorPiso,
                    o.valor_atualizado AS saldo
                FROM rico.operacoes o
                INNER JOIN ricoaux.tbl_est_piso p
                    ON p.id_operacao = o.id_operacao
                WHERE o.ndg IN (:ndgs)
                  AND o.lote = :lote
                ORDER BY o.ndg, o.id_operacao
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ndgs", ndgs)
                .addValue("lote", lote);

        Map<Long, List<OperacaoExportacao>> resultado = new HashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            long ndg = rs.getLong("ndg");
            resultado.computeIfAbsent(ndg, key -> new ArrayList<>())
                    .add(new OperacaoExportacao(
                            rs.getLong("id"),
                            rs.getString("nome"),
                            rs.getString("situacao"),
                            toLocalDateTime(rs.getTimestamp("dataInclusao")),
                            toLocalDate(rs.getDate("dataVencimento")),
                            rs.getBigDecimal("valorReferencia"),
                            rs.getBigDecimal("valorPiso"),
                            rs.getBigDecimal("saldo"),
                            100
                    ));
        });
        return resultado;
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
