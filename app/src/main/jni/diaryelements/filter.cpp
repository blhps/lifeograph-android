/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

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


#include "../helpers.hpp"
#include "../logid.hpp"
#include "filter.hpp"
#include "diary.hpp"
#ifndef __ANDROID__
#include "../app_window.hpp"
#include "../widgets/widget_filter.hpp"
#else
#include "../lifeograph.hpp"
#endif

using namespace LoG;

// FILTERER STATUS =================================================================================
bool
FiltererStatus::filter( const Entry* entry ) const
{
    return( m_included_statuses & entry->get_todo_status_effective() );
}
bool
FiltererStatus::filter( const Paragraph* para ) const
{
    return( m_included_statuses & para->get_todo_status() );
}

void
FiltererStatus::get_as_string( Ustring& string ) const
{
    string += "\nFs";
    string += ( m_included_statuses & ES::SHOW_NOT_TODO ) ? 'N' : 'n';
    string += ( m_included_statuses & ES::SHOW_TODO ) ? 'O' : 'o';
    string += ( m_included_statuses & ES::SHOW_PROGRESSED ) ? 'P' : 'p';
    string += ( m_included_statuses & ES::SHOW_DONE ) ? 'D' : 'd';
    string += ( m_included_statuses & ES::SHOW_CANCELED ) ? 'C' : 'c';
}
Ustring
FiltererStatus::get_as_human_readable_str() const
{
    Ustring str { STR::compose( is_not() ? _( "Todo status is NOT one of" )
                                         : _( "Todo status is one of" ), " {" ) };
    bool    F_add_comma { false };

    if( m_included_statuses & ES::SHOW_NOT_TODO )
    {
        str += _( "NOT TODO" );
        F_add_comma = true;
    }
    if( m_included_statuses & ES::SHOW_TODO )
    {
        if( F_add_comma ) str += ", "; else F_add_comma = true;
        str += _( "TODO" );
    }
    if( m_included_statuses & ES::SHOW_PROGRESSED )
    {
        if( F_add_comma ) str += ", "; else F_add_comma = true;
        str += _( "PROGRESSED" );
    }
    if( m_included_statuses & ES::SHOW_DONE )
    {
        if( F_add_comma ) str += ", "; else F_add_comma = true;
        str += _( "DONE" );
    }
    if( m_included_statuses & ES::SHOW_CANCELED )
    {
        if( F_add_comma ) str += ", "; else F_add_comma = true;
        str += _( "SHOW_CANCELED" );
    }

    str += "}";

    return str;
}

// FILTERER TAG VALUE ==============================================================================
bool
FiltererSize::filter( const Entry* entry ) const
{
    return filter_v( entry->get_size_adv( m_type ) );
}
bool
FiltererSize::filter( const Paragraph* para ) const
{
    return filter_v( para->get_size_adv( m_type ) );
}

bool
FiltererSize::filter_v( int value ) const
{
    bool         res_b{ true };
    bool         res_e{ true };

    if( m_range_b >= 0 )
    {
        if( m_F_incl_b )
        {
            if( value < m_range_b ) res_b = false;
        }
        else
        if( value <= m_range_b ) res_b = false;
    }

    if( m_range_e >= 0 )
    {
        if( m_F_incl_e )
        {
            if( value > m_range_e ) res_e = false;
        }
        else
        if( value >= m_range_e ) res_e = false;
    }

    return( res_b && res_e );
}

void
FiltererSize::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFz", m_type, m_F_incl_b ? '[' : '(' );

    if( m_range_b == -1 )
        string += '_';
    else
        string += std::to_string( m_range_b );

    string += ( m_F_incl_e ? '[' : '(' );

    if( m_range_e == -1 )
        string += '_';
    else
        string += std::to_string( m_range_e );
}
Ustring
FiltererSize::get_as_human_readable_str() const
{
    return STR::compose( VT::get_v< VT::SO, const char*, char >( m_type ), " ",
                         is_not() ? _( "is NOT within" ) : _( "is within" ), " ",
                         m_F_incl_b ? '[' : '(',
                         m_range_b, "..", m_range_e,
                         m_F_incl_e ? ']' : ')' );
}

// FILTERER FAVORITE ===============================================================================
bool
FiltererFavorite::filter( const Entry* entry ) const
{
    return( entry->is_favorite() );
}

void
FiltererFavorite::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFf~" );
}
Ustring
FiltererFavorite::get_as_human_readable_str() const
{
    return( is_not() ? _( "Is NOT FAVORITE" ) : _( "Is FAVORITE" ) );
}

// FILTERER TRASHED ================================================================================
bool
FiltererTrashed::filter( const Entry* entry ) const
{
    return( entry->is_trashed() );
}

void
FiltererTrashed::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFt~" );
}
Ustring
FiltererTrashed::get_as_human_readable_str() const
{
    return( is_not() ? _( "Is NOT TRASHED" ) : _( "Is TRASHED" ) );
}

// FILTERER UNIT ===================================================================================
bool
FiltererUnit::filter( const Entry* entry ) const
{
    return( entry->get_unit() == m_unit );
}

void
FiltererUnit::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFu~", m_unit );
}
Ustring
FiltererUnit::get_as_human_readable_str() const
{
    return( STR::compose( is_not() ? _( "Unit is NOT" ) : _( "Unit is" ),
                          " ", m_unit ) );
}

// FILTERER IS NOT =================================================================================
FiltererIs::FiltererIs( FiltererContainer* ctr, D::DEID id, int depth )
: Filterer( ctr ), m_p2entry( ctr->m_p2diary->get_entry_by_id( id ) ), m_depth( depth )
{ }

bool
FiltererIs::filter( const Entry* entry ) const
{
    if( !m_p2entry )
        return true;
    if( ( m_depth & VT::OP_DEPTH::ITSELF::I ) && entry->get_id() == m_p2entry->get_id() )
        return true;
    if( m_depth & VT::OP_DEPTH::DESCENDANTS::I )
        return( entry->is_descendant_of( m_p2entry ) );

    return false;
}

