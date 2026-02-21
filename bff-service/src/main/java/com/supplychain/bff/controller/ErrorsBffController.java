package com.supplychain.bff.controller;

import com.supplychain.bff.service.ConsumerServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
@CrossOrigin(origins = "*")
public class ErrorsBffController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorsBffController.class);

    @Autowired
    private ConsumerServiceClient consumerServiceClient;

    @GetMapping("/unresolved")
    public ResponseEntity<?> getUnresolvedErrors() {
        try {
            List<Map> errors = consumerServiceClient.getUnresolvedErrors();
            return ResponseEntity.ok(errors);
        } catch (Exception e) {
            logger.error("Error proxying unresolved errors request: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getErrorStats() {
        try {
            Map<String, Object> stats = consumerServiceClient.getErrorStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error proxying error stats request: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stage/{stage}")
    public ResponseEntity<?> getErrorsByStage(@PathVariable String stage) {
        try {
            List<Map> errors = consumerServiceClient.getErrorsByStage(stage);
            return ResponseEntity.ok(errors);
        } catch (Exception e) {
            logger.error("Error proxying errors by stage request: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<?> getErrorsByType(@PathVariable String eventType) {
        try {
            List<Map> errors = consumerServiceClient.getErrorsByType(eventType);
            return ResponseEntity.ok(errors);
        } catch (Exception e) {
            logger.error("Error proxying errors by type request: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveError(@PathVariable String id, @RequestBody Map<String, Object> resolution) {
        try {
            String result = consumerServiceClient.resolveError(id, resolution);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error proxying resolve error request: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
