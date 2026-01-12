package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreTestPlanUrlFactory;
import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.testplan.TestPlan;
import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class LreTestPlanApiClient {

    private final LreApiClientBaseRestApiClient apiClient;
    private final LreTestPlanUrlFactory urlFactory;

    public LreTestPlanApiClient(
            LreApiClientBaseRestApiClient apiClient,
            LreTestPlanUrlFactory urlFactory
    ) {
        this.apiClient = apiClient;
        this.urlFactory = urlFactory;
    }

    public List<TestPlan> fetchAllTestPlans(String domain, String project) {
        String url = urlFactory.getAllTestPlansUrl(domain, project);
        log.debug("Fetching all test plans for {}/{}", domain, project);

        // If LRE returns a JSON array, this is simplest (keeps your current behavior)
        TestPlan[] testPlans = apiClient.get(url, TestPlan[].class);

        return testPlans != null
                ? new ArrayList<>(Arrays.asList(testPlans))
                : new ArrayList<>();
    }

    public TestPlan createTestPlan(String domain, String project, String parentPath, String name) {
        String url = urlFactory.getCreateTestPlanUrl(domain, project);

        TestPlanCreationRequest req = new TestPlanCreationRequest(parentPath, name);

        // postJson uses application/json for request + accept
        TestPlan created = apiClient.postJson(url, req, TestPlan.class);

        if (created == null) {
            throw new LreException("Create test plan returned empty body");
        }

        return created;
    }
}
