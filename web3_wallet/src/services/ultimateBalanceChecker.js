const axios = require('axios');
const crypto = require('crypto');

class UltimateBalanceChecker {
  constructor() {
    // 🚀 超級多源 Bitcoin API 提供商（12個來源）
    this.bitcoinAPIs = [
      // 免費高速 APIs
      {
        name: 'Blockstream',
        url: 'https://blockstream.info/api',
        rateLimit: 50,
        priority: 1,
        method: 'GET',
        endpoint: '/address/{address}',
        parser: 'blockstream'
      },
      {
        name: 'BlockCypher',
        url: 'https://api.blockcypher.com/v1/btc/main',
        rateLimit: 200,
        priority: 2,
        method: 'GET',
        endpoint: '/addrs/{address}/balance',
        parser: 'blockcypher'
      },
      {
        name: 'Blockchain.info',
        url: 'https://blockchain.info',
        rateLimit: 10,
        priority: 3,
        method: 'GET',
        endpoint: '/q/addressbalance/{address}',
        parser: 'blockchain_info'
      },
      {
        name: 'Blockchair',
        url: 'https://api.blockchair.com/bitcoin',
        rateLimit: 30,
        priority: 4,
        method: 'GET',
        endpoint: '/dashboards/address/{address}',
        parser: 'blockchair'
      },
      {
        name: 'BitGo',
        url: 'https://www.bitgo.com/api/v1',
        rateLimit: 60,
        priority: 5,
        method: 'GET',
        endpoint: '/address/{address}',
        parser: 'bitgo'
      },
      {
        name: 'Insight',
        url: 'https://insight.bitpay.com/api',
        rateLimit: 40,
        priority: 6,
        method: 'GET',
        endpoint: '/addr/{address}',
        parser: 'insight'
      },
      {
        name: 'SoChain',
        url: 'https://sochain.com/api/v2',
        rateLimit: 300,
        priority: 7,
        method: 'GET',
        endpoint: '/get_address_balance/BTC/{address}',
        parser: 'sochain'
      },
      {
        name: 'Mempool.space',
        url: 'https://mempool.space/api',
        rateLimit: 60,
        priority: 8,
        method: 'GET',
        endpoint: '/address/{address}',
        parser: 'mempool'
      },
      {
        name: 'BTCExplorer',
        url: 'https://blockexplorer.com/api',
        rateLimit: 30,
        priority: 9,
        method: 'GET',
        endpoint: '/addr/{address}',
        parser: 'btcexplorer'
      },
      {
        name: 'CryptoID',
        url: 'https://chainz.cryptoid.info/btc/api.dws',
        rateLimit: 100,
        priority: 10,
        method: 'GET',
        endpoint: '?q=getbalance&a={address}',
        parser: 'cryptoid'
      },
      {
        name: 'SmartBit',
        url: 'https://api.smartbit.com.au/v1/blockchain',
        rateLimit: 50,
        priority: 11,
        method: 'GET',
        endpoint: '/address/{address}',
        parser: 'smartbit'
      },
      {
        name: 'BitCore',
        url: 'https://api.bitcore.io/api/BTC/mainnet',
        rateLimit: 80,
        priority: 12,
        method: 'GET',
        endpoint: '/address/{address}',
        parser: 'bitcore'
      }
    ];

    // 智能負載平衡配置
    this.queue = [];
    this.processing = false;
    this.maxConcurrent = 50; // 增加並發數
    this.activeRequests = 0;
    this.retryAttempts = 5; // 增加重試次數
    this.baseDelay = 50; // 減少基礎延遲

    // API 狀態追踪（更詳細）
    this.apiStats = new Map();
    this.apiHealthScore = new Map();
    this.initializeAPIStats();

    // 高級緩存系統
    this.cache = new Map();
    this.cacheExpiry = 10 * 60 * 1000; // 10分鐘緩存
    this.negativeCacheExpiry = 2 * 60 * 1000; // 2分鐘負緩存

    // 性能優化
    this.batchMode = true;
    this.adaptiveRateLimit = true;
    this.circuitBreaker = new Map();

    this.startQueueProcessor();
    this.startHealthMonitor();
  }

