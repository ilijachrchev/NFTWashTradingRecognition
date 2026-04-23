package model;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class Graph {

    private final Map<Integer, IntVec> adj;
    private long edgeCount;

    public Graph() {
        this.adj = new HashMap<>();
        this.edgeCount = 0;
    }

    public void addEdge(int from, int to) {
        if (from < 0 || to < 0) return;

        adj.computeIfAbsent(from, k -> new IntVec()).add(to);
        edgeCount++;
    }

    public IntVec getConnected(int nodeId) {
        IntVec v = adj.get(nodeId);
        return v == null ? IntVec.EMPTY : v;
    }

    public int nodeCount() {
        return adj.size();
    }

    public long edgeCount() {
        return edgeCount;
    }

    public void duplicateEdges() {
        long newEdgeCount = 0;

        for (IntVec v : adj.values()) {
            if (v.size <= 1) {
                newEdgeCount += v.size;;
                continue;
            }

            Arrays.sort(v.a, 0, v.size);

            int write = 1;
            for (int read = 1; read < v.size; read++) {
                if (v.a[read] != v.a[read - 1]) {
                    v.a[write++] = v.a[read];
                }
            }
            v.size = write;
            newEdgeCount += v.size;
        }
        edgeCount = newEdgeCount;
    }

    public static final class IntVec {
        public static final IntVec EMPTY = new IntVec(0, true);

        int[] a;
        int size;
        private final boolean fixedEmpty;

        public IntVec() {
            this.a = new int[4];
            this.size = 0;
            this.fixedEmpty = false;
        }

        private IntVec(int cap, boolean fixedEmpty) {
            this.a = new int[cap];
            this.size = 0;
            this.fixedEmpty = fixedEmpty;
        }

        public void add(int x) {
            if (fixedEmpty) throw new IllegalStateException("Cannot add to EMPTY");
            if (size == a.length) {
                int[] b = new int[a.length * 2];
                System.arraycopy(a, 0, b, 0, a.length);
                a = b;
            }
            a[size++] = x;
        }

        public int size() {
            return size;
        }

        public int get(int i) {
            return a[i];
        }
    }
}
