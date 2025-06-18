package com.ticket.ddd.infrastructure.persistence.repo;

import com.ticket.ddd.domain.model.entity.Ticket;
import com.ticket.ddd.domain.model.entity.TicketDetail;
import com.ticket.ddd.domain.repo.TicketDetailRepo;
import com.ticket.ddd.infrastructure.persistence.mapper.TicketDetailJPAMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class TicketDetailInfraRepoImpl implements TicketDetailRepo {
    @Autowired
    private TicketDetailJPAMapper ticketDetailJPAMapper;
    @Override
    public Optional<TicketDetail> findById(Long id) {
        return ticketDetailJPAMapper.findById(id);
    }
}
