package com.android.utils

import java.awt.geom.AffineTransform as AffineTransformJava
import java.awt.geom.Point2D
import kotlin.Pair
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformTest {
    private var log = false

    private fun log(value: Any?) {
        if (log) {
            println(value)
        }
    }

    @Test
    fun test() {
        repeat(100000) {
            val (t1, t2) = constructRandom()
            applyRandomOperations(t1, t2)
            test(t1, t2)
            log("\n")
        }
    }

    private fun constructRandom(): Pair<AffineTransformJava, Transform> {
        return randomInvoke(
            { AffineTransformJava() to Transform() },
            { AffineTransformJava(2.0, 0.0, 0.0, 0.5, 0.0, 0.0) to Transform(2.0, 0.0, 0.0, 0.5, 0.0, 0.0) },
            { AffineTransformJava(1.0, 0.0, 0.0, 1.0, 5.0, -8.0) to Transform(1.0, 0.0, 0.0, 1.0, 5.0, -8.0) },
            { AffineTransformJava(1.0, 1.5, 3.0, 1.0, 0.0, 0.0) to Transform(1.0, 1.5, 3.0, 1.0, 0.0, 0.0) },
            { AffineTransformJava(2.0, 1.5, 3.0, 0.5, 5.0, -8.0) to Transform(2.0, 1.5, 3.0, 0.5, 5.0, -8.0) },
            { AffineTransformJava().apply { setTransform(2.0, 1.5, 3.0, 0.5, 5.0, -8.0) } to Transform().apply { setFrom(2.0, 1.5, 3.0, 0.5, 5.0, -8.0) } },
        )
    }

    private fun applyRandomOperations(p1: AffineTransformJava, p2: Transform) {
        repeat(Random.nextInt(4)) {
            applyRandomOperation(p1, p2)
        }
    }

    private fun applyRandomOperation(p1: AffineTransformJava, p2: Transform) {
        val x = Random.nextInt(10) / 5.0
        val y = Random.nextInt(10) / 5.0
        val z = Random.nextInt(10) / 5.0
        randomInvoke(
            {
                log("scale")
                p1.scale(x, y)
                p2.scale(x, y)
            },
            {
                log("rotate")
                p1.rotate(x, y, z)
                p2.rotate(x, y, z)
            },
            {
                log("translate")
                p1.translate(x, y)
                p2.translate(x, y)
            },
            {
                log("shear")
                p1.shear(x, y)
                p2.skew(x, y)
            },
            {
                log("concatenate")
                val b1 = AffineTransformJava()
                val b2 = Transform()
                applyRandomOperation(b1, b2)
                p1.concatenate(b1)
                p2.concatenate(b2)
            },
            {
                log("preConcatenate")
                val b1 = AffineTransformJava()
                val b2 = Transform()
                applyRandomOperation(b1, b2)
                p1.preConcatenate(b1)
                p2.preConcatenate(b2)
            },
        )
    }

    private fun test(t1: AffineTransformJava, t2: Transform) {
        repeat(10) {
            testRandomTransform(t1, t2)
            testRandomTransform2(t1, t2)
            testRandomDelta(t1, t2)
            testRandomDelta2(t1, t2)
            testDeterminant(t1, t2)
            testType(t1, t2)
        }
    }

    private fun testRandomTransform(t1: AffineTransformJava, t2: Transform) {
        val p1 = Point2D.Float()
        val p2 = Point2D.Float()
        val testPoint = randomPoint()
        t1.transform(testPoint, p1)
        t2.map(testPoint, p2)
        assertEquals(p1.x, p2.x, 0.01f)
        assertEquals(p1.y, p2.y, 0.01f)
    }

    private fun testRandomTransform2(t1: AffineTransformJava, t2: Transform) {
        val p1 = DoubleArray(4)
        val p2 = DoubleArray(4)
        val testArr = doubleArrayOf(randomCoord(), randomCoord(), randomCoord(), randomCoord(), randomCoord())
        t1.transform(testArr, 0, p1, 0, 2)
        t2.map(testArr, 0, p2, 0, 2)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.transform(testArr, 1, p1, 0, 2)
        t2.map(testArr, 1, p2, 0, 2)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.transform(testArr, 1, p1, 0, 0)
        t2.map(testArr, 1, p2, 0, 0)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.transform(testArr, 2, p1, 0, 1)
        t2.map(testArr, 2, p2, 0, 1)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
    }

    private fun testRandomDelta(t1: AffineTransformJava, t2: Transform) {
        val p1 = Point2D.Float()
        val p2 = Point2D.Float()
        val testPoint = randomPoint()
        val d1 = t1.deltaTransform(testPoint, p1)
        val d2 = t2.mapWithoutTranslate(testPoint, p2)
        assertEquals(p1.x, p2.x, 0.01f)
        assertEquals(p1.y, p2.y, 0.01f)
        assertEquals(d1.x, d2.x, 0.01)
        assertEquals(d1.y, d2.y, 0.01)
    }

    private fun testRandomDelta2(t1: AffineTransformJava, t2: Transform) {
        val p1 = DoubleArray(4)
        val p2 = DoubleArray(4)
        val testArr = doubleArrayOf(randomCoord(), randomCoord(), randomCoord(), randomCoord(), randomCoord())
        t1.deltaTransform(testArr, 0, p1, 0, 2)
        t2.mapWithoutTranslate(testArr, 0, p2, 0, 2)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.deltaTransform(testArr, 1, p1, 0, 2)
        t2.mapWithoutTranslate(testArr, 1, p2, 0, 2)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.deltaTransform(testArr, 1, p1, 0, 0)
        t2.mapWithoutTranslate(testArr, 1, p2, 0, 0)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
        t1.deltaTransform(testArr, 2, p1, 0, 1)
        t2.mapWithoutTranslate(testArr, 2, p2, 0, 1)
        assertArrayEquals(p1, p2, 0.01)
        assertArrayEquals(p1, p2, 0.01)
    }

    private fun testDeterminant(t1: AffineTransformJava, t2: Transform) {
        assertEquals(t1.determinant, t2.determinant, 0.01)
    }

    private fun testType(t1: AffineTransformJava, t2: Transform) {
        assertEquals(t1.isIdentity, t2.isIdentity)
        assertEquals(
            t1.type == AffineTransformJava.TYPE_IDENTITY || t1.type == AffineTransformJava.TYPE_TRANSLATION,
            t2.isTranslationOnly
        )
    }

    private fun randomPoint() = Point2D.Double(randomCoord(), randomCoord())
    private fun randomCoord() = Random.nextInt(-10, 10) / 3.0

    private fun <T> randomInvoke(vararg actions: () -> T): T = actions.random().invoke()
}