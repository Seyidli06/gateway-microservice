package com.adil.apigateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ApiGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private static final DisposableServer profileBackend =
            HttpServer.create()
                    .port(0)
                    .route(routes -> {
                        routes.get(
                                "/profiles",
                                (request, response) ->
                                        response
                                                .header(
                                                        "Content-Type",
                                                        "application/json"
                                                )
                                                .sendString(
                                                        reactor.core.publisher.Mono.just(
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
                                                        "Content-Type",
                                                        "application/json"
                                                )
                                                .sendString(
                                                        reactor.core.publisher.Mono.just(
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
                    })
                    .bindNow();

    private static final DisposableServer feedbackBackend =
            HttpServer.create()
                    .port(0)
                    .route(routes ->
                            routes.get(
                                    "/feedback",
                                    (request, response) ->
                                            response
                                                    .header(
                                                            "Content-Type",
                                                            "application/json"
                                                    )
                                                    .sendString(
                                                            reactor.core.publisher.Mono.just(
                                                                    """
                                                                    {
                                                                      "service": "feedback",
                                                                      "result": "ok"
                                                                    }
                                                                    """
                                                            )
                                                    )
                            )
                    )
                    .bindNow();

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


    @Test
    void contextLoads() {
        assertNotNull(webTestClient);
    }

    @Test
    void profilesRoute_shouldForwardToProfileService() {
        webTestClient
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

    @Test
    void feedbackRoute_shouldForwardToFeedbackService() {
        webTestClient
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

    @AfterAll
    static void stopServers() {
        profileBackend.disposeNow();
        feedbackBackend.disposeNow();
    }

    @Test
    void profilesRoute_shouldPreserveQueryParameters() {
        webTestClient
                .get()
                .uri(
                        "/profiles/query-test?page=2&size=15&sortBy=name&direction=asc"
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.uri")
                .isEqualTo(
                        "/profiles/query-test?page=2&size=15&sortBy=name&direction=asc"
                );
    }

}