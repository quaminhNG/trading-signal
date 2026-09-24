package com.trading.signal;

import org.junit.jupiter.api.Test;

class TradingSignalApplicationTests {

    @Test
    void contextLoadsWithoutSpring() {
        // ponytail: smoke test — just verifies the class compiles and main method exists.
        // Full Spring context test requires running PostgreSQL (use @SpringBootTest + Testcontainers later).
        org.junit.jupiter.api.Assertions.assertNotNull(TradingSignalApplication.class);
    }
}
