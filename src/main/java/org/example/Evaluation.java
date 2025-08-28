package org.example;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;

import java.util.*;

@Getter
@Setter
public class Evaluation {

    private double averagePolygonComplexity;
    private double maxPolygonComplexity;
    private Map<RegionNode, Double> polygonComplexityByRegion;

    private double averageCartographicError;
    private double maxCartographicError;
    private Map<RegionNode, Double> cartographicErrorByRegion;

    private Region region;

    private int holes;
    private int nonHoles;
    private int edges;
    private double averageEdgeLength;

    public Evaluation(Region region) {
        this.region = region;
        polygonComplexityByRegion = new HashMap<>();
        cartographicErrorByRegion = new HashMap<>();
    }

    public void metrics(){

        holes=0;
        nonHoles=0;
        for(RegionNode regionNode : region.getRegionNodes()){
            if(!regionNode.getCorrespondingNode().isHole()) {
                nonHoles++;
            }else{
                holes++;
            }
        }
        edges=region.getCartogram().getEdges().size();
        averageEdgeLength=0;
        for(org.example.Edge edge : region.getCartogram().getEdges()){
            averageEdgeLength += Geometry.getDistance(edge);
        }
        averageEdgeLength/=edges;

        this.cartographicError();
//        System.out.println("Maximum Cartographic Error: "+(float)(Math.round(this.maxCartographicError*10000))/100);
//        System.out.println("Average Cartographic Error: "+(float)(Math.round(this.averageCartographicError*10000))/100);

        this.polygonComplexity();
//        System.out.println("Maximum Polygon Complexity: "+ (float)(Math.round(this.maxPolygonComplexity*10000))/100);
//        System.out.println("Average Polygon Complexity: "+(float)(Math.round(this.averagePolygonComplexity*10000))/100);

    }

    private void cartographicError(){

        averageCartographicError=0;
        maxCartographicError=0;

        double totalArea = 0;
        double totalWeight = 0;
        for(RegionNode regionNode : region.getRegionNodes()){
            if(!regionNode.getCorrespondingNode().isHole()) {
                totalArea += Math.abs(regionNode.getArea());
                totalWeight += Math.abs(regionNode.getCorrespondingNode().getWeight());
            }
        }

        region.setTotalArea(totalArea);
        region.setTotalWeight(totalWeight);

        for(RegionNode regionNode : region.getRegionNodes()){
            if(!regionNode.getCorrespondingNode().isHole()){
                double adjArea = Math.abs(totalWeight * Math.abs(regionNode.getArea()))/(totalArea);
                double ce = Math.abs(adjArea - regionNode.getCorrespondingNode().getWeight())/Math.max(adjArea, regionNode.getCorrespondingNode().getWeight());
                cartographicErrorByRegion.put(regionNode, ce);

                averageCartographicError += ce;
                if(ce>maxCartographicError){
                    maxCartographicError = ce;
                }
            }

        }
        averageCartographicError = averageCartographicError / nonHoles;
    }

    private void polygonComplexity(){
        averagePolygonComplexity=0;
        maxPolygonComplexity=0;
        for(RegionNode regionNode : region.getRegionNodes()){
            if(!regionNode.getCorrespondingNode().isHole()){
                regionNode.updateValues();

                List<Dart> polygon=regionNode.getBoundaryNodes();
                double secArea = Geometry.smallestEnclosingCircle(polygon);
                double polArea = Geometry.calculateAreaDarts(polygon);
                double comv = 1 - Math.abs(polArea)/(secArea * polygon.size() / 2 / Math.PI * Math.sin(2 * Math.PI / polygon.size()));
                double freq = Evaluation.calculateFreq(polygon);
                double[] arr = Evaluation.calculateAmpl(polygon);
                double ampl = arr[0];
                double comp = 0.8*ampl*freq + 0.2*comv;
                polygonComplexityByRegion.put(regionNode, comp);

                averagePolygonComplexity += comp;
                if(comp>maxPolygonComplexity){
                    maxPolygonComplexity = comp;
                }
            }
        }
        averagePolygonComplexity = averagePolygonComplexity / nonHoles;
    }


    private static double[] calculateAmpl(List<Dart> face){

        double[] hull = Geometry.convexHullAreaAndCircumference(face);

        double circ = 0;
        for(Dart d : face){
            circ += Geometry.getDistance(d.getEdge());
        }

        double ampl = (circ - hull[1])/ circ;
        double[] arr = new double[2];
        arr[0] = ampl;
        arr[1] = hull[0];
        return arr;
    }

    private static double calculateFreq(List<Dart> face){
        double lpCounter = 0;
        for(Dart d : face){
            Node a=d.getFrom();
            Node b=d.getTo();
            Node c=d.getNext().getTo();
            Vector2d yv1=Geometry.getVector(b, a);
            Vector2d yv2=Geometry.getVector(b, c);
            double ang= Geometry.clockwiseAngle(yv2, yv1);
            if(ang > Math.PI){
                lpCounter++;
            }
        }

        double lpPrime = lpCounter / (face.size() - 3);
        return 1 + 16* Math.pow((lpPrime - 0.5), 4) - 8*Math.pow((lpPrime - 0.5), 2);
    }
}
