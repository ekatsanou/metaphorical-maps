package org.example;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Algorithm {

    public static Metrics runAlgorithm(Graph graph, AlgorithmVersion version) {
//        long start = System.nanoTime();
        graph.determineOuterINodes();
        graph.determineInnerFaces();

        if (!Geometry.isTriangulated(graph)) {
            graph.triangulate();
            graph.getPlanar();
            graph.determineOuterINodes();
            graph.determineInnerFaces();
        }

        graph.color();
        GraphUtils.fixSize(graph);

        Region region = Transformer.transform(graph);
        Graph cartogram = region.getCartogram();

        JFrame frame = new JFrame("Dynamic Graph Viewer");
        GraphDrawer drawer = new GraphDrawer();
        drawer.setGraph(graph);
        drawer.setCartogram(region);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 1000);
        frame.add(drawer);
        frame.setVisible(true);

        region.updateValues();

        Evaluation evaluation = new Evaluation(region);
        int iterations = graph.getNodes().size() * 10 + 800;
        for (int i = 1; i <= iterations; i++) {
            cartogram.setDisplacements(new HashMap<>());

            region.performSmoothingsAndSubdivisions(version);

            cartogram.determineOuterINodes();
            cartogram.determineInnerFaces();

            ForceApplicator.improvedAngularResolution(region);
            ForceApplicator.vertexVertexRepulsion(region);
            ForceApplicator.vertexEdgeRepulsion(region);
            ForceApplicator.airPressureOuter(region);

            if (version.equals(AlgorithmVersion.MS)) {
                List<List<Double>> nodeFactors = new ArrayList<>();
                ForceApplicator.airPressure(region, false, nodeFactors);
            } else {
                List<List<Double>> nodeFactors = region.distanceFactors();
                ForceApplicator.airPressure(region, true, nodeFactors);
            }

            ImPrEdMovement.impredSanitize(region);

            for (Node node : cartogram.getNodes()) {
                node.displace(cartogram.getDisplacements().get(node));
            }

            drawer.setGraph(graph);
            drawer.setCartogram(region);

            region.updateValues();

        }
        evaluation.metrics();
//        long finish = System.nanoTime();
        return getMetrics(graph, evaluation, cartogram, 0);
    }

    private static Metrics getMetrics(Graph graph, Evaluation evaluation, Graph cartogram, long time) {
        Metrics metrics=new Metrics();
        metrics.setAveragePolygonComplexity(evaluation.getAveragePolygonComplexity());
        metrics.setMaxPolygonComplexity(evaluation.getMaxPolygonComplexity());
        metrics.setAverageCartographicError(evaluation.getAverageCartographicError());
        metrics.setMaxCartographicError(evaluation.getMaxCartographicError());
        metrics.setNodes(graph.getNodes().size());
        metrics.setEdges(graph.getEdges().size());
        metrics.setCartogramNodes(cartogram.getNodes().size());
        metrics.setHoles(evaluation.getHoles());
        metrics.setNonHoles(evaluation.getNonHoles());
        metrics.setCartogramEdges(cartogram.getEdges().size());
        metrics.setAverageEdgeLength(evaluation.getAverageEdgeLength());
        metrics.setRunningTime(time);
        return metrics;
    }
}
