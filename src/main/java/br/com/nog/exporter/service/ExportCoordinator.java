package br.com.nog.exporter.service;

import br.com.nog.exporter.dto.ExportJobResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExportCoordinator {

    private final ExportJobStore jobStore;
    private final ExportWorker worker;

    public ExportCoordinator(ExportJobStore jobStore, ExportWorker worker) {
        this.jobStore = jobStore;
        this.worker = worker;
    }

    public ExportJobResponse start(long lote) {
        if (lote <= 0) {
            throw new IllegalArgumentException("O lote deve ser maior que zero.");
        }

        ExportJobResponse job = jobStore.create(lote);
        worker.run(job.jobId(), lote);
        return job;
    }

    public Optional<ExportJobResponse> find(UUID id) {
        return jobStore.find(id);
    }
}
