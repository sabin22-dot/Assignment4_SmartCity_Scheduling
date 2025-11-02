import graph.scc.TarjanSCC;
import util.Graph;
import util.Metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
/** JUnit tests for SCC detection (Tarjan). */
public class SCC_Tests {
    @Test
    void simpleCycleAndChain() {
        // Graph: (0->1->2->0) is one SCC; 3->4->5 are singletons.
        Graph g = new Graph(6, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        g.addEdge(3, 4, 1);
        g.addEdge(4, 5, 1);
        TarjanSCC tarjan = new TarjanSCC();
        TarjanSCC.Result r = tarjan.findSCCs(g, new Metrics());

        assertEquals(4, r.compCount);          // [ {0,1,2}, {3}, {4}, {5} ]
        assertEquals(r.compOf[0], r.compOf[1]);
        assertEquals(r.compOf[1], r.compOf[2]);
    }
    @Test
    void singleNodeNoEdges() {
        Graph g = new Graph(1, true);

        TarjanSCC tarjan = new TarjanSCC();
        TarjanSCC.Result r = tarjan.findSCCs(g, new Metrics());

        assertEquals(1, r.compCount);
        assertEquals(1, r.components.get(0).size());
    }
}

