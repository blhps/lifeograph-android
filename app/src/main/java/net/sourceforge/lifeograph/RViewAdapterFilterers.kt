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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

private const val VIEWTYPE_F_STATUS     = 0
private const val VIEWTYPE_F_FAVORITE   = 1
private const val VIEWTYPE_F_IS         = 2

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
            VIEWTYPE_F_IS ->
                VHFiltererIs(inflater.inflate(R.layout.fragment_filterer_is, parent, false), this)
            else ->
                VHFiltererFavorite(inflater.inflate(R.layout.fragment_filterer_favorite, parent,
                                                    false), this)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if( mSelectionStatuses.isEmpty() )
            mSelectionStatuses.addAll(Collections.nCopies(mItems.size, false))


        when(holder) {
            is VHFiltererStatus -> {
                holder.mItem = mItems[position] as FiltererContainer.FiltererStatus
                holder.populate()
            }
            is VHFiltererIs -> {
                holder.mItem = mItems[position] as FiltererContainer.FiltererIs
                holder.populate()
            }
        }
    }

    override fun getItemViewType(pos: Int): Int {
        return when(mItems[pos]) {
            is FiltererContainer.FiltererStatus -> VIEWTYPE_F_STATUS
            is FiltererContainer.FiltererFavorite -> VIEWTYPE_F_FAVORITE
            is FiltererContainer.FiltererIs -> VIEWTYPE_F_IS
            else -> -1
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if(holder is RVAdapterElems.ViewHolder)
            holder.mView.isActivated = mSelectionStatuses[holder.bindingAdapterPosition]
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun setChecked(position: Int, isChecked: Boolean) {
        if( isChecked != mSelectionStatuses[position] )
            if( isChecked ) mSelCount++ else mSelCount--

        mSelectionStatuses[position] = isChecked
    }

//    fun isChecked(position: Int): Boolean {
//        return mSelectionStatuses[position]
//    }

    fun hasSelection(): Boolean {
        return mSelCount > 0
    }

    fun clearSelection(layoutman: RecyclerView.LayoutManager) {
        for((i, selected) in mSelectionStatuses.withIndex() ) {
            if( selected ) {
                val row = layoutman.findViewByPosition(i)
                row?.isActivated = false
                mSelectionStatuses[i] = false
            }
        }

        mSelCount = 0

        notifyDataSetChanged()
    }

    interface Listener {
        fun onElemClick(elem: Filterer?)
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()
    }

    interface VHFilterer {
        fun getType() : Int
        fun populate()

        //var m_p2container: FiltererContainer
    }

    class VHFiltererStatus(private val mView: View, private val mAdapter: RVAdapterFilterers) :
            RecyclerView.ViewHolder(mView), VHFilterer
    {
        private val mIVNotTodo:     ToggleImageButton = mView.findViewById(R.id.btn_flt_status_not_todo)
        private val mIVopen:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_open)
        private val mIVprogr:       ToggleImageButton = mView.findViewById(R.id.btn_flt_status_progressed)
        private val mIVdone:        ToggleImageButton = mView.findViewById(R.id.btn_flt_status_done)
        private val mIVcanceled:    ToggleImageButton = mView.findViewById(R.id.btn_flt_status_canceled)

        lateinit var mItem: FiltererContainer.FiltererStatus

        override fun getType(): Int { return VIEWTYPE_F_STATUS }
        init {
            mView.setOnClickListener { v: View ->
                if( mAdapter.mSelCount > 0 )
                    handleLongCLick(v)
                else
                    mAdapter.mListener.onElemClick(mItem)
            }
            mView.setOnLongClickListener { v: View ->
                handleLongCLick(v)
                true
            }
            mIVNotTodo.setOnClickListener { updateState() }
            mIVopen.setOnClickListener { updateState() }
            mIVprogr.setOnClickListener { updateState() }
            mIVdone.setOnClickListener { updateState() }
            mIVcanceled.setOnClickListener { updateState() }
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

        override fun populate() {
            mIVNotTodo.isChecked =  mItem.m_included_statuses and DiaryElement.ES_SHOW_NOT_TODO != 0
            mIVopen.isChecked =     mItem.m_included_statuses and DiaryElement.ES_SHOW_TODO != 0
            mIVprogr.isChecked =    mItem.m_included_statuses and DiaryElement.ES_SHOW_PROGRESSED != 0
            mIVdone.isChecked =     mItem.m_included_statuses and DiaryElement.ES_SHOW_DONE != 0
            mIVcanceled.isChecked = mItem.m_included_statuses and DiaryElement.ES_SHOW_CANCELED != 0
        }

        private fun updateState() {
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

    class VHFiltererFavorite(mView: View, private val mAdapter: RVAdapterFilterers) :
            RecyclerView.ViewHolder(mView), VHFilterer {
        val mTvTitle: TextView = mView.findViewById(R.id.title)

        lateinit var mItem: FiltererContainer.FiltererFavorite

        override fun getType(): Int { return VIEWTYPE_F_FAVORITE }

        override fun populate() {

        }
    }

    class VHFiltererIs(mView: View, private val mAdapter: RVAdapterFilterers) :
        RecyclerView.ViewHolder(mView), VHFilterer {
        val mTvTitle: TextView = mView.findViewById(R.id.title)

        lateinit var mItem: FiltererContainer.FiltererIs

        override fun getType(): Int { return VIEWTYPE_F_IS }

        override fun populate() {

        }
    }
}
