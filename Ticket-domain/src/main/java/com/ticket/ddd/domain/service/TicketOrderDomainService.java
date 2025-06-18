package com.ticket.ddd.domain.service;

public interface TicketOrderDomainService {
    boolean decreaseStockCas(Long ticketId,int oldStockAvailable,int quantity);
    boolean decreaseStock(Long ticketId,int quantity);
    int getStockAvailable(Long ticketId);
}
