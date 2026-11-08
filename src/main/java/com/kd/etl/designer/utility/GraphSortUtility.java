package com.kd.etl.designer.utility;

import java.util.*;


/**
 * Kahn algorithm to sort the graph in topological order
 */

public class GraphSortUtility {

    public static <T> List<T> topologicalSort(Map<T, List<T>> adjList) {
        Map<T, Integer> inDegree = new HashMap<>();

        for (T node : adjList.keySet()) {
            inDegree.putIfAbsent(node, 0);
            for (T neighbor : adjList.getOrDefault(node, Collections.emptyList())) {
                inDegree.putIfAbsent(neighbor, 0);
            }
        }

        for (T u : adjList.keySet()) {
            for (T v : adjList.getOrDefault(u, Collections.emptyList())) {
                inDegree.put(v, inDegree.get(v) + 1);
            }
        }

        Queue<T> queue = new LinkedList<>();
        for (Map.Entry<T, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<T> topoOrder = new ArrayList<>();

        while (!queue.isEmpty()) {
            T u = queue.poll();
            topoOrder.add(u);

            for (T neighbor : adjList.getOrDefault(u, Collections.emptyList())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);

                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (topoOrder.size() == inDegree.size()) {
            return topoOrder;
        } else {
            return new ArrayList<>();
        }
    }
}


