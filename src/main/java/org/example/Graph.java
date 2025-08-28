package org.example;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.joml.Vector2d;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
@Setter
public class Graph {

    public static final Color[] colors=new Color[]{new Color(244, 162, 97), new Color(138, 177, 125),new Color(38, 70, 83),
            new Color(231, 111, 81), new Color(233, 196, 106), new Color(42, 157, 140)};


    private Set<Node> nodes = new HashSet<>();
    private Set<Edge> edges = new HashSet<>();
    private List<Node> outerFaceNodes = new LinkedList<>();
    private List<Dart> outerface = new LinkedList<>();
    private List<List<Dart>> faces = new LinkedList<>();
    private Map<Node, List<Dart>> outgoingDarts;
    private Map<Node, Vector2d> displacements;
    private Set<Node> addedNodes = new HashSet<>();

    public Node createNodeAt(double x, double y) {
        Node node = new Node(x, y);
        nodes.add(node);
        return node;
    }

    public Node createNodeAt(Node p) {
        return createNodeAt(p.getX(), p.getY());
    }

    public Edge addEdge(Node n1, Node n2) {
        n1.getNeighbors().add(n2);
        n2.getNeighbors().add(n1);
        Edge edge = new Edge(n1, n2);
        edges.add(edge);
        return edge;
    }

    public void removeEdge(Node n1, Node n2) {
        for (Edge edge : edges) {
            if(edge.getSource().equals(n1) && edge.getTarget().equals(n2) || edge.getTarget().equals(n1) && edge.getSource().equals(n2)) {
                this.removeEdge(edge);
                return;
            }
        }
    }

    public void removeEdge(Edge edge) {
        edges.remove(edge);
        edge.getSource().getNeighbors().remove(edge.getTarget());
        edge.getTarget().getNeighbors().remove(edge.getSource());
    }

    public void removeNode(Node node) {
        nodes.remove(node);
        for (Node n : node.getNeighbors()) {
            n.getNeighbors().remove(node);
        }
    }


    public void addDisplacement(Node n1, Vector2d vector) {
        Vector2d displacement = displacements.get(n1);
        if(displacement == null) {
            displacements.put(n1, vector);
        }else{
            displacements.put(n1, displacement.add(vector));
        }
    }

    public double distanceToClosestNeighbor(Node n1) {
        double distance = Double.MAX_VALUE;
        for(Node n2 : n1.getNeighbors()) {
            if(Geometry.getDistance(n1, n2) < distance) {
                distance = Geometry.getDistance(n1, n2);
            }
        }
        return distance;
    }

    public void determineOuterINodes() {
        List<Node> outer = new ArrayList<>();
        Node topMost = Geometry.findTopmostINode(this.getNodes().stream().toList());
        if (topMost == null) return;

        outer.add(topMost);
        Node prev = topMost;
        Node current = Geometry.findLeftmostNeighbor(topMost);
        if (current == null) return;

        outer.add(current);

        while (!current.equals(topMost)) {
            Node next = null;
            double maxAngle = Double.MIN_VALUE;
            for (Node neighbor : current.getNeighbors()) {
                if (neighbor.equals(prev)) continue;
                double angle = Geometry.angleBetween(prev, current, neighbor);
                if (angle > maxAngle) {
                    maxAngle = angle;
                    next = neighbor;
                }
            }
            if (next == null) break;
            outer.add(next);
            prev = current;
            current = next;
        }

        if (!outer.isEmpty() && outer.getLast().equals(topMost)) {
            outer.removeLast();
        }
        this.outerFaceNodes = outer;
    }

    public List<List<Dart>> determineInnerFaces() {

        List<Dart> halfEdges = new ArrayList<>();

        Map<String, Dart> edgeMap = new HashMap<>();
        for (Edge e : this.getEdges()) {
            Dart e1 = new Dart(e.getSource(), e.getTarget());
            e1.setEdge(e);
            Dart e2 = new Dart(e.getTarget(), e.getSource());
            e2.setEdge(e);
            e1.setTwin(e2);
            e2.setTwin(e1);
            halfEdges.add(e1);
            halfEdges.add(e2);

            edgeMap.put(e1.getFrom() + "-" + e1.getTo(), e1);
            edgeMap.put(e2.getFrom() + "-" + e2.getTo(), e2);
        }

        Map<Node, List<Dart>> outgoing = new HashMap<>();

        for (Dart he : halfEdges) {
            outgoing.computeIfAbsent(he.getFrom(), k -> new ArrayList<>()).add(he);
        }

        for (Map.Entry<Node, List<Dart>> entry : outgoing.entrySet()) {
            Node center = entry.getKey();
            entry.getValue().sort(Comparator.comparingDouble(he -> {
                double dx = he.getTo().getX() - center.getX();
                double dy = he.getTo().getY() - center.getY();
                return Math.atan2(dy, dx);
            }));
        }

        for (List<Dart> list : outgoing.values()) {
            int n = list.size();
            for (int i = 0; i < n; i++) {
                Dart curr = list.get(i);
                Dart next = list.get(Geometry.mod(i + 1, n));
                curr.getTwin().setNext(next);
            }
        }

        this.outgoingDarts = outgoing;

        List<List<Dart>> faces = new ArrayList<>();

        for (Dart he : halfEdges) {
            if (he.isVisited()) continue;

            List<Dart> face = new ArrayList<>();
            Dart walker = he;

            do {
                walker.setVisited(true);
                face.add(walker);
                walker = walker.getNext();
            } while (walker != he);

            faces.add(face);
        }

        List<Dart> outer = null;
        double maxArea = -1;

        for (List<Dart> face : faces) {
            List<Node> faceNodes = new ArrayList<>();
            for (Dart d : face) {
                faceNodes.add(d.getFrom());
            }
            double area = Geometry.calculateArea(faceNodes);
            if (area > maxArea) {
                maxArea = area;
                outer = face;
            }
        }

        faces.remove(outer);
        this.outerface = outer;
        this.faces = faces;
        return faces;
    }

