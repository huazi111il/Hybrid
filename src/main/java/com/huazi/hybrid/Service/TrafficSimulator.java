package com.huazi.hybrid.Service;

import com.huazi.hybrid.Pojo.GraphPojo.Edge;
import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class TrafficSimulator {

    @Autowired
    private Graph graph;

    private final Random random = new Random();

    /**
     * 随机更新一部分边的拥堵系数（可供手动调用或定时任务）
     */
    public void updateTraffic() {
        int updateCount = 0;
        for (long fromId : graph.getRoadNodeIds()) {
            if (random.nextDouble() < 0.01) { // 1% 的节点被选中
                List<Edge> edges = graph.getOutgoingEdges(fromId);
                if (!edges.isEmpty()) {
                    Edge edge = edges.get(random.nextInt(edges.size()));
                    double factor = 1.0 + random.nextDouble() * 2.0; // 1.0 ~ 3.0
                    edge.setCongestionFactor(factor);
                    updateCount++;
                }
            }
        }
        System.out.println("交通模拟更新了 " + updateCount + " 条边的拥堵系数");
    }

    /**
     * 定时任务：每60秒自动更新一次（可选，可注释掉）
     */
    // @Scheduled(fixedDelay = 60000)
    public void autoUpdate() {
        updateTraffic();
    }
}