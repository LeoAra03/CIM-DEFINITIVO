package com.industria.calidad

import android.graphics.Bitmap
import com.sistema.distribuido.network.ArucoDictionary
import com.sistema.distribuido.network.IndustrialVisionAnalyzer

object ArUcoGenerator {
    fun buildBitmap(
        size: Int = 250,
        markerId: Int = 7,
        dictionary: ArucoDictionary = ArucoDictionary.DICT_4X4_50
    ): Bitmap? {
        return IndustrialVisionAnalyzer.generateArucoMarker(markerId, size, dictionary)
    }

    fun buildBitmapMm(
        sizeMm: Int = 100,
        markerId: Int = 7,
        dictionary: ArucoDictionary = ArucoDictionary.DICT_4X4_50
    ): Bitmap? {
        return IndustrialVisionAnalyzer.generateArucoMarkerMm(markerId, sizeMm, dictionary)
    }
}
