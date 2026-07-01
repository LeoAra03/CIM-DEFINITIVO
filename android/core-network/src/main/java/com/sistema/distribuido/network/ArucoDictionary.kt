package com.sistema.distribuido.network

import org.opencv.objdetect.Objdetect

/**
 * Diccionarios ArUco estándar (Original ArUco).
 * Compatible con generadores web como fucolab.github.io/arucogen.
 */
enum class ArucoDictionary(
    val label: String,
    val opencvConstant: Int,
    val maxId: Int
) {
    DICT_4X4_50("4x4 (50)", Objdetect.DICT_4X4_50, 49),
    DICT_4X4_100("4x4 (100)", Objdetect.DICT_4X4_100, 99),
    DICT_4X4_250("4x4 (250)", Objdetect.DICT_4X4_250, 249),
    DICT_4X4_1000("4x4 (1000)", Objdetect.DICT_4X4_1000, 999),
    DICT_5X5_50("5x5 (50)", Objdetect.DICT_5X5_50, 49),
    DICT_5X5_100("5x5 (100)", Objdetect.DICT_5X5_100, 99),
    DICT_5X5_250("5x5 (250)", Objdetect.DICT_5X5_250, 249),
    DICT_5X5_1000("5x5 (1000)", Objdetect.DICT_5X5_1000, 999),
    DICT_6X6_50("6x6 (50)", Objdetect.DICT_6X6_50, 49),
    DICT_6X6_100("6x6 (100)", Objdetect.DICT_6X6_100, 99),
    DICT_6X6_250("6x6 (250)", Objdetect.DICT_6X6_250, 249),
    DICT_6X6_1000("6x6 (1000)", Objdetect.DICT_6X6_1000, 999),
    DICT_7X7_50("7x7 (50)", Objdetect.DICT_7X7_50, 49),
    DICT_7X7_100("7x7 (100)", Objdetect.DICT_7X7_100, 99),
    DICT_7X7_250("7x7 (250)", Objdetect.DICT_7X7_250, 249),
    DICT_7X7_1000("7x7 (1000)", Objdetect.DICT_7X7_1000, 999);

    fun clampId(id: Int): Int = id.coerceIn(0, maxId)

    /** ~10 px/mm a 96 DPI de pantalla; ajustable para impresión/láser. */
    fun mmToPixels(sizeMm: Int, dpi: Int = 96): Int {
        val px = (sizeMm * dpi / 25.4).toInt()
        return px.coerceIn(100, 2000)
    }

    companion object {
        val DEFAULT = DICT_4X4_50

        fun fromLabel(label: String): ArucoDictionary =
            entries.find { it.label == label } ?: DEFAULT

        fun fromName(name: String): ArucoDictionary =
            entries.find { it.name == name } ?: DEFAULT
    }
}
