package com.lre.gitlabintegration.services.audit;

import com.lre.gitlabintegration.dto.audit.AuditContext;
import com.lre.gitlabintegration.repository.UsageAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final UsageAuditRepository auditRepo;

    public void insertAudit(AuditContext ctx, AuditStatus status, String message) {
        var principal = ctx.principal();
        var projectInfo = ctx.projectInfo();

        String username = ctx.username() == null ? "" : ctx.username().trim();

        auditRepo.insert(new UsageAuditRepository.UsageAuditRow(
                principal.gitlabProjectId(),
                projectInfo != null ? projectInfo.name() : null,
                projectInfo != null ? projectInfo.pathWithNamespace() : null,
                principal.gitlabUserId(),
                username,
                ctx.domain(),
                ctx.project(),
                principal.ref(),
                ctx.tag(),
                status.name(),
                message
        ));
    }


    public void insertDenied(String domain, String project, String message) {
        auditRepo.insert(new UsageAuditRepository.UsageAuditRow(
                0L, null, null, 0L, null,
                domain, project, null, 0,
                AuditStatus.DENIED.name(), message
        ));
    }

}
