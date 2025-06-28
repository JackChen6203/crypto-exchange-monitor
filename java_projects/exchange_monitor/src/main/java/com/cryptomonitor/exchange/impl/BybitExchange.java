package com.cryptomonitor.exchange.impl;

import com.cryptomonitor.config.ExchangeConfig;
import com.cryptomonitor.exchange.AbstractExchange;
import com.cryptomonitor.model.MarketData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bybit交易所實現類
 */
public class BybitExchange extends AbstractExchange {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private WebSocketClient webSocketClient;
    
    public BybitExchange(ExchangeConfig config) {
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
        logger.info("初始化Bybit交易所連接...");
        testConnection();
        connected = true;
        logger.info("Bybit交易所連接成功");
    }
    
    @Override
    public void shutdown() {
        logger.info("關閉Bybit交易所連接...");
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        connected = false;
        logger.info("Bybit交易所連接已關閉");
    }
    
    private void testConnection() throws Exception {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/v5/market/time")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bybit API連接測試失敗: " + response.code());
            }
        }
    }
    
    @Override
    public double getLatestPrice(String symbol) throws Exception {
        String url = config.getBaseUrl() + "/v5/market/tickers?category=linear&symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取價格失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.get("result");
            if (result.has("list") && result.get("list").isArray() && result.get("list").size() > 0) {
                JsonNode ticker = result.get("list").get(0);
                double price = ticker.get("lastPrice").asDouble();
                
                handlePriceUpdate(symbol, price);
                return price;
            }
            
            throw new IOException("無法解析Bybit價格數據");
        }
    }
    
    @Override
    public MarketData.PositionData getPositionData(String symbol) throws Exception {
        String url = config.getBaseUrl() + "/v5/market/open-interest?category=linear&symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取持倉量失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.get("result");
            if (result.has("list") && result.get("list").isArray() && result.get("list").size() > 0) {
                JsonNode data = result.get("list").get(0);
                double openInterest = data.get("openInterest").asDouble();
                
                // Bybit不直接提供多空比例，這裡用簡化處理
                double longPosition = openInterest * 0.6; // 假設60%多頭
                double shortPosition = openInterest * 0.4; // 假設40%空頭
                
                handlePositionUpdate(symbol, longPosition, shortPosition);
                
                MarketData.PositionData previousPosition = lastPositions.getOrDefault(symbol, 
                        new MarketData.PositionData(name, symbol, 0, 0, 0, 0));
                
                return new MarketData.PositionData(
                        name, symbol, longPosition, shortPosition, 
                        previousPosition.getLongPosition(), previousPosition.getShortPosition());
            }
            
            throw new IOException("無法解析Bybit持倉量數據");
        }
    }
    
    /**
     * 生成API請求簽名
     */
    private String generateSignature(String params) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(config.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(params.getBytes(StandardCharsets.UTF_8));
        
        // 轉換為十六進制字符串
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
    
    @Override
    public List<String> getSupportedSymbols() throws Exception {
        String url = config.getBaseUrl() + "/v5/market/instruments-info?category=linear";
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取交易對列表失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.get("result");
            JsonNode instruments = result.get("list");
            
            List<String> symbols = new ArrayList<>();
            for (JsonNode instrument : instruments) {
                if (instrument.get("status").asText().equals("Trading")) {
                    symbols.add(instrument.get("symbol").asText());
                }
            }
            
            return symbols;
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
                logger.info("Bybit WebSocket連接已建立");
                
                // 訂閱價格流
                for (String symbol : priceCallbacks.keySet()) {
                    try {
                        String subscribeMsg = String.format("{\"op\":\"subscribe\",\"args\":[\"tickers.%s\"]}", symbol);
                        send(subscribeMsg);
                    } catch (Exception e) {
                        logger.error("訂閱Bybit價格流失敗", e);
                    }
                }
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    JsonNode root = objectMapper.readTree(message);
                    if (root.has("topic") && root.has("data")) {
                        String topic = root.get("topic").asText();
                        if (topic.startsWith("tickers.")) {
                            JsonNode data = root.get("data");
                            String symbol = data.get("symbol").asText();
                            double price = data.get("lastPrice").asDouble();
                            double changePercent = data.get("price24hPcnt").asDouble() * 100;
                            
                            MarketData.PriceData priceData = new MarketData.PriceData(
                                    name, symbol, price, 0, changePercent, 
                                    Instant.now().getEpochSecond());
                            
                            updatePriceData(symbol, priceData);
                        }
                    }
                } catch (Exception e) {
                    logger.error("處理Bybit WebSocket消息失敗", e);
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.warn("Bybit WebSocket連接已關閉: {} - {}", code, reason);
            }
            
            @Override
            public void onError(Exception ex) {
                logger.error("Bybit WebSocket發生錯誤", ex);
            }
        };
        
        webSocketClient.connect();
    }
}