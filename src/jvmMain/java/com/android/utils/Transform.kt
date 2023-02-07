package com.android.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity
import java.awt.geom.Point2D
import java.io.Serializable
import kotlin.math.PI

class Transform : Cloneable, Serializable {
    val isIdentity: Boolean get() = matrix.isIdentity()
    val isTranslationOnly: Boolean get() = matrix.isTranslationOnly
    val hasScale: Boolean get() = matrix.hasScale

    private var matrix: Matrix

    constructor() {
        matrix = Matrix()
    }

    constructor(
        scaleX: Double,
        shearY: Double,
        shearX: Double,
        scaleY: Double,
        translateX: Double,
        translateY: Double
    ) {
        matrix = Matrix()
        setFrom(scaleX, shearY, shearX, scaleY, translateX, translateY)
    }

    val determinant: Double get() = matrix.determinant

    fun translate(x: Double, y: Double) = matrix.translate(x.toFloat(), y.toFloat())

    fun rotate(angleRad: Double, pivotX: Double = 0.0, pivotY: Double = 0.0) {
        matrix.translate(pivotX.toFloat(), pivotY.toFloat())
        matrix.rotateZ((angleRad / PI * 180).toFloat())
        matrix.translate(-pivotX.toFloat(), -pivotY.toFloat())
    }

    fun scale(scaleX: Double, scaleY: Double) = matrix.scale(scaleX.toFloat(), scaleY.toFloat())
    fun skew(skewX: Double, skewY: Double) = matrix.skew(skewX.toFloat(), skewY.toFloat())
    fun reset() = matrix.reset()
    fun setFrom(transform: Transform) = matrix.setFrom(transform.matrix)

    fun setFrom(
        scaleX: Double, shearY: Double,
        shearX: Double, scaleY: Double,
        translateX: Double, translateY: Double
    ) {
        matrix.reset()
        matrix.values[Matrix.ScaleX] = scaleX.toFloat()
        matrix.values[Matrix.ScaleY] = scaleY.toFloat()
        matrix.values[Matrix.SkewX] = shearX.toFloat()
        matrix.values[Matrix.SkewY] = shearY.toFloat()
        matrix.values[Matrix.TranslateX] = translateX.toFloat()
        matrix.values[Matrix.TranslateY] = translateY.toFloat()
    }

    fun concatenate(transform: Transform) {
        matrix.setFrom(
            Matrix(transform.matrix.values.clone()).apply {
                timesAssign(matrix)
            }
        )
    }

    fun preConcatenate(transform: Transform) {
        matrix *= transform.matrix
    }

    fun map(src: Point2D, dst: Point2D? = null): Point2D {
        val resultJava = dst ?: Point2D.Double()
        val result = matrix.map(Offset(src.x.toFloat(), src.y.toFloat()))
        resultJava.setLocation(result.x.toDouble(), result.y.toDouble())
        return resultJava
    }

    fun map(
        src: FloatArray, srcOffset: Int,
        dst: FloatArray, dstOffset: Int,
        count: Int
    ) {
        apply(src, srcOffset, dst, dstOffset, count) {
            matrix.map(it)
        }
    }

    fun map(
        src: DoubleArray, srcOffset: Int,
        dst: DoubleArray, dstOffset: Int,
        count: Int
    ) {
        apply(src, srcOffset, dst, dstOffset, count) {
            matrix.map(it)
        }
    }

    fun mapWithoutTranslate(src: Point2D, dst: Point2D? = null): Point2D {
        val resultJava = dst ?: Point2D.Double()
        val result = matrix.mapWithoutTranslate(Offset(src.x.toFloat(), src.y.toFloat()))
        resultJava.setLocation(result.x.toDouble(), result.y.toDouble())
        return resultJava
    }

    fun mapWithoutTranslate(
        src: DoubleArray, srcOffset: Int,
        dst: DoubleArray, dstOffset: Int,
        count: Int
    ) {
        apply(src, srcOffset, dst, dstOffset, count) {
            matrix.mapWithoutTranslate(it)
        }
    }

    private inline fun apply(
        src: DoubleArray, srcOffset: Int,
        dst: DoubleArray, dstOffset: Int,
        count: Int,
        action: (Offset) -> Offset
    ) {
        var n = count
        var s = srcOffset
        var d = dstOffset
        while (--n >= 0) {
            val x = src[s++];
            val y = src[s++];
            val result = action(Offset(x.toFloat(), y.toFloat()))
            dst[d++] = result.x.toDouble()
            dst[d++] = result.y.toDouble()
        }
    }

    private inline fun apply(
        src: FloatArray, srcOffset: Int,
        dst: FloatArray, dstOffset: Int,
        count: Int,
        action: (Offset) -> Offset
    ) {
        var n = count
        var s = srcOffset
        var d = dstOffset
        while (--n >= 0) {
            val x = src[s++];
            val y = src[s++];
            val result = action(Offset(x, y))
            dst[d++] = result.x
            dst[d++] = result.y
        }
    }
}
