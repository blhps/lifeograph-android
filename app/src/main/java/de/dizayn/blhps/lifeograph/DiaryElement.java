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

import java.util.Comparator;

public abstract class DiaryElement {
    // FILTERING STATUS
    public final static int FS_CLEAR = 0;
    public final static int FS_FILTER_TEXT = 2;
    public final static int FS_FILTER_FAVORITES = 4;
    public final static int FS_FILTER_TAG = 8;
    public final static int FS_FILTER_DATE = 16;
    public final static int FS_NEW = 1024;

    // DEID
    public final static int DEID_DIARY = 10000; // reserved for Diary
    public final static int DEID_FIRST = 10001; // first sub item in the diary
    public final static int DEID_UNSET = 404; // :)
    public final static int HOME_CURRENT_ELEM = 1; // element shown at startup
    public final static int HOME_LAST_ELEM = 2; // element shown at startup
    public final static int HOME_FIXED_ELEM = 3;

    // SORTING CRITERIA
    public final static char SC_DATE = 'd';
    public final static char SC_SIZE = 's';

    public final static CharSequence STR_SEPARATOR = " - ";

    public static enum Type {
        // CAUTION: order is significant and shouldn't be changed!
        NONE( "" ),
        TAG( Lifeograph.getStr( R.string.tag ) ),
        UNTAGGED( Lifeograph.getStr( R.string.untagged ) ),
        TAG_CTG( Lifeograph.getStr( R.string.tag_ctg ) ),
        CHAPTER_CTG( Lifeograph.getStr( R.string.chapter_ctg ) ),
        FILTER( Lifeograph.getStr( R.string.filter ) ),
        // entry list elements:
        DIARY( Lifeograph.getStr( R.string.diary ) ),
        CHAPTER( Lifeograph.getStr( R.string.chapter ) ),
        TOPIC( Lifeograph.getStr( R.string.topic ) ),
        GROUP( Lifeograph.getStr( R.string.group ) ),
        ENTRY( Lifeograph.getStr( R.string.entry ) ),
        DATE( "" ),
        // virtual types:
        ALL_ENTRIES( Lifeograph.getStr( R.string.all_entries ) );

        public final String str;
        private Type( String v ) {
            this.str = v;
        }
    }

    // ELEMENT STATUSES
    public final static int ES_VOID             = 0x0;
    public final static int ES_EXPANDED         = 0x40;

    public final static int ES_NOT_FAVORED      = 0x100;
    public final static int ES_FAVORED          = 0x200;
    public final static int ES_FILTER_FAVORED   = ES_NOT_FAVORED|ES_FAVORED;
    public final static int ES_NOT_TRASHED      = 0x400;
    public final static int ES_TRASHED          = 0x800;
    public final static int ES_FILTER_TRASHED   = ES_NOT_TRASHED|ES_TRASHED;
    public final static int ES_NOT_TODO         = 0x1000;
    public final static int ES_TODO             = 0x2000;
    public final static int ES_PROGRESSED       = 0x4000;
    public final static int ES_DONE             = 0x8000;
    public final static int ES_CANCELED         = 0x10000;
    public final static int ES_FILTER_TODO      =
            ES_NOT_TODO|ES_TODO|ES_PROGRESSED|ES_DONE|ES_CANCELED;

    public final static int ES_ENTRY_DEFAULT        = ES_NOT_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    public final static int ES_ENTRY_DEFAULT_FAV    = ES_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    public final static int ES_CHAPTER_DEFAULT      = ES_EXPANDED|ES_NOT_TODO;

    // FILTER RELATED CONSTANTS AND ALIASES
    public final static int ES_SHOW_NOT_FAVORED = ES_NOT_FAVORED;
    public final static int ES_SHOW_FAVORED     = ES_FAVORED;
    public final static int ES_SHOW_NOT_TRASHED = ES_NOT_TRASHED;
    public final static int ES_SHOW_TRASHED     = ES_TRASHED;
    public final static int ES_SHOW_NOT_TODO    = ES_NOT_TODO;
    public final static int ES_SHOW_TODO        = ES_TODO;
    public final static int ES_SHOW_PROGRESSED  = ES_PROGRESSED;
    public final static int ES_SHOW_DONE        = ES_DONE;
    public final static int ES_SHOW_CANCELED    = ES_CANCELED;

    public final static int ES_FILTER_TAG           = 0x100000;
    public final static int ES_FILTER_DATE_BEGIN    = 0x200000;
    public final static int ES_FILTER_DATE_END      = 0x400000;
    public final static int ES_FILTER_INDIVIDUAL    = 0x800000;
    public final static int ES_FILTER_OUTSTANDING   = 0x20000000;

    public final static int ES_FILTERED_OUT         = 0x40000000;

    public final static int ES_FILTER_RESET         =
            ES_FILTER_FAVORED|ES_SHOW_NOT_TRASHED|ES_SHOW_NOT_TODO|ES_SHOW_TODO|ES_SHOW_PROGRESSED
                    |ES_FILTER_OUTSTANDING;
    public final static int ES_FILTER_MAX           = 0x7FFFFFFF; // the max for int in Java

    public DiaryElement( Diary diary, String name, int status ) {
        m_ptr2diary = diary;
        m_status = status;
        m_name = name;
        m_id = diary != null ? diary.create_new_id( this ) : DEID_UNSET;
    }

    public DiaryElement( Diary diary, int id, int status ) {
        m_ptr2diary = diary;
        m_status = status;
        m_id = id;
    }

    public int get_id() {
        return m_id;
    }

    abstract public Type get_type();

    public String get_type_name() {
        return get_type().str;
    }

    abstract public int get_size();

    abstract public int get_icon();

    public Date get_date() {
        return new Date( 0 );
    }

    // STRING METHODS
    public String get_name() {
        return m_name;
    }

    abstract public String get_info_str();

    public String get_list_str() {
        return m_name;
    }

    public String getListStrSecondary() {
        return get_info_str();
    }

    public int get_status() {
        return m_status;
    }
    public void set_status( int status ) {
        m_status = status;
    }

    public void set_status_flag( int flag, boolean add ) {
        if( add )
            m_status |= flag;
        else if( ( m_status & flag ) != 0 )
            m_status -= flag;
    }
    // only for entries and chapters:
    public int get_todo_status() {
        return ( m_status & ES_FILTER_TODO );
    }
    public void set_todo_status( int s ) {
        m_status -= ( m_status & ES_FILTER_TODO );
        m_status |= s;
    }

    public boolean is_favored()
    { return false; }

    String m_name;
    Diary m_ptr2diary = null;
    int m_id = 0;
    int m_status;

    static class CompareElemsByName implements Comparator< DiaryElement > {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            return( elem_l.m_name.compareTo( elem_r.m_name ) );
        }
    }

    static class CompareDates implements Comparator< Long > {
        public int compare( Long date_l, Long date_r ) {
            return (int) ( date_r - date_l );
        }
    }

    static class CompareNames implements Comparator< String > {
        public int compare( String strL, String strR ) {
            return( strL.compareToIgnoreCase( strR ) );
        }
    }

    static final CompareElemsByName compare_elems_by_name = new CompareElemsByName();
    static final CompareDates compare_dates = new CompareDates();
    static final CompareNames compare_names = new CompareNames();
}
