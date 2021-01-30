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

import java.util.Comparator;

public abstract class DiaryElement {
    // DEID
    final static int DEID_MIN           = 10000; // ids have to be greater than this
    final static int DEID_UNSET         = 404;   // :)
    final static int HOME_CURRENT_ENTRY = 1;     // entry shown at startup
    final static int HOME_LAST_ENTRY    = 2;     // entry shown at startup
    // NOTE: when HOME is fixed element, elements ID is used

    final static CharSequence STR_SEPARATOR = " - ";

    // layout type for list view section headers
    enum LayoutType {
        ELEMENT, HEADER_SIMPLE, HEADER_TAG_CTG, HEADER_CHAPTER_CTG
    }

    public enum Type {
        // CAUTION: order is significant and shouldn't be changed!
        NONE( 0, LayoutType.ELEMENT, "" ),
        CHAPTER_CTG( 1, LayoutType.HEADER_CHAPTER_CTG, Lifeograph.getStr( R.string.chapter_ctg ) ),
        THEME( 2, LayoutType.ELEMENT, Lifeograph.getStr( R.string.theme ) ),
        FILTER( 3, LayoutType.ELEMENT, Lifeograph.getStr( R.string.filter ) ),
        CHART( 4, LayoutType.ELEMENT, Lifeograph.getStr( R.string.chart ) ),
        TABLE( 5, LayoutType.ELEMENT, Lifeograph.getStr( R.string.table ) ),
        // entry list elements:
        DIARY( 6, LayoutType.ELEMENT, Lifeograph.getStr( R.string.diary ) ),
        ENTRY( 8, LayoutType.ELEMENT, Lifeograph.getStr( R.string.entry ) ),
        CHAPTER( 9, LayoutType.ELEMENT, Lifeograph.getStr( R.string.chapter ) ),
        DATE( 11, LayoutType.ELEMENT, "" );

        public final String str;
        public final LayoutType layout_type;
        public final int i;

        Type( int order, LayoutType l, String v ) {
            this.i = order;
            this.layout_type = l;
            this.str = v;
        }
    }

    // ELEMENT STATUSES
    final static int ES_VOID             = 0x0;
    final static int ES_EXPANDED         = 0x40;
    final static int ES_NOT_FAVORED      = 0x100;
    final static int ES_FAVORED          = 0x200;
    final static int ES_FILTER_FAVORED   = ES_NOT_FAVORED|ES_FAVORED;
    final static int ES_NOT_TRASHED      = 0x400;
    final static int ES_TRASHED          = 0x800;
    final static int ES_FILTER_TRASHED   = ES_NOT_TRASHED|ES_TRASHED;
    final static int ES_NOT_TODO         = 0x1000;
    // NOTE: NOT_TODO means AUTO when used together with other to do statuses
    final static int ES_TODO             = 0x2000;
    final static int ES_PROGRESSED       = 0x4000;
    final static int ES_DONE             = 0x8000;
    final static int ES_CANCELED         = 0x10000;
    final static int ES_FILTER_TODO      = ES_NOT_TODO|ES_TODO|ES_PROGRESSED|ES_DONE|ES_CANCELED;
    final static int ES_FILTER_TODO_PURE = ES_TODO|ES_PROGRESSED|ES_DONE|ES_CANCELED;
    final static int ES_ENTRY_DEFAULT    = ES_NOT_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    final static int ES_ENTRY_DEFAULT_FAV    = ES_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    final static int ES_CHAPTER_DEFAULT  = ES_EXPANDED|ES_NOT_TODO;

    // FILTER RELATED CONSTANTS AND ALIASES
    final static int ES_SHOW_NOT_FAVORED = ES_NOT_FAVORED;
    final static int ES_SHOW_FAVORED     = ES_FAVORED;
    final static int ES_SHOW_NOT_TRASHED = ES_NOT_TRASHED;
    final static int ES_SHOW_TRASHED     = ES_TRASHED;
    final static int ES_SHOW_NOT_TODO    = ES_NOT_TODO;
    final static int ES_SHOW_TODO        = ES_TODO;
    final static int ES_SHOW_PROGRESSED  = ES_PROGRESSED;
    final static int ES_SHOW_DONE        = ES_DONE;
    final static int ES_SHOW_CANCELED    = ES_CANCELED;
    final static int ES_FILTER_TAG           = 0x100000;
    final static int ES_FILTER_DATE_BEGIN    = 0x200000;
    final static int ES_FILTER_DATE_END      = 0x400000;
    final static int ES_FILTER_INDIVIDUAL    = 0x800000;
    final static int ES_FILTER_OUTSTANDING   = 0x20000000;
    final static int ES_FILTERED_OUT         = 0x40000000;
    final static int ES_FILTER_RESET         =
            ES_FILTER_FAVORED|ES_SHOW_NOT_TRASHED|ES_SHOW_NOT_TODO|ES_SHOW_TODO|ES_SHOW_PROGRESSED
             |ES_FILTER_OUTSTANDING;
    final static int ES_FILTER_MAX           = 0x7FFFFFFF; // the max for int in Java

