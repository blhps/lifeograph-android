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

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.util.*

class RViewAdapterBasic(private val mItems: List<Item>,
                        private val mSelectionStatuses: MutableList<Boolean>,
                        private var mListener: Listener) :
        RecyclerView.Adapter<RViewAdapterBasic.ViewHolder>() {
    // VARIABLES ===================================================================================
    var mSelCount: Int = 0
    private var mViewHolder: ViewHolder? = null

    // METHODS =====================================================================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_basic,
                                                               parent, false)
        mViewHolder = ViewHolder(view, this)
        return mViewHolder!!
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if( mSelectionStatuses.isEmpty() )
            mSelectionStatuses.addAll(Collections.nCopies(mItems.size, false))

        holder.mItem = mItems[position]
        holder.mImageView.setImageResource( mItems[position].mIcon )
        holder.mTextView.text = mItems[position].mName
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
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
        fun onItemClick(item: Item)
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()
    }

    // CLASSES =====================================================================================
    class Item(val mName: String, val mId: String, val mIcon: Int, val mIdNum: Int = 0) {
        override fun toString(): String {
            return mName
        }
    }

    class ViewHolder(val mView: View, private val mAdapter: RViewAdapterBasic)
        : RecyclerView.ViewHolder(mView) {
        val mImageView: ImageView = mView.findViewById(R.id.icon)
        val mTextView: TextView = mView.findViewById(R.id.title)
        var mItem: Item? = null

        init {
            mView.setOnClickListener { v: View ->
                if( mAdapter.mSelCount > 0 )
                    handleLongCLick(v)
                else
                    mAdapter.mListener.onItemClick(mItem!!)
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

        override fun toString(): String {
            return super.toString() + " '" + mTextView.text + "'"
        }
    }
}
