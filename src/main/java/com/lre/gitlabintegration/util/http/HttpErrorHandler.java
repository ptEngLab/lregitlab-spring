package com.lre.gitlabintegration.util.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lre.gitlabintegration.exceptions.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class HttpErrorHandler {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final int MAX_LOG_BODY_CHARS = 2000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final String CLIENT_ERROR = "Client error: Status=%d (%s), URL=%s, Context=%s, Message=%s";
    private static final String SERVER_ERROR = "Server error: Status=%d (%s), URL=%s, Context=%s, Message=%s";
    private static final String REQUEST_FAILED_MSG = "Request failed: URL=%s, Context=%s, Error=%s";
    private static final String RATE_LIMIT_MSG = "Rate limited: Status=%d (%s), URL=%s, Context=%s, Message=%s";

    public static RuntimeException toDomainException(RestClientException ex, String url, String context) {
        if (ex instanceof RestClientResponseException rre) {
            return mapResponseException(rre, url, context);
        }
        return mapGeneric(ex, url, context);
    }

    private static RuntimeException mapResponseException(RestClientResponseException ex, String url, String context) {
        HttpStatusCode status = ex.getStatusCode();
        String reason = ex.getStatusText();
        String errorBody = ex.getResponseBodyAsString();
        String message = extractErrorMessage(errorBody, reason);

        String bodySnippet = snippet(errorBody);

        if (status.is4xxClientError()) {

            if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("HTTP 404: url={}, context={}", url, context);
                return new ResourceNotFoundException(message, status);
            }

            boolean sensitive = status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN);

            String safeSnippet = sensitive ? "<omitted>" : bodySnippet;

            log.warn("HTTP 4xx: status={}, url={}, context={}, bodySnippet={}", status.value(), url, context, safeSnippet);

            if (sensitive) return new AuthException(message, status);


            if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                return new ClientErrorException(
                        String.format(RATE_LIMIT_MSG, status.value(), reason, url, context, message),
                        status,
                        ex
                );
            }


            return new ClientErrorException(
                    String.format(CLIENT_ERROR, status.value(), reason, url, context, message),
                    status,
                    ex
            );
        }

        if (status.is5xxServerError()) {
            log.error("HTTP 5xx: status={}, url={}, context={}, bodySnippet={}", status.value(), url, context, bodySnippet);

            return new ServerErrorException(
                    String.format(SERVER_ERROR, status.value(), reason, url, context, message),
                    status, ex);
        }

        log.error("HTTP non-2xx: status={}, url={}, context={}, bodySnippet={}", status.value(), url, context, bodySnippet);
        return new LreException(String.format(SERVER_ERROR, status.value(), reason, url, context, message), ex);

    }

    private static RuntimeException mapGeneric(RestClientException ex, String url, String context) {
        log.error("Request failed: url={}, context={}", url, context, ex);
        return new LreException(String.format(REQUEST_FAILED_MSG, url, context, ex.getMessage()), ex);
    }

    private static String extractErrorMessage(String errorBody, String defaultReason) {
        if (errorBody == null || errorBody.isBlank()) return defaultReason;

        try {
            JsonNode node = OBJECT_MAPPER.readTree(errorBody);

            if (node.has("message")) {
                JsonNode m = node.get("message");
                return m.isTextual() ? m.asText() : snippet(m.toString());
            }
            if (node.has("error")) {
                JsonNode m = node.get("error");
                return m.isTextual() ? m.asText() : snippet(m.toString());
            }

            if (node.has("error_description")) return node.get("error_description").asText();

            // Don’t return the full JSON; return a snippet instead
            return snippet(errorBody);

        } catch (Exception e) {
            log.debug("Failed to parse error response as JSON", e);
            return snippet(errorBody);
        }
    }

    private static String snippet(String s) {
        if (s == null || s.isBlank()) return "";
        String norm = WHITESPACE.matcher(s).replaceAll(" ").trim();
        return norm.length() > MAX_LOG_BODY_CHARS ? norm.substring(0, MAX_LOG_BODY_CHARS) + "..." : norm;
    }
}
