package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.config.http.BaseRestApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GitLabJobTokenApiClient extends BaseRestApiClient {

    public GitLabJobTokenApiClient(RestClient gitlabJobTokenRestClient) {
        super(gitlabJobTokenRestClient);
    }

    public GitLabJobInfo getCurrentJob(String jobToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("JOB-TOKEN", jobToken);
        return get("/api/v4/job", GitLabJobInfo.class, headers);
    }

    public record GitLabJobInfo(GitLabUser user, GitLabProject project, String ref, Boolean tag) { }

    public record GitLabUser(long id, String username) { }

    public record GitLabProject(long id) { }
}
