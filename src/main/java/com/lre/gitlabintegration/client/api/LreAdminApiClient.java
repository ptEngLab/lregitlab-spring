package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreAdminUrlFactory;
import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.lreauth.AuthenticationRequest;
import com.lre.gitlabintegration.dto.lreuser.LreUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class LreAdminApiClient {
    private final LreApiClientBaseRestApiClient apiClient;
    private final LreAdminUrlFactory lreAdminUrlFactory;


    public boolean login(String username, String password, boolean authenticateWithToken) {
        return authenticateWithToken
                ? loginForClient(username, password)
                : loginForUser(username, password);
    }

    public void logout() {
        String logoutUrl = lreAdminUrlFactory.getLogoutUrl();
        apiClient.getBodiless(logoutUrl);
        log.debug("Logout successful");
    }

    public List<LreUserDto> getUsersWithRoles() {
        String url = lreAdminUrlFactory.getUsersUrl();
        log.debug("Fetching users from LRE: {}", url);
        List<LreUserDto> users = apiClient.get(url, new ParameterizedTypeReference<>() {});
        return users != null ? users : Collections.emptyList();
    }


    private boolean loginForClient(String username, String password) {
        String authUrl = lreAdminUrlFactory.getAuthUrlForClient();

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
        String authUrl = lreAdminUrlFactory.getAuthUrlForUser();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
            apiClient.get(authUrl, Void.class, headers);
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
