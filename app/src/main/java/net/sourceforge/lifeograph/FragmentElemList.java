/***********************************************************************************

 Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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


import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FragmentElemList extends ListFragment
{
    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState ) {
        if( getArguments() != null )
            mCurTabIndex = getArguments().getInt( "tab" );

        mAdapterEntries = new DiaryElemAdapter( getActivity(),
                                                R.layout.list_item_element,
                                                R.id.title,
                                                mElems,
                                                inflater );
        this.setListAdapter( mAdapterEntries );

        ViewGroup rootView = ( ViewGroup ) inflater.inflate(
                R.layout.fragment_elem_list, container, false );

        return rootView;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        updateList();
    }

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach( activity );

        if( DiaryManager.class.isInstance( activity ) )
            mDiaryManager = ( DiaryManager ) activity;
        else
            throw new ClassCastException( activity.toString() + " must be a DiaryManager" );

        Log.d( Lifeograph.TAG, "FragmentElemList.onAttach() - " + activity.toString() );

        mDiaryManager.addFragment( this );
    }

    @Override
    public void onDetach() {
        mDiaryManager.removeFragment( this );
        Log.d( Lifeograph.TAG, "FragmentElemList.onDetach() - " + this.toString() );
        super.onDetach();
    }

    @Override
    public void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );
        switch( mElems.get( pos ).get_type() ) {
            case ENTRY:
            case TAG:
            case UNTAGGED:
            case CHAPTER:
            case TOPIC:
            case GROUP:
                Lifeograph.showElem( getActivity(), mElems.get( pos ) );
        }
    }

    public void updateList() {
        Log.d( Lifeograph.TAG, "DiaryFragment.updateList()" );

        mAdapterEntries.clear();
        mElems.clear();

        switch( mDiaryManager.getElement().get_type() ) {
            case DIARY:
                // ALL ENTRIES
                if( mCurTabIndex == 0 ) {
                    for( Entry e : Diary.diary.m_entries.values() ) {
                        if( !e.get_filtered_out() )
                            mElems.add( e );
                    }

                    Collections.sort( mElems, compareElems );
                }

                // CHAPTERS
                else if( mCurTabIndex == 1 ) {
                    // FREE CHAPTERS
                    if( !Diary.diary.m_groups.mMap.isEmpty() ) {
                        mElems.add( new HeaderElem( R.string.free_chapters ) );
                        for( Chapter c : Diary.diary.m_groups.mMap.descendingMap().values() ) {
                            mElems.add( c );
                        }
                    }
                    // NUMBERED CHAPTERS
                    if( !Diary.diary.m_topics.mMap.isEmpty() ) {
                        mElems.add( new HeaderElem( R.string.numbered_chapters ) );
                        for( Chapter c : Diary.diary.m_topics.mMap.descendingMap().values() ) {
                            mElems.add( c );
                        }
                    }
                    // DATED CHAPTERS
                    if( Diary.diary.m_chapter_categories.size() == 1 )
                        mElems.add( new HeaderElem( R.string.dated_chapters ) );

                    for( Chapter.Category cc : Diary.diary.m_chapter_categories.values() ) {
                        if( Diary.diary.m_chapter_categories.size() > 1 )
                            mElems.add( cc );

                        if( cc == Diary.diary.m_ptr2chapter_ctg_cur ) {
                            for( Chapter c : cc.mMap.values() ) {
                                mElems.add( c );
                            }

                            if( Diary.diary.m_orphans.get_size() > 0 )
                                mElems.add( Diary.diary.m_orphans );
                        }
                    }
                }

                // TAGS
                else if( mCurTabIndex == 2 ) {
                    // ROOT TAGS
                    for( Tag t : Diary.diary.m_tags.values() ) {
                        if( t.get_category() == null )
                            mElems.add( t );
                    }
                    // CATEGORIES
                    for( Tag.Category c : Diary.diary.m_tag_categories.values() ) {
                        mElems.add( c );
                        if( c.get_expanded() ) {
                            for( Tag t : c.mTags )
                                mElems.add( t );
                        }
                    }
                    // UNTAGGED META TAG
                    if( Diary.diary.m_untagged.get_size() > 0 ) {
                        mElems.add( new HeaderElem( R.string._empty_ ) );
                        mElems.add( Diary.diary.m_untagged );
                    }
                }
                break;
            case TAG:
            case UNTAGGED: {
                Tag t = ( Tag ) mDiaryManager.getElement();
                for( Entry e : t.mEntries ) {
                    if( !e.get_filtered_out() )
                        mElems.add( e );
                }

                Collections.sort( mElems, compareElems );
                break;
            }
            case CHAPTER:
            case TOPIC:
            case GROUP: {
                Chapter c = ( Chapter ) mDiaryManager.getElement();
                for( Entry e : c.mEntries ) {
                    if( !e.get_filtered_out() )
                        mElems.add( e );
                }

                Collections.sort( mElems, compareElems );
                break;
            }
        }
    }

    public interface DiaryManager
    {
        public void addFragment( FragmentElemList fragment );
        public void removeFragment( FragmentElemList fragment );

        public DiaryElement getElement();
    }

    private java.util.List< DiaryElement > mElems = new ArrayList< DiaryElement >();
    private DiaryElemAdapter mAdapterEntries = null;
    DiaryManager mDiaryManager;
    private int mCurTabIndex = 0;

    // ELEMENT LIST INTERFACE ======================================================================
    public interface ListOperations
    {
        public void updateList();
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
    class HeaderElem extends DiaryElement {

        public HeaderElem( int nameRsc ) {
            super( null, Lifeograph.getStr( nameRsc ), ES_VOID );
        }

        @Override
        public String get_info_str() {
            return "";
        }

        @Override
        public int get_icon() {
            return R.drawable.ic_diary;
        }

        @Override
        public Type get_type() {
            return Type.HEADER;
        }

        @Override
        public int get_size() {
            return 0;
        }
    }

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
                case TAG_CTG:
                    view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_TAG_CTG );
                    break;
                case CHAPTER_CTG:
                    view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_CHAPTER_CTG );
                    break;
                case HEADER:
                    view = mInflater.inflate( R.layout.list_section_simple, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_SIMPLE );
                    break;
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
            switch( elem.get_type().layout_type ) {
                case HEADER_TAG_CTG:
                    Tag.Category tc = ( Tag.Category ) elem;
                    tc.set_expanded( !tc.get_expanded() );
                    break;
                case HEADER_CHAPTER_CTG:
                    Chapter.Category cc = ( Chapter.Category ) elem;
                    Diary.diary.set_current_chapter_ctg( cc );
                    break;
            }

            updateList();
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            ViewHolder holder;
            TextView title;
            final DiaryElement elem = getItem( position );

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
                case HEADER_SIMPLE:
                    break;
                case HEADER_TAG_CTG: {
                    Tag.Category tc = ( Tag.Category ) elem;
                    ImageButton iconCollapse = holder.getIconCollapse();
                    iconCollapse.setImageResource(
                            tc.get_expanded() ? R.drawable.ic_expanded : R.drawable.ic_collapsed );
                    iconCollapse.setOnClickListener( new View.OnClickListener()
                    {
                        public void onClick( View v ) {
                            handleCollapse( elem );
                        }
                    } );
                    break;
                }
                case HEADER_CHAPTER_CTG: {
                    Chapter.Category cc = ( Chapter.Category ) elem;
                    ImageButton iconCollapse = holder.getIconCollapse();
                    iconCollapse.setImageResource( cc == Diary.diary.m_ptr2chapter_ctg_cur ?
                                                           R.drawable.ic_radio_sel : R.drawable.ic_radio_empty );
                    iconCollapse.setOnClickListener( new View.OnClickListener()
                    {
                        public void onClick( View v ) {
                            handleCollapse( elem );
                        }
                    } );
                    break;
                }
                case ELEMENT: {
                    TextView detail = holder.getDetail();
                    detail.setText( elem.getListStrSecondary() );

                    ImageView icon = holder.getIcon();
                    icon.setImageResource( elem.get_icon() );

                    ImageView icon2 = holder.getIcon2();
                    icon2.setImageResource( R.drawable.ic_favorite );
                    icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );
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
                if( null == mTitle ) {
                    mTitle = ( TextView ) mRow.findViewById( R.id.title );
                }
                return mTitle;
            }

            public TextView getDetail() {
                if( null == mDetail ) {
                    mDetail = ( TextView ) mRow.findViewById( R.id.detail );
                }
                return mDetail;
            }

            public ImageView getIcon() {
                if( null == mIcon ) {
                    mIcon = ( ImageView ) mRow.findViewById( R.id.icon );
                }
                return mIcon;
            }

            public ImageView getIcon2() {
                if( null == mIcon2 ) {
                    mIcon2 = ( ImageView ) mRow.findViewById( R.id.icon2 );
                }
                return mIcon2;
            }

            public ImageButton getIconCollapse() {
                if( null == mIconCollapse ) {
                    mIconCollapse = ( ImageButton ) mRow.findViewById( R.id.icon_collapse );
                }
                return mIconCollapse;
            }
        }
    }

}
