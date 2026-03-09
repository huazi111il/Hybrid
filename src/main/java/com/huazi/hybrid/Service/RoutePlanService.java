package com.huazi.hybrid.Service;

import com.huazi.hybrid.Pojo.GraphPojo.Edge;
import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import com.huazi.hybrid.Pojo.GraphPojo.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoutePlanService {

    @Autowired
    private Graph graph;

    // 最大速度用于启发式函数 (22.2 m/s ≈ 80 km/h)
    private static final double MAX_SPEED = 22.2;

    /**
     * 路径规划结果
     */
    public static class PathResult {
        public List<Long> nodeIds;      // 经过的节点ID（包括起点终点）
        public double totalTime;         // 总时间（秒）
        public PathResult(List<Long> nodeIds, double totalTime) {
            this.nodeIds = nodeIds;
            this.totalTime = totalTime;
        }
    }

    /**
     * A* 算法
     * @param startNodeId 起点节点ID
     * @param endNodeId   终点节点ID
     * @return 路径结果，若无路径返回null
     */
    public PathResult aStar(long startNodeId, long endNodeId) {
        System.out.println("=== A* start ===");
        System.out.println("startNodeId: " + startNodeId + ", endNodeId: " + endNodeId);

        // 检查起点/终点节点是否存在
        Node startNode = graph.getNode(startNodeId);
        Node endNode = graph.getNode(endNodeId);
        if (startNode == null || endNode == null) {
            System.out.println("起点或终点节点不存在");
            return null;
        }
        System.out.println("起点坐标: " + startNode.getLon() + "," + startNode.getLat());
        System.out.println("终点坐标: " + endNode.getLon() + "," + endNode.getLat());

        // 优先队列等初始化
        PriorityQueue<NodeRecord> open = new PriorityQueue<>(Comparator.comparingDouble(a -> a.f));
        Map<Long, NodeRecord> allNodes = new HashMap<>();

        double h = heuristic(startNode, endNode);
        NodeRecord startRecord = new NodeRecord(startNodeId, 0, h, null);
        open.add(startRecord);
        allNodes.put(startNodeId, startRecord);

        int explored = 0;
        while (!open.isEmpty()) {
            NodeRecord current = open.poll();
            explored++;
            if (explored % 1000 == 0) {
                System.out.println("已探索节点数: " + explored + ", 当前节点: " + current.nodeId);
            }
            if (current.nodeId == endNodeId) {
                System.out.println("找到目标！探索节点数: " + explored);
                return buildPathResult(allNodes, current);
            }

            List<Edge> edges = graph.getOutgoingEdges(current.nodeId);
            if (edges.isEmpty()) {
                // System.out.println("节点 " + current.nodeId + " 无出边");
            }
            for (Edge edge : edges) {
                long neighborId = edge.getToNodeId();
                double tentativeG = current.g + edge.getTime();

                NodeRecord neighborRecord = allNodes.get(neighborId);
                if (neighborRecord == null) {
                    Node neighborNode = graph.getNode(neighborId);
                    if (neighborNode == null) continue;
                    double neighborH = heuristic(neighborNode, endNode);
                    neighborRecord = new NodeRecord(neighborId, tentativeG, tentativeG + neighborH, current.nodeId);
                    open.add(neighborRecord);
                    allNodes.put(neighborId, neighborRecord);
                } else if (tentativeG < neighborRecord.g) {
                    neighborRecord.g = tentativeG;
                    neighborRecord.f = tentativeG + neighborRecord.h;
                    neighborRecord.parent = current.nodeId;
                    open.add(neighborRecord);
                }
            }
        }
        System.out.println("搜索结束，未找到路径。探索节点数: " + explored);
        return null;
    }

    /**
     * 启发式函数：直线距离 / 最大速度
     */
    private double heuristic(Node from, Node to) {
        return haversine(from.getLon(), from.getLat(), to.getLon(), to.getLat()) / MAX_SPEED;
    }

    private double haversine(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 从记录重建路径
     */
    private PathResult buildPathResult(Map<Long, NodeRecord> records, NodeRecord goal) {
        LinkedList<Long> path = new LinkedList<>();
        NodeRecord current = goal;
        while (current != null) {
            path.addFirst(current.nodeId);
            if (current.parent == null) break;
            current = records.get(current.parent);
        }
        return new PathResult(path, goal.g);
    }

    /**
     * A* 内部记录类
     */
    private static class NodeRecord {
        long nodeId;
        double g;      // 从起点到当前节点的实际代价
        double h;      // 启发值
        double f;      // 估计总代价
        Long parent;   // 父节点ID

        NodeRecord(long nodeId, double g, double f, Long parent) {
            this.nodeId = nodeId;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
}