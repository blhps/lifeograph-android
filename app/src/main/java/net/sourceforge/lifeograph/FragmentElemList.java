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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FragmentElemList extends ListFragment implements DiaryElemAdapter.ListOperations
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
                                                inflater,
                                                this );
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

        Log.d( Lifeograph.TAG, "onAttach - " + activity.toString() );

        mDiaryManager.addFragment( this );
    }

    @Override
    public void onStop() {
        mDiaryManager.removeFragment( this );
        super.onStop();
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

        // force menu update
        //invalidateOptionsMenu();
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

}
