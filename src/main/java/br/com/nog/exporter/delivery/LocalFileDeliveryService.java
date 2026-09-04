package br.com.nog.exporter.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "app.delivery.mode", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileDeliveryService implements FileDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileDeliveryService.class);

    @Override
    public void publish(Path file) {
        log.info("Arquivo finalizado localmente: {}", file.toAbsolutePath());
    }
}
