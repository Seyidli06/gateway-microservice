package com.adil.apigateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {

        return http

                /*
                 * Stateless REST API.
                 *
                 * Session/form-login authentication istifadə etmirik,
                 * buna görə hazırkı API üçün CSRF deaktivdir.
                 */
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                /*
                 * CORS configuration aşağıdakı
                 * corsConfigurationSource bean-dən götürülür.
                 */
                .cors(Customizer.withDefaults())

                /*
                 * Endpoint authorization.
                 */
                .authorizeExchange(exchanges -> exchanges

                        /*
                         * Container health checks public olmalıdır.
                         */
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        )
                        .permitAll()

                        /*
                         * Assignment-in hazırkı API-ləri public-dir.
                         *
                         * JWT əlavə edilərsə gələcəkdə
                         * authenticated() ilə dəyişdirilə bilər.
                         */
                        .pathMatchers("/profiles/**")
                        .permitAll()

                        .pathMatchers("/feedback/**")
                        .permitAll()

                        /*
                         * Browser CORS preflight requests.
                         */
                        .pathMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        /*
                         * Allowlist yanaşması:
                         * açıq şəkildə permit etmədiyimiz hər şey bağlıdır.
                         */
                        .anyExchange()
                        .denyAll()
                )

                /*
                 * Security response headers.
                 */
                .headers(headers -> headers

                        /*
                         * Clickjacking protection.
                         *
                         * Response iframe daxilində render edilə bilməz.
                         */
                        .frameOptions(frameOptions ->
                                frameOptions.mode(
                                        XFrameOptionsServerHttpHeadersWriter
                                                .Mode
                                                .DENY
                                )
                        )

                        /*
                         * Browser-in MIME type guessing etməsinin
                         * qarşısını alan X-Content-Type-Options
                         * Spring Security tərəfindən default verilir.
                         */

                        /*
                         * Referrer məlumatının başqa origin-lərə
                         * göndərilməməsi üçün daha sərt policy.
                         */
                        .referrerPolicy(referrerPolicy ->
                                referrerPolicy.policy(
                                        ReferrerPolicyServerHttpHeadersWriter
                                                .ReferrerPolicy
                                                .NO_REFERRER
                                )
                        )

                        /*
                         * API Gateway HTML/JS content serve etmədiyi üçün
                         * çox sərt CSP istifadə edə bilərik.
                         */
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'none'; " +
                                                "frame-ancestors 'none'; " +
                                                "base-uri 'none'; " +
                                                "form-action 'none'"
                                )
                        )

                        /*
                         * Gateway-in browser hardware/API capability-lərinə
                         * ehtiyacı yoxdur.
                         */
                        .permissionsPolicy(permissions ->
                                permissions.policy(
                                        "camera=(), " +
                                                "microphone=(), " +
                                                "geolocation=(), " +
                                                "payment=(), " +
                                                "usb=()"
                                )
                        )

                        /*
                         * Production HTTPS üçün HSTS.
                         *
                         * Spring Security HSTS header-i secure
                         * request-lərdə yazır.
                         */
                        .hsts(hsts ->
                                hsts
                                        .maxAge(Duration.ofDays(365))
                                        .includeSubdomains(true)
                                        .preload(false)
                        )
                )

                .build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            CorsProperties corsProperties
    ) {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * "*" istifadə etmirik.
         * Origin-lər application.yaml / environment-dən gəlir.
         */
        configuration.setAllowedOrigins(
                corsProperties.allowedOrigins()
        );

        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        HttpHeaders.AUTHORIZATION,
                        "X-Correlation-Id"
                )
        );

        /*
         * Frontend correlation ID-ni response-dan
         * oxuya bilsin.
         */
        configuration.setExposedHeaders(
                List.of(
                        "X-Correlation-Id"
                )
        );

        /*
         * Cookie/session authentication olmadığı üçün false.
         */
        configuration.setAllowCredentials(false);

        /*
         * Browser preflight nəticəsini 1 saat cache edə bilər.
         */
        configuration.setMaxAge(
                Duration.ofHours(1)
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}