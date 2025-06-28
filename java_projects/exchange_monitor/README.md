# 加密貨幣交易所監控系統

## 項目概述

這是一個用Java開發的加密貨幣交易所監控系統，能夠實時監控多個主要交易所（幣安、Bybit、Bitget、OKX、Hyperliquid）的持倉量和價格變動，並將異常波動通過Discord通知用戶。

## 功能特點

- 支持多個主要交易所的API和WebSocket連接
- 監控交易對的價格變動
- 監控交易對的持倉量變動（多空持倉）
- 可配置的異常波動閾值
- Discord通知（支持Bot和Webhook兩種模式）
- 自動重連和錯誤處理機制
- 詳細的日誌記錄

## 系統需求

- Java 11或更高版本
- Maven 3.6或更高版本
- 網絡連接以訪問交易所API
- Discord Bot Token或Webhook URL

## 快速開始

### 1. 配置

編輯`src/main/resources/config.properties`文件，設置以下參數：

- Discord配置（Token或Webhook URL）
- 交易所API密鑰和密碼
- 監控參數（間隔時間、價格和持倉量變動閾值）

### 2. 編譯

```bash
mvn clean package
```

### 3. 運行

```bash
java -jar target/crypto-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 配置說明

### Discord配置

```properties
# 使用Discord Bot
discord.token=YOUR_DISCORD_BOT_TOKEN
discord.channel_id=YOUR_DISCORD_CHANNEL_ID

# 或使用Discord Webhook
discord.webhook_url=YOUR_DISCORD_WEBHOOK_URL
```

### 監控配置

```properties
# 監控間隔（秒）
monitor.interval_seconds=60

# 價格變動閾值（百分比）
monitor.price_threshold_percent=1.0

# 持倉量變動閾值（百分比）
monitor.position_threshold_percent=5.0
```

### 交易所配置

每個交易所都需要配置API密鑰、密碼和URL：

```properties
# 幣安配置示例
binance.api_key=YOUR_BINANCE_API_KEY
binance.api_secret=YOUR_BINANCE_API_SECRET
binance.base_url=https://fapi.binance.com
binance.ws_url=wss://fstream.binance.com/ws
```

## 項目結構

```
src/main/java/com/cryptomonitor/
├── Main.java                    # 程序入口
├── config/                      # 配置類
│   ├── AppConfig.java           # 應用配置
│   ├── DiscordConfig.java       # Discord配置
│   ├── ExchangeConfig.java      # 交易所配置
│   └── MonitorConfig.java       # 監控配置
├── discord/                     # Discord相關
│   └── DiscordService.java      # Discord服務
├── exchange/                    # 交易所相關
│   ├── Exchange.java            # 交易所接口
│   ├── AbstractExchange.java    # 交易所抽象類
│   ├── ExchangeManager.java     # 交易所管理器
│   └── impl/                    # 交易所實現
│       ├── BinanceExchange.java # 幣安實現
│       ├── BybitExchange.java   # Bybit實現
│       ├── BitgetExchange.java  # Bitget實現
│       ├── OKXExchange.java     # OKX實現
│       └── HyperliquidExchange.java # Hyperliquid實現
├── model/                       # 數據模型
│   └── MarketData.java          # 市場數據
└── monitor/                     # 監控相關
    └── MonitorManager.java      # 監控管理器
```

## 擴展支持

如需添加新的交易所支持，請按照以下步驟：

1. 在`config.properties`中添加新交易所的配置
2. 在`exchange/impl/`目錄下創建新的交易所實現類，繼承`AbstractExchange`
3. 在`ExchangeManager.java`中添加新交易所的初始化代碼

## 日誌

日誌文件保存在`logs/`目錄下：
- `crypto-monitor.log`: 一般日誌
- `error.log`: 錯誤日誌

## 許可證

MIT