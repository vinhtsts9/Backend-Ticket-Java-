# 🚀 HƯỚNG DẪN CHẠY DỰ ÁN TICKET REDIS SENTINEL

## 1️⃣ KHỞI ĐỘNG DOCKER
```bash
cd environment
docker-compose up -d
```
✅ Chờ 10 giây để tất cả services khởi động

---

## 2️⃣ SEED DỮ LIỆU REDIS

```bash
# Seed stock vé vào Redis
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:1:stock 1000
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:2:stock 500
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:3:stock 2000
docker-compose exec -T redis-master redis-cli -a 123456 SET ticket:4:stock 1000

# Kiểm tra dữ liệu
docker-compose exec -T redis-master redis-cli -a 123456 KEYS "*"
```

---

## 3️⃣ TEST CƠNG API TICKET

```bash
# Lấy danh sách vé
curl http://localhost:1122/api/tickets

# Chi tiết vé ID 1
curl http://localhost:1122/api/tickets/1

# Mua vé (POST request)
curl -X POST http://localhost:1122/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1001, "ticketItemId":1, "quantity":5}'
```

---

## 4️⃣ TEST REDIS SENTINEL (High Availability)

```bash
# 1. Kiểm tra status sentinels
docker-compose exec redis-sentinel1 redis-cli -p 26379 sentinel masters

# 2. Kiểm tra replicas
docker-compose exec redis-sentinel1 redis-cli -p 26379 sentinel slaves mymaster

# 3. SIM: Tắt master để test failover
docker-compose stop redis-master

# 4. Kiểm tra - sentinels sẽ tự promote slave lên master
docker-compose exec redis-sentinel1 redis-cli -p 26379 sentinel masters

# 5. Bật master lại
docker-compose start redis-master
```

---

## 5️⃣ TEST DB CONSISTENCY

```bash
# Xem stock sau khi mua vé
docker-compose exec mysql mysql -u root -p123456 vetautet \
  -e "SELECT id, name, stock_available FROM ticket_item;"

# Xem đơn hàng
docker-compose exec mysql mysql -u root -p123456 vetautet \
  -e "SELECT * FROM ticket_order_202502;"
```

---

## 6️⃣ TEST REDIS PERSISTENCE

```bash
# 1. Tắt toàn bộ containers
docker-compose down

# 2. Bật lại
docker-compose up -d

# 3. Kiểm tra dữ liệu Redis vẫn lưu
docker-compose exec redis-master redis-cli -a 123456 GET ticket:1:stock
# Output: 1000 ✅
```

---

## 7️⃣ MONITOR LOG

```bash
# Xem log Redis Sentinels (real-time)
docker-compose logs -f redis-sentinel1 redis-sentinel2 redis-sentinel3

# Xem log ticket-service
docker-compose logs -f ticket-service

# Xem log MySQL
docker-compose logs -f mysql
```

---

## 8️⃣ LOAD TEST PERFORMANCE

```bash
# Dùng JMeter (sẵn trong benchmark/jmeter/)
cd benchmark/jmeter/bin

# Chạy load test (Windows)
jmeter -t ../Aggregate\ Report.jmx

# Hoặc dùng Apache Bench
ab -n 1000 -c 10 http://localhost:1122/api/tickets
```

---

## 9️⃣ DỪNG & CLEANUP

```bash
# Dừng containers
docker-compose down

# Xóa volumes (cảnh báo: xóa dữ liệu)
docker-compose down -v
```

---

## ✅ KIỂM TRA HOẠT ĐỘNG BÌNH THƯỜNG

- ✓ ticket-service: http://localhost:1122
- ✓ MySQL: localhost:3316 (user: root, pass: 123456)
- ✓ Redis Master: localhost:6379 (port)
- ✓ Redis Sentinels: 26379, 26380, 26381

**Nếu có lỗi, xem log:** `docker-compose logs [service-name]`
