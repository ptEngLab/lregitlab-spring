package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreAuthUrlFactory;
import com.lre.gitlabintegration.dto.lreauth.AuthenticationRequest;
import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class LreAuthApiClient {

    private final RestClient lreRestClient;
    private final LreAuthUrlFactory authUrlFactory;

    public LreAuthApiClient(
            @Qualifier("lreRestClient") RestClient lreRestClient,
            LreAuthUrlFactory authUrlFactory
    ) {
        this.lreRestClient = lreRestClient;
        this.authUrlFactory = authUrlFactory;
    }

    public boolean login(String username, String password, boolean authenticateWithToken) {
        return authenticateWithToken
                ? loginForClient(username, password)
                : loginForUser(username, password);
    }

    public void logout() {
        String logoutUrl = authUrlFactory.getLogoutUrl();

        try {
            lreRestClient.get()
                    .uri(logoutUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Logout successful");
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(
                    e,
                    logoutUrl,
                    "LRE logout"
            );
        }
    }

    public void loginToWebProject(String domain, String project) {
        String webLoginUrl = authUrlFactory.getAuthUrlWeb(domain, project);

        try {
            lreRestClient.get()
                    .uri(webLoginUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Web login successful for project: {}/{}", domain, project);
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(
                    e,
                    webLoginUrl,
                    "LRE web login for project: " + domain + "/" + project
            );
            throw e;
        }
    }

    private boolean loginForClient(String username, String password) {
        String authUrl = authUrlFactory.getAuthUrlForClient();

        try {
            AuthenticationRequest authRequest =
                    new AuthenticationRequest(username, password);

            lreRestClient.post()
                    .uri(authUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(authRequest)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("LRE authentication successful with token");
            return true;
        } catch (RestClientResponseException e) {
            if (isAuthFailure(e.getStatusCode())) {
                log.warn(
                        "LRE token authentication failed: {}",
                        e.getStatusCode()
                );
                return false;
            }

            HttpErrorHandler.handleRestClientError(
                    e,
                    authUrl,
                    "LRE Token authentication"
            );
            throw e;
        } catch (RestClientException e) {
            log.error(
                    "Token authentication failed: {}",
                    e.getMessage()
            );
            HttpErrorHandler.handleRestClientError(
                    e,
                    authUrl,
                    "LRE Token authentication"
            );
            throw e;
        }
    }

    private boolean loginForUser(String username, String password) {
        String authUrl = authUrlFactory.getAuthUrlForUser();

        try {
            lreRestClient.get()
                    .uri(authUrl)
                    .headers(h ->
                            h.setBasicAuth(
                                    username,
                                    password,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .retrieve()
                    .toBodilessEntity();

            log.debug("LRE Authentication successful with username/password");
            return true;
        } catch (RestClientResponseException e) {
            if (isAuthFailure(e.getStatusCode())) {
                log.warn(
                        "LRE basic authentication failed: {}",
                        e.getStatusCode()
                );
                return false;
            }

            HttpErrorHandler.handleRestClientError(
                    e,
                    authUrl,
                    "LRE basic authentication"
            );
            throw e;
        } catch (RestClientException e) {
            log.error(
                    "Basic authentication failed: {}",
                    e.getMessage()
            );
            HttpErrorHandler.handleRestClientError(
                    e,
                    authUrl,
                    "LRE basic authentication"
            );
            throw e;
        }
    }

    private boolean isAuthFailure(HttpStatusCode statusCode) {
        return statusCode.value() == 401 || statusCode.value() == 403;
    }
}
