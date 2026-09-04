package br.com.nog.exporter.controller;

import br.com.nog.exporter.dto.ExportJobResponse;
import br.com.nog.exporter.service.ExportCoordinator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportCoordinator coordinator;

    public ExportController(ExportCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @PostMapping("/lotes/{lote}")
    public ResponseEntity<ExportJobResponse> exportar(@PathVariable long lote) {
        ExportJobResponse job = coordinator.start(lote);
        return ResponseEntity.accepted()
                .location(URI.create("/api/exports/" + job.jobId()))
                .body(job);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ExportJobResponse> status(@PathVariable UUID jobId) {
        return coordinator.find(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
