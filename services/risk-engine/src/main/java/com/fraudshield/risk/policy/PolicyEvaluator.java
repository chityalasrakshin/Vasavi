package com.fraudshield.risk.policy;

import com.fraudshield.contracts.Decision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic threshold sweep. Inputs must already be time-split and labelled externally. */
public final class PolicyEvaluator {
    private PolicyEvaluator() {}
    public record Observation(double score, boolean fraud, double amount) {}
    public record Result(double reviewThreshold, double blockThreshold, long payments, double fraudLossPrevented,
                         double genuineFrictionCost, double missedFraudLoss, double totalCost,
                         double costPer1000, double netPreventedLoss, double frictionRate,
                         long approved, long reviewed, long blocked) {}

    public static Result select(List<Observation> data, CostPolicy policy) {
        Result best = null;
        double[] candidates = {.05, .1, .2, .3, .4, .5, .6, .7, .8, .85, .9, .95, .99, 1.0};
        for (double review : candidates) for (double block : candidates) {
            if (review >= block) continue;
            Result candidate = evaluate(data, policy, review, block);
            if (candidate.frictionRate() <= policy.maxFrictionRate() && (best == null || candidate.totalCost() < best.totalCost())) best = candidate;
        }
        return best == null ? evaluate(data, policy, .5, .85) : best;
    }

    public static Result evaluate(List<Observation> data, CostPolicy p, double review, double block) {
        long a = 0, r = 0, b = 0; double prevented = 0, friction = 0, missed = 0;
        for (Observation o : data) {
            Decision d = o.score() >= block ? Decision.BLOCK : o.score() >= review ? Decision.REVIEW : Decision.APPROVE;
            if (d == Decision.APPROVE) a++; else if (d == Decision.REVIEW) { r++; friction += o.fraud() ? 0 : p.genuineFrictionReview(); } else { b++; friction += o.fraud() ? 0 : p.genuineFrictionBlock(); }
            if (o.fraud()) { if (d == Decision.APPROVE) missed += o.amount() * p.missedFraudLossRate(); else prevented += p.prevented(d, o.amount()); }
        }
        double total = friction + missed;
        double rate = data.isEmpty() ? 0 : (r + b) / (double) data.size();
        return new Result(review, block, data.size(), prevented, friction, missed, total, data.isEmpty() ? 0 : total * 1000 / data.size(), prevented - total, rate, a, r, b);
    }
}
