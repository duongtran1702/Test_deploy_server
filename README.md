# 📡 Danh Sách API Kiểm Thử Trên VPS (Không Dùng Domain)

> Tài liệu này liệt kê toàn bộ các REST API được triển khai trực tiếp trên VPS qua địa chỉ IP và Port để phục vụ việc kiểm thử qua **cURL**, **Postman** hoặc **Trình duyệt**.

---

## 📌 1. Thông Tin Kết Nối Trực Tiếp (Base URL)

| Thành phần | Địa chỉ IP / Port (Chưa dùng Domain) | Ghi chú |
| :--- | :--- | :--- |
| **IP VPS** | `221.121.4.77` | *(Thay đổi nếu bạn đổi IP VPS)* |
| **Order Service API** | `http://221.121.4.77:8081` | Quản lý vòng đời đơn hàng, Producer Kafka, Circuit Breaker |
| **Payment Service API** | `http://221.121.4.77:8082` | Xử lý thanh toán, Consumer Kafka, Bơm lỗi Chaos Lab |
| **Kafka UI (Web Dashboard)** | `http://221.121.4.77:8090` | Xem trực quan Topic `orders.v1`, `payments.v1`, `orders.v1.DLT` |
| **Order Swagger UI** | `http://221.121.4.77:8081/swagger-ui.html` | Xem và chạy thử trực tiếp API Order trên trình duyệt |
| **Payment Swagger UI** | `http://221.121.4.77:8082/swagger-ui.html` | Xem và chạy thử trực tiếp API Payment trên trình duyệt |

> ⚠️ **Lưu ý Firewall trên VPS:** Nếu không gọi được vào các port trên, hãy SSH vào VPS và mở port:
> ```bash
> sudo ufw allow 8081/tcp
> sudo ufw allow 8082/tcp
> sudo ufw allow 8090/tcp
> sudo ufw reload
> ```

---

## 📋 2. Bảng Tổng Hợp Endpoint

### A. Order Service (Port 8081)
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/orders` | Tạo 1 đơn hàng mới (bắn Event vào Kafka `orders.v1`) |
| `GET` | `/api/orders` | Lấy danh sách toàn bộ đơn hàng |
| `GET` | `/api/orders/{id}` | Lấy chi tiết đơn hàng theo `id` |
| `PUT` / `POST` | `/api/orders/{id}/cancel` | Hủy đơn hàng |
| `PUT` / `POST` | `/api/orders/{id}/ship` | Đánh dấu đơn hàng đã gửi hàng |
| `POST` | `/api/orders/{id}/test-sync-payment` | Test gọi đồng bộ sang Payment Service qua Circuit Breaker |
| `POST` | `/api/orders/batch?count=5&failurePercent=0` | Tạo hàng loạt đơn hàng mẫu để test tải / test lỗi |
| `DELETE` | `/api/orders` | Xóa toàn bộ danh sách đơn hàng trong RAM |
| `GET` | `/api/orders/metrics` | Thống kê số lượng đơn theo trạng thái |
| `GET` | `/api/orders/dlt` | Danh sách message bị kẹt trong Dead Letter Topic |
| `POST` | `/api/orders/dlt/replay/{orderId}` | Replay (bắn lại) đơn từ DLT về Kafka `orders.v1` |
| `GET` | `/actuator/health` | Kiểm tra tình trạng sống (Health probe) |
| `GET` | `/actuator/circuitbreakers` | Xem trạng thái mạch Resilience4j (`CLOSED`, `OPEN`, `HALF_OPEN`) |
| `GET` | `/actuator/circuitbreakerevents` | Lịch sử các lần đóng / mở mạch |

---

### B. Payment Service (Port 8082)
| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `POST` | `/api/payment/process-direct` | Xử lý thanh toán đồng bộ trực tiếp (cho test Circuit Breaker) |
| `GET` | `/api/payment/transactions` | Lấy danh sách tất cả giao dịch thanh toán |
| `DELETE` | `/api/payment/transactions` | Xóa sạch lịch sử giao dịch và reset cache chống trùng lặp |
| `GET` | `/api/payment/metrics` | Thống kê giao dịch (thành công, thất bại, idempotent skips) |
| `GET` | `/api/chaos/config` | Xem cấu hình bơm lỗi Chaos hiện tại |
| `POST` | `/api/chaos/config` | Cập nhật cấu hình bơm lỗi (độ trễ, tỷ lệ lỗi %, làm sập gateway) |
| `GET` | `/actuator/health` | Kiểm tra tình trạng sống của Payment Service |

---

## 🛠️ 3. Chi Tiết API & Lệnh cURL Kiểm Thử

### 📦 PHẦN 1: ORDER SERVICE (`:8081`)

#### 1.1. Tạo đơn hàng mới (Happy Path)
- **Endpoint:** `POST http://221.121.4.77:8081/api/orders`
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8081/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "item": "MacBook Pro M3 Max",
    "amount": 3499.00,
    "simulateFailure": false,
    "simulatePoisonPill": false
  }'
