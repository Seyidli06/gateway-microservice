package com.adil.apigateway.filter;

import static com.adil.apigateway.security.SecurityConstants.CORRELATION_ID_HEADER;
import static com.adil.apigateway.security.SecurityConstants.MAX_CORRELATION_ID_LENGTH;
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
import com.adil.apigateway.security.SecurityConstants;

import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {



    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "Proxy-Authorization",
            "X-API-Key"
    );

    private static final Pattern SAFE_CORRELATION_ID =
            Pattern.compile("^[A-Za-z0-9._-]+$");

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
            return generateCorrelationId();
        }

        String normalizedCorrelationId =
                existingCorrelationId.trim();

        if (normalizedCorrelationId.length()
                > MAX_CORRELATION_ID_LENGTH) {
            return generateCorrelationId();
        }

        if (!SAFE_CORRELATION_ID
                .matcher(normalizedCorrelationId)
                .matches()) {
            return generateCorrelationId();
        }

        return normalizedCorrelationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
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