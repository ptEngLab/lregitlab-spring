package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.dto.sync.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        List<GitLabCommit> previous = ctx.previous();
        List<GitLabCommit> current = ctx.current();
        SyncRequest req = ctx.request();

        log.info("Performing INCREMENTAL sync for project {}", req.getLreProject());

        SyncResult result = analyzer.analyze(previous, current);
        if (!result.hasChanges()) {
            List<SyncResponse.ScriptChange> unchangedChanges = SyncResponseBuilder.buildUnchangedList(result.unchangedScripts());
            log.info("No changes detected");
            return SyncResponseBuilder.noChanges(unchangedChanges);
        }

        logSyncSummary(result);

        // Perform uploads and deletes
        List<SyncResponse.ScriptChange> uploadChanges = lreSyncService.uploadScripts(ctx.request(), result.scriptsToUpload());

        List<SyncResponse.ScriptChange> deleteChanges = lreSyncService.deleteScripts(ctx.request(), result.scriptsToDelete());

        // Create unchanged list
        List<SyncResponse.ScriptChange> unchangedChanges = SyncResponseBuilder.buildUnchangedList(result.unchangedScripts());

        // Categorize all changes
        List<SyncResponse.ScriptChange> uploadedSuccess = uploadChanges.stream()
                .filter(c -> STATUS_SUCCESS.equals(c.status()))
                .toList();

        List<SyncResponse.ScriptChange> deletedSuccess = deleteChanges.stream()
                .filter(c -> STATUS_SUCCESS.equals(c.status()))
                .toList();

        List<SyncResponse.ScriptChange> allFailed = new ArrayList<>();

        allFailed.addAll(uploadChanges.stream()
                .filter(c -> STATUS_FAILED.equals(c.status()))
                .toList());

        allFailed.addAll(deleteChanges.stream()
                .filter(c -> STATUS_FAILED.equals(c.status()))
                .toList());

        stateUpdater.updateStateWithPartialSuccess(ctx, result, uploadedSuccess, deletedSuccess);

        // Log summary
        List<SyncResponse.ScriptChange> allChanges = new ArrayList<>();
        allChanges.addAll(uploadChanges);
        allChanges.addAll(deleteChanges);
        lreSyncService.logSyncSummary(allChanges);

        return SyncResponseBuilder.incremental(uploadedSuccess, deletedSuccess, unchangedChanges, allFailed);
    }

    /**
     * Logs a summary of the sync analysis results.
     */
    private void logSyncSummary(SyncResult result) {
        log.info("SYNC SUMMARY: total={} | upload={} | delete={} | unchanged={}",
                result.totalScripts(),
                result.scriptsToUpload().size(),
                result.scriptsToDelete().size(),
                result.unchangedScripts().size());
    }
}
