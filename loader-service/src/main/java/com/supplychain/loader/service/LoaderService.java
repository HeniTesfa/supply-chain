// Declares this class belongs to the loader service's service layer package
package com.supplychain.loader.service;

// Import the SLF4J Logger interface for structured logging throughout the loader service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create a logger instance bound to this service class
import org.slf4j.LoggerFactory;
// Import Spring's @Value annotation which injects values from application.yml properties or environment variables into fields
import org.springframework.beans.factory.annotation.Value;
// Import Spring's @Service stereotype annotation to mark this class as a service-layer bean in the Spring container
import org.springframework.stereotype.Service;
// Import Spring WebFlux's WebClient which is a non-blocking, reactive HTTP client used to make REST calls to downstream services
import org.springframework.web.reactive.function.client.WebClient;

// Import the Map interface used for the event data payload that is routed to downstream services
import java.util.Map;

/**
 * Loader Service
 *
 * Routes events to appropriate downstream services based on event type:
 * - item -> item-service (Port 8083)
 * - trade-item -> trade-item-service (Port 8084)
 * - supplier-supply -> supplier-supply-service (Port 8085)
 * - shipment -> shipment-service (Port 8086)
 */
// @Service marks this class as a Spring service component, registering it as a singleton bean for dependency injection
@Service
// Declares the LoaderService class which is the core event routing engine of the loader microservice
// It receives events from the consumer service and forwards them to the appropriate downstream processing service
public class LoaderService {

    // Creates a static logger instance bound to this class for logging routing decisions and errors
    private static final Logger logger = LoggerFactory.getLogger(LoaderService.class);

    // The WebClient instance used to make HTTP POST requests to downstream services for event forwarding
    private final WebClient webClient;

    // @Value injects the item-service URL from application.yml property "services.item-service.url"
    // The default value "http://localhost:8083" is used during local development; overridden in Docker via environment variables
    @Value("${services.item-service.url:http://localhost:8083}")
    // Holds the base URL of the item-service where item events will be routed (e.g., "http://localhost:8083")
    private String itemServiceUrl;

    // @Value injects the trade-item-service URL from application.yml property "services.trade-item-service.url"
    // The default value "http://localhost:8084" is used during local development
    @Value("${services.trade-item-service.url:http://localhost:8084}")
    // Holds the base URL of the trade-item-service where trade item events will be routed (e.g., "http://localhost:8084")
    private String tradeItemServiceUrl;

    // @Value injects the supplier-supply-service URL from application.yml property "services.supplier-supply-service.url"
    // The default value "http://localhost:8085" is used during local development
    @Value("${services.supplier-supply-service.url:http://localhost:8085}")
    // Holds the base URL of the supplier-supply-service where supplier supply events will be routed (e.g., "http://localhost:8085")
    private String supplierSupplyServiceUrl;

    // @Value injects the shipment-service URL from application.yml property "services.shipment-service.url"
    // The default value "http://localhost:8086" is used during local development
    @Value("${services.shipment-service.url:http://localhost:8086}")
    // Holds the base URL of the shipment-service where shipment events will be routed (e.g., "http://localhost:8086")
    private String shipmentServiceUrl;

    // Constructor that accepts a WebClient.Builder (injected by Spring from the WebClientConfig bean) and builds the WebClient
    // Constructor injection is preferred over @Autowired field injection because it makes dependencies explicit and testable
    public LoaderService(WebClient.Builder webClientBuilder) {
        // Build the WebClient instance from the injected builder, using default settings from WebClientConfig
        this.webClient = webClientBuilder.build();
    }

