// Package declaration: places this class in the supplier supply controller layer of the supply chain application
package com.supplychain.suppliersupply.controller;

// Import the SupplierSupplyProcessingService that contains the core business logic for supplier supply event processing
import com.supplychain.suppliersupply.service.SupplierSupplyProcessingService;
// Import the SLF4J Logger interface for creating structured log statements in the controller
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory used to instantiate Logger objects bound to a specific class
import org.slf4j.LoggerFactory;
// Import the Spring @Autowired annotation for automatic dependency injection of Spring-managed beans
import org.springframework.beans.factory.annotation.Autowired;
// Import Spring's ResponseEntity class for building HTTP responses with status codes, headers, and body content
import org.springframework.http.ResponseEntity;
// Import Spring Web annotations: @RestController, @RequestMapping, @CrossOrigin, @PostMapping, @GetMapping, @RequestBody
import org.springframework.web.bind.annotation.*;

// Import the Java Map interface for accepting flexible JSON payloads as key-value pairs in request bodies
import java.util.Map;

/**
 * Supplier Supply Controller
 *
 * REST API for receiving supplier supply events from loader service
 */
// Marks this class as a REST controller, combining @Controller and @ResponseBody so all methods return serialized response data
@RestController
// Maps all endpoints in this controller under the "/api" base path (e.g., /api/process, /api/health)
@RequestMapping("/api")
// Enables Cross-Origin Resource Sharing for all origins, allowing any frontend or external service to call these endpoints
@CrossOrigin(origins = "*")
// Declares the SupplierSupplyController class that exposes REST endpoints for supplier supply event processing
public class SupplierSupplyController {

    // Creates a static final Logger instance for this controller class, used to log request lifecycle events
    private static final Logger logger = LoggerFactory.getLogger(SupplierSupplyController.class);

    // Injects the SupplierSupplyProcessingService bean automatically by Spring's dependency injection container
    // The @Autowired annotation tells Spring to resolve and wire the matching service bean at application startup
    @Autowired
    // Declares the processingService field holding the reference to the supplier supply processing business logic
    private SupplierSupplyProcessingService processingService;

    /**
     * Process supplier supply event
     *
     * Called by loader service with supplier supply event data
     *
     * Example request:
     * {
     *   "eventId": "evt_supplier-supply_def456",
     *   "eventType": "supplier-supply",
     *   "skuId": "SKU001",
     *   "warehouseId": "WH001",
     *   "warehouseName": "Main Warehouse",
     *   "availableQuantity": 100,
     *   "reservedQuantity": 20,
     *   "onOrderQuantity": 50,
     *   "reorderPoint": 30,
     *   "reorderQuantity": 100
     * }
     */
    // Maps HTTP POST requests to the "/api/process" URL path to this method for supplier supply event ingestion
    @PostMapping("/process")
    // Defines the endpoint method that receives a supplier supply event payload and returns an HTTP response
    // @RequestBody deserializes the incoming JSON request body into a Map<String, Object> parameter
    public ResponseEntity<String> processSupplierSupply(@RequestBody Map<String, Object> supplierSupplyEvent) {
        // Wraps the entire processing logic in a try-catch block to gracefully handle any exceptions
        try {
            // Logs an informational message indicating that a new supplier supply event has been received
            logger.info("Received supplier supply event for processing");

            // Delegates the actual supplier supply processing to the service layer for validation and business logic execution
            processingService.processSupplierSupply(supplierSupplyEvent);

            // Logs a success message after the processing service completes without throwing an exception
            logger.info("Supplier supply processed successfully");
            // Returns an HTTP 200 OK response with a success message string as the response body
            return ResponseEntity.ok("Supplier supply processed successfully");

        // Catches any exception thrown during supplier supply processing (validation errors, runtime errors, etc.)
        } catch (Exception e) {
            // Logs the error at ERROR level with the exception message and full stack trace for debugging
            logger.error("Failed to process supplier supply: {}", e.getMessage(), e);
            // Returns an HTTP 500 Internal Server Error response with a descriptive error message as the body
            return ResponseEntity.internalServerError()
                .body("Failed to process supplier supply: " + e.getMessage());
        }
    }

    /**
     * Health check
     */
    // Maps HTTP GET requests to the "/api/health" URL path to this health check method
    @GetMapping("/health")
    // Defines the health check endpoint that returns service status information as a JSON map
    public ResponseEntity<Map<String, String>> health() {
        // Returns an HTTP 200 OK response with an immutable map containing service status and service identity
        // Map.of() creates an unmodifiable map with "status" set to "UP" and "service" set to "supplier-supply-service"
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "supplier-supply-service"
        ));
    }
}
