package com.example.perceptualhashalgorithmv2

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import kotlin.math.cos
import kotlin.math.sqrt

object Util {
    private const val reduceSize = 32
    private const val blockSize = 8
    private val coefficients = DoubleArray(reduceSize).also {
        it[0] = 1 / sqrt(2.0)
        it.fill(1.0, 1, reduceSize)
    }
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
    fun getImageHash(path: String): String {
        val scaledBitmap = getScaledBitmap(path)
        return getImageHash(scaledBitmap)

    }

    private fun getScaledBitmap(path: String) =
        BitmapFactory.decodeFile(path).scale(reduceSize, reduceSize)

    private fun hamDist(finger1: String, finger2: String): Int {
        var dist = 0
        if (finger1.length == finger2.length) {
            repeat(finger1.length) {
                if (finger1[it] != finger2[it])
                    dist++
            }
        } else {
            if (finger1.length > finger2.length)
                repeat(finger1.length - finger2.length) {
                    finger2.plus("1")
                }
            else
                repeat(finger2.length - finger1.length) {
                    finger1.plus("1")
                }
            dist = hamDist(finger1, finger2)
        }
        return dist

    }

    private fun getGrayPixels(bitmap: Bitmap): Array<DoubleArray> {
        val pixels = Array(bitmap.width) { DoubleArray(bitmap.height) }
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val grayValue = computeGrayValue(bitmap.getPixel(i, j))
                //  log("Gray Value for $i $j $grayValue")
                pixels[i][j] = grayValue
            }
        }
        return pixels
    }

    private fun computeGrayValue(pixel: Int): Double {
        val red = pixel shr 16 and 0xFF
        val green = pixel shr 8 and 0xFF
        val blue = pixel and 255
        return (0.299 * red + 0.587 * green + 0.114 * blue)
    }

    private fun getImageHash(bitmap: Bitmap): String {
        //TO GRAY
        val grayPixels = getGrayPixels(bitmap)
        // APPLIED DCT
        val transformedPixels = applyDCT(grayPixels)
        //FIND AVG
        val pixelAvg = getPixelAvg(transformedPixels)
        //FIND HASH
        return getImageHash(transformedPixels, pixelAvg)
    }

    private fun getImageHash(transformedPixels: Array<DoubleArray>, pixelAvg: Double): String {
        var hash = ""
        for (x in 0 until blockSize) {
            for (y in 0 until blockSize) {
                if (x != 0 && y != 0) {
                    hash += if (transformedPixels[x][y] > pixelAvg) "1" else "0"
                }
            }
        }
        return hash
    }

    private fun getPixelAvg(transformedPixels: Array<DoubleArray>): Double {
        var total = 0.0
        for (x in 0 until blockSize) {
            for (y in 0 until blockSize) {
                total += transformedPixels[x][y]
            }
        }
        total -= transformedPixels[0][0]
        return total / (blockSize * blockSize - 1).toDouble()
    }

    private fun getBlue(img: Bitmap, x: Int, y: Int): Int {
        return img.getPixel(x, y) and 0xff
    }

    private fun applyDCT(f: Array<DoubleArray>): Array<DoubleArray> {
        val N = reduceSize
        val F = Array(N) { DoubleArray(N) }

        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                for (i in 0 until N) {
                    for (j in 0 until N) {
                        sum += (cos((2 * i + 1) / (2.0 * N) * u * Math.PI) * cos((2 * j + 1) / (2.0 * N) * v * Math.PI) * (f[i][j]))
                    }
                }
                sum *= ((coefficients[u] * coefficients[v]) / 4.0)
                F[u][v] = sum
            }
        }
        return F
    }
}

fun log(string: String) = println("APP : $string")

data class Group(
    val original: MainViewModel.ImageWithHash,
    val list: ArrayList<MainViewModel.ImageWithHash> = arrayListOf()
)
