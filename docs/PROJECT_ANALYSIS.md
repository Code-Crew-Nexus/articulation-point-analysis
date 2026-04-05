# Project Analysis

## Overview
This project is a Java CLI application for network reliability analysis. It reads an undirected graph from CSV input or generates a synthetic scale-free network, then evaluates the graph using Tarjan-style depth-first search.

The project now covers both:

- articulation-point detection
- biconnected-component decomposition

That combination makes the tool much more useful for reliability studies because it identifies both fragile cut vertices and resilient graph blocks.

## Current Architecture

### Core modules
- `Graph.java`: stores the graph using adjacency lists with internal integer indexing for performance
- `NetworkSimulator.java`: generates Barabasi-Albert style scale-free networks
- `ReliabilityAnalyzer.java`: performs iterative Tarjan-style analysis and returns structural metrics
- `App.java`: provides CLI orchestration, summary printing, and result classification
- `GraphVisualizer.java`: exports an HTML visualization with articulation-point and BCC-aware styling

### Data flow
1. The CLI loads or generates a graph.
2. The analyzer computes discovery times, low-link values, articulation points, and biconnected components.
3. The CLI prints a concise report with critical nodes, block summaries, and a one-line conclusion.
4. Optional HTML export highlights articulation points and colors graph blocks for exploration.

## Algorithmic Notes

### Articulation points
The analyzer uses an iterative Tarjan-style DFS instead of recursive DFS. That avoids stack overflow on large graphs and preserves the expected `O(V + E)` complexity.

### Biconnected components
An edge stack is maintained during DFS traversal. Whenever a DFS child satisfies `low[child] >= disc[parent]`, the corresponding edge block is popped and materialized as a biconnected component.

This allows the project to report:

- total biconnected components
- bridge-like blocks
- largest robust block
- whether the whole graph is biconnected

## Improvements Applied

### Functional improvements
- Added biconnected-component detection to the main analysis pipeline
- Added graph classification output:
  - `Biconnected Graph`
  - `Connected but not Biconnected`
  - `Disconnected Graph`
- Added richer terminal summaries and top-BCC reporting
- Updated the HTML visualization so blocks are color-coded and nodes show BCC membership details

### Repository hygiene improvements
- Added a root `.gitignore`
- Marked compiled `.class` files and `out/` as build outputs instead of source assets
- Prepared a dedicated `docs/` folder for project material and presentation assets

## Validation Summary

The project was cross-checked with representative runs:

- `sample_graph.csv`
- generated scale-free graph with `20` nodes and `m = 2`
- a manual 4-cycle sanity graph

Observed behavior:

- sample graph correctly reports multiple articulation points and multiple BCCs
- generated graph reports a large robust core and bridge-like tail blocks
- cycle graph reports zero articulation points and one biconnected component

## Recommended Next Steps

- add unit tests for small canonical graph shapes
- export machine-readable JSON summaries
- add bridge detection as a first-class report item
- add benchmark scripts for large generated graphs
