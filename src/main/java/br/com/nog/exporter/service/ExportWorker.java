package br.com.nog.exporter.service;

import br.com.nog.exporter.dto.ExportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExportWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);

    private final ClienteExportService exportService;
    private final ExportJobStore jobStore;

    public ExportWorker(ClienteExportService exportService, ExportJobStore jobStore) {
        this.exportService = exportService;
        this.jobStore = jobStore;
    }

    @Async("exportExecutor")
    public void run(UUID jobId, long lote) {
        jobStore.markRunning(jobId);
        log.info("Iniciando exportação do lote {}. Job={}", lote, jobId);

        try {
            ExportSummary summary = exportService.exportar(
                    lote,
                    (processed, lastNdg) -> jobStore.updateProgress(jobId, processed, lastNdg)
            );
            jobStore.markCompleted(jobId, summary);
            log.info("Exportação concluída. Job={}, lote={}, clientes={}, arquivos={}",
                    jobId, lote, summary.clientesExportados(), summary.arquivosGerados());
        } catch (Exception e) {
            jobStore.markFailed(jobId, e);
            log.error("Falha na exportação. Job={}, lote={}", jobId, lote, e);
        }
    }
}
