/* *********************************************************************************

    Copyright (C) 2025 Ahmet Öztürk (aoz_2@yahoo.com)

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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

private const val VIEWTYPE_F_STATUS         = 0
private const val VIEWTYPE_F_FAVORITE       = 1
private const val VIEWTYPE_F_TRASHED        = 2
private const val VIEWTYPE_F_IS             = 3
private const val VIEWTYPE_F_HAS_TAG        = 4
//private const val VIEWTYPE_F_THEME          = 5
//private const val VIEWTYPE_F_BTWN_DATES     = 6
//private const val VIEWTYPE_F_BTWN_ENTRIES   = 7
//private const val VIEWTYPE_F_COMPLETION     = 8
//private const val VIEWTYPE_F_CONTAINER      = 9

class RVAdapterFilterers(private val mItems: List<Filterer>,
                         private val mSelectionStatuses: MutableList<Boolean>,
                         var mListener: Listener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    var mSelCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when( viewType ) {
            VIEWTYPE_F_STATUS ->
                VHFiltererStatus(inflater.inflate(R.layout.fragment_filterer_status, parent, false), this)
            VIEWTYPE_F_TRASHED ->
                VHFiltererTrashed(inflater.inflate(R.layout.fragment_filterer_trashed, parent, false) , this)
            VIEWTYPE_F_IS ->
                VHFiltererIs(inflater.inflate(R.layout.fragment_filterer_is, parent, false), this)
            VIEWTYPE_F_HAS_TAG ->
                VHFiltererHasTag(inflater.inflate(R.layout.fragment_filterer_has_tag, parent, false),
                                 this)
            VIEWTYPE_F_FAVORITE ->
                VHFiltererFavorite(inflater.inflate(R.layout.fragment_filterer_favorite, parent,
                                   false), this)
            else ->
                VHFiltererUnsupported(inflater.inflate(R.layout.fragment_filterer_unsopported,
                                                      parent,
                                   false), this)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if( mSelectionStatuses.isEmpty() )
            mSelectionStatuses.addAll(Collections.nCopies(mItems.size, false))

        (holder as VHFilterer).setItem(mItems[position] )
        holder.populate()
    }

    override fun getItemViewType(pos: Int): Int {
        return when(mItems[pos]) {
            is FiltererContainer.FiltererStatus -> VIEWTYPE_F_STATUS
            is FiltererContainer.FiltererFavorite -> VIEWTYPE_F_FAVORITE
            is FiltererContainer.FiltererTrashed -> VIEWTYPE_F_TRASHED
            is FiltererContainer.FiltererIs -> VIEWTYPE_F_IS
            is FiltererContainer.FiltererHasTag -> VIEWTYPE_F_HAS_TAG
            else -> -1
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if(holder is RVAdapterElems.ViewHolder)
            holder.mView.isActivated = mSelectionStatuses[holder.bindingAdapterPosition]
    }

    override fun getItemCount(): Int { return mItems.size }

    fun setChecked(position: Int, isChecked: Boolean) {
        if( isChecked != mSelectionStatuses[position] )
            if( isChecked ) mSelCount++ else mSelCount--

        mSelectionStatuses[position] = isChecked
    }

//    fun isChecked(position: Int): Boolean {
//        return mSelectionStatuses[position]
//    }

    fun hasSelection(): Boolean { return mSelCount > 0 }

//    fun clearSelection(layoutman: RecyclerView.LayoutManager) {
//        for((i, selected) in mSelectionStatuses.withIndex() ) {
//            if( selected ) {
//                val row = layoutman.findViewByPosition(i)
//                row?.isActivated = false
//                mSelectionStatuses[i] = false
//            }
//        }
//
//        mSelCount = 0
//
//        notifyDataSetChanged()
//    }

    interface Listener {
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()
    }

    abstract class VHFilterer(mView: View, private val mAdapter: RVAdapterFilterers) :
            RecyclerView.ViewHolder(mView) {

        init {
            mView.setOnClickListener { v: View ->
                if(mAdapter.mSelCount > 0)
                    handleLongCLick(v)
            }
            mView.setOnLongClickListener { v: View ->
                handleLongCLick(v)
                true
            }
        }

        private fun handleLongCLick(v: View) {
            if( mAdapter.hasSelection() || mAdapter.mListener.enterSelectionMode() ) {
                v.isActivated = !v.isActivated
                mAdapter.setChecked(bindingAdapterPosition, v.isActivated)

                if( !mAdapter.hasSelection() )
                    mAdapter.mListener.exitSelectionMode()
                else
                    mAdapter.mListener.updateActionBarSubtitle()
            }
        }

        abstract fun setItem(item: Filterer)
        abstract fun getType() : Int
        abstract fun populate()
        abstract fun updateState()

        val mToggleNot: ToggleImageButton by lazy {
            mView.findViewById<ToggleImageButton>(R.id.trueFalse).apply {
                setOnClickListener { updateState() }
            }
        }
    }

    class VHFiltererStatus(mView: View, mAdapter: RVAdapterFilterers) : VHFilterer(mView, mAdapter)
    {
        private val mIVNotTodo:     ToggleImageButton = mView.findViewById(R.id.btn_flt_status_not_todo)
        private val mIVopen:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_open)
        private val mIVprogr:       ToggleImageButton = mView.findViewById(R.id.btn_flt_status_progressed)
        private val mIVdone:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_done)
        private val mIVcanceled:    ToggleImageButton = mView.findViewById(R.id.btn_flt_status_canceled)

        private lateinit var mItem: FiltererContainer.FiltererStatus

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererStatus }

        override fun getType(): Int { return VIEWTYPE_F_STATUS }

        init {
            mToggleNot.visibility = View.GONE
            mIVNotTodo.setOnClickListener { updateState() }
            mIVopen.setOnClickListener { updateState() }
            mIVprogr.setOnClickListener { updateState() }
            mIVdone.setOnClickListener { updateState() }
            mIVcanceled.setOnClickListener { updateState() }
        }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has

            mIVNotTodo.isChecked =  mItem.m_included_statuses and DiaryElement.ES_SHOW_NOT_TODO != 0
            mIVopen.isChecked =     mItem.m_included_statuses and DiaryElement.ES_SHOW_TODO != 0
            mIVprogr.isChecked =    mItem.m_included_statuses and DiaryElement.ES_SHOW_PROGRESSED != 0
            mIVdone.isChecked =     mItem.m_included_statuses and DiaryElement.ES_SHOW_DONE != 0
            mIVcanceled.isChecked = mItem.m_included_statuses and DiaryElement.ES_SHOW_CANCELED != 0
        }

        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked

            mItem.m_included_statuses = 0
            if( mIVNotTodo.isChecked)
                mItem.m_included_statuses = DiaryElement.ES_SHOW_NOT_TODO
            if( mIVopen.isChecked )
                mItem.m_included_statuses = mItem.m_included_statuses or DiaryElement.ES_SHOW_TODO
            if( mIVprogr.isChecked )
                mItem.m_included_statuses = mItem.m_included_statuses or DiaryElement
                    .ES_SHOW_PROGRESSED
            if( mIVdone.isChecked )
                mItem.m_included_statuses = mItem.m_included_statuses or DiaryElement.ES_SHOW_DONE
            if( mIVcanceled.isChecked )
                mItem.m_included_statuses = mItem.m_included_statuses or DiaryElement
                    .ES_SHOW_CANCELED
        }

    }

    class VHFiltererFavorite(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        private lateinit var mItem: FiltererContainer.FiltererFavorite

        init {
            //mToggleNot.setOnClickListener { updateState() }
        }

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererFavorite }

        override fun getType(): Int { return VIEWTYPE_F_FAVORITE }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has
        }
        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked
        }
    }

    class VHFiltererTrashed(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        private lateinit var mItem: FiltererContainer.FiltererTrashed

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererTrashed }

        override fun getType(): Int { return VIEWTYPE_F_TRASHED }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has
        }
        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked
        }
    }

    class VHFiltererIs(mView: View, mAdapter: RVAdapterFilterers) : VHFilterer(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)
        private val mEntryName: EditText = mView.findViewById(R.id.entry_name)

        private lateinit var mItem: FiltererContainer.FiltererIs

        init {
            // not supported for now:
            mToggleNot.visibility = View.GONE
        }

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererIs }

        override fun getType(): Int { return VIEWTYPE_F_IS }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has
            if( mItem.m_id != 404 /*TODO: DiaryElement.DEID_UNSET*/ )
                mEntryName.setText(Diary.getMain().get_entry_by_id(mItem.m_id)._name)
        }
        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked
            // TODO: get from an autocompletion system
            if(!mEntryName.text.isEmpty()) {
                val entry = Diary.getMain().get_entry_by_name(mEntryName.text.toString())
                if(entry != null)
                    mItem.m_id = entry._id
            }
        }
}

    class VHFiltererHasTag(mView: View, mAdapter: RVAdapterFilterers) : VHFilterer(mView,
                                                                                   mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)
        private val mEntryName: EditText = mView.findViewById(R.id.entry_name)

        private lateinit var mItem: FiltererContainer.FiltererHasTag

        init {
            // not supported for now:
            mToggleNot.visibility = View.GONE
        }

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererHasTag }

        override fun getType(): Int { return VIEWTYPE_F_HAS_TAG }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has
            if( mItem.m_tag != null )
                mEntryName.setText(mItem.m_tag._name)
        }

        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked
            // TODO: get from an autocompletion system
            if(!mEntryName.text.isEmpty()) {
                val entry = Diary.getMain().get_entry_by_name(mEntryName.text.toString())
                if(entry != null)
                    mItem.m_tag = entry
            }
        }
    }

    class VHFiltererUnsupported(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        private lateinit var mItem: FiltererContainer.FiltererFavorite

        override fun setItem(item: Filterer) { mItem = item as FiltererContainer.FiltererFavorite }

        override fun getType(): Int { return VIEWTYPE_F_FAVORITE }

        override fun populate() {
            mToggleNot.isChecked = !mItem.m_f_has
        }
        override fun updateState() {
            mItem.m_f_has = !mToggleNot.isChecked
        }
    }
}