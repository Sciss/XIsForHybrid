///*
// *  Sonagrams.scala
// *  (X is for Hybrid)
// *
// *  Copyright (c) 2021 Hanns Holger Rutz. All rights reserved.
// *
// *	This software is published under the GNU Affero General Public License v3+
// *
// *
// *  For further information, please contact Hanns Holger Rutz at
// *  contact@sciss.de
// */
//
//package de.sciss.xisforhybrid
//
//import de.sciss.file.*
//import de.sciss.fscape
//import de.sciss.fscape.lucre.graph.ImageFileOut
//import de.sciss.lucre.{Cursor, IntObj}
//import de.sciss.lucre.synth.InMemory
//import de.sciss.proc
//
//object Sonagrams:
//  def main(args: Array[String]): Unit =
//    run(Config())
//
//  case class Config(
//                   fIn      : File          = file("in.aif"),
//                   markers  : Vector[Long]  = Kreuzen.markers,
//                   dpi      : Int           = 300,
//                   widthMM  : Int           = 148,
//                   heightMM : Int           = 210,
//                   freqLo   : Int           = 80,
//                   freqHi   : Int           = 18000,
//                   ):
//
//    val width : Int = (widthMM  * (dpi / 25.4) + 0.5).toInt
//    val height: Int = (heightMM * (dpi / 25.4) + 0.5).toInt
//
//  end Config
//
//  def run(c: Config): Unit =
//    val gFSc = fscape.Graph {
//      import de.sciss.fscape.graph.{AudioFileIn as _, ImageFileOut as _, *}
//      import de.sciss.fscape.lucre.graph.*
//      import de.sciss.fscape.lucre.graph.Ops.*
//
//      val fIn         = "in"
//      val fOut        = "out"
//      val freqLo      = "freq-lo"       .attr(32.0)
//      val freqHi      = "freq-hi"       .attr(18000.0)
//      val floorDb     = "floor-db"      .attr(-96.0)
//      val ceilDb      = "ceil-db"       .attr( 0.0)
////      val bandsPerOct = "bands-per-oct" .attr(12)
//      val numBands    = "num-bands"     .attr(100)
//      val fileType    = "out-type"      .attr(0)
//      val smpFmt      = "out-format"    .attr(1)
//      val quality     = "out-quality"   .attr(90)
//      val fftSize     = "fft-size"      .attr(8192)
//      val timeResMS   = "time-res-ms"   .attr(20.0)
//      val rotate      = "rotate"        .attr(0).toInt.clip(0, 1)
//      val invert      = "invert"        .attr(0).toInt.clip(0, 1)
//
//      val in        = AudioFileIn(fIn)
//      val sr        = in.sampleRate
//      val numFrames = in.numFrames
//      val minFreqN  = freqLo / sr
//      val maxFreqN  = freqHi / sr
////      val numOct    = (freqHi / freqLo).log2
////      val numBands  = ((numOct * bandsPerOct) + 0.5).toInt.max(1)
//      val timeRes   = (timeResMS * 0.001 * sr + 0.5).toInt.max(1)
//      val slid      = Sliding(in, size = fftSize, step = timeRes)
//      val win0      = GenWindow.Hann(fftSize)
//      val numWin    = (numFrames + timeRes - 1) / timeRes
//      val win       = RepeatWindow(win0, fftSize, num = numWin)
//      val windowed0 = slid * win
//      val windowed  = RotateWindow(windowed0, fftSize, fftSize >> 1)
//      val fft       = Real1FFT(windowed, size = fftSize, mode = 0)
//
//      val cq0 = ConstQ(fft, fftSize = fftSize,
//        minFreqN = minFreqN, maxFreqN = maxFreqN, numBands = numBands)
//
//      val numFramesOut = numWin * numBands
//      val cq    = cq0.take(numFramesOut)
//      val cqDb  = cq.ampDb
//
//      val outLo = invert.toDouble
//      val outHi = (1 - invert).toDouble
//      val sig0  = cqDb.linLin(floorDb, ceilDb, outLo, outHi)
//      val sig1  = sig0.clip(0.0, 1.0)
//      val sig   = If (rotate) Then {
//        RotateFlipMatrix(sig1, rows = numWin, columns = numBands,
//          mode = RotateFlipMatrix.Rot90CCW)
//      } Else sig1
//      val r1      = 1 - rotate
//      val width   = numBands * r1 + numWin   * rotate
//      val height  = numWin   * r1 + numBands * rotate
//
//      ImageFileOut(fOut, sig, width = width, height = height, fileType = fileType,
//        sampleFormat = smpFmt, quality = quality)
//    }
//
////    val gCtl = proc.Control.Graph {
////      import de.sciss.lucre.expr.graph._
////      val rFSc  = Runner("fsc")
////      val lb    = LoadBang()
////      lb --> rFSc.runWith(
////        "freq-lo" -> c.freqLo,
////        "freq-hi" -> c.freqHi,
////      )
////    }
//
//    type T = InMemory.Txn
//    given cursor: Cursor[T] = InMemory()
//    cursor.step { implicit tx =>
//      given proc.Universe[T] = proc.Universe.dummy[T]
//      val fsc = proc.FScape[T]()
//      fsc.graph() = gFSc
//      fsc.attr.put("freq-lo", IntObj.newConst(c.freqLo))
//      fsc.attr.put("freq-hi", IntObj.newConst(c.freqHi))
//      fsc.attr.put("num-bands", IntObj.newConst(c.height))
//      val r = proc.Runner(fsc)
//      r.run()
//    }
//
//    ()
