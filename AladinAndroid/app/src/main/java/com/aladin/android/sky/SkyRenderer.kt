@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.sky

import com.aladin.android.data.hips.HiPSProperties
import kotlin.math.hypot

/**
 * Visible tile. We keep the corners and center in celestial coordinates
 * (ra/dec degrees), NOT in screen pixels: this way draw projects them with the
 * LIVE camera and pan/zoom moves the tiles smoothly without waiting to
 * recalculate the set of visible tiles.
 *
 * `scalePx` is a representative value (screen px per native tile px)
 * calculated at compute time, only to decide the `inSampleSize`.
 */
data class VisibleTile(
    val order: Int,
    val npix: Long,
    val cornersRadec: Array<DoubleArray>,
    val centerRadec: DoubleArray,
    val scalePx: Float,
    /** True if this tile stopped subdividing here: it's the correct resolution for its
     * on-screen size, not just an intermediate progressive-refinement base layer. */
    val isLeaf: Boolean = true,
)

/**
 * A cell in the mesh into which a tile is subdivided for placement: the 4x4
 * affine matrix that places it on screen and the bitmap slice (srcOffset/srcSize)
 * to paint in it.
 */
data class MeshCell(
    val matrix: FloatArray,
    val srcX: Int,
    val srcY: Int,
    val srcW: Int,
    val srcH: Int,
)

/**
 * Tile placement on screen as a 4x4 affine matrix (16 floats, layout of
 * `androidx.compose.ui.graphics.Matrix`). The bitmap is drawn at (0,0) and the
 * matrix maps its pixel coordinates to the screen.
 */
private fun affineMatrix(o: FloatArray, e1: FloatArray, e3: FloatArray, w: Float, h: Float): FloatArray {
    // x' = ax*u + bx*v + ox ; y' = ay*u + by*v + oy  (u,v = bitmap pixel coords)
    val ax = (e1[0] - o[0]) / w
    val ay = (e1[1] - o[1]) / w
    val bx = (e3[0] - o[0]) / h
    val by = (e3[1] - o[1]) / h
    return floatArrayOf(
        ax, ay, 0f, 0f,
        bx, by, 0f, 0f,
        0f, 0f, 1f, 0f,
        o[0], o[1], 0f, 1f,
    )
}

/**
 * Computes which HiPS tiles to draw for a given projection, by recursing
 * the NESTED tree (the children of (order,npix) are 4*npix+{0,1,2,3} at order+1).
 * Only needs HEALPix forward (center and corners).
 */
object SkyRenderer {

    /**
     * Culling margin (px) beyond the screen: preloads the immediate edge
     * so panning doesn't show black.
     */
    private const val CULL_MARGIN_PX = 512f

    /**
     * Default scale threshold to consider a tile "enough detail" and
     * stop subdividing. `scalePx` = screen px per native tile px
     * (projected). We subdivide only while the tile is UPSCALED beyond
     * its native resolution (scalePx > targetScale); beyond that, subdividing
     * adds no real detail (more tiles for the SAME native image) and only spikes
     * the tile count -> endless loading.
     *
     * This mirrors `maxOrder(v)` from Aladin desktop, which picks the order where
     * ~1 tile px ≈ 1 screen px (with slight oversampling for sharpness).
     * We used to do `bboxSpan <= 280`, but the HiPS tile is a rhombus and its
     * axis-aligned bbox is ~1.4x the side, so it subdivided one order too many: at
     * moderate FoV (e.g. 2°) it spawned hundreds of ~60px tiles (452!) and
     * loading at 3s/tile became eternal. With scalePx the count stays at
     * ~8-30 tiles per screen across the whole zoom range.
     *
     * The actual value is provided by the `targetScale` param in computeVisibleTiles,
     * adjustable from Settings (TileQuality).
     */
    private const val DEFAULT_TARGET_SCALE = 1.5f

    /** Hard limit of visible tiles: avoids pathological avalanches (poles / extreme FoV). */
    private const val MAX_TILES = 600

