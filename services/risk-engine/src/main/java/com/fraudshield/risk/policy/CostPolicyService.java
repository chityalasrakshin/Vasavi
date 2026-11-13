package com.fraudshield.risk.policy;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CostPolicyService {
    private static final String KEY = "risk:cost-policy";
    private final StringRedisTemplate redis;
    private final AtomicReference<CostPolicy> active = new AtomicReference<>(CostPolicy.defaults());

    public CostPolicyService(StringRedisTemplate redis) { this.redis = redis; }
    public CostPolicy current() { return active.get(); }
    public CostPolicy update(CostPolicy p) {
        redis.opsForHash().putAll(KEY, Map.of("blockPrevented", p.fraudLossPreventedBlock(), "reviewPrevented", p.fraudLossPreventedReview(),
                "reviewFriction", p.genuineFrictionReview(), "blockFriction", p.genuineFrictionBlock(),
                "missedFraudRate", p.missedFraudLossRate(), "maxFrictionRate", p.maxFrictionRate()));
        active.set(p); return p;
    }
}
