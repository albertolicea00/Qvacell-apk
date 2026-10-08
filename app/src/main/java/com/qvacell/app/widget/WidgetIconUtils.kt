package com.qvacell.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath

fun imageVectorToBitmap(imageVector: ImageVector, sizePx: Int, tintArgb: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tintArgb
        style = Paint.Style.FILL
    }

    val scaleX = sizePx / imageVector.viewportWidth
    val scaleY = sizePx / imageVector.viewportHeight

    fun drawGroup(group: VectorGroup, parentMatrix: Matrix) {
        val matrix = Matrix(parentMatrix)
        if (group.rotation != 0f || group.scaleX != 1f || group.scaleY != 1f ||
            group.translationX != 0f || group.translationY != 0f
        ) {
            val local = Matrix()
            local.preTranslate(group.translationX, group.translationY)
            local.preRotate(group.rotation, group.pivotX, group.pivotY)
            local.preScale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
            matrix.preConcat(local)
        }

        for (node in group) {
            when (node) {
                is VectorPath -> {
                    val path = node.pathData.toAndroidPath()
                    path.transform(matrix)
                    canvas.drawPath(path, paint)
                }
                is VectorGroup -> drawGroup(node, matrix)
            }
        }
    }

    val rootMatrix = Matrix()
    rootMatrix.setScale(scaleX, scaleY)
    drawGroup(imageVector.root, rootMatrix)

    return bitmap
}

private fun List<PathNode>.toAndroidPath(): Path {
    val path = Path()
    var cx = 0f; var cy = 0f
    var lx = 0f; var ly = 0f // last control point

    forEach { node ->
        when (node) {
            is PathNode.MoveTo -> { path.moveTo(node.x, node.y); cx = node.x; cy = node.y; lx = cx; ly = cy }
            is PathNode.RelativeMoveTo -> { path.rMoveTo(node.dx, node.dy); cx += node.dx; cy += node.dy; lx = cx; ly = cy }
            is PathNode.LineTo -> { path.lineTo(node.x, node.y); cx = node.x; cy = node.y; lx = cx; ly = cy }
            is PathNode.RelativeLineTo -> { path.rLineTo(node.dx, node.dy); cx += node.dx; cy += node.dy; lx = cx; ly = cy }
            is PathNode.HorizontalTo -> { path.lineTo(node.x, cy); cx = node.x; lx = cx; ly = cy }
            is PathNode.RelativeHorizontalTo -> { path.rLineTo(node.dx, 0f); cx += node.dx; lx = cx; ly = cy }
            is PathNode.VerticalTo -> { path.lineTo(cx, node.y); cy = node.y; lx = cx; ly = cy }
            is PathNode.RelativeVerticalTo -> { path.rLineTo(0f, node.dy); cy += node.dy; lx = cx; ly = cy }
            is PathNode.CurveTo -> {
                path.cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
                lx = node.x2; ly = node.y2; cx = node.x3; cy = node.y3
            }
            is PathNode.RelativeCurveTo -> {
                path.rCubicTo(node.dx1, node.dy1, node.dx2, node.dy2, node.dx3, node.dy3)
                lx = cx + node.dx2; ly = cy + node.dy2; cx += node.dx3; cy += node.dy3
            }
            is PathNode.ReflectiveCurveTo -> {
                val rx = 2 * cx - lx; val ry = 2 * cy - ly
                path.cubicTo(rx, ry, node.x1, node.y1, node.x2, node.y2)
                lx = node.x1; ly = node.y1; cx = node.x2; cy = node.y2
            }
            is PathNode.RelativeReflectiveCurveTo -> {
                val rx = cx - lx; val ry = cy - ly
                path.rCubicTo(rx, ry, node.dx1, node.dy1, node.dx2, node.dy2)
                lx = cx + node.dx1; ly = cy + node.dy1; cx += node.dx2; cy += node.dy2
            }
            is PathNode.QuadTo -> {
                path.quadTo(node.x1, node.y1, node.x2, node.y2)
                lx = node.x1; ly = node.y1; cx = node.x2; cy = node.y2
            }
            is PathNode.RelativeQuadTo -> {
                path.rQuadTo(node.dx1, node.dy1, node.dx2, node.dy2)
                lx = cx + node.dx1; ly = cy + node.dy1; cx += node.dx2; cy += node.dy2
            }
            is PathNode.ReflectiveQuadTo -> {
                val rx = 2 * cx - lx; val ry = 2 * cy - ly
                path.quadTo(rx, ry, node.x, node.y)
                lx = rx; ly = ry; cx = node.x; cy = node.y
            }
            is PathNode.RelativeReflectiveQuadTo -> {
                val rx = cx - lx; val ry = cy - ly
                path.rQuadTo(rx, ry, node.dx, node.dy)
                lx = cx + rx; ly = cy + ry; cx += node.dx; cy += node.dy
            }
            is PathNode.ArcTo -> {
                // Approximate arc: straight line to end point (rare in Material Icons)
                path.lineTo(node.arcStartX, node.arcStartY)
                cx = node.arcStartX; cy = node.arcStartY; lx = cx; ly = cy
            }
            is PathNode.RelativeArcTo -> {
                path.rLineTo(node.arcStartDx, node.arcStartDy)
                cx += node.arcStartDx; cy += node.arcStartDy; lx = cx; ly = cy
            }
            PathNode.Close -> { path.close(); lx = cx; ly = cy }
        }
    }
    return path
}
