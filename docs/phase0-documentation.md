# Phase 0 — Tài liệu Nền tảng

> **Mục tiêu Phase 0:** Dựng xong "bộ xương" hoàn chỉnh để tất cả Phase sau có thể xây tiếp mà không phải sửa nền.  
> Bao gồm: project skeleton, database, xác thực JWT, quản lý user, quản lý instruments, và pipeline thu thập dữ liệu giá.

---

## 1. Cách chạy project

```bash
# Bước 1: Khởi động PostgreSQL (cần Docker Desktop)
docker-compose up -d

# Bước 2: Chạy ứng dụng (Flyway tự tạo tables lần đầu)
mvn spring-boot:run

# Bước 3: Verify — đăng ký user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456","fullName":"Nguyen Van A"}'
```

**Yêu cầu:** Java 17+, Maven, Docker Desktop.

---

## 2. Cấu trúc thư mục

```
trading-signal/
│
├── docker-compose.yml          # Chạy PostgreSQL 16 container
├── .env                        # Biến môi trường cho Docker (credentials DB)
├── pom.xml                     # Maven dependencies + build config
│
├── src/main/resources/
│   ├── application.yml         # Config toàn bộ ứng dụng
│   └── db/migration/           # Flyway — tạo bảng tự động khi app khởi động
│       ├── V1__create_users.sql
│       ├── V2__create_instruments.sql
│       └── V3__create_price_candles.sql
│
├── src/main/java/com/trading/signal/
│   ├── TradingSignalApplication.java   # Entry point
│   ├── entity/           # JPA Entities — ánh xạ bảng trong DB
│   ├── repository/       # Spring Data — truy vấn DB
│   ├── service/          # Business logic
│   ├── controller/       # REST API endpoints
│   ├── dto/              # Data Transfer Objects (request/response)
│   ├── security/         # JWT + Spring Security
│   ├── collector/        # Thu thập dữ liệu giá từ sàn
│   └── exception/        # Xử lý lỗi tập trung
│
├── src/test/java/...
│   ├── TradingSignalApplicationTests.java   # Smoke test
│   └── security/JwtTokenProviderTest.java   # Test JWT
│
└── docs/                 # Tài liệu dự án
```

---

## 3. Chi tiết từng file — theo luồng dữ liệu

### 3.1 Hạ tầng (Infrastructure)

| File | Chức năng |
|------|-----------|
| `pom.xml` | Khai báo dependencies: Spring Boot 3.3.4, Spring Security, JPA, PostgreSQL driver, Flyway, JWT (jjwt 0.12.6), Lombok. Build bằng Maven. |
| `docker-compose.yml` | Tạo container PostgreSQL 16 trên port 5432. Dữ liệu persist qua Docker volume `postgres_data` — restart không mất data. |
| `.env` | Chứa credentials DB cho Docker (`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`). File này nằm trong `.gitignore`, không commit lên git. |
| `application.yml` | Config tập trung: datasource (trỏ DB), JWT (secret key, thời gian sống token), collector (URL sàn, cron schedule, bật/tắt adapter). |

### 3.2 Database Migrations (Flyway)

Flyway tự động chạy các file SQL theo thứ tự version (V1, V2, V3...) khi app khởi động. **Không dùng `ddl-auto`** — mọi thay đổi schema đều qua migration file, dễ track và rollback.

| File                           | Tạo bảng        | Ghi chú                                                                                                                                                                                |
| ------------------------------ | --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `V1__create_users.sql`         | `users`         | Lưu thông tin đăng nhập. `email` UNIQUE, `password_hash` BCrypt, `role` (USER/ADMIN).                                                                                                  |
| `V2__create_instruments.sql`   | `instruments`   | Danh mục mã giao dịch (BTCUSDT, AAPL...). `symbol` UNIQUE, `is_active` để bật/tắt thu thập data.                                                                                       |
| `V3__create_price_candles.sql` | `price_candles` | Dữ liệu nến OHLCV — bảng lớn nhất hệ thống. Composite index `(instrument_id, timeframe, open_time DESC)` để query nhanh. Unique constraint chống trùng dữ liệu khi collector chạy lại. |

### 3.3 Entity Layer — ánh xạ bảng DB thành Java object

| File               | Map bảng        | Điểm đáng chú ý                                                                                                                                                              |
| ------------------ | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `User.java`        | `users`         | Enum `Role` (USER/ADMIN). `@PrePersist` tự gán `createdAt` và role mặc định.                                                                                                 |
| `Instrument.java`  | `instruments`   | Enum `InstrumentType` (CRYPTO/STOCK). `@PrePersist` mặc định `active = true`.                                                                                                |
| `PriceCandle.java` | `price_candles` | OHLCV dùng `BigDecimal` (không dùng `double` — tránh lỗi làm tròn dấu phẩy động, đặc biệt quan trọng với crypto có giá rất nhỏ). Quan hệ `@ManyToOne` LAZY tới `Instrument`. |

### 3.4 Repository Layer — truy vấn DB

