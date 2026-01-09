package com.lre.gitlabintegration.repository;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.repository.mapper.GitLabCommitRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class SyncStateRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JdbcTemplate jdbcTemplate;

    public SyncStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GitLabCommit> findPreviousCommits(SyncRequest request) {
        String sql = """
            SELECT commit_sha, script_path_unix, committed_date
            FROM gitlab_lre_sync_state
            WHERE gitlab_project_id = ?
              AND lre_domain = ?
              AND lre_project = ?
            """;

        return jdbcTemplate.query(
                sql,
                new GitLabCommitRowMapper(),
                request.getGitlabProjectId(),
                request.getLreDomain(),
                request.getLreProject()
        );
    }

    @Transactional
    public void saveCommits(SyncRequest request, List<GitLabCommit> commits) {
        String deleteSql = """
            DELETE FROM gitlab_lre_sync_state
            WHERE gitlab_project_id = ?
              AND lre_domain = ?
              AND lre_project = ?
            """;

        jdbcTemplate.update(
                deleteSql,
                request.getGitlabProjectId(),
                request.getLreDomain(),
                request.getLreProject()
        );

        String insertSql = """
            INSERT INTO gitlab_lre_sync_state
            (gitlab_project_id, lre_domain, lre_project, lre_script_id,
             script_path_unix, commit_sha, committed_date, last_synced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String now = LocalDateTime.now().format(FORMATTER);

        for (GitLabCommit commit : commits) {
            jdbcTemplate.update(
                    insertSql,
                    request.getGitlabProjectId(),
                    request.getLreDomain(),
                    request.getLreProject(),
                    null, // lre_script_id - set after upload
                    commit.getPath(),
                    commit.getSha(),
                    commit.getCommittedDate(),
                    now
            );
        }

    }

    @Transactional
    public void updateScriptId(SyncRequest request, String scriptPath, Integer lreScriptId) {
        String sql = """
            UPDATE gitlab_lre_sync_state
            SET lre_script_id = ?,
                last_synced_at = ?
            WHERE gitlab_project_id = ?
              AND lre_domain = ?
              AND lre_project = ?
              AND script_path_unix = ?
            """;

        jdbcTemplate.update(
                sql,
                lreScriptId,
                LocalDateTime.now().format(FORMATTER),
                request.getGitlabProjectId(),
                request.getLreDomain(),
                request.getLreProject(),
                scriptPath
        );
    }

    @Transactional
    public void deleteCommits(SyncRequest request, List<GitLabCommit> commits) {
        String sql = """
            DELETE FROM gitlab_lre_sync_state
            WHERE gitlab_project_id = ?
              AND lre_domain = ?
              AND lre_project = ?
              AND script_path_unix = ?
            """;

        for (GitLabCommit commit : commits) {
            jdbcTemplate.update(
                    sql,
                    request.getGitlabProjectId(),
                    request.getLreDomain(),
                    request.getLreProject(),
                    commit.getPath()
            );
        }
    }
}
