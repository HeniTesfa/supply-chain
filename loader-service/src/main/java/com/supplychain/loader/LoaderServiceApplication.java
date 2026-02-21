// Declares this class belongs to the root package of the loader service
package com.supplychain.loader;

// Import SpringApplication which provides the static run() method to bootstrap and launch a Spring Boot application
import org.springframework.boot.SpringApplication;
// Import @SpringBootApplication which is a convenience annotation combining:
// @Configuration (marks this class as a bean definition source),
// @EnableAutoConfiguration (auto-configures beans based on classpath dependencies like WebFlux),
// and @ComponentScan (scans the com.supplychain.loader package and subpackages for Spring components)
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Loader Service Application
 *
 * Event Router - Routes events to appropriate downstream services:
 * - item -> item-service (Port 8083)
 * - trade-item -> trade-item-service (Port 8084)
 * - supplier-supply -> supplier-supply-service (Port 8085)
 * - shipment -> shipment-service (Port 8086)
 *
 * Port: 8082
 */
// @SpringBootApplication enables auto-configuration, component scanning, and marks this as the primary configuration class
// Spring Boot will auto-configure WebClient, the embedded web server, and other beans based on the classpath and application.yml
@SpringBootApplication
// Declares the LoaderServiceApplication class which serves as the entry point for the loader microservice
// This service runs on port 8082 (configured in application.yml) and acts as an event router, receiving events
// from the consumer-service and forwarding them to the correct downstream processing service based on event type
public class LoaderServiceApplication {

    // The main method serves as the JVM entry point; it is the first method called when the application starts
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring application by creating the ApplicationContext,
        // performing component scanning (finds LoaderController, LoaderService, WebClientConfig),
        // auto-configuring beans, starting the embedded Tomcat server on port 8082,
        // and beginning to accept HTTP requests for event routing
        // The first argument is the primary configuration class, the second is command-line arguments passed to the JVM
        SpringApplication.run(LoaderServiceApplication.class, args);
    }
}