void
FiltererIs::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFn~~", VT::get_v< VT::OP_DEPTH, char, int >( m_depth ),
                            "~~~~", // reserved (4)
                            get_id_raw_failsafe( m_p2entry ) );
}
Ustring
FiltererIs::get_as_human_readable_str() const
{
    return Ustring::compose(
        // TRANSLATORS: the template for: "Is [NOT] the entry/descendant of/itself or desc. of"
        _( "%1 %2 %3" ),
        is_not() ? _( "Is NOT" ): _( "Is" ),
        m_depth == VT::OP_DEPTH::ITSELF::I ? _( "the entry" )
        : ( m_depth == VT::OP_DEPTH::DESCENDANTS::I ? _( "descendant of" )
                                                           : _( "itself or a descendant of" ) ) );
}

// FILTERER TAGGED BY ==============================================================================
bool
FiltererTaggedBy::filter( const Entry* entry ) const
{
    return( m_tag == nullptr || entry->has_tag_broad( m_tag, m_F_consider_parents ) );
}
bool
FiltererTaggedBy::filter( const Paragraph* para ) const
{
    return( m_tag == nullptr || para->has_tag_broad( m_tag, m_F_consider_parents ) );
}

void
FiltererTaggedBy::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFr", m_F_consider_parents ? 'P' : '~',
                            get_id_raw_failsafe( m_tag ) );
}
Ustring
FiltererTaggedBy::get_as_human_readable_str() const
{
    return( STR::compose(
        m_F_consider_parents ? _( "Itself or one of its parents" ) : _( "Itself" ),
        is_not() ? _( "is NOT tagged by" ) : _( "is tagged by" ), " ",
        m_tag ? "\"" + m_tag->get_name() + "\"" : "--" ) );
}

// FILTERER SUBTAGGED BY ===========================================================================
bool
FiltererSubtaggedBy::filter( const Entry* entry ) const
{
    if( m_tag_parent == nullptr )
        return true;

    DiaryElemTag* subtag { nullptr };
    switch( m_type )
    {
        case VT::FIRST:
            subtag = entry->get_sub_tag_first( m_tag_parent );
            break;
        case VT::LAST:
            subtag = entry->get_sub_tag_last( m_tag_parent );
            break;
        case VT::HIGHEST:
            subtag = entry->get_sub_tag_highest( m_tag_parent );
            break;
        case VT::LOWEST:
            subtag = entry->get_sub_tag_lowest( m_tag_parent );
            break;
    }

    return filter_common( subtag );
}
bool
FiltererSubtaggedBy::filter( const Paragraph* para ) const
{
    if( m_tag_parent == nullptr )
        return true;

    DiaryElemTag* subtag { nullptr };
    switch( m_type )
    {
        case VT::FIRST:
            subtag = para->get_subtag_first( m_tag_parent );
            break;
        case VT::LAST:
            subtag = para->get_subtag_last( m_tag_parent );
            break;
        case VT::HIGHEST:
            subtag = para->get_subtag_highest( m_tag_parent );
            break;
        case VT::LOWEST:
            subtag = para->get_subtag_lowest( m_tag_parent );
            break;
    }

    return filter_common( subtag );
}
bool
FiltererSubtaggedBy::filter_common( DiaryElemTag* subtag ) const
{
    if( !subtag )      return false;
    if( !m_tag_child ) return true; // if no child tag is selected, any tag will do

    switch( m_relation )
    {
        default:    return( subtag->get_id() == m_tag_child->get_id() );
        case '<':   return( subtag->get_sibling_order() < m_tag_child->get_sibling_order() );
        case '>':   return( subtag->get_sibling_order() > m_tag_child->get_sibling_order() );
    }
}

void
FiltererSubtaggedBy::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFb", get_id_raw_failsafe( m_tag_parent ),
                                    '|', m_relation, '|',
                                    get_id_raw_failsafe( m_tag_child ),
                                    '|', // separator
                                    m_type );
}
Ustring
FiltererSubtaggedBy::get_as_human_readable_str() const
{
    Ustring str;

    switch( m_type )
    {
        case VT::FIRST:   str = _( "First subtag of" ); break;
        case VT::LAST:    str = _( "Last subtag of" ); break;
        case VT::HIGHEST: str = _( "Highest subtag of" ); break;
        case VT::LOWEST:  str = _( "Lowest subtag of" ); break;
    }

    str += STR::compose( " ",
                         m_tag_parent ? "\"" + m_tag_parent->get_name() + "\"" : "--", " ",
                         is_not() ? _( "is NOT" ) : _( "is" ), " ", m_relation, " ",
                         m_tag_child ? "\"" + m_tag_child->get_name() + "\"" : "--" );

    return str;
}

// FILTERER DEFINES TAG =============================================================================
bool
FiltererDefinesTag::filter( const Paragraph* para ) const
{
    return para->defines_tag();
}

void
FiltererDefinesTag::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF@~" );
}
Ustring
FiltererDefinesTag::get_as_human_readable_str() const
{
    return( is_not() ? _( "Does NOT define a tag" ) : _( "Defines a tag" ) );
}

// FILTERER TAG VALUE ==============================================================================
bool
FiltererTagValue::filter( const Entry* entry ) const
{
    if( m_tag == nullptr ) return true;

    double v;
    switch( m_type )
    {
        case VT::TVTS::PLANNED::I:  v = entry->get_tag_value_planned( m_tag, false ); break;
        case VT::TVTS::REALIZED::I: v = entry->get_tag_value( m_tag, false ); break;
        //case VT::TVTS::REMAINING::I:
        default:                    v = entry->get_tag_value_remaining( m_tag, false ); break;
    }
    return filter_v( v );
}
bool
FiltererTagValue::filter( const Paragraph* para ) const
{
    if( m_tag == nullptr ) return true;

    int c{ 0 }; // dummy
    double v;
    switch( m_type )
    {
        case VT::TVTS::PLANNED::I:  v = para->get_tag_value_planned( m_tag, c ); break;
        case VT::TVTS::REALIZED::I: v = para->get_tag_value( m_tag, c ); break;
        //case VT::TVTS::REMAINING::I:
        default:                    v = para->get_tag_value_remaining( m_tag, c ); break;
    }
    return filter_v( v );
}

