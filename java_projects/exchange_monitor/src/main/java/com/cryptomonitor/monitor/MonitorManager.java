package com.cryptomonitor.monitor;

import com.cryptomonitor.config.MonitorConfig;
import com.cryptomonitor.discord.DiscordService;
import com.cryptomonitor.exchange.Exchange;
import com.cryptomonitor.exchange.ExchangeManager;
import com.cryptomonitor.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 監控管理器，負責協調交易所數據監控和通知發送
 */
public class MonitorManager {
    private static final Logger logger = LoggerFactory.getLogger(MonitorManager.class);
    private final ExchangeManager exchangeManager;
    private final DiscordService discordService;
    private final MonitorConfig config;
    private final ScheduledExecutorService scheduler;
    private final Map<String, List<String>> monitoredSymbols;
    private boolean isRunning = false;
    
    public MonitorManager(ExchangeManager exchangeManager, DiscordService discordService, MonitorConfig config) {
        this.exchangeManager = exchangeManager;
        this.discordService = discordService;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.monitoredSymbols = new HashMap<>();
    }
    
    /**
     * 啟動監控
     */
    public void startMonitoring() {
        if (isRunning) {
            logger.warn("監控已經在運行中");
            return;
        }
        
        logger.info("啟動交易所數據監控...");
        
        // 初始化監控的交易對
        initializeMonitoredSymbols();
        
        // 設置價格和持倉量變動回調
        setupCallbacks();
        
        // 啟動定時任務，定期獲取數據
        scheduler.scheduleAtFixedRate(
                this::fetchAllData,
                0,
                config.getIntervalSeconds(),
                TimeUnit.SECONDS
        );
        
        isRunning = true;
        logger.info("監控已啟動，間隔時間: {}秒", config.getIntervalSeconds());
    }
    
    /**
     * 停止監控
     */
    public void stopMonitoring() {
        if (!isRunning) {
            return;
        }
        
        logger.info("停止交易所數據監控...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        isRunning = false;
        logger.info("監控已停止");
    }
    
    /**
     * 初始化監控的交易對
     */
    private void initializeMonitoredSymbols() {
        for (Exchange exchange : exchangeManager.getAllExchanges()) {
            try {
                // 獲取交易所支持的交易對
                List<String> symbols = exchange.getSupportedSymbols();
                
                // 選擇要監控的交易對（這裡簡化為前10個）
                List<String> selected = new ArrayList<>();
                for (int i = 0; i < Math.min(10, symbols.size()); i++) {
                    selected.add(symbols.get(i));
                }
                
                monitoredSymbols.put(exchange.getName().toLowerCase(), selected);
                logger.info("已為{}設置{}個監控交易對", exchange.getName(), selected.size());
            } catch (Exception e) {
                logger.error("獲取{}交易對失敗", exchange.getName(), e);
            }
        }
    }
    
    /**
     * 設置價格和持倉量變動回調
     */
    private void setupCallbacks() {
        for (Exchange exchange : exchangeManager.getAllExchanges()) {
            List<String> symbols = monitoredSymbols.get(exchange.getName().toLowerCase());
            if (symbols == null) {
                continue;
            }
            
            for (String symbol : symbols) {
                try {
                    // 訂閱價格變動
                    exchange.subscribePriceChanges(symbol, priceData -> {
                        // 檢查價格變動是否超過閾值
                        if (Math.abs(priceData.getChangePercent()) >= config.getPriceThresholdPercent()) {
                            logger.info("檢測到價格異動: {}", priceData);
                            discordService.sendPriceAlert(priceData);
                        }
                    });
                    
                    // 訂閱持倉量變動
                    exchange.subscribePositionChanges(symbol, positionData -> {
                        // 檢查持倉量變動是否超過閾值
                        if (Math.abs(positionData.getLongChangePercent()) >= config.getPositionThresholdPercent() ||
                            Math.abs(positionData.getShortChangePercent()) >= config.getPositionThresholdPercent()) {
                            logger.info("檢測到持倉量異動: {}", positionData);
                            discordService.sendPositionAlert(positionData);
                        }
                    });
                    
                    logger.info("已為{}設置{}的價格和持倉量監控", exchange.getName(), symbol);
                } catch (Exception e) {
                    logger.error("為{}設置{}的監控失敗", exchange.getName(), symbol, e);
                }
            }
        }
    }
    
    /**
     * 獲取所有交易所的數據
     */
    private void fetchAllData() {
        logger.debug("開始獲取交易所數據...");
        
        for (Exchange exchange : exchangeManager.getAllExchanges()) {
            List<String> symbols = monitoredSymbols.get(exchange.getName().toLowerCase());
            if (symbols == null) {
                continue;
            }
            
            for (String symbol : symbols) {
                try {
                    // 獲取最新價格
                    double price = exchange.getLatestPrice(symbol);
                    logger.debug("{}的{}價格: {}", exchange.getName(), symbol, price);
                    
                    // 獲取持倉量數據
                    MarketData.PositionData positionData = exchange.getPositionData(symbol);
                    logger.debug("{}的{}持倉量: {}", exchange.getName(), symbol, positionData);
                } catch (Exception e) {
                    logger.error("獲取{}的{}數據失敗", exchange.getName(), symbol, e);
                }
            }
        }
        
        logger.debug("交易所數據獲取完成");
    }
}