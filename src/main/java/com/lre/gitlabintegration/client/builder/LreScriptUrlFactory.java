package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreScriptUrlFactory {

    /**
     * Get all Scripts URL
     */
    public String getScriptsUrl(String domainName, String projectName) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/Scripts")
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }

    /**
     * This is same as scripts URL for uploads
     */
    public String getScriptUploadUrl(String domainName, String projectName) {
        return getScriptsUrl(domainName, projectName);
    }

    /**
     * Get script by ID URL
     */
    public String getScriptByIdUrl(String domainName, String projectName, int id) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/Scripts/{id}")
                .buildAndExpand(domainName, projectName, id)
                .toUriString();
    }

    /**
     * Same as script by ID for deletes
     */
    public String getScriptDeleteUrl(String domainName, String projectName, int id) {
        return getScriptByIdUrl(domainName, projectName, id);
    }
}
