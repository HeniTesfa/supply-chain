// Package declaration: places this test class in the loader service's controller package
package com.supplychain.loader.controller;

// Import Jackson's ObjectMapper for serializing Java objects to JSON request bodies
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the LoaderService that the controller delegates to for event routing
import com.supplychain.loader.service.LoaderService;
// Import JUnit 5 @Test annotation to mark methods as test cases
import org.junit.jupiter.api.Test;
// Import @Autowired for Spring dependency injection of MockMvc and ObjectMapper
import org.springframework.beans.factory.annotation.Autowired;
// Import @WebMvcTest to configure a Spring MVC test context with only the specified controller
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Import @MockBean to create a Mockito mock registered in the Spring application context
import org.springframework.boot.test.mock.mockito.MockBean;
// Import MediaType for specifying Content-Type headers in HTTP requests
import org.springframework.http.MediaType;
// Import MockMvc for simulating HTTP requests without starting a full server
import org.springframework.test.web.servlet.MockMvc;

// Import HashMap for creating mutable test event data maps
import java.util.HashMap;
// Import Map interface for event payloads
import java.util.Map;

// Import Mockito's any() matcher for matching any argument type
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's when() to stub mock method return values
import static org.mockito.Mockito.when;
// Import MockMvcRequestBuilders static methods (get, post, put, delete) for building HTTP requests
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// Import MockMvcResultMatchers static methods (status, jsonPath, content) for asserting HTTP responses
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads only the web layer for LoaderController (controller + serialization, no full context)
@WebMvcTest(LoaderController.class)
// Declare the test class with package-private visibility
class LoaderControllerTest {

    // Inject MockMvc for performing simulated HTTP requests against the controller
    @Autowired private MockMvc mockMvc;
    // Inject the auto-configured ObjectMapper for serializing request bodies to JSON
    @Autowired private ObjectMapper objectMapper;
    // Create a mock LoaderService bean to replace the real service in the Spring context
    @MockBean private LoaderService loaderService;

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/loader/route with a valid event returns 200 OK with success=true
    void routeEvent_success_returnsOk() throws Exception {
        // Stub: when routeEvent is called with any event data, return a success message string
        when(loaderService.routeEvent(any())).thenReturn("Event routed to item service");

        // Create a test event payload with an explicit event type and SKU ID
        Map<String, Object> event = new HashMap<>();
        // Set the event type for explicit routing (not inferred)
        event.put("eventType", "item");
        // Set the SKU ID field
        event.put("skuId", "SKU001");

        // Act: perform a POST request to the route endpoint with JSON body
        mockMvc.perform(post("/api/loader/route")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize the event map to JSON and set as request content
                        .content(objectMapper.writeValueAsString(event)))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: response JSON contains success=true
                .andExpect(jsonPath("$.success").value(true))
                // Assert: response JSON contains a "message" field (the routing result message)
                .andExpect(jsonPath("$.message").exists());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/loader/route returns 500 when the service throws a RuntimeException
    void routeEvent_serviceFails_returns500() throws Exception {
        // Stub: when routeEvent is called, throw a RuntimeException to simulate an unknown event type
        when(loaderService.routeEvent(any()))
                .thenThrow(new RuntimeException("Unknown event type: invalid"));

        // Create a test event payload with an invalid event type
        Map<String, Object> event = new HashMap<>();
        // Set an invalid event type that the service does not recognize
        event.put("eventType", "invalid");

        // Act: perform a POST request to the route endpoint with the invalid event
        mockMvc.perform(post("/api/loader/route")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize the event map to JSON
                        .content(objectMapper.writeValueAsString(event)))
                // Assert: HTTP status is 500 Internal Server Error
                .andExpect(status().isInternalServerError())
                // Assert: response JSON contains success=false
                .andExpect(jsonPath("$.success").value(false))
                // Assert: response JSON contains an "error" field with the exception message
                .andExpect(jsonPath("$.error").exists());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/loader/health returns the health check status for the loader service
    void health_returnsUpStatus() throws Exception {
        // Act: perform a GET request to the health endpoint
        mockMvc.perform(get("/api/loader/health"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: the status field is "UP" indicating the service is healthy
                .andExpect(jsonPath("$.status").value("UP"))
                // Assert: the service name identifies this as the loader-service
                .andExpect(jsonPath("$.service").value("loader-service"));
    }
}
