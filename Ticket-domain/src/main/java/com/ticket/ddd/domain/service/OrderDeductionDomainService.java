package com.ticket.ddd.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import com.ticket.ddd.domain.model.entity.TicketOrder;

public interface OrderDeductionDomainService {
    void insertOrder(String yearMonth,TicketOrder ticketOrder);
    List<Object[]> findAll(String yearMonth);
    Object[] findByOrderNumber(String yearMonth,String orderNumber);
    List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate);
}
