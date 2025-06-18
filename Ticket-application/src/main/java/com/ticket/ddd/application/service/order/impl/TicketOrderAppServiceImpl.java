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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStockCAS(Long ticketId,int quantity) {
        try {
            int oldStockAvailable = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);
            if(oldStockAvailable == 0) {
                log.info("Case: oldStockAvailable is 0");
                // if oldStockAvailable = 0 or then StockInventory unavailable -> return to client
                return false;
            }
            boolean isDecreaseStockSuccess = ticketOrderDomainService.decreaseStock(ticketId, quantity);
            
            if(isDecreaseStockSuccess) {
                TicketOrder tickerOrderPlace = new TicketOrder();
                //
                int userId = ThreadLocalRandom.current().nextInt(1, 10);
                tickerOrderPlace.setUserId(userId);
                tickerOrderPlace.setOrderNumber("OKX-SGN-"+userId+"-" + System.currentTimeMillis()); //  System.currentTimeMillis() -> always -> 03 month
                tickerOrderPlace.setTotalAmount(new BigDecimal(quantity * 5000));
                tickerOrderPlace.setTerminalId("OKX-SGN");
                tickerOrderPlace.setOrderNotes("Orrder -> Pending");
                // Now how to find out which table is currently? -> 202503
//                String nTable = "202504";
                String nTable = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

                log.info("currently nTbale----> | {}", nTable);
                orderDeductionDomainService.insertOrder(nTable, tickerOrderPlace);
            }
            return true;
        } catch (PessimisticLockException e) {
            log.warn("Pessimistic Locking failed for ticketId={}", ticketId);
            return false;
        } catch (LockTimeoutException e) {
            log.error("Lock timeout while processing ticketId={}", ticketId, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error when decreasing stock for ticketId={}", ticketId, e);
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
                ((Timestamp) row[5]).toLocalDateTime(), // Chuyển Timestamp sang LocalDateTime
                (String) row[6],
                ((Timestamp) row[7]).toLocalDateTime(), // Chuyển Timestamp sang LocalDateTime
                ((Timestamp) row[8]).toLocalDateTime()  // Chuyển Timestamp sang LocalDateTime
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
//        return orderDeductionDomainService.findByOrderNumber(yearMonth, orderNumber);
        if(row == null){
            return null;
//            throw new EntityNotFoundException("Order not found: " + orderNumber);
        }
        return new TicketOrderDTO( // toMapStruct()
                ((Number) row[0]).intValue(),  // id
                ((Number) row[1]).intValue(),  // userId
                (String) row[2],               // orderNumber
                (BigDecimal) row[3],           // totalAmount
                (String) row[4],               // terminalId
                ((Timestamp) row[5]).toLocalDateTime(), // orderDate
                (String) row[6],               // orderNotes
                ((Timestamp) row[7]).toLocalDateTime(), // updatedAt
                ((Timestamp) row[8]).toLocalDateTime()  // createdAt
        );
    }
    // chuyển đổi
    private String extractYearMonthFromOrderNumber(String orderNumber) {
        try {
            // Lấy timestamp từ orderNumber
            String[] parts = orderNumber.split("-");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid order number format");
            }
            long timestamp = Long.parseLong(parts[parts.length - 1]);

            // Chuyển đổi timestamp thành LocalDateTime
            LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Format thành yyyyMM
            return dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract yearMonth from orderNumber: " + orderNumber, e);
        }
    }

}
