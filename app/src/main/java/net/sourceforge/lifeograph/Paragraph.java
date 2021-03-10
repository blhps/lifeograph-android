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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import androidx.annotation.NonNull;

public class Paragraph
{
    final static char JT_LEFT = '<';
    final static char JT_CENTER = '|';
    final static char JT_RIGHT = '>';

    Paragraph( String text, Entry host, int index ) {
        m_host = host;
        m_para_no = index;
        m_text = text;

        update_per_text();
    }

    Paragraph
    get_prev(){
        if( m_host == null || m_para_no == 0 )
            return null;

        return( m_host.m_paragraphs.get( m_para_no - 1 ) );
    }

    Paragraph
    get_next(){
        if( m_host == null )
            return null;

        if( m_para_no == ( m_host.m_paragraphs.size() - 1 ) )
            return null;

        return( m_host.m_paragraphs.get( m_para_no + 1 ) );
    }

    Paragraph
    get_parent() {
        if( m_host == null || m_para_no == 0 )
            return null;

        for( int i = m_para_no; i > 0; i-- )
        {
            Paragraph previous = m_host.m_paragraphs.get( i );

            if( previous.m_heading_level > this.m_heading_level )
                return previous;
            else
            if( previous.m_indentation_level < this.m_indentation_level && !previous.is_empty() )
                return previous;

            if( previous.m_para_no == 0 )
                return null;
        }

        try {
            throw new Exception( "Unexpected point reached while searching for parent para" );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean is_empty(){
        return m_text.isEmpty();
    }
    public char get_char( int i ){
        return m_text.charAt( i );
    }

    String
    get_substr_after( int i ) {
        return m_text.substring( i );
    }

    public int get_size(){
        return m_text.length();
    }

    public void set_text( String text ){
        m_text = text;
        update_per_text();
    }
    public void append( String text ){
        m_text += text;
        update_per_text();
    }
    public void insert_text( int pos, String text ){
        StringBuilder str_bld = new StringBuilder( m_text );
        str_bld.insert( pos, text );
        m_text = str_bld.toString();
        update_per_text();
    }
    public void erase_text( int pos, int size ) {
        StringBuilder str_bld = new StringBuilder( m_text );
        str_bld.delete( pos, pos + size );
        m_text = str_bld.toString();
        update_per_text();
    }
    public void replace_text( int pos, int size, String text ) {
        StringBuilder str_bld = new StringBuilder( m_text );
        str_bld.delete( pos, pos + size );
        str_bld.insert( pos, text );
        m_text = str_bld.toString();
        update_per_text();
    }

    public void clear_tags() {
        m_tags.clear();
        m_tags_planned.clear();
        m_tags_in_order.clear();
    }

    public void
    set_tag( String tag, double value ) {
        m_tags.put( tag, value );
        m_tags_in_order.add( tag );
    }
    public void
    set_tag( String tag, double v_real, double v_planned ) {
        m_tags.put( tag, v_real );
        m_tags_planned.put( tag, v_planned );
        m_tags_in_order.add( tag );
    }

    public boolean
    has_tag( Entry tag ) {
        return( m_tags.containsKey( tag.get_name() ) );
    }

    public boolean
    has_tag_planned( Entry tag ) {
        return( m_tags_planned.containsKey( tag.get_name() ) );
    }

    boolean
    has_tag_broad( Entry tag ) /* in broad sense*/ {
        if( m_tags.containsKey( tag.get_name() ) )
            return true;

        for( Entry child_tag : tag.get_descendants() ) {
            if( has_tag_broad( child_tag ) ) // recursion
                return true;
        }

        Paragraph parent_para = get_parent();
        if( parent_para != null )
            return parent_para.has_tag_broad( tag ); // recursion
        else
            return false;
    }

    double
    get_value_for_tag( Entry tag, Lifeograph.MutableInt count ) {
        if( !has_tag( tag ) )
            return 0.0;
        else {
            count.v++;
            //noinspection ConstantConditions
            return m_tags.get( tag.get_name() );
        }
    }

    double
    get_value_planned_for_tag( Entry tag, Lifeograph.MutableInt count ) {
        if( !has_tag_planned( tag ) )
            return 0.0;
        else {
            count.v++;
            //noinspection ConstantConditions
            return m_tags_planned.get( tag.m_name );
        }
    }

    double
    get_value_remaining_for_tag( Entry tag, Lifeograph.MutableInt count ) {
        if( !has_tag_planned( tag ) )
            return 0.0;
        else {
            count.v++;
            //noinspection ConstantConditions
            return( m_tags_planned.get( tag.get_name() ) - m_tags.get( tag.get_name() ) );
        }
    }

    double
    get_value_for_tag( @NonNull ChartData chart_data, Lifeograph.MutableInt count ) {
        if( chart_data.para_filter_tag != null && !has_tag_broad( chart_data.para_filter_tag ) )
            return 0.0;
        else
        if( !has_tag( chart_data.tag ) )
            return 0.0;
        else {
            count.v++;
            //noinspection ConstantConditions
            return m_tags.get( chart_data.tag.get_name() );
        }
    }

    double
    get_value_planned_for_tag( @NonNull ChartData chart_data, Lifeograph.MutableInt count ) {
        if( !has_tag_broad( chart_data.para_filter_tag ) )
            return 0.0;
        else
        if( !has_tag_planned( chart_data.tag ) )
            return 0.0;
        else {
            count.v++;
            //noinspection ConstantConditions
            return m_tags_planned.get( chart_data.tag.get_name() );
        }
    }

    // return which sub tag of a parent tag is present in the map
    Entry
    get_sub_tag_first( Entry parent_tag ) {
        for( String tag_name : m_tags_in_order ) {
            Entry tag = m_host.m_p2diary.get_entry_by_name( tag_name );

            if( tag != null && Date.is_descendant_of( tag.get_date_t(), parent_tag.get_date_t() ) )
                return tag;
        }

        return null;
    }

    Entry
    get_sub_tag_last( Entry parent_tag ) {
        for( int i = ( m_tags_in_order.size() - 1 ); i >= 0; i-- ) {
            Entry tag = m_host.m_p2diary.get_entry_by_name( m_tags_in_order.get( i ) );

            if( tag != null && Date.is_descendant_of( tag.get_date_t(), parent_tag.get_date_t() ) )
                return tag;
        }

        return null;
    }

    Entry
    get_sub_tag_lowest( Entry parent ) {
        Entry sub_tag_lowest = null;

        if( parent != null ) {
            for( String tag_name : m_tags_in_order ) {
                Entry sub_tag = m_host.m_p2diary.get_entry_by_name( tag_name );
                if( sub_tag != null && Date.is_descendant_of( sub_tag.get_date_t(),
                                                              parent.get_date_t() ) ) {
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
    get_sub_tag_highest( Entry parent ) {
        Entry sub_tag_highest = null;

        if( parent != null ) {
            for( String tag_name : m_tags_in_order )
            {
                Entry sub_tag = m_host.m_p2diary.get_entry_by_name( tag_name );
                if( sub_tag != null && Date.is_descendant_of( sub_tag.get_date_t(),
                                                              parent.get_date_t() ) ) {
                    if( sub_tag_highest != null )
                    {
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
    get_completion() {
        if( m_host == null ) return 0.0;

        final double wl = get_workload();

        if( wl == 0.0 )
            return 0.0;

        return( get_completed() / wl );
    }

    double
    get_completed() {
        if( m_host == null ) return 0.0;

        Entry tag_comp = m_host.m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_for_tag( tag_comp, new Lifeograph.MutableInt() );
    }

    double
    get_workload() {
        if( m_host == null ) return 0.0;

        Entry tag_comp = m_host.m_p2diary.get_completion_tag();

        if( tag_comp == null )
            return 0.0;

        return get_value_planned_for_tag( tag_comp, new Lifeograph.MutableInt() );
    }

    boolean
    has_date() {
        return( m_date != Date.NOT_SET );
    }

    long
    get_date_broad() {
        if( m_date != Date.NOT_SET )
            return m_date;
        else
        if( m_host != null )
            return m_host.get_date_t();

        return Date.NOT_SET;
    }

    void
    clear_references() {
//        for( Paragraph ref : m_references )
//            ref = null;
        m_references.clear();
    }
    void
    add_reference( Paragraph ref ) {
        m_references.add( ref );
    }
    void
    remove_reference( Paragraph ref2remove ) {
        m_references.remove( ref2remove );
    }



    protected void
    update_per_text() {

    }

    public Entry m_host;
    public int   m_para_no;
    public int   m_status = DiaryElement.ES_NOT_TODO;
    public int   m_indentation_level = 0;
    public int   m_heading_level = 0;
    public char  m_justification = JT_LEFT;

    protected String                m_text;
    protected Map< String, Double > m_tags = new TreeMap<>();
    protected Map< String, Double > m_tags_planned = new TreeMap<>();
    protected List< String >        m_tags_in_order = new ArrayList<>();
    protected long                  m_date = Date.NOT_SET;
    protected Set< Paragraph >      m_references = new HashSet<>();
}
