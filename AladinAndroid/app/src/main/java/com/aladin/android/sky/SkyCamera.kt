package com.aladin.android.sky

import com.aladin.android.model.SkyPosition

/**
 * Sky camera state: center (ra,dec), vertical FoV and rotation.
 * Immutable; operations return a new state.
 */
data class SkyCamera(
    val center: SkyPosition = SkyPosition(0.0, 0.0),
    val fovDeg: Double = 60.0,
    val rotationDeg: Double = 0.0,
) {
    init {
        require(fovDeg > 0.0)
    }

    fun projection(widthPx: Float, heightPx: Float): Projection = Projection(
        ra0 = center.raDeg,
        dec0 = center.decDeg,
        rotationDeg = rotationDeg,
        radPerPixel = Projection.radPerPixelFromFov(fovDeg, heightPx),
        widthPx = widthPx,
        heightPx = heightPx,
    )

    fun panBy(deltaPxX: Float, deltaPxY: Float, widthPx: Float, heightPx: Float): SkyCamera {
        val proj = projection(widthPx, heightPx)
        // Move the center in the opposite direction of the drag.
        val newCenter = proj.inverse(widthPx / 2f - deltaPxX, heightPx / 2f - deltaPxY)
        return copy(center = SkyPosition(newCenter[0], newCenter[1]))
    }

    fun zoom(factor: Float, widthPx: Float, heightPx: Float, focusX: Float, focusY: Float): SkyCamera {
        val newFov = (fovDeg / factor).coerceIn(MIN_FOV, MAX_FOV)
        if (newFov == fovDeg) return this
        // Standard zoom (photo-like): the sky point under the pinch focus
        // must remain under the focus after zooming.
        val oldProj = projection(widthPx, heightPx)
        val anchor = oldProj.inverse(focusX, focusY) // sky point under focus
        // Projection with the new FoV but still centered on the old center: it
        // tells us where the anchor falls on screen now.
        val newProj = Projection(
            ra0 = center.raDeg, dec0 = center.decDeg, rotationDeg = rotationDeg,
            radPerPixel = Projection.radPerPixelFromFov(newFov, heightPx),
            widthPx = widthPx, heightPx = heightPx,
        )
        val anchorScreen = newProj.forward(anchor[0], anchor[1])
            ?: return copy(fovDeg = newFov)
        // Shift the screen center so the anchor returns to focus.
        // Moving the screen center by D shifts the content by -D, so
        // to move the anchor from anchorScreen to focus: newCenterScreen = center + (anchorScreen - focus).
        val newCenterScreenX = widthPx / 2f + (anchorScreen[0] - focusX)
        val newCenterScreenY = heightPx / 2f + (anchorScreen[1] - focusY)
        val newCenter = newProj.inverse(newCenterScreenX, newCenterScreenY)
        return copy(center = SkyPosition(newCenter[0], newCenter[1]), fovDeg = newFov)
    }

    fun recenterTo(pos: SkyPosition): SkyCamera = copy(center = pos)

    companion object {
        const val MIN_FOV = 0.005 // ~18 arcsec
        // With ZEA projection (see Projection.kt) the projection radius is
        // bounded (w = sqrt((1+z)/2) never reaches 0 except exactly at the antipode),
        // so unlike gnomonic we can zoom out much more without giant parallelograms
        // crossing the screen. Aladin desktop allows up to ~180° with ZEA (see
        // Projection.getDefaultType); we leave a small margin under 180° so the
        // antipode (where projection is undefined) stays off-screen.
        const val MAX_FOV = 175.0
    }
}