package com.example.perceptualhashalgorithmv2

import android.Manifest

object Util {
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
}
fun log(string:String) = println("APP : $string")