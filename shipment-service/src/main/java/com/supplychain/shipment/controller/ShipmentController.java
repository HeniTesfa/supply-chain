// Package declaration: places this class in the shipment controller layer of the supply chain application
package com.supplychain.shipment.controller;

// Import the ShipmentProcessingService that contains the core business logic for shipment event processing
import com.supplychain.shipment.service.ShipmentProcessingService;
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
 * Shipment Controller
 *
 * REST API for receiving shipment events from loader service
 */
// Marks this class as a REST controller, combining @Controller and @ResponseBody so all methods return serialized response data
@RestController
// Maps all endpoints in this controller under the "/api" base path (e.g., /api/process, /api/health)
@RequestMapping("/api")
// Enables Cross-Origin Resource Sharing for all origins, allowing any frontend or external service to call these endpoints
@CrossOrigin(origins = "*")
// Declares the ShipmentController class that exposes REST endpoints for shipment event processing
public class ShipmentController {

    // Creates a static final Logger instance for this controller class, used to log request lifecycle events
    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    // Injects the ShipmentProcessingService bean automatically by Spring's dependency injection container
    // The @Autowired annotation tells Spring to resolve and wire the matching service bean at application startup
    @Autowired
    // Declares the processingService field holding the reference to the shipment processing business logic
    private ShipmentProcessingService processingService;

    /**
     * Process shipment event
     *
     * Called by loader service with shipment event data
     *
     * Example request:
     * {
     *   "eventId": "evt_shipment_ghi789",
     *   "eventType": "shipment",
     *   "trackingNumber": "TRACK123456",
     *   "orderId": "ORD001",
     *   "carrier": "FedEx",
     *   "shipmentStatus": "IN_TRANSIT",
     *   "originLocation": "New York, NY",
     *   "destinationLocation": "Los Angeles, CA",
     *   "currentLocation": "Chicago, IL"
     * }
     */
    // Maps HTTP POST requests to the "/api/process" URL path to this method for shipment event ingestion
    @PostMapping("/process")
    // Defines the endpoint method that receives a shipment event payload and returns an HTTP response
    // @RequestBody deserializes the incoming JSON request body into a Map<String, Object> parameter
    public ResponseEntity<String> processShipment(@RequestBody Map<String, Object> shipmentEvent) {
        // Wraps the entire processing logic in a try-catch block to gracefully handle any exceptions
        try {
            // Logs an informational message indicating that a new shipment event has been received for processing
            logger.info("📥 Received shipment event for processing");

            // Delegates the actual shipment processing to the service layer for validation and business logic execution
            processingService.processShipment(shipmentEvent);

            // Logs a success message after the processing service completes without throwing an exception
            logger.info("✅ Shipment processed successfully");
            // Returns an HTTP 200 OK response with a success message string as the response body
            return ResponseEntity.ok("Shipment processed successfully");

        // Catches any exception thrown during shipment processing (validation errors, runtime errors, etc.)
        } catch (Exception e) {
            // Logs the error at ERROR level with the exception message and full stack trace for debugging
            logger.error("❌ Failed to process shipment: {}", e.getMessage(), e);
            // Returns an HTTP 500 Internal Server Error response with a descriptive error message as the body
            return ResponseEntity.internalServerError()
                .body("Failed to process shipment: " + e.getMessage());
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
        // Map.of() creates an unmodifiable map with "status" set to "UP" and "service" set to "shipment-service"
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "shipment-service"
        ));
    }
}
