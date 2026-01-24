package com.ticket.ddd.application.service.order.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ticket.ddd.application.model.TicketOrderDTO;
import com.ticket.ddd.application.service.order.TicketOrderAppService;
import com.ticket.ddd.application.service.order.cache.StockOrderCacheService;
import com.ticket.ddd.domain.model.entity.TicketOrder;
import com.ticket.ddd.domain.service.OrderDeductionDomainService;
import com.ticket.ddd.domain.service.TicketOrderDomainService;
import com.ticket.ddd.infrastructure.cache.redis.RedisCompensatingTransaction;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TicketOrderAppServiceImpl implements TicketOrderAppService {
    @Autowired
    private TicketOrderDomainService ticketOrderDomainService;
    @Autowired
    private OrderDeductionDomainService orderDeductionDomainService;
    @Autowired
    private StockOrderCacheService stockOrderCacheService;
    @Autowired
    private RedisCompensatingTransaction redisCompensatingTransaction;

    /**
     * Decrease stock with transaction boundaries fixed
     * Flow:
     * 1. Decrease Redis cache (Lua script - atomic)
     * 2. Decrease DB with pessimistic lock
     * 3. Create order
     * 4. If any step fails, rollback Redis
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStockCAS(Long ticketId, int quantity) {
        log.info("[FLOW] Start decreaseStockCAS: ticketId={}, quantity={}", ticketId, quantity);
        
        try {
            // STEP 1: Try to decrease stock in Redis cache (Lua script - atomic)
            log.debug("[FLOW] Step 1: Attempting to decrease Redis cache");
            int oldStockAvailable = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);
            
            if (oldStockAvailable == 0) {
                log.info("[FLOW] Step 1 FAILED: Stock unavailable in Redis cache");
                return false;
            }
            log.info("[FLOW] Step 1 SUCCESS: Redis cache decreased, oldStock={}", oldStockAvailable);

            // STEP 2: Decrease stock in database with pessimistic lock
            log.debug("[FLOW] Step 2: Attempting to decrease DB with pessimistic lock");
            boolean isDecreaseStockSuccess = ticketOrderDomainService.decreaseStockWithPessimisticLock(ticketId, quantity);
            
            if (!isDecreaseStockSuccess) {
                log.warn("[FLOW] Step 2 FAILED: DB decrease failed, rolling back Redis");
                // Compensating transaction: restore Redis cache
                redisCompensatingTransaction.increaseStockCache(ticketId, quantity);
                redisCompensatingTransaction.logCompensation(ticketId, "decreaseStockCAS", 
                    "DB pessimistic lock failed");
                return false;
            }
            log.info("[FLOW] Step 2 SUCCESS: DB stock decreased with pessimistic lock");

            // STEP 3: Create order within transaction
            log.debug("[FLOW] Step 3: Creating order");
            TicketOrder tickerOrderPlace = new TicketOrder();
            int userId = ThreadLocalRandom.current().nextInt(1, 10);
            tickerOrderPlace.setUserId(userId);
            tickerOrderPlace.setOrderNumber("OKX-SGN-" + userId + "-" + System.currentTimeMillis());
            tickerOrderPlace.setTotalAmount(new BigDecimal(quantity * 5000));
            tickerOrderPlace.setTerminalId("OKX-SGN");
            tickerOrderPlace.setOrderNotes("Order -> Pending");
            String nTable = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

            orderDeductionDomainService.insertOrder(nTable, tickerOrderPlace);
            log.info("[FLOW] Step 3 SUCCESS: Order created, orderId={}, table={}", 
                tickerOrderPlace.getId(), nTable);

            log.info("[FLOW] COMPLETED: decreaseStockCAS successful for ticketId={}", ticketId);
            return true;
            
        } catch (PessimisticLockException e) {
            log.warn("[FLOW] FAILED: Pessimistic lock timeout for ticketId={}, rolling back Redis", ticketId, e);
            redisCompensatingTransaction.increaseStockCache(ticketId, quantity);
            redisCompensatingTransaction.logCompensation(ticketId, "decreaseStockCAS", 
                "Pessimistic lock timeout: " + e.getMessage());
            return false;
            
        } catch (LockTimeoutException e) {
            log.error("[FLOW] FAILED: Lock timeout for ticketId={}, rolling back Redis", ticketId, e);
            redisCompensatingTransaction.increaseStockCache(ticketId, quantity);
            redisCompensatingTransaction.logCompensation(ticketId, "decreaseStockCAS", 
                "Lock timeout: " + e.getMessage());
            return false;
            
        } catch (Exception e) {
            log.error("[FLOW] FAILED: Unexpected error for ticketId={}, rolling back Redis", ticketId, e);
            // Try to restore Redis on any exception
            try {
                redisCompensatingTransaction.increaseStockCache(ticketId, quantity);
                redisCompensatingTransaction.logCompensation(ticketId, "decreaseStockCAS", 
                    "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            } catch (Exception compensationError) {
                log.error("[FLOW] CRITICAL: Failed to compensate Redis rollback for ticketId={}", 
                    ticketId, compensationError);
            }
            return false;
        }
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketOrderDomainService.getStockAvailable(ticketId);
    }
    @Override
    public List<TicketOrderDTO> findAll(String yearMonth) {
        List<Object[]> results = orderDeductionDomainService.findAll(yearMonth);
        return results.stream().map(row -> new TicketOrderDTO(
                (Integer) row[0],
                (Integer) row[1],
                (String) row[2],
                (BigDecimal) row[3],
                (String) row[4],
                ((Timestamp) row[5]).toLocalDateTime(),
                (String) row[6],
                ((Timestamp) row[7]).toLocalDateTime(),
                ((Timestamp) row[8]).toLocalDateTime()
        )).toList();
    }
    @Override
    public boolean insertOrder(String yearMonth,TicketOrder ticketOrder) {
        orderDeductionDomainService.insertOrder(yearMonth, ticketOrder);
        return true;
    }
    
    @Override
    public TicketOrderDTO findByOrderNumber(String yearMonth, String orderNumber) {
        String nTable = extractYearMonthFromOrderNumber(orderNumber);
        log.info("nTable: findByOrderNumber ={}", nTable);
        Object[] row = orderDeductionDomainService.findByOrderNumber(nTable, orderNumber);
        if(row == null){
            return null;
        }
        return new TicketOrderDTO(
                ((Number) row[0]).intValue(),
                ((Number) row[1]).intValue(),
                (String) row[2],
                (BigDecimal) row[3],
                (String) row[4],
                ((Timestamp) row[5]).toLocalDateTime(),
                (String) row[6],
                ((Timestamp) row[7]).toLocalDateTime(),
                ((Timestamp) row[8]).toLocalDateTime()
        );
    }

    private String extractYearMonthFromOrderNumber(String orderNumber) {
        try {
            String[] parts = orderNumber.split("-");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid order number format");
            }
            long timestamp = Long.parseLong(parts[parts.length - 1]);
            LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            return dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract yearMonth from orderNumber: " + orderNumber, e);
        }
    }
}

