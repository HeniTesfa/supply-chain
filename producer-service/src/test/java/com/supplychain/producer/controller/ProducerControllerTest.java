// Package declaration: places this test class in the producer service's controller package
package com.supplychain.producer.controller;

// Import Jackson's ObjectMapper for serializing Java objects to JSON request bodies
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the KafkaProducerService that the controller delegates to for event publishing
import com.supplychain.producer.service.KafkaProducerService;
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

// Import HashMap for creating mutable test data maps
import java.util.HashMap;
// Import Map interface for request body and response payloads
import java.util.Map;

// Import Mockito argument matchers: any(), eq(), anyString(), isNull() for flexible stubbing
import static org.mockito.ArgumentMatchers.*;
// Import Mockito's verify() to assert that a specific mock method was called
import static org.mockito.Mockito.verify;
// Import Mockito's when() to stub mock method return values
import static org.mockito.Mockito.when;
// Import MockMvcRequestBuilders static methods (get, post, put, delete) for building HTTP requests
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// Import MockMvcResultMatchers static methods (status, jsonPath, content) for asserting HTTP responses
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads only the web layer for ProducerController (controller + JSON serialization only)
@WebMvcTest(ProducerController.class)
// Declare the test class with package-private visibility
class ProducerControllerTest {

    // Inject MockMvc for performing simulated HTTP requests against the controller
    @Autowired private MockMvc mockMvc;
    // Inject the auto-configured ObjectMapper for serializing request bodies to JSON
    @Autowired private ObjectMapper objectMapper;
    // Create a mock KafkaProducerService bean to replace the real service in the Spring context
    @MockBean private KafkaProducerService producerService;

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/item with valid data returns 200 OK with success=true
    void createItemEvent_success_returnsOk() throws Exception {
        // Stub: when publishEvent is called for "item" type with any data and no idempotency key, return success
        when(producerService.publishEvent(eq("item"), any(), isNull()))
                .thenReturn(successResponse());

        // Act: perform a POST request to the item event creation endpoint
        mockMvc.perform(post("/api/producer/item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize the item data map to JSON and set as request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: response JSON contains success=true
                .andExpect(jsonPath("$.success").value(true));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/item with Idempotency-Key header passes the key to the service
    void createItemEvent_withIdempotencyKey_passesHeaderToService() throws Exception {
        // Stub: when publishEvent is called for "item" with idempotency key "my-key", return success
        when(producerService.publishEvent(eq("item"), any(), eq("my-key")))
                .thenReturn(successResponse());

        // Act: perform a POST request with the Idempotency-Key header set to "my-key"
        mockMvc.perform(post("/api/producer/item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Add the Idempotency-Key HTTP header for producer-side deduplication
                        .header("Idempotency-Key", "my-key")
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk());

        // Assert: verify that publishEvent was called with the exact idempotency key "my-key"
        verify(producerService).publishEvent(eq("item"), any(), eq("my-key"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/trade-item routes to the trade-item event type
    void createTradeItemEvent_success_returnsOk() throws Exception {
        // Stub: when publishEvent is called for "trade-item" type with no idempotency key, return success
        when(producerService.publishEvent(eq("trade-item"), any(), isNull()))
                .thenReturn(successResponse());

        // Act: perform a POST request to the trade-item event creation endpoint
        mockMvc.perform(post("/api/producer/trade-item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk());

        // Assert: verify that publishEvent was called with event type "trade-item" and null idempotency key
        verify(producerService).publishEvent(eq("trade-item"), any(), isNull());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/supplier-supply routes to the supplier-supply event type
    void createSupplierSupplyEvent_success_returnsOk() throws Exception {
        // Stub: when publishEvent is called for "supplier-supply" type, return success
        when(producerService.publishEvent(eq("supplier-supply"), any(), isNull()))
                .thenReturn(successResponse());

        // Act: perform a POST request to the supplier-supply event creation endpoint
        mockMvc.perform(post("/api/producer/supplier-supply")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk());

        // Assert: verify that publishEvent was called with event type "supplier-supply"
        verify(producerService).publishEvent(eq("supplier-supply"), any(), isNull());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/shipment routes to the shipment event type
    void createShipmentEvent_success_returnsOk() throws Exception {
        // Stub: when publishEvent is called for "shipment" type, return success
        when(producerService.publishEvent(eq("shipment"), any(), isNull()))
                .thenReturn(successResponse());

        // Act: perform a POST request to the shipment event creation endpoint
        mockMvc.perform(post("/api/producer/shipment")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk());

        // Assert: verify that publishEvent was called with event type "shipment"
        verify(producerService).publishEvent(eq("shipment"), any(), isNull());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/item returns 500 when the service throws a RuntimeException
    void createItemEvent_serviceFails_returns500() throws Exception {
        // Stub: when publishEvent is called with any arguments, throw a RuntimeException
        when(producerService.publishEvent(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Kafka down"));

        // Act: perform a POST request to the item endpoint (service will throw)
        mockMvc.perform(post("/api/producer/item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 500 Internal Server Error
                .andExpect(status().isInternalServerError())
                // Assert: the error message from the exception is included in the response JSON
                .andExpect(jsonPath("$.error").value("Kafka down"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: GET /api/producer/health returns the health check status for the producer service
    void health_returnsUpStatus() throws Exception {
        // Act: perform a GET request to the health endpoint
        mockMvc.perform(get("/api/producer/health"))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk())
                // Assert: the status field is "UP" indicating the service is healthy
                .andExpect(jsonPath("$.status").value("UP"))
                // Assert: the service name identifies this as the producer-service
                .andExpect(jsonPath("$.service").value("producer-service"));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: POST /api/producer/item without an Idempotency-Key header passes null to the service
    void createItemEvent_noIdempotencyHeader_sendsNull() throws Exception {
        // Stub: when publishEvent is called for "item" type with null idempotency key, return success
        when(producerService.publishEvent(eq("item"), any(), isNull()))
                .thenReturn(successResponse());

        // Act: perform a POST request WITHOUT the Idempotency-Key header
        mockMvc.perform(post("/api/producer/item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK
                .andExpect(status().isOk());

        // Assert: verify that publishEvent was called with null as the idempotency key parameter
        verify(producerService).publishEvent(eq("item"), any(), isNull());
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the service detects a duplicate, the response includes duplicate=true
    void createItemEvent_duplicateResponse_returnsOk() throws Exception {
        // Create a response map that indicates a duplicate was detected
        Map<String, Object> dupResponse = new HashMap<>();
        // Set success to true (duplicate detection is not a failure)
        dupResponse.put("success", true);
        // Set duplicate flag to true indicating this was a previously published event
        dupResponse.put("duplicate", true);
        // Set the original event ID from the first publication
        dupResponse.put("eventId", "evt_item_old");
        // Stub: when publishEvent is called, return the duplicate response
        when(producerService.publishEvent(anyString(), any(), any()))
                .thenReturn(dupResponse);

        // Act: perform a POST request to the item endpoint
        mockMvc.perform(post("/api/producer/item")
                        // Set the Content-Type header to application/json
                        .contentType(MediaType.APPLICATION_JSON)
                        // Serialize and set the request body
                        .content(objectMapper.writeValueAsString(itemData())))
                // Assert: HTTP status is 200 OK (duplicates are not errors)
                .andExpect(status().isOk())
                // Assert: the response JSON indicates this was a duplicate submission
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    // --- Helpers ---

    // Helper method to create a simple item event data map for use in test requests
    private Map<String, Object> itemData() {
        // Create a mutable HashMap for the item data
        Map<String, Object> data = new HashMap<>();
        // Set the SKU ID field
        data.put("skuId", "SKU001");
        // Set the item name field
        data.put("itemName", "Laptop");
        // Set the price field
        data.put("price", 999.99);
        // Return the populated item data map
        return data;
    }

    // Helper method to create a successful publish response map mimicking KafkaProducerService output
    private Map<String, Object> successResponse() {
        // Create a mutable HashMap for the success response
        Map<String, Object> resp = new HashMap<>();
        // Set success flag to true
        resp.put("success", true);
        // Set the generated event ID
        resp.put("eventId", "evt_item_abc");
        // Set the Kafka topic the event was published to
        resp.put("topic", "supply-chain.item.events");
        // Set the Kafka partition number where the message was stored
        resp.put("partition", 0);
        // Set the Kafka offset within the partition
        resp.put("offset", 1L);
        // Set duplicate flag to false (this is a new event, not a duplicate)
        resp.put("duplicate", false);
        // Return the populated success response map
        return resp;
    }
}
