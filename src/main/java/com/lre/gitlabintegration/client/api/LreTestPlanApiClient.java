package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreTestPlanUrlFactory;
import com.lre.gitlabintegration.dto.testplan.TestPlan;
import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import com.lre.gitlabintegration.exceptions.LreException;
import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class LreTestPlanApiClient {

    private final RestClient lreRestClient;
    private final LreTestPlanUrlFactory urlFactory;

    public LreTestPlanApiClient(@Qualifier("lreRestClient") RestClient lreRestClient, LreTestPlanUrlFactory urlFactory) {
        this.lreRestClient = lreRestClient;
        this.urlFactory = urlFactory;
    }

    public List<TestPlan> fetchAllTestPlans(String domain, String project) {
        String url = urlFactory.getAllTestPlansUrl(domain, project);
        log.debug("Fetching all test plans for {}/{}", domain, project);

        try {
            TestPlan[] testPlans = lreRestClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TestPlan[].class);

            return testPlans != null
                    ? new ArrayList<>(Arrays.asList(testPlans))
                    : new ArrayList<>();
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Fetch all test plans for project: " + domain + "/" + project);
            throw new IllegalStateException("This should never be reached, handleRestClientError always throws an exception");
        }
    }

    public TestPlan createTestPlan(String domain, String project, String parentPath, String name) {
        String url = urlFactory.getCreateTestPlanUrl(domain, project);

        try {
            TestPlanCreationRequest req = new TestPlanCreationRequest(parentPath, name);

            TestPlan created = lreRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(TestPlan.class);

            if (created == null) throw new LreException("Create test plan returned empty body");


            return created;
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Create test plan for project: " + domain + "/" + project);
            throw new IllegalStateException("This should never be reached, handleRestClientError always throws an exception");
        }
    }
}
