package com.dbcopilot.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${copilot.llm.provider:openai}")
    private String llmProvider;

    @Value("${copilot.llm.openai.api-key:demo-key}")
    private String openAiApiKey;

    @Value("${copilot.llm.openai.model-name:gpt-4o-mini}")
    private String openAiModelName;

    @Value("${copilot.llm.openai.temperature:0.0}")
    private double openAiTemperature;

    @Value("${copilot.llm.openai.timeout-seconds:60}")
    private int openAiTimeoutSeconds;

    @Value("${copilot.llm.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${copilot.llm.ollama.model-name:llama3.1}")
    private String ollamaModelName;

    @Value("${copilot.llm.ollama.temperature:0.0}")
    private double ollamaTemperature;

    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        if ("ollama".equalsIgnoreCase(llmProvider)) {
            log.info("Initializing LangChain4j OllamaChatModel with URL: {} and Model: {}", ollamaBaseUrl, ollamaModelName);
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModelName)
                    .temperature(ollamaTemperature)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else if ("openai".equalsIgnoreCase(llmProvider) && openAiApiKey != null && !openAiApiKey.isBlank() && !openAiApiKey.equalsIgnoreCase("demo-key")) {
            log.info("Initializing LangChain4j OpenAiChatModel with Model: {}", openAiModelName);
            return OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModelName)
                    .temperature(openAiTemperature)
                    .timeout(Duration.ofSeconds(openAiTimeoutSeconds))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        } else {
            log.warn("No active LLM API key provided. Initializing Mock/Rule-based ChatLanguageModel for local testing.");
            return createMockChatLanguageModel();
        }
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("Initializing in-process AllMiniLmL6V2EmbeddingModel (dimension: 384, local ONNX runtime)");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Fallback mock model with deterministic SQL synthesis rules for testing without an API key
     */
    private ChatLanguageModel createMockChatLanguageModel() {
        return new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).text().toLowerCase();
                
                String sql;
                String explanation;

                if (lastMessage.contains("spending") || lastMessage.contains("revenue") || lastMessage.contains("top 5 customer")) {
                    sql = "SELECT c.id, c.full_name, c.email, c.loyalty_tier, SUM(o.total_amount) AS total_spent, COUNT(o.id) AS total_orders " +
                          "FROM customers c " +
                          "JOIN orders o ON c.id = o.customer_id " +
                          "WHERE o.status = 'COMPLETED' " +
                          "GROUP BY c.id, c.full_name, c.email, c.loyalty_tier " +
                          "ORDER BY total_spent DESC " +
                          "LIMIT 5;";
                    explanation = "Aggregates total order value per customer from completed orders, grouped by customer details and sorted descending by spend.";
                } else if (lastMessage.contains("product") && (lastMessage.contains("category") || lastMessage.contains("rating") || lastMessage.contains("review"))) {
                    sql = "SELECT cat.name AS category_name, COUNT(p.id) AS product_count, ROUND(AVG(p.rating), 2) AS avg_product_rating, ROUND(AVG(p.unit_price), 2) AS avg_price " +
                          "FROM categories cat " +
                          "JOIN products p ON cat.id = p.category_id " +
                          "GROUP BY cat.name " +
                          "ORDER BY avg_product_rating DESC;";
                    explanation = "Groups active products by category and calculates average ratings and product counts sorted by highest rated.";
                } else if (lastMessage.contains("inactive") || lastMessage.contains("no order")) {
                    sql = "SELECT c.id, c.full_name, c.email, c.signup_date " +
                          "FROM customers c " +
                          "LEFT JOIN orders o ON c.id = o.customer_id " +
                          "WHERE o.id IS NULL " +
                          "ORDER BY c.signup_date DESC;";
                    explanation = "Finds customers who have registered but have not placed any orders yet using a LEFT JOIN.";
                } else if (lastMessage.contains("error") || lastMessage.contains("correct") || lastMessage.contains("fail")) {
                    // Correction agent simulated response
                    sql = "SELECT c.id, c.full_name, c.loyalty_tier, o.total_amount, o.order_date " +
                          "FROM customers c " +
                          "JOIN orders o ON c.id = o.customer_id " +
                          "ORDER BY o.order_date DESC " +
                          "LIMIT 10;";
                    explanation = "Corrected column names and resolved table join aliases.";
                } else {
                    sql = "SELECT id, full_name, email, loyalty_tier, lifetime_spend, signup_date FROM customers ORDER BY lifetime_spend DESC LIMIT 10;";
                    explanation = "Retrieved customer records sorted by lifetime spend.";
                }

                String responseContent = String.format("```sql\n%s\n```\n\n**Explanation:** %s", sql, explanation);
                return Response.from(AiMessage.from(responseContent));
            }
        };
    }
}
