import React, { useEffect, useRef } from 'react';
import { createChart, ColorType, CandlestickSeries, LineSeries } from 'lightweight-charts';
import type { IChartApi, Time } from 'lightweight-charts';

export interface ChartDataDto {
    time: string; // ISO string from backend
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    rsi14?: number;
    ma5?: number;
    ma20?: number;
    ma50?: number;
    macd?: number;
    macdSignal?: number;
    volumeRatio?: number;
}

interface TradingChartProps {
    data: ChartDataDto[];
    slPrice?: number;
    tpPrice?: number;
    entryPrice?: number;
}

export const TradingChart: React.FC<TradingChartProps> = ({ data, slPrice, tpPrice, entryPrice }) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candlestickSeriesRef = useRef<any>(null);
    const ma5SeriesRef = useRef<any>(null);
    const ma20SeriesRef = useRef<any>(null);
    const ma50SeriesRef = useRef<any>(null);
    const hasFittedRef = useRef(false);

    // 1. Initialize Chart instance ONCE on mount
    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: 'transparent' },
                textColor: '#64748b',
            },
            grid: {
                vertLines: { color: 'rgba(148, 163, 184, 0.1)' },
                horzLines: { color: 'rgba(148, 163, 184, 0.1)' },
            },
            crosshair: {
                mode: 0,
            },
            rightPriceScale: {
                borderVisible: false,
            },
            timeScale: {
                borderVisible: false,
                timeVisible: true,
            },
            height: 500,
        });

        chartRef.current = chart;

        // Series
        candlestickSeriesRef.current = chart.addSeries(CandlestickSeries, {
            upColor: '#3b82f6',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#3b82f6',
            wickDownColor: '#ef4444',
        });

        ma5SeriesRef.current = chart.addSeries(LineSeries, { color: '#f59e0b', lineWidth: 1, title: 'MA5' });
        ma20SeriesRef.current = chart.addSeries(LineSeries, { color: '#3b82f6', lineWidth: 2, title: 'MA20' });
        ma50SeriesRef.current = chart.addSeries(LineSeries, { color: '#ec4899', lineWidth: 2, title: 'MA50' });

        // Create references for SL/TP price lines that we will update dynamically
        candlestickSeriesRef.current.slLine = null;
        candlestickSeriesRef.current.tpLine = null;
        candlestickSeriesRef.current.entryLine = null;

        // ResizeObserver
        const resizeObserver = new ResizeObserver(entries => {
            if (entries[0] && chartContainerRef.current && chartRef.current) {
                const newWidth = Math.floor(entries[0].contentRect.width);
                if (newWidth > 0) {
                    chartRef.current.applyOptions({ width: newWidth });
                }
            }
        });

        resizeObserver.observe(chartContainerRef.current);

        return () => {
            resizeObserver.disconnect();
            chart.remove();
            chartRef.current = null;
            candlestickSeriesRef.current = null;
            ma5SeriesRef.current = null;
            ma20SeriesRef.current = null;
            ma50SeriesRef.current = null;
        };
    }, []);

    // 2. Update data seamlessly without recreating chart or jumping scroll
    useEffect(() => {
        if (!candlestickSeriesRef.current || !data || data.length === 0) return;

        const formattedData = data.map(item => ({
            ...item,
            time: (new Date(item.time).getTime() / 1000) as Time,
        })).sort((a, b) => (a.time as number) - (b.time as number));

        candlestickSeriesRef.current.setData(formattedData.map(d => ({
            time: d.time,
            open: d.open,
            high: d.high,
            low: d.low,
            close: d.close,
        })));

        if (ma5SeriesRef.current) {
            ma5SeriesRef.current.setData(formattedData.filter(d => d.ma5 != null).map(d => ({ time: d.time, value: d.ma5! })));
        }
        if (ma20SeriesRef.current) {
            ma20SeriesRef.current.setData(formattedData.filter(d => d.ma20 != null).map(d => ({ time: d.time, value: d.ma20! })));
        }
        if (ma50SeriesRef.current) {
            ma50SeriesRef.current.setData(formattedData.filter(d => d.ma50 != null).map(d => ({ time: d.time, value: d.ma50! })));
        }

        // Only fit content on the very first data load
        if (!hasFittedRef.current && chartRef.current) {
            chartRef.current.timeScale().fitContent();
            hasFittedRef.current = true;
        }
    }, [data]);

    // 3. Update Price Lines when position changes
    useEffect(() => {
        const series = candlestickSeriesRef.current;
        if (!series) return;

        // Clear existing lines
        if (series.slLine) { series.removePriceLine(series.slLine); series.slLine = null; }
        if (series.tpLine) { series.removePriceLine(series.tpLine); series.tpLine = null; }
        if (series.entryLine) { series.removePriceLine(series.entryLine); series.entryLine = null; }

        if (entryPrice) {
            series.entryLine = series.createPriceLine({
                price: entryPrice,
                color: '#3b82f6',
                lineWidth: 2,
                lineStyle: 2, // Dashed
                axisLabelVisible: true,
                title: 'ENTRY',
            });
        }
        if (slPrice) {
            series.slLine = series.createPriceLine({
                price: slPrice,
                color: '#ef4444',
                lineWidth: 2,
                lineStyle: 1, // Dotted
                axisLabelVisible: true,
                title: 'SL',
            });
        }
        if (tpPrice) {
            series.tpLine = series.createPriceLine({
                price: tpPrice,
                color: '#10b981',
                lineWidth: 2,
                lineStyle: 1,
                axisLabelVisible: true,
                title: 'TP',
            });
        }
    }, [slPrice, tpPrice, entryPrice]);

    return (
        <div className="chart-wrapper">
            <div className="chart-header">
                <h3 className="chart-title">BTCUSDT - 1H</h3>
            </div>
            <div ref={chartContainerRef} className="tv-lightweight-charts" />
        </div>
    );
};
