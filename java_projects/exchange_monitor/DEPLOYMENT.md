# 部署指南

## 概述

本文檔說明如何部署加密貨幣交易所監控系統的各種方式。

## 系統需求

### 硬體需求
- **CPU**: 1核心以上（推薦2核心）
- **記憶體**: 512MB以上（推薦1GB）
- **儲存空間**: 1GB以上
- **網路**: 穩定的網際網路連接

### 軟體需求
- **Java**: OpenJDK 17或更高版本
- **Docker**: 20.10+（Docker部署方式）
- **Docker Compose**: 2.0+（Docker部署方式）
- **Maven**: 3.8+（從源碼構建）

## 部署方式

### 1. Docker部署（推薦）

#### 快速開始
```bash
# 克隆專案
git clone <repository-url>
cd exchange_monitor

# 配置應用
cp src/main/resources/config.properties .
# 編輯config.properties設置你的Discord和交易所配置

# 構建和啟動
./deploy.sh build
./deploy.sh start

# 查看日誌
./deploy.sh logs
```

#### 詳細步驟

1. **準備配置文件**
```bash
cp src/main/resources/config.properties .
```

2. **編輯配置文件**
```properties
# Discord設定
discord.token=YOUR_BOT_TOKEN
discord.channelId=YOUR_CHANNEL_ID
discord.webhookUrl=YOUR_WEBHOOK_URL

# 交易所啟用設定
exchange.binance.enabled=true
exchange.bybit.enabled=true
exchange.bitget.enabled=false
exchange.okex.enabled=true
exchange.hyperliquid.enabled=false

# 監控設定
monitor.interval.seconds=60
monitor.price.threshold.percent=1.0
monitor.position.threshold.percent=5.0
```

3. **構建Docker鏡像**
```bash
docker build -t crypto-exchange-monitor .
```

4. **啟動服務**
```bash
docker-compose up -d
```

5. **檢查狀態**
```bash
docker-compose ps
docker-compose logs -f crypto-monitor
```

### 2. Systemd服務部署

#### 安裝步驟

1. **創建用戶和目錄**
```bash
sudo useradd -r -s /bin/false crypto-monitor
sudo mkdir -p /opt/crypto-monitor/logs
sudo chown -R crypto-monitor:crypto-monitor /opt/crypto-monitor
```

2. **複製應用文件**
```bash
# 編譯應用
mvn clean package

# 複製文件
sudo cp target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/crypto-monitor/
sudo cp config.properties /opt/crypto-monitor/
sudo cp crypto-monitor.service /etc/systemd/system/

# 設置權限
sudo chmod 644 /etc/systemd/system/crypto-monitor.service
sudo chown crypto-monitor:crypto-monitor /opt/crypto-monitor/*
```

3. **啟動服務**
```bash
sudo systemctl daemon-reload
sudo systemctl enable crypto-monitor
sudo systemctl start crypto-monitor

# 檢查狀態
sudo systemctl status crypto-monitor
sudo journalctl -u crypto-monitor -f
```

### 3. 直接運行

#### Java直接運行
```bash
# 編譯
mvn clean package

# 運行
java -Xmx512m -Xms256m -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 配置說明

### Discord設定

#### Bot模式設定
1. 在Discord Developer Portal創建Bot
2. 獲取Bot Token
3. 將Bot添加到你的伺服器
4. 獲取頻道ID

```properties
discord.token=YOUR_BOT_TOKEN
discord.channelId=YOUR_CHANNEL_ID
```

#### Webhook模式設定
1. 在Discord頻道設定中創建Webhook
2. 複製Webhook URL

```properties
discord.webhookUrl=YOUR_WEBHOOK_URL
```

### 交易所設定

```properties
# 啟用/禁用特定交易所
exchange.binance.enabled=true
exchange.bybit.enabled=true
exchange.bitget.enabled=false
exchange.okex.enabled=true
exchange.hyperliquid.enabled=false
```

### 監控參數設定

```properties
# 檢查間隔（秒）
monitor.interval.seconds=60

