package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncAnalyzer {

    /**
     * Compares previous commits with current commits and determines
     * which scripts need to be uploaded, deleted, or are unchanged.
     */
    public SyncResult analyze(
            List<GitLabCommit> previous,
            List<GitLabCommit> current
    ) {
        // Defensive null handling
        previous = previous != null ? previous : List.of();
        current = current != null ? current : List.of();

        if (previous.isEmpty() && current.isEmpty()) {
            return new SyncResult(List.of(), List.of(), List.of());
        }

        Map<String, GitLabCommit> prevMap = toMap(previous);
        Map<String, GitLabCommit> currMap = toMap(current);

        List<GitLabCommit> toUpload = new ArrayList<>();
        List<GitLabCommit> toDelete = new ArrayList<>();
        List<GitLabCommit> unchanged = new ArrayList<>();

        // Deletions - scripts that existed before but don't exist now
        previous.forEach(commit -> {
            if (!currMap.containsKey(commit.getPath())) {
                log.debug("Script deleted: {}", commit.getPath());
                toDelete.add(commit);
            }
        });

        // Additions or modifications - new scripts or scripts with different SHA
        current.forEach(commit -> {
            GitLabCommit prev = prevMap.get(commit.getPath());

            if (prev == null) {
                log.debug("Script new to upload: {}", commit.getPath());
                toUpload.add(commit);
            } else if (hasChanged(prev, commit)) {
                log.debug("Script changed to upload: {}", commit.getPath());
                toUpload.add(commit);
            } else {
                unchanged.add(commit);
            }
        });

        log.info(
                "Analysis complete: upload={}, delete={}, unchanged={}",
                toUpload.size(),
                toDelete.size(),
                unchanged.size()
        );

        return new SyncResult(toUpload, toDelete, unchanged);
    }

    /**
     * Checks if a commit has changed by comparing SHA values
     */
    private boolean hasChanged(GitLabCommit previous, GitLabCommit current) {
        if (previous == null || current == null) {
            return true;
        }

        // Check if either commit is empty
        if (previous.isEmpty() || current.isEmpty()) {
            return previous.isEmpty() != current.isEmpty();
        }

        // Compare SHA values
        return !Objects.equals(previous.getSha(), current.getSha());
    }

    /**
     * Converts a list of commits to a map keyed by path
     */
    private Map<String, GitLabCommit> toMap(List<GitLabCommit> commits) {
        return commits.stream()
                .filter(Objects::nonNull)
                .filter(commit -> commit.getPath() != null)
                .collect(Collectors.toMap(
                        GitLabCommit::getPath,
                        commit -> commit,
                        (existing, duplicate) -> {
                            log.warn(
                                    "Duplicate path found: {}, keeping first occurrence",
                                    existing.getPath()
                            );
                            return existing;
                        }
                ));
    }
}
