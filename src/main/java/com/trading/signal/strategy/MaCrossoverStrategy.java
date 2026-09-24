package com.trading.signal.strategy;

import com.trading.signal.entity.IndicatorSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * MA Crossover + RSI + Volume + Trend filter strategy.
 * Thống nhất logic cho cả live signal và backtest (DRY).
 *
 * Confidence Score tính thực sự:
 *   Base: 50
 *   + RSI distance bonus (tối đa +15)
 *   + Volume confirmation (+10)
 *   + MACD histogram alignment (+10)
 *   + Trend alignment MA50 (+15)
 *   - Sideways market / ADX thấp (-20 đến -30)
 */
@Component
public class MaCrossoverStrategy implements TradingStrategy {

    private static final BigDecimal RSI_OVERSOLD = new BigDecimal("30");
    private static final BigDecimal RSI_OVERBOUGHT = new BigDecimal("70");
    private static final BigDecimal RSI_EXTREME_LOW = new BigDecimal("20");
    private static final BigDecimal RSI_EXTREME_HIGH = new BigDecimal("80");
    private static final BigDecimal VOLUME_THRESHOLD = new BigDecimal("1.2");
    private static final BigDecimal ADX_TRENDING = new BigDecimal("20");
    private static final BigDecimal ADX_STRONG_TREND = new BigDecimal("30");

    @Override
    public String getName() {
        return "MA_Crossover_RSI_v2";
    }

    @Override
    public List<SignalCandidate> evaluate(List<IndicatorSnapshot> snapshots) {
        if (snapshots.size() < 2) return List.of();

        List<SignalCandidate> candidates = new ArrayList<>();

        for (int i = 1; i < snapshots.size(); i++) {
            IndicatorSnapshot prev = snapshots.get(i - 1);
            IndicatorSnapshot curr = snapshots.get(i);

            if (!hasRequiredIndicators(prev, curr)) continue;

            BigDecimal rsi = curr.getRsi14();
            BigDecimal prevRsi = prev.getRsi14();
            BigDecimal currMa5 = curr.getMa5();
            BigDecimal currMa20 = curr.getMa20();
            BigDecimal prevMa5 = prev.getMa5();
            BigDecimal prevMa20 = prev.getMa20();

            // --- BUY SIGNALS ---

            // Pattern 1: Golden Cross + RSI confirmation
            boolean isGoldenCross = prevMa5.compareTo(prevMa20) <= 0 && currMa5.compareTo(currMa20) > 0;
            if (isGoldenCross && rsi.compareTo(RSI_OVERBOUGHT) < 0) {
                BigDecimal confidence = calculateBuyConfidence(curr, "GOLDEN_CROSS");
                Map<String, BigDecimal> indicators = buildIndicatorMap(curr);
                candidates.add(new SignalCandidate(
                        "BUY", curr.getCandleTime(), confidence,
                        "Golden Cross", "MA5 cắt lên MA20 — xu hướng tăng mạnh",
                        indicators
                ));
                continue;
            }

            // Pattern 2: RSI Oversold Rebound (RSI vượt lên 30 từ dưới)
            if (prevRsi != null && prevRsi.compareTo(RSI_OVERSOLD) < 0
                    && rsi.compareTo(RSI_OVERSOLD) >= 0
                    && currMa5.compareTo(currMa20) > 0) { // Chỉ mua khi MA5 > MA20 (trend up)
                BigDecimal confidence = calculateBuyConfidence(curr, "RSI_REBOUND");
                Map<String, BigDecimal> indicators = buildIndicatorMap(curr);
                candidates.add(new SignalCandidate(
                        "BUY", curr.getCandleTime(), confidence,
                        "RSI Oversold Rebound", "RSI thoát vùng quá bán + MA5 > MA20",
                        indicators
                ));
                continue;
            }

            // --- SELL SIGNALS ---

            // Pattern 3: Death Cross + RSI confirmation
            boolean isDeathCross = prevMa5.compareTo(prevMa20) >= 0 && currMa5.compareTo(currMa20) < 0;
            if (isDeathCross && rsi.compareTo(RSI_OVERSOLD) > 0) {
                BigDecimal confidence = calculateSellConfidence(curr, "DEATH_CROSS");
                Map<String, BigDecimal> indicators = buildIndicatorMap(curr);
                candidates.add(new SignalCandidate(
                        "SELL", curr.getCandleTime(), confidence,
                        "Death Cross", "MA5 cắt xuống MA20 — xu hướng giảm",
                        indicators
                ));
                continue;
            }

            // Pattern 4: RSI Overbought Pullback (RSI rớt xuống 70 từ trên)
            if (prevRsi != null && prevRsi.compareTo(RSI_OVERBOUGHT) > 0
                    && rsi.compareTo(RSI_OVERBOUGHT) <= 0
                    && currMa5.compareTo(currMa20) < 0) { // Chỉ bán khi MA5 < MA20 (trend down)
                BigDecimal confidence = calculateSellConfidence(curr, "RSI_PULLBACK");
                Map<String, BigDecimal> indicators = buildIndicatorMap(curr);
                candidates.add(new SignalCandidate(
                        "SELL", curr.getCandleTime(), confidence,
                        "RSI Overbought Pullback", "RSI rời vùng quá mua + MA5 < MA20",
                        indicators
                ));
            }
        }

        return candidates;
    }

