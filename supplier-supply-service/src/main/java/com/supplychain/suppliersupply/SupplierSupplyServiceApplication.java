// Package declaration: places this main application class in the root package of the supplier supply service
package com.supplychain.suppliersupply;

// Import Spring Boot's SpringApplication class which provides the static run() method to bootstrap the application
import org.springframework.boot.SpringApplication;
// Import the @SpringBootApplication annotation which combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Supplier Supply Service Application
 *
 * Processes supplier supply events - supply level management
 *
 * Responsibilities:
 * - Receive supplier supply events from loader service
 * - Track available, reserved, and on-order quantities
 * - Monitor reorder points
 * - Generate low stock alerts
 * - Manage warehouse-specific supply levels
 *
 * Port: 8085
 */
// Marks this class as the Spring Boot application entry point; enables auto-configuration, component scanning,
// and allows defining extra configuration within the com.supplychain.suppliersupply package and its subpackages
@SpringBootApplication
// Declares the main application class for the Supplier Supply Service microservice
public class SupplierSupplyServiceApplication {

    // The main method serves as the JVM entry point; Java runtime calls this method when the application starts
    // The args parameter captures any command-line arguments passed to the application at startup
    public static void main(String[] args) {
        // Bootstraps the Spring Boot application by creating the ApplicationContext, performing auto-configuration,
        // starting the embedded Tomcat server on port 8085, and scanning for all Spring components in this package
        SpringApplication.run(SupplierSupplyServiceApplication.class, args);
    }
}
