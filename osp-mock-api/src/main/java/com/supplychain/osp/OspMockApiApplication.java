// Package declaration: places this main application class in the root package of the OSP mock API service
package com.supplychain.osp;

// Import Spring Boot's SpringApplication class which provides the static run() method to bootstrap the application
import org.springframework.boot.SpringApplication;
// Import the @SpringBootApplication annotation which combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSP Mock API Application
 *
 * Simulates external OSP (Order Service Provider) API
 * Used for testing item-service integration
 *
 * Features:
 * - In-memory item storage
 * - GET /osp/api/items/{skuId} - Retrieve item
 * - POST /osp/api/items - Create/Update item
 * - Test endpoints for slow responses and errors
 * - Pre-populated with sample data
 *
 * Port: 9000
 */
// Marks this class as the Spring Boot application entry point; enables auto-configuration, component scanning,
// and allows defining extra configuration within the com.supplychain.osp package and its subpackages
@SpringBootApplication
// Declares the main application class for the OSP Mock API microservice
public class OspMockApiApplication {

    // The main method serves as the JVM entry point; Java runtime calls this method when the application starts
    // The args parameter captures any command-line arguments passed to the application at startup
    public static void main(String[] args) {
        // Bootstraps the Spring Boot application by creating the ApplicationContext, performing auto-configuration,
        // starting the embedded Tomcat server on port 9000, and scanning for all Spring components in this package
        SpringApplication.run(OspMockApiApplication.class, args);
    }
}
