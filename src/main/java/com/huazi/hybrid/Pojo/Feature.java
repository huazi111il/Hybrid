package com.huazi.hybrid.Pojo;
/*要素

 */

import lombok.Data;

import java.util.Map;

@Data
public class Feature {
    private final String type = "Feature";
    private Geometry geometry;
    private Map<String, Object> properties; // 可存放 totalTime、segments 等
}