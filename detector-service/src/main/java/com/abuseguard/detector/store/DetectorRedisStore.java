package com.abuseguard.detector.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class DetectorRedisStore {

    private final StringRedisTemplate redisTemplate;

    public DetectorRedisStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordTimestamp(String key, long timestamp) {
        redisTemplate.opsForZSet().add(key, String.valueOf(timestamp), timestamp);
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }

    public long countInWindow(String key, long fromTimestamp, long toTimestamp) {
        return redisTemplate.opsForZSet().count(key, fromTimestamp, toTimestamp);
    }

    public void removeOldEntries(String key, long cutoffTimestamp) {
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoffTimestamp);
    }

    public void addToSet(String key, String member) {
        redisTemplate.opsForSet().add(key, member);
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }

    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    public void pushToList(String key, String value, int maxSize) {
        redisTemplate.opsForList().rightPush(key, value);
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > maxSize) {
            redisTemplate.opsForList().leftPop(key);
        }
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }

    public List<String> getList(String key, int start, int end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    public void addToHyperLogLog(String key, String... values) {
        redisTemplate.opsForHyperLogLog().add(key, values);
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }

    public long hyperLogLogSize(String key) {
        Long size = redisTemplate.opsForHyperLogLog().size(key);
        return size != null ? size : 0;
    }

    public void recordSpikeData(String ip, long timestamp) {
        String shortKey = "spike:short:" + ip;
        String longKey = "spike:long:" + ip;

        recordTimestamp(shortKey, timestamp);
        recordTimestamp(longKey, timestamp);

        long shortCutoff = timestamp - 10_000;
        long longCutoff = timestamp - 60_000;

        removeOldEntries(shortKey, shortCutoff);
        removeOldEntries(longKey, longCutoff);
    }

    public long getSpikeShortCount(String ip, long timestamp) {
        String key = "spike:short:" + ip;
        long cutoff = timestamp - 10_000;
        removeOldEntries(key, cutoff);
        return countInWindow(key, cutoff, timestamp);
    }

    public long getSpikeLongCount(String ip, long timestamp) {
        String key = "spike:long:" + ip;
        long cutoff = timestamp - 60_000;
        removeOldEntries(key, cutoff);
        return countInWindow(key, cutoff, timestamp);
    }

    public void recordBruteForceAttempt(String endpoint, String ip, long timestamp) {
        String key = "bf:" + endpoint.hashCode() + ":" + ip;
        recordTimestamp(key, timestamp);

        String endpointKey = "bf:endpoint:" + ip;
        addToSet(endpointKey, endpoint);
    }

    public long getBruteForceCount(String endpoint, String ip, long timestamp) {
        String key = "bf:" + endpoint.hashCode() + ":" + ip;
        long cutoff = timestamp - 60_000;
        removeOldEntries(key, cutoff);
        return countInWindow(key, cutoff, timestamp);
    }

    public void recordBotTimestamp(String apiKey, long timestamp) {
        String key = "bot:ts:" + apiKey;
        pushToList(key, String.valueOf(timestamp), 20);
    }

    public List<String> getBotTimestamps(String apiKey) {
        String key = "bot:ts:" + apiKey;
        return getList(key, 0, -1);
    }

    public void recordIpEndpoint(String ip, String endpoint, long timestamp) {
        int bucket = (int) (timestamp / 300_000);
        String key = "ip:hll:" + ip + ":" + bucket;
        addToHyperLogLog(key, endpoint);
    }

    public long getIpUniqueEndpoints(String ip, long timestamp) {
        int bucket = (int) (timestamp / 300_000);
        String key = "ip:hll:" + ip + ":" + bucket;
        return hyperLogLogSize(key);
    }

    public void recordSequence(String apiKey, String endpoint) {
        String key = "seq:" + apiKey;
        pushToList(key, endpoint, 30);
    }

    public List<String> getSequence(String apiKey) {
        String key = "seq:" + apiKey;
        return getList(key, 0, -1);
    }
}