  // 初始化 API 統計
  initializeAPIStats() {
    this.bitcoinAPIs.forEach(api => {
      this.apiStats.set(api.name, {
        totalRequests: 0,
        successfulRequests: 0,
        failedRequests: 0,
        averageResponseTime: 0,
        lastRequestTime: 0,
        isActive: true,
        errorCount: 0,
        lastError: null,
        consecutiveFailures: 0,
        successStreak: 0,
        hourlyRequests: 0,
        dailyRequests: 0,
        lastHourReset: Date.now(),
        lastDayReset: Date.now()
      });
      
      this.apiHealthScore.set(api.name, 100);
      this.circuitBreaker.set(api.name, {
        state: 'CLOSED', // CLOSED, OPEN, HALF_OPEN
        failures: 0,
        lastFailureTime: 0,
        timeout: 30000 // 30秒
      });
    });
  }

  // 健康監控器
  startHealthMonitor() {
    setInterval(() => {
      this.updateHealthScores();
      this.resetHourlyCounters();
      this.cleanExpiredCache();
      this.updateCircuitBreakers();
    }, 60000); // 每分鐘檢查
  }

  // 更新健康分數
  updateHealthScores() {
    for (const [apiName, stats] of this.apiStats) {
      let score = 100;
      
      if (stats.totalRequests > 0) {
        const successRate = stats.successfulRequests / stats.totalRequests;
        const responseTimeScore = Math.max(0, 100 - (stats.averageResponseTime / 100));
        const stabilityScore = Math.max(0, 100 - (stats.consecutiveFailures * 20));
        
        score = (successRate * 50) + (responseTimeScore * 30) + (stabilityScore * 20);
      }
      
      this.apiHealthScore.set(apiName, Math.max(0, Math.min(100, score)));
    }
  }

  // 超級智能 API 選擇
  selectBestAPI() {
    const now = Date.now();
    const availableAPIs = this.bitcoinAPIs.filter(api => {
      const stats = this.apiStats.get(api.name);
      const circuit = this.circuitBreaker.get(api.name);
      const healthScore = this.apiHealthScore.get(api.name);
      
      // 基本可用性檢查
      if (!stats.isActive || circuit.state === 'OPEN') return false;
      if (healthScore < 30) return false; // 健康分數太低
      
      // 自適應速率限制
      if (this.adaptiveRateLimit) {
        const timeSinceLastRequest = now - stats.lastRequestTime;
        const dynamicRateLimit = this.calculateDynamicRateLimit(api, stats);
        if (timeSinceLastRequest < (1000 / dynamicRateLimit)) return false;
      }
      
      return true;
    });

    if (availableAPIs.length === 0) {
      // 緊急模式：使用任何可用的API
      const emergencyAPIs = this.bitcoinAPIs.filter(api => {
        const stats = this.apiStats.get(api.name);
        return stats.isActive;
      });
      
      if (emergencyAPIs.length > 0) {
        return emergencyAPIs[0];
      }
      
      // 最後手段：重置所有API狀態
      this.resetAllAPIs();
      return this.bitcoinAPIs[0];
    }

    // 智能選擇算法
    return availableAPIs.sort((a, b) => {
      const aHealth = this.apiHealthScore.get(a.name);
      const bHealth = this.apiHealthScore.get(b.name);
      const aStats = this.apiStats.get(a.name);
      const bStats = this.apiStats.get(b.name);
      
      // 綜合評分
      const aScore = (aHealth * 0.4) + 
                    ((aStats.successStreak * 2) * 0.3) +
                    (a.priority === 1 ? 30 : (1 / a.priority) * 20) * 0.3;
      
      const bScore = (bHealth * 0.4) + 
                    ((bStats.successStreak * 2) * 0.3) +
                    (b.priority === 1 ? 30 : (1 / b.priority) * 20) * 0.3;
      
      return bScore - aScore;
    })[0];
  }

