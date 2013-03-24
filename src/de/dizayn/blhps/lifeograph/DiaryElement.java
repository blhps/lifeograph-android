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
    public final static long DEID_MIN = 10000; // reserved for Diary itself
    public final static long DEID_UNSET = 404; // :)
    public final static long HOME_CURRENT_ELEM = 1; // element shown at startup
    public final static long HOME_LAST_ELEM = 2; // element shown at startup
    public final static long HOME_FIXED_ELEM = 3;

    // SORTING CRITERIA
    public final static char SC_DATE = 'd';
    public final static char SC_SIZE = 's';

    public final static CharSequence STR_SEPARATOR = " - ";

    public static enum Type {
        // CAUTION: order is significant and shouldn't be changed!
        NONE, TAG, TAG_CTG, THEME, CHAPTER_CTG,
        // entry list elements:
        DIARY, CHAPTER, TOPIC, ENTRY, DATE,
        // additional (virtual) types:
        ALLBYDATE, ALLBYSIZE;
    }

    public DiaryElement( Diary diary, String name ) {
        m_diary = diary;
        m_name = name;
        m_id = diary != null ? diary.create_new_id( this ) : DEID_UNSET;
    }

    public String get_name() {
        return m_name;
    }

    abstract public String getSubStr();

    public String getListStr() {
        return m_name;
    }

    public String getListStrSecondary() {
        return getSubStr();
    }

    abstract public int get_icon();

    public long get_id() {
        return m_id;
    }

    abstract public Type get_type();

    abstract public int get_size();

    protected String m_name;
    protected Diary m_diary = null;
    protected long m_id = 0;

    static class CompareElemsByName implements Comparator< DiaryElement > {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            return( elem_l.m_name.compareTo( elem_r.m_name ) );
        }
    };

    static class CompareDates implements Comparator< Long > {
        public int compare( Long date_l, Long date_r ) {
            return ( int ) ( date_r - date_l );
        }
    };

    static class CompareNames implements Comparator< String > {
        public int compare( String strL, String strR ) {
            return( strL.compareToIgnoreCase( strR ) );
        }
    };

    static public final CompareElemsByName compare_elems_by_name = new CompareElemsByName();
    static public final CompareDates compare_dates = new CompareDates();
    static public final CompareNames compare_names = new CompareNames();
}
