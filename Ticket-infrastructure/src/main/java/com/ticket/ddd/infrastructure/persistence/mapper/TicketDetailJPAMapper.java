package com.ticket.ddd.infrastructure.persistence.mapper;

import com.ticket.ddd.domain.model.entity.TicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketDetailJPAMapper extends JpaRepository<TicketDetail,Long> {
    Optional<TicketDetail> findById(Long id);
}
