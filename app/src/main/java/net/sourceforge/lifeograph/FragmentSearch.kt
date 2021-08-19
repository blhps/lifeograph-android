/***************************************************************************************************
 Copyright (C) 2021. Ahmet Öztürk (aoz_2@yahoo.com)

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
 **************************************************************************************************/

package net.sourceforge.lifeograph

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class FragmentSearch : FragmentDiaryEditor(), RViewAdapterBasic.Listener {
    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_search
    override val mMenuId: Int   = 0

    private val          mElems: MutableList<RViewAdapterBasic.Item> = ArrayList()
    private val          mSelectionStatuses: MutableList<Boolean> = ArrayList()
    private lateinit var mAdapter: RViewAdapterBasic
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mEditText: EditText
    private lateinit var mButtonSearchTextClear: Button
    private lateinit var mButtonOnlyInFiltered: ToggleImageButton

    private var          mMatchCount = 0

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this

        mRecyclerView = view.findViewById(R.id.list_matches)
        mEditText = view.findViewById(R.id.search_text)
        mButtonSearchTextClear = view.findViewById(R.id.buttonClearText)
        mButtonOnlyInFiltered = view.findViewById(R.id.search_in_filtered_only)

        mAdapter = RViewAdapterBasic(mElems, mSelectionStatuses, this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)

        if(Diary.d.is_search_active) {
            mEditText.setText(Diary.d._search_text)
            mButtonSearchTextClear.visibility = View.VISIBLE
        }

        mEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                handleSearchTextChanged(s.toString())
            }
        })

        mButtonSearchTextClear.setOnClickListener { mEditText.setText("") }
    }

    override fun onResume() {
        super.onResume()

        updateSubtitle()
        updateList()
    }

    private fun handleSearchTextChanged(text: String) {
        val searchStr = text.toLowerCase(Locale.ROOT)

        mMatchCount = Diary.d.set_search_text(searchStr, mButtonOnlyInFiltered.isChecked)

        mButtonSearchTextClear.visibility = if(text.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        updateSubtitle()
        updateList()
    }

    private fun updateList() {
        val matches = Diary.d.m_matches

        mElems.clear()

        if( matches != null && matches.isNotEmpty() ) {
            var prevPara: Paragraph? = null

            for( (i, match) in matches.withIndex() ) {
                if( i > 200 ) break
                if( match.para == prevPara ) continue
                mElems.add(RViewAdapterBasic.Item(match.para.m_text, "",
                                                  match.para.m_host._icon,
                                                  match.para.m_host.m_id))
                prevPara = match.para
            }
        }

        mAdapter.notifyDataSetChanged()
    }

    private fun updateSubtitle() {
        Lifeograph.getActionBar().subtitle =
                if(mMatchCount == 0 )
                    Lifeograph.getStr(R.string.no_matches)
                else
                    "%s (%d)".format(Lifeograph.getStr(R.string.matches), mMatchCount)
    }

    override fun onItemClick(item: RViewAdapterBasic.Item) {
        Lifeograph.showElem(Diary.d.get_element(item.mIdNum))
    }

    override fun enterSelectionMode(): Boolean = false
    override fun exitSelectionMode() {}
    override fun updateActionBarSubtitle() {}
}
