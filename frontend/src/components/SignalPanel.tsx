import React from 'react';

export const SignalPanel: React.FC = () => {
    return (
        <div className="dark-panel flex-col gap-24">
            <h3 style={{ fontSize: '1.2rem', marginBottom: '8px' }}>Latest Signals</h3>
            
            {/* Mock Data for now */}
            <div className="flex-between" style={{ paddingBottom: '16px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
                <div className="flex-col">
                    <span style={{ fontWeight: 600 }}>BUY</span>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>RSI &lt; 30 + MA5 cross</span>
                </div>
                <div style={{ color: 'var(--accent-green)', fontWeight: 700 }}>80%</div>
            </div>

            <div className="flex-between" style={{ paddingBottom: '16px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
                <div className="flex-col">
                    <span style={{ fontWeight: 600 }}>SELL</span>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>RSI &gt; 70 + MA5 cross</span>
                </div>
                <div style={{ color: 'var(--accent-red)', fontWeight: 700 }}>85%</div>
            </div>

            <div style={{ 
                marginTop: 'auto', 
                padding: '16px', 
                borderRadius: '12px', 
                background: 'linear-gradient(135deg, rgba(255,107,53,0.2) 0%, rgba(255,107,53,0) 100%)',
                border: '1px solid rgba(255,107,53,0.3)'
            }}>
                <h4 style={{ color: 'var(--accent-orange)', marginBottom: '8px' }}>Active Pattern</h4>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
                    Searching for bullish divergence on 1H timeframe.
                </p>
            </div>
        </div>
    );
};
