package com.aladin.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.aladin.android.SkyUiState
import com.aladin.android.SkyViewModel
import com.aladin.android.data.hips.TileRepository
import com.aladin.android.model.SkyPosition
import com.aladin.android.sky.SkyRenderer
import com.aladin.android.sky.VisibleTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import com.aladin.android.ui.theme.GridLine
import com.aladin.android.ui.theme.MarkerGreen
import com.aladin.android.ui.theme.MarkerRed
import com.aladin.android.ui.theme.SkyBlack
import com.aladin.android.ui.theme.TextPrimary
import kotlin.math.hypot

/**
 * Builds the Compose `ColorFilter` for view modes
 * (brightness/contrast/inversion) from settings. Returns null if everything
 * is at defaults (no cost in draw).
 *
 * The matrix operates on colors in [0,1]: out = sign·c·(v - 0.5) + 0.5 + b,
 * with sign = -1 if inverted, c = contrast, b = brightness. Identity with
 * default values (sign=1, c=1, b=0).
 */
private object SkyCanvasColorFilter {
    fun fromSettings(s: com.aladin.android.AppSettings): ColorFilter? {
        val invert = s.invert
        val c = s.contrast
        val b = s.brightness
        if (!invert && c == 1f && b == 0f) return null
        val sign = if (invert) -1f else 1f
        val off = -sign * c * 0.5f + 0.5f + b
        val m = floatArrayOf(
            sign * c, 0f, 0f, 0f, off,
            0f, sign * c, 0f, 0f, off,
            0f, 0f, sign * c, 0f, off,
            0f, 0f, 0f, 1f, 0f,
        )
        return ColorFilter.colorMatrix(ColorMatrix(m))
    }
}

/** Grid steps in degrees; chooses the smallest one that leaves ~6 lines on screen. */
private val GRID_STEPS_DEG = doubleArrayOf(0.5, 1.0, 2.0, 5.0, 10.0, 15.0, 30.0, 45.0, 90.0)

private fun gridStepDeg(fovDeg: Double): Double {
    for (s in GRID_STEPS_DEG) if (fovDeg / s <= 6.0) return s
    return 90.0
}

/**
 * Draws meridians (constant RA) and parallels (constant Dec) projected with the
 * live camera. The step adapts to the FoV (like the Aladin desktop grid, see
 * `ViewSimple.calculGrid`): the more zoom-out, the more spaced the lines. Each
 * curve is approximated by segments between sampled points; segments with a
 * huge screen jump (passing through the limb/antipode or RA wrap) are discarded
 * to avoid crossing the view with a spurious line.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordGrid(
    proj: com.aladin.android.sky.Projection,
    fovDeg: Double,
) {
    val step = gridStepDeg(fovDeg)
    val sampleStep = step.coerceAtLeast(2.0) / 6.0
    val maxJump = maxOf(size.width, size.height) * 1.5f
    val marginPx = 200f
    val minX = -marginPx; val maxX = size.width + marginPx
    val minY = -marginPx; val maxY = size.height + marginPx

    fun onScreen(p: Offset) = p.x in minX..maxX && p.y in minY..maxY

    fun drawCurve(points: List<FloatArray?>) {
        var prev: Offset? = null
        for (raw in points) {
            val pt = raw?.let { Offset(it[0], it[1]) }
            if (prev != null && pt != null) {
                if (hypot(pt.x - prev.x, pt.y - prev.y) < maxJump && (onScreen(prev) || onScreen(pt))) {
                    drawLine(GridLine.copy(alpha = 0.35f), prev, pt, strokeWidth = 1f)
                }
            }
            prev = pt
        }
    }

    // Meridians: constant RA, Dec from -90 to 90.
    var ra = 0.0
    while (ra < 360.0) {
        val pts = ArrayList<FloatArray?>()
        var dec = -90.0
        while (dec <= 90.0) {
            pts.add(proj.forward(ra, dec))
            dec += sampleStep
        }
        drawCurve(pts)
        ra += step
    }

    // Parallels: constant Dec (without exact poles), RA from 0 to 360.
    var dec = -90.0 + step
    while (dec < 90.0) {
        val pts = ArrayList<FloatArray?>()
        var raS = 0.0
        while (raS <= 360.0) {
            pts.add(proj.forward(raS, dec))
            raS += sampleStep
        }
        drawCurve(pts)
        dec += step
    }
}

/**
 * Full-screen Canvas rendering the HiPS sky + overlays and managing
 * touch gestures (pan, zoom, tap). 100% native Android UI.
 *
 * Tiles are requested outside of draw (in a LaunchedEffect) and draw only reads
 * the cache; invalidation is triggered by TileRepository (coalesced).
 */