| File | Entity | Các query đáng chú ý |
|------|--------|----------------------|
| `UserRepository.java` | `User` | `findByEmail()` — dùng cho đăng nhập. `existsByEmail()` — check trùng khi đăng ký. |
| `InstrumentRepository.java` | `Instrument` | Filter theo `type`, `active`, hoặc kết hợp cả hai. `existsBySymbol()` — check trùng khi tạo mới. |
| `PriceCandleRepository.java` | `PriceCandle` | **3 query quan trọng:** (1) `findCandlesBefore` — keyset pagination lấy N nến gần nhất (không dùng OFFSET). (2) `findCandlesBetween` — lấy nến trong khoảng thời gian. (3) `findClosestCandle` — tra giá tại thời điểm T (Phase sau dùng cho trade_logs). |

> **Keyset pagination** là gì? Thay vì `OFFSET 1000 LIMIT 100` (DB phải duyệt qua 1000 row rồi bỏ), ta dùng `WHERE open_time < :lastSeen ORDER BY open_time DESC LIMIT 100` — luôn nhanh O(1) nhờ index.

### 3.5 Security Layer — xác thực JWT

Luồng xác thực:

```
Client gửi request
  → JwtAuthenticationFilter đọc header "Authorization: Bearer <token>"
    → JwtTokenProvider validate + parse token
      → CustomUserDetailsService load user từ DB
        → Set SecurityContext → request đi tiếp vào Controller
```

| File | Vai trò |
|------|---------|
| `JwtTokenProvider.java` | Tạo và validate JWT. Hai loại token: **access** (sống 15 phút, dùng gọi API) và **refresh** (sống 7 ngày, dùng xin access token mới). Dùng HMAC-SHA256 ký token. |
| `JwtAuthenticationFilter.java` | `OncePerRequestFilter` — chạy trước mọi request. Trích Bearer token từ header, validate, rồi set user vào SecurityContext. **Chỉ chấp nhận access token** (không cho dùng refresh token để gọi API). |
| `CustomUserDetailsService.java` | Implement `UserDetailsService` của Spring Security. Load user từ DB theo email, trả về `UserDetails` kèm role (ROLE_USER hoặc ROLE_ADMIN). |
| `SecurityConfig.java` | Cấu hình Spring Security: **stateless** (không dùng session), CSRF tắt (vì dùng JWT). Phân quyền: `/api/auth/**` ai cũng vào được; Instruments GET cần đăng nhập; Instruments POST/PUT/DELETE cần ADMIN. |

### 3.6 DTO Layer — dữ liệu vào/ra API

Dùng **Java record** — immutable, tự sinh getter/equals/hashCode, code gọn. Validation bằng `@Valid` + Bean Validation annotations.

| Package | File | Dùng cho |
|---------|------|----------|
| `dto/auth/` | `RegisterRequest.java` | Body đăng ký: email (valid format), password (6-100 ký tự), fullName. |
| | `LoginRequest.java` | Body đăng nhập: email, password. |
| | `AuthResponse.java` | Response trả về: accessToken, refreshToken, email, role. |
| | `RefreshTokenRequest.java` | Body refresh: refreshToken. |
| `dto/user/` | `UserResponse.java` | Thông tin user (không có password_hash). |
| | `UpdateUserRequest.java` | Cập nhật profile: fullName. |
| `dto/instrument/` | `InstrumentRequest.java` | Tạo/sửa instrument: symbol, type (CRYPTO/STOCK), exchange. |
| | `InstrumentResponse.java` | Thông tin instrument trả về client. |

### 3.7 Service Layer — business logic

| File | Logic chính |
|------|-------------|
| `AuthService.java` | **Register:** check email trùng → hash password (BCrypt) → save → trả token. **Login:** dùng `AuthenticationManager` verify credentials → trả token. **Refresh:** validate refresh token → trả cặp token mới. |
| `InstrumentService.java` | CRUD chuẩn. Đặc biệt: `deactivate()` (soft delete — set `is_active=false` thay vì xóa cứng, vì `price_candles` có FK trỏ tới instrument). |

### 3.8 Controller Layer — REST API

| File | Endpoints |
|------|-----------|
| `AuthController.java` | `POST /api/auth/register` — Đăng ký (public) |
| | `POST /api/auth/login` — Đăng nhập (public) |
| | `POST /api/auth/refresh` — Refresh token (public) |
| `UserController.java` | `GET /api/users/me` — Xem profile (authenticated) |
| | `PUT /api/users/me` — Sửa profile (authenticated) |
| `InstrumentController.java` | `GET /api/instruments` — List (filter: `type`, `activeOnly`) |
| | `GET /api/instruments/{id}` — Chi tiết |
| | `POST /api/instruments` — Tạo mới (ADMIN) |
| | `PUT /api/instruments/{id}` — Cập nhật (ADMIN) |
| | `DELETE /api/instruments/{id}` — Soft delete (ADMIN) |

### 3.9 Collector Layer — thu thập dữ liệu giá

Luồng hoạt động:

