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
import android.util.Log
import android.view.*
import android.widget.HorizontalScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.sourceforge.lifeograph.Lifeograph.DiaryEditor
import java.util.*

abstract class FragmentListElems : Fragment(), DiaryEditor, RVAdapterElems.Listener,
                                   DialogInquireText.InquireListener
{
    // VARIABLES ===================================================================================
    protected abstract val mLayoutId: Int
    protected abstract val mMenuId: Int
    protected abstract val mName: String

    protected val mElems: MutableList<DiaryElement> = ArrayList()
    protected val mSelectionStatuses: MutableList<Boolean> = ArrayList()
    protected lateinit var mMenu: Menu
    protected lateinit var mAdapter: RVAdapterElems
    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var mFabAdd: FloatingActionButton
    protected lateinit var mToolbar: HorizontalScrollView

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstState: Bundle?): View? {
        Log.d(Lifeograph.TAG, "FragmentChartList.onCreateView()")
        return inflater.inflate(mLayoutId, container, false)
    }

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
        Log.d(Lifeograph.TAG, "FragmentEntryList.onResume()")
        super.onResume()

        ( activity as FragmentHost? )!!.updateDrawerMenu(R.id.nav_charts)
        updateList()

        updateActionBarTitle()
        updateActionBarSubtitle()

        mToolbar.visibility = View.GONE
        //mFabAdd.setTranslationX( Diary.diary.is_in_edit_mode() ? 0 : 150 );
        mFabAdd.visibility = if(Diary.d.is_in_edit_mode) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        if(mAdapter.hasSelection()) mAdapter.clearSelection(
                Objects.requireNonNull(mRecyclerView.layoutManager)!!)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(mMenuId, menu)
        super.onCreateOptionsMenu(menu, inflater)
        mMenu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateMenuVisibilities()
    }

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

    protected open fun updateMenuVisibilities() {
        val flagWritable = Diary.d.is_in_edit_mode
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                Diary.d.can_enter_edit_mode()
        mMenu.findItem(R.id.logout_wo_save).isVisible = flagWritable
    }

    protected open fun updateList() {
    }

    protected open fun createNewElem() {
        // ask for name
        DialogInquireText(requireContext(),
                          R.string.create_chart,
                          Lifeograph.getStr(R.string.new_chart),
                          R.string.create,
                          this).show()
    }

    private fun updateActionBarTitle() {
        Lifeograph.getActionBar().title = Diary.d._title_str
    }

    // INTERFACE METHODS ===========================================================================
    // DiaryEditor INTERFACE METHODS
    override fun enableEditing() {
        updateMenuVisibilities()

        mFabAdd.show()
    }

    override fun handleBack(): Boolean {
        if(mAdapter.hasSelection()) {
            mAdapter.clearSelection(mRecyclerView.layoutManager!!)
            exitSelectionMode()
            return true
        }
        return false
    }

    // RecyclerViewAdapterDiaryElems.Listener INTERFACE METHODS
    override fun onElemClick(elem: DiaryElement?) {
        Lifeograph.showElem(elem!!)
    }

    override fun updateActionBarSubtitle() {
        val selCount = mAdapter.mSelCount
        if( selCount > 0 )
            Lifeograph.getActionBar().subtitle =
                    ( mName + " (" + selCount + " / " + mAdapter.itemCount + ")" )
        else
            Lifeograph.getActionBar().subtitle = mName + " (" + mAdapter.itemCount + ")"
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

    override fun onInquireAction(id: Int, text: String) {
        Lifeograph.showToast("not implemented yet")
    }
    override fun onInquireTextChanged(id: Int, text: String): Boolean {
        return text.isNotEmpty()
    }
}
