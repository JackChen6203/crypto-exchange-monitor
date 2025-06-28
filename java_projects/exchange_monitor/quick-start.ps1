# 🚀 加密貨幣監控系統快速開始腳本 (Windows PowerShell)
# 此腳本將引導您完成基本配置和部署

param(
    [string]$Mode = "interactive"
)

# 顏色輸出函數
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    
    switch ($Color) {
        "Green" { Write-Host $Message -ForegroundColor Green }
        "Yellow" { Write-Host $Message -ForegroundColor Yellow }
        "Red" { Write-Host $Message -ForegroundColor Red }
        "Blue" { Write-Host $Message -ForegroundColor Blue }
        "Cyan" { Write-Host $Message -ForegroundColor Cyan }
        default { Write-Host $Message }
    }
}

Write-ColorOutput "===========================================" "Blue"
Write-ColorOutput "🚀 加密貨幣監控系統快速開始" "Blue"
Write-ColorOutput "===========================================" "Blue"
Write-Host ""

# 檢查系統需求
function Test-Requirements {
    Write-ColorOutput "檢查系統需求..." "Yellow"
    
    # 檢查Java
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        if ($javaVersion) {
            Write-ColorOutput "✅ Java已安裝: $($javaVersion)" "Green"
        }
    }
    catch {
        Write-ColorOutput "❌ Java未安裝，請先安裝Java 17+" "Red"
        Start-Process "https://adoptium.net/"
        exit 1
    }
    
    # 檢查Maven
    try {
        $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
        if ($mavenVersion) {
            Write-ColorOutput "✅ Maven已安裝: $($mavenVersion)" "Green"
        }
    }
    catch {
        Write-ColorOutput "❌ Maven未安裝，請先安裝Maven" "Red"
        Start-Process "https://maven.apache.org/download.cgi"
        exit 1
    }
}

# 配置應用
function Set-ApplicationConfig {
    Write-Host ""
    Write-ColorOutput "📝 配置向導" "Yellow"
    Write-Host "請提供以下配置信息："
    Write-Host ""
    
    # 複製配置文件
    if (!(Test-Path "config.properties")) {
        Copy-Item "src\main\resources\config.properties" "config.properties"
        Write-ColorOutput "✅ 已創建配置文件" "Green"
    }
    
    Write-ColorOutput "選擇Discord通知方式：" "Blue"
    Write-Host "1) Discord Bot (推薦)"
    Write-Host "2) Discord Webhook (簡單)"
    $discordChoice = Read-Host "請選擇 (1-2)"
    
    if ($discordChoice -eq "1") {
        Write-Host ""
        Write-ColorOutput "Discord Bot設定：" "Yellow"
        Write-Host "1. 前往 https://discord.com/developers/applications"
        Write-Host "2. 創建應用程式 → 創建Bot → 複製Token"
        Write-Host "3. 添加Bot到伺服器並獲取頻道ID"
        Write-Host ""
        $discordToken = Read-Host "請輸入Discord Bot Token"
        $channelId = Read-Host "請輸入Discord頻道ID"
        
        # 更新配置文件
        $config = Get-Content "config.properties"
        $config = $config -replace "YOUR_DISCORD_BOT_TOKEN", $discordToken
        $config = $config -replace "YOUR_DISCORD_CHANNEL_ID", $channelId
        $config = $config -replace "discord\.webhookUrl=", "#discord.webhookUrl="
        Set-Content "config.properties" $config
    }
    elseif ($discordChoice -eq "2") {
        Write-Host ""
        Write-ColorOutput "Discord Webhook設定：" "Yellow"
        Write-Host "1. 前往目標Discord頻道設定"
        Write-Host "2. 整合 → Webhook → 創建Webhook"
        Write-Host "3. 複製Webhook URL"
        Write-Host ""
        $webhookUrl = Read-Host "請輸入Discord Webhook URL"
        
        # 更新配置文件
        $config = Get-Content "config.properties"
        $config = $config -replace "YOUR_DISCORD_WEBHOOK_URL", $webhookUrl
        $config = $config -replace "discord\.token=", "#discord.token="
        $config = $config -replace "discord\.channelId=", "#discord.channelId="
        Set-Content "config.properties" $config
    }
    
    Write-Host ""
    Write-ColorOutput "交易所設定：" "Blue"
    Write-Host "選擇要監控的交易所（可以稍後修改）："
    
    $exchanges = @("binance", "bybit", "bitget", "okex", "hyperliquid")
    $config = Get-Content "config.properties"
    
    foreach ($exchange in $exchanges) {
        $enableExchange = Read-Host "啟用 $exchange? (y/n)"
        if ($enableExchange -ne "y" -and $enableExchange -ne "Y") {
            $config = $config -replace "exchange\.$exchange\.enabled=true", "exchange.$exchange.enabled=false"
        }
    }
    
    Set-Content "config.properties" $config
    
    Write-Host ""
    Write-ColorOutput "✅ 配置完成！" "Green"
}

