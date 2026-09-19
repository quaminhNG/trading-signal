package com.trading.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradingSignalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingSignalApplication.class, args);
    }
}
