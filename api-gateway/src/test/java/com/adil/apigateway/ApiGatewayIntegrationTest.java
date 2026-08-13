package com.adil.apigateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(ApiGatewayIntegrationTest.TestJwtConfig.class)
class ApiGatewayIntegrationTest {

    private static final String USER_TOKEN = "user-token";
    private static final String ADMIN_TOKEN = "admin-token";

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    /*
     * Fake Profile Service.
     *
     * Gateway integration testində real Profile Service
     * qaldırmaq əvəzinə lightweight Reactor Netty server
     * istifadə edirik.
     */
    private static final DisposableServer profileBackend =
            HttpServer.create()
                    .port(0)
                    .route(routes -> {

                        routes.get(
                                "/profiles",
                                (request, response) ->
                                        response
                                                .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        "application/json"
                                                )
                                                .sendString(
                                                        Mono.just(
                                                                """
                                                                {
                                                                  "service": "profile",
                                                                  "result": "ok"
                                                                }
                                                                """
                                                        )
                                                )
                        );

                        routes.get(
                                "/profiles/query-test",
                                (request, response) ->
                                        response
                                                .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        "application/json"
                                                )
                                                .sendString(
                                                        Mono.just(
                                                                """
                                                                {
                                                                  "uri": "%s"
                                                                }
                                                                """.formatted(
                                                                        request.uri()
                                                                )
                                                        )
                                                )
                        );

