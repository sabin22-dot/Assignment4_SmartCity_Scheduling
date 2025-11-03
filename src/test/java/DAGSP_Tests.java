import graph.dagsp.DAGShortestPath;
import graph.topo.KahnTopoSort;
import util.Graph;
import util.Metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/** JUnit tests for DAG shortest-path routines. */
public class DAGSP_Tests {
    @Test
    void shortestPathSmallDAG() {
        Graph dag = new Graph(4, true);
        dag.addEdge(0, 1, 2);
        dag.addEdge(0, 2, 3);
        dag.addEdge(1, 3, 3);
        dag.addEdge(2, 3, 1);
        KahnTopoSort kt = new KahnTopoSort();
        KahnTopoSort.Result topoRes = kt.topoOrder(dag, new Metrics());
        DAGShortestPath sp = new DAGShortestPath();
        DAGShortestPath.DistResult res =
                sp.shortestPaths(dag, 0, topoRes.order, new Metrics());
        assertEquals(4.0, res.dist[3], 1e-9);
    }
    @Test
    void unreachableVertex() {
        Graph dag = new Graph(3, true);
        dag.addEdge(0, 1, 1);
        KahnTopoSort kt = new KahnTopoSort();
        KahnTopoSort.Result topoRes = kt.topoOrder(dag, new Metrics());
        DAGShortestPath sp = new DAGShortestPath();
        DAGShortestPath.DistResult res =
                sp.shortestPaths(dag, 0, topoRes.order, new Metrics());
        assertTrue(Double.isInfinite(res.dist[2]) || res.parent[2] == -1);
    }
}

