package com.ticket.ddd.domain.service;

import com.ticket.ddd.domain.model.entity.TicketDetail;

public interface TicketDetailDomainService {
    TicketDetail getTicketDetailById(Long ticketId);

}
