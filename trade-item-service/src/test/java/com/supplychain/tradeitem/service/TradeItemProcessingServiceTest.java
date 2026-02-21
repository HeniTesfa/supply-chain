package com.supplychain.tradeitem.service;

import com.supplychain.tradeitem.entity.TradeItemEntity;
import com.supplychain.tradeitem.repository.TradeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link TradeItemProcessingService}.
 *
 * Tests the core business logic for trade item event processing including:
 * - GTIN format validation (8, 12, 13, or 14 digit formats)
 * - Required field validation (GTIN, SKU ID)
 * - Minimum order quantity constraints (must be >= 1)
 * - Lead time validation (must be >= 0)
 * - Supplier and ordering info processing
 *
 * TradeItemRepository is mocked via Mockito to isolate from MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class TradeItemProcessingServiceTest {

    @Mock
    private TradeItemRepository tradeItemRepository;

    @InjectMocks
    private TradeItemProcessingService service;

    @BeforeEach
    void setUp() {
        // Lenient stubs — not all tests reach saveTradeItem(); validation tests throw first.
        lenient().when(tradeItemRepository.findByGtin(anyString())).thenReturn(Optional.empty());
        lenient().when(tradeItemRepository.save(any(TradeItemEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== GTIN Format Validation ====================

    /**
     * Verifies that all valid GTIN lengths (8, 12, 13, 14 digits) are accepted.
     * GTIN-8 is used for small items, GTIN-12 (UPC), GTIN-13 (EAN), GTIN-14 for cases/pallets.
     */
    @ParameterizedTest
    @ValueSource(strings = {"12345678", "123456789012", "1234567890123", "12345678901234"})
    void processTradeItem_validGtinLengths_succeeds(String gtin) {
        Map<String, Object> event = validEvent();
        event.put("gtin", gtin);
        assertThatNoException().isThrownBy(() -> service.processTradeItem(event));
    }

    /** Verifies that a missing GTIN field throws with a descriptive message. */
    @Test
    void processTradeItem_missingGtin_throws() {
        Map<String, Object> event = validEvent();
        event.remove("gtin");
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GTIN is required");
    }

    /** Verifies that a null GTIN value is caught during validation. */
    @Test
    void processTradeItem_nullGtin_throws() {
        Map<String, Object> event = validEvent();
        event.put("gtin", null);
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GTIN cannot be empty");
    }

    /** Verifies that a blank/whitespace-only GTIN is rejected. */
    @Test
    void processTradeItem_emptyGtin_throws() {
        Map<String, Object> event = validEvent();
        event.put("gtin", "  ");
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GTIN cannot be empty");
    }

    /**
     * Verifies that invalid GTIN formats are rejected. Valid GTINs must be exactly
     * 8, 12, 13, or 14 numeric digits. These test values fall outside those lengths
     * or contain non-digit characters.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "12345678901", "123456789012345", "ABCDEFGH"})
    void processTradeItem_invalidGtinFormat_throws(String gtin) {
        Map<String, Object> event = validEvent();
        event.put("gtin", gtin);
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GTIN must be 8, 12, 13, or 14 digits");
    }

    // ==================== SKU ID Validation ====================

    /** Verifies that SKU ID is a required field for trade item processing. */
    @Test
    void processTradeItem_missingSkuId_throws() {
        Map<String, Object> event = validEvent();
        event.remove("skuId");
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    // ==================== Min Order Quantity Validation ====================

    /** Verifies that a minimum order quantity of zero is rejected (must be >= 1). */
    @Test
    void processTradeItem_minOrderQuantityZero_throws() {
        Map<String, Object> event = validEvent();
        event.put("minOrderQuantity", 0);
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min order quantity must be at least 1");
    }

    /** Verifies that negative minimum order quantities are rejected. */
    @Test
    void processTradeItem_minOrderQuantityNegative_throws() {
        Map<String, Object> event = validEvent();
        event.put("minOrderQuantity", -5);
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min order quantity must be at least 1");
    }

    /** Verifies that the boundary value of 1 is accepted for minimum order quantity. */
    @Test
    void processTradeItem_minOrderQuantityOne_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("minOrderQuantity", 1);
        assertThatNoException().isThrownBy(() -> service.processTradeItem(event));
    }

    // ==================== Lead Time Validation ====================

    /** Verifies that negative lead time values are rejected. */
    @Test
    void processTradeItem_leadTimeNegative_throws() {
        Map<String, Object> event = validEvent();
        event.put("leadTimeDays", -1);
        assertThatThrownBy(() -> service.processTradeItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lead time cannot be negative");
    }

    /** Verifies that zero lead time is accepted (same-day supply). */
    @Test
    void processTradeItem_leadTimeZero_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("leadTimeDays", 0);
        assertThatNoException().isThrownBy(() -> service.processTradeItem(event));
    }

    /** Verifies that long lead times (>30 days) are accepted but trigger a warning log. */
    @Test
    void processTradeItem_longLeadTime_succeeds() {
        Map<String, Object> event = validEvent();
        event.put("leadTimeDays", 60);
        assertThatNoException().isThrownBy(() -> service.processTradeItem(event));
    }

    // ==================== Happy Path ====================

    /** Verifies that a fully populated valid event processes without errors. */
    @Test
    void processTradeItem_validEventWithAllFields_succeeds() {
        assertThatNoException().isThrownBy(() -> service.processTradeItem(validEvent()));
    }

    /** Verifies that only the required fields (GTIN + SKU ID) are needed to process. */
    @Test
    void processTradeItem_minimalValidEvent_succeeds() {
        Map<String, Object> event = new HashMap<>();
        event.put("gtin", "12345678");
        event.put("skuId", "SKU001");
        assertThatNoException().isThrownBy(() -> service.processTradeItem(event));
    }

    // ==================== Test Data Helper ====================

    /**
     * Creates a valid trade item event with all fields populated.
     * Uses a 14-digit GTIN, standard supplier info, and reasonable ordering params.
     */
    private Map<String, Object> validEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("gtin", "12345678901234");
        event.put("skuId", "SKU001");
        event.put("supplierId", "SUP001");
        event.put("supplierName", "ABC Supplier");
        event.put("description", "Test Item");
        event.put("unitOfMeasure", "EACH");
        event.put("minOrderQuantity", 10);
        event.put("leadTimeDays", 7);
        return event;
    }
}
