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
import android.util.Log
import android.view.View
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
//    private lateinit var    mToolbar: HorizontalScrollView
    private lateinit var    mButtonReset : ImageButton
    private lateinit var    mButtonSave : ImageButton
    private lateinit var    mButtonRemove : ImageButton
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
        super.onViewCreated(view, savedInstanceState)

        mRecyclerView = view.findViewById(R.id.list_elems)
        mAdapter = RVAdapterFilterers(mElems, mSelectionStatuses, this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)
        mFabAdd = view.findViewById(R.id.fab_add)
        mFabAdd.setOnClickListener { addFilterer() }
//        mToolbar = view.findViewById(R.id.toolbar_elem)
        mButtonReset = view.findViewById(R.id.reset_filter)
        mButtonSave = view.findViewById(R.id.save_filter)
        mButtonRemove = view.findViewById(R.id.remove_filterer)

        // LISTENERS
        mButtonReset.setOnClickListener { resetFilter() }
        mButtonSave.setOnClickListener { saveFilter() }
        mButtonRemove.setOnClickListener { removeFilterer() }

        // UI UPDATES (must come before listeners)
        updateFilterWidgets()
    }

    override fun onResume() {
        super.onResume()

        updateToolbarButtons()

        //Lifeograph.getActionBar().subtitle = mFilter._title_str
        Lifeograph.getActionBar().subtitle = mFilter._name // TODO implemet title str
    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }
//
//    override fun updateMenuVisibilities() {
//        super.updateMenuVisibilities()
//    }

    // DiaryEditor interface methods
    override fun enableEditing() {
        super.enableEditing()

        updateToolbarButtons()
    }

    private fun updateToolbarButtons() {
        val flagEdit = Diary.getMain().is_in_edit_mode
        mButtonSave.visibility = if(flagEdit) View.VISIBLE else View.GONE
        mButtonReset.visibility = if(flagEdit) View.VISIBLE else View.GONE
        mButtonRemove.visibility = View.GONE
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
                    "FAVORITE" -> { mStack.add_filterer_favorite( true ) }
                    "TRASHED" -> { mStack.add_filterer_trashed( true ) }
                    "IS" -> { mStack.add_filterer_is( 404 /*TODO: DEID.UNSET*/, true ) }
                    "HASTAG" -> { mStack.add_filterer_tagged_by( null, true ) }
                }
                mSelectionStatuses.add(false)
                updateFilterWidgets()
                mAdapter.notifyItemChanged( mStack.m_pipeline.size - 1 )
            }

            override fun populateItems(list: RVBasicList) {
                list.clear()

                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_status), "STATUS",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_favorite), "FAVORITE",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_trashed), "TRASHED",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filter_is), "IS",
                    R.drawable.ic_filter))
                list.add(RViewAdapterBasic.Item(
                    Lifeograph.getStr(R.string.filterer_has_tag), "HASTAG",
                    R.drawable.ic_filter))
            }
        }).show()
    }
    private fun removeFilterer() {
        val itemsToRemove = mutableListOf<Filterer>()
        val indicesToRemove = mutableListOf<Int>()

        // First, identify all items and their original indices to be removed
        // Iterate backwards to safely use indices for removal from mSelectionStatuses
        for (i in mSelectionStatuses.indices.reversed()) {
            if (mSelectionStatuses[i]) {
                // Check if the index is valid for mElems before trying to access it
                if (i < mElems.size) {
                    itemsToRemove.add(mElems[i]) // Add the Filterer object
                    indicesToRemove.add(i)       // Add the original index in mElems
                } else {
                    // Log an error or handle inconsistency if mSelectionStatuses is out of sync with mElems
                    Log.e("FragmentFilter", "Inconsistency: Selection status for out-of-bounds index $i")
                }
            }
        }

        if (itemsToRemove.isEmpty()) {
            // No items were selected for removal
            return
        }

        // 1. Remove from the primary data source (mStack)
        for (filtererToRemove in itemsToRemove) {
            mStack.remove_filterer(filtererToRemove)
        }

        // 2. Remove from the local lists used by the adapter (mElems and mSelectionStatuses)
        //    It's crucial to remove from the highest index to lowest to avoid shifting issues
        //    if you were modifying mElems and mSelectionStatuses directly in the first loop.
        //    Since indicesToRemove is already sorted from high to low (due to reversed iteration),
        //    this is straightforward.
        for (index in indicesToRemove) {
            if (index < mElems.size) { // Double-check bounds, though should be fine if synced
                mElems.removeAt(index)
            }
            if (index < mSelectionStatuses.size) { // Also for selection statuses
                mSelectionStatuses.removeAt(index)
            }
        }

        // 3. Notify the adapter about the removals.
        //    This is trickier for multiple, non-contiguous removals if you want animations.
        //    The most straightforward way (though less performant for animations)
        //    is to rebuild mElems and call notifyDataSetChanged or use ListAdapter.
        //    For individual notifications, you'd call notifyItemRemoved for each index
        //    (adjusting for previous removals) or group contiguous removals.

        // Option A: Simpler, but loses animations and less efficient
        // updateFilterWidgets() // Repopulate mElems from the updated mStack
        // mAdapter.notifyDataSetChanged()

        // Option B: More granular notifications (if you keep mElems and mSelectionStatuses manually updated)
        // This assumes `indicesToRemove` contains the original indices sorted from highest to lowest.
        // As you call notifyItemRemoved, subsequent items' indices effectively shift.
        // However, since RecyclerView processes these removals, it can often handle it.
        // But it's safer to notify in reverse order of removal as well.
        for (index in indicesToRemove.sortedDescending()) { // Ensure they are sorted high to low
            mAdapter.notifyItemRemoved(index)
        }
        // After removals, you might need to notify for range changes in the remaining items
        // to update their positions if your adapter relies on absolute positions for binding.
        // A simple way if many items might have shifted:
        if (indicesToRemove.isNotEmpty()) {
            // Find the lowest index that was removed
            val lowestRemovedIndex = indicesToRemove.minOrNull() ?: 0
            if (lowestRemovedIndex < mElems.size) {
                mAdapter.notifyItemRangeChanged(lowestRemovedIndex, mElems.size - lowestRemovedIndex)
            } else if (mElems.isNotEmpty()){ // If all items up to the end were removed, but some remain
                mAdapter.notifyItemRangeChanged(0, mElems.size)
            }
            //else if (indicesToRemove.isNotEmpty()) {
                // All items were removed, no range change needed, notifyDataSetChanged would also work
                // or ensure the last notifyItemRemoved covered the last item.
            //}
        }


        // 4. Update UI elements
        updateActionBarSubtitle()
        if (mAdapter.mSelCount == 0 || mSelectionStatuses.none { it }) { // Ensure selection is truly cleared
            exitSelectionMode()
        }
    }

    private fun saveFilter() {
        mFilter._definition = mStack._as_string

        Diary.getMain().updateAllEntriesFilterStatus()
    }
    private fun resetFilter() {
        mStack = mFilter._filterer_stack
        updateToolbarButtons()
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
        return if(Diary.getMain().is_in_edit_mode) {
            mButtonRemove.visibility = View.VISIBLE
            true
        }
        else false
    }

    override fun exitSelectionMode() {
        updateActionBarSubtitle()
        mButtonRemove.visibility = View.GONE
    }
}