bool
FiltererTagValue::filter_v( double value ) const
{
    bool         res_b{ true };
    bool         res_e{ true };

    if( m_range_b != Constants::INFINITY_MNS )
    {
        if( m_F_incl_b )
        {
            if( value < m_range_b ) res_b = false;
        }
        else
        if( value <= m_range_b ) res_b = false;
    }

    if( m_range_e != Constants::INFINITY_PLS )
    {
        if( m_F_incl_e )
        {
            if( value > m_range_e ) res_e = false;
        }
        else
        if( value >= m_range_e ) res_e = false;
    }

    return( res_b && res_e );
}

void
FiltererTagValue::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFv", get_id_raw_failsafe( m_tag ),
                                    VT::get_v< VT::TVTS, char, int >( m_type ),
                                    m_F_incl_b ? '[' : '(' );

    if( m_range_b == Constants::INFINITY_MNS )
        string += '_';
    else
        string += std::to_string( m_range_b );

    string += ( m_F_incl_e ? '[' : '(' );

    if( m_range_e == Constants::INFINITY_PLS )
        string += '_';
    else
        string += std::to_string( m_range_e );
}
Ustring
FiltererTagValue::get_as_human_readable_str() const
{
    Ustring str;

    switch( m_type )
    {
        case VT::TVTS::REALIZED::I:  str = _( "Realized value of" ); break;
        case VT::TVTS::PLANNED::I:   str = _( "Planned value of" ); break;
        case VT::TVTS::REMAINING::I: str = _( "Remaining value of" ); break;
    }

    str += STR::compose( " ",
                         m_tag ? "\"" + m_tag->get_name() + "\"": "--", " ",
                         is_not() ? _( "is NOT within" ) : _( "is within" ), " ",
                         m_F_incl_b ? '[' : '(',
                         m_range_b, "..", m_range_e,
                         m_F_incl_e ? ']' : ')' );

    return str;
}

// FILTERER THEME ==================================================================================
bool
FiltererTheme::filter( const Entry* entry ) const
{
    return( m_theme == nullptr || entry->has_theme( m_theme->get_name() ) );
}

void
FiltererTheme::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFh~", get_id_raw_failsafe( m_theme ) );
}
Ustring
FiltererTheme::get_as_human_readable_str() const
{
    return( STR::compose( is_not() ? _( "Has NOT the theme" ) : _( "Has the theme" ), " ",
                          m_theme ? "\"" + m_theme->get_name() + "\"" : "--" ) );
}

// FILTERER BETWEEN DATES ==========================================================================
bool
FiltererBetweenDates::filter( const Entry* entry ) const
{
    return filter_v( entry->get_date() );
}
bool
FiltererBetweenDates::filter( const Paragraph* para ) const
{
    return filter_v( para->get_date_broad() );
}
bool
FiltererBetweenDates::filter_v( DateV date ) const
{
    bool         res_b{ true };
    bool         res_e{ true };

    if( Date::is_set( m_date_b ) )
    {
        if( m_F_incl_b )
        {
            if( date < m_date_b ) res_b = false;
        }
        else
        if( date <= m_date_b ) res_b = false;
    }

    if( Date::is_set( m_date_e ) )
    {
        if( m_F_incl_e )
        {
            if( date > m_date_e ) res_e = false;
        }
        else
        if( date >= m_date_e ) res_e = false;
    }

    return( res_b && res_e );
}

void
FiltererBetweenDates::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFd", m_F_incl_b ? '[' : '(', m_date_b,
                                    m_F_incl_e ? '[' : '(', m_date_e );
}
Ustring
FiltererBetweenDates::get_as_human_readable_str() const
{
    return STR::compose( is_not() ? _( "Date is NOT within" ) : _( "Date is within" ), " ",
                         m_F_incl_b ? '[' : '(',
                         Date::format_string( m_date_b ), "..", Date::format_string( m_date_e ),
                         m_F_incl_e ? ']' : ')' );
}

// FILTERER BETWEEN DATE OFFSETS ===================================================================
bool
FiltererBetweenDateOffsets::filter( const Entry* entry ) const
{
    switch( m_date_type )
    {
        case VT::DT::START::C:      return filter_v( entry->get_date() );
        case VT::DT::FINISH::C:     return filter_v( entry->get_date_finish() );
        case VT::DT::CREATION::C:   return filter_v( entry->get_date_created() );
        default:                    return filter_v( entry->get_date_edited() );
    }
}
bool
FiltererBetweenDateOffsets::filter( const Paragraph* para ) const
{
    switch( m_date_type )
    {
        case VT::DT::START::C:      return filter_v( para->get_date_broad() );
        case VT::DT::FINISH::C:     return filter_v( para->get_date_finish() );
        case VT::DT::CREATION::C:   return filter_v( para->get_date_created() );
        default:                    return filter_v( para->get_date_edited() );
    }
}
bool
FiltererBetweenDateOffsets::filter_v( DateV date ) const
{
    const auto today  { Date::get_today() };
    auto date_bgn     { today };
    auto date_end     { today };
    Date::offset_days( date_bgn, m_offset_bgn );
    Date::offset_days( date_end, m_offset_end );

    return( date >= date_bgn && date <= date_end ); 
}

void
FiltererBetweenDateOffsets::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFj~", m_date_type, m_offset_bgn, '|', m_offset_end );
}
Ustring
FiltererBetweenDateOffsets::get_as_human_readable_str() const
{
    return Glib::ustring::compose( _( "%1 %2 within %3 and %4 days from today" ),
                                   VT::get_v< VT::DT, char const*, char >( m_date_type ),
                                   is_not() ? _( "is NOT" ) : _( "is" ),
                                   m_offset_bgn, m_offset_end );
}

