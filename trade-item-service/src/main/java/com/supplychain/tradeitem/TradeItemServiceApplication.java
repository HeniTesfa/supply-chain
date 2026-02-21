// Package declaration: places this main application class in the root package of the trade item service
package com.supplychain.tradeitem;

// Import Spring Boot's SpringApplication class which provides the static run() method to bootstrap the application
import org.springframework.boot.SpringApplication;
// Import the @SpringBootApplication annotation which combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Trade Item Service Application
 *
 * Processes trade item events - GTIN and supplier information
 *
 * Responsibilities:
 * - Receive trade item events from loader service
 * - Validate GTIN format (8, 12, 13, or 14 digits)
 * - Process supplier information
 * - Manage product descriptions and specifications
 * - Track min order quantities and lead times
 *
 * Port: 8084
 */
// Marks this class as the Spring Boot application entry point; enables auto-configuration, component scanning,
// and allows defining extra configuration within the com.supplychain.tradeitem package and its subpackages
@SpringBootApplication
// Declares the main application class for the Trade Item Service microservice
public class TradeItemServiceApplication {

    // The main method serves as the JVM entry point; Java runtime calls this method when the application starts
    // The args parameter captures any command-line arguments passed to the application at startup
    public static void main(String[] args) {
        // Bootstraps the Spring Boot application by creating the ApplicationContext, performing auto-configuration,
        // starting the embedded Tomcat server on port 8084, and scanning for all Spring components in this package
        SpringApplication.run(TradeItemServiceApplication.class, args);
    }
}
