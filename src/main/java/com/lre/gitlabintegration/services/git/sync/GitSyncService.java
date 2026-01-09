package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for synchronizing scripts between GitLab and LRE.
 * Handles both initial sync (all scripts) and incremental sync (only changes).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitSyncService {

    private final SyncExecutionManager executionManager;
    private final SyncModeResolver modeResolver;
    private final InitialSyncProcessor initialSyncProcessor;
    private final IncrementalSyncProcessor incrementalSyncProcessor;

    public SyncResponse sync(SyncRequest request) {
        return executionManager.execute(request, () -> {

            SyncContext ctx = modeResolver.resolve(request);
            log.info(
                    "Starting Git-LRE sync for project: {}, initial={}",
                    request.getLreProject(),
                    ctx.isInitial()
            );

            if (ctx.isInitial()) {
                return initialSyncProcessor.process(ctx);
            } else {
                return incrementalSyncProcessor.process(ctx);
            }
        });
    }
}
