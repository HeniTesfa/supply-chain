// Declares this class belongs to the item service's service layer package
package com.supplychain.item.service;

// Import the SLF4J Logger interface for structured logging throughout the item processing service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create a logger instance bound to this service class
import org.slf4j.LoggerFactory;
// Import Spring's @Value annotation which injects values from application.yml properties or environment variables
import org.springframework.beans.factory.annotation.Value;
// Import Spring's @Service stereotype annotation to mark this class as a service-layer bean managed by the Spring container
import org.springframework.stereotype.Service;
// Import Spring WebFlux's WebClient which is a non-blocking, reactive HTTP client used to call the external OSP API
import org.springframework.web.reactive.function.client.WebClient;

// Import the Map interface used for the item event data payload received from the loader service
import java.util.Map;

/**
 * Item Processing Service
 *
 * Core business logic for processing item events:
 * 1. Validate item data
 * 2. Transform data for OSP API
 * 3. Send to external OSP API
 * 4. Handle retry logic
 *
 * OSP (Order Service Provider) API Integration:
 * - POST /osp/api/items - Create/Update item in external system
 */
// @Service marks this class as a Spring service component, making it eligible for auto-detection via component scanning
// and registering it as a singleton bean in the Spring application context
@Service
// Declares the ItemProcessingService class which contains the core business logic for processing item events,
// including validation, transformation, and integration with the external OSP (Order Service Provider) API
public class ItemProcessingService {

    // Creates a static logger instance bound to this class for logging item processing steps and errors
    private static final Logger logger = LoggerFactory.getLogger(ItemProcessingService.class);

    // The WebClient instance used to make HTTP POST requests to the external OSP API
    private final WebClient webClient;

    // @Value injects the OSP API base URL from the "osp.api.url" property in application.yml
    // The default value "http://localhost:9000" is used during local development when the osp-mock-api service runs locally
    @Value("${osp.api.url:http://localhost:9000}")
    // Holds the base URL of the external OSP API (e.g., "http://localhost:9000" or the Docker service URL)
    private String ospApiUrl;

    // @Value injects the maximum number of retry attempts from the "osp.api.retry.max-attempts" property in application.yml
    // The default value of 3 means the service will attempt the OSP API call up to 3 times before giving up
    @Value("${osp.api.retry.max-attempts:3}")
    // Holds the maximum number of times to retry a failed OSP API call before throwing an exception
    private int maxRetryAttempts;

    // Constructor that accepts a WebClient.Builder (injected by Spring) and builds the WebClient instance
    // Constructor injection is used instead of @Autowired field injection for better testability and explicit dependencies
    public ItemProcessingService(WebClient.Builder webClientBuilder) {
        // Build the WebClient instance from the injected builder with default settings
        this.webClient = webClientBuilder.build();
    }

    /**
     * Process item event
     *
     * @param itemEvent - Item event data from loader service
     */
    // Public method that orchestrates the full item processing pipeline: validate, transform, and send to OSP API
    // Called by the ItemController when a POST request is received at /api/process
    public void processItem(Map<String, Object> itemEvent) {
        // Log an informational message with the SKU ID extracted from the event data for traceability
        logger.info("Processing item event - SKU: {}", itemEvent.get("skuId"));

        // Step 1: Validate the item event data to ensure all required fields are present and valid
        validateItemData(itemEvent);

        // Step 2: Transform the item event data into the format expected by the OSP API
        // In this implementation, the data is passed through unchanged, but this step allows for field mapping
        Map<String, Object> ospPayload = transformForOspApi(itemEvent);

        // Step 3: Send the transformed payload to the external OSP API with automatic retry on failure
        sendToOspApi(ospPayload);

        // Log that the entire item processing pipeline completed successfully
        logger.info("Item processing completed successfully");
    }

    /**
     * Validate item data
     */
    // Private method that validates the item event data, checking for required fields and valid values
    // Throws IllegalArgumentException if any validation rule is violated
    private void validateItemData(Map<String, Object> itemEvent) {
        // Log a debug message indicating that validation has started
        logger.debug("Validating item data...");

        // Check if the "skuId" field exists in the event data and is not null (SKU ID is a required field)
        if (!itemEvent.containsKey("skuId") || itemEvent.get("skuId") == null) {
            // Throw an IllegalArgumentException if skuId is missing or null, halting the processing pipeline
            throw new IllegalArgumentException("SKU ID is required");
        }

        // Cast the skuId value to a String for further validation
        String skuId = (String) itemEvent.get("skuId");
        // Check if the skuId is an empty or whitespace-only string after trimming
        if (skuId.trim().isEmpty()) {
            // Throw an IllegalArgumentException if skuId is empty, as it must contain actual characters
            throw new IllegalArgumentException("SKU ID cannot be empty");
        }

        // Check if the "itemName" field exists in the event data and is not null (item name is a required field)
        if (!itemEvent.containsKey("itemName") || itemEvent.get("itemName") == null) {
            // Throw an IllegalArgumentException if itemName is missing or null
            throw new IllegalArgumentException("Item name is required");
        }

        // Check if the optional "price" field is present in the event data
        if (itemEvent.containsKey("price")) {
            // Retrieve the price value as a generic Object since it could be a Number or String
            Object priceObj = itemEvent.get("price");
            // Only validate if the price value is not null
            if (priceObj != null) {
                // Convert the price to a double: if it is a Number type (Integer, Double, etc.), use doubleValue();
                // otherwise parse the string representation as a double
                double price = priceObj instanceof Number ?
                    ((Number) priceObj).doubleValue() :
                    Double.parseDouble(priceObj.toString());

                // Check if the price is negative, which is not a valid value for an item
                if (price < 0) {
                    // Throw an IllegalArgumentException if price is negative
                    throw new IllegalArgumentException("Price cannot be negative");
                }
            }
        }

        // Check if the optional "weight" field is present in the event data
        if (itemEvent.containsKey("weight")) {
            // Retrieve the weight value as a generic Object since it could be a Number or String
            Object weightObj = itemEvent.get("weight");
            // Only validate if the weight value is not null
            if (weightObj != null) {
                // Convert the weight to a double: if it is a Number type, use doubleValue();
                // otherwise parse the string representation as a double
                double weight = weightObj instanceof Number ?
                    ((Number) weightObj).doubleValue() :
                    Double.parseDouble(weightObj.toString());

                // Check if the weight is negative, which is not a physically valid value
                if (weight < 0) {
                    // Throw an IllegalArgumentException if weight is negative
                    throw new IllegalArgumentException("Weight cannot be negative");
                }
            }
        }

        // Log a debug message indicating that all validation checks passed successfully
        logger.debug("Item data validation passed");
    }

