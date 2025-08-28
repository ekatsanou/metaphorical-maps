package org.example;

public class GraphUtils {

    public static double maxX;
    public static double maxY;
    public static double minX;
    public static double minY;
    public static double coord = 1.0;

    public static void fixSize(Graph graph) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Node node : graph.getNodes()) {
            if (node.getX() > maxX) maxX = node.getX();
            if (node.getY() > maxY) maxY = node.getY();
            if (node.getX() < minX) minX = node.getX();
            if (node.getY() < minY) minY = node.getY();
        }

        GraphUtils.maxX = maxX;
        GraphUtils.maxY = maxY;
        GraphUtils.minX = minX;
        GraphUtils.minY = minY;

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        double width = maxX - minX;
        double height = maxY - minY;

        double scale = 400 / Math.max(width, height);

        for (Node node : graph.getNodes()) {
            node.setX((node.getX() - centerX) * scale);
            node.setY((node.getY() - centerY) * scale);
        }

    }

}
