package com.aislego.growth.service;

import com.aislego.growth.dto.FavoritesResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;

@Service
public class FavoritesService {
    private final JdbcTemplate jdbc;

    public FavoritesService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public FavoritesResponse list(Long userId) {
        var products = new HashSet<>(jdbc.queryForList(
                "select product_id from favorite_products where user_id = ?", Long.class, userId));
        var stores = new HashSet<>(jdbc.queryForList(
                "select supermarket_id from favorite_supermarkets where user_id = ?", Long.class, userId));
        return new FavoritesResponse(products, stores);
    }

    @Transactional
    public void addProduct(Long userId, Long productId) {
        jdbc.update("insert into favorite_products(user_id, product_id, created_at) values (?, ?, ?) " +
                "on conflict do nothing", userId, productId, Timestamp.from(Instant.now()));
    }

    @Transactional
    public void removeProduct(Long userId, Long productId) {
        jdbc.update("delete from favorite_products where user_id = ? and product_id = ?", userId, productId);
    }

    @Transactional
    public void addStore(Long userId, Long supermarketId) {
        jdbc.update("insert into favorite_supermarkets(user_id, supermarket_id, created_at) values (?, ?, ?) " +
                "on conflict do nothing", userId, supermarketId, Timestamp.from(Instant.now()));
    }

    @Transactional
    public void removeStore(Long userId, Long supermarketId) {
        jdbc.update("delete from favorite_supermarkets where user_id = ? and supermarket_id = ?", userId, supermarketId);
    }
}
