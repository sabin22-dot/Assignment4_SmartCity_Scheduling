package util;
import java.util.ArrayList;
import java.util.List;
/**
 * Lightweight directed graph with adjacency lists.
 * Used across SCC, Topo, and DAG shortest/longest algorithms.
 */
public class Graph {
    /** Number of vertices. */
    public final int n;
    /** True if the graph is directed. */
    public final boolean directed;
    /** Flat list of all edges (u -> v, w). */
    public final List<Edge> edges = new ArrayList<>();
    /** Adjacency lists: adj[u] contains outgoing edges from u. */
    public final List<List<Edge>> adj;
    public Graph(int n, boolean directed) {
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
    }
    /** Adds a directed edge u -> v with weight w. */
    public void addEdge(int u, int v, double w) {
        Edge e = new Edge(u, v, w);
        edges.add(e);
        adj.get(u).add(e);
    }
    /** Returns indegree of vertex v (counts incoming edges). */
    public int indegree(int v) {
        int d = 0;
        for (int u = 0; u < n; u++) {
            for (Edge e : adj.get(u)) {
                if (e.to == v) d++;
            }
        }
        return d;
    }
}
