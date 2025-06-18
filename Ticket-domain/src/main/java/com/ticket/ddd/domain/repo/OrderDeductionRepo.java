package com.ticket.ddd.domain.repo;

import java.time.LocalDateTime;
import java.util.List;

import com.ticket.ddd.domain.model.entity.TicketOrder;

public interface OrderDeductionRepo {
    void insertOrder(String yearMonth, TicketOrder ticketOrder);
    List<Object[]> findAll(String yearMonth);
    Object[] findByOrderNumber(String yearMonth,String orderNumber);
    List<Object[]> findByDateRange(String yearMonth,LocalDateTime startDate,LocalDateTime endDate);
}