# 構建應用
function Build-Application {
    Write-Host ""
    Write-ColorOutput "🔨 構建應用..." "Yellow"
    
    try {
        mvn clean package -DskipTests
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ 構建完成" "Green"
        } else {
            Write-ColorOutput "❌ 構建失敗" "Red"
            exit 1
        }
    }
    catch {
        Write-ColorOutput "❌ 構建過程中發生錯誤" "Red"
        exit 1
    }
}

# 部署選項
function Start-Deployment {
    Write-Host ""
    Write-ColorOutput "🚀 部署選項：" "Blue"
    Write-Host "1) 前台運行 (用於測試)"
    Write-Host "2) 背景運行"
    Write-Host "3) Docker部署"
    Write-Host "4) 只配置，稍後手動部署"
    
    $deployChoice = Read-Host "請選擇部署方式 (1-4)"
    
    # 創建logs目錄
    if (!(Test-Path "logs")) {
        New-Item -ItemType Directory -Path "logs" | Out-Null
    }
    
    switch ($deployChoice) {
        "1" {
            Write-ColorOutput "啟動前台運行..." "Yellow"
            Write-Host "按Ctrl+C停止應用"
            java -jar "target\crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
        }
        "2" {
            Write-ColorOutput "啟動背景運行..." "Yellow"
            $jarPath = "target\crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
            Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WindowStyle Hidden -RedirectStandardOutput "logs\app.log" -RedirectStandardError "logs\error.log"
            Write-ColorOutput "應用已在背景啟動" "Green"
            Write-Host "查看日誌: Get-Content logs\app.log -Wait"
            Write-Host "停止應用: Stop-Process -Name java -Force"
        }
        "3" {
            if (Get-Command docker -ErrorAction SilentlyContinue) {
                Write-ColorOutput "Docker部署..." "Yellow"
                if (Test-Path "deploy.sh") {
                    # 在Windows上運行bash腳本（需要Git Bash或WSL）
                    bash deploy.sh build
                    bash deploy.sh start
                    Write-ColorOutput "Docker部署完成" "Green"
                    Write-Host "查看日誌: bash deploy.sh logs"
                } else {
                    # 手動Docker命令
                    docker build -t crypto-exchange-monitor .
                    docker-compose up -d
                    Write-ColorOutput "Docker部署完成" "Green"
                    Write-Host "查看日誌: docker-compose logs -f crypto-monitor"
                }
            } else {
                Write-ColorOutput "❌ Docker未安裝" "Red"
                Start-Process "https://www.docker.com/products/docker-desktop"
            }
        }
        "4" {
            Write-ColorOutput "配置完成！" "Green"
            Write-Host "您可以稍後使用以下命令運行："
            Write-Host "java -jar target\crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
        }
    }
}

# 顯示完成信息
function Show-CompletionInfo {
    Write-Host ""
    Write-ColorOutput "===========================================" "Green"
    Write-ColorOutput "🎉 部署完成！" "Green"
    Write-ColorOutput "===========================================" "Green"
    Write-Host ""
    Write-ColorOutput "📋 重要信息：" "Blue"
    Write-Host "• 配置文件: config.properties"
    Write-Host "• 日誌位置: logs\"
    Write-Host "• 測試命令: java -jar target\crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
    Write-Host ""
    Write-ColorOutput "📖 文檔：" "Blue"
    Write-Host "• 詳細配置: DEPLOYMENT_CONFIG.md"
    Write-Host "• 完整部署指南: DEPLOYMENT.md"
    Write-Host ""
    Write-ColorOutput "🔧 常用命令：" "Blue"
    Write-Host "• 查看日誌: Get-Content logs\crypto-monitor.log -Wait"
    Write-Host "• 重啟服務: docker-compose restart (Docker)"
    Write-Host "• 修改配置: notepad config.properties"
    Write-Host ""
    Write-ColorOutput "監控將在Discord中發送通知！" "Yellow"
}

# 主執行流程
function Main {
    try {
        Test-Requirements
        Set-ApplicationConfig
        Build-Application
        Start-Deployment
        Show-CompletionInfo
    }
    catch {
        Write-ColorOutput "發生錯誤: $_" "Red"
        exit 1
    }
}

# 執行主函數
Main 