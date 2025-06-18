package com.ticket.ddd.application.service.ticket;

import com.ticket.ddd.application.model.TicketDetailDTO;

public interface TicketDetailAppService {
    TicketDetailDTO getTicketDetailById(Long ticketId, Long version);
    boolean orderTicketByUser(Long ticketId);
}
