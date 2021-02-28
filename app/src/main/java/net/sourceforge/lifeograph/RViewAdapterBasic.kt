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

class RViewAdapterBasic(private val mItems: List<Item>, var mListener: Listener) :
        RecyclerView.Adapter<RViewAdapterBasic.ViewHolder>() {
    // VARIABLES ===================================================================================
    var mViewHolder: ViewHolder? = null

    class Item(val mName: String, val mId: String, val mIcon: Int) {
        override fun toString(): String {
            return mName
        }
    }

    class ViewHolder(mView: View, listener: Listener) : RecyclerView.ViewHolder(mView) {
        val mImageView: ImageView = mView.findViewById(R.id.icon)
        val mTextView: TextView = mView.findViewById(R.id.title)
        var mItem: Item? = null

        override fun toString(): String {
            return super.toString() + " '" + mTextView.text + "'"
        }

        init {
            mView.setOnClickListener { listener.onItemClick(mItem!!) }
        }
    }

    // METHODS =====================================================================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_basic, parent, false)
        mViewHolder = ViewHolder(view, mListener)
        return mViewHolder!!
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mItems[position]
        holder.mImageView.setImageResource( mItems[position].mIcon )
        holder.mTextView.text = mItems[position].mName
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    interface Listener {
        fun onItemClick(item: Item)
    }
}
