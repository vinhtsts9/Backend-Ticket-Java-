# 📊 Phân Tích Hệ Thống Đồng Thời Cao - Ticket.com

## 1. 🔍 Tổng Quan Kiến Trúc

### Stack Công Nghệ
- **Backend**: Spring Boot 3.3.5 + Java 21
- **Database**: MySQL 8.0 (HikariCP)
- **Cache**: Redis Sentinel Cluster (3 nodes)
- **Monitoring**: Prometheus + Grafana + ELK Stack
- **Testing**: JMeter (Aggregate Report benchmarking)
- **Architecture Pattern**: Clean Architecture (DDD)

### Các Lớp Ứng Dụng
```
Ticket-start (Main Entry)
├── Ticket-controller (REST API)
├── Ticket-application (Business Logic + Caching)
├── Ticket-domain (Domain Models + Business Rules)
└── Ticket-infrastructure (DB, Redis, Cache, Locking)
```

---

## 2. ✅ ĐIỂM MẠNH - Những gì đã làm tốt

### A. Cấu Trúc Database Hiệp Hiệp
- ✅ **Table Partitioning** (Bảng orders theo tháng: 202504, 202503...)
  - Cải thiện query performance & dễ archive dữ liệu cũ
  - Phù hợp cho hệ thống bán vé cao tải

### B. Redis Cache & Locking
- ✅ **Redis Sentinel** (HA + Failover tự động)
- ✅ **Lua Script để giảm stock** (nguyên tử hóa giảm stock)
  ```lua
  if stock >= quantity then 
    stock -= quantity; return 1;
  else return 0;
  ```
  - Tránh race condition ở Redis layer

- ✅ **Redisson Distributed Lock**
  - Lock khóa an toàn cho multi-node deployment

- ✅ **CAS (Compare-And-Swap)** Pattern
  - `decreaseStockCas()`: chỉ cập nhật khi stock match expected value

### C. Cấu Hình Tomcat Tối Ưu
- ✅ Max threads: 200 (điều chỉnh hợp lý)
- ✅ Min-spare threads: 50 (ready pool)
- ✅ Accept-count: 20,000 (buffer queue)

### D. Connection Pool
- ✅ **HikariCP** (pool database hiệu suất cao)
  - Maximum: 20 connections
  - Minimum idle: 5 connections
  - Phù hợp với tỷ lệ request/DB ratio

### E. Resilience & Monitoring
- ✅ **Resilience4j Circuit Breaker** (khôi phục fault tolerance)
- ✅ **Rate Limiter** được cấu hình
- ✅ **Prometheus + Grafana** cho observability
- ✅ **Micrometer metrics** exported

### F. Virtual Threads
- ✅ Java 21 Virtual Threads enabled
  - Giảm memory overhead so với traditional threads
  - Tốt cho I/O-bound operations

---

## 3. ⚠️ ĐIỂM YẾU & RỦI RO - Cần Cải Thiện

### 🔴 A. Race Condition & Double-Booking

**Vấn đề**:
```java
// Flow hiện tại có gap logic
int oldStock = decreaseStockCacheByLua(ticketId, quantity);  // ← Lua script OK
boolean isDecreaseStockSuccess = ticketOrderDomainService.decreaseStock();  // ← Pessimistic lock?
if(isDecreaseStockSuccess) {
    insertOrder();  // ← Nếu insert fail → stock giảm nhưng đơn hàng không được tạo!
}
```

**Rủi ro**:
- ❌ Giảm stock Redis thành công, nhưng DB update fail → mất vé
- ❌ Insert order fail → inconsistent state giữa Redis & DB

**Đề xuất**:
```java
@Transactional(rollbackFor = Exception.class)
public boolean decreaseStockCAS(Long ticketId, int quantity) {
    // 1. Kiểm tra và giảm trên Redis (Lua)
    int oldStock = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);
    if(oldStock == 0) return false;
    
    try {
        // 2. Giảm trên DB với Pessimistic Lock
        boolean dbSuccess = ticketOrderDomainService.decreaseStockWithLock(ticketId, quantity);
        
        if(!dbSuccess) {
            // Rollback Redis nếu DB fail
            stockOrderCacheService.increaseStockCache(ticketId, quantity);
            return false;
        }
        
        // 3. Create order (trong transaction)
        TicketOrder order = createOrder(ticketId, userId, quantity);
        orderDeductionDomainService.insertOrder(getMonthTable(), order);
        
        return true;
    } catch(Exception e) {
        // Rollback Redis trên exception
        stockOrderCacheService.increaseStockCache(ticketId, quantity);
        throw e;
    }
}
```

