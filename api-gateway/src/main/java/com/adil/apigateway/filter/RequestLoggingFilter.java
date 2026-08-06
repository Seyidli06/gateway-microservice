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

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        ServerHttpRequest request = exchange.getRequest();

        long startTime = System.nanoTime();

        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        HttpHeaders safeHeaders = sanitizeHeaders(request.getHeaders());

        log.info(
                "Incoming request: method={}, path={}, query={}, headers={}",
                method,
                path,
                query,
                safeHeaders
        );

        return chain.filter(exchange)
                .doOnError(exception ->
                        log.error(
                                "Request failed: method={}, path={}, message={}",
                                method,
                                path,
                                exception.getMessage()
                        )
                )
                .doFinally(signalType -> {
                    long duration = Duration.ofNanos(
                            System.nanoTime() - startTime
                    ).toMillis();

                    HttpStatusCode statusCode =
                            exchange.getResponse().getStatusCode();

                    Object status = statusCode == null
                            ? "UNKNOWN"
                            : statusCode.value();

                    log.info(
                            "Completed request: method={}, path={}, status={}, durationMs={}",
                            method,
                            path,
                            status,
                            duration
                    );
                });
    }

    private HttpHeaders sanitizeHeaders(HttpHeaders originalHeaders) {
        HttpHeaders safeHeaders = new HttpHeaders();

        safeHeaders.putAll(originalHeaders);

        for (String sensitiveHeader : SENSITIVE_HEADERS) {
            if (safeHeaders.containsHeader(sensitiveHeader)) {
                safeHeaders.set(sensitiveHeader, "[REDACTED]");
            }
        }

        return safeHeaders;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}