// FILTERER BETWEEN ENTRIES ========================================================================
bool
FiltererBetweenEntries::filter( const Entry* entry ) const
{
    std::list< int >&& num   { entry->get_number_array() };
    std::list< int >&& num_b { m_entry_b ? m_entry_b->get_number_array() : std::list< int >() };
    std::list< int >&& num_e { m_entry_e ? m_entry_e->get_number_array() : std::list< int >() };
    bool               res_b { true };
    bool               res_e { true };

    auto less_than = []( std::list< int >& lil, std::list< int >& lim, bool F_or_equal )
    {
        auto&& it_l = lil.begin();
        auto&& it_m = lim.begin();

        while( true )
        {
            if     ( it_l != lil.end() && it_m == lim.end() ) return false;
            else if( it_l == lil.end() && it_m != lim.end() ) return true;
            else if( it_l == lil.end() && it_m == lim.end() ) break;

            if     ( *it_l > *it_m ) return false;
            else if( *it_l < *it_m ) return true;

            ++it_l;
            ++it_m;
        }

        return F_or_equal;
    };

    if     ( m_F_incl_b )
    {
        if( less_than( num, num_b, false ) ) res_b = false;
    }
    else if( less_than( num, num_b, true ) ) res_b = false;

    if( m_F_incl_e )
    {
        if( less_than( num_e, num, false ) ) res_e = false;
    }
    else if( less_than( num_e, num, true ) ) res_e = false;

    return( res_b && res_e );
}

void
FiltererBetweenEntries::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFe", m_F_incl_b ? '[' : '(',
                                    get_id_raw_failsafe( m_entry_b ),
                                    m_F_incl_e ? '[' : '(',
                                    get_id_raw_failsafe( m_entry_e ) );
}
Ustring
FiltererBetweenEntries::get_as_human_readable_str() const
{
    return STR::compose( is_not() ? _( "Entry is NOT between" ) : _( "Entry is between" ), " ",
                         m_F_incl_b ? '[' : '(',
                         m_entry_b ? "\"" + m_entry_b->get_name() + "\"" : "--", "..",
                         m_entry_e ? "\"" + m_entry_e->get_name() + "\"" : "--", "..",
                         m_F_incl_e ? ']' : ')' );
}

// FILTERER COMPLETION =============================================================================
bool
FiltererCompletion::filter( const Entry* entry ) const
{
    const double completion{ entry->get_completion() * 100.0 };
    if( m_compl_b <= m_compl_e )
        return( completion >= m_compl_b && completion <= m_compl_e );
    else
        return( completion >= m_compl_b || completion <= m_compl_e );
}

void
FiltererCompletion::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFc", m_compl_b, '&', m_compl_e );
}
Ustring
FiltererCompletion::get_as_human_readable_str() const
{
    return STR::compose( is_not() ? _( "Completion is NOT within" )
                                  : _( "Completion is within" ), " [",
                         m_compl_b, "..", m_compl_e, ']' );
}

// FILTERER CONTAINS TEXT ==========================================================================
bool
FiltererContainsText::filter( const Entry* entry ) const
{
    const Ustring text { ( m_case_sensitive && !m_use_regex ) ? m_text
                                                              : STR::lowercase( m_text ) };

    for( Paragraph* p = entry->get_paragraph_1st(); p; p = p->m_p2next )
    {
        if( m_use_regex || m_case_sensitive )
        {
            if( !m_use_regex )
            {
                if( p->get_text().find( text ) != Ustring::npos ) return true;
            }
            else
            {
                Glib::MatchInfo match_info;
                if( m_regex->match( p->get_text(), match_info ) ) return true;
            }
        }
        else
            if( STR::lowercase( p->get_text() ).find( text ) != Ustring::npos )
                return true;

        if( m_name_only ) break;
    }

    return false;
}
bool
FiltererContainsText::filter( const Paragraph* para ) const
{
    if     ( m_use_regex )
    {
        Glib::MatchInfo match_info;
        return( m_regex->match( para->get_text(), match_info ) );
    }
    else if( m_case_sensitive )
    {
        return( para->get_text().find( m_text ) != Ustring::npos );
    }
    else
        return( STR::lowercase( para->get_text() ).find( STR::lowercase( m_text ) )
                !=
                Ustring::npos );
}

void
FiltererContainsText::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFx~~~", m_use_regex ? 'R' : '_',
                                       m_case_sensitive ? 'C' : '_',
                                       m_name_only ? 'N' : '_',
                                       m_text );
}
Ustring
FiltererContainsText::get_as_human_readable_str() const
{
    return Glib::ustring::compose( _( "%1 %2 %3 %4 %5" ),
                                   ( m_name_only ? _( "Title" ) : _( "Text" ) ),
                                   ( is_not() ? _( "does NOT contain" ) : _( "contains" ) ),
                                   ( m_use_regex ? _( "the regex" ) : _( "the text" ) ),
                                   STR::compose( " \"", m_text, "\"" ),
                                   ( m_case_sensitive ? "  aA" : "" ) );
}

void
FiltererContainsText::update_regex()
{
    if( m_use_regex )
    {
        try
        {
            if( m_case_sensitive )
                m_regex = Glib::Regex::create( m_text, Glib::Regex::CompileFlags::UNGREEDY );
            else
                m_regex = Glib::Regex::create( m_text, Glib::Regex::CompileFlags::UNGREEDY |
                                                       Glib::Regex::CompileFlags::CASELESS );
        }
        catch ( const Glib::RegexError& e )
        {
            print_error( "Regex error:", e.what() );
        }
    }
}
// FILTERER IS CURRENT ENTRY =======================================================================
bool
FiltererIsCurrentEntry::filter( const Entry* entry ) const
{
#ifndef __ANDROID__
    return( AppWindow::p->UI_entry->is_cur_entry( entry, m_F_include_descendants ) );
#else
    return false; // TODO: stub!
#endif
}
bool
FiltererIsCurrentEntry::filter( const Paragraph* para ) const
{
    return( filter( para->m_host ) );
}

void
FiltererIsCurrentEntry::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFk", m_F_include_descendants ? 'D' : '_' );
}
Ustring
FiltererIsCurrentEntry::get_as_human_readable_str() const
{
    if( is_not() )
    {
        if( m_F_include_descendants )
            return _( "Is NOT the current entry nor one of its descendatns" );
        else
            return _( "Is NOT the current entry" );
    }
    else
    {
        if( m_F_include_descendants )
            return _( "Is the current entry or one of its descendatns" );
        else
            return _( "Is the current entry" );
    }
}

