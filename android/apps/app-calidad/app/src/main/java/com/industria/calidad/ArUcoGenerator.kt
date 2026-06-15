package com.industria.calidad

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

object ArUcoGenerator {
    fun buildBitmap(size: Int = 32, markerId: Int = 7): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        })

        val bits = markerId.toString(2).padStart(8, '0').toCharArray()
        val cell = size / maxOf(8, bits.size)
        val half = maxOf(1, cell / 2)

        for (row in 0 until size) {
            for (col in 0 until size) {
                val x = col / cell
                val y = row / cell
                val idx = (y * 8 + x) % bits.size
                val on = bits[idx] == '1'
                if (on || x == 0 || y == 0 || x == 7 || y == 7) {
                    canvas.drawRect(
                        col.toFloat(),
                        row.toFloat(),
                        col.toFloat() + 1f,
                        row.toFloat() + 1f,
                        paint
                    )
                }
            }
        }

        canvas.drawRect(0f, 0f, size.toFloat(), half.toFloat(), Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        })
        canvas.drawRect(0f, size - half.toFloat(), size.toFloat(), size.toFloat(), Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        })
        return bitmap
    }
}
