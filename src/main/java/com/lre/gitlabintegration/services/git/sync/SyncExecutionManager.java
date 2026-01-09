package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.exceptions.LreException;
import com.lre.gitlabintegration.repository.SyncLockRepository;
import com.lre.gitlabintegration.services.lre.authsession.LreSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncExecutionManager {

    private final SyncLockRepository lockRepository;
    private final LreSessionManager lreSessionManager;

    public SyncResponse execute(SyncRequest request, Supplier<SyncResponse> action) {
        if (!lockRepository.acquireLock(request)) {
            log.warn("Could not acquire lock for project: {}", request.getLreProject());
            return SyncResponseBuilder.failureResponse("LOCK",
                    new LreException("Cloud not acquire lock"),
                    request);
        }

        try {
            lreSessionManager.ensureAuthenticated(request.getLreDomain(), request.getLreProject());
            return action.get();
        } catch (Exception e) {
            log.error("Sync failed for project: {}", request.getLreProject(), e);
            return SyncResponseBuilder.failureResponse("SYNC EXCEPTION", e, request);
        } finally {
            lockRepository.releaseLock(request);
        }
    }
}
