package com.abuseguard.detector.engine;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.store.DetectorRedisStore;

import java.util.concurrent.CompletableFuture;

public interface Rule {
    String name();
    CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store);
}