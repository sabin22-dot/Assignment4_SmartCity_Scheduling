package util;
public class Edge {
    public final int from, to; public final double weight;
    public Edge(int from, int to, double weight){ this.from=from; this.to=to; this.weight=weight; }
    public String toString(){ return from+"->"+to+"("+weight+")"; }
}
