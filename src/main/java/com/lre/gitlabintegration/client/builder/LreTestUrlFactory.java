package com.lre.gitlabintegration.client.builder;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LreTestUrlFactory {

    public String getTestsUrl(String domainName, String projectName) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/tests")
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }

    public String getTestByIdUrl(String domainName, String projectName, int testId) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/tests/{id}")
                .buildAndExpand(domainName, projectName, testId)
                .toUriString();
    }

    public String getTestDeleteUrl(String domainName, String projectName, int testId) {
        return getTestByIdUrl(domainName, projectName, testId);
    }

    public String getTestUpdateUrl(String domainName, String projectName, int testId) {
        return getTestByIdUrl(domainName, projectName, testId);
    }

    public String getTestValidityUrl(String domainName, String projectName, int testId) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/tests/{id}/validity")
                .buildAndExpand(domainName, projectName, testId)
                .toUriString();
    }

    /**
     * Builds:
     * /.../tests?query={ID[176]} OR /.../tests?query={TsName[e2e_test]}
     *
     * Note: UriComponentsBuilder will usually URL-encode braces; LRE typically accepts it.
     * If your server requires raw braces, we can switch to build(false) / encoding tweaks.
     */
    public String getTestsByQueryUrl(String domainName, String projectName, String rawQueryExpression) {
        return UriComponentsBuilder
                .fromPath("LoadTest/rest/domains/{domainName}/projects/{projectName}/tests")
                .queryParam("query", rawQueryExpression)
                .buildAndExpand(domainName, projectName)
                .toUriString();
    }
}
