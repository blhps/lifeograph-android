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
import android.view.*
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

abstract class FragmentListElems : FragmentDiaryEditor(), RVAdapterElems.Listener,
                                   DialogInquireText.Listener
{
    // VARIABLES ===================================================================================
    protected abstract val mName: String

    protected val          mElems: MutableList<DiaryElement> = ArrayList()
    protected val          mSelectionStatuses: MutableList<Boolean> = ArrayList()
    protected lateinit var mAdapter: RVAdapterElems
    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var mFabAdd: FloatingActionButton
    private lateinit var   mToolbar: HorizontalScrollView
    protected var          mItemCount: Int = 0

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this

        mRecyclerView = view.findViewById(R.id.list_elems)
        mAdapter = RVAdapterElems(mElems, mSelectionStatuses, this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)
        mFabAdd = view.findViewById(R.id.fab_add)
        mFabAdd.setOnClickListener { createNewElem() }
        mToolbar = view.findViewById(R.id.toolbar_elem)
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        super.onResume()

        ( activity as FragmentHost? )!!.updateDrawerMenu(R.id.nav_charts)
        updateList()

        mToolbar.visibility = View.GONE

        Lifeograph.getActionBar().title = Diary.d._title_str
        updateActionBarSubtitle()
    }

    override fun onDestroyView() {
        if(mAdapter.hasSelection()) mAdapter.clearSelection(
                Objects.requireNonNull(mRecyclerView.layoutManager)!!)
        super.onDestroyView()
    }

    @SuppressLint("RestrictedApi")
    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        mToolbar.visibility = View.GONE
        //mFabAdd.setTranslationX( Diary.diary.is_in_edit_mode() ? 0 : 150 );
        mFabAdd.visibility = if(Diary.d.is_in_edit_mode) View.VISIBLE else View.GONE
    }

    protected abstract fun updateList()

    abstract fun createNewElem()

    override fun handleBack(): Boolean {
        if(mAdapter.hasSelection()) {
            mAdapter.clearSelection(mRecyclerView.layoutManager!!)
            exitSelectionMode()
            return true
        }
        return false
    }

    // INTERFACE METHODS ===========================================================================
    // RecyclerViewAdapterDiaryElems.Listener INTERFACE METHODS
    override fun onElemClick(elem: DiaryElement?) {
        Lifeograph.showElem(elem!!)
    }

    override fun updateActionBarSubtitle() {
        val selCount = mAdapter.mSelCount
        Lifeograph.getActionBar().subtitle =
                if( selCount > 0 ) "$mName ($selCount/$mItemCount)"
                else               "$mName ($mItemCount)"
    }

    override fun toggleExpanded(elem: DiaryElement?) { }

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

    override fun hasIcon2(elem: DiaryElement): Boolean {
        return false
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return 0
    }
}
