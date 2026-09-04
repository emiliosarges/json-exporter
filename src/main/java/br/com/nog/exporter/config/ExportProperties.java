package br.com.nog.exporter.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "app.export")
public class ExportProperties {

    @NotNull
    private Path outputDir = Path.of("./output");

    @Min(1)
    private int pageSize = 2000;

    @Min(1)
    private long maxRecordsPerFile = 100_000;

    @Min(1)
    private long maxFileSizeMb = 250;

    @Min(1)
    private int sizeCheckInterval = 500;

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getMaxRecordsPerFile() {
        return maxRecordsPerFile;
    }

    public void setMaxRecordsPerFile(long maxRecordsPerFile) {
        this.maxRecordsPerFile = maxRecordsPerFile;
    }

    public long getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(long maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public int getSizeCheckInterval() {
        return sizeCheckInterval;
    }

    public void setSizeCheckInterval(int sizeCheckInterval) {
        this.sizeCheckInterval = sizeCheckInterval;
    }

    public long maxFileSizeBytes() {
        return maxFileSizeMb * 1024L * 1024L;
    }
}
