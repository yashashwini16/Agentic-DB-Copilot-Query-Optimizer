package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete Copilot response containing generated SQL, execution results, trace, and cache stats")
public class QueryResponse {

    @Schema(description = "Original natural language user prompt", example = "Top 5 customers by revenue")
    private String userPrompt;

    @Schema(description = "Generated, verified, and executed PostgreSQL SQL query")
    private String generatedSql;

    @Schema(description = "Natural language explanation of the query logic and business context")
    private String explanation;

    @Schema(description = "Indicates whether the response was served directly from Pgvector semantic cache", example = "false")
    private boolean cached;

    @Schema(description = "Cosine similarity score if matched in semantic cache (0.0 to 1.0)", example = "0.94")
    private Double cacheSimilarity;

    @Schema(description = "Total roundtrip latency in milliseconds", example = "128")
    private long totalLatencyMs;

    @Schema(description = "Number of self-correction attempts required (0 if succeeded on first try)", example = "0")
    private int selfCorrectionAttempts;

    @Schema(description = "Tabular data rows and column headers returned from execution")
    private DatabaseQueryResult result;

    @Schema(description = "Performance analysis and index suggestions from PostgreSQL EXPLAIN")
    private QueryPlanAnalysis performancePlan;

    @Builder.Default
    @Schema(description = "Step-by-step multi-agent execution timeline trace")
    private List<AgentTraceStep> agentTrace = new ArrayList<>();

    @Schema(description = "Estimated token usage and cost savings estimate")
    private String costSavingsSummary;
}
