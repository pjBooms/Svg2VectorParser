package com.android.ide.common.vectordrawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser

class ImageVectorBuilder(val builder: ImageVector.Builder) {

    fun addPath(pathData: String, fill: Long) {
        builder.addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color(fill))
        )
    }

}