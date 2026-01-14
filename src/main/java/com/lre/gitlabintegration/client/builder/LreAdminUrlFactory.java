package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreAdminUrlFactory {

    /**
     * Get Auth Url for user authentication
     */
    public String getAuthUrlForUser() {
        return UriComponentsBuilder
                .fromPath("Admin/rest/authentication-point/authenticate")
                .toUriString();
    }

    /**
     * Get Auth Url for client based authentication
     */
    public String getAuthUrlForClient() {
        return UriComponentsBuilder
                .fromPath("Admin/rest/authentication-point/AuthenticateClient")
                .toUriString();
    }

    /**
     * Get Users Url
     */
    public String getUsersUrl() {
        return UriComponentsBuilder
                .fromPath("Admin/rest/v1/users")
                .queryParam("include", "user-roles")
                .toUriString();
    }


    /**
     * Get Logout Url
     */
    public String getLogoutUrl() {
        return UriComponentsBuilder
                .fromPath("Admin/rest/authentication-point/logout")
                .toUriString();
    }

}
