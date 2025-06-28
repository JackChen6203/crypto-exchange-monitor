# 🚀 完整API申請與部署流程指南

## 📋 目錄
1. [API申請流程](#api申請流程)
2. [Discord設定](#discord設定)
3. [本地測試](#本地測試)
4. [Git Repository設定](#git-repository設定)
5. [Qodidi部署流程](#qodidi部署流程)
6. [監控與維護](#監控與維護)

---

## 🔑 API申請流程

### 1. 幣安 (Binance) API申請

**步驟:**
1. 前往 [Binance官網](https://www.binance.com/) 註冊並完成身份驗證
2. 登入後點擊右上角頭像 → API管理
3. 創建新的API密鑰
4. 記錄 `API Key` 和 `Secret Key`
5. 在API設置中啟用 **期貨交易** 權限
6. 設定IP白名單（可選，建議設定）

**重要設定:**
```
權限設置: ✅ 讀取資訊 ✅ 期貨交易
IP限制: 建議設定你的伺服器IP
```

### 2. Bybit API申請

**步驟:**
1. 前往 [Bybit官網](https://www.bybit.com/) 註冊
2. 完成KYC驗證
3. 前往 帳戶與安全 → API
4. 創建新API密鑰
5. 記錄 `API Key` 和 `Secret Key`

**重要設定:**
```
權限設置: ✅ 讀取 ✅ 交易
產品類型: ✅ 衍生品
```

### 3. Bitget API申請

**步驟:**
1. 前往 [Bitget官網](https://www.bitget.com/) 註冊
2. 完成身份驗證
3. 前往 API管理
4. 創建API密鑰
5. 記錄 `API Key`、`Secret Key` 和 `Passphrase`

**重要設定:**
```
權限: ✅ 讀取 ✅ 交易
IP限制: 建議設定
```

### 4. OKX API申請

**步驟:**
1. 前往 [OKX官網](https://www.okx.com/) 註冊
2. 完成KYC
3. 前往 API
4. 創建V5 API
5. 記錄 `API Key`、`Secret Key` 和 `Passphrase`

**重要設定:**
```
權限: ✅ 讀取 ✅ 交易
API版本: V5
```

### 5. Hyperliquid API申請

**步驟:**
1. 前往 [Hyperliquid官網](https://hyperliquid.xyz/) 註冊
2. 連接錢包完成驗證
3. 前往設定 → API
4. 生成API密鑰
5. 記錄 `API Key` 和 `Secret Key`

---

## 💬 Discord設定

### 方式1: Discord Bot (推薦)

**創建Bot:**
1. 前往 [Discord Developer Portal](https://discord.com/developers/applications)
2. 點擊 "New Application"
3. 輸入應用程式名稱
4. 左側選單選擇 "Bot"
5. 點擊 "Add Bot"
6. 複製 Bot Token

**邀請Bot到伺服器:**
1. 左側選單選擇 "OAuth2" → "URL Generator"
2. 勾選 "bot" 範圍
3. 勾選 "Send Messages" 權限
4. 複製生成的URL並開啟
5. 選擇要加入的伺服器

**獲取頻道ID:**
1. 在Discord中右鍵點擊目標頻道
2. 選擇 "複製頻道ID"
3. 如果沒有看到此選項，請先在設定中開啟開發者模式

### 方式2: Discord Webhook

**創建Webhook:**
1. 在Discord頻道中點擊設定（齒輪圖標）
2. 選擇 "整合" → "Webhooks"
3. 點擊 "創建Webhook"
4. 自訂名稱和頭像
5. 複製Webhook URL

---

## 🧪 本地測試

### 1. 環境準備
```bash
# 確認Java版本
java -version

# 確認Maven版本
mvn -version
```

### 2. 配置設定
```bash
# 複製配置範例
cp src/main/resources/config.properties.example config.properties

# 編輯配置檔案
notepad config.properties  # Windows
# 或
vim config.properties       # Linux/Mac
```

### 3. 填入API資訊
```properties
# Discord設定
discord.token=你的Discord_Bot_Token
discord.channel_id=你的頻道ID

# 幣安API
binance.api_key=你的幣安API_Key
binance.api_secret=你的幣安Secret_Key

# 其他交易所依此類推...
```

### 4. 本地測試運行
```bash
# 編譯專案
mvn clean package

# 運行測試
java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 📂 Git Repository設定

### 1. 初始化Git並推送

```bash
# 初始化git (如果尚未初始化)
git init

# 添加所有檔案 (排除敏感資訊)
git add .

# 第一次提交
git commit -m "初始化加密貨幣監控系統"

# 添加遠端倉庫
git remote add origin https://github.com/你的用戶名/crypto-exchange-monitor.git

# 推送到遠端
git push -u origin master
```

### 2. 環境變數設定 (GitHub Secrets)

在GitHub Repository中設定以下Secrets:

```
DISCORD_TOKEN=你的Discord_Bot_Token
DISCORD_CHANNEL_ID=你的頻道ID
BINANCE_API_KEY=你的幣安API_Key
BINANCE_API_SECRET=你的幣安Secret_Key
BYBIT_API_KEY=你的Bybit_API_Key
BYBIT_API_SECRET=你的Bybit_Secret_Key
BITGET_API_KEY=你的Bitget_API_Key
BITGET_API_SECRET=你的Bitget_Secret_Key
BITGET_PASSPHRASE=你的Bitget_Passphrase
OKX_API_KEY=你的OKX_API_Key
OKX_API_SECRET=你的OKX_Secret_Key
OKX_PASSPHRASE=你的OKX_Passphrase
HYPERLIQUID_API_KEY=你的Hyperliquid_API_Key
HYPERLIQUID_API_SECRET=你的Hyperliquid_Secret_Key
```

---

## 🌐 Qodidi部署流程

### 1. 註冊Qodidi帳號
1. 前往 [Railway](https://railway.app/) 或 [Render](https://render.com/) 或 [Heroku](https://heroku.com/)
2. 使用GitHub帳號登入
3. 連接你的GitHub Repository

### 2. 創建新專案

**在Railway上部署:**
```bash
# 安裝Railway CLI
npm install -g @railway/cli

# 登入Railway
railway login

# 在專案目錄中初始化
railway init

# 部署
railway up
```

**在Render上部署:**
1. 在Render Dashboard中點擊 "New +"
2. 選擇 "Web Service"
3. 連接你的GitHub Repository
4. 設定以下參數:
   - **Name**: crypto-exchange-monitor
   - **Environment**: Docker
   - **Region**: Singapore (亞洲地區)
   - **Branch**: master

### 3. 環境變數設定

在部署平台中設定環境變數:
```
DISCORD_TOKEN=你的Token
DISCORD_CHANNEL_ID=你的頻道ID
BINANCE_API_KEY=你的API_Key
BINANCE_API_SECRET=你的Secret_Key
# ... 其他API密鑰
```

### 4. Dockerfile優化 (針對部署平台)

```dockerfile
# 多階段構建優化
FROM maven:3.8.6-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# 設定時區
RUN apk add --no-cache tzdata
ENV TZ=Asia/Taipei

# 健康檢查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD pgrep java > /dev/null || exit 1

# 運行應用
CMD ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
```

### 5. 部署自動化 (GitHub Actions)

創建 `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Production

on:
  push:
    branches: [ master ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn test
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Deploy to Railway
      uses: railwayapp/railway-deploy@v1
      with:
        railway_token: ${{ secrets.RAILWAY_TOKEN }}
        service: crypto-exchange-monitor
```

---

## 📊 監控與維護

### 1. 日誌監控

```bash
# 檢查應用程式日誌
railway logs

# 實時監控
railway logs --follow
```

### 2. 健康檢查

建立健康檢查端點 (可選):
```java
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

### 3. 效能監控

在配置中啟用JVM監控:
```properties
# JVM監控設定
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

### 4. 錯誤處理和重啟

```bash
# 重啟服務
railway restart

# 檢查服務狀態
railway status
```

---

## 🎯 快速部署腳本

創建一鍵部署腳本 `quick-deploy.sh`:

```bash
#!/bin/bash

echo "🚀 開始部署加密貨幣監控系統..."

# 檢查環境
if ! command -v git &> /dev/null; then
    echo "❌ Git 未安裝"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java 未安裝"
    exit 1
fi

# Git操作
echo "📂 推送到Git..."
git add .
git commit -m "更新配置 $(date)"
git push

# 部署到Railway
echo "🌐 部署到Railway..."
railway up

echo "✅ 部署完成！"
echo "📊 查看日誌: railway logs"
echo "🔗 訪問URL: railway open"
```

## 🔧 故障排除

### 常見問題:

1. **API連接失敗**
   - 檢查API密鑰是否正確
   - 確認IP白名單設定
   - 檢查網路連接

2. **Discord通知失敗**
   - 確認Bot有發送消息權限
   - 檢查頻道ID是否正確
   - 確認Webhook URL有效

3. **部署失敗**
   - 檢查Docker配置
   - 確認環境變數設定
   - 查看部署日誌

4. **記憶體不足**
   - 調整JVM參數: `-Xmx256m -Xms128m`
   - 優化監控間隔時間
   - 減少同時監控的交易對數量

---

## 📞 支援與聯繫

如遇到問題，請檢查:
1. 日誌檔案: `logs/crypto-monitor.log`
2. 錯誤日誌: `logs/error.log`
3. 系統狀態: `railway status`

---

**🎉 恭喜！你的加密貨幣監控系統已成功部署！** 