package com.ticket.ddd.controller.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.ddd.application.model.TicketOrderDTO;
import com.ticket.ddd.application.service.order.TicketOrderAppService;
import com.ticket.ddd.controller.model.enums.ResultUtil;
import com.ticket.ddd.controller.model.vo.ResultMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/order")
@Slf4j
public class TicketOrderController {
    @Autowired
    private TicketOrderAppService ticketOrderAppService;
    @GetMapping("/cas/{ticketId}/{quantity}")
    public boolean orderTicketByLevel(
        @PathVariable("ticketId") Long ticketId,
        @PathVariable("quantity") int quantity
    ) {

        return ticketOrderAppService.decreaseStockCAS(ticketId, quantity);
    }
    @GetMapping("/{userId}/list")
    public ResultMessage<List<TicketOrderDTO>> getListOrderByUser(
            @PathVariable("userId") Long userId,
            @RequestParam("ntable") String ntable
    ) {
        log.info("Controller:->getListOrderByUser | {}, {}", userId, ntable);
        return ResultUtil.data(ticketOrderAppService.findAll(ntable));
    }

    // get orderItem
    @GetMapping("/{userId}/{orderNumber}")
    public ResultMessage<TicketOrderDTO> getOrderByUser(
            @PathVariable("userId") Long userId,
            @PathVariable("orderNumber") String orderNumber
    ) {
        log.info("Controller:->getOrderByUser | {}, {}", userId, orderNumber);
        return ResultUtil.data(ticketOrderAppService.findByOrderNumber("2025xx",orderNumber));
    }
}
