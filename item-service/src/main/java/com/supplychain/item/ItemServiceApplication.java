// Declares this class belongs to the root package of the item service
package com.supplychain.item;

// Import SpringApplication which provides the static run() method to bootstrap and launch a Spring Boot application
import org.springframework.boot.SpringApplication;
// Import @SpringBootApplication which is a convenience annotation combining three annotations:
// @Configuration (marks this class as a source of bean definitions),
// @EnableAutoConfiguration (tells Spring Boot to auto-configure beans based on classpath dependencies like WebFlux),
// and @ComponentScan (enables scanning of the com.supplychain.item package and subpackages for Spring components)
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Item Service Application
 *
 * Processes item events and integrates with external OSP API
 *
 * Responsibilities:
 * - Receive item events from loader service
 * - Process and validate item data
 * - Send item data to external OSP API
 * - Handle retry logic for failed API calls
 *
 * Port: 8083
 */
// @SpringBootApplication enables auto-configuration, component scanning, and marks this as the primary configuration class
// Spring Boot will auto-configure WebClient, the embedded web server, and other beans based on the classpath and application.yml
@SpringBootApplication
// Declares the ItemServiceApplication class which serves as the entry point for the item microservice
// This service runs on port 8083 (configured in application.yml) and is responsible for processing item events
// received from the loader-service, validating item data, and forwarding it to the external OSP API
// with exponential backoff retry logic for resilient API integration
public class ItemServiceApplication {

    // The main method serves as the JVM entry point; it is the first method called when the application starts
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring application by creating the ApplicationContext,
        // performing component scanning (finds ItemController, ItemProcessingService, ItemRepository),
        // auto-configuring beans (WebClient, MongoDB, embedded Tomcat), starting the embedded web server on port 8083,
        // and beginning to accept HTTP requests for item event processing
        // The first argument is the primary configuration class, the second is command-line arguments passed to the JVM
        SpringApplication.run(ItemServiceApplication.class, args);
    }
}
