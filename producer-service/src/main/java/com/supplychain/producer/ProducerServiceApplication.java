// Declares this class belongs to the root package of the producer service
package com.supplychain.producer;

// Import SpringApplication which provides the static run() method to bootstrap and launch a Spring application
import org.springframework.boot.SpringApplication;
// Import @SpringBootApplication which is a convenience annotation combining three annotations:
// @Configuration (marks this class as a source of bean definitions),
// @EnableAutoConfiguration (tells Spring Boot to auto-configure beans based on classpath dependencies like Kafka, MongoDB),
// and @ComponentScan (enables scanning of the com.supplychain.producer package and subpackages for Spring components)
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Producer Service Application
 *
 * REST API for creating supply chain events and publishing to Kafka
 *
 * Features:
 * - Producer-side deduplication (idempotency keys)
 * - Content hash-based duplicate detection
 * - Transactional Kafka publishing
 * - MongoDB audit trail
 *
 * Port: 8087
 */
// @SpringBootApplication enables auto-configuration, component scanning, and marks this as the main configuration class
// Spring Boot will auto-configure KafkaTemplate, MongoTemplate, ObjectMapper, and other beans based on the classpath
// and the properties defined in application.yml
@SpringBootApplication
// Declares the ProducerServiceApplication class which serves as the entry point for the producer microservice
// This service runs on port 8087 (configured in application.yml) and provides REST endpoints for publishing
// supply chain events (item, trade-item, supplier-supply, shipment) to Apache Kafka
public class ProducerServiceApplication {

    // The main method serves as the JVM entry point; it is the first method called when the application starts
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the Spring application by creating the ApplicationContext,
        // performing component scanning, auto-configuring beans (Kafka producer, MongoDB, web server),
        // starting the embedded Tomcat server on port 8087, and beginning to accept HTTP requests
        // The first argument is the primary configuration class, the second is command-line arguments passed to the JVM
        SpringApplication.run(ProducerServiceApplication.class, args);
    }
}
