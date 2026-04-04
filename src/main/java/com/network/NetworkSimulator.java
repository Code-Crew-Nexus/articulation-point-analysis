package com.network;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * Simulates real-world network topologies.
 * Specifically generates Scale-Free Networks using the Barabási–Albert (BA) model.
 */
public class NetworkSimulator {

    /**
     * Generates a Scale-Free graph using preferential attachment.
     * 
     * @param targetNodes Total number of nodes to generate (V).
     * @param m The number of edges to attach from a new node to existing nodes.
     * @return A populated Graph instance.
     */
    public static Graph generateScaleFreeGraph(int targetNodes, int m) {
        if (m <= 0 || targetNodes < m) {
            throw new IllegalArgumentException("Invalid graph parameters.");
        }

        Graph graph = new Graph();

        // Reserve a small terminal chain so the generated graph always contains
        // at least a few articulation points instead of relying on random chance.
        int tailLength = targetNodes >= m + 5 ? 3 : 0;
        int coreNodes = targetNodes - tailLength;

        // We use a "roulette wheel" array to achieve O(1) preferential attachment selection.
        // The array stores node IDs, and each node ID appears exactly 'degree' times.
        // The total sum of degrees for V nodes and E edges is 2E. We allocate a large array.
        // For targetNodes nodes, approximately targetNodes * m edges will be formed.
        int totalEdgesApprox = coreNodes * m + tailLength;
        int[] roulette = new int[totalEdgesApprox * 2 + m * 5]; 
        int rouletteSize = 0;

        Random rand = new Random();

        // 1. Initial fully connected core of size m (m0 = m)
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                graph.addEdge(String.valueOf(i), String.valueOf(j));
                roulette[rouletteSize++] = i;
                roulette[rouletteSize++] = j;
            }
        }
        
        if (m == 1 && targetNodes > 1) {
            // Special initialization if m=1 to avoid an isolated first node.
            // A clique of size 1 has 0 edges. We need at least one edge to start.
            graph.addEdge("0", "1");
            roulette[rouletteSize++] = 0;
            roulette[rouletteSize++] = 1;
        }

        // 2. Add remaining core nodes sequentially, attaching to existing nodes preferentially
        int startNode = (m == 1) ? 2 : m;
        for (int i = startNode; i < coreNodes; i++) {
            Set<Integer> chosenTargets = new HashSet<>();
            
            // Pick m distinct targets
            while (chosenTargets.size() < m && chosenTargets.size() < (rouletteSize / 2) + 1) {
                int randomIdx = rand.nextInt(rouletteSize);
                int target = roulette[randomIdx];
                chosenTargets.add(target);
            }

            for (int target : chosenTargets) {
                graph.addEdge(String.valueOf(i), String.valueOf(target));
                // Add to roulette to increase their future preference probability
                roulette[rouletteSize++] = i;
                roulette[rouletteSize++] = target;
            }
        }

        // 3. Attach a short chain to the core so the graph contains guaranteed
        // articulation points even when the preferential-attachment core is dense.
        if (tailLength == 3) {
            int attachPoint = 0;
            String tail0 = String.valueOf(coreNodes);
            String tail1 = String.valueOf(coreNodes + 1);
            String tail2 = String.valueOf(coreNodes + 2);

            graph.addEdge(String.valueOf(attachPoint), tail0);
            graph.addEdge(tail0, tail1);
            graph.addEdge(tail1, tail2);
        }

        return graph;
    }
}
