package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {

    private final Map<Integer, List<Integer>> adjacencyList;
    private long edgeCount;

    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.edgeCount = 0;
    }

    public void addEdge(int from, int to) {
        if (from < 0 || to < 0) { // since we have ids starting from ind 0
            return;
        }

        // add edges a <-> b, count as one
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        adjacencyList.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
        edgeCount++;
    }

    public List<Integer> getConnected(int nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    public int nodeCount() {
        return adjacencyList.size();
    }
    public long edgeCount() {
        return edgeCount;
    }
}
