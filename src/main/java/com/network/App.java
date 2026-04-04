package com.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main command line application for Network Reliability Analysis.
 */
public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String filePath = null;
        int V = 0, m = 0;
        String visualPath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--file") && i + 1 < args.length) {
                filePath = args[++i];
            } else if (args[i].equals("--generate") && i + 2 < args.length) {
                V = Integer.parseInt(args[++i]);
                m = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--visualize") && i + 1 < args.length) {
                visualPath = args[++i];
            }
        }

        if (filePath == null && (V <= 0 || m <= 0)) {
             printUsage();
             return;
        }

        Graph graph = null;

        try {
            if (filePath != null) {
                System.out.println("Loading graph from CSV: " + filePath);
                graph = loadFromCsv(filePath);
            } else {
                System.out.printf("Generating Scale-Free network with %d nodes and m=%d...%n", V, m);
                long genStart = System.currentTimeMillis();
                graph = NetworkSimulator.generateScaleFreeGraph(V, m);
                System.out.printf("Graph generated in %d ms.%n", (System.currentTimeMillis() - genStart));
            }
        } catch (Exception e) {
            System.err.println("Error initializing graph: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.printf("Graph loaded successfully. Nodes: %d, Edges: %d%n", graph.getV(), graph.getE());
        System.out.println("Starting High-Performance Reliability Analysis...");

        // Pre-analysis memory footprint
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ReliabilityAnalyzer analyzer = new ReliabilityAnalyzer(graph);

        long startTime = System.nanoTime();
        ReliabilityAnalyzer.AnalysisResult result = analyzer.analyze();
        long endTime = System.nanoTime();

        // Post-analysis memory check
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memUsed = Math.max(0, memAfter - memBefore);

        System.out.printf("Analysis Complete in %.3f ms.%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("Memory allocated during analysis approx: %.2f MB%n", memUsed / (1024.0 * 1024.0));
        System.out.printf("Found %d Articulation Points.%n", result.articulationPoints.size());

        System.out.println("\nTop 10 Most Critical Nodes (by Impact Factor):");
        System.out.printf("%-20s | %-25s | %-25s%n", "Node ID", "Disconnected Components", "Largest Remaining Component");
        System.out.println("-".repeat(75));

        int limit = Math.min(10, result.articulationPoints.size());
        for (int i = 0; i < limit; i++) {
            ReliabilityAnalyzer.NodeImpact impact = result.articulationPoints.get(i);
            System.out.printf("%-20s | %-25d | %-25d%n", 
                impact.nodeIdentifier, impact.disconnectedComponents, impact.largestComponentSize);
        }

        if (visualPath != null) {
            if (graph.getV() > 2000) {
                System.err.println("\nWarning: Graph size (" + graph.getV() + " nodes) exceeds visualization safety limit (2000). Visualization export skipped.");
            } else {
                GraphVisualizer.exportToHtml(graph, result, visualPath);
            }
        }
    }

    private static Graph loadFromCsv(String filePath) throws IOException {
        Graph graph = new Graph();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            long lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue; // Skip empties and comments
                }
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    graph.addEdge(parts[0].trim(), parts[1].trim());
                } else {
                    System.err.println("Warning: Invalid line format at line " + lineCount + ": " + line);
                }
            }
        }
        return graph;
    }

    private static void printUsage() {
        System.out.println("Network Reliability Analyzer");
        System.out.println("Usage:");
        System.out.println("  java -cp . com.network.App --file <path_to_csv> [--visualize <output.html>]");
        System.out.println("  java -cp . com.network.App --generate <V> <m> [--visualize <output.html>]");
        System.out.println("      <V> = Number of nodes (e.g. 1000000)");
        System.out.println("      <m> = Number of edges per new node (e.g. 3)");
    }
}
