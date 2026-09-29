package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private const val DegToRad = (Math.PI / 180.0).toFloat()
private val Sqrt2 = sqrt(2f)

/**
 * One smoothed corner, solved.
 *
 * A plain rounded corner is a circular arc bolted onto two straight edges: curvature jumps from
 * zero to 1/R at the join and back again. The eye reads that discontinuity as a seam, which is why
 * an iOS icon looks softer than a `border-radius` of the same number. A smoothed corner spends
 * part of each edge ramping the curvature up before the arc and back down after it, so the
 * curvature is continuous the whole way round.
 *
 * The construction is Figma's, from *Desperately seeking squircles*: two cubic Béziers with a
 * circular arc between them. [smoothing] is the same 0..1 slider — 0 is an ordinary rounded
 * rectangle and 0.6 is the value Apple uses for app icons.
 *
 * ```
 *   p = (1 + s)·R      the edge the corner consumes, measured back from the apex
 *   β = 90°·(1 − s)    what is left of the circular arc
 *   θ = 45°·s          how far the arc's start is rotated away from the edge
 *   c, d               the offset of the arc's first point from the edge
 *   a = 2b             the Bézier handles, two thirds / one third of what is left
 * ```
 *
 * [budget] is how much edge this corner is allowed to eat before it would collide with its
 * neighbour. Smoothing is the first thing to give when the edge is too short to hold it: the
 * corner keeps the radius it was asked for and simply stops being smoothed. A capsule, whose
 * radius already eats the whole half-edge, lands on exactly zero smoothing — which is correct,
 * because a capsule has no straight edge to ramp into.
 */
private class SmoothCorner(val radius: Float, requestedSmoothing: Float, budget: Float) {

    /** How much of each adjacent edge the corner consumes, apex inwards. */
    val edge: Float

    private val handleFar: Float
    private val handleNear: Float
    private val arcInset: Float
    private val arcRise: Float
    private val arcDegrees: Float
    private val thetaDegrees: Float

    init {
        if (radius <= 0f || budget <= 0f) {
            edge = 0f
            handleFar = 0f; handleNear = 0f; arcInset = 0f; arcRise = 0f
            arcDegrees = 0f; thetaDegrees = 0f
        } else {
            val smoothing = min(requestedSmoothing, budget / radius - 1f).coerceIn(0f, 1f)
            edge = min((1f + smoothing) * radius, budget)
            thetaDegrees = 45f * smoothing
            arcDegrees = 90f * (1f - smoothing)
            val theta = thetaDegrees * DegToRad
            val halfTan = tan(theta / 2f)
            arcInset = radius * halfTan * cos(theta)
            arcRise = radius * halfTan * sin(theta)
            val chord = sin(arcDegrees * DegToRad / 2f) * radius * Sqrt2
            handleNear = (edge - chord - arcInset - arcRise) / 3f
            handleFar = 2f * handleNear
        }
    }

    /**
     * Draws the corner into [path], which must already be at this corner's start point.
     *
     * The corner is described once and used four times. [apex] is the corner of the bounding box;
     * `in` points from it back along the edge the path arrives on and `out` along the edge it
     * leaves by, so every point is the apex plus some distance along one or both. [baseAngle] is
     * where the arc would start with no smoothing — the direction of `-out` — and smoothing
     * rotates the start by θ while shortening the sweep to β.
     */
    fun addTo(
        path: Path,
        apexX: Float, apexY: Float,
        inX: Float, inY: Float,
        outX: Float, outY: Float,
        baseAngle: Float,
    ) {
        if (radius <= 0f) {
            path.lineTo(apexX, apexY)
            return
        }
        fun x(alongIn: Float, alongOut: Float) = apexX + inX * alongIn + outX * alongOut
        fun y(alongIn: Float, alongOut: Float) = apexY + inY * alongIn + outY * alongOut

        val toArc = edge - handleFar - handleNear - arcInset
        path.cubicTo(
            x(edge - handleFar, 0f), y(edge - handleFar, 0f),
            x(edge - handleFar - handleNear, 0f), y(edge - handleFar - handleNear, 0f),
            x(toArc, arcRise), y(toArc, arcRise),
        )
        if (arcDegrees > 0f) {
            val cx = apexX + inX * radius + outX * radius
            val cy = apexY + inY * radius + outY * radius
            path.arcTo(
                Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                baseAngle + thetaDegrees,
                arcDegrees,
                forceMoveTo = false,
            )
        }
        path.cubicTo(
            x(0f, edge - handleFar - handleNear), y(0f, edge - handleFar - handleNear),
            x(0f, edge - handleFar), y(0f, edge - handleFar),
            x(0f, edge), y(0f, edge),
        )
    }
}

