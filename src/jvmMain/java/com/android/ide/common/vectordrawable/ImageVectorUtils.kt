package com.android.ide.common.vectordrawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser

object ImageVectorUtils {

    @JvmStatic
    fun addPath(builder: ImageVector.Builder, pathData: String, fill: Long) {
        builder.addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color(fill))
        )
    }
}