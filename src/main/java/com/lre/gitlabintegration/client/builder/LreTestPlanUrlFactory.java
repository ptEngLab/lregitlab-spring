package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreTestPlanUrlFactory {

    /**
     * Get all Test Plans URL
     */
    public String getAllTestPlansUrl(String domainName, String projectName) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/testplan")
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }

    /**
     * This is same as all Test Plans URL
     */
    public String getCreateTestPlanUrl(String domainName, String projectName) {
        return getAllTestPlansUrl(domainName, projectName);
    }
}
