// Package declaration: places this class in the trade item controller layer of the supply chain application
package com.supplychain.tradeitem.controller;

// Import the TradeItemProcessingService that contains the core business logic for trade item event processing
import com.supplychain.tradeitem.service.TradeItemProcessingService;
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
 * Trade Item Controller
 *
 * REST API for receiving trade item events from loader service
 */
// Marks this class as a REST controller, combining @Controller and @ResponseBody so all methods return serialized data
@RestController
// Maps all endpoints in this controller under the "/api" base path (e.g., /api/process, /api/health)
@RequestMapping("/api")
// Enables Cross-Origin Resource Sharing for all origins, allowing any frontend or service to call these endpoints
@CrossOrigin(origins = "*")
// Declares the TradeItemController class that exposes REST endpoints for trade item event processing
public class TradeItemController {

    // Creates a static final Logger instance for this controller class, used to log request handling events
    private static final Logger logger = LoggerFactory.getLogger(TradeItemController.class);

    // Injects the TradeItemProcessingService bean automatically by Spring's dependency injection container
    // The @Autowired annotation tells Spring to find and wire the matching bean at application startup
    @Autowired
    // Declares the processingService field that holds the reference to the trade item processing business logic
    private TradeItemProcessingService processingService;

    /**
     * Process trade item event
     *
     * Called by loader service with trade item event data
     *
     * Example request:
     * {
     *   "eventId": "evt_trade-item_xyz789",
     *   "eventType": "trade-item",
     *   "gtin": "12345678901234",
     *   "skuId": "SKU001",
     *   "supplierId": "SUP001",
     *   "supplierName": "ABC Supplier",
     *   "description": "Laptop Computer",
     *   "unitOfMeasure": "EACH",
     *   "minOrderQuantity": 10,
     *   "leadTimeDays": 7
     * }
     */
    // Maps HTTP POST requests to the "/api/process" URL path to this method
    @PostMapping("/process")
    // Defines the endpoint method that receives a trade item event payload and returns an HTTP response
    // @RequestBody deserializes the incoming JSON request body into a Map<String, Object> parameter
    public ResponseEntity<String> processTradeItem(@RequestBody Map<String, Object> tradeItemEvent) {
        // Wraps the entire processing logic in a try-catch block to gracefully handle any exceptions
        try {
            // Logs an informational message indicating that a new trade item event has been received for processing
            logger.info("📥 Received trade item event for processing");

            // Delegates the actual trade item processing to the service layer which handles validation and business logic
            processingService.processTradeItem(tradeItemEvent);

            // Logs a success message after the processing service completes without throwing an exception
            logger.info("✅ Trade item processed successfully");
            // Returns an HTTP 200 OK response with a success message string as the response body
            return ResponseEntity.ok("Trade item processed successfully");

        // Catches any exception thrown during trade item processing (validation errors, runtime errors, etc.)
        } catch (Exception e) {
            // Logs the error at ERROR level with the exception message and full stack trace for debugging
            logger.error("❌ Failed to process trade item: {}", e.getMessage(), e);
            // Returns an HTTP 500 Internal Server Error response with a descriptive error message as the body
            return ResponseEntity.internalServerError()
                .body("Failed to process trade item: " + e.getMessage());
        }
    }

    /**
     * Health check
     */
    // Maps HTTP GET requests to the "/api/health" URL path to this health check method
    @GetMapping("/health")
    // Defines the health check endpoint that returns service status information as a JSON map
    public ResponseEntity<Map<String, String>> health() {
        // Returns an HTTP 200 OK response with an immutable map containing the service status and service name
        // Map.of() creates an unmodifiable map with "status" set to "UP" and "service" set to "trade-item-service"
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "trade-item-service"
        ));
    }
}
