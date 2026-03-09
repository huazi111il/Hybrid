package com.huazi.hybrid.Pojo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*抽象几何类

 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Point.class, name = "Point"),
        @JsonSubTypes.Type(value = LineString.class, name = "LineString")
})
public abstract class Geometry {
}