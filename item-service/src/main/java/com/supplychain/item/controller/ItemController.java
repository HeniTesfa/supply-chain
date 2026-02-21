// Declares this class belongs to the item service's controller layer package
package com.supplychain.item.controller;

// Import the ItemProcessingService which contains the core item processing business logic
import com.supplychain.item.service.ItemProcessingService;
// Import the SLF4J Logger interface for structured logging in the controller
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create a logger instance bound to this controller class
import org.slf4j.LoggerFactory;
// Import Spring's @Autowired annotation for automatic dependency injection of the ItemProcessingService bean
import org.springframework.beans.factory.annotation.Autowired;
// Import ResponseEntity which represents the full HTTP response (status code, headers, body) returned to the caller
import org.springframework.http.ResponseEntity;
// Import Spring Web annotations: @RestController, @RequestMapping, @CrossOrigin, @PostMapping, @GetMapping, etc.
import org.springframework.web.bind.annotation.*;

// Import the Map interface used for the request body (item event data) and health check response
import java.util.Map;

/**
 * Item Controller
 *
 * REST API for receiving item events from loader service
 */
// @RestController combines @Controller and @ResponseBody, meaning all handler methods return serialized data (JSON/String)
// directly in the response body rather than resolving to a view template
@RestController
// @RequestMapping("/api") sets the base URL path for all endpoints in this controller to /api
@RequestMapping("/api")
// @CrossOrigin(origins = "*") enables CORS for all origins, allowing requests from any service
// regardless of the host or port it is running on
@CrossOrigin(origins = "*")
// Declares the ItemController class which exposes REST endpoints for processing item events and health checks
// The loader-service calls this controller's /api/process endpoint to forward item events for processing
public class ItemController {

    // Creates a static logger instance bound to this controller class for logging incoming requests and processing results
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    // @Autowired injects the ItemProcessingService singleton bean that handles item validation, transformation, and OSP API calls
    @Autowired
    // The ItemProcessingService dependency that this controller delegates all item processing logic to
    private ItemProcessingService itemProcessingService;

    /**
     * Process item event
     *
     * Called by loader service with item event data
     *
     * Example request:
     * {
     *   "eventId": "evt_item_abc123",
     *   "eventType": "item",
     *   "skuId": "SKU001",
     *   "itemName": "Laptop",
     *   "category": "Electronics",
     *   "price": 999.99,
     *   "weight": 2.5,
     *   "dimensions": "10x10x10 cm",
     *   "status": "ACTIVE"
     * }
     */
    // @PostMapping("/process") maps HTTP POST requests to /api/process to this handler method
    // The loader-service forwards item events to this endpoint after determining the event type is "item"
    @PostMapping("/process")
    // Method that receives an item event from the loader-service and delegates processing to ItemProcessingService
    // Returns ResponseEntity<String> with either a success message (200 OK) or an error message (500 Internal Server Error)
    public ResponseEntity<String> processItem(@RequestBody Map<String, Object> itemEvent) {
        // Begin a try block to catch any exceptions that occur during item processing
        try {
            // Log an informational message that an item event has been received for processing
            logger.info("Received item event for processing");

            // Delegate to the ItemProcessingService to execute the full processing pipeline:
            // 1. Validate item data (check required fields, validate price/weight)
            // 2. Transform data for OSP API format
            // 3. Send to OSP API with exponential backoff retry
            itemProcessingService.processItem(itemEvent);

            // Log a success message after the item has been fully processed
            logger.info("Item processed successfully");
            // Return an HTTP 200 OK response with a plain text success message
            return ResponseEntity.ok("Item processed successfully");

        // Catch any exception thrown during item processing (e.g., validation errors, OSP API failures)
        } catch (Exception e) {
            // Log an error message with the exception details and full stack trace for troubleshooting
            logger.error("Failed to process item: {}", e.getMessage(), e);
            // Return an HTTP 500 Internal Server Error response with a plain text error message
            return ResponseEntity.internalServerError()
                .body("Failed to process item: " + e.getMessage());
        }
    }

    /**
     * Health check
     */
    // @GetMapping("/health") maps HTTP GET requests to /api/health to this handler method
    @GetMapping("/health")
    // Method that returns a simple health check response indicating the item service is running and healthy
    // Returns ResponseEntity with a Map<String, String> body containing the service status
    public ResponseEntity<Map<String, String>> health() {
        // Return an HTTP 200 OK response with an immutable map containing the service health status
        return ResponseEntity.ok(Map.of(
            // Key "status" with value "UP" indicates the item service is healthy and operational
            "status", "UP",
            // Key "service" with value "item-service" identifies which service responded to the health check
            "service", "item-service"
        ));
    }
}
