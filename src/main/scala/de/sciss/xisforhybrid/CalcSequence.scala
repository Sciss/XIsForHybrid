/*
 *  CalcSequence.scala
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

import de.sciss.numbers.Implicits.*
import de.sciss.tsp.LinKernighan

import java.io.{DataInputStream, File, FileInputStream}
import scala.collection.immutable.IndexedSeq as Vec

object CalcSequence:
  val tourS: String =
    """439, 99,119,344,345,457,281,158,124,427,466,311,295,208,354,268,497,338,164, 72,
      | 85,351, 69,455, 97, 96,123,127,231,219,364,429,198,155,321,197,260,191,357,388,
      |434,493,377,290,259,247,505,160,339,361,360,190,187,174,178,179,151,108,413,438,
      |440,494,451,441,359,401,482,194,446,492,456,436, 64,324,287,248,103,104,105,107,
      | 36,149,175,470,444,433,157,317,142,185,255,254,383,386,262,263,183,426,325,282,
      |237,296,243,236,313,315,375,270,206,181,328,327,340,337, 86, 35, 87,479,504,332,
      |319,261,213,271,368,371,162,277,211,408,447,453,445,272,343,346,309,218,392,491,
      |314,116,120, 32, 60,122,459,460,424, 33, 75, 74,419,330,229,292,251,239, 34,147,
      |144,148,256,253,378,379,489,500,499,342,472, 24,161,168,114,115,341,169,329,177,
      |278,214,274,112,293,223,180,301,216,355,347,186,462,100,121,140,297,437,230,495,
      |215,496,232,134,138,265,240,131, 51, 16,  7, 39, 11, 29, 31, 25, 23, 19,  3,  5,
      |  2,  1, 70, 89, 88, 41, 54,102,101, 37, 90,113, 78, 57,136,125,394,393,362,358,
      |288,376,471,483,490,469,465,299,300,276,220,289,133,204,167, 76,153,159,303,414,
      |380,416,154,316,195,207,196,350,348,212,203,210,366,443,398,390,387,395,152,118,
      | 84,137,156,488,410,331,222,374,334,326, 82,356,353,275,352,454,484,452,415,307,
      |312,221,403,369,279,172,189, 12, 10, 40,  8,  9,485,476,365,407,450,391,389,411,
      |412,106,170, 53, 27, 28, 22, 81, 30, 71,431,449,370,363,373,372,367,381,225,486,
      |252,238,188,249,333,435,481,244,280,291,306,227,308,501,503,209,224,318,478,242,
      |245,304,264,269,477,474,475,402,480,404,406,200,235,448,405,  6, 17, 14, 13, 65,
      | 63, 80, 59, 18, 47, 46, 79,109, 45, 49, 15, 73, 67, 66,126,117,163,173,166, 98,
      | 95, 94,  4, 21,430, 42, 26, 20, 52, 93,135, 77, 83,322,428,273,294,217,165,396,
      |432,397,399,267,266,335,320,250,461,473,458,425,323,193,257,258,226,417,145,146,
      |130,150,202,205,171,464,468,467,302,182,284,310,283,285,233,400,286,228,234,132,
      |241,246,184,143,129,128,422,423,421,487,420,418,384,382,409,463,502,349,298,498,
      |385,192,305,336,176,110,141, 91, 92, 68, 61, 50, 48, 55, 56, 62, 43, 58, 38,139,
      |111,199,201, 44,442""".stripMargin

  lazy val tour: Vec[Int] = tourS.split('\n').iterator.flatMap(_.split(',')).map(_.trim.toInt - 1).toIndexedSeq

  def main(args: Array[String]): Unit =
    if tour.isEmpty then calcTour()
    else
      assert (tour.size == 505)
      calcShifts()

  def calcShifts(): Unit =
    val Vec(shifts) = Cluster.calcShifts(Vec(tour))
    println("Shifts:")
    println(shifts.mkString(","))

  def calcTour(): Unit =
    val dataSq  = Cluster.loadCoefficients()
    val minC    = dataSq.iterator.flatten.min
    val maxC    = dataSq.iterator.flatten.max
    val numSona = dataSq.size
    println(s"numSona = $numSona, min coef $minC, max coef $maxC")

    val mf      = dataSq.flatten
    val numPar  = 505
    val sz      = mf.size
    assert (sz == (numPar * (numPar-1))/2, s"Unexpected matrix size $sz")

//    val maxSim = mf.max
//    println(s"Maximum similarity is $maxSim")

    val cost = Array.ofDim[Double](numPar, numPar)
    for
      vi <- 0        until numPar
      vj <- (vi + 1) until numPar
    do
      val vk    = vj - (vi + 1)
//      val c     = maxSim - dataSq(vi)(vk)
      val coeff = dataSq(vi)(vk)
      val dist  = coeff.linLin(minC, maxC, 1.0, 0.0)
      cost(vi)(vj) = dist
      cost(vj)(vi) = dist

    val rnd     = new util.Random(0)
    val N       = 10
    val tours   = Seq.tabulate(N) { x =>
      val tour0   = rnd.shuffle((0 until numPar).toVector).toArray
      val lk      = LinKernighan(cost, tour0)
      if x == 0 then println(s"Original cost: ${lk.tourCost}")
      lk.run()
      println(s"Optimized cost (${x + 1}/$N): ${lk.tourCost}")
      (lk.tourCost, lk.tour.toList)
    }
    val (bestCost, bestTour) = tours.minBy(_._1)
    println(s"Optimized cost: $bestCost")
    println(bestTour.map(_ + 1).mkString(","))  // 1-based index
  end calcTour