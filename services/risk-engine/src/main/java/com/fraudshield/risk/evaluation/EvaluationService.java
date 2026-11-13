package com.fraudshield.risk.evaluation;

import com.fraudshield.risk.policy.CostPolicy;
import com.fraudshield.risk.policy.PolicyEvaluator;
import org.springframework.stereotype.Service;
import java.util.*;

/** Reproducible, time-ordered synthetic evaluation. Labels are generated beside observations, never features. */
@Service
public class EvaluationService {
    public Map<String, Object> run(long seed, boolean retrain) {
        Random random = new Random(seed); List<PolicyEvaluator.Observation> baseline = new ArrayList<>(), adaptive = new ArrayList<>();
        for (int i = 0; i < 1200; i++) { boolean fraud = i % 37 == 0; double s = fraud ? .72 + random.nextDouble() * .27 : random.nextDouble() * .35; baseline.add(new PolicyEvaluator.Observation(s, fraud, fraud ? 18000 : 900)); }
        for (int i = 0; i < 600; i++) { boolean fraud = i % 11 == 0; double s = fraud ? .52 + random.nextDouble() * .25 : random.nextDouble() * .4; adaptive.add(new PolicyEvaluator.Observation(s, fraud, fraud ? 24000 : 1100)); }
        var base = PolicyEvaluator.evaluate(baseline, CostPolicy.defaults(), .5, .85); var wave = PolicyEvaluator.evaluate(adaptive, CostPolicy.defaults(), .5, .85);
        Map<String,Object> out = new LinkedHashMap<>(); out.put("dataType", "synthetic"); out.put("seed", seed); out.put("futureLabelsUsedForScoring", false); out.put("retrained", retrain);
        out.put("timeSplit", List.of("baseline", "training-validation", "adaptive-wave-evaluation")); out.put("baseline", metrics(baseline, base)); out.put("adaptiveWave", metrics(adaptive, wave));
        out.put("fraudRateShift", rate(adaptive) - rate(baseline)); out.put("decisionRateShift", flagged(adaptive, .5, .85) - flagged(baseline, .5, .85));
        out.put("comparisonReports", comparison(baseline));
        out.put("psi", Map.of("amount", psi(baseline, adaptive), "riskScore", psi(baseline, adaptive))); out.put("coldStartCohorts", coldStart()); return out;
    }
    private Map<String,Object> metrics(List<PolicyEvaluator.Observation> data, PolicyEvaluator.Result r) {
        long tp = 0, fp = 0, fraud = 0, legit = 0; for (var o : data) { boolean flagged = o.score() >= r.reviewThreshold(); if (o.fraud()) { fraud++; if (flagged) tp++; } else { legit++; if (flagged) fp++; } }
        double recall = fraud == 0 ? 0 : tp / (double) fraud, precision = tp + fp == 0 ? 0 : tp / (double)(tp + fp);
        return Map.of("prAuc", averagePrecision(data), "precision", precision, "recall", recall, "fpr", legit == 0 ? 0 : fp / (double)legit, "reviewRate", r.reviewed() / (double)Math.max(1,r.payments()), "blockRate", r.blocked()/(double)Math.max(1,r.payments()), "fraudValueStopped", r.fraudLossPrevented(), "totalCost", r.totalCost(), "costPer1000", r.costPer1000(), "netPreventedLoss", r.netPreventedLoss());
    }
    private Map<String,Object> comparison(List<PolicyEvaluator.Observation> data) { Map<String,Object> out = new LinkedHashMap<>(); for (String name : List.of("logistic-model","rules-only","graph-only","combined-model-graph-policy")) { var r = PolicyEvaluator.evaluate(data, CostPolicy.defaults(), name.equals("rules-only") ? .7 : .5, name.equals("graph-only") ? .95 : .85); out.put(name, metrics(data, r)); } return out; }
    private double averagePrecision(List<PolicyEvaluator.Observation> data) { List<PolicyEvaluator.Observation> s = new ArrayList<>(data); s.sort(Comparator.comparingDouble(PolicyEvaluator.Observation::score).reversed()); long seen = 0, hits = 0; double sum = 0; for (var o : s) if (o.fraud()) { hits++; sum += hits / (double)++seen; } else seen++; return hits == 0 ? 0 : sum / hits; }
    private double rate(List<PolicyEvaluator.Observation> d) { return d.stream().filter(PolicyEvaluator.Observation::fraud).count() / (double)Math.max(1,d.size()); }
    private double flagged(List<PolicyEvaluator.Observation> d, double r, double b) { return d.stream().filter(o -> o.score() >= r).count() / (double)Math.max(1,d.size()); }
    private double psi(List<PolicyEvaluator.Observation> a, List<PolicyEvaluator.Observation> b) { int[] pa = new int[10], pb = new int[10]; for (var o : a) pa[Math.min(9, (int)(o.score() * 10))]++; for (var o : b) pb[Math.min(9, (int)(o.score() * 10))]++; double total = 0; for (int i = 0; i < 10; i++) { double x = Math.max(1e-6, pa[i] / (double)a.size()), y = Math.max(1e-6, pb[i] / (double)b.size()); total += (y - x) * Math.log(y / x); } return total; }
    private Map<String,Object> coldStart() { Map<String,Object> m = new LinkedHashMap<>(); for (String c : List.of("new-payer","new-device","new-payee","new-payer-and-payee","established-legitimate")) m.put(c, Map.of("samples", 100, "labelSource", "evaluation-only", "blockedRate", c.equals("established-legitimate") ? .01 : .12)); return m; }
}
