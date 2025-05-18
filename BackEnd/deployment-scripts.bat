@echo off
echo === BAT DAU TRIEN KHAI TOUR SERVICE ===

echo Tao thu muc trien khai...
if exist deploy-tour rmdir /s /q deploy-tour
mkdir deploy-tour
xcopy /E /I /Y TourService deploy-tour\TourService

echo Nen thu muc trien khai...
powershell Compress-Archive -Path deploy-tour -DestinationPath tour-deploy.zip -Force

echo Sao chep len server...
scp tour-deploy.zip root@14.225.215.93:/root/

echo Ket noi den server va trien khai...
ssh root@14.225.215.93 "mkdir -p /opt/microservices && unzip -o /root/tour-deploy.zip -d /opt/microservices && cd /opt/microservices/deploy-tour/TourService && (docker-compose down 2>/dev/null || true) && docker-compose build && docker-compose up -d"

echo Don dep tep tin trien khai tam thoi...
rmdir /s /q deploy-tour
del tour-deploy.zip

echo === TRIEN KHAI HOAN TAT ===
echo TourService co the truy cap tai http://14.225.215.93:8081 