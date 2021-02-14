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
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import net.sourceforge.lifeograph.DialogInquireText.InquireListener
import java.io.File
import java.util.*

class FragmentListFilters : FragmentListElems(), InquireListener
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_filters
    override val mMenuId: Int   = R.menu.menu_chart
    override val mName: String  = Lifeograph.getStr(R.string.filters)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFabAdd.setOnClickListener {
            // ask for name
            val dlg = DialogInquireText(
                    context, R.string.create_filter,
                    Lifeograph.getStr(R.string.new_filter),
                    R.string.create, this)
            dlg.show()
        }

        var button = view.findViewById<ImageButton>(R.id.set_active)
        button.setOnClickListener { setSelActive() }
        button = view.findViewById(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
        button = view.findViewById(R.id.dismiss)
        button.setOnClickListener { dismissSel() }
    }
//    override fun onResume() {
//        super.onResume()
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if( id == R.id.enable_edit ) {
            Lifeograph.enableEditing(this)
            return true
        }
        else if( id == R.id.logout_wo_save ) {
            Lifeograph.logoutWithoutSaving(requireView())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateMenuVisibilities() {
        val flagWritable = Diary.diary.is_in_edit_mode
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                Diary.diary.can_enter_edit_mode()
        mMenu.findItem(R.id.logout_wo_save).isVisible = flagWritable
    }

    override fun updateList() {
        mElems.clear()
        mElems.addAll(Diary.diary.m_filters.values)
        //Collections.sort(mFilters, FragmentEntryList.compareElemsByName)
    }

    private fun addFilter(name: String, definition: String) {
        Diary.diary.create_filter(name, definition)
        handleElemNumberChanged()
        updateList()
        mAdapter.notifyDataSetChanged()
    }

    private fun setSelActive() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                if(Diary.diary.set_filter_active(filter._name)) {
                    mAdapter.notifyDataSetChanged()
                    break
                }
            }
        }
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                val dlg = DialogInquireText(
                        context, R.string.duplicate_filter,
                        filter._name,
                        R.string.create, this)
                dlg.show()
                break
            }
        }
    }

    private fun dismissSel() {
        var flagDeleted = false
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                if(Diary.diary.dismiss_filter(filter._name))
                    flagDeleted = true
            }
        }

        if(flagDeleted) {
            handleElemNumberChanged()
            updateList()
            mAdapter.clearSelection(mRecyclerView.layoutManager!!)
            exitSelectionMode()
        }
    }

    fun handleElemNumberChanged() {
        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(Diary.diary.m_filters.size, false))
    }

    // INTERFACE METHODS ===========================================================================
    override fun hasIcon2(elem: DiaryElement): Boolean {
        return Diary.diary._filter_active_name.equals(elem._name)
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return R.drawable.ic_check
    }

    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.create_filter) {
            addFilter(text, Filter.DEFINITION_EMPTY)
        }
        else if( id == R.string.duplicate_filter ) {
            for((i, selected) in mSelectionStatuses.withIndex()) {
                if(selected) {
                    val filter = mElems[i] as Filter
                    addFilter(text, filter._definition)
                    break
                }
            }
        }
    }
}
