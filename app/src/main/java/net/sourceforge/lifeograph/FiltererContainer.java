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

import android.util.Log;

import java.util.Vector;

import static net.sourceforge.lifeograph.DiaryElement.*;

public class FiltererContainer extends Filterer
{
    // FILTERERSTATUS ==============================================================================
    public static class FiltererStatus extends Filterer
    {
        FiltererStatus( Diary diary, FiltererContainer ctr, int es ) {
            super( diary, ctr );
            m_included_statuses = es;
        }

        @Override boolean
        filter( Entry entry ) {
            return( m_included_statuses & entry.get_todo_status_effective() ) != 0;
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFs" )
                  .append( ( m_included_statuses & ES_SHOW_NOT_TODO ) != 0 ? 'N' : 'n' )
                  .append( ( m_included_statuses & ES_SHOW_TODO ) != 0 ? 'O' : 'o' )
                  .append( ( m_included_statuses & ES_SHOW_PROGRESSED ) != 0 ? 'P' : 'p' )
                  .append( ( m_included_statuses & ES_SHOW_DONE ) != 0 ? 'D' : 'd' )
                  .append( ( m_included_statuses & ES_SHOW_CANCELED ) != 0 ? 'C' : 'c' );
        }

        int m_included_statuses;
    }

    // FILTERERFAVORITE ============================================================================
    public static class FiltererFavorite extends Filterer
    {
        FiltererFavorite( Diary diary, FiltererContainer ctr, boolean include ) {
            super( diary, ctr );
            m_include_favorite = include;
        }

        @Override boolean
        filter( Entry entry ) {
            return( entry.is_favored() == m_include_favorite );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFf" ).append( m_include_favorite ? 'y' : 'n' );
        }

        boolean m_include_favorite;
    }

    // FILTERERTRASHED =============================================================================
    public static class FiltererTrashed extends Filterer
    {
        FiltererTrashed( Diary diary, FiltererContainer ctr, boolean include ) {
            super( diary, ctr );
            m_include_trashed = include;
        }

        @Override boolean
        filter( Entry entry ) {
            return( entry.is_trashed() == m_include_trashed );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFt" ).append( m_include_trashed ? 'y' : 'n' );
        }

        boolean m_include_trashed;
    }

    // FILTERERIS ==================================================================================
    public static class FiltererIs extends Filterer
    {
        FiltererIs( Diary diary, FiltererContainer ctr, int id, boolean f_is ) {
            super( diary, ctr );
            m_id = id;
            m_f_is = f_is;
        }

        @Override boolean
        filter( Entry entry ) {
            return( ( entry.get_id() == m_id ) == m_f_is );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFi").append( m_f_is ? 'T' : 'F').append( m_id );
        }

        int     m_id;
        boolean m_f_is;
    }

    // FILTERERHASTAG ==============================================================================
    public static class FiltererHasTag extends Filterer
    {
        FiltererHasTag( Diary diary, FiltererContainer ctr, Entry tag, boolean f_has ) {
            super( diary, ctr );
            m_tag = tag;
            m_f_has = f_has;
        }

        @Override boolean
        filter( Entry entry ) {
            return( m_tag == null || ( entry.has_tag_broad( m_tag ) == m_f_has ) );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFr" ).append( m_f_has ? 'T' : 'F' )
                  .append( m_tag != null ? m_tag.get_id() : DEID_UNSET );
        }

        Entry   m_tag;
        boolean m_f_has;
    }

    // FILTERERTHEME ===============================================================================
    public static class FiltererTheme extends Filterer
    {
        FiltererTheme( Diary diary, FiltererContainer ctr, Theme theme, boolean f_has ) {
            super( diary, ctr );
            m_theme = theme;
            m_f_has = f_has;
        }

        @Override boolean
        filter( Entry entry ) {
            return( m_theme == null || ( entry.has_theme( m_theme.get_name() ) == m_f_has ) );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFh" ).append( m_f_has ? 'T' : 'F' );
            if( m_theme != null )
                string.append( m_theme.get_name() );
        }

        Theme   m_theme;
        boolean m_f_has;
    }

    // FILTERERBETWEENDATES ========================================================================
    public static class FiltererBetweenDates extends Filterer
    {
        FiltererBetweenDates( Diary diary, FiltererContainer ctr,
                              long date_b, boolean f_incl_b,
                              long date_e, boolean f_incl_e ) {
            super( diary, ctr );
            m_date_b = date_b;
            m_f_incl_b = f_incl_b;
            m_date_e = date_e;
            m_f_incl_e = f_incl_e;
        }

