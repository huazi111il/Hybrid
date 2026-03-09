package com.huazi.hybrid.Service.impl;

import com.huazi.hybrid.Service.RouteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteServiceImpl implements RouteService {
    @Override
    public List<List<Double>> mockPathCoords() {
        return List.of(
                List.of(116.397, 39.908),
                List.of(116.400, 39.905),
                List.of(116.405, 39.900),
                List.of(116.410, 39.895));
    }

}
