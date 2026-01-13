package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.GitLabApiClient;
import com.lre.gitlabintegration.config.properties.GitLabProperties;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.gitlab.GitLabTreeItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitRepositoryScanner {

    private final GitLabApiClient gitLabApiClient;
    private final int threadPoolSize;
    private final int gitlabPerPageRecords;

    public GitRepositoryScanner(GitLabApiClient gitLabApiClient,
                                GitLabProperties gitLabProperties) {
        this.gitLabApiClient = gitLabApiClient;
        this.threadPoolSize = gitLabProperties.getThreadPoolSize();
        this.gitlabPerPageRecords = gitLabProperties.getPerPageRecords();
    }

    public List<GitLabCommit> scanScripts(long projectId, String ref) {
        Set<String> scriptDirs = findScriptDirectories(projectId, ref);
        log.debug("Found {} scripts", scriptDirs.size());

        if (scriptDirs.isEmpty()) {
            log.info("No LRE script(s) found in the repository");
            return List.of();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        try {
            List<Future<GitLabCommit>> futures = scriptDirs.stream()
                    .map(path -> executor.submit(
                            () -> fetchCommitForPath(projectId, ref, path)))
                    .toList();

            List<GitLabCommit> commits = new ArrayList<>();
            for (Future<GitLabCommit> future : futures) {
                try {
                    GitLabCommit commit = future.get();
                    if (!commit.isEmpty()) {
                        commits.add(commit);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch commit for script path", e);
                }
            }

            return commits;
        } finally {
            shutdownExecutor(executor);
        }
    }

    private GitLabCommit fetchCommitForPath(long projectId, String ref, String path) {
        GitLabCommit commit = gitLabApiClient.getLatestCommitForPath(projectId, ref, path);
        if (!commit.isEmpty()) {
            commit.setPath(path);
        }
        return commit;
    }

    private Set<String> findScriptDirectories(long projectId, String ref) {
        List<GitLabTreeItem> allItems = scanEntireRepository(projectId, ref);
        return allItems.stream()
                .filter(i ->
                        "blob".equals(i.getType())
                                && i.getName().endsWith(".usr"))
                .map(i ->
                        i.getPath().substring(
                                0, i.getPath().lastIndexOf('/')))
                .collect(Collectors.toSet());
    }

    private List<GitLabTreeItem> scanEntireRepository(long projectId, String ref) {
        List<GitLabTreeItem> all = new ArrayList<>();
        int page = 1;

        while (true) {
            List<GitLabTreeItem> items =
                    gitLabApiClient.getRepositoryTree(page, projectId, ref);
            all.addAll(items);
            if (items.isEmpty() || items.size() < gitlabPerPageRecords) {
                break;
            }
            page++;
        }

        return all;
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
