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

import java.util.Vector;

public class FiltererContainer extends Filterer
{
    FiltererContainer( Diary diary, FiltererContainer ctr ) {
        super( diary, ctr );
        m_flag_or = false;
    }
    FiltererContainer( Diary diary, FiltererContainer ctr, boolean flag_or ) {
        super( diary, ctr );
        m_flag_or = flag_or;
    }

    void
    clear() {

    }

    void
    update_state() {

    }

    @Override boolean
    filter( Entry entry ) {

    }

    @Override boolean
    is_container() {
        return true;
    }

    boolean
    is_or() {
        return m_flag_or;
    }

    @Override void
    get_as_string( StringBuilder string ) {
        string.append( m_p2container != null ? "\nF(" : ( m_flag_or ? "F|" : "F&" ) );

        for( Filterer filterer : m_pipeline )
            filterer.get_as_string( string );

        if( m_p2container != null )
            string.append( "\nF)" );
    }

    void
    set_from_string( String string ) {
        if( m_p2container != null ) // only top level can do this
            return;

        String            line;
        int               line_offset = 0;
        FiltererContainer container = this;

        clear();

        while( STR.get_line( string, line_offset, line ) )
        {
            if( line.length() < 2 )   // should never occur
                continue;

            switch( line.charAt( 1 ) )
            {
                case 't':   // trashed (y/n)
                    container.add_filterer_trashed( line.charAt( 2 ) == 'y' );
                    break;
                case 'f':   // favorite (y/n)
                    container.add_filterer_favorite( line.charAt( 2 )  == 'y' );
                    break;
                case 's':   // status (ElemStatus)
                {
                    int status = 0;
                    if( line.length() > 6 ) {
                        if( line.charAt( 2 ) == 'N' ) status |= DiaryElement.ES_SHOW_NOT_TODO;
                        if( line.charAt( 3 ) == 'O' ) status |= DiaryElement.ES_SHOW_TODO;
                        if( line.charAt( 4 ) == 'P' ) status |= DiaryElement.ES_SHOW_PROGRESSED;
                        if( line.charAt( 5 ) == 'D' ) status |= DiaryElement.ES_SHOW_DONE;
                        if( line.charAt( 6 ) == 'C' ) status |= DiaryElement.ES_SHOW_CANCELED;
                    }
                    container.add_filterer_status( status );
                    break;
                }
                case 'i':   // is not (DEID)
                {
                    int id = Integer.parseInt( line.substring( 3 ) );
                    container.add_filterer_is( id, line.charAt( 2 ) == 'T' );
                    break;
                }
                case 'r':   // tagged/referenced by (DEID)
                {
                    int id = Integer.parseInt( line.substring( 3 ) );
                    container.add_filterer_tagged_by( m_p2diary.get_entry_by_id( id ),
                                                      line.charAt( 2 ) == 'T' );
                    break;
                }
                case 'h':   // theme (Ustring)
                {
                    String name = line.substring( 3 );
                    container.add_filterer_theme( m_p2diary.get_theme( name ),
                                                  line.charAt( 2 ) == 'T' );
                    break;
                }
                case 'd':   // between dates
                {
                    int  i_date = 3;
                    long date_b = STR.get_ul( line, i_date );
                    int  i_f_icl_e = i_date;
                    i_date++;
                    long date_e = STR.get_ul( line, i_date );
                    container.add_filterer_between_dates(
                            date_b, line.charAt( 2 ) == '[',
                            date_e, line.charAt( i_f_icl_e ) == '[' );
                    break;
                }
                case 'e':   // between entries
                {
                    int  i_date = 3;
                    long id_b = STR.get_ul( line, i_date );
                    int  i_f_icl_e = i_date;
                    i_date++;
                    long id_e = STR.get_ul( line, i_date );
                    container.add_filterer_between_entries(
                            m_p2diary.get_entry_by_id( id_b ), line.charAt( 2 ) == '[',
                            m_p2diary.get_entry_by_id( id_e ),
                            line.charAt( i_f_icl_e ) == '[' );
                    break;
                }
                case 'c':   // completion
                {
                    int    i_double = 2;
                    double double_b = STR.get_d( line, i_double );
                    i_double++;
                    double double_e = STR.get_d( line, i_double );
                    container.add_filterer_completion( double_b, double_e );
                    break;
                }
                case '(':   // sub group
                    container = container.add_filterer_subgroup();
                    break;
                case ')':   // end of sub group
                    container = container.m_p2container;
                    break;
                case '|':
                    m_flag_or = true;
                    break;
                case '&':
                    m_flag_or = false;
                    break;
                default:
                    PRINT_DEBUG( "Unrecognized filter string: ", line );
                    break;
            }
        }

        update_logic_label();
    }

    void
    toggle_logic() {

    }

    void
    update_logic_label() {

    }

    void
    add_filterer_status( int es ) {
        m_pipeline.add( new FiltererStatus( m_p2diary, this, es ) );
    }

    void
    add_filterer_favorite( boolean f_favorite ) {
        m_pipeline.add( new FiltererFavorite( m_p2diary, this, f_favorite ) );
    }

    void
    add_filterer_trashed( boolean f_trashed ) {
        m_pipeline.add( new FiltererTrashed( m_p2diary, this, f_trashed ) );
    }

    void
    add_filterer_is( int id, boolean f_is ) {
        m_pipeline.add( new FiltererIs( m_p2diary, this, id, f_is ) );
    }

    void
    add_filterer_tagged_by( Entry tag, boolean f_has ) {
        m_pipeline.add( new FiltererHasTag( m_p2diary, this, tag, f_has ) );
    }

    void
    add_filterer_theme( Theme theme, boolean f_has ) {
        m_pipeline.add( new FiltererTheme( m_p2diary, this, theme, f_has ) );
    }

    void
    add_filterer_between_dates( long date_b, boolean f_incl_b, long date_e, boolean f_incl_e ) {
        m_pipeline.add( new FiltererBetweenDates(
                m_p2diary, this, date_b, f_incl_b, date_e, f_incl_e ) );
    }

    void
    add_filterer_between_entries( Entry entry_b, boolean f_inc_b, Entry entry_e, boolean f_inc_e ) {
        m_pipeline.add( new FiltererBetweenEntries(
                m_p2diary, this, entry_b, f_inc_b, entry_e, f_inc_e ) );
    }

    void
    add_filterer_completion( double compl_b, double compl_e ) {
        m_pipeline.add( new FiltererCompletion( m_p2diary, this, compl_b, compl_e ) ); }

    FiltererContainer
    add_filterer_subgroup() {
        FiltererContainer container =
        new FiltererContainer( m_p2diary, this,
                               m_p2container != null && !( m_p2container.is_or() ) );
        m_pipeline.add( container );
        return container;
    }

    void
    remove_filterer( Filterer filterer ) {
        if( m_pipeline.remove( filterer ) )
            update_state();
    }

    protected Vector< Filterer > m_pipeline;
    protected boolean            m_flag_or;
}
