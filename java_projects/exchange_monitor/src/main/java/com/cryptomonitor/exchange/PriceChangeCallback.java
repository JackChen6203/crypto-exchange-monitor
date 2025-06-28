package com.cryptomonitor.exchange;

import com.cryptomonitor.model.MarketData;

/**
 * 價格變動回調接口
 */
@FunctionalInterface
public interface PriceChangeCallback {
    /**
     * 當價格發生變動時調用
     * @param priceData 價格數據
     */
    void onPriceChange(MarketData.PriceData priceData);
}