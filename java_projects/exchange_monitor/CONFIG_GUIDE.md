# 🚀 加密貨幣監控系統配置指南

## 📋 概述

本系統可以監控多個加密貨幣交易所的價格變動和持倉量數據，並通過 Discord 發送警報通知。

## 🔧 必要配置

### 1. Discord 通知配置（二選一）

#### 方法一：Discord Webhook（推薦 - 最簡單）

1. **進入 Discord 伺服器**
   - 選擇想要接收通知的頻道
   - 右鍵點擊頻道 → 編輯頻道 → 整合 → Webhook

2. **創建 Webhook**
   - 點擊「新增 Webhook」
   - 設置名稱（如：加密貨幣監控）
   - 複製 Webhook URL

3. **配置文件設置**
   ```properties
   # 註釋掉 Bot 配置
   # discord.token=YOUR_DISCORD_BOT_TOKEN_HERE
   # discord.channel_id=YOUR_DISCORD_CHANNEL_ID_HERE
   
   # 使用 Webhook URL
   discord.webhook_url=你剛複製的_WEBHOOK_URL
   ```

#### 方法二：Discord Bot Token

1. **創建 Discord 應用程式**
   - 前往 https://discord.com/developers/applications
   - 點擊「New Application」
   - 輸入應用程式名稱

2. **創建 Bot**
   - 進入「Bot」頁面
   - 點擊「Add Bot」
   - 複製 Bot Token

3. **邀請 Bot 到伺服器**
   - 進入「OAuth2」→「URL Generator」
   - 勾選「bot」權限
   - 勾選「Send Messages」、「Embed Links」權限
   - 複製生成的 URL，在瀏覽器中打開並邀請到伺服器

4. **獲取頻道 ID**
   - 在 Discord 中開啟開發者模式（用戶設定 → 進階 → 開發者模式）
   - 右鍵點擊目標頻道 → 複製 ID

5. **配置文件設置**
   ```properties
   discord.token=你的_BOT_TOKEN
   discord.channel_id=你的_頻道_ID
   
   # 註釋掉 Webhook
   # discord.webhook_url=https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE
   ```

### 2. 交易所 API 配置

> **⚠️ 重要提醒**：API Key 只需要**讀取權限**，請勿開啟交易權限！

#### 🟡 Binance（幣安）

1. **獲取 API Key**
   - 登入 https://www.binance.com
   - 前往 個人中心 → API 管理
   - 點擊「創建 API」
   - **權限設置**：僅勾選「Enable Reading」
   - 完成安全驗證

2. **配置文件**
   ```properties
   binance.api_key=你的_BINANCE_API_KEY
   binance.api_secret=你的_BINANCE_SECRET_KEY
   ```

#### 🟠 Bybit

1. **獲取 API Key**
   - 登入 https://www.bybit.com
   - 前往 API 管理
   - 創建新 API Key
   - **權限設置**：僅選擇「讀取」權限

2. **配置文件**
   ```properties
   bybit.api_key=你的_BYBIT_API_KEY
   bybit.api_secret=你的_BYBIT_SECRET_KEY
   ```

#### 🔵 Bitget

1. **獲取 API Key**
   - 登入 https://www.bitget.com
   - 前往 API 管理
   - 創建 API Key
   - 設置 Passphrase（交易密碼）
   - **權限設置**：僅開啟讀取權限

2. **配置文件**
   ```properties
   bitget.api_key=你的_BITGET_API_KEY
   bitget.api_secret=你的_BITGET_SECRET_KEY
   bitget.passphrase=你的_BITGET_PASSPHRASE
   ```

#### 🟢 OKX（歐易）

1. **獲取 API Key**
   - 登入 https://www.okx.com
   - 前往 API 管理
   - 創建 API Key
   - 設置 Passphrase
   - **權限設置**：僅開啟「讀取」權限

2. **配置文件**
   ```properties
   okx.api_key=你的_OKX_API_KEY
   okx.api_secret=你的_OKX_SECRET_KEY
   okx.passphrase=你的_OKX_PASSPHRASE
   ```

#### 🔴 Hyperliquid

1. **獲取 API Key**
   - 登入 https://app.hyperliquid.xyz
   - 前往 API 設定
   - 創建 API Key
   - **權限設置**：僅開啟讀取權限

2. **配置文件**
   ```properties
   hyperliquid.api_key=你的_HYPERLIQUID_API_KEY
   hyperliquid.api_secret=你的_HYPERLIQUID_SECRET_KEY
   ```

## 🔄 應用配置

### 監控參數調整

```properties
# 監控間隔時間（秒）
monitor.interval_seconds=60

# 價格變動閾值（百分比）- 超過此閾值才發送通知
monitor.price_threshold_percent=1.0

# 持倉量變動閾值（百分比）- 超過此閾值才發送通知
monitor.position_threshold_percent=5.0

# 監控的交易對（用逗號分隔）
monitor.symbols=BTCUSDT,ETHUSDT,SOLUSDT
```

### 啟用/停用交易所

```properties
# 可以選擇性開啟交易所監控
exchange.binance.enabled=true
exchange.bybit.enabled=true
exchange.bitget.enabled=false  # 設為 false 停用
exchange.okx.enabled=true
exchange.hyperliquid.enabled=true
```

## 🚀 快速開始

1. **複製配置文件**
   ```bash
   cp config.properties.example config.properties
   ```

2. **編輯配置文件**
   - 至少設置一個 Discord 通知方式
   - 設置至少一個交易所的 API 憑證

3. **重新啟動系統**
   ```bash
   docker-compose down
   docker-compose up -d
   ```

4. **檢查日誌**
   ```bash
   docker-compose logs -f
   ```

## ✅ 配置驗證

成功配置後，你應該在日誌中看到：

```
✅ 已創建交易所: Binance
✅ 已創建交易所: Bybit
✅ 交易所管理器初始化完成，共啟用 2 個交易所
✅ Discord Bot已連接到頻道: 你的頻道名稱
✅ 已發送系統啟動通知
```

在 Discord 中會收到系統啟動通知！

## 🛡️ 安全提醒

- ✅ API Key 僅開啟**讀取權限**
- ❌ 絕對不要開啟交易權限
- 🔒 定期更換 API Key
- 📝 妥善保管憑證信息
- 🚫 不要將憑證提交到版本控制系統

## 🔧 故障排除

### Discord 無法發送通知
- 檢查 Webhook URL 是否正確
- 確認 Bot 有頻道發送權限
- 查看日誌中的錯誤信息

### 交易所連接失敗
- 驗證 API 憑證是否正確
- 確認 API Key 有讀取權限
- 檢查網絡連接

### 監控無反應
- 確認至少有一個交易所配置正確
- 檢查監控參數設置
- 查看日誌輸出

## 📞 技術支援

如有問題，請檢查 `docker-compose logs` 輸出或提交 Issue。 