import React from 'react';
import { TrendingUpIcon, TrendingDownIcon } from './Icons';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: 'up' | 'down' | 'neutral';
  colorAccent?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  trend,
  colorAccent = 'var(--accent-blue)',
}) => {
  return (
    <div className="soft-card">
      <div className="flex-col gap-16">
        <div className="flex-between" style={{ alignItems: 'center' }}>
          <span className="text-muted" style={{ fontWeight: 500, fontSize: '0.9rem', whiteSpace: 'nowrap' }}>{title}</span>
          {/* Decorative indicator circle */}
          <div
            style={{
              width: '12px',
              height: '12px',
              borderRadius: '50%',
              backgroundColor: colorAccent,
              boxShadow: `0 0 10px ${colorAccent}`,
              flexShrink: 0,
            }}
          />
        </div>

        <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-main)', whiteSpace: 'nowrap' }}>
          {value}
        </div>

        {subtitle && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.85rem',
              color: trend === 'up' ? 'var(--accent-green)' : trend === 'down' ? 'var(--accent-red)' : 'var(--text-muted)',
              whiteSpace: 'nowrap',
            }}
          >
            {trend === 'up' && <TrendingUpIcon size={14} color="var(--accent-green)" />}
            {trend === 'down' && <TrendingDownIcon size={14} color="var(--accent-red)" />}
            <span style={{ whiteSpace: 'nowrap' }}>{subtitle}</span>
          </div>
        )}
      </div>
    </div>
  );
};
