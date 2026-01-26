package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.GitLabApiClient;
import com.lre.gitlabintegration.dto.audit.AuditContext;
import com.lre.gitlabintegration.dto.gitlab.GitLabProjectInfo;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.repository.GitLabProjectCacheRepository;
import com.lre.gitlabintegration.security.GitLabCiPrincipal;
import com.lre.gitlabintegration.services.audit.AuditService;
import com.lre.gitlabintegration.services.audit.AuditStatus;
import com.lre.gitlabintegration.services.security.LreAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncOrchestrationService {

    private final GitSyncService gitSyncService;
    private final LreAuthorizationService lreAuthorizationService;
    private final AuditService auditService;
    private final GitLabProjectCacheRepository projectCacheRepo;
    private final GitLabApiClient gitLabApiClient;

    public SyncResponse handleSync(String domain, String project, Authentication authentication) {
        domain = domain == null ? "" : domain.trim();
        project = project == null ? "" : project.trim();

        if (authentication == null || authentication.getPrincipal() == null) {
            auditService.insertDenied(domain, project, "Missing authentication");
            throw new AccessDeniedException("Missing authentication");
        }

        if (!(authentication.getPrincipal() instanceof GitLabCiPrincipal principal)) {
            auditService.insertDenied(domain, project, "Invalid principal type");
            throw new AccessDeniedException("Invalid principal type");
        }

        String username = principal.gitlabUsername() == null ? "" : principal.gitlabUsername().trim();
        int tag = principal.tag() ? 1 : 0;
        var ctx = new AuditContext(principal, null, username, domain, project, tag);

        if (username.isEmpty()) {
            auditService.insertAudit(ctx, AuditStatus.DENIED, "Missing username");
            throw new AccessDeniedException("Missing username");
        }

        // Project info is nice-to-have for audit; do not block if lookup fails
        GitLabProjectInfo projectInfo = null;
        try {
            projectInfo = getGitLabProjectInfo(principal.gitlabProjectId());
        } catch (Exception e) {
            log.warn("Failed to resolve GitLab project info for projectId={}: {}",
                    principal.gitlabProjectId(), e.getMessage());
        }

        ctx = new AuditContext(principal, projectInfo, username, domain, project, tag);
        try {
            lreAuthorizationService.assertUserHasProjectAccess(username, domain, project);
        } catch (AccessDeniedException ex) {

            auditService.insertAudit(ctx, AuditStatus.DENIED, ex.getMessage());
            throw ex;
        }

        SyncRequest request = new SyncRequest(
                principal.gitlabProjectId(),
                principal.ref(),
                domain,
                project
        );

        log.info("Sync request: gitlabProjectId={}, gitlabUserId={}, lreDomain={}, lreProject={}, ref={}",
                principal.gitlabProjectId(), principal.gitlabUserId(), domain, project, principal.ref());

        SyncResponse response = gitSyncService.sync(request);

        auditService.insertAudit(
                ctx,
                response.success() ? AuditStatus.SUCCESS : AuditStatus.FAIL,
                response.success() ? null : "Sync failed"
        );

        return response;
    }

    private GitLabProjectInfo getGitLabProjectInfo(long gitlabProjectId) {
        var cached = projectCacheRepo.find(gitlabProjectId);
        if (cached != null) {
            return new GitLabProjectInfo(
                    gitlabProjectId,
                    cached.name(),
                    cached.pathWithNamespace(),
                    cached.webUrl()
            );
        }

        GitLabProjectInfo p = gitLabApiClient.getProjectInfo(gitlabProjectId);
        if (p != null) projectCacheRepo.upsert(p.id(), p.name(), p.pathWithNamespace(), p.webUrl());

        return p;
    }

}
