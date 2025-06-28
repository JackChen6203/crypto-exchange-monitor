package com.cryptomonitor.exchange;

import com.cryptomonitor.model.MarketData;

/**
 * 持倉量變動回調接口
 */
@FunctionalInterface
public interface PositionChangeCallback {
    /**
     * 當持倉量發生變動時調用
     * @param positionData 持倉量數據
     */
    void onPositionChange(MarketData.PositionData positionData);
}