package br.com.nog.exporter.delivery;

import java.nio.file.Path;

public interface FileDeliveryService {

    void publish(Path file);
}
