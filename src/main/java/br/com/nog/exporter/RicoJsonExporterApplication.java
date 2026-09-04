package br.com.nog.exporter;

import br.com.nog.exporter.config.ExportProperties;
import br.com.nog.exporter.config.SftpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties({ExportProperties.class, SftpProperties.class})
public class RicoJsonExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RicoJsonExporterApplication.class, args);
    }
}
