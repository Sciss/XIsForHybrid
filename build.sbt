lazy val baseName       = "XIsForHybrid"
lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"

lazy val root = project.in(file("."))
  .settings(
    name         := baseName,
    description  := "Art book contribution",
    version      := projectVersion,
    homepage     := Some(url(s"https://github.com/Sciss/$baseName")),
    licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    scalaVersion := "3.0.1",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    libraryDependencies ++= Seq(
      ("de.sciss"   %% "cluster"          % deps.main.cluster).cross(CrossVersion.for3Use2_13), // graph clustering
      "de.sciss"    %% "fileutil"         % deps.main.fileUtil,       // utility functions
      "de.sciss"    %% "fscape-lucre"     % deps.main.fscape,         // DSP
      "de.sciss"    %% "linkernighantsp"  % deps.main.linKernighan,   // TSP algorithm
      "de.sciss"    %% "lucre-expr"       % deps.main.lucre,          // control programs
      "de.sciss"    %% "lucre-bdb"        % deps.main.lucre,          // data store
      "de.sciss"    %% "kollflitz"        % deps.main.kollFlitz,      // collection utilities
      "de.sciss"    %  "neuralgas-core"   % deps.main.neuralGas,      // topological learning
      "de.sciss"    %% "numbers"          % deps.main.numbers,        // numeric utilities
      "de.sciss"    %% "scalacollider"    % deps.main.scalaCollider,  // curve segments
      "de.sciss"    %% "serial"           % deps.main.serial,         // Serialization
      "org.rogach"  %% "scallop"          % deps.main.scallop,        // command line option parsing
    ),
  )

lazy val deps = new {
  lazy val main = new {
    val cluster       = "0.1.0"
    val fileUtil      = "1.1.5"
    val fscape        = "3.7.1"
    val kollFlitz     = "0.2.4"
    val linKernighan  = "0.1.3"
    val lucre         = "4.4.5"
    val neuralGas     = "2.4.0"
    val numbers       = "0.2.1"
    val scalaCollider = "2.6.4"
    val scallop       = "4.0.3"
    val serial        = "2.0.1"
  }
}
