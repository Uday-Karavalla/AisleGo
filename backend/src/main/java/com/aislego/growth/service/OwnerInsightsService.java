package com.aislego.growth.service;

import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.NotFoundException;
import com.aislego.growth.dto.OwnerInsightsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OwnerInsightsService {
    private final SupermarketRepository supermarketRepository;
    private final JdbcTemplate jdbc;

    public OwnerInsightsService(SupermarketRepository supermarketRepository, JdbcTemplate jdbc) {
        this.supermarketRepository = supermarketRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public OwnerInsightsResponse get(Long ownerId) {
        Long supermarketId = supermarketRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No supermarket is registered to this account")).getId();
        Timestamp since = Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS));
        var totals = jdbc.queryForMap("select count(*) as orders, coalesce(sum(total_amount), 0) as revenue " +
                "from orders where supermarket_id = ? and status <> 'CANCELLED' and created_at >= ?", supermarketId, since);
        long orders = ((Number) totals.get("orders")).longValue();
        BigDecimal revenue = new BigDecimal(totals.get("revenue").toString());
        BigDecimal average = orders == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        Long lowStock = jdbc.queryForObject("select count(*) from inventory i join branches b on b.id = i.branch_id " +
                "where b.supermarket_id = ? and i.quantity_on_hand <= 5", Long.class, supermarketId);
        var top = jdbc.query("select oi.product_name, sum(oi.quantity) units from order_items oi join orders o on o.id = oi.order_id " +
                        "where o.supermarket_id = ? and o.status <> 'CANCELLED' and o.created_at >= ? " +
                        "group by oi.product_name order by units desc limit 5",
                (rs, rowNum) -> new OwnerInsightsResponse.TopProduct(rs.getString(1), rs.getLong(2)), supermarketId, since);
        return new OwnerInsightsResponse(orders, revenue, average, lowStock == null ? 0 : lowStock, top);
    }
}
