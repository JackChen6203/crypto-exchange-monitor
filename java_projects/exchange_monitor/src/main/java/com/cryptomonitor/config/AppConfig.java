package com.cryptomonitor.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 應用程序配置類，負責加載和管理所有配置
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "config.properties";
    private static AppConfig instance;
    
    private final DiscordConfig discordConfig;
    private final List<ExchangeConfig> exchangeConfigs;
    private final MonitorConfig monitorConfig;
    
    private AppConfig() {
        // 加載配置文件
        Configuration config = loadConfiguration();
        
        // 初始化Discord配置
        this.discordConfig = new DiscordConfig(
                config.getString("discord.token", ""),
                config.getString("discord.channelId", ""),
                config.getString("discord.webhookUrl", "")
        );
        
        // 初始化監控配置
        this.monitorConfig = new MonitorConfig(
                config.getInt("monitor.interval.seconds", 60),
                config.getDouble("monitor.price.threshold.percent", 1.0),
                config.getDouble("monitor.position.threshold.percent", 5.0)
        );
        
        // 初始化交易所配置
        this.exchangeConfigs = new ArrayList<>();
        
        // 幣安配置
        if (config.getBoolean("exchange.binance.enabled", false)) {
            this.exchangeConfigs.add(new ExchangeConfig(
                    "Binance",
                    config.getString("exchange.binance.apiKey", ""),
                    config.getString("exchange.binance.secretKey", ""),
                    config.getString("exchange.binance.baseUrl", "https://api.binance.com"),
                    config.getString("exchange.binance.wsUrl", "wss://stream.binance.com:9443/ws")
            ));
        }
        
        // Bybit配置
        if (config.getBoolean("exchange.bybit.enabled", false)) {
            this.exchangeConfigs.add(new ExchangeConfig(
                    "Bybit",
                    config.getString("exchange.bybit.apiKey", ""),
                    config.getString("exchange.bybit.secretKey", ""),
                    config.getString("exchange.bybit.baseUrl", "https://api.bybit.com"),
                    config.getString("exchange.bybit.wsUrl", "wss://stream.bybit.com/v5/public")
            ));
        }
        
        // Bitget配置
        if (config.getBoolean("exchange.bitget.enabled", false)) {
            this.exchangeConfigs.add(new ExchangeConfig(
                    "Bitget",
                    config.getString("exchange.bitget.apiKey", ""),
                    config.getString("exchange.bitget.secretKey", ""),
                    config.getString("exchange.bitget.baseUrl", "https://api.bitget.com"),
                    config.getString("exchange.bitget.wsUrl", "wss://ws.bitget.com/mix/v1/stream")
            ));
        }
        
        // OKEx配置
        if (config.getBoolean("exchange.okex.enabled", false)) {
            this.exchangeConfigs.add(new ExchangeConfig(
                    "OKEx",
                    config.getString("exchange.okex.apiKey", ""),
                    config.getString("exchange.okex.secretKey", ""),
                    config.getString("exchange.okex.baseUrl", "https://www.okex.com"),
                    config.getString("exchange.okex.wsUrl", "wss://ws.okex.com:8443/ws/v5/public")
            ));
        }
        
        // Hyperliquid配置
        if (config.getBoolean("exchange.hyperliquid.enabled", false)) {
            this.exchangeConfigs.add(new ExchangeConfig(
                    "Hyperliquid",
                    config.getString("exchange.hyperliquid.apiKey", ""),
                    config.getString("exchange.hyperliquid.secretKey", ""),
                    config.getString("exchange.hyperliquid.baseUrl", "https://api.hyperliquid.xyz"),
                    config.getString("exchange.hyperliquid.wsUrl", "wss://api.hyperliquid.xyz/ws")
            ));
        }
    }
    
    /**
     * 加載配置文件
     */
    private Configuration loadConfiguration() {
        try {
            Configurations configs = new Configurations();
            File configFile = new File(CONFIG_FILE);
            
            if (!configFile.exists()) {
                logger.warn("配置文件{}不存在，將使用默認配置", CONFIG_FILE);
                return new org.apache.commons.configuration2.BaseConfiguration();
            }
            
            return configs.properties(configFile);
        } catch (ConfigurationException e) {
            logger.error("加載配置文件失敗", e);
            return new org.apache.commons.configuration2.BaseConfiguration();
        }
    }
    
    /**
     * 獲取AppConfig單例實例
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    public DiscordConfig getDiscordConfig() {
        return discordConfig;
    }
    
    public List<ExchangeConfig> getExchangeConfigs() {
        return exchangeConfigs;
    }
    
    public MonitorConfig getMonitorConfig() {
        return monitorConfig;
    }
}