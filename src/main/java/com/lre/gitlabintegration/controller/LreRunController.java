package com.lre.gitlabintegration.controller;

import com.lre.gitlabintegration.dto.lrescript.DownloadableStream;
import com.lre.gitlabintegration.security.GitLabCiPrincipal;
import com.lre.gitlabintegration.services.lre.run.LreRunOrchestrationService;
import jakarta.validation.constraints.Min;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/lre/domains/{domain}/projects/{project}/runs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LreRunController {

    private final LreRunOrchestrationService orchestrationService;

    // GET /api/lre/domains/{domain}/projects/{project}/runs/{runId}/status
    @GetMapping("/{runId}/status")
    public ResponseEntity<@NonNull RunStatus> getRunStatus(
            @PathVariable String domain,
            @PathVariable String project,
            @PathVariable @Min(value = 1, message = "Run ID must be at least 1") int runId,
            @AuthenticationPrincipal GitLabCiPrincipal principal
    ) {
        RunStatus status =
                orchestrationService.checkRunStatus(domain, project, runId, principal);
        return ResponseEntity.ok(status);
    }

    // GET /api/lre/domains/{domain}/projects/{project}/runs/{runId}/status/extended
    @GetMapping("/{runId}/status/extended")
    public ResponseEntity<@NonNull List<RunStatusExtended>> getRunStatusExtended(
            @PathVariable String domain,
            @PathVariable String project,
            @PathVariable @Min(value = 1, message = "Run ID must be at least 1") int runId,
            @AuthenticationPrincipal GitLabCiPrincipal principal
    ) {
        List<RunStatusExtended> results =
                orchestrationService.fetchRunStatusExtended(domain, project, runId, principal);
        return ResponseEntity.ok(results);
    }

    // POST /api/lre/domains/{domain}/projects/{project}/runs/{runId}/abort
    @PostMapping("/{runId}/abort")
    public ResponseEntity<@NonNull Void> abortRun(
            @PathVariable String domain,
            @PathVariable String project,
            @PathVariable @Min(value = 1, message = "Run ID must be at least 1") int runId,
            @AuthenticationPrincipal GitLabCiPrincipal principal
    ) {
        orchestrationService.abortRun(domain, project, runId, principal);
        return ResponseEntity.noContent().build();
    }


    // GET /api/lre/domains/{domain}/projects/{project}/runs/{runId}/results/{resultId}/download
    @GetMapping("/{runId}/results/{resultId}/download")
    public ResponseEntity<@NonNull StreamingResponseBody> downloadRunResultFile(
            @PathVariable String domain,
            @PathVariable String project,
            @PathVariable @Min(value = 1, message = "Run ID must be at least 1") int runId,
            @PathVariable @Min(value = 1, message = "Result ID must be at least 1") int resultId,
            @AuthenticationPrincipal GitLabCiPrincipal principal
    ) {
        DownloadableStream downloadable =
                orchestrationService.downloadRunResultFile(domain, project, runId, resultId, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        fileDownloadService.contentDisposition(downloadable.fileName()))
                // safest generic type (since we can't pre-read LRE headers reliably)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(downloadable.body());
    }

    @GetMapping("/{runId}/results/{resultId}/download-ui")
    public ResponseEntity<@NonNull StreamingResponseBody> downloadRunResultFileViaUi(
            @PathVariable String domain,
            @PathVariable String project,
            @PathVariable @Min(1) int runId,
            @PathVariable @Min(1) int resultId,
            @AuthenticationPrincipal GitLabCiPrincipal principal
    ) {
        DownloadableStream downloadable =
                orchestrationService.downloadRunResultViaUi(domain, project, runId, resultId, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        fileDownloadService.contentDisposition(downloadable.fileName()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(downloadable.body());
    }

}
