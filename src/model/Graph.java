package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {

    private final Map<Integer, List<Integer>> adjacencyList;
    private long edgeCount;

    public Graph() {
        this.adjacencyList = new HashMap<>(16_000_000);
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
}
