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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import net.sourceforge.lifeograph.helpers.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FragmentEntryList extends Fragment implements DialogPassword.Listener,
        Lifeograph.DiaryEditor, Lifeograph.DiaryView, RecyclerViewAdapterElems.Listener
{
    // VARIABLES ===================================================================================
    private final List< DiaryElement > mEntries = new ArrayList<>();
    private final List< Boolean >      mSelectionStatuses = new ArrayList<>();
    private Menu                       mMenu = null;
    private HorizontalScrollView       mToolbar;
    private RecyclerView               mRecyclerView;
    private RecyclerViewAdapterElems   mRecyclerViewAdapter;

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

        mToolbar = view.findViewById( R.id.toolbar_entry_item );

        ImageButton button = view.findViewById( R.id.btn_toggle_favorite );
        button.setOnClickListener( v -> toggleSelFavoredness( ) );

        button = view.findViewById( R.id.btn_todo_auto );
        button.setOnClickListener( v -> setTodoDone( ) );
    }

    @Override
    public void
    onResume() {
        Log.d( Lifeograph.TAG, "FragmentEntryList.onResume()" );
        super.onResume();

        ActivityMain.mViewCurrent = this;

        updateActionBarTitle();
        updateActionBarSubtitle();

        mToolbar.setVisibility( View.GONE );

        ( ( FragmentHost ) getActivity() ).updateDrawerMenu( R.id.nav_entries );

        updateList();
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
        else
        if( id == R.id.home && handleBack() ) {
            //finish();
            return true;
        }
        else
        if( id == R.id.add_password ) {
            new DialogPassword( getContext(),
                                Diary.diary,
                                DialogPassword.DPAction.DPA_ADD,
                                this ).show();
            return true;
        }
        else
        if( id == R.id.change_password ) {
            new DialogPassword( getContext(),
                                Diary.diary,
                                DialogPassword.DPAction.DPA_AUTHENTICATE,
                                this ).show();
            return true;
        }
        else
        if( id == R.id.export_plain_text ) {
            if( Diary.diary.write_txt() == Result.SUCCESS )
                Lifeograph.showToast( R.string.text_export_success );
            else
                Lifeograph.showToast( R.string.text_export_fail );
            return true;
        }
        else
        if( id == R.id.logout_wo_save ) {
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

        mMenu.findItem( R.id.add_elem ).setVisible( flagWritable );

        mMenu.findItem( R.id.export_plain_text ).setVisible( !Diary.diary.is_virtual() );

        mMenu.findItem( R.id.add_password ).setVisible( flagWritable && !flagEncrypted );
        mMenu.findItem( R.id.change_password ).setVisible( flagWritable && flagEncrypted );

        mMenu.findItem( R.id.logout_wo_save ).setVisible( flagWritable );
    }

    void
    updateList() {
        mEntries.clear();

        Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::ALL ENTRIES" );
        for( Entry e : Diary.diary.m_entries.values() ) {
            if( !e.get_filtered_out() )
                mEntries.add( e );
        }

        Collections.sort( mEntries, compareElems );
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
    setTodoDone() {
        int i = 0;
        for( Boolean selected : mSelectionStatuses ) {
            if( selected ) {
                Entry entry = ( Entry ) mEntries.get( i );
                entry.set_todo_status( DiaryElement.ES_DONE );
            }
            i++;
        }

        mRecyclerViewAdapter.notifyDataSetChanged();
    }

    // INTERFACE METHODS ===========================================================================
    // DiaryEditor INTERFACE METHODS
    @Override
    public void
    enableEditing() {
        updateMenuVisibilities();
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

    // COMPARATOR ==================================================================================
    static class CompareListElems implements Comparator< DiaryElement >
    {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            // NOTE: this function assumes only similar elements are listed at a time

            // SORT BY NAME
            if( elem_l.get_date_t() == Date.NOT_APPLICABLE ) {
                return 0;
            }
            // SORT BY DATE
            else {
                int direction =
                        ( elem_l.get_date().is_ordinal() && elem_r.get_date().is_ordinal() ) ?
                                -1 : 1;

                if( elem_l.get_date_t() > elem_r.get_date_t() )
                    return -direction;
                else if( elem_l.get_date_t() < elem_r.get_date_t() )
                    return direction;
                else
                    return 0;
            }
        }
    }

    static final CompareListElems compareElems = new CompareListElems();

    // HEADER PSEUDO ELEMENT CLASS =================================================================
//    static class HeaderElem extends DiaryElement
//    {
//        public HeaderElem( int nameRsc ) {
//            super( null, Lifeograph.getStr( nameRsc ), ES_VOID );
//        }
//
//        public String get_info_str() {
//            return "";
//        }
//
//        @Override
//        public int get_icon() {
//            return R.mipmap.ic_diary;
//        }
//
//        @Override
//        public Type get_type() {
//            return Type.HEADER;
//        }
//
//        @Override
//        public int get_size() {
//            return 0;
//        }
//    }

    // DIARY ELEMENT ADAPTER CLASS =================================================================
    static class DiaryElemAdapter extends ArrayAdapter< DiaryElement >
    {
        public DiaryElemAdapter( Context context,
                                 int resource,
                                 int textViewResourceId,
                                 java.util.List< DiaryElement > objects,
                                 LayoutInflater inflater ) {
            super( context, resource, textViewResourceId, objects );
            mInflater = inflater;
        }

        private ViewHolder setHolder( DiaryElement elem, ViewGroup par ) {
            View view;
            ViewHolder holder;

            switch( elem.get_type() ) {
//                case TAG_CTG:
//                    view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
//                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_TAG_CTG );
//                    break;
                case CHAPTER_CTG:
                    view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_CHAPTER_CTG );
                    break;
//                case HEADER:
//                    view = mInflater.inflate( R.layout.list_section_simple, par, false );
//                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_SIMPLE );
//                    break;
                default:
                    view = mInflater.inflate( R.layout.list_item_element, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.ELEMENT );
                    break;
            }

            view.setTag( holder );
            return holder;
        }

        public void handleCollapse( DiaryElement elem ) {
            Log.d( Lifeograph.TAG, "handle collapse " + elem.get_name() );
            Chapter.Category cc = ( Chapter.Category ) elem;
            Diary.diary.set_chapter_ctg_cur( cc );

            //updateList();
        }

        @NonNull
        @Override
        public View getView( int position, View convertView, @NonNull ViewGroup parent ) {
            ViewHolder holder;
            TextView title;
            final DiaryElement elem = getItem( position );
            assert elem != null;

            if( convertView == null ) {
                holder = setHolder( elem, parent );
                convertView = holder.getView();
            }
            else {
                holder = ( ViewHolder ) convertView.getTag();
            }

            if( holder.getLayoutType() != elem.get_type().layout_type ) {
                holder = setHolder( elem, parent );
                convertView = holder.getView();
            }

            title = holder.getName();
            title.setText( elem.get_list_str() );

            switch( holder.getLayoutType() ) {
//                case HEADER_SIMPLE:
//                    break;
                case HEADER_CHAPTER_CTG: {
                    Chapter.Category cc = ( Chapter.Category ) elem;
                    ImageButton iconCollapse = holder.getIconCollapse();
                    iconCollapse.setImageResource( cc == Diary.diary.m_p2chapter_ctg_cur ?
                                                           R.drawable.ic_radio_sel : R.drawable.ic_radio_empty );
                    iconCollapse.setOnClickListener( v -> handleCollapse( elem ) );
                    if( Diary.diary.is_in_edit_mode() )
                        holder.getIconOptions().setTag( cc );
                    else
                        holder.getIconOptions().setVisibility( View.INVISIBLE );
                    break;
                }
                case ELEMENT: {
                    TextView detail = holder.getDetail();
                    detail.setText( elem.get_info_str() );

                    ImageView icon = holder.getIcon();
                    icon.setImageResource( elem.get_icon() );

                    if( elem instanceof Entry ) {
                        ImageView icon2 = holder.getIcon2();
                        icon2.setImageResource( R.mipmap.ic_favorite );
                        icon2.setVisibility( ( ( Entry ) elem ).is_favored() ? View.VISIBLE :
                                                 View.INVISIBLE );
                    }
                    break;
                }
                default:
                    break;
            }

            return convertView;
        }

        private final LayoutInflater mInflater;

        // VIEW HOLDER =============================================================================
        private static class ViewHolder
        {
            private final View mRow;
            private TextView mTitle = null;
            private TextView mDetail = null;
            private ImageView mIcon = null;
            private ImageView mIcon2 = null;

            private ImageButton mIconCollapse = null;
            private ImageButton mIconOptions = null;

            private final DiaryElement.LayoutType mLayoutType;

            public ViewHolder( View row, DiaryElement.LayoutType layoutType ) {
                mRow = row;
                mLayoutType = layoutType;
            }

            public DiaryElement.LayoutType getLayoutType() {
                return mLayoutType;
            }

            public View getView() {
                return mRow;
            }

            public TextView getName() {
                if( mTitle == null ) {
                    mTitle = mRow.findViewById( R.id.title );
                }
                return mTitle;
            }

            public TextView getDetail() {
                if( mDetail == null ) {
                    mDetail = mRow.findViewById( R.id.detail );
                }
                return mDetail;
            }

            public ImageView getIcon() {
                if( mIcon == null ) {
                    mIcon = mRow.findViewById( R.id.icon );
                }
                return mIcon;
            }

            public ImageView getIcon2() {
                if( mIcon2 == null ) {
                    mIcon2 = mRow.findViewById( R.id.icon2 );
                }
                return mIcon2;
            }

            public ImageButton getIconCollapse() {
                if( mIconCollapse == null ) {
                    mIconCollapse = mRow.findViewById( R.id.icon_collapse );
                }
                return mIconCollapse;
            }

            public ImageButton getIconOptions() {
                if( mIconOptions == null ) {
                    mIconOptions = mRow.findViewById( R.id.icon_options );
                }
                return mIconOptions;
            }
        }
    }

}