```
@Scheduled cron trigger
  → DataCollectorService lấy danh sách instruments active
    → Chọn ExchangeClient phù hợp (Binance cho CRYPTO, Alpha Vantage cho STOCK)
      → Gọi API sàn lấy nến OHLCV
        → Upsert vào price_candles (skip nếu đã có)
```

| File | Vai trò |
|------|---------|
| `ExchangeClient.java` | **Interface** chung cho mọi adapter sàn. Hai method: `supports(type)` — adapter này hỗ trợ loại gì; `fetchCandles(...)` — lấy dữ liệu nến. Thêm sàn mới = thêm 1 class implement interface này. |
| `BinanceClient.java` | Adapter cho **Binance** (crypto). Gọi public API `/api/v3/klines` — **không cần API key**. Retry 3 lần với backoff (1s, 2s, 3s) khi lỗi. Parse response JSON array thành `PriceCandle`. Bật mặc định qua config `app.collector.binance.enabled=true`. |
| `AlphaVantageClient.java` | Adapter cho **Alpha Vantage** (stock). Cần API key (free tier 25 requests/ngày). Gọi `TIME_SERIES_DAILY`. **Tắt mặc định** — bật qua `app.collector.alpha-vantage.enabled=true` trong `application.yml`. |
| `DataCollectorService.java` | **Orchestrator.** Hai scheduled job: crypto chạy **mỗi giờ** (phút :05), stock chạy **mỗi ngày** (18:00 thứ 2-6). Lỗi 1 instrument không ảnh hưởng instrument khác (try-catch từng mã). |

### 3.10 Exception Layer — xử lý lỗi tập trung

| File | HTTP Status | Khi nào dùng |
|------|-------------|--------------|
| `ResourceNotFoundException.java` | 404 | Tìm user/instrument/candle không tồn tại. |
| `DuplicateResourceException.java` | 409 | Email đã đăng ký, symbol instrument đã tồn tại. |
| `ExternalApiException.java` | 502 | Binance/Alpha Vantage API lỗi sau khi retry. |
| `GlobalExceptionHandler.java` | — | `@RestControllerAdvice` bắt tất cả exception trên, trả JSON format thống nhất: `{timestamp, status, error, message}`. Cũng xử lý validation errors (400). |

---

## 4. Sơ đồ kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT                                │
│              (Postman / Frontend / curl)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP request
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter  →  SecurityConfig (phân quyền)     │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  Controller Layer                                            │
│  AuthController │ UserController │ InstrumentController       │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  Service Layer                                               │
│  AuthService │ InstrumentService │ DataCollectorService       │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  Repository Layer                                            │
│  UserRepo │ InstrumentRepo │ PriceCandleRepo                 │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│  PostgreSQL 16  (Docker)                                     │
│  users │ instruments │ price_candles                          │
└──────────────────────────────────────────────────────────────┘

         ┌─────────────────────────────────┐
         │  @Scheduled (cron)              │
         │  DataCollectorService           │
         │    → BinanceClient (crypto)     │
         │    → AlphaVantageClient (stock) │
         └─────────────┬───────────────────┘
                       │ Fetch OHLCV
                       ▼
              Binance / Alpha Vantage API
```

---

## 5. Quyết định thiết kế quan trọng

| Quyết định | Lý do |
|------------|-------|
| **Flyway** thay vì `ddl-auto=update` | Kiểm soát schema rõ ràng, có version history, rollback được. Trong team thực tế luôn dùng migration tool. |
| **BigDecimal** cho giá, không dùng `double` | Tránh lỗi floating-point. Crypto có giá rất nhỏ (0.00000001) — `double` sẽ mất precision. |
| **Keyset pagination** thay vì OFFSET | `price_candles` sẽ có hàng triệu row. OFFSET chậm tỷ lệ thuận với số trang, keyset luôn O(1). |
| **Soft delete** instruments | `price_candles` có FK trỏ tới `instruments`. Xóa cứng sẽ lỗi FK constraint hoặc mất dữ liệu lịch sử. |
| **`@ConditionalOnProperty`** cho adapter | Bật/tắt Binance hoặc Alpha Vantage qua config mà không cần sửa code. |
| **Access token / Refresh token** tách biệt | Access token sống ngắn (15 phút) giảm rủi ro bị đánh cắp. Refresh token sống dài (7 ngày) để UX không phải login liên tục. |
| **Java record** cho DTO | Immutable, code ngắn gọn, không cần Lombok cho DTO. |

---

## 6. Phase tiếp theo sẽ dùng gì từ Phase 0?

| Phase | Dùng từ Phase 0 |
|-------|-----------------|
| **Phase 1** — Indicator Engine | `PriceCandleRepository.findCandlesBetween()` để lấy dữ liệu tính RSI, MA, MACD. |
| **Phase 2** — Signal Engine | Entity `Instrument` + indicator data → sinh `trading_signals`. |
| **Phase 3** — Virtual Wallet & Trade Log | `PriceCandleRepository.findClosestCandle()` để tra giá thật khi mở/đóng lệnh. `User` entity để liên kết ví ảo. |
| **Phase 4** — Backtesting | Toàn bộ `price_candles` data pipeline là nền tảng. |
