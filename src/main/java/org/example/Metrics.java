package org.example;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Metrics {

    private double averagePolygonComplexity;
    private double maxPolygonComplexity;

    private double averageCartographicError;
    private double maxCartographicError;

    private double averageThinness;
    private double minThinness;
    private int absoluteOfThinness;

    private int nodes;
    private int edges;
    private int cartogramNodes;
    private int holes;
    private int nonHoles;
    private int cartogramEdges;
    private double averageEdgeLength;

    private long runningTime;
    private int weightRatio;
    private float nestingRatio;

    @Override
    public String toString() {
        String result = "\nGraph Nodes: " + nodes +
                "\nGraph Edges: " + edges +
                "\nCartogram Nodes: " + cartogramNodes +
                "\nCartogram Non Holes: " + nonHoles +
                "\nCartogram Holes: " + holes +
                "\nCartogram Edges: " + cartogramEdges +
                "\nCartogram Average Edge Length: " + averageEdgeLength +
                "\nMaximum Cartographic Error: " + (float) (Math.round(this.maxCartographicError * 10000)) / 100 +
                "\nAverage Cartographic Error: " + (float) (Math.round(this.averageCartographicError * 10000)) / 100 +
                "\nMaximum Polygon Complexity: " + (float) (Math.round(this.maxPolygonComplexity * 10000)) / 100 +
                "\nAverage Polygon Complexity: " + (float) (Math.round(this.averagePolygonComplexity * 10000)) / 100 +
                "\nMinimum Thinness: " + (float) (Math.round(this.minThinness * 1000000)) / 1000000 +
                "\nAverage Thinness: " + (float) (Math.round(this.averageThinness * 1000000)) / 1000000 +
                "\nNumber of thin skeleton vertice: " + absoluteOfThinness + "\n";
        return result;
    }
}
