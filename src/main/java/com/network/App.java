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
        String visualPath = null;
        int v = 0;
        int m = 0;
        boolean generateRequested = false;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--file":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value after --file.");
                        }
                        filePath = args[++i];
                        break;
                    case "--generate":
                        if (i + 2 >= args.length) {
                            throw new IllegalArgumentException("--generate requires <V> and <m>.");
                        }
                        v = Integer.parseInt(args[++i]);
                        m = Integer.parseInt(args[++i]);
                        generateRequested = true;
                        break;
                    case "--visualize":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value after --visualize.");
                        }
                        visualPath = args[++i];
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: --generate expects integer values for <V> and <m>.");
            printUsage();
            return;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            return;
        }

        if ((filePath == null && !generateRequested) || (filePath != null && generateRequested)) {
            System.err.println("Error: choose exactly one input mode: --file or --generate.");
            printUsage();
            return;
        }

        Graph graph;
        try {
            if (filePath != null) {
                System.out.println("Loading graph from CSV: " + filePath);
                graph = loadFromCsv(filePath);
            } else {
                System.out.printf("Generating Scale-Free network with %d nodes and m=%d...%n", v, m);
                long genStart = System.currentTimeMillis();
                graph = NetworkSimulator.generateScaleFreeGraph(v, m);
                System.out.printf("Graph generated in %d ms.%n", (System.currentTimeMillis() - genStart));
            }
        } catch (Exception e) {
            System.err.println("Error initializing graph: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.printf("Graph loaded successfully. Nodes: %d, Edges: %d%n", graph.getV(), graph.getE());
        System.out.println("Starting High-Performance Reliability Analysis...");

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ReliabilityAnalyzer analyzer = new ReliabilityAnalyzer(graph);
        long startTime = System.nanoTime();
        ReliabilityAnalyzer.AnalysisResult result = analyzer.analyze();
        long endTime = System.nanoTime();

        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memUsed = Math.max(0, memAfter - memBefore);

        System.out.printf("Analysis Complete in %.3f ms.%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("Memory allocated during analysis approx: %.2f MB%n", memUsed / (1024.0 * 1024.0));
        System.out.printf("Found %d Articulation Points.%n", result.articulationPoints.size());
        System.out.printf("Found %d Biconnected Components.%n", result.biconnectedComponents.size());

        System.out.println("\nGraph Structure Overview:");
        System.out.printf("Connected Components        : %d%n", result.connectedComponents);
        System.out.printf("Graph Classification        : %s%n", classifyGraph(result));
        System.out.printf("Bridge-Like Components      : %d%n", result.bridgeLikeComponents);
        System.out.printf("Largest Biconnected Block   : %d nodes%n", result.largestBiconnectedComponentSize);

        System.out.println("\nTop 10 Most Critical Nodes (by Impact Factor):");
        System.out.printf("%-20s | %-25s | %-25s%n", "Node ID", "Disconnected Components", "Largest Remaining Component");
        System.out.println("-".repeat(75));

        int limit = Math.min(10, result.articulationPoints.size());
        for (int i = 0; i < limit; i++) {
            ReliabilityAnalyzer.NodeImpact impact = result.articulationPoints.get(i);
            System.out.printf("%-20s | %-25d | %-25d%n",
                    impact.nodeIdentifier,
                    impact.disconnectedComponents,
                    impact.largestComponentSize);
        }
        if (limit == 0) {
            System.out.println("No articulation points found. The graph remains stable under any single-vertex removal.");
        }

        System.out.println("\nTop 5 Biconnected Components:");
        System.out.printf("%-12s | %-10s | %-10s | %-20s | %-15s%n",
                "Component", "Vertices", "Edges", "Articulation Nodes", "Block Type");
        System.out.println("-".repeat(80));

        int componentLimit = Math.min(5, result.biconnectedComponents.size());
        for (int i = 0; i < componentLimit; i++) {
            ReliabilityAnalyzer.BiconnectedComponent component = result.biconnectedComponents.get(i);
            System.out.printf("%-12s | %-10d | %-10d | %-20d | %-15s%n",
                    "BCC-" + component.id,
                    component.nodes.size(),
                    component.edgeCount,
                    component.articulationVertexCount,
                    component.bridgeLike ? "Bridge Block" : "Robust Block");
        }
        if (componentLimit == 0) {
            System.out.println("No biconnected components were extracted from the current graph.");
        }

        if (visualPath != null) {
            if (graph.getV() > 2000) {
                System.err.println("\nWarning: Graph size (" + graph.getV()
                        + " nodes) exceeds visualization safety limit (2000). Visualization export skipped.");
            } else {
                GraphVisualizer.exportToHtml(graph, result, visualPath);
            }
        }

        System.out.println();
        System.out.println(buildBriefSummary(result));
    }

    private static Graph loadFromCsv(String filePath) throws IOException {
        Graph graph = new Graph();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            long lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
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
        System.out.println("  java -cp out com.network.App --file <path_to_csv> [--visualize <output.html>]");
        System.out.println("  java -cp out com.network.App --generate <V> <m> [--visualize <output.html>]");
        System.out.println("      <V> = Number of nodes to generate (for example 1000)");
        System.out.println("      <m> = Number of edges attached by each new node (for example 3)");
    }

    private static String classifyGraph(ReliabilityAnalyzer.AnalysisResult result) {
        if (result.isBiconnectedGraph) {
            return "Biconnected Graph";
        }
        if (result.connectedComponents == 1) {
            return "Connected but not Biconnected";
        }
        return "Disconnected Graph";
    }

    private static String buildBriefSummary(ReliabilityAnalyzer.AnalysisResult result) {
        return String.format(
                "Brief summary: %s with %d connected component(s), %d articulation point(s), and %d biconnected component(s). Largest block spans %d node(s).",
                classifyGraph(result),
                result.connectedComponents,
                result.articulationPoints.size(),
                result.biconnectedComponents.size(),
                result.largestBiconnectedComponentSize);
    }
}
