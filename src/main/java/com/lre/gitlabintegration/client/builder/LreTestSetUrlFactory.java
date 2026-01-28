package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreTestSetUrlFactory {

    /**
     * Create or get all test set folders:
     * POST/GET /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsetfolders
     */
    public String getTestSetFoldersUrl(String domainName, String projectName) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/testsetfolders")
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }

    /**
     * Create a test set:
     * POST /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets
     */
    public String getCreateTestSetUrl(String domainName, String projectName) {
        return getTestSetsUrl(domainName, projectName);
    }

    /**
     * Get all test sets:
     * GET /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets
     */
    public String getTestSetsUrl(String domainName, String projectName) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets")
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }

    /**
     * Test set by id:
     * /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets/{id}
     */
    public String getTestSetByIdUrl(String domainName, String projectName, int testSetId) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets/{id}")
                .buildAndExpand(domainName, projectName, testSetId)
                .toUriString();
    }

    /**
     * Delete a test set:
     * DELETE /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets/{id}
     */
    public String getDeleteTestSetUrl(String domainName, String projectName, int testSetId) {
        return getTestSetByIdUrl(domainName, projectName, testSetId);
    }

    /**
     * Update test set name:
     * PUT /LoadTest/rest/domains/{domainName}/projects/{projectName}/testsets/{id}
     */
    public String getUpdateTestSetUrl(String domainName, String projectName, int testSetId) {
        return getTestSetByIdUrl(domainName, projectName, testSetId);
    }
}
