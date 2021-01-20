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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.sourceforge.lifeograph.helpers.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class FragmentEntryList extends ListFragment
{
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );
    }

    @Override
    public View
    onCreateView( @NonNull LayoutInflater inflater,
                  ViewGroup container,
                  Bundle savedInstanceState ) {
        Log.d( Lifeograph.TAG, "FragmentElemList.onCreateView()" );

        if( getArguments() != null )
            mCurTabIndex = getArguments().getInt( "tab" );

        mAdapterEntries = new DiaryElemAdapter( getActivity(),
                                                R.layout.list_item_element,
                                                R.id.title,
                                                mElems,
                                                inflater );
        this.setListAdapter( mAdapterEntries );

        return inflater.inflate( R.layout.fragment_elem_list, container, false );
    }

    @Override
    public void
    onResume() {
        Log.d( Lifeograph.TAG, "FragmentElemList.onResume()" );
        super.onResume();

        ActionBar actionbar = ( ( AppCompatActivity ) requireActivity() ).getSupportActionBar();
        if( actionbar != null ) {
            actionbar.setTitle( Diary.diary.get_title_str() );
            actionbar.setSubtitle( Diary.diary.get_info_str() );
        }

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
            //Lifeograph.enableEditing( this );
            return true;
        }
        else
        if( id == R.id.home ) {
            //finish();
            return true;
        }
        else
        if( id == R.id.add_password ) {
//            new DialogPassword( getContext(),
//                                Diary.diary,
//                                DialogPassword.DPAction.DPA_ADD,
//                                this ).show();
            return true;
        }
        else
        if( id == R.id.change_password ) {
//            new DialogPassword( getContext(),
//                                Diary.diary,
//                                DialogPassword.DPAction.DPA_AUTHENTICATE,
//                                this ).show();
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
            Lifeograph.showConfirmationPrompt( getContext(),
                                               R.string.logoutwosaving_confirm,
                                               R.string.logoutwosaving,
                                               ( dialog, id_ ) -> {
                                                   // unlike desktop version Android version
                                                   // does not back up changes
                                                   Diary.diary.setSavingEnabled( false );
                                                   //TODO finish();
                                               } );
            return true;
        }

        return super.onOptionsItemSelected( item );
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

    @Override
    public void onListItemClick( @NonNull ListView l, @NonNull View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );
        switch( mElems.get( pos ).get_type() ) {
            case ENTRY:
            case CHAPTER:
                Lifeograph.showElem( mElems.get( pos ) );
                break;
        }
    }

    public void updateList() {
        mAdapterEntries.clear();
        mElems.clear();

//        switch( mDiaryManager.getElement().get_type() ) {
//            case DIARY:
                // ALL ENTRIES
                if( mCurTabIndex == 0 ) {
                    Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::ALL ENTRIES" );
                    for( Entry e : Diary.diary.m_entries.values() ) {
                        if( !e.get_filtered_out() )
                            mElems.add( e );
                    }

                    Collections.sort( mElems, compareElems );
                }

                // CHAPTERS
                else if( mCurTabIndex == 1 ) {
                    Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::CHAPTERS" );
                }

                // TAGS
                else if( mCurTabIndex == 2 ) {
                    Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::TAGS" );
                }
//                break;
//            case CHAPTER:
//            {
//                Log.d( Lifeograph.TAG, "FragmentElemList.updateList()::CHAPTER ENTRIES" );
//                Chapter c = ( Chapter ) mDiaryManager.getElement();
//                for( Entry e : c.mEntries ) {
//                    if( !e.get_filtered_out() )
//                        mElems.add( e );
//                }
//
//                Collections.sort( mElems, compareElems );
//                break;
//            }
//        }
    }

    private java.util.List< DiaryElement > mElems = new ArrayList<>();
    private DiaryElemAdapter mAdapterEntries = null;
    DiaryManager mDiaryManager;
    private int  mCurTabIndex = 0;
    private Menu mMenu = null;

    // INTERFACE
    public interface DiaryManager
    {
//        void addFragment( FragmentElemList fragment );
//        void removeFragment( FragmentElemList fragment );

        DiaryElement getElement();
    }

    // ELEMENT LIST INTERFACE ======================================================================
    public interface ListOperations
    {
        void updateList();
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
    class DiaryElemAdapter extends ArrayAdapter< DiaryElement >
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

            updateList();
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
                    // detail.setText( elem.getListStrSecondary() );

                    ImageView icon = holder.getIcon();
                    icon.setImageResource( elem.get_icon() );

                    ImageView icon2 = holder.getIcon2();
                    icon2.setImageResource( R.mipmap.ic_favorite );
                    //icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );
                    break;
                }
                default:
                    break;
            }

            return convertView;
        }

        private LayoutInflater mInflater;

        // VIEW HOLDER =============================================================================
        private class ViewHolder
        {
            private View mRow;
            private TextView mTitle = null;
            private TextView mDetail = null;
            private ImageView mIcon = null;
            private ImageView mIcon2 = null;

            private ImageButton mIconCollapse = null;
            private ImageButton mIconOptions = null;

            private DiaryElement.LayoutType mLayoutType;

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
