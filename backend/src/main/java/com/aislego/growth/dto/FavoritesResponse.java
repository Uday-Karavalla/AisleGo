package com.aislego.growth.dto;

import java.util.Set;

public record FavoritesResponse(Set<Long> productIds, Set<Long> supermarketIds) {
}
