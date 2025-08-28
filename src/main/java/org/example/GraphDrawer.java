package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GraphDrawer extends JPanel {

    private Graph graph;
    private Region cartogram;

    public void setGraph(Graph graph) {
        this.graph = graph;
        repaint();
    }

    public void setCartogram(Region region) {
        this.cartogram = region;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        List<Node> allNodes = new java.util.ArrayList<>();
        allNodes.addAll(graph.getNodes());
        allNodes.addAll(cartogram.getCartogram().getNodes());

        if (allNodes.isEmpty()) return;

        double minX = allNodes.stream().mapToDouble(Node::getX).min().orElse(0);
        double maxX = allNodes.stream().mapToDouble(Node::getX).max().orElse(1);
        double minY = allNodes.stream().mapToDouble(Node::getY).min().orElse(0);
        double maxY = allNodes.stream().mapToDouble(Node::getY).max().orElse(1);

        double graphWidth = maxX - minX;
        double graphHeight = maxY - minY;

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        double scaleX = panelWidth / graphWidth * 0.9;  // Add padding
        double scaleY = panelHeight / graphHeight * 0.9;
        double scale = Math.min(scaleX, scaleY);  // Preserve aspect ratio

        double scaledGraphWidth = graphWidth * scale;
        double scaledGraphHeight = graphHeight * scale;
        double offsetX = (panelWidth - scaledGraphWidth) / 2.0;
        double offsetY = (panelHeight - scaledGraphHeight) / 2.0;

        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);
        g2.translate(-minX, -minY);

        drawGraph(g2, graph.getNodes().stream().toList(), graph.getEdges().stream().toList(), Color.RED);
        drawRegion(g2, cartogram);
    }

    private void drawGraph(Graphics g, List<Node> nodes, List<Edge> edges, Color color) {
        g.setColor(color);

        for (Edge edge : edges) {
            Node from = edge.getSource();
            Node to = edge.getTarget();
            g.drawLine((int) from.getX(), (int) from.getY(), (int) to.getX(), (int) to.getY());
        }

        int nodeRadius = 1;
        for (Node node : nodes) {
            if (node.isHole()){
                g.setColor(Color.GREEN);
            }else{
                g.setColor(color);
            }
            int x = (int) node.getX();
            int y = (int) node.getY();
            g.fillOval(x - nodeRadius, y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
        }
    }

    private void drawRegion(Graphics g, Region region) {
        Graphics2D g2 = (Graphics2D) g;

        for (RegionNode regionNode : region.getRegionNodes()) {
            Color regionColor;
            if (regionNode.getCorrespondingNode().isHole()) {
                regionColor = new Color(255, 255, 255);
            } else {
                int c=regionNode.getCorrespondingNode().getColor();
                regionColor = Graph.colors[c];
            }
            g2.setColor(regionColor);

            List<Dart> boundary = regionNode.getBoundaryNodes();
            if (boundary.isEmpty()) continue;

            Polygon poly = new Polygon();
            for (Dart dart : boundary) {
                Node node = dart.getFrom();
                poly.addPoint((int) node.getX(), (int) node.getY());
            }

            g2.fillPolygon(poly);

            g2.setColor(new Color(regionColor.getRed(), regionColor.getGreen(), regionColor.getBlue(), 200)); // Less transparent border
            g2.drawPolygon(poly);
        }


        drawGraph(g, region.getCartogram().getNodes().stream().toList(), region.getCartogram().getEdges().stream().toList(), Color.BLACK);
    }

}
