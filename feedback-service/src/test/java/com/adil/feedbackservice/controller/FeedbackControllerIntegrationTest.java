package com.adil.feedbackservice.controller;

import com.adil.feedbackservice.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class FeedbackControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        restTemplate = new RestTemplate();
    }

    @Test
    void createFeedback_shouldReturn201() {
        Map<String, Object> body = Map.of(
                "name", "Test User",
                "email", "TEST@EXAMPLE.COM",
                "message", "Great service"
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        url("/feedback"),
                        body,
                        Map.class
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "test@example.com",
                response.getBody().get("email")
        );
    }

    @Test
    void getFeedback_shouldReturnPaginatedResponse() {
        createFeedback(
                "User One",
                "one@example.com"
        );

        createFeedback(
                "User Two",
                "two@example.com"
        );

        ResponseEntity<Map> response =
                restTemplate.getForEntity(
                        url("/feedback?page=0&size=5"),
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                2,
                ((Number) response
                        .getBody()
                        .get("totalElements"))
                        .intValue()
        );
    }

    @Test
    void createFeedback_shouldReturn400ForInvalidInput() {
        Map<String, Object> body = Map.of(
                "name", "",
                "email", "invalid-email",
                "message", ""
        );

        try {
            restTemplate.postForEntity(
                    url("/feedback"),
                    body,
                    Map.class
            );

            fail("Expected 400 Bad Request");

        } catch (HttpClientErrorException e) {

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    e.getStatusCode()
            );

            Map<String, Object> errorBody =
                    e.getResponseBodyAs(Map.class);

            assertNotNull(errorBody);
            assertNotNull(
                    errorBody.get("timestamp")
            );

            assertEquals(
                    400,
                    ((Number) errorBody.get("status"))
                            .intValue()
            );

            assertEquals(
                    "Bad Request",
                    errorBody.get("error")
            );

            assertEquals(
                    "Request validation failed",
                    errorBody.get("message")
            );

            assertEquals(
                    "/feedback",
                    errorBody.get("path")
            );

            Map<String, String> validationErrors =
                    (Map<String, String>)
                            errorBody.get(
                                    "validationErrors"
                            );

            assertNotNull(validationErrors);

            assertTrue(
                    validationErrors.containsKey("name")
            );

            assertTrue(
                    validationErrors.containsKey("email")
            );

            assertTrue(
                    validationErrors.containsKey("message")
            );
        }
    }

    private Map createFeedback(
            String name,
            String email
    ) {
        Map<String, Object> body = Map.of(
                "name", name,
                "email", email,
                "message", "Test message"
        );

        return restTemplate.postForObject(
                url("/feedback"),
                body,
                Map.class
        );
    }

    private String url(String path) {
        return "http://localhost:"
                + port
                + path;
    }
}