package com.cryptomonitor.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarketDataTest {

    @Test
    void testPriceData_Creation() {
        MarketData.PriceData priceData = new MarketData.PriceData(
            "Binance", "BTCUSDT", 51000.0, 50000.0
        );
        
        assertEquals("Binance", priceData.getExchange());
        assertEquals("BTCUSDT", priceData.getSymbol());
        assertEquals(51000.0, priceData.getPrice());
        assertEquals(50000.0, priceData.getPreviousPrice());
        assertEquals(2.0, priceData.getChangePercent(), 0.01);
    }

    @Test
    void testPriceData_NegativeChange() {
        MarketData.PriceData priceData = new MarketData.PriceData(
            "Bybit", "ETHUSDT", 2940.0, 3000.0
        );
        
        assertEquals(-2.0, priceData.getChangePercent(), 0.01);
    }

    @Test
    void testPositionData_Creation() {
        MarketData.PositionData positionData = new MarketData.PositionData(
            "Binance", "BTCUSDT", 62.0, 38.0, 60.0, 40.0
        );
        
        assertEquals("Binance", positionData.getExchange());
        assertEquals("BTCUSDT", positionData.getSymbol());
        assertEquals(62.0, positionData.getLongPosition());
        assertEquals(38.0, positionData.getShortPosition());
        assertTrue(positionData.getTimestamp() > 0);
    }

    @Test
    void testPositionData_ChangeCalculation() {
        MarketData.PositionData positionData = new MarketData.PositionData(
            "Binance", "BTCUSDT", 62.0, 38.0, 60.0, 40.0
        );
        
        // Long change: (62-60)/60 * 100 = 3.33%
        assertEquals(3.33, positionData.getLongChangePercent(), 0.1);
        // Short change: (38-40)/40 * 100 = -5%
        assertEquals(-5.0, positionData.getShortChangePercent(), 0.1);
    }

    @Test
    void testToString() {
        MarketData.PriceData priceData = new MarketData.PriceData(
            "Binance", "BTCUSDT", 51000.0, 50000.0
        );
        
        String result = priceData.toString();
        assertTrue(result.contains("Binance"));
        assertTrue(result.contains("BTCUSDT"));
        assertTrue(result.contains("51000"));
        assertTrue(result.contains("2.00%"));
    }
} 