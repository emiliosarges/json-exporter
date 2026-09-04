package br.com.nog.exporter.model;

import java.time.LocalDate;

public record ClienteBase(
        String cpfCnpj,
        String nomeCliente,
        String tipoPessoa,
        LocalDate dataNascimento,
        String sexo,
        String estadoCivil,
        String profissao,
        Long codigo,
        String email
) {
}
