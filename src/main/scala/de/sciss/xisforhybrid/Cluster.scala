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

import de.sciss.file.*
import de.sciss.lucre.store.BerkeleyDB
import de.sciss.lucre.{DoubleVector, Folder, StringObj}
import de.sciss.proc.Implicits.*
import de.sciss.proc.{Durable, FScape, SoundProcesses, Workspace}
import de.sciss.serial.DataOutput

import java.net.URI
import java.util.Locale
import scala.collection.immutable.IndexedSeq as Vec

object Cluster:
  def main(args: Array[String]): Unit =
    if !CORR_FILE.isFile then obtainCoefficients()

  private val COOKIE_CORR = 0x436f7272

  private val CORR_FILE   = file("correlations.bin")

  def obtainCoefficients(): Unit =
    Locale.setDefault(Locale.US)
    SoundProcesses.init()
    FScape        .init()

    val f   = file("/data/projects/BookOfX/workspaces/sonograms.mllt")
    val dsf = BerkeleyDB.factory(f, createIfNecessary = false)
    val ws  = Workspace.Durable.read(f.toURI, dsf)
    type T  = Durable.Txn
    val data: Vec[Vec[Float]] = try
      ws.cursor.step { implicit tx =>
        val fSub = ws.root.$[Folder]("correlation").get
        val numChilden = fSub.size
        fSub.iterator.zipWithIndex.map {
          case (child: StringObj[T], ci) =>
            val nameExp = s"kreuzen-${ci + 1}-sono.png"
            val name    = child.value
            require (name == nameExp, s"$name != $nameExp")
            val sq      = child.attr.$[DoubleVector]("corr").fold(Vector.empty)(_.value)
            require (sq.size == numChilden - ci - 1)
  //          val sqS     = sq.map("%g".format(_))
  //          val sqG     = sqS.grouped(8)
  //          sqG.map { line => line.mkString("  ", ", ", "") } .mkString("  Vector(\n    ", "\n    ", "\n),")
            sq.map(_.toFloat)
        } .toVector // .mkString("val corr: Vec[Vec[Double]] =\n", "\n", "")
      }
    finally ws.close()

    val out = DataOutput.open(CORR_FILE)
    try
      out.writeInt(COOKIE_CORR)
      out.writeShort(data.size)
      data.foreach { ln =>
        out.writeShort(ln.size)
        ln.foreach(out.writeFloat)
      }

    finally out.close()

  end obtainCoefficients
