package com.dbcopilot.agent.tools;

import com.dbcopilot.dto.DatabaseQueryResult;
import com.dbcopilot.dto.QueryPlanAnalysis;
import com.dbcopilot.service.DatabaseExecutionService;
import com.dbcopilot.service.DatabaseSchemaService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseTools {

    private final DatabaseExecutionService executionService;
    private final DatabaseSchemaService schemaService;

    @Tool("Execute a SQL SELECT query against the PostgreSQL database to verify its execution and view the resulting rows or any SQL errors.")
    public String executeSql(@P("The PostgreSQL SQL query to execute") String sql) {
        log.info("Tool Call [executeSql]: {}", sql);
        DatabaseQueryResult result = executionService.executeQuery(sql);
        if (!result.isSuccess()) {
            return String.format("ERROR: Execution failed with message: %s (SQLState: %s)", 
                    result.getErrorMessage(), result.getSqlState());
        }
        return String.format("SUCCESS: Returned %d rows in %d ms. Sample columns: %s", 
                result.getRowCount(), result.getExecutionTimeMs(), String.join(", ", result.getColumns()));
    }

    @Tool("Inspect the database schema, tables, column types, primary keys, and foreign keys.")
    public String getDatabaseSchema() {
        log.info("Tool Call [getDatabaseSchema]");
        return schemaService.getSchemaPromptContext();
    }

    @Tool("Analyze the PostgreSQL execution plan (EXPLAIN ANALYZE) to identify cost, execution time, and missing indexes.")
    public String explainQueryPlan(@P("The SQL query to analyze") String sql) {
        log.info("Tool Call [explainQueryPlan]: {}", sql);
        QueryPlanAnalysis plan = executionService.explainQueryPlan(sql);
        return String.format("Cost: %.2f | Actual Time: %.2f ms | Sequential Scan: %s | Recommendations: %s",
                plan.getTotalCost(),
                plan.getActualExecutionTimeMs(),
                plan.isSequentialScanDetected(),
                String.join("; ", plan.getRecommendations()));
    }
}
