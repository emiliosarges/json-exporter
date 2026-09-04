package br.com.nog.exporter.model;

public record EnderecoExportacao(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String tipo,
        boolean principal
) {
}
