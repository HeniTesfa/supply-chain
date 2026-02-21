package com.supplychain.consumer.config; // Define the package for consumer service configuration classes

// Apache Kafka consumer configuration constants (e.g., BOOTSTRAP_SERVERS_CONFIG, GROUP_ID_CONFIG)
import org.apache.kafka.clients.consumer.ConsumerConfig;
// Kafka string deserializer for message keys (events use String keys like event IDs)
import org.apache.kafka.common.serialization.StringDeserializer;
// Spring annotation to inject externalized property values from application.yml
import org.springframework.beans.factory.annotation.Value;
// Spring annotation to declare a method that produces a Spring-managed bean
import org.springframework.context.annotation.Bean;
// Spring annotation to mark this class as a source of bean definitions
import org.springframework.context.annotation.Configuration;
// Spring Kafka annotation that enables Kafka listener infrastructure (required for @KafkaListener)
import org.springframework.kafka.annotation.EnableKafka;
// Factory that creates concurrent Kafka listener containers supporting multiple threads
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
// Interface for creating Kafka Consumer instances with specific configuration
import org.springframework.kafka.core.ConsumerFactory;
// Default implementation of ConsumerFactory that creates consumers from a properties map
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
// Spring Kafka JSON deserializer for converting Kafka message values from JSON to Java objects
import org.springframework.kafka.support.serializer.JsonDeserializer;

// Java HashMap for building the Kafka consumer configuration properties map
import java.util.HashMap;
// Java Map interface used to hold Kafka consumer configuration key-value pairs
import java.util.Map;

/**
 * Kafka Consumer Configuration
 *
 * Configures the Kafka consumer infrastructure for the consumer-service.
 * Key design decisions:
 * - Manual offset management (auto-commit disabled) for Level 1 deduplication
 * - JSON deserialization for flexible event schema handling
 * - Trusted packages set to wildcard for cross-service event compatibility
 * - Consumer group ensures load balancing across multiple service instances
 */
@EnableKafka // Activates Spring Kafka's listener annotation processing (@KafkaListener support)
@Configuration // Marks this class as a Spring configuration class that provides bean definitions
public class KafkaConsumerConfig {

    // Inject the Kafka bootstrap servers address from application.yml (default: localhost:9092)
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers; // Kafka broker connection string (e.g., "kafka:9092" in Docker)

    /**
     * Creates the ConsumerFactory bean that produces Kafka Consumer instances.
     * The factory is configured with all necessary consumer properties including
     * deserializers, consumer group, and offset management settings.
     *
     * @return A ConsumerFactory configured for String keys and generic Object values
     */
    @Bean // Register this method's return value as a Spring-managed bean in the application context
    public ConsumerFactory<String, Object> consumerFactory() {
        // Create a mutable map to hold all Kafka consumer configuration properties
        Map<String, Object> config = new HashMap<>();

        // Set the Kafka broker addresses for initial cluster connection
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Set the consumer group ID — all instances with this ID share topic partitions
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "supply-chain-consumer-group");
        // Configure the key deserializer — event keys are String-typed (event IDs)
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Configure the value deserializer — event payloads are JSON objects
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Allow deserialization of all Java packages (required for generic Map<String, Object> payloads)
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        // CRITICAL: Disable auto-commit for manual offset management (Level 1 deduplication)
        // Offsets are only committed after successful processing via Acknowledgment.acknowledge()
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        // Start reading from the earliest available offset when no committed offset exists
        // Ensures no events are missed when a new consumer group is created
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create and return the consumer factory with the assembled configuration
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Creates the KafkaListenerContainerFactory bean that produces listener containers.
     * The container factory wraps the consumer factory and adds container-level settings
     * like acknowledgment mode. This factory is referenced by @KafkaListener annotations.
     *
     * @return A ConcurrentKafkaListenerContainerFactory configured for manual acknowledgment
     */
    @Bean // Register this factory as a Spring-managed bean referenced by @KafkaListener's containerFactory
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        // Create a new concurrent listener container factory (supports multi-threaded consumption)
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        // Wire the consumer factory into the container factory for creating Consumer instances
        factory.setConsumerFactory(consumerFactory());
        // Set acknowledgment mode to MANUAL — the listener must call Acknowledgment.acknowledge()
        // to commit offsets. This is the foundation of Level 1 deduplication:
        // if processing fails, the offset is NOT committed and the message will be redelivered
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        // Return the fully configured container factory
        return factory;
    }
}
