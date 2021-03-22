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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import androidx.annotation.NonNull;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class Entry extends DiaryElement {
    Entry( Diary diary, long date, int status ) {
        super( diary, Lifeograph.getStr( R.string.empty_entry ), status );
        m_date = new Date( date );

        java.util.Date jd = new java.util.Date();
        m_date_created = ( int ) ( jd.getTime() / 1000L );
        m_date_edited = m_date_created;
        m_date_status = m_date_created;
    }
    Entry( Diary diary, long date ) {
        this( diary, date, ES_ENTRY_DEFAULT );
    }

    Entry
    get_parent(){
        return( m_p2diary.get_entry_by_date( m_date.get_parent() ) );
    }

    int
    get_child_count() {
        int count = 0;

        if( m_date.get_level() < 3 ) {
            Long date = get_date_t();

            if( date.equals( m_p2diary.m_entries.firstKey() ) ||
                m_p2diary.m_entries.get( date ) == null )
                return 0;

            do {
                date = m_p2diary.m_entries.lowerKey( date );
                if( date == null )
                    break;

                Entry entry = m_p2diary.m_entries.get( date );

                if( Objects.requireNonNull( entry ).get_filtered_out() ) // this may be optional in the future
                    continue;

                if( Date.is_child_of( entry.m_date.m_date, m_date.m_date ) )
                    count++;
                else
                if( !Date.is_descendant_of( entry.m_date.m_date, m_date.m_date ) )
                    break; // all descendants are consecutive
            }
            while( true );
        }

        return count;
    }

    Vector< Entry >
    get_descendants() {
        // NOTE: also returns grand-children
        Vector< Entry > descendants = new Vector<>();

        if( is_ordinal() && m_date.get_level() < 3 ) {
            Long date = get_date_t();

            if( date.equals( m_p2diary.m_entries.firstKey() ) ||
                m_p2diary.m_entries.get( date ) == null )
                return descendants;

            while( true )
            {
                date = m_p2diary.m_entries.lowerKey( date );
                if( date == null )
                    break;

                Entry entry = m_p2diary.m_entries.get( date );

                assert entry != null;
                if( Date.is_descendant_of( entry.m_date.m_date, m_date.m_date ) )
                    descendants.add( entry );
                else
                    break; // all descendants are consecutive
            }
        }

        return descendants;
    }

    int
    get_descendant_depth() {
        int level = m_date.get_level();

        if( level >= 3 ) // covers temporal entries, too
            return 0;

        int count = 0;
        Long date = get_date_t();

        if( date.equals( m_p2diary.m_entries.firstKey() ) ||
            m_p2diary.m_entries.get( date ) == null )
            return 0;

        while( true ) {
            date = m_p2diary.m_entries.lowerKey( date );
            if( date == null )
                break;

            Entry entry = m_p2diary.m_entries.get( date );

            assert entry != null;
            if( Date.is_descendant_of( entry.m_date.m_date, m_date.m_date ) )
            {
                int diff = entry.m_date.get_level() - level;
                if( diff > count )
                    count = diff;
            }
            else
                break; // all descendants are consecutive

            if( ( count + level ) >= 3 )
                break;
        }

        return count;
    }

    @Override
    public Type
    get_type() {
        return Type.ENTRY;
    }

    @Override
    public int
    get_size() {
        int size = 0;
        for( Paragraph para : m_paragraphs )
            size += ( para.get_size() + 1 );

        return( size > 0 ? size - 1 : 0 );
    }

    @Override
    public int
    get_icon() {
        if( ( m_status & ES_FILTER_TODO_PURE ) != 0 )
            return Lifeograph.getTodoIcon( m_status & ES_FILTER_TODO_PURE );
    else
        return( m_unit.isEmpty() ?
                ( m_date.get_order_3rd() == 0 ? R.drawable.ic_entry_parent : R.drawable.ic_entry ) :
                R.drawable.ic_tag );
    }

    @Override
    public Date
    get_date() {
        return m_date;
    }

    void
    set_date( long date ) {
        m_date.m_date = date;
    }

    boolean
    is_ordinal(){
        return( m_date.is_ordinal() );
    }
    boolean
    is_ordinal_hidden() {
        return( m_date.is_hidden() );
    }

    boolean
    has_name() {
        if( m_paragraphs.isEmpty() ) return false;
        return( !( m_paragraphs.get( 0 ).is_empty() ) );
    }

    void
    set_name( String new_name ) {
        if( new_name.isEmpty() )
            m_name = Lifeograph.getStr( R.string.empty_entry );
        else
            m_name = new_name;
    }

    void
    update_name() {
        if( m_paragraphs.isEmpty() )
            m_name = Lifeograph.getStr( R.string.empty_entry );
        else
            m_name = m_paragraphs.get( 0 ).m_text;
    }

    @Override
    public String
    get_title_str() {
        if( m_date.is_hidden() )
            return m_name;
        else
            return( m_date.format_string() + STR_SEPARATOR + m_name );
    }

    @Override
    public String
    get_info_str() {
        if( m_date.is_ordinal() ) {
            return( Lifeograph.getStr( R.string.entry_last_changed_on ) + " " +
                    Date.format_string_d( m_date_edited ) );
        }
        else {
            return m_date.get_weekday_str();
        }
    }

    // TEXTUAL CONTENTS ============================================================================
    boolean
    is_empty() {
        for( Paragraph para : m_paragraphs )
            if( !para.is_empty() )
                return false;

        return true;
    }

    String
    get_text() {
        StringBuilder text = new StringBuilder();

        for( Paragraph para : m_paragraphs ) {
            text.append( para.m_text )
                .append( '\n' );
        }

        if( text.length() > 0 )
            text.deleteCharAt( text.length() - 1 );

        return text.toString();
    }

    void
    clear_text() {
        m_paragraphs.clear();
    }

    void
    set_text( String text ) {
        clear_text();
        digest_text( text );

        update_name();
        update_todo_status();

        if( m_p2diary != null && get_type() != Type.CHAPTER )
            m_p2diary.update_entry_name( this );
    }

    // following is a lambda in C++:
    protected boolean
    calculate_next_para( int pos_bgn, Lifeograph.MutableInt pos_end, String text,
                         Lifeograph.MutableString para_text ) {
        boolean ret_value = true;

        pos_end.v = text.indexOf( '\n', pos_bgn );
        if( pos_end.v == -1 ) {
            pos_end.v = text.length();
            ret_value = false;
        }

        para_text.v = text.substring( pos_bgn, pos_end.v );

        return ret_value;
    }

    void
    insert_text( int pos, final String text ) throws Exception {
        Lifeograph.MutableInt para_offset = new Lifeograph.MutableInt( get_size() );
        Paragraph             para;

        if( text.isEmpty() )
            return;
        else
        if( pos == -1  ) { // append text mode
            para = add_paragraph( "" );
            pos = para_offset.v;
        }
        else
        if( m_paragraphs.isEmpty() )
            para = add_paragraph( "" );
        else {
            para_offset.v = 0;
            para = get_paragraph( pos, para_offset );
            if( para == null )
                throw new Exception( "Text cannot be inserted!" );
        }

        Lifeograph.MutableInt     pos_end      = new Lifeograph.MutableInt();
        Lifeograph.MutableString  para_text    = new Lifeograph.MutableString();
        String                    remnant_text = para.get_substr_after( pos - para_offset.v );

        if( calculate_next_para( 0, pos_end, text, para_text ) ) {
            // split paragraph
            if( !remnant_text.isEmpty() )
                para.erase_text( pos - para_offset.v, remnant_text.length() );

            if( !para_text.v.isEmpty() ) // i.e. the first char of text is not \n
                para.insert_text( pos - para_offset.v, para_text.v );

            while( calculate_next_para( pos_end.v + 1, pos_end, text, para_text ) )
                para = add_paragraph( para_text.v, para.m_para_no + 1 );

            add_paragraph( para_text + remnant_text, para.m_para_no + 1 );
        }
        else
            para.insert_text( pos - para_offset.v, text );

        m_date_edited = ( System.currentTimeMillis() / 1000L );
        update_name();
    }

    void
    erase_text( int pos_bgn, int pos_end ) {
        Lifeograph.MutableInt para_offset_bgn = new Lifeograph.MutableInt();
        Paragraph             para_bgn        = get_paragraph( pos_bgn, para_offset_bgn );

        if( para_bgn == null )
            return;

        // merge paragraphs into the first one, if necessary:
        int para_offset_end = ( para_offset_bgn.v + para_bgn.get_size() + 1 );
        Paragraph para_end = null;

        int num_of_deleted_paras = 0;

        Iterator< Paragraph > iter = m_paragraphs.listIterator( para_bgn.m_para_no + 1 );
        while( iter.hasNext() ) {
            if( para_offset_end <= pos_end )
            {
                para_end = iter.next();
                para_bgn.append( "\n" + para_end.m_text );
                para_offset_end += ( para_end.get_size() + 1 );    // +1 to account for the \n
                num_of_deleted_paras++;
            }
            else
                break;
        }

        // erase the paragraphs merged into the first one:
        if( para_end != null ) {
            for( int i = 0; i < num_of_deleted_paras; i++ )
                m_paragraphs.remove( para_bgn.m_para_no + 1 );

            // fix paragraph nos of subsequent paragraphs
            iter = m_paragraphs.listIterator( para_bgn.m_para_no + 1 );
            while( iter.hasNext() ) {
                Paragraph p = iter.next();
                p.m_para_no -= num_of_deleted_paras;
            }
        }

        // actually erase the text:
        para_bgn.erase_text( pos_bgn - para_offset_bgn.v, pos_end - pos_bgn );

        m_date_edited = System.currentTimeMillis() / 1000L;
    }

    void
    digest_text( String text ) {
        if( text.isEmpty() )
            return;

        int i = 0;
        int pt_bgn = 0, pt_end;
        boolean flag_terminate_loop = false;

        while( true ) {
            pt_end = text.indexOf( '\n', pt_bgn );
            if( pt_end == -1 ) {
                pt_end = text.length();
                flag_terminate_loop = true;
            }

            m_paragraphs.add(
                    new Paragraph( text.substring( pt_bgn, pt_end ), this, i++ ) );

            if( flag_terminate_loop )
                break; // end of while( true )

            pt_bgn = pt_end + 1;
        }
    }

    Paragraph
    get_paragraph( int pos, Lifeograph.MutableInt para_offset ) {
        for( Paragraph p : m_paragraphs ) {
            if( pos <= para_offset.v + p.get_size() ) {
                return p;
            }
            else
                para_offset.v += ( p.get_size() + 1 );  // +1 is for \n
        }

        return null;
    }
    Paragraph
    get_paragraph( int pos ) {
        int para_offset = 0;

        for( Paragraph para : m_paragraphs )
        {
            if( pos <= para_offset + para.get_size() )
                return para;
            else
                para_offset += ( para.get_size() + 1 );  // +1 is for \n
        }

        return null;
    }

    Paragraph
    add_paragraph( String text ) {
        Paragraph para = new Paragraph( text, this, m_paragraphs.size() );
        m_paragraphs.add( para );

        if( m_paragraphs.size() == 1 ) // first paragraph
            m_name = text;

        return para;
    }
    Paragraph
    add_paragraph( String text, int pos ) {
        if( pos > m_paragraphs.size() )
            pos = m_paragraphs.size();

        Paragraph para = new Paragraph( text, this, pos );
        m_paragraphs.add( pos, para );

        for( Paragraph p : m_paragraphs.subList( pos + 1, m_paragraphs.size() ) ) {
            p.m_para_no++;
        }

        if( pos == 0 ) // first paragraph
            m_name = text;

        return para;
    }

    void
    clear_paragraph_data( int pos_b, int pos_e ) {
        int para_offset = 0;

        for( Paragraph para : m_paragraphs ) {
            if( pos_b <= para_offset ) {
                if( pos_e >= ( para_offset + para.get_size() ) ) {
                    para.clear_tags();
                    para.m_date = Date.NOT_SET;
                }
                else
                    break;
            }
            para_offset += ( para.get_size() + 1 );  // +1 is for \n
        }
    }

    String
    get_description() { // returns 2nd non-empty paragraph
        int i = 0;
        for( Paragraph para : m_paragraphs ) {
            if( i > 0 && !para.is_empty() )
                return para.m_text;
            i++;
        }

        return "";
    }

    // FAVOREDNESS =================================================================================
    boolean
    is_favored() {
        return( ( m_status & ES_FAVORED ) != 0 );
    }

    void
    set_favored( boolean favored ) {
        if( favored )
        {
            m_status |= ES_FAVORED;
            m_status &= ( ~ES_NOT_FAVORED );
        }
        else
        {
            m_status |= ES_NOT_FAVORED;
            m_status &= ( ~ES_FAVORED );
        }
    }

    void
    toggle_favored() {
        m_status ^= ES_FILTER_FAVORED;
    }

    // LANGUAGE ====================================================================================
    String
    get_lang_final() {
        return m_option_lang.compareTo( Lifeograph.LANG_INHERIT_DIARY ) == 0 ? m_p2diary.get_lang()
                                                                           : m_option_lang;
    }

    // TRASH FUNCTIONALITY
    boolean
    is_trashed() {
        return( ( m_status & ES_TRASHED ) != 0 );
    }

    void
    set_trashed( boolean trashed ) {
        m_status -= ( m_status & ES_FILTER_TRASHED );
        m_status |= ( trashed ? ES_TRASHED : ES_NOT_TRASHED );
    }

    // TAGS ========================================================================================
    boolean
    has_tag( Entry tag ) {
        for( Paragraph para : m_paragraphs ) {
            if( para.has_tag( tag ) )
                return true;
        }

        return false;
    }
    boolean
    has_tag_broad( Entry tag ) {
        for( Paragraph para : m_paragraphs ) {
            if( para.has_tag_broad( tag ) )
                return true;
        }

        return false;
    }

    Set< String >
    get_tags() {
        Set< String > set = new HashSet<>();

        for( Paragraph para : m_paragraphs )
            set.addAll( para.m_tags.keySet() );

        return set;
    }

    double
    get_value_for_tag( Entry tag, boolean f_average ) {
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }
    double
    get_value_planned_for_tag( Entry tag, boolean f_average ) {
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_planned_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }
    double
    get_value_remaining_for_tag( Entry tag, boolean f_average ) {
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_remaining_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }

    Entry
    get_sub_tag_first( Entry tag ){
        if( tag != null ) {
            for( Paragraph para : m_paragraphs ) {
                Entry sub_tag = para.get_sub_tag_first( tag );
                if( sub_tag != null )
                    return sub_tag;
            }
        }

        return null;
    }
    Entry
    get_sub_tag_last( Entry tag ){
        if( tag != null ) {
            for( int i = ( m_paragraphs.size() - 1 ); i >= 0; i-- ) {
                Entry sub_tag = m_paragraphs.get( i ).get_sub_tag_last( tag );
                if( sub_tag != null )
                    return sub_tag;
            }
        }

        return null;
    }

    Entry
    get_sub_tag_lowest( Entry tag ) {
        Entry sub_tag_lowest = null;

        if( tag != null ) {
            for( Paragraph para : m_paragraphs ) {
                Entry sub_tag = para.get_sub_tag_lowest( tag );
                if( sub_tag != null ) {
                    if( sub_tag_lowest != null ) {
                        if( sub_tag.get_date_t() < sub_tag_lowest.get_date_t() )
                            sub_tag_lowest = sub_tag;
                    }
                    else
                        sub_tag_lowest = sub_tag;
                }
            }
        }

        return sub_tag_lowest;
    }
    Entry
    get_sub_tag_highest( Entry tag ) {
        Entry sub_tag_highest = null;

        if( tag != null ) {
            for( Paragraph para : m_paragraphs ) {
                Entry sub_tag = para.get_sub_tag_highest( tag );
                if( sub_tag != null ) {
                    if( sub_tag_highest != null ) {
                        if( sub_tag.get_date_t() > sub_tag_highest.get_date_t() )
                            sub_tag_highest = sub_tag;
                    }
                    else
                        sub_tag_highest = sub_tag;
                }
            }
        }

        return sub_tag_highest;
    }

    double
    get_value_for_tag( ChartData chart_data ) {
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_for_tag( chart_data, count );

        return( chart_data.is_average() ? value/count.v : value );
    }
    double
    get_value_planned_for_tag( ChartData chart_data ) {
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_planned_for_tag( chart_data, count );

        return( chart_data.is_average() ? value/count.v : value );
    }

    void
    add_tag( Entry tag, double value ) {
        if( is_empty() )
            add_paragraph( "" );

        if( value == 1.0 )
            add_paragraph( ":" + tag.get_name() + ":" );
        else
            add_paragraph( ":" + tag.get_name() + ":=" + value );
    }
    void
    add_tag( Entry tag ) {
        add_tag( tag, 1.0 );
    }

    // THEME =======================================================================================
    Theme
    get_theme() {
        return( m_p2theme != null ? m_p2theme : m_p2diary.get_theme_default() );
    }

    void
    set_theme( Theme theme ){
        m_p2theme = theme;
    }

    boolean
    is_theme_set() {
        return( m_p2theme != null );
    }

    boolean
    has_theme( String name ) {
        return( get_theme().m_name.equals( name ) ); }

    // TO-DO STATUS ================================================================================
    protected static boolean
    is_status_ready( int s ) {
        return( ( s & ES_PROGRESSED ) != 0 || ( ( s & ES_TODO ) != 0 && ( s & ES_DONE ) != 0 ) );
    }

    static int
    calculate_todo_status( String text ) {
        int pos_current = 0;
        int pos_end = text.length();
        char ch;
        char lf = '\t';
        int status = 0;
        int status2add = 0;

        for( ; pos_current < pos_end; ++pos_current )
        {
            ch = text.charAt( pos_current );

            // MARKUP PARSING
            switch( ch ) {
                case '\t':
                    if( lf == ch )
                        lf = '[';
                    else if( lf != '[' )
                        lf = '\t';
                    break;
                case '[':
                    if( lf == ch )
                        lf = 's';
                    else
                        lf = '\t';
                    break;
                case ' ':
                    if( lf == ' ' )
                        status |= status2add;
                    else
                        if( lf == 's' ){ lf = ']'; status2add = ES_TODO; } else lf = '\t';
                    break;
                case '~':
                    if( lf == 's' ){ lf = ']'; status2add = ES_PROGRESSED; } else lf = '\t';
                    break;
                case '+':
                    if( lf == 's' ){ lf = ']'; status2add = ES_DONE; } else lf = '\t';
                    break;
                case 'x':
                case 'X':
                    if( lf == 's' ){ lf = ']'; status2add = ES_CANCELED; } else lf = '\t';
                    break;
                case ']':
                    if( lf == ch )
                        lf = ' ';
                    else
                        lf = '\t';
                    break;
                default:
                    lf = '\t';
                    break;
            }

            if( is_status_ready( status ) )
                break;
        }

        return status;
    }

    protected int
    convert_status( int s ) {
        if( is_status_ready( s ) )
            return( ES_NOT_TODO | ES_PROGRESSED );

        switch( s ) {
            case ES_CANCELED:
                return( ES_NOT_TODO|ES_CANCELED );
            case ES_TODO:
            case ES_TODO|ES_CANCELED:
                return( ES_NOT_TODO|ES_TODO );
            case ES_DONE:
            case ES_DONE|ES_CANCELED:
                return( ES_NOT_TODO|ES_DONE );
            default:
                return ES_NOT_TODO;
        }
    }

    boolean
    update_todo_status() {
        int es = 0;

        for( Paragraph para : m_paragraphs ) {
            es |= calculate_todo_status( para.m_text );

            if( is_status_ready( es ) )
                break;
        }
        es = convert_status( es );

        if( es != get_todo_status() ) {
            set_todo_status( es );
            m_date_status = ( System.currentTimeMillis() / 1000L );
            return true;
        }

        return false;
    }

    double
    get_completion() {
        final double wl = get_workload();

        if( wl == 0.0 )
            return 0.0;

        return( get_completed() / wl );
    }

    double
    get_completed() {
        Entry tag_comp = m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_for_tag( tag_comp, false );
    }

    double
    get_workload() {
        Entry tag_comp = m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_planned_for_tag( tag_comp, false );
    }

    // LOCATION ====================================================================================
    void
    set_location( double lat, double lon ) {
        m_location.latitude = lat;
        m_location.longitude = lon;
    }
    void
    remove_location() {
        m_location.unset();
    }
    boolean
    is_location_set() {
        return m_location.is_set();
    }

    boolean
    is_map_path_set() {
        return( !m_map_path.isEmpty() );
    }
    void
    clear_map_path() {
        m_map_path.clear();
    }

    void
    add_map_path_point( double lat, double lon ) {
        m_map_path.add( new Coords( lat, lon ) );
    }
    void
    remove_last_map_path_point() {
        m_map_path.remove( m_map_path.size() - 1 );
    }

    void
    remove_map_path_point( Coords pt_other ) {
        int i= 0;
        for( Coords pt : m_map_path ) {
            if( pt.is_equal_to( pt_other ) ) {
                m_map_path.remove( i );
                return;
            }
            i++;
        }
    }

    Coords
    get_map_path_end() {
        return m_map_path.get( m_map_path.size() - 1 );
    }

    double
    get_map_path_length() {
        double  dist = 0.0;
        Coords  pt_prev = null;
        boolean flag_after_first = false;

        for( Coords pt : m_map_path ) {
            if( flag_after_first )
                dist += Coords.get_distance( pt_prev, pt );
        else
            flag_after_first = true;

            pt_prev = pt;
        }

        return( Lifeograph.sOptImperialUnits ? dist / Lifeograph.MI_TO_KM_RATIO : dist );
    }

    // SUB CLASSES =================================================================================
    static class Coords
    {
        Coords() {
            latitude = -0.1;
            longitude = -0.1;
        }
        Coords( double lat, double lon ) {
            latitude = lat;
            longitude = lon;
        }

        String
        to_string() {
            return( latitude + ", " + longitude );
        }

        boolean
        is_set() {
            return( latitude != -0.1 );
        }

        void
        unset() {
            latitude = -0.1;
            longitude = -0.1;
        }

        boolean
        is_equal_to( @NonNull Coords pt2 ) {
            return( pt2.latitude == latitude && pt2.longitude == longitude );
        }

        static double
        get_distance( Coords p1, Coords p2 ) {
            final double D      = 6371 * 2;
            final double to_rad = PI/180;
            double       phi1   = p1.latitude * to_rad;
            double       phi2   = p2.latitude * to_rad;
            double       dp     = phi2 - phi1;
            double       dl     = ( p2.longitude - p1.longitude ) * to_rad;
            double       a      = pow( sin( dp / 2 ), 2 ) +
                                  cos( phi1 ) * cos( phi2 ) * pow( sin( dl / 2 ), 2 );

            return( D * atan2( sqrt( a ), sqrt( 1 - a ) ) );
        }

        double latitude;
        double longitude;
    }

    Date m_date;
    long m_date_created;
    long m_date_edited;
    long m_date_status;

    List< Paragraph > m_paragraphs = new ArrayList<>();
    Coords            m_location   = new Coords();
    List< Coords >    m_map_path   = new ArrayList<>();
    Theme  m_p2theme = null;
    String m_unit = "";
    String m_option_lang = Lifeograph.LANG_INHERIT_DIARY; // empty means off
}
