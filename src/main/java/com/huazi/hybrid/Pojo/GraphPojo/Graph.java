package com.huazi.hybrid.Pojo.GraphPojo;

import lombok.Getter;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存图数据结构，存储节点和边，并提供查询能力
 */
@Component
public class Graph {

    // 节点存储：ID -> Node
    @Getter
    private final Map<Long, Node> nodes = new ConcurrentHashMap<>();

    // 邻接表：起始节点 ID -> 从该节点出发的所有边
    private final Map<Long, List<Edge>> adjacencyList = new ConcurrentHashMap<>();

    // 所有出现在边中的节点ID（即道路节点）
    private final Set<Long> roadNodeIds = ConcurrentHashMap.newKeySet();

    /**
     * 添加节点
     */
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    /**
     * 添加边，同时记录边的两个端点为道路节点
     */
    public void addEdge(Edge edge) {
        adjacencyList.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
        roadNodeIds.add(edge.getFromNodeId());
        roadNodeIds.add(edge.getToNodeId());
    }

    /**
     * 获取从某个节点出发的所有边
     */
    public List<Edge> getOutgoingEdges(long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 获取节点坐标
     */
    public Node getNode(long nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取所有道路节点ID（至少出现在一条边中的节点）
     */
    public Set<Long> getRoadNodeIds() {
        return roadNodeIds;
    }

    /**
     * 获取图中边的总数（所有邻接表边数之和）
     */
    public long getOutgoingEdgesCount() {
        return adjacencyList.values().stream().mapToLong(List::size).sum();
    }

    /**
     * 清空图（重新加载数据前调用）
     */
    public void clear() {
        nodes.clear();
        adjacencyList.clear();
        roadNodeIds.clear();
    }

    /**
     * 初始化后可以进行一些校验
     */
    @PostConstruct
    public void init() {
        System.out.println("Graph bean initialized, ready to load data.");
    }
}