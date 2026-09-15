package com.taskcenter.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Configuration
public class RateLimitConfig {

    @Bean
    public Supplier<Bucket> authRateLimiter() {
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    @Bean
    public java.util.Map<String, io.github.bucket4j.Bucket> bucketCache() {
        return new ConcurrentHashMap<>();
    }
}