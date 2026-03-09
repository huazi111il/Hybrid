package Pojo;

import lombok.Data;

import java.util.List;

/*点几何

 */
@Data
public class Point extends Geometry {
    private List<Double> coordinates; // [经度, 纬度]
}