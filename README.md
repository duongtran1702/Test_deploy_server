# 🚀 Production-Grade Event-Driven Microservices Architecture
### Spring Boot 4.x • Java 21 LTS • Gradle 8.14.6 • Apache Kafka (KRaft) • Resilience4j • React Dashboard

---

## 📌 1. Bảng tóm tắt Thư viện & Công nghệ của 2 Microservices
> **Tập trung chi tiết: Tên thư viện / công nghệ • Nơi sử dụng trong code • Mục đích giải quyết**

### 📦 Dịch vụ 1: `order-service` (Port: 8081)

| Thư viện / Công nghệ | Vị trí sử dụng trong source code | Mục đích giải quyết |
| :--- | :--- | :--- |
| **Spring Web** (`spring-boot-starter-web`) | • `OrderController.java`<br>• `DltController.java`<br>• `WebConfig.java` | • Cung cấp REST API tạo đơn (`POST /api/orders`), truy vấn đơn hàng, batch testing.<br>• Quản lý Dead Letter Queue và replay message.<br>• Cấu hình CORS và Filter chuỗi request. |
| **Spring Kafka** (`spring-kafka`) | • `KafkaTopicConfig.java`<br>• `KafkaProducerService.java`<br>• `PaymentEventListener.java` | • **Tự tạo Topic**: Tạo `orders.v1`, `payments.v1`, `orders.v1.DLT` (3 partitions).<br>• **Producer**: Bắn `OrderEvent` bất đồng bộ vào topic `orders.v1` kèm Header `x-correlation-id`.<br>• **Consumer**: Lắng nghe `payments.v1` để cập nhật trạng thái đơn hàng (`CONFIRMED`/`FAILED`).<br>• **DLT Consumer**: Lắng nghe `orders.v1.DLT` để phát hiện và lưu vết message bị lỗi. |
| **Resilience4j Circuit Breaker** (`resilience4j-spring-boot3`) | • `PaymentRestClient.java` (`@CircuitBreaker`)<br>• `application.yml` | • Ngắt kết nối đồng bộ sang Payment Service khi tỷ lệ lỗi vượt quá 50% (chuyển sang trạng thái `OPEN`).<br>• Ngăn chặn hiện tượng nghẽn thread pool (Cascading Failure).<br>• Kích hoạt hàm Fallback `paymentFallback()` để phục vụ người dùng mà không gây sập service. |
| **Resilience4j Retry & RateLimiter** | • `PaymentRestClient.java` (`@Retry`, `@RateLimiter`) | • **Retry**: Tự động thử lại gọi HTTP tối đa 3 lần với khoảng chờ tăng dần.<br>• **RateLimiter**: Giới hạn tần suất gọi tối đa 20 request/giây để chống quá tải. |
| **Spring Boot Actuator** (`spring-boot-starter-actuator`) | • `application.yml`<br>• Endpoint `/actuator/circuitbreakers`<br>• Endpoint `/actuator/health` | • Thu thập và expose metrics trạng thái Circuit Breaker thời gian thực cho Client UI.<br>• Cung cấp Kubernetes Liveness/Readiness probes (`probes.enabled: true`). |
| **Jakarta / Spring Validation** (`spring-boot-starter-validation`) | • `OrderRequest.java` (`@NotBlank`, `@DecimalMin`)<br>• `OrderController.java` (`@Valid`) | • Rà soát tính hợp lệ của dữ liệu đầu vào (tên khách hàng không được rỗng, số tiền đơn hàng phải > 0). |
| **Global Exception Handler** (`@RestControllerAdvice`) | • `GlobalExceptionHandler.java` | • Bắt lỗi tập trung (Validation, `CallNotPermittedException` khi Circuit Breaker OPEN, `RequestNotPermitted` khi Rate Limit vượt ngưỡng), chuẩn hóa response JSON theo chuẩn RFC 7807 (`ApiErrorResponse`). |
| **Springdoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`) | • `OpenApiConfig.java`<br>• `OrderController.java` | • Tự động sinh tài liệu API chuẩn OpenAPI 3.0 và giao diện kiểm thử tương tác tại `/swagger-ui.html`. |
| **SLF4J MDC (Mapped Diagnostic Context)** | • `WebConfig.java` (`correlationIdFilter`) | • Gán mã `X-Correlation-Id` vào context logging của mọi thread, cho phép Distributed Tracing xuyên suốt các service. |
| **Jackson Serializer** (`JacksonJsonSerializer` / `JacksonJsonDeserializer`) | • `application.yml` | • Tuần tự hóa đối tượng Java DTO thành binary JSON khi gửi qua Kafka và ngược lại. |

---

### 💳 Dịch vụ 2: `payment-service` (Port: 8082)

| Thư viện / Công nghệ | Vị trí sử dụng trong source code | Mục đích giải quyết |
| :--- | :--- | :--- |
| **Spring Kafka Consumer & Multi-Threading** | • `KafkaConsumerConfig.java`<br>• `OrderEventListener.java` (`@KafkaListener`) | • Tiêu thụ sự kiện `OrderEvent` từ topic `orders.v1`.<br>• Cấu hình `concurrency = 3` để đọc song song 3 partitions cùng lúc. |
| **Kafka DefaultErrorHandler & Exponential BackOff** | • `KafkaConsumerConfig.java` (`DefaultErrorHandler`) | • **Cơ chế chịu lỗi**: Tự động bắt lỗi runtime khi xử lý event và retry lại 3 lần với thời gian chờ tăng gấp đôi (500ms → 1000ms → 2000ms) trước khi bỏ cuộc. |
| **Kafka DeadLetterPublishingRecoverer (DLT)** | • `KafkaConsumerConfig.java` (`DeadLetterPublishingRecoverer`) | • **Xử lý Poison Pill**: Khi message bị lỗi sau 3 lần retry hoặc gặp lỗi dữ liệu không thể sửa, recoverer tự động định tuyến message vào topic `orders.v1.DLT` kèm thông tin lỗi, tránh làm tắc nghẽn hàng đợi (Head-of-Line Blocking). |
| **Spring Kafka Producer** (`KafkaTemplate`) | • `PaymentProcessorService.java` | • Xuất bản sự kiện kết quả thanh toán `PaymentResultEvent` vào topic `payments.v1` để thông báo cho `order-service`. |
| **Consumer Idempotency Engine** (Thread-Safe Sets) | • `IdempotencyService.java`<br>• `PaymentProcessorService.java` | • **Chống xử lý trùng lặp**: Lưu giữ các `orderId` đã xử lý thành công. Khi Kafka gửi lại message (do mạng lag lúc commit offset), service phát hiện và bỏ qua, không trừ tiền 2 lần (đảm bảo Exactly-Once Semantics). |
| **Chaos Engineering Fault Injection** | • `ChaosService.java`<br>• `ChaosController.java` | • Cung cấp động cơ bơm lỗi có thể tùy chỉnh từ Web UI: bơm độ trễ giả lập (`Thread.sleep`), tăng % lỗi ngẫu nhiên, kích hoạt lỗi sập Gateway 503, và ép lỗi Poison Pill để phục vụ kiểm thử khả năng chịu lỗi. |
| **Spring Web & Direct Testing API** | • `PaymentController.java` (`POST /api/payment/process-direct`) | • Cung cấp endpoint REST đồng bộ để `order-service` gọi sang, dùng làm kịch bản kiểm thử việc kích hoạt Circuit Breaker khi có sự cố. |
| **Spring Boot Actuator** (`spring-boot-starter-actuator`) | • `application.yml` | • Cung cấp `/actuator/health` (Kubernetes Probes) và `/actuator/metrics`. |
| **Springdoc OpenAPI** | • `OpenApiConfig.java`<br>• `PaymentController.java`, `ChaosController.java` | • Tài liệu Swagger UI cho các endpoint thanh toán và điều khiển Chaos tại `/swagger-ui.html`. |

---

## 🏛️ 2. Sơ đồ tương tác giữa 2 Service

```
                            [React Testing UI (:3000)]
                                    |         \
                     REST (Direct/Sync)        \ REST (Query & Simulate)
                                    |           \
                                    v            v
        +-----------------------------------+   +------------------------------------+
        |      ORDER-SERVICE (:8081)        |   |      PAYMENT-SERVICE (:8082)       |
        | - Spring Boot 4 / Java 21         |   | - Spring Boot 4 / Java 21          |
        | - Order Lifecycle & Outbox        |   | - Idempotency Deduplication Engine |
        | - Resilience4j Circuit Breaker    |   | - Chaos Fault Injection Sandbox    |
        | - Rate Limiter & Fallback         |   | - Dead Letter Recoverer (DLT)      |
        | - MDC Distributed Tracing         |   | - Exponential Retry Backoff        |
        +-----------------------------------+   +------------------------------------+
                    \                ^                       ^               /
          Produces   \        Consumes\             Consumes/        Produces/
           orders.v1  \       payments.v1\         orders.v1\    payments.v1/
                       v                  \                 /              v
        +----------------------------------------------------------------------------+
        |                       APACHE KAFKA CLUSTER (KRaft Mode)                    |
        |  - Topic: orders.v1 (3 partitions)                                         |
        |  - Topic: payments.v1 (3 partitions)                                       |
        |  - Topic: orders.v1.DLT (Dead Letter Topic - 1 partition)                  |
        +----------------------------------------------------------------------------+
                                              |
                                              v
                                [Kafka UI Console (:8090)]
