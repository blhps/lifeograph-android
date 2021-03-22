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
import android.widget.ImageButton
import net.sourceforge.lifeograph.DialogPassword.DPAction
import net.sourceforge.lifeograph.helpers.Result
import java.util.*

class FragmentListEntries : FragmentListElems(), DialogPassword.Listener,
                            RVAdapterElems.Listener {
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_list_entries
    override val mMenuId: Int   = R.menu.menu_list_entries
    override val mName: String  = Lifeograph.getStr( R.string.entries )

    companion object {
        val compareElemsByDate = CompareElemsByDate()
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var button = view.findViewById<ImageButton>(R.id.btn_toggle_favorite)
        button.setOnClickListener { toggleSelFavoredness() }
        button = view.findViewById(R.id.btn_todo_status)
        button.setOnClickListener { showStatusDlg() }
        button = view.findViewById(R.id.duplicate)
        button.setOnClickListener { duplicateSel() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.search_text -> {
                Lifeograph.mActivityMain.navigateTo(R.id.nav_search)
                true
            }
            R.id.add_password -> {
                DialogPassword(requireContext(),
                        Diary.d,
                        DPAction.DPA_ADD,
                        this).show()
                true
            }
            R.id.change_password -> {
                DialogPassword(requireContext(),
                        Diary.d,
                        DPAction.DPA_AUTHENTICATE,
                        this).show()
                true
            }
            R.id.export_plain_text -> {
                if(Diary.d.write_txt() == Result.SUCCESS)
                    Lifeograph.showToast(R.string.text_export_success)
                else
                    Lifeograph.showToast(R.string.text_export_fail)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // This callback will only be called when MyFragment is at least Started.
//        OnBackPressedCallback callback = new OnBackPressedCallback( true /* enabled by default */) {
//            @Override
//            public void handleOnBackPressed() {
//                // Handle the back button event
//                Log.d( Lifeograph.TAG, "CALLBACK PRESSED HANDLER!!" );
//                handleBack();
//            }
//        };
//        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val flagWritable = Diary.d.is_in_edit_mode
        val flagEncrypted = Diary.d.is_encrypted
        mMenu.findItem(R.id.export_plain_text).isVisible = !Diary.d.is_virtual
        mMenu.findItem(R.id.add_password).isVisible = flagWritable && !flagEncrypted
        mMenu.findItem(R.id.change_password).isVisible = flagWritable && flagEncrypted
    }

    override fun updateList() {
        fun addChapterCategoryToList(ctg: Chapter.Category) {
            for(chapter in ctg.mMap.values) {
                mElems.add(chapter)
                chapter.mHasChildren = !chapter.mEntries.isEmpty()
                if(!chapter._expanded) continue
                for(entry in chapter.mEntries) {
                    if(!entry._filtered_out) {
                        mElems.add(entry)
                    }
                }
            }
        }

        Log.d(Lifeograph.TAG, "FragmentElemList.updateList()::ALL ENTRIES")
        val firstChapterDate = Diary.d.m_p2chapter_ctg_cur._date_t
        mElems.clear()

        //if( ( Diary.diary.m_sorting_criteria & Diary.SoCr_FILTER_CRTR ) == Diary.SoCr_DATE ) {
        addChapterCategoryToList(Diary.d.m_p2chapter_ctg_cur)
        var entryPrev: Entry? = null
        var entryPrevUpdated = false
        for(entry in Diary.d.m_entries.descendingMap().values) {
            val isDescendant = entryPrev != null &&
                    Date.is_descendant_of(entry._date_t, entryPrev._date_t)
            if(entryPrevUpdated) entryPrev!!.mHasChildren = isDescendant
            entryPrevUpdated = false
            if(isDescendant && !entryPrev!!._expanded) continue

            // ordinals & orphans
            if(!entry._filtered_out &&
                (entry.is_ordinal || entry._date_t < firstChapterDate)) {
                mElems.add(entry)
            }
            // other entries were taken care of in add_chapter_category_to_list()
            entryPrev = entry
            entryPrevUpdated = true
        }

        mElems.add(HeaderElem( R.string.numbered_entries, Date.DATE_MAX ) );
        mElems.add(HeaderElem(R.string.free_entries, Date.NUMBERED_MIN))
        mElems.add(HeaderElem(R.string.dated_entries,
                              Date.make(Date.YEAR_MAX + 1, 12, 31, 0)))
        //}
        Collections.sort(mElems, compareElemsByDate)

        handleElemNumberChanged()
        mItemCount = mElems.size - 3
    }

    override fun createNewElem() {
        DialogPicker(requireContext(), object: DialogPicker.Listener{
            override fun onItemClick(item: RViewAdapterBasic.Item) {
                when(item.mId) {
                    "T" -> {
                        Lifeograph.goToToday()
                    }
                    "F" -> {
                        Lifeograph.addEntry(
                                Diary.d.get_available_order_1st(true), "")
                    }
                    else -> {
                        Lifeograph.addEntry(
                                Diary.d.get_available_order_1st(false), "")
                    }
                }
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

    override fun toggleExpanded(elem: DiaryElement?) {
        if(BuildConfig.DEBUG && elem == null) {
            error("Assertion failed")
        }
        elem!!._expanded = !elem._expanded
        updateList()
        mAdapter.notifyDataSetChanged()
    }

    override fun hasIcon2(elem: DiaryElement): Boolean {
        return if( elem is Entry ) elem.is_favored else false
    }
    override fun getIcon2(elem: DiaryElement): Int {
        return R.drawable.ic_favorite
    }

    private fun toggleSelFavoredness() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                entry.toggle_favored()
            }
        }
        mAdapter.notifyDataSetChanged()
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
            }
        }
        mAdapter.notifyDataSetChanged()
    }

    private fun duplicateSel() {
        for((i, selected) in mSelectionStatuses.withIndex()) {
            if(selected) {
                val entry = mElems[i] as Entry
                DialogInquireText(requireContext(),
                                  R.string.duplicate_entry,
                                  entry.m_name,
                                  R.string.create,
                                  this).show()
                break
            }
        }
        mAdapter.notifyDataSetChanged()
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
                    Lifeograph.duplicateEntry(entry, text)
                    break
                }
            }
            mAdapter.notifyDataSetChanged()
        }
    }

    // DialogPassword INTERFACE METHODS ============================================================
    override fun onDPAction(action: DPAction) {
        when(action) {
            DPAction.DPA_AUTHENTICATE -> DialogPassword(requireContext(),
                                                        Diary.d,
                                                        DPAction.DPA_ADD,
                                                        this).show()
            DPAction.DPAR_AUTH_FAILED -> Lifeograph.showToast(R.string.wrong_password)
            else -> Log.d(Lifeograph.TAG, "Unhandled DPAction")
        }
    }

    // RecyclerViewAdapterElems.Listener INTERFACE METHODS =========================================

    // COMPARATOR ==================================================================================
    class CompareElemsByDate : Comparator<DiaryElement> {
        override fun compare(elem_l: DiaryElement, elem_r: DiaryElement): Int {
            // SORT BY NAME
//            return if(elem_l._date_t == Date.NOT_APPLICABLE) {
//                0
//            }
//            else {
            val sc = Diary.d.m_sorting_criteria
            var direction = 1
            if(Date.is_same_kind(elem_l._date_t, elem_r._date_t)) {
                when {
                    Date.is_descendant_of(elem_l._date_t, elem_r._date_t) -> return 1
                    Date.is_descendant_of(elem_r._date_t, elem_l._date_t) -> return -1
                    else -> direction = when {
                        elem_l._date.is_ordinal -> {
                            if(sc and Diary.SoCr_FILTER_DIR == Diary.SoCr_ASCENDING) -1
                            else 1
                        }
                        sc and Diary.SoCr_FILTER_DIR_T == Diary.SoCr_ASCENDING_T -> -1
                        else -> 1
                    }
                }
            }
            return when {
                elem_l._date_t > elem_r._date_t -> -direction
                elem_l._date_t < elem_r._date_t -> direction
                else -> 0
            }
            //}
        }
    }

    // HEADER PSEUDO ELEMENT CLASS =================================================================
    internal class HeaderElem(nameRsc: Int, private val mDate: Long) :
            DiaryElement(null, Lifeograph.getStr(nameRsc), ES_VOID) {
        override fun get_type(): Type {
            return Type.NONE
        }

        override fun get_date_t(): Long {
            return mDate
        }
    }
}
