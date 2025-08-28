package org.example;

import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImPrEdMovement {

    public static void impredSanitize(Region region){
        Map<Node, Double[]> nodeMovementMap = new HashMap<>();
        for(Node node:region.getCartogram().getNodes()){
            nodeMovementMap.put(node, new Double[]{400d, 400d, 400d, 400d, 400d, 400d, 400d, 400d});
        }
        ImPrEdMovement.runStep(region, nodeMovementMap);
        Vector2d universalLine = new Vector2d(0,-1);

        for(Node node:region.getCartogram().getNodes()){
            Vector2d yv = region.getCartogram().getDisplacements().get(node);
            double prevLen = yv.length();

            Double[] maxLength = nodeMovementMap.get(node);
            double angle = Geometry.clockwiseAngle(universalLine, yv);
            int ind = (int) Math.floor(angle/Math.PI*4);
            if(maxLength[ind] < prevLen){
                yv.normalize();
                yv.mul(maxLength[ind]);
                region.getCartogram().getDisplacements().put(node, yv);
            }
        }
    }

    public static void runStep(Region region, Map<Node, Double[]> nodeMovementMap) {
        Graph cartogram = region.getCartogram();
        Map<Node, List<Edge>> edgesToWatch=ImPrEdMovement.edgesToWatch(region);
        // let allEdges = graph.edges.toArray()
        for(Node node : cartogram.getNodes()) {
            List<Edge> allEdges = edgesToWatch.get(node);
            for(Edge edge : allEdges) {
                if(edge.getSource() != node && edge.getTarget() != node){
                    ImPrEdMovement.maxNodeMovement(node, edge.getSource(), edge.getTarget(), nodeMovementMap);
                }
            }
        }
    }

    public static Map<Node,List<Edge>> edgesToWatch(Region region){

        Map<Node,List<Edge>> edgesToWatch = new HashMap<Node,List<Edge>>();
        for(RegionNode regionNode: region.getRegionNodes()){
            for(Dart dart: regionNode.getBoundaryNodes()){
                Node node=dart.getFrom();
                for(Dart dart2: regionNode.getBoundaryNodes()){
                    if(!dart2.getFrom().equals(node) && !dart2.getTo().equals(node)){
                        if(edgesToWatch.containsKey(node)){
                            edgesToWatch.get(node).add(dart2.getEdge());
                        }else{
                            ArrayList<Edge> edges = new ArrayList<Edge>();
                            edges.add(dart2.getEdge());
                            edgesToWatch.put(node,edges);
                        }
                    }
                }
            }
        }

        for(Dart dart: region.getCartogram().getOuterface()){
            Node node=dart.getFrom();
            for(Dart dart2: region.getCartogram().getOuterface()){
                if(!dart2.getFrom().equals(node) && !dart2.getTo().equals(node)){
                    if(edgesToWatch.containsKey(node)){
                        edgesToWatch.get(node).add(dart2.getEdge());
                    }else{
                        ArrayList<Edge> edges = new ArrayList<Edge>();
                        edges.add(dart2.getEdge());
                        edgesToWatch.put(node,edges);
                    }
                }
            }
        }

        return edgesToWatch;
    }

    public static void maxNodeMovement(Node node, Node v1, Node v2, Map<Node, Double[]> nodeMovementMap){
        Node ve=Geometry.getProjectionOnSegment(v1, v2, node);

        //get l line
        Vector2d orthoV = Geometry.getVector(ve, node);

        Node midPoint = new Node((node.getX() + ve.getX())/2, (node.getY() + ve.getY())/ 2);
        orthoV = new Vector2d(-orthoV.y, orthoV.x);
        orthoV = orthoV.normalize();

        Node collisionPointv = Geometry.getProjectionOnLine(node, midPoint, orthoV);
        Vector2d collisionV = Geometry.getVector(collisionPointv, node);

        Node collisionPointw = Geometry.getProjectionOnLine(v1, midPoint, orthoV);
        Vector2d collisionW = Geometry.getVector(collisionPointw, v1);

        Node collisionPointz = Geometry.getProjectionOnLine(v2, midPoint, orthoV);
        Vector2d collisionZ =Geometry.getVector(collisionPointz, v2);

        Vector2d universalLine = new Vector2d(0,-1);

        double movementV = 0.66*collisionV.length();
        if(movementV < 0.01){
            movementV = 0;
        }
        double angleV = Geometry.clockwiseAngle(universalLine, collisionV);
        double[] movArrV = ImPrEdMovement.maxMovementArr(movementV, angleV);

        double movementW = 0.66*collisionW.length();
        if(movementW < 0.01){
            movementW = 0;
        }
        double angleW = Geometry.clockwiseAngle(universalLine, collisionW);
        double[] movArrW = ImPrEdMovement.maxMovementArr(movementW, angleW);
        double movementZ = 0.66*collisionZ.length();
        if(movementZ < 0.01){
            movementZ = 0;
        }
        double angleZ = Geometry.clockwiseAngle(universalLine, collisionZ);
        double[] movArrZ = ImPrEdMovement.maxMovementArr(movementZ, angleZ);

        Double[] prevV = nodeMovementMap.get(node);
        for(int i = 0; i < 8; i++){
            if(prevV[i] > movArrV[i]){
                prevV[i] = movArrV[i];
            }
        }
        nodeMovementMap.put(node, prevV);
        Double[] prevW = nodeMovementMap.get(v1);
        for(int i = 0; i < 8; i++){
            if(prevW[i] > movArrW[i]){
                prevW[i] = movArrW[i];
            }
        }
        nodeMovementMap.put(v1, prevW);
        Double[] prevZ = nodeMovementMap.get(v2);
        for(int i = 0; i < 8; i++){
            if(prevZ[i] > movArrZ[i]){
                prevZ[i] = movArrZ[i];
            }
        }
        nodeMovementMap.put(v2, prevZ);


    }

    public static double[] maxMovementArr(double movement, double angle){
        int ind = (int) Math.floor(angle/Math.PI*4);
        double minMovement = 400;
        double[] movementArr = new double[]{minMovement, minMovement, minMovement, minMovement, minMovement, minMovement, minMovement, minMovement};

        movementArr[ind%8] = movement;
        movementArr[(ind - 1 + 8)%8] = movement  / Math.cos(angle - ((ind))*Math.PI / 4);
        movementArr[(ind - 2 + 8)%8] = movement  / Math.cos(angle - ((ind-1))*Math.PI / 4);
        movementArr[(ind + 2)%8] = movement  / Math.cos(angle - ((ind+2))*Math.PI / 4);
        movementArr[(ind + 1)%8] = movement  / Math.cos(angle - ((ind+1))*Math.PI / 4);

        return movementArr;
    }


}
