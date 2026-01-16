package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 7-sided rounded polygon shape (Heptagon) commonly referred to as "Cookie" shape in some design systems.
 */
class SevenSidedCookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f
        
        val sides = 7
        val angleStep = 2 * PI / sides
        // Start at -PI/2 to have a vertex pointing up
        val startAngle = -PI / 2
        
        val vertices = (0 until sides).map { i ->
            val angle = startAngle + i * angleStep
            Offset(
                centerX + radius * cos(angle).toFloat(),
                centerY + radius * sin(angle).toFloat()
            )
        }
        
        // Fraction of the side length used for the rounded corner (0.0 to 0.5)
        // 0.5 means the curve starts at the midpoint of the side (maximum roundness without overlap)
        val cornerFraction = 0.35f
        
        fun lerp(start: Offset, end: Offset, fraction: Float): Offset {
            return Offset(
                start.x + (end.x - start.x) * fraction,
                start.y + (end.y - start.y) * fraction
            )
        }
        
        for (i in 0 until sides) {
            val current = vertices[i]
            val prev = vertices[(i - 1 + sides) % sides]
            val next = vertices[(i + 1) % sides]
            
            // Point on the segment towards previous vertex
            val pointA = lerp(current, prev, cornerFraction)
            // Point on the segment towards next vertex
            val pointB = lerp(current, next, cornerFraction)
            
            if (i == 0) {
                path.moveTo(pointA.x, pointA.y)
            } else {
                path.lineTo(pointA.x, pointA.y)
            }
            // Draw curve around the corner
            path.quadraticBezierTo(current.x, current.y, pointB.x, pointB.y)
        }
        
        path.close()
        return Outline.Generic(path)
    }
}
