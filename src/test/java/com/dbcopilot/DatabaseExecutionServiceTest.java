package com.dbcopilot;

import com.dbcopilot.dto.DatabaseQueryResult;
import com.dbcopilot.service.DatabaseExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseExecutionServiceTest {

    @Autowired
    private DatabaseExecutionService executionService;

    @Test
    @DisplayName("Should sanitize markdown code fences and semicolons")
    void testSanitizeSql() {
        String input = "```sql\nSELECT * FROM customers WHERE id = 1;\n```";
        String sanitized = executionService.sanitizeSql(input);
        assertEquals("SELECT * FROM customers WHERE id = 1", sanitized);
    }

    @Test
    @DisplayName("Should block forbidden DDL commands via security guardrails")
    void testGuardrailsBlockForbiddenDdl() {
        assertThrows(SecurityException.class, () -> {
            executionService.validateGuardrails("DROP TABLE customers CASCADE;");
        });

        assertThrows(SecurityException.class, () -> {
            executionService.validateGuardrails("TRUNCATE TABLE orders;");
        });
    }

    @Test
    @DisplayName("Should reject DML commands when read-only is enforced")
    void testGuardrailsEnforceReadOnly() {
        assertThrows(SecurityException.class, () -> {
            executionService.validateGuardrails("DELETE FROM customers WHERE id = 5;");
        });

        assertThrows(SecurityException.class, () -> {
            executionService.validateGuardrails("UPDATE products SET unit_price = 0;");
        });
    }
}
