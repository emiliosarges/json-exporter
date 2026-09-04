package br.com.nog.exporter.service;

import br.com.nog.exporter.config.ExportProperties;
import br.com.nog.exporter.delivery.FileDeliveryService;
import br.com.nog.exporter.dto.ExportSummary;
import br.com.nog.exporter.model.ClienteBase;
import br.com.nog.exporter.model.ClienteExportacao;
import br.com.nog.exporter.model.EmailExportacao;
import br.com.nog.exporter.model.EnderecoExportacao;
import br.com.nog.exporter.model.OperacaoExportacao;
import br.com.nog.exporter.model.TelefoneExportacao;
import br.com.nog.exporter.repository.ClienteExportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
public class ClienteExportService {

    private final ClienteExportRepository repository;
    private final ObjectMapper objectMapper;
    private final ExportProperties properties;
    private final FileDeliveryService deliveryService;

    public ClienteExportService(
            ClienteExportRepository repository,
            ObjectMapper objectMapper,
            ExportProperties properties,
            FileDeliveryService deliveryService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.deliveryService = deliveryService;
    }

    public ExportSummary exportar(long lote, BiConsumer<Long, Long> progressCallback) throws IOException {
        Instant startedAt = Instant.now();
        limparArquivosAnteriores(lote);
        long lastNdg = 0;
        long processed = 0;

        try (RotatingJsonFileWriter writer = new RotatingJsonFileWriter(
                objectMapper,
                properties,
                deliveryService,
                lote)) {

            while (true) {
                List<ClienteBase> clientes = repository.buscarClientes(
                        lote,
                        lastNdg,
                        properties.getPageSize()
                );

                if (clientes.isEmpty()) {
                    break;
                }

                List<Long> ndgs = clientes.stream()
                        .map(ClienteBase::codigo)
                        .toList();

                Map<Long, List<TelefoneExportacao>> telefones = repository.buscarTelefones(ndgs);
                Map<Long, List<EnderecoExportacao>> enderecos = repository.buscarEnderecos(ndgs);
                Map<Long, List<OperacaoExportacao>> operacoes = repository.buscarOperacoes(ndgs, lote);

                for (ClienteBase cliente : clientes) {
                    writer.write(montarCliente(cliente, telefones, enderecos, operacoes));
                    processed++;
                }

                lastNdg = clientes.get(clientes.size() - 1).codigo();
                progressCallback.accept(processed, lastNdg);
            }

            return ExportSummary.of(
                    lote,
                    writer.getTotalRecords(),
                    writer.getGeneratedFiles(),
                    Duration.between(startedAt, Instant.now())
            );
        }
    }

    private void limparArquivosAnteriores(long lote) throws IOException {
        Files.createDirectories(properties.getOutputDir());
        String glob = "NOG" + lote + "_*.json";

        try (DirectoryStream<Path> files = Files.newDirectoryStream(properties.getOutputDir(), glob)) {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        }
    }

    private ClienteExportacao montarCliente(
            ClienteBase cliente,
            Map<Long, List<TelefoneExportacao>> telefones,
            Map<Long, List<EnderecoExportacao>> enderecos,
            Map<Long, List<OperacaoExportacao>> operacoes) {

        List<EmailExportacao> emails = cliente.email() == null || cliente.email().isBlank()
                ? List.of()
                : List.of(new EmailExportacao(cliente.email().trim(), true));

        return new ClienteExportacao(
                cliente.cpfCnpj(),
                cliente.nomeCliente(),
                cliente.tipoPessoa(),
                cliente.dataNascimento(),
                cliente.sexo(),
                cliente.estadoCivil(),
                cliente.profissao(),
                cliente.codigo(),
                telefones.getOrDefault(cliente.codigo(), List.of()),
                emails,
                enderecos.getOrDefault(cliente.codigo(), List.of()),
                operacoes.getOrDefault(cliente.codigo(), List.of())
        );
    }
}