    private BigDecimal calculateBuyConfidence(IndicatorSnapshot snap, String pattern) {
        BigDecimal score = new BigDecimal("50"); // Base

        // RSI bonus: RSI càng thấp (càng xa 30 về phía oversold) → càng mạnh
        BigDecimal rsi = snap.getRsi14();
        if (rsi.compareTo(RSI_EXTREME_LOW) <= 0) {
            score = score.add(new BigDecimal("15")); // RSI cực thấp
        } else if (rsi.compareTo(RSI_OVERSOLD) <= 0) {
            score = score.add(new BigDecimal("10")); // RSI vùng oversold
        } else if (rsi.compareTo(new BigDecimal("45")) <= 0) {
            score = score.add(new BigDecimal("5")); // RSI vùng trung bình thấp
        }

        // Volume confirmation: volume cao → mạnh
        if (snap.getVolumeRatio() != null && snap.getVolumeRatio().compareTo(VOLUME_THRESHOLD) > 0) {
            score = score.add(new BigDecimal("10"));
        }

        // MACD histogram alignment: MACD > signal → bullish
        if (snap.getMacd() != null && snap.getMacdSignal() != null
                && snap.getMacd().compareTo(snap.getMacdSignal()) > 0) {
            score = score.add(new BigDecimal("10"));
        }

        // Trend alignment: giá trên MA50 → uptrend
        if (snap.getMa50() != null && snap.getMa5() != null
                && snap.getMa5().compareTo(snap.getMa50()) > 0) {
            score = score.add(new BigDecimal("15"));
        }

        // Sideways penalty: ADX thấp → market không có xu hướng
        score = applyAdxPenalty(snap, score);

        return clampScore(score);
    }

    private BigDecimal calculateSellConfidence(IndicatorSnapshot snap, String pattern) {
        BigDecimal score = new BigDecimal("50"); // Base

        // RSI bonus: RSI càng cao → sell càng mạnh
        BigDecimal rsi = snap.getRsi14();
        if (rsi.compareTo(RSI_EXTREME_HIGH) >= 0) {
            score = score.add(new BigDecimal("15"));
        } else if (rsi.compareTo(RSI_OVERBOUGHT) >= 0) {
            score = score.add(new BigDecimal("10"));
        } else if (rsi.compareTo(new BigDecimal("55")) >= 0) {
            score = score.add(new BigDecimal("5"));
        }

        // Volume confirmation
        if (snap.getVolumeRatio() != null && snap.getVolumeRatio().compareTo(VOLUME_THRESHOLD) > 0) {
            score = score.add(new BigDecimal("10"));
        }

        // MACD histogram alignment: MACD < signal → bearish
        if (snap.getMacd() != null && snap.getMacdSignal() != null
                && snap.getMacd().compareTo(snap.getMacdSignal()) < 0) {
            score = score.add(new BigDecimal("10"));
        }

        // Trend alignment: giá dưới MA50 → downtrend
        if (snap.getMa50() != null && snap.getMa5() != null
                && snap.getMa5().compareTo(snap.getMa50()) < 0) {
            score = score.add(new BigDecimal("15"));
        }

        score = applyAdxPenalty(snap, score);

        return clampScore(score);
    }

    private BigDecimal applyAdxPenalty(IndicatorSnapshot snap, BigDecimal score) {
        if (snap.getAdx14() == null) return score; // Nếu chưa có ADX, không phạt

        BigDecimal adx = snap.getAdx14();
        if (adx.compareTo(ADX_TRENDING) < 0) {
            // ponytail: ADX < 20 = sideways, phạt nặng để tránh whipsaw
            score = score.subtract(new BigDecimal("25"));
        } else if (adx.compareTo(ADX_STRONG_TREND) >= 0) {
            // ADX > 30 = strong trend, thưởng nhẹ
            score = score.add(new BigDecimal("5"));
        }
        return score;
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (score.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasRequiredIndicators(IndicatorSnapshot prev, IndicatorSnapshot curr) {
        return curr.getRsi14() != null && curr.getMa5() != null && curr.getMa20() != null
                && prev.getMa5() != null && prev.getMa20() != null;
    }

    private Map<String, BigDecimal> buildIndicatorMap(IndicatorSnapshot snap) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (snap.getRsi14() != null) map.put("rsi14", snap.getRsi14());
        if (snap.getMa5() != null) map.put("ma5", snap.getMa5());
        if (snap.getMa20() != null) map.put("ma20", snap.getMa20());
        if (snap.getMa50() != null) map.put("ma50", snap.getMa50());
        if (snap.getMacd() != null) map.put("macd", snap.getMacd());
        if (snap.getMacdSignal() != null) map.put("macdSignal", snap.getMacdSignal());
        if (snap.getVolumeRatio() != null) map.put("volumeRatio", snap.getVolumeRatio());
        if (snap.getAtr14() != null) map.put("atr14", snap.getAtr14());
        if (snap.getAdx14() != null) map.put("adx14", snap.getAdx14());
        return map;
    }
}
