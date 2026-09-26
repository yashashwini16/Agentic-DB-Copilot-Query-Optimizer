package com.dbcopilot.controller;

import com.dbcopilot.dto.DatabaseQueryResult;
import com.dbcopilot.dto.QueryPlanAnalysis;
import com.dbcopilot.dto.QueryRequest;
import com.dbcopilot.dto.QueryResponse;
import com.dbcopilot.service.AgenticWorkflowService;
import com.dbcopilot.service.DatabaseExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
@Tag(name = "Agentic Copilot", description = "Endpoints for AI SQL generation, self-correction, execution, and EXPLAIN optimization")
@CrossOrigin(origins = "*")
public class CopilotController {

    private final AgenticWorkflowService workflowService;
    private final DatabaseExecutionService executionService;

    @PostMapping("/query")
    @Operation(summary = "Translate Natural Language into SQL, self-correct errors, and execute with Pgvector caching")
    public ResponseEntity<QueryResponse> processQuery(@Valid @RequestBody QueryRequest request) {
        QueryResponse response = workflowService.processQuery(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/direct-execute")
    @Operation(summary = "Directly execute a SQL statement on PostgreSQL with guardrail validation")
    public ResponseEntity<DatabaseQueryResult> directExecute(@RequestBody Map<String, String> body) {
        String sql = body.getOrDefault("sql", "");
        DatabaseQueryResult result = executionService.executeQuery(sql);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/explain")
    @Operation(summary = "Execute PostgreSQL EXPLAIN ANALYZE on a SQL statement")
    public ResponseEntity<QueryPlanAnalysis> explainQuery(@RequestBody Map<String, String> body) {
        String sql = body.getOrDefault("sql", "");
        QueryPlanAnalysis plan = executionService.explainQueryPlan(sql);
        return ResponseEntity.ok(plan);
    }
}
