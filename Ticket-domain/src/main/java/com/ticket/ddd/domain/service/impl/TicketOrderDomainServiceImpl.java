package com.ticket.ddd.domain.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ticket.ddd.domain.repo.TicketOrderRepo;
import com.ticket.ddd.domain.service.TicketOrderDomainService;

@Service
public class TicketOrderDomainServiceImpl implements TicketOrderDomainService {
    @Autowired
    private TicketOrderRepo ticketOrderRepo;

    @Override
    public boolean decreaseStockCas(Long ticketId,int oldStockAvailable,int quantity) {
        return ticketOrderRepo.decreaseStockCas(ticketId,oldStockAvailable,quantity);
    }
    @Override
    public boolean decreaseStock(Long ticketId,int quantity) {
        return ticketOrderRepo.decreaseStock(ticketId,quantity);
    }
    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketOrderRepo.getStockAvailable(ticketId);
    }
}
