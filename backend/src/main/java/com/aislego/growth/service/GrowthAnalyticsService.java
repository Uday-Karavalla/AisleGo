package com.aislego.growth.service;

import com.aislego.common.exception.BadRequestException;
import com.aislego.growth.domain.GrowthEvent;
import com.aislego.growth.dto.GrowthDashboardResponse;
import com.aislego.growth.dto.TrackEventRequest;
import com.aislego.growth.repository.GrowthEventRepository;
import com.aislego.identity.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Set;

@Service
public class GrowthAnalyticsService {
    private static final Set<String> ALLOWED = Set.of("page_view", "store_view", "search", "product_view",
            "add_to_cart", "coupon_apply", "begin_checkout", "purchase", "share", "pwa_install");
    private final GrowthEventRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public GrowthAnalyticsService(GrowthEventRepository repository, UserRepository userRepository,
                                  ObjectMapper objectMapper, JdbcTemplate jdbc) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    @Transactional
    public void track(Long userId, TrackEventRequest request) {
        if (!ALLOWED.contains(request.eventName())) throw new BadRequestException("Unsupported analytics event");
        GrowthEvent event = new GrowthEvent();
        event.setEventName(request.eventName());
        if (userId != null) event.setUser(userRepository.getReferenceById(userId));
        event.setSessionId(request.sessionId());
        try {
            String metadata = request.metadata() == null ? null : objectMapper.writeValueAsString(request.metadata());
            event.setMetadata(metadata != null && metadata.length() > 1000 ? metadata.substring(0, 1000) : metadata);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Analytics metadata is invalid");
        }
        event.setCreatedAt(Instant.now());
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public GrowthDashboardResponse dashboard(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        long visitors = count("select count(distinct coalesce(session_id, 'user-' || user_id::text)) from growth_events where created_at >= ?", since);
        long storeViews = countEvent("store_view", since);
        long searches = countEvent("search", since);
        long carts = countEvent("add_to_cart", since);
        long checkouts = countEvent("begin_checkout", since);
        long purchases = countEvent("purchase", since);
        long coupons = countEvent("coupon_apply", since);
        double conversion = visitors == 0 ? 0 : Math.round((purchases * 10000.0 / visitors)) / 100.0;
        LinkedHashMap<String, Long> daily = new LinkedHashMap<>();
        jdbc.query("select created_at::date::text, count(*) from growth_events where event_name = 'purchase' " +
                        "and created_at >= ? group by created_at::date order by created_at::date",
                (rs, rowNum) -> java.util.Map.entry(rs.getString(1), rs.getLong(2)),
                java.sql.Timestamp.from(since)).forEach(entry -> daily.put(entry.getKey(), entry.getValue()));
        return new GrowthDashboardResponse(safeDays, visitors, storeViews, searches, carts, checkouts,
                purchases, coupons, conversion, daily);
    }

    private long countEvent(String event, Instant since) {
        return count("select count(*) from growth_events where event_name = ? and created_at >= ?", event, since);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }
}
