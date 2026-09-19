package com.trading.signal;

import org.junit.jupiter.api.Test;

class TradingSignalApplicationTests {

    @Test
    void contextLoadsWithoutSpring() {
        // ponytail: smoke test — just verifies the class compiles and main method exists.
        // Full Spring context test requires running PostgreSQL (use @SpringBootTest + Testcontainers later).
        TradingSignalApplication.main(new String[]{"--spring.main.web-application-type=none", "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"});
    }
}
