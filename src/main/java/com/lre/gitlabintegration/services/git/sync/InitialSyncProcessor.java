package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.ScriptChange;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_FAILED;
import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_SUCCESS;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialSyncProcessor {

    private final LreSyncService lreSyncService;
    private final SyncStateRepository stateRepository;

    public SyncResponse process(SyncContext ctx) {
        List<GitLabCommit> current = ctx.current();
        SyncRequest req = ctx.request();

        log.info("Starting INITIAL sync for project {} ({} scripts)",
                req.getLreProject(), current.size());

        List<ScriptChange> uploadChanges = lreSyncService.uploadScripts(req, current);

        Map<String, List<ScriptChange>> grouped = uploadChanges.stream()
                .collect(Collectors.groupingBy(ScriptChange::status));

        List<ScriptChange> successful = grouped.getOrDefault(STATUS_SUCCESS, List.of());
        List<ScriptChange> failed = grouped.getOrDefault(STATUS_FAILED, List.of());

        saveSuccessfulState(req, current, successful);

        lreSyncService.logSyncSummary(uploadChanges);
        return SyncResponseBuilder.initialSync(successful, failed);
    }

    private void saveSuccessfulState(
            SyncRequest req,
            List<GitLabCommit> current,
            List<ScriptChange> successful
    ) {
        if (successful.isEmpty()) return;

        Map<String, GitLabCommit> commitsByPath = current.stream()
                .collect(Collectors.toMap(
                        GitLabCommit::getPath,
                        Function.identity(),
                        (a, b) -> a
                ));

        List<GitLabCommit> successfulCommits = new ArrayList<>(successful.size());
        Map<String, Integer> idsByPath = new HashMap<>();

        for (ScriptChange change : successful) {
            GitLabCommit commit = commitsByPath.get(change.path());
            if (commit == null) continue;

            successfulCommits.add(commit);

            if (change.lreScriptId() != null) {
                idsByPath.put(change.path(), change.lreScriptId());
            }
        }

        if (successfulCommits.isEmpty()) return;

        stateRepository.saveCommits(req, successfulCommits, idsByPath);
        log.info("Saved {} successfully uploaded scripts to state", successfulCommits.size());
    }
}
