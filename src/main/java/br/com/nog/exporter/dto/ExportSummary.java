package br.com.nog.exporter.dto;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public record ExportSummary(
        long lote,
        long clientesExportados,
        int arquivosGerados,
        List<String> arquivos,
        long duracaoSegundos
) {
    public static ExportSummary of(long lote, long clientes, List<Path> arquivos, Duration duracao) {
        return new ExportSummary(
                lote,
                clientes,
                arquivos.size(),
                arquivos.stream().map(path -> path.toAbsolutePath().toString()).toList(),
                duracao.toSeconds()
        );
    }
}
