package com.ticket.ddd.application.service.ticket.impl;

import com.ticket.ddd.application.mapper.TicketDetailMapper;
import com.ticket.ddd.application.model.TicketDetailDTO;
import com.ticket.ddd.application.model.cache.TicketDetailCache;
import com.ticket.ddd.application.service.ticket.TicketDetailAppService;
import com.ticket.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.ticket.ddd.domain.service.TicketDetailDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TicketDetailAppServiceImpl implements TicketDetailAppService {
    @Autowired
    private TicketDetailDomainService ticketDetailDomainService;
    @Autowired
    private TicketDetailCacheService ticketDetailCacheService;
    
    @Override
    public TicketDetailDTO getTicketDetailById(Long id,Long version) {
        log.info("Implement Application : {}, {} ",id,version);
        TicketDetailCache ticketDetailCache = ticketDetailCacheService.getTicketDetail(id, version);
        TicketDetailDTO ticketDetailDTO = TicketDetailMapper.mapperToTicketDTO(ticketDetailCache.getTicketDetail());
        ticketDetailDTO.setVersion(ticketDetailCache.getVersion());
        return ticketDetailDTO;
    }
    @Override
    public boolean orderTicketByUser(Long ticketId) {
        return ticketDetailCacheService.orderTicketByUser(ticketId);
    };

}
