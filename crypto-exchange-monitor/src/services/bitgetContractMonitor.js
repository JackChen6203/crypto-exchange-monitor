const WebSocket = require('ws');
const BitgetApi = require('./bitgetApi');
const Logger = require('../utils/logger');
const DatabaseManager = require('./databaseManager');

class BitgetContractMonitor {
  constructor(config, discordService) {
    this.config = config;
    this.discordService = discordService;
    this.bitgetApi = new BitgetApi(config);
    this.logger = new Logger(config);
    this.db = new DatabaseManager(config);
    
    // 持倉量數據存儲 (支持多個時間週期)
    this.openInterests = {
      current: new Map(),   // 當前數據
      '15m': new Map(),     // 15分鐘前
      '1h': new Map(),      // 1小時前  
      '4h': new Map(),      // 4小時前
      '1d': new Map()       // 1天前
    };
    
    // 資金費率數據存儲
    this.fundingRates = new Map();
    
    // 合約交易對列表
    this.contractSymbols = [];
    
    // 定期監控間隔
    this.monitoringInterval = null;
    this.reportingInterval = null;
    
    // 數據歷史記錄
    this.dataHistory = {
      openInterest: [],
      fundingRate: []
    };
  }

  async initialize() {
    try {
      this.logger.console('🚀 初始化Bitget合約監控系統...');
      
      // 初始化數據庫
      await this.db.initialize();
      
      // 測試API連接
      const connectionTest = await this.bitgetApi.testConnection();
      if (!connectionTest) {
        throw new Error('API連接測試失敗');
      }

      // 載入合約交易對
      await this.loadContractSymbols();
      
      // 初始化數據收集
      await this.collectInitialData();
      
      // 啟動定期監控
      this.startPeriodicMonitoring();
      
      // 啟動15分鐘報告
      this.startPeriodicReporting();
      
      this.logger.console('✅ Bitget合約監控系統初始化完成');
      
    } catch (error) {
      this.logger.error('❌ 初始化失敗:', error);
      throw error;
    }
  }

  async loadContractSymbols() {
    try {
      this.logger.info('📊 載入合約交易對...');
      
      // 獲取USDT永續合約
      const symbols = await this.bitgetApi.getSymbolsByProductType('umcbl');
      this.contractSymbols = symbols.filter(s => s.status === 'normal');
      
      this.logger.info(`📈 成功載入 ${this.contractSymbols.length} 個合約交易對`);
      
    } catch (error) {
      this.logger.error('❌ 載入合約交易對失敗:', error);
      // 使用主要合約作為備用
      this.contractSymbols = [
        { symbol: 'BTCUSDT', productType: 'umcbl' },
        { symbol: 'ETHUSDT', productType: 'umcbl' },
        { symbol: 'BNBUSDT', productType: 'umcbl' }
      ];
      this.logger.warn('⚠️ 使用備用合約交易對列表');
    }
  }

