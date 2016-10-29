/***********************************************************************************

    Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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

public class Filter extends DiaryElement
{
    public Filter( Diary d, String name )
    {
        super( d, name, ES_FILTER_RESET );
        m_tag = null;
        m_date_begin = 0;
        m_date_end = Date.DATE_MAX;
    }

    @Override
    public Type get_type() {
        return Type.FILTER;
    }

    @Override
    public int get_size() {
        return 0;
    }

    @Override
    public int get_icon() {
        return 0;
    }

    @Override
    public String get_info_str() {
        return "Filter";
    }

    public void reset() {
        m_status = ES_FILTER_RESET;
        m_tag = null;
        m_date_begin = 0;
        m_date_end = Date.DATE_MAX;
        m_entries.clear();
    }

    public void set( Filter source ) {
        m_status = source.m_status;
        m_tag = source.m_tag;
        m_date_begin = source.m_date_begin;
        m_date_end = source.m_date_end;
        m_entries = source.m_entries;   // is this OK?
    }

    public Tag get_tag() {
        return m_tag;
    }

    public void set_tag( Tag tag ) {
        m_tag = tag;

        if( tag == null )
            m_status &= ( ~ES_FILTER_TAG );
        else
            m_status |= ES_FILTER_TAG;

        m_status |= ES_FILTER_OUTSTANDING;
    }

    public void set_favorites( boolean flag_show_fav, boolean flag_show_not_fav ) {
        // clear previous values
        m_status &= ( ES_FILTER_FAVORED ^ ES_FILTER_MAX );

        if( flag_show_fav )
            m_status |= ES_SHOW_FAVORED;
        if( flag_show_not_fav )
            m_status |= ES_SHOW_NOT_FAVORED;

        m_status |= ES_FILTER_OUTSTANDING;
    }

    public void set_trash( boolean flag_show_trashed, boolean flag_show_not_trashed ) {
        // clear previous values
        m_status &= ( ES_FILTER_TRASHED ^ ES_FILTER_MAX );

        if( flag_show_trashed )
            m_status |= ES_SHOW_TRASHED;
        if( flag_show_not_trashed )
            m_status |= ES_SHOW_NOT_TRASHED;

        m_status |= ES_FILTER_OUTSTANDING;
    }

    public void set_todo( boolean flag_show_not_todo, boolean flag_show_todo,
                          boolean flag_show_progressed, boolean flag_show_done,
                          boolean flag_show_canceled ) {
        // clear previous values
        m_status &= ( ES_FILTER_TODO ^ ES_FILTER_MAX );

        if( flag_show_not_todo )
            m_status |= ES_SHOW_NOT_TODO;
        if( flag_show_todo )
            m_status |= ES_SHOW_TODO;
        if( flag_show_progressed )
            m_status |= ES_SHOW_PROGRESSED;
        if( flag_show_done )
            m_status |= ES_SHOW_DONE;
        if( flag_show_canceled )
            m_status |= ES_SHOW_CANCELED;

        m_status |= ES_FILTER_OUTSTANDING;
    }

    public long get_date_begin() {
        return m_date_begin;
    }

    public long get_date_end() {
        return m_date_end;
    }

    public void set_date_begin( long d ) {
        m_status |= ES_FILTER_DATE_BEGIN;
        m_status |= ES_FILTER_OUTSTANDING;
        m_date_begin = d;
    }

    public void set_date_end( long d ) {
        m_status |= ES_FILTER_DATE_END;
        m_status |= ES_FILTER_OUTSTANDING;
        m_date_end = d;
    }

    public void clear_dates() {
        m_date_begin = 0;
        m_date_end = Date.DATE_MAX;
        if( ( m_status & ES_FILTER_DATE_BEGIN ) != 0 )
            m_status -= ES_FILTER_DATE_BEGIN;
        if( ( m_status & ES_FILTER_DATE_END ) != 0 )
            m_status -= ES_FILTER_DATE_END;

        m_status |= ES_FILTER_OUTSTANDING;
    }

    public void set_status_applied() {
        if( ( m_status & ES_FILTER_OUTSTANDING ) != 0 )
            m_status -= ES_FILTER_OUTSTANDING;
    }

    public void set_status_outstanding() {
        m_status |= ES_FILTER_OUTSTANDING;
    }

    /*public final EntrySet get_entries()
    {
        return m_entries;
    }*/

    public void add_entry( Entry e ) {
        m_status |= ES_FILTER_INDIVIDUAL;
        m_status |= ES_FILTER_OUTSTANDING;
        m_entries.add( e );
    }

    public void remove_entry( Entry e ) {
        if( m_entries.contains( e ) ) {
            m_status |= ES_FILTER_OUTSTANDING;
            m_entries.remove( e );
        }
    }

    void clear_entries() {
        if( !m_entries.isEmpty() ) {
            m_status |= ES_FILTER_OUTSTANDING;
            m_entries.clear();
        }
    }

    public boolean is_entry_filtered( Entry e )
    {
        return m_entries.contains( e );
    }

    private Tag m_tag;
    private long m_date_begin;
    private long m_date_end;
    private java.util.List< Entry > m_entries = new ArrayList< Entry >();
}
