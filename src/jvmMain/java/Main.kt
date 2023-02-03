/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.android.ide.common.vectordrawable.Svg2Vector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path


val ic_insta = ImageVector.Builder(
    name = "ic_insta",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).path(
    fill = SolidColor(Color.Black),
    fillAlpha = 1f,
    stroke = null,
    strokeAlpha = 1f,
    strokeLineWidth = 1f,
    strokeLineCap = StrokeCap.Butt,
    strokeLineJoin = StrokeJoin.Bevel,
    strokeLineMiter = 1f,
    pathFillType = PathFillType.NonZero
) {
    moveTo(16.3f, 5.0f)
    curveToRelative(1.4f, 0.0f, 2.6f, 1.2f, 2.6f, 2.6f)
    verticalLineToRelative(8.8f)
    curveToRelative(0.0f, 1.4f, -1.2f, 2.6f, -2.6f, 2.6f)
    horizontalLineTo(7.6f)
    curveTo(6.2f, 19.0f, 5.0f, 17.8f, 5.0f, 16.4f)
    verticalLineTo(7.6f)
    curveTo(5.0f, 6.2f, 6.1f, 5.0f, 7.6f, 5.0f)
    horizontalLineToRelative(8.7f)
    moveToRelative(0.0f, -2.0f)
    horizontalLineTo(7.6f)
    curveTo(5.0f, 3.0f, 3.0f, 5.1f, 3.0f, 7.6f)
    verticalLineToRelative(8.8f)
    curveTo(3.0f, 18.9f, 5.1f, 21.0f, 7.6f, 21.0f)
    horizontalLineToRelative(8.8f)
    curveToRelative(2.5f, 0.0f, 4.6f, -2.1f, 4.6f, -4.6f)
    verticalLineTo(7.6f)
    curveTo(20.9f, 5.1f, 18.9f, 3.0f, 16.3f, 3.0f)
    close()
}.build()

@Composable
fun GreetingView(text: String) {
    Row(modifier = Modifier.background(Color.Gray)) {
        Image(
            rememberVectorPainter(image = useResource("ic_insta.svg", {Svg2Vector.sampleVector(it)})),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)
        )
        Image(
            rememberVectorPainter(image = useResource("test.svg", {Svg2Vector.sampleVector(it)})),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)
        )
        Image(
            rememberVectorPainter(ic_insta),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)

        )
        Image(
            painterResource("out.xml"),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)
        )
        Image(
            painterResource("ic_insta.xml"),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)
        )
        Image(
            painterResource("ic_insta.svg"),
            contentDescription = null,
            modifier = Modifier.padding(8.dp).requiredSize(100.dp)
        )
        Text(text = text)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() =
    singleWindowApplication(
        title = "SVG Parser",
        state = WindowState(size = DpSize(800.dp, 800.dp))
    ) {
        GreetingView("Hello!")
    }
