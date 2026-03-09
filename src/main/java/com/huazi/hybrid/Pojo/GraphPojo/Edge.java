package com.huazi.hybrid.Pojo.GraphPojo;

import lombok.Data;

@Data
public class Edge {
    private long fromNodeId;
    private long toNodeId;
    private double distance;
    private double time;
    private String roadName;
    private String roadType;
    private boolean oneWay;
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

    public double getRealTime() {
        return time * congestionFactor;
    }
}