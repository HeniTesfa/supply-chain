package com.supplychain.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.item.service.ItemProcessingService;
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

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ItemProcessingService processingService;

    @Test
    void processItem_success_returnsOk() throws Exception {
        doNothing().when(processingService).processItem(any());

        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("itemName", "Laptop");

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Item processed successfully"));
    }

    @Test
    void processItem_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("SKU ID is required"))
                .when(processingService).processItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Laptop\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SKU ID is required")));
    }

    @Test
    void processItem_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("OSP API down"))
                .when(processingService).processItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("skuId", "SKU001"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("item-service"));
    }
}
