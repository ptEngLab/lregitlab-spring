package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreAuthUrlFactory {

    /**
     * Get Auth Url for user authentication
     */
    public String getAuthUrlForUser() {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/authentication-point/authenticate")
                .toUriString();
    }

    /**
     * Get Auth Url for client based authentication
     */
    public String getAuthUrlForClient() {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/authentication-point/AuthenticateClient")
                .toUriString();
    }

    /**
     * Get Logout Url
     */
    public String getLogoutUrl() {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/authentication-point/logout")
                .toUriString();
    }

    /**
     * Get Auth Url for Web
     */
    public String getAuthUrlWeb(String domain, String project) {
        return UriComponentsBuilder
                .fromPath("loadtest/rest-pcweb/login/LoginToProject")
                .queryParam("domain", domain)
                .queryParam("project", project)
                .toUriString();
    }
}
