package Controller;

import Pojo.*;
import Service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    @Autowired
    private RouteService routeService;
    @PostMapping
    public ResponseEntity<FeatureCollection> planRoute(@RequestBody RouteRequest request) {

        // 1. 获取起点终点坐标
        double startLng = request.getStart().getLng();
        double startLat = request.getStart().getLat();
        double endLng = request.getEnd().getLng();
        double endLat = request.getEnd().getLat();

        // 2. 调用路径规划服务（这里用模拟数据）
        List<List<Double>> pathCoords = routeService.mockPathCoords();
        double totalTime = 1250; // 模拟总时间

        // 3. 构建 FeatureCollection
        FeatureCollection fc = new FeatureCollection();
        List<Feature> features = new ArrayList<>();

        // 3.1 路径线要素
        Feature lineFeature = new Feature();
        LineString line = new LineString();
        line.setCoordinates(pathCoords);
        lineFeature.setGeometry(line);
        Map<String, Object> lineProps = new HashMap<>();
        lineProps.put("totalTime", totalTime);
        lineFeature.setProperties(lineProps);
        features.add(lineFeature);

        // 3.2 起点要素
        Feature startFeature = new Feature();
        Point startPoint = new Point();
        startPoint.setCoordinates(List.of(startLng, startLat));
        startFeature.setGeometry(startPoint);
        Map<String, Object> startProps = new HashMap<>();
        startProps.put("type", "start");
        startFeature.setProperties(startProps);
        features.add(startFeature);

        // 3.3 终点要素
        Feature endFeature = new Feature();
        Point endPoint = new Point();
        endPoint.setCoordinates(List.of(endLng, endLat));
        endFeature.setGeometry(endPoint);
        Map<String, Object> endProps = new HashMap<>();
        endProps.put("type", "end");
        endFeature.setProperties(endProps);
        features.add(endFeature);

        fc.setFeatures(features);
        return ResponseEntity.ok(fc);
    }

}