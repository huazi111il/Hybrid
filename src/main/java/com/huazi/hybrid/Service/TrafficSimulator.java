package com.huazi.hybrid.Service;

import com.huazi.hybrid.Pojo.GraphPojo.Edge;
import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class TrafficSimulator {

    @Autowired
    private Graph graph;

    private final Random random = new Random();

    public void updateTraffic() {
        int updateCount = 0;
        for (long fromId : graph.getRoadNodeIds()) {
            if (random.nextDouble() < 0.01) {
                List<Edge> edges = graph.getOutgoingEdges(fromId);
                if (!edges.isEmpty()) {
                    Edge edge = edges.get(random.nextInt(edges.size()));
                    double factor = 1.0 + random.nextDouble() * 2.0;
                    edge.setCongestionFactor(factor);
                    updateCount++;
                }
            }
        }
        System.out.println("交通模拟更新了 " + updateCount + " 条边的拥堵系数");
    }
}