  // 動態速率限制計算
  calculateDynamicRateLimit(api, stats) {
    let dynamicRate = api.rateLimit;
    
    // 根據成功率調整
    if (stats.totalRequests > 10) {
      const successRate = stats.successfulRequests / stats.totalRequests;
      if (successRate > 0.95) {
        dynamicRate *= 1.5; // 增加50%
      } else if (successRate < 0.8) {
        dynamicRate *= 0.5; // 減少50%
      }
    }
    
    // 根據響應時間調整
    if (stats.averageResponseTime > 3000) {
      dynamicRate *= 0.7;
    } else if (stats.averageResponseTime < 500) {
      dynamicRate *= 1.2;
    }
    
    return Math.max(1, Math.min(dynamicRate, api.rateLimit * 2));
  }

  // 熔斷器更新
  updateCircuitBreakers() {
    const now = Date.now();
    
    for (const [apiName, circuit] of this.circuitBreaker) {
      if (circuit.state === 'OPEN') {
        if (now - circuit.lastFailureTime > circuit.timeout) {
          circuit.state = 'HALF_OPEN';
          circuit.failures = 0;
        }
      }
    }
  }

  // 解析不同 API 響應
  parseAPIResponse(data, parser, address) {
    let balance = { confirmed: 0, unconfirmed: 0, total: 0 };

    try {
      switch (parser) {
        case 'blockstream':
          balance = {
            confirmed: data.chain_stats.funded_txo_sum - data.chain_stats.spent_txo_sum,
            unconfirmed: data.mempool_stats.funded_txo_sum - data.mempool_stats.spent_txo_sum,
            total: (data.chain_stats.funded_txo_sum - data.chain_stats.spent_txo_sum) + 
                   (data.mempool_stats.funded_txo_sum - data.mempool_stats.spent_txo_sum)
          };
          break;
          
        case 'blockcypher':
          balance = {
            confirmed: data.balance || 0,
            unconfirmed: data.unconfirmed_balance || 0,
            total: (data.balance || 0) + (data.unconfirmed_balance || 0)
          };
          break;
          
        case 'blockchain_info':
          const totalSatoshis = parseInt(data) || 0;
          balance = { confirmed: totalSatoshis, unconfirmed: 0, total: totalSatoshis };
          break;
          
        case 'blockchair':
          const addrData = data.data[address];
          if (addrData) {
            balance = {
              confirmed: addrData.address.balance || 0,
              unconfirmed: addrData.address.unconfirmed_balance || 0,
              total: (addrData.address.balance || 0) + (addrData.address.unconfirmed_balance || 0)
            };
          }
          break;
          
        case 'sochain':
          if (data.status === 'success') {
            const balanceStr = data.data.confirmed_balance;
            const satoshis = Math.round(parseFloat(balanceStr) * 100000000);
            balance = { confirmed: satoshis, unconfirmed: 0, total: satoshis };
          }
          break;
          
        case 'mempool':
          balance = {
            confirmed: data.chain_stats.funded_txo_sum - data.chain_stats.spent_txo_sum,
            unconfirmed: data.mempool_stats.funded_txo_sum - data.mempool_stats.spent_txo_sum,
            total: (data.chain_stats.funded_txo_sum - data.chain_stats.spent_txo_sum) + 
                   (data.mempool_stats.funded_txo_sum - data.mempool_stats.spent_txo_sum)
          };
          break;
          
        case 'insight':
        case 'btcexplorer':
          balance = {
            confirmed: Math.round((data.balance || 0) * 100000000),
            unconfirmed: Math.round((data.unconfirmedBalance || 0) * 100000000),
            total: Math.round(((data.balance || 0) + (data.unconfirmedBalance || 0)) * 100000000)
          };
          break;
          
        case 'cryptoid':
          const balanceBTC = parseFloat(data) || 0;
          const satoshis = Math.round(balanceBTC * 100000000);
          balance = { confirmed: satoshis, unconfirmed: 0, total: satoshis };
          break;
          
        case 'smartbit':
          if (data.success && data.address) {
            balance = {
              confirmed: data.address.confirmed.balance_int || 0,
              unconfirmed: data.address.unconfirmed.balance_int || 0,
              total: (data.address.confirmed.balance_int || 0) + (data.address.unconfirmed.balance_int || 0)
            };
          }
          break;
          
        case 'bitcore':
          balance = {
            confirmed: data.balance || 0,
            unconfirmed: data.unconfirmedBalance || 0,
            total: (data.balance || 0) + (data.unconfirmedBalance || 0)
          };
          break;
          
        default:
          console.warn(`未知的解析器: ${parser}`);
      }
    } catch (parseError) {
      console.warn(`解析 ${parser} 響應失敗:`, parseError.message);
    }

    return {
      address,
      ...balance,
      balanceInBTC: balance.total / 100000000,
      hasBalance: balance.total > 0,
      type: 'bitcoin',
      source: parser
    };
  }

