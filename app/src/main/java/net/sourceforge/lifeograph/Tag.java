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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Tag extends DiaryElementChart {

    public static class Category extends DiaryElement
    {
        public Category( Diary diary, String name ) {
            super( diary, name, ES_EXPANDED );
            m_name = name;
        }

        @Override
        public Type get_type() {
            return Type.TAG_CTG;
        }

        @Override
        public int get_size() {
            return mTags.size();
        }

        @Override
        public int get_icon() {
            return R.mipmap.ic_tag;
        }

        @Override
        public String get_info_str() {
            return( mTags.size() + " entries" );
        }

        public boolean get_expanded() {
            return( ( m_status & ES_EXPANDED ) != 0 );
        }
        public void set_expanded( boolean expanded ) {
            set_status_flag( ES_EXPANDED, expanded );
        }

        public void add( Tag tag ) {
            mTags.add( tag );
        }

        public void remove( Tag tag ) {
            mTags.remove( tag );
        }

        // CONTENTS
        java.util.List< Tag > mTags = new ArrayList<>();
    }

//    public Tag( Diary diary ) {
//        super( diary, ES_VOID, "" );
//    }

    public Tag( Diary diary, String name, Category ctg, int chart_type ) {
        super( diary, name, ES_VOID, chart_type );
        m_ptr2category = ctg;
        if( ctg != null )
            ctg.add( this );
    }

    public Tag( Diary diary, String name, Category ctg ) {
        this( diary, name, ctg, DEFAULT_CHART_TYPE );
    }

    @Override
    public Type get_type() {
        return Type.TAG;
    }

    @Override
    public int get_size() {
        return mEntries.size();
    }

    @Override
    public int get_icon() {
        return( get_has_own_theme() ? R.mipmap.ic_theme_tag : R.mipmap.ic_tag );
    }

    @Override
    public String get_info_str() {
        return( get_size() + " entries" );
    }

    @Override
    public String getListStrSecondary() {
        return( "Tag with " + get_size() + " entries" );
    }

    static String escape_name( String name ) {
        StringBuilder result = new StringBuilder();
        char c;

        for( int i = 0; i < name.length(); i++ ) {
            c = name.charAt( i );
            if( c == '=' || c == '\\' )
                result.append( '\\' );
            result.append( c );
        }

        return result.toString();
    }

    public String get_name_and_value( Entry entry, boolean flag_escape, boolean flag_unit ) {
        StringBuilder result = new StringBuilder( flag_escape ? escape_name( m_name ) : m_name );

        if( !is_boolean() ) {
            result.append( " = " ).append( get_value( entry ) );

            // addressing Java-shortcomings
            if( result.toString().endsWith( ".0" ) || result.toString().endsWith( ",0" ) )
                result.delete( result.length() - 2, result.length() );

            if( flag_unit && !m_unit.isEmpty() )
                result.append( " " ).append( m_unit );
        }

        return result.toString();
    }
    public Category get_category() {
        return m_ptr2category;
    }

    public void set_category( Category ctg ) {
        if( m_ptr2category != null )
            m_ptr2category.remove( this );
        if( ctg != null )
            ctg.add( this );
        m_ptr2category = ctg;
    }

    public void add_entry( Entry entry ) {
        add_entry( entry, 1.0 );
    }
    public void add_entry( Entry entry, Double value ) {
        mEntries.put( entry, value );
    }

    public void remove_entry( Entry entry ) {
        mEntries.remove( entry );
    }

    // THEMES
    public Theme get_theme()
    {
        return( m_theme != null ? m_theme : Theme.System.get() );
    }

    public boolean get_has_own_theme()
    {
        return( m_theme != null );
    }

    public Theme get_own_theme() {
        if( m_theme == null ) {
            m_theme = new Theme();

            for( Entry entry : mEntries.keySet() )
                entry.update_theme();
        }

        return m_theme;
    }

    public Theme create_own_theme_duplicating( Theme theme ) {
        m_theme = new Theme( theme );

        for( Entry entry : mEntries.keySet() )
            entry.update_theme();

        return m_theme;
    }

    public void reset_theme() {
        if( m_theme != null ) {
            m_theme = null;

            for( Entry entry : mEntries.keySet() )
                entry.update_theme();
        }
    }

    // PARAMETRIC TAG SYSTEM PROPERTIES
    double get_value( Entry entry ) {
        if( mEntries.containsKey( entry ) )
            return mEntries.get( entry );
        else
            return -404;
    }

    double get_combined_value() {
        double grand = 0;
        for( double value : mEntries.values() )
            grand += value;

        return( ( m_chart_type & ChartData.AVERAGE ) != 0 ? grand / mEntries.size() : grand );
    }
    String get_combined_value_str() {
        return( get_combined_value() + " " + m_unit );
    }

    boolean is_boolean() {
        return ( ( m_chart_type & ChartData.VALUE_TYPE_MASK ) == ChartData.BOOLEAN );
    }

    String get_unit() {
        return m_unit;
    }

    void set_unit( String unit ) {
        m_unit = unit;
    }

    @Override
    ChartData create_chart_data() {
        if( mEntries.isEmpty() )
            return null;

        ChartData cp = new ChartData( m_chart_type );
        if( ! is_boolean() )
            cp.unit = m_unit;

        // order from old to new: d/v_before > d/v_last > d/v
        Date d_before = new Date( Date.NOT_SET );
        Date d_last = new Date( Date.NOT_SET );
        Date d = new Date( Date.NOT_SET );
        double v_before = 0, v_last = 0, v = 0;
        int no_of_entries = 0;

        // LAMBDA: auto add_value = [ & ]() -> ... see below

        for( Map.Entry< Entry, Double > iter : mEntries.descendingMap().entrySet() ) {
            d = iter.getKey().get_date();

            if( d.is_ordinal() )
                break;

            if( cp.start_date == 0 )
                cp.start_date = d.m_date;
            if( ! d_last.is_set() )
                d_last = d;

            v = is_boolean() ? 1.0 : iter.getValue();

            if( cp.calculate_distance( d, d_last ) > 0 )
            // add_value() = due to lack of lambdas:
            {
                boolean flag_sustain = ( m_chart_type & ChartData.VALUE_TYPE_MASK ) ==
                                       ChartData.AVERAGE;
                if( flag_sustain && no_of_entries > 1 )
                    v_last /= no_of_entries;

                if( cp.values.isEmpty() ) // first value is being entered i.e. v_before is not set
                    cp.add( 0, false, 0.0, v_last );
                else
                    cp.add( cp.calculate_distance( d_last,  d_before ),
                            flag_sustain, v_before, v_last );

                v_before = v_last;
                v_last = v;
                d_before = d_last;
                d_last = d;
                no_of_entries = 1;
            }
            else {
                v_last += v;
                no_of_entries++;
            }
        }

        //add_value() = due to lack of lambdas:
        {
            boolean flag_sustain = ( m_chart_type & ChartData.VALUE_TYPE_MASK ) ==
                                   ChartData.AVERAGE;
            if( flag_sustain && no_of_entries > 1 )
                v_last /= no_of_entries;

            if( cp.values.isEmpty() ) // first value is being entered i.e. v_before is not set
                cp.add( 0, false, 0.0, v_last );
            else
                cp.add( cp.calculate_distance( d_last,  d_before ),
                        flag_sustain, v_before, v_last );

            // NOTE: last assignments in lambda were not necessary here
        }

        //TODO: Diary.d.fill_up_chart_points( cp );

        return cp;
    }

    // MEMBER VARIABLES
    Category m_ptr2category;
    java.util.TreeMap< Entry, Double > mEntries =
            new TreeMap< Entry, Double >( DiaryElement.compare_elems_by_date );
    private Theme m_theme = null;
    String m_unit = "";
}
