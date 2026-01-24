#!/bin/bash

# Cấu hình cơ bản
MASTER_CONTAINER="redis-master"
SENTINEL_CONTAINERS=("redis-sentinel1" "redis-sentinel2" "redis-sentinel3")
SENTINEL_PORT=26379
REDIS_PORT=6379
QUORUM=2
AUTH_PASS="123456"

# Lấy IP của Redis master container
MASTER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $MASTER_CONTAINER)
echo "Redis master IP: $MASTER_IP"

# Tạo file sentinel.conf cho từng sentinel
for CONTAINER in "${SENTINEL_CONTAINERS[@]}"; do
    # Đặt tên folder tương ứng: sentinel1, sentinel2, sentinel3
    FOLDER_NAME=$(echo $CONTAINER | sed 's/redis-//')
    
    # Lấy IP của Sentinel container
    SENTINEL_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $CONTAINER)
    
    # Tạo thư mục nếu chưa có
    mkdir -p ./$FOLDER_NAME

    # Ghi nội dung vào sentinel.conf
    cat > ./$FOLDER_NAME/sentinel.conf <<EOF
port $SENTINEL_PORT
protected-mode no

sentinel monitor mymaster $MASTER_IP $REDIS_PORT $QUORUM
sentinel auth-pass mymaster $AUTH_PASS
sentinel announce-ip $SENTINEL_IP
sentinel announce-port $SENTINEL_PORT
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel resolve-hostnames no
EOF

    echo "Đã tạo $FOLDER_NAME/sentinel.conf với announce-ip $SENTINEL_IP"
done
