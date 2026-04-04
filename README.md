# Articulation Point Analysis

A Java CLI project to analyze network reliability using articulation point detection (Tarjan-style DFS) and optional HTML visualization.

## What It Does

- Loads a graph from CSV, or generates a scale-free graph.
- Finds articulation points and ranks their impact.
- Prints the top critical nodes in the terminal.
- Optionally exports an interactive HTML graph.

Perfect — since you’ve shown me the actual folder layout, here’s a **clean project structure section** you can add to your README.  

---

## Project Structure

```
ARTICULATION-POINT-ANALYSIS/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── network/
│                   ├── App.java                # CLI entry point
│                   ├── Graph.java              # Graph data structure
│                   ├── GraphVisualizer.java    # HTML exporter
│                   ├── NetworkSimulator.java   # Scale-free generator
│                   ├── ReliabilityAnalyzer.java# Articulation-point analysis
│
├── out/                     # Compiled .class files
├── sample_graph.csv          # Example input graph
├── scale_free_network.html   # Visualization output
├── test.html                 # Test visualization
├── README.md                 # Documentation
└── .idea/                    # IntelliJ project settings
```

---

## Requirements

- Java JDK 11 or later
- Windows PowerShell (commands below use PowerShell syntax)

## Compile

From repository root (`D:\ARTICULATION-POINT-ANALYSIS`):

```powershell
javac -d out src/main/java/com/network/*.java
```

From source folder (`D:\ARTICULATION-POINT-ANALYSIS\src\main\java`):

```powershell
javac (Get-ChildItem -Path .\com\network\*.java | ForEach-Object { $_.FullName })
```

## Run

### Option A: Run from repository root (recommended)

```powershell
java -cp out com.network.App --file sample_graph.csv --visualize scale_free_network.html
```

Generate a graph instead of loading CSV:

```powershell
java -cp out com.network.App --generate 1000 3 --visualize scale_free_network.html
```

### Option B: Run from `src/main/java`

```powershell
java com.network.App --file ..\..\..\sample_graph.csv --visualize ..\..\..\scale_free_network.html
```

Generate and visualize:

```powershell
java com.network.App --generate 1000 3 --visualize ..\..\..\scale_free_network.html
```

## Open the Graph Visualization

After running with `--visualize`, open:

- `scale_free_network.html`

in your browser.

## CSV Input Format

Each non-empty line should contain one undirected edge:

```text
nodeA,nodeB
nodeB,nodeC
```

Notes:
- Lines starting with `#` are treated as comments.
- Self-loops and duplicate edges are ignored by the graph builder.

## Command Reference

```text
java -cp . com.network.App --file <path_to_csv> [--visualize <output.html>]
java -cp . com.network.App --generate <V> <m> [--visualize <output.html>]
```

- `<V>`: number of nodes
- `<m>`: number of edges from each new node in generation

## Important Notes

- Visualization is skipped for graphs with more than 2000 nodes.
- Generated graphs include a small terminal chain to ensure articulation points are present.

## Troubleshooting

- `Could not find or load main class com.network.App`:
  - Use the correct classpath (`-cp out` from root), or
  - run from `src/main/java` where compiled package folders exist.
- No HTML output:
  - Ensure `--visualize <path>.html` is passed.
- Invalid file path:
  - Use relative paths from your current directory, or absolute paths.

## Contributors

This project was developed as part of the **DAA Project Based Learning (PBL)** initiative under **Code Crew Nexus**.

- **M. Sai Krishna** 
- **Rishit Ghosh**   
- **Md. Abdul Rayain**
