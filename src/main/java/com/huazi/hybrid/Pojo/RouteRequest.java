package com.huazi.hybrid.Pojo;

import lombok.Data;

/*请求对象RouteRequest

 */

@Data
public class RouteRequest {
    private Point start;
    private Point end;

    @Data
    public static class Point {
        private double lng; // 经度
        private double lat; // 纬度
    }
}