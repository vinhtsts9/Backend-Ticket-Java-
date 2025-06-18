package com.ticket.ddd.infrastructure.persistence.repo;

import com.ticket.ddd.domain.model.entity.TicketOrder;
import com.ticket.ddd.domain.repo.OrderDeductionRepo;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;


@Service
@Slf4j
public class OrderDeductionInfraRepoImpl implements OrderDeductionRepo {
    @Autowired
    private EntityManager entityManager;
    private static String tablePrefix = "ticket_order_";
    private String getTableName(String monthOrder) {
        return tablePrefix + monthOrder;
    }
    @Override
    @Transactional
    public void insertOrder(String yearMonth, TicketOrder order) {
        String  tableName = getTableName(yearMonth);
        String sql = "INSERT INTO " + tableName + " (order_number, user_id, total_amount, terminal_id, order_date, order_notes, updated_at, created_at) " +
                "VALUES (:orderNumber, :userId, :totalAmount, :terminalId, :orderDate, :orderNotes, :updatedAt, :createdAt)";

        // pls: rememeber that: add log by time
        LocalDateTime now = LocalDateTime.now();
        order.setOrderDate(now);
        order.setUpdatedAt(now);
        order.setCreatedAt(now);

        entityManager.createNativeQuery(sql)
                .setParameter("orderNumber", order.getOrderNumber())
                .setParameter("userId", order.getUserId())
                .setParameter("totalAmount", order.getTotalAmount())
                .setParameter("terminalId", order.getTerminalId())
                .setParameter("orderDate", order.getOrderDate())
                .setParameter("orderNotes", order.getOrderNotes())
                .setParameter("updatedAt", order.getUpdatedAt())
                .setParameter("createdAt", order.getCreatedAt())
                .executeUpdate();
    }

    @Override
    public List<Object[]> findAll(String yearMonth) {
        String tableName = getTableName(yearMonth);
        String sql = "SELECT * FROM " + tableName;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public Object[] findByOrderNumber(String yearMonth, String orderNumber) {
        String tableName = getTableName(yearMonth);
        String sql = "SELECT * FROM " + tableName + " WHERE order_number = :orderNumber";
        List<Object[]> resultList = entityManager.createNativeQuery(sql)
                .setParameter("orderNumber", orderNumber)
                .getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }

    @Override
    public List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate) {
        String tableName = getTableName(yearMonth);
        String sql = "SELECT * FROM " + tableName + " WHERE order_date BETWEEN :startDate AND :endDate";
        return entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }
}
