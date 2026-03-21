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
import android.widget.ImageButton
import androidx.lifecycle.Lifecycle
import net.sourceforge.lifeograph.DialogPassword.DPAction
import net.sourceforge.lifeograph.helpers.Result
import java.io.File
import java.util.*

class FragmentListEntries : FragmentListElems(), DialogPassword.Listener, RVAdapterElems.Listener {
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_entries
    override val mMenuId: Int   = R.menu.menu_list_entries
    override val mName: String  = Lifeograph.getStr( R.string.entries )

//    companion object {
//        val compareElemsByDate = CompareElemsByDate()
//    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mMenuId > 0) {
            requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        var button = view.findViewById<ImageButton>(R.id.btn_toggle_favorite)
        button.setOnClickListener { toggleSelFavoredness() }
        button = view.findViewById(R.id.btn_todo_status)
        button.setOnClickListener { showStatusDlg() }
        button = view.findViewById(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
        button = view.findViewById(R.id.dismiss)
        button.setOnClickListener { trashSel() }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val dm = Diary.getMain()
        return when(menuItem.itemId) {
            R.id.search_text -> {
                Lifeograph.mActivityMain.navigateTo(R.id.nav_search)
                true
            }
            R.id.add_password -> {
                DialogPassword(requireContext(),
                        dm,
                        DPAction.DPA_ADD,
                        this).show()
                true
            }
            R.id.change_password -> {
                DialogPassword(requireContext(),
                        dm,
                        DPAction.DPA_AUTHENTICATE,
                        this).show()
                true
            }
            R.id.export_plain_text -> {
                val file = File(dm._uri)
                val dirBackups = File(file.parent!! + "/backups")
                if(dirBackups.exists() || dirBackups.mkdirs()) {
                    val fileText = File(dirBackups, file.name + ".txt")
                    if(dm.write_txt(fileText.path, null) == Result.SUCCESS) {
                        Lifeograph.showToast(R.string.text_export_success)
                        return true
                    }
                }
                Lifeograph.showToast(R.string.text_export_fail)
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val dm = Diary.getMain()
        val flagWritable = dm.is_in_edit_mode
        val flagEncrypted = dm.is_encrypted
        mMenu.findItem(R.id.export_plain_text).isVisible = !dm.is_virtual
        mMenu.findItem(R.id.add_password).isVisible = flagWritable && !flagEncrypted
        mMenu.findItem(R.id.change_password).isVisible = flagWritable && flagEncrypted
    }

    override fun updateList() {
        fun addDescendantsToList(entry1st: Entry) {
            var entry: Entry? = entry1st
            while (entry != null) {
                if(!entry.is_filtered_out) {
                    mElems.add(entry)
                    if(entry.is_expanded && entry.has_children())
                        addDescendantsToList(entry.get_child_1st()!!)
                }
                entry = entry.get_next()
            }
        }

        Log.d(Lifeograph.TAG, "FragmentElemList.updateList()::ALL ENTRIES")
        mElems.clear()

        Diary.getMain().get_entry_1st()?.let { addDescendantsToList(it) }

//        mElems.add(HeaderElem( R.string.numbered_entries, Date.DATE_MAX ) )
//        mElems.add(HeaderElem(R.string.free_entries, Date.NUMBERED_MIN))
//        mElems.add(HeaderElem(R.string.dated_entries,
//                              Date.make(Date.YEAR_MAX + 1, 12, 31, 0)))
        //}

        handleElemNumberChanged()
        mItemCount = mElems.size - 3
    }

    override fun createNewElem() {
        DialogPicker(requireContext(), object: DialogPicker.Listener{
            override fun onItemClick(item: RViewAdapterBasic.Item) {
                Lifeograph.goToToday()
//                when(item.mId) {
//                    "T" -> {
//                        Lifeograph.goToToday()
//                    }
//                    "F" -> {
//                        Lifeograph.addEntry(
//                                Diary.d.get_available_order_1st(true), "")
//                    }
//                    else -> {
//                        Lifeograph.addEntry(
//                                Diary.d.get_available_order_1st(false), "")
//                    }
//                }
                handleElemNumberChanged()
            }

            override fun populateItems(list: RVBasicList) {
                list.clear()

                list.add(RViewAdapterBasic.Item(
                        Lifeograph.getStr(R.string.add_today), "T",
                        R.drawable.ic_entry))
                list.add(RViewAdapterBasic.Item(
                        Lifeograph.getStr(R.string.add_free), "F",
                        R.drawable.ic_entry))
                list.add(RViewAdapterBasic.Item(
                        Lifeograph.getStr(R.string.add_numbered), "N",
                        R.drawable.ic_entry))
            }
        }).show()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun toggleExpanded(tag: DiaryElemTag) {
        tag.is_expanded = !tag.is_expanded
        updateList()
        mAdapter.notifyDataSetChanged()
    }

    override fun hasIcon2(elem: DiaryElement): Boolean {
        return if( elem is Entry ) elem.is_favorite else false
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return R.drawable.ic_favorite
    }
    override fun hasIcon3(elem: DiaryElement): Boolean {
        return if( elem is Entry ) elem.is_trashed else false
    }

    private fun toggleSelFavoredness() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                entry.toggle_favorite()
                mAdapter.notifyItemChanged( i )
            }
        }
    }