---

### 🔴 B. Thiếu Pessimistic Lock trên Database

**Vấn đề**:
- JPA Query không có `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Có thể 2 request giảm stock cùng lúc từ Redis về DB

**Hiện tại**:
```java
@Query("UPDATE TicketDetail t SET t.stockAvailable = t.stockAvailable - :quantity " +
        "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity")
int decreaseStock(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);
```

**Cải thiện**:
```java
@Query("SELECT t FROM TicketDetail t WHERE t.id = :ticketId")
@Lock(LockModeType.PESSIMISTIC_WRITE)  // ← ADD THIS
Optional<TicketDetail> findByIdWithLock(@Param("ticketId") Long ticketId);

public boolean decreaseStockWithPessimisticLock(Long ticketId, int quantity) {
    TicketDetail ticket = ticketOrderJPAMapper.findByIdWithLock(ticketId)
        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    
    if(ticket.getStockAvailable() >= quantity) {
        ticket.setStockAvailable(ticket.getStockAvailable() - quantity);
        ticketOrderJPAMapper.save(ticket);
        return true;
    }
    return false;
}
```

---

### 🔴 C. Thread Pool Size Có Thể Quá Nhỏ

**Vấn đề**:
```yaml
server:
  tomcat:
    threads:
      max: 200        # ← Có thể quá nhỏ cho high concurrency
      min-spare: 50
    accept-count: 20000
```

**Tính toán**:
- 200 threads = ~200 concurrent requests (nếu mỗi request 1s)
- 20,000 queue = chờ ~100s ở queue (kém trải nghiệm)

**Đề xuất**:
```yaml
server:
  tomcat:
    threads:
      max: 500        # Tăng thành 500-1000 tùy theo server resources
      min-spare: 100
    accept-count: 5000  # Giảm queue kích thước, reject fast hơn
    max-connections: 8000
    max-http-post-size: 2MB
    compression:
      enabled: true
      min-response-size: 1024
```

---

### 🔴 D. Database Connection Pool Quá Nhỏ

**Vấn đề**:
```yaml
datasource:
  hikari:
    maximum-pool-size: 20    # ← Quá nhỏ nếu có 500 threads!
    minimum-idle: 5
```

**Tỷ lệ**:
- 500 threads vs 20 DB connections = 25:1 ratio
- Sẽ có **connection starvation** & waiting threads

**Đề xuất**:
```yaml
datasource:
  hikari:
    maximum-pool-size: 50    # Tăng lên (nhưng < num threads)
    minimum-idle: 10
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
    leak-detection-threshold: 60000  # Detect connection leaks
```

---

### 🔴 E. Không Có Read Replicas & Connection Pooling cho Reads

**Vấn đề**:
- Master DB cả read + write = bottleneck
- `getStockAvailable()` query không được optimize

**Đề xuất**:
```java
@Repository
public class ReadTicketDetailRepository {
    // Read từ replica
    @Query("SELECT t FROM TicketDetail t WHERE t.id = :id")
    Optional<TicketDetail> findById(@Param("id") Long id);
}

@Repository  
public class WriteTicketDetailRepository {
    // Write vào master
    void save(TicketDetail detail);
}

// Trong service
@Service
public class TicketDetailAppServiceImpl {
    @Autowired
    private ReadTicketDetailRepository readRepo;   // → Replica
    @Autowired
    private WriteTicketDetailRepository writeRepo; // → Master
    
    public int getStockAvailable(Long ticketId) {
        return readRepo.findById(ticketId)
            .map(TicketDetail::getStockAvailable)
            .orElse(0);
    }
}
```

---

### 🔴 F. Virtual Threads Enabled nhưng Chưa Tối Ưu

**Vấn đề**:
```yaml
spring:
  threads:
    virtual:
      enabled: true  # ✅ Enabled tốt
