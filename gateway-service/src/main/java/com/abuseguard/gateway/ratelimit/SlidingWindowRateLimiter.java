package com.abuseguard.gateway.ratelimit;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
public class SlidingWindowRateLimiter {

    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])
        local member = ARGV[4]
        redis.call('ZADD', key, now, member)
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)
        redis.call('EXPIRE', key, window + 1)
        local count = redis.call('ZCARD', key)
        return {count, limit}
        """;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List> script;

    public SlidingWindowRateLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);
    }

    public Mono<RateLimitResult> isAllowed(String key, int windowSeconds, int limit) {
        long now = Instant.now().toEpochMilli();
        String uniqueMember = now + ":" + Math.random();

        List<String> keys = List.of(key);
        List<String> args = List.of(
            String.valueOf(now),
            String.valueOf(windowSeconds),
            String.valueOf(limit),
            uniqueMember
        );

        return redisTemplate.execute(this.script, keys, args)
            .next()
            .map(result -> {
                @SuppressWarnings("unchecked")
                List<Long> counts = (List<Long>) result;
                long currentCount = counts.get(0);
                long limitValue = counts.get(1);
                boolean allowed = currentCount <= limitValue;
                long remaining = Math.max(0, limitValue - currentCount);
                return new RateLimitResult(allowed, currentCount, limitValue, remaining);
            })
            .onErrorReturn(new RateLimitResult(true, 0, limit, limit));
    }

    public Mono<RateLimitResult> isAllowedPerUser(String apiKey, int windowSeconds, int limit) {
        return isAllowed("rl:user:" + apiKey + ":" + windowSeconds + "s", windowSeconds, limit);
    }

    public Mono<RateLimitResult> isAllowedPerIp(String ip, int windowSeconds, int limit) {
        return isAllowed("rl:ip:" + ip + ":" + windowSeconds + "s", windowSeconds, limit);
    }
}