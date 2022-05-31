package com.example.perceptualhashalgorithmv2

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.graphics.scale
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sqrt


@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {
    private val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val projection: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
    private var cursor: Cursor? = null

    private val imagePathList = MutableLiveData<MutableList<M>>()
    fun getImagePathList() = imagePathList

    private val reduceSize = 32
    private val blockSize = 8
    private val coefficients = DoubleArray(reduceSize)

    init {
        for (i in 1 until reduceSize) {
            coefficients[i] = 1.0
        }
        coefficients[0] = 1 / sqrt(2.0)
    }


    fun getImagePathListFromSystem() {
        val list: MutableList<String> = CopyOnWriteArrayList()
        val mList: MutableList<M> = CopyOnWriteArrayList()
        var ct = 0
        cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.let { c ->
            while (c.moveToNext()) {
                val columnIndex = c.getColumnIndex(projection[0])
                println("PATH : ${ct++}")
                val path = c.getString(columnIndex)
                list.add(path)
            }
        }

        val options = BitmapFactory.Options().apply {
        }
        val current = System.currentTimeMillis()
        println("PATH : ----------------------------------------withContext Başı----------------------------------------- ")
        CoroutineScope(IO).launch {
            withContext(IO) {
                list.forEach {
                    launch {
                        if (mList.size > 30)
                            return@launch
                        val bitmap = BitmapFactory.decodeFile(it, options)
                        val scaledBitmap = bitmap.scale(reduceSize, reduceSize)
                        log(bitmap.width.toString() + " " + bitmap.height)
                        val finger = getFingerPrint(scaledBitmap)
                        mList.add(M(finger, it, bitmap))
                    }
                }
            }
            getImagePathList().postValue(mList)

            println("PATH : ----------------------------------------withContext Sonu----------------------------------------- ${System.currentTimeMillis() - current}")
        }
    }


    data class M(val fingerprint: String, val path: String, val bitmap: Bitmap)

    fun calculate(list: MutableList<M>): ArrayList<ArrayList<M>> {
        val mList = ArrayList<ArrayList<M>>()
        list.forEach { i ->
            list.forEach { j ->
                if (i != j) {
                    val dist = hamDist(i.fingerprint, j.fingerprint)
                    if (dist < 5) {
                        mList.add(arrayListOf(i, j))
                        return@forEach
                    }

                }

            }
        }
        return mList
    }


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

    private fun getFingerPrint(bitmap: Bitmap): String {
        val grayPixels: Array<DoubleArray> =
            getGrayPixels(bitmap)
        val dctVals = applyDCT(grayPixels)
        var total = 0.0

        for (x in 0 until blockSize) {
            for (y in 0 until blockSize) {
                total += dctVals!![x][y]
            }
        }
        total -= dctVals!![0][0]
        val avg = total / (blockSize * blockSize - 1).toDouble()
        var hash = ""
        for (x in 0 until blockSize) {
            for (y in 0 until blockSize) {
                if (x != 0 && y != 0) {
                    hash += if (dctVals[x][y] > avg) "1" else "0"
                }
            }
        }
        return hash
    }

    private fun getBlue(img: Bitmap, x: Int, y: Int): Int {
        return img.getPixel(x, y) and 0xff
    }

    private fun applyDCT(f: Array<DoubleArray>): Array<DoubleArray>? {
        val N = reduceSize
        val F = Array(N) { DoubleArray(N) }

        for (u in 0 until N) {
            for (v in 0 until N) {
                var sum = 0.0
                for (i in 0 until N) {
                    for (j in 0 until N) {
                        sum += (cos((2 * i + 1) / (2.0 * N) * u * Math.PI)
                                * cos(
                            (2 * j + 1) / (2.0 * N) * v
                                    * Math.PI
                        ) * (f[i][j]))
                    }
                }
                sum *= ((coefficients[u] * coefficients[v]) / 4.0)
                F[u][v] = sum
            }
        }
        return F
    }
}