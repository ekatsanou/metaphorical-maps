package org.example;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.ConvexHull2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.MonotoneChain;
import org.joml.Vector2d;

import java.awt.geom.Path2D;
import java.util.*;

public class Geometry {

    public static Node barycenter(List<Node> nodes) {
        double sumX = 0, sumY = 0;
        for (Node n : nodes) {
            sumX += n.getX();
            sumY += n.getY();
        }
        int count = nodes.size();
        return new Node(sumX / count, sumY / count);
    }

    public static Node barycenterDarts(List<Dart> darts) {
        List<Node> nodes = new ArrayList<>();
        for (Dart d : darts) {
            nodes.add(d.getFrom());
            nodes.add(d.getTo());
        }
        return barycenter(nodes);
    }

    public static double calculateArea(List<Node> pathNodes) {
        int numPoints = pathNodes.size();
        double[] X = new double[numPoints];
        double[] Y = new double[numPoints];

        // Extract x and y coordinates
        for (int i = 0; i < numPoints; i++) {
            X[i] = pathNodes.get(i).getX();
            Y[i] = pathNodes.get(i).getY();
        }

        // Shoelace formula
        double area = 0.0;
        int j = numPoints - 1;

        for (int i = 0; i < numPoints; i++) {
            area += (X[j] + X[i]) * (Y[j] - Y[i]);
            j = i;
        }

        return Math.abs(area / 2.0);
    }

    public static double calculateAreaDarts(List<Dart> darts) {
        List<Node> nodes = new ArrayList<>();
        for (Dart d : darts) {
            nodes.add(d.getFrom());
            nodes.add(d.getTo());
        }
        return calculateArea(nodes);
    }

    public static double angleBetween(Node a, Node b, Node c) {
        Vector2d A = new Vector2d(a.getX(), a.getY());
        Vector2d B = new Vector2d(b.getX(), b.getY());
        Vector2d C = new Vector2d(c.getX(), c.getY());

        Vector2d ba = new Vector2d(A).sub(B);
        Vector2d bc = new Vector2d(C).sub(B);

        double angle =  Math.atan2(bc.y, bc.x) - Math.atan2(ba.y, ba.x);

        if (angle < 0) {
            angle += (2 * Math.PI);
        }

        return angle;
    }

    public static double clockwiseAngle(Vector2d v1, Vector2d v2) {
        double angle1 = Math.atan2(v1.y, v1.x);
        double angle2 = Math.atan2(v2.y, v2.x);
        double angle = angle2 - angle1;
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    public static Vector2d rotate(Vector2d v, double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        double x = v.x * cos - v.y * sin;
        double y = v.x * sin + v.y * cos;
        return new Vector2d(x, y);
    }

    public static Vector2d getVector(Node to, Node from) {
        return new Vector2d(to.getX()-from.getX(), to.getY()-from.getY());
    }

    public static Node getProjectionOnLine(Node node, Node anchor, Vector2d direction) {
        // Vector from anchor to node
        Vector2d v = new Vector2d(node.getX() - anchor.getX(), node.getY() - anchor.getY());

        // Compute projection scalar t
        double t = v.dot(direction) / direction.lengthSquared();

        // Compute the projection point
        double projX = anchor.getX() + t * direction.x;
        double projY = anchor.getY() + t * direction.y;

        return new Node(projX, projY);
    }

    public static Node getProjectionOnSegment(Node start, Node end, Node point) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        double dx1 = point.getX() - start.getX();
        double dy1 = point.getY() - start.getY();

        double segmentLengthSquared = dx * dx + dy * dy;

        if (segmentLengthSquared == 0) {
            return new Node(start.getX(), start.getY());
        }

        double t = (dx1 * dx + dy1 * dy) / segmentLengthSquared;

        if (t < 0) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }

        double projX = start.getX() + t * dx;
        double projY = start.getY() + t * dy;

