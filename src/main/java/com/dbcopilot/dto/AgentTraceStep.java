package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed execution trace step captured across the multi-agent workflow")
public class AgentTraceStep {

    @Schema(description = "Sequential step order", example = "1")
    private int stepNumber;

    @Schema(description = "Name of the agent or tool executing the step", example = "SemanticCacheAgent")
    private String agentName;

    @Schema(description = "Action performed (e.g. CACHE_LOOKUP, SCHEMA_INSPECTION, SQL_GENERATION, SELF_CORRECTION, QUERY_EXECUTION, OPTIMIZATION)", example = "CACHE_LOOKUP")
    private String action;

    @Schema(description = "Status of the step: SUCCESS, FAILED, RETRYING, SKIPPED", example = "SUCCESS")
    private String status;

    @Schema(description = "Human-readable summary of what occurred", example = "Cache miss. Cosine similarity 0.72 < threshold 0.88.")
    private String description;

    @Schema(description = "Input or prompt provided to this agent/tool")
    private String input;

    @Schema(description = "Output or generated code / result from this agent/tool")
    private String output;

    @Schema(description = "Execution duration for this specific step in milliseconds", example = "42")
    private long durationMs;

    @Schema(description = "Additional metadata or diagnostic properties")
    private Map<String, Object> metadata;

    @Builder.Default
    @Schema(description = "Timestamp when the trace step occurred")
    private Instant timestamp = Instant.now();
}
