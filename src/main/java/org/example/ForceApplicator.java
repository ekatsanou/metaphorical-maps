package org.example;

import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public class ForceApplicator {

    protected static float airPressureStrength = 3f; // 3
    protected static float vertexVertexRepulsionStrength = 25f; //25
    protected static float vertexEdgeRepulsionStrength = 10f; //10
    protected static float angularResolutionStrength = 0.5f; //0.5
    protected static float stiffnessMultipliers = 0.02f;
    protected static float stiffnessHigh = 8f;

    public static void improvedAngularResolution(Region region){
        Graph cartogram=region.getCartogram();
        for(Node node: cartogram.getNodes()){
            List<Dart> nodeDarts=cartogram.getOutgoingDarts().get(node);
            for (int j=0; j<nodeDarts.size(); j++){

                Node lNode = nodeDarts.get(j).getEdge().getSource() == node
                        ? nodeDarts.get(j).getEdge().getTarget()
                        : nodeDarts.get(j).getEdge().getSource();
                int index=Geometry.mod(j+1, nodeDarts.size());
                Node rNode = nodeDarts.get(index).getEdge().getSource() == node
                        ? nodeDarts.get(index).getEdge().getTarget()
                        : nodeDarts.get(index).getEdge().getSource();

                Vector2d start1 = new Vector2d(node.getX(), node.getY());
                Vector2d end1 = new Vector2d(lNode.getX(), lNode.getY());

                Vector2d leftV = end1.sub(start1);

                Vector2d start2 = new Vector2d(node.getX(), node.getY());
                Vector2d end2 = new Vector2d(rNode.getX(), rNode.getY());

                Vector2d rightV = end2.sub(start2);

                double currAngle = Geometry.clockwiseAngle(leftV, rightV);

                Vector2d bisector = Geometry.rotate(leftV, currAngle/2);
                bisector.normalize();
                double prefAngle = (2 * Math.PI) / nodeDarts.size();
                double fraction = (prefAngle - currAngle) / currAngle;

                bisector.mul(ForceApplicator.angularResolutionStrength * fraction);

                cartogram.addDisplacement(node, bisector);
            }
        }
    }

    public static void vertexVertexRepulsion(Region region){
        Graph cartogram=region.getCartogram();
        for(RegionNode regionNode: region.getRegionNodes()){
            List<Dart> faceDarts=regionNode.getBoundaryNodes();
            for (int j = 0; j < faceDarts.size(); j++) {
                for (int k = 0; k < j; k++) {
                    Node p1 = faceDarts.get(j).getFrom();
                    Node p2 = faceDarts.get(k).getFrom();

                    Vector2d yV = new Vector2d(p1.getX()-p2.getX(), p1.getY()-p2.getY());
                    double dist = Geometry.getDistance(p1, p2);
                    yV.normalize();
                    yV.mul(ForceApplicator.vertexVertexRepulsionStrength / Math.pow(dist, 2));
                    cartogram.addDisplacement(p1, yV);
                    cartogram.addDisplacement(p2, Geometry.rotate(yV, Math.PI));
                }
            }
        }

        for (int j = 0; j < cartogram.getOuterFaceNodes().size(); j++) {
            for (int k = 0; k < j; k++) {
                Node p1 = cartogram.getOuterFaceNodes().get(j);
                Node p2 = cartogram.getOuterFaceNodes().get(k);
                Vector2d yV = new Vector2d(p1.getX()-p2.getX(), p1.getY()-p2.getY());
                double dist = yV.length();
                yV.normalize();
                yV.mul(ForceApplicator.vertexVertexRepulsionStrength / Math.pow(dist, 2));
                cartogram.addDisplacement(p1, yV);
                cartogram.addDisplacement(p2, Geometry.rotate(yV, Math.PI));
            }
        }
    }

    public static void vertexEdgeRepulsion(Region region){
        Graph cartogram=region.getCartogram();

        for (RegionNode regionNode: region.getRegionNodes()) {
            List<Dart> faceDarts=regionNode.getBoundaryNodes();
            for (int j = 0; j < faceDarts.size(); j++) {

                Node mainPoint = faceDarts.get(j).getFrom();
                for (Dart dart: faceDarts) {
                    if (!dart.getFrom().equals(mainPoint) && !dart.getTo().equals(mainPoint)) {
                        Node p1 = dart.getFrom();
                        Node p2 = dart.getTo();

                        Node closestPoint = Geometry.getProjectionOnSegment(p1, p2, mainPoint);
                        Vector2d nVector = new Vector2d(p1.getX()-p2.getX(), p1.getY()-p2.getY());
                        nVector=new Vector2d(-nVector.y, nVector.x);
                        nVector.normalize();
                        Vector2d orthoVector = new Vector2d(mainPoint.getX()- closestPoint.getX(), mainPoint.getY() - closestPoint.getY());
                        double dist = Geometry.getDistance(mainPoint, closestPoint);
                        Vector2d appliedVector = new Vector2d(orthoVector).normalize();
                        double scalarPr = Math.abs(nVector.dot(appliedVector));
                        appliedVector.mul((ForceApplicator.vertexEdgeRepulsionStrength / Math.pow(dist, 2)) * scalarPr);
                        cartogram.addDisplacement(mainPoint, appliedVector);
                    }
                }
            }
        }
    }

    public static void airPressureOuter(Region region){
        Graph cartogram=region.getCartogram();
        double outerCircumference = 0;

        List<Node> outerFNodes=cartogram.getOuterFaceNodes();
        for (int i = 0; i < outerFNodes.size(); i++) {
            Node v1 =outerFNodes.get(i);
            Node v2 =outerFNodes.get(Geometry.mod(i-1, outerFNodes.size()));
            outerCircumference += Geometry.getDistance(v1, v2);
        }

        for (int i = 0; i < outerFNodes.size(); i++) {
            Node v1 =outerFNodes.get(i);
            Node v2 =outerFNodes.get(Geometry.mod(i-1, outerFNodes.size()));
            double fraction = Geometry.getDistance(v1, v2) / outerCircumference;
            Vector2d v = new Vector2d(v1.getX()-v2.getX(), v1.getY() - v2.getY());
            v=Geometry.rotate(v, -Math.PI/2);
            v.normalize();
            v.mul(ForceApplicator.airPressureStrength * fraction);
            cartogram.addDisplacement(v1, v);
            cartogram.addDisplacement(v2, v);
        }
    }

    public static void airPressure(Region region, boolean isAdjustingStiffness, List<List<Double>> nodeFactors) {
        Graph cartogram=region.getCartogram();

        double totalArea = region.getTotalArea();
        double totalWeight = region.getTotalWeight();

        double maxStiffness = 0;

        int i=0;
        for(RegionNode regionNode: region.getRegionNodes()){
            List<Dart> faceDarts=regionNode.getBoundaryNodes();
            double faceWeight = regionNode.getCorrespondingNode().getWeight() + regionNode.getAddedWeight();
            double circumference = regionNode.calculateCircumference();
            double pressure = (faceWeight * totalArea) / (regionNode.getArea() * totalWeight);

            if (isAdjustingStiffness && !regionNode.getCorrespondingNode().isHole()) {
                if (Math.abs(pressure) > 1.005) {
                    if (regionNode.getHardness() < ForceApplicator.stiffnessHigh) {
                        regionNode.setHardness(regionNode.getHardness() + ForceApplicator.stiffnessMultipliers);
                    }
                    maxStiffness = Math.max(maxStiffness, Math.abs(1 - regionNode.getHardness()));
                } else if (Math.abs(pressure) < 0.995) {
                    if (regionNode.getHardness() > 1/ForceApplicator.stiffnessHigh) {
                        regionNode.setHardness(regionNode.getHardness() - ForceApplicator.stiffnessMultipliers);
                    }
                    maxStiffness = Math.max(maxStiffness, Math.abs(1 - regionNode.getHardness()));
                }
                pressure *= regionNode.getHardness();
            }

            int j=0;
            for (Dart dart: faceDarts) {
                Vector2d yV = Geometry.rotate(new Vector2d(dart.getTo().getX()-dart.getFrom().getX(), dart.getTo().getY()-dart.getFrom().getY()), Math.PI / 2);
                double edgeLength = Geometry.getDistance(dart.getEdge());

                if(!nodeFactors.isEmpty()){
                    double fraction = (edgeLength*nodeFactors.get(i).get(j)) / (nodeFactors.get(i).getLast());
                    Vector2d yV1 =new Vector2d(yV).normalize();
                    yV1.mul(ForceApplicator.airPressureStrength * fraction * pressure);
                    fraction = (edgeLength*nodeFactors.get(i).get(Geometry.mod(j+1, faceDarts.size()))) / (nodeFactors.get(i).getLast());
                    Vector2d yV2 = new Vector2d(yV).normalize();
                    yV2.mul(ForceApplicator.airPressureStrength * fraction * pressure);
                    cartogram.addDisplacement(dart.getTo(), yV2);
                    cartogram.addDisplacement(dart.getFrom(), yV1);
                }else{
                    double fraction = edgeLength / circumference;
                    yV.normalize();
                    yV.mul(ForceApplicator.airPressureStrength * fraction * pressure);
                    cartogram.addDisplacement(dart.getTo(), yV);
                    cartogram.addDisplacement(dart.getFrom(), yV);
                }
                j++;
            }
            i++;
        }
    }
}
