# PROJECT PLAN: Trading Signal & Pattern Recognition System
**Vai trò đánh giá:** Senior Engineer / Tech Lead review trước khi kick-off
**Ngày:** 2026-09-17
**Đối tượng:** Fresher++ portfolio project (Java Spring Boot)

---

## 1. ĐÁNH GIÁ TÍNH KHẢ THI (FEASIBILITY REVIEW)

### 1.1 Góc nhìn Senior — nói thẳng trước khi bắt đầu

Trước khi review chi tiết, cần làm rõ một điểm quan trọng:

> **"Gợi ý trading tỷ lệ thắng cao nhất" là một lời hứa nguy hiểm.** Không có hệ thống rule-based nào (MA, RSI, MACD) đạt "tỷ lệ thắng cao" ổn định trên thị trường thực — nếu có, quỹ đầu tư đã dùng nó và thị trường đã tự điều chỉnh (arbitrage away). Với một dự án portfolio, mục tiêu thực tế nên là: **xây một hệ thống thu thập dữ liệu, tính chỉ báo, sinh tín hiệu, ghi nhận kết quả, và so khớp pattern lịch sử** — không phải xây một "cỗ máy kiếm tiền". Điều này quan trọng vì cách bạn frame nó trong CV/phỏng vấn quyết định bạn có bị hỏi vặn "vậy win rate thực tế bao nhiêu, backtest ra sao" hay không.

**Khuyến nghị:** Đổi mindset dự án từ "trading tool sinh lời" → **"Signal Research & Backtesting Platform"**. Đây là câu chuyện đúng đắn, chuyên nghiệp và không bị hỏi khó.

### 1.2 Đánh giá độ phức tạp so với năng lực Fresher++

| Thành phần | Độ khó | Rủi ro nếu làm thiếu chuẩn bị |
|---|---|---|
| CRUD + Auth (JWT) | Thấp | Thấp — đã quen thuộc |
| Thu thập dữ liệu (external API, scheduled job) | Trung bình | Rate limit, API key, lỗi mạng không xử lý → job chết |
| Tính chỉ báo kỹ thuật (RSI, MA, MACD) | Trung bình-Cao | Sai công thức tài chính → cả hệ thống sai theo, khó phát hiện |
| Sinh tín hiệu (rule-based) | Trung bình | Rule đơn giản quá → không có gì để nói khi phỏng vấn |
| Backtesting engine | Cao | Đây là phần dễ bị làm sai nhất — "lookahead bias" (dùng dữ liệu tương lai để test) |
| Pattern matching (vector similarity) | Trung bình-Cao | Dễ overengineer, chọn sai thuật toán |
| Export PDF/Excel | Thấp-Trung bình | Ít rủi ro kỹ thuật |

**Kết luận:** Dự án khả thi nhưng **cần cắt giảm scope** để tránh sa lầy. Không làm ML thật, không cần real-time trading, không cần đặt lệnh thật.

### 1.3 Rủi ro lớn nhất cần lường trước

1. **Lookahead bias khi backtest** — nếu tính tín hiệu bằng dữ liệu đã biết cả tương lai, kết quả "thắng cao" sẽ ảo hoàn toàn. Đây là lỗi kinh điển, review kỹ ở mục Testing.
2. **API rate limit từ nguồn dữ liệu** (Binance/Alpha Vantage) — cần cache và không gọi liên tục.
3. **Định nghĩa "thắng" mơ hồ** — thắng theo % lời, theo thời gian nắm giữ, hay theo target price? Phải chốt rule rõ ràng trước khi code Pattern Storage.
4. **Scope creep** — dễ bị cuốn vào làm ML/AI thật sự, quá sức và quá thời gian cho 1 dự án portfolio.

---

## 2. DATABASE SCHEMA CHI TIẾT

### 2.1 ERD tổng quan (dạng text)

```
users ──< accounts ──< watchlists ──< watchlist_items >── instruments
  │                                                            │
  ├──< virtual_wallets ──< wallet_transactions                 │
  │                              │                              │
  └──< trade_logs >──────────────┘──────────────────────────────┘
              │
              └──< winning_patterns
                        │
instruments ──< price_candles                                  
instruments ──< indicator_snapshots                            
instruments ──< trading_signals                                
```

