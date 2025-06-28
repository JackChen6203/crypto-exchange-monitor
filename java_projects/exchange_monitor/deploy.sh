#!/bin/bash

# 加密貨幣交易所監控系統部署腳本
# 使用方法: ./deploy.sh [環境] [操作]
# 環境: dev|staging|prod
# 操作: build|start|stop|restart|logs|status

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="crypto-exchange-monitor"
DOCKER_IMAGE="$PROJECT_NAME:latest"
CONTAINER_NAME="$PROJECT_NAME"

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日誌函數
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 檢查依賴
check_dependencies() {
    log_info "檢查依賴..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安裝"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安裝"
        exit 1
    fi
    
    log_info "依賴檢查通過"
}

# 構建應用
build() {
    log_info "開始構建 $PROJECT_NAME..."
    
    cd "$SCRIPT_DIR"
    
    # Maven構建
    log_info "執行Maven構建..."
    mvn clean package -DskipTests
    
    # Docker構建
    log_info "構建Docker鏡像..."
    docker build -t "$DOCKER_IMAGE" .
    
    log_info "構建完成"
}

# 啟動服務
start() {
    log_info "啟動 $PROJECT_NAME..."
    
    cd "$SCRIPT_DIR"
    
    # 檢查配置文件
    if [ ! -f "config.properties" ]; then
        log_warn "config.properties 不存在，使用默認配置"
        cp src/main/resources/config.properties .
    fi
    
    # 創建日誌目錄
    mkdir -p logs
    
    # 啟動服務
    docker-compose up -d
    
    log_info "服務已啟動"
    log_info "使用 '$0 logs' 查看日誌"
    log_info "使用 '$0 status' 查看狀態"
}

# 停止服務
stop() {
    log_info "停止 $PROJECT_NAME..."
    
    cd "$SCRIPT_DIR"
    docker-compose down
    
    log_info "服務已停止"
}

# 重啟服務
restart() {
    log_info "重啟 $PROJECT_NAME..."
    stop
    start
}

# 查看日誌
show_logs() {
    cd "$SCRIPT_DIR"
    docker-compose logs -f "$PROJECT_NAME"
}

# 查看狀態
show_status() {
    cd "$SCRIPT_DIR"
    
    log_info "服務狀態:"
    docker-compose ps
    
    log_info "資源使用:"
    docker stats --no-stream "$CONTAINER_NAME" 2>/dev/null || log_warn "容器未運行"
}

# 清理
cleanup() {
    log_info "清理未使用的Docker資源..."
    
    docker system prune -f
    docker volume prune -f
    
    log_info "清理完成"
}

# 顯示幫助
show_help() {
    echo "使用方法: $0 [操作]"
    echo ""
    echo "操作:"
    echo "  build     構建應用和Docker鏡像"
    echo "  start     啟動服務"
    echo "  stop      停止服務"
    echo "  restart   重啟服務"
    echo "  logs      查看實時日誌"
    echo "  status    查看服務狀態"
    echo "  cleanup   清理Docker資源"
    echo "  help      顯示此幫助信息"
    echo ""
    echo "範例:"
    echo "  $0 build    # 構建應用"
    echo "  $0 start    # 啟動服務"
    echo "  $0 logs     # 查看日誌"
}

# 主函數
main() {
    local action="${1:-help}"
    
    case "$action" in
        build)
            check_dependencies
            build
            ;;
        start)
            check_dependencies
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        logs)
            show_logs
            ;;
        status)
            show_status
            ;;
        cleanup)
            cleanup
            ;;
        help|*)
            show_help
            ;;
    esac
}

# 執行主函數
main "$@" 