// Package declaration: places this main application class in the root package of the shipment service
package com.supplychain.shipment;

// Import Spring Boot's SpringApplication class which provides the static run() method to bootstrap the application
import org.springframework.boot.SpringApplication;
// Import the @SpringBootApplication annotation which combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Shipment Service Application
 *
 * Processes shipment tracking events
 *
 * Responsibilities:
 * - Receive shipment events from loader service
 * - Track shipment status updates
 * - Monitor carrier information
 * - Track location updates
 * - Manage delivery estimates
 * - Generate customer notifications
 *
 * Port: 8086
 */
// Marks this class as the Spring Boot application entry point; enables auto-configuration, component scanning,
// and allows defining extra configuration within the com.supplychain.shipment package and its subpackages
@SpringBootApplication
// Declares the main application class for the Shipment Service microservice
public class ShipmentServiceApplication {

    // The main method serves as the JVM entry point; Java runtime calls this method when the application starts
    // The args parameter captures any command-line arguments passed to the application at startup
    public static void main(String[] args) {
        // Bootstraps the Spring Boot application by creating the ApplicationContext, performing auto-configuration,
        // starting the embedded Tomcat server on port 8086, and scanning for all Spring components in this package
        SpringApplication.run(ShipmentServiceApplication.class, args);
    }
}
