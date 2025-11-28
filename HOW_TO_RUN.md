# Hướng dẫn Chạy Dự án Calculator GUI với gRPC

## Yêu cầu hệ thống
- Java 21 (hoặc Java 17+)
- Maven 3.6+ (hoặc sử dụng Maven Wrapper có sẵn)
- IDE: IntelliJ IDEA (khuyến nghị)

## Cách 1: Chạy từ IntelliJ IDEA

### Bước 1: Mở dự án
1. Mở IntelliJ IDEA
2. File → Open → Chọn thư mục `Calculator-GUI-with-gRPC-Nhom11`

### Bước 2: Cấu hình Run Configuration

#### Chạy GUI Application:
1. Click chuột phải vào file `Launcher.java` (hoặc `CalculatorGUI.java`)
2. Chọn **Run 'Launcher.main()'** hoặc **Run 'CalculatorGUI.main()'**
3. Hoặc tạo Run Configuration mới:
   - Run → Edit Configurations...
   - Click **+** → **Application**
   - **Name**: Calculator GUI
   - **Main class**: `com.calculator.calculatorguiwithgrpc.Launcher`
   - **VM options**: (để trống hoặc thêm nếu cần)
   - **Working directory**: `$PROJECT_DIR$`
   - Click **OK**

#### Chạy Server:
1. Click chuột phải vào file `CalculatorServer.java`
2. Chọn **Run 'CalculatorServer.main()'**
3. Hoặc tạo Run Configuration:
   - **Name**: Calculator Server
   - **Main class**: `com.calculator.calculatorguiwithgrpc.server.CalculatorServer`
   - Click **OK**

### Bước 3: Chạy ứng dụng
1. **Chạy Server trước**: Run → Calculator Server (hoặc nhấn Shift+F10 trên CalculatorServer.java)
2. **Chạy GUI sau**: Run → Calculator GUI (hoặc nhấn Shift+F10 trên Launcher.java)

## Cách 2: Chạy từ Command Line (Terminal)

### Bước 1: Compile dự án
```bash
# Sử dụng Maven Wrapper (Windows)
.\mvnw.cmd clean compile

# Hoặc nếu đã cài Maven
mvn clean compile
```

### Bước 2: Chạy Server
```bash
# Terminal 1 - Chạy Server
.\mvnw.cmd exec:java -Dexec.mainClass="com.calculator.calculatorguiwithgrpc.server.CalculatorServer"

# Hoặc
mvn exec:java -Dexec.mainClass="com.calculator.calculatorguiwithgrpc.server.CalculatorServer"
```

### Bước 3: Chạy GUI (Terminal mới)
```bash
# Terminal 2 - Chạy GUI
.\mvnw.cmd javafx:run

# Hoặc
mvn javafx:run
```

## Cách 3: Chạy trực tiếp từ Java

### Chạy Server:
```bash
java -cp "target/classes;target/dependency/*" com.calculator.calculatorguiwithgrpc.server.CalculatorServer
```

### Chạy GUI:
```bash
java -cp "target/classes;target/dependency/*" com.calculator.calculatorguiwithgrpc.Launcher
```

## Xử lý lỗi thường gặp

### Lỗi: "JavaFX runtime components are missing"
**Giải pháp**: Sử dụng `Launcher.java` thay vì chạy trực tiếp `CalculatorGUI.java`

### Lỗi: "Cannot connect to server"
**Giải pháp**: 
1. Đảm bảo Server đã được khởi động trước
2. Kiểm tra port 9090 có bị chiếm dụng không
3. Kiểm tra file `application.properties` có cấu hình đúng không

### Lỗi: "JAVA_HOME not found"
**Giải pháp**:
1. Cài đặt Java 21 (hoặc Java 17+)
2. Set biến môi trường JAVA_HOME trỏ đến thư mục Java
3. Thêm `%JAVA_HOME%\bin` vào PATH

### Lỗi: "Maven not found"
**Giải pháp**: 
1. Sử dụng Maven Wrapper (`mvnw.cmd` trên Windows)
2. Hoặc cài đặt Maven và thêm vào PATH

## Cấu trúc Run Configurations trong IntelliJ

### Run Configuration cho Server:
```
Name: Calculator Server
Type: Application
Main class: com.calculator.calculatorguiwithgrpc.server.CalculatorServer
Working directory: $PROJECT_DIR$
```

### Run Configuration cho GUI:
```
Name: Calculator GUI
Type: Application
Main class: com.calculator.calculatorguiwithgrpc.Launcher
Working directory: $PROJECT_DIR$
```

## Lưu ý
- **Luôn chạy Server trước**, sau đó mới chạy GUI
- Server chạy trên port **9090** (mặc định)
- Nếu muốn chạy cả 2 cùng lúc, cần 2 terminal/run configuration riêng biệt
- GUI sẽ tự động kết nối đến Server khi khởi động

