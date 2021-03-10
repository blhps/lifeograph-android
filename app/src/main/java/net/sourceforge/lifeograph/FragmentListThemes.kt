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
import android.view.View
import android.widget.ImageButton
import net.sourceforge.lifeograph.DialogInquireText.Listener
import java.util.*

class FragmentListThemes : FragmentListElems(), Listener
{
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_themes
    override val mMenuId: Int   = R.menu.menu_list_themes
    override val mName: String  = Lifeograph.getStr(R.string.themes)

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var button = view.findViewById<ImageButton>(R.id.set_active)
        button.setOnClickListener { setSelDefault() }
        button = view.findViewById(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
        button = view.findViewById(R.id.dismiss)
        button.setOnClickListener { dismissSel() }
    }

    @SuppressLint("RestrictedApi")
    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        mFabAdd.visibility = View.GONE
    }

    override fun updateList() {
        mElems.clear()
        mElems.addAll(Diary.d.m_themes.values)
        //Collections.sort(mFilters, FragmentEntryList.compareElemsByName)

        mItemCount = mElems.size

        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(Diary.d.m_themes.size, false))
    }

    override fun createNewElem() { }

    private fun setSelDefault() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val theme = mElems[i] as Theme
                Diary.d._theme_default = theme
                mAdapter.notifyDataSetChanged()
                break
            }
        }
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val theme = mElems[i] as Theme
                DialogInquireText(requireContext(),
                                  R.string.duplicate_filter,
                                  theme._name,
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
                if(Diary.d.dismiss_filter(filter._name))
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
    override fun hasIcon2(elem: DiaryElement): Boolean {
        return Diary.d._theme_default._name.equals(elem._name)
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return R.drawable.ic_check
    }

    override fun onInquireAction(id: Int, text: String) {
        if( id == R.string.duplicate_theme ) {
            for((i, selected) in mSelectionStatuses.withIndex()) {
                if(selected) {
                    val themeSrc = mElems[i] as Theme
                    val themeNew = Diary.d.create_theme(text)
                    themeSrc.copy_to(themeNew)
                    break
                }
            }
        }
    }
}
