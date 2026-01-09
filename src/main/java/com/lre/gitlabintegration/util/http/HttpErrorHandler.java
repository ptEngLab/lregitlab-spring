package com.lre.gitlabintegration.util.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lre.gitlabintegration.exceptions.GitLabResourceNotFoundException;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@UtilityClass
public class HttpErrorHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void handleRestClientError(
            RestClientException ex,
            String url,
            String context
    ) {
        if (ex instanceof HttpClientErrorException clientError) {
            handleClientError(clientError, url, context);
        } else if (ex instanceof HttpServerErrorException serverError) {
            handleServerError(serverError, url, context);
        } else {
            handleGenericError(ex, url, context);
        }
    }

    private static void handleClientError(
            HttpClientErrorException ex,
            String url,
            String context
    ) {
        int status = ex.getStatusCode().value();
        String errorBody = ex.getResponseBodyAsString();
        String reason = ex.getStatusText();
        String message = extractErrorMessage(errorBody, reason);

        String authError = String.format(
                "Unauthorized/Forbidden: Status=%d (%s), URL=%s, Context=%s, Message=%s",
                status,
                reason,
                url,
                context,
                message
        );

        String clientErrorMsg = String.format(
                "Client error: Status=%d (%s), URL=%s, Context=%s, Message=%s",
                status,
                reason,
                url,
                context,
                message
        );

        log.warn(
                "HTTP Client error: Status={}, URL={}, Context={}, Error={}",
                status,
                url,
                context,
                errorBody
        );

        if (status == 404) {
            throw new GitLabResourceNotFoundException(
                    message,
                    HttpStatus.valueOf(status)
            );
        }

        if (status == 401 || status == 403) {
            throw new LreException(authError, ex);
        }

        throw new LreException(clientErrorMsg, ex);
    }

    private static void handleServerError(
            HttpServerErrorException ex,
            String url,
            String context
    ) {
        int status = ex.getStatusCode().value();
        String errorBody = ex.getResponseBodyAsString();
        String reason = ex.getStatusText();
        String message = extractErrorMessage(errorBody, reason);

        log.error(
                "HTTP Server error: Status={}, URL={}, Context={}, Error={}",
                status,
                url,
                context,
                errorBody
        );

        String serverErrorMsg = String.format(
                "Server error: Status=%d (%s), URL=%s, Context=%s, Message=%s",
                status,
                reason,
                url,
                context,
                message
        );

        throw new LreException(serverErrorMsg, ex);
    }

    private static void handleGenericError(
            RestClientException ex,
            String url,
            String context
    ) {
        log.error(
                "Request failed: URL={}, Context={}",
                url,
                context,
                ex
        );

        String errorMsg = String.format(
                "Request failed: URL=%s, Context=%s, Error=%s",
                url,
                context,
                ex.getMessage()
        );

        throw new LreException(errorMsg, ex);
    }

    private static String extractErrorMessage(
            String errorBody,
            String defaultReason
    ) {
        if (errorBody == null || errorBody.isBlank()) {
            return defaultReason;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(errorBody);

            // Try common error message fields
            if (node.has("message")) {
                return node.get("message").asText();
            }
            if (node.has("error")) {
                return node.get("error").asText();
            }
            if (node.has("error_description")) {
                return node.get("error_description").asText();
            }

            // Return the whole JSON if no specific message field found
            return errorBody;

        } catch (Exception e) {
            log.debug(
                    "Failed to parse error response as JSON: {}",
                    errorBody,
                    e
            );
            return errorBody;
        }
    }
}
