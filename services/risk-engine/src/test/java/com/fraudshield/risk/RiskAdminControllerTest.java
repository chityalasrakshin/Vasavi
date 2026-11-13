package com.fraudshield.risk;

import com.fraudshield.risk.api.RiskAdminController;
import com.fraudshield.risk.evaluation.EvaluationService;
import com.fraudshield.risk.graph.GraphService;
import com.fraudshield.risk.graph.MuleAnalyzer;
import com.fraudshield.risk.model.ModelRegistry;
import com.fraudshield.risk.policy.CostPolicy;
import com.fraudshield.risk.policy.CostPolicyService;
import com.fraudshield.risk.scoring.RuntimeThresholds;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RiskAdminControllerTest {
    @Test void exposesCostPolicyAndEvaluationContracts() {
        var costs = mock(CostPolicyService.class); var eval = new EvaluationService();
        var expected = CostPolicy.defaults(); org.mockito.Mockito.when(costs.current()).thenReturn(expected);
        var controller = new RiskAdminController(mock(ModelRegistry.class), mock(RuntimeThresholds.class), mock(GraphService.class), mock(MuleAnalyzer.class), mock(StringRedisTemplate.class), costs, eval);
        assertThat(controller.costPolicy()).isEqualTo(expected);
        assertThat(controller.evaluation(99, false)).containsKeys("timeSplit", "psi", "coldStartCohorts");
    }

    @Test void exposesTheFullNetworkGraphContract() {
        var graph = new GraphService.Graph(null, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of("synthetic", true));
        var service = mock(GraphService.class);
        org.mockito.Mockito.when(service.overview(40)).thenReturn(graph);
        var controller = new RiskAdminController(mock(ModelRegistry.class), mock(RuntimeThresholds.class), service, mock(MuleAnalyzer.class), mock(StringRedisTemplate.class), mock(CostPolicyService.class), new EvaluationService());
        assertThat(controller.graphOverview(40)).isSameAs(graph);
    }
}
