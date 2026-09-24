import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { TradingChart } from './components/TradingChart';
import type { ChartDataDto } from './components/TradingChart';
import { StatCard } from './components/StatCard';
import { TradeHistory } from './components/TradeHistory';
import type { TradeLog } from './components/TradeHistory';
import { CollectorStatus } from './components/CollectorStatus';
import type { StatusData } from './components/CollectorStatus';
import { PatternSignals } from './components/PatternSignals';
import type { TradingSignalItem } from './components/PatternSignals';
import { PatternDetailView } from './components/PatternDetailView';
import { GuideView } from './components/GuideView';
import {
  WalletIcon,
  HistoryIcon,
  TargetIcon,
  ClockIcon,
  TrendingUpIcon,
  TrendingDownIcon,
  BarChartIcon,
  RefreshIcon,
  EyeIcon,
  InfoIcon,
} from './components/Icons';
import './index.css';

interface VirtualWallet {
  id: number;
  balance: number;
}

interface TradePosition {
  id: number;
  quantity: number;
  averagePrice: number;
  instrument?: { symbol: string };
  stopLossPrice?: number;
  takeProfitPrice?: number;
}

const API = 'http://localhost:8080';
const INITIAL_BALANCE = 10000;

function App() {
  const [data, setData] = useState<ChartDataDto[]>([]);
  const [wallet, setWallet] = useState<VirtualWallet | null>(null);
  const [positions, setPositions] = useState<TradePosition[]>([]);
  const [trades, setTrades] = useState<TradeLog[]>([]);
  const [signals, setSignals] = useState<TradingSignalItem[]>([]);
  const [status, setStatus] = useState<StatusData | null>(null);
  const [activeTab, setActiveTab] = useState<'trades' | 'patterns' | 'detail' | 'cron' | 'guide'>('trades');
  const [selectedSignal, setSelectedSignal] = useState<TradingSignalItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = async () => {
    try {
      const to = new Date().toISOString();
      const from = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();

      const [chartRes, walletRes, posRes, tradeRes, signalRes, statusRes] = await Promise.all([
        axios.get<ChartDataDto[]>(`${API}/api/v1/charts/1?timeframe=1h&from=${from}&to=${to}`),
        axios.get<VirtualWallet>(`${API}/api/v1/wallet`).catch(() => ({ data: null })),
        axios.get<TradePosition[]>(`${API}/api/v1/wallet/positions`).catch(() => ({ data: [] })),
        axios.get<TradeLog[]>(`${API}/api/v1/wallet/history`).catch(() => ({ data: [] })),
        axios.get<TradingSignalItem[]>(`${API}/api/v1/signals`).catch(() => ({ data: [] })),
        axios.get<StatusData>(`${API}/api/v1/status`).catch(() => ({ data: null })),
      ]);

      setData(chartRes.data);
      if (walletRes.data) setWallet(walletRes.data);
      setPositions(posRes.data || []);
      setTrades(tradeRes.data || []);
      setSignals(signalRes.data || []);
      if (statusRes.data) setStatus(statusRes.data);
    } catch (err: any) {
      setError(err.message || 'Không thể kết nối đến máy chủ.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
    // Auto-refresh every 10 seconds
    const interval = setInterval(fetchAll, 10000);
    return () => clearInterval(interval);
  }, []);

  const currentPrice = data.length > 0 ? data[data.length - 1].close : 0;
  const prevPrice = data.length > 1 ? data[data.length - 2].close : 0;
  const priceChange = currentPrice - prevPrice;
  const priceChangePercent = prevPrice > 0 ? ((priceChange / prevPrice) * 100).toFixed(2) : '0.00';
  const trend = priceChange >= 0 ? 'up' : 'down';

  const currentRsi = data.length > 0 && data[data.length - 1].rsi14 ? data[data.length - 1].rsi14!.toFixed(2) : '--';
  const currentMa50 = data.length > 0 && data[data.length - 1].ma50 ? data[data.length - 1].ma50!.toFixed(2) : '--';

  // Calculate wallet valuation: cash + value of open positions
  const positionsValue = positions.reduce((sum, p) => sum + Number(p.quantity) * currentPrice, 0);
  const totalEquity = wallet ? wallet.balance + positionsValue : INITIAL_BALANCE;
  const pnl = totalEquity - INITIAL_BALANCE;
  const pnlPercent = ((pnl / INITIAL_BALANCE) * 100).toFixed(2);
  const isProfit = pnl >= 0;
  const cashBalance = wallet ? wallet.balance : 0;
  const cashPercent = totalEquity > 0 ? ((cashBalance / totalEquity) * 100).toFixed(1) : '100';
  const cryptoPercent = totalEquity > 0 ? ((positionsValue / totalEquity) * 100).toFixed(1) : '0';

  // Profit/Loss trades metrics
  const closedTrades = trades.filter(t => t.profitLoss !== null);
  const winTrades = closedTrades.filter(t => (t.profitLoss || 0) > 0).length;
  const winRate = closedTrades.length > 0 ? ((winTrades / closedTrades.length) * 100).toFixed(1) : '0.0';

  const handleInspectSignal = (sig: TradingSignalItem) => {
    setSelectedSignal(sig);
    setActiveTab('detail');
  };

  const firstPos = positions.length > 0 ? positions[0] : null;
  const slPrice = firstPos?.stopLossPrice;
  const tpPrice = firstPos?.takeProfitPrice;
  const entryPrice = firstPos?.averagePrice;

  return (
    <div className="dashboard-container">
      {/* Left Sidebar: Key Market Metrics */}
      <div className="flex-col gap-24" style={{ minWidth: 0 }}>
        <div style={{ marginBottom: '20px', paddingLeft: '8px' }}>
          <h2 style={{ fontSize: '1.8rem', letterSpacing: '-0.5px', fontWeight: 800 }}>
            Trading<span className="gradient-text">Signal</span>
          </h2>
          <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '4px' }}>Nền tảng Giao dịch Thuật toán</p>
        </div>

        <StatCard
          title="Giá BTC / USDT"
          value={`$${currentPrice.toLocaleString()}`}
          subtitle={`${priceChange >= 0 ? '+' : ''}${priceChangePercent}% trong 1H`}
          trend={trend}
          colorAccent="var(--accent-blue)"
        />
        <StatCard
          title="Chỉ số RSI (14 chu kỳ)"
          value={currentRsi}
          subtitle={Number(currentRsi) > 70 ? 'Vùng Quá Mua' : Number(currentRsi) < 30 ? 'Vùng Quá Bán' : 'Vùng Trung Tính'}
          trend={Number(currentRsi) > 70 ? 'down' : Number(currentRsi) < 30 ? 'up' : 'neutral'}
          colorAccent="var(--accent-orange)"
        />
        <StatCard
          title="Đường MA (50 chu kỳ)"
          value={`$${currentMa50}`}
          colorAccent="var(--accent-green)"
        />
      </div>

      {/* Center Main Area: Chart + Large Tabbed Center Panel */}
      <div className="main-center-panel">
        {/* Main Candlestick Chart Area */}
        <div className="soft-card" style={{ padding: '0', overflow: 'hidden', display: 'flex', flexDirection: 'column', width: '100%', minWidth: 0, boxSizing: 'border-box' }}>
          <div className="flex-between" style={{ padding: '20px 24px 12px 24px' }}>
            <div>
              <h3 style={{ fontSize: '1.25rem', marginBottom: '4px' }}>Biểu đồ Thị trường</h3>
              <p className="text-muted" style={{ fontSize: '0.85rem' }}>Bitcoin / Tether US (BTCUSDT) • Nến 1 Giờ (1H)</p>
            </div>
            <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
              <span style={{ fontSize: '0.75rem', padding: '6px 12px', borderRadius: '8px', background: 'rgba(59,130,246,0.15)', color: 'var(--accent-blue)', fontWeight: 600, whiteSpace: 'nowrap', border: '1px solid rgba(59,130,246,0.3)', boxShadow: '0 0 10px rgba(59,130,246,0.1)' }}>
                Khung 1 Giờ
              </span>
              <span style={{ fontSize: '0.75rem', padding: '6px 12px', borderRadius: '8px', background: 'rgba(16,185,129,0.15)', color: 'var(--accent-green)', fontWeight: 600, whiteSpace: 'nowrap', border: '1px solid rgba(16,185,129,0.3)', boxShadow: '0 0 10px rgba(16,185,129,0.1)' }}>
                Binance Spot
              </span>
            </div>
          </div>

          <div style={{ flex: 1, padding: '16px', minWidth: 0, overflow: 'hidden' }}>
            {loading && <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>Đang tải dữ liệu thị trường...</div>}
            {error && <div style={{ padding: '60px', textAlign: 'center', color: 'var(--accent-red)' }}>{error}</div>}
            {!loading && !error && data.length === 0 && (
              <div style={{ padding: '60px', textAlign: 'center', color: 'var(--accent-orange)' }}>
                Chưa có dữ liệu nến cho khung thời gian này.
              </div>
            )}
            {!loading && !error && data.length > 0 && 
              <TradingChart 
                data={data} 
                slPrice={slPrice}
                tpPrice={tpPrice}
                entryPrice={entryPrice}
              />
            }
          </div>
        </div>

        {/* Large Central Tabbed Panel */}
        <div className="soft-card" style={{ padding: '24px', width: '100%', maxWidth: '100%', minWidth: 0, boxSizing: 'border-box', overflow: 'hidden' }}>
          {/* Tab Navigation Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', gap: '12px', flexWrap: 'wrap' }}>
            <div className="tab-group" style={{ margin: 0, maxWidth: '100%' }}>
              <button
                className={`tab-btn ${activeTab === 'trades' ? 'active' : ''}`}
                onClick={() => setActiveTab('trades')}
              >
                <HistoryIcon size={16} />
                <span>Lịch sử Giao dịch</span>
                {trades.length > 0 && <span className="tab-badge">{trades.length}</span>}
              </button>

              <button
                className={`tab-btn ${activeTab === 'patterns' ? 'active' : ''}`}
                onClick={() => setActiveTab('patterns')}
              >
                <TargetIcon size={16} />
                <span>Mẫu hình & Tín hiệu</span>
                {signals.length > 0 && <span className="tab-badge">{signals.length}</span>}
              </button>

              <button
                className={`tab-btn ${activeTab === 'detail' ? 'active' : ''}`}
                onClick={() => {
                  if (!selectedSignal && signals.length > 0) {
                    setSelectedSignal(signals[0]);
                  }
                  setActiveTab('detail');
                }}
              >
                <EyeIcon size={16} />
                <span>Chi tiết Mẫu hình</span>
                {selectedSignal && (
                  <span className="tab-badge" style={{ background: activeTab === 'detail' ? 'rgba(16,185,129,0.15)' : 'rgba(0,0,0,0.06)', color: activeTab === 'detail' ? 'var(--accent-green)' : 'inherit' }}>
                    #{selectedSignal.id}
                  </span>
                )}
              </button>

              <button
                className={`tab-btn ${activeTab === 'guide' ? 'active' : ''}`}
                onClick={() => setActiveTab('guide')}
              >
                <InfoIcon size={16} />
                <span>Hướng dẫn</span>
              </button>

              <button
                className={`tab-btn ${activeTab === 'cron' ? 'active' : ''}`}
                onClick={() => setActiveTab('cron')}
              >
                <ClockIcon size={16} />
                <span>Tiến trình Cron</span>
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: 'var(--accent-green)',
                    display: 'inline-block',
                    animation: 'pulse 2s infinite',
                  }}
                />
              </button>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', flexShrink: 0, marginLeft: 'auto' }}>
              <RefreshIcon size={14} />
              <span>Tự động cập nhật mỗi <b>10s</b></span>
            </div>
          </div>

          {/* Large Tab Contents */}
          <div style={{ minHeight: '380px', width: '100%', maxWidth: '100%', minWidth: 0, overflowX: 'auto' }}>
            {activeTab === 'trades' && <TradeHistory trades={trades} />}
            {activeTab === 'patterns' && (
              <PatternSignals
                signals={signals}
                onSelectSignal={handleInspectSignal}
              />
            )}
            {activeTab === 'detail' && (
              <PatternDetailView
                signal={selectedSignal}
                chartData={data}
                trades={trades}
                currentPrice={currentPrice}
                onBackToList={() => {
                  setSelectedSignal(null);
                  setActiveTab('patterns');
                }}
              />
            )}
            {activeTab === 'cron' && <CollectorStatus status={status} />}
            {activeTab === 'guide' && <GuideView />}
          </div>
        </div>
      </div>

      {/* Right Panel: Virtual Wallet & Performance Overview */}
      <div className="flex-col gap-24" style={{ minWidth: 0, width: '100%' }}>
        {/* Virtual Wallet Card */}
        <div className="soft-card" style={{ background: 'linear-gradient(145deg, var(--bg-color), rgba(59, 130, 246, 0.05))', padding: '22px' }}>
          {/* Card Top: Title & Initial Capital */}
          <div className="flex-between" style={{ marginBottom: '14px', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <WalletIcon size={20} color="var(--accent-blue)" />
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, whiteSpace: 'nowrap' }}>Ví Giao Dịch Ảo</h3>
            </div>
            <span style={{ fontSize: '0.75rem', background: 'rgba(59,130,246,0.15)', color: 'var(--accent-blue)', padding: '4px 12px', borderRadius: '12px', fontWeight: 600, whiteSpace: 'nowrap', flexShrink: 0, border: '1px solid rgba(59,130,246,0.3)', boxShadow: '0 0 10px rgba(59,130,246,0.1)' }}>
              Vốn gốc $10,000
            </span>
          </div>

          {/* MAIN HIGHLIGHT: Tổng Tài Sản Ròng & Trạng Thái Lời/Lỗ */}
          <div
            style={{
              background: isProfit ? 'rgba(16, 185, 129, 0.1)' : 'rgba(244, 63, 94, 0.1)',
              border: `1px solid ${isProfit ? 'rgba(16, 185, 129, 0.3)' : 'rgba(244, 63, 94, 0.3)'}`,
              borderRadius: '16px',
              padding: '20px',
              marginBottom: '20px',
              boxShadow: isProfit ? '0 0 20px rgba(16, 185, 129, 0.05)' : '0 0 20px rgba(244, 63, 94, 0.05)',
              position: 'relative',
              overflow: 'hidden'
            }}
          >
            <div className="flex-between" style={{ marginBottom: '6px', alignItems: 'center' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', whiteSpace: 'nowrap' }}>
                Tổng Tài Sản Ròng
              </span>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '0.75rem',
                  fontWeight: 800,
                  padding: '3px 8px',
                  borderRadius: '6px',
                  background: isProfit ? 'var(--accent-green)' : 'var(--accent-red)',
                  color: '#ffffff',
                  letterSpacing: '0.5px',
                  whiteSpace: 'nowrap',
                  flexShrink: 0,
                  boxShadow: isProfit ? '0 0 10px rgba(16, 185, 129, 0.4)' : '0 0 10px rgba(244, 63, 94, 0.4)',
                }}
              >
                {isProfit ? <TrendingUpIcon size={12} color="#ffffff" /> : <TrendingDownIcon size={12} color="#ffffff" />}
                {isProfit ? 'ĐANG LỜI' : 'ĐANG LỖ'}
              </span>
            </div>

            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--text-color)', letterSpacing: '-0.5px', marginBottom: '4px', whiteSpace: 'nowrap' }}>
              ${totalEquity.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.92rem', fontWeight: 700, color: isProfit ? 'var(--accent-green)' : 'var(--accent-red)' }}>
              <span style={{ whiteSpace: 'nowrap' }}>{isProfit ? '↗ +' : '↘ -'}${Math.abs(pnl).toFixed(2)} USDT ({isProfit ? '+' : ''}{pnlPercent}%)</span>
            </div>
          </div>

          {/* Cơ cấu danh mục: Tiền mặt vs Coin */}
          <div style={{ marginBottom: '16px' }}>
            <div className="flex-between" style={{ fontSize: '0.75rem', marginBottom: '6px', fontWeight: 600 }}>
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Cơ cấu danh mục</span>
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>
                Tiền mặt {cashPercent}% • Coin {cryptoPercent}%
              </span>
            </div>

            <div style={{ height: '7px', borderRadius: '6px', background: 'rgba(0,0,0,0.06)', display: 'flex', overflow: 'hidden', marginBottom: '10px' }}>
              <div style={{ width: `${cashPercent}%`, background: 'var(--accent-blue)', transition: 'width 0.4s ease' }} />
              <div style={{ width: `${cryptoPercent}%`, background: 'var(--accent-orange)', transition: 'width 0.4s ease' }} />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div className="soft-card-inner" style={{ padding: '10px 12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '2px' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-blue)', display: 'inline-block', flexShrink: 0 }} />
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>Tiền mặt (USDT)</span>
                </div>
                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>
                  ${wallet ? wallet.balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '---'}
                </div>
              </div>

              <div className="soft-card-inner" style={{ padding: '10px 12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '2px' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-orange)', display: 'inline-block', flexShrink: 0 }} />
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>Giá trị Coin (BTC)</span>
                </div>
                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--accent-orange)', whiteSpace: 'nowrap' }}>
                  ${positionsValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
              </div>
            </div>
          </div>

          {/* Danh sách tài sản đang nắm giữ */}
          <div style={{ borderTop: '1px solid rgba(0,0,0,0.08)', paddingTop: '14px' }}>
            <div className="flex-between" style={{ marginBottom: '10px', alignItems: 'center' }}>
              <span className="text-muted" style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', whiteSpace: 'nowrap' }}>
                Tài sản đang nắm giữ
              </span>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>
                {positions.length > 0 ? `${positions.length} loại coin` : '0 coin'}
              </span>
            </div>

            {positions.length > 0 ? (
              positions.map((p) => {
                const coinSymbol = p.instrument?.symbol ? p.instrument.symbol.replace('USDT', '') : 'BTC';
                const qty = Number(p.quantity);
                const entryPrice = Number(p.averagePrice);
                const posValue = qty * currentPrice;
                const costValue = qty * entryPrice;
                const posPnl = posValue - costValue;
                const posPnlPercent = costValue > 0 ? ((posPnl / costValue) * 100).toFixed(2) : '0.00';
                const isPosProfit = posPnl >= 0;

                return (
                  <div
                    key={p.id}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '10px',
                      background: 'rgba(255, 255, 255, 0.65)',
                      marginBottom: '10px',
                      border: `1px solid ${isPosProfit ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
                    }}
                  >
                    <div className="flex-between" style={{ marginBottom: '6px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <span
                          style={{
                            fontSize: '0.7rem',
                            fontWeight: 800,
                            background: 'rgba(255, 107, 53, 0.15)',
                            color: 'var(--accent-orange)',
                            padding: '2px 6px',
                            borderRadius: '4px',
                          }}
                        >
                          {coinSymbol}
                        </span>
                        <span style={{ fontWeight: 700, fontSize: '0.88rem' }}>Bitcoin</span>
                      </div>
                      <span style={{ fontWeight: 700, fontSize: '0.88rem', fontFamily: 'monospace' }}>
                        {qty.toFixed(6)} {coinSymbol}
                      </span>
                    </div>

                    <div className="flex-between" style={{ fontSize: '0.78rem', marginBottom: '8px', color: 'var(--text-muted)' }}>
                      <span>Giá trị thị trường:</span>
                      <span style={{ fontWeight: 700, color: 'var(--text-color)', fontFamily: 'monospace' }}>
                        ≈ ${posValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USDT
                      </span>
                    </div>

                    <div
                      style={{
                        background: 'rgba(0,0,0,0.03)',
                        padding: '6px 8px',
                        borderRadius: '6px',
                        fontSize: '0.72rem',
                        display: 'flex',
                        justifyContent: 'space-between',
                        marginBottom: '8px',
                      }}
                    >
                      <div>
                        <span className="text-muted">Giá mua TB: </span>
                        <span style={{ fontWeight: 600 }}>${entryPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}</span>
                      </div>
                      <div>
                        <span className="text-muted">Giá hiện tại: </span>
                        <span style={{ fontWeight: 600 }}>${currentPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}</span>
                      </div>
                    </div>

                    <div className="flex-between" style={{ fontSize: '0.78rem' }}>
                      <span className="text-muted">Lãi/Lỗ vị thế:</span>
                      <span
                        style={{
                          fontWeight: 700,
                          color: isPosProfit ? 'var(--accent-green)' : 'var(--accent-red)',
                          background: isPosProfit ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                          padding: '2px 8px',
                          borderRadius: '6px',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                        }}
                      >
                        {isPosProfit ? <TrendingUpIcon size={12} /> : <TrendingDownIcon size={12} />}
                        {isPosProfit ? '+' : ''}${posPnl.toFixed(2)} USDT ({isPosProfit ? '+' : ''}{posPnlPercent}%)
                      </span>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="soft-card-inner" style={{ textAlign: 'center', padding: '16px 12px', color: 'var(--text-muted)' }}>
                <div style={{ fontSize: '0.82rem', fontWeight: 600, marginBottom: '2px' }}>Đang giữ 100% Tiền mặt (USDT)</div>
                <div style={{ fontSize: '0.75rem' }}>Chưa có vị thế coin nào. Sẵn sàng mở vị thế khi có tín hiệu mua.</div>
              </div>
            )}
          </div>
        </div>

        {/* Strategy Performance Stats */}
        <div className="soft-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <BarChartIcon size={18} color="var(--accent-blue)" />
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, whiteSpace: 'nowrap' }}>Hiệu suất Chiến lược</h3>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '0.85rem' }}>
            <div className="flex-between">
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Tỷ lệ lệnh thắng</span>
              <span style={{ fontWeight: 700, color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>{winRate}%</span>
            </div>
            <div className="flex-between">
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Tổng số lệnh đã đóng</span>
              <span style={{ fontWeight: 600, whiteSpace: 'nowrap' }}>{closedTrades.length} Lệnh</span>
            </div>
            <div className="flex-between">
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Tổng mẫu hình đã quét</span>
              <span style={{ fontWeight: 600, whiteSpace: 'nowrap' }}>{signals.length} Tín hiệu</span>
            </div>
            <div className="flex-between">
              <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Phân bổ rủi ro</span>
              <span style={{ fontSize: '0.75rem', color: 'var(--accent-green)', fontWeight: 600, whiteSpace: 'nowrap' }}>20% Vốn / Lệnh</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
