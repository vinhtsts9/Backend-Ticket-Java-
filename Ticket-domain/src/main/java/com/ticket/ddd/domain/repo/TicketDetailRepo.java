package com.ticket.ddd.domain.repo;

import com.ticket.ddd.domain.model.entity.TicketDetail;

import java.util.Optional;

public interface TicketDetailRepo {
    Optional<TicketDetail> findById(Long id);
}
