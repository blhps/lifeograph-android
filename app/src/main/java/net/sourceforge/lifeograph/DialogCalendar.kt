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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

private const val MIN_SCALE = 0.95f
private const val MIN_ALPHA = 0.75f

class DialogCalendar : DialogFragment() {
    // VARIABLES ===================================================================================
    private lateinit var mViewPagerCal: ViewPager
    private lateinit var mAdapterPrev: GridCalAdapter
    private lateinit var mAdapterCurr: GridCalAdapter
    private lateinit var mAdapterNext: GridCalAdapter
    private var mFlagDuringJump = false
    private lateinit var mYearBar: ViewYearBar
    private lateinit var mMonthLabelPrev: TextView
    private lateinit var mMonthLabelCurr: TextView
    private lateinit var mMonthLabelNext: TextView
    private val mDate: Date = Date(Date.get_today(0))
    private lateinit var mButtonCreateChapter: Button
    private val mAllowEntryCreation: Boolean
    private val mAllowChapterCreation: Boolean = Diary.d.is_in_edit_mode
    private val mListener: Listener? = null

    // METHODS =====================================================================================
    init {
        mAllowEntryCreation = mAllowChapterCreation
    }
    //    DialogCalendar( Listener listener, boolean allowCreation ) {
    //        mListener = listener;
    //        mAllowEntryCreation = allowCreation;
    //        mAllowChapterCreation = allowCreation;
    //    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_calendar, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setTitle(R.string.calendar)
        mViewPagerCal = view.findViewById(R.id.vp_calendar)
        mYearBar = view.findViewById(R.id.year_bar)
        val buttonCreateEntry = view.findViewById<Button>(R.id.buttonCreateEntry)
        mButtonCreateChapter = view.findViewById(R.id.buttonCreateChapter)

        mViewPagerCal.adapter = CalendarPagerAdapter(requireContext())
        mViewPagerCal.addOnPageChangeListener(ViewPagerCalendarChangeListener())
        mViewPagerCal.setCurrentItem(1, false)
        mViewPagerCal.setPageTransformer(true, ZoomOutPageTransformer())

        mYearBar.setOnYearChangedListener(
                object : ViewYearBar.YearChangedListener {
                    override fun onYearChanged(year: Int) {
                        mDate._year = year
                        update()
                    }
                })

        buttonCreateEntry.setOnClickListener { createEntry() }
        buttonCreateEntry.visibility = if(mAllowEntryCreation) View.VISIBLE else View.GONE
        mButtonCreateChapter.setOnClickListener { createChapter() }
        mButtonCreateChapter.isEnabled = mAllowChapterCreation &&
                !Diary.d.m_p2chapter_ctg_cur.mMap.containsKey(mDate.m_date)
        mButtonCreateChapter.visibility = if(mAllowChapterCreation) View.VISIBLE else View.GONE
        dialog?.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    private fun createEntry() {
        val e = Diary.d.create_entry(mAdapterCurr.mDateCurrent.m_date, "")
        dismiss()
        Lifeograph.showElem(e)
    }

    private fun createChapter() {
        dismiss()
        mListener!!.createChapter(mAdapterCurr.mDateCurrent.m_date)
    }

    private fun update() {
        val datePrev = Date(Date.backward_months(mDate.m_date, 1))
        val dateNext = Date(Date.forward_months(mDate.m_date, 1))

        mMonthLabelPrev.text = datePrev._month_str
        mMonthLabelCurr.text = mDate._month_str
        mMonthLabelNext.text = dateNext._month_str

        mAdapterPrev.showMonth(datePrev)
        mAdapterCurr.showMonth(mDate)
        mAdapterNext.showMonth(dateNext)

        mButtonCreateChapter.isEnabled = Diary.d.is_in_edit_mode &&
                !Diary.d.m_p2chapter_ctg_cur.mMap.containsKey(mDate._pure)

        mYearBar.mYear = mDate._year
        mYearBar.invalidate()
    }

    private fun handleDayClicked(pos: Int) {
        if(pos < 7) return // day names row
        val e = Diary.d.get_entry_by_date(mAdapterCurr.mListDays[pos])
        if(e != null) {
            dismiss()
            Lifeograph.showElem(e)
        }
        else {
            mDate.m_date = mAdapterCurr.mListDays[pos]
            mYearBar.mYear = mDate._year
            update()
        }
    }

    interface Listener {
        fun createChapter(date: Long)
        //val relatedActivity: Activity?
    }

    // INLINE CLASSES ==============================================================================
    inner class CalendarPagerAdapter(val context: Context): PagerAdapter() {
        @SuppressLint("CutPasteId")
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.calendar, container,
                                          false) as ViewGroup

            val gridCalendar = layout.findViewById<GridView>(R.id.gv_calendar)

            when(position) {
                0 -> {
                    mMonthLabelPrev = layout.findViewById(R.id.tv_month_name)
                    mAdapterPrev = GridCalAdapter(Lifeograph.context, mDate)
                    gridCalendar.adapter = mAdapterPrev
                }
                1 -> {
                    mMonthLabelCurr = layout.findViewById(R.id.tv_month_name)
                    mAdapterCurr = GridCalAdapter(Lifeograph.context, mDate)
                    gridCalendar.adapter = mAdapterCurr
                }
                2 -> {
                    mMonthLabelNext = layout.findViewById(R.id.tv_month_name)
                    mAdapterNext = GridCalAdapter(Lifeograph.context, mDate)
                    gridCalendar.adapter = mAdapterNext
                }
            }
            if(::mMonthLabelPrev.isInitialized &&
               ::mMonthLabelCurr.isInitialized &&
               ::mMonthLabelNext.isInitialized) update()

            gridCalendar.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?,
                                                                     pos: Int, _: Long ->
                handleDayClicked(pos)
            }

            container.addView(layout)

            Log.d(Lifeograph.TAG, "Instantiate Item at pos:($position) in ViewPager")

            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {

            Log.d(Lifeograph.TAG, "DESTROY Item at pos:($position) in ViewPager")
            container.removeView(view as View)
        }

        override fun getCount(): Int {
            return 3
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }
    }

    inner class ViewPagerCalendarChangeListener : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(a: Int, b: Float, c: Int) {
            // We do nothing here.
        }

        override fun onPageSelected(position: Int) {
            Log.d(Lifeograph.TAG, "onPageSelected($position) mFlagDuringJ: $mFlagDuringJump")

            if(!mFlagDuringJump && ::mMonthLabelPrev.isInitialized) {
                mFlagDuringJump = true
                mViewPagerCal.setCurrentItem(1, false)
                mFlagDuringJump = false

                when {
                    position < 1 -> mDate.backward_months(1)
                    position > 1 -> mDate.forward_months(1)
                }

                update()
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            Log.d(Lifeograph.TAG, "onPageScrollStateChanged($state)")
        }
    }

    class ZoomOutPageTransformer : ViewPager.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                val pageHeight = height
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 1 -> { // [-1,1]
                        // Modify the default slide transition to shrink the page as well
                        val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position))
                        val vertMargin = pageHeight * (1 - scaleFactor) / 2
                        val horzMargin = pageWidth * (1 - scaleFactor) / 2
                        translationX = if (position < 0) {
                            horzMargin - vertMargin / 2
                        } else {
                            horzMargin + vertMargin / 2
                        }

                        // Scale the page down (between MIN_SCALE and 1)
                        scaleX = scaleFactor
                        scaleY = scaleFactor

                        // Fade the page relative to its size.
                        alpha = (MIN_ALPHA +
                                (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }

}
