package br.com.nog.exporter.service;

import br.com.nog.exporter.config.ExportProperties;
import br.com.nog.exporter.delivery.FileDeliveryService;
import br.com.nog.exporter.model.ClienteExportacao;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RotatingJsonFileWriter implements AutoCloseable {

    private final ObjectMapper objectMapper;
    private final ExportProperties properties;
    private final FileDeliveryService deliveryService;
    private final long lote;
    private final List<Path> generatedFiles = new ArrayList<>();

    private int sequence;
    private long recordsInCurrentFile;
    private long totalRecords;
    private JsonGenerator generator;
    private CountingOutputStream countingOutputStream;
    private Path currentFile;

    public RotatingJsonFileWriter(
            ObjectMapper objectMapper,
            ExportProperties properties,
            FileDeliveryService deliveryService,
            long lote) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.deliveryService = deliveryService;
        this.lote = lote;
    }

    public void write(ClienteExportacao cliente) throws IOException {
        if (generator == null) {
            openNextFile();
        }

        if (mustRotateBeforeNextRecord()) {
            closeCurrentFile();
            openNextFile();
        }

        generator.writeObject(cliente);
        recordsInCurrentFile++;
        totalRecords++;

        if (properties.getSizeCheckInterval() > 0
                && recordsInCurrentFile % properties.getSizeCheckInterval() == 0) {
            generator.flush();
        }
    }

    private boolean mustRotateBeforeNextRecord() throws IOException {
        if (recordsInCurrentFile >= properties.getMaxRecordsPerFile()) {
            return true;
        }

        if (recordsInCurrentFile == 0) {
            return false;
        }

        if (recordsInCurrentFile % properties.getSizeCheckInterval() == 0) {
            generator.flush();
        }

        return countingOutputStream.getCount() >= properties.maxFileSizeBytes();
    }

    private void openNextFile() throws IOException {
        Files.createDirectories(properties.getOutputDir());

        sequence++;
        recordsInCurrentFile = 0;
        currentFile = properties.getOutputDir().resolve(fileName(lote, sequence));

        OutputStream raw = Files.newOutputStream(
                currentFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        countingOutputStream = new CountingOutputStream(new BufferedOutputStream(raw, 1024 * 1024));
        generator = objectMapper.getFactory().createGenerator(countingOutputStream);
        generator.writeStartArray();
        generatedFiles.add(currentFile);
    }

    private void closeCurrentFile() throws IOException {
        if (generator == null) {
            return;
        }

        generator.writeEndArray();
        generator.close();
        generator = null;
        countingOutputStream = null;

        deliveryService.publish(currentFile);
        currentFile = null;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public int getFileCount() {
        return generatedFiles.size();
    }

    public List<Path> getGeneratedFiles() {
        return List.copyOf(generatedFiles);
    }

    static String fileName(long lote, int sequence) {
        return "NOG" + lote + "_" + String.format("%04d", sequence) + ".json";
    }

    @Override
    public void close() throws IOException {
        closeCurrentFile();
    }
}