    fun computeVisibleTiles(
        proj: Projection,
        props: HiPSProperties,
        targetScale: Float = DEFAULT_TARGET_SCALE,
    ): List<VisibleTile> {
        val startOrder = props.orderMin.coerceIn(0, props.order.coerceAtLeast(props.orderMin))
        val maxOrder = props.order
        val tileW = props.tileWidth.toFloat()

        // BFS by levels (order), NOT DFS. We used to traverse the NESTED tree
        // depth-first for each base tile (12 at order 0): a single branch could
        // go down to order 9 and exhaust the MAX_TILES budget on its own. At normal
        // FoV (~2°) there is only 1 base tile on screen, so it was never noticed.
        // But when zooming out completely there are up to 12 base tiles visible at
        // once, and with DFS the last ones in iteration order (0 until npix)
        // were left WITHOUT ANY tile -not even the coarse one- because the first
        // branch had consumed the quota: hence the "it goes totally black"
        // when zooming out a lot (those areas weren't even requested). With BFS
        // we ALWAYS complete a full level (everything visible at that order)
        // before subdividing to the next, so a wide FoV simply stays at a coarser
        // order showing the COMPLETE sky instead of black parts. This is also
        // what Aladin desktop does (PlanBG.drawLosangesAsync iterates order by order, cmin..max).
        var level = ArrayList<Long>(Healpix.npix(startOrder).toInt().coerceAtMost(12))
        for (ipix in 0 until Healpix.npix(startOrder)) level.add(ipix)

        val out = ArrayList<VisibleTile>()
        var order = startOrder
        while (level.isNotEmpty()) {
            val survivors = ArrayList<Long>()
            for (ipix in level) {
                val corners = Healpix.corners(order, ipix)
                val centerRadec = Healpix.center(order, ipix)
                val pts = ArrayList<FloatArray>(4)
                for (c in corners) proj.forward(c[0], c[1])?.let { pts.add(it) }
                val centerPx = proj.forward(centerRadec[0], centerRadec[1])

                // A tile with NO corner and no center passing the strict cutoff is
                // genuinely fully on the far side and must be excluded outright.
                // Using forwardClamped for those would still produce a (heavily
                // distorted) on-screen position, pulling completely unrelated,
                // wrong sky onto the view -- a second "layer" of stars overlapping
                // the real one.
                if (pts.isEmpty() && centerPx == null) continue

                // For the tiles that DO have some part in the visible hemisphere,
                // build the culling bbox from all 4 corners, substituting
                // forwardClamped only for the ones that individually failed the
                // strict cutoff. A coarse/large tile can have its center (and even
                // most corners) fail while a real edge is still on screen; bbox-ing
                // from `pts` alone (only the successful ones, possibly zero) used
                // to drop those tiles entirely -- a whole-tile-shaped black gap at
                // wide FoV, distinct from (and not fixed by) placementMesh's
                // clamping of the draw mesh itself.
                val boundsCorners = corners.map { c -> proj.forward(c[0], c[1]) ?: proj.forwardClamped(c[0], c[1]) }
                val margin = CULL_MARGIN_PX
                val bbox = bbox(boundsCorners)
                val intersects = bbox != null &&
                    bbox[2] >= -margin && bbox[0] <= proj.widthPx + margin &&
                    bbox[3] >= -margin && bbox[1] <= proj.heightPx + margin
                if (!intersects) continue

                // Representative scale (screen px per native tile px). Using boundsCorners
                // ensures a reliable scale even for tiles along the visible horizon where
                // plain forward() returns null for some corners.
                val scalePx = if (boundsCorners.size >= 2) {
                    val len = hypot(boundsCorners[1][0] - boundsCorners[0][0], boundsCorners[1][1] - boundsCorners[0][1])
                    if (len > 0f) len / tileW else 1f
                } else {
                    1f
                }

                // Leaf when the tile is at native resolution or finer (scalePx <= targetScale).
                // Previously, tiles straddling the horizon (pts.size < 4) were forced to keep
                // subdividing all the way down to maxOrder to mask distortion caused by a bug
                // in forwardClamped. That endless horizon subdivision quickly exceeded MAX_TILES,
                // aborting the level-by-level BFS mid-way and causing cmin (branchMax - 3) to
                // discard coarse base layers, leaving solid-black holes across the sky. With
                // forwardClamped projecting correctly to the perimeter, horizon tiles render
                // smoothly at their proper resolution without forced subdivision.
                val enoughDetail = scalePx <= targetScale
                val willSubdivide = order < maxOrder && !enoughDetail
                val isLeaf = !willSubdivide

                // We emit ALL on-screen nodes (not just leaves): draw uses the
                // coarse ancestor as a base for progressive refinement.
                out.add(VisibleTile(order, ipix, corners, centerRadec, scalePx, isLeaf))

                if (willSubdivide) {
                    survivors.add(ipix)
                }
            }
            // We only check the budget AFTER completing the whole level
            // (never halfway): this way a coarse level is always complete, without
            // gaps, even if it slightly exceeds MAX_TILES (e.g. order 3 = 768
            // base tiles on the whole sphere, > 600, but still allowed).
            if (survivors.isEmpty() || out.size >= MAX_TILES) break
            val nextLevel = ArrayList<Long>(survivors.size * 4)
            for (p in survivors) {
                nextLevel.add(4 * p); nextLevel.add(4 * p + 1)
                nextLevel.add(4 * p + 2); nextLevel.add(4 * p + 3)
            }
            level = nextLevel
            order++
        }

        // Progressive refinement (identical to Aladin desktop —see
        // PlanBG.drawLosangesAsync, which draws ready tiles from every order
        // cmin..max, coarse first as base and fine on top). We keep only
        // the top ~4 levels of EACH BRANCH (base cell from startOrder),
        // NOT a global cmin.
        //
        // BUG this fixes: with a global cmin (maxObs of ALL out, cmin =
        // maxObs-3 applied to ALL tiles), at a wide FoV different branches
        // (different base cells visible at once) reach very different depths
        // -one near the center might reach order 8 while another, more
        // deformed/smaller on screen, already has "enoughDetail" at order 2-.
        // With global cmin = 8-3 = 5, the branch that stopped at order 2 loses ALL
        // its tiles (2 < 5): that screen region gets no tiles at all, not even
        // the coarse one -> total black. That is exactly what happened when zooming
        // out completely. By computing cmin PER BRANCH, each base cell keeps its
        // own top ~4 levels relative to ITS OWN max depth, so every visible branch
        // always has at least one tile.
        if (out.isEmpty()) return out
        val floorOrder = props.orderMin.coerceAtLeast(0)
        val branchMaxOrder = HashMap<Long, Int>()
        for (t in out) {
            val shift = 2 * (t.order - startOrder)
            val branch = if (shift >= 0) t.npix shr shift else t.npix
            val cur = branchMaxOrder[branch]
            if (cur == null || t.order > cur) branchMaxOrder[branch] = t.order
        }
        // Keep the top ~4 levels of each branch as progressive-refinement layers
        // (coarse base -> fine top). BUT a leaf -- a tile that stopped subdividing
        // because it already had enough on-screen detail (scalePx <= targetScale,
        // all 4 corners project) -- is the *finest* content layer for ITS specific
        // sub-area, and it has no descendants in `out` (it never subdivided). If a
        // sibling sub-area of the same branch reached a much higher order, cmin =
        // branchMax-3 would drop this leaf even though nothing finer covers its
        // area -> that patch renders solid black with the bitmap already downloaded.
        // So leaves are always kept, independent of cmin; cmin only prunes redundant
        // coarse *non-leaf* ancestors below the kept window.
        val filtered = ArrayList<VisibleTile>(out.size)
        for (t in out) {
            val shift = 2 * (t.order - startOrder)
            val branch = if (shift >= 0) t.npix shr shift else t.npix
            val branchMax = branchMaxOrder[branch] ?: t.order
            val cmin = (branchMax - 3).coerceAtLeast(floorOrder)
            if (t.isLeaf || t.order >= cmin) filtered.add(t)
        }
        // Ascending order: coarse (low order) first = base; fine (high order)
        // after = drawn on top. Without this, a coarse tile drawn after a fine
        // one would cover it.
        filtered.sortBy { it.order }
        return filtered
    }

