package com.cryptomonitor.config;

/**
 * 交易所配置類，包含連接交易所API所需的配置信息
 */
public class ExchangeConfig {
    private final String name;
    private final String apiKey;
    private final String secretKey;
    private final String baseUrl;
    private final String wsUrl;
    
    public ExchangeConfig(String name, String apiKey, String secretKey, String baseUrl, String wsUrl) {
        this.name = name;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
        this.wsUrl = wsUrl;
    }
    
    public String getName() {
        return name;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getWsUrl() {
        return wsUrl;
    }
    
    /**
     * 檢查API憑證是否有效
     * @return 如果憑證有效返回true
     */
    public boolean isCredentialsValid() {
        return apiKey != null && !apiKey.trim().isEmpty() && 
               !apiKey.startsWith("YOUR_") &&
               secretKey != null && !secretKey.trim().isEmpty() && 
               !secretKey.startsWith("YOUR_");
    }
    
    @Override
    public String toString() {
        return "ExchangeConfig{" +
                "name='" + name + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", wsUrl='" + wsUrl + '\'' +
                '}';
    }
}