package com.huazi.hybrid.Pojo.GraphPojo;

import lombok.Getter;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Graph {
    @Getter
    private final Map<Long, Node> nodes = new ConcurrentHashMap<>();
    private final Map<Long, List<Edge>> adjacencyList = new ConcurrentHashMap<>();
    private final Set<Long> roadNodeIds = ConcurrentHashMap.newKeySet();

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(Edge edge) {
        adjacencyList.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
        roadNodeIds.add(edge.getFromNodeId());
        roadNodeIds.add(edge.getToNodeId());
    }

    public List<Edge> getOutgoingEdges(long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public Node getNode(long nodeId) {
        return nodes.get(nodeId);
    }

    public Set<Long> getRoadNodeIds() {
        return roadNodeIds;
    }

    public long getOutgoingEdgesCount() {
        return adjacencyList.values().stream().mapToLong(List::size).sum();
    }

    public void clear() {
        nodes.clear();
        adjacencyList.clear();
        roadNodeIds.clear();
    }

    @PostConstruct
    public void init() {
        System.out.println("Graph bean initialized, ready to load data.");
    }
}