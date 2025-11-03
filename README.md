# Assignment 4 — Smart City / Smart Campus Scheduling

**Topics:** Strongly Connected Components (SCC) & Shortest Paths in DAGs  
**Student:** Sabina Zhumagaliyeva  
**Group:** SE-2422

---

## Project Goal

This project integrates two algorithmic topics in one pipeline for a realistic planning case **“Smart City / Smart Campus Scheduling”**:

- Detect **cycles** (Strongly Connected Components) in service-task dependencies and **compress** them into components.  
- Build the **condensation DAG**, compute a **topological order**, and run **shortest** / **longest (critical)** path DP to plan execution.

Typical tasks in the scenario: street cleaning, maintenance, sensor updates, repairs. Some dependencies are cyclic (must be grouped), others acyclic (can be scheduled optimally).

---

## Project Structure

src/  
├─ main/java/  
│ ├─ graph/  
│ │ ├─ scc/ → TarjanSCC, CondensationGraphBuilder  
│ │ ├─ topo/ → TopologicalSort  
│ │ ├─ dagsp/ → ShortestPathDAG, LongestPathDAG  
│ │ ├─ metrics/ → Metrics, AlgorithmMetricsRunner  
│ │ └─ model/ → Graph, Edge  
│ └─ util/ → RandomGraphGenerator, GraphLoader  
├─ test/java/graph/ → GraphAlgorithmsTest  
data_final/ → 9 datasets (small/medium/large)  
metrics/ → recorded performance metrics (.json)  



---
## Build & Run

```bash
mvn clean test
mvn -DskipTests exec:java
```

**Outputs**
- `results/results.csv` — compact per-dataset table (sizes, metrics, timings, critical length).
- `results/details/*.txt` — SCC lists, condensation DAG edges, topo & derived orders, full distance vectors, example paths.  
  *Note:* times can vary slightly per run (JVM warm‑up / OS noise). Operation counters are deterministic.

---

## Dataset Summary

Nine directed datasets (Small 6–10 nodes, Medium 10–20, Large 20–50) with mixed density and several SCCs.  
`V_dag = SCC_count`. As an estimate for `E_dag`, we use the number of relaxations in DAG DP (each DAG edge is relaxed once): `E_dag_est = max(SP_relax, LP_relax)`.

| dataset     | size   | n  | m  | SCC_count (=V_dag) | E_dag_est | Notes                     |
|-------------|--------|----|----|--------------------|-----------|---------------------------|
| large1.json | Large  | 22 | 18 | 18                 | 12        | mixed, several small SCCs |
| large2.json | Large  | 35 | 12 | 32                 | 7         | sparse, near-DAG          |
| large3.json | Large  | 48 | 13 | 44                 | 7         | sparse, near-DAG          |
| medium1.json| Medium | 12 | 12 | 9                  | 7         | mixed cyclic + DAG        |
| medium2.json| Medium | 15 | 14 | 13                 | 11        | several SCCs              |
| medium3.json| Medium | 18 | 14 | 15                 | 9         | mixed                     |
| small1.json | Small  | 8  | 7  | 6                  | 4         | simple chain + isolated   |
| small2.json | Small  | 7  | 7  | 7                  | 7         | pure DAG (singletons)     |
| small3.json | Small  |10  |11  | 6                  | 5         | few cycles                |

---

## Performance & Metrics (per task)

| dataset     | SCC_ms | Topo_ms | SP_ms | LP_ms | SP_relax | LP_relax | CriticalLen |
|-------------|-------:|--------:|------:|------:|---------:|---------:|------------:|
| large1.json | 0.047  | 0.017   | 0.017 | 0.009 | 12       | 12       | 24          |
| large2.json | 0.103  | 0.016   | 0.008 | 0.015 | 7        | 7        | 14          |
| large3.json | 0.041  | 0.020   | 0.005 | 0.009 | 7        | 7        | 14          |
| medium1.json| 0.019  | 0.004   | 0.002 | 0.002 | 6        | 7        | 8           |
| medium2.json| 0.014  | 0.004   | 0.002 | 0.002 | 5        | 11       | 8           |
| medium3.json| 0.012  | 0.005   | 0.002 | 0.002 | 2        | 9        | 7           |
| small1.json | 0.006  | 0.002   | 0.001 | 0.001 | 3        | 4        | 11          |
| small2.json | 0.006  | 0.003   | 0.002 | 0.001 | 7        | 7        | 8           |
| small3.json | 0.023  | 0.004   | 0.003 | 0.002 | 3        | 5        | 6           |

**Sanity checks (hold for all datasets):**
- `scc_dfs_calls == n` and `scc_dfs_edges == m` (linear Tarjan pass).  
- `kahn_pushes == kahn_pops == V_dag == SCC_count` (one queue cycle per condensed node).

---

## Analysis

### SCC Detection (Tarjan)
- **Complexity:** O(V + E).  
- Finishes ≤ 0.12 ms and outputs SCC lists + `vertex → compId`.  
- Condensation shrinks the problem (`n → V_dag`), speeding up later steps.

### Topological Ordering (Kahn)
- `push/pop` counts match `V_dag`.  
- Timings ≤ 0.020 ms; works after SCC on cyclic originals.

### Shortest Path in DAG
- DP over topological order (SSSP); each DAG edge relaxes once.

### Longest (Critical) Path
- max-DP on the same order; timings close to SSSP; `CriticalLen` = dominant chain.

---

## Conclusions

| Aspect                   | Finding                                                                 |
|-------------------------|-------------------------------------------------------------------------|
| SCC (Tarjan)            | Detect & compress cycles; linear and lightweight.                        |
| Topological Sort (Kahn) | Clean ordering + easy metrics; good validator post-SCC.                  |
| DAG Shortest Path       | Linear DP; minimizes time/cost from a source.                            |
| DAG Longest / Critical  | Identifies the bottleneck chain (makespan).                              |
| Overall Performance     | All steps < 1 ms; linear scaling validated.                              |

---

## Reproducibility

- **Build:** `mvn clean test`  
- **Run:** `mvn -DskipTests exec:java`  
- **Outputs:** `results/results.csv`, `results/details/*.txt`  
- **Tests:** `src/test/java`
