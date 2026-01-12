package com.lre.gitlabintegration.services.lre.authsession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LreStartupLogin {

    private final LreLoginManager loginManager;

    @EventListener(ApplicationReadyEvent.class)
    public void loginOnStartup(){

        try {
            boolean loggedIn = loginManager.ensureLoggedIn();
            log.info("LRE Startup login {}", loggedIn ? "successful" : "already logged in");
        } catch (Exception e) {
            log.error("LRE start up login failed (will retry on demand)", e);
        }
    }
}