```
- **Response mẫu (`201 Created`):**
```json
{
  "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "CUST-001",
  "item": "MacBook Pro M3 Max",
  "amount": 3499.00,
  "status": "PENDING",
  "createdAt": "2026-10-15T01:10:00Z",
  "correlationId": "ORDER-1760490600000"
}
```

---

#### 1.2. Tạo đơn cố tình gây lỗi Poison Pill (Đẩy vào DLT)
- **Endpoint:** `POST http://221.121.4.77:8081/api/orders`
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8081/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-POISON",
    "item": "Toxic Payload Item",
    "amount": 99.00,
    "simulateFailure": true,
    "simulatePoisonPill": true
  }'
```

---

#### 1.3. Lấy danh sách toàn bộ đơn hàng
- **Endpoint:** `GET http://221.121.4.77:8081/api/orders`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8081/api/orders"
```

---

#### 1.4. Xem chi tiết 1 đơn hàng theo ID
- **Endpoint:** `GET http://221.121.4.77:8081/api/orders/{orderId}`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8081/api/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6"
```

---

#### 1.5. Hủy đơn hàng
- **Endpoint:** `PUT http://221.121.4.77:8081/api/orders/{orderId}/cancel`
- **cURL:**
```bash
curl -X PUT "http://221.121.4.77:8081/api/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6/cancel"
```

---

#### 1.6. Đánh dấu đơn hàng đã giao (Ship)
- **Endpoint:** `PUT http://221.121.4.77:8081/api/orders/{orderId}/ship`
- **cURL:**
```bash
curl -X PUT "http://221.121.4.77:8081/api/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6/ship"
```

---

#### 1.7. Test gọi REST đồng bộ & Kích hoạt Circuit Breaker
- **Endpoint:** `POST http://221.121.4.77:8081/api/orders/{orderId}/test-sync-payment`
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8081/api/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6/test-sync-payment"
```
- **Response khi Circuit Breaker OPEN (Fallback an toàn):**
```json
{
  "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "FALLBACK_ACCEPTED",
  "message": "Payment service unavailable. Order queued for asynchronous reconciliation via Kafka fallback."
}
```

---

#### 1.8. Bắn tải hàng loạt đơn hàng mẫu (Batch Testing)
- **Endpoint:** `POST http://221.121.4.77:8081/api/orders/batch?count=5&failurePercent=20`
- **Tham số URL:**
  - `count`: Số lượng đơn muốn tạo (mặc định 5)
  - `failurePercent`: Tỷ lệ % muốn tạo đơn lỗi (mặc định 0)
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8081/api/orders/batch?count=10&failurePercent=30"
```

---

#### 1.9. Xem danh sách đơn trong Dead Letter Topic (DLT)
- **Endpoint:** `GET http://221.121.4.77:8081/api/orders/dlt`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8081/api/orders/dlt"
```

---

#### 1.10. Replay (Bắn lại) đơn hàng bị kẹt từ DLT về Kafka
- **Endpoint:** `POST http://221.121.4.77:8081/api/orders/dlt/replay/{orderId}`
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8081/api/orders/dlt/replay/3fa85f64-5717-4562-b3fc-2c963f66afa6"
```

---

#### 1.11. Xem thống kê Order Metrics
- **Endpoint:** `GET http://221.121.4.77:8081/api/orders/metrics`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8081/api/orders/metrics"
```

---

#### 1.12. Xóa toàn bộ đơn hàng để làm mới môi trường test
- **Endpoint:** `DELETE http://221.121.4.77:8081/api/orders`
- **cURL:**
```bash
curl -X DELETE "http://221.121.4.77:8081/api/orders"
```

---

#### 1.13. Kiểm tra Circuit Breaker & Health (Actuator)
- **Xem trạng thái Circuit Breaker:**
```bash
curl -X GET "http://221.121.4.77:8081/actuator/circuitbreakers"
```
- **Xem Health Probe:**
```bash
curl -X GET "http://221.121.4.77:8081/actuator/health"
```

---

### 💳 PHẦN 2: PAYMENT SERVICE (`:8082`)

#### 2.1. Gọi thanh toán trực tiếp qua REST
- **Endpoint:** `POST http://221.121.4.77:8082/api/payment/process-direct`
- **cURL:**
```bash
curl -X POST "http://221.121.4.77:8082/api/payment/process-direct" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "DIRECT-TEST-001",
    "customerId": "CUST-001",
    "amount": 250.0
  }'
```

---

#### 2.2. Lấy danh sách toàn bộ giao dịch thanh toán
- **Endpoint:** `GET http://221.121.4.77:8082/api/payment/transactions`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8082/api/payment/transactions"
```

---

#### 2.3. Xem thống kê giao dịch thanh toán
- **Endpoint:** `GET http://221.121.4.77:8082/api/payment/metrics`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8082/api/payment/metrics"
```

---

#### 2.4. Xóa toàn bộ lịch sử thanh toán & Reset Idempotency Cache
- **Endpoint:** `DELETE http://221.121.4.77:8082/api/payment/transactions`
- **cURL:**
```bash
curl -X DELETE "http://221.121.4.77:8082/api/payment/transactions"
```

