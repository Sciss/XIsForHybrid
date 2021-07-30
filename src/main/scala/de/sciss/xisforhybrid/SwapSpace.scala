package de.sciss.xisforhybrid

import de.sciss.file._
import scala.language.implicitConversions

object SwapSpace {
  def main(args: Array[String]): Unit =
    val argIdx    = args.indexOf("--start") + 1
    val startIdx  = if argIdx > 0 && argIdx < args.length then args(argIdx).toInt else 1
    val c = Neural.Config(
      imgInTemp   = file("/data/projects/applications/210715_SteiermarkDigitaleImpulse/materials/lichens%da_lvl_gray2.jpg"),
      imgOutTemp  = file("/data/projects/applications/210715_SteiermarkDigitaleImpulse/materials/lichens%da_gng3.png"),
      invertPD    = false,
      grayPD      = true,
      maxNodes    = 5000,
      gngStepSize = 200,  // 500 is maximum
      gngUtility  = 0.0, // 8.0, // 1.0,
      gngLambda   = 400,
      gngEdgeAge  = 800,
      gngEpsilon  = 1.0e-3,
      gngEpsilon2 = 1.0e-4,
      gngAlpha    = 0.8,
      gngBeta     = 1.0e-5,
      startInIdx  = 4,
      endInIdx    = 4,
      startOutIdx = 4,
      rngSeed     = 0xDADB,
      repeat      = 2000000 / 200,
      frameStep   = 1,
      strokeWidth = 1.0,
    )
    Neural.run(c)
}
