package com.ticket.ddd.application.service.order;

import java.util.List;

import com.ticket.ddd.application.model.TicketOrderDTO;
import com.ticket.ddd.domain.model.entity.TicketOrder;

public interface TicketOrderAppService {
    boolean decreaseStockCAS(Long ticketId, int quantity);

    int getStockAvailable(Long ticketId);

    // order..
    List<TicketOrderDTO> findAll(String yearMonth);
    boolean insertOrder(String yearMonth, TicketOrder ticketOrder);
    TicketOrderDTO findByOrderNumber(String yearMonth, String orderNumber);

}
