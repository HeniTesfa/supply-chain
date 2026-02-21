// Package declaration: places this class in the shipment service layer of the supply chain application
package com.supplychain.shipment.service;

// Import the SLF4J Logger interface for creating structured log statements throughout the service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory, a factory class used to create Logger instances bound to specific classes
import org.slf4j.LoggerFactory;
// Import the Spring @Service annotation to mark this class as a Spring-managed service component bean
import org.springframework.stereotype.Service;

// Import the Java Map interface for handling key-value pair event data received from the loader service
import java.util.Map;

/**
 * Shipment Processing Service
 *
 * Core business logic for processing shipment tracking events:
 * - Tracking number management
 * - Carrier integration
 * - Shipment status updates
 * - Location tracking
 * - Delivery estimation
 * - Customer notifications
 */
// Marks this class as a Spring service component, registering it as a singleton bean in the application context
// Spring's component scanning will automatically detect and instantiate this class at startup
@Service
// Declares the ShipmentProcessingService class containing all shipment event processing business logic
public class ShipmentProcessingService {

    // Creates a static final Logger instance bound to this class for consistent, class-identified log output
    // The logger is used throughout the service to track shipment processing steps, status changes, and alerts
    private static final Logger logger = LoggerFactory.getLogger(ShipmentProcessingService.class);

    /**
     * Process shipment event
     *
     * @param shipmentEvent - Shipment event data from loader service
     */
    // Public method serving as the main entry point for processing a shipment event from the loader service
    // Accepts a Map<String, Object> representing the deserialized JSON event payload
    public void processShipment(Map<String, Object> shipmentEvent) {
        // Extracts the tracking number from the event map, the primary identifier for the shipment
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        // Extracts the order ID from the event map, linking this shipment to the originating customer order
        String orderId = (String) shipmentEvent.get("orderId");
        // Extracts the carrier name from the event map (e.g., "FedEx", "UPS", "USPS")
        String carrier = (String) shipmentEvent.get("carrier");
        // Extracts the shipment status from the event map (e.g., "IN_TRANSIT", "DELIVERED", "DELAYED")
        String shipmentStatus = (String) shipmentEvent.get("shipmentStatus");
        // Extracts the current location of the shipment from the event map for tracking purposes
        String currentLocation = (String) shipmentEvent.get("currentLocation");

        // Logs an informational message with all key shipment fields for operational visibility and troubleshooting
        logger.info("🔄 Processing shipment - Tracking: {}, Order: {}, Carrier: {}, Status: {}, Location: {}",
            trackingNumber, orderId, carrier, shipmentStatus, currentLocation);

        // Step 1: Calls the validation method to ensure required fields (tracking number, carrier) are present and valid
        validateShipment(shipmentEvent);

        // Step 2: Calls the status processing method to execute status-specific business logic (notifications, alerts)
        processShipmentStatus(shipmentStatus, shipmentEvent);

        // Step 3: Calls the tracking update method to record the latest tracking information
        updateTracking(shipmentEvent);

        // In a real system, you would:
        // 1. Update tracking database
        // 2. Send customer notifications (email/SMS)
        // 3. Update order management system
        // 4. Trigger alerts for delays
        // 5. Calculate estimated delivery
        // 6. Sync with carrier API for real-time updates

        // Logs a success message indicating the entire shipment processing pipeline completed without errors
        logger.info("✅ Shipment processing completed successfully");
    }

    /**
     * Validate shipment data
     */
    // Private method that validates all required fields in the shipment event payload
    // Throws IllegalArgumentException if tracking number or carrier are missing or empty
    private void validateShipment(Map<String, Object> shipmentEvent) {
        // Logs a debug-level message indicating the start of the validation phase
        logger.debug("Validating shipment data...");

        // Required: Tracking number
        // Checks if the event map contains the "trackingNumber" key; absence means a required field is missing
        if (!shipmentEvent.containsKey("trackingNumber")) {
            // Throws an IllegalArgumentException to reject events that lack the tracking number field entirely
            throw new IllegalArgumentException("Tracking number is required");
        }

        // Retrieves the tracking number value from the map, casting it to String for further validation
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        // Checks if the tracking number is null or contains only whitespace, which are invalid even when the key exists
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            // Throws an IllegalArgumentException because a present but empty tracking number is not acceptable
            throw new IllegalArgumentException("Tracking number cannot be empty");
        }

        // Required: Carrier
        // Checks if the event map contains the "carrier" key, which is mandatory for shipment routing
        if (!shipmentEvent.containsKey("carrier")) {
            // Throws an IllegalArgumentException to reject events that do not specify a carrier
            throw new IllegalArgumentException("Carrier is required");
        }

        // Retrieves the carrier value from the map, casting it to String for validation
        String carrier = (String) shipmentEvent.get("carrier");
        // Checks if the carrier is null or empty, which would prevent proper shipment handling
        if (carrier == null || carrier.trim().isEmpty()) {
            // Throws an IllegalArgumentException because carrier must be a non-empty value
            throw new IllegalArgumentException("Carrier cannot be empty");
        }

