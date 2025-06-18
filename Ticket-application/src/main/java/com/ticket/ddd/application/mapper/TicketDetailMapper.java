package com.ticket.ddd.application.mapper;

import org.springframework.beans.BeanUtils;

import com.ticket.ddd.application.model.TicketDetailDTO;
import com.ticket.ddd.domain.model.entity.TicketDetail;

public class TicketDetailMapper {
    public static TicketDetailDTO mapperToTicketDTO(TicketDetail ticketDetail) {
        if (ticketDetail == null) return null;
        TicketDetailDTO ticketDetailDTO = new TicketDetailDTO();
        BeanUtils.copyProperties(ticketDetail, ticketDetailDTO);
        return ticketDetailDTO;
    };
}

