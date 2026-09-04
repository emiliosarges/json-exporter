package br.com.nog.exporter.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "app.delivery.mode", havingValue = "SFTP")
public class SftpFileDeliveryService implements FileDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SftpFileDeliveryService.class);

    private final SftpRemoteFileTemplate template;

    public SftpFileDeliveryService(SftpRemoteFileTemplate template) {
        this.template = template;
    }

    @Override
    public void publish(Path file) {
        String remotePath = template.send(
                MessageBuilder.withPayload(file.toFile()).build(),
                FileExistsMode.REPLACE
        );

        if (remotePath == null) {
            throw new IllegalStateException("SFTP não retornou o caminho remoto para " + file.getFileName());
        }

        log.info("Arquivo enviado para SFTP: {} -> {}", file.getFileName(), remotePath);
    }
}
