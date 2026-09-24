import React from 'react';
import { DatabaseIcon, TargetIcon, WalletIcon, ArrowRightIcon, ActivityIcon, CheckCircleIcon } from './Icons';

export const GuideView: React.FC = () => {
  return (
    <div style={{ padding: '8px', color: 'var(--text-color)' }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.2rem', marginBottom: '8px' }}>Hướng Dẫn Sử Dụng & Nguyên Lý Hoạt Động</h2>
        <p className="text-muted" style={{ fontSize: '0.9rem', lineHeight: '1.5' }}>
          Hệ thống giao dịch thuật toán tự động theo dõi, phân tích kỹ thuật và mô phỏng giao dịch 24/7. Dưới đây là các tính năng hệ thống hỗ trợ và luồng hoạt động chính.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginBottom: '32px' }}>
        <div className="soft-card-inner" style={{ padding: '16px' }}>
          <h3 style={{ fontSize: '1.05rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircleIcon size={18} color="var(--accent-green)" /> Các Tính Năng Hệ Thống Hỗ Trợ
          </h3>
          <ul style={{ paddingLeft: '24px', margin: 0, fontSize: '0.88rem', lineHeight: '1.8', color: 'var(--text-color)' }}>
            <li><b>Thu thập Dữ liệu (Real-time):</b> Tự động lấy nến 1H từ sàn giao dịch Binance Spot.</li>
            <li><b>Phân tích Mẫu hình & Tín hiệu:</b> Tự động nhận diện các mô hình Đảo chiều (M-Top, W-Bottom, Vai Đầu Vai) và xu hướng giá.</li>
            <li><b>Chỉ báo Kỹ thuật Hỗ trợ:</b> Tích hợp đường trung bình MA50 và RSI(14) để lọc tín hiệu nhiễu.</li>
            <li><b>Giao dịch Tự động:</b> Mô phỏng ví tiền $10,000 (Paper Trading), tự động tính toán khối lượng lệnh và chốt lời/cắt lỗ theo tỷ lệ RR (Risk/Reward).</li>
          </ul>
        </div>
      </div>

      <h3 style={{ fontSize: '1.05rem', marginBottom: '16px' }}>Luồng Hoạt Động (Workflow)</h3>
      <div style={{ 
        display: 'flex', 
        flexDirection: 'row', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        background: 'rgba(0,0,0,0.02)',
        padding: '24px',
        borderRadius: '12px',
        border: '1px dashed rgba(0,0,0,0.1)',
        gap: '12px',
        overflowX: 'auto'
      }}>
        
        {/* Step 1 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', flex: 1, minWidth: '120px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(59,130,246,0.1)', color: 'var(--accent-blue)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '12px' }}>
            <DatabaseIcon size={24} />
          </div>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '4px' }}>1. Thu Thập</h4>
          <p className="text-muted" style={{ fontSize: '0.75rem', margin: 0 }}>Cron Job chạy mỗi phút để lấy dữ liệu nến mới nhất.</p>
        </div>

        <ArrowRightIcon size={20} color="var(--text-muted)" />

        {/* Step 2 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', flex: 1, minWidth: '120px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(245,158,11,0.1)', color: 'var(--accent-orange)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '12px' }}>
            <ActivityIcon size={24} />
          </div>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '4px' }}>2. Phân Tích</h4>
          <p className="text-muted" style={{ fontSize: '0.75rem', margin: 0 }}>Tính toán MA, RSI và tìm kiếm các Mẫu Hình Đảo Chiều.</p>
        </div>

        <ArrowRightIcon size={20} color="var(--text-muted)" />

        {/* Step 3 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', flex: 1, minWidth: '120px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(139,92,246,0.1)', color: '#8b5cf6', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '12px' }}>
            <TargetIcon size={24} />
          </div>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '4px' }}>3. Tín Hiệu</h4>
          <p className="text-muted" style={{ fontSize: '0.75rem', margin: 0 }}>Tạo tín hiệu Giao Dịch nếu điều kiện xu hướng được thỏa mãn.</p>
        </div>

        <ArrowRightIcon size={20} color="var(--text-muted)" />

        {/* Step 4 */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', flex: 1, minWidth: '120px' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '50%', background: 'rgba(16,185,129,0.1)', color: 'var(--accent-green)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '12px' }}>
            <WalletIcon size={24} />
          </div>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '4px' }}>4. Khớp Lệnh</h4>
          <p className="text-muted" style={{ fontSize: '0.75rem', margin: 0 }}>Khớp lệnh ảo, tự động cập nhật số dư USDT và Coin.</p>
        </div>

      </div>
    </div>
  );
};