    /**
     * Route event to appropriate service based on event type
     *
     * @param event - Event data with eventType field
     * @return Response message
     */
    // Public method that routes an incoming event to the correct downstream service based on its event type
    // Returns a string message describing the routing result
    public String routeEvent(Map<String, Object> event) {
        // Extract the event type from the event data map (either from the "eventType" field or inferred from field presence)
        String eventType = extractEventType(event);

        // Log an informational message indicating which event type is being routed
        logger.info("Routing {} event", eventType);

        // Look up the target downstream service URL based on the extracted event type
        String targetUrl = getTargetServiceUrl(eventType);

        // Check if the target URL is null, which means the event type could not be matched to any known service
        if (targetUrl == null) {
            // Log an error that the event type is unknown and cannot be routed
            logger.error("Unknown event type: {}", eventType);
            // Throw a RuntimeException to indicate routing failure, which will result in a 500 error response
            throw new RuntimeException("Unknown event type: " + eventType);
        }

        // Begin a try block to handle exceptions that may occur during the HTTP call to the downstream service
        try {
            // Use WebClient to make a synchronous HTTP POST request to the target service's /api/process endpoint
            String response = webClient.post()
                // Set the full URI by appending "/api/process" to the target service's base URL
                .uri(targetUrl + "/api/process")
                // Set the request body to the event data map, which will be serialized as JSON
                .bodyValue(event)
                // Send the request and begin processing the response
                .retrieve()
                // Extract the response body as a Mono<String> (a reactive single-value publisher)
                .bodyToMono(String.class)
                // Block the current thread and wait for the response, converting from reactive to synchronous execution
                .block();

            // Log a success message indicating the event was routed successfully to the target service
            logger.info("Event routed successfully to: {}", targetUrl);
            // Return a descriptive message indicating which service the event was routed to and what response was received
            return "Event routed to " + eventType + " service: " + response;

        // Catch any exception that occurs during the HTTP request to the downstream service
        } catch (Exception e) {
            // Log an error with the target URL and the exception message for debugging connectivity issues
            logger.error("Failed to route event to {}: {}", targetUrl, e.getMessage());
            // Throw a RuntimeException wrapping the original error to propagate the failure to the controller
            throw new RuntimeException("Failed to route event: " + e.getMessage());
        }
    }

    /**
     * Extract event type from event object
     */
    // Private helper method that determines the event type from the event data map
    // First checks for an explicit "eventType" field, then falls back to field-presence inference
    private String extractEventType(Map<String, Object> event) {
        // Check if the event map contains an explicit "eventType" key (set by the producer service during publishing)
        if (event.containsKey("eventType")) {
            // Cast and return the eventType value as a String
            return (String) event.get("eventType");
        }

        // Fallback: infer the event type by checking which characteristic fields are present in the payload
        // If the event contains a "gtin" field (Global Trade Item Number), it is a trade item event
        if (event.containsKey("gtin")) {
            return "trade-item";
        // If the event contains a "trackingNumber" field, it is a shipment tracking event
        } else if (event.containsKey("trackingNumber")) {
            return "shipment";
        // If the event contains a "warehouseId" field, it is a supplier supply event
        } else if (event.containsKey("warehouseId")) {
            return "supplier-supply";
        // If the event contains a "skuId" field (Stock Keeping Unit), it is an item event
        } else if (event.containsKey("skuId")) {
            return "item";
        }

        // If no known fields are found, return "unknown" which will cause the routing to fail with an error
        return "unknown";
    }

    /**
     * Get target service URL based on event type
     */
    // Private helper method that maps an event type string to the corresponding downstream service's base URL
    // Returns null if the event type is not recognized
    private String getTargetServiceUrl(String eventType) {
        // Guard clause: return null immediately if the event type is null
        if (eventType == null) {
            return null;
        }

        // Switch on the lowercase version of the event type for case-insensitive matching
        switch (eventType.toLowerCase()) {
            // "item" events are routed to the item-service for item processing and OSP API integration
            case "item":
                // Log the target URL at debug level for troubleshooting routing decisions
                logger.debug("Target: item-service at {}", itemServiceUrl);
                // Return the item-service base URL (e.g., "http://localhost:8083")
                return itemServiceUrl;

            // "trade-item" or "tradeitem" events are routed to the trade-item-service for GTIN/supplier management
            // Both hyphenated and non-hyphenated forms are accepted for flexibility
            case "trade-item":
            case "tradeitem":
                // Log the target URL at debug level
                logger.debug("Target: trade-item-service at {}", tradeItemServiceUrl);
                // Return the trade-item-service base URL (e.g., "http://localhost:8084")
                return tradeItemServiceUrl;

            // "supplier-supply" events are routed to the supplier-supply-service for supplier level tracking
            case "supplier-supply":
                // Log the target URL at debug level
                logger.debug("Target: supplier-supply-service at {}", supplierSupplyServiceUrl);
                // Return the supplier-supply-service base URL (e.g., "http://localhost:8085")
                return supplierSupplyServiceUrl;

            // "shipment" events are routed to the shipment-service for shipment tracking
            case "shipment":
                // Log the target URL at debug level
                logger.debug("Target: shipment-service at {}", shipmentServiceUrl);
                // Return the shipment-service base URL (e.g., "http://localhost:8086")
                return shipmentServiceUrl;

            // If the event type does not match any known type, return null to signal that routing cannot proceed
            default:
                return null;
        }
    }
}
