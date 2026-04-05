package com.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * ReliabilityAnalyzer performs network reliability analysis using iterative Tarjan-style DFS.
 * It reports both articulation points and biconnected components in a single traversal.
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

    public static class BiconnectedComponent {
        public final int id;
        public final List<String> nodes;
        public final int edgeCount;
        public final int articulationVertexCount;
        public final boolean bridgeLike;

        final List<Integer> nodeIndices;
        final List<int[]> edgeIndices;

        public BiconnectedComponent(
                int id,
                List<Integer> nodeIndices,
                List<String> nodes,
                List<int[]> edgeIndices,
                int articulationVertexCount) {
            this.id = id;
            this.nodeIndices = Collections.unmodifiableList(nodeIndices);
            this.nodes = Collections.unmodifiableList(nodes);
            this.edgeIndices = Collections.unmodifiableList(edgeIndices);
            this.edgeCount = edgeIndices.size();
            this.articulationVertexCount = articulationVertexCount;
            this.bridgeLike = edgeCount == 1;
        }
    }

    public static class AnalysisResult {
        public final int totalNodes;
        public final int totalEdges;
        public final int connectedComponents;
        public final boolean isBiconnectedGraph;
        public final List<NodeImpact> articulationPoints;
        public final List<BiconnectedComponent> biconnectedComponents;
        public final int bridgeLikeComponents;
        public final int largestBiconnectedComponentSize;

        public AnalysisResult(
                int totalNodes,
                int totalEdges,
                int connectedComponents,
                boolean isBiconnectedGraph,
                List<NodeImpact> articulationPoints,
                List<BiconnectedComponent> biconnectedComponents,
                int bridgeLikeComponents,
                int largestBiconnectedComponentSize) {
            this.totalNodes = totalNodes;
            this.totalEdges = totalEdges;
            this.connectedComponents = connectedComponents;
            this.isBiconnectedGraph = isBiconnectedGraph;
            this.articulationPoints = articulationPoints;
            this.biconnectedComponents = biconnectedComponents;
            this.bridgeLikeComponents = bridgeLikeComponents;
            this.largestBiconnectedComponentSize = largestBiconnectedComponentSize;
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
        if (V == 0) {
            return new AnalysisResult(0, graph.getE(), 0, false, new ArrayList<>(), new ArrayList<>(), 0, 0);
        }

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

        // Edge stack used to materialize biconnected components.
        int edgeCapacity = Math.max(1, graph.getE());
        int[] edgeStackU = new int[edgeCapacity];
        int[] edgeStackV = new int[edgeCapacity];
        int edgeTop = -1;

        List<RawBiconnectedComponent> rawComponents = new ArrayList<>();

        int time = 0;
        int connectedComponents = 0;

        for (int i = 0; i < V; i++) {
            if (disc[i] == 0) {
                connectedComponents++;
                int startOrder = orderPtr;
                int componentEdgeBase = edgeTop + 1;

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

                        if (disc[v] == 0) {
                            // Tree-edge. Push to both simulated stacks.
                            edgeTop++;
                            edgeStackU[edgeTop] = u;
                            edgeStackV[edgeTop] = v;

                            disc[v] = low[v] = ++time;
                            subtreeSize[v] = 1;

                            head++;
                            stackU[head] = v;
                            stackP[head] = u;
                            stackEdgeIndex[head] = 0;

                            order[orderPtr++] = v;
                            pushed = true;
                            break; // Stop current node loop, process new child
                        } else if (disc[v] < disc[u]) {
                            // Back-edge found. It provides a path higher up the DFS tree.
                            edgeTop++;
                            edgeStackU[edgeTop] = u;
                            edgeStackV[edgeTop] = v;
                            low[u] = Math.min(low[u], disc[v]);
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

                                ComponentPopResult component = popComponentUntil(edgeStackU, edgeStackV, edgeTop, parentNode, childNode);
                                edgeTop = component.newTop;
                                rawComponents.add(component.component);
                            }
                        }
                    }
                } // End inner iterative DFS

                // Complete the connected component's sizes
                int cSize = orderPtr - startOrder;
                for (int j = startOrder; j < orderPtr; j++) {
                    compSizeArr[order[j]] = cSize;
                }

                if (edgeTop >= componentEdgeBase) {
                    ComponentPopResult component = popRemainingComponent(edgeStackU, edgeStackV, edgeTop, componentEdgeBase);
                    edgeTop = component.newTop;
                    rawComponents.add(component.component);
                }
            } // End if (disc[i] == 0)
        } // End outer loop over V

        // Prepare the result payload
        List<NodeImpact> articulationPoints = new ArrayList<>();
        boolean[] isArticulation = new boolean[V];
        
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
                isArticulation[i] = true;
            }
        }

        // Sort by impact metrics for useful results
        articulationPoints.sort(Comparator.comparingInt((NodeImpact ni) -> ni.disconnectedComponents)
                .reversed()
                .thenComparingInt(ni -> ni.largestComponentSize)
                .thenComparing(ni -> ni.nodeIdentifier));

        List<BiconnectedComponent> biconnectedComponents = buildBiconnectedComponents(rawComponents, isArticulation);
        int bridgeLikeComponents = 0;
        int largestBiconnectedComponentSize = 0;

        for (BiconnectedComponent component : biconnectedComponents) {
            if (component.bridgeLike) {
                bridgeLikeComponents++;
            }
            largestBiconnectedComponentSize = Math.max(largestBiconnectedComponentSize, component.nodes.size());
        }

        boolean isBiconnectedGraph = connectedComponents == 1 && V >= 3 && articulationPoints.isEmpty();

        return new AnalysisResult(
                V,
                graph.getE(),
                connectedComponents,
                isBiconnectedGraph,
                articulationPoints,
                biconnectedComponents,
                bridgeLikeComponents,
                largestBiconnectedComponentSize);
    }

    private List<BiconnectedComponent> buildBiconnectedComponents(
            List<RawBiconnectedComponent> rawComponents,
            boolean[] isArticulation) {
        rawComponents.sort(Comparator
                .comparingInt((RawBiconnectedComponent component) -> component.nodeIndices.size())
                .reversed()
                .thenComparing(Comparator.comparingInt((RawBiconnectedComponent component) -> component.edgeIndices.size()).reversed()));

        List<BiconnectedComponent> components = new ArrayList<>(rawComponents.size());
        for (int i = 0; i < rawComponents.size(); i++) {
            RawBiconnectedComponent rawComponent = rawComponents.get(i);
            List<String> nodes = new ArrayList<>(rawComponent.nodeIndices.size());
            int articulationVertexCount = 0;

            for (int nodeIndex : rawComponent.nodeIndices) {
                nodes.add(graph.getNodeIdentifier(nodeIndex));
                if (isArticulation[nodeIndex]) {
                    articulationVertexCount++;
                }
            }

            nodes.sort(String::compareTo);
            components.add(new BiconnectedComponent(
                    i + 1,
                    new ArrayList<>(rawComponent.nodeIndices),
                    nodes,
                    cloneEdgeList(rawComponent.edgeIndices),
                    articulationVertexCount));
        }
        return components;
    }

    private static List<int[]> cloneEdgeList(List<int[]> edges) {
        List<int[]> copy = new ArrayList<>(edges.size());
        for (int[] edge : edges) {
            copy.add(new int[] { edge[0], edge[1] });
        }
        return copy;
    }

    private static ComponentPopResult popComponentUntil(
            int[] edgeStackU,
            int[] edgeStackV,
            int currentTop,
            int stopU,
            int stopV) {
        return popEdges(edgeStackU, edgeStackV, currentTop, -1, stopU, stopV, true);
    }

    private static ComponentPopResult popRemainingComponent(
            int[] edgeStackU,
            int[] edgeStackV,
            int currentTop,
            int minTopInclusive) {
        return popEdges(edgeStackU, edgeStackV, currentTop, minTopInclusive, -1, -1, false);
    }

    private static ComponentPopResult popEdges(
            int[] edgeStackU,
            int[] edgeStackV,
            int currentTop,
            int minTopInclusive,
            int stopU,
            int stopV,
            boolean stopAtEdge) {
        LinkedHashSet<Integer> uniqueNodes = new LinkedHashSet<>();
        List<int[]> edges = new ArrayList<>();

        int top = currentTop;
        while (top >= 0 && (!stopAtEdge || top >= minTopInclusive)) {
            int u = edgeStackU[top];
            int v = edgeStackV[top];
            edges.add(new int[] { u, v });
            uniqueNodes.add(u);
            uniqueNodes.add(v);
            top--;

            if (stopAtEdge && ((u == stopU && v == stopV) || (u == stopV && v == stopU))) {
                break;
            }
            if (!stopAtEdge && top < minTopInclusive) {
                break;
            }
        }

        return new ComponentPopResult(new RawBiconnectedComponent(new ArrayList<>(uniqueNodes), edges), top);
    }

    private static class RawBiconnectedComponent {
        final List<Integer> nodeIndices;
        final List<int[]> edgeIndices;

        RawBiconnectedComponent(List<Integer> nodeIndices, List<int[]> edgeIndices) {
            this.nodeIndices = nodeIndices;
            this.edgeIndices = edgeIndices;
        }
    }

    private static class ComponentPopResult {
        final RawBiconnectedComponent component;
        final int newTop;

        ComponentPopResult(RawBiconnectedComponent component, int newTop) {
            this.component = component;
            this.newTop = newTop;
        }
    }
}
