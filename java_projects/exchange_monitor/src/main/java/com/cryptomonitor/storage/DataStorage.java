package com.cryptomonitor.storage;

import com.cryptomonitor.model.MarketData;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 數據存儲接口
 */
public interface DataStorage {
    
    /**
     * 保存價格數據
     * @param priceData 價格數據
     */
    void savePriceData(MarketData.PriceData priceData);
    
    /**
     * 保存持倉數據
     * @param positionData 持倉數據
     */
    void savePositionData(MarketData.PositionData positionData);
    
    /**
     * 獲取歷史價格數據
     * @param exchange 交易所名稱
     * @param symbol 交易對
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 價格數據列表
     */
    List<MarketData.PriceData> getPriceHistory(String exchange, String symbol, 
                                               LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 獲取歷史持倉數據
     * @param exchange 交易所名稱
     * @param symbol 交易對
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 持倉數據列表
     */
    List<MarketData.PositionData> getPositionHistory(String exchange, String symbol, 
                                                      LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 清理過期數據
     * @param daysToKeep 保留天數
     */
    void cleanupOldData(int daysToKeep);
    
    /**
     * 關閉存儲連接
     */
    void close();
} 