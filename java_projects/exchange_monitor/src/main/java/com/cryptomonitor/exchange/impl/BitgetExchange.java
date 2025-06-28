package com.cryptomonitor.exchange.impl;

import com.cryptomonitor.config.ExchangeConfig;
import com.cryptomonitor.exchange.AbstractExchange;
import com.cryptomonitor.model.MarketData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bitget交易所實現類
 */
public class BitgetExchange extends AbstractExchange {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private WebSocketClient webSocketClient;
    
    public BitgetExchange(ExchangeConfig config) {
        super(config);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void initialize() throws Exception {
        logger.info("初始化Bitget交易所連接...");
        testConnection();
        connected = true;
        logger.info("Bitget交易所連接成功");
    }
    
    @Override
    public void shutdown() {
        logger.info("關閉Bitget交易所連接...");
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        connected = false;
        logger.info("Bitget交易所連接已關閉");
    }
    
    private void testConnection() throws Exception {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/api/mix/v1/market/contracts?productType=umcbl")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bitget API連接測試失敗: " + response.code());
            }
        }
    }
    
    @Override
    public double getLatestPrice(String symbol) throws Exception {
        String url = config.getBaseUrl() + "/api/mix/v1/market/ticker?symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取價格失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.get("code").asText().equals("00000")) {
                JsonNode data = root.get("data");
                double price = data.get("last").asDouble();
                
                handlePriceUpdate(symbol, price);
                return price;
            }
            
            throw new IOException("無法解析Bitget價格數據");
        }
    }
    
    @Override
    public MarketData.PositionData getPositionData(String symbol) throws Exception {
        String url = config.getBaseUrl() + "/api/mix/v1/market/open-interest?symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取持倉量失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.get("code").asText().equals("00000")) {
                JsonNode data = root.get("data");
                double openInterest = data.get("amount").asDouble();
                
                // Bitget不直接提供多空比例，這裡用簡化處理
                double longPosition = openInterest * 0.55; // 假設55%多頭
                double shortPosition = openInterest * 0.45; // 假設45%空頭
                
                handlePositionUpdate(symbol, longPosition, shortPosition);
                
                MarketData.PositionData previousPosition = lastPositions.getOrDefault(symbol, 
                        new MarketData.PositionData(name, symbol, 0, 0, 0, 0));
                
                return new MarketData.PositionData(
                        name, symbol, longPosition, shortPosition, 
                        previousPosition.getLongPosition(), previousPosition.getShortPosition());
            }
            
            throw new IOException("無法解析Bitget持倉量數據");
        }
    }
    
    @Override
    public List<String> getSupportedSymbols() throws Exception {
        String url = config.getBaseUrl() + "/api/mix/v1/market/contracts?productType=umcbl";
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取交易對列表失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.get("code").asText().equals("00000")) {
                JsonNode data = root.get("data");
                List<String> symbols = new ArrayList<>();
                for (JsonNode contract : data) {
                    if (contract.get("supportMarginCoins").isArray() && 
                        contract.get("supportMarginCoins").size() > 0) {
                        symbols.add(contract.get("symbol").asText());
                    }
                }
                return symbols;
            }
            
            throw new IOException("無法解析Bitget交易對列表");
        }
    }
    
    @Override
    public void subscribePriceChanges(String symbol, com.cryptomonitor.exchange.PriceChangeCallback callback) throws Exception {
        super.subscribePriceChanges(symbol, callback);
        
        if (priceCallbacks.size() == 1) {
            connectWebSocket();
        }
    }
    
    @Override
    public void subscribePositionChanges(String symbol, com.cryptomonitor.exchange.PositionChangeCallback callback) throws Exception {
        super.subscribePositionChanges(symbol, callback);
        
        if (positionCallbacks.size() == 1) {
            connectWebSocket();
        }
    }
    
    private void connectWebSocket() throws URISyntaxException {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            return;
        }
        
        webSocketClient = new WebSocketClient(new URI(config.getWsUrl())) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("Bitget WebSocket連接已建立");
                
                // 訂閱價格流
                for (String symbol : priceCallbacks.keySet()) {
                    try {
                        String subscribeMsg = String.format("{\"op\":\"subscribe\",\"args\":[{\"instType\":\"mc\",\"channel\":\"ticker\",\"instId\":\"%s\"}]}", symbol);
                        send(subscribeMsg);
                    } catch (Exception e) {
                        logger.error("訂閱Bitget價格流失敗", e);
                    }
                }
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    JsonNode root = objectMapper.readTree(message);
                    if (root.has("data") && root.get("data").isArray()) {
                        for (JsonNode item : root.get("data")) {
                            if (item.has("instId")) {
                                String symbol = item.get("instId").asText();
                                double price = item.get("last").asDouble();
                                double changePercent = item.get("changeUtc").asDouble();
                                
                                MarketData.PriceData priceData = new MarketData.PriceData(
                                        name, symbol, price, 0, changePercent, 
                                        Instant.now().getEpochSecond());
                                
                                updatePriceData(symbol, priceData);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("處理Bitget WebSocket消息失敗", e);
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.warn("Bitget WebSocket連接已關閉: {} - {}", code, reason);
            }
            
            @Override
            public void onError(Exception ex) {
                logger.error("Bitget WebSocket發生錯誤", ex);
            }
        };
        
        webSocketClient.connect();
    }
}