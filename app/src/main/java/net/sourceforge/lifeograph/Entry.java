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

    protected Entry(long nativePtr) {
        super(nativePtr);
    }

    Entry
    get_parent(){
        long ptr = nativeGetParent(mNativePtr);
        return ptr != 0 ? new Entry(ptr) : null;
    }

    int
    get_child_count() {
        return nativeGetChildCount(mNativePtr);
    }

    Vector< Entry >
    get_descendants() {
        // This might be expensive to create all Java objects
        long[] ptrs = nativeGetDescendants(mNativePtr);
        Vector<Entry> descendants = new Vector<>(ptrs.length);
        for (long ptr : ptrs) descendants.add(new Entry(ptr));
        return descendants;
    }

    int
    get_descendant_depth() {
        return nativeGetDescendantDepth(mNativePtr);
    }

    @Override
    public int
    get_icon() { // Java specific
        if( ( m_status & ES_FILTER_TODO_PURE ) != 0 )
            return Lifeograph.getTodoIcon( m_status & ES_FILTER_TODO_PURE );
        else
            return( m_unit.isEmpty() ?
                    ( m_date.get_order_3rd() == 0 ? R.drawable.ic_entry_parent : R.drawable.ic_entry ) :
                    R.drawable.ic_tag );
    }


    public String
    get_name() {
        if (mNativePtr != 0) {
            return nativeGetName(mNativePtr);
        }
        return super.get_name();
    }

    void
    set_name( String new_name ) {
        if (mNativePtr != 0) {
            // Entries don't have set_name in C++?
            // Actually they update based on first paragraph
        }
        if( new_name.isEmpty() )
            m_name = Lifeograph.getStr( R.string.empty_entry );
        else
            m_name = new_name;
    }

    void
    update_name() {
        if (mNativePtr != 0) {
            nativeUpdateName(mNativePtr);
            return;
        }
        if( m_paragraphs.isEmpty() )
            m_name = Lifeograph.getStr( R.string.empty_entry );
        else
            m_name = m_paragraphs.get( 0 ).m_text;
    }

    @Override
    public String
    get_title_str() {
        if (mNativePtr != 0) {
            // TODO: implement in C++ or reconstruct here
        }
        if( m_date.is_hidden() )
            return m_name;
        else
            return( m_date.format_string() + STR_SEPARATOR + m_name );
    }

    @Override
    public String
    get_info_str() {
        if (mNativePtr != 0) {
            // TODO: delegate to C++
        }
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
        return nativeIsEmpty(mNativePtr);
    }

    String
    get_text() {
        return nativeGetText(mNativePtr);
    }

    void
    clear_text() {
        nativeClearText(mNativePtr);
    }

    void
    set_text( String text ) {
        nativeSetText(mNativePtr, text);
    }

    void
    insert_text( int pos, final String text ) throws Exception {
        nativeInsertText(mNativePtr, pos, text);
    }

    void
    erase_text( int pos_bgn, int pos_end ) {
        nativeEraseText(mNativePtr, pos_bgn, pos_end);
    }

    Paragraph
    get_paragraph( int pos, Lifeograph.MutableInt para_offset ) {
        if (mNativePtr != 0) {
            // TODO
        }
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
        if (mNativePtr != 0) {
            long ptr = nativeGetParagraphAtPos(mNativePtr, pos);
            return ptr != 0 ? new Paragraph(ptr) : null;
        }
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
        if (mNativePtr != 0) {
            // long ptr = nativeAddParagraph(mNativePtr, text);
            // return new Paragraph(ptr);
        }
        Paragraph para = new Paragraph( text, this, m_paragraphs.size() );
        m_paragraphs.add( para );

        if( m_paragraphs.size() == 1 ) // first paragraph
            m_name = text;

        return para;
    }
    Paragraph
    add_paragraph( String text, int pos ) {
        if (mNativePtr != 0) {
            // TODO
        }
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
        if (mNativePtr != 0) {
            // nativeClearParagraphData(mNativePtr, pos_b, pos_e);
            return;
        }
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
        return nativeGetDescription(mNativePtr);
    }

    // FAVOREDNESS =================================================================================
    boolean
    is_favored() {
        return nativeIsFavorite(mNativePtr);
    }

    void
    set_favored( boolean favored ) {
        if (mNativePtr != 0) {
            // nativeSetFavorite(mNativePtr, favored);
            return;
        }
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
        nativeToggleFavorite(mNativePtr);
    }

    // LANGUAGE ====================================================================================
    String
    get_lang_final() {
        if (mNativePtr != 0) {
            return nativeGetLangFinal(mNativePtr);
        }
        return m_option_lang.compareTo( Lifeograph.LANG_INHERIT_DIARY ) == 0 ? m_p2diary.get_lang()
                                                                           : m_option_lang;
    }

    // TRASH FUNCTIONALITY
    boolean
    is_trashed() {
        return nativeIsTrashed(mNativePtr);
    }

    void
    set_trashed( boolean trashed ) {
        nativeSetTrashed(mNativePtr, trashed);
    }

    // TAGS ========================================================================================
    boolean
    has_tag( Entry tag ) {
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            return nativeHasTag(mNativePtr, tag.mNativePtr);
        }
        for( Paragraph para : m_paragraphs ) {
            if( para.has_tag( tag ) )
                return true;
        }

        return false;
    }

    Set< String >
    get_tags() {
        if (mNativePtr != 0) {
            // TODO: delegate to C++
        }
        Set< String > set = new HashSet<>();

        for( Paragraph para : m_paragraphs )
            set.addAll( para.m_tags.keySet() );

        return set;
    }

    double
    get_value_for_tag( Entry tag, boolean f_average ) {
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            return nativeGetTagValue(mNativePtr, tag.mNativePtr, f_average);
        }
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }
    double
    get_value_planned_for_tag( Entry tag, boolean f_average ) {
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            return nativeGetTagValuePlanned(mNativePtr, tag.mNativePtr, f_average);
        }
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_planned_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }
    double
    get_value_remaining_for_tag( Entry tag, boolean f_average ) {
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            return nativeGetTagValueRemaining(mNativePtr, tag.mNativePtr, f_average);
        }
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_remaining_for_tag( tag, count );

        return( f_average ? value/count.v : value );
    }

    Entry
    get_sub_tag_first( Entry tag ){
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            long ptr = nativeGetSubTagFirst(mNativePtr, tag.mNativePtr);
            return ptr != 0 ? new Entry(ptr) : null;
        }
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
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            long ptr = nativeGetSubTagLast(mNativePtr, tag.mNativePtr);
            return ptr != 0 ? new Entry(ptr) : null;
        }
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
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            long ptr = nativeGetSubTagLowest(mNativePtr, tag.mNativePtr);
            return ptr != 0 ? new Entry(ptr) : null;
        }
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
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            long ptr = nativeGetSubTagHighest(mNativePtr, tag.mNativePtr);
            return ptr != 0 ? new Entry(ptr) : null;
        }
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
        // TODO: delegate to C++
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_for_tag( chart_data, count );

        return( chart_data.is_average() ? value/count.v : value );
    }
    double
    get_value_planned_for_tag( ChartData chart_data ) {
        // TODO: delegate to C++
        double                value = 0.0;
        Lifeograph.MutableInt count = new Lifeograph.MutableInt();

        for( Paragraph para : m_paragraphs )
            value += para.get_value_planned_for_tag( chart_data, count );

        return( chart_data.is_average() ? value/count.v : value );
    }

    void
    add_tag( Entry tag, double value ) {
        if (mNativePtr != 0 && tag.mNativePtr != 0) {
            nativeAddTag(mNativePtr, tag.mNativePtr, value);
            return;
        }
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
        if (mNativePtr != 0) {
            // TODO
        }
        return( m_p2theme != null ? m_p2theme : m_p2diary.get_theme_default() );
    }

    void
    set_theme( Theme theme ){
        if (mNativePtr != 0) {
            // TODO
        }
        m_p2theme = theme;
    }

    boolean
    is_theme_set() {
        if (mNativePtr != 0) {
            return nativeIsThemeSet(mNativePtr);
        }
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
        if (mNativePtr != 0) {
            return nativeUpdateTodoStatus(mNativePtr);
        }
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
        if (mNativePtr != 0) {
            return nativeGetCompletion(mNativePtr);
        }
        final double wl = get_workload();

        if( wl == 0.0 )
            return 0.0;

        return( get_completed() / wl );
    }

    double
    get_completed() {
        if (mNativePtr != 0) {
            return nativeGetCompleted(mNativePtr);
        }
        Entry tag_comp = m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_for_tag( tag_comp, false );
    }

    double
    get_workload() {
        if (mNativePtr != 0) {
            return nativeGetWorkload(mNativePtr);
        }
        Entry tag_comp = m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_planned_for_tag( tag_comp, false );
    }

    // LOCATION ====================================================================================
    void
    set_location( double lat, double lon ) {
        if (mNativePtr != 0) {
            // TODO
        }
        m_location.latitude = lat;
        m_location.longitude = lon;
    }
    void
    remove_location() {
        if (mNativePtr != 0) {
            // TODO
        }
        m_location.unset();
    }
    boolean
    is_location_set() {
        if (mNativePtr != 0) {
            return nativeHasLocation(mNativePtr);
        }
        return m_location.is_set();
    }

    boolean
    is_map_path_set() {
        if (mNativePtr != 0) {
            return !nativeGetMapPath(mNativePtr).isEmpty();
        }
        return( !m_map_path.isEmpty() );
    }
    void
    clear_map_path() {
        nativeClearMapPath(mNativePtr);
    }

    void
    add_map_path_point( double lat, double lon ) {
        if (mNativePtr != 0) {
            // TODO
        }
        m_map_path.add( new Coords( lat, lon ) );
    }
    void
    remove_last_map_path_point() {
        if (mNativePtr != 0) {
            // TODO
        }
        m_map_path.remove( m_map_path.size() - 1 );
    }

    void
    remove_map_path_point( Coords pt_other ) {
        if (mNativePtr != 0) {
            // TODO
        }
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
        if (mNativePtr != 0) {
            // TODO
        }
        return m_map_path.get( m_map_path.size() - 1 );
    }

    double
    get_map_path_length() {
        return nativeGetMapPathLength(mNativePtr);
    }

    // SUB CLASSES =================================================================================
    public static class Coords
    {
        public Coords() {
            latitude = -0.1;
            longitude = -0.1;
        }
        public Coords( double lat, double lon ) {
            latitude = lat;
            longitude = lon;
        }

        public String
        to_string() {
            return( latitude + ", " + longitude );
        }

        public boolean
        is_set() {
            return( latitude != -0.1 );
        }

        public void
        unset() {
            latitude = -0.1;
            longitude = -0.1;
        }

        public boolean
        is_equal_to( @NonNull Coords pt2 ) {
            return( pt2.latitude == latitude && pt2.longitude == longitude );
        }

        public static double
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

        public double latitude;
        public double longitude;
    }

    private native long nativeGetId(long ptr);
    private native long nativeGetParent(long ptr);
    private native int nativeGetChildCount(long ptr);
    private native long[] nativeGetDescendants(long ptr);
    private native int nativeGetDescendantDepth(long ptr);
    private native int nativeGetSize(long ptr);
    private native long nativeGetDate(long ptr);
    private native void nativeSetDate(long ptr, long date);
    private native boolean nativeHasName(long ptr);
    private native String nativeGetName(long ptr);
    private native void nativeUpdateName(long ptr);
    private native boolean nativeIsEmpty(long ptr);
    private native String nativeGetText(long ptr);
    private native void nativeClearText(long ptr);
    private native void nativeSetText(long ptr, String text);
    private native void nativeInsertText(long ptr, int pos, String text);
    private native void nativeEraseText(long ptr, int pos_bgn, int pos_end);
    private native long nativeGetParagraphAtPos(long ptr, int pos);
    private native String nativeGetDescription(long ptr);
    private native boolean nativeIsFavorite(long ptr);
    private native void nativeToggleFavorite(long ptr);
    private native String nativeGetLangFinal(long ptr);
    private native boolean nativeIsTrashed(long ptr);
    private native void nativeSetTrashed(long ptr, boolean trashed);
    private native boolean nativeHasTag(long ptr, long tagPtr);
    private native boolean nativeHasTagBroad(long ptr, long tagPtr);
    private native double nativeGetTagValue(long ptr, long tagPtr, boolean average);
    private native double nativeGetTagValuePlanned(long ptr, long tagPtr, boolean average);
    private native double nativeGetTagValueRemaining(long ptr, long tagPtr, boolean average);
    private native long nativeGetSubTagFirst(long ptr, long tagPtr);
    private native long nativeGetSubTagLast(long ptr, long tagPtr);
    private native long nativeGetSubTagLowest(long ptr, long tagPtr);
    private native long nativeGetSubTagHighest(long ptr, long tagPtr);
    private native void nativeAddTag(long ptr, long tagPtr, double value);
    private native boolean nativeIsThemeSet(long ptr);
    private native boolean nativeUpdateTodoStatus(long ptr);
    private native double nativeGetCompletion(long ptr);
    private native double nativeGetCompleted(long ptr);
    private native double nativeGetWorkload(long ptr);
    private native boolean nativeHasLocation(long ptr);
    private native List<Coords> nativeGetMapPath(long ptr);
    private native void nativeClearMapPath(long ptr);
    private native double nativeGetMapPathLength(long ptr);

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
