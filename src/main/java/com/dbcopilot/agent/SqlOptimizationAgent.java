package com.dbcopilot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SqlOptimizationAgent {

    @SystemMessage("""
        You are a PostgreSQL Query Performance Tuning Specialist.
        Analyze the provided SQL query along with its PostgreSQL EXPLAIN execution plan.
        Provide concrete optimization recommendations, identifying potential bottlenecks like sequential table scans, excessive sort buffers, or cartesian joins.
        Suggest any specific B-Tree, GIN, or BRIN index creation commands that would improve execution time.
        Keep your advice concise and formatted with bullet points and SQL code blocks.
        """)
    String optimize(
            @V("query") String query,
            @UserMessage String explainPlan
    );
}