```

---

## 📁 3. Cấu trúc thư mục dự án

```
d:\Research\
├── docker-compose.yml              # Khởi động Kafka KRaft, Kafka UI, Order Service, Payment Service
├── README.md                       # Tài liệu kiến trúc và hướng dẫn
├── server/                         # Backend Microservices (Java 21, Gradle 8.14.6)
│   ├── gradlew.bat / gradlew       # Gradle Wrapper
│   ├── settings.gradle
│   ├── build.gradle                # Spring Boot 4.0.8, Java 21 LTS
│   ├── common-lib/                 # Thư viện DTOs và Models dùng chung
│   │   └── src/main/java/com/production/common/
│   ├── order-service/              # Microservice Order (:8081)
│   │   ├── Dockerfile
│   │   ├── build.gradle
│   │   └── src/main/java/com/production/order/
│   └── payment-service/            # Microservice Payment & Chaos Engine (:8082)
│       ├── Dockerfile
│       ├── build.gradle
│       └── src/main/java/com/production/payment/
└── client/                         # Frontend React Dashboard (Vite 5, Glassmorphism UI)
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx                 # Dashboard chính, điều phối tab và realtime refresh
        ├── api.js                  # Tầng kết nối REST tới các microservices & Actuator
        ├── index.css               # Modern Design System (Glassmorphism, Dark Mode)
        └── components/
            ├── Navbar.jsx          # Live Status indicator, Circuit Breaker state
            ├── MetricsCards.jsx    # Thống kê trực quan
            ├── OrderSimulator.jsx  # Tạo đơn lẻ, nạp tải Batch 5 / Batch 10 Chaos
            ├── OrderTable.jsx      # Bảng đơn hàng thời gian thực, test CB trực tiếp
            ├── OrderDetailModal.jsx# Trace order, Kafka event flow & JSON payload
            ├── ChaosControl.jsx    # Phòng thí nghiệm Chaos: Chỉnh % lỗi, độ trễ, 503
            ├── DltInspector.jsx    # Soi Dead Letter Topic và nút Replay về Kafka
            └── ArchitectureModal.jsx
