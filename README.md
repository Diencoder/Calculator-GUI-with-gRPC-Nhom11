# Calculator GUI with gRPC

A modern calculator application with a JavaFX graphical user interface that communicates with a gRPC server for performing mathematical calculations.

## 🚀 Features

- **Modern JavaFX GUI**: Clean and intuitive calculator interface
- **gRPC Communication**: High-performance client-server communication using Protocol Buffers
- **Basic Operations**: Addition, subtraction, multiplication, division, modulo, and power
- **Advanced Operations**: Square root, logarithm, trigonometric functions, and more
- **Real-time Validation**: Input validation and error handling
- **Logging**: Comprehensive logging for debugging and monitoring
- **Health Checks**: Server health monitoring

## 🏗️ Architecture

```
┌─────────────────┐    gRPC     ┌─────────────────┐
│   JavaFX GUI    │◄──────────►│  gRPC Server    │
│   (Client)      │             │  (Calculator)   │
└─────────────────┘             └─────────────────┘
```

## 📁 Project Structure

```
Calculator-GUI-with-gRPC/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/calculator/calculatorguiwithgrpc/
│   │   │       ├── client/                    # gRPC Client
│   │   │       │   └── CalculatorClient.java
│   │   │       ├── server/                    # gRPC Server
│   │   │       │   ├── CalculatorServer.java
│   │   │       │   ├── CalculatorServiceImpl.java
│   │   │       │   ├── AdvancedCalculatorService.java
│   │   │       │   └── LogHandler.java
│   │   │       ├── gui/                       # JavaFX GUI
│   │   │       │   ├── CalculatorGUI.java
│   │   │       │   └── CalculatorController.java
│   │   │       ├── utils/                     # Utilities
│   │   │       │   └── ValidationUtils.java
│   │   │       └── proto/                     # Generated Proto Classes
│   │   ├── resources/
│   │   │   └── com/calculator/calculatorguiwithgrpc/
│   │   │       ├── gui/
│   │   │       │   └── calculator-view.fxml
│   │   │       └── proto/
│   │   │           └── calculator.proto
│   │   └── proto/                             # Generated Proto Files
│   └── test/
│       └── java/
│           └── com/calculator/calculatorguiwithgrpc/
├── pom.xml
└── README.md
```

## 🛠️ Prerequisites

- **Java 17+**: Required for JavaFX and gRPC
- **Maven 3.6+**: For dependency management and building
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code recommended

## 📦 Dependencies

- **JavaFX 17.0.6**: GUI framework
- **gRPC 1.58.0**: RPC framework
- **Protocol Buffers 3.24.4**: Data serialization
- **SLF4J + Logback**: Logging framework
- **JUnit 5**: Testing framework

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Calculator-GUI-with-gRPC
```

### 2. Generate Protocol Buffer Classes

```bash
mvn clean compile
```

### 3. Start the gRPC Server

```bash
mvn exec:java -Dexec.mainClass="com.calculator.calculatorguiwithgrpc.server.CalculatorServer"
```

### 4. Start the GUI Application

```bash
mvn javafx:run
```

## 🎯 Usage

### Basic Operations
- **Numbers**: Click number buttons (0-9)
- **Operators**: Click +, -, ×, ÷, %, ^
- **Decimal**: Click . for decimal point
- **Equals**: Click = to calculate
- **Clear**: Click C to clear all
- **Sign Change**: Click ± to change sign

### Advanced Operations
- Toggle to "Advanced Mode" for additional functions
- **sqrt**: Square root
- **log**: Natural logarithm
- **sin/cos/tan**: Trigonometric functions
- **abs**: Absolute value
- **ceil/floor/round**: Rounding functions
- **max/min**: Maximum/minimum values

## 🔧 Development

### Building the Project

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package
```

### Running Tests

```bash
mvn test
```

### Code Generation

The project uses Maven plugins to automatically generate gRPC classes from the `.proto` file:

```bash
mvn protobuf:compile
mvn protobuf:compile-custom
```

## 📊 API Documentation

### gRPC Service Definition

```protobuf
service CalculatorService {
  rpc Calculate(CalculationRequest) returns (CalculationResponse);
  rpc StreamCalculate(stream CalculationRequest) returns (stream CalculationResponse);
  rpc HealthCheck(HealthCheckRequest) returns (HealthCheckResponse);
}
```

### Message Types

- **CalculationRequest**: Contains operands, operator, and request ID
- **CalculationResponse**: Contains result, success status, and error message
- **HealthCheckRequest/Response**: For server health monitoring

## 🐛 Troubleshooting

### Common Issues

1. **Server Connection Failed**
   - Ensure the gRPC server is running on port 9090
   - Check firewall settings
   - Verify server logs for errors

2. **JavaFX Runtime Not Found**
   - Ensure JavaFX is properly installed
   - Check module path configuration
   - Use `mvn javafx:run` instead of direct execution

3. **Protocol Buffer Generation Failed**
   - Ensure Maven plugins are properly configured
   - Check OS compatibility for protoc executable
   - Run `mvn clean compile` to regenerate

### Logs

Check the console output for detailed logging information:
- Server logs: gRPC server startup and request processing
- Client logs: Connection status and calculation results
- GUI logs: User interactions and error handling

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Team

- **Người 1**: Core Server (`CalculatorServer.java`, `CalculatorServiceImpl.java`)
- **Người 2**: Core Client (`CalculatorClient.java`)
- **Người 3**: Core GUI (`CalculatorGUI.java`)
- **Người 4**: Utilities (`ValidationUtils.java`)
- **Người 5**: Advanced Server & Logging (`AdvancedCalculatorService.java`, `LogHandler.java`)
- **Người 6**: gRPC Definition & Build (`calculator.proto`, `pom.xml`)

## 🎓 Educational Value

This project demonstrates:
- **gRPC**: Modern RPC framework with Protocol Buffers
- **JavaFX**: Modern Java GUI development
- **Client-Server Architecture**: Distributed system design
- **Maven**: Build automation and dependency management
- **Logging**: Application monitoring and debugging
- **Error Handling**: Robust error management
- **Testing**: Unit and integration testing

## 📚 References

- [gRPC Java Documentation](https://grpc.io/docs/languages/java/)
- [JavaFX Documentation](https://openjfx.io/)
- [Protocol Buffers Guide](https://developers.google.com/protocol-buffers)
- [Maven Documentation](https://maven.apache.org/guides/)
