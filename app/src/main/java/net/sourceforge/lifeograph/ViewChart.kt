/* *********************************************************************************

    Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)

    This file is part of Lifeograph.

    Lifeograph is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lifeograph is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.

 ***********************************************************************************/

package net.sourceforge.lifeograph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.toColorInt
import net.sourceforge.lifeograph.helpers.Date
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.*
import androidx.core.graphics.withSave

@Suppress("PropertyName", "FunctionName")
class ViewChart(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var mNativePtr: Long

    @Throws(Throwable::class)
    protected fun finalize() {
        if(mNativePtr != 0L) {
            nativeDestroy(mNativePtr)
            mNativePtr = 0
        }
        //super.finalize()
    }

    fun calculateAndPlot() {
        val data = mData ?: return

        data.calculate_points()
        update_h_x_values()
        set_zoom(if(data.type == ChartData.TYPE_DATE) 1f else 0f)
        invalidate()
    }

    // GEOMETRICAL VARIABLES =======================================================================
    val m_x_offset: Float get() = nativeGetXOffset(mNativePtr)
    val m_y_offset: Float get() = nativeGetYOffset(mNativePtr)
    val m_width: Int get() = nativeGetWidth(mNativePtr)
    val m_height: Int get() = nativeGetHeight(mNativePtr)
    val m_step_count: Int get() = nativeGetStepCount(mNativePtr)
    val m_step_start: Int get() = nativeGetStepStart(mNativePtr)
    val m_pre_steps: Int get() = nativeGetPreSteps(mNativePtr)
    val m_post_steps: Int get() = nativeGetPostSteps(mNativePtr)
    val m_zoom_level: Float get() = nativeGetZoomLevel(mNativePtr)

    //val m_border_curve: Float get() = nativeGetBorderCurve(mNativePtr)
    val m_border_label: Float get() = nativeGetBorderLabel(mNativePtr)
    val m_label_size: Float get() = nativeGetLabelSize(mNativePtr)
    val m_label_height: Float get() = nativeGetLabelHeight(mNativePtr)

    val m_h_x_values: Float get() = nativeGetHXValues(mNativePtr)
    val m_width_col_min: Float get() = nativeGetWidthColMin(mNativePtr)

    val m_x_min: Float get() = nativeGetXMin(mNativePtr)

    val m_y_max: Float get() = nativeGetYMax(mNativePtr)
    val m_y_0: Float get() = nativeGetY0(mNativePtr)
    val m_step_x: Float get() = nativeGetStepX(mNativePtr)
    val m_coefficient: Float get() = nativeGetCoefficient(mNativePtr)
    val m_ov_height: Float get() = nativeGetOVHeight(mNativePtr)
    val m_step_x_ov: Float get() = nativeGetStepXOV(mNativePtr)
    val m_coeff_ov: Float get() = nativeGetCoeffOV(mNativePtr)

    private fun update_h_x_values() { nativeUpdateHXValues(mNativePtr) }
    private fun update_pre_and_post_steps() { nativeUpdatePreAndPostSteps(mNativePtr) }
    private fun get_value_at(i: Int): Double { return nativeGetValueAt(mNativePtr, i) }

    private var mUnitLineThk = 1f

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPath = Path()
    var mData: ChartData? = null

    private val mSwipeGestureDetector: GestureDetector
    private val mScaleGestureDetector: ScaleGestureDetector

    init {
        mPaint.strokeJoin = Paint.Join.ROUND
        mSwipeGestureDetector = GestureDetector(context, SwipeGestureListener())
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
        setOnTouchListener { v, event ->
            v?.performClick()
            mSwipeGestureDetector.onTouchEvent(event!!) || mScaleGestureDetector.onTouchEvent(event)
        }
    }

//    private fun updateLabelSize() {
//        // TODO
//    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resize(w, h)
    }

    private fun resize(w: Int, h: Int) { nativeResize(mNativePtr, w, h) }

    private fun drawXValuesStrPrepare(canvas: Canvas): Float {
        val data = mData ?: return 0f

        if( data.style == ChartData.STYLE_LINE ) {
            mPaint.style = Paint.Style.FILL
            mPaint.color = "#E6E6E6".toColorInt() // 0.9, 0.9, 0.9
            canvas.drawRect( m_x_offset, m_y_offset + m_y_max,
                             m_width.toFloat(), m_h_x_values, mPaint )
        }

        //m_layout->set_font_description( m_font_main );
        //m_layout->set_width( 1.3 * m_h_x_values * Pango::SCALE );
        //m_layout->set_ellipsize( Pango::EllipsizeMode::END );
        mPaint.textAlign = Paint.Align.LEFT

        mPaint.color = Color.BLACK
        return( m_y_offset + m_height - m_ov_height - m_h_x_values )
    }

    private fun drawXValueStrColumn(canvas: Canvas, txt: String, i: Int, barTop: Float) {
        val data = mData ?: return

        mPath.reset()
        if( data.style == ChartData.STYLE_BARS )
            mPath.moveTo(m_x_offset + m_x_min + m_step_x * ( i + 0.5f ), barTop)
        else
            mPath.moveTo(m_x_offset + m_x_min + m_step_x * i, barTop)

        if( data.style == ChartData.STYLE_LINE ) {
            mPath.rLineTo( 0f, m_label_height )
            //mPath.rMoveTo( m_border_label, -m_label_height )
        }

        canvas.withSave {
            // Move the canvas origin to the text starting point
            // We add m_border_label to keep it away from the tick line
            val textX = x + m_border_label
            val textY = barTop + (if(data.style == ChartData.STYLE_LINE) m_label_height else 0f)

            translate(textX, textY)

            rotate(45f)

            // Draw the text at the new (0,0)
            // Adjust y to center the text relative to the line if necessary
            drawText(txt, 0f, 0f, mPaint)
        }
    }

    private fun drawXValuesNum(canvas: Canvas) {
        val data = mData ?: return

        val itV = data.values_num.iterator()
        val barTop = drawXValuesStrPrepare(canvas)

        for( i in 0 until m_step_count ) {
            val (k, _) = itV.next()
            drawXValueStrColumn(canvas, Lifeograph.formatNumber(k), i, barTop)
        }
    }

    private fun drawXValuesStr(canvas: Canvas) {
        val data = mData ?: return

        val itV = data.values_index2str.iterator()
        val barTop = drawXValuesStrPrepare(canvas)

        for( i in 0 until m_step_count ) {
            val (_, v) = itV.next()
            drawXValueStrColumn(canvas, v, i, barTop)
        }
    }

    private fun drawXValuesDate(canvas: Canvas) {
        val data = mData ?: return

        // YEAR & MONTH BAR
        val period = data.period
        var stepGrid = ceil(m_width_col_min / m_step_x).toInt()
        var stepGridFirst = 0
        val barTop = m_y_offset + m_height - m_ov_height - m_h_x_values
        val itDate = data.values_date.iterator()
        var yearLast = 0

        repeat(m_step_start) { itDate.next() } // advance to start point
        var kv = itDate.next()

        if( period != ChartData.PERIOD_YEARLY )
        {
            stepGrid = when {
                stepGrid > 12 -> stepGrid + 12 - (stepGrid % 12)
                stepGrid > 6 -> 12
                stepGrid > 4 -> 6
                stepGrid > 3 -> 4
                stepGrid > 2 -> 3
                else -> stepGrid
            }

            val sgReduced = if(stepGrid > 12) 12 else stepGrid
            stepGridFirst = ( sgReduced - ( Date.get_month(kv.key) % sgReduced ) + 1 )%sgReduced
        }

        mPath.reset()

        mPaint.color = "#E6E6E6".toColorInt() // 0.9, 0.9, 0.9
        canvas.drawRect(m_x_offset, barTop, m_width.toFloat(),
                        m_h_x_values, mPaint)

        //m_layout->set_font_description( m_font_main );
        mPaint.textAlign = Paint.Align.LEFT
        mPaint.color = Color.BLACK

        repeat(stepGridFirst) { itDate.next() }
        kv = itDate.next()

        var i = stepGridFirst
        while(true) {
            val date = kv.key

            if( data.style == ChartData.STYLE_BARS )
                mPath.moveTo(m_x_offset + m_x_min + m_step_x * ( i + 0.5f ), barTop)
            else
                mPath.moveTo(m_x_offset + m_x_min + m_step_x * i, barTop)
            mPath.rLineTo( 0f, m_label_height )
            mPath.rLineTo( m_border_label, -m_label_height )

            // this gets the coordinates at the very end of the current path:
            val measure = PathMeasure(mPath, false)
            val lastPos = FloatArray(2)
            measure.getPosTan(measure.length, lastPos, null)

            if( period == ChartData.PERIOD_YEARLY ) {
                canvas.drawText(Date.get_year_str( date ), lastPos[0], lastPos[1], mPaint)
            }
            else
            {
                if( stepGrid < 12 )
                {
                    val dateStr = if( period == ChartData.PERIOD_MONTHLY )
                        Date.get_month_str( date )
                    else // weekly
                        Date.format_string( date, "MD")
                    canvas.drawText(dateStr, lastPos[0], lastPos[1], mPaint)
                }

                if( yearLast != Date.get_year(date) ) {
                    canvas.drawText( Date.get_year_str( date ), lastPos[0], lastPos[1] + m_label_height, mPaint)
                    yearLast = Date.get_year(date)
                }
            }

            i += stepGrid
            if( i < m_step_count )
                repeat(stepGrid) { itDate.next() }
            else
                break
        }
    }

    private fun drawXValues(canvas: Canvas) {
        val data = mData ?: return
        when(data.type) {
            ChartData.TYPE_STRING -> drawXValuesStr(canvas)
            ChartData.TYPE_NUMBER -> drawXValuesNum(canvas)
            ChartData.TYPE_DATE -> drawXValuesDate(canvas)
        }

        mPaint.color = Color.valueOf(0.7f, 0.7f, 0.7f).toArgb()
        canvas.drawPath(mPath, mPaint)
    }

    private fun drawYLevels(canvas: Canvas) {
        val data = mData ?: return

        val stepY = data.v_grid_step * m_coefficient

        mPaint.color = Color.valueOf( 0.6f, 0.6f, 0.6f ).toArgb()
        mPaint.strokeWidth = mUnitLineThk
        mPaint.textAlign = Paint.Align.LEFT
        //m_layout->set_width( 150 * Pango::SCALE );

        mPath.reset()
        for (i in 0..4) {
            // horizontal lines:
            mPath.moveTo( m_x_offset + m_width, (m_y_offset + m_y_max - i * stepY).toFloat())
            mPath.rLineTo( -m_width.toFloat(), 0f )

            // labels:
            val valStr = Lifeograph.formatNumber( data.v_grid_min + data.v_grid_step * i ) +
                                                  " " + data.unit
            canvas.drawText(valStr, 4 * m_border_label, -m_label_size, mPaint)

            canvas.drawPath( mPath, mPaint )
        }
    }

    private fun drawLine( canvas: Canvas ) {
        val data = mData ?: return

        mPaint.color = Color.valueOf( 0.7f, 0.4f, 0.4f ).toArgb()
        //mPaint.lineJoin = Paint.Join.BEVEL
        mPaint.strokeWidth = mUnitLineThk * 3

        drawLine2( canvas, false )

        // UNDERLAY
        if( data.has_underlay() ) {
            mPaint.color = Color.valueOf( 0.1f, 0.7f, 0.7f ).toArgb()
            mPaint.strokeWidth = mUnitLineThk * 2

            //cr->set_dash( s_dash_pattern, 0 )
            drawLine2( canvas, true )
            //cr->unset_dash()
        }
    }
    private fun drawLine2( canvas: Canvas, fUnderlay: Boolean ) {
        val data = mData ?: return
        when( data.type ) {
            ChartData.TYPE_STRING -> drawLine3( canvas, data.values_str, fUnderlay )
            ChartData.TYPE_NUMBER -> drawLine3( canvas, data.values_num, fUnderlay )
            ChartData.TYPE_DATE -> drawLine3( canvas, data.values_date, fUnderlay )
        }
    }
    private fun drawLine3(canvas: Canvas, values: Map<*, YValues>, fUnderlay: Boolean) {
        val data = mData ?: return

        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mUnitLineThk * 3
        mPaint.color = "#B26666".toColorInt() // 0.7, 0.4, 0.4

        mPath.reset()

        val itV = values.entries.iterator()
        repeat(m_step_start - m_pre_steps) { itV.next() } // advance to start
        val (_, v) = itV.next()

        mPath.moveTo( m_x_offset + m_x_min - m_step_x * m_pre_steps,
                      m_y_offset + m_y_max
                              - m_coefficient * ( if(fUnderlay) v.u else v.v ).toFloat()
                              - data.v_grid_min.toFloat() )

        for( i in 1 until (m_step_count + m_pre_steps + m_post_steps)) {
            val (_, v) = itV.next()

            // may not be needed if( vy != std::numeric_limits< double >::max() )
            mPath.lineTo( m_x_offset + m_x_min + m_step_x * ( i - m_pre_steps ),
                          m_y_offset + m_y_max
                                  - m_coefficient * ( if(fUnderlay) v.u else v.v ).toFloat()
                                  - data.v_grid_min.toFloat() )
        }
        canvas.drawPath(mPath, mPaint)
    }

    private fun drawBars( canvas: Canvas ) {
        val data = mData ?: return

        mPaint.color = Color.valueOf( 0.7f, 0.4f, 0.4f ).toArgb()
        mPaint.style = Paint.Style.FILL
        //mPaint.lineJoin = Paint.Join.BEVEL

        drawBars2( canvas, false )

        // UNDERLAY
        if( data.has_underlay() ) {
            mPaint.color = Color.valueOf( 0.1f, 0.7f, 0.7f ).toArgb()
            mPaint.strokeWidth = mUnitLineThk * 2

            //cr->set_dash( s_dash_pattern, 0 )
            drawBars2( canvas, true )
            //cr->unset_dash()
        }
    }
    private fun drawBars2( canvas: Canvas, fUnderlay: Boolean ) {
        val data = mData ?: return
        when(data.type) {
            ChartData.TYPE_STRING -> drawBars3( canvas, data.values_str, fUnderlay )
            ChartData.TYPE_NUMBER -> drawBars3( canvas, data.values_num, fUnderlay )
            ChartData.TYPE_DATE -> drawBars3( canvas, data.values_date, fUnderlay )
        }
    }
    private fun drawBars3(canvas: Canvas, values: Map<*, YValues>, fUnderlay: Boolean) {
        val itV = values.entries.iterator()
        val fShowV = !fUnderlay //and ( m_step_x * Pango::SCALE ) > ( m_font_bold.get_size() * 4 ) };
        repeat(m_step_start - m_pre_steps) { itV.next() } // advance to start

        if( fShowV ) {
            //m_layout->set_font_description( m_font_bold )
            //m_layout->set_width( m_step_x * 0.8 * Pango::SCALE )
            mPaint.textAlign = Paint.Align.CENTER
        }

        mPaint.style = if(fUnderlay) Paint.Style.STROKE else Paint.Style.FILL

        for( i in 0 until m_step_count + m_pre_steps + m_post_steps ) {
            val (_, v) = itV.next()
            val barH = m_coefficient * ( if(fUnderlay) v.u else v.v).toFloat()

            if( barH != 0f )
                canvas.drawRect(m_x_offset + m_x_min + m_step_x * ( i - m_pre_steps + 0.1f ),
                                m_y_offset + m_y_0,
                                m_step_x * 0.8f,
                                -barH,
                                mPaint )

            if( fShowV && v.v != 0.0 ) {
                canvas.drawText( Lifeograph.formatNumber( v.v),
                                 m_x_offset + m_x_min + m_step_x * ( i - m_pre_steps + 0.1f ),
                                 m_y_offset + m_y_0 - barH - ( if(barH < 0f) 0f
                                                                    else m_label_height ),
                                 mPaint )
            }
        }
    }

    private fun drawOverview(canvas: Canvas) {
        val data = mData ?: return

        // OVERVIEW REGION
        mPaint.color = Color.LTGRAY
        mPaint.style = Paint.Style.FILL
        canvas.drawRect( m_x_offset, m_y_offset + m_height - m_ov_height,
                         m_width.toFloat(), m_ov_height, mPaint)

//        if( m_F_overview_hovered )
//            mPaint.color = Color.WHITE
//        else
            mPaint.color = Color.valueOf( 0.95f, 0.95f, 0.95f ).toArgb()
        canvas.drawRect( m_x_offset + m_border_label + m_step_start * m_step_x_ov,
                         m_y_offset + m_height - m_ov_height,
                         ( m_step_count - 1 ) * m_step_x_ov,
                         m_ov_height,
                         mPaint)

        // OVERVIEW LINE
        mPaint.color = Color.valueOf( 0.9f, 0.3f, 0.3f ).toArgb()
        // TODO: mPaint.lineJoin = Paint.Join.BEVEL
        mPaint.strokeWidth = mUnitLineThk * 2

        mPath.reset()
        mPath.moveTo( m_x_offset + m_border_label,
                      m_y_offset + m_height - m_border_label
                              - m_coeff_ov * ( get_value_at( 0 ) - data.v_min ).toFloat() )

        for(i in 1 until data.span) {
            mPath.rLineTo( m_step_x_ov,
                           - m_coeff_ov * ( get_value_at( i ) - get_value_at( i - 1 ) ).toFloat() )
        }
        canvas.drawPath(mPath, mPaint)

        // DIVIDER
//        if( m_F_overview_hovered )
//            cr->set_source_rgb( 0.2, 0.2, 0.2 )
//        else
            mPaint.color = Color.valueOf( 0.45f, 0.45f, 0.45f ).toArgb()
        canvas.drawRect( m_x_offset + 1f, m_y_offset + m_height - m_ov_height,
                         m_width - 2f, m_ov_height - 1f, mPaint )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = mData ?: return

        // BACKGROUND
        //if( !m_F_printing_mode ) {
            canvas.drawColor(Color.WHITE)
        //}

        // HANDLE THERE-IS-TOO-FEW-ENTRIES-CASE SPECIALLY
        if( data.span < 2 ) {
            mPaint.color = Color.BLACK
            mPaint.textAlign = Paint.Align.CENTER
            mPaint.style = Paint.Style.FILL
            canvas.drawText("INSUFFICIENT DATA", m_width / 2f, m_height / 2f, mPaint)
            return
        }

        // NUMBER OF STEPS IN THE PRE- AND POST-BORDERS
        update_pre_and_post_steps()

        // ENTRY BACKGROUNDS
//        if( data.type == ChartData.TYPE_DATE )
//            TODO: drawMilestones(canvas)

        // YEAR & MONTH BAR
        drawXValues(canvas)

        // Y LEVELS
        drawYLevels(canvas)

        // GRAPH LINE OR BARS
        if( data.style == ChartData.STYLE_LINE )
            drawLine(canvas)
        else
            drawBars(canvas)

        // TOOLTIP
//        if( m_F_widget_hovered and m_hovered_step >= 0 )
//            drawTooltip(canvas)

        // OVERVIEW
        if( m_step_count < data.span )
            drawOverview(canvas)
    }



    fun scroll(offset: Int) {
        nativeScroll(mNativePtr, offset)
        invalidate()
    }

    fun set_zoom(level: Float) {
        nativeSetZoom(mNativePtr, level)
        invalidate()
    }

    init {
        mNativePtr = nativeCreate()
    }

    // NATIVE FUNCTIONS ============================================================================
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)

    private external fun nativeGetXOffset(ptr: Long): Float
    private external fun nativeGetYOffset(ptr: Long): Float
    private external fun nativeGetWidth(ptr: Long): Int
    private external fun nativeGetHeight(ptr: Long): Int
    private external fun nativeGetStepCount(ptr: Long): Int
    private external fun nativeGetStepStart(ptr: Long): Int
    private external fun nativeGetPreSteps(ptr: Long): Int
    private external fun nativeGetPostSteps(ptr: Long): Int
    private external fun nativeGetZoomLevel(ptr: Long): Float
    private external fun nativeSetZoom(ptr: Long, level: Float)
    private external fun nativeGetBorderLabel(ptr: Long): Float
    private external fun nativeGetLabelSize(ptr: Long): Float
    private external fun nativeGetLabelHeight(ptr: Long): Float
    private external fun nativeGetHXValues(ptr: Long): Float
    private external fun nativeGetWidthColMin(ptr: Long): Float
    private external fun nativeGetXMin(ptr: Long): Float
    private external fun nativeGetYMax(ptr: Long): Float
    private external fun nativeGetY0(ptr: Long): Float
    private external fun nativeGetStepX(ptr: Long): Float
    private external fun nativeGetCoefficient(ptr: Long): Float
    private external fun nativeGetOVHeight(ptr: Long): Float
    private external fun nativeGetStepXOV(ptr: Long): Float
    private external fun nativeGetCoeffOV(ptr: Long): Float
    private external fun nativeUpdateHXValues(ptr: Long)
    private external fun nativeUpdatePreAndPostSteps(ptr: Long)
    private external fun nativeGetValueAt(ptr: Long, i: Int): Double
    private external fun nativeResize(ptr: Long, w: Int, h: Int)
    private external fun nativeScroll(ptr: Long, offset: Int): Double

    // INNER CLASSES ===============================================================================
    inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if(m_zoom_level < 0.05)
                return false

            if(detector.scaleFactor > 1) { // zoom out
                set_zoom(m_zoom_level * 0.95f)
            }
            else { // zoom in
                set_zoom(m_zoom_level * 1.1f)
            }
            return true
        }
    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        var mCumulativeScroll = 0f

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distX: Float, distY: Float):
                Boolean {

            mCumulativeScroll += distX

            Log.d(Lifeograph.TAG, "CumuScroll: $mCumulativeScroll | StepX: $m_step_x")

            if(abs(mCumulativeScroll) > m_step_x) {
                scroll((mCumulativeScroll / m_step_x).toInt())
                mCumulativeScroll = 0f
            }

            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }


}