// FILTERER CHILD FILTER ===========================================================================
FiltererChildFilter::FiltererChildFilter( FiltererContainer* ctr, Filter* filter )
: Filterer( ctr ), m_p2filter( filter )
{
    if( m_p2filter ) m_FC_stack = m_p2filter->get_filterer_stack();
}
FiltererChildFilter::~FiltererChildFilter()
{
    if( m_FC_stack ) delete m_FC_stack;
}

bool
FiltererChildFilter::filter( const Entry* entry ) const
{
    return( m_FC_stack ? m_FC_stack->filter( entry ) : true );
}

bool
FiltererChildFilter::filter( const Paragraph* para ) const
{
    return( m_FC_stack ? m_FC_stack->filter( para ) : true );
}

void
FiltererChildFilter::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFl", get_id_raw_failsafe( m_p2filter ) );
}
Ustring
FiltererChildFilter::get_as_human_readable_str() const
{
    return( STR::compose( is_not() ? _( "Does NOT satisfy the filter" )
                                   : _( "Satisfies the filter" ), " ",
                          m_p2filter ? "\"" + m_p2filter->get_name() + "\"" : "--" ) );
}

// FILTERER IS IMAGE ===============================================================================
bool
FiltererHasImage::filter( const Paragraph* para ) const
{
    return( para->is_image() );
}

void
FiltererHasImage::get_as_string( Ustring& string ) const
{
    string += "\nFg";
}
Ustring
FiltererHasImage::get_as_human_readable_str() const
{
    return( is_not() ? _( "Does NOT have image" ) : _( "Has image" ) );
}

// FILTERER HAS COORD ==============================================================================
bool
FiltererHasCoords::filter( const Entry* entry ) const
{
    return( entry->has_location() || entry->get_map_path_length() > 0.0 );
}

bool
FiltererHasCoords::filter( const Paragraph* para ) const
{
    return para->has_property( PROP::LOCATION );
}

void
FiltererHasCoords::get_as_string( Ustring& string ) const
{
    string += "\nFo";
}
Ustring
FiltererHasCoords::get_as_human_readable_str() const
{
    return( is_not() ? _( "Does NOT have map location" ) : _( "Has map location" ) );
}

// FILTERER TITLE TYPE =============================================================================
bool
FiltererTitleStyle::filter( const Entry* entry ) const
{
    return( entry->get_title_style() == VT::get_v< VT::ETS, int, char >( m_title_style ) );
}

void
FiltererTitleStyle::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFy", m_title_style );
}
Ustring
FiltererTitleStyle::get_as_human_readable_str() const
{
    Ustring str { STR::compose( is_not() ? _( "Title style is NOT" )
                                         : _( "Title style is" ), " " ) };

    switch( m_title_style )
    {
        case VT::ETS::NAME_AND_DESCRIPT::C: str += VT::ETS::NAME_AND_DESCRIPT::S; break;
        case VT::ETS::NUMBER_AND_NAME::C:   str += VT::ETS::NUMBER_AND_NAME::S; break;
        case VT::ETS::NAME_ONLY::C:         str += VT::ETS::NAME_ONLY::S; break;
        case VT::ETS::DATE_AND_NAME::C:     str += VT::ETS::DATE_AND_NAME::S; break;
        case VT::ETS::MILESTONE::C:         str += VT::ETS::MILESTONE::S; break;
    }

    return str;
}

// FILTERER HEADING LEVEL ==========================================================================
bool
FiltererHeadingLevel::filter( const Paragraph* para ) const
{
    return( m_H_levels.find( VT::get_v< VT::PHS, char, int >( para->get_heading_level() ) ) !=
            String::npos );
}

void
FiltererHeadingLevel::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF#~~~~", m_H_levels );
}
Ustring
FiltererHeadingLevel::get_as_human_readable_str() const
{
    Ustring str { STR::compose( is_not() ? _( "Heading level is NOT one of" )
                                         : _( "Heading level is one of" ), " {" ) };
    bool    F_add_comma { false };
    
    for( char c : m_H_levels )
    {
        if( F_add_comma ) str += ", "; else F_add_comma = true;
        str += VT::get_v< VT::PHS, const char*, char >( c );
    }

    str += "}";

    return str;
}

// FILTERER PARA TYPE ==============================================================================
bool
FiltererListType::filter( const Paragraph* para ) const
{
    return( para->get_list_type() == m_type );
}

void
FiltererListType::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF*~~~~~", VT::get_v< VT::PLS, char, int >( m_type ) );
}
Ustring
FiltererListType::get_as_human_readable_str() const
{
    Ustring str { STR::compose( is_not() ? _( "Paragraph type is NOT" )
                                         : _( "Paragraph type is" ),
                                " ", VT::get_v< VT::PLS, char const *, int >( m_type ) ) };
    return str;
}

// FILTERER INDENTATION LEVEL ======================================================================
bool
FiltererIndentationLevel::filter( const Paragraph* para ) const
{
    switch( m_relation )
    {
        default:  return( para->get_indent_level() == m_indentation );
        case '<': return( para->get_indent_level() < m_indentation );
        case '>': return( para->get_indent_level() > m_indentation );
    }
}

void
FiltererIndentationLevel::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF>>~~~~~", m_relation, m_indentation );
}
Ustring
FiltererIndentationLevel::get_as_human_readable_str() const
{
    Ustring str { STR::compose( is_not() ? _( "Indentation level is NOT" )
                                         : _( "Indentation level is" ),
                                " ", m_relation, " ", m_indentation ) };
    return str;
}

// FILTERER HAS FORMAT =============================================================================
bool
FiltererHasFormat::filter( const Paragraph* para ) const
{
    for( auto f : para->m_formats )
        if( f->type == m_format || ( f->type & m_format & VT::HFT_F_LINK_MANUAL ) ) return true;
    return false;
}

void
FiltererHasFormat::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nFm~~", VT::get_v< VT::FMT, char, int >( m_format ) );
}
Ustring
FiltererHasFormat::get_as_human_readable_str() const
{
    return( Ustring::compose( _( "%1 the format: %2" ),
                              ( is_not() ? _( "Has NOT" ) : _( "Has" ) ),
                              VT::get_v< VT::FMT, const char*, int >( m_format ) ) );
}

