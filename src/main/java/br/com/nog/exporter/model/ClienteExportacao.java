package br.com.nog.exporter.model;

import java.time.LocalDate;
import java.util.List;

public record ClienteExportacao(
        String cpfCnpj,
        String nomeCliente,
        String tipoPessoa,
        LocalDate dataNascimento,
        String sexo,
        String estadoCivil,
        String profissao,
        Long codigo,
        List<TelefoneExportacao> telefones,
        List<EmailExportacao> emails,
        List<EnderecoExportacao> enderecos,
        List<OperacaoExportacao> operacoes
) {
}