### 2.2 Chi tiết từng bảng

#### `users`
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| email | VARCHAR(255) UNIQUE | |
| password_hash | VARCHAR(255) | BCrypt |
| full_name | VARCHAR(100) | |
| role | VARCHAR(20) | USER / ADMIN |
| created_at | TIMESTAMP | |

#### `instruments` (mã coin/cổ phiếu được theo dõi)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| symbol | VARCHAR(20) UNIQUE | vd: BTCUSDT, AAPL |
| type | VARCHAR(20) | CRYPTO / STOCK |
| exchange | VARCHAR(50) | Binance, NASDAQ... |
| is_active | BOOLEAN | có đang thu thập data không |

#### `price_candles` (dữ liệu OHLCV — bảng lớn nhất, cần index kỹ)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| instrument_id | BIGINT FK | |
| timeframe | VARCHAR(10) | 1h, 4h, 1d |
| open_time | TIMESTAMP | |
| open, high, low, close | DECIMAL(18,8) | |
| volume | DECIMAL(18,8) | |
| **UNIQUE** | (instrument_id, timeframe, open_time) | tránh trùng dữ liệu |

> **Index bắt buộc:** `(instrument_id, timeframe, open_time)` — mọi query đều lọc theo 3 cột này.

#### `indicator_snapshots` (giá trị chỉ báo tại từng thời điểm — tách riêng để không tính lại nhiều lần)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| instrument_id | BIGINT FK | |
| candle_time | TIMESTAMP | |
| rsi_14 | DECIMAL(10,4) | |
| ma_5, ma_20, ma_50 | DECIMAL(18,8) | |
| macd, macd_signal | DECIMAL(18,8) | |
| volume_ratio | DECIMAL(10,4) | volume hiện tại / trung bình 20 phiên |

#### `trading_signals` (tín hiệu hệ thống sinh ra)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| instrument_id | BIGINT FK | |
| generated_at | TIMESTAMP | |
| signal_type | VARCHAR(10) | BUY / SELL / HOLD |
| confidence_score | DECIMAL(5,2) | 0–100, tính từ rule + pattern match |
| rule_basis | TEXT (JSON) | rule nào kích hoạt, để giải thích lại được (explainability) |
| matched_pattern_id | BIGINT FK NULL | nếu khớp với pattern lịch sử |

#### `trade_logs` (lệnh mô phỏng người dùng ghi nhận — KHÔNG phải lệnh thật)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| wallet_id | BIGINT FK | ví ảo bị ảnh hưởng bởi lệnh này |
| instrument_id | BIGINT FK | |
| signal_id | BIGINT FK NULL | liên kết tới tín hiệu đã theo (nếu có) |
| entry_price, exit_price | DECIMAL(18,8) | **bắt buộc lấy từ `price_candles` tại entry_time/exit_time, không cho nhập tay** |
| entry_time, exit_time | TIMESTAMP | |
| result | VARCHAR(10) | WIN / LOSS / BREAKEVEN |
| pnl_percent | DECIMAL(10,4) | |
| note | TEXT | |

#### `virtual_wallets` (ví tiền ảo — mô hình paper trading: đánh giá thật, tiền giả)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| user_id | BIGINT FK UNIQUE | mỗi user 1 ví (mở rộng multi-portfolio ở phase sau nếu cần) |
| balance | DECIMAL(18,2) | số dư ảo hiện tại |
| initial_balance | DECIMAL(18,2) | vốn ban đầu, dùng tính % lời tổng và vẽ equity curve |
| version | BIGINT | dùng cho optimistic locking (`@Version`), chống race condition khi 2 lệnh đóng cùng lúc |
| updated_at | TIMESTAMP | |

#### `wallet_transactions` (lịch sử biến động ví — bắt buộc để audit và không lệch sổ)
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| wallet_id | BIGINT FK | |
| trade_log_id | BIGINT FK | lệnh nào gây ra biến động này |
| amount | DECIMAL(18,2) | dương nếu thắng, âm nếu thua |
| balance_after | DECIMAL(18,2) | snapshot số dư sau giao dịch — tiện dựng equity curve mà không phải cộng dồn lại từ đầu |
| created_at | TIMESTAMP | |

