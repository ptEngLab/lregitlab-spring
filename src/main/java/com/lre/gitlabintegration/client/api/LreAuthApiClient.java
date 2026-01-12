package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreAuthUrlFactory;
import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.lreauth.AuthenticationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
public class LreAuthApiClient {

    private final LreApiClientBaseRestApiClient apiClient;
    private final LreAuthUrlFactory authUrlFactory;

    public LreAuthApiClient(
            LreApiClientBaseRestApiClient apiClient,
            LreAuthUrlFactory authUrlFactory
    ) {
        this.apiClient = apiClient;
        this.authUrlFactory = authUrlFactory;
    }

    public boolean login(String username, String password, boolean authenticateWithToken) {
        return authenticateWithToken
                ? loginForClient(username, password)
                : loginForUser(username, password);
    }

    public void logout() {
        String logoutUrl = authUrlFactory.getLogoutUrl();
        apiClient.getBodiless(logoutUrl); // default Accept: JSON from BaseRestApiClient
        log.debug("Logout successful");
    }

    public void loginToWebProject(String domain, String project) {
        String webLoginUrl = authUrlFactory.getAuthUrlWeb(domain, project);
        apiClient.getBodiless(webLoginUrl);
        log.debug("Web login successful for project: {}/{}", domain, project);
    }

    private boolean loginForClient(String username, String password) {
        String authUrl = authUrlFactory.getAuthUrlForClient();

        try {
            AuthenticationRequest authRequest = new AuthenticationRequest(username, password);
            apiClient.postJsonBodiless(authUrl, authRequest);
            log.debug("LRE authentication successful with token");
            return true;

        } catch (RestClientResponseException e) {
            if (isAuthFailure(e.getStatusCode())) {
                log.warn("LRE token authentication failed: {}", e.getStatusCode());
                return false;
            }
            throw e; // already mapped by BaseRestApiClient into domain exception for other RestClientExceptions
        }
    }

    private boolean loginForUser(String username, String password) {
        String authUrl = authUrlFactory.getAuthUrlForUser();

        try {
            apiClient.getWithBasicAuthBodiless(authUrl, username, password, MediaType.APPLICATION_JSON);
            log.debug("LRE Authentication successful with username/password");
            return true;

        } catch (RestClientResponseException e) {
            if (isAuthFailure(e.getStatusCode())) {
                log.warn("LRE basic authentication failed: {}", e.getStatusCode());
                return false;
            }
            throw e;
        }
    }

    private boolean isAuthFailure(HttpStatusCode statusCode) {
        return statusCode.value() == 401 || statusCode.value() == 403;
    }
}
