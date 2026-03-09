package com.huazi.hybrid.Pojo.GraphPojo;
import lombok.Data;

/**
 * 图中的边，表示两个节点之间的路段
 */
@Data
public class Edge {
    private long fromNodeId;      // 起始节点 ID
    private long toNodeId;        // 目标节点 ID
    private double distance;       // 距离（米）
    private double time;           // 通行时间（秒）
    private String roadName;       // 道路名称（可选）
    private String roadType;       // 道路类型，如 motorway, primary, residential 等
    private boolean oneWay;        // 是否单向

    public Edge(long fromNodeId, long toNodeId, double distance, double time, String roadName, String roadType, boolean oneWay) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distance = distance;
        this.time = time;
        this.roadName = roadName;
        this.roadType = roadType;
        this.oneWay = oneWay;
    }
}