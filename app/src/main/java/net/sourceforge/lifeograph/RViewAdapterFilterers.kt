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

class RVAdapterFilterers(private val mItems: List<Filterer>,
                         private val mSelectionStatuses: MutableList<Boolean>,
                         var mListener: Listener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    var mSelCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when( viewType.toChar() ) {
            Filter.FT_STATUS ->
                VHFiltererStatus(inflater.inflate(R.layout.fragment_filterer_status, parent, false), this)
            Filter.FT_TRASHED ->
                VHFiltererTrashed(inflater.inflate(R.layout.fragment_filterer_trashed, parent, false) , this)
            Filter.FT_IS ->
                VHFiltererIs(inflater.inflate(R.layout.fragment_filterer_is, parent, false), this)
            Filter.FT_HAS_TAG ->
                VHFiltererHasTag(inflater.inflate(R.layout.fragment_filterer_has_tag, parent, false),
                                 this)
            Filter.FT_FAVORITE ->
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

        if( holder is VHFilterer<*> ) {
            holder.setItem( mItems[position] )
            holder.populate()
        }
    }

    override fun getItemViewType(pos: Int): Int {
        val type = when(mItems[pos]) {
            is FiltererContainer.FiltererStatus -> Filter.FT_STATUS
            is FiltererContainer.FiltererFavorite -> Filter.FT_FAVORITE
            is FiltererContainer.FiltererTrashed -> Filter.FT_TRASHED
            is FiltererContainer.FiltererIs -> Filter.FT_IS
            is FiltererContainer.FiltererHasTag -> Filter.FT_HAS_TAG
            else -> Filter.FT_UNSOPPRTED
        }
        return type.code
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

    abstract class VHFilterer<T: Filterer>(mView: View, private val mAdapter: RVAdapterFilterers) :
            RecyclerView.ViewHolder(mView) {

        protected lateinit var mItem: T

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

        @Suppress("UNCHECKED_CAST")
        fun setItem(item: Filterer) {
            mItem = item as T
        }

        abstract fun getType() : Char
        open fun populate() {
            mToggleNot.isChecked = !mItem.f_not
        }
        open fun updateState() {
            mItem.f_not = !mToggleNot.isChecked
        }

        val mToggleNot: ToggleImageButton by lazy {
            mView.findViewById<ToggleImageButton>(R.id.trueFalse).apply {
                setOnClickListener { updateState() }
            }
        }
    }

    class VHFiltererStatus(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererStatus>(mView,
                                                                                                      mAdapter)
    {
        private val mIVNotTodo:     ToggleImageButton = mView.findViewById(R.id.btn_flt_status_not_todo)
        private val mIVopen:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_open)
        private val mIVprogr:       ToggleImageButton = mView.findViewById(R.id.btn_flt_status_progressed)
        private val mIVdone:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_done)
        private val mIVcanceled:    ToggleImageButton = mView.findViewById(R.id.btn_flt_status_canceled)


        override fun getType(): Char { return Filter.FT_STATUS }

        init {
            mToggleNot.visibility = View.GONE
            mIVNotTodo.setOnClickListener { updateState() }
            mIVopen.setOnClickListener { updateState() }
            mIVprogr.setOnClickListener { updateState() }
            mIVdone.setOnClickListener { updateState() }
            mIVcanceled.setOnClickListener { updateState() }
        }

        override fun populate() {
            super.populate()

            val incStts: Int = mItem.included_statuses
            mIVNotTodo.isChecked =  ( incStts and DiaryElement.ES_SHOW_NOT_TODO ) != 0
            mIVopen.isChecked =     ( incStts and DiaryElement.ES_SHOW_TODO ) != 0
            mIVprogr.isChecked =    ( incStts and DiaryElement.ES_SHOW_PROGRESSED ) != 0
            mIVdone.isChecked =     ( incStts and DiaryElement.ES_SHOW_DONE ) != 0
            mIVcanceled.isChecked = ( incStts and DiaryElement.ES_SHOW_CANCELED ) != 0
        }

        override fun updateState() {
            super.updateState()

            var incStts = 0
            if( mIVNotTodo.isChecked)
                incStts = DiaryElement.ES_SHOW_NOT_TODO
            if( mIVopen.isChecked )
                incStts = incStts or DiaryElement.ES_SHOW_TODO
            if( mIVprogr.isChecked )
                incStts = incStts or DiaryElement.ES_SHOW_PROGRESSED
            if( mIVdone.isChecked )
                incStts = incStts or DiaryElement.ES_SHOW_DONE
            if( mIVcanceled.isChecked )
                incStts = incStts or DiaryElement.ES_SHOW_CANCELED

            mItem.included_statuses = incStts
        }
    }

    class VHFiltererFavorite(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererFavorite>(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        override fun getType(): Char { return Filter.FT_FAVORITE }
    }

    class VHFiltererTrashed(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererTrashed>(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        override fun getType(): Char { return Filter.FT_TRASHED }
    }

    class VHFiltererIs(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererIs>(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)
        private val mEntryName: EditText = mView.findViewById(R.id.entry_name)

        override fun getType(): Char { return Filter.FT_IS }

        override fun populate() {
            super.populate()

            if( mItem.id != Diary.DEID_UNSET )
                mEntryName.setText(Diary.main.get_tag_by_id(mItem.id)?._name)
        }
        override fun updateState() {
            super.updateState()

            // TODO: get from an autocompletion system
            if(!mEntryName.text.isEmpty()) {
                val tag = Diary.main.get_tag_by_name(mEntryName.text.toString())
                if(tag != null)
                    mItem.id = tag._id
                else
                    mItem.id = Diary.DEID_UNSET
            }
        }
}

    class VHFiltererHasTag(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererHasTag>(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)
        private val mEntryName: EditText = mView.findViewById(R.id.entry_name)

        override fun getType(): Char { return Filter.FT_HAS_TAG }

        override fun populate() {
            super.populate()

            if( mItem.id != Diary.DEID_UNSET )
                mEntryName.setText(Diary.main.get_tag_by_id(mItem.id)?._name)
        }

        override fun updateState() {
            super.updateState()

            // TODO: get from an autocompletion system
            if(!mEntryName.text.isEmpty()) {
                val tag = Diary.main.get_tag_by_name(mEntryName.text.toString())
                if(tag != null)
                    mItem.id = tag._id
                else
                    mItem.id = Diary.DEID_UNSET
            }
        }
    }

    class VHFiltererUnsupported(mView: View, mAdapter: RVAdapterFilterers) :
        VHFilterer<FiltererContainer.FiltererFavorite>(mView, mAdapter) {
        //val mTvTitle: TextView = mView.findViewById(R.id.title)

        override fun getType(): Char { return Filter.FT_UNSOPPRTED }
    }
}