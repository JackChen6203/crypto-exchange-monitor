# 🚀 加密貨幣監控系統配置和部署指南

## 📋 準備工作

### 必需的Discord設定

#### 方式1: Discord Bot（推薦）
1. 前往 [Discord Developer Portal](https://discord.com/developers/applications)
2. 創建新應用程式 → 創建Bot
3. 複製Bot Token
4. 在OAuth2 → URL Generator中選擇：
   - `bot` scope
   - `Send Messages` 權限
5. 使用生成的URL將Bot添加到您的伺服器
6. 開啟開發者模式，右鍵點擊頻道獲取頻道ID

#### 方式2: Discord Webhook（簡單）
1. 在目標頻道 → 頻道設定 → 整合 → Webhook
2. 創建新Webhook，複製URL

### 可選的交易所API設定
如果您有交易所API密鑰，可以獲得更詳細的數據：
- **Binance**: https://www.binance.com/en/my/settings/api-management
- **Bybit**: https://www.bybit.com/app/user/api-management
- **Bitget**: https://www.bitget.com/api/doc#overview
- **OKX**: https://www.okx.com/account/my-api
- **Hyperliquid**: https://app.hyperliquid.xyz/API

## ⚙️ 配置步驟

### 1. 複製配置文件
```bash
cd exchange_monitor
cp src/main/resources/config.properties .
```

### 2. 編輯配置文件
```bash
nano config.properties
# 或使用您喜歡的編輯器
```

### 3. 基本配置範例
```properties
# === Discord設定（擇一配置） ===
# Bot模式
discord.token=YOUR_BOT_TOKEN
discord.channelId=YOUR_CHANNEL_ID

# 或Webhook模式
discord.webhookUrl=YOUR_WEBHOOK_URL

# === 監控設定 ===
monitor.interval.seconds=60                    # 檢查間隔60秒
monitor.price.threshold.percent=1.0            # 價格變動1%觸發通知
monitor.position.threshold.percent=5.0         # 持倉變動5%觸發通知

# === 交易所啟用設定 ===
exchange.binance.enabled=true
exchange.bybit.enabled=true
exchange.bitget.enabled=false                  # 可選擇性啟用
exchange.okex.enabled=true
exchange.hyperliquid.enabled=false

# === 可選：交易所API設定（無API也可運行） ===
# Binance
exchange.binance.apiKey=YOUR_API_KEY
exchange.binance.secretKey=YOUR_SECRET_KEY

# Bybit
exchange.bybit.apiKey=YOUR_API_KEY
exchange.bybit.secretKey=YOUR_SECRET_KEY
```

## 🐳 部署方式

### 方式1: Docker部署（推薦）

#### 快速啟動
```bash
# 1. 構建應用
./deploy.sh build

# 2. 啟動服務
./deploy.sh start

# 3. 查看日誌
./deploy.sh logs

# 4. 查看狀態
./deploy.sh status
```

#### 手動Docker步驟
```bash
# 構建鏡像
docker build -t crypto-exchange-monitor .

# 啟動容器
docker-compose up -d

# 查看日誌
docker-compose logs -f crypto-monitor
```

### 方式2: 直接運行

#### 編譯和運行
```bash
# 編譯專案
mvn clean package

# 運行應用
java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### 背景運行
```bash
# Linux/Mac背景運行
nohup java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar > logs/app.log 2>&1 &

# Windows背景運行
start /b java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 方式3: Systemd服務（Linux伺服器）

#### 安裝服務
```bash
# 複製服務文件
sudo cp crypto-monitor.service /etc/systemd/system/

# 重載systemd
sudo systemctl daemon-reload

# 啟用服務
sudo systemctl enable crypto-monitor

# 啟動服務
sudo systemctl start crypto-monitor

# 查看狀態
sudo systemctl status crypto-monitor
```

## 📊 監控和維護

### 日誌查看
```bash
# Docker方式
./deploy.sh logs

# Systemd方式
sudo journalctl -u crypto-monitor -f

# 直接運行方式
tail -f logs/crypto-monitor.log
```

### 服務控制
```bash
# 重啟服務
./deploy.sh restart

# 停止服務
./deploy.sh stop

# 清理資源
./deploy.sh cleanup
```

### 常見問題排解

#### 1. Discord連接失敗
- 檢查Bot Token是否正確
- 確認Bot已加入伺服器
- 檢查頻道ID是否正確

#### 2. 交易所連接錯誤
- 檢查網絡連接
- 驗證API密鑰（如果使用）
- 確認交易所服務正常

#### 3. 記憶體不足
```bash
# 調整Java記憶體設定
export JAVA_OPTS="-Xmx1G -Xms512m"
```

## 📈 效能調優

### 記憶體設定
```properties
# 在docker-compose.yml中調整
environment:
  - JAVA_OPTS=-Xmx1G -Xms512m
```

### 監控頻率調整
```properties
# 減少檢查頻率以降低資源使用
monitor.interval.seconds=120
```

### 交易所選擇
```properties
# 只啟用需要的交易所
exchange.binance.enabled=true
exchange.bybit.enabled=false
exchange.bitget.enabled=false
exchange.okex.enabled=false
exchange.hyperliquid.enabled=false
```

## 🔒 安全建議

1. **API密鑰安全**
   - 使用只讀權限的API密鑰
   - 定期輪換API密鑰
   - 不要將密鑰提交到版本控制

2. **Discord安全**
   - 使用專用的監控頻道
   - 限制Bot權限最小化

3. **系統安全**
   - 定期更新系統和Java
   - 使用防火牆限制網絡訪問
   - 監控系統資源使用

## 📱 使用說明

啟動後，系統將：
1. 每60秒檢查交易所數據
2. 當價格變動超過1%時發送Discord通知
3. 當持倉量變動超過5%時發送Discord通知
4. 自動記錄所有活動到日誌文件

### Discord通知範例
```
🚨 價格警報
交易所: Binance
交易對: BTC/USDT
當前價格: $45,123.45
變動: +2.34% (↗️)
時間: 2024-01-15 14:30:25
```

## 🔄 更新和維護

### 應用更新
```bash
# 停止服務
./deploy.sh stop

# 更新代碼
git pull

# 重新構建和啟動
./deploy.sh build
./deploy.sh start
```

### 配置更新
```bash
# 修改配置後重啟
nano config.properties
./deploy.sh restart
``` 