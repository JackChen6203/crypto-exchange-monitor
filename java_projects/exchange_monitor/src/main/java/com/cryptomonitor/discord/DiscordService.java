package com.cryptomonitor.discord;

import com.cryptomonitor.config.DiscordConfig;
import com.cryptomonitor.model.MarketData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Discord服務類，負責發送通知到Discord
 */
public class DiscordService {
    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    private final DiscordConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private JDA jdaClient;
    private TextChannel channel;
    
    public DiscordService(DiscordConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        
        if (config.isValid()) {
            initialize();
        } else {
            logger.warn("Discord配置無效，無法初始化Discord服務");
        }
    }
    
    /**
     * 初始化Discord客戶端
     */
    private void initialize() {
        if (config.isWebhookMode()) {
            logger.info("使用Webhook模式初始化Discord服務");
            // Webhook模式不需要初始化JDA客戶端
        } else {
            logger.info("使用Bot模式初始化Discord服務");
            try {
                jdaClient = JDABuilder.createDefault(config.getToken())
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                        .build();
                
                // 等待JDA客戶端就緒
                jdaClient.awaitReady();
                
                // 獲取目標頻道
                channel = jdaClient.getTextChannelById(config.getChannelId());
                if (channel == null) {
                    logger.error("無法找到指定的Discord頻道: {}", config.getChannelId());
                } else {
                    logger.info("Discord Bot已連接到頻道: {}", channel.getName());
                }
            } catch (Exception e) {
                logger.error("初始化Discord Bot失敗", e);
            }
        }
    }
    
    /**
     * 關閉Discord服務
     */
    public void shutdown() {
        if (jdaClient != null) {
            jdaClient.shutdown();
            logger.info("Discord Bot已關閉");
        }
    }
    
    /**
     * 發送價格變動通知
     * @param priceData 價格數據
     */
    public void sendPriceAlert(MarketData.PriceData priceData) {
        try {
            // 只有當價格變動超過閾值時才發送通知
            if (Math.abs(priceData.getChangePercent()) < 0.5) {
                return;
            }
            
            String title = String.format("%s %s 價格異動警報", priceData.getExchange(), priceData.getSymbol());
            String description = String.format("當前價格: %.8f\n變動百分比: %.2f%%\n前一價格: %.8f", 
                    priceData.getPrice(), priceData.getChangePercent(), priceData.getPreviousPrice());
            
            // 根據價格變動方向設置顏色
            Color color = priceData.getChangePercent() > 0 ? Color.GREEN : Color.RED;
            
            sendEmbed(title, description, color, priceData.getTimestamp());
            logger.info("已發送價格異動通知: {}", title);
        } catch (Exception e) {
            logger.error("發送價格異動通知失敗", e);
        }
    }
    
    /**
     * 發送持倉量變動通知
     * @param positionData 持倉量數據
     */
    public void sendPositionAlert(MarketData.PositionData positionData) {
        try {
            // 只有當持倉量變動超過閾值時才發送通知
            if (Math.abs(positionData.getLongChangePercent()) < 1.0 && 
                Math.abs(positionData.getShortChangePercent()) < 1.0) {
                return;
            }
            
            String title = String.format("%s %s 持倉量異動警報", positionData.getExchange(), positionData.getSymbol());
            String description = String.format("多頭持倉: %.2f (變動: %.2f%%)\n空頭持倉: %.2f (變動: %.2f%%)\n多空比: %.2f", 
                    positionData.getLongPosition(), positionData.getLongChangePercent(), 
                    positionData.getShortPosition(), positionData.getShortChangePercent(), 
                    positionData.getLongShortRatio());
            
            // 根據多空比變動設置顏色
            Color color = positionData.getLongChangePercent() > positionData.getShortChangePercent() ? 
                    Color.GREEN : Color.RED;
            
            sendEmbed(title, description, color, positionData.getTimestamp());
            logger.info("已發送持倉量異動通知: {}", title);
        } catch (Exception e) {
            logger.error("發送持倉量異動通知失敗", e);
        }
    }
    
    /**
     * 發送嵌入消息
     * @param title 標題
     * @param description 描述
     * @param color 顏色
     * @param timestamp 時間戳
     */
    private void sendEmbed(String title, String description, Color color, long timestamp) {
        if (config.isWebhookMode()) {
            sendWebhookEmbed(title, description, color, timestamp);
        } else if (channel != null) {
            sendBotEmbed(title, description, color, timestamp);
        } else {
            logger.warn("Discord服務未正確初始化，無法發送消息");
        }
    }
    
    /**
     * 使用Bot發送嵌入消息
     */
    private void sendBotEmbed(String title, String description, Color color, long timestamp) {
        try {
            net.dv8tion.jda.api.EmbedBuilder embedBuilder = new net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(color)
                    .setTimestamp(Instant.ofEpochSecond(timestamp));
            
            channel.sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            logger.error("使用Bot發送嵌入消息失敗", e);
        }
    }
    
    /**
     * 使用Webhook發送嵌入消息
     */
    private void sendWebhookEmbed(String title, String description, Color color, long timestamp) {
        try {
            // 構建Webhook請求
            ObjectNode rootNode = objectMapper.createObjectNode();
            ObjectNode embedNode = objectMapper.createObjectNode();
            
            embedNode.put("title", title);
            embedNode.put("description", description);
            embedNode.put("color", color.getRGB() & 0xFFFFFF); // Discord需要的顏色格式
            
            // 格式化時間
            String formattedTime = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            embedNode.put("timestamp", formattedTime);
            
            rootNode.putArray("embeds").add(embedNode);
            
            // 發送Webhook請求
            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(rootNode),
                    MediaType.parse("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(config.getWebhookUrl())
                    .post(body)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("發送Webhook失敗: " + response.code());
                }
            }
        } catch (Exception e) {
            logger.error("使用Webhook發送嵌入消息失敗", e);
        }
    }
}