        @Override boolean
        filter( Entry entry ) {
            final long date = entry.get_date_t();
            boolean    res_b = true;
            boolean    res_e = true;

            if( m_date_b != Date.NOT_SET ) {
                if( m_f_incl_b ) {
                    if( date < m_date_b ) res_b = false;
                }
                else
                if( date <= m_date_b ) res_b = false;
            }

            if( m_date_e != Date.NOT_SET ) {
                if( m_f_incl_e ) {
                    if( date > m_date_e ) res_e = false;
                }
                else
                if( date >= m_date_e ) res_e = false;
            }

            if( m_date_b <= m_date_e )
                return( res_b && res_e );
            else
                return( res_b || res_e );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFd" )
                  .append( m_f_incl_b ? '[' : '(' ).append( m_date_b )
                  .append( m_f_incl_e ? '[' : '(' ).append( m_date_e );
        }

        long    m_date_b;
        boolean m_f_incl_b;
        long    m_date_e;
        boolean m_f_incl_e;
    }

    // FILTERERBETWEENENTRIES ======================================================================
    public static class FiltererBetweenEntries extends Filterer
    {
        FiltererBetweenEntries( Diary diary, FiltererContainer ctr,
                                Entry entry_b, boolean f_incl_b,
                                Entry entry_e, boolean f_incl_e ) {
            super( diary, ctr );
            m_entry_b = entry_b;
            m_f_incl_b = f_incl_b;
            m_entry_e = entry_e;
            m_f_incl_e = f_incl_e;
        }

        @Override boolean
        filter( Entry entry ) {
            final long date = entry.get_date_t();
            final long date_b = ( m_entry_b != null ? m_entry_b.get_date_t() : 0 );
            final long date_e = ( m_entry_e != null ? m_entry_e.get_date_t() : Date.DATE_MAX );
            boolean    res_b = true;
            boolean    res_e = true;

            if( m_f_incl_b ) {
                if( date < date_b ) res_b = false;
            }
            else
            if( date <= date_b ) res_b = false;

            if( m_f_incl_e ) {
                if( date > date_e ) res_e = false;
            }
            else
            if( date >= date_e ) res_e = false;

            if( date_b <= date_e )
                return( res_b && res_e );
            else
                return( res_b || res_e );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFe" )
                  .append( m_f_incl_b ? '[' : '(' ).append( m_entry_b != null ? m_entry_b.get_id() :
                                                            DEID_UNSET )
                  .append( m_f_incl_e ? '[' : '(' ).append( m_entry_e != null ? m_entry_e.get_id() :
                                                            DEID_UNSET );
        }

        Entry   m_entry_b;
        boolean m_f_incl_b;
        Entry   m_entry_e;
        boolean m_f_incl_e;
    }

    // FILTERERCOMPLETION ==========================================================================
    public static class FiltererCompletion extends Filterer
    {
        FiltererCompletion( Diary diary, FiltererContainer ctr, double compl_b, double compl_e  ) {
            super( diary, ctr );
            m_compl_b = compl_b;
            m_compl_e = compl_e;
        }

        @Override boolean
        filter( Entry entry ) {
            final double completion = entry.get_completion() * 100.0;
            if( m_compl_b <= m_compl_e )
                return( completion >= m_compl_b && completion <= m_compl_e );
            else
                return( completion >= m_compl_b || completion <= m_compl_e );
        }

        @Override void
        get_as_string( StringBuilder string ) {
            string.append( "\nFc" ).append( m_compl_b ).append( '&' ).append( m_compl_e );
        }

        double m_compl_b;
        double m_compl_e;
    }

