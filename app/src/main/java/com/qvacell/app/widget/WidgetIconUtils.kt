package com.qvacell.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath

fun shapeBackgroundBitmap(sizePx: Int, color: Int, shape: String): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val s = sizePx.toFloat()
    val cx = s / 2f; val cy = s / 2f
    when (shape) {
        "circle"        -> canvas.drawCircle(cx, cy, s / 2f, paint)
        "square"        -> canvas.drawRect(0f, 0f, s, s, paint)
        "rounded_square"-> canvas.drawRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, paint)
        "squircle"      -> canvas.drawRoundRect(RectF(0f, 0f, s, s), s * 0.40f, s * 0.40f, paint)
        "hexagon" -> {
            val path = Path(); val r = s * 0.48f
            for (i in 0 until 6) {
                val a = (Math.PI * i / 3 - Math.PI / 6).toFloat()
                val x = cx + r * Math.cos(a.toDouble()).toFloat()
                val y = cy + r * Math.sin(a.toDouble()).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close(); canvas.drawPath(path, paint)
        }
        "star" -> {
            val path = Path(); val outerR = s * 0.48f; val innerR = s * 0.22f
            for (i in 0 until 10) {
                val a = (Math.PI * i / 5 - Math.PI / 2).toFloat()
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (Math.cos(a.toDouble()) * r).toFloat()
                val y = cy + (Math.sin(a.toDouble()) * r).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close(); canvas.drawPath(path, paint)
        }
        "flower" -> {
            val path = Path(); val r = s * 0.30f; val cr = s * 0.42f
            path.moveTo(cx, cy - r)
            path.cubicTo(cx + cr, cy - r, cx + r, cy - cr, cx + r, cy)
            path.cubicTo(cx + r, cy + cr, cx + cr, cy + r, cx, cy + r)
            path.cubicTo(cx - cr, cy + r, cx - r, cy + cr, cx - r, cy)
            path.cubicTo(cx - r, cy - cr, cx - cr, cy - r, cx, cy - r)
            path.close(); canvas.drawPath(path, paint)
        }
        "diamond" -> {
            val path = Path()
            path.moveTo(cx, 0f); path.lineTo(s, cy)
            path.lineTo(cx, s); path.lineTo(0f, cy)
            path.close(); canvas.drawPath(path, paint)
        }
        "rounded_diamond" -> {
            val path = Path(); val d = s * 0.10f / Math.sqrt(2.0).toFloat()
            path.moveTo(cx - d, d); path.quadTo(cx, 0f, cx + d, d)
            path.lineTo(s - d, cy - d); path.quadTo(s, cy, s - d, cy + d)
            path.lineTo(cx + d, s - d); path.quadTo(cx, s, cx - d, s - d)
            path.lineTo(d, cy + d); path.quadTo(0f, cy, d, cy - d)
            path.close(); canvas.drawPath(path, paint)
        }
        "rounded_hexagon" -> {
            val path = Path(); val r = s * 0.46f; val cr = s * 0.07f
            val verts = Array(6) { i ->
                val a = Math.PI * i / 3 - Math.PI / 6
                floatArrayOf(cx + r * Math.cos(a).toFloat(), cy + r * Math.sin(a).toFloat())
            }
            for (i in 0 until 6) {
                val prev = verts[(i + 5) % 6]; val curr = verts[i]; val next = verts[(i + 1) % 6]
                fun len(dx: Float, dy: Float) = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val dx1 = curr[0] - prev[0]; val dy1 = curr[1] - prev[1]; val l1 = len(dx1, dy1)
                val dx2 = next[0] - curr[0]; val dy2 = next[1] - curr[1]; val l2 = len(dx2, dy2)
                val fx = curr[0] - cr * dx1 / l1; val fy = curr[1] - cr * dy1 / l1
                val tx = curr[0] + cr * dx2 / l2; val ty = curr[1] + cr * dy2 / l2
                if (i == 0) path.moveTo(fx, fy) else path.lineTo(fx, fy)
                path.quadTo(curr[0], curr[1], tx, ty)
            }
            path.close(); canvas.drawPath(path, paint)
        }
        else -> canvas.drawRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, paint)
    }
    return bitmap
}

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
