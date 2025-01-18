/* *********************************************************************************

 Copyright (C) 2012-2025 Ahmet Öztürk (aoz_2@yahoo.com)

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
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FragmentFilter : FragmentDiaryEditor(), RVAdapterFilterers.Listener  {
    // VARIABLES ===================================================================================
    override val            mLayoutId: Int = R.layout.fragment_filter
    override val            mMenuId: Int   = R.menu.menu_chart // same for now

    //protected val           mName: String

    private val             mElems: MutableList<Filterer> = ArrayList()
    private val             mSelectionStatuses: MutableList<Boolean> = ArrayList()
    private lateinit var    mAdapter: RVAdapterFilterers
    private lateinit var    mRecyclerView: RecyclerView
    private lateinit var    mFabAdd: FloatingActionButton
    private lateinit var    mToolbar: HorizontalScrollView
    companion object {
        lateinit var mFilter: Filter
        lateinit var mStack: FiltererContainer
        fun setFilter(filter: Filter) {
            mFilter = filter
            mStack = mFilter._filterer_stack
        }
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this

        mRecyclerView = view.findViewById(R.id.list_elems)
        mAdapter = RVAdapterFilterers(mElems, mSelectionStatuses, this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)
        mFabAdd = view.findViewById(R.id.fab_add)
        mFabAdd.setOnClickListener { addFilterer() }
        mToolbar = view.findViewById(R.id.toolbar_elem)


        // FILLING WIDGETS

        // UI UPDATES (must come before listeners)
        updateFilterWidgets()

        // LISTENERS
        val buttonFilterReset = view.findViewById<ImageButton>(R.id.reset_filter)
        buttonFilterReset.setOnClickListener { resetFilter() }
        val buttonFilterSave = view.findViewById<ImageButton>(R.id.save_filter)
        buttonFilterSave.setOnClickListener { saveFilter() }
        val buttonFilterRemove = view.findViewById<ImageButton>(R.id.remove_filterer)
        buttonFilterRemove.setOnClickListener { removeFilterer() }
    }

    override fun onResume() {
        super.onResume()

        mToolbar.visibility = View.GONE

        Lifeograph.getActionBar().subtitle = mFilter._title_str
    }

    override fun onDestroy() {
        super.onDestroy()
        saveFilter()
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()
    }

    private fun updateFilterWidgets() {
        mElems.clear()
        for(filterer in mStack.m_pipeline)
            mElems.add(filterer)
    }

    private fun addFilterer() {
        DialogPicker(requireContext(), object: DialogPicker.Listener{
            override fun onItemClick(item: RViewAdapterBasic.Item) {
                when(item.mId) {
                    "STATUS" -> { mStack.add_filterer_status( DiaryElement.ES_FILTER_TRASHED ) }
                    "TRASHED" -> { mStack.add_filterer_trashed( true ) }
                    "IS" -> { mStack.add_filterer_is( DiaryElement.DEID_UNSET, true ) }
                    else -> { mStack.add_filterer_favorite( true ) }
                }
                updateFilterWidgets()
            }

            override fun populateItems(list: RVBasicList) {
                list.clear()

                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_status), "STATUS",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_is), "IS",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_favorite), "FAVORITE",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_trashed), "TRASHED",
                    R.drawable.ic_filter))
            }
        }).show()
    }
    private fun removeFilterer() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filterer = mElems[i]
                mStack.remove_filterer( filterer )
                mAdapter.notifyItemChanged( i )
                break
            }
        }
    }
    private fun saveFilter() {
        val sb = StringBuilder()
        mStack.get_as_string( sb )
        mFilter._definition = sb.toString()

        Diary.d.updateAllEntriesFilterStatus()
    }
    private fun resetFilter() {
        mStack = mFilter._filterer_stack
        updateFilterWidgets()
    }

    override fun updateActionBarSubtitle() {
        val selCount = mAdapter.mSelCount
        val itmCount = mAdapter.itemCount
        Lifeograph.getActionBar().subtitle =
            if( selCount > 0 ) "${mFilter._name} ($selCount/$itmCount)"
            else               "${mFilter._name} ($itmCount)"
    }

    override fun enterSelectionMode(): Boolean {
        return if(Diary.d.is_in_edit_mode) {
            mToolbar.visibility = View.VISIBLE
            true
        }
        else false
    }

    override fun exitSelectionMode() {
        updateActionBarSubtitle()
        mToolbar.visibility = View.GONE
    }
}
