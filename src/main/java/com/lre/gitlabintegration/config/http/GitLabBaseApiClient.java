package com.lre.gitlabintegration.config.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitLabBaseApiClient extends BaseRestApiClient {

    public GitLabBaseApiClient(@Qualifier("gitlabRestClient") RestClient gitlabRestClient) {
        super(gitlabRestClient);
    }
}
