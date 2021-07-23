/*
 *  Neural.scala
 *  (X is for Hybrid)
 *
 *  Copyright (c) 2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.xisforhybrid

import de.sciss.file.*
import de.sciss.kollflitz.Ops.*
import de.sciss.neuralgas.{Algorithm, ComputeGNG, EdgeGNG, ImagePD, NodeGNG, PD}
import de.sciss.synth.Curve
import de.sciss.{kollflitz, neuralgas, numbers}

import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, RenderingHints}
import java.io.{BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, FileInputStream, FileOutputStream}
import javax.imageio.ImageIO
import scala.collection.immutable.IndexedSeq as Vec
import scala.language.implicitConversions

object Neural:
  val baseDir = file("/data/projects/BookOfX")

  def main(args: Array[String]): Unit =
    for idx <- 1 to 505 do
      val c = Config(
        imgInTemp   = baseDir / "sonograms" / "kreuzen-%d-sono.png",
        imgOutTemp  = baseDir / "render"    / "kreuzen-%d-sono-gng.png",
        gngOutTemp  = baseDir / "gng"       / "kreuzen-%d-sono.gng",
        invertPD    = true,
        maxNodes    = 5000,
        gngStepSize = 500,  // 500 is maximum
        gngUtility  = 5.0,
        gngLambda   = 200,
        gngEdgeAge  = 800,
        gngEpsilon  = 1.0e-3,
        gngEpsilon2 = 1.0e-4,
        gngAlpha    = 0.8,
        gngBeta     = 5.0e-6,
        startInIdx  = idx,
        endInIdx    = idx,
        startOutIdx = idx,
        rngSeed     = idx,
        repeat      = 5000,
        frameStep   = 1,
      )
      val t1 = System.currentTimeMillis()
      run(c)
      val t2 = System.currentTimeMillis()
      println(s" for index $idx, took ${(t2-t1+500)/1000} s.")

  def formatTemplate(temp: File, n: Int): File =
    temp.replaceName(temp.name.format(n))

  case class Segment(targetFrame: Int, targetLevel: Double, curve: Curve = Curve.linear):
    require (targetFrame > 0)

  object Envelope:
    given constant: Conversion[Double , Envelope] = Envelope(_, Vector.empty)
    given fromInt : Conversion[Int    , Envelope] = Envelope(_, Vector.empty)

    /*
        constant:
          3.4

        default curve:
          3.4;10000,4.0

        custom curve:
          3.4;10000,4.0,lin
          3.4;10000,4.0,linear
          3.4;10000,4.0,-2

     */

