import React from 'react';
import { EyeIcon, TrendingUpIcon, TrendingDownIcon, TargetIcon } from './Icons';

export interface TradingSignalItem {
  id: number;
  signalType: 'BUY' | 'SELL' | 'HOLD';
  confidenceScore: number | null;
  ruleBasis: string | null;
  generatedAt: string;
  instrument?: {
    symbol: string;
  };
}

interface PatternSignalsProps {
  signals: TradingSignalItem[];
  onSelectSignal?: (signal: TradingSignalItem) => void;
}

export const PatternSignals: React.FC<PatternSignalsProps> = ({ signals, onSelectSignal }) => {
  if (signals.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 16px', color: 'var(--text-muted)' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '12px' }}>
          <TargetIcon size={40} color="var(--text-muted)" />
        </div>
        <p style={{ fontSize: '1rem', fontWeight: 600 }}>Chưa có mẫu hình kỹ thuật nào được phát hiện</p>
        <p style={{ fontSize: '0.85rem', marginTop: '6px', opacity: 0.8 }}>
          Hệ thống đang quét mẫu hình Golden Cross, Death Cross và RSI Reversal mỗi 10 giây.
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
            <th>Tín hiệu</th>
            <th>Tên Mẫu hình</th>
            <th style={{ textAlign: 'center' }}>Độ tin cậy</th>
            <th>Chỉ số lúc quét</th>
            <th>Căn cứ kích hoạt</th>
            <th style={{ textAlign: 'center' }}>Hành động</th>
          </tr>
        </thead>
        <tbody>
          {signals.map((sig) => {
            const isBuy = sig.signalType === 'BUY';
            let parsed: any = {};
            try {
              if (sig.ruleBasis) {
                parsed = JSON.parse(sig.ruleBasis);
              }
            } catch (e) {
              parsed = { reason: sig.ruleBasis };
            }

            const patternName = parsed.pattern || (isBuy ? 'Mẫu hình Đảo chiều Tăng' : 'Mẫu hình Đảo chiều Giảm');
            
            let reason = parsed.reason || '';
            if (reason.includes('cắt lên') || reason.includes('vượt lên') || reason.includes('crossed above')) {
              reason = 'MA5 cắt lên MA20 báo hiệu xu hướng tăng';
            } else if (reason.includes('cắt xuống') || reason.includes('crossed below')) {
              reason = 'MA5 cắt xuống MA20 báo hiệu xu hướng giảm';
            } else if (reason.includes('phục hồi') || reason.includes('quá bán') || reason.includes('rebounded')) {
              reason = 'RSI phục hồi từ vùng quá bán (< 30)';
            } else if (reason.includes('quay đầu') || reason.includes('quá mua') || reason.includes('dropped below')) {
              reason = 'RSI quay đầu giảm từ vùng quá mua (> 70)';
            }

            const time = new Date(sig.generatedAt);
            const timeStr = time.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
              + ' ' + time.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

            return (
              <tr
                key={sig.id}
                style={{ cursor: 'pointer' }}
                onClick={() => onSelectSignal?.(sig)}
              >
                <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                  {timeStr}
                </td>
                <td style={{ fontWeight: 600, fontSize: '0.85rem' }}>
                  {sig.instrument?.symbol || 'BTCUSDT'}
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
                <td style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-color)', whiteSpace: 'nowrap' }}>
                  {patternName}
                </td>
                <td style={{ textAlign: 'center' }}>
                  {sig.confidenceScore ? (
                    <span
                      style={{
                        display: 'inline-block',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        color: 'var(--accent-blue)',
                        background: 'rgba(59, 130, 246, 0.12)',
                        padding: '2px 8px',
                        borderRadius: '12px',
                      }}
                    >
                      {Number(sig.confidenceScore).toFixed(0)}%
                    </span>
                  ) : '--'}
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'nowrap', alignItems: 'center' }}>
                    {parsed.rsi !== undefined && parsed.rsi !== null && (
                      <span style={{ fontSize: '0.75rem', background: 'rgba(0,0,0,0.05)', padding: '2px 6px', borderRadius: '4px', whiteSpace: 'nowrap' }}>
                        RSI: <b>{Number(parsed.rsi).toFixed(1)}</b>
                      </span>
                    )}
                    {parsed.ma5 !== undefined && parsed.ma5 !== null && (
                      <span style={{ fontSize: '0.75rem', background: 'rgba(0,0,0,0.05)', padding: '2px 6px', borderRadius: '4px', whiteSpace: 'nowrap' }}>
                        MA5: <b>${Number(parsed.ma5).toLocaleString(undefined, { maximumFractionDigits: 0 })}</b>
                      </span>
                    )}
                  </div>
                </td>
                <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)', maxWidth: '220px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {reason}
                </td>
                <td style={{ textAlign: 'center' }}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectSignal?.(sig);
                    }}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      background: 'rgba(59, 130, 246, 0.1)',
                      color: 'var(--accent-blue)',
                      border: 'none',
                      borderRadius: '6px',
                      padding: '4px 10px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    <EyeIcon size={14} />
                    <span>Chi tiết</span>
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
