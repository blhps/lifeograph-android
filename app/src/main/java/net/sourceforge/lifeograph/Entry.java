/***********************************************************************************

    Copyright (C) 2012-2016 Ahmet Öztürk (aoz_2@yahoo.com)

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

import android.util.Log;

import java.util.ArrayList;

public class Entry extends DiaryElement {
    public Entry( Diary diary, long date, String text, boolean favored ) {
        super( diary, "", favored ? ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );
        m_date = new Date( date );

        // java.util.Date jd = new java.util.Date();
        // m_date_created = jd.getTime() / 1000;
        m_date_created = ( int ) ( System.currentTimeMillis() / 1000L );
        m_date_changed = m_date_created;
        m_date_status = m_date_created;

        m_text = text;
        m_ptr2theme_tag = null;
        calculate_title( text );
    }

    public Entry( Diary diary, long date, boolean favored ) {
        super( diary, Lifeograph.getStr( R.string.empty_entry ),
                favored ? ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );
        m_date = new Date( date );

        java.util.Date jd = new java.util.Date();
        m_date_created = ( int ) ( jd.getTime() / 1000L );
        m_date_changed = m_date_created;
        m_date_status = m_date_created;

        m_ptr2theme_tag = null;
    }

    @Override
    public Type get_type() {
        return Type.ENTRY;
    }

    @Override
    public int get_size() {
        return m_text.length();
    }

    @Override
    public int get_icon() {
        //return( is_favored() ? R.mipmap.ic_action_favorite : R.mipmap.ic_entry );

        switch( get_todo_status() )
        {
            case ES_TODO:
                return R.mipmap.ic_todo_open;
            case ES_PROGRESSED:
                return R.mipmap.ic_todo_progressed;
            case ES_DONE:
                return R.mipmap.ic_todo_done;
            case ES_CANCELED:
                return R.mipmap.ic_todo_canceled;
            default:
                return R.mipmap.ic_entry;
        }
    }

    @Override
    public Date get_date() {
        return m_date;
    }

    public void set_date( long date ) {
        m_date.m_date = date;
    }

    @Override
    public String get_title_str() {
        if( ! m_date.is_hidden() ) {
            StringBuilder title = new StringBuilder();
            title.append( m_date.format_string() );

            if( !m_date.is_ordinal() && Lifeograph.getScreenWidth() > 3.0 )
                title.append( ", " ).append( get_date().get_weekday_str() );

            return title.toString();
        }
        else {
            if( m_ptr2diary.m_groups.getMap().containsKey( m_date.get_pure() ) )
                return m_ptr2diary.m_groups.getMap().get( m_date.get_pure() ).get_name();
            else
                return "/"; // TODO find a better name
        }
    }

    @Override
    public String get_info_str() {
        return( Date.format_string_d( m_date_changed ) );
    }

    @Override
    public String get_list_str() {
        if( m_date.is_hidden() )
            return m_name;
        else
            return( m_date.format_string() + STR_SEPARATOR + m_name );
    }

    @Override
    public String getListStrSecondary() {
        if( m_date.is_ordinal() ) {
            return( Lifeograph.getStr( R.string.entry_last_changed_on ) + " " +
                    Date.format_string_d( m_date_changed ) );
        }
        else {
            return m_date.get_weekday_str();
        }
    }

    public String get_text() {
        return m_text;
    }

    public void set_text( String text ) {
        m_text = text;
    }

    // FILTERING
    public boolean get_filtered_out() {
        Filter filter = m_ptr2diary.get_filter();
        int fs = filter.get_status();

        boolean flag_filteredout = ( ( m_status & ES_FILTERED_OUT ) != 0 );

        while( ( fs & ES_FILTER_OUTSTANDING ) != 0 )  // this loop is meant for a single iteration
        // loop used instead of if to be able to break out
        {
//            TODO WILL BE IMPLEMENTED IN 0.3
//            flag_filteredout = ( ( fs & ES_FILTER_TRASHED & m_status ) == 0 );
//
//            // no need to continue if already filtered out
//            if( flag_filteredout )
//                break;

            flag_filteredout = ( ( fs & ES_FILTER_FAVORED & m_status ) == 0 );
            if( flag_filteredout )
                break;

            flag_filteredout = ( ( fs & ES_FILTER_TODO & m_status ) == 0 );
            if( flag_filteredout )
                break;

//          TODO WILL BE IMPLEMENTED IN 0.3
//            if( ( fs & ES_FILTER_DATE_BEGIN ) != 0 )
//                if( m_date.m_date < filter.get_date_begin() )
//                {
//                    flag_filteredout = true;
//                    break;
//                }
//
//            if( ( fs & ES_FILTER_DATE_END ) != 0 )
//                if( m_date.m_date > filter.get_date_end() )
//                {
//                    flag_filteredout = true;
//                    break;
//                }
//
//            if( ( fs & ES_FILTER_TAG ) != 0 )
//            {
//                if( filter.get_tag().get_type() == Type.TAG )
//                {
//                    if( ! m_tags.contains( filter.get_tag() ) )
//                    {
//                        flag_filteredout = true;
//                        break;
//                    }
//                }
//                else // untagged
//                {
//                    if( ! m_tags.isEmpty() )
//                    {
//                        flag_filteredout = true;
//                        break;
//                    }
//                }
//            }
//
//            if( ( fs & ES_FILTER_INDIVIDUAL ) != 0 )
//            {
//                if( filter.is_entry_filtered( this ) )
//                {
//                    flag_filteredout = true;
//                    break;
//                }
//            }

            if( m_ptr2diary.is_search_active() )
                flag_filteredout = !m_text.toLowerCase().contains( m_ptr2diary.get_search_text() );

            break;
        }

        if( ( fs & ES_FILTER_OUTSTANDING ) != 0 )
            set_filtered_out( flag_filteredout );

        return flag_filteredout;
    }

    public void set_filtered_out( boolean filteredout ) {
        if( filteredout )
            m_status |= ES_FILTERED_OUT;
        else if( ( m_status & ES_FILTERED_OUT ) != 0 )
            m_status -= ES_FILTERED_OUT;
    }

    // FAVORITE ENTRY
    @Override
    public boolean is_favored() {
        return( ( m_status & ES_FAVORED ) != 0 );
    }

    public void set_favored( boolean favored ) {
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

    public void toggle_favored() {
        m_status ^= ES_FILTER_FAVORED;
    }

    // LANGUAGE
    public String get_lang() {
        return m_option_lang;
    }

    public String get_lang_final() {
        return m_option_lang.compareTo( Lifeograph.LANG_INHERIT_DIARY ) == 0 ? m_ptr2diary.get_lang()
                                                                           : m_option_lang;
    }

    public void set_lang( String lang ) {
        m_option_lang = lang;
    }

    // TRASH FUNCTIONALITY
    public boolean is_trashed() {
        return( ( m_status & ES_TRASHED ) != 0 );
    }

    public void set_trashed( boolean trashed ) {
        set_status_flag( ES_TRASHED, trashed );
    }

    // TAGS
    public java.util.List< Tag > get_tags() { return m_tags; }

    public boolean add_tag( Tag tag ) {
        return add_tag( tag, 1.0 );
    }
    public boolean add_tag( Tag tag, double value ) {
        if( tag.get_type() == Type.UNTAGGED ) { // may not be used in android actually
            return clear_tags();
        }
        else if( m_tags.add( tag ) ) {
            tag.add_entry( this, value );
            m_ptr2diary.get_untagged().remove_entry( this );

            if( m_ptr2theme_tag == null && tag.get_has_own_theme() )
                m_ptr2theme_tag = tag;

            return true;
        }
        else
            return false;
    }

    public boolean remove_tag( Tag tag ) {
        if( m_tags.remove( tag ) ) {
            tag.remove_entry( this );

            if( m_tags.isEmpty() )
                m_ptr2diary.get_untagged().add_entry( this );

            // if this tag was the theme tag, re-adjust the theme tag
            if( m_ptr2theme_tag == tag ) {
                for( Tag t : m_tags ) {
                    if( t.get_has_own_theme() ) {
                        m_ptr2theme_tag = t;
                        return true;
                    }
                }

                m_ptr2theme_tag = null;
            }

            return true;
        }
        else
            return false;
    }

    public boolean clear_tags() {
        if( m_tags.isEmpty() )
            return false;

        for( Tag tag : m_tags )
            tag.remove_entry( this );

        m_tags.clear();

        return true;
    }

    // THEME
    public Theme get_theme() {
        return( m_ptr2theme_tag != null ?
                m_ptr2theme_tag.get_theme() : m_ptr2diary.get_untagged().get_theme() );
    }

    public Tag get_theme_tag()
    {
        return m_ptr2theme_tag;
    }

    public void set_theme_tag( Tag tag ) {
        // theme tag must be in the tag set
        if( m_tags.contains( tag ) )
            m_ptr2theme_tag = tag;
    }

    public boolean get_theme_is_set() {
        return( m_ptr2theme_tag != null );
    }

    public void update_theme() { // called when a tag gained or lost custom theme
        if( m_ptr2theme_tag != null ) { // if there already was a theme tag set
            if( !m_ptr2theme_tag.get_has_own_theme() ) // if it is no longer a theme tag
                m_ptr2theme_tag = null;
        }

        if( m_ptr2theme_tag == null ) {
            // check if another tag has its own theme and set it
            for( Tag tag : m_tags ) {
                if( tag.get_has_own_theme() ) {
                    m_ptr2theme_tag = tag;
                    break;
                }
            }
        }
    }

    private void calculate_title( String text ) {
        if( text.length() < 1 ) {
            m_name = Lifeograph.getStr( R.string.empty_entry );
        }
        else {
            int pos = text.indexOf( '\n' );
            if( pos == -1 )
                m_name = text;
            else
                m_name = text.substring( 0, pos );
        }
    }

    Date m_date;
    long m_date_created;
    long m_date_changed;
    long m_date_status;
    String m_text = ""; // must be initialized to prevent crashes on empty entries with tags
    java.util.List< Tag > m_tags = new ArrayList< Tag >();
    String m_location = "";
    private Tag m_ptr2theme_tag;
    private String m_option_lang = Lifeograph.LANG_INHERIT_DIARY; // empty means off
}