    private fun showStatusDlg() {
        DialogPicker(requireContext(), object: DialogPicker.Listener{
            override fun onItemClick(item: RViewAdapterBasic.Item) {
                Log.d(Lifeograph.TAG, "TOFO item Clicked: " + item.mName)
                setSelTodoStatus(item.mId)
            }

            override fun populateItems(list: RVBasicList) {
                list.clear()

                list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_auto),
                                                "A",
                                                R.drawable.ic_todo_auto))
                list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_open),
                                                " ",
                                                R.drawable.ic_todo_open))
                list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_progressed),
                                                "~",
                                                R.drawable.ic_todo_progressed))
                list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_done),
                                                "+",
                                                R.drawable.ic_todo_done))
                list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_canceled),
                                                "x",
                                                R.drawable.ic_todo_canceled))
            }
        }).show()
    }

    private fun setSelTodoStatus(status: String) {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                entry._todo_status = when(status) {
                    "A" -> DiaryElement.ES_NOT_TODO
                    " " -> DiaryElement.ES_TODO
                    "~" -> DiaryElement.ES_PROGRESSED
                    "+" -> DiaryElement.ES_DONE
                    else -> DiaryElement.ES_CANCELED
                }

                mAdapter.notifyItemChanged( i )
            }
        }
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                DialogInquireText(requireContext(),
                                  R.string.duplicate_entry,
                                  entry._name,
                                  R.string.create,
                                  this).show()
                mAdapter.notifyItemChanged( i )
                break
            }
        }
    }

    private fun trashSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                entry.is_trashed = !entry.is_trashed
                mAdapter.notifyItemChanged( i )
                break
            }
        }
    }

    fun handleElemNumberChanged() {
        mSelectionStatuses.clear()
        mSelectionStatuses.addAll(Collections.nCopies(mElems.size, false))
    }

    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.duplicate_entry) {
            for((i, selected) in mSelectionStatuses.withIndex()) {
                if(selected) {
                    val entry = mElems[i] as Entry
                    Lifeograph.duplicateEntry(entry)
                    mAdapter.notifyItemChanged( i )
                    break
                }
            }
        }
    }

    // DialogPassword INTERFACE METHODS ============================================================
    override fun onDPAction(action: DPAction) {
        when(action) {
            DPAction.DPA_AUTHENTICATE -> DialogPassword(requireContext(),
                                                        Diary.getMain(),
                                                        DPAction.DPA_ADD,
                                                        this).show()
            DPAction.DPAR_AUTH_FAILED -> Lifeograph.showToast(R.string.wrong_password)
            else -> Log.d(Lifeograph.TAG, "Unhandled DPAction")
        }
    }

    // RecyclerViewAdapterElems.Listener INTERFACE METHODS =========================================

    // COMPARATOR ==================================================================================
//    class CompareElemsByDate : Comparator<DiaryElemTag> {
//        override fun compare(elemL: DiaryElemTag, elemR: DiaryElemTag): Int {
//            val direction = 1
//            return when {
//                elemL._date > elemR._date -> -direction
//                elemL._date < elemR._date -> direction
//                else -> 0
//            }
//        }
//    }

    // HEADER PSEUDO ELEMENT CLASS =================================================================
//    internal class HeaderElem(nameRsc: Int, private val mDate: Long) :
//            DiaryElemTag(0) {
//        override fun get_type(): Type {
//            return Type.NONE
//        }
//    }
}
