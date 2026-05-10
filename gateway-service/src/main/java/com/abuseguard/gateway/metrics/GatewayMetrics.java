package com.abuseguard.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GatewayMetrics {

    private final Counter requestsCounter;
    private final Counter blockedCounter;
    private final Counter rateLimitedCounter;
    private final Counter flaggedCounter;
    private final Counter kafkaDroppedCounter;
    private final Counter redisErrorsCounter;
    private final Timer requestDurationTimer;

    public GatewayMetrics(MeterRegistry registry) {
        this.requestsCounter = Counter.builder("gateway_requests_total")
            .description("Total number of requests processed")
            .register(registry);

        this.blockedCounter = Counter.builder("gateway_blocked_total")
            .description("Total number of blocked requests")
            .register(registry);

        this.rateLimitedCounter = Counter.builder("gateway_ratelimit_exceeded_total")
            .description("Total number of rate limited requests")
            .register(registry);

        this.flaggedCounter = Counter.builder("gateway_flagged_total")
            .description("Total number of flagged requests")
            .register(registry);

        this.kafkaDroppedCounter = Counter.builder("gateway_kafka_dropped_total")
            .description("Total number of dropped Kafka events")
            .register(registry);

        this.redisErrorsCounter = Counter.builder("gateway_redis_errors_total")
            .description("Total number of Redis errors")
            .register(registry);

        this.requestDurationTimer = Timer.builder("gateway_request_duration_seconds")
            .description("Request processing duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public void incrementRequests() {
        requestsCounter.increment();
    }

    public void incrementBlocked() {
        blockedCounter.increment();
    }

    public void incrementRateLimited() {
        rateLimitedCounter.increment();
    }

    public void incrementFlagged() {
        flaggedCounter.increment();
    }

    public void incrementKafkaDropped() {
        kafkaDroppedCounter.increment();
    }

    public void incrementRedisErrors() {
        redisErrorsCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordDuration(Timer.Sample sample) {
        sample.stop(requestDurationTimer);
    }
}