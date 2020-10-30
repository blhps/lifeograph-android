/* *********************************************************************************

    Copyright (C) 2012-2020 Ahmet Öztürk (aoz_2@yahoo.com)

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
import java.util.TreeSet;

public class Chapter extends DiaryElementChart {
    public static class Category extends DiaryElement {

        public Category( Diary diary, String name ) {
            super( diary, name, ES_VOID );
            mMap = new java.util.TreeMap<>( DiaryElement.compare_dates );
            m_date_min = 0;
        }
        // for topics and groups which do not have names:
        public Category( Diary diary, long date_min ) {
            super( diary, DEID_UNSET, ES_VOID );
            mMap = new java.util.TreeMap<>( DiaryElement.compare_dates );
            m_date_min = date_min;
        }

        @Override
        public Type get_type() {
            return Type.CHAPTER_CTG;
        }

        @Override
        public int get_size() {
            return mMap.size();
        }

        @Override
        public int get_icon() {
            return R.mipmap.ic_diary;
        }

        @Override
        public String get_info_str() {
            return( mMap.size() + " entries" );
        }

        public boolean empty() {
            return mMap.isEmpty();
        }

        public Chapter create_chapter( String name, long date ) {
            Chapter chapter = new Chapter( m_ptr2diary, name, date );
            add( chapter );
            return chapter;
        }

        public Chapter create_chapter_ordinal( String name ) {
            return create_chapter( name, get_free_order_ordinal() );
        }

        public boolean set_chapter_date( Chapter chapter, long date ) {
            if( BuildConfig.DEBUG ) {
                if( chapter.is_ordinal() ) { throw new AssertionError(); }
                if( mMap.containsKey( date ) ) { throw new AssertionError(); }
            }

            if( chapter.m_date_begin.m_date != Date.NOT_SET ) {
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

                    if( c.m_date_begin.m_date == chapter.m_date_begin.m_date )
                        flagChapterFound = true;
                }
                if( !flagChapterFound )
                    return false; // chapter is not a member of the set

                mMap.remove( chapter.m_date_begin.m_date );
            }

            chapter.set_date( date );

            add( chapter );

            return true;
        }

        public long get_free_order_ordinal() {
            if( mMap.isEmpty() )
                return( m_date_min );

            Date d = new Date( ( Long ) mMap.keySet().toArray()[ 0 ] );
            d.forward_ordinal_order();
            return d.m_date;
        }

        public Chapter getChapterEarlier( Chapter chapter ) {
            boolean found = false;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date ) {
                    found = true;
                }
                else if( found ) {
                    return e.getValue();
                }
            }
            return null;
        }

        public Chapter getChapterLater( Chapter chapter ) {
            Chapter chapter_later = null;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date )
                    return chapter_later;
                else if( e.getKey() < chapter.m_date_begin.m_date )
                    break;

                chapter_later = e.getValue();
            }
            return null;
        }

        public java.util.TreeMap< Long, Chapter > getMap() {    // Java only
            return mMap;
        }

        private boolean add( Chapter chapter ) {
            mMap.put( chapter.m_date_begin.m_date, chapter );

            Chapter chapter_prev = null;
            int i = 0;
            boolean flagFound = false;

            for( Map.Entry< Long, Chapter > entry : mMap.entrySet() ) {
                if( entry.getValue().m_date_begin.m_date == chapter.m_date_begin.m_date ) {
                    if( i == 0 ) // latest
                        chapter.recalculate_span( null );
                    else
                        chapter.recalculate_span( chapter_prev );
                    flagFound = true;
                }
                else if( flagFound ) { // fix earlier entry
                    entry.getValue().recalculate_span( chapter );
                    break;
                }

                chapter_prev = entry.getValue();
                i++;
            }

            return true; // reserved
        }

        java.util.TreeMap< Long, Chapter > mMap;
        final long m_date_min;
    }

    public Chapter( Diary diary, String name, long date ) {
        super( diary, name, ES_CHAPTER_DEFAULT );
        m_date_begin = new Date( date );
        update_type();
    }

    @Override
    public Type get_type() {
        return m_type;
    }

    @Override
    public int get_size() {
        return mEntries.size();
    }

    @Override
    public int get_icon() {
        switch( m_status & ES_FILTER_TODO )
        {
            case ES_TODO:
                return R.mipmap.ic_todo_open;
            case ES_PROGRESSED:
                return R.mipmap.ic_todo_progressed;
            case ES_DONE:
                return R.mipmap.ic_todo_done;
            case ES_CANCELED:
                return R.mipmap.ic_todo_canceled;
            default:
                return( get_date_icon() );
        }
    }

    private int get_date_icon() {
        return( m_date_begin.is_ordinal() ?
                    ( m_date_begin.is_hidden() ? R.mipmap.ic_chapter_f : R.mipmap.ic_chapter_o ) :
                    R.mipmap.ic_chapter_t );
    }

    @Override
    public Date get_date() {
        return m_date_begin;
    }

    @Override
    public String get_title_str() {
        if( m_date_begin.is_hidden() )
            return m_name;
        else
            return( m_date_begin.format_string() + STR_SEPARATOR + m_name );
    }

    @Override
    public String get_info_str() {
        return( get_size() + " entries" );
    }

    @Override
    public String getListStrSecondary() {
        return( get_type_name() + " with " + get_size() + " entries" );
    }

    public boolean is_ordinal() {
        return m_date_begin.is_ordinal();
    }

    public void update_type() {
        if( m_date_begin.is_hidden() )
            m_type = Type.GROUP;
        else if( m_date_begin.is_ordinal() )
            m_type = Type.TOPIC;
        else
            m_type = Type.CHAPTER;
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
    public boolean get_expanded() {
        return( ( m_status & ES_EXPANDED ) != 0 );
    }

    public void set_expanded( boolean expanded ) {
        set_status_flag( ES_EXPANDED, expanded );
    }

    public void set_date( long date ) {
        m_date_begin.m_date = date;
        update_type();
    }

    public Date get_free_order() {
        Date date = new Date( m_date_begin.m_date );
        Diary.diary.make_free_entry_order( date );
        return date;
    }

    private void recalculate_span( Chapter next ) {
        if( next == null )
            m_time_span = 0; // unlimited
        else if( next.m_date_begin.is_ordinal() )
            m_time_span = 0; // last temporal chapter: unlimited
        else
            m_time_span = m_date_begin.calculate_days_between( next.m_date_begin );
    }

    int get_color() {
        return m_color;
    }

    void set_color( int color ) {
        m_color = color;
    }

    @Override
    ChartPoints create_chart_data() {
        if( mEntries.isEmpty() )
            return null;

        ChartPoints cp = new ChartPoints( m_chart_type );
        Date d_last = new Date( Date.NOT_SET );

        for( Entry entry : mEntries.descendingSet() )
            cp.add_plain( d_last, entry.get_date() );

        //Diary.diary.fill_up_chart_points( cp );

        return cp;
    }

    // DATA
    Date m_date_begin;
    int m_time_span = 0;
    Type m_type;
    java.util.TreeSet< Entry > mEntries =
            new TreeSet< Entry >( DiaryElement.compare_elems_by_date );
    int m_color = Color.WHITE;
}
