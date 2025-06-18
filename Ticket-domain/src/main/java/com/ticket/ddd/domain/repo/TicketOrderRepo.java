package com.ticket.ddd.domain.repo;

public interface TicketOrderRepo {
    boolean decreaseStockCas(Long ticketId,int oldStockAvailable,int quantity);
    int getStockAvailable(Long ticketId);
    boolean decreaseStock(Long ticketId,int quantity);

}
