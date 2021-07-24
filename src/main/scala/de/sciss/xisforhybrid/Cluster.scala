/*
 *  Cluster.scala
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

import breeze.linalg.DenseMatrix
import de.sciss.cluster.STSC
import de.sciss.file.*
import de.sciss.lucre.store.BerkeleyDB
import de.sciss.lucre.{DoubleVector, Folder, IntVector, Obj, StringObj}
import de.sciss.numbers.Implicits.*
import de.sciss.proc.Implicits.*
import de.sciss.proc.{Durable, FScape, SoundProcesses, Workspace}
import de.sciss.serial.{DataInput, DataOutput}

import java.net.URI
import java.util.Locale
import scala.collection.immutable.IndexedSeq as Vec

object Cluster:
  def main(args: Array[String]): Unit =
    if !CORR_FILE .isFile then obtainCoefficients()
    if !SHIFT_FILE.isFile then obtainShifts()
    run()

  private val COOKIE_CORR   = 0x436f7272
  private val COOKIE_SHIFT  = 0x53686674

  private val CORR_FILE   = file("correlations.bin")
  private val SHIFT_FILE  = file("shifts.bin")

  val NUM_IMAGES = 4
  val NUM_LAYERS = 16

  /*

    First we partition in the number of images (4).
    Then for each image, we want to find a number of layers.
    To do so, we partition each partition into the number of layers (16),
    and pick one element per partition (e.g. the one that minimises
    distances to the others within its cluster).

   */
  def run(): Unit =
    val dataSq  = loadCoefficients()
    val minC    = dataSq.iterator.flatten.min
    val maxC    = dataSq.iterator.flatten.max
    val numSona = dataSq.size
    println(s"numSona = $numSona, min coef $minC, max coef $maxC")
    val simMat  = DenseMatrix.zeros[Double](numSona, numSona)
    for ri <- 0 until numSona do
      val row = dataSq(ri)
      require (row.size == numSona - ri - 1)
      for ci <- 0 until row.size do
        val coeff = row(ci)
        val dist  = coeff.linLin(minC, maxC, 1.0, 0.0)
        val qi    = ri + ci + 1
        simMat(ri, qi) = dist
        simMat(qi, ri) = dist

    val STSC.Result(numImg, _, clustersImg) = STSC.clusterDistances(simMat, NUM_IMAGES, NUM_IMAGES)
    assert (numImg == NUM_IMAGES)
    val clusterSizesImg = List.tabulate(numImg)(n => clustersImg.count(_ == n))
    println(s"Cluster sizes are ${clusterSizesImg.mkString("[", ", ", "]")}")
    assert (clusterSizesImg.sum == numSona)
    require (clusterSizesImg.forall(_ >= NUM_LAYERS))

    val select: Vec[Vec[Int]] = Vector.tabulate(numImg) { ii =>
      val clusterIndices = clustersImg.iterator.zipWithIndex.collect {
        case (`ii`, idx) => idx
      } .toIndexedSeq

      val clusterSz = clusterIndices.size
      if clusterSz == NUM_LAYERS then
        println(s"For image $ii, we immediately have $NUM_LAYERS layers.")
        clusterIndices
      else
        val subMat = DenseMatrix.zeros[Double](clusterSz, clusterSz)
        for ri <- 0 until clusterSz do
          for ci <- 0 until clusterSz do
            val rj    = clusterIndices(ri)
            val cj    = clusterIndices(ci)
            subMat(ri, ci) = simMat(rj, cj)

        val STSC.Result(numLay, _, clustersLay) = STSC.clusterDistances(subMat, NUM_LAYERS, NUM_LAYERS)
        assert (numLay == NUM_LAYERS)
        // note: what can happen with smaller populations is that some clusters are empty!
        // in that case we return index -1
        val clusterSizesLay = List.tabulate(numLay)(n => clustersLay.count(_ == n))
        // println(clustersLay.mkString(" | "))
        println(s"For image $ii, cluster sizes are ${clusterSizesLay.mkString("[", ", ", "]")}")
        Vector.tabulate(numLay)(li => clustersLay.iterator.zipWithIndex.collectFirst {
          case (`li`, idx) => clusterIndices(idx)
        } .getOrElse(-1) )

      end if
    }
    println("Selection:")
    println(select.map(_.mkString("[", ", ", "]")).mkString("\n"))
    val unique = select.flatten.filterNot(_ == -1)
    assert (unique == unique.distinct)

    println("Shifts:")
    val shiftSq = loadShifts()
    val imgShifts0 = select.map { img =>
      val imgF = img.filterNot(_ == -1)
      val imgS = imgF.toSet
      img.map { li =>
        if li == -1 then Int.MaxValue else
          val others = imgS - li
          val dSq = others.map { that =>
            val i = math.min(li, that)
            val j = math.max(li, that) - (i + 1)
            val d = shiftSq(i)(j)
            if i == li then -d else +d
          }
          ((dSq.sum.toDouble / others.size) * 0.5 + 0.5).toInt
      }
    }
    val minShift  = imgShifts0.flatten.filterNot(_ == Int.MaxValue).min
    val imgShifts = imgShifts0.map { img =>
      img.map {
        case Int.MaxValue => 0
        case x            => x - minShift
      }
    }
    println(imgShifts.map(_.mkString("[", ", ", "]")).mkString("\n"))

  end run

  def loadCoefficients(): Vec[Vec[Double]] =
    val in = DataInput.open(CORR_FILE)
    try
      require (in.readInt() == COOKIE_CORR)
      val numChildren = in.readShort()
      Vector.fill(numChildren) {
        val lineSz = in.readShort()
        Vector.fill(lineSz)(in.readFloat().toDouble)
      }
    finally in.close()

  def loadShifts(): Vec[Vec[Int]] =
    val in = DataInput.open(SHIFT_FILE)
    try
      require (in.readInt() == COOKIE_SHIFT)
      val numChildren = in.readShort()
      Vector.fill(numChildren) {
        val lineSz = in.readShort()
        Vector.fill(lineSz)(in.readShort().toInt)
      }
    finally in.close()

  type T = Durable.Txn

  def obtainShifts(): Unit =
    obtainFromWorkspace(cookie = COOKIE_SHIFT, outF = SHIFT_FILE) { implicit tx =>
      child => child.attr.$[IntVector]("shift").fold(Vector.empty)(_.value.map(_.toShort))
    } { (out, a) => out.writeShort(a) }

  def obtainCoefficients(): Unit =
    obtainFromWorkspace(cookie = COOKIE_CORR, outF = CORR_FILE) { implicit tx =>
      child => child.attr.$[DoubleVector]("corr").fold(Vector.empty)(_.value.map(_.toFloat))
    } { (out, a) => out.writeFloat(a) }

  def obtainFromWorkspace[A](cookie: Int, outF: File)(getSq: T => Obj[T] => Vec[A])(writeElem: (DataOutput, A) => Unit) : Unit =
    Locale.setDefault(Locale.US)
    SoundProcesses.init()
    FScape        .init()

    val wsF = file("/data/projects/BookOfX/workspaces/sonograms.mllt")
    val dsf = BerkeleyDB.factory(wsF, createIfNecessary = false)
    val ws  = Workspace.Durable.read(wsF.toURI, dsf)
    val data: Vec[Vec[A]] = try
      ws.cursor.step { implicit tx =>
        val fSub = ws.root.$[Folder]("correlation").get
        val numChilden = fSub.size
        fSub.iterator.zipWithIndex.map {
          case (child: StringObj[T], ci) =>
            val nameExp = s"kreuzen-${ci + 1}-sono.png"
            val name    = child.value
            require (name == nameExp, s"$name != $nameExp")
            val sq      = getSq(tx)(child) // child.attr.$[DoubleVector](key).fold(Vector.empty)(_.value)
            require (sq.size == numChilden - ci - 1)
            sq
        } .toVector // .mkString("val corr: Vec[Vec[Double]] =\n", "\n", "")
      }
    finally ws.close()

    val out = DataOutput.open(outF)
    try
      out.writeInt(cookie)
      out.writeShort(data.size)
      data.foreach { ln =>
        out.writeShort(ln.size)
        ln.foreach(writeElem(out, _))
      }

    finally out.close()

  end obtainFromWorkspace
