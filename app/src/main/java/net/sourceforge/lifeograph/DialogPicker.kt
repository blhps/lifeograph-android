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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

typealias RVBasicList = MutableList<RViewAdapterBasic.Item>

class DialogPicker(context: Context, val mListener: Listener) : Dialog(context){
    // VARIABLES ===================================================================================
    private lateinit var mRecyclerView: RecyclerView
    private val mItems: RVBasicList = ArrayList()
    private val mSelectionStatuses: MutableList<Boolean> = ArrayList()
    private val mAdapter: RViewAdapterBasic =
            RViewAdapterBasic(mItems, mSelectionStatuses,
                              object: RViewAdapterBasic.Listener {
                                  override fun onItemClick(item: RViewAdapterBasic.Item) {
                                      dismiss()
                                      mListener.onItemClick(item)
                                  }
                                  override fun enterSelectionMode(): Boolean { return false }
                                  override fun exitSelectionMode() { }
                                  override fun updateActionBarSubtitle() { }
                              })

    interface Listener {
        fun populateItems(list: RVBasicList)
        fun onItemClick(item: RViewAdapterBasic.Item)
    }

// METHODS =========================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_picker)
        setCancelable(true)
        setTitle(R.string.todo_auto)

        mRecyclerView = findViewById(R.id.item_list)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(context)

        mListener.populateItems(mItems)
    }
}
