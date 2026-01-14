package com.lre.gitlabintegration.client.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lre.gitlabintegration.client.builder.GitLabUrlFactory;
import com.lre.gitlabintegration.config.http.BaseRestApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GitLabJobTokenApiClient extends BaseRestApiClient {

    private final GitLabUrlFactory gitLabUrlFactory;

    public GitLabJobTokenApiClient(RestClient gitlabJobTokenRestClient, GitLabUrlFactory gitLabUrlFactory) {
        super(gitlabJobTokenRestClient);
        this.gitLabUrlFactory = gitLabUrlFactory;
    }

    public GitLabJobInfo getCurrentJob(HttpHeaders headers) {
        String url = gitLabUrlFactory.getJobUrl();
        return get(url, GitLabJobInfo.class, headers);
    }

    public record GitLabJobInfo(GitLabUser user, GitLabPipeline pipeline, String ref, Boolean tag) {
    }

    public record GitLabUser(long id, String username, String name) {
    }

    public record GitLabPipeline(long id,
                                 @JsonProperty("project_id") long projectId,
                                 @JsonProperty("web_url") String webUrl) {
    }
}
