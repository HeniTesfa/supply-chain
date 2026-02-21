package com.supplychain.shipment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.shipment.service.ShipmentProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ShipmentProcessingService processingService;

    @Test
    void processShipment_success_returnsOk() throws Exception {
        doNothing().when(processingService).processShipment(any());

        Map<String, Object> event = new HashMap<>();
        event.put("trackingNumber", "TRACK123");
        event.put("carrier", "FedEx");
        event.put("shipmentStatus", "IN_TRANSIT");

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

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("shipment-service"));
    }
}
