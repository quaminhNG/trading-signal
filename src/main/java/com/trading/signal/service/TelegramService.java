package com.trading.signal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Ponytail: No heavy Telegram dependencies needed. A simple RestTemplate POST is enough.
 */
@Slf4j
@Service
public class TelegramService {

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.debug("Telegram alert skipped (not configured): {}", message);
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> request = new HashMap<>();
            request.put("chat_id", chatId);
            request.put("text", message);
            request.put("parse_mode", "HTML");

            restTemplate.postForObject(url, request, String.class);
            log.debug("Telegram alert sent");
        } catch (Exception e) {
            log.error("Failed to send Telegram message: {}", e.getMessage());
        }
    }

    public void sendEmergencyAlert(String message) {
        sendMessage("🚨 <b>EMERGENCY ALERT</b> 🚨\n" + message);
    }
    
    public void sendTradeAlert(String message) {
        sendMessage("📊 <b>TRADE UPDATE</b>\n" + message);
    }
}
