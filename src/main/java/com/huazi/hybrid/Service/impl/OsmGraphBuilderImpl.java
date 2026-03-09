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

    @Value("${osm.file.path:data/city.osm.pbf}")
    private String osmFilePath;

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
        graph.clear();

        InputStream input = Files.newInputStream(Paths.get(filePath));
        OsmIterator iterator = new PbfIterator(input, true);

        Map<Long, Node> tempNodes = new HashMap<>();
        List<Way> tempWays = new ArrayList<>();

        iterator.forEachRemaining(container -> {
            if (container.getType() == EntityType.Node) {
                OsmNode osmNode = (OsmNode) container.getEntity();
                Node node = new Node(osmNode.getId(), osmNode.getLatitude(), osmNode.getLongitude());
                tempNodes.put(node.getId(), node);
            } else if (container.getType() == EntityType.Way) {
                OsmWay osmWay = (OsmWay) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(osmWay);
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

        tempNodes.values().forEach(graph::addNode);

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

        buildSubwayNetwork();
    }

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
                return 22.2;
            case "trunk":
            case "trunk_link":
                return 19.4;
            case "primary":
            case "primary_link":
                return 16.7;
            case "secondary":
            case "secondary_link":
                return 13.9;
            case "tertiary":
            case "tertiary_link":
                return 11.1;
            case "residential":
                return 8.3;
            case "service":
                return 5.6;
            default:
                return 11.1;
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

    private void buildSubwayNetwork() {
        System.out.println("开始构建地铁网络...");
        long offset = 10000000000L;

        List<SubwayStation> stations = Arrays.asList(
                new SubwayStation(1 + offset, "海口站", 110.316, 20.030),
                new SubwayStation(2 + offset, "龙华站", 110.340, 20.030),
                new SubwayStation(3 + offset, "国兴站", 110.365, 20.015),
                new SubwayStation(4 + offset, "府城站", 110.380, 20.000)
        );

        for (SubwayStation s : stations) {
            graph.addNode(new Node(s.id, s.lat, s.lon));
        }

        // 地铁速度提高到30 m/s（108 km/h）
        double subwaySpeed = 30.0;
        List<Long> lineNodeIds = Arrays.asList(
                stations.get(0).id, stations.get(1).id, stations.get(2).id, stations.get(3).id
        );
        for (int i = 0; i < lineNodeIds.size() - 1; i++) {
            long fromId = lineNodeIds.get(i);
            long toId = lineNodeIds.get(i + 1);
            Node fromNode = graph.getNode(fromId);
            Node toNode = graph.getNode(toId);
            double distance = haversine(fromNode.getLon(), fromNode.getLat(), toNode.getLon(), toNode.getLat());
            double time = distance / subwaySpeed;
            Edge edge = new Edge(fromId, toId, distance, time, "subway", "subway", false);
            graph.addEdge(edge);
            Edge reverse = new Edge(toId, fromId, distance, time, "subway", "subway", false);
            graph.addEdge(reverse);
            System.out.printf("添加地铁边: %d <-> %d, 距离 %.0f m, 时间 %.1f s%n", fromId, toId, distance, time);
        }

        double walkSpeed = 1.4;
        double maxWalkDistance = 1000; // 增加到1000米
        int transferCount = 0;
        for (SubwayStation station : stations) {
            for (Node roadNode : graph.getNodes().values()) {
                if (roadNode.getId() > offset) continue;
                double dist = haversine(station.lon, station.lat, roadNode.getLon(), roadNode.getLat());
                if (dist <= maxWalkDistance) {
                    double walkTime = dist / walkSpeed;
                    graph.addEdge(new Edge(roadNode.getId(), station.id, dist, walkTime, "walk", "walk", false));
                    graph.addEdge(new Edge(station.id, roadNode.getId(), dist, walkTime, "walk", "walk", false));
                    transferCount++;
                }
            }
        }
        System.out.println("换乘边添加数量: " + transferCount);
        System.out.println("地铁网络构建完成，当前总节点数: " + graph.getNodes().size());
        System.out.println("总边数: " + graph.getOutgoingEdgesCount());
    }

    private static class SubwayStation {
        long id;
        String name;
        double lon, lat;
        SubwayStation(long id, String name, double lon, double lat) {
            this.id = id; this.name = name; this.lon = lon; this.lat = lat;
        }
    }
}