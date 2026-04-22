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
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import net.sourceforge.lifeograph.DialogInquireText.Listener
import java.util.Collections

class FragmentListFilters : FragmentListElems(), Listener
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_filters
    override val mMenuId: Int   = R.menu.menu_chart
    override val mName: String  = Lifeograph.getStr(R.string.filters)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var button = view.findViewById<ImageButton>(R.id.set_active)
        button.setOnClickListener { setSelActive() }
        button = view.findViewById(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
        button = view.findViewById(R.id.dismiss)
        button.setOnClickListener { dismissSel() }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when( menuItem.itemId) {
            R.id.enable_edit -> {
                Lifeograph.enableEditing(this)
                true
            }
            R.id.logout_wo_save -> {
                Lifeograph.logoutWithoutSaving(requireView())
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun updateMenuVisibilities() {
        val dm = Diary.main
        val flagWritable = dm.is_in_edit_mode()
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                dm.can_enter_edit_mode()
        mMenu.findItem(R.id.logout_wo_save).isVisible = flagWritable
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun updateList() {
        val dm = Diary.main
        mElems.clear()
        mElems.add((Filter.ALL))
        dm.get_filter_nontrashed()?.let { mElems.add(it) }
        dm.get_filter_trashed()?.let { mElems.add(it) }
        mElems.addAll(dm.get_filters() as Collection<DiaryElement>)

        mItemCount = mElems.size

        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(mItemCount, false))
        mAdapter.notifyDataSetChanged()
    }

    override fun createNewElem() {
        // ask for name
        DialogInquireText(requireContext(),
                          R.string.create_filter,
                          Lifeograph.getStr(R.string.new_filter),
                          R.string.create, this).show()
    }

    private fun setSelActive() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                val dm = Diary.main
                dm.set_filter_list(filter)
                dm.update_all_entries_filter_status()
                exitSelectionMode()
                updateList()
                break
            }
        }
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                DialogInquireText( requireContext(),
                                   R.string.duplicate_filter,
                                   filter._name,
                                   R.string.create, this).show()
                break
            }
        }
    }

    private fun dismissSel() {
        var flagDeleted = false
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val filter = mElems[i] as Filter
                if(Diary.main.dismiss_filter(filter._name))
                    flagDeleted = true
            }
        }

        if(flagDeleted) {
            updateList()
            exitSelectionMode()
        }
    }

    // INTERFACE METHODS ===========================================================================
    override fun hasIcon2(elem: DiaryElement): Boolean {
        val filter = Diary.main.get_filter_list()
        return if (filter != null) (filter.mNativePtr == elem.mNativePtr) else false
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return R.drawable.ic_check
    }

    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.create_filter) {
            Diary.main.create_filter(text)
            updateList()
        }
        else if( id == R.string.duplicate_filter ) {
            for((i, selected) in mSelectionStatuses.withIndex()) {
                if(selected) {
                    val filter = mElems[i] as Filter
                    Diary.main.duplicate_filter(filter._name)
                    updateList()
                    break
                }
            }
        }
    }
}
