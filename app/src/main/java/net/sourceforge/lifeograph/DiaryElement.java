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

public class DiaryElement {
    // DEID
    public final static long DEID_UNSET         = 404L;   // :)
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
    public final static int ES_VOID             = 0x0;
    public final static int ES_EXPANDED         = 0x40;
    public final static int ES_NOT_FAVORED      = 0x100;
    public final static int ES_FAVORED          = 0x200;
    public final static int ES_FILTER_FAVORED   = ES_NOT_FAVORED|ES_FAVORED;
    public final static int ES_NOT_TRASHED      = 0x400;
    public final static int ES_TRASHED          = 0x800;
    public final static int ES_FILTER_TRASHED   = ES_NOT_TRASHED|ES_TRASHED;
    public final static int ES_NOT_TODO         = 0x1000;
    // NOTE: NOT_TODO means AUTO when used together with other to do statuses
    public final static int ES_TODO             = 0x2000;
    public final static int ES_PROGRESSED       = 0x4000;
    public final static int ES_DONE             = 0x8000;
    public final static int ES_CANCELED         = 0x10000;
    public final static int ES_FILTER_TODO      = ES_NOT_TODO|ES_TODO|ES_PROGRESSED|ES_DONE|ES_CANCELED;
    public final static int ES_FILTER_TODO_PURE = ES_TODO|ES_PROGRESSED|ES_DONE|ES_CANCELED;
    public final static int ES_ENTRY_DEFAULT    = ES_NOT_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    public final static int ES_ENTRY_DEFAULT_FAV    = ES_FAVORED|ES_NOT_TRASHED|ES_NOT_TODO;
    public final static int ES_CHAPTER_DEFAULT  = ES_EXPANDED|ES_NOT_TODO;

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
    public final static int ES_FILTER_OUT           = 0x40000000;
    public final static int ES_FILTER_RESET         =
            ES_FILTER_FAVORED|ES_SHOW_NOT_TRASHED|ES_SHOW_NOT_TODO|ES_SHOW_TODO|ES_SHOW_PROGRESSED
             |ES_FILTER_OUTSTANDING;
    public final static int ES_FILTER_MAX           = 0x7FFFFFFF; // the max for int in Java

//    public DiaryElement( Diary diary, String name, int status ) {
//        m_p2diary = diary;
//        m_status = status;
//        m_name = name;
//        m_id = diary != null ? diary.create_new_id( this ) : DEID_UNSET;
//    }
//
//    public DiaryElement( Diary diary, long id, int status ) {
//        m_p2diary = diary;
//        m_status = status;
//        m_id = id;
//    }

    protected DiaryElement(long nativePtr) {
        mNativePtr = nativePtr;
    }

    public int
    get_id() {
        return nativeGetId(mNativePtr);
    }

    boolean
    is_equal_to( DiaryElement other ) {
        return mNativePtr == other.mNativePtr;
    }

    public Type
    get_type() {
        return nativeGetType(mNativePtr);
    }
//    public String get_type_name() {
//        return get_type().str;
//    }

    public int get_size() {
        return nativeGetSize(mNativePtr);
    }

    public int get_icon() {
        try {
            throw new Exception( "This function should never called" );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        return 0;
    }

    // STRING METHODS
    public String
    get_name() {
        return nativeGetName(mNativePtr);
    }

    public void
    set_name( String name ) {
        nativeSetName(mNativePtr, name);
    }

    public String // TODO: stub...
    get_list_str() {
        return get_name();
    }
    public String
    get_info_str() {
        return "";
    }

//    public int get_status() {
//        return nativeGetStatus(mNativePtr);
//    }
//    public void set_status( int status ) {
//        nativeSetStatus(mNativePtr, status);
//    }

    public long mNativePtr;

    static class CompareElemsByName implements Comparator< DiaryElement > {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            return( elem_l.get_name().compareTo( elem_r.get_name() ) );
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

    public static final CompareDates compare_dates = new CompareDates();
    public static final CompareNames compare_names = new CompareNames();

    // NATIVE METHODS ==============================================================================
    private native int nativeGetId(long ptr);
    private native String nativeGetName(long ptr);
    private native void nativeSetName(long ptr, String name);
    private native Type nativeGetType(long ptr);
    private native int nativeGetSize(long ptr);
}
