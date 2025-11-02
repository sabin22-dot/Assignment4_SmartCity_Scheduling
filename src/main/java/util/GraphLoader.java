package util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
/**
 * Loads graph datasets from JSON files.
 *
 * Expected format:
 * {
 *   "directed": true,
 *   "n": N,
 *   "edges": [{"u":..,"v":..,"w":..}, ...],
 *   "source": S,               // optional
 *   "weight_model": "edge"     // optional; defaults to "edge"
 * }
 */
public final class GraphLoader {
    /** Immutable dataset bundle. */
    public static final class Dataset {
        public final Graph graph;
        public final Integer source;      // may be null
        public final String weightModel;  // e.g., "edge"
        public final boolean directed;
        public Dataset(Graph graph,
                       Integer source,
                       String weightModel,
                       boolean directed) {
            this.graph = graph;
            this.source = source;
            this.weightModel = weightModel;
            this.directed = directed;
        }
    }
    private GraphLoader() {}
    /** Reads a dataset JSON file and constructs a Graph + metadata. */
    public static Dataset loadDataset(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(path));

        boolean directed = root.has("directed")
                ? root.get("directed").asBoolean()
                : true;
        int n = root.get("n").asInt();
        Graph g = new Graph(n, directed);

        JsonNode edges = root.get("edges");
        if (edges != null && edges.isArray()) {
            for (JsonNode e : edges) {
                int u = e.get("u").asInt();
                int v = e.get("v").asInt();
                double w = e.get("w").asDouble();
                g.addEdge(u, v, w);
            }
        }
        Integer source = root.has("source") ? root.get("source").asInt() : null;
        String wm = root.has("weight_model")
                ? root.get("weight_model").asText()
                : "edge";

        return new Dataset(g, source, wm, directed);
    }
}
