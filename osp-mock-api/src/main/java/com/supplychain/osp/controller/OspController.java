// Package declaration: places this class in the OSP mock API controller layer of the supply chain application
package com.supplychain.osp.controller;

// Import the OspMockService that contains the in-memory storage and business logic for the mock OSP API
import com.supplychain.osp.service.OspMockService;
// Import the SLF4J Logger interface for creating structured log statements in the controller
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory used to instantiate Logger objects bound to a specific class
import org.slf4j.LoggerFactory;
// Import the Spring @Autowired annotation for automatic dependency injection of Spring-managed beans
import org.springframework.beans.factory.annotation.Autowired;
// Import Spring's ResponseEntity class for building HTTP responses with status codes, headers, and body content
import org.springframework.http.ResponseEntity;
// Import Spring Web annotations: @RestController, @RequestMapping, @CrossOrigin, @GetMapping, @PostMapping, @PathVariable, @RequestBody
import org.springframework.web.bind.annotation.*;

// Import the Java Map interface for accepting and returning flexible JSON payloads as key-value pairs
import java.util.Map;

/**
 * OSP Mock API Controller
 *
 * Simulates external OSP (Order Service Provider) API
 * Used for testing item-service integration
 */
// Marks this class as a REST controller, combining @Controller and @ResponseBody so all methods return serialized response data
@RestController
// Maps all endpoints in this controller under the "/osp/api" base path, simulating the external OSP API URL structure
@RequestMapping("/osp/api")
// Enables Cross-Origin Resource Sharing for all origins, allowing any frontend or service to call these mock endpoints
@CrossOrigin(origins = "*")
// Declares the OspController class that exposes REST endpoints simulating an external OSP (Order Service Provider) API
public class OspController {

    // Creates a static final Logger instance for this controller class, used to log all incoming API requests
    private static final Logger logger = LoggerFactory.getLogger(OspController.class);

    // Injects the OspMockService bean automatically by Spring's dependency injection container
    // The @Autowired annotation tells Spring to resolve and wire the matching service bean at application startup
    @Autowired
    // Declares the ospMockService field holding the reference to the mock OSP service with in-memory item storage
    private OspMockService ospMockService;

    /**
     * GET item endpoint - simulates OSP API's item retrieval
     *
     * Example: GET /osp/api/items/SKU001
     */
    // Maps HTTP GET requests to the "/osp/api/items/{skuId}" URL path, where {skuId} is a path variable
    @GetMapping("/items/{skuId}")
    // Defines the endpoint method that retrieves an item by its SKU ID from the mock in-memory store
    // @PathVariable extracts the skuId from the URL path and binds it to the method parameter
    // Returns ResponseEntity<?> with wildcard type since the response body varies between success (item map) and error (empty)
    public ResponseEntity<?> getItem(@PathVariable String skuId) {
        // Wraps the retrieval logic in a try-catch block to handle cases where the item is not found
        try {
            // Logs the incoming GET request with the requested SKU ID for operational tracking
            logger.info("GET /osp/api/items/{} - Retrieving item", skuId);

            // Calls the mock service to retrieve item data by SKU ID; throws RuntimeException if not found
            Map<String, Object> itemData = ospMockService.getItemData(skuId);

            // Returns an HTTP 200 OK response with the item data map serialized as JSON in the response body
            return ResponseEntity.ok(itemData);

        // Catches any exception thrown when the item is not found in the mock storage
        } catch (Exception e) {
            // Logs an error message indicating that the requested item was not found in the store
            logger.error("Item not found: {}", skuId);
            // Returns an HTTP 404 Not Found response with an empty body, simulating a standard REST "not found" response
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST item endpoint - simulates OSP API's item creation/update
     *
     * Example: POST /osp/api/items
     * Body: {
     *   "skuId": "SKU001",
     *   "itemName": "Laptop",
     *   "category": "Electronics",
     *   "price": 999.99
     * }
     */
    // Maps HTTP POST requests to the "/osp/api/items" URL path for creating or updating items
    @PostMapping("/items")
    // Defines the endpoint method that creates or updates an item in the mock in-memory store
    // @RequestBody deserializes the incoming JSON request body into a Map<String, Object> parameter
    // Returns ResponseEntity<?> with wildcard type since the response varies between success and error formats
    public ResponseEntity<?> createOrUpdateItem(@RequestBody Map<String, Object> itemData) {
        // Wraps the create/update logic in a try-catch block to handle validation errors
        try {
            // Logs the incoming POST request indicating an item create or update operation
            logger.info("POST /osp/api/items - Creating/updating item");

            // Calls the mock service to create or update the item in the in-memory store and gets the response
            Map<String, Object> response = ospMockService.createOrUpdateItem(itemData);

            // Returns an HTTP 200 OK response with the success response map serialized as JSON
            return ResponseEntity.ok(response);

        // Catches any exception thrown during item creation or update (e.g., missing SKU ID)
        } catch (Exception e) {
            // Logs an error message with the exception details for debugging
            logger.error("Failed to create/update item: {}", e.getMessage());
            // Returns an HTTP 400 Bad Request response with a JSON body containing the error message
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    // Maps HTTP GET requests to the "/osp/api/health" URL path to this health check method
    @GetMapping("/health")
    // Defines the health check endpoint that returns service status, identity, and current item count
    public ResponseEntity<Map<String, String>> health() {
        // Returns an HTTP 200 OK response with an immutable map containing service status, name, and item count
        // The item count is retrieved from the mock service and converted to a String for the response map
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "osp-mock-api",
            "itemCount", String.valueOf(ospMockService.getItemCount())
        ));
    }

    /**
     * Test endpoint to simulate slow response (for testing retry logic)
     */
    // Maps HTTP GET requests to "/osp/api/items/{skuId}/slow" for simulating a slow external API response
    @GetMapping("/items/{skuId}/slow")
    // Defines the endpoint method that simulates a slow API response by introducing a 5-second delay
    // @PathVariable extracts the skuId from the URL path; the method declares InterruptedException for Thread.sleep()
    public ResponseEntity<?> getItemSlow(@PathVariable String skuId) throws InterruptedException {
        // Logs the incoming request indicating that a slow response simulation has been triggered
        logger.info("GET /osp/api/items/{}/slow - Simulating slow response", skuId);

        // Simulate slow API (5 seconds)
        // Pauses the current thread for 5000 milliseconds (5 seconds) to simulate network latency or processing delay
        // This is used by the item-service to test its retry logic with exponential backoff
        Thread.sleep(5000);

        // After the delay, delegates to the normal getItem() method to return the actual item data
        // This simulates a slow but eventually successful response from the external OSP API
        return getItem(skuId);
    }

    /**
     * Test endpoint to simulate error (for testing error handling)
     */
    // Maps HTTP GET requests to "/osp/api/items/{skuId}/error" for simulating an external API error
    @GetMapping("/items/{skuId}/error")
    // Defines the endpoint method that always returns a 500 error response, simulating an OSP API failure
    // @PathVariable extracts the skuId from the URL path for logging purposes
    public ResponseEntity<?> getItemError(@PathVariable String skuId) {
        // Logs the incoming request indicating that an error simulation has been triggered
        logger.info("GET /osp/api/items/{}/error - Simulating API error", skuId);

        // Simulate API error
        // Returns an HTTP 500 Internal Server Error response with a JSON body containing error details
        // This is used by the item-service to test its error handling and retry mechanisms
        return ResponseEntity.internalServerError()
            .body(Map.of(
                "error", "Simulated OSP API error",
                "message", "This is a test error endpoint"
            ));
    }
}
