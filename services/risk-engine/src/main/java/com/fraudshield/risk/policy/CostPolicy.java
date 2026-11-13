package com.fraudshield.risk.policy;

import com.fraudshield.contracts.Decision;

/** Auditable INR assumptions used only for decision-policy evaluation. */
public record CostPolicy(double fraudLossPreventedBlock, double fraudLossPreventedReview,
                         double genuineFrictionReview, double genuineFrictionBlock,
                         double missedFraudLossRate, double maxFrictionRate) {
    public CostPolicy {
        if (fraudLossPreventedBlock < 0 || fraudLossPreventedReview < 0 || genuineFrictionReview < 0
                || genuineFrictionBlock < 0 || missedFraudLossRate < 0 || maxFrictionRate < 0 || maxFrictionRate > 1)
            throw new IllegalArgumentException("Cost assumptions must be non-negative and maxFrictionRate must be <= 1");
    }

    public static CostPolicy defaults() {
        return new CostPolicy(1.0, 0.65, 75, 250, 1.0, 0.08);
    }

    public double prevented(Decision d, double fraudValue) {
        return fraudValue * (d == Decision.BLOCK ? fraudLossPreventedBlock : d == Decision.REVIEW ? fraudLossPreventedReview : 0);
    }
}
