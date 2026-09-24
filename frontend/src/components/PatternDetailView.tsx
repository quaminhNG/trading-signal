import React from 'react';
import type { TradingSignalItem } from './PatternSignals';
import type { ChartDataDto } from './TradingChart';
import type { TradeLog } from './TradeHistory';
import {
  TrendingUpIcon,
  TrendingDownIcon,
  ActivityIcon,
  LayersIcon,
  TargetIcon,
  InfoIcon,
  ArrowRightIcon,
} from './Icons';

interface PatternDetailViewProps {
  signal: TradingSignalItem | null;
  chartData: ChartDataDto[];
  trades: TradeLog[];
  currentPrice: number;
  onBackToList: () => void;
}

export const PatternDetailView: React.FC<PatternDetailViewProps> = ({
  signal,
  chartData,
  trades,
  currentPrice,
  onBackToList,
}) => {
  if (!signal) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 16px', color: 'var(--text-muted)' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
          <TargetIcon size={44} color="var(--accent-blue)" />
        </div>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '8px', color: 'var(--text-color)' }}>
          Chưa chọn Mẫu hình nào
        </h3>
        <p style={{ fontSize: '0.85rem', maxWidth: '420px', margin: '0 auto 20px auto', opacity: 0.8, lineHeight: 1.5 }}>
          Vui lòng bấm vào danh sách mẫu hình từ tab <b>Mẫu hình & Tín hiệu</b> để xem phân tích chi tiết bối cảnh giá, chỉ số kỹ thuật và kết quả khớp lệnh tại đây.
        </p>
        <button
          onClick={onBackToList}
          className="tab-btn active"
          style={{ padding: '8px 20px', display: 'inline-flex', cursor: 'pointer', border: '1px solid rgba(0,0,0,0.08)' }}
        >
          <span>Xem Danh sách Mẫu hình</span>
          <ArrowRightIcon size={14} />
        </button>
      </div>
    );
  }

  const isBuy = signal.signalType === 'BUY';
  let parsed: any = {};
  try {
    if (signal.ruleBasis) {
      parsed = JSON.parse(signal.ruleBasis);
    }
  } catch (e) {
    parsed = { reason: signal.ruleBasis };
  }

  const patternName = parsed.pattern || (isBuy ? 'Mẫu hình Đảo chiều Tăng' : 'Mẫu hình Đảo chiều Giảm');
  
  let reason = parsed.reason || 'Điều kiện chỉ số kỹ thuật được kích hoạt theo thuật toán định lượng.';
  if (reason.includes('cắt lên') || reason.includes('vượt lên') || reason.includes('crossed above')) {
    reason = 'Đường MA ngắn hạn (MA5) cắt lên trên MA trung hạn (MA20), báo hiệu sự xuất hiện của xu hướng tăng giá mạnh mẽ.';
  } else if (reason.includes('cắt xuống') || reason.includes('crossed below')) {
    reason = 'Đường MA ngắn hạn (MA5) cắt xuống dưới MA trung hạn (MA20), báo hiệu áp lực giảm giá và đà phân phối của thị trường.';
  } else if (reason.includes('phục hồi') || reason.includes('quá bán') || reason.includes('rebounded')) {
    reason = 'Chỉ số RSI bật tăng trở lại từ ngưỡng quá bán (< 30), xác nhận lực bắt đáy mạnh và rủi ro điều chỉnh giảm đã cạn kiệt.';
  } else if (reason.includes('quay đầu') || reason.includes('quá mua') || reason.includes('dropped below')) {
    reason = 'Chỉ số RSI quay đầu giảm từ ngưỡng quá mua (> 70), báo hiệu bên mua suy yếu và rủi ro chốt lời trên diện rộng.';
  }

  const sigTime = new Date(signal.generatedAt);
  const timeStr = sigTime.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
    + ' ' + sigTime.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  // Find matching candle in chartData (closest by time)
  const sigTimestamp = sigTime.getTime();
  const candle = chartData.reduce((closest, curr) => {
    const currTime = new Date(curr.time).getTime();
    if (!closest) return curr;
    const closestDiff = Math.abs(new Date(closest.time).getTime() - sigTimestamp);
    const currDiff = Math.abs(currTime - sigTimestamp);
    return currDiff < closestDiff ? curr : closest;
  }, chartData[0] as ChartDataDto | undefined);

  // Find matching trade in trades list (closest created_at)
  const matchingTrade = trades.reduce((closest, curr) => {
    const tTime = new Date(curr.createdAt).getTime();
    if (!closest) return curr;
    const closestDiff = Math.abs(new Date(closest.createdAt).getTime() - sigTimestamp);
    const currDiff = Math.abs(tTime - sigTimestamp);
    return currDiff < closestDiff && currDiff < 600000 ? curr : closest; // within 10 min
  }, undefined as TradeLog | undefined);

  // Price context
  const open = candle ? candle.open : parsed.openPrice || 0;
  const high = candle ? candle.high : parsed.highPrice || 0;
  const low = candle ? candle.low : parsed.lowPrice || 0;
  const close = candle ? candle.close : parsed.closePrice || 0;
  const volume = candle ? candle.volume : parsed.volume || 0;
  const candleDiff = close - open;
  const candleDiffPercent = open > 0 ? ((candleDiff / open) * 100).toFixed(2) : '0.00';

  // Technical indicators
  const rsi = parsed.rsi !== undefined && parsed.rsi !== null ? Number(parsed.rsi) : candle?.rsi14 || null;
  const ma5 = parsed.ma5 !== undefined && parsed.ma5 !== null ? Number(parsed.ma5) : candle?.ma5 || null;
  const ma20 = parsed.ma20 !== undefined && parsed.ma20 !== null ? Number(parsed.ma20) : candle?.ma20 || null;
  const maSpread = ma5 && ma20 ? ma5 - ma20 : null;

  // Subsequent trajectory (check 3 candles ahead if available)
  const candleIndex = candle ? chartData.findIndex(c => c.time === candle.time) : -1;
  const nextCandles = candleIndex >= 0 && candleIndex < chartData.length - 1
    ? chartData.slice(candleIndex + 1, Math.min(candleIndex + 4, chartData.length))
    : [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Navigation Header */}
      <div style={{ borderBottom: '1px solid rgba(0,0,0,0.06)', paddingBottom: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div className="flex-between" style={{ alignItems: 'center' }}>
          <button
            onClick={onBackToList}
            style={{
              background: 'rgba(0,0,0,0.05)',
              border: 'none',
              borderRadius: '8px',
              padding: '6px 14px',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
              color: 'var(--text-color)',
              whiteSpace: 'nowrap',
              flexShrink: 0,
            }}
          >
            <span>← Quay lại danh sách</span>
          </button>

          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', flexShrink: 0 }}>
            {signal.instrument?.symbol || 'BTCUSDT'} • Phát hiện lúc: <b>{timeStr}</b>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontWeight: 800,
              fontSize: '0.8rem',
              color: isBuy ? 'var(--accent-green)' : 'var(--accent-red)',
              background: isBuy ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
              padding: '4px 12px',
              borderRadius: '6px',
              letterSpacing: '0.5px',
              whiteSpace: 'nowrap',
              flexShrink: 0,
            }}
          >
            {isBuy ? <TrendingUpIcon size={14} /> : <TrendingDownIcon size={14} />}
            TÍN HIỆU {isBuy ? 'MUA' : 'BÁN'}
          </span>

          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, whiteSpace: 'nowrap' }}>
            {patternName}
          </h3>

          {signal.confidenceScore && (
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                background: 'rgba(59, 130, 246, 0.12)',
                color: 'var(--accent-blue)',
                padding: '3px 10px',
                borderRadius: '12px',
                whiteSpace: 'nowrap',
                flexShrink: 0,
              }}
            >
              Độ tin cậy: {Number(signal.confidenceScore).toFixed(0)}%
            </span>
          )}
        </div>
      </div>

      {/* Section 1: Bối cảnh Thị trường & Cây nến tại thời điểm Kích hoạt */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <ActivityIcon size={18} color="var(--accent-blue)" />
          <h4 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, whiteSpace: 'nowrap' }}>Bối cảnh Thị trường & Nến Kích hoạt</h4>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(115px, 1fr))', gap: '10px' }}>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Giá Mở (Open)</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>${open.toLocaleString()}</span>
          </div>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Cao nhất (High)</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>${high.toLocaleString()}</span>
          </div>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Thấp nhất (Low)</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>${low.toLocaleString()}</span>
          </div>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Giá Đóng (Close)</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', fontFamily: 'monospace', color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>${close.toLocaleString()}</span>
          </div>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Biến động nến</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', color: candleDiff >= 0 ? 'var(--accent-green)' : 'var(--accent-red)', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
              {candleDiff >= 0 ? '+' : ''}{candleDiffPercent}%
            </span>
          </div>
          <div className="soft-card-inner" style={{ padding: '12px 14px' }}>
            <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '2px', whiteSpace: 'nowrap' }}>Khối lượng (Volume)</span>
            <span style={{ fontWeight: 700, fontSize: '0.95rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>{Number(volume).toFixed(2)} BTC</span>
          </div>
        </div>
      </div>

      {/* Section 2: Phân tích Chỉ số Kỹ thuật */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <LayersIcon size={18} color="var(--accent-orange)" />
          <h4 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, whiteSpace: 'nowrap' }}>Chỉ số Kỹ thuật tại Thời điểm Kích hoạt</h4>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', marginBottom: '14px' }}>
          {/* RSI Card */}
          <div className="soft-card-inner" style={{ padding: '16px' }}>
            <div className="flex-between" style={{ marginBottom: '8px' }}>
              <span style={{ fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap' }}>Chỉ số Sức mạnh Tương đối (RSI 14)</span>
              <span style={{ fontWeight: 700, color: rsi && rsi > 70 ? 'var(--accent-red)' : rsi && rsi < 30 ? 'var(--accent-green)' : 'var(--accent-blue)', whiteSpace: 'nowrap' }}>
                {rsi !== null ? rsi.toFixed(2) : '--'}
              </span>
            </div>
            <div style={{ background: 'rgba(0,0,0,0.06)', borderRadius: '6px', height: '8px', overflow: 'hidden', position: 'relative' }}>
              {rsi !== null && (
                <div
                  style={{
                    position: 'absolute',
                    left: 0,
                    top: 0,
                    bottom: 0,
                    width: `${Math.min(100, Math.max(0, rsi))}%`,
                    background: rsi > 70 ? 'var(--accent-red)' : rsi < 30 ? 'var(--accent-green)' : 'var(--accent-blue)',
                    borderRadius: '6px',
                  }}
                />
              )}
            </div>
            <div className="flex-between" style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '4px', whiteSpace: 'nowrap' }}>
              <span>Quá bán (&lt;30)</span>
              <span>Vùng Trung tính (50)</span>
              <span>Quá mua (&gt;70)</span>
            </div>
          </div>

          {/* Moving Averages Card */}
          <div className="soft-card-inner" style={{ padding: '16px' }}>
            <div className="flex-between" style={{ marginBottom: '6px' }}>
              <span style={{ fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap' }}>Đường Trung Bình Động (MA Cross)</span>
              {maSpread !== null && (
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: maSpread >= 0 ? 'var(--accent-green)' : 'var(--accent-red)', whiteSpace: 'nowrap' }}>
                  {maSpread >= 0 ? 'MA5 > MA20' : 'MA5 < MA20'}
                </span>
              )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.8rem' }}>
              <div className="flex-between">
                <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Đường MA ngắn (MA5):</span>
                <span style={{ fontWeight: 600, fontFamily: 'monospace', whiteSpace: 'nowrap' }}>${ma5 !== null ? ma5.toLocaleString(undefined, { maximumFractionDigits: 2 }) : '--'}</span>
              </div>
              <div className="flex-between">
                <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Đường MA trung (MA20):</span>
                <span style={{ fontWeight: 600, fontFamily: 'monospace', whiteSpace: 'nowrap' }}>${ma20 !== null ? ma20.toLocaleString(undefined, { maximumFractionDigits: 2 }) : '--'}</span>
              </div>
              {maSpread !== null && (
                <div className="flex-between" style={{ borderTop: '1px solid rgba(0,0,0,0.05)', paddingTop: '4px' }}>
                  <span className="text-muted" style={{ whiteSpace: 'nowrap' }}>Độ lệch giao cắt (Spread):</span>
                  <span style={{ fontWeight: 700, fontFamily: 'monospace', color: maSpread >= 0 ? 'var(--accent-green)' : 'var(--accent-red)', whiteSpace: 'nowrap' }}>
                    {maSpread >= 0 ? '+' : ''}${maSpread.toFixed(2)}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Trigger Explanation Card */}
        <div className="soft-card-inner" style={{ padding: '14px 16px', background: 'rgba(59, 130, 246, 0.05)', border: '1px solid rgba(59, 130, 246, 0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
            <InfoIcon size={16} color="var(--accent-blue)" />
            <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>Căn cứ Thuật toán Định lượng</span>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-color)', margin: 0, lineHeight: 1.5 }}>
            {reason}
          </p>
        </div>
      </div>

      {/* Section 3: Diễn biến Khớp lệnh & Kết quả Giao dịch Ảo */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <TargetIcon size={18} color="var(--accent-green)" />
          <h4 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, whiteSpace: 'nowrap' }}>Diễn biến Khớp lệnh & Kết quả Giao dịch Ảo</h4>
        </div>

        {matchingTrade ? (
          <div className="soft-card-inner" style={{ padding: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px', marginBottom: '12px' }}>
              <div>
                <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', whiteSpace: 'nowrap' }}>Lệnh Khớp</span>
                <span style={{ fontWeight: 700, fontSize: '0.9rem', color: matchingTrade.type === 'BUY' ? 'var(--accent-green)' : 'var(--accent-red)', whiteSpace: 'nowrap' }}>
                  LỆNH {matchingTrade.type === 'BUY' ? 'MUA' : 'BÁN'} ĐÃ KHỚP
                </span>
              </div>
              <div>
                <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', whiteSpace: 'nowrap' }}>Giá Khớp Lệnh</span>
                <span style={{ fontWeight: 700, fontSize: '0.9rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
                  ${Number(matchingTrade.price).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
              <div>
                <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', whiteSpace: 'nowrap' }}>Khối Lượng Vị Thế</span>
                <span style={{ fontWeight: 700, fontSize: '0.9rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
                  {Number(matchingTrade.quantity).toFixed(6)} BTC
                </span>
              </div>
              <div>
                <span className="text-muted" style={{ fontSize: '0.75rem', display: 'block', whiteSpace: 'nowrap' }}>Vốn Phân Bổ (20% NAV)</span>
                <span style={{ fontWeight: 700, fontSize: '0.9rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
                  ${(Number(matchingTrade.quantity) * Number(matchingTrade.price)).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} USDT
                </span>
              </div>
            </div>

            <div style={{ borderTop: '1px solid rgba(0,0,0,0.06)', paddingTop: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
              <div>
                <span className="text-muted" style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>Kết quả lệnh: </span>
                {matchingTrade.profitLoss !== null ? (
                  <span style={{ fontWeight: 700, fontSize: '0.85rem', color: matchingTrade.profitLoss >= 0 ? 'var(--accent-green)' : 'var(--accent-red)', whiteSpace: 'nowrap' }}>
                    Đã đóng vị thế • Lời/Lỗ thực tế: {matchingTrade.profitLoss >= 0 ? '+' : ''}{Number(matchingTrade.profitLoss).toFixed(2)} USDT
                  </span>
                ) : (
                  <span style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--accent-blue)', whiteSpace: 'nowrap' }}>
                    Đã mở vị thế • Giá thị trường hiện tại: ${currentPrice.toLocaleString()}
                  </span>
                )}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', flexShrink: 0 }}>
                Mã lệnh: #{matchingTrade.id}
              </div>
            </div>
          </div>
        ) : (
          <div className="soft-card-inner" style={{ padding: '14px 16px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Tín hiệu đã được ghi nhận vào nhật ký kiểm toán. Lệnh được phân bổ tự động theo tỷ lệ 20% vốn khả dụng.
          </div>
        )}
      </div>

      {/* Section 4: Diễn biến Giá tiếp theo (Phản ứng sau Tín hiệu) */}
      {nextCandles.length > 0 && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
            <TrendingUpIcon size={18} color="var(--accent-blue)" />
            <h4 style={{ fontSize: '0.95rem', fontWeight: 700, margin: 0, whiteSpace: 'nowrap' }}>Diễn biến Giá tiếp theo (Phản ứng sau Tín hiệu)</h4>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${nextCandles.length}, 1fr)`, gap: '10px' }}>
            {nextCandles.map((nc, idx) => {
              const diff = nc.close - close;
              const diffPct = close > 0 ? ((diff / close) * 100).toFixed(2) : '0.00';
              const isGain = diff >= 0;

              return (
                <div key={idx} className="soft-card-inner" style={{ padding: '10px 12px', textAlign: 'center' }}>
                  <div className="text-muted" style={{ fontSize: '0.75rem', marginBottom: '2px', whiteSpace: 'nowrap' }}>
                    +{idx + 1} Giờ sau
                  </div>
                  <div style={{ fontWeight: 700, fontSize: '0.85rem', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
                    ${nc.close.toLocaleString()}
                  </div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: isGain ? 'var(--accent-green)' : 'var(--accent-red)', whiteSpace: 'nowrap' }}>
                    {isGain ? '+' : ''}{diffPct}%
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
