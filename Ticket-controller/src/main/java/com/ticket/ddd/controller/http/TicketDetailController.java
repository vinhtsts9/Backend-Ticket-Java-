package com.ticket.ddd.controller.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.ddd.application.model.TicketDetailDTO;
import com.ticket.ddd.application.service.ticket.TicketDetailAppService;
import com.ticket.ddd.controller.model.enums.ResultUtil;
import com.ticket.ddd.controller.model.vo.ResultMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ticket")
@Slf4j
public class TicketDetailController {
    @Autowired
    private TicketDetailAppService ticketDetailAppService;
    @GetMapping("/ping/java")
    public ResponseEntity<Object> ping() throws InterruptedException {
        Thread.sleep(1000);
        return ResponseEntity.status(HttpStatus.OK)
        .body(new Response("OK"));
    }
    public static class Response {
        private String status;

        public Response(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
    @GetMapping("/{ticketId}/detail/{detailId}")
    public ResultMessage<TicketDetailDTO> getTicketDetail(
        @PathVariable("ticketId") Long ticketId,
        @PathVariable("detailId") Long detailId,
        @RequestParam(name = "version", required = false) Long version
    ){
        return ResultUtil.data(ticketDetailAppService.getTicketDetailById(ticketId, version));
    }
    @GetMapping("/{ticketId}/detail/{detailId}/")
    public boolean orderTicketByUser(
        @PathVariable("ticketId") Long ticketId,
        @PathVariable("detailId") Long detailId    
    ) {
        return ticketDetailAppService.orderTicketByUser(ticketId);
    }

}
