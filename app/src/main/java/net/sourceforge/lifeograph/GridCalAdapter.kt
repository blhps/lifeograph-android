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
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import java.util.*

// Days in Current Month
internal class GridCalAdapter(context: Context, date: Date) : BaseAdapter() {
    private val mContext: Context = context
    private var mDaysInMonth = 0
    val mDateCurrent = Date(date.m_date)
    var mListDays: MutableList<Long> = ArrayList()

    init {
        showMonth(date)
    }

    override fun getItem(position: Int): String {
        return mListDays[position].toString()
    }

    override fun getCount(): Int {
        return mListDays.size
    }

    fun showMonth(date: Date) {
        mDateCurrent.m_date = date.m_date
        mListDays.clear()
        notifyDataSetChanged()

        // HEADER
        for(i in 0..6) {
            mListDays.add(0L)
        }
        mDaysInMonth = date._days_in_month
        val date2 = Date(date.m_date)
        date2._day = 1
        val numSlotBefore = date2._weekday
        val prevMonth = Date(date.m_date)
        prevMonth.backward_months(1)
        val prevMonthLength = prevMonth._days_in_month
        val nextMonth = Date(date2.m_date)
        nextMonth.forward_months(1)

        // Prev Month days
        for(i in (prevMonthLength - numSlotBefore + 1)..prevMonthLength) {
            mListDays.add(Date.make(prevMonth._year, prevMonth._month, i, 1))
        }

        // Current Month Days
        for(i in 0 until mDaysInMonth) {
            mListDays.add(Date.make(date._year, date._month, i + 1, 1))
        }

        // Next Month days
        //final int numSlotAfter = 7 - ( ( numSlotBefore + mDaysInMonth ) % 7 );
        // always use 6 rows:
        val numSlotAfter = (42 - numSlotBefore - mDaysInMonth)
        for(i in 1..numSlotAfter) {
            mListDays.add(Date.make(nextMonth._year, nextMonth._month, i, 1))
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row: View =
            if(convertView == null) {
                val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                        as LayoutInflater
                inflater.inflate(R.layout.cal_day, parent, false)
            }
            else
                convertView
        val tvDayNo = row.findViewById<TextView>(R.id.calendar_day_gridcell)
        //TextView num_events_per_day = ( TextView ) row.findViewById( R.id.num_events_per_day );
        //num_events_per_day.setTextColor( Color.GREEN );
        if(position < 7) {
            tvDayNo.text = Date.WEEKDAYSSHORT[position + 1]
            tvDayNo.setTextColor(ContextCompat.getColor(mContext, R.color.t_mid))
            tvDayNo.textScaleX = 0.65f
        }
        else {
            val date = Date(mListDays[position] + 1)
            tvDayNo.text = date._day.toString()
            val flagWithinMonth = date._month == mDateCurrent._month
            val flagWeekDay = date._weekday > 0
            when {
                Diary.d.get_entry_count_on_day(date) > 0 -> {
                    TextViewCompat.setTextAppearance(tvDayNo, R.style.boldText)
                    tvDayNo.setTextColor(
                            if(flagWithinMonth) ContextCompat.getColor(mContext, R.color.t_darker)
                            else Color.DKGRAY)
                }
                else -> {
                    TextViewCompat.setTextAppearance(tvDayNo, R.style.normalText)
                    tvDayNo.setTextColor(
                            when {
                                flagWithinMonth && flagWeekDay -> // weekdays within month
                                    ContextCompat.getColor(mContext, R.color.t_mid)
                                flagWithinMonth -> // weekends within month
                                    ContextCompat.getColor(mContext, R.color.t_light)
                                else ->
                                    Color.GRAY
                            } )

                }
            }

            tvDayNo.setBackgroundColor(
                when {
                    date._pure == mDateCurrent._pure ->
                        ContextCompat.getColor(mContext, R.color.t_lighter)
                    Diary.d.is_open && Diary.d.m_p2chapter_ctg_cur.mMap.containsKey(date._pure) ->
                        ContextCompat.getColor(mContext, R.color.t_lightest)
                    else ->
                        Color.TRANSPARENT
                } )
        }
        return row
    }
}
