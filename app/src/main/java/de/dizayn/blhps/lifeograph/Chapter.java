/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

import java.util.Map;

public class Chapter extends DiaryElement {
    public static class Category extends DiaryElement {

        public Category( Diary diary, String name ) {
            super( diary, name, ES_VOID );
            mMap = new java.util.TreeMap< Long, Chapter >( DiaryElement.compare_dates );
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
        public String getSubStr() {
            return "Size: " + mMap.size();
        }

        @Override
        public int get_icon() {
            return R.drawable.ic_diary;
        }

        public Chapter create_chapter( String name, long date ) {
            Chapter chapter = new Chapter( mDiary, name, date );
            mMap.put( date, chapter );
            return chapter;
        }

        public Chapter add_chapter( String name ) {
            Chapter chapter = new Chapter( mDiary, name, get_free_order_ordinal().m_date );
            chapter.set_expanded( true );
            mMap.put( chapter.m_date_begin.m_date, chapter );
            return chapter;
        }

        public void dismiss_chapter( Chapter chapter ) {
            boolean found = false;

            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date ) {
                    found = true;
                }
                else if( found ) {
                    Chapter chapter_earlier = e.getValue();
                    if( chapter.m_time_span > 0 )
                        chapter_earlier.m_time_span += chapter.m_time_span;
                    else
                        chapter_earlier.m_time_span = 0;
                    break;
                }
            }
            mMap.remove( chapter.m_date_begin.m_date );
        }

        Date get_free_order_ordinal() {
            if( mMap.size() > 0 ) {
                Date d = new Date( ( Integer ) mMap.keySet().toArray()[ 0 ] );
                if( d.is_ordinal() ) {
                    d.forward_ordinal_order();
                    return d;
                }
            }

            return( new Date( 0, 0 ) );
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

        protected java.util.Map< Long, Chapter > mMap;
    }

    public Chapter( Diary diary, String name ) {
        super( diary, name, ES_NOT_TODO );
        m_date_begin = new Date();
    }

    public Chapter( Diary diary, String name, long date ) {
        super( diary, name, ES_NOT_TODO );
        m_date_begin = new Date( date );
    }

    @Override
    public Type get_type() {
        if( m_date_begin != null )
            if( m_date_begin.is_ordinal() )
                return Type.TOPIC;
        return Type.CHAPTER;
    }

    @Override
    public int get_size() {
        return m_size;
    }

    boolean is_ordinal() {
        return m_date_begin.is_ordinal();
    }

    @Override
    public String getListStr() {
        return( m_date_begin.format_string( false ) + STR_SEPARATOR + m_name );
    }

    @Override
    public String getSubStr() {
        return ( m_date_begin.is_ordinal() ? "Topic" : "Chapter" ) + " with " + m_size + " Entries";
    }

    @Override
    public int get_icon() {
        return( m_date_begin.is_ordinal() ? R.drawable.ic_topic : R.drawable.ic_chapter );
    }

    public boolean get_expanded() {
        return( ( m_status & ES_EXPANDED ) != 0 );
    }

    public void set_expanded( boolean expanded ) {
        set_status_flag( ES_EXPANDED, expanded );
    }

    public void set_date( long date ) {
        m_date_begin.m_date = date;
    }

    void recalculate_span( Chapter next ) {
        if( next == null )
            m_time_span = 0; // unlimited
        else if( next.m_date_begin.is_ordinal() )
            m_time_span = 0; // last temporal chapter: unlimited
        else
            m_time_span = m_date_begin.calculate_days_between( next.m_date_begin );
    }

    public Date get_free_order() {
        Date date = new Date( m_date_begin.m_date );
        Diary.diary.make_free_entry_order( date );
        return date;
    }

    public int get_todo_status()
    {
        return( m_status & ES_FILTER_TODO );
    }

    public void set_todo_status( long s )
    {
        m_status -= ( m_status & ES_FILTER_TODO );
        m_status |= s;
    }

    protected Date m_date_begin;
    int m_time_span = 0;
    protected int m_size = 0;
}
