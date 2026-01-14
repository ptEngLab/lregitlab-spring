package com.lre.gitlabintegration.client.builder;

import com.lre.gitlabintegration.config.properties.GitLabProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
@Slf4j
public class GitLabUrlFactory {

    private final int perPageRecords;

    public GitLabUrlFactory(GitLabProperties gitlabProperties) {
        this.perPageRecords = gitlabProperties.getPerPageRecords();
    }

    /**
     * Get repository tree URL with pagination
     */
    public String getRepositoryTreeUrl(int page, String ref, long projectId, String path) {
        return UriComponentsBuilder
                .fromPath("projects/{projectId}/repository/tree")
                .queryParam("ref", ref)
                .queryParam("recursive", true)
                .queryParam("per_page", perPageRecords)
                .queryParam("page", page)
                .queryParamIfPresent("path", Optional.ofNullable(path))
                .buildAndExpand(projectId)
                .toUriString();
    }

    public String getRepositoryTreeUrl(int page, long projectId, String ref) {
        return getRepositoryTreeUrl(page, ref, projectId, null);
    }

    /**
     * Get current job URL
     */
    public String getJobUrl() {
        return UriComponentsBuilder
                .fromPath("job")
                .build()
                .toUriString();
    }

    /**
     * Get latest commit URL for a specific path
     */
    public String getLatestCommitUrlForPath(long projectId, String ref, String path) {
        return UriComponentsBuilder
                .fromPath("projects/{projectId}/repository/commits")
                .queryParam("ref_name", ref)
                .queryParamIfPresent("path", Optional.ofNullable(path))
                .queryParam("per_page", 1)
                .buildAndExpand(projectId)
                .toUriString();
    }

    /**
     * Get repository archive URL
     */
    public String getRepositoryArchiveUrl(long projectId, String commitSha, String path) {
        return UriComponentsBuilder
                .fromPath("projects/{projectId}/repository/archive.zip")
                .queryParamIfPresent("path", Optional.ofNullable(path))
                .queryParamIfPresent("sha", Optional.ofNullable(commitSha))
                .buildAndExpand(projectId)
                .toUriString();
    }
}
