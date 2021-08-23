[![Build Status](https://github.com/Sciss/XIsForHybrid/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/XIsForHybrid/actions?query=workflow%3A%22Scala+CI%22)

# X is for Hybrid

This repository contains code for an art book contribution.

(C)opyright 2021 by Hanns Holger Rutz. All rights reserved. This project is released under the
[GNU Affero General Public License](https://github.com/Sciss/XIsForHybrid/blob/main/LICENSE) v3+ and
comes with absolutely no warranties. Mellite workspaces and visual output provided under CC BY-NC-ND 4.0.
To contact the author, send an e-mail to `contact at sciss.de`.

## building

Builds with sbt against Scala 3.

## data

Indices are zero-based:

```
Selection:
[-1, 124, 120, 70, 23, 59, 43, 110, 112, 3, 351, 99, 71, 452, 361, 52]
[-1, -1, 0, 1, 57, -1, 58, 24, 17, 19, 41, 22, 76, 4, 5, 62]
[223, 144, 204, 34, 122, 123, 149, 32, 121, 226, 478, 31, 72, 420, 81, 381]
[10, -1, 12, 77, 54, 6, 26, 16, 27, 63, 45, 49, 68, 44, 8, 14]
```

These are the sonogram indices for the four quadrants. 
Q0 has 15 (14 = even) layers, Q1 has 13 (12 = even) layers, 
Q2 has all 16 layers, Q3 has 15 (14 = even) layers. They need shift adjustments.

```
Shifts:
[0, 75, 86, 44, 29, 72, 58, 67, 60, 63, 52, 75, 53, 88, 68, 67]
[0, 0, 74, 63, 68, 0, 57, 76, 54, 52, 77, 62, 81, 36, 64, 59]
[71, 63, 84, 21, 41, 53, 66, 49, 68, 73, 82, 66, 76, 65, 60, 76]
[0, 0, 52, 60, 82, 68, 65, 73, 25, 92, 60, 70, 80, 61, 86, 78]
```

## composite

Each two adjacent layers are grouped with the bottom layer "normal", the top layer "divide". "Missing layers"
(index -1) are represented by matte white.
All layer groups are composed through "multiply". GIMP data type is 32-bit float (only here does
the divide do something strange). 

Version "n3": After flattening, 'CIE Ich Noise' with parameters
(Duling -> 2), (Lightness -> 25), (Chroma -> 25), (Hue -> 3) and (Random Seed -> <image-index+1>) is added.
Then a black/white threshold at 0.15 is applied, before converting to binary bitmap.

Version "gamma4": After flattening, open 'Color Levels', and set gamma to 4.0.
Downscaling from 600 dpi to 300 dpi gives much softer and brighter image in ink-jet print, capturing well
the materiality of the ink. 600 dpi prints are way too dark.

## caption

X is for Hybrid. The German word “kreuzen” means to cross or to hybridise. When spoken – ˈkʁɔɪ̯t͡sn̩ –  the sonogram
of this word has a cross shape itself with both a temporal and a spectral stop gap, creating four quadrants: a 
crossing means two things meet and depart, both become transformed. The graphics are based on hundreds of recorded 
instances of the word, each revealing a slightly different articulation. The corpus is clustered into four 
quadrants and a subset of each cluster is composited after rendering each individual using the Growing Neural 
Gas algorithm, which imposes its own characteristics.

## tour

(for CARPA7 video)

```
Optimized cost: 215.5104503164643
439, 99,119,344,345,457,281,158,124,427,466,311,295,208,354,268,497,338,164, 72,
 85,351, 69,455, 97, 96,123,127,231,219,364,429,198,155,321,197,260,191,357,388,
434,493,377,290,259,247,505,160,339,361,360,190,187,174,178,179,151,108,413,438,
440,494,451,441,359,401,482,194,446,492,456,436, 64,324,287,248,103,104,105,107,
 36,149,175,470,444,433,157,317,142,185,255,254,383,386,262,263,183,426,325,282,
237,296,243,236,313,315,375,270,206,181,328,327,340,337, 86, 35, 87,479,504,332,
319,261,213,271,368,371,162,277,211,408,447,453,445,272,343,346,309,218,392,491,
314,116,120, 32, 60,122,459,460,424, 33, 75, 74,419,330,229,292,251,239, 34,147,
144,148,256,253,378,379,489,500,499,342,472, 24,161,168,114,115,341,169,329,177,
278,214,274,112,293,223,180,301,216,355,347,186,462,100,121,140,297,437,230,495,
215,496,232,134,138,265,240,131, 51, 16,  7, 39, 11, 29, 31, 25, 23, 19,  3,  5,
  2,  1, 70, 89, 88, 41, 54,102,101, 37, 90,113, 78, 57,136,125,394,393,362,358,
288,376,471,483,490,469,465,299,300,276,220,289,133,204,167, 76,153,159,303,414,
380,416,154,316,195,207,196,350,348,212,203,210,366,443,398,390,387,395,152,118,
 84,137,156,488,410,331,222,374,334,326, 82,356,353,275,352,454,484,452,415,307,
312,221,403,369,279,172,189, 12, 10, 40,  8,  9,485,476,365,407,450,391,389,411,
412,106,170, 53, 27, 28, 22, 81, 30, 71,431,449,370,363,373,372,367,381,225,486,
252,238,188,249,333,435,481,244,280,291,306,227,308,501,503,209,224,318,478,242,
245,304,264,269,477,474,475,402,480,404,406,200,235,448,405,  6, 17, 14, 13, 65,
 63, 80, 59, 18, 47, 46, 79,109, 45, 49, 15, 73, 67, 66,126,117,163,173,166, 98,
 95, 94,  4, 21,430, 42, 26, 20, 52, 93,135, 77, 83,322,428,273,294,217,165,396,
432,397,399,267,266,335,320,250,461,473,458,425,323,193,257,258,226,417,145,146,
130,150,202,205,171,464,468,467,302,182,284,310,283,285,233,400,286,228,234,132,
241,246,184,143,129,128,422,423,421,487,420,418,384,382,409,463,502,349,298,498,
385,192,305,336,176,110,141, 91, 92, 68, 61, 50, 48, 55, 56, 62, 43, 58, 38,139,
111,199,201, 44,442
```
