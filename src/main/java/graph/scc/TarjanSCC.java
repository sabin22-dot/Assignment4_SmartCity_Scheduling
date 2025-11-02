package graph.scc;
import util.Edge;
import util.Graph;
import util.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * Tarjan's algorithm for Strongly Connected Components (SCC).
 * Provides vertex->component map and a method to build the condensation DAG.
 */
public class TarjanSCC {
    /** Result bundle for SCC computation. */
    public static class Result {
        public final List<List<Integer>> components;
        public final int[] compOf;
        public final int compCount;
        public Result(List<List<Integer>> components, int[] compOf, int compCount) {
            this.components = components;
            this.compOf = compOf;
            this.compCount = compCount;
        }
    }

    private int timer;
    private int[] disc, low, compOf;
    private boolean[] on;
    private Deque<Integer> stack;
    private List<List<Integer>> comps;
    private Metrics metrics;
    /**
     * Finds SCCs in a directed graph. Instrumented with DFS visit/edge counters.
     */
    public Result findSCCs(Graph g, Metrics m) {
        this.metrics = m;
        int n = g.n;
        timer = 0;
        disc = new int[n];
        low = new int[n];
        Arrays.fill(disc, -1);
        Arrays.fill(low, -1);
        compOf = new int[n];
        Arrays.fill(compOf, -1);

        on = new boolean[n];
        stack = new ArrayDeque<>();
        comps = new ArrayList<>();

        m.start();
        for (int v = 0; v < n; v++) {
            if (disc[v] == -1) {
                dfs(g, v);
            }
        }
        m.stop();
        return new Result(comps, compOf, comps.size());
    }
    // DFS with discovery/low-link values and on-stack marking.
    private void dfs(Graph g, int v) {
        disc[v] = low[v] = timer++;
        stack.push(v);
        on[v] = true;
        metrics.inc("scc_dfs_calls");
        for (Edge e : g.adj.get(v)) {
            metrics.inc("scc_dfs_edges");
            int to = e.to;

            if (disc[to] == -1) {
                dfs(g, to);
                low[v] = Math.min(low[v], low[to]);
            } else if (on[to]) {
                low[v] = Math.min(low[v], disc[to]);
            }
        }
        // Root of an SCC reached -> pop stack until v.
        if (low[v] == disc[v]) {
            List<Integer> comp = new ArrayList<>();
            while (true) {
                int u = stack.pop();
                on[u] = false;
                compOf[u] = comps.size();
                comp.add(u);
                if (u == v) break;
            }
            comps.add(comp);
        }
    }
    /**
     * Builds the condensation DAG (each SCC becomes a node; parallel edges deduplicated).
     */
    public Graph buildCondensation(Graph g, Result r) {
        Graph dag = new Graph(r.compCount, true);
        Set<Long> seen = new HashSet<>();
        for (Edge e : g.edges) {
            int a = r.compOf[e.from];
            int b = r.compOf[e.to];
            if (a == b) continue;

            long key = (((long) a) << 32) ^ b;
            if (seen.add(key)) {
                dag.addEdge(a, b, e.weight);
            }
        }
        return dag;
    }
}
