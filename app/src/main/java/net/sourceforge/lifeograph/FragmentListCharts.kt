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

import android.util.Log
import android.view.*

class FragmentListCharts : FragmentListElems()
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_chart
    override val mMenuId: Int   = R.menu.menu_chart
    override val mName: String  = Lifeograph.getStr( R.string.charts )

    // METHODS =====================================================================================
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
        Log.d(Lifeograph.TAG, "FragmentChartList.updateList()::ALL ENTRIES")
        mElems.addAll(Diary.diary.m_charts.values)
        //Collections.sort(mElems, FragmentEntryList.compareElemsByDate)
    }

    // INTERFACE METHODS ===========================================================================
    override fun onInquireAction( id: Int, text: String ) {
        Lifeograph.showToast("not implemented yet")
    }
    override fun onInquireTextChanged( id: Int, text: String ): Boolean {
        Lifeograph.showToast("not implemented yet")
        return false
    }
}
