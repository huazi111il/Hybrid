package com.huazi.hybrid.Service;

import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import com.huazi.hybrid.Pojo.GraphPojo.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    @Autowired
    private Graph graph;

    public Node getNode(long nodeId) {
        return graph.getNode(nodeId);
    }

    /**
     * 查找距离给定经纬度最近的节点（简单遍历，适用于节点数<200万）
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
        return bestNodeId;
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
}