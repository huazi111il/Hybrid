package com.huazi.hybrid.Pojo.GraphPojo;

import lombok.Data;

/**
 * 图节点，对应 OSM 中的 Node
 */
@Data
public class Node {
    private long id;          // OSM 节点 ID
    private double lat;       // 纬度
    private double lon;       // 经度

    public Node(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }
}