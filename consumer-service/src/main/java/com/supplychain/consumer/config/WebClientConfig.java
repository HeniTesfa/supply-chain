// Package declaration: this class belongs to the config sub-package of the consumer service
package com.supplychain.consumer.config;

// Import the @Bean annotation, which tells Spring that a method produces a bean to be managed by the Spring container
import org.springframework.context.annotation.Bean;

// Import the @Configuration annotation, which indicates that this class declares one or more @Bean methods
import org.springframework.context.annotation.Configuration;

// Import WebClient, which is Spring WebFlux's non-blocking, reactive HTTP client used for making HTTP requests
import org.springframework.web.reactive.function.client.WebClient;

// @Configuration marks this class as a source of bean definitions for the Spring application context
// Spring will process this class at startup and register any @Bean methods it finds
@Configuration
public class WebClientConfig {

    // @Bean annotation tells Spring to register the return value of this method as a bean in the application context
    // This bean can then be injected (autowired) into other Spring components that need to make HTTP calls
    // The method creates and returns a WebClient.Builder instance, which is a factory for building WebClient instances
    // By providing a Builder (not a built WebClient), each consumer can customize the WebClient with their own base URL, headers, etc.
    // This is used by LoaderServiceClient to create a WebClient for forwarding events to the loader-service
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Return a new WebClient.Builder with default settings; consumers of this bean will call .build() after customization
        return WebClient.builder();
    }
}
