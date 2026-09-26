package com.dbcopilot.controller;

import com.dbcopilot.dto.CacheEntryDto;
import com.dbcopilot.dto.CacheMetricsDto;
import com.dbcopilot.service.SemanticCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "Semantic Cache Analytics", description = "Endpoints for inspecting Pgvector vector caching, hit rates, and latency metrics")
@CrossOrigin(origins = "*")
public class CacheController {

    private final SemanticCacheService semanticCacheService;

    @GetMapping("/metrics")
    @Operation(summary = "Get aggregated cache performance metrics (hit rate, latency reduction %, token savings)")
    public ResponseEntity<CacheMetricsDto> getMetrics() {
        return ResponseEntity.ok(semanticCacheService.getMetrics());
    }

    @GetMapping("/entries")
    @Operation(summary = "List all cached natural queries and their verified SQL mappings")
    public ResponseEntity<List<CacheEntryDto>> getEntries() {
        return ResponseEntity.ok(semanticCacheService.getAllEntries());
    }

    @DeleteMapping
    @Operation(summary = "Clear all semantic cache entries and in-memory vectors")
    public ResponseEntity<Map<String, String>> clearCache() {
        semanticCacheService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Semantic cache cleared successfully"));
    }
}
