package com.ticket.ddd.domain.service.impl;

import com.ticket.ddd.domain.model.entity.TicketOrder;
import com.ticket.ddd.domain.repo.OrderDeductionRepo;
import com.ticket.ddd.domain.service.OrderDeductionDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class OrderDeductionDomainServiceImpl implements OrderDeductionDomainService {
    @Autowired
    private OrderDeductionRepo orderDeductionRepo;
    @Override
    public void insertOrder(String yearMonth, TicketOrder ticketOrder) {
        orderDeductionRepo.insertOrder(yearMonth,ticketOrder);
    }
    @Override
    public List<Object[]> findAll(String yearMonth) {
        return orderDeductionRepo.findAll(yearMonth);
    }
    @Override
    public Object[] findByOrderNumber(String yearMonth,String orderNumber) {
        return orderDeductionRepo.findByOrderNumber(yearMonth, orderNumber);
    }
    @Override
    public List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate) {
        return List.of();
    }


}
