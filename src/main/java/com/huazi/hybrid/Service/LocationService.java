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
            throw new RuntimeException("图中没有节点");
        }
        return bestNodeId;
    }

    public long findNearestRoadNode(double lon, double lat, boolean requireOutgoing) {
        long bestNodeId = -1;
        double bestDistance = Double.MAX_VALUE;
        Set<Long> roadNodeIds = graph.getRoadNodeIds();
        for (long nodeId : roadNodeIds) {
            if (requireOutgoing && graph.getOutgoingEdges(nodeId).isEmpty()) {
                continue;
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

    public Node getNode(long nodeId) {
        return graph.getNode(nodeId);
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