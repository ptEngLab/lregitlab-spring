package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreTestUrlFactory;
import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.lretest.TestCreateRequest;
import com.lre.gitlabintegration.dto.lretest.TestSummary;
import com.lre.gitlabintegration.dto.lretest.TestValidity;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LreTestApiClient {

    private final LreApiClientBaseRestApiClient apiClient;
    private final LreTestUrlFactory testUrlFactory;

    public LreTestApiClient(LreApiClientBaseRestApiClient apiClient, LreTestUrlFactory testUrlFactory) {
        this.apiClient = apiClient;
        this.testUrlFactory = testUrlFactory;
    }

    /** GET /tests */
    public List<TestSummary> getAllTests(String domain, String project) {
        String url = testUrlFactory.getTestsUrl(domain, project);
        log.debug("Fetching all tests for {}/{}", domain, project);

        // LRE often returns XML by default for this endpoint (docs show XML). :contentReference[oaicite:6]{index=6}
        // If your base client is JSON-centric, either:
        //  1) Ensure it can deserialize XML, OR
        //  2) Add a method that returns String and parse yourself.
        return apiClient.get(url, new ParameterizedTypeReference<>() {});
    }

    /** GET /tests?query={ID[176]} or {TsName[name]} */
    public List<TestSummary> getTestsByIdQuery(String domain, String project, int testId) {
        String raw = "{ID[" + testId + "]}";
        String url = testUrlFactory.getTestsByQueryUrl(domain, project, raw);
        log.debug("Fetching tests by query {}", raw);

        return apiClient.get(url, new ParameterizedTypeReference<>() {});
    }

    public List<TestSummary> getTestsByNameQuery(String domain, String project, String testName) {
        String raw = "{TsName[" + testName + "]}";
        String url = testUrlFactory.getTestsByQueryUrl(domain, project, raw);
        log.debug("Fetching tests by query {}", raw);

        return apiClient.get(url, new ParameterizedTypeReference<>() {});
    }

    /** GET /tests/{id} */
    public Object getTestById(String domain, String project, int testId) {
        String url = testUrlFactory.getTestByIdUrl(domain, project, testId);
        log.debug("Fetching test by ID: {}", testId);

        // Response can be XML or JSON. :contentReference[oaicite:7]{index=7}
        // If you have a fully-modeled DTO for the “full test”, swap Object -> TestDetails.
        return apiClient.get(url, Object.class);
    }

    /** POST /tests (create) */
    public Object createTest(String domain, String project, TestCreateRequest request) {
        String url = testUrlFactory.getTestsUrl(domain, project);
        log.debug("Creating test in {}/{}: {}", domain, project, request.getName());

        // Create supports XML or JSON. :contentReference[oaicite:8]{index=8}
        Object created = apiClient.post(url, request, Object.class, MediaType.APPLICATION_JSON);

        if (created == null) {
            throw new LreException("LRE returned null response for test creation: " + request.getName());
        }
        return created;
    }

    /** PUT /tests/{id} (update) - documented as XML */
    public void updateTestXml(String domain, String project, int testId, String testContentXml) {
        String url = testUrlFactory.getTestUpdateUrl(domain, project, testId);
        log.debug("Updating test {} in {}/{}", testId, domain, project);

        // Docs: Content-Type application/xml; PUT returns no body. :contentReference[oaicite:9]{index=9}
        apiClient.put(url, testContentXml, Void.class, MediaType.APPLICATION_XML);
    }

    /** DELETE /tests/{id} */
    public void deleteTest(String domain, String project, int testId) {
        String url = testUrlFactory.getTestDeleteUrl(domain, project, testId);
        log.debug("Deleting test {} in {}/{}", testId, domain, project);

        apiClient.delete(url);
    }

    /** GET /tests/{id}/validity */
    public TestValidity getTestValidity(String domain, String project, int testId) {
        String url = testUrlFactory.getTestValidityUrl(domain, project, testId);
        log.debug("Fetching test validity for testId={}", testId);

        return apiClient.get(url, TestValidity.class);
    }
}
