package app;

import graph.dagsp.DAGShortestPath;
import graph.scc.TarjanSCC;
import graph.topo.KahnTopoSort;
import util.Edge;
import util.Graph;
import util.GraphLoader;
import util.Metrics;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Main {

    // Console debug (does not affect CSV / details files)
    private static final boolean PRINT_CONSOLE = false;

    public static void main(String[] args) throws Exception {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            System.err.println("Data folder not found");
            return;
        }

        String[] files = dataDir.list((d, n) -> n.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.err.println("No JSON datasets");
            return;
        }
        Arrays.sort(files);

        // Prepare outputs
        File resultsDir = new File("results");
        resultsDir.mkdirs();
        File detailsDir = new File(resultsDir, "details");
        detailsDir.mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(resultsDir, "results.csv")))) {
            writeCsvHeader(pw);
            for (String f : files) {
                processDatasetToCsvAndDetails(dataDir, f, pw, detailsDir);
            }
        }

        System.out.println("Saved table to results/results.csv");
        System.out.println("Per-dataset details in results/details/*.txt");
    }

    private static void processDatasetToCsvAndDetails(File dataDir,
                                                      String fileName,
                                                      PrintWriter pw,
                                                      File detailsDir) throws Exception {
        if (PRINT_CONSOLE) System.out.println("\n--- Dataset: " + fileName + " ---");

        GraphLoader.Dataset ds = GraphLoader.loadDataset(new File(dataDir, fileName).getPath());
        Graph g = ds.graph;

        // ---------- SCC (Tarjan) ----------
        TarjanSCC scc = new TarjanSCC();
        Metrics mScc = new Metrics();
        TarjanSCC.Result r = scc.findSCCs(g, mScc);

        // ---------- Condensation DAG + Topological order (Kahn) ----------
        Graph dag = scc.buildCondensation(g, r);
        int Vdag = dag.n;
        int Edag = 0;
        for (int u = 0; u < dag.n; u++) Edag += dag.adj.get(u).size();

        KahnTopoSort topo = new KahnTopoSort();
        Metrics mTopo = new Metrics();
        KahnTopoSort.Result tr = topo.topoOrder(dag, mTopo);

        // ---------- DAG shortest paths (single source) ----------
        DAGShortestPath dsp = new DAGShortestPath();
        double spMs = 0.0;
        long spRelax = 0L;
        Integer sourceVertex = ds.source;          // исходная вершина (может быть null)
        Integer sourceComp = null;                 // компонент-источник (если есть source)
        Integer spExampleTargetComp = null;
        String spExamplePath = "NA";
        double[] spDist = null;                    // для деталей: вектор дистанций

        if (ds.source != null) {
            sourceComp = r.compOf[ds.source];

            Metrics mSp = new Metrics();
            DAGShortestPath.DistResult sp =
                    dsp.shortestPaths(dag, sourceComp, tr.order, mSp);
            spMs = mSp.timeMs();
            spRelax = mSp.get("dagsp_relaxations");
            spDist = sp.dist;

            // choose a demo shortest path to a reachable sink (or any reachable)
            boolean[] isSink = new boolean[dag.n];
            Arrays.fill(isSink, true);
            for (int u = 0; u < dag.n; u++) {
                for (Edge e : dag.adj.get(u)) isSink[u] = false;
            }
            double best = Double.POSITIVE_INFINITY;
            int bestT = -1;
            for (int v = 0; v < dag.n; v++) {
                if (isSink[v] && Double.isFinite(sp.dist[v]) && sp.dist[v] < best) {
                    best = sp.dist[v];
                    bestT = v;
                }
            }
            if (bestT < 0) {
                for (int v = 0; v < dag.n; v++) {
                    if (Double.isFinite(sp.dist[v]) && v != sourceComp) { bestT = v; break; }
                }
            }
            if (bestT >= 0) {
                spExampleTargetComp = bestT;
                List<Integer> path = DAGShortestPath.reconstructPath(sourceComp, bestT, sp.parent);
                spExamplePath = joinPath(path);
            }
        } else if (PRINT_CONSOLE) {
            System.out.println("No source in dataset; skipping shortest paths.");
        }

        // ---------- DAG longest (critical) path ----------
        Metrics mLp = new Metrics();
        DAGShortestPath.DistResult lp =
                dsp.longestPathGlobal(dag, tr.order, mLp);
        double lpMs = mLp.timeMs();
        long lpRelax = mLp.get("daglp_relaxations");
        List<Integer> critPath = DAGShortestPath.reconstructPathToMax(lp.dist, lp.parent);
        double critLen = Double.NEGATIVE_INFINITY;
        for (double v : lp.dist) if (v > critLen) critLen = v;

        if (PRINT_CONSOLE) {
            System.out.println("Critical length=" + fmt(critLen) + " (ms=" + fmt(lpMs) + ", relax=" + lpRelax + ")");
        }

        // ---------- CSV row (only the required summary fields) ----------
        writeCsvRow(
                pw,
                fileName,
                ds.weightModel,
                g.n, g.edges.size(),
                r.compCount,
                mScc.timeMs(), mScc.get("scc_dfs_calls"), mScc.get("scc_dfs_edges"),
                Vdag, Edag,
                mTopo.timeMs(), mTopo.get("kahn_pops"), mTopo.get("kahn_pushes"),
                spMs, spRelax,
                lpMs, lpRelax, critLen,
                sourceVertex == null ? "NA" : String.valueOf(sourceVertex),
                sourceComp == null ? "NA" : String.valueOf(sourceComp),
                spExampleTargetComp == null ? "NA" : String.valueOf(spExampleTargetComp),
                spExamplePath,
                critPath.isEmpty() ? "NA" : joinPath(critPath)
        );

        // ---------- Details file (for the report: SCC list, DAG edges, orders, distances) ----------
        writeDetails(detailsDir, fileName, ds.weightModel, sourceVertex, sourceComp,
                r, dag, tr.order, deriveOrder(r, tr.order), spDist);
    }

    private static List<Integer> deriveOrder(TarjanSCC.Result r, List<Integer> compTopo) {
        List<Integer> derived = new ArrayList<>();
        for (int c : compTopo) derived.addAll(r.components.get(c));
        return derived;
    }

    private static void writeDetails(File detailsDir,
                                     String dataset,
                                     String weightModel,
                                     Integer sourceVertex,
                                     Integer sourceComp,
                                     TarjanSCC.Result r,
                                     Graph dag,
                                     List<Integer> compTopo,
                                     List<Integer> derived,
                                     double[] spDist) throws Exception {
        String base = dataset.replaceFirst("\\.json$", "");
        File out = new File(detailsDir, base + "_details.txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(out))) {
            w.println("dataset: " + dataset);
            w.println("weight_model: " + (weightModel == null ? "edge" : weightModel));
            w.println("source_vertex: " + (sourceVertex == null ? "NA" : sourceVertex));
            w.println("source_comp: " + (sourceComp == null ? "NA" : sourceComp));
            w.println();

            // SCC components
            w.println("SCC components (index: size | nodes):");
            for (int i = 0; i < r.components.size(); i++) {
                List<Integer> comp = r.components.get(i);
                w.println("  " + i + ": " + comp.size() + " | " + comp);
            }
            w.println();

            // Condensation DAG edges
            int Vdag = dag.n; int Edag = 0; for (int u = 0; u < dag.n; u++) Edag += dag.adj.get(u).size();
            w.println("Condensation DAG: V_dag=" + Vdag + ", E_dag=" + Edag);
            w.println("dag_edges (u -> v, w):");
            for (int u = 0; u < dag.n; u++) {
                for (Edge e : dag.adj.get(u)) {
                    w.println("  " + u + " -> " + e.to + " , w=" + fmt(e.weight));
                }
            }
            w.println();

            // Orders
            w.println("topo_order_components:");
            w.println("  " + compTopo);
            w.println("derived_order_vertices:");
            w.println("  " + derived);
            w.println();

            // Distances vector
            if (spDist != null) {
                w.println("dist_from_source (by component index):");
                StringBuilder sb = new StringBuilder("  [");
                for (int i = 0; i < spDist.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(Double.isFinite(spDist[i]) ? fmt(spDist[i]) : "INF");
                }
                sb.append("]");
                w.println(sb.toString());
            } else {
                w.println("dist_from_source: NA (no source in dataset)");
            }
        }
    }

    private static void writeCsvHeader(PrintWriter pw) {
        pw.println(String.join(",",
                "dataset",
                "weight_model",
                "n",
                "m",
                "SCC_count",
                "SCC_ms",
                "scc_dfs_calls",
                "scc_dfs_edges",
                "V_dag",
                "E_dag",
                "Topo_ms",
                "kahn_pops",
                "kahn_pushes",
                "SP_ms",
                "SP_relax",
                "LP_ms",
                "LP_relax",
                "CriticalLen",
                "source_vertex",
                "source_comp",
                "sp_example_target_comp",
                "sp_example_path_components",
                "critical_path_components"));
        pw.flush();
    }

    private static void writeCsvRow(PrintWriter pw,
                                    String dataset,
                                    String weightModel,
                                    int n, int m,
                                    int sccCount,
                                    double sccMs, long dfsCalls, long dfsEdges,
                                    int vDag, int eDag,
                                    double topoMs, long pops, long pushes,
                                    double spMs, long spRelax,
                                    double lpMs, long lpRelax, double criticalLen,
                                    String sourceVertex,
                                    String sourceComp,
                                    String spExampleTarget,
                                    String spExamplePath,
                                    String criticalPath) {
        String row = String.join(",",
                csv(dataset),
                csv(weightModel == null ? "edge" : weightModel),
                String.valueOf(n),
                String.valueOf(m),
                String.valueOf(sccCount),
                fmt(sccMs),
                String.valueOf(dfsCalls),
                String.valueOf(dfsEdges),
                String.valueOf(vDag),
                String.valueOf(eDag),
                fmt(topoMs),
                String.valueOf(pops),
                String.valueOf(pushes),
                fmt(spMs),
                String.valueOf(spRelax),
                fmt(lpMs),
                String.valueOf(lpRelax),
                fmt(criticalLen),
                csv(sourceVertex),
                csv(sourceComp),
                csv(spExampleTarget),
                csv(spExamplePath),
                csv(criticalPath)
        );
        pw.println(row);
        pw.flush();
    }

    private static String joinPath(List<Integer> path) {
        if (path == null || path.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append("->");
            sb.append(path.get(i));
        }
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String t = s.replace("\"", "\"\"");
        return needsQuote ? ("\"" + t + "\"") : t;
    }

    private static String fmt(double x) {
        return Double.isFinite(x) ? String.format(Locale.US, "%.3f", x) : "INF";
    }
}

