package com.aislego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

/**
 * AisleGo backend entrypoint.
 *
 * Built as a modular monolith: {@code common}, {@code identity}, {@code catalogue},
 * {@code inventory}, {@code orders} and {@code payments} are separate packages with
 * their own entities/services/controllers so they can be extracted into microservices
 * later without a rewrite. This build implements only the "first working flow":
 * store discovery -> browse catalogue -> add to cart -> checkout -> order placed.
 */
@SpringBootApplication
public class AisleGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AisleGoApplication.class, args);
    }

    /**
     * Clock/zone injected wherever "now" is needed (e.g. store opening-hours checks in
     * {@code StoreDiscoveryService}) so tests can substitute a fixed {@link Clock} instead of
     * depending on real wall-clock time. Fixed to {@code Asia/Kolkata} rather than
     * {@link Clock#systemDefaultZone()}: all seed data is Bengaluru, and containers run in
     * UTC by default, so opening/closing-hour comparisons would otherwise be off by the
     * IST offset (stores would show closed during real business hours for ~5.5h/day). A real
     * per-branch/per-city timezone is future work once AisleGo operates outside one city.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
