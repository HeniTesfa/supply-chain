package com.supplychain.tradeitem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.tradeitem.service.TradeItemProcessingService;
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

@WebMvcTest(TradeItemController.class)
class TradeItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TradeItemProcessingService processingService;

    @Test
    void processTradeItem_success_returnsOk() throws Exception {
        doNothing().when(processingService).processTradeItem(any());

        Map<String, Object> event = new HashMap<>();
        event.put("gtin", "12345678901234");
        event.put("skuId", "SKU001");

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Trade item processed successfully"));
    }

    @Test
    void processTradeItem_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("GTIN is required"))
                .when(processingService).processTradeItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":\"SKU001\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GTIN is required")));
    }

    @Test
    void processTradeItem_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected error"))
                .when(processingService).processTradeItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gtin", "12345678"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("trade-item-service"));
    }
}
