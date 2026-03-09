package com.huazi.hybrid.Pojo;

import lombok.Data;

import java.util.List;
/*GeoJSON 根对象

 */

@Data
public class FeatureCollection {
    private final String type = "FeatureCollection";
    private List<Feature> features;
}