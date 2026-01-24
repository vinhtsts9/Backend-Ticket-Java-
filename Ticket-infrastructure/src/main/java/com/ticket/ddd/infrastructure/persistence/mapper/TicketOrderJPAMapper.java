package com.ticket.ddd.infrastructure.persistence.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.ddd.domain.model.entity.TicketDetail;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TicketOrderJPAMapper extends JpaRepository<TicketDetail,Long> {
    @Query("SELECT t.stockAvailable FROM TicketDetail t WHERE t.id = :ticketId")
    int getStockAvailable(@Param("ticketId") Long ticketId);

    @Query("SELECT t FROM TicketDetail t WHERE t.id = :ticketId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional
    Optional<TicketDetail> findByIdWithPessimisticLock(@Param("ticketId") Long ticketId);

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
            "t.stockAvailable = t.stockAvailable - :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity")
    int decreaseStock(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " + 
            "t.stockAvailable = :oldStockAvailable - :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable = :oldStockAvailable")
    int decreaseStockCas(@Param("ticketId") Long ticketId,@Param("oldStockAvailable") int oldStockAvailable,@Param("quantity") int quantity);
}