@Composable
fun SkyCanvas(
    state: SkyUiState,
    vm: SkyViewModel,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val camera = state.camera
    val props = state.props
    val survey = state.survey
    val tileRepo = vm.tileRepo

    // Visible tiles: heavy computation (HEALPix recursion) OUTSIDE the main thread
    // to avoid blocking UI or triggering ANR.
    //
    // IMPORTANT: we collect the StateFlow instead of keying LaunchedEffect by the
    // camera. StateFlow is already conflated and collect processes emissions in
    // SEQUENCE WITHOUT canceling the ongoing recalculation. If we keyed by `camera`,
    // every pan/zoom tick (~60/s) would cancel the previous recalculation before
    // it finished, so new tiles would only be requested when stopping the gesture
    // ("only loads on stop"). With collect, each recalculation runs to completion
    // and gets assigned -> tiles come in progressively as you move, and zooming in
    // increases order (more quality), just like the original Aladin.
    var visibleTiles by remember { mutableStateOf(emptyList<VisibleTile>()) }
    LaunchedEffect(canvasSize) {
        vm.state.collect { s ->
            val p = s.props
            if (p != null && canvasSize.width > 0 && canvasSize.height > 0) {
                val proj = s.camera.projection(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                val tiles = withContext(Dispatchers.Default) {
                    SkyRenderer.computeVisibleTiles(proj, p, s.settings.quality.targetScale)
                }
                visibleTiles = tiles
            } else {
                visibleTiles = emptyList()
            }
        }
    }

    // Request missing tiles (outside draw). Re-runs when the visible set changes.
    LaunchedEffect(visibleTiles, survey, props) {
        if (props != null) {
            for (t in visibleTiles) {
                val sample = TileRepository.sampleSizeFor(t.scalePx)
                if (tileRepo.get(survey.id, t.order, t.npix, sample) == null) {
                    tileRepo.request(survey, props.ext, t.order, t.npix, t.scalePx)
                }
            }
        }
    }

    // Always-fresh values for the tap detector (which does not restart with camera).
    val cameraState = rememberUpdatedState(camera)
    val propsState = rememberUpdatedState(props)
    val measureModeState = rememberUpdatedState(state.measureMode)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            // Unified gesture detector (1-finger pan, 2-finger pinch-zoom
            // around the centroid, and tap). In a single pointerInput to avoid
            // detector conflicts that made pinch be interpreted as pan.
            // Standard image-viewer behavior.
            .pointerInput(Unit) {
                // Double-tap state persistent across gestures (for standard
                // image-viewer zoom: double tap zooms in/out).
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val firstPos = first.position
                    var lastCentroid = firstPos
                    var lastPinchDist = 0f
                    var totalMove = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.isEmpty()) break
                        active.forEach { it.consume() }
                        val cnt = active.size
                        val centroid = if (cnt == 1) active[0].position
                        else active.fold(Offset.Zero) { acc, c -> acc + c.position } / cnt.toFloat()

                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            val w = canvasSize.width.toFloat()
                            val h = canvasSize.height.toFloat()
                            if (cnt >= 2) {
                                // Pinch: zoom by the distance variation between 2 fingers.
                                val dist = (active[0].position - active[1].position).getDistance()
                                if (lastPinchDist > 1f && dist > 1f) {
                                    val zoomFactor = (dist / lastPinchDist).coerceIn(0.5f, 2.5f)
                                    if (zoomFactor != 1f) {
                                        vm.zoomBy(zoomFactor, w, h, centroid.x, centroid.y)
                                    }
                                }
                                // Pan by the displacement of the 2 fingers' centroid.
                                val panDelta = centroid - lastCentroid
                                if (panDelta != Offset.Zero) vm.panBy(panDelta.x, panDelta.y, w, h)
                                lastPinchDist = dist
                            } else {
                                // 1 finger: pan.
                                val panDelta = centroid - lastCentroid
                                if (panDelta != Offset.Zero) {
                                    vm.panBy(panDelta.x, panDelta.y, w, h)
                                    totalMove += panDelta.getDistance()
                                }
                            }
                        }
                        lastCentroid = centroid
                    }
                    // Tap: quick touch with almost no movement.
                    if (totalMove < 24f && System.currentTimeMillis() - downTime < 300) {
                        val now = System.currentTimeMillis()
                        val isDouble = (now - lastTapTime < 320) &&
                            (firstPos - lastTapPos).getDistance() < 48f
                        if (isDouble) {
                            // Double tap: standard image-viewer zoom.
                            // If we are already very close (<=1°), we zoom out; else zoom in 2.5x
                            // towards the tapped point. Same path as pinch (vm.zoomBy),
                            // so it exercises the whole zoom pipeline.
                            val w = canvasSize.width.toFloat()
                            val h = canvasSize.height.toFloat()
                            if (w > 0 && h > 0) {
                                val factor = if (cameraState.value.fovDeg <= 1.0) 0.4f else 2.5f
                                vm.zoomBy(factor, w, h, firstPos.x, firstPos.y)
                            }
                            lastTapTime = 0L
                        } else {
                            // Single tap (possible first of a double): identify/measure.
                            lastTapTime = now
                            lastTapPos = firstPos
                            val w = canvasSize.width; val h = canvasSize.height
                            val p = propsState.value
                            if (w > 0 && h > 0 && p != null) {
                                val proj = cameraState.value.projection(w.toFloat(), h.toFloat())
                                val radec = proj.inverse(firstPos.x, firstPos.y)
                                val pos = SkyPosition(radec[0], radec[1])
                                // Single tap: only acts in measure mode. Tap
                                // identification used Simbad, whose endpoint is
                                // unresponsive -> removed to avoid showing a
                                // misleading "no results". Measure mode works fine.
                                if (measureModeState.value) vm.measureTap(pos)
                            }
                        }
                    }
                }
            }
    ) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return@Canvas
        drawRect(SkyBlack, size = size)

        val proj = camera.projection(size.width, size.height)
        // Color filter for view modes (brightness/contrast/inversion).
        // Null if all at defaults -> zero cost.
        val tileColorFilter = SkyCanvasColorFilter.fromSettings(state.settings)
        if (props != null) {
            for (t in visibleTiles) {
                val sample = TileRepository.sampleSizeFor(t.scalePx)
                val bmp = tileRepo.get(survey.id, t.order, t.npix, sample) ?: continue
                // Guard: a recycled bitmap (evicted from cache between frames)
                // would cause a native crash on draw; skip it this frame.
                if (bmp.isRecycled) continue
                // Cell mesh with LIVE camera: each cell is placed with its
                // own affine (parallelogram) and its corresponding bitmap slice.
                // This way tile edges match (no gaps) and the tile moves
                // smoothly on pan/zoom without waiting to recalculate the set.
                val cells = SkyRenderer.placementMesh(proj, t, bmp.width, bmp.height) ?: continue
                val img = bmp.asImageBitmap()
                for (cell in cells) {
                    withTransform({
                        transform(Matrix(cell.matrix))
                    }) {
                        drawImage(
                            image = img,
                            srcOffset = IntOffset(cell.srcX, cell.srcY),
                            srcSize = IntSize(cell.srcW, cell.srcH),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(cell.srcW, cell.srcH),
                            colorFilter = tileColorFilter,
                        )
                    }
                }
            }
        }

        // Coordinate grid (RA/Dec), if enabled in Settings.
        if (state.settings.showGrid) {
            drawCoordGrid(proj, camera.fovDeg)
        }

        // Catalog overlays
        for (row in state.catalogRows) {
            val p = proj.forward(row.raDeg, row.decDeg) ?: continue
            val r = row.mag?.let { (8f - it.toFloat().coerceIn(0f, 20f) * 0.3f).coerceIn(1.5f, 5f) } ?: 2.5f
            drawCircle(color = MarkerGreen.copy(alpha = 0.85f), radius = r, center = Offset(p[0], p[1]))
        }

        // Manual markers
        for (m in state.markers) {
            val p = proj.forward(m.position.raDeg, m.position.decDeg) ?: continue
            drawCircle(color = androidx.compose.ui.graphics.Color(m.color), radius = 6f, center = Offset(p[0], p[1]))
            drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 6f, center = Offset(p[0], p[1]), style = Stroke(width = 1.5f))
        }

        // Measurements
        for (meas in state.measurements) {
            val a = proj.forward(meas.from.raDeg, meas.from.decDeg) ?: continue
            val b = proj.forward(meas.to.raDeg, meas.to.decDeg) ?: continue
            drawLine(MarkerRed, Offset(a[0], a[1]), Offset(b[0], b[1]), strokeWidth = 2f)
            drawCircle(MarkerRed, 4f, Offset(a[0], a[1]))
            drawCircle(MarkerRed, 4f, Offset(b[0], b[1]))
        }
        state.pendingMeasureFrom?.let { from ->
            proj.forward(from.raDeg, from.decDeg)?.let { p ->
                drawCircle(TextPrimary, 6f, Offset(p[0], p[1]))
            }
        }

        // Center crosshair
        val c = Offset(size.width / 2f, size.height / 2f)
        drawLine(GridLine, Offset(c.x - 12f, c.y), Offset(c.x + 12f, c.y), strokeWidth = 1f)
        drawLine(GridLine, Offset(c.x, c.y - 12f), Offset(c.x, c.y + 12f), strokeWidth = 1f)
    }
}