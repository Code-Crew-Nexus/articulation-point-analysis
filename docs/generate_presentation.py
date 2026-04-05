from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from xml.sax.saxutils import escape
import zipfile


SLIDE_W = 12_192_000
SLIDE_H = 6_858_000

HEADER_COLOR = "0F172A"
ACCENT_COLOR = "0F766E"
ACCENT_ALT = "2563EB"
TEXT_COLOR = "1E293B"
MUTED_COLOR = "475569"
LIGHT_BG = "F8FAFC"


def shape_rect(shape_id: int, name: str, x: int, y: int, cx: int, cy: int, color: str) -> str:
    return f"""
    <p:sp>
      <p:nvSpPr>
        <p:cNvPr id="{shape_id}" name="{escape(name)}"/>
        <p:cNvSpPr/>
        <p:nvPr/>
      </p:nvSpPr>
      <p:spPr>
        <a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>
        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
        <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
        <a:ln><a:noFill/></a:ln>
      </p:spPr>
      <p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
    </p:sp>
    """.strip()


def paragraphs_xml(lines: list[str], size: int, color: str, bold_first: bool = False) -> str:
    paragraphs = []
    for index, line in enumerate(lines):
        bold_attr = ' b="1"' if (bold_first and index == 0) else ""
        paragraphs.append(
            f"""
            <a:p>
              <a:r>
                <a:rPr lang="en-US" sz="{size}"{bold_attr}>
                  <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
                  <a:latin typeface="Aptos"/>
                </a:rPr>
                <a:t>{escape(line)}</a:t>
              </a:r>
              <a:endParaRPr lang="en-US" sz="{size}"/>
            </a:p>
            """.strip()
        )
    return "\n".join(paragraphs)


def shape_text(
    shape_id: int,
    name: str,
    x: int,
    y: int,
    cx: int,
    cy: int,
    lines: list[str],
    size: int,
    color: str,
    *,
    bold_first: bool = False,
    center: bool = False,
) -> str:
    anchor = "ctr" if center else "t"
    align = '<a:pPr algn="ctr"/>' if center else ""
    paragraphs = []
    for index, line in enumerate(lines):
        bold_attr = ' b="1"' if (bold_first and index == 0) else ""
        paragraphs.append(
            f"""
            <a:p>
              {align}
              <a:r>
                <a:rPr lang="en-US" sz="{size}"{bold_attr}>
                  <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
                  <a:latin typeface="Aptos"/>
                </a:rPr>
                <a:t>{escape(line)}</a:t>
              </a:r>
              <a:endParaRPr lang="en-US" sz="{size}"/>
            </a:p>
            """.strip()
        )

    joined = "\n".join(paragraphs)
    return f"""
    <p:sp>
      <p:nvSpPr>
        <p:cNvPr id="{shape_id}" name="{escape(name)}"/>
        <p:cNvSpPr txBox="1"/>
        <p:nvPr/>
      </p:nvSpPr>
      <p:spPr>
        <a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>
        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
        <a:noFill/>
        <a:ln><a:noFill/></a:ln>
      </p:spPr>
      <p:txBody>
        <a:bodyPr wrap="square" anchor="{anchor}"/>
        <a:lstStyle/>
        {joined}
      </p:txBody>
    </p:sp>
    """.strip()


