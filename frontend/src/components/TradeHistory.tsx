import React from 'react';
import { TrendingUpIcon, TrendingDownIcon, HistoryIcon, CheckCircleIcon } from './Icons';

export interface TradeLog {
  id: number;
  type: string;
  quantity: number;
  price: number;
  profitLoss: number | null;
  createdAt: string;
  instrument?: {
    symbol: string;
  };
}

export const TradeHistory: React.FC<{ trades: TradeLog[] }> = ({ trades }) => {
  if (trades.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 16px', color: 'var(--text-muted)' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '12px' }}>
          <HistoryIcon size={40} color="var(--text-muted)" />
        </div>
        <p style={{ fontSize: '1rem', fontWeight: 600 }}>Chưa có giao dịch nào được thực hiện</p>
        <p style={{ fontSize: '0.85rem', marginTop: '6px', opacity: 0.8 }}>
          Khi các mẫu hình (Golden Cross, RSI Reversal...) xuất hiện, hệ thống Paper Trading sẽ tự động khớp lệnh tại đây.
        </p>
      </div>
    );
  }

  return (
    <div className="data-table-container" style={{ maxHeight: '480px', overflowY: 'auto' }}>
      <table className="data-table">
        <thead>
          <tr>
            <th>Thời gian</th>
            <th>Cặp tiền</th>
            <th>Loại lệnh</th>
            <th style={{ textAlign: 'right' }}>Khối lượng</th>
            <th style={{ textAlign: 'right' }}>Giá khớp</th>
            <th style={{ textAlign: 'right' }}>Tổng giá trị</th>
            <th style={{ textAlign: 'right' }}>Lời / Lỗ ròng</th>
            <th style={{ textAlign: 'center' }}>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {trades.map((trade) => {
            const isBuy = trade.type === 'BUY';
            const time = new Date(trade.createdAt);
            const timeStr = time.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
              + ' ' + time.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
            const totalValue = Number(trade.quantity) * Number(trade.price);

            return (
              <tr key={trade.id}>
                <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                  {timeStr}
                </td>
                <td style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                  {trade.instrument?.symbol || 'BTCUSDT'}
                </td>
                <td>
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      fontWeight: 700,
                      fontSize: '0.75rem',
                      color: isBuy ? 'var(--accent-green)' : 'var(--accent-red)',
                      background: isBuy ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      letterSpacing: '0.5px',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {isBuy ? <TrendingUpIcon size={12} /> : <TrendingDownIcon size={12} />}
                    {isBuy ? 'MUA' : 'BÁN'}
                  </span>
                </td>
                <td style={{ textAlign: 'right', fontWeight: 500, fontFamily: 'monospace' }}>
                  {Number(trade.quantity).toFixed(6)}
                </td>
                <td style={{ textAlign: 'right', fontWeight: 600, fontFamily: 'monospace' }}>
                  ${Number(trade.price).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </td>
                <td style={{ textAlign: 'right', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                  ${totalValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </td>
                <td style={{ textAlign: 'right' }}>
                  {trade.profitLoss !== null ? (
                    <span
                      style={{
                        fontWeight: 700,
                        fontSize: '0.85rem',
                        color: trade.profitLoss >= 0 ? 'var(--accent-green)' : 'var(--accent-red)',
                        fontFamily: 'monospace',
                      }}
                    >
                      {trade.profitLoss >= 0 ? '+' : ''}${Number(trade.profitLoss).toFixed(2)}
                    </span>
                  ) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>--</span>
                  )}
                </td>
                <td style={{ textAlign: 'center' }}>
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      color: 'var(--accent-green)',
                      background: 'rgba(16, 185, 129, 0.1)',
                      padding: '2px 8px',
                      borderRadius: '10px',
                    }}
                  >
                    <CheckCircleIcon size={12} />
                    Đã khớp
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
