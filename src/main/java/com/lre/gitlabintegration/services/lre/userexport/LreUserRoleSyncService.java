package com.lre.gitlabintegration.services.lre.userexport;

import com.lre.gitlabintegration.client.api.LreAdminApiClient;
import com.lre.gitlabintegration.config.properties.LreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class LreUserRoleSyncService {

    private final LreAdminApiClient lreClient;
    private final LreProperties props;
    private final LreUserRolePersistenceService persistence;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            initialDelayString = "${lre.user-role-sync.initial-delay-ms:5000}",
            fixedDelayString = "${lre.user-role-sync.fixed-delay-ms:600000}"
    )
    public void syncUsersAndRoles() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping LRE users→DB sync (previous run still running)");
            return;
        }

        log.info("Starting LRE users→DB sync...");

        boolean loggedIn = false;

        try {
            loggedIn = lreClient.login(props.getUsername(), props.getPassword(), props.isAuthenticateWithToken());
            var users = lreClient.getUsersWithRoles();
            persistence.persistUsersAndRoles(users);

            log.info("Completed LRE users→DB sync. users={}", users.size());
        } catch (Exception e) {
            log.error("LRE users→DB sync failed", e);
        } finally {
            if (loggedIn) logout();
            running.set(false);
        }
    }

    private void logout() {
        try {
            lreClient.logout();
        } catch (Exception e) {
            log.warn("LRE logout failed", e);
        }
    }

}
