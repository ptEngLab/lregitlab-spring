package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.GitLabUrlFactory;
import com.lre.gitlabintegration.config.http.GitLabBaseApiClient;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.gitlab.GitLabTreeItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GitLabApiClient {

    private final GitLabBaseApiClient apiClient;
    private final GitLabUrlFactory gitLabUrlFactory;

    public GitLabApiClient(GitLabBaseApiClient apiClient, GitLabUrlFactory gitLabUrlFactory) {
        this.apiClient = apiClient;
        this.gitLabUrlFactory = gitLabUrlFactory;
    }

    public List<GitLabTreeItem> getRepositoryTree(int page, long projectId, String ref) {
        String url = gitLabUrlFactory.getRepositoryTreeUrl(page, projectId, ref);
        log.debug("Fetching repository tree from: {}", url);
        List<GitLabTreeItem> items = apiClient.get(url, new ParameterizedTypeReference<>() {});
        return items != null ? items : Collections.emptyList();
    }

    public GitLabCommit getLatestCommitForPath(long projectId, String ref, String path) {
        String url = gitLabUrlFactory.getLatestCommitUrlForPath(projectId, ref, path);
        log.debug("Fetching latest commit for path: {}", path);

        List<GitLabCommit> commits = apiClient.get(url, new ParameterizedTypeReference<>() {});
        return (commits != null && !commits.isEmpty()) ? commits.get(0) : new GitLabCommit();
    }

    public boolean downloadRepositoryArchive(long projectId, String commitSha, String path, Path destPath) {
        String url = gitLabUrlFactory.getRepositoryArchiveUrl(projectId, commitSha, path);
        log.debug("Downloading repository archive from: {}", url);

        try {
            prepareDestinationDirectory(destPath);
            byte[] archive = downloadArchive(url);
            Files.write(destPath, archive, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Successfully downloaded archive to: {}", destPath);
            return true;
        } catch (IOException e) {
            log.error("I/O error downloading archive from {} to {}: {}", url, destPath, e.getMessage());
            return false;
        }
    }

    private byte[] downloadArchive(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        return apiClient.get(url, byte[].class, headers);
    }

    private void prepareDestinationDirectory(Path destPath) throws IOException {
        Path parent = destPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