//    implicit object Read extends scopt.Read[Envelope] {
//      def arity = 1
//
//      def reads: String => Envelope = { s =>
//        val arr         = s.split(';')
//        val startLevel  = arr(0).toDouble
//        val segB        = Vector.newBuilder[Segment]
//        for (i <- 1 until arr.length) {
//          val segArr = arr(i).split(',')
//          val tgtFrame  = segArr(0).toInt
//          val tgtLevel  = segArr(1).toDouble
//          val curve     = segArr.length match {
//            case 2 => Curve.linear
//            case 3 => segArr(2).toLowerCase match {
//              case "step"                 => Curve.step
//              case "lin" | "linear"       => Curve.linear
//              case "exp" | "exponential"  => Curve.exponential
//              case "sine"                 => Curve.sine
//              case "welch"                => Curve.welch
//              case "squared"              => Curve.squared
//              case "cubed"                => Curve.cubed
//              case x                      => Curve.parametric(x.toFloat)
//            }
//            case _ => throw new IllegalArgumentException(s"Invalid envelope segment: ${arr(i)}")
//          }
//          segB += Segment(targetFrame = tgtFrame, targetLevel = tgtLevel, curve = curve)
//        }
//        Envelope(startLevel = startLevel, segments = segB.result())
//      }
//    }

  case class Envelope(startLevel: Double, segments: Vec[Segment]):
    override def toString: String =
      if (segments.isEmpty) startLevel.toFloat.toString
      else s"$productPrefix($startLevel, ${segments.mkString("Vec(", ", ", ")")})"

    private[this] val pairs = (Segment(targetFrame = 1, targetLevel = startLevel) +: segments).mapPairs { (s1, s2) =>
      (s1.targetLevel, s2.targetFrame - s1.targetFrame, s2)
    }

    private[this] val frameAccum =
      (0 +: segments.map(_.targetFrame)).toArray

    private[this] val numFrames   = if (frameAccum.isEmpty) 0           else frameAccum.last
    private[this] val lastLevel   = if (segments  .isEmpty) startLevel  else segments.last.targetLevel

    def levelAt(frame: Int): Double =
      if frame <= 0 then startLevel else if (frame >= numFrames) then lastLevel
      else
        val i = java.util.Arrays.binarySearch(frameAccum, frame)
        val j = if (i < 0) -(i + 2) else i
        val (startLevel, segFrames, seg) = pairs(j)
        val pos = (frame - frameAccum(j)).toDouble / segFrames
        seg.curve.levelAt(pos = pos.toFloat, y1 = startLevel.toFloat, y2 = seg.targetLevel.toFloat)

  end Envelope

  final val GNG_COOKIE = 0x474E4701   // "GNG\1"

  def readGNG(c: ComputeGNG, fIn: File): Unit =
    val fis = new FileInputStream(fIn)
    try
      val dis = new DataInputStream(new BufferedInputStream(fis))
      val cookie = dis.readInt    ()
      require (cookie == GNG_COOKIE, s"Unexpected cookie ${cookie.toHexString} != ${GNG_COOKIE.toHexString}")
      c.algorithm           = Algorithm.values.apply(dis.readInt())
      c.alphaGNG            = dis.readFloat  ()
      c.autoStopB           = dis.readBoolean()
      c.setBetaGNG           (dis.readFloat  ())
      c.delEdge_f           = dis.readFloat  ()
      c.delEdge_i           = dis.readFloat  ()
      c.e_f                 = dis.readFloat  ()
      c.e_i                 = dis.readFloat  ()
      c.epsilon             = dis.readFloat  ()
      c.epsilonGNG          = dis.readFloat  ()
      c.epsilonGNG2         = dis.readFloat  ()
      c.errorBestLBG_U      = dis.readFloat  ()
      c.fineTuningB         = dis.readBoolean()
      //      c.fineTuningS
      c.gridWidth           = dis.readInt    ()
      c.GNG_U_B             = dis.readBoolean()
      c.gridHeight          = dis.readInt    ()
      c.l_f                 = dis.readFloat  ()
      c.l_i                 = dis.readFloat  ()
      c.lambdaGNG           = dis.readInt    ()
      c.LBG_U_B             = dis.readBoolean()
      c.maxEdgeAge          = dis.readInt    ()
      c.maxNodes            = dis.readInt    ()
      c.maxYGG              = dis.readInt    ()
      c.nNodesChangedB      = dis.readBoolean()
      c.noNewNodesGGB       = dis.readBoolean()
      c.noNewNodesGNGB      = dis.readBoolean()
      c.numDiscreteSignals  = dis.readInt    ()
      c.numSignals          = dis.readInt    ()
      c.panelWidth          = dis.readInt    ()
      c.panelHeight         = dis.readInt    ()
      c.readyLBG_B          = dis.readBoolean()
      c.rndInitB            = dis.readBoolean()
      c.sigma               = dis.readFloat  ()
      c.sigma_f             = dis.readFloat  ()
      c.sigma_i             = dis.readFloat  ()
      c.SignalX             = dis.readFloat  ()
      c.SignalY             = dis.readFloat  ()
      c.stepSize            = dis.readInt    ()
      c.t_max               = dis.readFloat  ()
      c.torusGGB            = dis.readBoolean()
      c.torusSOMB           = dis.readBoolean()
      c.utilityGNG          = dis.readFloat  ()
      c.valueGraph          = dis.readFloat  ()
      c.variableB           = dis.readBoolean()

      c.nNodes = dis.readInt()
      var i = 0
      while i < c.nNodes do
        val n = new NodeGNG
        n.error                   = dis.readFloat  ()
        n.hasMoved                = dis.readBoolean()
        n.isMostRecentlyInserted  = dis.readBoolean()
        n.isWinner                = dis.readBoolean()
        n.isSecond                = dis.readBoolean()
        n.sqrDist                 = dis.readFloat  ()
        n.tau                     = dis.readFloat  ()
        n.utility                 = dis.readFloat  ()
        n.x                       = dis.readFloat  ()
        n.y                       = dis.readFloat  ()
        n.x_grid                  = dis.readInt    ()
        n.y_grid                  = dis.readInt    ()

        val nn = dis.readInt()
        var j = 0
        while j < nn do
          val ni = dis.readInt()
          n.addNeighbor(ni)
          j += 1

        c.nodes(i) = n
        i += 1

      c.nEdges = dis.readInt()
      i = 0
      while i < c.nEdges do
        val e = new EdgeGNG
        e.from  = dis.readInt()
        e.to    = dis.readInt()
        e.age   = dis.readInt()

        c.edges(i) = e
        i += 1

    finally
      fis.close()

  end readGNG

  def writeGNG(c: ComputeGNG, fOut: File): Unit =
    val fos = new FileOutputStream(fOut)
    try
      val dos = new DataOutputStream(new BufferedOutputStream(fos))
      dos writeInt      GNG_COOKIE
      dos writeInt      c.algorithm.ordinal()
      dos writeFloat    c.alphaGNG
      dos writeBoolean  c.autoStopB
      dos writeFloat    c.getBetaGNG
      dos writeFloat    c.delEdge_f
      dos writeFloat    c.delEdge_i
      dos writeFloat    c.e_f
      dos writeFloat    c.e_i
      dos writeFloat    c.epsilon
      dos writeFloat    c.epsilonGNG
      dos writeFloat    c.epsilonGNG2
      dos writeFloat    c.errorBestLBG_U
      dos writeBoolean  c.fineTuningB
      //      c.fineTuningS
      dos writeBoolean  c.GNG_U_B
      dos writeInt      c.gridWidth
      dos writeInt      c.gridHeight
      dos writeFloat    c.l_f
      dos writeFloat    c.l_i
      dos writeInt      c.lambdaGNG
      dos writeBoolean  c.LBG_U_B
      dos writeInt      c.maxEdgeAge
      dos writeInt      c.maxNodes
      dos writeInt      c.maxYGG
      dos writeBoolean  c.nNodesChangedB
      dos writeBoolean  c.noNewNodesGGB
      dos writeBoolean  c.noNewNodesGNGB
      dos writeInt      c.numDiscreteSignals
      dos writeInt      c.numSignals
      dos writeInt      c.panelWidth
      dos writeInt      c.panelHeight
      dos writeBoolean  c.readyLBG_B
      dos writeBoolean  c.rndInitB
      dos writeFloat    c.sigma
      dos writeFloat    c.sigma_f
      dos writeFloat    c.sigma_i
      dos writeFloat    c.SignalX
      dos writeFloat    c.SignalY
      dos writeInt      c.stepSize
      dos writeFloat    c.t_max
      dos writeBoolean  c.torusGGB
      dos writeBoolean  c.torusSOMB
      dos writeFloat    c.utilityGNG
      dos writeFloat    c.valueGraph
      dos writeBoolean  c.variableB

      dos.writeInt(c.nNodes)
      var i = 0
      while i < c.nNodes do
        val n = c.nodes(i)
        dos writeFloat    n.error
        dos writeBoolean  n.hasMoved
        dos writeBoolean  n.isMostRecentlyInserted
        dos writeBoolean  n.isWinner
        dos writeBoolean  n.isSecond
        dos writeFloat    n.sqrDist
        dos writeFloat    n.tau
        dos writeFloat    n.utility
        dos writeFloat    n.x
        dos writeFloat    n.y
        dos writeInt      n.x_grid
        dos writeInt      n.y_grid
        val nn = n.numNeighbors
        dos writeInt      nn
        var j = 0
        while (j < nn) do
          dos writeInt    n.neighbor.apply(j)
          j += 1

        i += 1

      dos.writeInt(c.nEdges)
      i = 0
      while i < c.nEdges do
        val e = c.edges(i)
        dos writeInt e.from
        dos writeInt e.to
        dos writeInt e.age
        i += 1

      dos.flush()

    finally
      fos.close()

  end writeGNG

  case class Config(
                     imgInTemp      : File      = file("in-%d.png"),
                     imgOutTemp     : File      = file("out-%d.png"),
                     gngOutTemp     : File      = null,
                     widthOut       : Int       = 0,
                     heightOut      : Int       = 0,
                     invertPD       : Boolean   = false,
                     invertImgOut   : Boolean   = false,
                     strokeWidth    : Double    = 2.0,
                     gngStepSize    : Envelope  = 27,
                     gngLambda      : Envelope  = 27,
                     gngEdgeAge     : Envelope  = 108,
                     gngEpsilon     : Envelope  = 0.05,
                     gngEpsilon2    : Envelope  = 1.0e-4,
                     gngAlpha       : Envelope  = 0.2,
                     gngBeta        : Envelope  = 5.0e-6,
                     gngUtility     : Envelope  = 18.0,
                     rngSeed        : Int       = 0xBEE,
                     maxNodes       : Envelope  = 4000,
                     startInIdx     : Int       = 1,
                     endInIdx       : Int       = -1,
                     startOutIdx    : Int       = 1,
                     skipFrames     : Int       = 0,
                     frameStep      : Int       = 10,
                     holdFirst      : Int       = 0,
                     repeat         : Int       = 1,
                   ):
    def formatImgIn (frame: Int): File = formatTemplate(imgInTemp , frame)
    def formatImgOut(frame: Int): File = formatTemplate(imgOutTemp, frame)
    def formatGNGOut(frame: Int): File = formatTemplate(gngOutTemp, frame)

  case class Edge(from: Int, to: Int)

  def renderImage(config: Config, c: ComputeGNG, fImgOut: File, quiet: Boolean = false): Unit =
    val wIn     = c.panelWidth
    val hIn     = c.panelHeight
    val wOut    = if (config.widthOut  == 0) wIn else config.widthOut
    val hOut    = if (config.heightOut == 0) hIn else config.heightOut
    val sx      = wOut.toDouble / wIn
    val sy      = hOut.toDouble / hIn
    val img     = new BufferedImage(wOut, hOut, BufferedImage.TYPE_BYTE_GRAY)
    val g       = img.createGraphics()
    val colrBg  = if (config.invertImgOut) Color.black else Color.white
    val colrFg  = if (config.invertImgOut) Color.white else Color.black
    g.setColor(colrBg)
    g.fillRect(0, 0, wOut, hOut)
    if (sx != 1.0 || sy != 1.0) g.scale(sx, sy)
    g.setColor(colrFg)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON   )
    g.setRenderingHint(RenderingHints.KEY_RENDERING     , RenderingHints.VALUE_RENDER_QUALITY )
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE    )
    g.setStroke(new BasicStroke(config.strokeWidth.toFloat))
    val ln = new Line2D.Double()
    var i = 0
    while i < c.nEdges do
      val e = c.edges(i)
      val nFrom = c.nodes(e.from)
      val nTo   = c.nodes(e.to  )
      ln.setLine(nFrom.x, nFrom.y, nTo.x, nTo.y)
      g.draw(ln)
      i += 1

    g.dispose()
    val fmt   = if fImgOut.extL == "png" then "png" else "jpg"
    ImageIO.write(img, fmt, fImgOut)
    if (!quiet) println(s"Wrote ${fImgOut.name}")
  end renderImage

  case class Point2D(x: Float, y: Float)

  case class ResGNG(surfaceWidthPx: Int, surfaceHeightPx: Int, nodes: Vec[Point2D], edges: Vec[Edge])

  def run(config: Config): Unit =
    import config.*

    val c = new ComputeGNG

    require (formatImgIn(startInIdx).isFile)
    require (formatImgIn (0) != formatImgIn (1), "Input  image template does not specify frame index")
    require (formatImgOut(0) != formatImgOut(1), "Output image template does not specify frame index")
    require (formatGNGOut(0) != formatGNGOut(1), "Output GNG   template does not specify frame index")
    val endInIdx1       = if endInIdx >= 0 then endInIdx else
      val numImages = Iterator.from(startInIdx).indexWhere { idx =>
        val f = formatImgIn(idx)
        !f.isFile
      }
      startInIdx + numImages - 1

    val frameInIndices0 = Vector.fill(holdFirst)(startInIdx) ++ (startInIdx to endInIdx1) :+ endInIdx1
    val frameInIndices  = if holdFirst == 0 then frameInIndices0 else Vector.fill(holdFirst)(startInIdx) ++ frameInIndices0
    var frameOutIdx     = startOutIdx
    var frameOutCnt     = 0
    val numOutFrames    = (frameInIndices.size - 1) * frameStep
    val numOutRep       = numOutFrames.toLong * repeat
    println(s"numOutFrames = $numOutFrames")

    def fillParams(frame: Int): Unit =
      val vBeta     = gngBeta     .levelAt(frame).toFloat
      val vUtility  = gngUtility  .levelAt(frame).toFloat
      val vMaxNodes = maxNodes    .levelAt(frame).round.toInt
      val vStepSize = gngStepSize .levelAt(frame).round.toInt
      val vLambda   = gngLambda   .levelAt(frame).round.toInt
      val vEdgeAge  = gngEdgeAge  .levelAt(frame).round.toInt
      val vEpsilon  = gngEpsilon  .levelAt(frame).toFloat
      val vEpsilon2 = gngEpsilon2 .levelAt(frame).toFloat
      val vAlpha    = gngAlpha    .levelAt(frame).toFloat

      import numbers.Implicits.*

      c.maxNodes        = math.max(2, vMaxNodes)
      c.stepSize        = math.max(1, vStepSize)
      c.algorithm       = neuralgas.Algorithm.GNGU
      c.lambdaGNG       = math.max(1, vLambda)
      c.maxEdgeAge      = math.max(1, vEdgeAge)
      c.epsilonGNG      = vEpsilon .clip(0f, 1f)
      c.epsilonGNG2     = vEpsilon2.clip(0f, 1f)
      c.alphaGNG        = vAlpha   .clip(0f, 1f)
      c.setBetaGNG(vBeta.clip(0f, 1f))
      c.noNewNodesGNGB  = false
      c.GNG_U_B         = true
      c.utilityGNG      = vUtility
      c.autoStopB       = false

    fillParams(0)
    c.reset()
    val rnd = c.getRNG
    rnd.setSeed(rngSeed)
    c.addNode(null)
    c.addNode(null)

    var imgFloorIdx             = -1
    var imgCeilIdx              = -1
    var imgFloor: BufferedImage = null
    var imgCeil : BufferedImage = null
    var pdFloor : PD            = null
    var pdCeil  : PD            = null

    val res           = new ComputeGNG.Result
    var cValid        = true

    var lastProgress  = 0
    println("_" * 100)
    frameInIndices.foreachPair { (frameInFloor, frameInCeil) =>
      if imgFloorIdx != frameInFloor then
        imgFloorIdx = frameInFloor
        if imgFloorIdx == imgCeilIdx then
          imgFloor    = imgCeil
          pdFloor     = pdCeil
        else
          val imgInF  = formatImgIn(imgFloorIdx)
          imgFloor    = ImageIO.read(imgInF)
          pdFloor     = new ImagePD(imgFloor, !invertPD)

      if imgCeilIdx != frameInCeil then
        imgCeilIdx  = frameInCeil
        if imgCeilIdx == imgFloorIdx then
          imgCeil     = imgFloor
          pdCeil      = pdFloor
        else
          val imgInF  = formatImgIn(imgCeilIdx)
          imgCeil     = ImageIO.read(imgInF)
          pdCeil      = new ImagePD(imgCeil, !invertPD)

      for fStep <- 0 until frameStep do
        val fGNG      = formatGNGOut(frameOutIdx)
        val fImgOut   = formatImgOut(frameOutIdx)
        val hasGNG    = fGNG    .length() > 0
        val hasImage  = fImgOut .length() > 0L
        val isSkip    = frameOutIdx <= skipFrames

        if hasGNG then
          cValid = false
          rnd.setSeed(rnd.nextLong()) // XXX TODO --- should store a recoverable seed

        if (!hasGNG || !hasImage) && !cValid && !isSkip then
          val fGNGPrev = formatGNGOut(frameOutIdx - 1)
          readGNG(c, fGNGPrev)
          cValid = true

        if !hasGNG && !isSkip then
          fillParams(frameOutIdx - 1)
          import numbers.Implicits.*
          val weight    = fStep.linLin(0, frameStep.toDouble, 0, 1)
          val isFloor   = rnd.nextDouble() >= weight
          c.pd          = if (isFloor) pdFloor  else pdCeil
          val img       = if (isFloor) imgFloor else imgCeil
          val w         = img.getWidth
          val h         = img.getHeight
          c.panelWidth  = w
          c.panelHeight = h

          if repeat == 1 then c.learn(res)
          else
            var fi = frameOutCnt.toLong * repeat * 100
            for i <- 0 until repeat do
              c.learn(res)
              val progress: Int = (fi / numOutRep).toInt
              if lastProgress < progress then
                while lastProgress < progress do
                  print('#')
                  lastProgress += 1
              end if
              fi += 100

          writeGNG(c, fGNG)

        if !hasImage && !isSkip then
          renderImage(config, c = c, fImgOut = fImgOut, quiet = true)

        frameOutCnt += 1
        val progress: Int = (frameOutCnt * 100) / numOutFrames
        if lastProgress < progress then
          while lastProgress < progress do
            print('#')
            lastProgress += 1

        frameOutIdx += 1

      end for
    }

  end run

