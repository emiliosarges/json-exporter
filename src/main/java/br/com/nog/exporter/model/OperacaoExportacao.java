package br.com.nog.exporter.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OperacaoExportacao(
        Long id,
        String nome,
        String situacao,
        LocalDateTime dataInclusao,
        LocalDate dataVencimento,
        BigDecimal valorReferencia,
        BigDecimal valorPiso,
        BigDecimal saldo,
        int oferta
) {
}
