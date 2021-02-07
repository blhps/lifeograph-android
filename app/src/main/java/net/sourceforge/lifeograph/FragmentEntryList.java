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

package net.sourceforge.lifeograph;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.sourceforge.lifeograph.helpers.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class FragmentEntryList extends Fragment implements DialogPassword.Listener,
        Lifeograph.DiaryEditor, Lifeograph.DiaryView, RecyclerViewAdapterElems.Listener
{
    // VARIABLES ===================================================================================
    private final List< DiaryElement > mEntries = new ArrayList<>();
    private final List< Boolean > mSelectionStatuses = new ArrayList<>();
    private Menu mMenu = null;
    private HorizontalScrollView mToolbar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapterElems mRecyclerViewAdapter;
    private FloatingActionButton mFabAddEntry = null;

    // METHODS =====================================================================================
    @Override
    public void
    onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );

        // This callback will only be called when MyFragment is at least Started.
//        OnBackPressedCallback callback = new OnBackPressedCallback( true /* enabled by default */) {
//            @Override
//            public void handleOnBackPressed() {
//                // Handle the back button event
//                Log.d( Lifeograph.TAG, "CALLBACK PRESSED HANDLER!!" );
//                handleBack();
//            }
//        };
//        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }

    @Override
    public View
    onCreateView( @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstState ) {
        Log.d( Lifeograph.TAG, "FragmentEntryList.onCreateView()" );
        return inflater.inflate( R.layout.fragment_list_entry, container, false );
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState ) {
        mRecyclerView = view.findViewById( R.id.list );
        mRecyclerViewAdapter = new RecyclerViewAdapterElems( mEntries, mSelectionStatuses, this,
                                                             true, true );
        mRecyclerView.setAdapter( mRecyclerViewAdapter );
        mRecyclerView.setLayoutManager( new LinearLayoutManager( getContext() ) );

        mFabAddEntry = ( FloatingActionButton ) view.findViewById( R.id.fab_add_entry );
        mFabAddEntry.setOnClickListener( view1 -> {
//            PopupMenu popup = new PopupMenu( getContext(), view1 );
//            MenuInflater inflater = popup.getMenuInflater();
//            inflater.inflate(R.menu.menu_entry, popup.getMenu());
//            popup.show();


            NewEntryDialogFragment nedf = new NewEntryDialogFragment( this );
            nedf.show( getActivity().getSupportFragmentManager(), "xxx" );
        } );

        mToolbar = view.findViewById( R.id.toolbar_entry_item );

        ImageButton button = view.findViewById( R.id.btn_toggle_favorite );
        button.setOnClickListener( v -> toggleSelFavoredness() );

        button = view.findViewById( R.id.btn_todo_auto );
        button.setOnClickListener( v -> setSelTodoStatus( DiaryElement.ES_NOT_TODO ) );

        button = view.findViewById( R.id.btn_todo_open );
        button.setOnClickListener( v -> setSelTodoStatus( DiaryElement.ES_TODO ) );

        button = view.findViewById( R.id.btn_todo_done );
        button.setOnClickListener( v -> setSelTodoStatus( DiaryElement.ES_DONE ) );
    }

    @SuppressLint( "RestrictedApi" )
    @Override
    public void
    onResume() {
        Log.d( Lifeograph.TAG, "FragmentEntryList.onResume()" );
        super.onResume();

        ActivityMain.mViewCurrent = this;

        updateActionBarTitle();
        updateActionBarSubtitle();

        mToolbar.setVisibility( View.GONE );
        //mFabAddEntry.setTranslationX( Diary.diary.is_in_edit_mode() ? 0 : 150 );
        mFabAddEntry.setVisibility( Diary.diary.is_in_edit_mode() ? View.VISIBLE : View.GONE );

        ( ( FragmentHost ) requireActivity() ).updateDrawerMenu( R.id.nav_entries );

        updateList();
    }

    @Override
    public void
    onDestroyView() {
        if( mRecyclerViewAdapter.hasSelection() )
            mRecyclerViewAdapter.clearSelection(
                    Objects.requireNonNull( mRecyclerView.getLayoutManager() ) );

        super.onDestroyView();
    }

    @Override
    public void
    onCreateOptionsMenu( @NonNull Menu menu, MenuInflater inflater ) {
        inflater.inflate( R.menu.menu_diary, menu );

        super.onCreateOptionsMenu( menu, inflater );

//        MenuItem item = menu.findItem( R.id.add_elem );
//        AddElemAction addElemAction = ( AddElemAction ) MenuItemCompat.getActionProvider( item );
//        addElemAction.mParent = this;

        mMenu = menu;
    }

    @Override
    public void
    onPrepareOptionsMenu( @NonNull Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        updateMenuVisibilities();
    }

    @Override
    public boolean
    onOptionsItemSelected( MenuItem item ) {
        int id = item.getItemId();

        if( id == R.id.enable_edit ) {
            Lifeograph.enableEditing( this );
            return true;
        }
        else if( id == R.id.home && handleBack() ) {
            //finish();
            return true;
        }
        else if( id == R.id.add_password ) {
            new DialogPassword( getContext(),
                                Diary.diary,
                                DialogPassword.DPAction.DPA_ADD,
                                this ).show();
            return true;
        }
        else if( id == R.id.change_password ) {
            new DialogPassword( getContext(),
                                Diary.diary,
                                DialogPassword.DPAction.DPA_AUTHENTICATE,
                                this ).show();
            return true;
        }
        else if( id == R.id.export_plain_text ) {
            if( Diary.diary.write_txt() == Result.SUCCESS )
                Lifeograph.showToast( R.string.text_export_success );
            else
                Lifeograph.showToast( R.string.text_export_fail );
            return true;
        }
        else if( id == R.id.logout_wo_save ) {
            Lifeograph.logoutWithoutSaving( requireView() );
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public boolean
    handleBack() {
        if( mRecyclerViewAdapter.hasSelection() ) {
            mRecyclerViewAdapter.clearSelection( mRecyclerView.getLayoutManager() );
            updateActionBarSubtitle();
            exitSelectionMode();
            return true;
        }
        return false;
    }

    private void
    updateMenuVisibilities() {
        boolean flagWritable = Diary.diary.is_in_edit_mode();
        boolean flagEncrypted = Diary.diary.is_encrypted();

        mMenu.findItem( R.id.enable_edit ).setVisible( !flagWritable &&
                                                       Diary.diary.can_enter_edit_mode() );

        mMenu.findItem( R.id.export_plain_text ).setVisible( !Diary.diary.is_virtual() );

        mMenu.findItem( R.id.add_password ).setVisible( flagWritable && !flagEncrypted );
        mMenu.findItem( R.id.change_password ).setVisible( flagWritable && flagEncrypted );

        mMenu.findItem( R.id.logout_wo_save ).setVisible( flagWritable );
    }

    void
    add_chapter_category_to_list( Chapter.Category ctg ) {
        for( Chapter chapter : ctg.mMap.values() ) {
            mEntries.add( chapter );

            chapter.mHasChildren = !chapter.mEntries.isEmpty();

            if( !chapter.get_expanded() )
                continue;

            for( Entry entry : chapter.mEntries ) {
                if( !entry.get_filtered_out() ) {
                    mEntries.add( entry );
                }
            }
        }
    }

    void
    updateList() {
        Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::ALL ENTRIES" );
        final long first_chapter_date = Diary.diary.m_p2chapter_ctg_cur.get_date_t();

        mEntries.clear();

        //if( ( Diary.diary.m_sorting_criteria & Diary.SoCr_FILTER_CRTR ) == Diary.SoCr_DATE ) {
        add_chapter_category_to_list( Diary.diary.m_p2chapter_ctg_cur );

        Entry entry_prev = null;
        boolean entry_prev_updated = false;

        for( Entry entry : Diary.diary.m_entries.descendingMap().values() ) {
            final boolean is_descendant =
                    ( entry_prev != null &&
                      Date.is_descendant_of( entry.get_date_t(), entry_prev.get_date_t() ) );

            if( entry_prev_updated )
                entry_prev.mHasChildren = is_descendant;

            entry_prev_updated = false;

            if( is_descendant && !entry_prev.get_expanded() )
                continue;

            // ordinals & orphans
            if( !entry.get_filtered_out() &&
                ( entry.is_ordinal() || entry.get_date_t() < first_chapter_date ) ) {
                mEntries.add( entry );
            }
            // other entries were taken care of in add_chapter_category_to_list()

            entry_prev = entry;
            entry_prev_updated = true;
        }

//        mEntries.add( new HeaderElem( R.string.numbered_entries, Date.DATE_MAX ) );
        mEntries.add( new HeaderElem( R.string.free_entries, Date.NUMBERED_MIN ) );
        mEntries.add( new HeaderElem( R.string.dated_entries,
                                      Date.make( Date.YEAR_MAX + 1, 12, 31, 0 ) ) );
        //}


        Collections.sort( mEntries, compareElemsByDate );
    }

    void
    updateActionBarTitle() {
        Lifeograph.getActionBar().setTitle( Diary.diary.get_title_str() );
    }

    @Override
    public void
    updateActionBarSubtitle() {
        int selCount = mRecyclerViewAdapter.getMSelCount();
        if( selCount > 0 ) {
            Lifeograph.getActionBar().setSubtitle( "Entries (" + selCount + " / "
                                                   + Diary.diary.get_size() + ")" );
        }
        else {
            Lifeograph.getActionBar().setSubtitle( "Entries (" + Diary.diary.get_size() + ")" );
        }
    }

    @Override
    public void
    toggleExpanded( DiaryElement elem ) {
        assert elem != null;
        elem.set_expanded( !elem.get_expanded() );
        updateList();
        mRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean
    enterSelectionMode() {
        if( Diary.diary.is_in_edit_mode() ) {
            mToolbar.setVisibility( View.VISIBLE );
            return true;
        }
        else
            return false;
    }

    @Override
    public void
    exitSelectionMode() {
        mToolbar.setVisibility( View.GONE );
    }

    void
    toggleSelFavoredness() {
        int i = 0;
        for( Boolean selected : mSelectionStatuses ) {
            if( selected ) {
                Entry entry = ( Entry ) mEntries.get( i );
                entry.toggle_favored();
            }
            i++;
        }

        mRecyclerViewAdapter.notifyDataSetChanged();
    }

    void
    setSelTodoStatus( int status ) {
        int i = 0;
        for( Boolean selected : mSelectionStatuses ) {
            if( selected ) {
                Entry entry = ( Entry ) mEntries.get( i );
                entry.set_todo_status( status );
            }
            i++;
        }

        mRecyclerViewAdapter.notifyDataSetChanged();
    }

    void
    handleEntryNumberChanged() {
        mSelectionStatuses.clear();
        mSelectionStatuses.addAll( Collections.nCopies( Diary.diary.get_size(), false ) );
    }

    // INTERFACE METHODS ===========================================================================
    // DiaryEditor INTERFACE METHODS
    @SuppressLint( "RestrictedApi" ) // due to a bug
    @Override
    public void
    enableEditing() {
        updateMenuVisibilities();
        //mFabAddEntry.setVisibility( View.VISIBLE );

        //mFabAddEntry.animate().translationX( -getResources().getDimension( R.dimen.dim50dp ) );
        mFabAddEntry.show();
    }

    // DialogPassword INTERFACE METHODS
    @Override
    public void
    onDPAction( DialogPassword.DPAction action ) {
        switch( action ) {
            case DPA_AUTHENTICATE:
                new DialogPassword( getContext(), Diary.diary,
                                    DialogPassword.DPAction.DPA_ADD,
                                    this ).show();
                break;
            case DPAR_AUTH_FAILED:
                Lifeograph.showToast( R.string.wrong_password );
                break;
        }
    }

    // RecyclerViewAdapterElems.Listener INTERFACE METHODS
    @Override
    public void
    onElemClick( DiaryElement elem ) {
        Lifeograph.showElem( elem );
    }

    // ELEMENT LIST INTERFACE ======================================================================
    public interface ListOperations
    {
        void updateList();
    }

    // INTERFACE
    public interface DiaryManager
    {
//        void addFragment( FragmentElemList fragment );
//        void removeFragment( FragmentElemList fragment );

        DiaryElement getElement();
    }

    // DIALOGFRAGMENT
    public static class NewEntryDialogFragment extends DialogFragment
    {
        FragmentEntryList mFel;

        public NewEntryDialogFragment( FragmentEntryList fel ) {
            mFel = fel;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog( Bundle savedInstanceState ) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
            // Create the AlertDialog object and return it
            builder.setTitle( "New Entry" )
                   .setItems( R.array.array_new_entry_types,
                              ( dialog, which ) -> {
                                  // The 'which' argument contains the index position
                                  // of the selected item
                                  switch( which ) {
                                      case 0:
                                          Lifeograph.goToToday();
                                          mFel.handleEntryNumberChanged();
                                          break;
                                      case 1:
                                          Lifeograph.addEntry(
                                                  Diary.diary.get_available_order_1st( true ), "" );
                                          mFel.handleEntryNumberChanged();

                                          break;
                                      case 2:
                                          Lifeograph.addEntry(
                                                  Diary.diary.get_available_order_1st( false ),
                                                  "" );
                                          mFel.handleEntryNumberChanged();
                                          break;
                                  }
                              } );

            return builder.create();
        }
    }

    // COMPARATOR ==================================================================================
    static class CompareElemsByDate implements Comparator< DiaryElement >
    {
        public int
        compare( DiaryElement elem_l, DiaryElement elem_r ) {
            // SORT BY NAME
            if( elem_l.get_date_t() == Date.NOT_APPLICABLE ) {
                return 0;
            }
            // SORT BY DATE
            else {
                int sc = Diary.diary.m_sorting_criteria;
                int direction = 1;

                if( Date.is_same_kind( elem_l.get_date_t(), elem_r.get_date_t() ) ) {
                    if( elem_l.get_date().is_ordinal() )
                        direction = ( ( sc & Diary.SoCr_FILTER_DIR ) == Diary.SoCr_ASCENDING ?
                                      -1 : 1 );
                    else
                        direction = ( ( sc & Diary.SoCr_FILTER_DIR_T ) == Diary.SoCr_ASCENDING_T ?
                                      -1 : 1 );
                }

                if( elem_l.get_date_t() > elem_r.get_date_t() )
                    return -direction;
                else if( elem_l.get_date_t() < elem_r.get_date_t() )
                    return direction;
                else
                    return 0;
            }
        }
    }

    static final CompareElemsByDate compareElemsByDate = new CompareElemsByDate();

    // HEADER PSEUDO ELEMENT CLASS =================================================================
    static class HeaderElem extends DiaryElement
    {
        private final long mDate;

        public HeaderElem( int nameRsc, long date ) {
            super( null, Lifeograph.getStr( nameRsc ), ES_VOID );
            mDate = date;
        }

        @Override
        public Type get_type() {
            return Type.NONE;
        }

        @Override
        public long get_date_t() {
            return mDate;
        }
    }
}
