package com.lre.gitlabintegration.controller;

import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.services.git.sync.SyncOrchestrationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lre")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncOrchestrationService syncOrchestrationService;

    @PostMapping("/domain/{domain}/project/{project}/sync")
    public ResponseEntity<@NonNull SyncResponse> sync(
            @PathVariable String domain,
            @PathVariable String project,
            Authentication authentication
    ) {
        SyncResponse response = syncOrchestrationService.handleSync(domain, project, authentication);
        return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }


}
