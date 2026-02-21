// Declares this class belongs to the producer service's controller layer package
package com.supplychain.producer.controller;

// Import the KafkaProducerService which handles Kafka publishing and deduplication logic
import com.supplychain.producer.service.KafkaProducerService;
// Import the SLF4J Logger interface for structured logging in the controller
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create a logger instance bound to this controller class
import org.slf4j.LoggerFactory;
// Import Spring's @Autowired annotation for automatic dependency injection of the producer service bean
import org.springframework.beans.factory.annotation.Autowired;
// Import ResponseEntity which represents the entire HTTP response including status code, headers, and body
import org.springframework.http.ResponseEntity;
// Import Spring Web annotations: @RestController, @RequestMapping, @CrossOrigin, @PostMapping, @GetMapping, etc.
import org.springframework.web.bind.annotation.*;

// Import the Map interface used for accepting JSON request bodies and building JSON response objects
import java.util.Map;

/**
 * Producer Controller
 *
 * REST API for creating supply chain events
 *
 * Endpoints:
 * POST /api/producer/item - Create item event
 * POST /api/producer/trade-item - Create trade item event
 * POST /api/producer/supplier-supply - Create supplier supply event
 * POST /api/producer/shipment - Create shipment event
 */
// @RestController combines @Controller and @ResponseBody, meaning all methods return data directly serialized as JSON
// rather than resolving to a view template
@RestController
// @RequestMapping("/api/producer") sets the base URL path prefix for all endpoints in this controller
@RequestMapping("/api/producer")
// @CrossOrigin(origins = "*") enables Cross-Origin Resource Sharing for all origins, allowing the React frontend
// running on a different port (3000) to make API calls to this service (8087)
@CrossOrigin(origins = "*")
// Declares the ProducerController class which exposes REST endpoints for creating supply chain events
public class ProducerController {

    // Creates a static logger instance bound to this controller class for logging request handling and errors
    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    // @Autowired injects the KafkaProducerService singleton bean that handles event publishing to Kafka
    @Autowired
    // The KafkaProducerService dependency that this controller delegates all event publishing logic to
    private KafkaProducerService producerService;

