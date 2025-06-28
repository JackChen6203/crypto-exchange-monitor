package com.cryptomonitor;

import com.cryptomonitor.config.AppConfig;
import com.cryptomonitor.discord.DiscordService;
import com.cryptomonitor.exchange.ExchangeManager;
import com.cryptomonitor.monitor.MonitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加密貨幣交易所數據監控應用程序的主入口點
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("啟動加密貨幣交易所數據監控系統...");
        
        try {
            // 加載配置
            AppConfig appConfig = AppConfig.getInstance();
            logger.info("配置加載完成");
            
            // 初始化Discord服務
            DiscordService discordService = new DiscordService(appConfig.getDiscordConfig());
            logger.info("Discord服務初始化完成");
            
            // 初始化交易所管理器
            ExchangeManager exchangeManager = new ExchangeManager(appConfig.getExchangeConfigs());
            logger.info("交易所管理器初始化完成，已配置{}個交易所", exchangeManager.getExchangeCount());
            
            // 初始化監控管理器
            MonitorManager monitorManager = new MonitorManager(exchangeManager, discordService, appConfig.getMonitorConfig());
            logger.info("監控管理器初始化完成");
            
            // 啟動監控
            monitorManager.startMonitoring();
            logger.info("監控已啟動");
            
            // 添加關閉鉤子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在關閉系統...");
                monitorManager.stopMonitoring();
                exchangeManager.shutdown();
                discordService.shutdown();
                logger.info("系統已安全關閉");
            }));
            
            logger.info("系統已完全啟動，按Ctrl+C停止");
        } catch (Exception e) {
            logger.error("系統啟動失敗", e);
            System.exit(1);
        }
    }
}