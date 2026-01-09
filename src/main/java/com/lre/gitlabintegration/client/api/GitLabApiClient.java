package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.GitLabUrlFactory;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.gitlab.GitLabTreeItem;
import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GitLabApiClient {

    private final RestClient gitlabRestClient;
    private final GitLabUrlFactory gitLabUrlFactory;

    public GitLabApiClient(
            @Qualifier("gitlabRestClient") RestClient gitlabRestClient,
            GitLabUrlFactory gitLabUrlFactory
    ) {
        this.gitlabRestClient = gitlabRestClient;
        this.gitLabUrlFactory = gitLabUrlFactory;
    }

    public List<GitLabTreeItem> getRepositoryTree(int page, int projectId, String ref) {
        String url = gitLabUrlFactory.getRepositoryTreeUrl(page, projectId, ref);
        log.debug("Fetching repository tree from: {}", url);

        try {
            List<GitLabTreeItem> items = gitlabRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return items != null ? items : Collections.emptyList();
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(
                    e,
                    url,
                    "Fetching repository tree for project: " + projectId
            );
            return Collections.emptyList();
        }
    }

    public GitLabCommit getLatestCommitForPath(int projectId, String ref, String path) {
        String url = gitLabUrlFactory.getLatestCommitUrlForPath(projectId, ref, path);
        log.debug("Fetching latest commit for path: {}", path);

        try {
            List<GitLabCommit> commits = gitlabRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return (commits != null && !commits.isEmpty())
                    ? commits.get(0)
                    : new GitLabCommit();
        } catch (RestClientException e) {
            log.debug("No commit found for path: {}", path);
            return new GitLabCommit();
        }
    }

    public boolean downloadRepositoryArchive(
            int projectId,
            String commitSha,
            String path,
            Path destPath
    ) {
        String url = gitLabUrlFactory.getRepositoryArchiveUrl(projectId, commitSha, path);

        try {
            prepareDestinationDirectory(destPath);
            downloadAndSaveArchive(url, destPath);
            log.debug("Successfully downloaded archive to: {}", destPath);
            return true;
        } catch (IOException e) {
            log.error(
                    "I/O error downloading archive from {} to {}: {}",
                    url,
                    destPath,
                    e.getMessage()
            );
            return false;
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(
                    e,
                    url,
                    "downloading repository: " + projectId
            );
            return false;
        } catch (Exception e) {
            log.error(
                    "Unexpected error downloading archive from {} to {}",
                    url,
                    destPath,
                    e
            );
            return false;
        }
    }

    private void prepareDestinationDirectory(Path destPath) throws IOException {
        Path parent = destPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void downloadAndSaveArchive(String url, Path destPath) {
        gitlabRestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((request, response) -> {
                    validateResponse(response);
                    validateNotHtml(response);
                    saveResponseToFile(response, destPath);
                    return null;
                });
    }

    private void validateResponse(ClientHttpResponse response) throws IOException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException(
                    "GitLab returned error status: " + response.getStatusCode()
            );
        }
    }

    private void validateNotHtml(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null && contentType.includes(MediaType.TEXT_HTML)) {
            throw new IOException(
                    "GitLab returned HTML instead of expected content. Status: "
                            + response.getStatusCode()
            );
        }
    }

    private void saveResponseToFile(ClientHttpResponse response, Path destPath) throws IOException {
        try (InputStream in = response.getBody()) {
            Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);

        }
    }
}
