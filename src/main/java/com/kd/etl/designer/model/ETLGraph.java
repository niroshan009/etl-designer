package com.kd.etl.designer.model;

import java.util.*;

public class ETLGraph<T> {
    private final Map<T, List<T>> adjacencyList;

    public ETLGraph(){
        this.adjacencyList = new HashMap<>();
    }

    public void addVertex(T vertex){
        this.adjacencyList.putIfAbsent(vertex, new ArrayList<>());
    }

    public void addEdge(T source, T destination){
        addVertex(source);
        addVertex(destination);

        adjacencyList.get(source).add(destination);
    }

    public List<T> getNeighbors(T vertex){
        return adjacencyList.getOrDefault(vertex, Collections.emptyList());
    }

    public void printGraph() {
        for (Map.Entry<T, List<T>> entry : adjacencyList.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    public T getVertex(T vertex){
        return this.adjacencyList.keySet().stream().filter(e -> e.equals(vertex)).findFirst().orElse(null);
    }

    public Map<T, List<T>> getAdjacencyList(){
        return this.adjacencyList;
    }


    public String getGraphString(){
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<T, List<T>> entry : adjacencyList.entrySet()) {
            stringBuilder.append(entry.getKey()+" ->"+ entry.getValue());
        }
        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }
}
