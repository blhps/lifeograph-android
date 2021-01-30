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

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.sourceforge.lifeograph.Lifeograph.DiaryEditor
import java.util.*

class FragmentChartList : Fragment(), DiaryEditor, RecyclerViewAdapterElems.Listener,
        DialogInquireText.InquireListener
{
    override fun onCreate( savedInstanceState: Bundle? ) {
        super.onCreate( savedInstanceState )
        setHasOptionsMenu( true )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstState:
    Bundle?): View? {
        Log.d( Lifeograph.TAG, "FragmentChartList.onCreateView()" )
        return inflater.inflate(R.layout.fragment_list_chart, container, false)
    }

    override fun onViewCreated( view: View, savedInstanceState: Bundle? ) {
        val recyclerView: RecyclerView = view.findViewById( R.id.list_charts )
        val adapter = RecyclerViewAdapterElems( mChartElems, this )
        recyclerView.layoutManager = LinearLayoutManager( view.context )
        recyclerView.adapter = adapter
        val fab: FloatingActionButton = view.findViewById( R.id.fab_add_chart )
        fab.setOnClickListener { createNewChart() }
    }

    override fun onResume() {
        Log.d(Lifeograph.TAG, "FragmentEntryList.onResume()")
        super.onResume()
        val actionbar = (requireActivity() as AppCompatActivity).supportActionBar
        if (actionbar != null) {
            actionbar.title = Diary.diary._title_str
            actionbar.subtitle = "Charts (" + Diary.diary.m_charts.size + ")"
        }
        (activity as FragmentHost?)!!.updateDrawerMenu(R.id.nav_charts)
        updateList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_chart, menu)
        super.onCreateOptionsMenu(menu, inflater)
        mMenu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateMenuVisibilities()
    }

    override fun onOptionsItemSelected( item: MenuItem ): Boolean {
        val id = item.itemId
        if( id == R.id.enable_edit ) {
            Lifeograph.enableEditing(this )
            return true
        }
        else if( id == R.id.logout_wo_save ) {
            Lifeograph.showConfirmationPrompt( context,
                    R.string.logoutwosaving_confirm,
                    R.string.logoutwosaving
            ) { dialog: DialogInterface?, id_: Int ->
                // unlike desktop version Android version
                // does not back up changes
                Diary.diary.setSavingEnabled( false )
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateMenuVisibilities() {
        val flagWritable = Diary.diary.is_in_edit_mode
        mMenu!!.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                Diary.diary.can_enter_edit_mode()
        mMenu!!.findItem(R.id.logout_wo_save).isVisible = flagWritable
    }

    private fun updateList() {
        mChartElems.clear()
        Log.d( Lifeograph.TAG, "FragmentChartList.updateList()::ALL ENTRIES" )
        mChartElems.addAll( Diary.diary.m_charts.values )
        Collections.sort( mChartElems, FragmentEntryList.compareElems )
    }

    private fun createNewChart() {
        // ask for name
        val dlg = DialogInquireText( context, R.string.create_chart,
                                     Lifeograph.getStr( R.string.new_chart ),
                                     R.string.create, this )
        dlg.show()
    }

    // INTERFACE METHODS ===========================================================================
    // DiaryEditor INTERFACE METHODS
    override fun enableEditing() {
        updateMenuVisibilities()
    }

    //RecyclerViewAdapterDiaryElems.Listener INTERFACE METHODS
    override fun onElemClick( elem: DiaryElement ) {}

    override fun onInquireAction( id: Int, text: String? ) {
        Lifeograph.showToast( "not implemented yet" )
    }
    override fun onInquireTextChanged(id: Int, text: String?): Boolean {
        Lifeograph.showToast( "not implemented yet" )
        return false
    }

    // VARIABLES ===================================================================================
    private val mChartElems: MutableList<DiaryElement> = ArrayList()
    private var mMenu: Menu? = null
}
