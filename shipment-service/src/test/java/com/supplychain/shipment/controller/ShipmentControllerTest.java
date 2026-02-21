package com.supplychain.shipment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.shipment.entity.ShipmentEntity;
import com.supplychain.shipment.repository.ShipmentRepository;
import com.supplychain.shipment.service.ShipmentProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ShipmentProcessingService processingService;
    @MockBean private ShipmentRepository shipmentRepository;

    // ── POST /api/process ─────────────────────────────────────────────────────

    @Test
    void processShipment_success_returnsOk() throws Exception {
        doNothing().when(processingService).processShipment(any());

        Map<String, Object> event = Map.of(
                "trackingNumber", "TRACK123",
                "carrier", "FedEx",
                "shipmentStatus", "IN_TRANSIT");

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Shipment processed successfully"));
    }

    @Test
    void processShipment_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("Tracking number is required"))
                .when(processingService).processShipment(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrier\":\"FedEx\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tracking number is required")));
    }

    @Test
    void processShipment_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected error"))
                .when(processingService).processShipment(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trackingNumber", "T1"))))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /api/shipments ────────────────────────────────────────────────────

    @Test
    void getAllShipments_returnsShipmentList() throws Exception {
        ShipmentEntity shipment = ShipmentEntity.builder()
                .id("1").trackingNumber("TRACK123").carrier("FedEx").shipmentStatus("IN_TRANSIT").build();
        when(shipmentRepository.findAll()).thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber").value("TRACK123"))
                .andExpect(jsonPath("$[0].carrier").value("FedEx"));
    }

    @Test
    void getAllShipments_emptyList_returnsEmptyArray() throws Exception {
        when(shipmentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/shipments/{id} ───────────────────────────────────────────────

    @Test
    void getShipmentById_found_returnsShipment() throws Exception {
        ShipmentEntity shipment = ShipmentEntity.builder().id("1").trackingNumber("TRACK123").carrier("FedEx").build();
        when(shipmentRepository.findById("1")).thenReturn(Optional.of(shipment));

        mockMvc.perform(get("/api/shipments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123"));
    }

    @Test
    void getShipmentById_notFound_returns404() throws Exception {
        when(shipmentRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/shipments/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/shipments/tracking/{trackingNumber} ──────────────────────────

    @Test
    void getShipmentByTrackingNumber_found_returnsShipment() throws Exception {
        ShipmentEntity shipment = ShipmentEntity.builder().id("1").trackingNumber("TRACK123").carrier("FedEx").build();
        when(shipmentRepository.findByTrackingNumber("TRACK123")).thenReturn(Optional.of(shipment));

        mockMvc.perform(get("/api/shipments/tracking/TRACK123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123"));
    }

    @Test
    void getShipmentByTrackingNumber_notFound_returns404() throws Exception {
        when(shipmentRepository.findByTrackingNumber("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/shipments/tracking/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/shipments/status/{status} ────────────────────────────────────

    @Test
    void getShipmentsByStatus_returnsMatchingShipments() throws Exception {
        ShipmentEntity shipment = ShipmentEntity.builder()
                .id("1").trackingNumber("TRACK123").shipmentStatus("IN_TRANSIT").build();
        when(shipmentRepository.findByShipmentStatus("IN_TRANSIT")).thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/shipments/status/IN_TRANSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shipmentStatus").value("IN_TRANSIT"));
    }

    @Test
    void getShipmentsByStatus_noMatches_returnsEmptyArray() throws Exception {
        when(shipmentRepository.findByShipmentStatus("RETURNED")).thenReturn(List.of());

        mockMvc.perform(get("/api/shipments/status/RETURNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/shipments/order/{orderId} ────────────────────────────────────

    @Test
    void getShipmentsByOrderId_returnsMatchingShipments() throws Exception {
        ShipmentEntity shipment = ShipmentEntity.builder()
                .id("1").trackingNumber("TRACK123").orderId("ORD001").build();
        when(shipmentRepository.findByOrderId("ORD001")).thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/shipments/order/ORD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("ORD001"));
    }

    @Test
    void getShipmentsByOrderId_noMatches_returnsEmptyArray() throws Exception {
        when(shipmentRepository.findByOrderId("ORD999")).thenReturn(List.of());

        mockMvc.perform(get("/api/shipments/order/ORD999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/health ───────────────────────────────────────────────────────

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("shipment-service"));
    }
}
