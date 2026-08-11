package com.adil.apigateway.security;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver clientIpKeyResolver() {

        return exchange -> {

            InetSocketAddress remoteAddress =
                    exchange.getRequest().getRemoteAddress();

            if (remoteAddress == null) {
                return Mono.just("unknown-client");
            }

            if (remoteAddress.getAddress() != null) {
                return Mono.just(
                        remoteAddress
                                .getAddress()
                                .getHostAddress()
                );
            }

            String host = remoteAddress.getHostString();

            if (host == null || host.isBlank()) {
                return Mono.just("unknown-client");
            }

            return Mono.just(host);
        };
    }
}