    /**
     * Transform data for OSP API
     *
     * OSP API may have different field names or data formats
     */
    // Private method that transforms the item event data into the format required by the external OSP API
    // Currently a pass-through, but serves as an extensibility point for field mapping and data conversion
    private Map<String, Object> transformForOspApi(Map<String, Object> itemEvent) {
        // Log a debug message indicating that transformation has started
        logger.debug("Transforming data for OSP API...");

        // In this simple case, we pass through most fields
        // In a real system, you might need to:
        // - Rename fields
        // - Convert units (e.g., pounds to kilograms)
        // - Add additional metadata
        // - Format dates differently

        // Return the item event data unchanged; in a production system, this would return a new transformed map
        return itemEvent;
    }

    /**
     * Send to OSP API with retry logic
     */
    // Private method that sends the item payload to the external OSP API with exponential backoff retry logic
    // Retries up to maxRetryAttempts times (default 3) with increasing wait times: 2s, 4s, 8s, etc.
    // Throws RuntimeException if all retry attempts fail
    private void sendToOspApi(Map<String, Object> ospPayload) {
        // Extract the SKU ID from the payload for logging purposes
        String skuId = (String) ospPayload.get("skuId");
        // Initialize the attempt counter to track the current retry attempt number
        int attempt = 0;
        // Variable to store the last exception encountered, used in the final error message if all retries fail
        Exception lastException = null;

        // Retry loop: continue attempting the API call while the attempt count is below the maximum
        while (attempt < maxRetryAttempts) {
            // Increment the attempt counter before each try (first attempt is 1, not 0)
            attempt++;

            // Begin a try block to catch exceptions from the OSP API call
            try {
                // Log an informational message showing the current attempt number, max attempts, and SKU ID
                logger.info("Sending to OSP API (attempt {}/{}) - SKU: {}",
                    attempt, maxRetryAttempts, skuId);

                // Use WebClient to make a synchronous HTTP POST request to the OSP API's /osp/api/items endpoint
                String response = webClient.post()
                    // Set the full URI by appending "/osp/api/items" to the OSP API base URL
                    .uri(ospApiUrl + "/osp/api/items")
                    // Set the request body to the OSP payload map, which will be serialized as JSON
                    .bodyValue(ospPayload)
                    // Send the request and begin processing the response
                    .retrieve()
                    // Extract the response body as a Mono<String> (a reactive single-value publisher)
                    .bodyToMono(String.class)
                    // Block the current thread and wait for the response, converting from reactive to synchronous execution
                    .block();

                // Log a success message with the response body from the OSP API
                logger.info("OSP API call successful - Response: {}", response);
                // Return immediately on success, exiting the retry loop
                return; // Success!

            // Catch any exception thrown during the OSP API call (e.g., connection refused, timeout, 5xx error)
            } catch (Exception e) {
                // Store the exception so it can be included in the final error message if all retries fail
                lastException = e;
                // Log a warning message with the attempt number and error details
                logger.warn("OSP API call failed (attempt {}/{}): {}",
                    attempt, maxRetryAttempts, e.getMessage());

                // Check if there are remaining retry attempts available
                if (attempt < maxRetryAttempts) {
                    // Calculate the wait time using exponential backoff: 2^attempt * 1000ms
                    // Attempt 1 -> 2000ms (2s), Attempt 2 -> 4000ms (4s), Attempt 3 -> 8000ms (8s)
                    int waitMs = (int) Math.pow(2, attempt) * 1000; // 2s, 4s, 8s...
                    // Log the wait duration before the next retry
                    logger.info("Waiting {}ms before retry...", waitMs);

                    // Begin a try block to handle InterruptedException during the sleep
                    try {
                        // Pause the current thread for the calculated wait duration before retrying
                        Thread.sleep(waitMs);
                    // Catch InterruptedException which occurs if another thread interrupts this thread during sleep
                    } catch (InterruptedException ie) {
                        // Restore the interrupt flag on the current thread so callers can detect the interruption
                        Thread.currentThread().interrupt();
                        // Throw a RuntimeException wrapping the InterruptedException to abort the retry loop
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        // Execution reaches here only when all retry attempts have been exhausted without a successful API call
        // Log an error message indicating the total number of failed attempts
        logger.error("OSP API call failed after {} attempts", maxRetryAttempts);
        // Throw a RuntimeException with a descriptive message and the last exception as the cause
        // This propagates the failure back through the controller to the loader service
        throw new RuntimeException(
            "Failed to send to OSP API after " + maxRetryAttempts + " attempts: " +
            lastException.getMessage(),
            lastException
        );
    }
}
