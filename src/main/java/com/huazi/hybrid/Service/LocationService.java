package com.huazi.hybrid.Service;

import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import com.huazi.hybrid.Pojo.GraphPojo.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LocationService {

    @Autowired
    private Graph graph;

    /**
     * 查找距离给定经纬度最近的节点（遍历所有节点，不考虑道路连接）
     * 适用于只有节点数据但不需要道路连通性的场景
     */
    public long findNearestNode(double lon, double lat) {
        long bestNodeId = -1;
        double bestDistance = Double.MAX_VALUE;
        for (Node node : graph.getNodes().values()) {
            double dist = haversine(lon, lat, node.getLon(), node.getLat());
            if (dist < bestDistance) {
                bestDistance = dist;
                bestNodeId = node.getId();
            }
        }
        if (bestNodeId == -1) {
            throw new RuntimeException("图中没有节点，无法查找最近点");
        }
        return bestNodeId;
    }

    /**
     * 查找距离给定经纬度最近的道路节点（只考虑出现在边中的节点）
     * @param lon 经度
     * @param lat 纬度
     * @param requireOutgoing 是否要求节点有出边（作为起点时需要）
     * @return 节点ID
     */
    public long findNearestRoadNode(double lon, double lat, boolean requireOutgoing) {
        long bestNodeId = -1;
        double bestDistance = Double.MAX_VALUE;
        Set<Long> roadNodeIds = graph.getRoadNodeIds();
        for (long nodeId : roadNodeIds) {
            if (requireOutgoing && graph.getOutgoingEdges(nodeId).isEmpty()) {
                continue; // 跳过没有出边的节点
            }
            Node node = graph.getNode(nodeId);
            if (node == null) continue;
            double dist = haversine(lon, lat, node.getLon(), node.getLat());
            if (dist < bestDistance) {
                bestDistance = dist;
                bestNodeId = nodeId;
            }
        }
        if (bestNodeId == -1) {
            throw new RuntimeException("没有找到符合条件的道路节点");
        }
        return bestNodeId;
    }

    /**
     * 获取节点对象
     */
    public Node getNode(long nodeId) {
        return graph.getNode(nodeId);
    }

    /**
     * 计算两个经纬度点之间的距离（米），使用 Haversine 公式
     */
    private double haversine(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371000; // 地球半径，单位米
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}