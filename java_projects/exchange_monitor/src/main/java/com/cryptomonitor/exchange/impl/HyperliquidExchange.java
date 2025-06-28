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
 * Hyperliquid交易所實現類
 */
public class HyperliquidExchange extends AbstractExchange {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private WebSocketClient webSocketClient;
    
    public HyperliquidExchange(ExchangeConfig config) {
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
        logger.info("初始化Hyperliquid交易所連接...");
        testConnection();
        connected = true;
        logger.info("Hyperliquid交易所連接成功");
    }
    
    @Override
    public void shutdown() {
        logger.info("關閉Hyperliquid交易所連接...");
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        connected = false;
        logger.info("Hyperliquid交易所連接已關閉");
    }
    
    private void testConnection() throws Exception {
        // Hyperliquid使用POST請求獲取交易信息
        String requestBody = "{\"type\":\"meta\"}";
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/info")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Hyperliquid API連接測試失敗: " + response.code());
            }
        }
    }
    
    @Override
    public double getLatestPrice(String symbol) throws Exception {
        String requestBody = "{\"type\":\"l2Book\",\"coin\":\"" + symbol + "\"}";
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/info")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取價格失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.has("levels") && root.get("levels").isArray()) {
                JsonNode levels = root.get("levels");
                if (levels.size() >= 2) {
                    // 取買一和賣一的中間價格
                    double bidPrice = levels.get(0).get(0).get(0).asDouble();
                    double askPrice = levels.get(1).get(0).get(0).asDouble();
                    double price = (bidPrice + askPrice) / 2;
                    
                    handlePriceUpdate(symbol, price);
                    return price;
                }
            }
            
            throw new IOException("無法解析Hyperliquid價格數據");
        }
    }
    
    @Override
    public MarketData.PositionData getPositionData(String symbol) throws Exception {
        String requestBody = "{\"type\":\"openInterest\",\"coin\":\"" + symbol + "\"}";
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/info")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取持倉量失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.has("openInterest")) {
                double openInterest = root.get("openInterest").asDouble();
                
                // Hyperliquid不直接提供多空比例，這裡用簡化處理
                double longPosition = openInterest * 0.58; // 假設58%多頭
                double shortPosition = openInterest * 0.42; // 假設42%空頭
                
                handlePositionUpdate(symbol, longPosition, shortPosition);
                
                MarketData.PositionData previousPosition = lastPositions.getOrDefault(symbol, 
                        new MarketData.PositionData(name, symbol, 0, 0, 0, 0));
                
                return new MarketData.PositionData(
                        name, symbol, longPosition, shortPosition, 
                        previousPosition.getLongPosition(), previousPosition.getShortPosition());
            }
            
            throw new IOException("無法解析Hyperliquid持倉量數據");
        }
    }
    
    @Override
    public List<String> getSupportedSymbols() throws Exception {
        String requestBody = "{\"type\":\"meta\"}";
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/info")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取交易對列表失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            
            if (root.has("universe") && root.get("universe").isArray()) {
                JsonNode universe = root.get("universe");
                List<String> symbols = new ArrayList<>();
                for (JsonNode item : universe) {
                    if (item.has("name")) {
                        symbols.add(item.get("name").asText());
                    }
                }
                return symbols;
            }
            
            throw new IOException("無法解析Hyperliquid交易對列表");
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
                logger.info("Hyperliquid WebSocket連接已建立");
                
                // 訂閱價格流
                for (String symbol : priceCallbacks.keySet()) {
                    try {
                        String subscribeMsg = String.format("{\"method\":\"subscribe\",\"subscription\":{\"type\":\"l2Book\",\"coin\":\"%s\"}}", symbol);
                        send(subscribeMsg);
                    } catch (Exception e) {
                        logger.error("訂閱Hyperliquid價格流失敗", e);
                    }
                }
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    JsonNode root = objectMapper.readTree(message);
                    if (root.has("data") && root.has("channel")) {
                        String channel = root.get("channel").asText();
                        JsonNode data = root.get("data");
                        
                        if (channel.equals("l2Book") && data.has("coin")) {
                            String symbol = data.get("coin").asText();
                            
                            if (data.has("levels") && data.get("levels").isArray()) {
                                JsonNode levels = data.get("levels");
                                if (levels.size() >= 2) {
                                    // 取買一和賣一的中間價格
                                    double bidPrice = levels.get(0).get(0).get(0).asDouble();
                                    double askPrice = levels.get(1).get(0).get(0).asDouble();
                                    double price = (bidPrice + askPrice) / 2;
                                    
                                    MarketData.PriceData priceData = new MarketData.PriceData(
                                            name, symbol, price, 0, 0, 
                                            Instant.now().getEpochSecond());
                                    
                                    updatePriceData(symbol, priceData);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("處理Hyperliquid WebSocket消息失敗", e);
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.warn("Hyperliquid WebSocket連接已關閉: {} - {}", code, reason);
            }
            
            @Override
            public void onError(Exception ex) {
                logger.error("Hyperliquid WebSocket發生錯誤", ex);
            }
        };
        
        webSocketClient.connect();
    }
}