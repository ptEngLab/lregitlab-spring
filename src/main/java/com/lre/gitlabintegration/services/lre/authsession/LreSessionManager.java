package com.lre.gitlabintegration.services.lre.authsession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LreSessionManager {

    private final LreLoginManager loginManager;
    private final LreProjectBindingManager projectBindingManager;

    /**
     * Public entrypoint used by callers.
     * Ensures user is authenticated and bound to a given project.
     */
    public void ensureAuthenticated(String domain, String project) {
        boolean didLogin = loginManager.ensureLoggedIn();
        if (didLogin) projectBindingManager.clearAllBindings();

        projectBindingManager.ensureProjectBound(domain, project);
    }

    public void clearProjectBindings() {
        projectBindingManager.clearAllBindings();
    }
}
