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

import java.util.ArrayList;

public class Entry extends DiaryElement {
    public Entry( Diary diary, long date, String text, boolean favored ) {
        super( diary, "", favored ? ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );
        m_date = new Date( date );

        // java.util.Date jd = new java.util.Date();
        // m_date_created = jd.getTime() / 1000;
        m_date_created = ( int ) ( System.currentTimeMillis() / 1000L );
        m_date_changed = m_date_created;

        m_text = new String( text );
        m_ptr2theme_tag = null;
        calculate_title( text );
    }

    public Entry( Diary diary, long date, boolean favored ) {
        super( diary, Lifeobase.getStr( R.string.empty_entry ),
                favored ? ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );
        m_date = new Date( date );

        java.util.Date jd = new java.util.Date();
        m_date_created = ( int ) ( jd.getTime() / 1000L );
        m_date_changed = m_date_created;

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

    public String getHeadStr() {
        return( m_date.is_ordinal() ? getListStr() : m_date.format_string( true ) );
    }

    @Override
    public String getSubStr() {
        return Lifeobase.activityDiary.getString( R.string.entry_last_changed_on ) + " "
               + Date.format_string_do( m_date_changed );
    }

    @Override
    public String getListStr() {
        return( m_date.format_string( false ) + STR_SEPARATOR + m_name );
    }

    @Override
    public String getListStrSecondary() {
        return m_date.is_ordinal() ? Date.format_string_do( m_date_changed )
                                  : m_date.getWeekdayStr();
    }

    @Override
    public int get_icon() {
        //return( is_favored() ? R.drawable.ic_favorite : R.drawable.ic_entry );

        switch( get_todo_status() )
        {
            case ES_TODO:
                return R.drawable.ic_todo_open;
            case ES_DONE:
                return R.drawable.ic_todo_done;
            case ES_CANCELED:
                return R.drawable.ic_todo_canceled;
            default:
                return R.drawable.ic_entry;
        }
    }

    public String get_text() {
        return m_text;
    }

    public void set_text( String text ) {
        m_text = text;
    }

    // FILTERING
    boolean get_filtered_out() {
        Filter filter = mDiary.get_filter();
        int fs = filter.get_status();

        boolean flag_filteredout = ( ( m_status & ES_FILTERED_OUT ) != 0 );

        while( ( fs & ES_FILTER_OUTSTANDING) != 0 )  // this loop is meant for a single iteration
        // loop used instead of if to be able to break out
        {
            flag_filteredout = ( ( fs & ES_FILTER_TRASHED & m_status ) == 0 );

            // no need to continue if already filtered out
            if( flag_filteredout )
                break;

            flag_filteredout = ( ( fs & ES_FILTER_FAVORED & m_status ) == 0 );
            if( flag_filteredout )
                break;

            flag_filteredout = ( ( fs & ES_FILTER_TODO & m_status ) == 0 );
            if( flag_filteredout )
                break;

            if( ( fs & ES_FILTER_DATE_BEGIN ) != 0 )
                if( m_date.m_date < filter.get_date_begin() )
                {
                    flag_filteredout = true;
                    break;
                }

            if( ( fs & ES_FILTER_DATE_END ) != 0 )
                if( m_date.m_date > filter.get_date_end() )
                {
                    flag_filteredout = true;
                    break;
                }

            if( ( fs & ES_FILTER_TAG ) != 0 )
            {
                if( filter.get_tag().get_type() == Type.TAG )
                {
                    if( ! m_tags.contains( filter.get_tag() ) )
                    {
                        flag_filteredout = true;
                        break;
                    }
                }
                else // untagged
                {
                    if( ! m_tags.isEmpty() )
                    {
                        flag_filteredout = true;
                        break;
                    }
                }
            }

            if( ( fs & ES_FILTER_INDIVIDUAL ) != 0 )
            {
                if( filter.is_entry_filtered( this ) )
                {
                    flag_filteredout = true;
                    break;
                }
            }

            /* TODO
            if( mDiary.is_search_active() )
                flag_filteredout = ( m_text.lowercase().find( mDiary.get_search_text() )
                        ==  );*/

            break;
        }

        if( ( fs & ES_FILTER_OUTSTANDING ) != 0 )
            set_filtered_out( flag_filteredout );

        return flag_filteredout;
    }

    void set_filtered_out( boolean filteredout ) {
        if( filteredout )
            m_status |= ES_FILTERED_OUT;
        else if( ( m_status & ES_FILTERED_OUT ) != 0 )
            m_status -= ES_FILTERED_OUT;
    }

    // FAVORITE ENTRY
    @Override
    public boolean is_favored()
    { return( ( m_status & ES_FAVORED ) != 0 ); }

    public void set_favored( boolean favored )
    {
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

    public void toggle_favored()
    { m_status ^= ES_FILTER_FAVORED; }

    // LANGUAGE
    public String get_lang() {
        return m_option_lang;
    }

    public String get_lang_final() {
        return m_option_lang.compareTo( Lifeobase.LANG_INHERIT_DIARY ) == 0 ? mDiary.get_lang()
                                                                           : m_option_lang;
    }

    void set_lang( String lang ) {
        m_option_lang = lang;
    }

    // TRASH FUNCTIONALITY
    boolean is_trashed() {
        return( ( m_status & ES_TRASHED ) != 0 );
    }

    void set_trashed( boolean trashed ) {
        set_status_flag( ES_TRASHED, trashed );
    }

    // TAGS
    public boolean add_tag( Tag tag ) {
        if( m_tags.add( tag ) ) {
            tag.add_entry( this );
            return true;
        }
        else
            return false;
    }

    public boolean remove_tag( Tag tag ) {
        if( m_tags.remove( tag ) ) {
            tag.remove_entry( this );
            return true;
        }
        else
            return false;
    }

    protected void calculate_title( String text ) {
        if( text.length() < 1 ) {
            m_name = Lifeobase.getStr( R.string.empty_entry );
        }
        else {
            int pos = text.indexOf( '\n' );
            if( pos == -1 )
                m_name = text;
            else
                m_name = text.substring( 0, pos );
        }
    }

    // THEME
    public Theme get_theme()
    {
        return( m_ptr2theme_tag != null ?
                m_ptr2theme_tag.get_theme() : mDiary.get_untagged().get_theme() );
    }

    public Tag get_theme_tag()
    {
        return m_ptr2theme_tag;
    }

    public void set_theme_tag( Tag tag )
    {
        // theme tag must be in the tag set
        if( m_tags.contains( tag ) )
            m_ptr2theme_tag = tag;
    }

    public boolean get_theme_is_set() {
        return( m_ptr2theme_tag != null );
    }

    public void update_theme() // called when a tag gained or lost custom theme
    {
        if( m_ptr2theme_tag != null ) // if there already was a theme tag set
        {
            if( m_ptr2theme_tag.get_has_own_theme() == false ) // if it is no longer a theme tag
                m_ptr2theme_tag = null;
        }

        if( m_ptr2theme_tag == null )
        {
            // check if another tag has its own theme and set it
            for( Tag tag : m_tags )
            {
                if( tag.get_has_own_theme() )
                {
                    m_ptr2theme_tag = tag;
                    break;
                }
            }
        }
    }

    public int get_todo_status()
    {
        return( m_status & ES_FILTER_TODO );
    }

    public void set_todo_status( int s )
    {
        m_status -= ( m_status & ES_FILTER_TODO );
        m_status |= s;
    }

    public Date m_date;
    public long m_date_created;
    public long m_date_changed;
    public String m_text = new String();
    protected java.util.List< Tag > m_tags = new ArrayList< Tag >();
    protected Tag m_ptr2theme_tag;

    protected String m_option_lang = Lifeobase.LANG_INHERIT_DIARY; // empty means off
}
