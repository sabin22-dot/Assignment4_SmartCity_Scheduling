package util;
/**
 * Common metrics interface for instrumentation.
 * Tracks operation counters and wall-clock time using System.nanoTime().
 */
import java.util.*;
public class Metrics {
    private long startNs, elapsedNs; private final Map<String,Long> c=new HashMap<>();
    public void start(){ startNs=System.nanoTime(); }
    public void stop(){ elapsedNs=System.nanoTime()-startNs; }
    public double timeMs(){ return elapsedNs/1_000_000.0; }
    public void inc(String k){ c.put(k, c.getOrDefault(k,0L)+1); }
    public long get(String k){ return c.getOrDefault(k,0L); }
}