```

Nhưng không có **thread pool configuration** cho virtual threads.

**Đề xuất**:
```java
@Configuration
public class VirtualThreadsConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("virtual-async-");
        executor.setVirtualThreads(true);  // ← Key property!
        executor.initialize();
        return executor;
    }
}
```

---

### 🔴 G. Chưa Có Global Transaction Management

**Vấn đề**:
- Kafka/Message Queue cho async processing? Không thấy
- Compensating transactions (saga pattern) cho distributed transactions? Không có
- Order history thay vì direct insert?

**Đề xuất**:
```java
@Service
public class DistributedOrderServiceImpl {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Transactional(rollbackFor = Exception.class)
    public void placeOrder(Long ticketId, int quantity) {
        // 1. Decrease stock (nguyên tử)
        if(!decreaseStockCAS(ticketId, quantity)) {
            throw new InsufficientStockException();
        }
        
        // 2. Create order
        TicketOrder order = createOrder(ticketId, quantity);
        
        // 3. Publish event (async) cho order processing
        OrderEvent event = OrderEvent.builder()
            .orderId(order.getId())
            .ticketId(ticketId)
            .quantity(quantity)
            .build();
        kafkaTemplate.send("order-events", event);
    }
}
```

---

### 🔴 H. Lack of Caching Strategy

**Vấn đề**:
- Chỉ cache stock ở Redis, không cache ticket details (name, price, description)
- Query `findAll()` trên partition table mỗi lần không cache

**Đề xuất**:
```java
@Service
public class TicketDetailCacheService {
    private static final String TICKET_DETAILS_KEY = "ticket:details:";
    private static final long CACHE_TTL = 3600; // 1 hour
    
    @Cacheable(value = "ticketDetails", key = "#ticketId")
    public TicketDetailCache getTicketDetail(Long ticketId) {
        TicketDetail ticket = ticketDetailRepository.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("Ticket not found"));
        
        TicketDetailCache cache = new TicketDetailCache(ticket);
        redisTemplate.opsForValue().set(
            TICKET_DETAILS_KEY + ticketId, 
            cache, 
            Duration.ofSeconds(CACHE_TTL)
        );
        return cache;
    }
}
```

---

### 🔴 I. Missing Health Checks & Graceful Shutdown

**Vấn đề**:
- Actuator endpoints exposed nhưng health checks cho dependencies không hoàn chỉnh
- Không có graceful shutdown logic

**Đề xuất**:
```java
@Configuration
public class HealthCheckConfig {
    @Bean
    public HealthIndicator redisHealthIndicator(RedisTemplate<String, Object> template) {
        return () -> {
            try {
                template.getConnectionFactory().getConnection().ping();
                return Health.up().build();
            } catch(Exception e) {
                return Health.down().withException(e).build();
            }
        };
    }
    
    @Bean
    public HealthIndicator dbHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.isValid(2);
                return Health.up().build();
            } catch(Exception e) {
                return Health.down().withException(e).build();
            }
        };
    }
}

