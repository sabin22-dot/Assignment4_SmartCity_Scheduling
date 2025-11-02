package graph.topo;
import util.Edge;
import util.Graph;
import util.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
/**
 * Kahn's algorithm for topological ordering on a DAG.
 * Instrumented with queue pushes/pops counters.
 */
public class KahnTopoSort {
    /** Result of topological sorting. */
    public static class Result {
        public final List<Integer> order;
        public final boolean isDAG;
        public Result(List<Integer> order, boolean isDAG) {
            this.order = order;
            this.isDAG = isDAG;
        }
    }
    /**
     * Computes a topological order using Kahn's algorithm.
     * Counts "kahn_pushes" and "kahn_pops" via Metrics.
     */
    public Result topoOrder(Graph dag, Metrics m) {
        final int n = dag.n;
        // Compute indegrees
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (Edge e : dag.adj.get(u)) {
                indeg[e.to]++;
            }
        }
        // Initialize queue with zero indegree vertices
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) {
                q.add(i);
                m.inc("kahn_pushes");
            }
        }
        List<Integer> ord = new ArrayList<>();
        m.start();
        // Pop vertex, decrease indegrees of its out-neighbors
        while (!q.isEmpty()) {
            int u = q.remove();
            m.inc("kahn_pops");
            ord.add(u);
            for (Edge e : dag.adj.get(u)) {
                if (--indeg[e.to] == 0) {
                    q.add(e.to);
                    m.inc("kahn_pushes");
                }
            }
        }
        m.stop();
        return new Result(ord, ord.size() == n);
    }
}

