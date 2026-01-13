package com.lre.gitlabintegration.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Locale;

@Repository
public class AllowedLreAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public AllowedLreAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Strict authorization:
     * caller must match BOTH gitlab project + gitlab user and be allowed for the LRE domain/project.
     */
    public boolean isAllowed(long gitlabProjectId, long gitlabUserId, String lreDomain, String lreProject) {
        String domain = normalize(lreDomain);
        String project = normalize(lreProject);

        if (domain.isBlank() || project.isBlank()) return false;

        String sql = """
                SELECT EXISTS(
                    SELECT 1
                    FROM allowed_lre_access
                    WHERE enabled = 1
                      AND gitlab_project_id = ?
                      AND gitlab_user_id = ?
                      AND lre_domain = ?
                      AND lre_project = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                gitlabProjectId,
                gitlabUserId,
                domain,
                project
        );

        return Boolean.TRUE.equals(exists);
    }

    /**
     * Convenience method: insert or update an allowlist entry.
     * Uses SQLite upsert.
     */
    public void upsert(long gitlabProjectId, long gitlabUserId, String lreDomain, String lreProject, boolean enabled) {
        String domain = normalize(lreDomain);
        String project = normalize(lreProject);

        String sql = """
                INSERT INTO allowed_lre_access
                  (gitlab_project_id, gitlab_user_id, lre_domain, lre_project, enabled)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(gitlab_project_id, gitlab_user_id, lre_domain, lre_project)
                DO UPDATE SET enabled = excluded.enabled
                """;

        jdbcTemplate.update(sql,
                gitlabProjectId,
                gitlabUserId,
                domain,
                project,
                enabled ? 1 : 0
        );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
