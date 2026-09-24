import React from 'react';
import { ActivityIcon, DatabaseIcon, LayersIcon, TargetIcon, HistoryIcon, RefreshIcon } from './Icons';

export interface StatusData {
  lastCollectedAt: string;
  totalCandles: number;
  totalSignals: number;
  totalTrades: number;
  collectorCron: string;
  instrumentsActive: number;
}

export const CollectorStatus: React.FC<{ status: StatusData | null }> = ({ status }) => {
  if (!status) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 16px', color: 'var(--text-muted)' }}>
        <p style={{ fontSize: '0.9rem' }}>Đang tải trạng thái tiến trình hệ thống...</p>
      </div>
    );
  }

  const isNever = status.lastCollectedAt === 'Never';
  const lastTime = isNever ? null : new Date(status.lastCollectedAt);
  const secondsAgo = lastTime ? Math.floor((Date.now() - lastTime.getTime()) / 1000) : null;
  const isActive = secondsAgo !== null && secondsAgo < 30;

  const formatAgo = (s: number) => {
    if (s < 60) return `${s} giây trước`;
    if (s < 3600) return `${Math.floor(s / 60)} phút trước`;
    return `${Math.floor(s / 3600)} giờ trước`;
  };

  return (
    <div style={{ padding: '8px 0', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Top Banner */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '16px 20px',
          borderRadius: '12px',
          background: isActive ? 'rgba(16, 185, 129, 0.08)' : 'rgba(255, 107, 53, 0.08)',
          border: `1px solid ${isActive ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 107, 53, 0.25)'}`,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <span
            style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              background: isActive ? 'var(--accent-green)' : 'var(--accent-orange)',
              boxShadow: isActive ? '0 0 10px var(--accent-green)' : 'none',
              animation: isActive ? 'pulse 2s infinite' : 'none',
            }}
          />
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem', color: isActive ? 'var(--accent-green)' : 'var(--accent-orange)' }}>
              {isActive ? 'Bộ thu thập đang Hoạt động (Active)' : 'Bộ thu thập Tạm dừng (Idle)'}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '2px' }}>
              Nguồn Binance REST API • Lần đồng bộ gần nhất: <b>{secondsAgo !== null ? formatAgo(secondsAgo) : 'Chưa có'}</b>
            </div>
          </div>
        </div>

        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Chu kỳ Cron
          </div>
          <code
            style={{
              fontSize: '0.85rem',
              fontWeight: 600,
              background: 'rgba(0, 0, 0, 0.06)',
              padding: '3px 8px',
              borderRadius: '6px',
            }}
          >
            {status.collectorCron}
          </code>
        </div>
      </div>

      {/* 4 Large KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px' }}>
        <div className="soft-card-inner" style={{ padding: '16px', textAlign: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '6px' }}>
            <DatabaseIcon size={20} color="var(--accent-blue)" />
          </div>
          <div className="text-muted" style={{ fontSize: '0.8rem', marginBottom: '4px' }}>Tổng số Nến (Candles)</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--accent-blue)' }}>
            {status.totalCandles.toLocaleString()}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>Khung 1H (30 ngày gần nhất)</div>
        </div>

        <div className="soft-card-inner" style={{ padding: '16px', textAlign: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '6px' }}>
            <TargetIcon size={20} color="var(--accent-orange)" />
          </div>
          <div className="text-muted" style={{ fontSize: '0.8rem', marginBottom: '4px' }}>Mẫu hình / Tín hiệu</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--accent-orange)' }}>
            {status.totalSignals}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>Golden/Death Cross & RSI</div>
        </div>

        <div className="soft-card-inner" style={{ padding: '16px', textAlign: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '6px' }}>
            <HistoryIcon size={20} color="var(--accent-green)" />
          </div>
          <div className="text-muted" style={{ fontSize: '0.8rem', marginBottom: '4px' }}>Lệnh Paper Trading</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--accent-green)' }}>
            {status.totalTrades}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>Khớp tự động theo Signal</div>
        </div>

        <div className="soft-card-inner" style={{ padding: '16px', textAlign: 'center' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '6px' }}>
            <LayersIcon size={20} color="var(--text-color)" />
          </div>
          <div className="text-muted" style={{ fontSize: '0.8rem', marginBottom: '4px' }}>Cặp tiền theo dõi</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>
            {status.instrumentsActive}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>BTCUSDT (Binance Spot)</div>
        </div>
      </div>

      {/* Info Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '16px',
          marginTop: '4px',
        }}
      >
        <div className="soft-card-inner" style={{ padding: '14px 16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
            <RefreshIcon size={16} color="var(--accent-blue)" />
            <span>Chu trình Thu thập & Phân tích</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
            Mỗi 10 giây, Data Collector truy vấn 720 nến gần nhất từ Binance, lọc nến trùng và cập nhật các chỉ báo RSI, MA5, MA20, MACD.
          </p>
        </div>

        <div className="soft-card-inner" style={{ padding: '14px 16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600, fontSize: '0.85rem', marginBottom: '6px' }}>
            <ActivityIcon size={16} color="var(--accent-green)" />
            <span>Cơ chế Tự động Khớp lệnh Ảo</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
            Khi phát hiện điểm cắt MA hoặc RSI đảo chiều, SignalEngine kích hoạt sự kiện để PaperTradingService thực thi lệnh mua/bán với vốn ảo.
          </p>
        </div>
      </div>
    </div>
  );
};
