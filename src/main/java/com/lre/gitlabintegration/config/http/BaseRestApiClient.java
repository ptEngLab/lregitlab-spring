package com.lre.gitlabintegration.config.http;

import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class BaseRestApiClient {

    private final RestClient restClient;

    public BaseRestApiClient(RestClient restClient) {
        this.restClient = restClient.mutate()
                // Default: accept JSON responses (can be overridden per request)
                .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }

    /* =========================
       Header Management
       ========================= */

    /**
     * Merge default headers + custom headers + accept override.
     * Priority:
     * 1. acceptOverride (highest)
     * 2. customHeaders (overrides defaults)
     * 3. default headers (from builder)

     * Note: This uses overwrite-per-header-key semantics (target.put).
     */
    private void applyHeaders(HttpHeaders target, HttpHeaders custom, MediaType acceptOverride) {

        // Apply custom headers (overwrite per key), defensively copying values
        if (custom != null && !custom.isEmpty()) {
            custom.forEach((key, values) -> target.put(key, new ArrayList<>(values)));
        }

        // Accept override has the highest priority
        if (acceptOverride != null) {
            target.setAccept(List.of(acceptOverride));
        }
    }

    /* =========================
       GET
       ========================= */

    public <T> T get(String url, Class<T> responseType) {
        return get(url, responseType, null);
    }

    public <T> T get(String url, Class<T> responseType, HttpHeaders customHeaders) {
        try {
            return restClient.get()
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, null))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toDomainException(e, HttpMethod.GET, url);
        }
    }

    public <T> T get(String url, ParameterizedTypeReference<@NonNull T> responseType, HttpHeaders customHeaders) {
        try {
            return restClient.get()
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, null))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toDomainException(e, HttpMethod.GET, url);
        }
    }

    public <T> T get(String url, ParameterizedTypeReference<@NonNull T> responseType) {
        return get(url, responseType, null);
    }
        public <T> ResponseEntity<@NonNull T> getEntity(String url, Class<T> responseType, HttpHeaders customHeaders) {
        try {
            return restClient.get()
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, null))
                    .retrieve()
                    .toEntity(responseType);
        } catch (RestClientException e) {
            throw toDomainException(e, HttpMethod.GET, url);
        }
    }

    /* =========================
       Generic exchange (with body)
       ========================= */

    public <T, R> T exchangeWithBody(
            HttpMethod method,
            String url,
            R requestBody,
            MediaType contentType,      // what we're sending
            MediaType accept,           // what we want back (null = default)
            Class<T> responseType,
            HttpHeaders customHeaders
    ) {
        try {
            RestClient.RequestBodySpec spec = restClient
                    .method(method)
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, accept));

            if (contentType != null) {
                spec.contentType(contentType);
            }
            if (requestBody != null) {
                spec.body(requestBody);
            }

            return spec.retrieve().body(responseType);

        } catch (RestClientException e) {
            throw toDomainException(e, method, url);
        }
    }

    public <T, R> T exchangeWithBody(
            HttpMethod method,
            String url,
            R requestBody,
            MediaType contentType,
            MediaType accept,
            ParameterizedTypeReference<@NonNull T> responseType,
            HttpHeaders customHeaders
    ) {
        try {
            RestClient.RequestBodySpec spec = restClient
                    .method(method)
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, accept));

            if (contentType != null) {
                spec.contentType(contentType);
            }
            if (requestBody != null) {
                spec.body(requestBody);
            }

            return spec.retrieve().body(responseType);

        } catch (RestClientException e) {
            throw toDomainException(e, method, url);
        }
    }

    public <T, R> ResponseEntity<@NonNull T> exchangeEntityWithBody(
            HttpMethod method,
            String url,
            R requestBody,
            MediaType contentType,
            MediaType accept,
            Class<T> responseType,
            HttpHeaders customHeaders
    ) {
        try {
            RestClient.RequestBodySpec spec = restClient
                    .method(method)
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, accept));

            if (contentType != null) {
                spec.contentType(contentType);
            }
            if (requestBody != null) {
                spec.body(requestBody);
            }

            return spec.retrieve().toEntity(responseType);

        } catch (RestClientException e) {
            throw toDomainException(e, method, url);
        }
    }

    /* =========================
       JSON templates
       ========================= */

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

    /* =========================
       XML templates
       ========================= */

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

    /* =========================
       MULTIPART (Send form data, Receive JSON)
       ========================= */

    public <T> T postMultipart(
            String url,
            MultiValueMap<@NonNull String, Object> parts,
            Class<T> responseType,
            HttpHeaders customHeaders
    ) {
        Objects.requireNonNull(parts, "parts must not be null");

        try {
            return restClient.post()
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, MediaType.APPLICATION_JSON))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toDomainException(e, HttpMethod.POST, url);
        }
    }

    /* =========================
       DELETE
       ========================= */

    public void delete(String url) {
        delete(url, null);
    }

    public void delete(String url, HttpHeaders customHeaders) {
        try {
            restClient.delete()
                    .uri(url)
                    .headers(h -> applyHeaders(h, customHeaders, null))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw toDomainException(e, HttpMethod.DELETE, url);
        }
    }

    /* =========================
       Error handling
       ========================= */

    private RuntimeException toDomainException(RestClientException e, HttpMethod method, String url) {
        // IMPORTANT: HttpErrorHandler returns the exception to throw — do not ignore it.
        return HttpErrorHandler.toDomainException(e, url, method + " " + url);
    }
}
