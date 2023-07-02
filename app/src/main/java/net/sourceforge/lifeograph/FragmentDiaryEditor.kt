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
import android.view.*
import androidx.fragment.app.Fragment

abstract class FragmentDiaryEditor : Fragment() {
    // VARIABLES ===================================================================================
    protected abstract val mLayoutId: Int
    protected abstract val mMenuId: Int

    protected lateinit var mMenu: Menu

    val isMenuInitialized get() = this::mMenu.isInitialized

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(mMenuId > 0)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstState: Bundle?): View? {
        Log.d(Lifeograph.TAG, "FragmentChartList.onCreateView()")
        return inflater.inflate(mLayoutId, container, false)
    }

//    override fun onResume() {
//        super.onResume()
//        if(this::mMenu.isInitialized)
//            updateMenuVisibilities()
//    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if(mMenuId > 0)
            inflater.inflate(mMenuId, menu)
        super.onCreateOptionsMenu(menu, inflater)
        mMenu = menu
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateMenuVisibilities()
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.home -> {
                handleBack()
            }
            R.id.enable_edit -> {
                Lifeograph.enableEditing(this)
                true
            }
            R.id.logout_wo_save -> {
                Lifeograph.logoutWithoutSaving(requireView())
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    open fun updateMenuVisibilities() {
        val flagWritable = Diary.d.is_in_edit_mode
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                Diary.d.can_enter_edit_mode()
        mMenu.findItem(R.id.logout_wo_save).isVisible = flagWritable
    }

    open fun enableEditing() {
        updateMenuVisibilities()
    }

    open fun handleBack(): Boolean {
        return false
    }
}
