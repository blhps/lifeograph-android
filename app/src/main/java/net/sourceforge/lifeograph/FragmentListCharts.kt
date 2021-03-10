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
import android.view.View
import android.widget.ImageButton
import java.util.*

class FragmentListCharts : FragmentListElems()
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_charts
    override val mMenuId: Int   = R.menu.menu_chart
    override val mName: String  = Lifeograph.getStr( R.string.charts )

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var button = view.findViewById<ImageButton>(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
        button = view.findViewById(R.id.dismiss)
        button.setOnClickListener { dismissSel() }
    }

    override fun updateList() {
        mElems.clear()
        Log.d(Lifeograph.TAG, "FragmentChartList.updateList()::ALL ENTRIES")
        mElems.addAll(Diary.d.m_charts.values)
        //Collections.sort(mElems, FragmentEntryList.compareElemsByDate)

        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(Diary.d.m_charts.size, false))
        mItemCount = mElems.size
    }

    override fun createNewElem() {
        // ask for name
        DialogInquireText(requireContext(),
                          R.string.create_chart,
                          Lifeograph.getStr(R.string.new_chart),
                          R.string.create,
                          this).show()
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                DialogInquireText(requireContext(),
                                  R.string.duplicate_chart,
                                  mElems[i]._name,
                                  R.string.create, this).show()
                break
            }
        }
    }

    private fun dismissSel() {
        var flagDeleted = false
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                if(Diary.d.dismiss_chart(mElems[i]._name))
                    flagDeleted = true
            }
        }

        if(flagDeleted) {
            updateList()
            mAdapter.clearSelection(mRecyclerView.layoutManager!!)
            exitSelectionMode()
        }
    }

    // INTERFACE METHODS ===========================================================================
    override fun onInquireAction( id: Int, text: String ) {
        Lifeograph.showToast("not implemented yet")
    }
}
