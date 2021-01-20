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

import android.graphics.Color;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class Chapter extends Entry {
    public static class Category extends DiaryElement {
        Category( Diary diary, String name ) {
            super( diary, name, ES_VOID );
        }

        @Override
        public Type
        get_type() {
            return Type.CHAPTER_CTG;
        }

        @Override
        public int
        get_size() {
            return mMap.size();
        }

        String
        get_info_str() {
            return( mMap.size() + " entries" );
        }

        boolean
        isEmpty() {
            return mMap.isEmpty();
        }

        Chapter
        get_chapter_at( long date ) {
            return mMap.get( date );
        }

        Chapter
        get_chapter_around( long date ) {
            for( Chapter chapter : mMap.values() ) {
                if( chapter.m_date.m_date <= date )
                    return chapter;
            }

            return null;
        }

        Chapter
        create_chapter( long date, boolean F_favorite, boolean F_trashed, boolean F_expanded ) {
            int status = ( ES_NOT_TODO |
                           ( F_favorite ? ES_FAVORED : ES_NOT_FAVORED ) |
                           ( F_trashed ? ES_TRASHED : ES_NOT_TRASHED ) );
            if( F_expanded )
                status |= ES_EXPANDED;

            Chapter chapter = new Chapter( m_p2diary, Date.get_pure( date ), status );

            add( chapter );

            return chapter;
        }

        boolean
        set_chapter_date( Chapter chapter, long date ) {
            if( BuildConfig.DEBUG ) {
                if( chapter.is_ordinal() ) { throw new AssertionError(); }
            }

            if( mMap.containsKey( date ) )
                return false;

            if( chapter.m_date.m_date != Date.NOT_SET ) {
                // fix time span
                boolean flagChapterFound = false;
                for( Chapter c : mMap.values() ) {
                    if( flagChapterFound ) {
                        if( chapter.m_time_span > 0 )
                            c.m_time_span += chapter.m_time_span;
                        else
                            c.m_time_span = 0;
                        break;
                    }

                    if( c.m_date.m_date == chapter.m_date.m_date )
                        flagChapterFound = true;
                }
                if( !flagChapterFound )
                    return false; // chapter is not a member of the set

                mMap.remove( chapter.m_date.m_date );
                chapter.set_ctg( null );
            }

            chapter.set_date( date );

            add( chapter );

            return true;
        }

        Chapter
        getChapterEarlier( Chapter chapter ) {
            boolean found = false;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date.m_date ) {
                    found = true;
                }
                else if( found ) {
                    return e.getValue();
                }
            }
            return null;
        }

        Chapter
        getChapterLater( Chapter chapter ) {
            Chapter chapter_later = null;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date.m_date )
                    return chapter_later;
                else if( e.getKey() < chapter.m_date.m_date )
                    break;

                chapter_later = e.getValue();
            }
            return null;
        }

        protected boolean
        add( Chapter chapter ) {
            mMap.put( chapter.m_date.m_date, chapter );

            Chapter chapter_prev = null;
            int i = 0;
            boolean flagFound = false;

            for( Map.Entry< Long, Chapter > kv_chapter : mMap.entrySet() ) {
                if( kv_chapter.getValue().m_date.m_date == chapter.m_date.m_date ) {
                    if( i == 0 ) // latest
                        chapter.recalculate_span( null );
                    else
                        chapter.recalculate_span( chapter_prev );
                    flagFound = true;
                }
                else if( flagFound ) { // fix earlier entry
                    kv_chapter.getValue().recalculate_span( chapter );
                    break;
                }

                chapter_prev = kv_chapter.getValue();
                i++;
            }

            chapter.set_ctg( this );

            return true; // reserved
        }

        java.util.TreeMap< Long, Chapter > mMap =
                new java.util.TreeMap<>( DiaryElement.compare_dates );
    }

    Chapter( Diary d, long date, int status ) {
        super( d, date, status );
    }

    @Override
    public Type
    get_type() {
        return Type.CHAPTER;
    }

    @Override
    public int
    get_icon() {
        if( ( m_status & ES_FILTER_TODO_PURE ) != 0 )
            return Lifeograph.get_todo_icon( m_status & ES_FILTER_TODO_PURE );
        else
            return R.mipmap.ic_chapter_t;
    }

    @Override
    public String get_title_str() {
        if( m_date.is_hidden() )
            return m_name;
        else
            return( m_date.format_string() + STR_SEPARATOR + m_name );
    }

    @Override
    public String get_info_str() {
        return( get_size() + " entries" );
    }

    @Override
    public String getListStrSecondary() {
        return( get_type_name() + " with " + get_size() + " entries" );
    }

    @Override
    void
    set_date( long date ) {
        if( ! Date.is_ordinal( date ) ) m_date.m_date = date;
    }

    // REFERRER RELATED METHODS ====================================================================
    public void clear() {
        mEntries.clear();
    }

    public void insert( Entry e ) {
        mEntries.add( e );
    }

    public void erase( Entry e ) {
        mEntries.remove( e );
    }

    public boolean find( Entry e ) {
        return mEntries.contains( e );
    }

    // CHAPTER FUNCTIONS ===========================================================================
    void
    set_ctg( Chapter.Category ctg ) {
        m_p2ctg = ctg;
    }

    long
    get_free_order() {
        return m_p2diary.get_available_order_sub( m_date );
    }

    protected void
    recalculate_span( Chapter next ) {
        if( next == null )
            m_time_span = 0; // unlimited
        else if( next.m_date.is_ordinal() )
            m_time_span = 0; // last temporal chapter: unlimited
        else
            m_time_span = m_date.calculate_days_between( next.m_date );
    }

    int
    get_entry_count() {
        return mEntries.size();
    }

    // DATA
    int      m_time_span = 0;
    java.util.TreeSet< Entry >
             mEntries = new TreeSet< Entry >( DiaryElement.compare_elems_by_date );
    int      m_color = Color.WHITE;
    Category m_p2ctg = null;
}
