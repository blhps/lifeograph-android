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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class RecyclerViewAdapterElems( private val mItems: List<DiaryElement>,
                                private val mSelectionStatuses: MutableList<Boolean>,
                                var mListener: Listener,
                                private val mHasIcon2: Boolean = false,
                                private val mHasDetailText: Boolean = false ) :
        RecyclerView.Adapter< RecyclerViewAdapterElems.ViewHolder >()
{
    var mSelCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                                 .inflate(R.layout.list_item_element, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if( mSelectionStatuses.isEmpty() )
            mSelectionStatuses.addAll(Collections.nCopies(mItems.size, false))

        holder.mItem = mItems[position]
        holder.mIVIcon.setImageResource(mItems[position]._icon)
        holder.mTVTitle.text = mItems[position]._list_str
        if( mHasDetailText )
            holder.mTVDetails.text = mItems[position]._info_str
        else
            holder.mTVDetails.visibility = View.INVISIBLE

        if( mHasIcon2 && holder.mItem is Entry && ( holder.mItem as Entry ).is_favored )
            holder.mIVIcon2.setImageResource(R.mipmap.ic_favorite)
        else
            holder.mIVIcon2.visibility = View.INVISIBLE
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.mView.isActivated = mSelectionStatuses[holder.adapterPosition]
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun setChecked( position: Int, isChecked: Boolean ) {
        if( isChecked != mSelectionStatuses[position] )
            if( isChecked ) mSelCount++ else mSelCount--

        mSelectionStatuses[position] = isChecked
    }

    fun isChecked(position: Int): Boolean {
        return mSelectionStatuses[position]
    }

    fun hasSelection(): Boolean {
        return mSelCount > 0
    }

    fun clearSelection( layoutman: RecyclerView.LayoutManager ) {
        for( ( i, selected ) in mSelectionStatuses.withIndex() ) {
            if( selected ) {
                val row = layoutman.findViewByPosition( i )
                row?.isActivated = false
                mSelectionStatuses[i] = false
            }
        }

        mSelCount = 0

        notifyDataSetChanged()
    }

    interface Listener {
        fun onElemClick(elem: DiaryElement?)
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()
    }

    class ViewHolder(val mView: View, private val mAdapter: RecyclerViewAdapterElems) :
            RecyclerView.ViewHolder( mView )
    {
        val mIVIcon:    ImageView = mView.findViewById(R.id.icon)
        val mIVIcon2:   ImageView = mView.findViewById(R.id.icon2)
        val mTVTitle:   TextView = mView.findViewById(R.id.title)
        val mTVDetails: TextView = mView.findViewById(R.id.detail)
        var mItem:      DiaryElement? = null

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
        }

        private fun handleLongCLick( v: View ) {
            if( mAdapter.hasSelection() || mAdapter.mListener.enterSelectionMode() ) {
                v.isActivated = !v.isActivated
                mAdapter.setChecked( adapterPosition, v.isActivated )
                mAdapter.mListener.updateActionBarSubtitle()
            }
            if( !mAdapter.hasSelection() )
                mAdapter.mListener.exitSelectionMode()
        }
    }
}
