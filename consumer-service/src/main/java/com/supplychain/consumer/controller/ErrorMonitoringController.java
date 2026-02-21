// Package declaration: this class belongs to the controller sub-package of the consumer service
package com.supplychain.consumer.controller;

// Import the ErrorLog entity class, which represents an error log document stored in MongoDB
import com.supplychain.consumer.entity.ErrorLog;

// Import the ErrorLogRepository interface, which provides database operations for ErrorLog documents
import com.supplychain.consumer.repository.ErrorLogRepository;

// Import the @Autowired annotation, which enables automatic dependency injection by Spring
import org.springframework.beans.factory.annotation.Autowired;

// Import ResponseEntity, which represents an HTTP response including status code, headers, and body
import org.springframework.http.ResponseEntity;

// Import REST controller annotations for defining HTTP endpoints and request mappings
// This wildcard import includes @RestController, @RequestMapping, @GetMapping, @PutMapping,
// @PathVariable, @RequestBody, and @CrossOrigin
import org.springframework.web.bind.annotation.*;

// Import List collection type, used to return multiple error log entries in API responses
import java.util.List;

// Import Map collection type, used for accepting JSON request bodies and returning stats as key-value pairs
import java.util.Map;

// @RestController combines @Controller and @ResponseBody, meaning all methods return data directly (as JSON) rather than view names
@RestController
// @RequestMapping sets the base URL path for all endpoints in this controller to "/api/errors"
@RequestMapping("/api/errors")
// @CrossOrigin allows requests from any origin (domain), enabling the monitoring-ui React frontend on port 3001 to call these endpoints
@CrossOrigin(origins = "*")
public class ErrorMonitoringController {

    // @Autowired tells Spring to inject the ErrorLogRepository bean into this field automatically at runtime
    // This repository provides methods to query and manipulate error log documents in MongoDB
    @Autowired
    private ErrorLogRepository errorLogRepository;

    // @GetMapping maps HTTP GET requests to "/api/errors/unresolved" to this method
    // This endpoint retrieves all error logs that have not yet been marked as resolved
    @GetMapping("/unresolved")
    public ResponseEntity<List<ErrorLog>> getUnresolvedErrors() {
        // Call the repository method that queries MongoDB for all ErrorLog documents where resolved == false
        List<ErrorLog> errors = errorLogRepository.findByResolvedFalse();
        // Wrap the list of unresolved errors in a ResponseEntity with HTTP 200 OK status and return it as JSON
        return ResponseEntity.ok(errors);
    }

    // @GetMapping maps HTTP GET requests to "/api/errors/stage/{stage}" where {stage} is a path variable
    // This endpoint filters unresolved errors by their failure stage (e.g., DB_SAVE, OSP_API, LOADER_SERVICE)
    @GetMapping("/stage/{stage}")
    // @PathVariable binds the {stage} URL segment to the stage method parameter
    public ResponseEntity<List<ErrorLog>> getErrorsByStage(@PathVariable String stage) {
        // Query MongoDB for unresolved errors that match the specified failure stage
        // The repository method name follows Spring Data's naming convention to auto-generate the query
        List<ErrorLog> errors = errorLogRepository.findByFailureStageAndResolvedFalse(stage);
        // Return the filtered list of errors with HTTP 200 OK status
        return ResponseEntity.ok(errors);
    }

    // @GetMapping maps HTTP GET requests to "/api/errors/type/{eventType}" where {eventType} is a path variable
    // This endpoint filters unresolved errors by their event type (e.g., item, trade-item, shipment, supplier-supply)
    @GetMapping("/type/{eventType}")
    // @PathVariable binds the {eventType} URL segment to the eventType method parameter
    public ResponseEntity<List<ErrorLog>> getErrorsByEventType(@PathVariable String eventType) {
        // Query MongoDB for unresolved errors that match the specified event type
        List<ErrorLog> errors = errorLogRepository.findByEventTypeAndResolvedFalse(eventType);
        // Return the filtered list of errors with HTTP 200 OK status
        return ResponseEntity.ok(errors);
    }

    // @PutMapping maps HTTP PUT requests to "/api/errors/{id}/resolve" where {id} is the error log's MongoDB document ID
    // This endpoint marks a specific error as resolved, recording who resolved it and any notes about the resolution
    @PutMapping("/{id}/resolve")
    public ResponseEntity<String> resolveError(
            // @PathVariable extracts the {id} value from the URL path and binds it to the id parameter
            @PathVariable String id,
            // @RequestBody tells Spring to deserialize the incoming JSON body into a Map of String key-value pairs
            @RequestBody Map<String, String> request) {

        // Look up the error log by its ID in MongoDB; throw a RuntimeException if no document with that ID exists
        ErrorLog errorLog = errorLogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Error not found"));

        // Set the resolved flag to true, indicating this error has been addressed
        errorLog.setResolved(true);
        // Record the timestamp of when the error was resolved using the current date and time
        errorLog.setResolvedAt(java.time.LocalDateTime.now());
        // Extract the "resolvedBy" field from the request body and set it on the error log (identifies who resolved it)
        errorLog.setResolvedBy(request.get("resolvedBy"));
        // Extract the "notes" field from the request body and set it as the resolution notes on the error log
        errorLog.setResolutionNotes(request.get("notes"));

        // Save the updated error log document back to MongoDB, persisting all the resolution details
        errorLogRepository.save(errorLog);

        // Return a plain text confirmation message with HTTP 200 OK status
        return ResponseEntity.ok("Error marked as resolved");
    }

    // @GetMapping maps HTTP GET requests to "/api/errors/stats" to this method
    // This endpoint returns aggregate statistics about errors: total count, unresolved count, and resolved count
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getErrorStats() {
        // Count the total number of error log documents in MongoDB using the built-in count() method
        long totalErrors = errorLogRepository.count();
        // Get the count of unresolved errors by fetching all unresolved errors and getting the list size
        long unresolvedErrors = errorLogRepository.findByResolvedFalse().size();

        // Build an immutable map containing the three statistics: total, unresolved, and resolved (calculated by subtraction)
        // Map.of() creates an unmodifiable map with the given key-value pairs
        Map<String, Object> stats = Map.of(
            "totalErrors", totalErrors,                        // Total number of error logs ever recorded
            "unresolvedErrors", unresolvedErrors,              // Number of errors still pending resolution
            "resolvedErrors", totalErrors - unresolvedErrors   // Number of errors that have been resolved (derived value)
        );

        // Return the statistics map as JSON with HTTP 200 OK status
        return ResponseEntity.ok(stats);
    }
}
