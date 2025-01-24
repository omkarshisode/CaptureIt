package com.example.captureit.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.captureit.R

/**
 * A custom view that creates an overlay with a clear viewport area and a semi-transparent dark background.
 * It is used to highlight a portion of the screen while obscuring the rest of the screen.
 *
 * This view is commonly used for capturing a specific area of the screen, such as in a photo capture
 * scenario where only a portion of the view should be visible.
 */
class CustomOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Paint used to draw the semi-transparent black overlay */
    private val dimPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.semi_transparent_black)
        style = Paint.Style.FILL
    }

    /** Paint used to create the "clear" effect inside the viewport area */
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    /** Paint used to draw the border around the viewport */
    private val borderPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = 4f.dpToPx()
        isAntiAlias = true
    }

    /** The rectangular area that remains clear (viewable) in the center */
    private var viewportRect: RectF? = null

    /** Corner radius of the viewport for rounded corners */
    private val cornerRadius = 16f.dpToPx()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the semi-transparent overlay on the entire canvas
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // Initialize the viewportRect if it hasn't been set
        if (viewportRect == null) {
            val left = (width - 240f.dpToPx()) / 2
            val top = (height - 240f.dpToPx()) / 2
            val right = left + 240f.dpToPx()
            val bottom = top + 240f.dpToPx()
            viewportRect = RectF(left, top, right, bottom)
        }

        // Draw the clear area (viewport) and the border around it
        viewportRect?.let {
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, clearPaint)
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, borderPaint)
        }
    }

    /**
     * Convert a float value in dp to px based on the device's screen density.
     */
    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    /**
     * Get the coordinates of the viewport rectangle.
     *
     * @return A map with the viewport's x, y, width, and height.
     */
    fun getViewportCoordinates(): Map<String, Float>? {
        return viewportRect?.let {
            mapOf(
                "x" to it.left,
                "y" to it.top,
                "width" to it.width(),
                "height" to it.height()
            )
        }
    }

    /**
     * Get the rectangle representing the viewport area.
     *
     * @return The viewport rectangle.
     */
    fun getViewportRect(): RectF? {
        return viewportRect
    }
}
