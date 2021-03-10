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

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner

class FragmentFilter : FragmentDiaryEditor(), DialogInquireText.Listener  {
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_filter
    override val mMenuId: Int   = R.menu.menu_chart // same for now

    private lateinit var mEditSearch: EditText
    private lateinit var mButtonShowTodoNot: ToggleImageButton
    private lateinit var mButtonShowTodoOpen: ToggleImageButton
    private lateinit var mButtonShowTodoProgressed: ToggleImageButton
    private lateinit var mButtonShowTodoDone: ToggleImageButton
    private lateinit var mButtonShowTodoCanceled: ToggleImageButton
    private lateinit var mSpinnerShowFavorite: Spinner

    companion object {
        lateinit var mFilter: Filter
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this

        // FILLING WIDGETS
        mButtonShowTodoNot = view.findViewById(R.id.show_todo_not)
        mButtonShowTodoOpen = view.findViewById(R.id.show_todo_open)
        mButtonShowTodoProgressed = view.findViewById(R.id.show_todo_progressed)
        mButtonShowTodoDone = view.findViewById(R.id.show_todo_done)
        mButtonShowTodoCanceled = view.findViewById(R.id.show_todo_canceled)
        mSpinnerShowFavorite = view.findViewById(R.id.spinnerFavorites)
        if(Lifeograph.screenHeight >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mEditSearch.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        // UI UPDATES (must come before listeners)
        updateFilterWidgets(Diary.d.m_filter_active._status)

        // LISTENERS
        mButtonShowTodoNot.setOnClickListener { handleFilterTodoChanged() }
        mButtonShowTodoOpen.setOnClickListener { handleFilterTodoChanged() }
        mButtonShowTodoProgressed.setOnClickListener { handleFilterTodoChanged() }
        mButtonShowTodoDone.setOnClickListener { handleFilterTodoChanged() }
        mButtonShowTodoCanceled.setOnClickListener { handleFilterTodoChanged() }
        mSpinnerShowFavorite.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(pv: AdapterView<*>?, v: View, pos: Int, id: Long) {
                // onItemSelected() is fired unnecessarily during initialization, so:
                if(mInitialized) handleFilterFavoriteChanged(pos) else mInitialized = true
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                Log.d(Lifeograph.TAG, "Filter Favorites onNothingSelected")
            }

            private var mInitialized = false
        }
        val buttonFilterReset = view.findViewById<Button>(R.id.buttonFilterReset)
        buttonFilterReset.setOnClickListener { resetFilter() }
        val buttonFilterSave = view.findViewById<Button>(R.id.buttonFilterSave)
        buttonFilterSave.setOnClickListener { saveFilter() }
    }

    override fun onResume() {
        super.onResume()

        Lifeograph.getActionBar().subtitle = mFilter._title_str
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.rename -> {
                DialogInquireText(requireContext(),
                                  R.string.rename,
                                  mFilter.m_name,
                                  R.string.apply,
                                  this).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val flagWritable = Diary.d.is_in_edit_mode
        mMenu.findItem(R.id.rename).isVisible = flagWritable
    }

    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.rename) {
            Diary.d.rename_filter(mFilter.m_name, text)
            Lifeograph.getActionBar().subtitle = mFilter._title_str
        }
    }

    private fun updateFilterWidgets(fs: Int) {
        mButtonShowTodoNot.isChecked = fs and DiaryElement.ES_SHOW_NOT_TODO != 0
        mButtonShowTodoOpen.isChecked = fs and DiaryElement.ES_SHOW_TODO != 0
        mButtonShowTodoProgressed.isChecked = fs and DiaryElement.ES_SHOW_PROGRESSED != 0
        mButtonShowTodoDone.isChecked = fs and DiaryElement.ES_SHOW_DONE != 0
        mButtonShowTodoCanceled.isChecked = fs and DiaryElement.ES_SHOW_CANCELED != 0
        when(fs and DiaryElement.ES_FILTER_FAVORED) {
            DiaryElement.ES_SHOW_FAVORED -> mSpinnerShowFavorite.setSelection(2)
            DiaryElement.ES_SHOW_NOT_FAVORED -> mSpinnerShowFavorite.setSelection(1)
            DiaryElement.ES_FILTER_FAVORED -> mSpinnerShowFavorite.setSelection(0)
        }
    }

    private fun handleFilterTodoChanged() {
//        Diary.diary.m_filter_active.set_todo(
//                mButtonShowTodoNot.isChecked(),
//                mButtonShowTodoOpen.isChecked(),
//                mButtonShowTodoProgressed.isChecked(),
//                mButtonShowTodoDone.isChecked(),
//                mButtonShowTodoCanceled.isChecked() );
//
//        mListOperations.updateList();
    }

    fun handleFilterFavoriteChanged(i: Int) {
        Log.d(Lifeograph.TAG, i.toString() )
//        var showFav = true
//        var showNotFav = true
//        when(i) {
//            0 -> {
//                showFav = true
//                showNotFav = true
//            }
//            1 -> {
//                showFav = false
//                showNotFav = true
//            }
//            2 -> {
//                showFav = true
//                showNotFav = false
//            }
//        }

        //mFilter.set_favorites( showFav, showNotFav )
    }

    private fun resetFilter() {
//        updateFilterWidgets( Diary.diary.m_filter_default.get_status() );
//        Diary.diary.m_filter_active.set_status_outstanding();
        //mListOperations.updateList();
    }

    private fun saveFilter() {
        Lifeograph.showToast(R.string.filter_saved)
        //        Diary.diary.m_filter_default.set( Diary.diary.m_filter_active );
    }
}