    /**
     * Subdivides the tile into an NxN mesh and returns one cell per subquadrant,
     * each with its affine matrix (parallelogram) and the corresponding bitmap
     * slice.
     *
     * Bitmap orientation (HiPS convention, identical to Aladin desktop —see
     * CDSHealpix.corners() which returns [S,E,W,N] and HealpixKey.drawTriangle
     * which maps TL=S, TR=W, BL=E, BR=N): the bitmap X axis runs along +fy
     * (corner S->W) and the Y axis along +fx (corner S->E). Thus the grid is
     * indexed as grid[fy_i][fx_j] (NOT the other way around): if the axes are
     * swapped, each tile will be rotated 90° and misaligned with its neighbors.
     *
     * `n` is adaptive: 1 cell if the tile hardly deforms (near the center of
     * the FoV), 4x4=16 if distortion is noticeable (near the FoV edge).
     * With n=1 the only cell is the whole parallelogram (anchors S, W, E exact).
     */
    fun placementMesh(
        proj: Projection,
        tile: VisibleTile,
        bmpW: Int,
        bmpH: Int,
    ): List<MeshCell>? {
        val n = meshSubdivisions(proj, tile)
        val cw = bmpW / n
        val ch = bmpH / n
        // grid[i][j] = projected point at (fy=i/n, fx=j/n) of the tile. Uses
        // forwardClamped (never null) so tiles straddling the edge of the visible
        // hemisphere still get a full mesh instead of dropping cells into a gap
        // (see Projection.forwardClamped for why).
        val grid = Array(n + 1) { i ->
            Array(n + 1) { j ->
                val ra = Healpix.radecAt(tile.order, tile.npix, j.toDouble() / n, i.toDouble() / n)
                proj.forwardClamped(ra[0], ra[1])
            }
        }
        val cells = ArrayList<MeshCell>(n * n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                val o = grid[i][j]       // (fy=i/n,   fx=j/n)
                val e1 = grid[i + 1][j]  // +fy  -> bitmap X axis
                val e3 = grid[i][j + 1]  // +fx  -> bitmap Y axis
                // Anti-limb safeguard: if the cell projects to a parallelogram
                // MUCH larger than the screen, it's an edge/extreme artifact
                // (tile near the gnomonic horizon that stretches brutally).
                // Drawing it would paint a "weird texture" across the whole view.
                // We skip it; its valid neighbor cells cover the actual area.
                val spanW = hypot(e1[0] - o[0], e1[1] - o[1])
                val spanH = hypot(e3[0] - o[0], e3[1] - o[1])
                val maxSpan = 1.5f * maxOf(proj.widthPx, proj.heightPx)
                if (spanW > maxSpan || spanH > maxSpan) continue
                cells.add(
                    MeshCell(
                        affineMatrix(o, e1, e3, cw.toFloat(), ch.toFloat()),
                        i * cw, j * ch, cw, ch,
                    )
                )
            }
        }
        return if (cells.isEmpty()) null else cells
    }

    /**
     * Decides how many subdivisions (per axis) the tile needs.
     *
     * Two independent reasons to subdivide more:
     * 1. Local flatness: does a single affine (parallelogram) already match the
     *    tile's own 4 true corners closely? (predicted 4th vertex vs actual).
     * 2. Screen size: ZEA is only LOCALLY close to affine. A coarse (low-order)
     *    tile at a wide FoV can pass check 1 -- its own 4 corners fit a flat
     *    parallelogram fine -- while still curving away from that parallelogram
     *    *between* the corners enough to visibly diverge from a neighboring
     *    tile's shared edge (which that neighbor computes independently along
     *    the same true curve). That showed up as tiles looking "not joined"
     *    specifically at wide FoV, where base HEALPix tiles span hundreds of
     *    screen px: neither reported error nor a black gap, just a seam where
     *    two flat approximations of the same curve don't quite meet. Forcing
     *    more subdivisions once a tile's screen footprint is large enough keeps
     *    both sides of a shared edge close enough to the true curve to visually
     *    coincide, regardless of how "flat" the tile looks on its own.
     */
    private fun meshSubdivisions(proj: Projection, tile: VisibleTile): Int {
        // forwardClamped: never null, so the size/flatness estimate below always
        // has all 4 corners even for a tile straddling the render cutoff.
        val p = Array(4) { i ->
            val c = tile.cornersRadec[i]
            proj.forward(c[0], c[1]) ?: proj.forwardClamped(c[0], c[1])
        }
        val a = p[0]; val b = p[1]; val c2 = p[2]; val d = p[3]
        val predX = b[0] + d[0] - a[0]
        val predY = b[1] + d[1] - a[1]
        val flatnessErr = hypot(predX - c2[0], predY - c2[1])
        val nFromFlatness = if (flatnessErr < 1.5f) 1 else 4

        val maxSpan = maxOf(
            hypot(b[0] - a[0], b[1] - a[1]),
            hypot(d[0] - a[0], d[1] - a[1]),
        )
        val nFromSize = when {
            maxSpan > 600f -> 8
            maxSpan > 300f -> 6
            maxSpan > 150f -> 4
            else -> 1
        }
        return maxOf(nFromFlatness, nFromSize)
    }

    private fun bbox(pts: List<FloatArray>): FloatArray? {
        if (pts.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in pts) {
            if (p[0] < minX) minX = p[0]
            if (p[1] < minY) minY = p[1]
            if (p[0] > maxX) maxX = p[0]
            if (p[1] > maxY) maxY = p[1]
        }
        return floatArrayOf(minX, minY, maxX, maxY)
    }
}