> **Nguyên tắc bất biến (invariant) phải giữ đúng:** `wallet.balance` tại bất kỳ thời điểm nào phải bằng `initial_balance + SUM(wallet_transactions.amount)`. Đây là điều kiện dùng để viết test đối chiếu sổ sách (mục 5.2).

#### `winning_patterns` (bối cảnh chỉ báo tại các lệnh thắng — trái tim của ý tưởng "học từ lịch sử")
| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | BIGINT PK | |
| source_trade_log_id | BIGINT FK | lệnh thắng nào tạo ra pattern này |
| instrument_id | BIGINT FK | |
| feature_vector | TEXT (JSON) | vd: {"rsi":28.5,"ma_cross":1,"vol_ratio":1.8,"macd_hist":0.02} — chuẩn hoá (normalize) trước khi lưu |
| pnl_percent | DECIMAL(10,4) | mức lời để đánh giá "pattern mạnh" |
| created_at | TIMESTAMP | |

> **Thiết kế quan trọng:** `feature_vector` lưu dạng JSON để linh hoạt thêm/bớt chỉ báo mà không phải migrate schema liên tục. Khi cần so khớp, load ra và tính cosine similarity trong Java, KHÔNG tính trong SQL.

### 2.3 Quyết định thiết kế cần cân nhắc kỹ (Design Decisions)

- **Time-series data lớn dần theo thời gian** → cân nhắc partition theo tháng nếu dùng PostgreSQL, hoặc archive dữ liệu cũ hơn 1-2 năm ra bảng riêng.
- **Không lưu indicator tính lại được từ price_candles** trừ khi cần truy vấn nhanh — đây là trade-off "lưu trước (denormalize) vs tính lại (normalize)". Với dự án học tập, nên **lưu trước** để luyện viết batch job tính toán định kỳ.
- **feature_vector dạng JSON** thay vì tách cột riêng cho từng chỉ báo → linh hoạt hơn nhưng khó query bằng SQL thuần (chấp nhận trade-off, xử lý ở tầng service).
- **Mô hình paper trading (tiền ảo trên giá thật):** `trade_logs` không cho nhập giá tay — entry/exit price phải được service tự tra từ `price_candles` theo `entry_time`/`exit_time` do người dùng chọn. Điều này đảm bảo pattern lưu lại phản ánh đúng thị trường thật, không bị người dùng "gian lận" giá để tạo pattern ảo.
- **`wallet_transactions` là nguồn sự thật (source of truth)**, không phải `virtual_wallets.balance` — balance chỉ là cache/snapshot để đọc nhanh, luôn có thể tính lại từ transactions nếu nghi ngờ sai lệch.

---

## 3. FEATURE ROADMAP — ƯU TIÊN LÀM GÌ TRƯỚC

Nguyên tắc ưu tiên: **làm nền tảng chắc trước, chức năng "thông minh" sau cùng** — vì mọi tính toán phía sau phụ thuộc vào dữ liệu đúng.

### Phase 0 — Nền tảng (bắt buộc, không có gì để demo nếu thiếu)
1. Setup project, database, JWT auth, user CRUD
2. Entity `instruments` + CRUD quản lý danh sách theo dõi
3. **Data Collector**: gọi API Binance/Alpha Vantage, lưu vào `price_candles` (scheduled job mỗi giờ/ngày)
   - *Ưu tiên cao nhất vì mọi thứ khác phụ thuộc vào có dữ liệu sạch*

### Phase 1 — Tính toán & hiển thị cơ bản
4. Indicator Engine: tính RSI, MA, MACD từ `price_candles` → lưu `indicator_snapshots`
5. REST API trả về dữ liệu chart-ready (giá + chỉ báo theo khoảng thời gian)
6. Unit test kỹ phần tính chỉ báo (đây là phần dễ sai nhất, phải test bằng số liệu tham chiếu thật)

### Phase 2 — Sinh tín hiệu
7. Signal Engine: rule-based đơn giản (vd: RSI < 30 + MA5 cắt lên MA20 → BUY)
8. Lưu `trading_signals` kèm `rule_basis` để giải thích được lý do

