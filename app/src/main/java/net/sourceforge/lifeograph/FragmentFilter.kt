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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import net.sourceforge.lifeograph.Lifeograph.DiaryEditor
import java.util.*

class FragmentFilter : Fragment(), DiaryEditor {
    // VARIABLES ===================================================================================
    private lateinit var mEditSearch: EditText
    private lateinit var mButtonSearchTextClear: Button
    private lateinit var mButtonShowTodoNot: ToggleImageButton
    private lateinit var mButtonShowTodoOpen: ToggleImageButton
    private lateinit var mButtonShowTodoProgressed: ToggleImageButton
    private lateinit var mButtonShowTodoDone: ToggleImageButton
    private lateinit var mButtonShowTodoCanceled: ToggleImageButton
    private lateinit var mSpinnerShowFavorite: Spinner
    private var mTextInitialized = false

    companion object {
        lateinit var mFilter: Filter
    }

    // METHODS =====================================================================================
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        Log.d(Lifeograph.TAG, "FragmentFilter.onCreateView()")
        val rootView = inflater.inflate(
                R.layout.fragment_filter, container, false) as ViewGroup

        // FILLING WIDGETS
        mEditSearch = rootView.findViewById(R.id.editTextSearch)
        mButtonSearchTextClear = rootView.findViewById(R.id.buttonClearText)
        mButtonShowTodoNot = rootView.findViewById(R.id.show_todo_not)
        mButtonShowTodoOpen = rootView.findViewById(R.id.show_todo_open)
        mButtonShowTodoProgressed = rootView.findViewById(R.id.show_todo_progressed)
        mButtonShowTodoDone = rootView.findViewById(R.id.show_todo_done)
        mButtonShowTodoCanceled = rootView.findViewById(R.id.show_todo_canceled)
        mSpinnerShowFavorite = rootView.findViewById(R.id.spinnerFavorites)
        if(Lifeograph.screenHeight >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mEditSearch.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        // UI UPDATES (must come before listeners)
        updateFilterWidgets(Diary.d.m_filter_active._status)
        if(Diary.d.is_search_active) {
            mEditSearch.setText(Diary.d._search_text)
            mButtonSearchTextClear.visibility = View.VISIBLE
        }

        // LISTENERS
        mEditSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if(mTextInitialized) {
                    handleSearchTextChanged(s.toString())
                    mButtonSearchTextClear.visibility =
                            if(s.isNotEmpty()) View.VISIBLE else View.INVISIBLE
                }
                else mTextInitialized = true
            }
        })
        mButtonSearchTextClear.setOnClickListener { mEditSearch.setText("") }
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
        val buttonFilterReset = rootView.findViewById<Button>(R.id.buttonFilterReset)
        buttonFilterReset.setOnClickListener { resetFilter() }
        val buttonFilterSave = rootView.findViewById<Button>(R.id.buttonFilterSave)
        buttonFilterSave.setOnClickListener { saveFilter() }
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(Lifeograph.TAG, "FragmentFilter.onAttach() - $context")

//        if( context instanceof FragmentListEntries.ListOperations )
//            mListOperations = ( FragmentListEntries.ListOperations ) context;
//        else
//            throw new ClassCastException( context.toString() + " must implement ListOperations" );
    }

    override fun onResume() {
        super.onResume()
        mTextInitialized = false
        mEditSearch.setText(Diary.d._search_text)
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(Lifeograph.TAG, "FragmentFilter.onDetach() - $this")

//        mListOperations = null;
    }

    fun handleSearchTextChanged(text: String) {
        Diary.d.set_search_text(text.toLowerCase(Locale.ROOT), false)
        //        mListOperations.updateList();
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
        var showFav = true
        var showNotFav = true
        when(i) {
            0 -> {
                showFav = true
                showNotFav = true
            }
            1 -> {
                showFav = false
                showNotFav = true
            }
            2 -> {
                showFav = true
                showNotFav = false
            }
        }

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

    override fun enableEditing() {
        TODO("Not yet implemented")
    }

    override fun handleBack(): Boolean {
        return false
    }
}
