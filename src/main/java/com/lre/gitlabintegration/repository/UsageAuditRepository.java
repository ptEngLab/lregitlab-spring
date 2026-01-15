package com.lre.gitlabintegration.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UsageAuditRepository {

    private final JdbcTemplate jdbc;

    public void insert(UsageAuditRow r) {
        jdbc.update("""
            INSERT INTO audit_gitlab_lre_usage (
              gitlab_project_id, gitlab_project_name, gitlab_project_path,
              gitlab_user_id, gitlab_username,
              lre_domain, lre_project, ref, tag,
              outcome, message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            r.gitlabProjectId(), r.gitlabProjectName(), r.gitlabProjectPath(),
            r.gitlabUserId(), r.gitlabUsername(),
            r.lreDomain(), r.lreProject(), r.ref(), r.tag(),
            r.outcome(), r.message()
        );
    }

    public record UsageAuditRow(
            long gitlabProjectId,
            String gitlabProjectName,
            String gitlabProjectPath,
            long gitlabUserId,
            String gitlabUsername,
            String lreDomain,
            String lreProject,
            String ref,
            int tag,
            String outcome,
            String message
    ) {}
}