    /**
     * Create item event
     *
     * Example request:
     * {
     *   "skuId": "SKU001",
     *   "itemName": "Laptop",
     *   "category": "Electronics",
     *   "price": 999.99,
     *   "weight": 2.5,
     *   "dimensions": "10x10x10 cm",
     *   "status": "ACTIVE",
     *   "action": "CREATE"
     * }
     */
    // @PostMapping("/item") maps HTTP POST requests to /api/producer/item to this method
    @PostMapping("/item")
    // Method that handles item event creation; accepts a JSON body and an optional Idempotency-Key header
    // Returns ResponseEntity<?> with wildcard type to allow either a success map or an error map response body
    public ResponseEntity<?> createItemEvent(
            // @RequestBody deserializes the incoming JSON request body into a Map<String, Object> representing the event data
            @RequestBody Map<String, Object> eventData,
            // @RequestHeader extracts the "Idempotency-Key" HTTP header; required=false means it is optional and will be null if absent
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Begin a try block to catch any exceptions that occur during event publishing
        try {
            // Log an informational message that an item event creation request has been received
            logger.info("Received item event creation request");
            // Delegate to the KafkaProducerService to publish the event with type "item", the event data, and the optional idempotency key
            Map<String, Object> response = producerService.publishEvent("item", eventData, idempotencyKey);
            // Return an HTTP 200 OK response with the publish result map (contains eventId, topic, partition, offset, duplicate flag)
            return ResponseEntity.ok(response);
        // Catch any exception thrown during event publishing (e.g., Kafka errors, deduplication failures)
        } catch (Exception e) {
            // Log an error message with the exception details for troubleshooting
            logger.error("Failed to create item event: {}", e.getMessage());
            // Return an HTTP 500 Internal Server Error response with a JSON body containing the error message
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create trade item event
     *
     * Example request:
     * {
     *   "gtin": "12345678901234",
     *   "skuId": "SKU001",
     *   "supplierId": "SUP001",
     *   "supplierName": "ABC Supplier",
     *   "description": "Laptop Computer",
     *   "unitOfMeasure": "EACH",
     *   "minOrderQuantity": 10,
     *   "leadTimeDays": 7,
     *   "action": "CREATE"
     * }
     */
    // @PostMapping("/trade-item") maps HTTP POST requests to /api/producer/trade-item to this method
    @PostMapping("/trade-item")
    // Method that handles trade item event creation; trade items represent GTIN/supplier relationships
    // Returns ResponseEntity<?> with wildcard type for flexible success/error response bodies
    public ResponseEntity<?> createTradeItemEvent(
            // @RequestBody deserializes the incoming JSON request body into a Map containing trade item event fields
            @RequestBody Map<String, Object> eventData,
            // @RequestHeader extracts the optional "Idempotency-Key" header for preventing duplicate submissions
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Begin a try block to catch any exceptions during trade item event publishing
        try {
            // Log an informational message that a trade item event creation request has been received
            logger.info("Received trade item event creation request");
            // Delegate to the KafkaProducerService to publish the event with type "trade-item"
            Map<String, Object> response = producerService.publishEvent("trade-item", eventData, idempotencyKey);
            // Return an HTTP 200 OK response with the publish result map
            return ResponseEntity.ok(response);
        // Catch any exception thrown during trade item event publishing
        } catch (Exception e) {
            // Log an error message with the exception details
            logger.error("Failed to create trade item event: {}", e.getMessage());
            // Return an HTTP 500 Internal Server Error response with the error message
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create supplier supply event
     * * Example request:
     * {
     * "supplyEventId": "SUP-EVT-101",
     * "supplierId": "SUP-99",
     * "supplierName": "Global Parts Co",
     * "skuId": "SKU-FINAL-SUCCESS",
     * "gtin": "10876543210987",
     * "quantitySupplied": 500,
     * "unitCost": 42.50,
     * "action": "SUPPLY"
     * }
     */
    // @PostMapping("/supplier-supply") maps HTTP POST requests to /api/producer/supplier-supply to this method
    @PostMapping("/supplier-supply")
    // Method that handles supplier supply event creation; supplier supply events track supplier-level inventory
    // Returns ResponseEntity<?> with wildcard type for flexible success/error response bodies
    public ResponseEntity<?> createSupplierSupplyEvent(
            // @RequestBody deserializes the incoming JSON request body into a Map containing supplier supply event fields
            @RequestBody Map<String, Object> eventData,
            // @RequestHeader extracts the optional "Idempotency-Key" header for deduplication on the producer side
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Begin a try block to catch any exceptions during supplier supply event publishing
        try {
            // Log an informational message that a supplier event creation request has been received
            logger.info("Received supplier event creation request");
            // Delegate to the KafkaProducerService to publish the event with type "supplier-supply"
            Map<String, Object> response = producerService.publishEvent("supplier-supply", eventData, idempotencyKey);
            // Return an HTTP 200 OK response with the publish result map
            return ResponseEntity.ok(response);
        // Catch any exception thrown during supplier supply event publishing
        } catch (Exception e) {
            // Log an error message with the exception details
            logger.error("Failed to create supplier event: {}", e.getMessage());
            // Return an HTTP 500 Internal Server Error response with the error message
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create shipment event
     *
     * Example request:
     * {
     *   "trackingNumber": "TRACK123456",
     *   "orderId": "ORD001",
     *   "carrier": "FedEx",
     *   "shipmentStatus": "IN_TRANSIT",
     *   "originLocation": "New York, NY",
     *   "destinationLocation": "Los Angeles, CA",
     *   "currentLocation": "Chicago, IL",
     *   "action": "UPDATE"
     * }
     */
    // @PostMapping("/shipment") maps HTTP POST requests to /api/producer/shipment to this method
    @PostMapping("/shipment")
    // Method that handles shipment event creation; shipment events track packages through the delivery pipeline
    // Returns ResponseEntity<?> with wildcard type for flexible success/error response bodies
    public ResponseEntity<?> createShipmentEvent(
            // @RequestBody deserializes the incoming JSON request body into a Map containing shipment event fields
            @RequestBody Map<String, Object> eventData,
            // @RequestHeader extracts the optional "Idempotency-Key" header for preventing duplicate shipment event submissions
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Begin a try block to catch any exceptions during shipment event publishing
        try {
            // Log an informational message that a shipment event creation request has been received
            logger.info("Received shipment event creation request");
            // Delegate to the KafkaProducerService to publish the event with type "shipment"
            Map<String, Object> response = producerService.publishEvent("shipment", eventData, idempotencyKey);
            // Return an HTTP 200 OK response with the publish result map
            return ResponseEntity.ok(response);
        // Catch any exception thrown during shipment event publishing
        } catch (Exception e) {
            // Log an error message with the exception details
            logger.error("Failed to create shipment event: {}", e.getMessage());
            // Return an HTTP 500 Internal Server Error response with the error message
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    // @GetMapping("/health") maps HTTP GET requests to /api/producer/health to this method
    @GetMapping("/health")
    // Method that returns a simple health check response indicating the producer service is running
    // Returns ResponseEntity with a Map<String, String> body containing the service status
    public ResponseEntity<Map<String, String>> health() {
        // Return an HTTP 200 OK response with an immutable map containing the service status "UP" and the service name
        return ResponseEntity.ok(Map.of(
            // Key "status" with value "UP" indicates the service is healthy and operational
            "status", "UP",
            // Key "service" with value "producer-service" identifies which service responded to the health check
            "service", "producer-service"
        ));
    }
}
