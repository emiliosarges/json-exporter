package br.com.nog.exporter.dto;

import java.time.Instant;
import java.util.UUID;

public record ExportJobResponse(
        UUID jobId,
        long lote,
        ExportJobStatus status,
        long clientesProcessados,
        long ultimoNdg,
        Integer arquivosGerados,
        String erro,
        Instant criadoEm,
        Instant iniciadoEm,
        Instant finalizadoEm
) {
}
