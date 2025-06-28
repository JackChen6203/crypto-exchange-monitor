package com.cryptomonitor.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testDiscordConfig() {
        DiscordConfig config = new DiscordConfig("test_token", "123456", "webhook_url");
        
        assertEquals("test_token", config.getToken());
        assertEquals("123456", config.getChannelId());
        assertEquals("webhook_url", config.getWebhookUrl());
    }

    @Test
    void testExchangeConfig() {
        ExchangeConfig config = new ExchangeConfig(
            "Binance", "api_key", "secret_key", 
            "https://api.binance.com", "wss://stream.binance.com:9443/ws"
        );
        
        assertEquals("Binance", config.getName());
        assertEquals("api_key", config.getApiKey());
        assertEquals("secret_key", config.getSecretKey());
        assertEquals("https://api.binance.com", config.getBaseUrl());
        assertEquals("wss://stream.binance.com:9443/ws", config.getWsUrl());
    }

    @Test
    void testMonitorConfig() {
        MonitorConfig config = new MonitorConfig(60, 1.0, 5.0);
        
        assertEquals(60, config.getIntervalSeconds());
        assertEquals(1.0, config.getPriceThresholdPercent());
        assertEquals(5.0, config.getPositionThresholdPercent());
    }

    @Test
    void testAppConfigSingleton() {
        AppConfig instance1 = AppConfig.getInstance();
        AppConfig instance2 = AppConfig.getInstance();
        
        assertSame(instance1, instance2);
        assertNotNull(instance1.getDiscordConfig());
        assertNotNull(instance1.getMonitorConfig());
        assertNotNull(instance1.getExchangeConfigs());
    }
} 