                        routes.post(
                                "/profiles",
                                (request, response) ->
                                        response
                                                .status(201)
                                                .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        "application/json"
                                                )
                                                .sendString(
                                                        Mono.just(
                                                                """
                                                                {
                                                                  "service": "profile",
                                                                  "result": "created"
                                                                }
                                                                """
                                                        )
                                                )
                        );
                    })
                    .bindNow();

    /*
     * Fake Feedback Service.
     */
    private static final DisposableServer feedbackBackend =
            HttpServer.create()
                    .port(0)
                    .route(routes -> {

                        routes.get(
                                "/feedback",
                                (request, response) ->
                                        response
                                                .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        "application/json"
                                                )
                                                .sendString(
                                                        Mono.just(
                                                                """
                                                                {
                                                                  "service": "feedback",
                                                                  "result": "ok"
                                                                }
                                                                """
                                                        )
                                                )
                        );

                        routes.post(
                                "/feedback",
                                (request, response) ->
                                        response
                                                .status(201)
                                                .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        "application/json"
                                                )
                                                .sendString(
                                                        Mono.just(
                                                                """
                                                                {
                                                                  "service": "feedback",
                                                                  "result": "created"
                                                                }
                                                                """
                                                        )
                                                )
                        );
                    })
                    .bindNow();

    /*
     * application-test.yaml route-larının fake backend-lərə
     * yönəlməsi üçün dynamic URL-lər.
     */
    @DynamicPropertySource
    static void registerProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "TEST_PROFILE_SERVICE_URL",
                () -> "http://localhost:"
                        + profileBackend.port()
        );

        registry.add(
                "TEST_FEEDBACK_SERVICE_URL",
                () -> "http://localhost:"
                        + feedbackBackend.port()
        );
    }

    @BeforeEach
    void setUp() {

        webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(
                        "http://localhost:" + port
                )
                .build();
    }

    /*
     * USER JWT ilə request göndərən client.
     */
    private WebTestClient userClient() {

        return webTestClient
                .mutate()
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + USER_TOKEN
                )
                .build();
    }

    /*
     * ADMIN JWT ilə request göndərən client.
     */
    private WebTestClient adminClient() {

        return webTestClient
                .mutate()
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + ADMIN_TOKEN
                )
                .build();
    }

    @Test
    void contextLoads() {

        assertNotNull(webTestClient);
    }

    /*
     * GET /profiles USER üçün icazəlidir.
     *
     * Həm authentication,
     * həm RBAC,
     * həm də routing test olunur.
     */
    @Test
    void profilesRoute_shouldForwardToProfileService() {

        userClient()
                .get()
                .uri("/profiles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("profile")
                .jsonPath("$.result")
                .isEqualTo("ok");
    }

    /*
     * GET /feedback yalnız ADMIN üçündür.
     */
    @Test
    void feedbackRoute_shouldForwardToFeedbackService() {

        adminClient()
                .get()
                .uri("/feedback")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("feedback")
                .jsonPath("$.result")
                .isEqualTo("ok");
    }

    /*
     * Gateway query parameter-ləri dəyişməməlidir.
     */
    @Test
    void profilesRoute_shouldPreserveQueryParameters() {

        userClient()
                .get()
                .uri(
                        "/profiles/query-test"
                                + "?page=2"
                                + "&size=15"
                                + "&sortBy=name"
                                + "&direction=asc"
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.uri")
                .isEqualTo(
                        "/profiles/query-test"
                                + "?page=2"
                                + "&size=15"
                                + "&sortBy=name"
                                + "&direction=asc"
                );
    }

    /*
     * Client correlation ID göndərmirsə,
     * Gateway özü yaratmalıdır.
     */
    @Test
    void gateway_shouldAddCorrelationIdWhenMissing() {

        userClient()
                .get()
                .uri("/profiles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists("X-Correlation-Id");
    }

    /*
     * Valid correlation ID qorunmalıdır.
     */
    @Test
    void gateway_shouldPreserveExistingCorrelationId() {

        String correlationId =
                "test-correlation-123";

        userClient()
                .get()
                .uri("/profiles")
                .header(
                        "X-Correlation-Id",
                        correlationId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals(
                        "X-Correlation-Id",
                        correlationId
                );
    }

    /*
     * Phase 1-də əlavə etdiyimiz explicit valid-ID testi.
     */
    @Test
    void gateway_shouldPreserveValidCorrelationId() {

        String correlationId =
                "mentor-test-123";

        userClient()
                .get()
                .uri("/profiles")
                .header(
                        "X-Correlation-Id",
                        correlationId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals(
                        "X-Correlation-Id",
                        correlationId
                );
    }

    /*
     * 100 simvoldan uzun correlation ID
     * təhlükəsiz yeni UUID ilə əvəz olunmalıdır.
     */
    @Test
    void gateway_shouldReplaceTooLongCorrelationId() {

        String invalidCorrelationId =
                "a".repeat(101);

        userClient()
                .get()
                .uri("/profiles")
                .header(
                        "X-Correlation-Id",
                        invalidCorrelationId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .value(
                        "X-Correlation-Id",
                        value -> {

                            assertNotEquals(
                                    invalidCorrelationId,
                                    value
                            );

                            assertFalse(
                                    value.isBlank()
                            );
                        }
                );
    }

    /*
     * Whitelist-ə uyğun olmayan correlation ID
     * qəbul edilməməlidir.
     */
    @Test
    void gateway_shouldReplaceMalformedCorrelationId() {

        String invalidCorrelationId =
                "abc 123 / malicious";

        userClient()
                .get()
                .uri("/profiles")
                .header(
                        "X-Correlation-Id",
                        invalidCorrelationId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .value(
                        "X-Correlation-Id",
                        value -> {

                            assertNotEquals(
                                    invalidCorrelationId,
                                    value
                            );

                            assertFalse(
                                    value.isBlank()
                            );
                        }
                );
    }

    /*
     * Protected resource token olmadan çağırıla bilməz.
     */
    @Test
    void profilesRoute_withoutToken_shouldBeUnauthorized() {

        webTestClient
                .get()
                .uri("/profiles")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    /*
     * USER feedback list-i görə bilməz.
     *
     * Authentication var,
     * authority kifayət deyil → 403.
     */
    @Test
    void feedbackRoute_withUserRole_shouldBeForbidden() {

        userClient()
                .get()
                .uri("/feedback")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    /*
     * Tanınmayan protected route token olmadan
     * reject olunmalıdır.
     */
    @Test
    void unknownRoute_shouldBeUnauthorized() {

        webTestClient
                .get()
                .uri("/unknown")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @AfterAll
    static void stopServers() {

        profileBackend.disposeNow();
        feedbackBackend.disposeNow();
    }

    /*
     * Test zamanı real Keycloak-a bağlanmırıq.
     *
     * Bu decoder yalnız integration test üçündür.
     *
     * "user-token"
     *      ↓
     * realm_access.roles = [USER]
     *
     * "admin-token"
     *      ↓
     * realm_access.roles = [USER, ADMIN]
     *
     * Sonra production-da istifadə etdiyimiz
     * JwtRoleConverter bunları ROLE_USER /
     * ROLE_ADMIN authority-lərinə çevirir.
     */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        @Primary
        ReactiveJwtDecoder testJwtDecoder() {

            return token -> {

                if (USER_TOKEN.equals(token)) {

                    return Mono.just(
                            createJwt(
                                    token,
                                    "test-user",
                                    List.of("USER")
                            )
                    );
                }

                if (ADMIN_TOKEN.equals(token)) {

                    return Mono.just(
                            createJwt(
                                    token,
                                    "test-admin",
                                    List.of(
                                            "USER",
                                            "ADMIN"
                                    )
                            )
                    );
                }

                return Mono.error(
                        new BadJwtException(
                                "Invalid test JWT"
                        )
                );
            };
        }

        private Jwt createJwt(
                String token,
                String username,
                List<String> roles
        ) {

            Instant now = Instant.now();

            return Jwt
                    .withTokenValue(token)
                    .header(
                            "alg",
                            "RS256"
                    )
                    .subject(username)
                    .issuedAt(now)
                    .expiresAt(
                            now.plusSeconds(300)
                    )
                    .claim(
                            "preferred_username",
                            username
                    )
                    .claim(
                            "realm_access",
                            Map.of(
                                    "roles",
                                    roles
                            )
                    )
                    .build();
        }
    }

    @Test
    void profilesRoute_withoutToken_shouldReturn401() {
        webTestClient
                .get()
                .uri("/profiles")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void profilesRoute_withUserRole_shouldReturn200() {
        userClient()
                .get()
                .uri("/profiles")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void profilesCreate_withUserRole_shouldReturn403() {
        userClient()
                .post()
                .uri("/profiles")
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .bodyValue(
                        """
                        {
                          "name": "Test User",
                          "email": "test@example.com",
                          "bio": "test"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void profilesCreate_withAdminRole_shouldReturn201() {
        adminClient()
                .post()
                .uri("/profiles")
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .bodyValue(
                        """
                        {
                          "name": "Admin User",
                          "email": "admin@example.com",
                          "bio": "admin"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("profile")
                .jsonPath("$.result")
                .isEqualTo("created");
    }

    @Test
    void feedbackCreate_withUserRole_shouldReturn201() {
        userClient()
                .post()
                .uri("/feedback")
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .bodyValue(
                        """
                        {
                          "name": "User",
                          "email": "user@example.com",
                          "message": "feedback"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("feedback")
                .jsonPath("$.result")
                .isEqualTo("created");
    }

    @Test
    void feedbackList_withUserRole_shouldReturn403() {
        userClient()
                .get()
                .uri("/feedback")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void feedbackList_withAdminRole_shouldReturn200() {
        adminClient()
                .get()
                .uri("/feedback")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("feedback");
    }

    @Test
    void profilesRoute_withInvalidToken_shouldReturn401() {
        webTestClient
                .get()
                .uri("/profiles")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer invalid-token"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}