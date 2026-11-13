package com.fraudshield.risk;

import com.fraudshield.risk.evaluation.EvaluationService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluationServiceTest {
    @Test void sameSeedIsReproducibleAndLabelsStayOutOfScoring() {
        var service = new EvaluationService(); var first = service.run(7, false); var second = service.run(7, false);
        assertThat(first).isEqualTo(second); assertThat(first).containsEntry("dataType", "synthetic").containsEntry("futureLabelsUsedForScoring", false);
        assertThat(first).containsKeys("psi", "coldStartCohorts", "adaptiveWave");
    }
}
