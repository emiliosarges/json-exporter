package br.com.nog.exporter.model;

public record TelefoneExportacao(
        String ddd,
        String numero,
        String tipo,
        String status
) {
}