@Component
public class GracefulShutdown {
    @PreDestroy
    public void gracefulShutdown() {
        log.info("Graceful shutdown started...");
        // Drain queues, close connections, etc.
        log.info("Graceful shutdown completed");
    }
}
```

---

### 🔴 J. Monitoring & Alerting Gaps

**Vấn đề**:
- Prometheus + Grafana setup ✅
- Nhưng missing key metrics:
  - Stock depletion rate
  - Order success/failure rate
  - Queue wait time
  - Redis memory usage warning

**Đề xuất**:
```java
@Component
public class CustomMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger pendingOrders = new AtomicInteger(0);
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        Gauge.builder("ticket.orders.pending", pendingOrders, AtomicInteger::get)
            .description("Pending order requests")
            .register(meterRegistry);
    }
    
    public void recordOrderAttempt(boolean success, long duration) {
        meterRegistry.timer("order.process.duration").record(duration, TimeUnit.MILLISECONDS);
        meterRegistry.counter("order.attempt", "success", String.valueOf(success)).increment();
    }
}
```

---

## 4. 🎯 ĐỀ XUẤT HƯỚNG PHÁT TRIỂN

### Phase 1: Critical Fixes (1-2 tuần)
1. **Add Pessimistic Lock** trên DB queries
2. **Fix transaction boundaries** (Redis + DB consistency)
3. **Increase thread pool size** (200 → 500)
4. **Increase DB connection pool** (20 → 50)
5. **Add compensating transactions** cho failures

### Phase 2: Performance Optimization (2-3 tuần)
1. **Implement Read Replicas** (read-only queries)
2. **Add Multi-level caching**:
   - L1: Local Caffeine Cache (hot data)
   - L2: Redis Cache (distributed)
   - L3: Database (persistent)

3. **Async Event Processing** (Kafka/RabbitMQ):
   - Stock decrease sync (transactional)
   - Order creation async (fire & forget)
   - Notifications async

4. **Database Query Optimization**:
   ```sql
   CREATE INDEX idx_ticket_stock ON ticket_detail(id, stock_available);
   CREATE INDEX idx_order_month_user ON orders_202504(user_id, created_at);
   ```

### Phase 3: Scalability (3-4 tuần)
1. **Implement Message Queue** (Kafka/RabbitMQ):
   ```
   Order Request → Queue → Workers (scale horizontally)
   ```

2. **Add Circuit Breaker** cho external APIs
3. **Implement API Rate Limiting** per user/IP
4. **Add Load Balancer** (Nginx) với sticky sessions
5. **Horizontal Scaling** với Redis cluster replication

### Phase 4: Enterprise Features (Ongoing)
1. **Distributed Tracing** (Sleuth + Zipkin)
2. **Advanced Monitoring** (Custom dashboards)
3. **Security Hardening** (HTTPS, Rate limiting, Input validation)
4. **Disaster Recovery** (Backup strategy, RTO/RPO)
5. **A/B Testing** framework

---

## 5. 📈 Benchmarking & Load Testing

### JMeter Configuration Đề Xuất
```jmx
<!-- Current: HTTP Request level 0.jmx -->
<!-- Scenarios to test: -->

1. Normal Load
   - 100 users
   - Ramp-up: 30s
   - Duration: 5m
   
2. Peak Load
   - 500 users
   - Ramp-up: 10s
   - Duration: 10m
   - Throughput: Track success rate
   
3. Stress Test
   - 1000 users
   - Ramp-up: 5s
   - Duration: 5m
   - Expected: Graceful degradation
   
4. Spike Test
   - 100 → 1000 users instantly
   - Duration: 1m
   - Check recovery time
```

---

## 6. 🔐 Security Considerations

- ❌ Password hardcoded (123456) → Use secrets manager
- ❌ DB user `root` with default password
- ✅ Resilience4j có, nhưng cần enable authentication
- ⚠️ No HTTPS/TLS enforcement

**Fix**:
```yaml
spring:
  application:
    name: ticket-service
  security:
    user:
      name: admin
      password: ${ADMIN_PASSWORD:}  # From environment
  datasource:
    hikari:
      username: ${DB_USER}
      password: ${DB_PASSWORD}
```

---

## 7. 📊 Summary Table

| Aspect | Status | Priority | Effort |
|--------|--------|----------|--------|
| Architecture | ✅ Good | - | - |
| Caching Strategy | ⚠️ Partial | HIGH | 1 week |
| Database Locking | 🔴 Missing | CRITICAL | 2-3 days |
| Thread Pool | ⚠️ Small | HIGH | 1 day |
| Connection Pool | ⚠️ Small | HIGH | 1 day |
| Async Processing | 🔴 Missing | MEDIUM | 1-2 weeks |
| Read Replicas | 🔴 Missing | MEDIUM | 1-2 weeks |
| Monitoring | ✅ Good | LOW | - |
| Security | 🔴 Weak | MEDIUM | 3-5 days |
| Load Testing | ✅ Exists | MEDIUM | 1 week |

---

## 8. 🎓 Kết Luận

### Hiện Tại
✅ Hệ thống có **nền tảng tốt** với DDD + Cache + Distributed Lock
⚠️ Nhưng chưa **production-ready** cho high concurrency
🔴 Race conditions + inconsistent state risks

### Để Production-Ready
1. **Fix critical issues** (locking, transaction boundaries)
2. **Optimize resources** (thread pool, connection pool)
3. **Add async processing** (Kafka/Message Queue)
4. **Implement comprehensive monitoring** (metrics, alerts)
5. **Load testing** với targets: 1000+ concurrent users

### Dự kiến Timeline
- **1 tuần**: Critical fixes → 1,000-2,000 concurrent users
- **3 tuần**: Performance optimizations → 5,000-10,000 concurrent users
- **6 tuần**: Full scalability → 50,000+ concurrent users (với horizontal scaling)

