package com.supplychain.item.service;

import com.supplychain.item.repository.ItemRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ItemProcessingService}.
 *
 * Tests the core business logic for item event processing including:
 * - Required field validation (SKU ID, item name)
 * - Non-negative constraints for price and weight
 * - OSP API integration via WebClient with retry logic
 * - Exponential backoff retry behavior (2^n * 1000ms)
 * - Behavior when all retries are exhausted
 *
 * Uses MockWebServer to simulate the external OSP API without real HTTP calls.
 * ItemRepository is mocked via Mockito to isolate from MongoDB.
 */
class ItemProcessingServiceTest {

    private MockWebServer mockWebServer;
    private ItemProcessingService service;
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        itemRepository = Mockito.mock(ItemRepository.class);
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new ItemProcessingService(WebClient.builder());
        setField(service, "ospApiUrl", baseUrl);
        setField(service, "maxRetryAttempts", 3);
        setField(service, "itemRepository", itemRepository);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ==================== Validation: SKU ID ====================

    /** Verifies that a missing SKU ID field throws a validation error. */
    @Test
    void processItem_missingSkuId_throws() {
        Map<String, Object> event = validEvent();
        event.remove("skuId");
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that a null SKU ID value is caught during validation. */
    @Test
    void processItem_nullSkuId_throws() {
        Map<String, Object> event = validEvent();
        event.put("skuId", null);
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID is required");
    }

    /** Verifies that a blank/whitespace-only SKU ID is rejected. */
    @Test
    void processItem_emptySkuId_throws() {
        Map<String, Object> event = validEvent();
        event.put("skuId", "  ");
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU ID cannot be empty");
    }

    // ==================== Validation: Item Name ====================

    /** Verifies that a missing item name throws a validation error. */
    @Test
    void processItem_missingItemName_throws() {
        Map<String, Object> event = validEvent();
        event.remove("itemName");
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item name is required");
    }

    // ==================== Validation: Price and Weight ====================

    /** Verifies that a negative price is rejected. */
    @Test
    void processItem_negativePrice_throws() {
        Map<String, Object> event = validEvent();
        event.put("price", -10.0);
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price cannot be negative");
    }

    /** Verifies that a negative weight is rejected. */
    @Test
    void processItem_negativeWeight_throws() {
        Map<String, Object> event = validEvent();
        event.put("weight", -1.0);
        assertThatThrownBy(() -> service.processItem(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weight cannot be negative");
    }

    /** Verifies that zero price and weight are valid (boundary case). */
    @Test
    void processItem_zeroPriceAndWeight_succeeds() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"success\":true}").setResponseCode(200));
        Map<String, Object> event = validEvent();
        event.put("price", 0.0);
        event.put("weight", 0.0);
        assertThatNoException().isThrownBy(() -> service.processItem(event));
    }

    // ==================== OSP API Call ====================

    /** Verifies that a successful OSP API call completes without errors. */
    @Test
    void processItem_ospApiSuccess_completesSuccessfully() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"success\":true}").setResponseCode(200));

        assertThatNoException().isThrownBy(() -> service.processItem(validEvent()));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    // ==================== Retry Logic ====================

    /**
     * Verifies exponential backoff retry: first attempt fails (500), second succeeds.
     * The service should make exactly 2 requests to the mock server.
     */
    @Test
    void processItem_ospApiFailsThenSucceeds_retries() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        mockWebServer.enqueue(new MockResponse().setBody("{\"success\":true}").setResponseCode(200));

        setFieldSafe(service, "maxRetryAttempts", 2);

        assertThatNoException().isThrownBy(() -> service.processItem(validEvent()));
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    /**
     * Verifies that after all retry attempts are exhausted (3 failures),
     * a RuntimeException is thrown with the attempt count.
     */
    @Test
    void processItem_ospApiAllRetriesFail_throwsAfterMaxAttempts() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        setFieldSafe(service, "maxRetryAttempts", 3);

        assertThatThrownBy(() -> service.processItem(validEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send to OSP API after 3 attempts");
    }

    /** Verifies that with maxRetryAttempts=1, only one attempt is made before failing. */
    @Test
    void processItem_singleRetryAllowed_failsAfterOneAttempt() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        setFieldSafe(service, "maxRetryAttempts", 1);

        assertThatThrownBy(() -> service.processItem(validEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send to OSP API after 1 attempts");
    }

    // ==================== Optional Fields ====================

    /** Verifies that events without price or weight fields are processed successfully. */
    @Test
    void processItem_noPriceOrWeight_succeeds() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"success\":true}").setResponseCode(200));
        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("itemName", "Basic Item");
        assertThatNoException().isThrownBy(() -> service.processItem(event));
    }

    // ==================== Test Helpers ====================

    /**
     * Creates a valid item event with all fields populated.
     * Represents a typical electronics item with price and weight.
     */
    private Map<String, Object> validEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("itemName", "Laptop");
        event.put("category", "Electronics");
        event.put("price", 999.99);
        event.put("weight", 2.5);
        return event;
    }

    /** Sets a private field value on the target object using reflection. */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** Sets a private field value, wrapping checked exceptions in RuntimeException. */
    private void setFieldSafe(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
