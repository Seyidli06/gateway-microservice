package com.adil.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "Proxy-Authorization",
            "X-API-Key"
    );

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        ServerHttpRequest originalRequest = exchange.getRequest();

        String correlationId = resolveCorrelationId(originalRequest);

        ServerHttpRequest request = originalRequest
                .mutate()
                .headers(headers ->
                        headers.set(
                                CORRELATION_ID_HEADER,
                                correlationId
                        )
                )
                .build();

        ServerWebExchange mutatedExchange = exchange
                .mutate()
                .request(request)
                .build();

        mutatedExchange.getResponse()
                .getHeaders()
                .set(
                        CORRELATION_ID_HEADER,
                        correlationId
                );

        long startTime = System.nanoTime();

        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();

        HttpHeaders safeHeaders =
                sanitizeHeaders(request.getHeaders());

        log.info(
                "Incoming request: correlationId={}, method={}, path={}, query={}, headers={}",
                correlationId,
                method,
                path,
                query,
                safeHeaders
        );

        return chain.filter(mutatedExchange)
                .doOnError(exception ->
                        log.error(
                                "Request failed: correlationId={}, method={}, path={}, message={}",
                                correlationId,
                                method,
                                path,
                                exception.getMessage()
                        )
                )
                .doFinally(signalType -> {
                    long durationMs = Duration
                            .ofNanos(
                                    System.nanoTime() - startTime
                            )
                            .toMillis();

                    HttpStatusCode statusCode =
                            mutatedExchange
                                    .getResponse()
                                    .getStatusCode();

                    Object status =
                            statusCode == null
                                    ? "UNKNOWN"
                                    : statusCode.value();

                    log.info(
                            "Completed request: correlationId={}, method={}, path={}, status={}, durationMs={}",
                            correlationId,
                            method,
                            path,
                            status,
                            durationMs
                    );
                });
    }

    private String resolveCorrelationId(
            ServerHttpRequest request
    ) {
        String existingCorrelationId =
                request.getHeaders()
                        .getFirst(CORRELATION_ID_HEADER);

        if (existingCorrelationId == null
                || existingCorrelationId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return existingCorrelationId.trim();
    }

    private HttpHeaders sanitizeHeaders(
            HttpHeaders originalHeaders
    ) {
        HttpHeaders safeHeaders = new HttpHeaders();

        safeHeaders.putAll(originalHeaders);

        for (String sensitiveHeader : SENSITIVE_HEADERS) {
            if (safeHeaders.containsHeader(sensitiveHeader)) {
                safeHeaders.set(
                        sensitiveHeader,
                        "[REDACTED]"
                );
            }
        }

        return safeHeaders;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}