        return new Node(projX, projY);
    }

    public static double getGraphDistanceToPoint(List<Dart> darts, int start, int end, Node projection) {
        int n = darts.size();

        double distanceCW = 0.0;
        int i = start;
        while (i != end) {
            Dart d = darts.get(i);
            distanceCW += Geometry.getDistance(d.getFrom(), d.getTo());
            i = Geometry.mod(i + 1, n);
        }
        Dart dEnd = darts.get(end);
        distanceCW += Geometry.getDistance(dEnd.getFrom(), projection);

        double distanceCCW = 0.0;
        i = start;
        while (i != Geometry.mod(end+1, n)) {
            int prev = Geometry.mod(i - 1, n);
            Dart d = darts.get(prev);
            distanceCCW += Geometry.getDistance(d.getFrom(), d.getTo());
            i = prev;
        }
        dEnd = darts.get(end);
        distanceCCW += Geometry.getDistance(dEnd.getTo(), projection);

        return Math.min(distanceCW, distanceCCW);
    }

    public static boolean isTriangulated(Graph graph){
        List<List<Dart>> faces=graph.determineInnerFaces();
        for(List<Dart> face:faces){
            if (face.size()!=3){
                return false;
            }
        }
        return true;
    }

    public static boolean isPlanar(Graph graph){
        List<Edge> edges = graph.getEdges().stream().toList();

        for (int i = 0; i < edges.size(); i++) {
            for (int j = i + 1; j < edges.size(); j++) {
                Edge e1 = edges.get(i);
                Edge e2 = edges.get(j);

                Node a = e1.getSource();
                Node b = e1.getTarget();
                Node c = e2.getSource();
                Node d = e2.getTarget();

                if (doSegmentsIntersect(a, b, c, d)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static double smallestEnclosingCircle(List<Dart> darts) {
        double smallestRadius = 100000;

        for(int i = 0; i < darts.size(); i++){
            for(int j = 0; j < i; j++){
                boolean isEncl = true;
                Node p1 = darts.get(i).getFrom();
                Node p2 = darts.get(j).getFrom();
                Node center = new Node(p1.getX()+0.5*p2.getX(), p1.getY()+0.5*p2.getY());
                double radius = Geometry.getDistance(p1, p2)/2;
                for (Dart dart : darts) {
                    if (Geometry.getDistance(center, dart.getFrom()) > radius + 0.001) {
                        isEncl = false;
                        break;
                    }
                }
                if(isEncl){
                    if(radius < smallestRadius){
                        smallestRadius = radius;
                    }
                }
                for(int k = 0; k < j; k++){
                    boolean isEnclosing = true;
                    p1 = darts.get(i).getFrom();
                    p2 = darts.get(j).getFrom();
                    Node p3 = darts.get(k).getFrom();
                    Vector2d ab = Geometry.getVector(p1, p2);
                    Vector2d ac = Geometry.getVector(p1, p3);
                    Vector2d bc = Geometry.getVector(p2, p3);
                    double angA = Geometry.mod((Geometry.clockwiseAngle(ab, ac)), Math.PI);
                    double angB = Geometry.mod((Geometry.clockwiseAngle(bc, Geometry.rotate(ab, Math.PI))), Math.PI);
                    double angC = Geometry.mod((Geometry.clockwiseAngle(ac,bc)), Math.PI);
                    double sin2a = Math.sin(2*angA);
                    double sin2b = Math.sin(2*angB);
                    double sin2c = Math.sin(2*angC);
                    double denominator = sin2a + sin2b + sin2c;
                    center = new Node((p1.getX()*sin2a + p2.getX()*sin2b + p3.getX()*sin2c)/denominator, (p1.getY()*sin2a + p2.getY()*sin2b + p3.getY()*sin2c)/denominator);
                    radius =  Geometry.getDistance(p1, center);
                    for (Dart dart : darts) {
                        if (Geometry.getDistance(center, dart.getFrom()) > radius + 0.001) {
                            isEnclosing = false;
                            break;
                        }
                    }
                    if(isEnclosing){
                        if(radius < smallestRadius){
                            smallestRadius = radius;
                        }
                    }
                }
            }
        }
        return Math.PI*Math.pow(smallestRadius, 2);
    }

    public static boolean doSegmentsIntersect(Node a, Node b, Node c, Node d) {
        if (a == c || a == d || b == c || b == d) return false;

        return ccw(a, c, d) != ccw(b, c, d) && ccw(a, b, c) != ccw(a, b, d);
    }

    public static boolean ccw(Node a, Node b, Node c) {
        return (c.getY() - a.getY()) * (b.getX() - a.getX()) >
                (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    public static boolean isClockwise(List<Dart> polygon) {
        double sum = 0.0;
        for (Dart dart : polygon) {
            Node p1 = dart.getFrom();
            Node p2 = dart.getTo();
            sum += (p2.getX() - p1.getX()) * (p2.getY() + p1.getY());
        }
        return sum > 0;
    }

    public static boolean isPointInPolygonOfDarts(Node node, List<Dart> polygon) {
        Path2D path = new Path2D.Double();
        if (polygon.isEmpty()) return false;

        Node first = polygon.getFirst().getFrom();
        path.moveTo(first.getX(), first.getY());

        for (int i = 1; i < polygon.size(); i++) {
            Node n = polygon.get(i).getFrom();
            path.lineTo(n.getX(), n.getY());
        }
        path.closePath();

        return path.contains(node.getX(), node.getY());
    }

    public static boolean isPointInPolygon(Node node, List<Node> polygon) {
        Path2D path = new Path2D.Double();
        if (polygon.isEmpty()) return false;

        Node first = polygon.getFirst();
        path.moveTo(first.getX(), first.getY());

        for (int i = 1; i < polygon.size(); i++) {
            Node n = polygon.get(i);
            path.lineTo(n.getX(), n.getY());
        }
        path.closePath();

        return path.contains(node.getX(), node.getY());
    }

    public static double[] convexHullAreaAndCircumference(List<Dart> polygon) {
        Set<Vector2D> inputPoints = new HashSet<>();
        for (Dart dart : polygon) {
            Node node = dart.getTo(); // or getFrom(), just be consistent
            inputPoints.add(new Vector2D(node.getX(), node.getY()));
        }
        MonotoneChain hullGenerator = new MonotoneChain();
        ConvexHull2D hull = hullGenerator.generate(inputPoints);

        Vector2D[] hullVertices = hull.getVertices();

        double area = 0.0;
        double circumference = 0.0;
        for (int i = 0; i < hullVertices.length; i++) {
            Vector2D p1 = hullVertices[i];
            Vector2D p2 = hullVertices[(i + 1) % hullVertices.length];
            circumference += Geometry.getDistance(new Node(p1.getX(), p1.getY()), new Node(p2.getX(), p2.getY()));
            area += (p1.getX() * p2.getY() - p2.getX() * p1.getY());
        }
        return new double[] {Math.abs(area) / 2.0, circumference};
    }

    public static Node getEdgeMidpoint(Edge edge) {
        double midX = (edge.getSource().getX() + edge.getTarget().getX()) / 2.0;
        double midY = (edge.getSource().getY() + edge.getTarget().getY()) / 2.0;
        return new Node(midX, midY);
    }

    public static Node getMidpoint(Node n1, Node n2) {
        double midX = (n1.getX() + n2.getX()) / 2.0;
        double midY = (n1.getY() + n2.getY()) / 2.0;
        return new Node(midX, midY);
    }

    public static double getDistance(Node n1, Node n2) {
        double dx = n2.getX() - n1.getX();
        double dy = n2.getY() - n1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double getDistance(Edge e) {
        return getDistance(e.getSource(), e.getTarget());
    }

    public static int mod(int a, int b) {
        return ((a % b) + b) % b;
    }

    public static double mod(double a, double b) {
        return ((a % b) + b) % b;
    }

    public static double[][] multiply(double[][] a, double[][] b) {
        int aNumRows = a.length;
        int aNumCols = a[0].length;
        int bNumRows = b.length;
        int bNumCols = b[0].length;

        if (aNumCols != bNumRows) {
            throw new IllegalArgumentException("Number of columns of matrix A must equal number of rows of matrix B.");
        }

        double[][] result = new double[aNumRows][bNumCols];

        for (int r = 0; r < aNumRows; r++) {
            for (int c = 0; c < bNumCols; c++) {
                result[r][c] = 0;
                for (int i = 0; i < aNumCols; i++) {
                    result[r][c] += a[r][i] * b[i][c];
                }
            }
        }

        return result;
    }


    static Node findLeftmostNeighbor(Node node) {
        double minAngle = 200;
        Node leftmost = null;
        for (Node neighbor : node.getNeighbors()) {
            double dy=node.getY()-neighbor.getY();
            double dx = neighbor.getX()-node.getX();
            double angle = mod(Math.atan(dy/dx), Math.PI);
            if (angle < minAngle) {
                minAngle = angle;
                leftmost = neighbor;
            }
        }

        return leftmost;
    }

    public static Node findTopmostINode(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return null;

        Node topmost = nodes.getFirst();
        for (Node node : nodes) {
            double y = node.getY();
            double x = node.getX();
            double bestY = topmost.getY();
            double bestX = topmost.getX();

            if (y < bestY || (y == bestY && x > bestX)) {
                topmost = node;
            }
        }

        return topmost;
    }
}
