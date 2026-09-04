package br.com.nog.exporter.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RotatingJsonFileWriterTest {

    @Test
    void deveMontarNomeComLoteESequencia() {
        assertThat(RotatingJsonFileWriter.fileName(123, 1))
                .isEqualTo("NOG123_0001.json");

        assertThat(RotatingJsonFileWriter.fileName(9876, 42))
                .isEqualTo("NOG9876_0042.json");
    }
}
