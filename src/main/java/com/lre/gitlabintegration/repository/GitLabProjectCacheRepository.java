package com.lre.gitlabintegration.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GitLabProjectCacheRepository {

    private final JdbcTemplate jdbc;

    public record ProjectInfo(String name, String pathWithNamespace, String webUrl) {
    }

    public ProjectInfo find(long projectId) {
        var rows = jdbc.query("""
                        SELECT name, path_with_namespace, web_url
                        FROM gitlab_project
                        WHERE gitlab_project_id = ?
                        """,
                (rs, rowNum) -> new ProjectInfo(
                        rs.getString("name"), rs.getString("path_with_namespace"),
                        rs.getString("web_url")),
                projectId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }


    public void upsert(long projectId, String name, String pathWithNamespace, String webUrl) {
        jdbc.update("""
                        INSERT INTO gitlab_project (gitlab_project_id, name, path_with_namespace, web_url, last_updated_utc)
                        VALUES (?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
                        ON CONFLICT(gitlab_project_id) DO UPDATE SET
                          name = excluded.name,
                          path_with_namespace = excluded.path_with_namespace,
                          web_url = excluded.web_url,
                          last_updated_utc = strftime('%Y-%m-%dT%H:%M:%fZ','now')
                        """,
                projectId, name, pathWithNamespace, webUrl
        );
    }

}
