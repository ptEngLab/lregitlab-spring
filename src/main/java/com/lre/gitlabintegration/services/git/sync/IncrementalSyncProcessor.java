package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.lre.gitlabintegration.services.git.sync.SyncResponseBuilder.incremental;
import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_FAILED;
import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_SUCCESS;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalSyncProcessor {

    private final SyncAnalyzer analyzer;
    private final LreSyncService lreSyncService;
    private final SyncStateUpdater stateUpdater;

    public SyncResponse process(SyncContext ctx) {
        List<SyncStateEntry> previous = ctx.previous();
        List<GitLabCommit> current = ctx.current();
        SyncRequest req = ctx.request();

        log.info("Performing INCREMENTAL sync for project {}", req.getLreProject());

        SyncResult result = analyzer.analyze(previous, current);
        if (!result.hasChanges()) {
            List<ScriptChange> unchangedChanges = SyncResponseBuilder.buildUnchangedList(result.unchangedScripts());
            log.info("No changes detected");
            return SyncResponseBuilder.noChanges(unchangedChanges);
        }

        logSyncSummary(result);

        List<ScriptChange> uploadChanges = lreSyncService.uploadScripts(req, result.scriptsToUpload());
        List<ScriptChange> deleteChanges = lreSyncService.deleteScripts(req, result.scriptsToDelete());

        List<ScriptChange> unchangedChanges = SyncResponseBuilder.buildUnchangedList(result.unchangedScripts());

        ChangeGroups groups = categorize(uploadChanges, deleteChanges);

        stateUpdater.updateStateWithPartialSuccess(ctx, result, groups.uploadedSuccess(), groups.deletedSuccess());

        List<ScriptChange> allChanges = new ArrayList<>(uploadChanges.size() + deleteChanges.size());
        allChanges.addAll(uploadChanges);
        allChanges.addAll(deleteChanges);
        lreSyncService.logSyncSummary(allChanges);

        return incremental(groups.uploadedSuccess(), groups.deletedSuccess(), unchangedChanges, groups.failed());
    }

    private void logSyncSummary(SyncResult result) {
        log.info("SYNC SUMMARY: total={} | upload={} | delete={} | unchanged={}",
                result.totalScripts(),
                result.scriptsToUpload().size(),
                result.scriptsToDelete().size(),
                result.unchangedScripts().size());
    }

    private ChangeGroups categorize(List<ScriptChange> uploads, List<ScriptChange> deletes) {

        List<ScriptChange> uploadedSuccess = new ArrayList<>();
        List<ScriptChange> deletedSuccess = new ArrayList<>();
        List<ScriptChange> failed = new ArrayList<>();

        // Process uploads
        for (ScriptChange c : uploads) {
            if (STATUS_SUCCESS.equals(c.status())) {
                uploadedSuccess.add(c);
            } else if (STATUS_FAILED.equals(c.status())) {
                failed.add(c);
            }
        }

        // Process deletes
        for (ScriptChange c : deletes) {
            if (STATUS_SUCCESS.equals(c.status())) {
                deletedSuccess.add(c);
            } else if (STATUS_FAILED.equals(c.status())) {
                failed.add(c);
            }
        }

        return new ChangeGroups(uploadedSuccess, deletedSuccess, failed);
    }

}