    // FILTERERCONTAINER ===========================================================================
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
        m_pipeline.clear();
    }

    void
    update_state() { }

    @Override boolean
    filter( Entry entry ) {
        if( m_pipeline.isEmpty() )
            return true;

        for( Filterer filterer : m_pipeline ) {
            if( filterer.filter( entry ) ) {
                if( m_flag_or )
                    return true;
            }
            else
            if( ! m_flag_or )
                return false;
        }

        return( ! m_flag_or );
    }

    @Override boolean
    is_container() {
        return true;
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

        Lifeograph.MutableString line        = new Lifeograph.MutableString();
        Lifeograph.MutableInt    line_offset = new Lifeograph.MutableInt();
        FiltererContainer        container   = this;

        clear();

        while( Lifeograph.get_line( string, line_offset, line ) )
        {
            if( line.v.length() < 2 )   // should never occur
                continue;

            switch( line.v.charAt( 1 ) )
            {
                case 't':   // trashed (y/n)
                    container.add_filterer_trashed( line.v.charAt( 2 ) == 'y' );
                    break;
                case 'f':   // favorite (y/n)
                    container.add_filterer_favorite( line.v.charAt( 2 )  == 'y' );
                    break;
                case 's':   // status (ElemStatus)
                {
                    int status = 0;
                    if( line.v.length() > 6 ) {
                        if( line.v.charAt( 2 ) == 'N' ) status |= ES_SHOW_NOT_TODO;
                        if( line.v.charAt( 3 ) == 'O' ) status |= DiaryElement.ES_SHOW_TODO;
                        if( line.v.charAt( 4 ) == 'P' ) status |= DiaryElement.ES_SHOW_PROGRESSED;
                        if( line.v.charAt( 5 ) == 'D' ) status |= DiaryElement.ES_SHOW_DONE;
                        if( line.v.charAt( 6 ) == 'C' ) status |= DiaryElement.ES_SHOW_CANCELED;
                    }
                    container.add_filterer_status( status );
                    break;
                }
                case 'i':   // is not (DEID)
                {
                    int id = Integer.parseInt( line.v.substring( 3 ) );
                    container.add_filterer_is( id, line.v.charAt( 2 ) == 'T' );
                    break;
                }
                case 'r':   // tagged/referenced by (DEID)
                {
                    int id = Integer.parseInt( line.v.substring( 3 ) );
                    container.add_filterer_tagged_by( m_p2diary.get_entry_by_id( id ),
                                                      line.v.charAt( 2 ) == 'T' );
                    break;
                }
                case 'h':   // theme (Ustring)
                {
                    String name = line.v.substring( 3 );
                    container.add_filterer_theme( m_p2diary.get_theme( name ),
                                                  line.v.charAt( 2 ) == 'T' );
                    break;
                }
                case 'd':   // between dates
                {
                    Lifeograph.MutableInt i_date = new Lifeograph.MutableInt( 3 );
                    long                  date_b = Lifeograph.get_long( line.v, i_date );
                    int                   i_f_icl_e = i_date.v;
                    i_date.v++;
                    long                  date_e = Lifeograph.get_long( line.v, i_date );
                    container.add_filterer_between_dates(
                            date_b, line.v.charAt( 2 ) == '[',
                            date_e, line.v.charAt( i_f_icl_e ) == '[' );
                    break;
                }
                case 'e':   // between entries
                {
                    Lifeograph.MutableInt i_id = new Lifeograph.MutableInt( 3 );
                    int                   id_b = Lifeograph.get_int( line.v, i_id );
                    int                   i_f_icl_e = i_id.v;
                    i_id.v++;
                    int                   id_e = Lifeograph.get_int( line.v, i_id );
                    container.add_filterer_between_entries(
                            m_p2diary.get_entry_by_id( id_b ), line.v.charAt( 2 ) == '[',
                            m_p2diary.get_entry_by_id( id_e ),
                            line.v.charAt( i_f_icl_e ) == '[' );
                    break;
                }
                case 'c':   // completion
                {
                    Lifeograph.MutableInt i_double = new Lifeograph.MutableInt( 2 );
                    double                double_b = Lifeograph.get_double( line.v, i_double );
                    i_double.v++;
                    double                double_e = Lifeograph.get_double( line.v, i_double );
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
                    Log.d( Lifeograph.TAG, "Unrecognized filter string: " + line );
                    break;
            }
        }

        update_logic_label();
    }

    void
    toggle_logic() { }

    void
    update_logic_label() { }

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
        m_pipeline.add(
                new FiltererBetweenDates( m_p2diary, this, date_b, f_incl_b, date_e, f_incl_e ) );
    }

    void
    add_filterer_between_entries( Entry entry_b, boolean f_inc_b, Entry entry_e, boolean f_inc_e ) {
        m_pipeline.add(
                new FiltererBetweenEntries( m_p2diary, this, entry_b, f_inc_b, entry_e, f_inc_e ) );
    }

    void
    add_filterer_completion( double compl_b, double compl_e ) {
        m_pipeline.add( new FiltererCompletion( m_p2diary, this, compl_b, compl_e ) );
    }

    FiltererContainer
    add_filterer_subgroup() {
        FiltererContainer container =
        new FiltererContainer( m_p2diary, this,
                               m_p2container != null && !( m_p2container.m_flag_or ) );
        m_pipeline.add( container );
        return container;
    }

    void
    remove_filterer( Filterer filterer ) {
        if( m_pipeline.remove( filterer ) )
            update_state();
    }

    protected Vector< Filterer > m_pipeline = new Vector<>();
    protected boolean            m_flag_or;
}
