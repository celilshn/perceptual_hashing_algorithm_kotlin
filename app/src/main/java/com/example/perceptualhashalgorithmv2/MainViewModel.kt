package com.example.perceptualhashalgorithmv2

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject


@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {
    private val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val projection: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
    private var cursor: Cursor? = null

    private val imagePathList = MutableLiveData<MutableList<ImageWithHash>>()
    fun getImagePathList() = imagePathList

    data class ImageWithHash(val hash: String, val path: String)
/*

    init {
        val mList: MutableList<ImageWithHash> = CopyOnWriteArrayList()
        val currentTimeMillis = System.currentTimeMillis()
        val imagePathList = getImagePathListFromSystem().take(250)
        val chunkedList = imagePathList.chunked(100)
        println("PATH : ----------------------------------------withContext Başı----------------------------------------- ")
        CoroutineScope(Dispatchers.Default).launch {
            val it = chunkedList
                .map {
                    Util.getImageHashWithList(it)
                }

            mList.addAll(it.await())
                .collect()
            mList.addAll
            getImagePathList().value = (mList)
        }
        println("PATH : ----------------------------------------withContext Sonu----------------------------------------- ${System.currentTimeMillis() - currentTimeMillis}")
    }

*/

        init {
            val mList: MutableList<ImageWithHash> = CopyOnWriteArrayList()
            val mListAll: MutableList<ImageWithHash> = CopyOnWriteArrayList()
            val currentTimeMillis = System.currentTimeMillis()
            val imagePathList = getImagePathListFromSystem().take(250)
            val chunkedList = imagePathList.chunked(100)
            CoroutineScope(Default).launch {
                println("PATH : ----------------------------------------withContext Başı----------------------------------------- ")

                withContext(Default) {
                    chunkedList
                        .asFlow()
                        .map {
                            async { Util.getImageHashWithList(it).also {
                                println("PATH : ${Thread.currentThread()}")
                            } }
                        }
                        .map {
                            mList.addAll(it.await())
                        }.collect()

                }

                launch(Main) {
                    getImagePathList().value = (mList)
                    println("PATH : ----------------------------------------withContext Sonu----------------------------------------- ${System.currentTimeMillis() - currentTimeMillis}")
                }
            }
        }
    fun getImagePathListFromSystem(): MutableList<String> {
        val list: MutableList<String> = ArrayList()
        cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.let { c ->
            while (c.moveToNext()) {
                val columnIndex = c.getColumnIndex(projection[0])
                val path = c.getString(columnIndex)
                list.add(path)
            }
        }
        return list
    }

    fun calculate(list: MutableList<ImageWithHash>): ArrayList<Group> {
        val mList = ArrayList<Group>()
        val blockedList = ArrayList<String>()
        list.forEach { i ->
            val group = Group(i)
            list.forEach { j ->
                if (i != j && !blockedList.contains(i.path)) {
                    if (hamDist(i.hash, j.hash) < 5)
                        group.list.add(j).also {
                            blockedList.add(j.path)
                        }

                }
            }
            if (group.list.isNotEmpty())
                mList.add(group)
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
}