package org.example;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Region {

    private List<RegionNode> regionNodes;
    private Double totalArea;
    private Double totalWeight;
    private Graph cartogram;
    private Map<Dart, RegionNode> dartOfRegionNode;

    public Region() {
        regionNodes = new ArrayList<>();
        dartOfRegionNode = new HashMap<>();
    }

    public void updateValues(){

        for (RegionNode regionNode : regionNodes) {
            regionNode.updateValues();
        }

        double totalArea = 0;
        double totalWeight = 0;

        for(RegionNode regionNode: this.getRegionNodes()){
            if(!regionNode.getCorrespondingNode().isHole()){
                totalArea += Math.abs(regionNode.getArea());
                totalWeight += Math.abs(regionNode.getCorrespondingNode().getWeight());
            }

        }

        this.setTotalArea(totalArea);
        this.setTotalWeight(totalWeight);
    }

    public void deleteAddedNodes(Set<Node> addedNodes){
        for(int i=0; i<this.getRegionNodes().size(); i++){
            RegionNode regionNode = this.getRegionNodes().get(i);
            if(addedNodes.contains(regionNode.getCorrespondingNode())){
                this.getRegionNodes().remove(i);
                i--;
            }
        }
    }

    public List<List<Double>> distanceFactors(){
        List<List<Double>> nodeFactors = new ArrayList<>();

        double radius=Math.sqrt(Math.abs(totalArea)/Math.PI);

        for(RegionNode regionNode : regionNodes){
            List<Double> distances=new ArrayList<>();
            double sum=0;
            List<Dart> darts=regionNode.getBoundaryNodes();
            for(int i=0;i<darts.size();i++){
                double graphDist=0;
                double minDist=Integer.MAX_VALUE;
                Dart dart=darts.get(i);
                int j = Geometry.mod((i + 1), darts.size());
                double edgeLength= Geometry.getDistance(dart.getFrom(), dart.getTo());
                double prevEdgeLength= Geometry.getDistance(darts.get(Geometry.mod(i-1, darts.size())).getFrom(), darts.get(Geometry.mod(i-1, darts.size())).getTo());

                while (j != Geometry.mod(i - 1, darts.size())) {

                    Node proj=Geometry.getProjectionOnSegment(darts.get(j).getFrom(), darts.get(j).getTo(), dart.getFrom());
                    graphDist += Geometry.getGraphDistanceToPoint(darts, i, j, proj);
                    double dist = Geometry.getDistance(proj, darts.get(i).getFrom());

                    if(dist<0.9*graphDist && dist<minDist){
                        minDist=dist;
                    }

                    j = Geometry.mod((j + 1), darts.size());
                }
                double alpha=0.05*radius/minDist;
                // let alpha=0.4*average/minDist; This is the same
                double value = 1 + Math.signum(alpha - 1) * Math.log1p(Math.abs(alpha - 1));

                sum += (value*edgeLength+value*prevEdgeLength);
                distances.add(2*value);
            }
            distances.add(sum);
            nodeFactors.add(distances);
        }
        return nodeFactors;
    }

    public void performSmoothingsAndSubdivisions(AlgorithmVersion algorithmVersion) {
        double average = 0;
        for (Edge edge : cartogram.getEdges()) {
            average += Geometry.getDistance(edge);
        }
        average /= cartogram.getEdges().size();

        this.performSubdivisions(average, algorithmVersion);
        this.performSmoothings(average);


        for(RegionNode regionNode: regionNodes){
            List<Dart> darts=regionNode.getBoundaryNodes();
            List<Dart> newDarts=new ArrayList<>();
            Dart dart=darts.getFirst();
            newDarts.add(dart);
            Dart nextDart=dart.getNext();
            while(nextDart!=dart){
                newDarts.add(nextDart);
                nextDart=nextDart.getNext();
            }
            regionNode.setBoundaryNodes(newDarts);
        }
    }

    private void performSubdivisions(double average, AlgorithmVersion algorithmVersion) {
        for (RegionNode regionNode : regionNodes) {
            List<Dart> darts = regionNode.getBoundaryNodes();
            for(int i=0; i<darts.size(); i++){
                Dart dart = darts.get(i);
                Edge e=dart.getEdge();
                Node from=dart.getFrom();
                Node to=dart.getTo();
                float threshold;
                if(algorithmVersion.equals(AlgorithmVersion.MS)){
                    threshold=2f;
                }else{
                    if(regionNode.getCorrespondingNode().isHole()){
                        threshold=2f;
                    }else{
                        threshold=2f;
                    }
                }
                if (Geometry.getDistance(from, to) > threshold * average) {
                    //Graph Operations
                    cartogram.getEdges().remove(e);
                    Node n=cartogram.createNodeAt(Geometry.getMidpoint(from, to));
                    Edge fn=new Edge(from, n);
                    cartogram.getEdges().add(fn);

                    Edge nt=new Edge(n, to);
                    cartogram.getEdges().add(nt);

                    from.getNeighbors().remove(to);
                    from.getNeighbors().add(n);
                    to.getNeighbors().remove(from);
                    to.getNeighbors().add(n);
                    n.setNeighbors(new HashSet<>());
                    n.getNeighbors().add(from);
                    n.getNeighbors().add(to);

                    //Dart Operations in regionNode
                    Dart d1=new Dart(from, n);
                    Dart d11=new Dart(n, from);

                    Dart d2=new Dart(n, to);
                    Dart d22=new Dart(to, n);

                    d1.setTwin(d11);
                    d1.setNext(d2);
                    d1.setEdge(fn);

                    d11.setTwin(d1);
                    d11.setNext(dart.getTwin().getNext());
                    d11.setEdge(fn);

                    d2.setTwin(d22);
                    d2.setNext(dart.getNext());
                    d2.setEdge(nt);

                    d22.setTwin(d2);
                    d22.setNext(d11);
                    d22.setEdge(nt);

                    Dart p1 = darts.get(Geometry.mod(i-1, darts.size()));
                    p1.setNext(d1);

                    darts.set(i,d1);
                    darts.add(Geometry.mod(i+1, darts.size()+1),d2);
                    dartOfRegionNode.remove(dart);
                    dartOfRegionNode.put(d1, regionNode);
                    dartOfRegionNode.put(d2, regionNode);

                    //Dart Operations in opposite regionNode
                    Dart oppositeDart=dart.getTwin();
                    if(dartOfRegionNode.containsKey(oppositeDart)){
                        RegionNode oppositeFace=dartOfRegionNode.get(oppositeDart);
                        List<Dart> oppositeDarts=oppositeFace.getBoundaryNodes();
                        for(int j=0; j<oppositeDarts.size(); j++){
                            if (oppositeDarts.get(j) == oppositeDart){

                                Dart p2 = oppositeDarts.get(Geometry.mod(j-1, oppositeDarts.size()));
                                p2.setNext(d22);

                                oppositeDarts.set(j,d22);
                                oppositeDarts.add(Geometry.mod(j+1, oppositeDarts.size()+1),d11);
                                dartOfRegionNode.remove(oppositeDart);
                                dartOfRegionNode.put(d11, oppositeFace);
                                dartOfRegionNode.put(d22, oppositeFace);
                                break;
                            }
                        }
                    }
                    i++;

                }
            }
        }
    }

    private void performSmoothings(double average) {
        List<Node> nodes = cartogram.getNodes().stream().toList();
        for(int m=0; m<nodes.size(); m++){
            Node node=nodes.get(m);
            if(cartogram.distanceToClosestNeighbor(node) <= average/10 && node.getNeighbors().size()==2){
                List<Node> neighbors=node.getNeighbors().stream().toList();
                Node v1=neighbors.getFirst();
                Node v2=neighbors.getLast();

                List<Node> polygon = new ArrayList<>();
                polygon.add(v1);
                polygon.add(node);
                polygon.add(v2);

                boolean canSmooth=true;
                for(Node n :  cartogram.getNodes()) {
                    if(n != v1 && n != node && n != v2 && Geometry.isPointInPolygon(n, polygon)){
                        canSmooth=false;
                        break;
                    }
                }

                if(canSmooth){
                    Dart p1;
                    Dart p11 = null;
                    Dart d1;
                    Dart d11;
                    Dart d2;
                    Dart d22;
                    outerloop:
                    for(RegionNode regionNode : regionNodes){
                        List<Dart> darts=regionNode.getBoundaryNodes();
                        for(int i=0; i<darts.size();i++){
                            Dart dart=darts.get(i);
                            if(dart.getFrom().equals(v1) && dart.getTo().equals(node) || dart.getFrom().equals(v2) && dart.getTo().equals(node)){
                                Node n1=dart.getFrom();
                                Node n2=dart.getNext().getTo();
                                d1=dart;
                                d11=dart.getTwin();
                                d2=dart.getNext();
                                d22=d2.getTwin();
                                p1=darts.get(Geometry.mod(i-1, darts.size()));


                                Edge e1=d1.getEdge();
                                Edge e2=d2.getEdge();

                                //Graph Operations
                                cartogram.getEdges().remove(e1);
                                cartogram.getEdges().remove(e2);
                                cartogram.getNodes().remove(node);
                                m--;

                                n1.getNeighbors().remove(node);
                                n2.getNeighbors().remove(node);

                                Edge e=new Edge(n1, n2);
                                cartogram.getEdges().add(e);
                                n1.getNeighbors().add(n2);
                                n2.getNeighbors().add(n1);

                                Dart dNew=new Dart(n1, n2);

                                Dart dNewOpposite=new Dart(n2, n1);

                                dNew.setTwin(dNewOpposite);
                                dNew.setNext(d2.getNext());
                                dNew.setEdge(e);

                                dNewOpposite.setTwin(dNew);
                                dNewOpposite.setNext(d11.getNext());
                                dNewOpposite.setEdge(e);

                                darts.set(i, dNew);
                                darts.remove(Geometry.mod(i+1, darts.size()));


                                RegionNode oppositeRegionNode=dartOfRegionNode.get(d22);
                                if(oppositeRegionNode!=null){
                                    List<Dart> oppositeDarts=oppositeRegionNode.getBoundaryNodes();
                                    for(int j=0; j<oppositeDarts.size(); j++){
                                        Dart oppositeDart=oppositeDarts.get(j);
                                        if (oppositeDart.equals(d22)){
                                            p11=oppositeDarts.get(Geometry.mod(j-1, oppositeRegionNode.getBoundaryNodes().size()));
                                            p11.setNext(dNewOpposite);
                                            oppositeDarts.set(j, dNewOpposite);
                                            oppositeDarts.remove(Geometry.mod(j+1, oppositeRegionNode.getBoundaryNodes().size()));
                                            dartOfRegionNode.put(dNewOpposite, oppositeRegionNode);
                                            break;
                                        }
                                    }
                                }

                                p1.setNext(dNew);

                                dartOfRegionNode.remove(d1);
                                dartOfRegionNode.remove(d2);
                                dartOfRegionNode.remove(d22);
                                dartOfRegionNode.remove(d11);
                                dartOfRegionNode.put(dNew, regionNode);

                                break outerloop;
                            }
                        }


                    }
                }

            }
        }
    }

}