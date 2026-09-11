package com.brannenservices.fieldassistant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object EmbeddedArt {
    private fun decode(data:String):Bitmap = BitmapFactory.decodeByteArray(Base64.decode(data, Base64.DEFAULT),0,Base64.decode(data, Base64.DEFAULT).size)
    val floridaMan: Bitmap by lazy { decode(FM) }
    val flamingo: Bitmap by lazy { decode(FL) }
    private const val FM = "PLACEHOLDER"
    private const val FL = "PLACEHOLDER"
}
