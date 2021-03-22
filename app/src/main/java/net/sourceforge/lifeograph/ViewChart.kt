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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.core.content.ContextCompat
import net.sourceforge.lifeograph.Lifeograph.Companion.screenShortEdge
import java.util.*
import kotlin.math.*


class ViewChart(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // CONSTANTS ===================================================================================
    private val mBorderCurve = screenShortEdge * Lifeograph.sDPIX / 25f
    private val mBorderLabel = mBorderCurve / 3f
    private val cOffsetLabel = mBorderCurve / 6f
    private val cHeightLabel = mBorderCurve / 1.75f
    private val cHeightBar = cHeightLabel + cOffsetLabel
    private val cCoeffOverview = mBorderCurve / 2f
    private val cWidthColMin = mBorderCurve * 2f
    private val cStrokeWidth = mBorderCurve / 30f
    private val cLabelY = cHeightLabel
    private val cXMin = mBorderCurve + mBorderLabel
    private val cYMin = mBorderCurve

    // VARIABLES ===================================================================================
    private val mPaint: Paint = Paint()
    private val mPath: Path = Path()
    var         mData: ChartData? = null

    // GEOMETRICAL VARIABLES =======================================================================
    private var mWidth = -1
    private var mHeight = -1
    private var mSpan = 0
    private var mStepCount = 0
    private var mStepStart = 0
    private var mZoomLevel = 1.0
    private var mVMin = 0.0
    private var mVMax = 0.0
    private var mXMax = 0f
    private var mYMax = 0f
    private var mYMid = 0f
    private var mAmplitude = 0f
    private var mLength = 0f
    private var mStepX = 0f
    private var mCoefficient = 0f
    private var mOvHeight = 0f
    private var mStepXOv = 0f
    private var mAmpliOv = 0f
    private var mCoeffOv = 0f
    private val mSwipeGestureDetector: GestureDetector
    private val mScaleGestureDetector: ScaleGestureDetector

    // METHODS =====================================================================================
    init {
        mPaint.isAntiAlias = true
        mPaint.color = Color.BLACK
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeWidth = cStrokeWidth * 4
        mSwipeGestureDetector = GestureDetector(context, SwipeGestureListener())
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
        setOnTouchListener { v: View?, event: MotionEvent? ->
            v!!.performClick()
            mSwipeGestureDetector.onTouchEvent(event) || mScaleGestureDetector.onTouchEvent(event)
        }
    }

    //    public void set_points( ChartData points, float zoom_level ) {
    //        m_data = points;
    //        m_zoom_level = zoom_level;
    //        m_span = points != null ? points.get_span() : 0;
    //
    //        if( m_width > 0 ) { // if on_size_allocate is executed before
    //            update_col_geom( true );
    //            invalidate();
    //        }
    //    }
    private fun updateColGeom(flag_new: Boolean) {
        // 100% zoom:
        val stepCountNominal: Int = (mLength / cWidthColMin).toInt() + 1
        val stepCountMin: Int = if(mSpan > stepCountNominal) stepCountNominal else mSpan

        if(mData!!.is_underlay_planned) {
            mVMin = min(mData!!.v_min, mData!!.v_plan_min)
            mVMax = max(mData!!.v_max, mData!!.v_plan_max)
        }
        else {
            mVMin = mData!!.v_min
            mVMax = mData!!.v_max
        }

        mStepCount = ( mZoomLevel * ( mSpan - stepCountMin ) ).toInt() + stepCountMin
        mStepX = if(mStepCount < 3) mLength else mLength / (mStepCount - 1)

        mOvHeight = if(mStepCount < mSpan) log10(mHeight.toDouble()).toFloat() * cCoeffOverview
                    else 0f

        val mltp: Int = if((mData!!.type and ChartData.PERIOD_MASK) == ChartData.YEARLY) 1 else 2
        mYMax = mHeight - mltp * cHeightBar - mOvHeight
        mYMid = ( mYMax + cYMin ) / 2
        mAmplitude = mYMax - cYMin
        mCoefficient = if( mVMax == mVMin ) 0f else mAmplitude / (mVMax - mVMin).toFloat()

        val colStartMax = mSpan - mStepCount
        if( flag_new || mStepStart > colStartMax )
            mStepStart = colStartMax

        // OVERVIEW PARAMETERS
        mAmpliOv = mOvHeight - 2 * cOffsetLabel
        mCoeffOv = if( mVMax == mVMin ) 0.5f else mAmpliOv / ( mVMax - mVMin ).toFloat()
        mStepXOv = mWidth - 2f * cOffsetLabel
        if(mSpan > 1)
            mStepXOv /= mSpan - 1
    }

    private fun getFiltererStack(): FiltererContainer? {
        return if(mData!!.filter != null) mData!!.filter._filterer_stack else null
    }

    private fun getPeriodDate(date: Long): Long {
        when(mData!!.type and ChartData.PERIOD_MASK) {
            ChartData.WEEKLY -> {
                return Date.get_pure(Date.backward_to_week_start(date))
            }
            ChartData.MONTHLY -> return Date.get_yearmonth(date) + Date.make_day(1)
            ChartData.YEARLY -> return (date and Date.FILTER_YEAR) + Date.make_month(
                    1) + Date.make_day(1)
        }
        return 0
    }

    fun calculatePoints(zoom_level: Double) {
        mData!!.clear_points()

        class Values(val v: Double, val p: Double)

        val fc = getFiltererStack()
        val entries: Collection<Entry> = Diary.d.m_entries.descendingMap().values
        var v = 1.0
        var vPlan = 0.0
        val yAxis = mData!!._y_axis
        val mapValues: MutableMap<Long, MutableList<Values>> = TreeMap() // multimap

        for(entry in entries) {
            if(yAxis == ChartData.TAG_VALUE_PARA) {
                if(mData!!.tag != null && !entry.has_tag(mData!!.tag)) continue
            }
            else {
                if(entry.is_ordinal) continue
                if(mData!!.tag != null && mData!!.is_tagged_only && !entry.has_tag(
                            mData!!.tag)) continue
            }
            if(fc != null && !fc.filter(entry)) continue
            when(yAxis) {
                ChartData.COUNT -> {
                }
                ChartData.TEXT_LENGTH -> v = entry._size.toDouble()
                ChartData.MAP_PATH_LENGTH -> v = entry._map_path_length
                ChartData.TAG_VALUE_ENTRY -> if(mData!!.tag != null) {
                    v = entry.get_value_for_tag(mData)
                    vPlan = entry.get_value_planned_for_tag(mData)
                }
                ChartData.TAG_VALUE_PARA -> if(mData!!.tag != null) {
                    // first sort the values by date:
                    for(para in entry.m_paragraphs) {
                        val date = para._date_broad
                        if(date == Date.NOT_SET || Date.is_ordinal(date)) continue
                        if(!para.has_tag(mData!!.tag)) continue
                        val c = Lifeograph.MutableInt() // dummy
                        v = para.get_value_for_tag(mData!!, c)
                        vPlan = para.get_value_planned_for_tag(mData!!, c)
                        val periodDate = getPeriodDate(date)
                        var list = mapValues[periodDate]
                        if(list != null) list.add(Values(v, vPlan))
                        else {
                            list = ArrayList()
                            list.add(Values(v, vPlan))
                            mapValues[periodDate] = list
                        }
                    }
                }
            }
            if(yAxis != ChartData.TAG_VALUE_PARA) // para values are added in their case
                mData!!.add_value(getPeriodDate(entry._date_t), v, vPlan)
        }
        if(yAxis == ChartData.TAG_VALUE_PARA) {
            // feed the values in order:
            for((key, list) in mapValues) {
                for(values in list) mData!!.add_value(key, values.v, values.p)
            }
        }
        mData!!.update_min_max()
        Diary.d.fill_up_chart_data(mData)
        if(zoom_level >= 0.0) mZoomLevel = zoom_level
        mSpan = mData!!._span
        if(mWidth > 0) { // if on_size_allocate is executed before
            updateColGeom(zoom_level >= 0.0)
            invalidate()
        }
    }

    fun setZoom(level: Double) {
        if(level == mZoomLevel) return

        mZoomLevel = if(level > 1.0) 1.0 else level.coerceAtLeast(0.0)
        if(mWidth > 0) { // if on_size_allocate is executed before
            Log.d(Lifeograph.TAG, "zoom event: $mZoomLevel")
            updateColGeom(false)
            invalidate()
        }
    }

    fun scroll(offset: Int) {
        if((mStepStart + offset >= 0) && (mStepStart + offset <= mSpan - mStepCount))
            mStepStart += offset
        else
            return

        invalidate()
    }

    // override onSizeChanged
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val flagFirst = mWidth < 0
        mWidth = w
        mHeight = h
        mXMax = mWidth - mBorderCurve
        mLength = mXMax - cXMin
        updateColGeom(flagFirst)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // reset path
        mPath.reset()

        // BACKGROUND COLOR (contrary to Linux version, background is not white in Android)
        //canvas.drawColor(ContextCompat.getColor(context, R.color.t_lightest))
        canvas.drawColor(Color.WHITE)

        // HANDLE THERE-IS-TOO-FEW-ENTRIES-CASE SPECIALLY
        if(mData == null || mSpan < 2) {
            mPaint.color = Color.BLACK
            mPaint.textSize = cHeightLabel * 3
            mPaint.textAlign = Paint.Align.CENTER
            mPaint.style = Paint.Style.FILL

            canvas.drawText("INSUFFICIENT DATA", mWidth / 2f, mHeight / 2f, mPaint)

            return
        }

        // NUMBER OF STEPS IN THE PRE AND POST BORDERS
        var preSteps = ceil(cXMin / mStepX).toInt()
        if(preSteps > mStepStart)
            preSteps = mStepStart

        var postSteps = ceil(mBorderCurve / mStepX).toInt()
        if(postSteps > mSpan - mStepCount - mStepStart)
            postSteps = mSpan - mStepCount - mStepStart

        // YEAR & MONTH BAR
        mPaint.color = Color.parseColor("#DDDDDD")
        mPaint.style = Paint.Style.FILL
        val period = mData!!._period
        var stepGrid = ceil(cWidthColMin / mStepX).toInt()
        var stepGridFirst = 0

        if(period == ChartData.YEARLY)
            canvas.drawRect(0f, mYMax, mWidth.toFloat(), mYMax + cHeightBar, mPaint)
        else {
            stepGrid = when {
                stepGrid > 12 -> stepGrid + 12 -(stepGrid % 12)
                stepGrid > 6  -> 12
                stepGrid > 4  -> 6
                stepGrid > 3  -> 4
                //stepGrid > 2 ->  3
                else          -> 3
            }

            stepGridFirst = (( stepGrid - ( Date.get_month(mData!!.dates[mStepStart])
                    % stepGrid ) + 1 ) % stepGrid)

            canvas.drawRect(0f, mYMax, mWidth.toFloat(), mYMax + cHeightBar * 2, mPaint)
        }

        // HORIZONTAL LINES
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.GRAY
        mPaint.strokeWidth = cStrokeWidth / 2

        val gridStepY = ( mYMax - cYMin ) / 4
        for(i in 0..3) {
            mPath.moveTo(0f, cYMin + i * gridStepY)
            mPath.lineTo(mWidth.toFloat(), cYMin + i * gridStepY)
        }
        canvas.drawPath(mPath, mPaint) // draws both vertical and horizontal lines
        mPath.reset()

        // y LABELS
        mPaint.color = Color.BLACK
        mPaint.style = Paint.Style.FILL
        mPaint.textSize = cHeightLabel

        canvas.drawText(Lifeograph.formatNumber(mVMax) + " " + mData!!.unit,
                        mBorderLabel,
                        cYMin - cOffsetLabel,
                        mPaint)

        canvas.drawText(Lifeograph.formatNumber((mVMax + mVMin) / 2) + " " + mData!!.unit,
                        mBorderLabel,
                        mYMid - cOffsetLabel,
                        mPaint)

        canvas.drawText(Lifeograph.formatNumber(mVMin) + " " + mData!!.unit,
                        mBorderLabel,
                        mYMax - cOffsetLabel,
                        mPaint)

        // YEAR & MONTH LABELS + VERTICAL LINES
        var yearLast = 0
        mPaint.color = Color.BLACK
        mPaint.strokeWidth = cStrokeWidth / 2
        mPaint.style = Paint.Style.FILL

        for(i in stepGridFirst until mStepCount step stepGrid) {
            val date = mData!!.dates[mStepStart + i]

            mPath.moveTo(cXMin + mStepX * i, mYMax + cLabelY)
            mPath.lineTo(cXMin + mStepX * i, 0f)

            if(period == ChartData.YEARLY) {
                //mPath.moveTo( cXMin + mStepX * i + cOffsetLabel, mYMax )
                canvas.drawText(Date.get_year(date).toString(),
                                cXMin + mStepX * i + cOffsetLabel,
                                mYMax + cHeightLabel,
                                mPaint)
            }
            else {
                if(stepGrid < 12) {
                    if(period == ChartData.MONTHLY)
                        canvas.drawText(Date.get_month(date).toString(),
                                        cXMin + mStepX * i + cOffsetLabel,
                                        mYMax + cHeightLabel,
                                        mPaint)
                    else // weekly
                        canvas.drawText(Date.format_string(date, "MD"),
                                        cXMin + mStepX * i + cOffsetLabel,
                                        mYMax + cHeightLabel,
                                        mPaint)
                }

                if( yearLast != Date.get_year(date) ) {
                    canvas.drawText(Date.get_year(date).toString(),
                                    cXMin + mStepX * i + cOffsetLabel,
                                    mYMax + cLabelY * if(stepGrid < 12) 2 else 1,
                                    mPaint)
                    yearLast = Date.get_year(date)
                }
            }
        }
        mPaint.style = Paint.Style.STROKE
        canvas.drawPath(mPath, mPaint) // draws both vertical and horizontal lines
        mPath.reset()

        // GRAPH LINE
        mPaint.color = ContextCompat.getColor(context, R.color.t_darker)
        mPaint.strokeWidth = 4 * cStrokeWidth

        mPath.moveTo(cXMin - mStepX * preSteps,
                     mYMax - mCoefficient *
                             (mData!!.values[mStepStart - preSteps] - mVMin).toFloat())

        for(i in 1 until mStepCount + preSteps + postSteps) {
            mPath.lineTo(cXMin + mStepX * (i - preSteps),
                         mYMax - mCoefficient *
                                 (mData!!.values[i + mStepStart - preSteps] - mVMin).toFloat())
        }
        canvas.drawPath(mPath, mPaint)
        mPath.reset()

        // UNDERLAY PREV YEAR
        if(mData!!.is_underlay_prev_year) {
            mPaint.color = ContextCompat.getColor(context, R.color.t_dark)
            //cr->set_dash( s_dash_pattern, 0 );
            mPaint.strokeWidth = cStrokeWidth * 2

            var stepStartUnderlay = if(mStepStart > 12) mStepStart - 12 else 0
            val iStart = if(mStepStart < 12) 12 - mStepStart else 0

            mPath.moveTo(cXMin - mStepX * iStart,
                         mYMax - mCoefficient *
                                 (mData!!.values[stepStartUnderlay] - mVMin).toFloat())

            for( i in iStart+1..mStepCount ) {
                mPath.lineTo(cXMin + mStepX * i,
                             mYMax - mCoefficient *
                                     (mData!!.values[++stepStartUnderlay] - mVMin).toFloat())
            }
            canvas.drawPath(mPath, mPaint)
            mPath.reset()
            //cr->unset_dash();
        }

        // UNDERLAY PLANNED VALUES
        else if(mData!!.is_underlay_planned) {
            mPaint.color = ContextCompat.getColor(context, R.color.t_dark)
            //cr->set_dash( s_dash_pattern, 0 );
            mPaint.strokeWidth = cStrokeWidth * 2

            mPath.moveTo(cXMin,
                         mYMax - mCoefficient *
                                 (mData!!.values_plan[mStepStart] - mVMin).toFloat())

            for(i in 1 until mStepCount) {
                mPath.lineTo(cXMin + mStepX * i,
                             mYMax - mCoefficient *
                                     (mData!!.values_plan[mStepStart + i] - mVMin).toFloat())
            }
            canvas.drawPath(mPath, mPaint)
            mPath.reset()
            //cr->unset_dash();
        }

        // OVERVIEW
        if(mStepCount < mSpan) {
            // OVERVIEW REGION
            mPaint.style = Paint.Style.FILL
            mPaint.color = Color.parseColor("#DDDDDD")

            canvas.drawRect(0f, mHeight - mOvHeight,
                            mWidth.toFloat(), mHeight.toFloat(),
                            mPaint)

            mPaint.color = ContextCompat.getColor(context, R.color.t_lighter)
            val ptX = cOffsetLabel + (mStepStart * mStepXOv)
            canvas.drawRect(ptX, mHeight - mOvHeight,
                            ptX + ((mStepCount - 1) * mStepXOv), mHeight.toFloat(),
                            mPaint)

            // OVERVIEW LINE
            mPaint.color = ContextCompat.getColor(context, R.color.t_mid)
            mPaint.strokeWidth = 2 * cStrokeWidth
            mPaint.style = Paint.Style.STROKE

            mPath.moveTo(cOffsetLabel,
                         (mHeight - cOffsetLabel - mCoeffOv * (mData!!.values[0] - mVMin)).toFloat())
            for( i in 1 until mSpan ) {
                mPath.lineTo(cOffsetLabel + mStepXOv * i,
                             (mHeight - cOffsetLabel - mCoeffOv * (mData!!.values[i] - mVMin)).toFloat())
            }
            canvas.drawPath(mPath, mPaint)
            mPath.reset()
        }
    }

    // INNER CLASSES ===============================================================================
    inner class ScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if(mZoomLevel < 0.05)
                return false

            if(detector.scaleFactor > 1) { // zoom out
                setZoom(mZoomLevel * 0.95)
            }
            else { // zoom in
                setZoom(mZoomLevel * 1.1)
            }
            return true
        }
    }

    inner class SwipeGestureListener : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distX: Float, distY: Float):
                Boolean {
            scroll((distX * mStepCount / mSpan).toInt())

            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
    }
}
