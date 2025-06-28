package com.cryptomonitor.config;

/**
 * 監控配置類，包含監控參數設置
 */
public class MonitorConfig {
    private final int intervalSeconds;
    private final double priceThresholdPercent;
    private final double positionThresholdPercent;
    
    public MonitorConfig(int intervalSeconds, double priceThresholdPercent, double positionThresholdPercent) {
        this.intervalSeconds = intervalSeconds;
        this.priceThresholdPercent = priceThresholdPercent;
        this.positionThresholdPercent = positionThresholdPercent;
    }
    
    /**
     * 獲取監控間隔時間（秒）
     */
    public int getIntervalSeconds() {
        return intervalSeconds;
    }
    
    /**
     * 獲取價格變動閾值百分比
     */
    public double getPriceThresholdPercent() {
        return priceThresholdPercent;
    }
    
    /**
     * 獲取持倉量變動閾值百分比
     */
    public double getPositionThresholdPercent() {
        return positionThresholdPercent;
    }
    
    /**
     * 獲取價格變動閾值（小數形式）
     */
    public double getPriceThreshold() {
        return priceThresholdPercent / 100.0;
    }
    
    /**
     * 獲取持倉量變動閾值（小數形式）
     */
    public double getPositionThreshold() {
        return positionThresholdPercent / 100.0;
    }
    
    @Override
    public String toString() {
        return "MonitorConfig{" +
                "intervalSeconds=" + intervalSeconds +
                ", priceThresholdPercent=" + priceThresholdPercent +
                ", positionThresholdPercent=" + positionThresholdPercent +
                '}';
    }
}