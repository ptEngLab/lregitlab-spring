package com.lre.gitlabintegration.controller;

import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.security.GitLabCiJobTokenAuthFilter;
import com.lre.gitlabintegration.services.git.sync.GitSyncService;
import com.lre.gitlabintegration.services.security.LreAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lre")
@Slf4j
@RequiredArgsConstructor
public class SyncController {

    private final GitSyncService gitSyncService;
    private final LreAuthorizationService lreAuthorizationService;

    @PostMapping("/domain/{domain}/project/{project}/sync")
    public ResponseEntity<@NonNull SyncResponse> sync(
            @NonNull @PathVariable String domain,
            @NonNull @PathVariable String project,
            @NonNull Authentication authentication
    ) {

        if (!(authentication.getPrincipal() instanceof GitLabCiJobTokenAuthFilter.GitLabCiPrincipal principal)) {
            throw new AccessDeniedException("Invalid principal type");
        }

        domain = domain.trim();
        project = project.trim();

        String username = principal.gitlabUsername().trim();
        if (username.isEmpty()) throw new AccessDeniedException("Missing username");

        lreAuthorizationService.assertUserHasProjectAccess(username, domain, project);

        SyncRequest request = new SyncRequest(
                principal.gitlabProjectId(),
                principal.ref(),
                domain,
                project
        );

        log.info("Sync request: gitlabProjectId={}, lreDomain={}, lreProject={}, ref={}",
                principal.gitlabProjectId(), domain, project, principal.ref());

        SyncResponse response = gitSyncService.sync(request);

        return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
