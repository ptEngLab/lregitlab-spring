package com.lre.gitlabintegration.config.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LreApiClientBaseRestApiClient extends BaseRestApiClient {

    public LreApiClientBaseRestApiClient(@Qualifier("lreRestClient") RestClient lreRestClient) {
        super(lreRestClient);
    }
}
