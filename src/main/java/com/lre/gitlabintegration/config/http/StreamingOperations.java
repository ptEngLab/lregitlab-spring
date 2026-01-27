package com.lre.gitlabintegration.config.http;

import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class StreamingOperations {

    private final RestClient restClient;

    /**
     * Helper method to perform HTTP request and stream the response.
     */
    private void performStreamRequest(
            HttpMethod method, String url, OutputStream out, HttpHeaders headers, MultiValueMap<String, String> body
    ) {
        Objects.requireNonNull(out, "OutputStream must not be null!");

        // Use RestClient to perform the request
        try {
            var requestSpec = restClient.method(method)
                    .uri(url)
                    .headers(getHeaders(headers))  // Ensure headers are passed as a Consumer
                    .accept(MediaType.APPLICATION_JSON);

            // Add body for POST requests
            if (body != null) {
                requestSpec.body(body);
            }

            requestSpec.exchange((HttpRequest req, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) -> {
                if (!res.getStatusCode().is2xxSuccessful()) {
                    handleErrorResponse(url, res);
                }

                try (InputStream inputStream = res.getBody()) {
                    inputStream.transferTo(out);
                    out.flush();
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

        } catch (UncheckedIOException e) {
            throw HttpErrorHandler.toDomainException(
                    new RestClientException("Stream transfer failed"), url, method.name()
            );
        } catch (RestClientException e) {
            throw HttpErrorHandler.toDomainException(e, url, method.name() + " " + url);
        }
    }

    /**
     * Returns headers as a Consumer (if null or empty, returns an empty consumer).
     */
    private Consumer<HttpHeaders> getHeaders(HttpHeaders headers) {
        return h -> {
            if (headers != null && !headers.isEmpty()) {
                h.putAll(headers);  // Modify the headers if they are not null or empty
            }
        };
    }

    /**
     * Handles error responses from the server.
     */
    private void handleErrorResponse(String url, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) throws IOException {
        if (!res.getStatusCode().is2xxSuccessful() && res.getStatusCode().is3xxRedirection()) {
            throw new RestClientException("Non-2xx/3xx response: " + res.getStatusCode());
        }

        try (InputStream errorStream = res.getBody()) {
            byte[] errorBytes = errorStream.readAllBytes();
            throw new RestClientResponseException(
                    "Non-2xx response for url: " + url,
                    res.getStatusCode(), res.getStatusText(),
                    res.getHeaders(), errorBytes, StandardCharsets.UTF_8
            );
        }
    }

    /**
     * Streams response body for a GET request.
     */
    public void streamTo(String url, OutputStream out, HttpHeaders headers) {
        performStreamRequest(HttpMethod.GET, url, out, headers, null);
    }

    /**
     * Streams response body after posting form data.
     */
    public void postFormAndStream(String url, Map<String, String> form, OutputStream out, HttpHeaders headers) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        form.forEach(body::add);
        performStreamRequest(HttpMethod.POST, url, out, headers, body);
    }
}
