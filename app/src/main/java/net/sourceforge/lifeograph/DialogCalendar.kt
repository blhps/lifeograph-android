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
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import net.sourceforge.lifeograph.Lifeograph.Companion.showElem
import kotlin.math.abs

private const val MIN_SCALE = 0.95f
private const val MIN_ALPHA = 0.75f

class DialogCalendar : DialogFragment() {
    // VARIABLES ===================================================================================
    private lateinit var mViewPagerCal: ViewPager
    private var mAdapter: GridCalAdapter? = null
    private var mDate: Date = Date(Date.get_today(0))
    private lateinit var mNumberPickerMonth: NumberPicker
    private lateinit var mNumberPickerYear: NumberPicker
    private lateinit var mButtonCreateChapter: Button
    private val mAllowEntryCreation: Boolean
    private val mAllowChapterCreation: Boolean = Diary.d.is_in_edit_mode
    private val mListener: Listener? = null

    // METHODS =====================================================================================
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
//        val gridCalendar = view.findViewById<GridView>(R.id.gridViewCalendar)
        mViewPagerCal = view.findViewById(R.id.vp_calendar)
        mAdapter = GridCalAdapter(Lifeograph.context, mDate)
        mNumberPickerMonth = view.findViewById(R.id.numberPickerMonth)
        mNumberPickerYear = view.findViewById(R.id.numberPickerYear)
        val buttonCreateEntry = view.findViewById<Button>(R.id.buttonCreateEntry)
        mButtonCreateChapter = view.findViewById(R.id.buttonCreateChapter)
        mAdapter!!.notifyDataSetChanged()
//        gridCalendar.adapter = mAdapter
//        gridCalendar.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?,
//                                                                 pos: Int, _: Long ->
//            handleDayClicked(pos)
//        }
        mViewPagerCal.adapter = CalendarPagerAdapter(requireContext())
        mViewPagerCal.addOnPageChangeListener(ViewPagerCalendarChangeListener())
        mViewPagerCal.setCurrentItem(1, false)
        mViewPagerCal.setPageTransformer(true, ZoomOutPageTransformer())

        mNumberPickerMonth.setOnValueChangedListener { _: NumberPicker?, _: Int, n: Int ->
            mDate._month = n
            if(mDate._day > mDate._days_in_month) mDate._day = mDate._days_in_month
            handleDayChanged()
        }
        mNumberPickerYear.setOnValueChangedListener { _: NumberPicker?, _: Int, n: Int ->
            mDate._year = n
            mDate._day = mDate._days_in_month
            handleDayChanged()
        }
        mNumberPickerMonth.minValue = 1
        mNumberPickerMonth.maxValue = 12
        mNumberPickerYear.minValue = Date.YEAR_MIN
        mNumberPickerYear.maxValue = Date.YEAR_MAX
        mNumberPickerMonth.value = mDate._month
        mNumberPickerYear.value = mDate._year
        buttonCreateEntry.setOnClickListener { createEntry() }
        buttonCreateEntry.visibility = if(mAllowEntryCreation) View.VISIBLE else View.INVISIBLE
        mButtonCreateChapter.setOnClickListener { createChapter() }
        mButtonCreateChapter.isEnabled = mAllowChapterCreation &&
                !Diary.d.m_p2chapter_ctg_cur.mMap.containsKey(mDate.m_date)
        mButtonCreateChapter.visibility = if(mAllowChapterCreation) View.VISIBLE else View.INVISIBLE
        dialog?.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    private fun createEntry() {
        val e = Diary.d.create_entry(
                mAdapter!!.mDateCurrent.m_date, "")
        dismiss()
        showElem(e)
    }

    private fun createChapter() {
        dismiss()
        mListener!!.createChapter(mAdapter!!.mDateCurrent.m_date)
    }

    private fun handleDayChanged() {
        mAdapter!!.showMonth(mDate)
        mButtonCreateChapter.isEnabled = Diary.d.is_in_edit_mode &&
                !Diary.d.m_p2chapter_ctg_cur.mMap.containsKey(mDate.m_date)
    }

    private fun handleDayClicked(pos: Int) {
        if(pos < 7) return
        val e = Diary.d.m_entries[mAdapter!!.mListDays[pos] + 1]
        if(e != null) {
            dismiss()
            showElem(e)
        }
        else {
            mDate.m_date = mAdapter!!.mListDays[pos]
            mNumberPickerMonth.value = mDate._month
            mNumberPickerYear.value = mDate._year
            handleDayChanged()
        }
    }

    interface Listener {
        fun createChapter(date: Long)
        //val relatedActivity: Activity?
    }

    init {
        mAllowEntryCreation = mAllowChapterCreation
    }

    // INLINE CLASSES ==============================================================================
    inner class CalendarPagerAdapter(val context: Context): PagerAdapter() {
        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.calendar, collection,
                                          false) as ViewGroup

            val gridCalendar = layout.findViewById<GridView>(R.id.gv_calendar)
            gridCalendar.adapter = mAdapter
            gridCalendar.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?,
                                                                     pos: Int, _: Long ->
                handleDayClicked(pos)
            }

            collection.addView(layout)

            Log.d(Lifeograph.TAG, "Instantiate Item at pos:($position) in ViewPager")
//            when(position) {
//                0 -> {
//                    mDate.backward_months(1)
//                    mViewPagerCal.setCurrentItem(1, false)
//                }
//                2 -> {
//                    mDate.forward_months(1)
//                    //handleDayChanged()
//                    mViewPagerCal.setCurrentItem(1, false)
//                }
//            }

            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
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

        private var flagJump = false

        override fun onPageScrolled(position: Int,
                                       positionOffset: Float,
                                       positionOffsetPixels: Int) {
            // We do nothing here.
        }

        override fun onPageSelected(position: Int) {
            Log.d(Lifeograph.TAG, "onPageSelected($position) in ViewPager")
            when(position) {
                0 -> {
                    flagJump = true
                    mDate.backward_months(1)
                    handleDayChanged()

                }
                2 -> {
                    flagJump = true
                    mDate.forward_months(1)
                    handleDayChanged()

                }
                else -> flagJump = false
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            //Let's wait for the animation to complete then do the jump.
            if (flagJump && state == ViewPager.SCROLL_STATE_IDLE) {
                // Jump without animation so the user is not aware what happened.
                mViewPagerCal.setCurrentItem(1, false)
            }
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
