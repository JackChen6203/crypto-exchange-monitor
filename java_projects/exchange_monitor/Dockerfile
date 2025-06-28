# 使用OpenJDK 17作為基礎鏡像
FROM openjdk:17-jdk-alpine

# 設置工作目錄
WORKDIR /app

# 複製pom.xml和源代碼
COPY pom.xml .
COPY src ./src

# 安裝Maven
RUN apk add --no-cache maven

# 編譯應用程式
RUN mvn clean package -DskipTests

# 創建日誌目錄並設置權限
RUN mkdir -p /app/logs && chmod 755 /app/logs

# 複製配置文件 - 如果 config.properties 存在則複製，否則使用範例文件
COPY config.properties* ./
RUN if [ ! -f config.properties ]; then cp config.properties.example config.properties; fi

# 暴露JVM監控端口（可選）
EXPOSE 8080

# 設置環境變量
ENV JAVA_OPTS="-Xmx512m -Xms256m -Dlogback.configurationFile=classpath:logback.xml"

# 運行應用程式
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/crypto-exchange-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar"]

# 健康檢查（可選）
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD pgrep java > /dev/null || exit 1

# 標籤
LABEL maintainer="crypto-monitor-team"
LABEL version="1.0.0"
LABEL description="加密貨幣交易所監控系統" 