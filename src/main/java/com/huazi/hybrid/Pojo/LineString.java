package com.huazi.hybrid.Pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/*x线几何

 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LineString extends Geometry {
    private List<List<Double>> coordinates; // [[经度, 纬度], ...]
}