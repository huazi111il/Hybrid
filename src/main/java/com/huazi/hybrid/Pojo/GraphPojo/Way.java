package com.huazi.hybrid.Pojo.GraphPojo;


import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * OSM Way 的临时表示，用于解析过程中
 */
@Data
public class Way {
    private long id;                       // OSM Way ID
    private List<Long> nodeIds;             // 组成这条路的节点 ID 列表
    private Map<String, String> tags;       // 标签，如 highway=primary, name=xx 等

    public Way(long id, List<Long> nodeIds, Map<String, String> tags) {
        this.id = id;
        this.nodeIds = nodeIds;
        this.tags = tags;
    }
}