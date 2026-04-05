package com.network;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphVisualizer {
    private static final String[] COMPONENT_COLORS = {
            "#0F766E", "#2563EB", "#EA580C", "#7C3AED", "#DC2626",
            "#059669", "#B45309", "#1D4ED8", "#BE185D", "#4F46E5"
    };
    private static final String DEFAULT_NODE_COLOR = "#9DB7D5";
    private static final String DEFAULT_EDGE_COLOR = "#94A3B8";

    public static void exportToHtml(Graph graph, ReliabilityAnalyzer.AnalysisResult result, String outputFilename) {
        // Collect articulation points for fast lookup
        Set<String> apSet = new HashSet<>();
        for (ReliabilityAnalyzer.NodeImpact impact : result.articulationPoints) {
            apSet.add(impact.nodeIdentifier);
        }

        Map<Integer, Integer> nodePrimaryComponent = new HashMap<>();
        Map<Integer, Integer> nodePrimaryComponentSize = new HashMap<>();
        int[] componentMembershipCount = new int[graph.getV()];
        Map<String, Integer> edgeToComponent = new HashMap<>();

        for (ReliabilityAnalyzer.BiconnectedComponent component : result.biconnectedComponents) {
            int componentSize = component.nodes.size();

            for (int nodeIndex : component.nodeIndices) {
                componentMembershipCount[nodeIndex]++;
                int bestSize = nodePrimaryComponentSize.getOrDefault(nodeIndex, -1);
                if (componentSize > bestSize) {
                    nodePrimaryComponent.put(nodeIndex, component.id);
                    nodePrimaryComponentSize.put(nodeIndex, componentSize);
                }
            }

            for (int[] edge : component.edgeIndices) {
                edgeToComponent.put(edgeKey(edge[0], edge[1]), component.id);
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("  <title>Network Reliability Visualization</title>");
            writer.println("  <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.2/standalone/umd/vis-network.min.js\"></script>");
            writer.println("  <style type=\"text/css\">");
            writer.println("    body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; display: flex; flex-direction: column; height: 100vh; background: linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);}");
            writer.println("    #header { padding: 18px 24px; background: rgba(255,255,255,0.95); backdrop-filter: blur(8px); box-shadow: 0 10px 30px rgba(15,23,42,0.08); display: flex; justify-content: space-between; align-items: center; gap: 18px; z-index: 10; }");
            writer.println("    #header h2 { margin: 0; color: #0f172a; }");
            writer.println("    #header p { margin: 6px 0 0; color: #475569; font-size: 14px; }");
            writer.println("    #mynetwork { flex: 1; width: 100%; min-height: 800px; background: radial-gradient(circle at top, #ffffff 0%, #eff6ff 52%, #e2e8f0 100%); border-top: 1px solid #cbd5e1; }");
            writer.println("    .btn { padding: 10px 20px; background: #ff4d4d; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; transition: background 0.2s; }");
            writer.println("    .btn:hover { background: #e60000; }");
            writer.println("    .btn.active { background: #4CAF50; }");
            writer.println("    .stats { display: flex; flex-wrap: wrap; gap: 12px; color: #334155; font-size: 14px; margin-top: 10px; }");
            writer.println("    .chip { background: #e2e8f0; border-radius: 999px; padding: 6px 12px; }");
            writer.println("    .chip b { color: #b91c1c; }");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            
            writer.println("<div id=\"header\">");
            writer.println("  <div>");
            writer.println("    <h2>Network Graph Visualization</h2>");
            writer.println("    <p>Articulation points remain highlighted in red while biconnected blocks are color-coded across the graph.</p>");
            writer.println("    <div class=\"stats\">");
            writer.println("      <span class=\"chip\">Nodes: " + graph.getV() + "</span>");
            writer.println("      <span class=\"chip\">Edges: " + graph.getE() + "</span>");
            writer.println("      <span class=\"chip\">Articulation Points: <b>" + result.articulationPoints.size() + "</b></span>");
            writer.println("      <span class=\"chip\">Biconnected Components: " + result.biconnectedComponents.size() + "</span>");
            writer.println("      <span class=\"chip\">Largest Block: " + result.largestBiconnectedComponentSize + " nodes</span>");
            writer.println("    </div>");
            writer.println("  </div>");
            writer.println("  <button id=\"toggleBtn\" class=\"btn\" onclick=\"toggleAPs()\">Remove Articulation Points</button>");
            writer.println("</div>");

            writer.println("<div id=\"mynetwork\"></div>");

            writer.println("<script type=\"text/javascript\">");
            
            // Build JS Nodes
            writer.println("  var nodesData = [");
            int[] vertices = graph.getAllVertices();
            for (int u : vertices) {
                String id = graph.getNodeIdentifier(u);
                int componentId = nodePrimaryComponent.getOrDefault(u, 0);
                String componentColor = componentId == 0 ? DEFAULT_NODE_COLOR : componentColor(componentId);
                String title = "Node: " + id
                        + "\nMemberships in BCCs: " + componentMembershipCount[u]
                        + "\nPrimary BCC: " + (componentId == 0 ? "None" : "BCC-" + componentId);
                if (apSet.contains(id)) {
                    writer.println("    { id: '" + escapeForJavaScript(id) + "', label: '" + escapeForJavaScript(id) + "', title: '" + escapeForJavaScript(title) + "', isAP: true, color: { background: '#ef4444', border: '#991b1b'}, font: {color: 'white'}, size: 24, borderWidth: 3, shadow: true },");
                } else {
                    writer.println("    { id: '" + escapeForJavaScript(id) + "', label: '" + escapeForJavaScript(id) + "', title: '" + escapeForJavaScript(title) + "', isAP: false, color: { background: '" + componentColor + "', border: '#0f172a' }, size: 16, borderWidth: 1.5 },");
                }
            }
            writer.println("  ];");

            // Build JS Edges
            writer.println("  var edgesData = [");
            for (int u : vertices) {
                for (int v : graph.getAdj(u)) {
                    if (u < v) { // prevent duplicates in undirected graph
                        int componentId = edgeToComponent.getOrDefault(edgeKey(u, v), 0);
                        String color = componentId == 0 ? DEFAULT_EDGE_COLOR : componentColor(componentId);
                        writer.println("    { from: '" + escapeForJavaScript(graph.getNodeIdentifier(u)) + "', to: '" + escapeForJavaScript(graph.getNodeIdentifier(v)) + "', color: { color: '" + color + "', highlight: '" + color + "' }, width: " + (componentId == 0 ? "1.2" : "2.2") + " },");
                    }
                }
            }
            writer.println("  ];");

            // vis.js initialization
            writer.println("  var nodes = new vis.DataSet(nodesData);");
            writer.println("  var edges = new vis.DataSet(edgesData);");
            writer.println("  var container = document.getElementById('mynetwork');");
            writer.println("  var data = { nodes: nodes, edges: edges };");
            writer.println("  var options = {");
            writer.println("    nodes: { shape: 'dot' },");
            writer.println("    physics: { ");
            writer.println("        barnesHut: { gravitationalConstant: -2400, centralGravity: 0.24, springLength: 100 },");
            writer.println("        stabilization: { iterations: 200 }");
            writer.println("    },");
            writer.println("    edges: { smooth: { type: 'dynamic' } },");
            writer.println("    interaction: { hover: true, tooltipDelay: 120 }");
            writer.println("  };");
            writer.println("  var network = new vis.Network(container, data, options);");

            // Toggle function
            writer.println("  var apsRemoved = false;");
            writer.println("  function toggleAPs() {");
            writer.println("    var btn = document.getElementById('toggleBtn');");
            writer.println("    if (!apsRemoved) {");
            writer.println("      // Remove APs");
            writer.println("      var updates = [];");
            writer.println("      nodesData.forEach(function(n) {");
            writer.println("        if(n.isAP) updates.push({id: n.id, hidden: true});");
            writer.println("      });");
            writer.println("      nodes.update(updates);");
            writer.println("      btn.innerHTML = 'Restore Articulation Points';");
            writer.println("      btn.classList.add('active');");
            writer.println("      apsRemoved = true;");
            writer.println("    } else {");
            writer.println("      // Restore APs");
            writer.println("      var updates = [];");
            writer.println("      nodesData.forEach(function(n) {");
            writer.println("        if(n.isAP) updates.push({id: n.id, hidden: false});");
            writer.println("      });");
            writer.println("      nodes.update(updates);");
            writer.println("      btn.innerHTML = 'Remove Articulation Points';");
            writer.println("      btn.classList.remove('active');");
            writer.println("      apsRemoved = false;");
            writer.println("    }");
            writer.println("  }");

            writer.println("</script>");
            writer.println("</body>");
            writer.println("</html>");

            System.out.println("\nVisualization successfully exported to: " + outputFilename);
            System.out.println("Open this HTML file in your web browser to view the interactive graph.");

        } catch (IOException e) {
            System.err.println("Error writing HTML visualization: " + e.getMessage());
        }
    }

    private static String componentColor(int componentId) {
        return COMPONENT_COLORS[(componentId - 1) % COMPONENT_COLORS.length];
    }

    private static String edgeKey(int u, int v) {
        return Math.min(u, v) + ":" + Math.max(u, v);
    }

    private static String escapeForJavaScript(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