```

---

## 🚀 4. Hướng dẫn chạy dự án

### Cách 1: Chạy toàn bộ với Docker Compose (Khuyên dùng)
```powershell
# 1. Đóng gói các microservices:
cd d:\Research\server
.\gradlew.bat bootJar -x test
cd d:\Research

# 2. Khởi chạy toàn bộ hệ thống bằng Docker Compose:
docker compose up --build -d

# 3. Khởi chạy client React:
cd d:\Research\client
npm install
npm run dev
```

---

### Cách 2: Chạy cục bộ bằng Gradle & Terminal

#### Bước 1: Khởi động Kafka KRaft
```powershell
docker run -d --name local-kafka -p 9092:9092 -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller -e CLUSTER_ID="4L622nShTUiBenVznRAALA" -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 apache/kafka:latest
```

#### Bước 2: Chạy Order Service (Terminal 1)
```powershell
cd d:\Research\server
.\gradlew.bat :order-service:bootRun
```
*Port:* `http://localhost:8081`

#### Bước 3: Chạy Payment Service (Terminal 2)
```powershell
cd d:\Research\server
.\gradlew.bat :payment-service:bootRun
```
*Port:* `http://localhost:8082`

#### Bước 4: Chạy React Client Dashboard (Terminal 3)
```powershell
cd d:\Research\client
npm run dev
```
*Port:* `http://localhost:3000`

