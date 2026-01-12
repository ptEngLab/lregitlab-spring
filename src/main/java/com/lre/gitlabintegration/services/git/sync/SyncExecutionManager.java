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

import static com.lre.gitlabintegration.services.git.sync.SyncResponseBuilder.failureResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncExecutionManager {

    private final SyncLockRepository lockRepository;

    private final LreSessionManager lreSessionManager;

    public SyncResponse execute(SyncRequest request, Supplier<SyncResponse> action) {

        boolean locked = false;

        try {

            locked = lockRepository.acquireLock(request);

            if (!locked) {

                log.warn("Could not acquire lock for project: {}", request.getLreProject());

                return failureResponse("LOCK_NOT_ACQUIRED", new LreException("Could not acquire lock"), request);
            }

            lreSessionManager.ensureAuthenticated(request.getLreDomain(), request.getLreProject());

            return action.get();

        } catch (Exception e) {

            log.error("Sync failed for project: {}", request.getLreProject(), e);

            return failureResponse("SYNC_EXCEPTION", e, request);

        } finally {

            if (locked) lockRepository.releaseLock(request);

        }
    }
}