  async collectInitialData() {
    try {
      this.logger.info('📊 收集初始數據...');
      
      // 批量獲取所有合約的開倉量
      const openInterestData = await this.bitgetApi.getAllOpenInterest('umcbl');
      
      // 存儲當前開倉量數據
      openInterestData.forEach(async (data) => {
        this.openInterests.current.set(data.symbol, data);
        // 保存到數據庫
        try {
          await this.db.saveOpenInterest(data);
        } catch (error) {
          this.logger.debug(`⚠️ 保存 ${data.symbol} 持倉量數據失敗:`, error.message);
        }
      });
      
      // 獲取資金費率 (分批處理避免API限制)
      const batchSize = 10;
      for (let i = 0; i < this.contractSymbols.length; i += batchSize) {
        const batch = this.contractSymbols.slice(i, i + batchSize);
        
        await Promise.all(batch.map(async (contract) => {
          try {
            const fundingRate = await this.bitgetApi.getFundingRate(contract.symbol, 'umcbl');
            this.fundingRates.set(contract.symbol, fundingRate);
            // 保存到數據庫
            await this.db.saveFundingRate(fundingRate);
          } catch (error) {
            this.logger.warn(`⚠️ 獲取 ${contract.symbol} 資金費率失敗:`, error.message);
          }
        }));
        
        // 批次間延遲
        if (i + batchSize < this.contractSymbols.length) {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
      
      this.logger.info(`✅ 收集到 ${this.openInterests.current.size} 個開倉量數據和 ${this.fundingRates.size} 個資金費率數據`);
      
    } catch (error) {
      this.logger.error('❌ 收集初始數據失敗:', error);
    }
  }

  startPeriodicMonitoring() {
    // 每5分鐘更新一次數據
    this.monitoringInterval = setInterval(async () => {
      await this.updateContractData();
    }, 5 * 60 * 1000); // 5分鐘
    
    this.logger.info('🔄 啟動定期監控 (每5分鐘更新合約數據)');
  }

  startPeriodicReporting() {
    // 每15分鐘發送一次報告
    this.reportingInterval = setInterval(async () => {
      await this.generateAndSendReport();
    }, 15 * 60 * 1000); // 15分鐘
    
    this.logger.info('📊 啟動定期報告 (每15分鐘發送持倉異動和資金費率排行)');
  }

  async updateContractData() {
    try {
      this.logger.debug('🔍 更新合約數據中...');
      
      // 備份歷史數據
      this.backupHistoricalData();
      
      // 獲取最新開倉量數據
      const openInterestData = await this.bitgetApi.getAllOpenInterest('umcbl');
      
      // 更新當前開倉量數據
      openInterestData.forEach(data => {
        this.openInterests.current.set(data.symbol, data);
      });
      
      // 更新資金費率數據 (分批處理)
      const activeSylmbols = Array.from(this.openInterests.current.keys()).slice(0, 50); // 只更新前50個活躍合約
      const batchSize = 10;
      
      for (let i = 0; i < activeSylmbols.length; i += batchSize) {
        const batch = activeSylmbols.slice(i, i + batchSize);
        
        await Promise.all(batch.map(async (symbol) => {
          try {
            const fundingRate = await this.bitgetApi.getFundingRate(symbol, 'umcbl');
            this.fundingRates.set(symbol, fundingRate);
          } catch (error) {
            this.logger.debug(`⚠️ 更新 ${symbol} 資金費率失敗:`, error.message);
          }
        }));
        
        // 批次間延遲
        await new Promise(resolve => setTimeout(resolve, 500));
      }
      
      this.logger.debug('✅ 合約數據更新完成');
      
    } catch (error) {
      this.logger.error('❌ 更新合約數據失敗:', error);
    }
  }

  backupHistoricalData() {
    const now = Date.now();
    const currentData = this.openInterests.current;
    
    // 檢查是否需要備份到各個時間點
    if (this.shouldBackup('15m', now)) {
      this.openInterests['15m'] = new Map(currentData);
    }
    if (this.shouldBackup('1h', now)) {
      this.openInterests['1h'] = new Map(currentData);
    }
    if (this.shouldBackup('4h', now)) {
      this.openInterests['4h'] = new Map(currentData);
    }
    if (this.shouldBackup('1d', now)) {
      this.openInterests['1d'] = new Map(currentData);
    }
  }

  shouldBackup(period, now) {
    // 簡化的備份邏輯，實際應該基於精確的時間間隔
    const intervals = {
      '15m': 15 * 60 * 1000,
      '1h': 60 * 60 * 1000,
      '4h': 4 * 60 * 60 * 1000,
      '1d': 24 * 60 * 60 * 1000
    };
    
    if (!this.lastBackupTime) {
      this.lastBackupTime = {};
    }
    
    const lastBackup = this.lastBackupTime[period] || 0;
    if (now - lastBackup >= intervals[period]) {
      this.lastBackupTime[period] = now;
      return true;
    }
    return false;
  }

  async generateAndSendReport() {
    try {
      this.logger.info('📊 生成持倉異動和資金費率排行報告...');
      
      // 生成持倉異動排行
      const positionChanges = this.calculatePositionChanges();
      
      // 生成資金費率排行
      const fundingRateRankings = this.calculateFundingRateRankings();
      
      // 發送Discord報告
      await this.sendPositionChangeReport(positionChanges);
      await this.sendFundingRateReport(fundingRateRankings);
      
      this.logger.info('✅ 報告發送完成');
      
    } catch (error) {
      this.logger.error('❌ 生成報告失敗:', error);
    }
  }

  calculatePositionChanges() {
    const periods = ['15m', '1h', '4h', '1d'];
    const results = {};
    
    periods.forEach(period => {
      const currentData = this.openInterests.current;
      const historicalData = this.openInterests[period];
      const changes = [];
      
      if (historicalData && historicalData.size > 0) {
        currentData.forEach((current, symbol) => {
          const historical = historicalData.get(symbol);
          if (historical) {
            const change = current.openInterestUsd - historical.openInterestUsd;
            const changePercent = (change / historical.openInterestUsd) * 100;
            
            changes.push({
              symbol,
              current: current.openInterestUsd,
              previous: historical.openInterestUsd,
              change,
              changePercent
            });
          }
        });
      }
      
      // 排序：正異動和負異動分別排序
      const positiveChanges = changes
        .filter(c => c.change > 0)
        .sort((a, b) => b.changePercent - a.changePercent)
        .slice(0, 15);
        
      const negativeChanges = changes
        .filter(c => c.change < 0)
        .sort((a, b) => a.changePercent - b.changePercent)
        .slice(0, 15);
      
      results[period] = {
        positive: positiveChanges,
        negative: negativeChanges
      };
    });
    
    return results;
  }

  calculateFundingRateRankings() {
    const fundingRates = Array.from(this.fundingRates.values())
      .filter(rate => rate.fundingRate != null)
      .map(rate => ({
        symbol: rate.symbol,
        fundingRate: rate.fundingRate,
        fundingRatePercent: rate.fundingRate * 100
      }));
    
    // 正資金費率排行（最高15個）
    const positiveFunding = fundingRates
      .filter(rate => rate.fundingRate > 0)
      .sort((a, b) => b.fundingRate - a.fundingRate)
      .slice(0, 15);
    
    // 負資金費率排行（最低15個）
    const negativeFunding = fundingRates
      .filter(rate => rate.fundingRate < 0)
      .sort((a, b) => a.fundingRate - b.fundingRate)
      .slice(0, 15);
    
    return {
      positive: positiveFunding,
      negative: negativeFunding
    };
  }

  async sendPositionChangeReport(positionChanges) {
    const periods = ['15m', '1h', '4h', '1d'];
    const periodNames = {
      '15m': '15分',
      '1h': '1時', 
      '4h': '4時',
      '1d': '日線'
    };
    
    // 收集所有期間的正異動數據，合併成一個表格
    await this.sendCombinedPositiveChangesReport(positionChanges, periods, periodNames);
    
    // 間隔發送
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // 收集所有期間的負異動數據，合併成一個表格
    await this.sendCombinedNegativeChangesReport(positionChanges, periods, periodNames);
  }

  async sendCombinedPositiveChangesReport(positionChanges, periods, periodNames) {
    // 收集所有交易對的多時間週期數據
    const combinedData = new Map();
    
    periods.forEach(period => {
      const data = positionChanges[period];
      if (data && data.positive.length > 0) {
        data.positive.slice(0, 10).forEach(item => { // 每個時間週期取前10名
          if (!combinedData.has(item.symbol)) {
            combinedData.set(item.symbol, {
              symbol: item.symbol,
              current: item.current,
              changes: {}
            });
          }
          combinedData.get(item.symbol).changes[period] = item.changePercent;
        });
      }
    });

    if (combinedData.size === 0) return;

    // 按15分鐘變化排序
    const sortedData = Array.from(combinedData.values())
      .sort((a, b) => (b.changes['15m'] || 0) - (a.changes['15m'] || 0))
      .slice(0, 15);

    // 生成表格
    const tableRows = sortedData.map((item, index) => {
      const symbol = item.symbol.padEnd(10);
      const current = this.formatNumber(item.current).padStart(8);
      const change15m = (item.changes['15m'] || 0).toFixed(2).padStart(6) + '%';
      const change1h = (item.changes['1h'] || 0).toFixed(2).padStart(6) + '%';
      const change4h = (item.changes['4h'] || 0).toFixed(2).padStart(6) + '%';
      const change1d = (item.changes['1d'] || 0).toFixed(2).padStart(6) + '%';
      
      return `${(index + 1).toString().padStart(2)} | ${symbol} | ${current} | ${change15m} | ${change1h} | ${change4h} | ${change1d}`;
    }).join('\n');

    const tableContent = `\`\`\`
📈 持倉量增長排行 TOP15 (多時間週期對比)

排名 | 交易對      | 當前持倉   | 15分   | 1時    | 4時    | 日線
-----|-----------|----------|--------|--------|--------|--------
${tableRows}
\`\`\``;

    await this.discordService.sendMessage(tableContent);
  }

  async sendCombinedNegativeChangesReport(positionChanges, periods, periodNames) {
    // 收集所有交易對的多時間週期數據
    const combinedData = new Map();
    
    periods.forEach(period => {
      const data = positionChanges[period];
      if (data && data.negative.length > 0) {
        data.negative.slice(0, 10).forEach(item => { // 每個時間週期取前10名
          if (!combinedData.has(item.symbol)) {
            combinedData.set(item.symbol, {
              symbol: item.symbol,
              current: item.current,
              changes: {}
            });
          }
          combinedData.get(item.symbol).changes[period] = item.changePercent;
        });
      }
    });

    if (combinedData.size === 0) return;

    // 按15分鐘變化排序（負值，所以是從小到大）
    const sortedData = Array.from(combinedData.values())
      .sort((a, b) => (a.changes['15m'] || 0) - (b.changes['15m'] || 0))
      .slice(0, 15);

    // 生成表格
    const tableRows = sortedData.map((item, index) => {
      const symbol = item.symbol.padEnd(10);
      const current = this.formatNumber(item.current).padStart(8);
      const change15m = (item.changes['15m'] || 0).toFixed(2).padStart(6) + '%';
      const change1h = (item.changes['1h'] || 0).toFixed(2).padStart(6) + '%';
      const change4h = (item.changes['4h'] || 0).toFixed(2).padStart(6) + '%';
      const change1d = (item.changes['1d'] || 0).toFixed(2).padStart(6) + '%';
      
      return `${(index + 1).toString().padStart(2)} | ${symbol} | ${current} | ${change15m} | ${change1h} | ${change4h} | ${change1d}`;
    }).join('\n');

    const tableContent = `\`\`\`
📉 持倉量減少排行 TOP15 (多時間週期對比)

排名 | 交易對      | 當前持倉   | 15分   | 1時    | 4時    | 日線
-----|-----------|----------|--------|--------|--------|--------
${tableRows}
\`\`\``;

    await this.discordService.sendMessage(tableContent);
  }

  async sendFundingRateReport(fundingRateRankings) {
    // 合併正負資金費率在一個表格中
    const positiveRates = fundingRateRankings.positive.slice(0, 15);
    const negativeRates = fundingRateRankings.negative.slice(0, 15);
    
    if (positiveRates.length === 0 && negativeRates.length === 0) return;

    // 生成正資金費率部分
    const positiveRows = positiveRates.map((item, index) => 
      `${(index + 1).toString().padStart(2)} | ${item.symbol.padEnd(10)} | ${item.fundingRatePercent.toFixed(4).padStart(7)}%`
    );

    // 生成負資金費率部分
    const negativeRows = negativeRates.map((item, index) => 
      `${(index + 1).toString().padStart(2)} | ${item.symbol.padEnd(10)} | ${item.fundingRatePercent.toFixed(4).padStart(7)}%`
    );

    // 創建並列表格 - 分別顯示正負費率
    const maxRows = Math.max(positiveRows.length, negativeRows.length);
    const combinedRows = [];
    
    for (let i = 0; i < maxRows; i++) {
      const positiveRow = positiveRows[i] || '   |            |        ';
      const negativeRow = negativeRows[i] || '   |            |        ';
      combinedRows.push(`${positiveRow} || ${negativeRow}`);
    }

    const tableContent = `\`\`\`
💰💸 資金費率排行 TOP15

正費率(多頭付費)                    || 負費率(空頭付費)
排名| 交易對     | 費率     || 排名| 交易對     | 費率
----|-----------|----------||-----|-----------|----------
${combinedRows.join('\n')}
\`\`\``;

    await this.discordService.sendMessage(tableContent);
  }

  formatNumber(num) {
    if (num >= 1e9) {
      return (num / 1e9).toFixed(2) + 'B';
    } else if (num >= 1e6) {
      return (num / 1e6).toFixed(2) + 'M';
    } else if (num >= 1e3) {
      return (num / 1e3).toFixed(2) + 'K';
    }
    return num.toFixed(2);
  }

  stop() {
    if (this.monitoringInterval) {
      clearInterval(this.monitoringInterval);
      this.monitoringInterval = null;
    }
    
    if (this.reportingInterval) {
      clearInterval(this.reportingInterval);
      this.reportingInterval = null;
    }
    
    // 關閉數據庫連接
    if (this.db) {
      this.db.close();
    }
    
    this.logger.info('⏹️ 合約監控系統已停止');
  }

  getStatus() {
    return {
      isRunning: this.monitoringInterval !== null,
      contractSymbols: this.contractSymbols.length,
      openInterestData: this.openInterests.current.size,
      fundingRateData: this.fundingRates.size,
      tradingType: '合約交易'
    };
  }
}

module.exports = BitgetContractMonitor;