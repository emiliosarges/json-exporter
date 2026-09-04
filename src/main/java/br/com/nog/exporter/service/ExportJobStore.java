package br.com.nog.exporter.service;

import br.com.nog.exporter.dto.ExportJobResponse;
import br.com.nog.exporter.dto.ExportJobStatus;
import br.com.nog.exporter.dto.ExportSummary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExportJobStore {

    private final Map<UUID, MutableJob> jobs = new ConcurrentHashMap<>();

    public ExportJobResponse create(long lote) {
        UUID id = UUID.randomUUID();
        MutableJob job = new MutableJob(id, lote);
        jobs.put(id, job);
        return job.snapshot();
    }

    public Optional<ExportJobResponse> find(UUID id) {
        MutableJob job = jobs.get(id);
        return job == null ? Optional.empty() : Optional.of(job.snapshot());
    }

    public void markRunning(UUID id) {
        job(id).markRunning();
    }

    public void updateProgress(UUID id, long processed, long lastNdg) {
        job(id).updateProgress(processed, lastNdg);
    }

    public void markCompleted(UUID id, ExportSummary summary) {
        job(id).markCompleted(summary);
    }

    public void markFailed(UUID id, Throwable error) {
        job(id).markFailed(error);
    }

    private MutableJob job(UUID id) {
        MutableJob job = jobs.get(id);
        if (job == null) {
            throw new IllegalArgumentException("Job não encontrado: " + id);
        }
        return job;
    }

    private static final class MutableJob {
        private final UUID id;
        private final long lote;
        private final Instant createdAt = Instant.now();
        private ExportJobStatus status = ExportJobStatus.QUEUED;
        private long processed;
        private long lastNdg;
        private Integer files;
        private String error;
        private Instant startedAt;
        private Instant finishedAt;

        private MutableJob(UUID id, long lote) {
            this.id = id;
            this.lote = lote;
        }

        synchronized void markRunning() {
            status = ExportJobStatus.RUNNING;
            startedAt = Instant.now();
        }

        synchronized void updateProgress(long processed, long lastNdg) {
            this.processed = processed;
            this.lastNdg = lastNdg;
        }

        synchronized void markCompleted(ExportSummary summary) {
            this.processed = summary.clientesExportados();
            this.files = summary.arquivosGerados();
            this.status = ExportJobStatus.COMPLETED;
            this.finishedAt = Instant.now();
        }

        synchronized void markFailed(Throwable throwable) {
            this.status = ExportJobStatus.FAILED;
            this.error = throwable.getMessage();
            this.finishedAt = Instant.now();
        }

        synchronized ExportJobResponse snapshot() {
            return new ExportJobResponse(
                    id,
                    lote,
                    status,
                    processed,
                    lastNdg,
                    files,
                    error,
                    createdAt,
                    startedAt,
                    finishedAt
            );
        }
    }
}
