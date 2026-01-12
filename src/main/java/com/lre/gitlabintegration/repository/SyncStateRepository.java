package com.lre.gitlabintegration.repository;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncStateEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
public class SyncStateRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JdbcTemplate jdbcTemplate;

    public SyncStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SyncStateEntry> findPreviousState(SyncRequest request) {
        String sql = """
                SELECT script_path_unix, commit_sha, committed_date, lre_script_id
                FROM gitlab_lre_sync_state
                WHERE gitlab_project_id = ?
                  AND lre_domain = ?
                  AND lre_project = ?
                """;

        return jdbcTemplate.query(sql,
                (rs, rowNum) ->
                        new SyncStateEntry(
                                rs.getString("script_path_unix"),
                                rs.getString("commit_sha"),
                                rs.getString("committed_date"),
                                (Integer) rs.getObject("lre_script_id")
                        ),
                request.getGitlabProjectId(),
                request.getLreDomain(),
                request.getLreProject()
        );
    }


    @Transactional
    public void saveCommits(SyncRequest request, List<GitLabCommit> commits, Map<String, Integer> lreIdByPath) {
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
            Integer lreId = lreIdByPath.get(commit.getPath());

            jdbcTemplate.update(
                    insertSql,
                    request.getGitlabProjectId(),
                    request.getLreDomain(),
                    request.getLreProject(),
                    lreId,
                    commit.getPath(),
                    commit.getSha(),
                    commit.getCommittedDate(),
                    now
            );
        }

    }

}
