// Package declaration: places this test class in the consumer service's controller package
package com.supplychain.consumer.controller;

// Import Jackson's ObjectMapper for serializing Java objects to JSON request bodies
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the ErrorLog entity class that represents error records stored in MongoDB
import com.supplychain.consumer.entity.ErrorLog;
// Import the ErrorLogRepository interface for mocking database operations on error logs
import com.supplychain.consumer.repository.ErrorLogRepository;
// Import JUnit 5 @Test annotation to mark methods as test cases
import org.junit.jupiter.api.Test;
// Import @Autowired for Spring dependency injection of MockMvc and ObjectMapper
import org.springframework.beans.factory.annotation.Autowired;
// Import @WebMvcTest to configure a Spring MVC test context with only the specified controller
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Import @MockBean to create a Mockito mock that is registered in the Spring application context
import org.springframework.boot.test.mock.mockito.MockBean;
// Import MediaType for specifying Content-Type headers in HTTP requests
import org.springframework.http.MediaType;
// Import MockMvc for simulating HTTP requests without starting a full server
import org.springframework.test.web.servlet.MockMvc;

// Import LocalDateTime for building ErrorLog entities with timestamp fields
import java.time.LocalDateTime;
// Import List for wrapping repository query results
import java.util.List;
// Import Map for creating JSON request body payloads
import java.util.Map;
// Import Optional for wrapping findById repository results
import java.util.Optional;

// Import Mockito's any() matcher for matching any argument of a given type in mock verification
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's verify() to assert that a specific mock method was called
import static org.mockito.Mockito.verify;
// Import Mockito's when() to stub mock method return values
import static org.mockito.Mockito.when;
// Import MockMvcRequestBuilders static methods (get, post, put, delete) for building HTTP requests
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// Import MockMvcResultMatchers static methods (status, jsonPath, content) for asserting HTTP responses
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads only the web layer for ErrorMonitoringController (no full application context)
// This focuses the test on HTTP request handling, JSON serialization, and controller logic
@WebMvcTest(ErrorMonitoringController.class)
// Declare the test class with package-private visibility
class ErrorMonitoringControllerTest {

