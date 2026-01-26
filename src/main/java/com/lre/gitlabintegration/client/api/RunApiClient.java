package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.lrescript.WebFormsTokenParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunApiClient {

    private final LreApiClientBaseRestApiClient apiClient;
    private final LreRunUrlFactory urlFactory;
    private final FileDownloadService fileDownloadService;

    /**
     * Starts a new test run in LRE.
     *
     * @param testId  the ID of the test to run
     * @param request the timeslot check request containing run configuration
     * @return RunResponse containing the run ID and status
     */
    public RunResponse startRun(int testId, TimeslotCheckRequest request) {
        log.debug("Starting test run for test ID: {}", testId);
        String url = urlFactory.getStartRunUrlWeb(testId);
        return apiClient.postJson(url, request, RunResponse.class);
    }

    /**
     * Aborts a running test in LRE.
     *
     * @param domain  the LRE domain name
     * @param project the LRE project name
     * @param runId   the ID of the run to abort
     */
    public void abort(String domain, String project, int runId) {
        log.debug("Aborting run ID: {}", runId);
        String url = urlFactory.getAbortRunUrl(domain, project, runId);
        apiClient.getBodiless(url);
    }

    /**
     * Fetches basic run status for a specific run.
     * Lightweight method for monitoring and quick status checks.
     *
     * @param domainName  the LRE domain name
     * @param projectName the LRE project name
     * @param runId       the ID of the run
     * @return RunStatus containing basic run information
     */
    public RunStatus fetchStatus(String domainName, String projectName, int runId) {
        log.debug("Fetching run status for run ID: {}", runId);
        String url = urlFactory.getRunStatusUrl(domainName, projectName, runId);
        return apiClient.get(url, RunStatus.class);
    }


    public void streamDownloadResultViaUi(String domain, String project, int resultId, OutputStream out) {

        // 1) GET the dialog page (HTML)
        String getUrl = urlFactory.getDownloadResultDialogUrl(resultId);
        HttpHeaders htmlHeaders = fileDownloadService.createHtmlHeaders();
        String html = apiClient.get(getUrl, String.class, htmlHeaders);

        // 2) Parse WebForms hidden fields
        Map<String, String> tokens = WebFormsTokenParser.parse(html);

        // 3) POST form (this triggers actual download)
        String postUrl = urlFactory.getDownloadResultDialogPostUrl();
        HttpHeaders formHeaders = fileDownloadService.createFormPostHeaders();

        Map<String, String> form = WebFormsTokenParser.buildDownloadOkForm(tokens, resultId);

        apiClient.postFormAndStream(postUrl, form, out, formHeaders);
    }
}
