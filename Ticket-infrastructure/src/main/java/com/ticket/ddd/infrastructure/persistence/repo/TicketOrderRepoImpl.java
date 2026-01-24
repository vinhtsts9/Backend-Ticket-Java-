package com.ticket.ddd.infrastructure.persistence.repo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ticket.ddd.domain.model.entity.TicketDetail;
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
    public boolean decreaseStockWithPessimisticLock(Long ticketId, int quantity) {
        try {
            var ticketOptional = ticketOrderJPAMapper.findByIdWithPessimisticLock(ticketId);
            
            if (ticketOptional.isEmpty()) {
                log.warn("[PESSIMISTIC-LOCK] Ticket not found: {}", ticketId);
                return false;
            }
            
            TicketDetail ticket = ticketOptional.get();
            
            if (ticket.getStockAvailable() < quantity) {
                log.info("[PESSIMISTIC-LOCK] Insufficient stock: ticketId={}, required={}, available={}", 
                    ticketId, quantity, ticket.getStockAvailable());
                return false;
            }
            
            // Update stock within transaction (pessimistic lock still held)
            ticket.setStockAvailable(ticket.getStockAvailable() - quantity);
            ticketOrderJPAMapper.save(ticket);
            
            log.info("[PESSIMISTIC-LOCK] Stock decreased: ticketId={}, quantity={}, remaining={}", 
                ticketId, quantity, ticket.getStockAvailable());
            
            return true;
        } catch (Exception e) {
            log.error("[PESSIMISTIC-LOCK] Failed to decrease stock with lock: ticketId={}, quantity={}", 
                ticketId, quantity, e);
            return false;
        }
    }
    @Override
    public int getStockAvailable(Long ticketID) {
        return ticketOrderJPAMapper.getStockAvailable(ticketID);
    }
}

