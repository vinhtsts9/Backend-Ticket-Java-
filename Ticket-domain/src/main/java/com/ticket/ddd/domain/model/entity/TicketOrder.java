package com.ticket.ddd.domain.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TicketOrder {
    private Integer id;
    private String orderNumber;
    private Integer userId;
    private BigDecimal totalAmount;
    private String terminalId;
    private LocalDateTime orderDate;
    private String orderNotes;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
