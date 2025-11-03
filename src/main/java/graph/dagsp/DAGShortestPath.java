package graph.dagsp;
/**
 * DP on topological order over a DAG:
 *  - Single-source shortest paths (DP along topo order)
 *  - Global longest (critical) path (max-DP along topo order)
 *
 * Metrics keys (Metrics):
 *  - "dagsp_relaxations" : number of relaxations in shortest paths
 *  - "daglp_relaxations" : number of relaxations in longest paths
 */
import util.*;
import java.util.*;
public class DAGShortestPath {
    /** Distances and parents for path reconstruction. */
    public static class DistResult {
        public final double[] dist;
        public final int[] parent;
        public DistResult(double[] d, int[] p) { this.dist = d; this.parent = p; }
    }
    /** Single-source shortest paths on a DAG using a provided topological order (edge weights). */
    public DistResult shortestPaths(Graph dag, int src, List<Integer> topo, Metrics m) {
        // Validate topological order
        if (topo == null || topo.size() != dag.n) {
            throw new IllegalArgumentException("Invalid topological order size: " +
                    (topo == null ? "null" : topo.size()) + " vs dag.n=" + dag.n);
        }
        final int n = dag.n;
        final double INF = Double.POSITIVE_INFINITY;
        double[] dist = new double[n];
        int[] parent  = new int[n];
        Arrays.fill(dist, INF);
        Arrays.fill(parent, -1);
        dist[src] = 0.0;

        m.start();
        for (int u : topo) {
            if (Double.isInfinite(dist[u])) continue; // unreachable so far
            for (Edge e : dag.adj.get(u)) {
                m.inc("dagsp_relaxations");
                double cand = dist[u] + e.weight;
                if (cand < dist[e.to]) {
                    dist[e.to] = cand;
                    parent[e.to] = u;
                }
            }
        }
        m.stop();
        return new DistResult(dist, parent);
    }
    /** Global longest (critical) path via max-DP along the topological order. */
    public DistResult longestPathGlobal(Graph dag, List<Integer> topo, Metrics m) {
        // Validate topological order
        if (topo == null || topo.size() != dag.n) {
            throw new IllegalArgumentException("Invalid topological order size: " +
                    (topo == null ? "null" : topo.size()) + " vs dag.n=" + dag.n);
        }
        final int n = dag.n;
        final double NEG = Double.NEGATIVE_INFINITY;

        double[] dist = new double[n];
        int[] parent  = new int[n];
        Arrays.fill(dist, NEG);
        Arrays.fill(parent, -1);
        // Initialize all sources (indegree == 0) with 0
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) for (Edge e : dag.adj.get(u)) indeg[e.to]++;
        for (int v = 0; v < n; v++) if (indeg[v] == 0) dist[v] = 0.0;

        m.start();
        for (int u : topo) {
            if (dist[u] == NEG) continue; // unreachable in current max-DP
            for (Edge e : dag.adj.get(u)) {
                m.inc("daglp_relaxations");
                double cand = dist[u] + e.weight;
                if (cand > dist[e.to]) {
                    dist[e.to] = cand;
                    parent[e.to] = u;
                }
            }
        }
        m.stop();
        return new DistResult(dist, parent);
    }
    /** Reconstruct path s->t using parent array; returns empty list if unreachable. */
    public static List<Integer> reconstructPath(int s, int t, int[] parent) {
        List<Integer> path = new ArrayList<>();
        for (int cur = t; cur != -1; cur = parent[cur]) {
            path.add(cur);
            if (cur == s) break;
        }
        Collections.reverse(path);
        return path.isEmpty() || path.get(0) != s ? Collections.emptyList() : path;
    }
    /** Reconstruct one of the globally maximum paths (by the largest dist[v]). */
    public static List<Integer> reconstructPathToMax(double[] dist, int[] parent) {
        int bestV = -1; double best = Double.NEGATIVE_INFINITY;
        for (int v = 0; v < dist.length; v++) if (dist[v] > best) { best = dist[v]; bestV = v; }
        if (bestV < 0) return Collections.emptyList();
        List<Integer> p = new ArrayList<>();
        for (int cur = bestV; cur != -1; cur = parent[cur]) p.add(cur);
        Collections.reverse(p);
        return p;
    }
}

