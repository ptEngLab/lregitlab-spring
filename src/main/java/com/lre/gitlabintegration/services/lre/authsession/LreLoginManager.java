package com.lre.gitlabintegration.services.lre.authsession;

import com.lre.gitlabintegration.client.api.LreAuthApiClient;
import com.lre.gitlabintegration.config.properties.LreProperties;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class LreLoginManager {

    private final LreAuthApiClient lreAuthApiClient;
    private final LreProperties props;
    private final Clock clock;

    private final ReentrantLock loginLock = new ReentrantLock();

    private volatile Instant lastLoginAt = Instant.EPOCH;
    private volatile Instant lastLoginFailureAt = Instant.EPOCH;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private static final String ERR_AUTH_BACKOFF = "LRE authentication temporarily disabled due to %d consecutive failures. Will retry after %s";
    private static final String ERR_AUTH_IN_BACKOFF = "LRE authentication in backoff period";
    private static final String ERR_AUTH_INVALID_CREDENTIALS = "LRE authentication failed - invalid credentials";
    private static final String ERR_AUTH_FAILED = "LRE authentication failed: %s";

    public boolean ensureLoggedIn() {

        Instant now = Instant.now(clock);
        int maxFailures = props.getMaxConsecutiveFailures();
        Duration backoff = props.getFailureBackoff();

        resetFailuresIfBackoffElapsed(now, maxFailures, backoff);

        if (isSessionFresh(now)) return false;

        if (shouldBackOff(now, maxFailures, backoff)) {
            Instant nextAllowed = lastLoginFailureAt.plus(backoff);
            throw new LreException(String.format(ERR_AUTH_BACKOFF, consecutiveFailures.get(), nextAllowed));
        }

        return performLogin();
    }

    private boolean performLogin() {
        loginLock.lock();
        try {
            Instant now = Instant.now(clock);
            int maxFailures = props.getMaxConsecutiveFailures();
            Duration backoff = props.getFailureBackoff();

            resetFailuresIfBackoffElapsed(now, maxFailures, backoff);

            if (shouldBackOff(now, maxFailures, backoff)) throw new LreException(ERR_AUTH_IN_BACKOFF);

            if (isSessionFresh(now)) return false;

            attemptLogin(now);
            return true;

        } finally {
            loginLock.unlock();
        }
    }

    private void attemptLogin(Instant now) {
        String username = props.getUsername();
        String password = props.getPassword();
        boolean useToken = props.isAuthenticateWithToken();

        final boolean success;
        try {
            success = lreAuthApiClient.login(username, password, useToken);
        } catch (LreException e) {
            handleLoginFailure(now, safeMsg(e));
            throw e;
        } catch (Exception e) {
            handleLoginFailure(now, safeMsg(e));
            throw new LreException(String.format(ERR_AUTH_FAILED, safeMsg(e)), e);
        }

        if (!success) {
            handleLoginFailure(now, "invalid credentials");
            throw new LreException(ERR_AUTH_INVALID_CREDENTIALS);
        }

        handleLoginSuccess(now);
    }

    private String safeMsg(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private void handleLoginSuccess(Instant at) {
        lastLoginAt = at;
        consecutiveFailures.set(0);
        lastLoginFailureAt = Instant.EPOCH;
        log.info("LRE session authenticated successfully at {}", lastLoginAt);
    }

    private void handleLoginFailure(Instant at, String reason) {
        int failures = consecutiveFailures.incrementAndGet();
        lastLoginFailureAt = at;

        int maxFailures = props.getMaxConsecutiveFailures();
        Duration backoff = props.getFailureBackoff();

        if (failures >= maxFailures) {
            Instant backoffEnd = at.plus(backoff);
            log.error("LRE authentication failed (attempt {}/{}). Entering backoff for {} (until {}). Reason={}",
                    failures, maxFailures, backoff, backoffEnd, reason);
        } else {
            log.warn("LRE authentication failed (attempt {}/{}). Reason={}",
                    failures, maxFailures, reason);
        }
    }

    private void resetFailuresIfBackoffElapsed(Instant now, int maxFailures, Duration backoff) {
        if (consecutiveFailures.get() < maxFailures) return;

        Instant backoffEnd = lastLoginFailureAt.plus(backoff);
        if (!now.isBefore(backoffEnd)) {
            consecutiveFailures.set(0);
            lastLoginFailureAt = Instant.EPOCH;
        }
    }

    private boolean shouldBackOff(Instant now, int maxFailures, Duration backoff) {
        if (consecutiveFailures.get() < maxFailures) return false;
        return now.isBefore(lastLoginFailureAt.plus(backoff));
    }


    private boolean isSessionFresh(Instant now) {
        Duration ttl = props.getSessionTtl();
        return Duration.between(lastLoginAt, now).compareTo(ttl) < 0;
    }
}
