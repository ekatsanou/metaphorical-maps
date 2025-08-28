//package org.example;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//
//import javax.swing.*;
//import java.io.File;
//import java.util.*;
//
////TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
//// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//public class Main {
//    public static void main(String[] args) {
//        try {
//            File rootFolder = new File("src/main/resources/test_data/testing_holes/graphs_holes6");
////            AlgorithmVersion[] algorithmVersions = AlgorithmVersion.values();
//            Map<String, Map<AlgorithmVersion, Metrics>> results = new HashMap<>();
//
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.enable(SerializationFeature.INDENT_OUTPUT);
//
//            int totalGraphs = 0;
////            for (File folder : Objects.requireNonNull(rootFolder.listFiles(File::isDirectory))) {
////                for(File subfolder : Objects.requireNonNull(folder.listFiles(File::isDirectory))) {
//                    totalGraphs += Objects.requireNonNull(rootFolder.listFiles((dir, name) -> name.endsWith(".graphml"))).length;
////                }
////            }
////            totalGraphs *= algorithmVersions.length;
//
//            int progress = 0;
////            for (File folder : Objects.requireNonNull(rootFolder.listFiles(File::isDirectory))) {
////                String folderName = folder.getName();
////                for(File subfolder : Objects.requireNonNull(folder.listFiles(File::isDirectory))){
////                    String subFolderName = subfolder.getName();
////                    File[] graphFiles = rootFolder.listFiles((dir, name) -> name.endsWith(".graphml"));
////                    int folderTotal = graphFiles.length * algorithmVersions.length;
//                    int folderProgress = 0;
//
//                    System.out.println("\nProcessing folder: " +rootFolder);
//                    for (File graphFile : Objects.requireNonNull(rootFolder.listFiles((dir, name) -> name.endsWith(".graphml")))) {
//                        String graphName = graphFile.getName();
//                        Map<AlgorithmVersion, Metrics> versionMetrics = new HashMap<>();
//
//                        try {
//                            Graph graph = GraphMLParser.parseGraphML(graphFile);
//
////                            for (AlgorithmVersion version : algorithmVersions) {
//                                Metrics metrics = Algorithm.runAlgorithm(graph, AlgorithmVersion.NEW);
//                                versionMetrics.put(AlgorithmVersion.NEW, metrics);
//
//                                progress++;
//                                folderProgress++;
//                                printProgress(progress, totalGraphs);
//                                printFolderProgress(folderProgress, totalGraphs);
////                            }
//
//                            results.put(graphName, versionMetrics);
//
//                            // 🔁 Save intermediate results after each graph
//                            mapper.writeValue(new File(rootFolder.getName()+"_parameters.json"), results);
//
//                        } catch (Exception inner) {
//                            System.err.println("Failed on file " + graphFile.getName() + ": " + inner.getMessage());
//                            inner.printStackTrace();
//                        }
//                    }
////                }
////            }
//
//            // ✅ Final write to full file
//            mapper.writeValue(new File("experiment_parameters2.json"), results);
//            System.out.println("Results written to experiment_parameters.json");
//
//        } catch (Exception e) {
//            System.err.println("Experiment failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    public static void printProgress(int current, int total) {
//        int barLength = 50;
//        double percent = (double) current / total;
//        int filledLength = (int) (barLength * percent);
//
//        String bar = "\r[" +
//                "=".repeat(Math.max(0, filledLength)) +
//                " ".repeat(Math.max(0, barLength - filledLength)) +
//                "] " +
//                String.format("%.1f", percent * 100) + "%";
//
//        System.out.print(bar);
//
//        if (current == total) System.out.println(); // move to next line at end
//    }
//
//    public static void printFolderProgress(int current, int total) {
//        int barLength = 30;
//        double percent = (double) current / total;
//        int filledLength = (int) (barLength * percent);
//
//        String bar = "\r   Folder Progress: [" +
//                "=".repeat(Math.max(0, filledLength)) +
//                " ".repeat(Math.max(0, barLength - filledLength)) +
//                "] " +
//                String.format("%.1f", percent * 100) + "%";
//
//        System.out.print(bar);
//
//        if (current == total) System.out.println();
//    }
//
//}

package org.example;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        try {

            File graphMLFile = new File("src/main/resources/test_data/testing_nodes/graphs_nodes75/graph_n75_nest0_w5_14_7535.graphml");

            Graph graph = GraphMLParser.parseGraphML(graphMLFile);
            Metrics metrics = Algorithm.runAlgorithm(graph, AlgorithmVersion.NEW);

            System.out.println(metrics);


        } catch (Exception e) {
            System.err.println("Failed to parse GraphML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}