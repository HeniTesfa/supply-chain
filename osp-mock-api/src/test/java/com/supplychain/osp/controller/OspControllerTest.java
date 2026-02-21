package com.supplychain.osp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.osp.service.OspMockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OspController.class)
class OspControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OspMockService ospMockService;

    @Test
    void getItem_existingSku_returnsItem() throws Exception {
        Map<String, Object> item = Map.of("skuId", "SKU001", "itemName", "Laptop");
        when(ospMockService.getItemData("SKU001")).thenReturn(item);

        mockMvc.perform(get("/osp/api/items/SKU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value("SKU001"))
                .andExpect(jsonPath("$.itemName").value("Laptop"));
    }

    @Test
    void getItem_nonExistentSku_returns404() throws Exception {
        when(ospMockService.getItemData("NONEXISTENT"))
                .thenThrow(new RuntimeException("Item not found"));

        mockMvc.perform(get("/osp/api/items/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrUpdateItem_validData_returnsSuccess() throws Exception {
        Map<String, Object> response = Map.of("success", true, "skuId", "SKU001");
        when(ospMockService.createOrUpdateItem(any())).thenReturn(response);

        Map<String, Object> item = new HashMap<>();
        item.put("skuId", "SKU001");
        item.put("itemName", "Laptop");

        mockMvc.perform(post("/osp/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createOrUpdateItem_invalidData_returns400() throws Exception {
        when(ospMockService.createOrUpdateItem(any()))
                .thenThrow(new IllegalArgumentException("SKU ID is required"));

        mockMvc.perform(post("/osp/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Laptop\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SKU ID is required"));
    }

    @Test
    void health_returnsUpStatus() throws Exception {
        when(ospMockService.getItemCount()).thenReturn(5);

        mockMvc.perform(get("/osp/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("osp-mock-api"))
                .andExpect(jsonPath("$.itemCount").value("5"));
    }

    @Test
    void getItemError_returnsServerError() throws Exception {
        mockMvc.perform(get("/osp/api/items/SKU001/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Simulated OSP API error"));
    }
}
