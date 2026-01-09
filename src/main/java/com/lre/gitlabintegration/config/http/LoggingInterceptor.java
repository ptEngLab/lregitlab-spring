package com.lre.gitlabintegration.config.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            byte @NonNull [] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {

        logRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    private void logRequest(@NonNull HttpRequest request, byte @NonNull [] body) {
        log.info("============================== Request Begin ==============================");
        log.info("URI          : {}", request.getURI());
        log.info("Method       : {}", request.getMethod());
        log.info("Headers      : {}", request.getHeaders());
        log.info("Request body : {}", new String(body, StandardCharsets.UTF_8));
        log.info("============================== Request End ================================");
    }

    private void logResponse(@NonNull ClientHttpResponse response) throws IOException {
        log.info("============================== Response Begin ==============================");
        log.info("Status code  : {}", response.getStatusCode());
        log.info("Status text  : {}", response.getStatusText());
        log.info("Headers      : {}", response.getHeaders());
        log.info("============================== Response End ================================");
    }
}
