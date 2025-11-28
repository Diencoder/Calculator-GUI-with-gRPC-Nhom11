# Configuration Management Guide

## Tổng Quan

Dự án đã được refactor để sử dụng file cấu hình `application.properties` thay vì hard-coded values. Điều này giúp:
- Dễ dàng thay đổi cấu hình mà không cần rebuild
- Quản lý cấu hình tập trung
- Hỗ trợ nhiều môi trường (dev, test, production)

## File Cấu Hình

File cấu hình chính: `src/main/resources/application.properties`

### Cấu Trúc

```properties
# Server Configuration
calculator.server.host=localhost
calculator.server.port=9090
calculator.server.shutdown.timeout.seconds=30

# Client Configuration
calculator.client.host=localhost
calculator.client.port=9090
calculator.client.request.timeout.seconds=10
calculator.client.connection.timeout.seconds=5
calculator.client.max.retry.attempts=3
calculator.client.retry.delay.millis=500
calculator.client.keepalive.time.seconds=30
calculator.client.keepalive.timeout.seconds=5

# GUI Configuration
calculator.gui.history.max.entries=100
calculator.gui.window.width=1200
calculator.gui.window.height=750
calculator.gui.display.font.size=48

# Validation Configuration
calculator.validation.max.safe.value=1.0E15
calculator.validation.min.safe.value=-1.0E15
calculator.validation.max.exponent=1000

# Logging Configuration
calculator.logging.history.file=logs/calculation-history.log
calculator.logging.level=INFO
```

## Sử Dụng

### 1. Thay Đổi Cấu Hình

Chỉ cần chỉnh sửa file `application.properties` và restart ứng dụng:

```properties
# Ví dụ: Thay đổi port server
calculator.server.port=9091

# Ví dụ: Tăng timeout
calculator.client.request.timeout.seconds=20

# Ví dụ: Tăng số lượng lịch sử
calculator.gui.history.max.entries=200
```

### 2. Trong Code

Sử dụng `AppConfig` để đọc cấu hình:

```java
import com.calculator.calculatorguiwithgrpc.config.AppConfig;

AppConfig config = AppConfig.getInstance();

// Đọc giá trị
String host = config.getServerHost();
int port = config.getServerPort();
int maxHistory = config.getGuiHistoryMaxEntries();
```

### 3. Default Values

Nếu file config không tồn tại hoặc thiếu key, hệ thống sẽ sử dụng giá trị mặc định:
- Server port: 9090
- Client timeout: 10 seconds
- Max history entries: 100
- etc.

## Các Class Đã Được Refactor

### 1. `AppConfig` (Mới)
- Class quản lý cấu hình singleton
- Đọc từ `application.properties`
- Cung cấp getters cho tất cả config values

### 2. `CalculatorClientConfig`
- Builder pattern giữ nguyên
- Default values giờ đọc từ `AppConfig` thay vì hard-coded

### 3. `CalculatorServer`
- Port đọc từ config
- Shutdown timeout đọc từ config
- Có thể override port qua constructor

### 4. `CalculatorClient`
- KeepAlive settings đọc từ config
- Tất cả timeouts đọc từ config

### 5. `CalculatorGUI`
- Window size đọc từ config
- Max history entries đọc từ config
- Display font size đọc từ config

### 6. `CalculatorInputValidator`
- Validation limits đọc từ config
- Max safe value, min safe value, max exponent

### 7. `CalculationHistoryLogger`
- Log file path đọc từ config

## Ví Dụ Sử Dụng

### Thay Đổi Port Server

1. Mở `src/main/resources/application.properties`
2. Thay đổi:
   ```properties
   calculator.server.port=9091
   calculator.client.port=9091
   ```
3. Restart server và client

### Tăng Timeout Cho Client

```properties
calculator.client.request.timeout.seconds=30
calculator.client.connection.timeout.seconds=10
```

### Tăng Số Lượng Lịch Sử

```properties
calculator.gui.history.max.entries=500
```

### Thay Đổi Validation Limits

```properties
calculator.validation.max.safe.value=1.0E20
calculator.validation.min.safe.value=-1.0E20
calculator.validation.max.exponent=2000
```

## Lưu Ý

1. **File Location**: File config phải ở `src/main/resources/application.properties`
2. **Format**: Sử dụng format `key=value`, không có spaces quanh dấu `=`
3. **Comments**: Sử dụng `#` để comment
4. **Type Safety**: `AppConfig` tự động parse types (int, long, double, boolean)
5. **Error Handling**: Nếu giá trị không hợp lệ, sẽ sử dụng default value và log warning

## Testing

Để test với cấu hình khác, có thể:
1. Tạo file `application-test.properties`
2. Load trong test code:
   ```java
   // Trong test
   System.setProperty("config.file", "application-test.properties");
   ```

## Tương Lai

Có thể mở rộng để hỗ trợ:
- Nhiều file config (dev, test, prod)
- Environment variables override
- Hot reload config (không cần restart)
- YAML format thay vì properties


