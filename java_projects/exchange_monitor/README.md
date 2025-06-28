# 🚀 加密貨幣交易所監控系統

一個全面的加密貨幣交易所數據監控系統，支援多個主流交易所的價格和持倉量監控，並通過 Discord 發送即時警報。

## ✨ 核心功能

- 📊 **多交易所支援**: Binance、Bybit、Bitget、OKX、Hyperliquid
- 🔔 **Discord 通知**: 支援 Bot Token 和 Webhook 兩種模式
- ⚡ **即時監控**: 價格變動和持倉量異動警報
- 🔧 **智能管理**: 自動跳過未配置 API 的交易所
- 🐳 **Docker 部署**: 一鍵部署，支援多平台
- 📱 **上線通知**: 系統啟動時自動發送 Discord 通知

## 🚀 快速開始

### 1. 克隆專案

```bash
git clone <repository-url>
cd exchange_monitor
```

### 2. 配置系統

**📖 詳細配置請參考**: [**CONFIG_GUIDE.md**](CONFIG_GUIDE.md)

```bash
# 複製配置文件
cp config.properties.example config.properties

# 編輯配置文件，設置 Discord 和交易所 API
nano config.properties
```

### 3. 啟動系統

```bash
# Docker 部署
docker-compose up -d

# 或使用快速部署腳本
./quick-deploy.ps1 -Platform docker    # Windows
./quick-start.sh                       # Linux/Mac
```

### 4. 驗證運行

```bash
# 查看日誌
docker-compose logs -f

# 檢查容器狀態
docker-compose ps
```

## 📋 系統要求

- Docker & Docker Compose
- 至少一個交易所的 API 憑證（僅需讀取權限）
- Discord Bot Token 或 Webhook URL

## 🔧 配置說明

### Discord 設置（二選一）

#### 方法一：Webhook（推薦）
```properties
discord.webhook_url=https://discord.com/api/webhooks/YOUR_WEBHOOK_URL
```

#### 方法二：Bot Token
```properties
discord.token=YOUR_BOT_TOKEN
discord.channel_id=YOUR_CHANNEL_ID
```

### 交易所 API 設置

```properties
# 僅需要讀取權限的 API Key
binance.api_key=YOUR_BINANCE_API_KEY
binance.api_secret=YOUR_BINANCE_SECRET_KEY

bybit.api_key=YOUR_BYBIT_API_KEY
bybit.api_secret=YOUR_BYBIT_SECRET_KEY
# ... 其他交易所
```

**⚠️ 安全提醒**: API Key 僅需要讀取權限，切勿開啟交易權限！

## 📊 監控參數

```properties
# 監控間隔（秒）
monitor.interval_seconds=60

# 價格變動閾值（%）
monitor.price_threshold_percent=1.0

# 持倉量變動閾值（%）
monitor.position_threshold_percent=5.0

# 監控交易對
monitor.symbols=BTCUSDT,ETHUSDT,SOLUSDT
```

## 🎯 功能特色

### 智能交易所管理
- ✅ 自動檢查 API 憑證有效性
- ⏭️ 跳過未配置或無效的交易所
- 📝 清晰的日誌輸出

### 多樣化通知
- 🎨 美觀的 Discord Embed 消息
- 🚀 系統啟動通知
- 📈 價格異動警報
- 📊 持倉量變動警報

### 容器化部署
- 🐳 Docker 支援
- 🔄 自動重啟
- 📋 健康檢查
- 📊 資源限制

## 📂 專案結構

```
exchange_monitor/
├── src/main/java/com/cryptomonitor/
│   ├── Main.java                    # 應用程式入口
│   ├── config/                      # 配置管理
│   ├── discord/                     # Discord 服務
│   ├── exchange/                    # 交易所抽象層
│   │   └── impl/                    # 交易所實現
│   ├── monitor/                     # 監控管理
│   └── model/                       # 數據模型
├── docker-compose.yml              # Docker 編排
├── Dockerfile                      # Docker 鏡像
├── config.properties               # 主配置文件
├── CONFIG_GUIDE.md                 # 詳細配置指南
└── quick-deploy.ps1                # 快速部署腳本
```

## 🔍 日誌示例

```
✅ 已創建交易所: Binance
✅ 已創建交易所: Bybit
✅ 交易所管理器初始化完成，共啟用 2 個交易所
✅ Discord服務初始化完成
✅ 已發送系統啟動通知
✅ 監控已啟動，間隔時間: 60秒
```

## 🛠️ 開發指南

### 本地開發

```bash
# 編譯專案
mvn clean package

# 運行測試
mvn test

# 啟動應用
java -jar target/crypto-exchange-monitor-*-jar-with-dependencies.jar
```

### 添加新交易所

1. 在 `exchange/impl/` 中實現 `Exchange` 接口
2. 在 `ExchangeManager` 中註冊新交易所
3. 添加相應的配置項

## 🐛 故障排除

### 常見問題

- **Discord 無法發送**: 檢查 Webhook URL 或 Bot 權限
- **交易所連接失敗**: 驗證 API 憑證和權限
- **監控無反應**: 確認至少配置一個有效交易所

### 檢查日誌

```bash
# 即時日誌
docker-compose logs -f

# 查看錯誤
docker-compose logs | grep ERROR
```

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支
3. 提交變更
4. 創建 Pull Request

## 📄 授權條款

MIT License - 詳見 [LICENSE](LICENSE) 文件

## 📞 技術支援

- 📖 **詳細配置**: [CONFIG_GUIDE.md](CONFIG_GUIDE.md)
- 🐛 **問題回報**: GitHub Issues
- 📧 **技術討論**: GitHub Discussions

---

⭐ 如果這個專案對你有幫助，請給個 Star！