# 價格變動閾值（百分比）
monitor.price.threshold.percent=1.0

# 持倉量變動閾值（百分比）
monitor.position.threshold.percent=5.0
```

## 監控和維護

### 日誌查看

#### Docker部署
```bash
# 實時日誌
docker-compose logs -f crypto-monitor

# 歷史日誌
docker-compose logs crypto-monitor
```

#### Systemd部署
```bash
# 實時日誌
sudo journalctl -u crypto-monitor -f

# 歷史日誌
sudo journalctl -u crypto-monitor --since "1 hour ago"
```

### 健康檢查

#### Docker健康檢查
```bash
docker inspect crypto-exchange-monitor | grep Health -A 10
```

#### 手動健康檢查
```bash
# 檢查進程
pgrep -f crypto-exchange-monitor

# 檢查網路連接
netstat -tlnp | grep java

# 檢查資源使用
docker stats crypto-exchange-monitor
```

### 重啟服務

#### Docker
```bash
./deploy.sh restart
# 或
docker-compose restart crypto-monitor
```

#### Systemd
```bash
sudo systemctl restart crypto-monitor
```

### 更新應用

#### Docker更新
```bash
git pull
./deploy.sh build
./deploy.sh restart
```

#### Systemd更新
```bash
git pull
mvn clean package
sudo systemctl stop crypto-monitor
sudo cp target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/crypto-monitor/
sudo systemctl start crypto-monitor
```

## 故障排除

### 常見問題

#### 1. 應用無法啟動
- 檢查Java版本是否為17+
- 檢查配置文件是否存在
- 檢查日誌中的錯誤信息

#### 2. Discord通知無法發送
- 檢查Bot Token是否正確
- 檢查Bot是否有發送消息權限
- 檢查Webhook URL是否有效

#### 3. 交易所連接失敗
- 檢查網路連接
- 檢查交易所API是否可用
- 查看具體錯誤信息

#### 4. 記憶體不足
- 調整JVM堆內存設置
- 檢查系統可用記憶體
- 考慮禁用部分交易所

### 日誌分析

#### 正常運行日誌
```
[INFO] Discord Bot已連接到頻道: general
[INFO] 已創建交易所: Binance
[INFO] 已創建交易所: Bybit
[INFO] 監控管理器已啟動
```

#### 錯誤日誌
```
[ERROR] 初始化Discord Bot失敗
[ERROR] 創建交易所失敗: Binance
[WARN] 配置文件config.properties不存在，將使用默認配置
```

## 安全建議

1. **限制文件權限**
   - 配置文件設為只讀
   - 日誌目錄設置適當權限

2. **網路安全**
   - 使用防火牆限制不必要的端口
   - 定期更新系統和依賴

3. **機密信息保護**
   - 不要在版本控制中提交敏感配置
   - 考慮使用環境變量或密鑰管理系統

4. **監控告警**
   - 設置應用運行狀態監控
   - 配置異常情況告警

## 性能調優

### JVM參數調優
```bash
# 基本設置
java -Xmx512m -Xms256m -jar app.jar

# 詳細調優
java -Xmx512m -Xms256m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails \
     -XX:+PrintGCTimeStamps \
     -Xloggc:gc.log \
     -jar app.jar
```

### Docker資源限制
```yaml
deploy:
  resources:
    limits:
      memory: 512M
      cpus: '1.0'
    reservations:
      memory: 256M
      cpus: '0.5'
```

## 擴展和客製化

### 添加新交易所
1. 實現Exchange介面
2. 在ExchangeManager中註冊
3. 添加對應配置

### 自定義通知
1. 擴展DiscordService
2. 實現新的通知管道
3. 配置通知規則

### 數據存儲
1. 添加數據庫依賴
2. 實現數據存儲服務
3. 配置持久化策略 