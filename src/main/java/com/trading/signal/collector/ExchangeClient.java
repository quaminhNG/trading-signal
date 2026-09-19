package com.trading.signal.collector;

import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import java.time.Instant;
import java.util.List;

/**
 * Interface chung cho các adapter thu thập dữ liệu giá từ sàn.
 * Mỗi sàn (Binance, Alpha Vantage...) implement interface này.
 */
public interface ExchangeClient {

    /** Adapter này hỗ trợ loại instrument nào? */
    boolean supports(Instrument.InstrumentType type);

    /**
     * Lấy dữ liệu nến OHLCV từ sàn.
     * @param symbol    Mã giao dịch (vd: BTCUSDT, AAPL)
     * @param timeframe Khung thời gian (vd: 1h, 4h, 1d)
     * @param from      Thời điểm bắt đầu
     * @param to        Thời điểm kết thúc
     * @return Danh sách nến (chưa set instrument — caller sẽ gán)
     */
    List<PriceCandle> fetchCandles(String symbol, String timeframe, Instant from, Instant to);
}
