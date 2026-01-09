package com.lre.gitlabintegration.controller;

import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.services.git.sync.GitSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class SyncController {

    private final GitSyncService gitSyncService;

    @PostMapping("/sync")
    public ResponseEntity<@NonNull SyncResponse> sync(
            @Valid @RequestBody SyncRequest request
    ) {
        log.info(
                "Received sync request for project: {}, ref: {}",
                request.getLreProject(),
                request.getRef()
        );

        SyncResponse response = gitSyncService.sync(request);

        log.info(
                "Sync completed - Success: {}, Uploaded: {}, Deleted: {}, Unchanged: {}, Failed: {}",
                response.success(),
                response.summary().uploaded(),
                response.summary().deleted(),
                response.summary().unchanged(),
                response.summary().failed()
        );

        return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
