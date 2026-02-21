// Package declaration: this class belongs to the service sub-package of the consumer service
package com.supplychain.consumer.service;

// Import SLF4J Logger interface, which provides a logging API that abstracts the underlying logging framework
import org.slf4j.Logger;

// Import LoggerFactory, which is the factory class used to create Logger instances for specific classes
import org.slf4j.LoggerFactory;

// Import @Value annotation, which injects values from application properties or environment variables into fields
import org.springframework.beans.factory.annotation.Value;

// Import @Service annotation, which marks this class as a Spring service component for automatic detection and bean registration
import org.springframework.stereotype.Service;

// Import WebClient, Spring WebFlux's reactive, non-blocking HTTP client used for making HTTP requests to other services
import org.springframework.web.reactive.function.client.WebClient;

// Import Map from java.util, used to represent the event data as a key-value pair structure (generic JSON-like object)
import java.util.Map;

// @Service marks this class as a Spring-managed service bean, making it eligible for dependency injection into other components
// This client is responsible for forwarding processed events from the consumer-service to the loader-service via HTTP
@Service
public class LoaderServiceClient {

    // Create a static logger instance for this class to log informational messages and errors during event forwarding
    // The logger is bound to LoaderServiceClient.class so log messages include this class name for easy identification
    private static final Logger logger = LoggerFactory.getLogger(LoaderServiceClient.class);

    // Declare a final WebClient instance variable that will be used to make HTTP requests to the loader service
    // WebClient is preferred over RestTemplate in reactive/modern Spring applications for its non-blocking capabilities
    private final WebClient webClient;

    // @Value injects the loader service URL from the application configuration (application.yml)
    // The syntax ${loader.service.url:http://localhost:8082} means: use the property "loader.service.url" if it exists,
    // otherwise fall back to the default value "http://localhost:8082" (used during local development)
    // In Docker, this is overridden via environment variables to point to the containerized loader service
    @Value("${loader.service.url:http://localhost:8082}")
    private String loaderServiceUrl;

    // Constructor that accepts a WebClient.Builder (injected by Spring from the WebClientConfig bean)
    // The Builder pattern allows the WebClient to be configured before being built into a usable instance
    public LoaderServiceClient(WebClient.Builder webClientBuilder) {
        // Build the WebClient instance from the injected builder with default settings
        // The base URL is not set here because it is prepended manually in each request using loaderServiceUrl
        this.webClient = webClientBuilder.build();
    }

    // Public method to forward a supply chain event (represented as a Map) to the loader service for routing
    // The loader service will then determine the event type and route it to the appropriate downstream service
    // This method is called by the KafkaConsumerService after successful deduplication and database persistence
    public void forwardToLoader(Map<String, Object> event) {
        try {
            // Make a synchronous HTTP POST request to the loader service's routing endpoint
            String response = webClient.post()
                // Set the full URI by concatenating the loader service base URL with the routing API path
                .uri(loaderServiceUrl + "/api/loader/route")
                // Set the request body to the event map, which will be serialized to JSON automatically
                .bodyValue(event)
                // Send the request and begin processing the response; retrieve() initiates the HTTP exchange
                .retrieve()
                // Convert the response body to a Mono<String> (a reactive type representing a single async value)
                .bodyToMono(String.class)
                // Block the current thread and wait for the response synchronously (converts reactive to imperative)
                // This is acceptable here because the consumer processes events sequentially per partition
                .block();

            // Log a success message indicating the event was successfully forwarded to the loader service
            logger.info("Successfully forwarded to loader service");
        } catch (Exception e) {
            // Log an error message with the exception details if the HTTP call to the loader service fails
            logger.error("Failed to forward to loader service: {}", e.getMessage());
            // Re-throw the exception wrapped in a RuntimeException so the calling code can handle the failure
            // This allows the KafkaConsumerService to catch this error and log it to the error_logs collection
            throw new RuntimeException("Loader service call failed", e);
        }
    }
}
