// Declares this class belongs to the loader service's configuration package
package com.supplychain.loader.config;

// Import Spring's @Bean annotation which marks a method as a bean producer, registering the return value in the application context
import org.springframework.context.annotation.Bean;
// Import Spring's @Configuration annotation which marks this class as a source of bean definitions for the Spring container
import org.springframework.context.annotation.Configuration;
// Import Spring WebFlux's WebClient class which is a non-blocking, reactive HTTP client for making REST API calls
// WebClient is the modern replacement for RestTemplate and supports both synchronous and asynchronous HTTP operations
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient Configuration
 *
 * Configures WebClient for making HTTP calls to downstream services
 */
// @Configuration marks this class as a Spring configuration class, indicating it contains @Bean methods
// that should be processed by the Spring container to generate bean definitions and service requests at runtime
@Configuration
// Declares the WebClientConfig class which provides the WebClient.Builder bean used by the LoaderService
// to make HTTP calls to downstream microservices (item-service, trade-item-service, supplier-supply-service, shipment-service)
public class WebClientConfig {

    // @Bean registers the return value of this method as a Spring bean in the application context
    // Other components can then inject this WebClient.Builder via constructor injection or @Autowired
    @Bean
    // Factory method that creates and returns a WebClient.Builder instance with default configuration
    // Returns WebClient.Builder (not WebClient) so that consumers can further customize the builder before calling build()
    // This allows each service class to add its own base URL, default headers, or other settings
    public WebClient.Builder webClientBuilder() {
        // Creates and returns a new WebClient.Builder with default settings (no base URL, default codecs, default connector)
        // The builder pattern allows downstream classes like LoaderService to call .build() to create the final WebClient
        return WebClient.builder();
    }
}
