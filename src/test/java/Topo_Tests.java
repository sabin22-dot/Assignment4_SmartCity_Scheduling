import graph.topo.KahnTopoSort;
import util.Graph;
import util.Metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
/** JUnit tests for Kahn topological sort. */
public class Topo_Tests {
    @Test
    void dagTopoOrder() {
        // DAG: 0 -> {1,2} -> 3 -> 4
        Graph dag = new Graph(5, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 4, 1);
        KahnTopoSort kt = new KahnTopoSort();
        KahnTopoSort.Result r = kt.topoOrder(dag, new Metrics());
        assertTrue(r.isDAG);
        assertEquals(5, r.order.size());
        assertEquals(0, r.order.get(0));  // source first
        assertEquals(4, r.order.get(4));  // sink last
    }
    @Test
    void cycleDetected() {
        // 0->1->2->0 cycle: not a DAG; order size < 3
        Graph g = new Graph(3, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        KahnTopoSort kt = new KahnTopoSort();
        KahnTopoSort.Result r = kt.topoOrder(g, new Metrics());
        assertFalse(r.isDAG);
        assertTrue(r.order.size() < 3);
    }
}

