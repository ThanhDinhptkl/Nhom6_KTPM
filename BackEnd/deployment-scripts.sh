#!/bin/bash

# Màu sắc cho output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== BẮT ĐẦU TRIỂN KHAI TOUR SERVICE ===${NC}"

# Tạo thư mục triển khai tạm thời
echo -e "${YELLOW}Tạo thư mục triển khai...${NC}"
mkdir -p deploy-tour
cp -r TourService deploy-tour/

# Nén thư mục triển khai
echo -e "${YELLOW}Nén thư mục triển khai...${NC}"
tar -czvf tour-deploy.tar.gz deploy-tour

# Sao chép lên server
echo -e "${YELLOW}Sao chép lên server...${NC}"
scp tour-deploy.tar.gz root@14.225.215.93:/root/

# Triển khai trên server
echo -e "${YELLOW}Kết nối đến server và triển khai...${NC}"
ssh root@14.225.215.93 << 'ENDSSH'
  echo "Giải nén tệp tin triển khai..."
  mkdir -p /opt/microservices
  tar -xzvf tour-deploy.tar.gz -C /opt/microservices
  cd /opt/microservices/deploy-tour/TourService
  
  # Kiểm tra và cài đặt Docker nếu cần
  if ! command -v docker &> /dev/null; then
    echo "Docker chưa được cài đặt. Đang cài đặt Docker..."
    apt-get update
    apt-get install -y docker.io
    systemctl enable docker
    systemctl start docker
  fi
  
  # Kiểm tra và cài đặt Docker Compose nếu cần
  if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose chưa được cài đặt. Đang cài đặt Docker Compose..."
    apt-get install -y docker-compose
  fi

  # Dừng container hiện tại nếu đang chạy
  docker-compose down 2>/dev/null
  
  # Xây dựng và chạy dịch vụ
  docker-compose build
  docker-compose up -d
  
  echo "Đã hoàn tất triển khai TourService!"
  echo "TourService có thể truy cập tại http://14.225.215.93:8081"
ENDSSH

# Dọn dẹp thư mục triển khai tạm thời 
echo -e "${YELLOW}Dọn dẹp tệp tin triển khai tạm thời...${NC}"
rm -rf deploy-tour
rm tour-deploy.tar.gz

echo -e "${GREEN}=== TRIỂN KHAI HOÀN TẤT ===${NC}"
echo -e "${GREEN}TourService có thể truy cập tại http://14.225.215.93:8081${NC}" 