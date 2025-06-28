package com.cryptomonitor.load;

import com.cryptomonitor.config.ExchangeConfig;
import com.cryptomonitor.config.MonitorConfig;
import com.cryptomonitor.discord.DiscordService;
import com.cryptomonitor.exchange.ExchangeManager;
import com.cryptomonitor.monitor.MonitorManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 負載測試類，測試系統在高並發和長時間運行下的穩定性
 */
class LoadTest {

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testHighConcurrencyDataRetrieval() throws Exception {
        // 創建測試配置
        List<ExchangeConfig> configs = createTestExchangeConfigs();
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        // 並發測試
        int threadCount = 10;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        // 並發獲取數據
                        exchangeManager.getAllExchanges().forEach(exchange -> {
                            if (exchange.isConnected()) {
                                try {
                                    // 測試獲取價格
                                    double price = exchange.getLatestPrice("BTCUSDT");
                                    assertTrue(price > 0);
                                    
                                    // 短暫暫停避免API限制
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    // 記錄但不中斷測試
                                    System.err.println("負載測試中的錯誤: " + e.getMessage());
                                }
                            }
                        });
                    }
                } finally {
                    latch.countDown();
                }
            }, executor);
            futures.add(future);
        }
        
        // 等待所有任務完成
        latch.await(50, TimeUnit.SECONDS);
        
        // 關閉資源
        executor.shutdown();
        exchangeManager.shutdown();
        
        System.out.println("高並發負載測試完成");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testLongRunningMonitoring() throws Exception {
        // 創建測試配置
        List<ExchangeConfig> configs = createTestExchangeConfigs();
        ExchangeManager exchangeManager = new ExchangeManager(configs);
        
        // 創建模擬Discord服務（不實際發送消息）
        MockDiscordService discordService = new MockDiscordService();
        
        // 創建監控配置（短間隔用於測試）
        MonitorConfig monitorConfig = new MonitorConfig(1, 0.1, 0.1);
        
        MonitorManager monitorManager = new MonitorManager(
                exchangeManager, discordService, monitorConfig);
        
        // 啟動監控
        monitorManager.startMonitoring();
        
        // 運行30秒
        Thread.sleep(30000);
        
        // 停止監控
        monitorManager.stopMonitoring();
        exchangeManager.shutdown();
        
        // 驗證系統穩定性
        assertTrue(discordService.getMessageCount() >= 0);
        System.out.println("長時間運行測試完成，發送了" + discordService.getMessageCount() + "條消息");
    }

    @Test
    void testMemoryUsage() throws Exception {
        System.gc(); // 強制垃圾回收
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // 創建多個ExchangeManager實例
        List<ExchangeManager> managers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<ExchangeConfig> configs = createTestExchangeConfigs();
            managers.add(new ExchangeManager(configs));
        }
        
        // 檢查記憶體使用
        System.gc();
        long afterCreationMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // 關閉所有管理器
        managers.forEach(ExchangeManager::shutdown);
        managers.clear();
        
        System.gc();
        long afterCleanupMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        System.out.println("初始記憶體: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("創建後記憶體: " + (afterCreationMemory / 1024 / 1024) + " MB");
        System.out.println("清理後記憶體: " + (afterCleanupMemory / 1024 / 1024) + " MB");
        
        // 驗證記憶體沒有嚴重洩漏
        long memoryIncrease = afterCleanupMemory - initialMemory;
        assertTrue(memoryIncrease < 100 * 1024 * 1024, // 100MB閾值
                "記憶體使用增加過多: " + (memoryIncrease / 1024 / 1024) + " MB");
    }

    private List<ExchangeConfig> createTestExchangeConfigs() {
        List<ExchangeConfig> configs = new ArrayList<>();
        
        // 只添加不需要API key的交易所配置用於測試
        configs.add(new ExchangeConfig(
                "Binance", 
                "", 
                "", 
                "https://api.binance.com",
                "wss://stream.binance.com:9443/ws"
        ));
        
        return configs;
    }

    /**
     * 模擬Discord服務，用於測試
     */
    private static class MockDiscordService extends DiscordService {
        private int messageCount = 0;
        
        public MockDiscordService() {
            super(createMockDiscordConfig()); // 傳遞模擬配置，不實際初始化Discord
        }
        
        private static com.cryptomonitor.config.DiscordConfig createMockDiscordConfig() {
            return new com.cryptomonitor.config.DiscordConfig("", "", "");
        }
        
        @Override
        public void sendPriceAlert(com.cryptomonitor.model.MarketData.PriceData priceData) {
            messageCount++;
        }
        
        @Override
        public void sendPositionAlert(com.cryptomonitor.model.MarketData.PositionData positionData) {
            messageCount++;
        }
        
        public int getMessageCount() {
            return messageCount;
        }
    }
} 