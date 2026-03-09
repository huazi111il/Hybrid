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

    private static final double MAX_SPEED = 22.2;

    public static class PathSegment {
        public String mode;
        public List<Long> nodeIds;
        public double distance;
        public double time;
        public double avgCongestion;

        public PathSegment(String mode, List<Long> nodeIds, double distance, double time, double avgCongestion) {
            this.mode = mode;
            this.nodeIds = nodeIds;
            this.distance = distance;
            this.time = time;
            this.avgCongestion = avgCongestion;
        }
    }

    public static class PathResult {
        public List<Long> nodeIds;
        public double totalTime;
        public List<PathSegment> segments;

        public PathResult(List<Long> nodeIds, double totalTime, List<PathSegment> segments) {
            this.nodeIds = nodeIds;
            this.totalTime = totalTime;
            this.segments = segments;
        }
    }

    public PathResult aStar(long startNodeId, long endNodeId) {
        System.out.println("=== A* start ===");
        System.out.println("startNodeId: " + startNodeId + ", endNodeId: " + endNodeId);

        Node startNode = graph.getNode(startNodeId);
        Node endNode = graph.getNode(endNodeId);
        if (startNode == null || endNode == null) {
            System.out.println("起点或终点节点不存在");
            return null;
        }

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
            for (Edge edge : edges) {
                long neighborId = edge.getToNodeId();
                double tentativeG = current.g + edge.getRealTime();

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

    private PathResult buildPathResult(Map<Long, NodeRecord> records, NodeRecord goal) {
        LinkedList<Long> path = new LinkedList<>();
        NodeRecord current = goal;
        while (current != null) {
            path.addFirst(current.nodeId);
            if (current.parent == null) break;
            current = records.get(current.parent);
        }

        List<PathSegment> segments = new ArrayList<>();
        if (path.size() >= 2) {
            String currentMode = null;
            List<Long> currentNodes = new ArrayList<>();
            double currentDistance = 0;
            double currentTime = 0;
            double currentCongestionSum = 0;
            int currentEdgeCount = 0;

            currentNodes.add(path.get(0));

            for (int i = 0; i < path.size() - 1; i++) {
                long fromId = path.get(i);
                long toId = path.get(i + 1);
                Edge edge = findEdge(fromId, toId);
                if (edge == null) continue;

                String mode = mapMode(edge.getRoadType());

                if (currentMode == null) {
                    currentMode = mode;
                }

                if (!mode.equals(currentMode)) {
                    double avgCong = currentEdgeCount > 0 ? currentCongestionSum / currentEdgeCount : 1.0;
                    segments.add(new PathSegment(currentMode, new ArrayList<>(currentNodes),
                            currentDistance, currentTime, avgCong));
                    // 开始新段
                    currentNodes.clear();
                    currentNodes.add(fromId);
                    currentDistance = 0;
                    currentTime = 0;
                    currentCongestionSum = 0;
                    currentEdgeCount = 0;
                    currentMode = mode;
                }

                currentNodes.add(toId);
                currentDistance += edge.getDistance();
                currentTime += edge.getRealTime();
                currentCongestionSum += edge.getCongestionFactor();
                currentEdgeCount++;
            }

            // 最后一段
            if (currentMode != null) {
                double avgCong = currentEdgeCount > 0 ? currentCongestionSum / currentEdgeCount : 1.0;
                segments.add(new PathSegment(currentMode, new ArrayList<>(currentNodes),
                        currentDistance, currentTime, avgCong));
            }
        }

        return new PathResult(path, goal.g, segments);
    }

    private String mapMode(String roadType) {
        if (roadType == null) return "drive";
        if (roadType.equals("subway")) return "subway";
        if (roadType.equals("walk")) return "walk";
        return "drive";
    }

    private Edge findEdge(long fromId, long toId) {
        List<Edge> edges = graph.getOutgoingEdges(fromId);
        for (Edge e : edges) {
            if (e.getToNodeId() == toId) return e;
        }
        return null;
    }

    private static class NodeRecord {
        long nodeId;
        double g;
        double h;
        double f;
        Long parent;

        NodeRecord(long nodeId, double g, double f, Long parent) {
            this.nodeId = nodeId;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
}