package com.adil.profileservice.controller;

import com.adil.profileservice.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProfileControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProfileRepository profileRepository;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        restTemplate = new RestTemplate();
    }

    @Test
    void createProfile_shouldReturn201() {
        Map<String, Object> body = Map.of(
                "name", "Adil Mammadov",
                "email", "ADIL@EXAMPLE.COM",
                "bio", "Java Backend Developer"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/profiles"),
                body,
                Map.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "adil@example.com",
                response.getBody().get("email")
        );
    }

    @Test
    void getProfiles_shouldReturnPaginatedResponse() {
        createProfile("User One", "one@example.com");
        createProfile("User Two", "two@example.com");

        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/profiles?page=0&size=5"),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals(
                2,
                ((Number) response.getBody()
                        .get("totalElements"))
                        .intValue()
        );
    }

    @Test
    void getProfileById_shouldReturnProfile() {
        Map created = createProfile(
                "Adil",
                "adil@example.com"
        );

        Long id =
                ((Number) created.get("id"))
                        .longValue();

        ResponseEntity<Map> response =
                restTemplate.getForEntity(
                        url("/profiles/" + id),
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "adil@example.com",
                response.getBody().get("email")
        );
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() {
        Map created =
                createProfile(
                        "Old Name",
                        "old@example.com"
                );

        Long id =
                ((Number) created.get("id"))
                        .longValue();

        Map<String, Object> updateBody =
                Map.of(
                        "name", "New Name",
                        "email", "NEW@EXAMPLE.COM",
                        "bio", "New Bio"
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        updateBody,
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url("/profiles/" + id),
                        HttpMethod.PUT,
                        entity,
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "New Name",
                response.getBody().get("name")
        );

        assertEquals(
                "new@example.com",
                response.getBody().get("email")
        );
    }

    @Test
    void deleteProfile_shouldReturn204() {
        Map created =
                createProfile(
                        "Delete Me",
                        "delete@example.com"
                );

        Long id =
                ((Number) created.get("id"))
                        .longValue();

        ResponseEntity<Void> response =
                restTemplate.exchange(
                        url("/profiles/" + id),
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        assertFalse(
                profileRepository.existsById(id)
        );
    }

    @Test
    void createProfile_shouldReturn409ForDuplicateEmail() {
        createProfile(
                "First",
                "same@example.com"
        );

        Map<String, Object> body = Map.of(
                "name", "Second",
                "email", "same@example.com",
                "bio", "Another"
        );

        try {
            restTemplate.postForEntity(
                    url("/profiles"),
                    body,
                    Map.class
            );

            fail("Expected 409 Conflict");

        } catch (HttpClientErrorException e) {

            assertEquals(
                    HttpStatus.CONFLICT,
                    e.getStatusCode()
            );

            Map<String, Object> errorBody =
                    e.getResponseBodyAs(Map.class);

            assertNotNull(errorBody);
            assertNotNull(
                    errorBody.get("timestamp")
            );

            assertEquals(
                    409,
                    ((Number) errorBody.get("status"))
                            .intValue()
            );

            assertEquals(
                    "Conflict",
                    errorBody.get("error")
            );

            assertEquals(
                    "Profile with email 'same@example.com' already exists",
                    errorBody.get("message")
            );

            assertEquals(
                    "/profiles",
                    errorBody.get("path")
            );
        }
    }

    @Test
    void createProfile_shouldReturn400ForInvalidInput() {
        Map<String, Object> body = Map.of(
                "name", "",
                "email", "invalid-email",
                "bio", ""
        );

        try {
            restTemplate.postForEntity(
                    url("/profiles"),
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
                    "/profiles",
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
        }
    }

    @Test
    void getProfileById_shouldReturn404WithStandardErrorContract() {
        try {
            restTemplate.getForEntity(
                    url("/profiles/999999"),
                    Map.class
            );

            fail("Expected 404 Not Found");

        } catch (HttpClientErrorException e) {

            assertEquals(
                    HttpStatus.NOT_FOUND,
                    e.getStatusCode()
            );

            Map<String, Object> errorBody =
                    e.getResponseBodyAs(Map.class);

            assertNotNull(errorBody);
            assertNotNull(
                    errorBody.get("timestamp")
            );

            assertEquals(
                    404,
                    ((Number) errorBody.get("status"))
                            .intValue()
            );

            assertEquals(
                    "Not Found",
                    errorBody.get("error")
            );

            assertEquals(
                    "Profile not found with id: 999999",
                    errorBody.get("message")
            );

            assertEquals(
                    "/profiles/999999",
                    errorBody.get("path")
            );
        }
    }

    private Map createProfile(
            String name,
            String email
    ) {
        Map<String, Object> body = Map.of(
                "name", name,
                "email", email,
                "bio", "Test Bio"
        );

        return restTemplate.postForObject(
                url("/profiles"),
                body,
                Map.class
        );
    }

    private String url(String path) {
        return "http://localhost:"
                + port
                + path;
    }

    @Test
    void updateProfile_shouldReturn409WhenEmailBelongsToAnotherProfile() {
        Map first = createProfile(
                "First User",
                "first@example.com"
        );

        createProfile(
                "Second User",
                "second@example.com"
        );

        Long firstId =
                ((Number) first.get("id"))
                        .longValue();

        Map<String, Object> updateBody = Map.of(
                "name", "First User",
                "email", "second@example.com",
                "bio", "Updated Bio"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(
                        updateBody,
                        headers
                );

        try {
            restTemplate.exchange(
                    url("/profiles/" + firstId),
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            fail("Expected 409 Conflict");

        } catch (HttpClientErrorException e) {
            assertEquals(
                    HttpStatus.CONFLICT,
                    e.getStatusCode()
            );
        }
    }

    @Test
    void deleteProfile_shouldMakeProfileUnavailable() {
        Map created = createProfile(
                "Delete Me",
                "delete2@example.com"
        );

        Long id =
                ((Number) created.get("id"))
                        .longValue();

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        url("/profiles/" + id),
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                deleteResponse.getStatusCode()
        );

        assertThrows(
                HttpClientErrorException.NotFound.class,
                () -> restTemplate.getForEntity(
                        url("/profiles/" + id),
                        Map.class
                )
        );
    }
}