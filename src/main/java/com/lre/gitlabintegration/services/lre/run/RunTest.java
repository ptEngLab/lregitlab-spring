package com.lre.gitlabintegration.services.lre.run;

import com.lre.gitlabintegration.client.api.RunApiClient;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunTest {

    private final RunApiClient runApiClient;

    public TimeslotCheckResponse calculateAvailability(int testId, TimeslotCheckRequest request) {
        log.debug("Calculating timeslot availability for test ID: {}", testId);

        TimeslotCheckResponse timeslotCheckResponse =
                runApiClient.checkTimeslotAvailability(testId, request);

        if (timeslotCheckResponse.hasErrors()) {
            log.error(
                "Timeslot validation failed for testId={}: {}",
                testId,
                timeslotCheckResponse.errors()
            );
            throw new TimeslotValidationException(timeslotCheckResponse.errors());
        }

        log.debug("Timeslot available for test ID: {}", testId);
        return timeslotCheckResponse;
    }

    public RunResponse startRun(int testId, TimeslotCheckRequest request) {
        log.debug("Starting run for test ID: {}", testId);

        // Validate timeslot availability first
        calculateAvailability(testId, request);

        RunResponse response = runApiClient.startRun(testId, request);

        if (response == null) {
            log.error(
                "Failed to start run for testId={} - received null response",
                testId
            );
            throw new LreException("Failed to start run for test ID: " + testId);
        }

        log.info(
            "Test run started successfully: runId={}, testId={}, name={}",
            response.getQcRunId(),
            response.getTestId(),
            response.getName()
        );

        return response;
    }
}
