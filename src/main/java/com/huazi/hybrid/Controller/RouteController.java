package com.huazi.hybrid.Controller;

import com.huazi.hybrid.Pojo.GraphPojo.Node;
import com.huazi.hybrid.Service.LocationService;
import com.huazi.hybrid.Service.RoutePlanService;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private LocationService locationService;

    @Autowired
    private RoutePlanService routePlanService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> planRoute(@RequestBody RouteRequest request) {

        // 1. 坐标转节点ID
        long startNodeId = locationService.findNearestNode(request.start.lng, request.start.lat);
        long endNodeId = locationService.findNearestNode(request.end.lng, request.end.lat);

        // 2. 调用 A* 算法
        RoutePlanService.PathResult result = routePlanService.aStar(startNodeId, endNodeId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        // 3. 将节点ID列表转换为经纬度坐标列表
        List<List<Double>> pathCoords = result.nodeIds.stream()
                .map(nodeId -> {
                    Node node = locationService.getNode(nodeId); // 需在LocationService中添加getNode方法
                    return List.of(node.getLon(), node.getLat());
                })
                .collect(Collectors.toList());

        // 4. 构建GeoJSON响应
        Map<String, Object> response = buildGeoJson(pathCoords, result.totalTime,
                request.start.lng, request.start.lat,
                request.end.lng, request.end.lat);
        return ResponseEntity.ok(response);
    }

    // 内部请求类
    static class RouteRequest {
        public Point start;
        public Point end;
        static class Point {
            public double lng;
            public double lat;
        }
    }

    private Map<String, Object> buildGeoJson(List<List<Double>> pathCoords, double totalTime,
                                             double startLng, double startLat,
                                             double endLng, double endLat) {
        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();

        // 路径线
        Map<String, Object> lineFeature = new HashMap<>();
        lineFeature.put("type", "Feature");
        Map<String, Object> lineGeometry = new HashMap<>();
        lineGeometry.put("type", "LineString");
        lineGeometry.put("coordinates", pathCoords);
        lineFeature.put("geometry", lineGeometry);
        Map<String, Object> lineProps = new HashMap<>();
        lineProps.put("totalTime", totalTime);
        lineFeature.put("properties", lineProps);
        features.add(lineFeature);

        // 起点
        features.add(createPointFeature(startLng, startLat, "start"));
        // 终点
        features.add(createPointFeature(endLng, endLat, "end"));

        featureCollection.put("features", features);
        return featureCollection;
    }

    private Map<String, Object> createPointFeature(double lng, double lat, String type) {
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", List.of(lng, lat));
        feature.put("geometry", geometry);
        Map<String, Object> props = new HashMap<>();
        props.put("type", type);
        feature.put("properties", props);
        return feature;
    }
}