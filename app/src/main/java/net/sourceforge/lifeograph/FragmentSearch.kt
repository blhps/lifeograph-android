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
    private var mFRestartSearch = false

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRecyclerView = view.findViewById(R.id.list_matches)
        mEditText = view.findViewById(R.id.search_text)
        mButtonSearchTextClear = view.findViewById(R.id.buttonClearText)
        mButtonOnlyInFiltered = view.findViewById(R.id.search_in_filtered_only)

        mAdapter = RViewAdapterBasic(mElems, mSelectionStatuses, this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)

        val dm = Diary.getMain()
        if(dm.is_search_in_progress) {
            mEditText.setText(dm._search_str)
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
        val searchStr = text.lowercase(Locale.ROOT)

        val dm = Diary.getMain()
        dm._search_str = searchStr
        if( dm.is_search_in_progress) {
            mFRestartSearch = true
            dm.stop_search()
        }
        else {
            dm.start_search()
        }

        mButtonSearchTextClear.visibility = if(text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    fun handleSearchFinished() {
        if( mFRestartSearch ) {
            mFRestartSearch = false
            Diary.getMain().start_search()
        }
        else {
            updateSubtitle()
            updateList()
        }
    }

    private fun updateList() {
        if (!this::mAdapter.isInitialized) return

        val matches = Diary.getMain()._matches

        mElems.clear()

        if( matches != null && matches.isNotEmpty() ) {
            var prevPara: Paragraph? = null

            for( (i, match) in matches.withIndex() ) {
                if( i > 200 ) break
                val p2para = Diary.getMain().get_paragraph_by_id(match.get_id_lo())
                if( p2para == prevPara ) continue
                mElems.add(RViewAdapterBasic.Item(p2para._text, "",
                                                  p2para._host.icon,
                                                  p2para._id))
                prevPara = p2para
            }
        }

        mAdapter.notifyDataSetChanged()
    }

    private fun updateSubtitle() {
        val matchCount = Diary.getMain()._match_count
        Lifeograph.getActionBar().subtitle =
                if(matchCount == 0 )
                    Lifeograph.getStr(R.string.no_matches)
                else
                    "%s (%d)".format(Lifeograph.getStr(R.string.matches), matchCount)
    }

    override fun onItemClick(item: RViewAdapterBasic.Item) {
        Lifeograph.showElem(Diary.getMain().get_element(item.mIdNum))
    }

    override fun enterSelectionMode(): Boolean = false
    override fun exitSelectionMode() {}
    override fun updateActionBarSubtitle() {}
}