    public void triangulate() {

        double barycenterX = 0;
        double barycenterY = 0;

        double rad = 1000;
        int k = 0;
        for (Node node : outerFaceNodes) {
            node.setX(rad * Math.cos(2 * k * Math.PI / outerFaceNodes.size()));
            node.setY(rad * Math.sin(2 * k * Math.PI / outerFaceNodes.size()));
            k++;
        }

        for (Node node : outerFaceNodes) {
            barycenterX = barycenterX + node.getX();
            barycenterY = barycenterY + node.getY();
        }
        barycenterX = barycenterX / outerFaceNodes.size();
        barycenterY = barycenterY / outerFaceNodes.size();

        for (int i = 0; i < faces.size(); i++) {
            //console.log(faces[i].size)
            if (faces.get(i).size() > 3) {
                List<Dart> face = faces.get(i);

                Node n = this.createNodeAt(barycenterX - i, barycenterY - i);
                double num = 0;
                for (int j = 0; j < face.size(); j++) {
                    Edge e = this.addEdge(n, face.get(j).getFrom());
                    num = num + Math.sqrt(face.get(j).getFrom().getWeight());
                }
                num = (double) 1 / (4 * Math.pow(face.size(), 1)) * Math.pow(num, 2);

                n.setWeight(num);
                n.setHole(true);
                addedNodes.add(n);
            }
        }
    }

    private void putOuterfaceInCycle(){
        double rad = 3000;
        for(int i = 0; i < outerFaceNodes.size(); i++){
            Node n=outerFaceNodes.get(i);
            n.setX(rad * Math.cos(2 * i * Math.PI / outerFaceNodes.size()));
            n.setY(rad * Math.sin(2 * i * Math.PI / outerFaceNodes.size()));
        }
    }

    private Object[] calcCoords(){
        this.putOuterfaceInCycle();

        List<Node> innerNodes = new ArrayList<>();

        for(Node n : nodes){
            if(!outerFaceNodes.contains(n)){
                innerNodes.add(n);
            }
        }

        double[][] matrix=new double[innerNodes.size()][innerNodes.size()];
        for(int i = 0; i < innerNodes.size(); i++){
            List<Node> neig = innerNodes.get(i).getNeighbors().stream().toList();
            for(int j = 0; j < innerNodes.size(); j++ ){
                if(neig.contains(innerNodes.get(j))){
                    matrix[i][j] = (double) -1 / neig.size();
                }
                else if(innerNodes.get(i)== innerNodes.get(j))
                {matrix[i][j] = 1;}
                else{matrix[i][j] = 0;}
            }
        }

        double[] constX = new double[innerNodes.size()];
        double[] constY = new double[innerNodes.size()];
        for(int i =0; i < innerNodes.size(); i++){
            List<Node> neig = innerNodes.get(i).getNeighbors().stream().toList();
            constX[i] = 0;
            constY[i] = 0;
            for(int j = 0; j < outerFaceNodes.size(); j++){
                if(neig.contains(outerFaceNodes.get(j))){
                    constX[i] = constX[i] + outerFaceNodes.get(j).getX();
                    constY[i] = constY[i] + outerFaceNodes.get(j).getY();
                }
            }
            constX[i] = constX[i] / neig.size();
            constY[i] = constY[i] / neig.size();
        }

        return new Object[]{constX, constY, matrix, innerNodes};
    }

    public void getPlanar(){
        Object[] arr = calcCoords();
        double[] xC = (double[]) arr[0];
        double[] yC = (double[]) arr[1];
        double[][] matrix = (double[][]) arr[2];
        List<Node> innerNodes = (List<Node>) arr[3];

        double[][] invmat = null;
        if(matrix.length >= 1){
            RealMatrix mat = MatrixUtils.createRealMatrix(matrix);
            RealMatrix inverse = new LUDecomposition(mat).getSolver().getInverse();
            invmat = inverse.getData();
        }

        double[][] finalX;
        double[][] finalY;

        if(xC.length <= 1){
            finalX = new double[1][1];
            finalY = new double[1][1];
            if(xC.length == 1){
                finalX[0][0] = xC[0];
                finalY[0][0] = yC[0];
            }
        }
        else{
            double[][] xcTr = new double[xC.length][1];
            double[][] ycTr = new double[yC.length][1];
            for (int i = 0; i < xC.length; i++) {
                xcTr[i][0] = xC[i];
                ycTr[i][0] = yC[i];
            }
            finalX = Geometry.multiply(invmat, xcTr);
            finalY = Geometry.multiply(invmat, ycTr);
        }

        if(innerNodes.size() == 1){
            innerNodes.getFirst().setX(finalX[0][0]);
            innerNodes.getFirst().setY(finalY[0][0]);
        }
        else{
            for(int i = 0; i < innerNodes.size(); i++){
                innerNodes.get(i).setX(finalX[i][0]);
                innerNodes.get(i).setY(finalY[i][0]);
            }
        }

    }

    public void deleteAddedNodes(){
        for(Node n : addedNodes){
            this.getNodes().remove(n);
        }
    }


    public void color() {
        int maxColors = 6;

        for (Node node : nodes) {
            boolean[] used = new boolean[maxColors];

            for (Node neighbor : node.getNeighbors()) {
                if (neighbor.getColor() != -1 && neighbor.getColor() < maxColors) {
                    used[neighbor.getColor()] = true;
                }
            }

            for (int c = 0; c < maxColors; c++) {
                if (!used[c]) {
                    node.setColor(c);
                    break;
                }
            }
        }
    }

}
