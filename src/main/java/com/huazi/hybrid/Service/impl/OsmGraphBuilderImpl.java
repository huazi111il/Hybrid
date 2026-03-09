package com.huazi.hybrid.Service.impl;

import com.huazi.hybrid.Pojo.GraphPojo.Edge;
import com.huazi.hybrid.Pojo.GraphPojo.Node;
import com.huazi.hybrid.Pojo.GraphPojo.Way;
import com.huazi.hybrid.Service.OsmGraphBuilder;
import com.huazi.hybrid.Pojo.GraphPojo.Graph;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class OsmGraphBuilderImpl implements OsmGraphBuilder {
    public OsmGraphBuilderImpl() {
        System.out.println("OsmGraphBuilderImpl constructor called");
    }

    @Autowired
    private Graph graph;

    @Value("${osm.file.path:data/city.osm.pbf}") // 默认路径，可在 application.properties 中修改
    private String osmFilePath;

    /**
     * 应用启动时自动加载 OSM 数据
     */
    @PostConstruct
    public void autoLoad() {
        System.out.println("autoLoad executed");
        try {
            System.out.println("开始加载 OSM 数据...");
            long start = System.currentTimeMillis();
            buildGraph(osmFilePath);
            long end = System.currentTimeMillis();
            System.out.println("OSM 数据加载完成，耗时：" + (end - start) + " ms");
            System.out.println("节点数：" + graph.getNodes().size());
            System.out.println("边数：" + graph.getOutgoingEdgesCount());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("OSM 数据加载失败！");
        }
    }

    @Override
    public void buildGraph(String filePath) throws IOException {
        // 清空旧数据
        graph.clear();

        // 1. 创建 OSM 迭代器
        InputStream input = Files.newInputStream(Paths.get(filePath));
        OsmIterator iterator = new PbfIterator(input, true);

        // 临时存储：节点ID -> Node
        Map<Long, Node> tempNodes = new HashMap<>();
        // 临时存储：Way 列表
        List<Way> tempWays = new ArrayList<>();

        // 2. 第一遍扫描：收集所有节点和符合条件的道路
        iterator.forEachRemaining(container -> {
            if (container.getType() == EntityType.Node) {
                OsmNode osmNode = (OsmNode) container.getEntity();
                Node node = new Node(osmNode.getId(), osmNode.getLatitude(), osmNode.getLongitude());
                tempNodes.put(node.getId(), node);
            } else if (container.getType() == EntityType.Way) {
                OsmWay osmWay = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmWay);

                // 只保留机动车道
                if (isRoadway(tags)) {
                    List<Long> nodeIds = new ArrayList<>();
                    for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
                        nodeIds.add(osmWay.getNodeId(i));
                    }
                    tempWays.add(new Way(osmWay.getId(), nodeIds, tags));
                }
            }
        });
        input.close();

        // 3. 将所有节点加入 Graph
        tempNodes.values().forEach(graph::addNode);

        // 4. 第二遍扫描：处理每条 Way，拆分成边
        for (Way way : tempWays) {
            List<Long> nodeIds = way.getNodeIds();
            Map<String, String> tags = way.getTags();

            String roadName = tags.getOrDefault("name", "");
            String roadType = tags.get("highway");
            boolean oneWay = isOneWay(tags);

            for (int i = 0; i < nodeIds.size() - 1; i++) {
                long fromId = nodeIds.get(i);
                long toId = nodeIds.get(i + 1);

                Node fromNode = tempNodes.get(fromId);
                Node toNode = tempNodes.get(toId);
                if (fromNode == null || toNode == null) continue;

                double distance = haversine(fromNode.getLon(), fromNode.getLat(),
                        toNode.getLon(), toNode.getLat());
                double speed = getSpeedForWayType(roadType);
                double time = distance / speed;

                Edge edge = new Edge(fromId, toId, distance, time, roadName, roadType, oneWay);
                graph.addEdge(edge);

                if (!oneWay) {
                    Edge reverseEdge = new Edge(toId, fromId, distance, time, roadName, roadType, oneWay);
                    graph.addEdge(reverseEdge);
                }
            }
        }
    }

    // ---------- 以下为辅助方法，与之前相同 ----------
    private boolean isRoadway(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null) return false;
        return highway.equals("motorway") || highway.equals("motorway_link")
                || highway.equals("trunk") || highway.equals("trunk_link")
                || highway.equals("primary") || highway.equals("primary_link")
                || highway.equals("secondary") || highway.equals("secondary_link")
                || highway.equals("tertiary") || highway.equals("tertiary_link")
                || highway.equals("residential") || highway.equals("unclassified")
                || highway.equals("service");
    }

    private boolean isOneWay(Map<String, String> tags) {
        String oneway = tags.get("oneway");
        return "yes".equals(oneway) || "1".equals(oneway) || "-1".equals(oneway);
    }

    private double getSpeedForWayType(String highway) {
        switch (highway) {
            case "motorway":
            case "motorway_link":
                return 27.8;
            case "trunk":
            case "trunk_link":
                return 25.0;
            case "primary":
            case "primary_link":
                return 22.2;
            case "secondary":
            case "secondary_link":
                return 19.4;
            case "tertiary":
            case "tertiary_link":
                return 16.7;
            case "residential":
                return 11.1;
            case "service":
                return 8.3;
            default:
                return 13.9;
        }
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
