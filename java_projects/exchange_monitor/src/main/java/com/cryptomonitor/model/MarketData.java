package com.cryptomonitor.model;

import java.time.Instant;

/**
 * 市場數據模型類，包含價格和持倉量數據
 */
public class MarketData {
    
    /**
     * 價格數據類
     */
    public static class PriceData {
        private final String exchange;
        private final String symbol;
        private final double price;
        private final double previousPrice;
        private final double changePercent;
        private final long timestamp;
        
        public PriceData(String exchange, String symbol, double price, double previousPrice) {
            this.exchange = exchange;
            this.symbol = symbol;
            this.price = price;
            this.previousPrice = previousPrice;
            this.changePercent = previousPrice > 0 ? ((price - previousPrice) / previousPrice) * 100 : 0;
            this.timestamp = Instant.now().getEpochSecond();
        }
        
        /**
         * 帶有預先計算變動百分比的構造函數
         */
        public PriceData(String exchange, String symbol, double price, 
                        double priceChange, double changePercent, long timestamp) {
            this.exchange = exchange;
            this.symbol = symbol;
            this.price = price;
            this.previousPrice = price - priceChange; // 根據變動計算之前的價格
            this.changePercent = changePercent;
            this.timestamp = timestamp;
        }
        
        public String getExchange() {
            return exchange;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getPrice() {
            return price;
        }
        
        public double getPreviousPrice() {
            return previousPrice;
        }
        
        public double getChangePercent() {
            return changePercent;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s 價格: %.8f (變動: %.2f%%)", 
                    exchange, symbol, price, changePercent);
        }
    }
    
    /**
     * 持倉量數據類
     */
    public static class PositionData {
        private final String exchange;
        private final String symbol;
        private final double longPosition;
        private final double shortPosition;
        private final double previousLongPosition;
        private final double previousShortPosition;
        private final double longChangePercent;
        private final double shortChangePercent;
        private final double longShortRatio;
        private final long timestamp;
        
        public PositionData(String exchange, String symbol, 
                           double longPosition, double shortPosition, 
                           double previousLongPosition, double previousShortPosition) {
            this.exchange = exchange;
            this.symbol = symbol;
            this.longPosition = longPosition;
            this.shortPosition = shortPosition;
            this.previousLongPosition = previousLongPosition;
            this.previousShortPosition = previousShortPosition;
            
            this.longChangePercent = previousLongPosition > 0 ? 
                    ((longPosition - previousLongPosition) / previousLongPosition) * 100 : 0;
            
            this.shortChangePercent = previousShortPosition > 0 ? 
                    ((shortPosition - previousShortPosition) / previousShortPosition) * 100 : 0;
            
            this.longShortRatio = shortPosition > 0 ? longPosition / shortPosition : 0;
            this.timestamp = Instant.now().getEpochSecond();
        }
        
        /**
         * 帶有預先計算變動百分比的構造函數
         */
        public PositionData(String exchange, String symbol, 
                           double longPosition, double shortPosition, 
                           double longChangePercent, double shortChangePercent,
                           double longShortRatio, long timestamp) {
            this.exchange = exchange;
            this.symbol = symbol;
            this.longPosition = longPosition;
            this.shortPosition = shortPosition;
            this.previousLongPosition = 0; // 不需要保存之前的值，因為已經計算了變動百分比
            this.previousShortPosition = 0; // 不需要保存之前的值，因為已經計算了變動百分比
            this.longChangePercent = longChangePercent;
            this.shortChangePercent = shortChangePercent;
            this.longShortRatio = longShortRatio;
            this.timestamp = timestamp;
        }
        
        public String getExchange() {
            return exchange;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getLongPosition() {
            return longPosition;
        }
        
        public double getShortPosition() {
            return shortPosition;
        }
        
        public double getPreviousLongPosition() {
            return previousLongPosition;
        }
        
        public double getPreviousShortPosition() {
            return previousShortPosition;
        }
        
        public double getLongChangePercent() {
            return longChangePercent;
        }
        
        public double getShortChangePercent() {
            return shortChangePercent;
        }
        
        public double getLongShortRatio() {
            return longShortRatio;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s 持倉量 - 多頭: %.2f (變動: %.2f%%) | 空頭: %.2f (變動: %.2f%%) | 多空比: %.2f", 
                    exchange, symbol, longPosition, longChangePercent, 
                    shortPosition, shortChangePercent, longShortRatio);
        }
    }
}