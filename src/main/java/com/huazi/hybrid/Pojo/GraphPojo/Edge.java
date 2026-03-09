package com.huazi.hybrid.Pojo.GraphPojo;

import lombok.Data;

/**
 * 图中的边，表示两个节点之间的路段
 */
@Data
public class Edge {
    private long fromNodeId;
    private long toNodeId;
    private double distance;       // 距离（米）
    private double time;           // 基础通行时间（秒）
    private String roadName;
    private String roadType;
    private boolean oneWay;

    // 新增：拥堵系数，默认1.0（畅通）
    private double congestionFactor = 1.0;

    public Edge(long fromNodeId, long toNodeId, double distance, double time, String roadName, String roadType, boolean oneWay) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distance = distance;
        this.time = time;
        this.roadName = roadName;
        this.roadType = roadType;
        this.oneWay = oneWay;
    }

    // 新增：获取实时通行时间（考虑拥堵）
    public double getRealTime() {
        return time * congestionFactor;
    }
}