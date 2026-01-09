package com.lre.gitlabintegration.repository;

import com.lre.gitlabintegration.dto.sync.SyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SyncLockRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JdbcTemplate jdbcTemplate;

    public boolean acquireLock(SyncRequest request) {
        String sql = """
            INSERT INTO sync_lock (gitlab_project_id, lre_domain, lre_project, locked_at)
            VALUES (?, ?, ?, ?)
            """;

        try {
            jdbcTemplate.update(
                    sql,
                    request.getGitlabProjectId(),
                    request.getLreDomain(),
                    request.getLreProject(),
                    LocalDateTime.now().format(FORMATTER)
            );

            log.info("Acquired sync lock for project: {}", request.getLreProject());
            return true;
        } catch (DuplicateKeyException e) {
            log.warn("Sync already in progress for project: {}", request.getLreProject());
            return false;
        }
    }

    public void releaseLock(SyncRequest request) {
        String sql = """
            DELETE FROM sync_lock
            WHERE gitlab_project_id = ?
              AND lre_domain = ?
              AND lre_project = ?
            """;

        jdbcTemplate.update(
                sql,
                request.getGitlabProjectId(),
                request.getLreDomain(),
                request.getLreProject()
        );

        log.info("Released sync lock for project: {}", request.getLreProject());
    }
}