/**
 * A rounded rectangle whose corners have continuous curvature — the shape Apple calls a squircle
 * and Figma calls corner smoothing.
 *
 * Drop-in for `RoundedCornerShape`: it is a [CornerBasedShape], so it still answers `topStart` and
 * friends. That matters beyond tidiness — the liquid-glass lens reads the corner radii off the
 * shape to know how far in to refract, and it only does that for a `CornerBasedShape`.
 *
 * Deliberately not applied to everything. See [RoutineShapes] for which shapes take it and why.
 */
@Immutable
class ContinuousCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val smoothing: Float,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val ltr = layoutDirection == LayoutDirection.Ltr
        val tl = if (ltr) topStart else topEnd
        val tr = if (ltr) topEnd else topStart
        val br = if (ltr) bottomEnd else bottomStart
        val bl = if (ltr) bottomStart else bottomEnd

        val w = size.width
        val h = size.height
        // Each edge is shared by two corners, split in proportion to their radii — the same split
        // the base class already uses to keep the radii themselves from overlapping.
        val topLeft = SmoothCorner(tl, smoothing, min(share(w, tl, tr), share(h, tl, bl)))
        val topRight = SmoothCorner(tr, smoothing, min(share(w, tr, tl), share(h, tr, br)))
        val bottomRight = SmoothCorner(br, smoothing, min(share(w, br, bl), share(h, br, tr)))
        val bottomLeft = SmoothCorner(bl, smoothing, min(share(w, bl, br), share(h, bl, tl)))

        val path = Path()
        path.moveTo(0f, topLeft.edge)
        topLeft.addTo(path, 0f, 0f, 0f, 1f, 1f, 0f, baseAngle = 180f)
        path.lineTo(w - topRight.edge, 0f)
        topRight.addTo(path, w, 0f, -1f, 0f, 0f, 1f, baseAngle = -90f)
        path.lineTo(w, h - bottomRight.edge)
        bottomRight.addTo(path, w, h, 0f, -1f, -1f, 0f, baseAngle = 0f)
        path.lineTo(bottomLeft.edge, h)
        bottomLeft.addTo(path, 0f, h, 1f, 0f, 0f, -1f, baseAngle = 90f)
        path.close()
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): ContinuousCornerShape = ContinuousCornerShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContinuousCornerShape) return false
        return topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart &&
            smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }

    override fun toString(): String =
        "ContinuousCornerShape(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, " +
            "bottomStart=$bottomStart, smoothing=$smoothing)"
}

/** How much of [side] this corner may claim before it would meet its neighbour on that edge. */
private fun share(side: Float, own: Float, neighbour: Float): Float =
    if (own + neighbour <= 0f) side else side * own / (own + neighbour)

/** Every corner the same. */
fun ContinuousCornerShape(radius: Dp, smoothing: Float): ContinuousCornerShape =
    ContinuousCornerShape(
        CornerSize(radius), CornerSize(radius), CornerSize(radius), CornerSize(radius), smoothing,
    )

/** Per-corner, for shapes that meet an edge of the screen on one side. */
fun ContinuousCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    smoothing: Float,
): ContinuousCornerShape = ContinuousCornerShape(
    CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart), smoothing,
)