### Phase 3 — Virtual Wallet, Trade Log & Pattern (phần "câu chuyện hay" của dự án)
9. Tạo `virtual_wallets` tự động khi user đăng ký (balance mặc định, vd 10,000 ảo)
10. CRUD `trade_logs` — người dùng chọn instrument + thời điểm vào/ra, hệ thống tự tra giá thật từ `price_candles` (không nhập tay)
11. Logic đóng lệnh (đây là nghiệp vụ lõi, viết cẩn thận trong 1 `@Transactional` duy nhất):
    - Tính `pnl_percent` từ entry/exit price thật
    - Cộng/trừ `virtual_wallets.balance`, ghi `wallet_transactions`
    - Nếu WIN và `pnl_percent` vượt ngưỡng → tự động tạo `winning_patterns`
    - Toàn bộ rollback nếu bất kỳ bước nào lỗi — không để ví bị trừ mà lệnh không ghi
12. Pattern Matching Service: so khớp tín hiệu mới với patterns cũ (cosine similarity) → tăng `confidence_score`

### Phase 4 — Backtesting (làm sau cùng vì rủi ro lookahead bias cao nhất)
13. Backtest engine: chạy lại rule trên dữ liệu lịch sử, tính win rate, max drawdown
14. **Bắt buộc walk-forward**: chỉ dùng dữ liệu quá khứ tại mỗi điểm test, không leak tương lai

### Phase 5 — Điểm cộng khác biệt (nếu còn thời gian)
15. Export báo cáo PDF/Excel (Apache POI/iText) — bao gồm equity curve của ví ảo
16. WebSocket để đẩy tín hiệu real-time lên UI
17. Dashboard thống kê: win rate theo từng loại pattern, theo từng mã, và tăng trưởng vốn ảo theo thời gian

**Không làm trong scope portfolio này:** đặt lệnh thật qua sàn, ML/deep learning thật, xử lý dữ liệu tick-by-tick tần suất cao.

---

## 4. KIẾN TRÚC & TỐI ƯU HOÁ

### 4.1 Kiến trúc tầng (Layered Architecture)
```
Controller → Service → Repository → Entity
         ↓
       DTO (request/response riêng biệt, không expose Entity trực tiếp)
         ↓
   @ControllerAdvice (xử lý exception tập trung)
```

### 4.2 Điểm tối ưu cần chú ý ngay từ đầu

| Vấn đề | Giải pháp |
|---|---|
| Query price_candles theo khoảng thời gian lớn | Index composite (instrument_id, timeframe, open_time), phân trang bằng keyset pagination thay vì OFFSET |
| Tính chỉ báo lặp lại nhiều lần | Cache kết quả `indicator_snapshots`, chỉ tính incremental khi có nến mới |
| Gọi API bên ngoài | Retry + backoff, circuit breaker (Resilience4j) để job không chết khi API lỗi |
| Pattern matching quét toàn bộ bảng | Giới hạn so khớp trong N patterns gần nhất/cùng instrument, không quét toàn hệ thống |
| N+1 query khi load trade_logs kèm instrument | Dùng `@EntityGraph` hoặc JOIN FETCH |
| Export Excel/PDF dữ liệu lớn | Xử lý bất đồng bộ (async job + thông báo khi xong), tránh block request |
| **Race condition khi 2 lệnh đóng cùng lúc trên 1 ví** | Optimistic locking (`@Version`) trên `virtual_wallets`; nếu conflict thì retry hoặc trả lỗi cho client thử lại |
| **Ví bị trừ tiền nhưng lệnh ghi lỗi giữa chừng (crash, exception)** | Toàn bộ luồng đóng lệnh (tính pnl → cập nhật balance → ghi transaction → tạo pattern) nằm trong 1 `@Transactional`, rollback triệt để nếu bất kỳ bước nào lỗi |
| Balance âm do trừ vượt số dư | Validate ở tầng service trước khi trừ, chặn giao dịch nếu không đủ số dư (chưa hỗ trợ margin/đòn bẩy ở phase này) |

### 4.3 Bảo mật
- JWT với refresh token, access token thời gian sống ngắn
- Validate input kỹ (Bean Validation `@Valid`) — đặc biệt số tiền, ngày tháng
- Rate limit API để tránh lạm dụng (đặc biệt endpoint gọi tới data collector)
- Không log API key của sàn ra console/log file

