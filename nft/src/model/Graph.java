package model;

import java.util.*;

public class Graph {

    private final Map<Integer, List<Integer>> adj;
    private long edgeCount;

    public Graph() {
        this.adj = new HashMap<>();
        this.edgeCount = 0;
    }

    public void addEdge(int from, int to) {
        if (from < 0 || to < 0) return;

        if (!adj.containsKey(from) ) {
            adj.put(from, new ArrayList<>());
        }

        adj.get(from).add(to);
        edgeCount++;
    }

    public List<Integer> getConnected(int nodeId) {
        return adj.getOrDefault(nodeId, Collections.emptyList());
    }

    public long edgeCount() {
        return edgeCount;
    }

    public void removeDuplicateEdges() {
        long newEdgeCount = 0;

        for (List<Integer> neighbours : adj.values()) {
            Set<Integer> unique = new HashSet<>(neighbours);

            neighbours.clear();
            neighbours.addAll(unique);

            newEdgeCount += neighbours.size();
        }
        edgeCount = newEdgeCount;
    }
}
