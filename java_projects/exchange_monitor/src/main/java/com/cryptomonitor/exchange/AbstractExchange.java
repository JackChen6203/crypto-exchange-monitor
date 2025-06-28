package com.cryptomonitor.exchange;

import com.cryptomonitor.config.ExchangeConfig;
import com.cryptomonitor.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象交易所類，實現共用的功能
 */
public abstract class AbstractExchange implements Exchange {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ExchangeConfig config;
    protected final String name;
    protected final Map<String, PriceChangeCallback> priceCallbacks = new HashMap<>();
    protected final Map<String, PositionChangeCallback> positionCallbacks = new HashMap<>();
    protected boolean connected = false;
    
    // 緩存最新的價格和持倉數據
    protected final Map<String, Double> lastPrices = new HashMap<>();
    protected final Map<String, MarketData.PositionData> lastPositions = new HashMap<>();
    
    public AbstractExchange(ExchangeConfig config) {
        this.config = config;
        this.name = config.getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public void subscribePriceChanges(String symbol, PriceChangeCallback callback) throws Exception {
        priceCallbacks.put(symbol, callback);
        logger.info("{}已訂閱{}價格變動", name, symbol);
    }
    
    @Override
    public void subscribePositionChanges(String symbol, PositionChangeCallback callback) throws Exception {
        positionCallbacks.put(symbol, callback);
        logger.info("{}已訂閱{}持倉量變動", name, symbol);
    }
    
    @Override
    public void unsubscribePriceChanges(String symbol) throws Exception {
        priceCallbacks.remove(symbol);
        logger.info("{}已取消訂閱{}價格變動", name, symbol);
    }
    
    @Override
    public void unsubscribePositionChanges(String symbol) throws Exception {
        positionCallbacks.remove(symbol);
        logger.info("{}已取消訂閱{}持倉量變動", name, symbol);
    }
    
    /**
     * 處理價格更新並通知訂閱者
     * @param symbol 交易對符號
     * @param price 最新價格
     */
    protected void handlePriceUpdate(String symbol, double price) {
        Double previousPrice = lastPrices.getOrDefault(symbol, 0.0);
        lastPrices.put(symbol, price);
        
        // 創建價格數據對象
        MarketData.PriceData priceData = new MarketData.PriceData(
                name, symbol, price, previousPrice);
        
        // 通知訂閱者
        PriceChangeCallback callback = priceCallbacks.get(symbol);
        if (callback != null) {
            callback.onPriceChange(priceData);
        }
    }
    
    /**
     * 處理持倉量更新並通知訂閱者
     * @param symbol 交易對符號
     * @param longPosition 多頭持倉量
     * @param shortPosition 空頭持倉量
     */
    protected void handlePositionUpdate(String symbol, double longPosition, double shortPosition) {
        MarketData.PositionData previousPosition = lastPositions.getOrDefault(symbol, 
                new MarketData.PositionData(name, symbol, 0, 0, 0, 0));
        
        // 創建持倉量數據對象
        MarketData.PositionData positionData = new MarketData.PositionData(
                name, symbol, longPosition, shortPosition, 
                previousPosition.getLongPosition(), previousPosition.getShortPosition());
        
        lastPositions.put(symbol, positionData);
        
        // 通知訂閱者
        PositionChangeCallback callback = positionCallbacks.get(symbol);
        if (callback != null) {
            callback.onPositionChange(positionData);
        }
    }
    
    /**
     * 更新價格數據並通知訂閱者
     * @param symbol 交易對符號
     * @param priceData 價格數據
     */
    protected void updatePriceData(String symbol, MarketData.PriceData priceData) {
        // 更新緩存
        lastPrices.put(symbol, priceData.getPrice());
        
        // 通知訂閱者
        PriceChangeCallback callback = priceCallbacks.get(symbol);
        if (callback != null) {
            callback.onPriceChange(priceData);
        }
    }
    
    /**
     * 更新持倉數據並通知訂閱者
     * @param symbol 交易對符號
     * @param positionData 持倉數據
     */
    protected void updatePositionData(String symbol, MarketData.PositionData positionData) {
        // 更新緩存
        lastPositions.put(symbol, positionData);
        
        // 通知訂閱者
        PositionChangeCallback callback = positionCallbacks.get(symbol);
        if (callback != null) {
            callback.onPositionChange(positionData);
        }
    }
    
    /**
     * 獲取上一次的持倉數據
     * @param symbol 交易對符號
     * @return 持倉數據，如果沒有則返回null
     */
    protected MarketData.PositionData getLastPositionData(String symbol) {
        return lastPositions.get(symbol);
    }
}