---

## 5. TESTING STRATEGY

### 5.1 Theo tầng

| Loại test | Phạm vi | Công cụ |
|---|---|---|
| Unit test | Indicator Engine (RSI, MA, MACD) — **quan trọng nhất, dùng số liệu tham chiếu từ TradingView/Investing.com để so sánh** | JUnit 5 |
| Unit test | Signal rule logic | JUnit 5 + Mockito |
| Integration test | Repository layer (query JPQL phức tạp) | Testcontainers (PostgreSQL thật, không dùng H2 để tránh sai lệch behavior) |
| Integration test | Data Collector với API bên ngoài | WireMock (giả lập response API, tránh gọi thật khi test) |
| Contract test | REST endpoints | MockMvc / RestAssured |
| Backtest validation | Đảm bảo không lookahead bias | Test riêng: chạy backtest trên dữ liệu đã biết trước kết quả tay, so sánh |
| Integration test | Virtual Wallet: concurrency, rollback, đối chiếu sổ | Testcontainers + test giả lập request đồng thời (CompletableFuture/ExecutorService) |

### 5.2 Điểm test đặc biệt quan trọng (senior sẽ hỏi kỹ điểm này)
- **Test lookahead bias**: viết test case cụ thể đảm bảo tại thời điểm T, hệ thống KHÔNG thể truy cập dữ liệu candle sau thời điểm T.
- **Test edge case tài chính**: chia cho 0 (volume=0), giá âm/null từ API lỗi, gap dữ liệu (thiếu nến do downtime).
- **Test độ chính xác chỉ báo** bằng cách so với nguồn uy tín, sai số cho phép < 0.01%.
- **Test concurrency ví ảo**: giả lập 2 lệnh đóng cùng lúc trên cùng 1 ví → đảm bảo balance cuối cùng chính xác, không mất/thừa tiền do race condition.
- **Test transaction rollback**: giả lập lỗi giữa chừng (vd tạo `winning_patterns` thất bại) → đảm bảo `wallet_transactions` và `balance` không bị cập nhật treo (partial write).
- **Test đối chiếu sổ sách (reconciliation)**: với mọi ví, `balance` phải luôn bằng `initial_balance + SUM(wallet_transactions.amount)` — viết test chạy định kỳ kiểm tra invariant này.

---

## 6. NON-FUNCTIONAL & METRICS ĐÁNH GIÁ THÀNH CÔNG

- **Không đo bằng "win rate cao"** (dễ overfit, không đáng tin với dự án nhỏ) — đo bằng:
  - Data pipeline chạy ổn định (uptime của scheduled job, tỷ lệ lỗi gọi API)
  - Độ chính xác chỉ báo so với nguồn tham chiếu
  - Test coverage của Indicator Engine & Backtest Engine (nên >80%)
  - Thời gian phản hồi API chart-ready < 300ms với dữ liệu 1 năm
  - **Tăng trưởng vốn ảo theo thời gian (equity curve)** — dựng từ `wallet_transactions.balance_after`, trực quan và dễ demo trong phỏng vấn hơn con số win rate đơn thuần, đồng thời cho thấy cả drawdown (mức sụt vốn) chứ không chỉ tổng lời

---

## 7. TÓM TẮT KHUYẾN NGHỊ CỦA SENIOR

1. Đổi framing dự án thành **"Signal Research & Backtesting Platform"** thay vì "hệ thống thắng cao" — tránh bị hỏi khó và đúng bản chất kỹ thuật hơn.
2. Làm đúng thứ tự Phase 0 → 4, đừng nhảy vào Pattern Matching/Backtest trước khi data pipeline ổn định.
3. Đầu tư thời gian nhiều nhất vào **test độ chính xác của Indicator Engine** và **chống lookahead bias** — đây là 2 điểm một senior thật sự sẽ hỏi sâu khi review code của bạn.
4. Đừng làm ML thật trong bản đầu — cosine similarity/rule-based là đủ để kể câu chuyện "hệ thống học từ lịch sử" mà vẫn trong tầm với.