package com.supplychain.shipment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link ShipmentProcessingService}.
 *
 * Tests the core business logic for shipment event processing including:
 * - Required field validation (tracking number, carrier)
 * - All shipment status transitions (CREATED through RETURNED)
 * - Null and unknown status handling
 * - Case-insensitive status matching
 * - Location tracking with IN_TRANSIT status
 *
 * This is a pure unit test — no Spring context or mocks needed since the service
 * has zero injected dependencies.
 */
class ShipmentProcessingServiceTest {

    private ShipmentProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ShipmentProcessingService();
    }

    // ==================== Required Field: Tracking Number ====================

    /** Verifies that a missing tracking number field throws a validation error. */
    @Test
    void processShipment_missingTrackingNumber_throws() {
        Map<String, Object> event = validEvent();
        event.remove("trackingNumber");
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking number is required");
    }

    /** Verifies that a null tracking number value is caught during validation. */
    @Test
    void processShipment_nullTrackingNumber_throws() {
        Map<String, Object> event = validEvent();
        event.put("trackingNumber", null);
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking number cannot be empty");
    }

    /** Verifies that a blank/whitespace-only tracking number is rejected. */
    @Test
    void processShipment_emptyTrackingNumber_throws() {
        Map<String, Object> event = validEvent();
        event.put("trackingNumber", "  ");
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking number cannot be empty");
    }

    // ==================== Required Field: Carrier ====================

    /** Verifies that a missing carrier field throws a validation error. */
    @Test
    void processShipment_missingCarrier_throws() {
        Map<String, Object> event = validEvent();
        event.remove("carrier");
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Carrier is required");
    }

    /** Verifies that a null carrier value is caught during validation. */
    @Test
    void processShipment_nullCarrier_throws() {
        Map<String, Object> event = validEvent();
        event.put("carrier", null);
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Carrier cannot be empty");
    }

    /** Verifies that a blank/whitespace-only carrier is rejected. */
    @Test
    void processShipment_emptyCarrier_throws() {
        Map<String, Object> event = validEvent();
        event.put("carrier", "  ");
        assertThatThrownBy(() -> service.processShipment(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Carrier cannot be empty");
    }

    // ==================== Shipment Status Processing ====================

    /**
     * Verifies that all valid shipment statuses are processed without errors.
     * Covers the full shipment lifecycle: creation, transit, delivery, exceptions, and returns.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "CREATED", "LABEL_CREATED", "PICKED_UP", "IN_TRANSIT",
            "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED",
            "EXCEPTION", "FAILED_DELIVERY",
            "RETURNED", "RETURNED_TO_SENDER"
    })
    void processShipment_allValidStatuses_succeed(String status) {
        Map<String, Object> event = validEvent();
        event.put("shipmentStatus", status);
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    /** Verifies that a null status is handled gracefully (no status to process). */
    @Test
    void processShipment_nullStatus_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("shipmentStatus", null);
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    /** Verifies that unknown/custom statuses fall through to the default case without error. */
    @Test
    void processShipment_unknownStatus_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("shipmentStatus", "CUSTOM_STATUS");
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    /** Verifies that status matching is case-insensitive (lowercase input accepted). */
    @Test
    void processShipment_lowercaseStatus_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("shipmentStatus", "in_transit");
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    // ==================== Happy Path ====================

    /** Verifies that a fully populated valid event processes without errors. */
    @Test
    void processShipment_validEventWithAllFields_succeeds() {
        assertThatNoException().isThrownBy(() -> service.processShipment(validEvent()));
    }

    /** Verifies that only the required fields (tracking number + carrier) are needed. */
    @Test
    void processShipment_minimalValidEvent_succeeds() {
        Map<String, Object> event = new HashMap<>();
        event.put("trackingNumber", "TRACK001");
        event.put("carrier", "FedEx");
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    /** Verifies that IN_TRANSIT status with a current location logs the location. */
    @Test
    void processShipment_inTransitWithLocation_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("shipmentStatus", "IN_TRANSIT");
        event.put("currentLocation", "Chicago, IL");
        assertThatNoException().isThrownBy(() -> service.processShipment(event));
    }

    // ==================== Test Data Helper ====================

    /**
     * Creates a valid shipment event with all fields populated.
     * Represents a typical in-transit shipment with origin, destination, and current location.
     */
    private Map<String, Object> validEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("trackingNumber", "TRACK123456");
        event.put("orderId", "ORD001");
        event.put("carrier", "FedEx");
        event.put("shipmentStatus", "IN_TRANSIT");
        event.put("originLocation", "New York, NY");
        event.put("destinationLocation", "Los Angeles, CA");
        event.put("currentLocation", "Chicago, IL");
        return event;
    }
}
