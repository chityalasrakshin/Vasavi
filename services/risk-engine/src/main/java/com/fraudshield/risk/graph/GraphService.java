package com.fraudshield.risk.graph;

import com.fraudshield.risk.feature.FeatureStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/** Breadth-first walk of the payment graph around one account, for the investigation view. */
@Service
public class GraphService {

    private static final int MAX_NODES = 60;
    private static final int MAX_NEIGHBOURS = 15;

    private final StringRedisTemplate redis;

    public GraphService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Graph around(String root, int depth) {
        return build(root, depth, false);
    }

    /** Full graph view, bounded for a safe analyst-facing dashboard. */
    public Graph overview(int limit) {
        Set<String> accounts = redis.keys("g:out:*");
        if (accounts == null || accounts.isEmpty()) return new Graph(null, List.of(), List.of(), List.of(), provenance());
        List<String> roots = accounts.stream().map(k -> k.substring("g:out:".length())).limit(Math.max(1, Math.min(limit, 200))).toList();
        Map<String, Node> nodes = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>();
        for (String root : roots) collect(root, 1, nodes, edges);
        return finish(null, nodes, edges);
    }

    private Graph build(String root, int depth, boolean ignored) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>();
        Deque<String[]> queue = new ArrayDeque<>();
        queue.add(new String[]{root, "0"});
        nodes.put(root, node(root, 0, 0, 0));

        while (!queue.isEmpty() && nodes.size() < MAX_NODES) {
            String[] cur = queue.poll();
            String vpa = cur[0];
            int level = Integer.parseInt(cur[1]);
            if (level >= depth) continue;

            Set<String> out = redis.opsForZSet().reverseRange("g:out:" + vpa, 0, MAX_NEIGHBOURS - 1);
            if (out != null) {
                for (String payee : out) {
                    edges.add(edge(vpa, payee));
                    if (!nodes.containsKey(payee) && nodes.size() < MAX_NODES) {
                        nodes.put(payee, node(payee, level + 1, 0, 0));
                        queue.add(new String[]{payee, String.valueOf(level + 1)});
                    }
                }
            }
            Set<String> in = redis.opsForZSet().reverseRange("g:in:" + vpa, 0, MAX_NEIGHBOURS - 1);
            if (in != null) {
                for (String payer : in) {
                    if (edges.stream().noneMatch(e -> e.from().equals(payer) && e.to().equals(vpa))) {
                        edges.add(edge(payer, vpa));
                    }
                    if (!nodes.containsKey(payer) && nodes.size() < MAX_NODES) {
                        nodes.put(payer, node(payer, level + 1, 0, 0));
                        queue.add(new String[]{payer, String.valueOf(level + 1)});
                    }
                }
            }
        }
        List<Edge> visible = edges.stream()
                .filter(e -> nodes.containsKey(e.from()) && nodes.containsKey(e.to()))
                .distinct()
                .toList();
        return finish(root, nodes, visible);
    }

    private void collect(String root, int depth, Map<String, Node> nodes, List<Edge> edges) {
        Set<String> out = redis.opsForZSet().reverseRange("g:out:" + root, 0, MAX_NEIGHBOURS - 1);
        if (out == null) return;
        nodes.putIfAbsent(root, node(root, 0, 0, 0));
        for (String payee : out) {
            nodes.putIfAbsent(payee, node(payee, depth, 0, 0));
            edges.add(edge(root, payee));
        }
    }

    private Graph finish(String root, Map<String, Node> rawNodes, List<Edge> rawEdges) {
        List<Edge> edges = rawEdges.stream().filter(e -> rawNodes.containsKey(e.from()) && rawNodes.containsKey(e.to())).distinct().toList();
        Map<String, Integer> degree = new LinkedHashMap<>(); Map<String, Long> volume = new LinkedHashMap<>();
        for (Edge e : edges) {
            degree.merge(e.from(), 1, Integer::sum); degree.merge(e.to(), 1, Integer::sum);
            volume.merge(e.from(), e.amount(), Long::sum); volume.merge(e.to(), e.amount(), Long::sum);
        }
        List<Node> nodes = rawNodes.entrySet().stream().map(e -> {
            int d = degree.getOrDefault(e.getKey(), 0); long v = volume.getOrDefault(e.getKey(), 0L);
            return node(e.getKey(), e.getValue().level(), d, v);
        }).toList();
        List<String> rings = rings(nodes, edges);
        return new Graph(root, nodes, edges, rings, provenance());
    }

    private Node node(String vpa, int level, int degree, long volume) {
        Double mule = redis.opsForZSet().score(FeatureStore.MULE_WATCH, vpa);
        Boolean denied = redis.opsForSet().isMember(FeatureStore.DENY_LIST, vpa);
        double score = mule == null ? 0 : mule;
        String role = Boolean.TRUE.equals(denied) || score >= 0.7 ? "MULE" : degree >= 6 ? "HUB" : degree >= 3 ? "BRIDGE" : "NORMAL";
        List<String> reasons = score >= 0.3 ? List.of("mule score from fan-in, pass-through and flagged inbound signals") : List.of();
        return new Node(vpa, "ACCOUNT", level, score, Boolean.TRUE.equals(denied), role, degree, volume, reasons);
    }

    private Edge edge(String from, String to) {
        Object amt = redis.opsForHash().get("g:amt:" + from, to);
        Object cnt = redis.opsForHash().get("g:cnt:" + from, to);
        Object risk = redis.opsForHash().get("g:risk:" + from, to);
        return new Edge(from, to,
                amt == null ? 0 : Math.round(Double.parseDouble(amt.toString())),
                cnt == null ? 0 : Long.parseLong(cnt.toString()),
                risk == null ? 0 : Double.parseDouble(risk.toString()), "PAYMENT");
    }

    private List<String> rings(List<Node> nodes, List<Edge> edges) {
        if (edges.isEmpty()) return List.of();
        Set<String> sources = new HashSet<>(); Set<String> targets = new HashSet<>();
        edges.forEach(e -> { sources.add(e.from()); targets.add(e.to()); });
        List<String> out = new ArrayList<>();
        if (nodes.stream().anyMatch(n -> n.degree() >= 5)) out.add("STAR (demo heuristic: high-degree account)");
        if (sources.equals(targets) && nodes.size() >= 3) out.add("CYCLE (demo heuristic: all accounts both send and receive)");
        if (edges.size() > nodes.size() * 2) out.add("DENSE (demo heuristic: more than two links per account)");
        return out;
    }

    private Map<String, Object> provenance() { return Map.of("source", "FraudShield Kafka payment events", "synthetic", true, "groundTruthUsed", false); }

    public record Node(String id, String type, int level, double muleScore, boolean denied, String role,
                       int degree, long volume, List<String> reasons) {
    }

    public record Edge(String from, String to, long amount, long count, double riskScore, String relationshipType) {
    }

    public record Graph(String root, List<Node> nodes, List<Edge> edges, List<String> rings,
                        Map<String, Object> provenance) {
    }
}
