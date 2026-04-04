package com.network;

import java.util.*;

/**
 * Core Graph data structure utilizing adjacency lists.
 * It maps String node identifiers to 0-indexed integer identifiers internally
 * for maximum performance with arrays.
 */
public class Graph {
    // Maps string identifiers to integer indices
    private final Map<String, Integer> nodeToIndex = new LinkedHashMap<>();
    // Maps integer indices to string identifiers
    private final List<String> indexToNode = new ArrayList<>();
    
    // Adjacency list representation using contiguous lists
    private final List<List<Integer>> adjList = new ArrayList<>();

    private int edgeCount = 0;

    /**
     * Gets the integer index for a given node identifier.
     * If it does not exist, registers the node and returns its new index.
     * 
     * @param identifier The string identifier of the node.
     * @return The 0-indexed integer associated with the node.
     */
    private int getOrAddNode(String identifier) {
        Integer index = nodeToIndex.get(identifier);
        if (index == null) {
            index = indexToNode.size();
            nodeToIndex.put(identifier, index);
            indexToNode.add(identifier);
            adjList.add(new ArrayList<>());
        }
        return index;
    }

    /**
     * Adds an undirected edge to the graph. The method ignores self-loops and parallel edges.
     *
     * @param src The source node string identifier.
     * @param dest The destination node string identifier.
     */
    public void addEdge(String src, String dest) {
        if (src.equals(dest)) {
            // Ignore self-loops
            return;
        }

        int u = getOrAddNode(src);
        int v = getOrAddNode(dest);

        // Check for parallel edges
        // Note: For massive scale-free networks, searching the adj list can be slow if degrees are high,
        // but typically scale-free has small average degree. For absolute performance we might not check this on add,
        // or we use a set instead of a list. But List provides fastest iteration, so we'll check it sparingly,
        // or assume the CSV parser might just let it be. Let's do a simple check.
        // Wait, for millions of edges, this O(degree) check is fine.
        // Actually, Barabasi-Albert won't generate parallel edges but we must handle CSV properly.
        // We'll trust the List check for now, or use a custom filter later. To save O(degree), 
        // we can skip parallel edge check if we guarantee it from generation or use a cleaner approach.
        // Let's implement the basic contains check.
        if (adjList.get(u).contains(v)) {
            return; 
        }

        adjList.get(u).add(v);
        adjList.get(v).add(u);
        edgeCount++;
    }

    /**
     * Number of vertices
     */
    public int getV() {
        return indexToNode.size();
    }

    /**
     * Number of edges
     */
    public int getE() {
        return edgeCount;
    }

    /**
     * Get adjacent vertices for a given vertex index
     */
    public List<Integer> getAdj(int v) {
        return adjList.get(v);
    }
    
    /**
     * Maps index back to original String identifier
     */
    public String getNodeIdentifier(int v) {
        return indexToNode.get(v);
    }

    /**
     * Gets all valid node indices
     */
    public int[] getAllVertices() {
        int[] v = new int[indexToNode.size()];
        for(int i = 0; i < v.length; i++) v[i] = i;
        return v;
    }
}
