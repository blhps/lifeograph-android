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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val VIEWTYPE_HEADER = 0
private const val VIEWTYPE_ITEM   = 1

class RVAdapterElems(private val mItems :List<DiaryElement>,
                     private val mSelectionStatuses: MutableList<Boolean>,
                     var mListener: Listener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    var mSelCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if( viewType == VIEWTYPE_HEADER ) {
            ViewHolderHeader(inflater.inflate(R.layout.list_item_header, parent, false))
        }
        else {
            ViewHolder(inflater.inflate(R.layout.list_item_element, parent, false), this)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if( mSelectionStatuses.isEmpty() )
            mSelectionStatuses.addAll(Collections.nCopies(mItems.size, false))

        val elem: DiaryElement = mItems[position]

        if( holder is ViewHolder ) {
            holder.mItem = elem
            holder.mIVIcon.setImageResource(elem._icon)

            holder.mTVTitle.text = elem._list_str
            holder.mTVDetails.text = elem._info_str

            if(mListener.hasIcon2(elem)) {
                holder.mIVIcon2.setImageResource(mListener.getIcon2(elem))
                holder.mIVIcon2.visibility = View.VISIBLE
            }
            else
                holder.mIVIcon2.visibility = View.INVISIBLE

            if(mListener.hasIcon3(elem)) {
                //holder.mIVIcon3.setImageResource(mListener.getIcon3(elem))
                holder.mIVIcon3.visibility = View.VISIBLE
            }
            else
                holder.mIVIcon3.visibility = View.INVISIBLE
        }
        else if(holder is ViewHolderHeader) {
            holder.mTvTitle.text = elem._name
        }
    }

    override fun getItemViewType(pos: Int): Int {
        return if(mItems[pos]._type == DiaryElement.Type.NONE) VIEWTYPE_HEADER else VIEWTYPE_ITEM
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if(holder is ViewHolder)
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
        fun onElemClick(elem: DiaryElement?)
        fun toggleExpanded(elem: DiaryElemTag)
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()

        fun hasIcon2(elem: DiaryElement): Boolean
        fun getIcon2(elem: DiaryElement): Int
        fun hasIcon3(elem: DiaryElement): Boolean
        //fun getIcon3(elem: DiaryElement): Int
    }

    class ViewHolder(val mView: View, private val mAdapter: RVAdapterElems) :
            RecyclerView.ViewHolder(mView)
    {
        val mSpacerL1:  TextView = mView.findViewById(R.id.spacer_L1)
        val mSpacerL2:  TextView = mView.findViewById(R.id.spacer_L2)
        val mIVIcon:    ImageView = mView.findViewById(R.id.icon)
        val mIVIcon2:   ImageView = mView.findViewById(R.id.icon2)
        val mIVIcon3:   ImageView = mView.findViewById(R.id.icon3)
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
    }

    class ViewHolderHeader(mView: View) :
            RecyclerView.ViewHolder(mView) {
        val mTvTitle: TextView = mView.findViewById(R.id.title)
    }
}
