package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegionNode {

    private List<Dart> boundaryNodes = new LinkedList<>();
    private Node correspondingNode;
    private Double area;
    private Double addedWeight = 0.0d;
    private Double hardness = 1.0d;

    public double calculateCircumference() {
        double circumference = 0;
        for (Dart dart : boundaryNodes) {
            circumference += Geometry.getDistance(dart.getFrom(), dart.getTo());
        }
        return circumference;
    }

    public void updateValues(){
        area = Geometry.calculateAreaDarts(boundaryNodes);
    }
}