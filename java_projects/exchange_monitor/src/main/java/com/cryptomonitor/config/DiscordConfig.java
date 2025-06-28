package com.cryptomonitor.config;

/**
 * Discord配置類
 */
public class DiscordConfig {
    private final String token;
    private final String channelId;
    private final String webhookUrl;
    
    public DiscordConfig(String token, String channelId, String webhookUrl) {
        this.token = token;
        this.channelId = channelId;
        this.webhookUrl = webhookUrl;
    }
    
    public String getToken() {
        return token;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    /**
     * 檢查配置是否有效
     * @return 如果配置有效返回true
     */
    public boolean isValid() {
        return isWebhookMode() || isBotMode();
    }
    
    /**
     * 檢查是否為Webhook模式
     * @return 如果是Webhook模式返回true
     */
    public boolean isWebhookMode() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty() && 
               !webhookUrl.equals("YOUR_DISCORD_WEBHOOK_URL");
    }
    
    /**
     * 檢查是否為Bot模式
     * @return 如果是Bot模式返回true
     */
    public boolean isBotMode() {
        return token != null && !token.trim().isEmpty() && 
               !token.equals("YOUR_DISCORD_BOT_TOKEN") &&
               channelId != null && !channelId.trim().isEmpty() && 
               !channelId.equals("YOUR_DISCORD_CHANNEL_ID");
    }
    
    @Override
    public String toString() {
        return "DiscordConfig{" +
                "hasToken=" + (token != null && !token.isEmpty()) +
                ", hasChannelId=" + (channelId != null && !channelId.isEmpty()) +
                ", hasWebhookUrl=" + (webhookUrl != null && !webhookUrl.isEmpty()) +
                '}';
    }
}