package com.fraudshield.risk;

import com.fraudshield.risk.policy.CostPolicy;
import com.fraudshield.risk.policy.PolicyEvaluator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyEvaluatorTest {
    @Test void reportsAuditableCostsAndFriction() {
        var data = List.of(new PolicyEvaluator.Observation(.95, true, 1000), new PolicyEvaluator.Observation(.9, false, 500), new PolicyEvaluator.Observation(.1, true, 2000));
        var r = PolicyEvaluator.evaluate(data, CostPolicy.defaults(), .5, .85);
        assertThat(r.blocked()).isEqualTo(2); assertThat(r.frictionRate()).isEqualTo(2.0 / 3); assertThat(r.fraudLossPrevented()).isEqualTo(1000.0); assertThat(r.totalCost()).isEqualTo(2250.0);
    }
    @Test void sweepHonoursMaximumFriction() {
        var r = PolicyEvaluator.select(List.of(new PolicyEvaluator.Observation(.9, false, 100)), new CostPolicy(1,.5,75,250,1,0));
        assertThat(r.frictionRate()).isZero();
    }
}
