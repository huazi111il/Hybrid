package com.huazi.hybrid.Service;

import java.io.IOException;

/**
 * OSM 图构建器接口
 */
public interface OsmGraphBuilder {
    /**
     * 从指定路径的 OSM PBF 文件构建内存图
     * @param filePath OSM 文件路径
     * @throws IOException 文件读取异常
     */
    void buildGraph(String filePath) throws IOException;

    /**
     * 获取构建后的图（可选，如果外部需要直接访问 Graph）
     */
    // 可以不加，因为 Graph 本身是独立的 Bean
}
