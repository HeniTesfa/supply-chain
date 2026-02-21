package com.supplychain.osp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link OspMockService}.
 *
 * Tests the mock OSP (Order Service Provider) API behavior including:
 * - Pre-populated test data initialization (5 sample items)
 * - Item retrieval by SKU ID (CRUD operations on ConcurrentHashMap)
 * - Item creation and update with metadata enrichment
 * - Input validation for SKU ID (null/empty checks)
 * - Clear all items functionality
 *
 * This is a pure unit test — no Spring context needed since the service
 * uses in-memory ConcurrentHashMap storage with no external dependencies.
 */
class OspMockServiceTest {

    private OspMockService service;

    @BeforeEach
    void setUp() {
        service = new OspMockService();
    }

    // ==================== Pre-populated Test Data ====================

    /** Verifies that the constructor initializes exactly 5 sample items (SKU001-SKU005). */
    @Test
    void constructor_initializesTestData() {
        assertThat(service.getItemCount()).isEqualTo(5);
    }

    /** Verifies that pre-populated SKU001 has the expected fields (Laptop, Electronics, $999.99). */
    @Test
    void getItemData_prePopulatedSKU001_returnsLaptop() {
        Map<String, Object> item = service.getItemData("SKU001");
        assertThat(item.get("itemName")).isEqualTo("Laptop");
        assertThat(item.get("category")).isEqualTo("Electronics");
        assertThat(item.get("price")).isEqualTo(999.99);
    }

    /** Verifies that all 5 pre-populated items (SKU001-SKU005) are retrievable. */
    @Test
    void getItemData_allPrePopulatedItems_exist() {
        assertThatNoException().isThrownBy(() -> service.getItemData("SKU001"));
        assertThatNoException().isThrownBy(() -> service.getItemData("SKU002"));
        assertThatNoException().isThrownBy(() -> service.getItemData("SKU003"));
        assertThatNoException().isThrownBy(() -> service.getItemData("SKU004"));
        assertThatNoException().isThrownBy(() -> service.getItemData("SKU005"));
    }

    // ==================== Get Item ====================

    /** Verifies that retrieving a non-existent SKU throws a RuntimeException. */
    @Test
    void getItemData_nonExistentSku_throws() {
        assertThatThrownBy(() -> service.getItemData("NONEXISTENT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Item not found");
    }

    // ==================== Create/Update Item ====================

    /**
     * Verifies that creating a new item stores it in memory and returns a success response.
     * Also checks that OSP metadata (lastUpdated, ospApiVersion) is automatically added.
     */
    @Test
    void createOrUpdateItem_newItem_storesAndReturnsSuccess() {
        Map<String, Object> item = new HashMap<>();
        item.put("skuId", "SKU999");
        item.put("itemName", "New Item");

        Map<String, Object> response = service.createOrUpdateItem(item);

        assertThat(response.get("success")).isEqualTo(true);
        assertThat(response.get("skuId")).isEqualTo("SKU999");
        assertThat(service.getItemCount()).isEqualTo(6);

        // Verify stored item has OSP metadata
        Map<String, Object> stored = service.getItemData("SKU999");
        assertThat(stored.get("itemName")).isEqualTo("New Item");
        assertThat(stored).containsKey("lastUpdated");
        assertThat(stored.get("ospApiVersion")).isEqualTo("1.0");
    }

    /** Verifies that a null SKU ID throws an IllegalArgumentException. */
    @Test
    void createOrUpdateItem_nullSkuId_throws() {
        Map<String, Object> item = new HashMap<>();
        item.put("skuId", null);
        assertThatThrownBy(() -> service.createOrUpdateItem(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that a blank/whitespace-only SKU ID throws an IllegalArgumentException. */
    @Test
    void createOrUpdateItem_emptySkuId_throws() {
        Map<String, Object> item = new HashMap<>();
        item.put("skuId", "  ");
        assertThatThrownBy(() -> service.createOrUpdateItem(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that updating an existing item overwrites the data without changing the count. */
    @Test
    void createOrUpdateItem_existingItem_updates() {
        Map<String, Object> update = new HashMap<>();
        update.put("skuId", "SKU001");
        update.put("itemName", "Updated Laptop");

        service.createOrUpdateItem(update);

        Map<String, Object> stored = service.getItemData("SKU001");
        assertThat(stored.get("itemName")).isEqualTo("Updated Laptop");
        assertThat(service.getItemCount()).isEqualTo(5); // count unchanged
    }

    // ==================== Clear All ====================

    /** Verifies that clearAllItems removes all entries from storage. */
    @Test
    void clearAllItems_removesEverything() {
        assertThat(service.getItemCount()).isGreaterThan(0);
        service.clearAllItems();
        assertThat(service.getItemCount()).isEqualTo(0);
    }

    // ==================== Get All ====================

    /** Verifies that getAllItems returns all stored items as a list. */
    @Test
    void getAllItems_returnsAllStoredItems() {
        List<Map<String, Object>> items = service.getAllItems();
        assertThat(items).hasSize(5);
    }
}
