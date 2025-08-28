package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transformer {

    public static Region transform(Graph graph) {
        List<Node> nodes = graph.getNodes().stream().toList();
        Graph cartogram = new Graph();
        Map<Edge, Node> internalEdgeToVertex= new HashMap<>();
        Map<Edge, Node> outerEdgesToVertexIn= new HashMap<>();
        Map<Edge, Node> outerEdgesToVertexOn= new HashMap<>();
        Map<Node, Node> outerNDummies= new HashMap<>();
        Map<Node, Node> outerDummies= new HashMap<>();
        if(nodes.size()<3){
            return null;
        }
        for(Node node:graph.getOuterFaceNodes()){
            Node n=cartogram.createNodeAt(node);
            outerNDummies.put(node, n);
            outerDummies.put(n, node);
        }
        for(Edge edge:graph.getEdges()){
            boolean foundDart=false;
            boolean foundOpposite=false;
            Dart edgeDart=null;
            outerloop:
            for (List<Dart> face: graph.getFaces()){
                for (Dart faceDart: face){
                    if(faceDart.getEdge().equals(edge)){
                        foundDart=true;
                        edgeDart=faceDart;
                        break outerloop;
                    }
                }
            }
            outerloop:
            for (List<Dart> face: graph.getFaces()){
                for (Dart faceDart: face){
                    if(faceDart.equals(edgeDart.getTwin())){
                        foundOpposite=true;
                        break outerloop;
                    }
                }
            }
            boolean isOuter= !foundOpposite || !foundDart;
            if (!isOuter) {
                internalEdgeToVertex.put(edge, cartogram.createNodeAt(Geometry.getEdgeMidpoint(edge)));
            }else{
                outerEdgesToVertexOn.put(edge, cartogram.createNodeAt(Geometry.getEdgeMidpoint(edge)));
            }
        }
        for(List<Dart> face: graph.getFaces()){
            Node barycenter=cartogram.createNodeAt(Geometry.barycenterDarts(face));
            for(Dart dart:face){
                Edge e=dart.getEdge();
                if (graph.getOuterFaceNodes().contains(e.getSource()) && graph.getOuterFaceNodes().contains(e.getTarget()) && outerEdgesToVertexOn.containsKey(e)) {
                    outerEdgesToVertexIn.put(e, cartogram.createNodeAt(Geometry.getMidpoint(barycenter, outerEdgesToVertexOn.get(e))));
                    cartogram.addEdge(barycenter, outerEdgesToVertexIn.get(e));
                    cartogram.addEdge(outerEdgesToVertexOn.get(e), outerEdgesToVertexIn.get(e));
                    cartogram.addEdge(outerNDummies.get(e.getSource()), outerEdgesToVertexOn.get(e));
                    cartogram.addEdge(outerNDummies.get(e.getTarget()), outerEdgesToVertexOn.get(e));
                }else{
                    cartogram.addEdge(barycenter, internalEdgeToVertex.get(e));
                }
            }
        }
        return getRegions(graph, cartogram, outerDummies);
    }

    public static Region getRegions(Graph graph, Graph cartogram, Map<Node, Node> outerNodes){
        Region region = new Region();
        region.setCartogram(cartogram);

        cartogram.determineOuterINodes();
        cartogram.determineInnerFaces();
        List<List<Dart>> faces = cartogram.getFaces();
        for(List<Dart> face:faces){
            RegionNode regionNode = new RegionNode();
            boolean found = false;
            for(Dart dart:face){
                if(outerNodes.containsKey(dart.getFrom())){
                    regionNode.setCorrespondingNode(outerNodes.get(dart.getFrom()));
                    regionNode.setBoundaryNodes(face);
                    found = true;
                    break;
                }
            }
            if(found){
                for(Dart dart:face){
                    region.getDartOfRegionNode().put(dart, regionNode);
                }
            }
            else{
                for(Node node: graph.getNodes()){
                    if(Geometry.isPointInPolygonOfDarts(node, face)){
                        regionNode.setCorrespondingNode(node);
                        regionNode.setBoundaryNodes(face);
                        for(Dart dart:face){
                            region.getDartOfRegionNode().put(dart, regionNode);
                        }
                        break;
                    }
                }
            }
            region.getRegionNodes().add(regionNode);
        }
        return region;
    }
}