        // Logs a debug-level confirmation that all validation checks passed, including the validated tracking number
        logger.debug("✓ Shipment validation passed - Tracking: {}", trackingNumber);
    }

    /**
     * Process shipment based on status
     */
    // Private method that executes status-specific business logic based on the shipment's current status
    // Uses a switch statement to handle each possible shipment lifecycle state with appropriate logging and actions
    private void processShipmentStatus(String status, Map<String, Object> shipmentEvent) {
        // Checks if the status field is null, meaning no status was provided in the event
        if (status == null) {
            // Logs a debug message indicating no status was specified, and returns early without further processing
            logger.debug("No status specified");
            // Returns immediately since there is no status-specific logic to execute
            return;
        }

        // Extracts the tracking number from the event map for use in status-specific log messages
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");

        // Switches on the uppercase version of the status string to handle all possible shipment states
        // toUpperCase() ensures case-insensitive matching against the defined status constants
        switch (status.toUpperCase()) {
            // Handles the CREATED status: shipment label has been generated but package not yet picked up
            case "CREATED":
            // Also handles the LABEL_CREATED status as an alias for the CREATED state
            case "LABEL_CREATED":
                // Logs an info message indicating a new shipment has been created with the given tracking number
                logger.info("📦 Shipment created - Tracking: {}", trackingNumber);
                // Send notification: "Your order has been packaged"
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the PICKED_UP status: carrier has collected the package from the origin facility
            case "PICKED_UP":
                // Logs an info message indicating the shipment has been picked up from its origin location
                logger.info("🚚 Shipment picked up from origin - Tracking: {}", trackingNumber);
                // Send notification: "Your order has been picked up"
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the IN_TRANSIT status: package is currently moving through the carrier's network
            case "IN_TRANSIT":
                // Logs an info message indicating the shipment is currently in transit between facilities
                logger.info("✈️  Shipment in transit - Tracking: {}", trackingNumber);
                // Extracts the current location from the event to provide detailed transit tracking information
                String currentLocation = (String) shipmentEvent.get("currentLocation");
                // Checks if a current location was provided in the event data
                if (currentLocation != null) {
                    // Logs the current geographic location of the shipment for tracking visibility
                    logger.info("📍 Current location: {}", currentLocation);
                }
                // Send notification: "Your order is on the way"
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the OUT_FOR_DELIVERY status: package is on the final delivery vehicle heading to the customer
            case "OUT_FOR_DELIVERY":
                // Logs an info message indicating the shipment is out for final delivery to the customer
                logger.info("🏠 Shipment out for delivery - Tracking: {}", trackingNumber);
                // Send notification: "Your order will arrive today"
                // Alert customer to be available
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the DELIVERED status: package has been successfully delivered to the recipient
            case "DELIVERED":
                // Logs an info message indicating the shipment has been delivered successfully
                logger.info("✅ Shipment delivered successfully - Tracking: {}", trackingNumber);
                // Send notification: "Your order has been delivered"
                // Close shipment tracking
                // Update order status to fulfilled
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the DELAYED status: shipment is behind schedule and will not arrive on the estimated date
            case "DELAYED":
                // Logs a warning message indicating the shipment has been delayed, requiring attention
                logger.warn("⏰ Shipment delayed - Tracking: {}", trackingNumber);
                // Send notification: "Your order has been delayed"
                // Update estimated delivery date
                // Escalate to customer service
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the EXCEPTION status: an unexpected issue occurred during shipment transit
            case "EXCEPTION":
            // Also handles the FAILED_DELIVERY status as a delivery attempt that was unsuccessful
            case "FAILED_DELIVERY":
                // Logs an error message indicating a shipment exception occurred, requiring immediate investigation
                logger.error("❌ Shipment exception occurred - Tracking: {}", trackingNumber);
                // Alert customer service immediately
                // Send notification: "Issue with your delivery"
                // Investigate root cause
                // Arrange re-delivery or customer pickup
                // Falls through to the break statement to exit the switch block
                break;

            // Handles the RETURNED status: package is being sent back to the sender
            case "RETURNED":
            // Also handles the RETURNED_TO_SENDER status as an alias for the returned state
            case "RETURNED_TO_SENDER":
                // Logs a warning message indicating the shipment is being returned to the sender
                logger.warn("↩️  Shipment returned to sender - Tracking: {}", trackingNumber);
                // Send notification: "Your order is being returned"
                // Initiate refund process
                // Investigate why delivery failed
                // Falls through to the break statement to exit the switch block
                break;

            // Default case handles any unrecognized or custom shipment statuses not explicitly covered above
            default:
                // Logs the unrecognized status at debug level for informational purposes without raising an error
                logger.debug("Shipment status: {}", status);
        }
    }

    /**
     * Update tracking information
     */
    // Private method that updates the tracking information record for the shipment
    // In a real system, this would persist tracking data to a database and sync with external systems
    private void updateTracking(Map<String, Object> shipmentEvent) {
        // Extracts the tracking number from the event map for logging the tracking update operation
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        // Extracts the carrier name from the event map for logging alongside the tracking number
        String carrier = (String) shipmentEvent.get("carrier");

        // Logs an info message indicating that tracking information is being updated for the given carrier and tracking number
        logger.info("🔄 Updating tracking info - Carrier: {}, Tracking: {}", carrier, trackingNumber);

        // In a real system:
        // 1. Update tracking database with latest info
        // 2. Store location history
        // 3. Calculate transit time
        // 4. Compare against SLA
        // 5. Update customer-facing tracking page
        // 6. Sync with analytics for carrier performance
    }
}
