// Package declaration: this is the root package for the consumer service application
package com.supplychain.consumer;

// Import SpringApplication, which is the entry point class that bootstraps and launches a Spring Boot application
import org.springframework.boot.SpringApplication;

// Import @SpringBootApplication, a convenience annotation that combines three annotations:
// @Configuration (marks this as a source of bean definitions),
// @EnableAutoConfiguration (enables Spring Boot's auto-configuration based on classpath dependencies),
// and @ComponentScan (tells Spring to scan this package and sub-packages for components, services, controllers, etc.)
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication marks this as the main configuration class and entry point for the Spring Boot application
// Spring will automatically scan the com.supplychain.consumer package and all its sub-packages
// (config, controller, entity, model, repository, service) for Spring-managed beans
@SpringBootApplication
public class ConsumerServiceApplication {

    // The main method is the standard Java entry point; it is called by the JVM when the application starts
    // The args parameter receives any command-line arguments passed to the application at startup
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring application context, starts the embedded web server (Tomcat),
        // initializes all beans (Kafka consumers, MongoDB repositories, REST controllers, etc.),
        // and begins listening for incoming HTTP requests on port 8081 as configured in application.yml
        // The first argument identifies this class as the primary Spring configuration source
        // The second argument passes through any command-line arguments to the Spring environment
        SpringApplication.run(ConsumerServiceApplication.class, args);
    }
}