    // Inject Spring's MockMvc for simulating HTTP requests to the controller under test
    @Autowired private MockMvc mockMvc;
    // Inject the auto-configured ObjectMapper for serializing request body objects to JSON
    @Autowired private ObjectMapper objectMapper;
    // Create a mock ErrorLogRepository bean in the Spring context to replace the real repository
    @MockBean private ErrorLogRepository errorLogRepository;

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/unresolved returns a list of unresolved error logs
    void getUnresolvedErrors_returnsErrorList() throws Exception {
        // Build an ErrorLog entity using the builder pattern with test data
        ErrorLog error = ErrorLog.builder()
                // Set the MongoDB document ID
                .id("err1")
                // Set the event ID that caused the error
                .eventId("evt_001")
                // Set the event type (item, trade-item, etc.) that failed
                .eventType("item")
                // Set the failure stage indicating where in the pipeline the error occurred
                .failureStage("DB_SAVE")
                // Set the error message describing what went wrong
                .errorMessage("Connection refused")
                // Set resolved to false indicating this error has not been fixed yet
                .resolved(false)
                // Build the ErrorLog object
                .build();
        // Stub: when the repository queries for unresolved errors, return a single-element list
        when(errorLogRepository.findByResolvedFalse()).thenReturn(List.of(error));

        // Act: perform a GET request to the unresolved errors endpoint
        mockMvc.perform(get("/api/errors/unresolved"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: first element in the JSON array has the expected event ID
                .andExpect(jsonPath("$[0].eventId").value("evt_001"))
                // Assert: first element has the expected failure stage
                .andExpect(jsonPath("$[0].failureStage").value("DB_SAVE"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/unresolved returns an empty array when no unresolved errors exist
    void getUnresolvedErrors_empty_returnsEmptyList() throws Exception {
        // Stub: when the repository queries for unresolved errors, return an empty list
        when(errorLogRepository.findByResolvedFalse()).thenReturn(List.of());

        // Act: perform a GET request to the unresolved errors endpoint
        mockMvc.perform(get("/api/errors/unresolved"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: the response body is a JSON array
                .andExpect(jsonPath("$").isArray())
                // Assert: the JSON array is empty (no unresolved errors)
                .andExpect(jsonPath("$").isEmpty());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/stage/{stage} returns errors filtered by failure stage
    void getErrorsByStage_returnsFilteredErrors() throws Exception {
        // Build an ErrorLog entity with OSP_API failure stage
        ErrorLog error = ErrorLog.builder()
                // Set the MongoDB document ID
                .id("err1")
                // Set the failure stage to OSP_API (external API failure)
                .failureStage("OSP_API")
                // Set resolved to false
                .resolved(false)
                // Build the ErrorLog object
                .build();
        // Stub: when querying errors by stage "OSP_API" that are unresolved, return the error
        when(errorLogRepository.findByFailureStageAndResolvedFalse("OSP_API"))
                .thenReturn(List.of(error));

        // Act: perform a GET request with stage path variable "OSP_API"
        mockMvc.perform(get("/api/errors/stage/OSP_API"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: first element has the expected failure stage "OSP_API"
                .andExpect(jsonPath("$[0].failureStage").value("OSP_API"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/type/{type} returns errors filtered by event type
    void getErrorsByEventType_returnsFilteredErrors() throws Exception {
        // Build an ErrorLog entity with "shipment" event type
        ErrorLog error = ErrorLog.builder()
                // Set the MongoDB document ID
                .id("err1")
                // Set the event type to "shipment"
                .eventType("shipment")
                // Set resolved to false
                .resolved(false)
                // Build the ErrorLog object
                .build();
        // Stub: when querying errors by event type "shipment" that are unresolved, return the error
        when(errorLogRepository.findByEventTypeAndResolvedFalse("shipment"))
                .thenReturn(List.of(error));

        // Act: perform a GET request with event type path variable "shipment"
        mockMvc.perform(get("/api/errors/type/shipment"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: first element has the expected event type "shipment"
                .andExpect(jsonPath("$[0].eventType").value("shipment"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: PUT /api/errors/{id}/resolve marks an existing error as resolved
    void resolveError_existingError_marksResolved() throws Exception {
        // Build an unresolved ErrorLog entity that will be found by ID
        ErrorLog error = ErrorLog.builder()
                // Set the MongoDB document ID that will be looked up
                .id("err1")
                // Set resolved to false (currently unresolved)
                .resolved(false)
                // Build the ErrorLog object
                .build();
        // Stub: when finding by ID "err1", return the unresolved error
        when(errorLogRepository.findById("err1")).thenReturn(Optional.of(error));

        // Create the request body with resolution details
        Map<String, String> body = Map.of(
                // Set who resolved this error
                "resolvedBy", "admin",
                // Set any notes about the resolution
                "notes", "Fixed manually"
        );

        // Act: perform a PUT request to resolve error "err1" with JSON body
        mockMvc.perform(put("/api/errors/err1/resolve")
                        // Set the request Content-Type to JSON
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize the body map to JSON and set as request content
                        .content(objectMapper.writeValueAsString(body)))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: response body contains the success message
                .andExpect(content().string("Error marked as resolved"));

        // Assert: verify that the error log was saved back to the repository (with resolved=true)
        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: PUT /api/errors/{id}/resolve throws an exception when the error ID does not exist
    void resolveError_notFound_throwsException() throws Exception {
        // Stub: when finding by ID "nonexistent", return empty (error not found)
        when(errorLogRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Assert: attempting to resolve a nonexistent error throws an Exception
        // The controller throws when Optional.empty() is encountered
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            // Act: perform a PUT request with a nonexistent error ID
            mockMvc.perform(put("/api/errors/nonexistent/resolve")
                    // Set the request Content-Type to JSON
                    .contentType(MediaType.APPLICATION_JSON)
                    // Provide a JSON body with resolve details
                    .content("{\"resolvedBy\":\"admin\",\"notes\":\"test\"}"))
        );
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/stats returns total, unresolved, and resolved error counts
    void getErrorStats_returnsStatistics() throws Exception {
        // Stub: total error count is 10 (includes both resolved and unresolved)
        when(errorLogRepository.count()).thenReturn(10L);
        // Stub: 3 unresolved errors exist (3 new ErrorLog objects in a list)
        when(errorLogRepository.findByResolvedFalse()).thenReturn(
                List.of(new ErrorLog(), new ErrorLog(), new ErrorLog()));

        // Act: perform a GET request to the stats endpoint
        mockMvc.perform(get("/api/errors/stats"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: totalErrors is 10 (from count())
                .andExpect(jsonPath("$.totalErrors").value(10))
                // Assert: unresolvedErrors is 3 (from findByResolvedFalse().size())
                .andExpect(jsonPath("$.unresolvedErrors").value(3))
                // Assert: resolvedErrors is 7 (calculated as total - unresolved = 10 - 3)
                .andExpect(jsonPath("$.resolvedErrors").value(7));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/errors/stats returns all zeros when no errors exist in the system
    void getErrorStats_noErrors_returnsZeros() throws Exception {
        // Stub: total error count is 0 (no errors have ever been recorded)
        when(errorLogRepository.count()).thenReturn(0L);
        // Stub: no unresolved errors exist
        when(errorLogRepository.findByResolvedFalse()).thenReturn(List.of());

        // Act: perform a GET request to the stats endpoint
        mockMvc.perform(get("/api/errors/stats"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: totalErrors is 0
                .andExpect(jsonPath("$.totalErrors").value(0))
                // Assert: unresolvedErrors is 0
                .andExpect(jsonPath("$.unresolvedErrors").value(0))
                // Assert: resolvedErrors is 0 (0 - 0 = 0)
                .andExpect(jsonPath("$.resolvedErrors").value(0));
    }
}
