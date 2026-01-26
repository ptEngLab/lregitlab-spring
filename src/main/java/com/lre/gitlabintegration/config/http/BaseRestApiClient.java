package com.lre.gitlabintegration.config.http;

import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class BaseRestApiClient {

    private final RestClient restClient;

    public BaseRestApiClient(RestClient restClient) {
        this.restClient = restClient.mutate()
                // Default: accept JSON responses (can be overridden per request)
                .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }

    /* ==========================================================
       Header handling
       ========================================================== */

    private void applyHeaders(HttpHeaders target, HttpHeaders custom, MediaType acceptOverride) {

        if (custom != null && !custom.isEmpty()) custom.forEach((k, v) -> target.put(k, new ArrayList<>(v)));

        if (acceptOverride != null) target.setAccept(List.of(acceptOverride));

    }

    /* ==========================================================
       Core request builder
       ========================================================== */

    private <R> RestClient.RequestBodySpec buildRequest(HttpMethod method, String url, R body,
                                                        MediaType contentType, MediaType accept, HttpHeaders headers) {

        RestClient.RequestBodySpec spec = restClient.method(method).uri(url).headers(h -> applyHeaders(h, headers, accept));

        if (contentType != null) spec.contentType(contentType);
        if (body != null) spec.body(body);

        return spec;
    }

    /* ==========================================================
       Core executor (single choke point)
       ========================================================== */

    private <T, R> T execute(HttpMethod method, String url, R body,
                             MediaType contentType, MediaType accept, HttpHeaders headers,
                             Function<RestClient.ResponseSpec, T> extractor) {
        try {
            RestClient.ResponseSpec responseSpec = buildRequest(method, url, body, contentType, accept, headers).retrieve();

            return extractor.apply(responseSpec);

        } catch (RestClientException e) {
            throw toDomainException(e, method, url);
        }
    }

    /* ==========================================================
       GET
       ========================================================== */

    public <T> T get(String url, Class<T> responseType) {
        return get(url, responseType, null);
    }

    public <T> T get(String url, Class<T> responseType, HttpHeaders headers) {
        // if caller uses Void.class, treat as bodiless
        if (responseType == Void.class) {
            getBodiless(url, null, headers);
            return null;
        }

        return execute(HttpMethod.GET, url, null, null, null,
                headers, rs -> rs.body(responseType));
    }

    public <T> T get(String url, ParameterizedTypeReference<@NonNull T> responseType) {
        return get(url, responseType, null);
    }

    public <T> T get(String url, ParameterizedTypeReference<@NonNull T> responseType, HttpHeaders headers) {
        return execute(
                HttpMethod.GET, url, null, null, null, headers,
                rs -> rs.body(responseType)
        );
    }

    public <T> ResponseEntity<@NonNull T> getEntity(
            String url,
            Class<T> responseType,
            HttpHeaders headers
    ) {
        return execute(
                HttpMethod.GET, url, null, null, null, headers,
                rs -> rs.toEntity(responseType)
        );
    }

    public void getBodiless(String url) {
        getBodiless(url, null, null);
    }

    public void getBodiless(String url, MediaType accept, HttpHeaders headers) {
        execute(
                HttpMethod.GET, url, null, null, accept, headers,
                rs -> {
                    rs.toBodilessEntity();
                    return null;
                }
        );
    }


    /* ==========================================================
       Generic exchange (with body)
       ========================================================== */

    public <T, R> T exchangeWithBody(
            HttpMethod method,
            String url,
            R body,
            MediaType contentType,
            MediaType accept,
            Class<T> responseType,
            HttpHeaders headers
    ) {
        // Optional nicety: if caller uses Void.class, treat as bodiless
        if (responseType == Void.class) {
            execute(
                    method, url, body, contentType, accept, headers,
                    rs -> {
                        rs.toBodilessEntity();
                        return null;
                    }
            );
            return null;
        }

        return execute(
                method, url, body, contentType, accept, headers,
                rs -> rs.body(responseType)
        );
    }

    public <T, R> T exchangeWithBody(
            HttpMethod method,
            String url,
            R body,
            MediaType contentType,
            MediaType accept,
            ParameterizedTypeReference<@NonNull T> responseType,
            HttpHeaders headers
    ) {
        return execute(
                method, url, body, contentType, accept, headers,
                rs -> rs.body(responseType)
        );
    }

    public <T, R> ResponseEntity<@NonNull T> exchangeEntityWithBody(
            HttpMethod method,
            String url,
            R body,
            MediaType contentType,
            MediaType accept,
            Class<T> responseType,
            HttpHeaders headers
    ) {
        return execute(
                method, url, body, contentType, accept, headers,
                rs -> rs.toEntity(responseType)
        );
    }

    /* ==========================================================
       JSON templates
       ========================================================== */

    public <T, R> T postJson(String url, R body, Class<T> responseType) {
        return postJson(url, body, responseType, null);
    }

    public <T, R> T postJson(String url, R body, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.POST,
                url,
                body,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    public <R> void postJsonBodiless(String url, R body) {
        postJsonBodiless(url, body, null);
    }

    public <R> void postJsonBodiless(String url, R body, HttpHeaders headers) {
        execute(
                HttpMethod.POST,
                url,
                body,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON,
                headers,
                rs -> {
                    rs.toBodilessEntity();
                    return null;
                }
        );
    }

    public <T, R> T putJson(String url, R body, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.PUT,
                url,
                body,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    public <T, R> T patchJson(String url, R body, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.PATCH,
                url,
                body,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    /* ==========================================================
       XML templates
       ========================================================== */

    public <T, R> T postXmlReceiveJson(String url, R xmlBody, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.POST,
                url,
                xmlBody,
                MediaType.APPLICATION_XML,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    public <T, R> T putXmlReceiveJson(String url, R xmlBody, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.PUT,
                url,
                xmlBody,
                MediaType.APPLICATION_XML,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    public <T, R> T patchXmlReceiveJson(String url, R xmlBody, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.PATCH,
                url,
                xmlBody,
                MediaType.APPLICATION_XML,
                MediaType.APPLICATION_JSON,
                responseType,
                headers
        );
    }

    public <T, R> T postXml(String url, R xmlBody, Class<T> responseType, HttpHeaders headers) {
        return exchangeWithBody(
                HttpMethod.POST,
                url,
                xmlBody,
                MediaType.APPLICATION_XML,
                MediaType.APPLICATION_XML,
                responseType,
                headers
        );
    }

    /* ==========================================================
       MULTIPART (Send form data, Receive JSON)
       ========================================================== */

    public <T> T postMultipart(
            String url,
            MultiValueMap<@NonNull String, Object> parts,
            Class<T> responseType,
            HttpHeaders headers
    ) {
        Objects.requireNonNull(parts, "parts must not be null");

        return execute(
                HttpMethod.POST,
                url,
                parts,
                MediaType.MULTIPART_FORM_DATA,
                MediaType.APPLICATION_JSON,
                headers,
                rs -> rs.body(responseType)
        );
    }

    /* ==========================================================
       DELETE
       ========================================================== */

    public void delete(String url) {
        delete(url, null);
    }

    public void delete(String url, HttpHeaders headers) {
        execute(
                HttpMethod.DELETE,
                url,
                null,
                null,
                null,
                headers,
                rs -> {
                    rs.toBodilessEntity();
                    return null;
                }
        );
    }

    /* ==========================================================
       Error handling
       ========================================================== */

    private RuntimeException toDomainException(
            RestClientException e,
            HttpMethod method,
            String url
    ) {
        return HttpErrorHandler.toDomainException(e, url, method + " " + url);
    }


    public void streamTo(String url, OutputStream out, MediaType accept, HttpHeaders headers) {
        Objects.requireNonNull(out, "OutputStream must not be null");

        try {
            buildRequest(HttpMethod.GET, url, null, null, accept, headers)
                    .exchange((HttpRequest req, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) -> {

                        // IMPORTANT: exchange() does NOT throw on non-2xx
                        if (!res.getStatusCode().is2xxSuccessful()) {
                            throw new RestClientException("Non-2xx response: " + res.getStatusCode());
                        }

                        try (InputStream inputStream = res.getBody()) {
                            if (inputStream == null) {
                                throw new IOException("Empty response body for: " + url);
                            }
                            inputStream.transferTo(out);
                            out.flush();
                            return null;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

        } catch (UncheckedIOException e) {
            throw HttpErrorHandler.toDomainException(
                    new RestClientException("Stream transfer failed", e.getCause()),
                    url,
                    "GET " + url
            );
        } catch (RestClientException e) {
            throw HttpErrorHandler.toDomainException(e, url, "GET " + url);
        }
    }

}
