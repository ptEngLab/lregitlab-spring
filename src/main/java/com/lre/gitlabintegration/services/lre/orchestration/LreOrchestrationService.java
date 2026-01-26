package com.lre.gitlabintegration.services.lre.orchestration;

import com.lre.gitlabintegration.dto.audit.AuditContext;
import com.lre.gitlabintegration.security.GitLabCiPrincipal;
import com.lre.gitlabintegration.services.lre.authsession.LreSessionManager;
import com.lre.gitlabintegration.services.lre.run.RunTest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LreOrchestrationService {

    private final RunTest runTest;
    private final AuditExecutor auditExecutor;
    private final GitLabAuthGuard authGuard;
    private final LreSessionManager lreSessionManager;

    public TimeslotCheckResponse checkTimeslotAvailability(
            String domain,
            String project,
            int testId,
            TimeslotCheckRequest request,
            GitLabCiPrincipal principal
    ) {

        AuditContext ctx =
                authGuard.requireProjectAccess(domain, project, principal);

        lreSessionManager.ensureAuthenticated(ctx.domain(), ctx.project());

        return auditExecutor.execute(
                ctx,
                () -> runTest.calculateAvailability(testId, request),
                this::isTimeslotCheckSuccessful,
                "Timeslot check failed"
        );
    }

    public RunResponse triggerRun(
            String domain,
            String project,
            int testId,
            TimeslotCheckRequest request,
            GitLabCiPrincipal principal
    ) {

        AuditContext ctx =
                authGuard.requireProjectAccess(domain, project, principal);

        lreSessionManager.ensureAuthenticated(ctx.domain(), ctx.project());

        return auditExecutor.execute(
                ctx,
                () -> runTest.startRun(testId, request),
                this::isRunSuccessful,
                "Start run failed"
        );
    }

    /**
     * Checks if the timeslot check was successful.
     * A timeslot check is considered successful if it has no errors.
     */
    private boolean isTimeslotCheckSuccessful(TimeslotCheckResponse response) {
        return response != null && !response.hasErrors();
    }

    /**
     * Checks if the run was successfully started.
     * A run is considered successful if it has a valid runId (> 0).
     */
    private boolean isRunSuccessful(RunResponse response) {
        return response != null && response.getRunId() > 0;
    }
}
