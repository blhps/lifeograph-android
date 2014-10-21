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

import java.util.Map;
import java.util.TreeSet;

public class Chapter extends DiaryElement {
    public static class Category extends DiaryElement {

        public Category( Diary diary, String name ) {
            super( diary, name, ES_VOID );
            mMap = new java.util.TreeMap< Long, Chapter >( DiaryElement.compare_dates );
            m_date_min = 0;
        }
        // for topics and groups which do not have names:
        public Category( Diary diary, long date_min ) {
            super( diary, DEID_UNSET, ES_VOID );
            mMap = new java.util.TreeMap< Long, Chapter >( DiaryElement.compare_dates );
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
            return R.drawable.ic_diary;
        }

        @Override
        public String get_info_str() {
            return "Size: " + mMap.size();
        }

        public boolean empty() {
            return mMap.isEmpty();
        }

        public Chapter create_chapter( String name, long date ) {
            Chapter chapter = new Chapter( m_ptr2diary, name, date );
            mMap.put( date, chapter );
            return chapter;
        }

        public Chapter create_chapter_ordinal( String name ) {
            return create_chapter( name, get_free_order_ordinal() );
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
                return R.drawable.ic_todo_open;
            case ES_PROGRESSED:
                return R.drawable.ic_todo_progressed;
            case ES_DONE:
                return R.drawable.ic_todo_done;
            case ES_CANCELED:
                return R.drawable.ic_todo_canceled;
            default:
                return( m_date_begin.is_ordinal() ? R.drawable.ic_chapter_ord : R.drawable.ic_calendar );
        }
    }

    @Override
    public Date get_date() {
        return m_date_begin;
    }

    @Override
    public String get_info_str() {
        return( get_type_name() + " with " + get_size() + " Entries" );
    }

    @Override
    public String get_list_str() {
        if( m_date_begin.is_hidden() )
            return m_name;
        else
            return( m_date_begin.format_string() + STR_SEPARATOR + m_name );
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

    Date m_date_begin;
    int m_time_span = 0;
    Type m_type;
    java.util.TreeSet< Entry > mEntries =
            new TreeSet< Entry >( DiaryElement.compare_elems_by_date );
}
