<div align="center">

# Articulation Point Analysis

![Project Status: Active](https://img.shields.io/badge/status-active-brightgreen)
![GitHub license](https://img.shields.io/github/license/Code-Crew-Nexus/articulation-point-analysis?color=purple)
![GitHub repo size](https://img.shields.io/github/repo-size/Code-Crew-Nexus/articulation-point-analysis?color=blue)
![GitHub language count](https://img.shields.io/github/languages/count/Code-Crew-Nexus/articulation-point-analysis?color=yellow)
![GitHub top language](https://img.shields.io/github/languages/top/Code-Crew-Nexus/articulation-point-analysis?color=orange)
![GitHub last commit](https://img.shields.io/github/last-commit/Code-Crew-Nexus/articulation-point-analysis?color=red)

![GitHub forks](https://img.shields.io/github/forks/Code-Crew-Nexus/articulation-point-analysis?style=social)
![GitHub stars](https://img.shields.io/github/stars/Code-Crew-Nexus/articulation-point-analysis?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/Code-Crew-Nexus/articulation-point-analysis?style=social)

---

## Languages & Tools

![Java](https://img.shields.io/badge/Java-007396?logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white)
![GraphViz](https://img.shields.io/badge/GraphViz-4B0082?logo=graphviz&logoColor=white)

</div>

---

## Overview
Articulation Point Analysis is a **Java CLI project** to analyze network reliability using **Tarjan-style DFS articulation point detection**. It supports optional **HTML visualization** for interactive exploration of critical nodes.

---

## What It Does
- Loads a graph from CSV, or generates a scale-free graph  
- Finds articulation points and ranks their impact  
- Prints the top critical nodes in the terminal  
- Optionally exports an interactive HTML graph  

---

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
> Future contributors welcome! Fork the repo, submit pull requests, and help improve the project. 

---