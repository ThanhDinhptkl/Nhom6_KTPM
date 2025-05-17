# Discovery Server (Eureka)

Service này đóng vai trò là Eureka Server cho kiến trúc microservices, giúp các service khác đăng ký và khám phá lẫn nhau.

## Cấu trúc

- Dockerfile: Cấu hình để build container cho Eureka Server
- docker-compose.yml: Cấu hình để khởi chạy Eureka Server trong môi trường Docker

## Cài đặt và Triển khai

### Yêu cầu
- Docker và Docker Compose đã được cài đặt
- Java 17

### Triển khai

1. Clone repository về VPS:
```bash
git clone <repository-url>
cd BackEnd/discovery-server
```

2. Build và chạy service:
```bash
docker-compose up -d
```

3. Kiểm tra trạng thái:
```bash
docker-compose ps
```

4. Xem logs:
```bash
docker-compose logs -f
```

### Kiểm tra hoạt động

Sau khi khởi động, bạn có thể truy cập Eureka Dashboard tại:
```
http://[server-ip]:8761
```

## Kết nối với các Service khác

Các service khác trong kiến trúc microservices cần được cấu hình để kết nối với Eureka Server này bằng cách thiết lập URL của Eureka server trong tệp cấu hình hoặc biến môi trường:

```
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://[discovery-server-ip]:8761/eureka/
EUREKA_CLIENT_ENABLED=true
EUREKA_INSTANCE_PREFER_IP_ADDRESS=true
```

Đảm bảo thay thế `[discovery-server-ip]` bằng địa chỉ IP thực của VPS chạy Eureka Server.

## Lưu ý

- Máy chủ Eureka nên được đặt ở nơi có kết nối ổn định và truy cập được từ tất cả các service khác.
- Mở cổng 8761 trên tường lửa của VPS để cho phép các service khác truy cập.
- Để đảm bảo tính sẵn sàng cao, có thể triển khai nhiều Eureka Server và cấu hình chúng để tìm kiếm lẫn nhau. 