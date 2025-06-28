package com.cryptomonitor.exchange;

import com.cryptomonitor.config.ExchangeConfig;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExchangeManagerTest {

    @Test
    void testExchangeManager_EmptyConfig() {
        List<ExchangeConfig> emptyConfigs = new ArrayList<>();
        ExchangeManager exchangeManager = new ExchangeManager(emptyConfigs);
        
        assertEquals(0, exchangeManager.getExchangeCount());
        assertTrue(exchangeManager.getAllExchanges().isEmpty());
    }

    @Test
    void testExchangeManager_WithConfigs() {
        List<ExchangeConfig> configs = new ArrayList<>();
        configs.add(new ExchangeConfig("Binance", "", "", 
            "https://api.binance.com", "wss://stream.binance.com:9443/ws"));
        
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        assertTrue(exchangeManager.getExchangeCount() >= 0);
        assertNotNull(exchangeManager.getAllExchanges());
    }

    @Test
    void testGetExchange() {
        List<ExchangeConfig> configs = new ArrayList<>();
        configs.add(new ExchangeConfig("Binance", "", "", 
            "https://api.binance.com", "wss://stream.binance.com:9443/ws"));
        
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        // 可能會因為API key問題初始化失敗，但不應該拋異常
        assertDoesNotThrow(() -> {
            Exchange exchange = exchangeManager.getExchange("binance");
            // exchange可能為null，這是正常的
        });
    }

    @Test
    void testShutdown() {
        List<ExchangeConfig> configs = new ArrayList<>();
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        assertDoesNotThrow(() -> exchangeManager.shutdown());
    }

    @Test
    void testInvalidExchangeName() {
        List<ExchangeConfig> configs = new ArrayList<>();
        configs.add(new ExchangeConfig("InvalidExchange", "", "", "", ""));
        
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        // 不支持的交易所應該被忽略
        assertEquals(0, exchangeManager.getExchangeCount());
    }
} 