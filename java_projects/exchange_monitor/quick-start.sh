#!/bin/bash

# 🚀 加密貨幣監控系統快速開始腳本
# 此腳本將引導您完成基本配置和部署

set -e

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}🚀 加密貨幣監控系統快速開始${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# 檢查依賴
check_requirements() {
    echo -e "${YELLOW}檢查系統需求...${NC}"
    
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java未安裝，請先安裝Java 17+${NC}"
        exit 1
    fi
    
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven未安裝，請先安裝Maven${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Java和Maven已安裝${NC}"
}

# 配置向導
configure_app() {
    echo ""
    echo -e "${YELLOW}📝 配置向導${NC}"
    echo "請提供以下配置信息："
    echo ""
    
    # 複製配置文件
    if [ ! -f "config.properties" ]; then
        cp src/main/resources/config.properties .
        echo -e "${GREEN}✅ 已創建配置文件${NC}"
    fi
    
    echo -e "${BLUE}選擇Discord通知方式：${NC}"
    echo "1) Discord Bot (推薦)"
    echo "2) Discord Webhook (簡單)"
    read -p "請選擇 (1-2): " discord_choice
    
    if [ "$discord_choice" = "1" ]; then
        echo ""
        echo -e "${YELLOW}Discord Bot設定：${NC}"
        echo "1. 前往 https://discord.com/developers/applications"
        echo "2. 創建應用程式 → 創建Bot → 複製Token"
        echo "3. 添加Bot到伺服器並獲取頻道ID"
        echo ""
        read -p "請輸入Discord Bot Token: " discord_token
        read -p "請輸入Discord頻道ID: " channel_id
        
        # 更新配置文件
        sed -i.bak "s/YOUR_DISCORD_BOT_TOKEN/$discord_token/g" config.properties
        sed -i.bak "s/YOUR_DISCORD_CHANNEL_ID/$channel_id/g" config.properties
        sed -i.bak "s/discord.webhookUrl=/#discord.webhookUrl=/g" config.properties
        
    elif [ "$discord_choice" = "2" ]; then
        echo ""
        echo -e "${YELLOW}Discord Webhook設定：${NC}"
        echo "1. 前往目標Discord頻道設定"
        echo "2. 整合 → Webhook → 創建Webhook"
        echo "3. 複製Webhook URL"
        echo ""
        read -p "請輸入Discord Webhook URL: " webhook_url
        
        # 更新配置文件
        sed -i.bak "s/YOUR_DISCORD_WEBHOOK_URL/$webhook_url/g" config.properties
        sed -i.bak "s/discord.token=/#discord.token=/g" config.properties
        sed -i.bak "s/discord.channelId=/#discord.channelId=/g" config.properties
    fi
    
    echo ""
    echo -e "${BLUE}交易所設定：${NC}"
    echo "選擇要監控的交易所（可以稍後修改）："
    
    exchanges=("binance" "bybit" "bitget" "okex" "hyperliquid")
    for exchange in "${exchanges[@]}"; do
        read -p "啟用 ${exchange}? (y/n): " enable_exchange
        if [ "$enable_exchange" != "y" ] && [ "$enable_exchange" != "Y" ]; then
            sed -i.bak "s/exchange.${exchange}.enabled=true/exchange.${exchange}.enabled=false/g" config.properties
        fi
    done
    
    echo ""
    echo -e "${GREEN}✅ 配置完成！${NC}"
}

# 構建應用
build_app() {
    echo ""
    echo -e "${YELLOW}🔨 構建應用...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✅ 構建完成${NC}"
}

# 測試配置
test_config() {
    echo ""
    echo -e "${YELLOW}🧪 測試配置...${NC}"
    
    # 創建logs目錄
    mkdir -p logs
    
    echo "正在測試Discord連接..."
    timeout 30s java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar --test || {
        echo -e "${YELLOW}⚠️  測試超時，這是正常的。應用將在生產環境中持續運行。${NC}"
    }
}

# 部署選項
deploy_options() {
    echo ""
    echo -e "${BLUE}🚀 部署選項：${NC}"
    echo "1) 前台運行 (用於測試)"
    echo "2) 背景運行"
    echo "3) Docker部署"
    echo "4) 只配置，稍後手動部署"
    
    read -p "請選擇部署方式 (1-4): " deploy_choice
    
    case $deploy_choice in
        1)
            echo -e "${YELLOW}啟動前台運行...${NC}"
            echo "按Ctrl+C停止應用"
            java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
            ;;
        2)
            echo -e "${YELLOW}啟動背景運行...${NC}"
            nohup java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar > logs/app.log 2>&1 &
            echo "應用已在背景啟動"
            echo "查看日誌: tail -f logs/app.log"
            echo "停止應用: pkill -f crypto-exchange-monitor"
            ;;
        3)
            if command -v docker &> /dev/null; then
                echo -e "${YELLOW}Docker部署...${NC}"
                chmod +x deploy.sh
                ./deploy.sh build
                ./deploy.sh start
                echo "Docker部署完成"
                echo "查看日誌: ./deploy.sh logs"
            else
                echo -e "${RED}❌ Docker未安裝${NC}"
            fi
            ;;
        4)
            echo -e "${GREEN}配置完成！${NC}"
            echo "您可以稍後使用以下命令運行："
            echo "java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
            ;;
    esac
}

# 顯示完成信息
show_completion() {
    echo ""
    echo -e "${GREEN}===========================================${NC}"
    echo -e "${GREEN}🎉 部署完成！${NC}"
    echo -e "${GREEN}===========================================${NC}"
    echo ""
    echo -e "${BLUE}📋 重要信息：${NC}"
    echo "• 配置文件: config.properties"
    echo "• 日誌位置: logs/"
    echo "• 測試命令: java -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"
    echo ""
    echo -e "${BLUE}📖 文檔：${NC}"
    echo "• 詳細配置: DEPLOYMENT_CONFIG.md"
    echo "• 完整部署指南: DEPLOYMENT.md"
    echo ""
    echo -e "${BLUE}🔧 常用命令：${NC}"
    echo "• 查看日誌: tail -f logs/crypto-monitor.log"
    echo "• 重啟服務: ./deploy.sh restart (Docker)"
    echo "• 修改配置: nano config.properties"
    echo ""
    echo -e "${YELLOW}監控將在Discord中發送通知！${NC}"
}

# 主執行流程
main() {
    check_requirements
    configure_app
    build_app
    deploy_options
    show_completion
}

# 執行主函數
main "$@" 