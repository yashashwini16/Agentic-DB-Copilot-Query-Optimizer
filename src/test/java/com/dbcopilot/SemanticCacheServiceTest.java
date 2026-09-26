package com.dbcopilot;

import com.dbcopilot.service.SemanticCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SemanticCacheServiceTest {

    @Autowired
    private SemanticCacheService semanticCacheService;

    @Test
    @DisplayName("Should detect cache hit for semantically identical queries")
    void testSemanticCacheHit() {
        String original = "Show me the top 5 spending customers";
        String generatedSql = "SELECT * FROM customers ORDER BY lifetime_spend DESC LIMIT 5";
        String explanation = "Retrieves top 5 customers ordered by lifetime spend.";

        semanticCacheService.cacheQuery(original, generatedSql, explanation, 150L);

        // Semantically similar variation
        String queryVariation = "Who are the top 5 highest spending customers?";
        SemanticCacheService.CacheLookupResult result = semanticCacheService.searchCache(queryVariation);

        assertTrue(result.isHit(), "Expected semantic cache hit for synonymous query");
        assertNotNull(result.getEntry());
        assertEquals(generatedSql, result.getEntry().getGeneratedSql());
        assertTrue(result.getSimilarity() >= 0.85);
    }
}
