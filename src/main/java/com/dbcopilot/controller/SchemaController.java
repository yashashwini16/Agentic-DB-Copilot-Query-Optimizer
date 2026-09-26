package com.dbcopilot.controller;

import com.dbcopilot.dto.SchemaMetadataDto;
import com.dbcopilot.service.DatabaseSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
@Tag(name = "Schema Explorer", description = "Endpoints for database schema reflection and LLM prompt context inspection")
@CrossOrigin(origins = "*")
public class SchemaController {

    private final DatabaseSchemaService schemaService;

    @GetMapping
    @Operation(summary = "Get complete metadata for tables, columns, primary keys, and foreign keys")
    public ResponseEntity<SchemaMetadataDto> getSchema() {
        return ResponseEntity.ok(schemaService.getSchemaMetadata());
    }

    @GetMapping("/prompt-context")
    @Operation(summary = "Get the dynamic LLM schema context string injected into system prompts")
    public ResponseEntity<Map<String, String>> getSchemaPromptContext() {
        return ResponseEntity.ok(Map.of("context", schemaService.getSchemaPromptContext()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Invalidate and refresh cached schema metadata")
    public ResponseEntity<Map<String, String>> refreshSchema() {
        schemaService.invalidateCache();
        return ResponseEntity.ok(Map.of("message", "Schema metadata refreshed successfully"));
    }
}
