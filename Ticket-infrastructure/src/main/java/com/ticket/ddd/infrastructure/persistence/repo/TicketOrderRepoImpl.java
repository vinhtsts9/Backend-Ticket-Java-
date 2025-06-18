package com.ticket.ddd.infrastructure.persistence.repo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ticket.ddd.domain.repo.TicketOrderRepo;
import com.ticket.ddd.infrastructure.persistence.mapper.TicketOrderJPAMapper;

@Service
@Slf4j
public class TicketOrderRepoImpl implements TicketOrderRepo {
    @Autowired
    private TicketOrderJPAMapper ticketOrderJPAMapper;
    @Override
    public boolean decreaseStockCas(Long ticketId,int oldStockAvailable,int quantity) {
        log.info("Run test:decreaseStockLevel3CAS with: | {}, {}, {} ", ticketId, oldStockAvailable, quantity);
        return ticketOrderJPAMapper.decreaseStockCas(ticketId, oldStockAvailable, quantity) >0;
    }
    @Override
    public boolean decreaseStock(Long ticketId,int quantity) {
        return ticketOrderJPAMapper.decreaseStock(ticketId,quantity) > 0;
    }
    @Override
    public int getStockAvailable(Long ticketID) {
        return ticketOrderJPAMapper.getStockAvailable(ticketID);
    }
}
