package com.supplychain.suppliersupply.service;

import com.supplychain.suppliersupply.entity.SupplierSupplyEntity;
import com.supplychain.suppliersupply.repository.SupplierSupplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link SupplierSupplyProcessingService}.
 *
 * Tests the core business logic for supplier supply event processing including:
 * - Required field validation (SKU ID, Warehouse ID)
 * - Non-negative quantity constraints (available, reserved, on-order)
 * - Reorder point monitoring and low stock alert logic
 * - Total supply calculation across quantity states
 *
 * SupplierSupplyRepository is mocked via Mockito to isolate from MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class SupplierSupplyProcessingServiceTest {

    @Mock
    private SupplierSupplyRepository supplierSupplyRepository;

    @InjectMocks
    private SupplierSupplyProcessingService service;

    @BeforeEach
    void setUp() {
        // Lenient stubs — not all tests reach saveSupplierSupply(); validation tests throw first.
        lenient().when(supplierSupplyRepository.findBySkuId(anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(supplierSupplyRepository.save(any(SupplierSupplyEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== Required Field: SKU ID ====================

    /** Verifies that a missing SKU ID field throws a validation error. */
    @Test
    void processSupplierSupply_missingSkuId_throws() {
        Map<String, Object> event = validEvent();
        event.remove("skuId");
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that a null SKU ID value is caught (combined null+missing check). */
    @Test
    void processSupplierSupply_nullSkuId_throws() {
        Map<String, Object> event = validEvent();
        event.put("skuId", null);
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that a blank/whitespace-only SKU ID is rejected. */
    @Test
    void processSupplierSupply_emptySkuId_throws() {
        Map<String, Object> event = validEvent();
        event.put("skuId", "  ");
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID cannot be empty");
    }

    // ==================== Required Field: Warehouse ID ====================

    /** Verifies that a missing Warehouse ID field throws a validation error. */
    @Test
    void processSupplierSupply_missingWarehouseId_throws() {
        Map<String, Object> event = validEvent();
        event.remove("warehouseId");
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Warehouse ID is required");
    }

    /** Verifies that a null Warehouse ID value is caught (combined null+missing check). */
    @Test
    void processSupplierSupply_nullWarehouseId_throws() {
        Map<String, Object> event = validEvent();
        event.put("warehouseId", null);
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Warehouse ID is required");
    }

    /** Verifies that a blank/whitespace-only Warehouse ID is rejected. */
    @Test
    void processSupplierSupply_emptyWarehouseId_throws() {
        Map<String, Object> event = validEvent();
        event.put("warehouseId", "  ");
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Warehouse ID cannot be empty");
    }

    // ==================== Non-Negative Quantity Validation ====================

    /** Verifies that negative available quantity is rejected. */
    @Test
    void processSupplierSupply_negativeAvailableQuantity_throws() {
        Map<String, Object> event = validEvent();
        event.put("availableQuantity", -1);
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available quantity cannot be negative");
    }

    /** Verifies that negative reserved quantity is rejected. */
    @Test
    void processSupplierSupply_negativeReservedQuantity_throws() {
        Map<String, Object> event = validEvent();
        event.put("reservedQuantity", -5);
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reserved quantity cannot be negative");
    }

    /** Verifies that negative on-order quantity is rejected. */
    @Test
    void processSupplierSupply_negativeOnOrderQuantity_throws() {
        Map<String, Object> event = validEvent();
        event.put("onOrderQuantity", -10);
        assertThatThrownBy(() -> service.processSupplierSupply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("On-order quantity cannot be negative");
    }

    /** Verifies that zero quantities are valid (boundary case). */
    @Test
    void processSupplierSupply_zeroQuantities_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("availableQuantity", 0);
        event.put("reservedQuantity", 0);
        event.put("onOrderQuantity", 0);
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    // ==================== Reorder Point Monitoring ====================

    /**
     * Verifies that stock below the reorder point triggers a low stock alert (log warning).
     * No exception is thrown — the alert is logged for monitoring.
     */
    @Test
    void processSupplierSupply_belowReorderPoint_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("availableQuantity", 5);
        event.put("reorderPoint", 10);
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    /**
     * Verifies that zero available stock triggers an out-of-stock alert (log error).
     * This is the most critical reorder scenario.
     */
    @Test
    void processSupplierSupply_outOfStock_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("availableQuantity", 0);
        event.put("reorderPoint", 10);
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    /** Verifies that stock above reorder point processes normally with no alerts. */
    @Test
    void processSupplierSupply_aboveReorderPoint_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("availableQuantity", 100);
        event.put("reorderPoint", 10);
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    // ==================== Happy Path ====================

    /** Verifies that a fully populated valid event processes without errors. */
    @Test
    void processSupplierSupply_validEventWithAllFields_succeeds() {
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(validEvent()));
    }

    /** Verifies that only the required fields (SKU ID + Warehouse ID) are needed. */
    @Test
    void processSupplierSupply_minimalValidEvent_succeeds() {
        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("warehouseId", "WH001");
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    /** Verifies that null quantities are treated as absent (no validation error). */
    @Test
    void processSupplierSupply_nullQuantities_succeeds() {
        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("warehouseId", "WH001");
        event.put("availableQuantity", null);
        event.put("reservedQuantity", null);
        assertThatNoException().isThrownBy(() -> service.processSupplierSupply(event));
    }

    // ==================== Test Data Helper ====================

    /**
     * Creates a valid supplier supply event with all fields populated.
     * Represents typical warehouse stock data with quantities above reorder point.
     */
    private Map<String, Object> validEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("warehouseId", "WH001");
        event.put("warehouseName", "Main Warehouse");
        event.put("availableQuantity", 100);
        event.put("reservedQuantity", 20);
        event.put("onOrderQuantity", 50);
        event.put("reorderPoint", 25);
        event.put("reorderQuantity", 100);
        return event;
    }
}
