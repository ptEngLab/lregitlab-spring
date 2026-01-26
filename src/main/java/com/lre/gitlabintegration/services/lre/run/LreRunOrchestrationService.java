package com.lre.gitlabintegration.services.lre.run;

import com.lre.gitlabintegration.client.api.RunApiClient;
import com.lre.gitlabintegration.dto.audit.AuditContext;
import com.lre.gitlabintegration.dto.lrescript.DownloadableStream;
import com.lre.gitlabintegration.security.GitLabCiPrincipal;
import com.lre.gitlabintegration.services.lre.authsession.LreSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Service
@RequiredArgsConstructor
public class LreRunOrchestrationService {

    private final RunApiClient runApiClient;




    public DownloadableStream downloadRunResultFile(
            String domain, String project, int runId, int resultId, GitLabCiPrincipal principal
    ) {
        AuditContext ctx = authGuard.requireProjectAccess(domain, project, principal);
        LreSessionManager.ensureAuthenticated(ctx.domain(), ctx.project());

        // deterministic filename because LRE headers can't be safely obtained before streaming begins
        String fileName = fileDownloadService.buildResultFileName(runId, resultId);

        StreamingResponseBody body = out -> {
            runApiClient.streamRunResultFile(ctx.domain(), ctx.project(), runId, resultId, out);
            out.flush();
        };

        return new DownloadableStream(body, fileName);
    }

    public DownloadableStream downloadRunResultViaUi(
            String domain, String project, int runId, int resultId, GitLabCiPrincipal principal
    ) {
        AuditContext ctx = authGuard.requireProjectAccess(domain, project, principal);
        LreSessionManager.ensureAuthenticated(ctx.domain(), ctx.project());

        String fileName = "lre-result-" + runId + "-" + resultId + ".bin";

        StreamingResponseBody body = out -> {
            runApiClient.streamDownloadResultViaUi(ctx.domain(), ctx.project(), resultId, out);
            out.flush();
        };

        return new DownloadableStream(body, fileName);
    }

}
