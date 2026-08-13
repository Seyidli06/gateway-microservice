package com.adil.apigateway.security;

public final class SecurityConstants {

    public static final String CORRELATION_ID_HEADER =
            "X-Correlation-Id";

    public static final int MAX_CORRELATION_ID_LENGTH = 100;

    private SecurityConstants() {
    }
}