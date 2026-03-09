package com.huazi.hybrid.Controller;

import com.huazi.hybrid.Pojo.GraphPojo.Node;
import com.huazi.hybrid.Service.LocationService;
import com.huazi.hybrid.Service.RoutePlanService;
import com.huazi.hybrid.Service.TrafficSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private LocationService locationService;

    @Autowired
    private RoutePlanService routePlanService;

    @Autowired
    private TrafficSimulator trafficSimulator;

    @PostMapping
    public ResponseEntity<Map<String, Object>> planRoute(@RequestBody RouteRequest request) {

        long startNodeId = locationService.findNearestRoadNode(request.start.lng, request.start.lat, true);
        long endNodeId = locationService.findNearestRoadNode(request.end.lng, request.end.lat, false);

        RoutePlanService.PathResult result = routePlanService.aStar(startNodeId, endNodeId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        List<List<Double>> pathCoords = result.nodeIds.stream()
                .map(nodeId -> {
                    Node node = locationService.getNode(nodeId);
                    return List.of(node.getLon(), node.getLat());
                })
                .collect(Collectors.toList());

        // 构建分段信息
        List<Map<String, Object>> segmentsJson = new ArrayList<>();
        if (result.segments != null) {
            for (RoutePlanService.PathSegment seg : result.segments) {
                List<List<Double>> segCoords = seg.nodeIds.stream()
                        .map(nodeId -> {
                            Node node = locationService.getNode(nodeId);
                            return List.of(node.getLon(), node.getLat());
                        })
                        .collect(Collectors.toList());
                Map<String, Object> segMap = new HashMap<>();
                segMap.put("mode", seg.mode);
                segMap.put("coordinates", segCoords);
                segMap.put("distance", seg.distance);
                segMap.put("time", seg.time);
                segMap.put("avgCongestion", seg.avgCongestion);
                segmentsJson.add(segMap);
            }
        }

        Map<String, Object> response = buildGeoJson(pathCoords, result.totalTime,
                request.start.lng, request.start.lat,
                request.end.lng, request.end.lat,
                segmentsJson);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/traffic/random")
    public ResponseEntity<?> randomTraffic() {
        trafficSimulator.updateTraffic();
        return ResponseEntity.ok().build();
    }

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
                                             double endLng, double endLat,
                                             List<Map<String, Object>> segments) {
        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();

        Map<String, Object> lineFeature = new HashMap<>();
        lineFeature.put("type", "Feature");
        Map<String, Object> lineGeometry = new HashMap<>();
        lineGeometry.put("type", "LineString");
        lineGeometry.put("coordinates", pathCoords);
        lineFeature.put("geometry", lineGeometry);

        Map<String, Object> lineProps = new HashMap<>();
        lineProps.put("totalTime", totalTime);
        if (segments != null && !segments.isEmpty()) {
            lineProps.put("segments", segments);
        }
        lineFeature.put("properties", lineProps);
        features.add(lineFeature);

        features.add(createPointFeature(startLng, startLat, "start"));
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