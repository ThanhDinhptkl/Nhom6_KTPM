# Ansible Simple - Triển khai Discovery Service

Repo này chứa cấu hình Ansible đơn giản để triển khai Discovery Service lên server.

## Cấu trúc thư mục
- `hosts` - File inventory chứa thông tin server
- `deploy.yml` - Playbook triển khai service
- `run-ansible.txt` - Các lệnh tham khảo

## Cách chạy với Docker

```bash
# Chạy container Ansible
docker run --rm -it -v ${PWD}:/ansible -v C:/Users/ACER/.ssh:/root/.ssh -w /ansible cytopia/ansible:latest-tools bash

# Sửa quyền SSH key
chmod 600 /root/.ssh/id_rsa
chmod 644 /root/.ssh/id_rsa.pub

# Chạy playbook để triển khai
ansible-playbook -i hosts deploy.yml
```

## Dừng service

Để dừng Discovery Service:
```bash
# Bằng Ansible
ansible -i hosts server -m shell -a "pkill -f 'java -jar /opt/discovery-service/discovery-server.jar'"

# Hoặc trực tiếp SSH và chạy
ssh root@14.225.215.93
pkill -f "java -jar /opt/discovery-service/discovery-server.jar"
```

## Kiểm tra service

Truy cập http://14.225.215.93:8761 để xem giao diện Eureka Server 