def make_slide_xml(title: str, body_lines: list[str], footer: str) -> str:
    shapes = [
        shape_rect(2, "Header", 0, 0, SLIDE_W, 900_000, HEADER_COLOR),
        shape_rect(3, "Footer", 0, SLIDE_H - 220_000, SLIDE_W, 220_000, ACCENT_COLOR),
        shape_text(4, "Title", 500_000, 160_000, SLIDE_W - 1_000_000, 520_000, [title], 2600, "FFFFFF", bold_first=True),
        shape_text(5, "Body", 720_000, 1_220_000, SLIDE_W - 1_440_000, 4_800_000, body_lines, 1800, TEXT_COLOR),
        shape_text(6, "FooterText", 720_000, SLIDE_H - 215_000, SLIDE_W - 1_440_000, 150_000, [footer], 900, "FFFFFF"),
    ]
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg>
      <p:bgPr>
        <a:solidFill><a:srgbClr val="{LIGHT_BG}"/></a:solidFill>
        <a:effectLst/>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="0" cy="0"/>
          <a:chOff x="0" y="0"/>
          <a:chExt cx="0" cy="0"/>
        </a:xfrm>
      </p:grpSpPr>
      {' '.join(shapes)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>
"""


def slide_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1"
    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout"
    Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>
"""


def build_deck(slides: list[tuple[str, list[str], str]]) -> dict[str, str]:
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    slide_overrides = "\n".join(
        f'  <Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, len(slides) + 1)
    )
    slide_rel_entries = "\n".join(
        f'    <Relationship Id="rId{i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        for i in range(1, len(slides) + 1)
    )
    slide_id_entries = "\n".join(
        f'    <p:sldId id="{255 + i}" r:id="rId{i + 1}"/>'
        for i in range(1, len(slides) + 1)
    )

    files = {
        "[Content_Types].xml": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/>
  <Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/>
  <Override PartName="/ppt/tableStyles.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
{slide_overrides}
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
""",
        "_rels/.rels": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
""",
        "docProps/app.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
            xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Office PowerPoint</Application>
  <PresentationFormat>Widescreen</PresentationFormat>
  <Slides>8</Slides>
  <Notes>0</Notes>
  <HiddenSlides>0</HiddenSlides>
  <MMClips>0</MMClips>
  <ScaleCrop>false</ScaleCrop>
  <HeadingPairs>
    <vt:vector size="2" baseType="variant">
      <vt:variant><vt:lpstr>Theme</vt:lpstr></vt:variant>
      <vt:variant><vt:i4>1</vt:i4></vt:variant>
    </vt:vector>
  </HeadingPairs>
  <TitlesOfParts>
    <vt:vector size="1" baseType="lpstr">
      <vt:lpstr>Office Theme</vt:lpstr>
    </vt:vector>
  </TitlesOfParts>
  <Company>Code Crew Nexus</Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>
""",
        "docProps/core.xml": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                   xmlns:dc="http://purl.org/dc/elements/1.1/"
                   xmlns:dcterms="http://purl.org/dc/terms/"
                   xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Articulation Point Analysis and Biconnected Components</dc:title>
  <dc:subject>Network Reliability Analysis</dc:subject>
  <dc:creator>OpenAI Codex</dc:creator>
  <cp:keywords>graph theory, articulation points, biconnected components, Tarjan, network reliability</cp:keywords>
  <dc:description>Project presentation generated for the articulation-point-analysis repository.</dc:description>
  <cp:lastModifiedBy>OpenAI Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified>
</cp:coreProperties>
""",
        "ppt/presentation.xml": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rId1"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>
{slide_id_entries}
  </p:sldIdLst>
  <p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>
""",
        "ppt/_rels/presentation.xml.rels": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
{slide_rel_entries}
</Relationships>
""",
        "ppt/presProps.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentationPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                  xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>
""",
        "ppt/viewProps.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:viewPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
          xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
          lastView="sldView">
  <p:normalViewPr/>
  <p:slideViewPr/>
  <p:notesTextViewPr/>
  <p:gridSpacing cx="780288" cy="780288"/>
</p:viewPr>
""",
        "ppt/tableStyles.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:tblStyleLst xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" def="{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}"/>
""",
        "ppt/slideMasters/slideMaster1.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld name="Clean Master">
    <p:bg>
      <p:bgPr>
        <a:solidFill><a:srgbClr val="F8FAFC"/></a:solidFill>
        <a:effectLst/>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="0" cy="0"/>
          <a:chOff x="0" y="0"/>
          <a:chExt cx="0" cy="0"/>
        </a:xfrm>
      </p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst>
    <p:sldLayoutId id="2147483649" r:id="rId1"/>
  </p:sldLayoutIdLst>
  <p:txStyles>
    <p:titleStyle/>
    <p:bodyStyle/>
    <p:otherStyle/>
  </p:txStyles>
</p:sldMaster>
""",
        "ppt/slideMasters/_rels/slideMaster1.xml.rels": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>
""",
        "ppt/slideLayouts/slideLayout1.xml": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
             type="blank" preserve="1">
  <p:cSld name="Blank">
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="0" cy="0"/>
          <a:chOff x="0" y="0"/>
          <a:chExt cx="0" cy="0"/>
        </a:xfrm>
      </p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>
""",
        "ppt/slideLayouts/_rels/slideLayout1.xml.rels": """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>
""",
        "ppt/theme/theme1.xml": f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Network Reliability Theme">
  <a:themeElements>
    <a:clrScheme name="Network Reliability">
      <a:dk1><a:srgbClr val="{HEADER_COLOR}"/></a:dk1>
      <a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="{TEXT_COLOR}"/></a:dk2>
      <a:lt2><a:srgbClr val="{LIGHT_BG}"/></a:lt2>
      <a:accent1><a:srgbClr val="{ACCENT_COLOR}"/></a:accent1>
      <a:accent2><a:srgbClr val="{ACCENT_ALT}"/></a:accent2>
      <a:accent3><a:srgbClr val="EA580C"/></a:accent3>
      <a:accent4><a:srgbClr val="7C3AED"/></a:accent4>
      <a:accent5><a:srgbClr val="DC2626"/></a:accent5>
      <a:accent6><a:srgbClr val="{MUTED_COLOR}"/></a:accent6>
      <a:hlink><a:srgbClr val="2563EB"/></a:hlink>
      <a:folHlink><a:srgbClr val="7C3AED"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Clean Sans">
      <a:majorFont>
        <a:latin typeface="Aptos Display"/>
        <a:ea typeface=""/>
        <a:cs typeface=""/>
      </a:majorFont>
      <a:minorFont>
        <a:latin typeface="Aptos"/>
        <a:ea typeface=""/>
        <a:cs typeface=""/>
      </a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="Clean Format">
      <a:fillStyleLst>
        <a:solidFill><a:schemeClr val="lt1"/></a:solidFill>
        <a:solidFill><a:schemeClr val="accent1"/></a:solidFill>
        <a:solidFill><a:schemeClr val="accent2"/></a:solidFill>
      </a:fillStyleLst>
      <a:lnStyleLst>
        <a:ln w="9525" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="dk1"/></a:solidFill></a:ln>
        <a:ln w="25400" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="accent1"/></a:solidFill></a:ln>
        <a:ln w="38100" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="accent2"/></a:solidFill></a:ln>
      </a:lnStyleLst>
      <a:effectStyleLst>
        <a:effectStyle><a:effectLst/></a:effectStyle>
        <a:effectStyle><a:effectLst/></a:effectStyle>
        <a:effectStyle><a:effectLst/></a:effectStyle>
      </a:effectStyleLst>
      <a:bgFillStyleLst>
        <a:solidFill><a:schemeClr val="lt2"/></a:solidFill>
        <a:solidFill><a:schemeClr val="lt1"/></a:solidFill>
        <a:solidFill><a:schemeClr val="accent1"/></a:solidFill>
      </a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
  <a:objectDefaults/>
  <a:extraClrSchemeLst/>
</a:theme>
""",
    }

    for index, (title, lines, footer) in enumerate(slides, start=1):
        files[f"ppt/slides/slide{index}.xml"] = make_slide_xml(title, lines, footer)
        files[f"ppt/slides/_rels/slide{index}.xml.rels"] = slide_rels()

    return files


def write_presentation(output_path: Path) -> None:
    slides = [
        (
            "Articulation Point Analysis",
            [
                "Project focus: network reliability using articulation points and biconnected components",
                "Language and platform: Java CLI with optional HTML visualization",
                "Current deliverable: faster structural insight into fragile and robust graph regions",
                "Repository now includes cleaner build hygiene, richer summaries, and project documentation",
            ],
            "Code Crew Nexus | DAA PBL",
        ),
        (
            "Why This Project Matters",
            [
                "Reliability analysis answers a simple question: which parts of a network are dangerous single points of failure?",
                "Articulation points expose fragile vertices whose removal disconnects connectivity.",
                "Biconnected components reveal robust blocks where no single vertex can break the whole subgraph.",
                "Together, they support better reasoning for network design, debugging, and teaching graph theory.",
            ],
            "Problem framing and motivation",
        ),
        (
            "Graph Theory Foundations",
            [
                "Articulation point: a vertex whose removal increases the number of connected components.",
                "Biconnected component: a maximal subgraph with no articulation vertex inside the block.",
                "Bridge-like block: a tiny BCC made from a single edge, often revealing a narrow connection path.",
                "Classification used by the CLI: Biconnected Graph, Connected but not Biconnected, or Disconnected Graph.",
            ],
            "Core concepts used in the analyzer",
        ),
        (
            "Project Architecture",
            [
                "Graph.java stores adjacency lists with internal integer indexing for efficient traversal.",
                "ReliabilityAnalyzer.java performs iterative Tarjan-style DFS and now extracts BCCs with an edge stack.",
                "App.java handles loading, generation, execution timing, and terminal summaries.",
                "GraphVisualizer.java exports an interactive HTML file where articulation points and blocks are visually separated.",
                "NetworkSimulator.java generates scale-free graphs to mimic real network behavior.",
            ],
            "How the codebase is organized",
        ),
        (
            "Algorithm Design",
            [
                "Traversal uses iterative DFS to avoid recursion limits on larger graphs.",
                "disc[] and low[] preserve classic Tarjan logic in O(V + E) time.",
                "subtree metrics are reused to compute articulation-point impact without extra graph traversals.",
                "An edge stack is popped whenever low[child] >= disc[parent], materializing one biconnected component.",
                "The final result also reports connected components, bridge-like blocks, and largest robust block size.",
            ],
            "Implementation strategy",
        ),
        (
            "User Experience Improvements",
            [
                "Terminal output now includes graph classification, top articulation points, top BCCs, and a brief final summary.",
                "Visualization colors edges and nodes by block membership while keeping articulation points highlighted in red.",
                "Tooltips reveal BCC memberships and primary block assignment for each node.",
                "Documentation now includes a dedicated docs folder plus a project analysis note and this presentation deck.",
            ],
            "What changed in this iteration",
        ),
        (
            "Validation Snapshot",
            [
                "Sample CSV result: 12 nodes, 13 edges, 4 articulation points, 7 biconnected components, disconnected graph.",
                "Generated scale-free test: large robust core with a short articulation-heavy tail.",
                "Cycle sanity check: 4 nodes, 0 articulation points, 1 biconnected component, correctly classified as biconnected.",
                "Compile path was rechecked after cleanup to ensure the project still builds cleanly from source.",
            ],
            "Verification and confidence",
        ),
        (
            "Recommended Next Steps",
            [
                "Add small canonical unit tests for paths, cycles, trees, cliques, and disconnected graphs.",
                "Export JSON or CSV summaries for automated evaluation.",
                "Add first-class bridge detection and ranking to complement BCC reporting.",
                "Benchmark larger generated graphs and compare iterative performance characteristics.",
            ],
            "Future-ready roadmap",
        ),
    ]

    files = build_deck(slides)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for relative_path, content in files.items():
            archive.writestr(relative_path, content)


if __name__ == "__main__":
    target = Path(__file__).resolve().parent / "Articulation_Point_Analysis_and_Biconnected_Components.pptx"
    write_presentation(target)
    print(f"Presentation generated at: {target}")
