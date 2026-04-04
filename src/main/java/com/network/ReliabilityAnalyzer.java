package com.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ReliabilityAnalyzer performs Network Reliability Analysis via Articulation Point 
 * Detection using an Iterative Tarjan's Algorithm.
 */
public class ReliabilityAnalyzer {

    public static class NodeImpact {
        public final String nodeIdentifier;
        public final int disconnectedComponents;
        public final int largestComponentSize;

        public NodeImpact(String nodeIdentifier, int disconnectedComponents, int largestComponentSize) {
            this.nodeIdentifier = nodeIdentifier;
            this.disconnectedComponents = disconnectedComponents;
            this.largestComponentSize = largestComponentSize;
        }
    }

    public static class AnalysisResult {
        public final int totalNodes;
        public final int totalEdges;
        public final List<NodeImpact> articulationPoints;

        public AnalysisResult(int totalNodes, int totalEdges, List<NodeImpact> articulationPoints) {
            this.totalNodes = totalNodes;
            this.totalEdges = totalEdges;
            this.articulationPoints = articulationPoints;
        }
    }

    private final Graph graph;

    public ReliabilityAnalyzer(Graph graph) {
        this.graph = graph;
    }

    /**
     * Executes Iterative Tarjan's Algorithm to find Articulation Points in O(V + E) time.
     * Calculates Impact Factors (Component Sizes and Counts) without requiring subsequent graph traversals.
     * 
     * Detailed logic of lowLink:
     * - `discoveryTime` captures the order in which nodes are visited in the DFS tree.
     * - `lowLinkValue` captures the lowest discovery time reachable from a node, including at most one back-edge.
     * - If a child's lowLinkValue >= parent's discoveryTime, no path exists from the child's subtree to an ancestor of the parent.
     *   Hence, removing the parent disconnects the child's subtree.
     */
    public AnalysisResult analyze() {
        int V = graph.getV();

        // Standard Tarjan state tracking
        int[] disc = new int[V];
        int[] low = new int[V];
        int[] subtreeSize = new int[V];

        // Impact metrics tracking
        int[] numSeparated = new int[V]; // Number of detached tree branches when this node is removed
        int[] sumSeparated = new int[V]; // Sum of nodes in those detached branches
        int[] largestSeparated = new int[V]; // The size of the largest detached branch

        // Used to track which nodes belong to which full connected components
        int[] compSizeArr = new int[V]; 
        int[] order = new int[V];
        int orderPtr = 0;

        // Custom stack for iterative DFS to avoid StackOverflowError on massive graphs
        int[] stackU = new int[V];
        int[] stackP = new int[V];
        int[] stackEdgeIndex = new int[V];

        int time = 0;

        for (int i = 0; i < V; i++) {
            if (disc[i] == 0) {
                int startOrder = orderPtr;

                int head = 0;
                stackU[head] = i;
                stackP[head] = -1;
                stackEdgeIndex[head] = 0;

                disc[i] = low[i] = ++time;
                subtreeSize[i] = 1;

                order[orderPtr++] = i;

                while (head >= 0) {
                    int u = stackU[head];
                    int p = stackP[head];
                    int edgeIdx = stackEdgeIndex[head];
                    List<Integer> adj = graph.getAdj(u);

                    boolean pushed = false;

                    while (edgeIdx < adj.size()) {
                        int v = adj.get(edgeIdx);
                        edgeIdx++;
                        stackEdgeIndex[head] = edgeIdx; // Resume from here later

                        if (v == p) {
                            continue; // Do not traverse back immediately using the same edge
                        }

                        if (disc[v] != 0) {
                            // Back-edge found. It provides a path higher up the DFS tree.
                            low[u] = Math.min(low[u], disc[v]);
                        } else {
                            // Tree-edge. Push to simulated stack.
                            disc[v] = low[v] = ++time;
                            subtreeSize[v] = 1;

                            head++;
                            stackU[head] = v;
                            stackP[head] = u;
                            stackEdgeIndex[head] = 0;

                            order[orderPtr++] = v;
                            pushed = true;
                            break; // Stop current node loop, process new child
                        }
                    }

                    if (!pushed) {
                        // Finished processing node 'u' entirely
                        head--;
                        if (head >= 0) {
                            int parentNode = stackU[head];
                            int childNode = u;

                            // Propagate back the tree size and lowest linked ancestor
                            low[parentNode] = Math.min(low[parentNode], low[childNode]);
                            subtreeSize[parentNode] += subtreeSize[childNode];

                            // Tarjan's bridge / articulation point core logic
                            if (low[childNode] >= disc[parentNode]) {
                                // removing parentNode will drop childNode's entire subtree
                                sumSeparated[parentNode] += subtreeSize[childNode];
                                numSeparated[parentNode]++;
                                if (subtreeSize[childNode] > largestSeparated[parentNode]) {
                                    largestSeparated[parentNode] = subtreeSize[childNode];
                                }
                            }
                        }
                    }
                } // End inner iterative DFS

                // Complete the connected component's sizes
                int cSize = orderPtr - startOrder;
                for (int j = startOrder; j < orderPtr; j++) {
                    compSizeArr[order[j]] = cSize;
                }
            } // End if (disc[i] == 0)
        } // End outer loop over V

        // Prepare the result payload
        List<NodeImpact> articulationPoints = new ArrayList<>();
        
        // Root logic: node is AP if it's the root of a DFS tree with >= 2 children.
        // Non-root logic: node is AP if it has >= 1 child with low >= disc.
        for (int i = 0; i < V; i++) {
            // Because numSeparated is perfectly populated and sumSeparated captures exact counts,
            // we can decipher if it's an articulation point logically.
            int remaining = compSizeArr[i] - 1 - sumSeparated[i];
            int pieces = numSeparated[i] + (remaining > 0 ? 1 : 0);

            // A vertex is an AP iff its removal disconnects the graph into > 1 piece.
            if (pieces > 1) {
                int largestComponent = Math.max(largestSeparated[i], remaining);
                articulationPoints.add(new NodeImpact(graph.getNodeIdentifier(i), pieces, largestComponent));
            }
        }

        // Sort by impact metrics for useful results
        articulationPoints.sort(Comparator.comparingInt((NodeImpact ni) -> ni.disconnectedComponents)
                .reversed()
                .thenComparingInt(ni -> ni.largestComponentSize));

        return new AnalysisResult(V, graph.getE(), articulationPoints);
    }
}
