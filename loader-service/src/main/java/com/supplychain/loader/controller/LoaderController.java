// Declares this class belongs to the loader service's controller layer package
package com.supplychain.loader.controller;

// Import the LoaderService which contains the core event routing logic
import com.supplychain.loader.service.LoaderService;
// Import the SLF4J Logger interface for structured logging in the controller
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create a logger instance bound to this controller class
import org.slf4j.LoggerFactory;
// Import Spring's @Autowired annotation for automatic dependency injection of the LoaderService bean
import org.springframework.beans.factory.annotation.Autowired;
// Import ResponseEntity which represents the full HTTP response (status code, headers, body) returned to the client
import org.springframework.http.ResponseEntity;
// Import Spring Web annotations: @RestController, @RequestMapping, @CrossOrigin, @PostMapping, @GetMapping, etc.
import org.springframework.web.bind.annotation.*;

// Import the Map interface used for request/response bodies in the REST API endpoints
import java.util.Map;

/**
 * Loader Controller
 *
 * REST API for routing events to appropriate downstream services
 */
// @RestController combines @Controller and @ResponseBody, meaning all handler methods return serialized data (JSON)
// rather than view names, making this suitable for a REST API
@RestController
// @RequestMapping("/api/loader") sets the base URL path for all endpoints in this controller to /api/loader
@RequestMapping("/api/loader")
// @CrossOrigin(origins = "*") enables CORS for all origins, allowing requests from the consumer-service
// or any other service running on a different host/port
@CrossOrigin(origins = "*")
// Declares the LoaderController class which exposes REST endpoints for event routing and health checks
public class LoaderController {

    // Creates a static logger instance bound to this controller class for logging incoming requests and errors
    private static final Logger logger = LoggerFactory.getLogger(LoaderController.class);

    // @Autowired injects the LoaderService singleton bean that handles the event routing logic
    @Autowired
    // The LoaderService dependency that this controller delegates all routing logic to
    private LoaderService loaderService;

    /**
     * Route event to appropriate service
     *
     * Request body should contain:
     * {
     *   "eventType": "item|trade-item|supplier-supply|shipment",
     *   ... event data ...
     * }
     */
    // @PostMapping("/route") maps HTTP POST requests to /api/loader/route to this handler method
    @PostMapping("/route")
    // Method that receives an event via HTTP POST and routes it to the appropriate downstream service
    // Returns ResponseEntity<?> with wildcard type to support both success and error response body formats
    public ResponseEntity<?> routeEvent(@RequestBody Map<String, Object> event) {
        // Begin a try block to catch any exceptions that occur during event routing
        try {
            // Log an informational message that an event has been received for routing
            logger.info("Received event for routing");

            // Delegate to the LoaderService to determine the event type, look up the target service URL,
            // and forward the event via HTTP POST to the downstream service's /api/process endpoint
            String result = loaderService.routeEvent(event);

            // Return an HTTP 200 OK response with a JSON body containing success=true and the routing result message
            return ResponseEntity.ok(Map.of(
                // Indicates the routing operation completed successfully
                "success", true,
                // Contains the descriptive result message from the loader service (e.g., "Event routed to item service: ...")
                "message", result
            ));

        // Catch any exception thrown during event routing (e.g., unknown event type, downstream service unavailable)
        } catch (Exception e) {
            // Log an error message with the exception details for troubleshooting routing failures
            logger.error("Failed to route event: {}", e.getMessage());
            // Return an HTTP 500 Internal Server Error response with a JSON body containing success=false and the error message
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    // Indicates the routing operation failed
                    "success", false,
                    // Contains the error message describing why routing failed
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Health check
     */
    // @GetMapping("/health") maps HTTP GET requests to /api/loader/health to this handler method
    @GetMapping("/health")
    // Method that returns a simple health check response indicating the loader service is running and healthy
    // Returns ResponseEntity with a Map<String, String> body containing the service status
    public ResponseEntity<Map<String, String>> health() {
        // Return an HTTP 200 OK response with an immutable map containing the service health status
        return ResponseEntity.ok(Map.of(
            // Key "status" with value "UP" indicates the loader service is healthy and operational
            "status", "UP",
            // Key "service" with value "loader-service" identifies which service responded to the health check
            "service", "loader-service"
        ));
    }
}