---

#### 2.5. Xem cấu hình phòng thí nghiệm Chaos
- **Endpoint:** `GET http://221.121.4.77:8082/api/chaos/config`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8082/api/chaos/config"
```

---

#### 2.6. Cập nhật cấu hình Chaos (Bơm lỗi kiểm thử)
- **Endpoint:** `POST http://221.121.4.77:8082/api/chaos/config`
- **Các trường cấu hình:**
  - `failureRatePercent`: Tỷ lệ % giao dịch bị lỗi (0 - 100)
  - `simulatedLatencyMs`: Giả lập độ trễ mạng (vd: 3000ms)
  - `simulateGatewayDown`: Giả lập Payment Gateway bị sập hoàn toàn (trả về lỗi HTTP 503)
  - `forceDLTRouting`: Buộc mọi message đẩy thẳng vào Dead Letter Topic
- **cURL mẫu: Kích hoạt sập Gateway để kiểm tra Circuit Breaker:**
```bash
curl -X POST "http://221.121.4.77:8082/api/chaos/config" \
  -H "Content-Type: application/json" \
  -d '{
    "failureRatePercent": 0,
    "simulatedLatencyMs": 0,
    "simulateGatewayDown": true,
    "forceDLTRouting": false
  }'
```
- **cURL mẫu: Reset Chaos về bình thường:**
```bash
curl -X POST "http://221.121.4.77:8082/api/chaos/config" \
  -H "Content-Type: application/json" \
  -d '{
    "failureRatePercent": 0,
    "simulatedLatencyMs": 0,
    "simulateGatewayDown": false,
    "forceDLTRouting": false
  }'
```

---

#### 2.7. Kiểm tra Health của Payment Service
- **Endpoint:** `GET http://221.121.4.77:8082/actuator/health`
- **cURL:**
```bash
curl -X GET "http://221.121.4.77:8082/actuator/health"
```

---

## 🚀 4. Kịch Bản Kiểm Thử Nhanh (Copy & Chạy Liền)

### Kịch bản 1: Luồng chuẩn (Happy Path)
1. **Tạo đơn hàng:**
   ```bash
   curl -s -X POST "http://221.121.4.77:8081/api/orders" \
     -H "Content-Type: application/json" \
     -d '{"customerId":"Tester","item":"iPhone 16 Pro","amount":1200,"simulateFailure":false,"simulatePoisonPill":false}'
   ```
2. **Kiểm tra trạng thái đơn (Sau 1s Kafka tiêu thụ sự kiện):**
   ```bash
   curl -s "http://221.121.4.77:8081/api/orders" | grep "CONFIRMED"
   ```

---

### Kịch bản 2: Test Dead Letter Topic (DLT) & Phục Hồi (Replay)
1. **Tạo đơn hàng độc (Poison Pill):**
   ```bash
   curl -s -X POST "http://221.121.4.77:8081/api/orders" \
     -H "Content-Type: application/json" \
     -d '{"customerId":"Attacker","item":"Poison Item","amount":50,"simulateFailure":true,"simulatePoisonPill":true}'
   ```
2. **Xem danh sách đơn bị cách ly trong DLT:**
   ```bash
   curl -s "http://221.121.4.77:8081/api/orders/dlt"
   ```
3. **Replay đơn hàng (Thay `<ORDER_ID>` bằng ID lấy từ bước 2):**
   ```bash
   curl -X POST "http://221.121.4.77:8081/api/orders/dlt/replay/<ORDER_ID>"
   ```

---

### Kịch bản 3: Test Circuit Breaker & Fallback
1. **Kích hoạt Gateway sập (HTTP 503):**
   ```bash
   curl -X POST "http://221.121.4.77:8082/api/chaos/config" \
     -H "Content-Type: application/json" \
     -d '{"failureRatePercent":0,"simulatedLatencyMs":0,"simulateGatewayDown":true,"forceDLTRouting":false}'
   ```
2. **Tạo một đơn hàng để lấy ID:**
   ```bash
   curl -s -X POST "http://221.121.4.77:8081/api/orders" \
     -H "Content-Type: application/json" \
     -d '{"customerId":"CB-Tester","item":"Test CB Item","amount":100,"simulateFailure":false,"simulatePoisonPill":false}'
   ```
3. **Gọi liên tiếp 4-5 lần test sync payment với ID vừa tạo để kích hoạt ngắt mạch:**
   ```bash
   curl -X POST "http://221.121.4.77:8081/api/orders/<ORDER_ID>/test-sync-payment"
   ```
4. **Kiểm tra trạng thái Circuit Breaker chuyển sang `OPEN`:**
   ```bash
   curl -s "http://221.121.4.77:8081/actuator/circuitbreakers"
   ```
5. **Khôi phục lại hệ thống:**
   ```bash
   curl -X POST "http://221.121.4.77:8082/api/chaos/config" \
     -H "Content-Type: application/json" \
     -d '{"failureRatePercent":0,"simulatedLatencyMs":0,"simulateGatewayDown":false,"forceDLTRouting":false}'
   ```
