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
 * 幣安交易所實現類
 */
public class BinanceExchange extends AbstractExchange {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private WebSocketClient webSocketClient;
    
    public BinanceExchange(ExchangeConfig config) {
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
        logger.info("初始化幣安交易所連接...");
        // 測試連接
        testConnection();
        connected = true;
        logger.info("幣安交易所連接成功");
    }
    
    @Override
    public void shutdown() {
        logger.info("關閉幣安交易所連接...");
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        connected = false;
        logger.info("幣安交易所連接已關閉");
    }
    
    /**
     * 測試API連接
     */
    private void testConnection() throws Exception {
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/api/v3/ping")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("幣安API連接測試失敗: " + response.code());
            }
        }
    }
    
    @Override
    public double getLatestPrice(String symbol) throws Exception {
        String url = config.getBaseUrl() + "/api/v3/ticker/price?symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取價格失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            double price = root.get("price").asDouble();
            
            // 更新緩存並通知訂閱者
            handlePriceUpdate(symbol, price);
            
            return price;
        }
    }
    
    @Override
    public MarketData.PositionData getPositionData(String symbol) throws Exception {
        // 幣安的持倉量數據需要通過Open Interest API獲取
        String url = config.getBaseUrl() + "/fapi/v1/openInterest?symbol=" + symbol;
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取持倉量失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            double openInterest = root.get("openInterest").asDouble();
            
            // 獲取多空比例數據
            url = config.getBaseUrl() + "/fapi/v1/globalLongShortAccountRatio?symbol=" + symbol + "&period=5m&limit=1";
            request = new Request.Builder().url(url).build();
            
            try (Response ratioResponse = httpClient.newCall(request).execute()) {
                if (!ratioResponse.isSuccessful()) {
                    throw new IOException("獲取多空比例失敗: " + ratioResponse.code());
                }
                
                responseBody = ratioResponse.body().string();
                JsonNode ratioRoot = objectMapper.readTree(responseBody);
                if (ratioRoot.isArray() && ratioRoot.size() > 0) {
                    JsonNode ratioData = ratioRoot.get(0);
                    double longShortRatio = ratioData.get("longShortRatio").asDouble();
                    
                    // 根據多空比例計算多頭和空頭持倉量
                    double longPosition = (openInterest * longShortRatio) / (1 + longShortRatio);
                    double shortPosition = openInterest - longPosition;
                    
                    // 更新緩存並通知訂閱者
                    handlePositionUpdate(symbol, longPosition, shortPosition);
                    
                    // 獲取上一次的持倉數據
                    MarketData.PositionData previousPosition = lastPositions.getOrDefault(symbol, 
                            new MarketData.PositionData(name, symbol, 0, 0, 0, 0));
                    
                    return new MarketData.PositionData(
                            name, symbol, longPosition, shortPosition, 
                            previousPosition.getLongPosition(), previousPosition.getShortPosition());
                }
            }
            
            throw new IOException("無法解析持倉量數據");
        }
    }
    
    @Override
    public List<String> getSupportedSymbols() throws Exception {
        String url = config.getBaseUrl() + "/api/v3/exchangeInfo";
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("獲取交易對列表失敗: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode symbols = root.get("symbols");
            
            List<String> result = new ArrayList<>();
            for (JsonNode symbol : symbols) {
                if (symbol.get("status").asText().equals("TRADING")) {
                    result.add(symbol.get("symbol").asText());
                }
            }
            
            return result;
        }
    }
    
    @Override
    public void subscribePriceChanges(String symbol, com.cryptomonitor.exchange.PriceChangeCallback callback) throws Exception {
        super.subscribePriceChanges(symbol, callback);
        
        // 如果是第一個訂閱，則創建WebSocket連接
        if (priceCallbacks.size() == 1) {
            connectWebSocket();
        }
    }
    
    @Override
    public void subscribePositionChanges(String symbol, com.cryptomonitor.exchange.PositionChangeCallback callback) throws Exception {
        super.subscribePositionChanges(symbol, callback);
        
        // 如果是第一個訂閱，則創建WebSocket連接
        if (positionCallbacks.size() == 1) {
            connectWebSocket();
        }
    }
    
    /**
     * 連接WebSocket
     */
    private void connectWebSocket() throws URISyntaxException {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            return;
        }
        
        // 創建訂閱列表
        List<String> streams = new ArrayList<>();
        for (String symbol : priceCallbacks.keySet()) {
            streams.add(symbol.toLowerCase() + "@ticker");
        }
        
        // 如果沒有需要訂閱的流，則返回
        if (streams.isEmpty()) {
            return;
        }
        
        String streamUrl = config.getWsUrl() + "/" + String.join("/", streams);
        webSocketClient = new WebSocketClient(new URI(streamUrl)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("幣安WebSocket連接已建立");
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    JsonNode root = objectMapper.readTree(message);
                    if (root.has("stream") && root.has("data")) {
                        String stream = root.get("stream").asText();
                        JsonNode data = root.get("data");
                        
                        if (stream.endsWith("@ticker")) {
                            String symbol = data.get("s").asText();
                            double price = data.get("c").asDouble(); // 收盤價格
                            double changePercent = data.get("P").asDouble(); // 24小時價格變動百分比
                            
                            // 創建價格數據對象
                            MarketData.PriceData priceData = new MarketData.PriceData(
                                    name, symbol, price, 0, changePercent, 
                                    Instant.now().getEpochSecond());
                            
                            // 更新緩存並通知訂閱者
                            updatePriceData(symbol, priceData);
                        }
                    }
                } catch (Exception e) {
                    logger.error("處理幣安WebSocket消息失敗", e);
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.warn("幣安WebSocket連接已關閉: {} - {}", code, reason);
                // 可以實現重連邏輯
            }
            
            @Override
            public void onError(Exception ex) {
                logger.error("幣安WebSocket發生錯誤", ex);
            }
        };
        
        webSocketClient.connect();
    }
}