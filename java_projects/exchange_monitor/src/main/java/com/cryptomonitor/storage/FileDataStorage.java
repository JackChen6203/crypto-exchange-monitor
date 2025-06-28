package com.cryptomonitor.storage;

import com.cryptomonitor.model.MarketData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基於文件的數據存儲實現
 */
public class FileDataStorage implements DataStorage {
    private static final Logger logger = LoggerFactory.getLogger(FileDataStorage.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final Path dataDirectory;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public FileDataStorage(String dataDirectoryPath) {
        this.dataDirectory = Paths.get(dataDirectoryPath);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // 創建數據目錄
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(dataDirectory.resolve("prices"));
            Files.createDirectories(dataDirectory.resolve("positions"));
        } catch (IOException e) {
            logger.error("創建數據目錄失敗", e);
        }
    }
    
    @Override
    public void savePriceData(MarketData.PriceData priceData) {
        lock.writeLock().lock();
        try {
            String date = LocalDateTime.now().format(DATE_FORMATTER);
            Path filePath = dataDirectory.resolve("prices")
                    .resolve(priceData.getExchange().toLowerCase())
                    .resolve(date + ".jsonl");
            
            // 創建目錄
            Files.createDirectories(filePath.getParent());
            
            // 創建數據記錄
            DataRecord record = new DataRecord(
                    LocalDateTime.now(),
                    priceData.getExchange(),
                    priceData.getSymbol(),
                    "price",
                    objectMapper.writeValueAsString(priceData)
            );
            
            // 追加到文件
            String jsonLine = objectMapper.writeValueAsString(record) + "\n";
            Files.write(filePath, jsonLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            logger.error("保存價格數據失敗", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void savePositionData(MarketData.PositionData positionData) {
        lock.writeLock().lock();
        try {
            String date = LocalDateTime.now().format(DATE_FORMATTER);
            Path filePath = dataDirectory.resolve("positions")
                    .resolve(positionData.getExchange().toLowerCase())
                    .resolve(date + ".jsonl");
            
            // 創建目錄
            Files.createDirectories(filePath.getParent());
            
            // 創建數據記錄
            DataRecord record = new DataRecord(
                    LocalDateTime.now(),
                    positionData.getExchange(),
                    positionData.getSymbol(),
                    "position",
                    objectMapper.writeValueAsString(positionData)
            );
            
            // 追加到文件
            String jsonLine = objectMapper.writeValueAsString(record) + "\n";
            Files.write(filePath, jsonLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            logger.error("保存持倉數據失敗", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<MarketData.PriceData> getPriceHistory(String exchange, String symbol, 
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        lock.readLock().lock();
        try {
            List<MarketData.PriceData> result = new ArrayList<>();
            
            // 遍歷時間範圍內的所有日期
            LocalDateTime currentDate = startTime.toLocalDate().atStartOfDay();
            while (!currentDate.isAfter(endTime)) {
                String date = currentDate.format(DATE_FORMATTER);
                Path filePath = dataDirectory.resolve("prices")
                        .resolve(exchange.toLowerCase())
                        .resolve(date + ".jsonl");
                
                if (Files.exists(filePath)) {
                    result.addAll(readPriceDataFromFile(filePath, symbol, startTime, endTime));
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<MarketData.PositionData> getPositionHistory(String exchange, String symbol, 
                                                             LocalDateTime startTime, LocalDateTime endTime) {
        lock.readLock().lock();
        try {
            List<MarketData.PositionData> result = new ArrayList<>();
            
            // 遍歷時間範圍內的所有日期
            LocalDateTime currentDate = startTime.toLocalDate().atStartOfDay();
            while (!currentDate.isAfter(endTime)) {
                String date = currentDate.format(DATE_FORMATTER);
                Path filePath = dataDirectory.resolve("positions")
                        .resolve(exchange.toLowerCase())
                        .resolve(date + ".jsonl");
                
                if (Files.exists(filePath)) {
                    result.addAll(readPositionDataFromFile(filePath, symbol, startTime, endTime));
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void cleanupOldData(int daysToKeep) {
        lock.writeLock().lock();
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            
            // 清理價格數據
            cleanupDirectory(dataDirectory.resolve("prices"), cutoffDate);
            
            // 清理持倉數據
            cleanupDirectory(dataDirectory.resolve("positions"), cutoffDate);
            
            logger.info("已清理{}天前的數據", daysToKeep);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void close() {
        logger.info("文件數據存儲已關閉");
    }
    
    private List<MarketData.PriceData> readPriceDataFromFile(Path filePath, String symbol, 
                                                              LocalDateTime startTime, LocalDateTime endTime) {
        List<MarketData.PriceData> result = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    DataRecord record = objectMapper.readValue(line, DataRecord.class);
                    
                    // 檢查時間範圍和交易對
                    if (record.getTimestamp().isAfter(startTime) && 
                        record.getTimestamp().isBefore(endTime) &&
                        record.getSymbol().equals(symbol)) {
                        
                        MarketData.PriceData priceData = objectMapper.readValue(
                                record.getData(), MarketData.PriceData.class);
                        result.add(priceData);
                    }
                } catch (Exception e) {
                    logger.warn("解析價格數據行失敗: {}", line, e);
                }
            }
        } catch (IOException e) {
            logger.error("讀取價格數據文件失敗: {}", filePath, e);
        }
        
        return result;
    }
    
    private List<MarketData.PositionData> readPositionDataFromFile(Path filePath, String symbol, 
                                                                    LocalDateTime startTime, LocalDateTime endTime) {
        List<MarketData.PositionData> result = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    DataRecord record = objectMapper.readValue(line, DataRecord.class);
                    
                    // 檢查時間範圍和交易對
                    if (record.getTimestamp().isAfter(startTime) && 
                        record.getTimestamp().isBefore(endTime) &&
                        record.getSymbol().equals(symbol)) {
                        
                        MarketData.PositionData positionData = objectMapper.readValue(
                                record.getData(), MarketData.PositionData.class);
                        result.add(positionData);
                    }
                } catch (Exception e) {
                    logger.warn("解析持倉數據行失敗: {}", line, e);
                }
            }
        } catch (IOException e) {
            logger.error("讀取持倉數據文件失敗: {}", filePath, e);
        }
        
        return result;
    }
    
    private void cleanupDirectory(Path directory, LocalDateTime cutoffDate) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString();
                            if (fileName.endsWith(".jsonl")) {
                                String dateStr = fileName.replace(".jsonl", "");
                                try {
                                    LocalDateTime fileDate = LocalDateTime.parse(dateStr + "T00:00:00");
                                    return fileDate.isBefore(cutoffDate);
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return false;
                        })
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                logger.debug("已刪除過期文件: {}", path);
                            } catch (IOException e) {
                                logger.warn("刪除文件失敗: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.error("清理目錄失敗: {}", directory, e);
        }
    }
    
    /**
     * 數據記錄結構
     */
    public static class DataRecord {
        private LocalDateTime timestamp;
        private String exchange;
        private String symbol;
        private String type;
        private String data;
        
        public DataRecord() {}
        
        public DataRecord(LocalDateTime timestamp, String exchange, String symbol, String type, String data) {
            this.timestamp = timestamp;
            this.exchange = exchange;
            this.symbol = symbol;
            this.type = type;
            this.data = data;
        }
        
        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }
        
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
} 