// FILTERER EQUALS =================================================================================
bool
FiltererEquals::filter( const double value ) const
{
    switch( m_relation )
    {
        default:  return( value == m_value );
        case '<': return( value < m_value );
        case '>': return( value > m_value );
    }
}

void
FiltererEquals::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF", m_relation, m_value );
}
Ustring
FiltererEquals::get_as_human_readable_str() const
{
    return( STR::compose( is_not() ? _( "Value is NOT" ) : _( "Value is" ), " ",
                          m_relation, " ", m_value ) );
}

// FILTERER SCRIPT =================================================================================
bool
FiltererScript::filter( const double value ) const
{
    if( !m_script.empty() )
        return PyBindings::run_script_name_return< bool, const double& >( m_script, "run", value );
    else
        return true;
}
bool
FiltererScript::filter( const Paragraph* p ) const
{
    if( !m_script.empty() )
        return PyBindings::run_script_name_return< bool, const Paragraph*& >( m_script, "run", p );
    else
        return true;
}
bool
FiltererScript::filter( const Entry* e ) const
{
    if( !m_script.empty() )
        return PyBindings::run_script_name_return< bool, const Entry*& >( m_script, "run", e );
    else
        return true;
}

void
FiltererScript::get_as_string( Ustring& string ) const
{
    string += STR::compose( "\nF$~~~~~~~~~~", m_script );
}
Ustring
FiltererScript::get_as_human_readable_str() const
{
    return( Ustring::compose( is_not() ? _( "Does not satisfy script %1" )
                                       : _( "Satisfies script %1" ), m_script ) );
}

// FILTERER CONTAINER ==============================================================================
// this is re-implemented in WidgetFilter:
void
FiltererContainer::remove_filterer( Filterer* filterer )
{
    // NOTE: no longer deletes the filterer as it causes conflicts with the GUI version.
    // manually delete the filterer if this is called from a non-GUI context

    for( auto&& iter = m_pipeline.begin(); iter != m_pipeline.end(); iter++ )
    {
        if( *iter == filterer )
        {
            m_pipeline.erase( iter );
            // delete filterer;
            break;
        }
    }
}

// this is re-implemented in WidgetFilter:
void
FiltererContainer::clear_pipeline()
{
    for( auto& filterer : m_pipeline )
        delete filterer;

    m_pipeline.clear();
}

void
FiltererContainer::get_as_string( Ustring& string ) const
{
    if( !m_p2container ) // top level
    {
        string += ( m_F_or ? "F|" : "F&" );
        string += "\nFV1"; // definition version
    }
    else
        string += "\nF(";

    for( auto& filterer : m_pipeline )
    {
        filterer->get_as_string( string );
        if( filterer->is_not() )
            string += "\nF!";
    }

    if( m_p2container != nullptr ) // not top level
        string += "\nF)";
}
Ustring
FiltererContainer::get_as_human_readable_str() const
{
    return STR::compose( m_F_or ? _( "Satisfies at least one of" ) : _( "Satisfies all of"), ":" );
}

