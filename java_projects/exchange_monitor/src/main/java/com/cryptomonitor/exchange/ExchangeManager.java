package com.cryptomonitor.exchange;

import com.cryptomonitor.config.ExchangeConfig;
import com.cryptomonitor.exchange.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易所管理器，負責創建和管理所有交易所實例
 */
public class ExchangeManager {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeManager.class);
    private final Map<String, Exchange> exchanges = new HashMap<>();
    
    /**
     * 構造函數，初始化所有配置的交易所
     * @param exchangeConfigs 交易所配置列表
     */
    public ExchangeManager(List<ExchangeConfig> exchangeConfigs) {
        for (ExchangeConfig config : exchangeConfigs) {
            try {
                // 檢查 API 憑證是否有效
                if (!config.isCredentialsValid()) {
                    logger.warn("跳過交易所 {} - API 憑證無效或未設置", config.getName());
                    continue;
                }
                
                Exchange exchange = createExchange(config);
                if (exchange != null) {
                    exchanges.put(config.getName().toLowerCase(), exchange);
                    logger.info("已創建交易所: {}", config.getName());
                }
            } catch (Exception e) {
                logger.error("創建交易所失敗: " + config.getName(), e);
            }
        }
        
        logger.info("交易所管理器初始化完成，共啟用 {} 個交易所", exchanges.size());
    }
    
    /**
     * 根據配置創建交易所實例
     * @param config 交易所配置
     * @return 交易所實例
     */
    private Exchange createExchange(ExchangeConfig config) {
        String name = config.getName().toLowerCase();
        Exchange exchange = null;
        
        switch (name) {
            case "binance":
                exchange = new BinanceExchange(config);
                break;
            case "bybit":
                exchange = new BybitExchange(config);
                break;
            case "bitget":
                exchange = new BitgetExchange(config);
                break;
            case "okex":
                exchange = new OKXExchange(config);
                break;
            case "hyperliquid":
                exchange = new HyperliquidExchange(config);
                break;
            default:
                logger.warn("不支持的交易所類型: {}", name);
                return null;
        }
        
        try {
            exchange.initialize();
            return exchange;
        } catch (Exception e) {
            logger.error("初始化交易所失敗: " + name, e);
            return null;
        }
    }
    
    /**
     * 獲取指定名稱的交易所
     * @param name 交易所名稱
     * @return 交易所實例，如果不存在返回null
     */
    public Exchange getExchange(String name) {
        return exchanges.get(name.toLowerCase());
    }
    
    /**
     * 獲取所有交易所
     * @return 交易所列表
     */
    public List<Exchange> getAllExchanges() {
        return new ArrayList<>(exchanges.values());
    }
    
    /**
     * 獲取交易所數量
     * @return 交易所數量
     */
    public int getExchangeCount() {
        return exchanges.size();
    }
    
    /**
     * 關閉所有交易所連接
     */
    public void shutdown() {
        for (Exchange exchange : exchanges.values()) {
            try {
                exchange.shutdown();
                logger.info("已關閉交易所: {}", exchange.getName());
            } catch (Exception e) {
                logger.error("關閉交易所失敗: " + exchange.getName(), e);
            }
        }
        exchanges.clear();
    }
}