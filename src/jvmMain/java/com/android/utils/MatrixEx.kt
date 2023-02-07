package com.android.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity

val Matrix.isTranslationOnly: Boolean get() {
    val x = values[Matrix.TranslateX]
    val y = values[Matrix.TranslateY]
    val z = values[Matrix.TranslateZ]
    val matrix = Matrix()
    matrix.setFrom(this)
    values[Matrix.TranslateX] = 0f
    values[Matrix.TranslateY] = 0f
    values[Matrix.TranslateZ] = 0f
    val result = isIdentity()
    values[Matrix.TranslateX] = x
    values[Matrix.TranslateY] = y
    values[Matrix.TranslateZ] = z
    return result
}

val Matrix.hasScale: Boolean get() {
    val a1 = Offset(0f, 1f)
    val a2 = Offset(1f, 0f)
    val b1 = map(a1)
    val b2 = map(a2)
    val isLine1LengthChanged = b1 dot b1 != 1f
    val isLine2LengthChanged = b2 dot b2 != 1f
    return isLine1LengthChanged || isLine2LengthChanged
}

private infix fun Offset.dot(other: Offset) = x * other.x + y * other.y

fun Matrix.skew(skewX: Float, skewY: Float) {
    setFrom(
        Matrix().apply {
            values[Matrix.SkewX] = skewX
            values[Matrix.SkewY] = skewY
        }.apply {
            timesAssign(this@skew)
        }
    )
}

fun Matrix.mapWithoutTranslate(point: Offset): Offset {
    val x = values[Matrix.TranslateX]
    val y = values[Matrix.TranslateY]
    val z = values[Matrix.TranslateZ]
    values[Matrix.TranslateX] = 0f
    values[Matrix.TranslateY] = 0f
    values[Matrix.TranslateZ] = 0f
    val result = map(point)
    values[Matrix.TranslateX] = x
    values[Matrix.TranslateY] = y
    values[Matrix.TranslateZ] = z
    return result
}
val Matrix.determinant: Double
    get() {
        // copy from Matrix.invert
        val a00 = this[0, 0]
        val a01 = this[0, 1]
        val a02 = this[0, 2]
        val a03 = this[0, 3]
        val a10 = this[1, 0]
        val a11 = this[1, 1]
        val a12 = this[1, 2]
        val a13 = this[1, 3]
        val a20 = this[2, 0]
        val a21 = this[2, 1]
        val a22 = this[2, 2]
        val a23 = this[2, 3]
        val a30 = this[3, 0]
        val a31 = this[3, 1]
        val a32 = this[3, 2]
        val a33 = this[3, 3]
        val b00 = a00 * a11 - a01 * a10
        val b01 = a00 * a12 - a02 * a10
        val b02 = a00 * a13 - a03 * a10
        val b03 = a01 * a12 - a02 * a11
        val b04 = a01 * a13 - a03 * a11
        val b05 = a02 * a13 - a03 * a12
        val b06 = a20 * a31 - a21 * a30
        val b07 = a20 * a32 - a22 * a30
        val b08 = a20 * a33 - a23 * a30
        val b09 = a21 * a32 - a22 * a31
        val b10 = a21 * a33 - a23 * a31
        val b11 = a22 * a33 - a23 * a32
        val det =
            (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06)
        return det.toDouble()
    }