  // 執行餘額檢查
  async executeBalanceCheck(address) {
    // 檢查緩存
    const cacheKey = `btc:${address}`;
    const cached = this.cache.get(cacheKey);
    if (cached && (Date.now() - cached.timestamp) < this.cacheExpiry) {
      return cached.data;
    }

    const api = this.selectBestAPI();
    if (!api) {
      throw new Error('沒有可用的 API');
    }

    const startTime = Date.now();
    const stats = this.apiStats.get(api.name);
    const circuit = this.circuitBreaker.get(api.name);

    try {
      const url = api.url + api.endpoint.replace('{address}', address);
      const response = await axios.get(url, {
        timeout: 8000,
        headers: {
          'User-Agent': 'UltimateBalanceChecker/1.0',
          'Accept': 'application/json'
        }
      });

      const responseTime = Date.now() - startTime;
      this.updateAPIStats(api.name, true, responseTime);
      this.updateCircuitBreaker(api.name, true);

      const result = this.parseAPIResponse(response.data, api.parser, address);

      // 緩存結果
      const cacheExpiry = result.hasBalance ? this.cacheExpiry : this.negativeCacheExpiry;
      this.cache.set(cacheKey, {
        data: result,
        timestamp: Date.now()
      });

      return result;

    } catch (error) {
      const responseTime = Date.now() - startTime;
      this.updateAPIStats(api.name, false, responseTime, error.message);
      this.updateCircuitBreaker(api.name, false);
      throw error;
    }
  }

  // 更新熔斷器狀態
  updateCircuitBreaker(apiName, success) {
    const circuit = this.circuitBreaker.get(apiName);
    
    if (success) {
      circuit.failures = 0;
      if (circuit.state === 'HALF_OPEN') {
        circuit.state = 'CLOSED';
      }
    } else {
      circuit.failures++;
      circuit.lastFailureTime = Date.now();
      
      if (circuit.failures >= 5) {
        circuit.state = 'OPEN';
      }
    }
  }

  // 更新 API 統計
  updateAPIStats(apiName, success, responseTime, error = null) {
    const stats = this.apiStats.get(apiName);
    stats.totalRequests++;
    stats.lastRequestTime = Date.now();

    if (success) {
      stats.successfulRequests++;
      stats.consecutiveFailures = 0;
      stats.successStreak++;
      stats.averageResponseTime = (stats.averageResponseTime + responseTime) / 2;
      stats.errorCount = Math.max(0, stats.errorCount - 1);
    } else {
      stats.failedRequests++;
      stats.consecutiveFailures++;
      stats.successStreak = 0;
      stats.errorCount++;
      stats.lastError = error;
      
      // 動態禁用
      if (stats.consecutiveFailures >= 10) {
        stats.isActive = false;
        setTimeout(() => {
          stats.isActive = true;
          stats.consecutiveFailures = 0;
        }, 120000); // 2分鐘後重新啟用
      }
    }
  }

