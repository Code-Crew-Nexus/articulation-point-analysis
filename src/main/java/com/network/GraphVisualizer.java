package com.network;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class GraphVisualizer {

    public static void exportToHtml(Graph graph, ReliabilityAnalyzer.AnalysisResult result, String outputFilename) {
        // Collect articulation points for fast lookup
        Set<String> apSet = new HashSet<>();
        for (ReliabilityAnalyzer.NodeImpact impact : result.articulationPoints) {
            apSet.add(impact.nodeIdentifier);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("  <title>Network Reliability Visualization</title>");
            writer.println("  <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.2/standalone/umd/vis-network.min.js\"></script>");
            writer.println("  <style type=\"text/css\">");
            writer.println("    body { font-family: sans-serif; margin: 0; padding: 0; display: flex; flex-direction: column; height: 100vh; background: #f0f2f5;}");
            writer.println("    #header { padding: 15px 20px; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.1); display: flex; justify-content: space-between; align-items: center; z-index: 10; }");
            writer.println("    #header h2 { margin: 0; color: #333; }");
            writer.println("    #mynetwork { flex: 1; width: 100%; min-height: 800px; background: #fafafa; border-top: 1px solid #ddd; }");
            writer.println("    .btn { padding: 10px 20px; background: #ff4d4d; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; transition: background 0.2s; }");
            writer.println("    .btn:hover { background: #e60000; }");
            writer.println("    .btn.active { background: #4CAF50; }");
            writer.println("    .stats { color: #666; font-size: 14px; }");
            writer.println("    .stats b { color: #d32f2f; }");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            
            writer.println("<div id=\"header\">");
            writer.println("  <div>");
            writer.println("    <h2>Network Graph Visualization</h2>");
            writer.println("    <div class=\"stats\">Nodes: " + graph.getV() + " | Edges: " + graph.getE() + " | Articulation Points: <b>" + result.articulationPoints.size() + "</b></div>");
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
                if (apSet.contains(id)) {
                    writer.println("    { id: '" + id + "', label: '" + id + "', isAP: true, color: { background: '#ff4d4d', border: '#cc0000'}, font: {color: 'white'}, size: 25, borderWidth: 2, shadow: true },");
                } else {
                    writer.println("    { id: '" + id + "', label: '" + id + "', isAP: false, color: { background: '#97c2fc', border: '#2b7ce9' }, size: 15 },");
                }
            }
            writer.println("  ];");

            // Build JS Edges
            writer.println("  var edgesData = [");
            for (int u : vertices) {
                for (int v : graph.getAdj(u)) {
                    if (u < v) { // prevent duplicates in undirected graph
                        writer.println("    { from: '" + graph.getNodeIdentifier(u) + "', to: '" + graph.getNodeIdentifier(v) + "' },");
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
            writer.println("        barnesHut: { gravitationalConstant: -2000, centralGravity: 0.3, springLength: 95 },");
            writer.println("        stabilization: { iterations: 200 }");
            writer.println("    },");
            writer.println("    interaction: { hover: true }");
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
}