void
FiltererContainer::set_from_string( const Ustring& string )
{
    // when a new filter is added, update WidgetFilter::can_definition_filter_class(), too
    if( m_p2container ) // only top level can do this
        return;

    String              line;
    UstringSize         line_offset { 0 };
    FiltererContainer*  container   { this };
    int                 version     { 0 };

    clear_pipeline();

    while( STR::get_line( string, line_offset, line ) )
    {
        if( line.size() < 2 )   // should never occur
            continue;

        switch( line[ 1 ] )
        {
            case 'V':   // version
                version = std::stoi( line.substr( 2 ) );
                break;
            case 'b':   // subtagged by (DEID)
            {
                int        index { 2 };
                const auto id_p  { D::DEID( LoGID32( STR::get_i32( line, index ) ) ) };
                const auto rel   { line[ ++index ] };
                ++index;
                const auto id_c  { D::DEID( LoGID32( STR::get_i32( line, ++index ) ) ) };
                const auto type  { STR::get_i32( line, ++index ) };
                container->add< FiltererSubtaggedBy >( m_p2diary->get_tag_by_id( id_p ),
                                                       m_p2diary->get_tag_by_id( id_c ),
                                                       rel, type );
                break;
            }
            case 'c':   // completion
            {
                int i_double { 2 };
                const double double_b{ STR::get_d( line, i_double ) };
                i_double += 2;
                const double double_e{ STR::get_d( line, i_double ) };
                container->add< FiltererCompletion >( double_b, double_e );
                break;
            }
            case 'd':   // between dates
            {
                int         i_date{ 3 };
                const DateV date_b{ STR::get_i64( line, i_date ) };
                const int   i_f_icl_e = i_date;
                i_date++;
                const DateV date_e{ STR::get_i64( line, i_date ) };
                container->add< FiltererBetweenDates >(
                        date_b, line[ 2 ] == '[', date_e, line[ i_f_icl_e ] == '[' );
                break;
            }
            case 'e':   // between entries (DEID)
            {
                int        i_id       { 3 };
                const auto id_b       { D::DEID( LoGID32( STR::get_i32( line, i_id ) ) ) };
                const int  i_f_icl_e  { i_id };
                i_id++;
                const auto id_e       { D::DEID( LoGID32( STR::get_i32( line, i_id ) ) ) };
                container->add< FiltererBetweenEntries >(
                        m_p2diary->get_entry_by_id( id_b ), line[ 2 ] == '[',
                        m_p2diary->get_entry_by_id( id_e ), line[ i_f_icl_e ] == '[' );
                break;
            }
            case 'f':   // favorite
                container->add< FiltererFavorite >();
                if( line[ 2 ] == 'n' ) // support for old diaries
                    set_last_filterer_not();
                break;
            case 'g':   // is image
                container->add< FiltererHasImage >();
                break;
            case 'h':   // theme (DEID)
            {
                if( version < 1 )
                {
                    const Ustring&& name{ line.substr( 3 ) };
                    container->add< FiltererTheme >( m_p2diary->get_theme( name ) );
                    if( line[ 2 ] == 'F' ) // support for old diaries
                        set_last_filterer_not();
                }
                else
                {
                    const auto id    { D::DEID{ line.substr( 3 ) } };
                    const auto theme { m_p2diary->get_element2< Theme >( id ) };
                    container->add< FiltererTheme >( theme );
                }
                break;
            }
            case 'i':   // is entry (DEID) --legacy
            {
                const auto id { D::DEID{ line.substr( 3 ) } };
                container->add< FiltererIs >( id, VT::OP_DEPTH::ITSELF::I );
                if( line[ 2 ] == 'F' ) // support for old diaries
                    set_last_filterer_not();
                break;
            }
            case 'j':   // between date offsets
            {
                UstringSize pos { 4 }; // 2 and 3 are reserved
                const int offset_bgn { std::stoi( STR::get_substr_delim( line, pos, '|' ) ) };
                const int offset_end { std::stoi( line.substr( pos + 1 ) ) };
                container->add< FiltererBetweenDateOffsets >( line[ 3 ], offset_bgn, offset_end );
                break;
            }
            case 'k':   // is current entry
            {
                container->add< FiltererIsCurrentEntry >( line[ 2 ] == 'D' );
                break;
            }
            case 'l':   // child filter (DEID)
            {
                container->add< FiltererChildFilter >(
                        m_p2diary->get_filter( D::DEID{ line.substr( 2 ) } ) );
                break;
            }
            case 'm':   // has format
            {
                container->add< FiltererHasFormat >( line[ 4 ] );
                break;
            }
            case 'n':   // is entry or descendant of (DEID)
            {
                const auto id { D::DEID( line.substr( 9 ) ) };
                if     ( line[ 3 ] == 'I' )
                    container->add< FiltererIs >( id, VT::OP_DEPTH::ITSELF_AND_DESCS::I );
                else if( line[ 3 ] == '_' )
                    container->add< FiltererIs >( id, VT::OP_DEPTH::DESCENDANTS::I );
                else
                    container->add< FiltererIs >(
                            id, VT::get_v< VT::OP_DEPTH, int, char>( line[ 4 ] ) );

                if( line[ 2 ] == 'F' ) // support for old diaries
                    set_last_filterer_not();
                break;
            }
            case 'o':   // has map coordinates (bool)
                container->add< FiltererHasCoords >();
                break;
            case 'r':   // tagged/referenced by (DEID)
            {
                const auto id{ D::DEID( line.substr( 3 ) ) };
                container->add< FiltererTaggedBy >( m_p2diary->get_tag_by_id( id ),
                                                    line[ 2 ] == 'P' );
                if( line[ 2 ] == 'F' ) // support for old diaries
                    set_last_filterer_not();
                break;
            }
            case 's':   // status (ElemStatus)
            {
                ElemStatus status{ 0 };
                if( line.size() > 6 )
                {
                    if( line[ 2 ] == 'N' ) status |= ES::SHOW_NOT_TODO;
                    if( line[ 3 ] == 'O' ) status |= ES::SHOW_TODO;
                    if( line[ 4 ] == 'P' ) status |= ES::SHOW_PROGRESSED;
                    if( line[ 5 ] == 'D' ) status |= ES::SHOW_DONE;
                    if( line[ 6 ] == 'C' ) status |= ES::SHOW_CANCELED;
                }
                container->add< FiltererStatus >( status );
                break;
            }
            case 't':   // trashed
                container->add< FiltererTrashed >();
                if( line[ 2 ] == 'n' ) // support for old diaries
                    set_last_filterer_not();
                break;
            case 'u':   // unit
                container->add< FiltererUnit >( line.substr( 3 ) );
                if( line[ 2 ] == 'F' ) // support for old diaries
                    set_last_filterer_not();
                break;
            case 'v':   // tag value
            {
                int          index    { 2 };
                const auto   id_tag   { D::DEID( LoGID32( STR::get_i32( line, index ) ) ) };
                const char   type_ch  { line[ index ] };
                const bool   f_incl_b { line[ ++index ] == '[' };
                const double range_b  { STR::get_d( line, ++index, Constants::INFINITY_MNS ) };
                const bool   f_incl_e { line[ ++index ] == '[' };
                const double range_e  { STR::get_d( line, ++index, Constants::INFINITY_PLS ) };
                int          type     { VT::get_v< VT::TVTS, int, char >( type_ch ) };

                container->add< FiltererTagValue >( m_p2diary->get_tag_by_id( id_tag ), type,
                                                    range_b, f_incl_b, range_e, f_incl_e );
                break;
            }
            // case 'w':   // dropped, was used by Owner
            //     break;
            case 'x':   // contains text (Ustring)
                if( line.size() > 8 )
                    container->add< FiltererContainsText >( line.substr( 8 ),
                                                            line[ 6 ] == 'C', line[ 5 ] == 'R',
                                                            line[ 7 ] == 'N' );

                break;
            case 'y':   // title style
                container->add< FiltererTitleStyle >( line[ 2 ] );
                break;
            case 'z':   // size
            {
                int          index    { 2 };
                const char   type_ch  { line[ index ] };
                const bool   f_incl_b { line[ ++index ] == '[' };
                const double range_b  { STR::get_d( line, ++index ) };
                const bool   f_incl_e { line[ ++index ] == '[' };
                const double range_e  { STR::get_d( line, ++index ) };

                container->add< FiltererSize >( type_ch, range_b, f_incl_b, range_e, f_incl_e );
                break;
            }
            case '>':
                if( line[ 2 ] == '>' )
                {
                    container->add< FiltererIndentationLevel >( line[ 8 ],
                                                                std::stoi( line.substr( 9 ) ) );
                    break;
                }
                // no break;
            case '=':   // equals
            case '<':
            {
                int i_double{ 2 };
                const double value{ STR::get_d( line, i_double ) };
                container->add< FiltererEquals >( line[ 1 ], value );
                break;
            }
            case '#':   // haeding level
                if( line.size() > 6 )
                    container->add< FiltererHeadingLevel >( line.substr( 6 ) );
                break;
            case '*':   // para list type
                container->add< FiltererListType >( VT::get_v< VT::PLS, int, char >( line[ 7 ] ) );
                break;
            case '$':   // script
                if( line.size() > 12 )
                    container->add< FiltererScript >( line.substr( 12 ) );
                break;
            case '(':   // sub group
                container = container->add< FiltererContainer >();
                break;
            case ')':   // end of sub group
                container = this;
                break;
            case '@':   // defines tag
                container->add< FiltererDefinesTag >();
                break;
            case '|':
                m_F_or = true;
                break;
            case '&':
                m_F_or = false;
                break;
            case '!':
                set_last_filterer_not();
                break;
            default:
                PRINT_DEBUG( "Unrecognized filter string: ", line );
                break;
        }
    }
}

