package com.adil.profileservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-Id";

    private static final String MDC_KEY =
            "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        MDC.put(MDC_KEY, correlationId);

        try {
            log.info(
                    "Incoming backend request: method={}, path={}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);

            log.info(
                    "Completed backend request: method={}, path={}, status={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus()
            );
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}