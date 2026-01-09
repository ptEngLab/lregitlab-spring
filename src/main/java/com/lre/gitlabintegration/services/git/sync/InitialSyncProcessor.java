package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialSyncProcessor {

    private final LreSyncService lreSyncService;
    private final SyncStateRepository stateRepository;

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    public SyncResponse process(SyncContext ctx) {
        List<GitLabCommit> current = ctx.current();
        SyncRequest req = ctx.request();

        log.info("Performing INITIAL sync with {} scripts for project {}",
                current.size(), req.getLreProject());

        List<SyncResponse.ScriptChange> uploadChanges = lreSyncService.uploadScripts(req, current);

        List<SyncResponse.ScriptChange> successful = uploadChanges.stream()
                .filter(c -> STATUS_SUCCESS.equals(c.status()))
                .toList();

        List<SyncResponse.ScriptChange> failed = uploadChanges.stream()
                .filter(c -> STATUS_FAILED.equals(c.status()))
                .toList();

        if (!successful.isEmpty()) {
            List<GitLabCommit> successfulCommits = getSuccessfulCommits(current, successful);
            stateRepository.saveCommits(req, successfulCommits);
            log.info("Saved {} successfully uploaded scripts to state", successfulCommits.size());
        }

        lreSyncService.logSyncSummary(uploadChanges);
        return SyncResponseBuilder.initialSync(successful, failed);
    }

    /**
     * Extracts successful commits from the full list based on successful changes.
     */
    private List<GitLabCommit> getSuccessfulCommits(
            List<GitLabCommit> allCommits,
            List<SyncResponse.ScriptChange> successful
    ) {
        Set<String> successfulPaths = successful.stream()
                .map(SyncResponse.ScriptChange::path)
                .collect(Collectors.toSet());

        return allCommits.stream()
                .filter(commit -> successfulPaths.contains(commit.getPath()))
                .toList();
    }
}
