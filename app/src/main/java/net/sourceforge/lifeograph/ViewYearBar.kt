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
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs


class ViewYearBar(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // VARIABLES ===================================================================================
    private val mPaint: Paint = Paint()
    var         mYear: Int = Date.get_year(Date.get_today(0))
    private val mSwipeGestureDetector: GestureDetector
    private var mListener: YearChangedListener? = null

    // GEOMETRICAL VARIABLES =======================================================================
    private var mWidth = -1
    private var mHeight = -1
    private val mHeightLabel = Lifeograph.screenShortEdge * Lifeograph.sDPIX / 25f
    private var mScrollThreshold = 0f
    private var mCumulativeScroll = 0f
    private val mSizes = arrayOf(1.0f, 1.2f, 1.6f)
    private var mXPositions = arrayOf(0.1f, 0.27f, 0.5f, 0.73f, 0.9f)

    // METHODS =====================================================================================
    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.textAlign = Paint.Align.CENTER
        mSwipeGestureDetector = GestureDetector(context, GestureListener())
        setOnTouchListener { v: View?, event: MotionEvent? ->
            v!!.performClick()
            if(event!!.action == KeyEvent.ACTION_UP) {
                resetPos()
                true
            }
            else
                mSwipeGestureDetector.onTouchEvent(event)
        }
    }

    fun setOnYearChangedListener(listener: YearChangedListener) {
        mListener = listener
    }

    private fun resetPos() {
        mCumulativeScroll = 0f
        invalidate()
    }

    fun scroll(offset: Int) {
        mYear += offset

        mListener!!.onYearChanged(mYear)

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mScrollThreshold = w / 12f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        for(i in 0..4) {
            mPaint.textSize = mHeightLabel * mSizes[2 - abs(i - 2)]
            mPaint.color = when(i) {
                0, 4 -> Color.LTGRAY
                1, 3 -> Color.GRAY
                else -> ContextCompat.getColor(context, R.color.t_darker)
            }
            canvas.drawText((mYear + i - 2).toString(),
                            (mWidth * mXPositions[i]) - mCumulativeScroll,
                            (mHeight + mPaint.textSize) / 2f,
                            mPaint)
        }
    }

    // INNER CLASSES ===============================================================================
    inner class GestureListener : SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distX: Float, distY: Float):
                Boolean {
            mCumulativeScroll += distX

            if(abs(mCumulativeScroll) > mScrollThreshold) {
                mCumulativeScroll = 0f
                scroll(if(distX > 0) 1 else -1)
            }
            else
                invalidate()

            return true
        }

        // it does not work if we dont override this:
        override fun onDown(e: MotionEvent): Boolean {
            mCumulativeScroll = 0f
            return true
        }
    }

    // INTERFACE
    interface YearChangedListener {
        fun onYearChanged(year: Int)
    }
}
