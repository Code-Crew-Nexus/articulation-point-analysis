param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Remove-ComObject {
    param([object]$ComObject)
    if ($null -ne $ComObject) {
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($ComObject)
    }
}

function Get-ProjectContext {
    param([string]$Root)

    Push-Location $Root
    try {
        if (-not (Test-Path "out")) {
            New-Item -ItemType Directory -Path "out" | Out-Null
        }

        & javac -d out src/main/java/com/network/*.java | Out-Null

        $sampleOutput = (& java -cp out com.network.App --file sample_graph.csv) -join [Environment]::NewLine
        $generatedOutput = (& java -cp out com.network.App --generate 20 2) -join [Environment]::NewLine

        [PSCustomObject]@{
            SampleOutput = $sampleOutput
            GeneratedOutput = $generatedOutput
            GeneratedOn = (Get-Date)
            Contributors = @(
                "M. Sai Krishna",
                "Rishit Ghosh",
                "Md. Abdul Rayain"
            )
        }
    } finally {
        Pop-Location
    }
}

function Add-FormattedTextBox {
    param(
        [object]$Slide,
        [float]$Left,
        [float]$Top,
        [float]$Width,
        [float]$Height,
        [string]$Text,
        [float]$FontSize = 18,
        [string]$FontName = "Aptos",
        [int]$Color = 0x1E293B,
        [int]$ParagraphFormat = 1,
        [switch]$Bold
    )

    $shape = $Slide.Shapes.AddTextbox(1, $Left, $Top, $Width, $Height)
    $textRange = $shape.TextFrame.TextRange
    $textRange.Text = $Text
    $textRange.Font.Name = $FontName
    $textRange.Font.Size = $FontSize
    $textRange.Font.Color.RGB = $Color
    $textRange.ParagraphFormat.Alignment = $ParagraphFormat
    if ($Bold) {
        $textRange.Font.Bold = -1
    }
    return $shape
}

function Add-SlideChrome {
    param(
        [object]$Slide,
        [string]$Title,
        [string]$Subtitle = ""
    )

    $slideWidth = 13.333 * 72
    $slideHeight = 7.5 * 72

    $bg = $Slide.Shapes.AddShape(1, 0, 0, $slideWidth, $slideHeight)
    $bg.Fill.ForeColor.RGB = 0xF8FAFC
    $bg.Line.Visible = 0

    $header = $Slide.Shapes.AddShape(1, 0, 0, $slideWidth, 0.95 * 72)
    $header.Fill.ForeColor.RGB = 0x0F172A
    $header.Line.Visible = 0

    $accent = $Slide.Shapes.AddShape(1, 0, 0.95 * 72, $slideWidth, 0.08 * 72)
    $accent.Fill.ForeColor.RGB = 0x0F766E
    $accent.Line.Visible = 0

    Add-FormattedTextBox -Slide $Slide -Left (0.45*72) -Top (0.17*72) -Width (8.8*72) -Height (0.42*72) -Text $Title -FontSize 24 -FontName "Aptos Display" -Color 0xFFFFFF -Bold | Out-Null

    if ($Subtitle) {
        Add-FormattedTextBox -Slide $Slide -Left (0.48*72) -Top (1.2*72) -Width (11.8*72) -Height (0.35*72) -Text $Subtitle -FontSize 11 -FontName "Aptos" -Color 0x475569 | Out-Null
    }

    $footer = $Slide.Shapes.AddShape(1, 0, 7.16*72, $slideWidth, 0.34*72)
    $footer.Fill.ForeColor.RGB = 0xE2E8F0
    $footer.Line.Visible = 0

    Add-FormattedTextBox -Slide $Slide -Left (0.42*72) -Top (7.19*72) -Width (12*72) -Height (0.2*72) -Text "Articulation Point Analysis | DAA PBL | Code Crew Nexus" -FontSize 9 -Color 0x334155 | Out-Null
}

function Set-BulletText {
    param(
        [object]$Shape,
        [string[]]$Lines,
        [float]$FontSize = 18,
        [int]$Color = 0x1E293B
    )

    $shape.TextFrame.TextRange.Text = ($Lines -join "`r")
    $paragraphCount = $shape.TextFrame.TextRange.Paragraphs().Count
    for ($i = 1; $i -le $paragraphCount; $i++) {
        $p = $shape.TextFrame.TextRange.Paragraphs($i, 1)
        $p.Font.Name = "Aptos"
        $p.Font.Size = $FontSize
        $p.Font.Color.RGB = $Color
        $p.ParagraphFormat.Bullet.Visible = -1
        $p.ParagraphFormat.Bullet.Character = 8226
        $p.ParagraphFormat.SpaceAfter = 8
    }
}

function Add-BulletSlide {
    param(
        [object]$Presentation,
        [string]$Title,
        [string]$Subtitle,
        [string[]]$Bullets
    )

    $slide = $Presentation.Slides.Add($Presentation.Slides.Count + 1, 12)
    Add-SlideChrome -Slide $slide -Title $Title -Subtitle $Subtitle

    $panel = $slide.Shapes.AddShape(1, 0.55*72, 1.55*72, 12.1*72, 5.2*72)
    $panel.Fill.ForeColor.RGB = 0xFFFFFF
    $panel.Line.ForeColor.RGB = 0xCBD5E1
    $panel.Line.Weight = 1.25

    $textShape = $slide.Shapes.AddTextbox(1, 0.9*72, 1.85*72, 11.2*72, 4.7*72)
    Set-BulletText -Shape $textShape -Lines $Bullets -FontSize 19
}
function Add-TwoColumnSlide {
    param(
        [object]$Presentation,
        [string]$Title,
        [string]$Subtitle,
        [string]$LeftHeading,
        [string[]]$LeftBullets,
        [string]$RightHeading,
        [string[]]$RightBullets
    )

    $slide = $Presentation.Slides.Add($Presentation.Slides.Count + 1, 12)
    Add-SlideChrome -Slide $slide -Title $Title -Subtitle $Subtitle

    foreach ($spec in @(
        @{ Left = 0.55; Heading = $LeftHeading; Bullets = $LeftBullets; Fill = 0xFFFFFF },
        @{ Left = 6.65; Heading = $RightHeading; Bullets = $RightBullets; Fill = 0xF8FAFC }
    )) {
        $box = $slide.Shapes.AddShape(1, $spec.Left*72, 1.65*72, 5.75*72, 4.95*72)
        $box.Fill.ForeColor.RGB = $spec.Fill
        $box.Line.ForeColor.RGB = 0xCBD5E1
        $box.Line.Weight = 1.15

        Add-FormattedTextBox -Slide $slide -Left (($spec.Left + 0.25)*72) -Top (1.88*72) -Width (5.2*72) -Height (0.35*72) -Text $spec.Heading -FontSize 18 -FontName "Aptos Display" -Color 0x0F172A -Bold | Out-Null

        $textShape = $slide.Shapes.AddTextbox(1, ($spec.Left + 0.22)*72, 2.3*72, 5.15*72, 4.1*72)
        Set-BulletText -Shape $textShape -Lines $spec.Bullets -FontSize 16
    }
}

function Add-ArchitectureSlide {
    param([object]$Presentation)

    $slide = $Presentation.Slides.Add($Presentation.Slides.Count + 1, 12)
    Add-SlideChrome -Slide $slide -Title "Architecture And Data Flow" -Subtitle "How the CLI, analyzer, and visualizer fit together"

    $boxes = @(
        @{ X = 0.9; Y = 2.0; W = 2.1; H = 1.05; Text = "CSV / Generator"; Fill = 0xDBEAFE },
        @{ X = 3.45; Y = 2.0; W = 2.15; H = 1.05; Text = "Graph.java"; Fill = 0xDCFCE7 },
        @{ X = 6.0; Y = 2.0; W = 2.65; H = 1.05; Text = "ReliabilityAnalyzer.java"; Fill = 0xFEF3C7 },
        @{ X = 9.1; Y = 1.35; W = 2.2; H = 1.0; Text = "Terminal Summary"; Fill = 0xFCE7F3 },
        @{ X = 9.1; Y = 2.75; W = 2.2; H = 1.0; Text = "HTML Visualization"; Fill = 0xEDE9FE }
    )

    foreach ($b in $boxes) {
        $shape = $slide.Shapes.AddShape(5, $b.X*72, $b.Y*72, $b.W*72, $b.H*72)
        $shape.Fill.ForeColor.RGB = $b.Fill
        $shape.Line.ForeColor.RGB = 0x94A3B8
        $shape.Line.Weight = 1.2
        $shape.TextFrame.TextRange.Text = $b.Text
        $shape.TextFrame.TextRange.Font.Name = "Aptos Display"
        $shape.TextFrame.TextRange.Font.Size = 18
        $shape.TextFrame.TextRange.Font.Color.RGB = 0x0F172A
        $shape.TextFrame.TextRange.ParagraphFormat.Alignment = 2
        $shape.TextFrame.VerticalAnchor = 3
    }

    foreach ($arrow in @(
        @{ X = 3.02; Y = 2.38; W = 0.38; H = 0.18 },
        @{ X = 5.58; Y = 2.38; W = 0.38; H = 0.18 },
        @{ X = 8.72; Y = 1.82; W = 0.3; H = 0.18 },
        @{ X = 8.72; Y = 3.2; W = 0.3; H = 0.18 }
    )) {
        $a = $slide.Shapes.AddShape(33, $arrow.X*72, $arrow.Y*72, $arrow.W*72, $arrow.H*72)
        $a.Fill.ForeColor.RGB = 0x0F766E
        $a.Line.Visible = 0
    }

    Add-FormattedTextBox -Slide $slide -Left (1.0*72) -Top (4.15*72) -Width (11.25*72) -Height (1.65*72) -Text "The project uses an efficient integer-indexed graph core, then performs one iterative Tarjan-style traversal to produce articulation points, biconnected components, graph classification, and impact metrics. Those results are consumed by both the terminal report and the browser visualization." -FontSize 17 -Color 0x334155 | Out-Null
}

function Add-TableSlide {
    param(
        [object]$Presentation,
        [string]$Title,
        [string]$Subtitle,
        [string[]]$Headers,
        [object[][]]$Rows
    )

    $slide = $Presentation.Slides.Add($Presentation.Slides.Count + 1, 12)
    Add-SlideChrome -Slide $slide -Title $Title -Subtitle $Subtitle

    $tableShape = $slide.Shapes.AddTable($Rows.Count + 1, $Headers.Count, 0.7*72, 1.7*72, 11.95*72, 4.8*72)
    $table = $tableShape.Table

    for ($c = 1; $c -le $Headers.Count; $c++) {
        $cell = $table.Cell(1, $c).Shape
        $cell.TextFrame.TextRange.Text = [string]$Headers[$c - 1]
        $cell.Fill.ForeColor.RGB = 0x0F172A
        $cell.TextFrame.TextRange.Font.Name = "Aptos Display"
        $cell.TextFrame.TextRange.Font.Size = 15
        $cell.TextFrame.TextRange.Font.Color.RGB = 0xFFFFFF
    }

    for ($r = 0; $r -lt $Rows.Count; $r++) {
        for ($c = 0; $c -lt $Headers.Count; $c++) {
            $cell = $table.Cell($r + 2, $c + 1).Shape
            $cell.TextFrame.TextRange.Text = [string]$Rows[$r][$c]
            $cell.Fill.ForeColor.RGB = $(if ($r % 2 -eq 0) { 0xFFFFFF } else { 0xF8FAFC })
            $cell.TextFrame.TextRange.Font.Name = "Aptos"
            $cell.TextFrame.TextRange.Font.Size = 14
            $cell.TextFrame.TextRange.Font.Color.RGB = 0x1E293B
        }
    }
}

function Add-CodeSlide {
    param(
        [object]$Presentation,
        [string]$Title,
        [string]$Subtitle,
        [string]$CodeText
    )

    $slide = $Presentation.Slides.Add($Presentation.Slides.Count + 1, 12)
    Add-SlideChrome -Slide $slide -Title $Title -Subtitle $Subtitle

    $codeBox = $slide.Shapes.AddShape(1, 0.7*72, 1.65*72, 12.0*72, 5.35*72)
    $codeBox.Fill.ForeColor.RGB = 0x0F172A
    $codeBox.Line.ForeColor.RGB = 0x1D4ED8
    $codeBox.Line.Weight = 1.2

    $tb = $slide.Shapes.AddTextbox(1, 0.95*72, 1.9*72, 11.5*72, 4.85*72)
    $tb.TextFrame.TextRange.Text = $CodeText
    $tb.TextFrame.TextRange.Font.Name = "Consolas"
    $tb.TextFrame.TextRange.Font.Size = 14
    $tb.TextFrame.TextRange.Font.Color.RGB = 0xE2E8F0
}
function Add-ProfessionalPresentation {
    param(
        [string]$OutputPath,
        [pscustomobject]$Context
    )

    $ppt = $null
    $presentation = $null
    try {
        $ppt = New-Object -ComObject PowerPoint.Application
        $ppt.Visible = -1
        $presentation = $ppt.Presentations.Add()

        $titleSlide = $presentation.Slides.Add(1, 12)
        $bg = $titleSlide.Shapes.AddShape(1, 0, 0, 13.333*72, 7.5*72)
        $bg.Fill.ForeColor.RGB = 0x0F172A
        $bg.Line.Visible = 0
        $band = $titleSlide.Shapes.AddShape(1, 0, 5.85*72, 13.333*72, 0.45*72)
        $band.Fill.ForeColor.RGB = 0x0F766E
        $band.Line.Visible = 0
        $circle = $titleSlide.Shapes.AddShape(9, 9.7*72, 0.95*72, 2.3*72, 2.3*72)
        $circle.Fill.ForeColor.RGB = 0x1D4ED8
        $circle.Line.Visible = 0
        $circle2 = $titleSlide.Shapes.AddShape(9, 10.65*72, 1.75*72, 1.15*72, 1.15*72)
        $circle2.Fill.ForeColor.RGB = 0xF59E0B
        $circle2.Line.Visible = 0

        Add-FormattedTextBox -Slide $titleSlide -Left (0.7*72) -Top (1.0*72) -Width (8.2*72) -Height (1.0*72) -Text "Articulation Point Analysis" -FontSize 28 -FontName "Aptos Display" -Color 0xFFFFFF -Bold | Out-Null
        Add-FormattedTextBox -Slide $titleSlide -Left (0.72*72) -Top (2.05*72) -Width (8.9*72) -Height (0.9*72) -Text "Biconnected Components, Tarjan-Style Graph Analysis, and Interactive Reliability Visualization" -FontSize 20 -FontName "Aptos" -Color 0xCBD5E1 | Out-Null
        Add-FormattedTextBox -Slide $titleSlide -Left (0.74*72) -Top (3.2*72) -Width (6.4*72) -Height (1.0*72) -Text ("Prepared by Code Crew Nexus" + "`r" + (($Context.Contributors -join ", "))) -FontSize 16 -Color 0xE2E8F0 | Out-Null
        Add-FormattedTextBox -Slide $titleSlide -Left (0.74*72) -Top (6.15*72) -Width (7.6*72) -Height (0.3*72) -Text ("Generated on " + $Context.GeneratedOn.ToString("dd MMMM yyyy")) -FontSize 11 -Color 0xF8FAFC | Out-Null

        Add-BulletSlide -Presentation $presentation -Title "Project Overview" -Subtitle "Why this DAA PBL matters" -Bullets @(
            "The project studies graph reliability through articulation points and biconnected components in undirected networks.",
            "It supports two workflows: loading a CSV edge list and generating a scale-free network using preferential attachment.",
            "The analyzer reports fragile cut vertices, robust graph blocks, bridge-like bottlenecks, and graph-level classification.",
            "The CLI and HTML visualizer together make the project useful for demos, analysis, and report-ready interpretation."
        )

        Add-TwoColumnSlide -Presentation $presentation -Title "Problem Statement And Objectives" -Subtitle "What the project solves" -LeftHeading "Problem Statement" -LeftBullets @(
            "Single-point failures can disconnect communication, transport, or dependency networks.",
            "Articulation-point-only analysis reveals weak nodes but not the stronger internal block structure.",
            "The project needed to evolve from a cut-vertex detector into a more complete reliability analyzer."
        ) -RightHeading "Objectives" -RightBullets @(
            "Detect articulation points efficiently.",
            "Extract biconnected components in the same traversal.",
            "Classify the graph and summarize structural resilience.",
            "Present the findings cleanly in terminal and visual form."
        )

        Add-TwoColumnSlide -Presentation $presentation -Title "Core Graph Theory Concepts" -Subtitle "Foundation used by the analyzer" -LeftHeading "Fragility Concepts" -LeftBullets @(
            "Connected component: a maximal set of vertices with mutual reachability.",
            "Articulation point: a vertex whose removal increases the number of connected components.",
            "Bridge-like block: a tiny biconnected component formed by a single edge, often a narrow connector."
        ) -RightHeading "Robustness Concepts" -RightBullets @(
            "Biconnected component: a maximal subgraph that stays connected after removal of any one internal vertex.",
            "A graph is biconnected when it is connected and has no articulation point.",
            "Large BCCs reveal stable regions; articulation points typically sit at boundaries between them."
        )

        Add-BulletSlide -Presentation $presentation -Title "Tarjan-Style Algorithm Intuition" -Subtitle "Why one DFS is enough" -Bullets @(
            "Each vertex gets a discovery time disc[u] and a low-link value low[u].",
            "low[u] captures the earliest discovery time reachable from u using tree edges plus at most one back edge.",
            "If low[child] >= disc[parent], the child subtree cannot bypass the parent to reach an ancestor.",
            "That same condition both signals articulation impact and tells us when to pop one biconnected component from the edge stack."
        )

        Add-ArchitectureSlide -Presentation $presentation

        Add-TwoColumnSlide -Presentation $presentation -Title "Codebase Responsibilities" -Subtitle "How each source file contributes" -LeftHeading "Core Java Files" -LeftBullets @(
            "Graph.java stores adjacency lists and maps string IDs to integer indices for performance.",
            "ReliabilityAnalyzer.java performs iterative DFS, impact tracking, BCC extraction, and classification.",
            "App.java orchestrates loading, generation, timing, output formatting, and visualization export."
        ) -RightHeading "Supporting Files" -RightBullets @(
            "NetworkSimulator.java generates scale-free graphs with guaranteed articulation-sensitive tail behavior.",
            "GraphVisualizer.java exports HTML with articulation-point highlighting and BCC color coding.",
            "README.md and docs/ now document the project, report structure, and Office output pipeline."
        )

        Add-BulletSlide -Presentation $presentation -Title "Implementation Highlights" -Subtitle "Creative upgrades added to the project" -Bullets @(
            "Biconnected components are extracted without a second traversal by maintaining an edge stack during DFS.",
            "The CLI now reports graph classification, top articulation points, top BCCs, bridge-like counts, and a brief summary line.",
            "The HTML visualizer colors edges by block membership and shows BCC-aware node tooltips for easier exploration.",
            "Repository hygiene was improved by separating source files from generated bytecode and adding structured docs."
        )

        Add-CodeSlide -Presentation $presentation -Title "Representative CLI Output" -Subtitle "Sample graph execution snapshot" -CodeText $Context.SampleOutput

        Add-TableSlide -Presentation $presentation -Title "Validation Snapshot" -Subtitle "Representative results collected from the current build" -Headers @("Scenario", "Nodes", "Edges", "Articulation Points", "BCCs", "Classification") -Rows @(
            @("Sample CSV graph", "12", "13", "4", "7", "Disconnected Graph"),
            @("Generated graph (V=20, m=2)", "20", "34", "3", "4", "Connected but not Biconnected"),
            @("Cycle sanity graph", "4", "4", "0", "1", "Biconnected Graph")
        )

        Add-TwoColumnSlide -Presentation $presentation -Title "System Configuration" -Subtitle "Practical environment for this project" -LeftHeading "Hardware / Platform" -LeftBullets @(
            "Windows 11 workstation",
            "JDK 11+ capable environment",
            "Modern browser for HTML visualization",
            "Sufficient memory for large graph traversals"
        ) -RightHeading "Software Stack" -RightBullets @(
            "Java compiler and runtime",
            "PowerShell-based CLI workflow",
            "Microsoft Office 2021 for polished report generation",
            "Optional browser-based visualization using vis-network"
        )

        Add-BulletSlide -Presentation $presentation -Title "Applications And Use Cases" -Subtitle "Where this analysis is useful" -Bullets @(
            "Network design: identify routers, servers, or edges that behave like structural bottlenecks.",
            "Infrastructure planning: reason about failure sensitivity in utility, social, or dependency graphs.",
            "Education: demonstrate articulation points, low-link values, and biconnected components with concrete output.",
            "Algorithmic experimentation: compare synthetic scale-free behavior against curated example graphs."
        )

        Add-TwoColumnSlide -Presentation $presentation -Title "Strengths, Limits, And Next Steps" -Subtitle "Current status and natural extension points" -LeftHeading "Current Strengths" -LeftBullets @(
            "Linear-time DFS analysis",
            "Iterative implementation avoids recursion overflow",
            "Rich CLI summary and visual export",
            "Cleaner documentation and reproducible Office assets"
        ) -RightHeading "Future Scope" -RightBullets @(
            "Unit tests for canonical graph families",
            "JSON and CSV export of analysis results",
            "Explicit bridge detection as a first-class report item",
            "Benchmarks on larger generated graphs"
        )

        Add-BulletSlide -Presentation $presentation -Title "Conclusion" -Subtitle "Project outcome" -Bullets @(
            "The project has moved beyond articulation-point-only analysis and now explains both failure points and robust graph structure.",
            "Biconnected-component extraction makes the DAA PBL more complete, more demonstrative, and more professionally presentable.",
            "The final deliverable now includes source code, HTML visualization, detailed README, project analysis notes, a richer PowerPoint deck, and a formal Word report."
        )

        $presentation.SaveAs($OutputPath)
        $presentation.Close()
        $ppt.Quit()
    } finally {
        Remove-ComObject $presentation
        Remove-ComObject $ppt
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}
function Add-WordParagraph {
    param(
        [object]$Selection,
        [string]$Text,
        [string]$Style = "Normal",
        [int]$Alignment = -1,
        [float]$FontSize = 12,
        [string]$FontName = "Times New Roman",
        [switch]$Bold,
        [switch]$PageBreakBefore
    )

    if ($PageBreakBefore) {
        $Selection.InsertBreak(7)
    }

    if ($Alignment -lt 0) {
        if ($Style -eq "Heading 1") {
            $Alignment = 1
        }
        elseif ($Style -eq "Heading 2") {
            $Alignment = 0
        }
        else {
            $Alignment = 3
        }
    }

    $Selection.Style = $Style
    $Selection.ParagraphFormat.Alignment = $Alignment
    $Selection.ParagraphFormat.SpaceBefore = 0
    $Selection.ParagraphFormat.SpaceAfter = 10
    if ($Style -eq "Normal") {
        $Selection.ParagraphFormat.FirstLineIndent = 18
    }
    else {
        $Selection.ParagraphFormat.FirstLineIndent = 0
    }
    $Selection.Font.Name = $FontName
    $Selection.Font.Size = $FontSize
    $Selection.Font.Bold = $(if ($Bold) { 1 } else { 0 })
    $Selection.TypeText($Text)
    $Selection.TypeParagraph()
}

function Add-WordBullets {
    param(
        [object]$Selection,
        [string[]]$Items
    )

    foreach ($item in $Items) {
        $Selection.Style = "Normal"
        $Selection.Range.ListFormat.ApplyBulletDefault()
        $Selection.TypeText($item)
        $Selection.TypeParagraph()
    }
    $Selection.Range.ListFormat.RemoveNumbers()
}

function Add-WordCodeBlock {
    param(
        [object]$Selection,
        [string]$Text
    )

    $Selection.Style = "No Spacing"
    $Selection.Font.Name = "Consolas"
    $Selection.Font.Size = 9.5
    $Selection.Shading.BackgroundPatternColor = 15921906
    $Selection.ParagraphFormat.LeftIndent = 18
    $Selection.ParagraphFormat.RightIndent = 18
    $Selection.TypeText($Text)
    $Selection.TypeParagraph()
    $Selection.Shading.BackgroundPatternColor = 16777215
    $Selection.ParagraphFormat.LeftIndent = 0
    $Selection.ParagraphFormat.RightIndent = 0
}

function Add-WordTable {
    param(
        [object]$Document,
        [object]$Selection,
        [string[]]$Headers,
        [object[][]]$Rows
    )

    $range = $Selection.Range
    $table = $Document.Tables.Add($range, $Rows.Count + 1, $Headers.Count)
    $table.Style = "Table Grid"
    $table.Range.Font.Name = "Times New Roman"
    $table.Range.Font.Size = 11

    for ($c = 1; $c -le $Headers.Count; $c++) {
        $table.Cell(1, $c).Range.Text = [string]$Headers[$c - 1]
        $table.Cell(1, $c).Range.Bold = 1
        $table.Cell(1, $c).Range.Shading.BackgroundPatternColor = 15132390
    }

    for ($r = 0; $r -lt $Rows.Count; $r++) {
        for ($c = 0; $c -lt $Headers.Count; $c++) {
            $table.Cell($r + 2, $c + 1).Range.Text = [string]$Rows[$r][$c]
        }
    }

    $Selection.MoveDown()
    $Selection.TypeParagraph()
}

function Add-WordReport {
    param(
        [string]$OutputPath,
        [pscustomobject]$Context
    )

    $word = $null
    $document = $null
    try {
        $word = New-Object -ComObject Word.Application
        $word.Visible = $false
        $document = $word.Documents.Add()
        $selection = $word.Selection

        $document.PageSetup.TopMargin = 72
        $document.PageSetup.BottomMargin = 72
        $document.PageSetup.LeftMargin = 72
        $document.PageSetup.RightMargin = 72

        Add-WordParagraph -Selection $selection -Text "Design And Analysis Of Algorithms" -Alignment 1 -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Project Based Learning (PBL) Report On" -Alignment 1 -FontSize 14 -Bold
        Add-WordParagraph -Selection $selection -Text "Articulation Point Analysis And Biconnected Component Detection In Networks" -Alignment 1 -FontSize 20 -Bold
        Add-WordParagraph -Selection $selection -Text "A Java CLI Project with Tarjan-Style Graph Analysis and HTML Visualization" -Alignment 1 -FontSize 13
        Add-WordParagraph -Selection $selection -Text "" -Alignment 1
        Add-WordParagraph -Selection $selection -Text "Submitted by" -Alignment 1 -FontSize 13 -Bold
        foreach ($contributor in $Context.Contributors) {
            Add-WordParagraph -Selection $selection -Text $contributor -Alignment 1 -FontSize 12
        }
        Add-WordParagraph -Selection $selection -Text "Project Team: Code Crew Nexus" -Alignment 1 -FontSize 12
        Add-WordParagraph -Selection $selection -Text ("Generated on " + $Context.GeneratedOn.ToString("dd MMMM yyyy")) -Alignment 1 -FontSize 12
        Add-WordParagraph -Selection $selection -Text "Submitted in partial fulfilment of the Design and Analysis of Algorithms PBL work." -Alignment 1 -FontSize 12

        $selection.InsertBreak(7)

        Add-WordParagraph -Selection $selection -Text "CONTENTS" -Alignment 1 -FontSize 16 -Bold
        $tocRange = $selection.Range
        [void]$document.TablesOfContents.Add($tocRange, $true, 1, 3)
        $selection.InsertParagraphAfter()
        $selection.InsertBreak(7)

        Add-WordParagraph -Selection $selection -Text "CERTIFICATE" -Alignment 1 -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "This is to certify that the Design and Analysis of Algorithms (PBL) report entitled ""Articulation Point Analysis And Biconnected Component Detection In Networks"" is a bonafide work carried out by the Code Crew Nexus team as part of the project-based learning activity. The work demonstrates the design, implementation, validation, and documentation of a Java-based graph analysis system that detects articulation points, extracts biconnected components, classifies graph resilience, and exports interactive visualizations." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "The report documents the problem statement, theoretical foundation, implementation strategy, validation results, and future enhancement opportunities for the project." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "" -FontSize 12
        Add-WordParagraph -Selection $selection -Text "Faculty Guide / Reviewer: __________________________" -FontSize 12
        Add-WordParagraph -Selection $selection -Text "Project Team Representative: _______________________" -FontSize 12

        $selection.InsertBreak(7)

        Add-WordParagraph -Selection $selection -Text "DECLARATION" -Alignment 1 -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "We hereby declare that this report entitled ""Articulation Point Analysis And Biconnected Component Detection In Networks"" is based on our own project work for the Design and Analysis of Algorithms PBL activity. The implementation, analysis, and conclusions presented here are specific to this repository and have been prepared to explain the project architecture, algorithms, results, and documentation pipeline." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "We further declare that this report has been prepared specifically for this DAA project and has not been copied from any other report. A separate reference document was used only to understand the expected academic report structure." -FontSize 12
        foreach ($contributor in $Context.Contributors) {
            Add-WordParagraph -Selection $selection -Text ("Signature: ____________________    " + $contributor) -FontSize 12
        }
        Add-WordParagraph -Selection $selection -Text "ABSTRACT" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "This project is a Java command-line system for network reliability analysis using graph theory. It accepts undirected graphs from CSV input or generates scale-free graphs and then applies an iterative Tarjan-style depth-first search to compute articulation points and biconnected components. The articulation-point module reveals fragile vertices whose failure disconnects the graph, while the biconnected-component module reveals maximal robust blocks that remain connected under any single internal vertex failure." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "The latest version of the project also classifies the graph as biconnected, connected-but-not-biconnected, or disconnected; computes bridge-like component counts; highlights the largest robust block; prints ranked terminal summaries; and exports an interactive HTML visualization. The result is a more complete and professionally documented DAA PBL deliverable." -FontSize 12

        Add-WordParagraph -Selection $selection -Text "INTRODUCTION" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Graphs model many real-world systems such as computer networks, road systems, social graphs, dependency graphs, and communication structures. In such systems, reliability often depends on whether the removal of a single node can disconnect important portions of the network. This makes articulation points a natural first step in reliability analysis." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "However, articulation points alone do not fully describe the internal strength of the graph. Biconnected components complement them by identifying stable blocks. This project therefore combines both viewpoints and turns a cut-vertex detector into a broader graph-structure analyzer. The implementation uses iterative DFS so that larger inputs do not suffer from recursion depth limitations." -FontSize 12

        Add-WordParagraph -Selection $selection -Text "OBJECTIVES" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordBullets -Selection $selection -Items @(
            "Detect articulation points efficiently in undirected graphs.",
            "Extract biconnected components in the same traversal.",
            "Classify the graph structurally and report robustness indicators.",
            "Support both CSV-based analysis and synthetic scale-free graph generation.",
            "Provide terminal and HTML outputs suitable for demonstration and reporting."
        )

        Add-WordParagraph -Selection $selection -Text "THEORETICAL BACKGROUND" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "An articulation point is a vertex whose removal increases the number of connected components. A biconnected component is a maximal subgraph that contains no articulation point inside the block. Tarjan-style DFS uses two core arrays: disc[u], which stores the discovery order of a node, and low[u], which stores the earliest reachable ancestor discovery time through tree and back edges." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "When a DFS child satisfies low[child] >= disc[parent], the parent acts as a structural separator for that subtree. The same condition is also used to pop one block from the DFS edge stack and therefore materialize a biconnected component. This makes articulation-point detection and BCC extraction naturally compatible in one linear-time traversal." -FontSize 12

        Add-WordParagraph -Selection $selection -Text "EXAMPLE WITH SOLUTION" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Problem Statement" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordParagraph -Selection $selection -Text "Design a graph-analysis system that can identify fragile nodes and robust blocks in an undirected network while remaining efficient enough for both curated examples and generated graphs." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "Solution Walkthrough" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordBullets -Selection $selection -Items @(
            "Load the graph from CSV or generate it using the scale-free simulator.",
            "Map vertex labels to compact integer indices for fast array-based operations.",
            "Run iterative Tarjan-style DFS to compute disc, low, subtree sizes, and articulation impact metrics.",
            "Maintain an edge stack so that every low[child] >= disc[parent] event also produces one BCC.",
            "Summarize the results in the terminal and optionally export them to an HTML visualization."
        )

        Add-WordParagraph -Selection $selection -Text "SYSTEM CONFIGURATION (H/W AND S/W)" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Hardware / Platform" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordTable -Document $document -Selection $selection -Headers @("Component", "Specification") -Rows @(
            @("Operating System", "Windows 11"),
            @("Processor", "Any modern 64-bit CPU"),
            @("Memory", "Sufficient for Java-based graph traversal and HTML rendering"),
            @("Display", "Any system capable of opening browser-based output")
        )
        Add-WordParagraph -Selection $selection -Text "Software Stack" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordTable -Document $document -Selection $selection -Headers @("Software", "Purpose") -Rows @(
            @("Java JDK 11+", "Compilation and execution"),
            @("PowerShell", "Command-line execution on Windows"),
            @("Microsoft PowerPoint 2021", "Professional presentation generation"),
            @("Microsoft Word 2021", "Formal report generation"),
            @("Web Browser", "Viewing exported HTML visualization")
        )

        Add-WordParagraph -Selection $selection -Text "CODE / IMPLEMENTATION" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Repository Structure" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordCodeBlock -Selection $selection -Text @"
articulation-point-analysis/
|- src/main/java/com/network/
|  |- App.java
|  |- Graph.java
|  |- GraphVisualizer.java
|  |- NetworkSimulator.java
|  |- ReliabilityAnalyzer.java
|- docs/
|  |- PROJECT_ANALYSIS.md
|  |- generate_office_documents.ps1
|  |- Articulation_Point_Analysis_and_Biconnected_Components.pptx
|  |- Articulation_Point_Analysis_DAA_PBL_Report.docx
|- sample_graph.csv
|- README.md
"@
        Add-WordParagraph -Selection $selection -Text "Implementation Notes" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordBullets -Selection $selection -Items @(
            "Graph.java uses adjacency lists and string-to-index mapping for efficient traversal.",
            "ReliabilityAnalyzer.java now returns articulation points, BCCs, connected-component counts, bridge-like counts, and graph classification.",
            "App.java prints ranked summaries and a final one-line brief summary in the terminal.",
            "GraphVisualizer.java colors graph blocks and highlights articulation points in the HTML export."
        )
        Add-WordParagraph -Selection $selection -Text "RESULT / OUTPUT SCREENS" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "Sample CSV Output" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordCodeBlock -Selection $selection -Text $Context.SampleOutput
        Add-WordParagraph -Selection $selection -Text "Generated Graph Output" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordCodeBlock -Selection $selection -Text $Context.GeneratedOutput
        Add-WordParagraph -Selection $selection -Text "Interpretation" -Style "Heading 2" -FontSize 13 -Bold
        Add-WordParagraph -Selection $selection -Text "The sample graph demonstrates multiple articulation points and multiple biconnected components, while the generated graph demonstrates a larger robust core with a short articulation-heavy tail. This confirms that the new BCC analysis adds valuable structure beyond cut-vertex detection alone." -FontSize 12

        Add-WordParagraph -Selection $selection -Text "CONCLUSION" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordParagraph -Selection $selection -Text "This DAA PBL project now delivers a fuller graph-reliability analysis pipeline. The earlier articulation-point implementation has been extended into a richer tool that also extracts biconnected components, classifies graph structure, reports bridge-like blocks, and exports clearer visualization. The project is therefore more complete both algorithmically and academically." -FontSize 12
        Add-WordParagraph -Selection $selection -Text "The documentation and repository were also cleaned so that generated files are separated from source code and the project now includes a polished PowerPoint deck plus a formal Word report." -FontSize 12

        Add-WordParagraph -Selection $selection -Text "REFERENCES" -Style "Heading 1" -PageBreakBefore -FontSize 16 -Bold
        Add-WordBullets -Selection $selection -Items @(
            "R. E. Tarjan, Depth-first search and linear graph algorithms.",
            "Standard graph theory references for articulation points and biconnected components.",
            "Project source code and README from the articulation-point-analysis repository."
        )

        foreach ($toc in $document.TablesOfContents) {
            $toc.Update()
        }

        foreach ($section in $document.Sections) {
            $footerRange = $section.Footers(1).Range
            $footerRange.Text = "Articulation Point Analysis | DAA PBL"
            $footerRange.ParagraphFormat.Alignment = 1
            $footerRange.Font.Name = "Times New Roman"
            $footerRange.Font.Size = 9
            [void]$section.Footers(1).PageNumbers.Add(2)
        }

        $tempPath = [System.IO.Path]::ChangeExtension($OutputPath, '.tmp.docx')
        if (Test-Path $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
        $document.SaveAs2($tempPath, 12)
        $document.Close()
        $document = $word.Documents.Open($tempPath)
        $document.SaveAs2($OutputPath, 12)
        Remove-Item -LiteralPath $tempPath -Force
        $document.Close()
        $word.Quit()
    } finally {
        Remove-ComObject $document
        Remove-ComObject $word
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}

$context = Get-ProjectContext -Root $RepoRoot
$pptPath = Join-Path $PSScriptRoot "Articulation_Point_Analysis_and_Biconnected_Components.pptx"
$docPath = Join-Path $PSScriptRoot "Articulation_Point_Analysis_DAA_PBL_Report.docx"

Add-ProfessionalPresentation -OutputPath $pptPath -Context $context
Add-WordReport -OutputPath $docPath -Context $context

Write-Output ("PowerPoint generated at: " + $pptPath)
Write-Output ("Word report generated at: " + $docPath)





