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
the divide do something strange). After flattening, 'CIE Ich Noise' with parameters
(Duling -> 2), (Lightness -> 25), (Chroma -> 25), (Hue -> 3) and (Random Seed -> <image-index+1>) is added.
Then a black/white threshold at 0.15 is applied, before converting to binary bitmap.