  // 隊列處理
  async startQueueProcessor() {
    this.processing = true;
    
    while (this.processing) {
      if (this.queue.length > 0 && this.activeRequests < this.maxConcurrent) {
        const request = this.queue.shift();
        this.activeRequests++;
        
        this.processRequest(request)
          .finally(() => {
            this.activeRequests--;
          });
      }
      
      await new Promise(resolve => setTimeout(resolve, 5));
    }
  }

  // 處理請求
  async processRequest(request) {
    try {
      const result = await this.executeBalanceCheck(request.address);
      request.resolve(result);
    } catch (error) {
      if (request.retries < this.retryAttempts) {
        request.retries++;
        const delay = this.baseDelay * Math.pow(2, request.retries);
        setTimeout(() => {
          this.queue.unshift(request);
        }, delay);
      } else {
        request.reject(error);
      }
    }
  }

  // 公共介面
  async checkBalance(address) {
    return new Promise((resolve, reject) => {
      this.queue.push({
        address,
        resolve,
        reject,
        timestamp: Date.now(),
        retries: 0
      });
    });
  }

  // 批量檢查
  async checkMultipleBalances(addresses) {
    const promises = addresses.map(address => 
      this.checkBalance(address).catch(error => ({
        address,
        error: error.message,
        hasBalance: false,
        type: 'bitcoin'
      }))
    );
    
    return await Promise.all(promises);
  }

  // 重置 API
  resetAllAPIs() {
    for (const [apiName, stats] of this.apiStats) {
      stats.isActive = true;
      stats.consecutiveFailures = 0;
      stats.errorCount = 0;
    }
    
    for (const [apiName, circuit] of this.circuitBreaker) {
      circuit.state = 'CLOSED';
      circuit.failures = 0;
    }
  }

  // 重置計數器
  resetHourlyCounters() {
    const now = Date.now();
    for (const [apiName, stats] of this.apiStats) {
      if (now - stats.lastHourReset > 3600000) {
        stats.hourlyRequests = 0;
        stats.lastHourReset = now;
      }
      if (now - stats.lastDayReset > 86400000) {
        stats.dailyRequests = 0;
        stats.lastDayReset = now;
      }
    }
  }

  // 清理過期緩存
  cleanExpiredCache() {
    const now = Date.now();
    for (const [key, value] of this.cache) {
      if (now - value.timestamp > this.cacheExpiry) {
        this.cache.delete(key);
      }
    }
  }

  // 獲取統計
  getDetailedStats() {
    const stats = {};
    const totalHealth = Array.from(this.apiHealthScore.values()).reduce((a, b) => a + b, 0);
    
    for (const [apiName, apiStats] of this.apiStats) {
      stats[apiName] = {
        ...apiStats,
        healthScore: this.apiHealthScore.get(apiName),
        circuitState: this.circuitBreaker.get(apiName).state,
        successRate: apiStats.totalRequests > 0 ? 
          (apiStats.successfulRequests / apiStats.totalRequests * 100).toFixed(2) + '%' : '0%'
      };
    }
    
    return {
      apis: stats,
      overall: {
        totalAPIs: this.bitcoinAPIs.length,
        activeAPIs: Array.from(this.apiStats.values()).filter(s => s.isActive).length,
        averageHealth: (totalHealth / this.bitcoinAPIs.length).toFixed(1),
        queueSize: this.queue.length,
        activeRequests: this.activeRequests,
        cacheSize: this.cache.size
      }
    };
  }

  // 停止
  stop() {
    this.processing = false;
  }
}

module.exports = UltimateBalanceChecker;