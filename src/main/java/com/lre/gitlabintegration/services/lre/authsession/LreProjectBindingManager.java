package com.lre.gitlabintegration.services.lre.authsession;

import com.lre.gitlabintegration.client.api.LreAuthApiClient;
import com.lre.gitlabintegration.config.properties.LreProperties;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
@Component
public class LreProjectBindingManager {

    private final LreAuthApiClient lreAuthApiClient;
    private final LreProperties props;
    private final Clock clock;

    private final Map<String, Instant> projectBindAt = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    private static final String ERR_PROJECT_BIND_FAILED = "Failed to bind to LRE project: %s/%s";
    private static final String ERR_INVALID_PARAMS = "Domain and project must not be null or empty";

    public void ensureProjectBound(String domain, String project) {
        validateParameters(domain, project);

        domain = domain.trim();
        project = project.trim();

        String key = buildProjectKey(domain, project);
        Instant now = Instant.now(clock);

        if (isProjectBindingFresh(key, now)) return;

        performProjectBinding(domain, project, key);
    }

    public void clearAllBindings() {
        projectBindAt.clear();
        projectLocks.clear();
    }

    private void performProjectBinding(String domain, String project, String key) {
        ReentrantLock projectLock =
                projectLocks.computeIfAbsent(key, k -> new ReentrantLock());

        projectLock.lock();
        try {
            Instant now = Instant.now(clock);
            if (isProjectBindingFresh(key, now)) return;

            bindToProject(domain, project, key);

        } finally {
            projectLock.unlock();
        }
    }

    private boolean isProjectBindingFresh(String key, Instant now) {
        Instant lastBound = projectBindAt.get(key);
        if (lastBound == null) return false;

        Duration bindingAge = Duration.between(lastBound, now);
        return bindingAge.compareTo(props.getSessionTtl()) < 0;
    }

    private void bindToProject(String domain, String project, String key) {
        try {
            lreAuthApiClient.loginToWebProject(domain, project);

            Instant boundAt = Instant.now(clock);
            projectBindAt.put(key, boundAt);

            log.debug("Bound to LRE project: {}/{} at {}", domain, project, boundAt);
        } catch (Exception e) {
            String message = String.format(ERR_PROJECT_BIND_FAILED, domain, project);
            log.error("Failed to bind to project {}/{}: {}", domain, project, e.getMessage(), e);
            throw new LreException(message, e);
        }
    }

    private String buildProjectKey(String domain, String project) {
        return (domain + "/" + project).toLowerCase(Locale.ROOT);
    }

    private void validateParameters(String domain, String project) {
        if (!StringUtils.hasText(domain) || !StringUtils.hasText(project)) {
            throw new IllegalArgumentException(ERR_INVALID_PARAMS);
        }
    }
}