    public DiaryElement( Diary diary, String name, int status ) {
        m_p2diary = diary;
        m_status = status;
        m_name = name;
        m_id = diary != null ? diary.create_new_id( this ) : DEID_UNSET;
    }

    public DiaryElement( Diary diary, int id, int status ) {
        m_p2diary = diary;
        m_status = status;
        m_id = id;
    }

    int
    get_id() {
        return m_id;
    }
    protected void
    set_id( int id ) {
        m_id = id;
    }

    boolean
    is_equal_to( DiaryElement other ) {
        return( other.m_id == this.m_id );
    }

    boolean
    get_filtered_out() { return( ( m_status & ES_FILTERED_OUT ) != 0 ); }
    void
    set_filtered_out( boolean filteredout ) {
        if( filteredout )
            m_status |= ES_FILTERED_OUT;
        else if( ( m_status & ES_FILTERED_OUT ) != 0 )
            m_status -= ES_FILTERED_OUT;
    }

    abstract public Type get_type();

    public String get_type_name() {
        return get_type().str;
    }

    abstract public int get_size();

    public int get_icon() {
        try {
            throw new Exception( "This function should never called" );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        return 0;
    }

    public Date get_date() {
        return new Date( Date.NOT_APPLICABLE );
    }
    public long get_date_t() {
        return get_date().m_date;
    }

    // STRING METHODS
    String
    get_name() {
        return m_name;
    }

    void
    set_name( String name ) {
        m_name = name;
    }

    public String
    get_title_str() {
        return m_name;
    }
    public String
    get_info_str() {
        return "";
    }
    public String
    get_list_str() {
        return get_title_str();
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
    // only works for entries and chapters:
    public int get_todo_status() {
        return ( m_status & ES_FILTER_TODO );
    }
    public int get_todo_status_effective() {
        final int s = ( m_status & ES_FILTER_TODO_PURE );
        return( s != 0 ? s : ES_NOT_TODO );
    }
    void
    set_todo_status( int s ) {
        m_status -= ( m_status & ES_FILTER_TODO );
        m_status |= s;
    }

    boolean
    get_expanded(){
        return( ( m_status & ES_EXPANDED ) != 0 );
    }
    void
    set_expanded( boolean flag_expanded ) {
        set_status_flag( ES_EXPANDED, flag_expanded );
    }

    String  m_name;
    Diary   m_p2diary;
    int     m_id;
    int     m_status;

    static class CompareElemsByName implements Comparator< DiaryElement > {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            return( elem_l.m_name.compareTo( elem_r.m_name ) );
        }
    }

    static class CompareElemsByDate implements Comparator< DiaryElement > {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            final long diff = ( elem_r.get_date_t() - elem_l.get_date_t() );
            if( diff == 0 )
                return 0;
            else if( diff > 0 )
                return 1;
            else return -1;
        }
    }

    static class CompareDates implements Comparator< Long > {
        public int compare( Long date_l, Long date_r ) {
            final long diff = ( date_r - date_l );
            if( diff == 0 )
                return 0;
            else if( diff > 0 )
                return 1;
            else return -1;
        }
    }

    static class CompareNames implements Comparator< String > {
        public int compare( String strL, String strR ) {
            return( strL.compareToIgnoreCase( strR ) );
        }
    }

    //static final CompareElemsByName compare_elems_by_name = new CompareElemsByName();
    static final CompareElemsByDate compare_elems_by_date = new CompareElemsByDate();
    static final CompareDates compare_dates = new CompareDates();
    static final CompareNames compare_names = new CompareNames();
}
