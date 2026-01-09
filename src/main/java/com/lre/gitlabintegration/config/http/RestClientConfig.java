package com.lre.gitlabintegration.config.http;

import com.lre.gitlabintegration.config.properties.GitLabProperties;
import com.lre.gitlabintegration.config.properties.LreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RestClientConfig {

    private final GitLabProperties gitLabProperties;
    private final LreProperties lreProperties;
    private final SSLContext sslContext;

    @Value("${http.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${http.read.timeout:30000}")
    private int readTimeout;

    @Value("${http.connection.pool.max:50}")
    private int maxConnections;

    @Value("${http.connection.ttl:300000}") // 5 minutes default
    private int connectionTtl;

    @Value("${http.connection.idle.timeout:60000}") // 1 minute default
    private int idleTimeout;

    @Bean
    public RestClient gitlabRestClient() {
        log.info("GitLab Base URL configured as: {}", gitLabProperties.getUrl());

        HttpComponentsClientHttpRequestFactory factory = getHttpRequestFactory(sslContext, false);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(gitLabProperties.getUrl())
                .defaultHeader("PRIVATE-TOKEN", gitLabProperties.getToken())
//                .requestInterceptor(new LoggingInterceptor())
                .build();
    }

    @Bean
    public RestClient lreRestClient() {
        log.info("LRE Base URL configured as: {}", lreProperties.getUrl());

        HttpComponentsClientHttpRequestFactory factory = getHttpRequestFactory(sslContext, true);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(lreProperties.getUrl())
//                .requestInterceptor(new LoggingInterceptor())
                .build();
    }

    private HttpComponentsClientHttpRequestFactory getHttpRequestFactory(SSLContext sslContext, boolean enableCookies) {
        HttpClient httpClient = buildHttpClient(sslContext, enableCookies);
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(Duration.ofMillis(connectionTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        return factory;
    }

    private HttpClient buildHttpClient(SSLContext sslContext, boolean enableCookies) {
        // Create connection manager with proper timeout and eviction settings
        PoolingHttpClientConnectionManager connectionManager = createConnectionManager(sslContext);

        // Create request config
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        // Build HTTP client
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(TimeValue.of(idleTimeout, TimeUnit.MILLISECONDS))
                .evictExpiredConnections();

        // Enable cookie management for LRE (session-based authentication)
        if (enableCookies) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            clientBuilder.setDefaultCookieStore(cookieStore);
            log.debug("Cookie store enabled for session management");
        }

        HttpClient httpClient = clientBuilder.build();

        log.info("HTTP Client configured: connectionTimeout={}ms, readTimeout={}ms, "
                        + "maxConnections={}, connectionTtl={}ms, idleTimeout={}ms, cookies={}",
                connectionTimeout, readTimeout, maxConnections, connectionTtl, idleTimeout, enableCookies);

        return httpClient;
    }

    /**
     * Creates connection manager with proper timeout and eviction settings.
     */
    private PoolingHttpClientConnectionManager createConnectionManager(SSLContext sslContext) {
        DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

        // Connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setSocketTimeout(Timeout.ofMilliseconds(readTimeout))
                .setTimeToLive(TimeValue.ofMilliseconds(connectionTtl))
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(idleTimeout))
                .build();

        // Socket configuration
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        // Build connection manager
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .setDefaultConnectionConfig(connectionConfig)
                .setDefaultSocketConfig(socketConfig)
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxConnections / 2)
                .build();
    }
}
