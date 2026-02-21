package com.supplychain.loader.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoaderServiceTest {

    private MockWebServer mockWebServer;
    private LoaderService service;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        // Remove trailing slash
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        service = new LoaderService(WebClient.builder());

        // Set all service URLs to point to mock server
        setField(service, "itemServiceUrl", baseUrl);
        setField(service, "tradeItemServiceUrl", baseUrl);
        setField(service, "supplierSupplyServiceUrl", baseUrl);
        setField(service, "shipmentServiceUrl", baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ==================== Event type extraction (explicit) ====================

    @Test
    void routeEvent_explicitItemType_routesToItemService() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK").setResponseCode(200));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "item");
        event.put("skuId", "SKU001");

        service.routeEvent(event);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/process");
    }

    @Test
    void routeEvent_explicitTradeItemType_routes() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "trade-item");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_explicitSupplierSupplyType_routes() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "supplier-supply");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_explicitShipmentType_routes() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "shipment");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_tradeitemVariant_routes() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "tradeitem");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    // ==================== Fallback field inference ====================

    @Test
    void routeEvent_noEventTypeWithGtin_infersTradeItem() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("gtin", "12345678901234");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_noEventTypeWithTrackingNumber_infersShipment() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("trackingNumber", "TRACK123");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_noEventTypeWithWarehouseId_infersSupplierSupply() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("warehouseId", "WH001");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void routeEvent_noEventTypeWithSkuId_infersItem() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("OK"));

        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");

        service.routeEvent(event);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    // ==================== Error handling ====================

    @Test
    void routeEvent_unknownEventType_throws() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "unknown");

        assertThatThrownBy(() -> service.routeEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown event type");
    }

    @Test
    void routeEvent_downstreamServerError_throws() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "item");

        assertThatThrownBy(() -> service.routeEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to route event");
    }

    // ==================== Helper ====================

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
