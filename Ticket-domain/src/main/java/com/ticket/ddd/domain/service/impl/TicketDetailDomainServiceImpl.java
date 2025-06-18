package com.ticket.ddd.domain.service.impl;

import com.ticket.ddd.domain.model.entity.TicketDetail;
import com.ticket.ddd.domain.repo.TicketDetailRepo;
import com.ticket.ddd.domain.service.TicketDetailDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketDetailDomainServiceImpl implements TicketDetailDomainService {
    @Autowired
    private TicketDetailRepo ticketDetailRepo;
    @Override
    public TicketDetail getTicketDetailById(Long ticketId) {
        return ticketDetailRepo.findById(ticketId).orElse(null);

    }

}