const Ustring Filter::DEFINITION_DEFAULT    { "F&\nFt~\nF!\nFsNOPdc" };
const Ustring Filter::DEFINITION_MINIMAL    { "F&" };
const Ustring Filter::DEFINITION_CUR_ENTRY  { "F&\nFk" };
const Ustring Filter::DEFINITION_NONTRASHED { "F&\nFt~\nF!" };
const Ustring Filter::DEFINITION_TRASHED    { "F&\nFt~" };

const R2Pixbuf&
Filter::get_icon() const
{
    return Lifeograph::icons->filter;
}

FiltererContainer*
Filter::get_filterer_stack() const
{
    if( m_definition.empty() )
        return nullptr;

    return( new FiltererContainer( m_p2diary, m_definition ) );
}

bool
Filter::can_filter_class( int object_class ) const
{
    auto  FC_stack    { get_filterer_stack() };
    int   obj_classes { FC_stack->calculate_obj_classes() };

    delete FC_stack;

    // always accept empty filters:
    return( !obj_classes || ( obj_classes & object_class ) );
}

SKVVec
Filter::get_as_skvvec() const
{
    SKVVec sv;
    sv.push_back( { CSTR::NAME, get_name() } );

    auto FC_stack { get_filterer_stack() };

    sv.push_back( { CSTR::DEFINITION, FC_stack->get_as_human_readable_str() } );

    for( auto filterer : FC_stack->m_pipeline )
    {
        sv.push_back( { CSTR::DEFINITION, "  " + filterer->get_as_human_readable_str() } );

        if( filterer->is_container() )
        {
            auto container { dynamic_cast< FiltererContainer* >( filterer ) };
            for( auto subfltrer : container->m_pipeline )
                sv.push_back( { CSTR::DEFINITION, "    " + subfltrer->get_as_human_readable_str() } );
        }
    }

    delete FC_stack;

    return sv;
}

// UI FUNCTIONS ====================================================================================
#ifndef __ANDROID__
void FiltererStatus::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererStatusUI, FiltererStatus >( this ); }

void FiltererSize::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererSizeUI, FiltererSize >( this ); }

void FiltererFavorite::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererFavoriteUI, FiltererFavorite >( this ); }

void FiltererTrashed::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererTrashedUI, FiltererTrashed >( this ); }

void FiltererUnit::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererUnitUI, FiltererUnit >( this ); }

void FiltererIs::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererIsUI, FiltererIs >( this ); }

void FiltererTaggedBy::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererTaggedByUI, FiltererTaggedBy >( this ); }

void FiltererSubtaggedBy::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererSubtaggedByUI,FiltererSubtaggedBy >( this ); }

void FiltererDefinesTag::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererDefinesTagUI,FiltererDefinesTag >( this ); }

void FiltererTagValue::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererTagValueUI, FiltererTagValue >( this ); }

void FiltererTheme::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererThemeUI, FiltererTheme >( this ); }

void FiltererBetweenDates::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererBetweenDatesUI, FiltererBetweenDates >( this ); }

void FiltererBetweenDateOffsets::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererBetweenDateOffsetsUI, FiltererBetweenDateOffsets >( this ); }

void FiltererBetweenEntries::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererBetweenEntriesUI, FiltererBetweenEntries >( this ); }

void FiltererCompletion::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererCompletionUI, FiltererCompletion >( this ); }

void FiltererContainsText::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererContainsTextUI, FiltererContainsText >( this ); }

void FiltererChildFilter::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererChildFilterUI, FiltererChildFilter >( this ); }

void FiltererHasImage::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererHasImageUI, FiltererHasImage >( this ); }

void FiltererHasCoords::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererHasCoordsUI, FiltererHasCoords >( this ); }

void FiltererTitleStyle::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererTitleStyleUI, FiltererTitleStyle >( this ); }

void FiltererHeadingLevel::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererHeadingLevelUI, FiltererHeadingLevel >( this ); }

void FiltererListType::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererListTypeUI, FiltererListType >( this ); }

void FiltererIndentationLevel::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererIndentationLevelUI, FiltererIndentationLevel >( this ); }

void FiltererHasFormat::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererHasFormatUI, FiltererHasFormat >( this ); }

void FiltererEquals::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererEqualsUI, FiltererEquals >( this ); }

void FiltererScript::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererScriptUI, FiltererScript >( this ); }

void FiltererContainer::initialize_ui( FiltererContainerUI* p2FC )
{ p2FC->add_filterer< FiltererContainerUI, FiltererContainer >( this ); }
#else
void FiltererStatus::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererSize::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererFavorite::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererTrashed::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererUnit::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererIs::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererTaggedBy::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererSubtaggedBy::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererDefinesTag::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererTagValue::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererTheme::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererBetweenDates::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererBetweenDateOffsets::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererBetweenEntries::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererCompletion::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererContainsText::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererChildFilter::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererHasImage::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererHasCoords::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererTitleStyle::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererHeadingLevel::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererListType::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererIndentationLevel::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererHasFormat::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererEquals::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererScript::initialize_ui( FiltererContainerUI* p2FC ) {}
void FiltererContainer::initialize_ui( FiltererContainerUI* p2FC ) {}
#endif
