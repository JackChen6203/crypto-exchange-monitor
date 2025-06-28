package com.cryptomonitor.exchange;

import com.cryptomonitor.model.MarketData;

import java.util.List;

/**
 * 交易所接口，定義所有交易所必須實現的方法
 */
public interface Exchange {
    
    /**
     * 獲取交易所名稱
     * @return 交易所名稱
     */
    String getName();
    
    /**
     * 初始化交易所連接
     * @throws Exception 如果初始化失敗
     */
    void initialize() throws Exception;
    
    /**
     * 關閉交易所連接
     */
    void shutdown();
    
    /**
     * 獲取指定交易對的最新價格
     * @param symbol 交易對符號
     * @return 最新價格
     * @throws Exception 如果獲取失敗
     */
    double getLatestPrice(String symbol) throws Exception;
    
    /**
     * 獲取指定交易對的持倉量數據
     * @param symbol 交易對符號
     * @return 持倉量數據
     * @throws Exception 如果獲取失敗
     */
    MarketData.PositionData getPositionData(String symbol) throws Exception;
    
    /**
     * 獲取所有支持的交易對列表
     * @return 交易對列表
     * @throws Exception 如果獲取失敗
     */
    List<String> getSupportedSymbols() throws Exception;
    
    /**
     * 訂閱價格變動
     * @param symbol 交易對符號
     * @param callback 價格變動回調函數
     * @throws Exception 如果訂閱失敗
     */
    void subscribePriceChanges(String symbol, PriceChangeCallback callback) throws Exception;
    
    /**
     * 訂閱持倉量變動
     * @param symbol 交易對符號
     * @param callback 持倉量變動回調函數
     * @throws Exception 如果訂閱失敗
     */
    void subscribePositionChanges(String symbol, PositionChangeCallback callback) throws Exception;
    
    /**
     * 取消訂閱價格變動
     * @param symbol 交易對符號
     * @throws Exception 如果取消訂閱失敗
     */
    void unsubscribePriceChanges(String symbol) throws Exception;
    
    /**
     * 取消訂閱持倉量變動
     * @param symbol 交易對符號
     * @throws Exception 如果取消訂閱失敗
     */
    void unsubscribePositionChanges(String symbol) throws Exception;
    
    /**
     * 檢查交易所連接狀態
     * @return 如果連接正常返回true，否則返回false
     */
    boolean isConnected();
}