---

## 🧪 5. Kịch bản Test Chịu Lỗi & Chaos Engineering trên React UI

### Kịch bản 1: Luồng chuẩn Happy Path (Event-Driven)
1. Mở `http://localhost:3000`
2. Nhấn nút **"Batch 5 (Healthy)"** hoặc nhập thông tin và nhấn **"Produce to Kafka"**.
3. Quan sát:
   - Order Service nhận request HTTP, lưu trạng thái `PENDING`.
   - Sự kiện bắn vào Kafka topic `orders.v1`.
   - Payment Service tiêu thụ sự kiện, trừ tiền, bắn kết quả vào `payments.v1`.
   - Trạng thái đơn hàng tự động chuyển sang `CONFIRMED` màu xanh.

### Kịch bản 2: Test Dead Letter Topic (DLT) & Phục hồi (Replay)
1. Ở form tạo order, tick chọn **"Poison Pill (Forces DLT Routing)"**.
2. Nhấn **"Produce to Kafka"**.
3. Sự kiện bị lỗi unrecoverable. Consumer Kafka tự động chuyển message vào topic `orders.v1.DLT`.
4. Chuyển sang tab **"Dead Letter Topic"** trên menu:
   - Bạn sẽ thấy ngay message bị cách ly tại đây cùng nguyên nhân lỗi.
   - Nhấn nút **"Replay to Kafka"**: Message sẽ được bắn lại vào `orders.v1` với cờ poison đã được xóa để hệ thống tự phục hồi!

### Kịch bản 3: Test Circuit Breaker & Fallback (Resilience4j)
1. Chuyển sang tab **"Chaos Engineering Lab"**.
2. Tick chọn **"Simulate Payment Gateway 503 Outage (Trips Circuit Breaker)"** và nhấn **"Apply Chaos Rules"**.
3. Quay lại danh sách đơn hàng, click nút **"Test CB"** ở một đơn bất kỳ:
   - Gọi đồng bộ sang Payment Service bị lỗi 503.
   - Sau vài lần thất bại liên tiếp, Circuit Breaker chuyển trạng thái từ `CLOSED` 🟢 sang `OPEN` 🔴.
   - Response trả về thông báo Fallback an toàn: `FALLBACK_ACCEPTED: Queued for asynchronous reconciliation` thay vì làm sập ứng dụng!

---

## 🌐 6. Địa chỉ các cổng dịch vụ (Endpoints)

| Thành phần | URL | Mô tả |
| :--- | :--- | :--- |
| **React Tester Console** | `http://localhost:3000` | Giao diện kiểm thử, điều khiển Chaos & Telemetry |
| **Order Service API** | `http://localhost:8081` | Microservice quản lý đơn hàng & Producer |
| **Order Swagger UI** | `http://localhost:8081/swagger-ui.html` | Tài liệu OpenAPI của Order Service |
| **Order Actuator** | `http://localhost:8081/actuator` | Health, Metrics, Circuit Breakers |
| **Payment Service API** | `http://localhost:8082` | Microservice xử lý thanh toán & Consumer |
| **Payment Swagger UI** | `http://localhost:8082/swagger-ui.html` | Tài liệu OpenAPI của Payment Service |
| **Payment Actuator** | `http://localhost:8082/actuator` | Health & Metrics của Payment Service |
| **Kafka UI (Web)** | `http://localhost:8090` | Xem topic, partition, message offset trong trình duyệt |
| **Kafka Broker** | `localhost:9092` | Cổng kết nối Apache Kafka KRaft |
