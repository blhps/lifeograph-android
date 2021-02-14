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

class RecyclerViewAdapterElems(private val mItems: List<DiaryElement>,
                               private val mSelectionStatuses: MutableList<Boolean>,
                               var mListener: Listener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    var mSelCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if( viewType == VIEWTYPE_HEADER ) {
            ViewHolderHeader(inflater.inflate(R.layout.list_item_header, parent, false), this)
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

            when(elem._date._level) {
                2 -> {
                    holder.mSpacerL1.visibility = View.VISIBLE
                    holder.mSpacerL2.visibility = View.GONE
                }
                3 -> {
                    holder.mSpacerL1.visibility = View.VISIBLE
                    holder.mSpacerL2.visibility = View.VISIBLE
                }
                4 -> { // case for temporal elements
                    if(elem._date._order_3rd == 0) // chapters
                        holder.mSpacerL1.visibility = View.GONE
                    else
                        holder.mSpacerL1.visibility = View.VISIBLE
                    holder.mSpacerL2.visibility = View.GONE
                }
                else -> {
                    holder.mSpacerL1.visibility = View.GONE
                    holder.mSpacerL2.visibility = View.GONE
                }
            }

            holder.mTVTitle.text = elem._list_str
            holder.mTVDetails.text = elem._info_str

            if(mListener.hasIcon2(elem)) {
                holder.mIVIcon2.setImageResource(mListener.getIcon2(elem))
                holder.mIVIcon2.visibility = View.VISIBLE
            }
            else
                holder.mIVIcon2.visibility = View.INVISIBLE

            if(elem.mHasChildren) {
                holder.mExpander.visibility = View.VISIBLE
                if(elem._expanded)
                    holder.mExpander.setImageResource(R.drawable.ic_expanded)
                else
                    holder.mExpander.setImageResource(R.drawable.ic_collapsed)
            }
            else
                holder.mExpander.visibility = View.GONE
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
            holder.mView.isActivated = mSelectionStatuses[holder.adapterPosition]
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun setChecked(position: Int, isChecked: Boolean) {
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
        fun toggleExpanded(elem: DiaryElement?)
        fun updateActionBarSubtitle()
        fun enterSelectionMode(): Boolean
        fun exitSelectionMode()

        fun hasIcon2(elem: DiaryElement): Boolean
        fun getIcon2(elem: DiaryElement): Int
    }

    class ViewHolder(val mView: View, private val mAdapter: RecyclerViewAdapterElems) :
            RecyclerView.ViewHolder(mView)
    {
        val mSpacerL1:  TextView = mView.findViewById(R.id.spacer_L1)
        val mSpacerL2:  TextView = mView.findViewById(R.id.spacer_L2)
        val mIVIcon:    ImageView = mView.findViewById(R.id.icon)
        val mIVIcon2:   ImageView = mView.findViewById(R.id.icon2)
        val mTVTitle:   TextView = mView.findViewById(R.id.title)
        val mTVDetails: TextView = mView.findViewById(R.id.detail)
        val mExpander:  ImageButton = mView.findViewById(R.id.icon_collapse)
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
            mExpander.setOnClickListener {
                mAdapter.mListener.toggleExpanded(mItem)
            }
        }

        private fun handleLongCLick(v: View) {
            if( mAdapter.hasSelection() || mAdapter.mListener.enterSelectionMode() ) {
                v.isActivated = !v.isActivated
                mAdapter.setChecked(adapterPosition, v.isActivated)

                if( !mAdapter.hasSelection() )
                    mAdapter.mListener.exitSelectionMode()
                else
                    mAdapter.mListener.updateActionBarSubtitle()
            }
        }
    }

    class ViewHolderHeader(val mView: View, private val mAdapter: RecyclerViewAdapterElems) :
            RecyclerView.ViewHolder(mView) {
        val mTvTitle: TextView = mView.findViewById(R.id.title)
    }
}
