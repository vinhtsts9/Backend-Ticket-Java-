# 🚀 HƯỚNG DẪN CHẠY DỰ ÁN TICKET REDIS SENTINEL

## ⚠️ LỖI: Bean 'redissonClient' không được tạo

**Lỗi:** `Bean named 'redissonClient' is expected to be of type 'org.redisson.api.RedissonClient' but was actually of type 'org.springframework.beans.factory.support.NullBean'`

**Nguyên nhân:** RedissonConfig trả về `null` khi kết nối Redis Sentinel thất bại.

**Giải pháp:** Đã fix trong RedissonConfig.java:
- ✅ Thêm `@Primary` annotation
- ✅ Throw exception thay vì return null
- ✅ Fix log thông báo lỗi rõ ràng

---

## 1️⃣ BUILD PROJECT

```bash
# Build toàn bộ project
cd "c:\Users\admin\Downloads\Ticket.com-sentinel chạy ổn"
mvn clean package -DskipTests

# Jar file sẽ ở:
Ticket-start\target\Ticket-start-1.0-SNAPSHOT.jar
```

---

## 2️⃣ KHỞI ĐỘNG DOCKER

```bash
cd environment
docker-compose up -d
```
✅ Chờ 10-15 giây để tất cả services khởi động

---

## 3️⃣ SEED DỮ LIỆU REDIS

```bash
# Seed stock vé vào Redis
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:1:stock 1000
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:2:stock 500
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:3:stock 2000
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:4:stock 1000
```

---

## 4️⃣ TEST API TICKET

```bash
curl http://localhost:1122/api/tickets
```

---

## 5️⃣ TEST REDIS SENTINEL (HA)

```bash
# Check status
docker-compose exec redis-sentinel1 redis-cli -p 26379 sentinel masters

# Tắt master test failover
docker-compose stop redis-master

# Sentinels tự promote slave lên master
docker-compose exec redis-sentinel1 redis-cli -p 26379 sentinel masters

# Bật master lại
docker-compose start redis-master
```

---

## 6️⃣ MONITOR LOG

```bash
docker-compose logs -f ticket-service
docker-compose logs -f redis-sentinel1
```

---

## ✅ KIỂM TRA

- ✓ ticket-service: http://localhost:1122
- ✓ MySQL: localhost:3316
- ✓ Redis Sentinels: 26379, 26380, 26381
