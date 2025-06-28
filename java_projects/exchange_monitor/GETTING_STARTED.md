# 🚀 快速開始指南

歡迎使用加密貨幣交易所監控系統！這個指南將幫助您在5分鐘內完成配置和部署。

## 📋 前置需求

### 必需
- **Java 17+** - [下載地址](https://adoptium.net/)
- **Maven 3.8+** - [下載地址](https://maven.apache.org/download.cgi)
- **Discord頻道** - 用於接收通知

### 可選
- **Docker** - [下載地址](https://www.docker.com/products/docker-desktop) (推薦用於生產部署)
- **交易所API密鑰** - 獲得更詳細的數據

## ⚡ 一鍵部署

### Linux/Mac用戶
```bash
# 使用快速開始腳本
./quick-start.sh
```

### Windows用戶
```powershell
# 在PowerShell中執行
.\quick-start.ps1
```

## 📝 手動配置（如果您偏好手動設置）

### 1. Discord設定

#### 方式A: Discord Bot（推薦）
1. 前往 [Discord Developer Portal](https://discord.com/developers/applications)
2. 創建新應用程式 → Bot → 複製Token
3. 在OAuth2中生成邀請連結（需要`Send Messages`權限）
4. 邀請Bot到您的伺服器
5. 獲取頻道ID（開啟開發者模式，右鍵點擊頻道）

#### 方式B: Discord Webhook（簡單）
1. 前往目標頻道設定
2. 整合 → Webhook → 創建Webhook
3. 複製Webhook URL

### 2. 配置應用
```bash
# 複製配置文件
cp src/main/resources/config.properties .

# 編輯配置
nano config.properties  # 或使用您喜歡的編輯器
```

### 3. 基本配置
```properties
# Discord設定（擇一）
discord.token=YOUR_BOT_TOKEN
discord.channelId=YOUR_CHANNEL_ID
# 或
discord.webhookUrl=YOUR_WEBHOOK_URL

# 監控設定
monitor.interval.seconds=60
monitor.price.threshold.percent=1.0
monitor.position.threshold.percent=5.0

# 交易所啟用
exchange.binance.enabled=true
exchange.bybit.enabled=true
exchange.bitget.enabled=false
exchange.okex.enabled=true
exchange.hyperliquid.enabled=false
```

### 4. 構建和運行
```bash
# 構建應用
mvn clean package

# 運行
java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 🐳 Docker部署（推薦）

```bash
# 快速Docker部署
./deploy.sh build
./deploy.sh start

# 查看日誌
./deploy.sh logs
```

## 📱 使用方式

啟動後系統將：
- ✅ 每60秒檢查交易所數據
- ✅ 價格變動超過1%時發送通知
- ✅ 持倉量變動超過5%時發送通知
- ✅ 自動記錄所有活動

### Discord通知範例
```
🚨 價格警報
交易所: Binance
交易對: BTC/USDT
當前價格: $45,123.45
變動: +2.34% (↗️)
時間: 2024-01-15 14:30:25
```

## 🔧 常用命令

### 直接運行
```bash
# 前台運行（測試用）
java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar

# 背景運行
nohup java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar > logs/app.log 2>&1 &

# 查看日誌
tail -f logs/crypto-monitor.log
```

### Docker管理
```bash
./deploy.sh start     # 啟動服務
./deploy.sh stop      # 停止服務
./deploy.sh restart   # 重啟服務
./deploy.sh logs      # 查看日誌
./deploy.sh status    # 查看狀態
```

### Windows PowerShell
```powershell
# 前台運行
java -jar "target\crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"

# 查看日誌
Get-Content logs\crypto-monitor.log -Wait

# 停止服務
Stop-Process -Name java -Force
```

## 🔧 常見問題

### Q: Discord連接失敗？
**A:** 檢查：
- Bot Token是否正確
- Bot是否已加入伺服器
- 頻道ID是否正確
- 網絡連接是否正常

### Q: 記憶體使用過高？
**A:** 調整JVM設定：
```bash
export JAVA_OPTS="-Xmx512m -Xms256m"
```

### Q: 交易所連接錯誤？
**A:** 
- 檢查網絡連接
- 確認交易所服務正常
- 如使用API密鑰，請驗證密鑰有效性

### Q: 想要監控特定交易對？
**A:** 在配置文件中修改或聯繫開發者擴展功能。

## 📚 進階文檔

- **詳細配置指南**: [DEPLOYMENT_CONFIG.md](DEPLOYMENT_CONFIG.md)
- **完整部署文檔**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **專案總結**: [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)
- **任務清單**: [tasks.md](tasks.md)

## 💬 支援

如果遇到問題：
1. 檢查日誌文件 `logs/crypto-monitor.log`
2. 確認配置文件格式正確
3. 驗證網絡連接和Discord設定

## 🎯 下一步

系統運行後，您可以：
- 調整監控閾值
- 添加更多交易所
- 擴展監控指標
- 設置更複雜的通知規則

祝您使用愉快！🚀 