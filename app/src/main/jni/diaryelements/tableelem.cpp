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

#ifndef __ANDROID__
#include <pybind11/embed.h>
#endif

#include <random>

#include "diarydata.hpp"
#include "../helpers.hpp"
#include "tableelem.hpp"
#include "diary.hpp"
#include "../lifeograph.hpp"

#ifndef __ANDROID__
#include "../python/py_bindings.hpp"
#endif


using namespace LoG;

// TABLE COLUMN ====================================================================================
TableColumn::TableColumn( TableData* p2data, int id, Type t )
:   Named( get_sstr( CSTR::NEW_COLUMN ) ), m_id( id ), m_p2data( p2data ),
    m_type( t )
{ }

bool
TableColumn::set_type( Type type )
{
    if( type == TCT_GANTT )
    {
        if( m_p2data->m_p2col_gantt )
            return false;
        else
            m_p2data->m_p2col_gantt = this;
    }
    else if( m_p2data->m_p2col_gantt == this )
        m_p2data->m_p2col_gantt = nullptr;

    m_type = type;
    m_summary_func = ( is_numeric() ? VT::SUMF::SUM::C : VT::SUMF::NONE::C );

    return true;
}

bool
TableColumn::is_source_elem_para() const
{
    return( m_p2data->m_F_para_based ? m_source_elem >= VT::SRC_ITSELF
                                     : m_source_elem > VT::SRC_ITSELF );
}

bool
TableColumn::is_numeric() const
{
    switch( m_type )
    {
        // case TCT_ORDER: order is treated as text
        case TCT_NUMBER:
        case TCT_DURATION:
        case TCT_SIZE:
        case TCT_TAG_V:
        case TCT_COMPLETION:
        case TCT_PATH_LENGTH:
        case TCT_ARITHMETIC:
            return true;
        case TCT_SUBTAG:
        case TCT_REFERRERS:
            return is_counting();
        case TCT_SCRIPT:
            return m_opt_int1;
        default:
            return false;
    }
}
bool
TableColumn::is_enumerable( bool F_for_sorting ) const
// F_for_sorting: some columns can be enumerated under certain conditions for sorting
{
    switch( m_type )
    {
        case TCT_DURATION:
        case TCT_SIZE:
        case TCT_TAG_V:
        case TCT_COMPLETION:
        case TCT_PATH_LENGTH:
        case TCT_ARITHMETIC:
            return true;
        case TCT_DATE:
            // when sorting dates use the string version for custom formats
            return( !F_for_sorting || m_opt_str.empty() );
        case TCT_BOOL:
        case TCT_TODO_STATUS:
        case TCT_GANTT:
            return F_for_sorting;
        case TCT_SUBTAG:
            return( is_counting() || get_opt_int() != VT::TCOS_ALL );
        case TCT_REFERRERS:
            return( is_counting() );
        case TCT_SCRIPT:
            return m_opt_int1;
        default:
            return false;
    }
}

//bool
//TableColumn::is_fraction() const
//{
//    switch( m_type )
//    {
//        case TCT_COMPLETION:
//            return true;
//        default:
//            return false;
//    }
//}

bool
TableColumn::is_percentage() const
{
    switch( m_type )
    {
        case TCT_COMPLETION:
            return true;
        case TCT_ARITHMETIC:
            return( ( m_opt_int & VT::TCAf::FILTER ) == VT::TCAf::PTG::I );
        case TCT_TAG_V:
            return( m_opt_int == VT::TVT::COMPL_PERCENTAGE::I );
        case TCT_SCRIPT:
            return( m_opt_double1 != 0.0 );
        default:
            return false;
    }
}

Ustring
TableColumn::get_unit() const
{
    switch( m_type )
    {
        case TCT_DURATION:      return _( "days" );
        case TCT_SIZE:          return _( "chars" );
        case TCT_TAG_V:         return( m_p2tag ? m_p2tag->get_unit() : "" );
        case TCT_PATH_LENGTH:   return( Lifeograph::settings.use_imperial_units ? "mi" : "km" );
        default:                return "";
    }
}

Pango::Alignment
TableColumn::get_alignment() const
{
    if( m_type == TCT_TODO_STATUS )
        return Pango::Alignment::CENTER;
    else
    if( is_numeric() )
        return Pango::Alignment::RIGHT;
    else
        return Pango::Alignment::LEFT;
}

void
TableColumn::allocate_filter_stacks()
{
    if( m_type == TCT_BOOL )
    {
        auto filter{ m_p2data->m_p2diary->get_filter( LoGID32( m_opt_int ) ) };
        if( filter )
            m_FC_generic = filter->get_filterer_stack();
    }

    if( ( m_source_elem == VT::SRC_PARENT_FILTER ||
          m_source_elem >= VT::SRC_FCHILD_FILTER ) && m_filter_source_elem )
        m_FC_source_elem = m_filter_source_elem->get_filterer_stack();

    if( m_p2filter_count && is_counting() )
        m_FC_count = m_p2filter_count->get_filterer_stack();
}
void
TableColumn::deallocate_filter_stacks()
{
    if( m_FC_generic )
    {
        delete m_FC_generic;
        m_FC_generic = nullptr;
    }

    if( m_FC_source_elem )
    {
        delete m_FC_source_elem;
        m_FC_source_elem = nullptr;
    }

    if( m_FC_count )
    {
        delete m_FC_count;
        m_FC_count = nullptr;
    }
}

const Entry*
TableColumn::get_src_entry_as_needed( const Entry* entry ) const
{
    const Entry* ebgn{ m_p2data->m_F_para_based ? entry : entry->get_parent() };

    switch( m_source_elem )
    {
        case VT::SRC_PARENT_FILTER:
            if( m_FC_source_elem )
                for( const Entry* epf = ebgn; epf; epf = epf->get_parent() )
                {
                    if( m_FC_source_elem->filter( epf ) )
                        return epf;
                }
            break;
        case VT::SRC_GRANDPARENT:
            return( ebgn ? ebgn->get_parent() : nullptr );
        case VT::SRC_PARENT:
            return ebgn;
        case VT::SRC_ITSELF:
            return entry;
    }

    return nullptr;
}
const Paragraph*
TableColumn::get_src_para_as_needed( const Entry* entry ) const
{
    if( !m_FC_source_elem ) return nullptr;

    if     ( m_source_elem == VT::SRC_FCHILD_FILTER )
    {
        for( const Paragraph* para = entry->get_paragraph_1st(); para; para = para->m_p2next )
        {
            if( m_FC_source_elem->filter( para ) )
                return para;
        }
    }
    else if( m_source_elem == VT::SRC_LCHILD_FILTER )
    {
        for( const Paragraph* para = entry->get_paragraph_last(); para; para = para->m_p2prev )
        {
            if( m_FC_source_elem->filter( para ) )
                return para;
        }
    }

    return nullptr;
}
const Paragraph*
TableColumn::get_src_para_as_needed( const Paragraph* para ) const
{
    if( !m_FC_source_elem ) return nullptr;

    if     ( m_source_elem == VT::SRC_FCHILD_FILTER ) // here it means previous para satisfying
    {
        for( const Paragraph* p = para->m_p2prev; p; p = p->m_p2prev )
        {
            if( m_FC_source_elem->filter( p ) )
                return p;
        }
    }
    else if( m_source_elem == VT::SRC_LCHILD_FILTER ) // here it means next para satisfying
    {
        for( const Paragraph* p = para->m_p2next; p; p = p->m_p2next )
        {
            if( m_FC_source_elem->filter( p ) )
                return p;
        }
    }

    return nullptr;
}

void
TableColumn::calculate_value_num( const Entry* entry, TableLine* line ) const
{
    Value value  { 0.0 };
    Value weight { 1.0 };

    if     ( m_source_elem > VT::SRC_ITSELF )
    {
        auto para_src{ get_src_para_as_needed( entry ) };
        if( para_src )
        {
            calculate_value_num( para_src, line );
            return;
        }
        else
            entry = nullptr; // to make it skip the processing
    }
    else if( m_source_elem < VT::SRC_ITSELF )
        entry = get_src_entry_as_needed( entry );

    if( entry )
    {
        switch( m_type )
        {
            case TCT_NUMBER:
                value = entry->get_sibling_order();
                break;
            case TCT_DURATION:
                value = calculate_duration( entry, line );
                break;
            case TCT_SIZE:
                value = entry->get_size_adv( char( m_opt_int ) );
                break;
            case TCT_BOOL:
                value = ( m_FC_generic ? ( m_FC_generic->filter( entry ) ? 1.0 : 0.0 ) : 0.0 );
                break;
            case TCT_COMPLETION:
                value = entry->get_completed();
                weight = calculate_weight( entry );
                break;
            case TCT_TODO_STATUS:
                value = entry->get_todo_status_effective();
                break;
            case TCT_PATH_LENGTH:
                value = entry->get_map_path_length();
                break;
            case TCT_ARITHMETIC:
                value = calculate_arithmetic( line );
                break;
            case TCT_TAG_V:
                if( m_p2tag )
                {
                    const auto  F_avg{ bool( m_opt_int & VT::TVTC::AVERAGE::I ) };
                    Value       v;

                    if     ( m_opt_int & VT::TVTS::REMAINING::I )
                        v = entry->get_tag_value_remaining( m_p2tag, F_avg );
                    else if( m_opt_int & VT::TVTS::PLANNED::I )
                        v = entry->get_tag_value_planned( m_p2tag, F_avg );
                    else if( m_opt_int == VT::TVT::COMPL_PERCENTAGE::I )
                    {
                        v = entry->get_tag_value( m_p2tag, F_avg );
                        weight = entry->get_tag_value_planned( m_p2tag, F_avg );
                    }
                    else
                        v = entry->get_tag_value( m_p2tag, F_avg );

                    if( is_counting() && m_FC_count )
                    {
                        if( m_FC_count->filter( v ) )
                            value = 1;
                    }
                    else
                        value = v;
                }
                break;
            case TCT_SUBTAG:
                if( m_p2tag && is_counting() )
                {
                    DiaryElemTag* tag_sub { nullptr };
                    switch( m_opt_int )
                    {
                        case VT::FIRST:   tag_sub = entry->get_sub_tag_first( m_p2tag ); break;
                        case VT::LAST:    tag_sub = entry->get_sub_tag_last( m_p2tag ); break;
                        case VT::LOWEST:  tag_sub = entry->get_sub_tag_lowest( m_p2tag ); break;
                        case VT::HIGHEST: tag_sub = entry->get_sub_tag_highest( m_p2tag ); break;
                        case VT::TCOS_ALL:
                        {
                            auto&& subtags{ entry->get_sub_tags( m_p2tag ) };
                            if( m_p2filter_count )
                            {
                                int c{ 0 };
                                for( auto t : subtags ) { if( m_FC_count->filter( t ) ) c++; }
                                value = c;
                            }
                            else
                                value = subtags.size();
                        }
                        default:
                            break;
                    }

                    if( tag_sub )
                    {
                        if( !m_p2filter_count || m_FC_count->filter( tag_sub ) )
                            value = 1;
                    }
                }
                break;
            case TCT_REFERRERS:
                if( is_counting() )
                {
                    if( m_p2filter_count )
                    {
                        value = 0;
                        for( auto t : entry->get_references() )
                        { if( m_FC_count->filter( t ) ) ++value; }
                    }
                    else
                        value = entry->get_reference_count();
                            break;
                }
                break;
            case TCT_SCRIPT: // must never be handled here
            default:
                throw LoG::Error( "Illegal request!" );
        }
    }

    line->set_value_num( m_index, value, weight );
    format_number( line ); // always call after set_col_v_num()
}

Value
TableColumn::calculate_weight( const Entry* entry ) const
{
    if     ( m_source_elem > VT::SRC_ITSELF )
    {
        auto para_src{ get_src_para_as_needed( entry ) };
        return( para_src ? calculate_weight( para_src ) : 0.0 );
    }
    else if( m_source_elem < VT::SRC_ITSELF )
        entry = get_src_entry_as_needed( entry );
    if( !entry ) return 0.0;

    switch( m_type )
    {
        case TCT_COMPLETION:
            return entry->get_workload();
        default:
            return 0.0;
    }
}

Ustring
TableColumn::calculate_value_txt( const Entry* entry ) const
{
    if     ( m_source_elem > VT::SRC_ITSELF )
    {
        auto para_src{ get_src_para_as_needed( entry ) };
        return( para_src ? calculate_value_txt( para_src ) : "" );
    }
    else if( m_source_elem < VT::SRC_ITSELF )
        entry = get_src_entry_as_needed( entry );
    if( !entry ) return "";

    switch( m_type )
    {
        case TCT_TEXT:
        {
            Paragraph*  para{ nullptr };
            Ustring     text;
            switch( m_opt_int & VT::SEQ_FILTER )
            {
                case VT::TCT_SRC_ANCESTRAL:
                    text = entry->get_ancestry_path();
                    // no break:
                case VT::TCT_SRC_TITLE:         para = entry->get_paragraph_1st(); break;
                case VT::TCT_SRC_DESCRIPTION:   para = entry->get_description_para(); break;
                case VT::TCT_SRC_URI:           throw Error( "Impossible source" ); break;
            }

            if( m_opt_int & VT::TCT_CMPNT_NUMBER )
            {
                if( entry->get_title_style() == VT::ETS::NUMBER_AND_NAME::I )
                    // get the number from the entry...
                    text += ( entry->get_number_str() + "- " );
                else
                if( entry->get_title_style() == VT::ETS::DATE_AND_NAME::I )
                    text += ( Date::format_string( entry->get_date() ) + "- " );
            }

            if( para )
                // ...and not from the para
                text += para->get_text_stripped( m_opt_int & ~VT::TCT_CMPNT_NUMBER );
            return text;
        }
        case TCT_THEME:
            return entry->get_theme()->get_name();
        case TCT_SUBTAG:
            if( m_p2tag && !is_counting() && m_opt_int == VT::TCOS_ALL )
            {
                auto&& subtags{ entry->get_sub_tags( m_p2tag ) };
                Ustring tag_names;
                bool F_2nd_or_beyond{ false };
                for( auto t : subtags )
                {
                    if( F_2nd_or_beyond )
                        tag_names += "; ";
                    else
                        F_2nd_or_beyond = true;
                    tag_names += t->get_name();
                }
                return tag_names;
            }
            // no break
        case TCT_REFERRERS:
            if( !is_counting() )
            {
                Ustring tag_names;
                bool    F_2nd_or_beyond { false };
                for( auto t : entry->get_references() )
                {
                    if( F_2nd_or_beyond )
                        tag_names += "; ";
                    else
                        F_2nd_or_beyond = true;
                    tag_names += t->get_name();
                }
                return tag_names;
            }
            // no break
        default: // for other types this function should never be called in the first place
            return "internal error";
    }
}

DiaryElemTag*
TableColumn::calculate_value_tag( const Entry* entry ) const
{
    if     ( m_source_elem > VT::SRC_ITSELF )
    {
        auto para_src{ get_src_para_as_needed( entry ) };
        return( para_src ? calculate_value_tag( para_src ) : nullptr );
    }
    else if( m_source_elem < VT::SRC_ITSELF )
        entry = get_src_entry_as_needed( entry );
    if( !entry ) return nullptr;

    if( m_type == TCT_SUBTAG && m_p2tag && !is_counting() && m_opt_int != VT::TCOS_ALL )
    {
        switch( m_opt_int )
        {
            case VT::FIRST:   return entry->get_sub_tag_first( m_p2tag );
            case VT::LAST:    return entry->get_sub_tag_last( m_p2tag );
            case VT::LOWEST:  return entry->get_sub_tag_lowest( m_p2tag );
            case VT::HIGHEST: return entry->get_sub_tag_highest( m_p2tag );
            default:
                break;
        }
    }
    return nullptr;
}

DateV
TableColumn::calculate_value_date( const Entry* entry ) const
{
    if     ( m_source_elem > VT::SRC_ITSELF )
    {
        auto para_src{ get_src_para_as_needed( entry ) };
        return( para_src ? calculate_value_date( para_src ) : Date::NOT_SET );
    }
    else if( m_source_elem < VT::SRC_ITSELF )
        entry = get_src_entry_as_needed( entry );

    if( !entry || m_type != TCT_DATE ) return Date::NOT_SET;

    switch( m_opt_int & VT::BODY_FILTER )
    {
        case VT::DATE_CREATION: return entry->get_date_created(); break;
        case VT::DATE_CHANGE:   return entry->get_date_edited(); break;
        case VT::DATE_FINISH:   return entry->get_date_finish(); break;
        default:                return entry->get_date(); break;
    }
}

void
TableColumn::calculate_value_gantt( const Entry* entry, TableLine* line ) const
{
    for( Paragraph* p = entry->get_paragraph_1st(); p; p = p->m_p2next )
    {
        // only add when the last segment is closed:
        if( p->has_date() && ( line->m_periods.empty() || line->m_periods.back().w > 0.0 ) )
            line->m_periods.push_back( { 0.0, 0.0, p->get_date(), Date::NOT_SET } );
        if( p->has_date_finish() && !line->m_periods.empty() )
            line->m_periods.back().dend = p->get_date_finish();
    }

    if( line->m_periods.empty() )
        line->m_periods.push_back( { 0.0, 0.0, entry->get_date(), Date::NOT_SET } );
    if( line->m_periods.back().w == 0.0 ) // if last period was not closed
        line->m_periods.back().dend = entry->get_date_finish_calc();

    line->set_value_num( m_index, Date::get_secs_since_min( line->m_periods.front().dbgn ) );
}

void
TableColumn::calculate_value_num( const Paragraph* para, TableLine* line ) const
{
    Value value  { 0.0 };
    Value weight { 1.0 };

    if     ( m_source_elem < VT::SRC_ITSELF )
    {
        calculate_value_num( para->m_host, line );
        return;
    }
    // do not redirect when redirected from entry:
    else if( m_source_elem > VT::SRC_ITSELF && m_p2data->m_F_para_based )
        para = get_src_para_as_needed( para );

    if( para )
    {
        switch( m_type )
        {
            case TCT_NUMBER:
                value = ( para->m_order_in_host + 1 );
                break;
            case TCT_DURATION:
                value = calculate_duration( para, line );
                break;
            case TCT_SIZE:
                value = para->get_size_adv( char( m_opt_int ) );
                break;
            case TCT_BOOL:
                value = ( m_FC_generic ? ( m_FC_generic->filter( para ) ? 1.0 : 0.0 ) : 0.0 );
                break;
            case TCT_COMPLETION:
                value = para->get_completed();
                weight = calculate_weight( para );
                break;
            case TCT_TODO_STATUS:
                value = para->get_todo_status();
                break;
            case TCT_PATH_LENGTH:
                break; // not applicable for now, in the future we may associate paths with paras
            case TCT_ARITHMETIC:
                value = calculate_arithmetic( line );
                break;
            case TCT_TAG_V:
                if( m_p2tag )
                {
                    int   c { 0 }; // dummy;
                    Value v;

                    if     ( m_opt_int & VT::TVTS::REMAINING::I )
                        v = para->get_tag_value_remaining( m_p2tag, c );
                    else if( m_opt_int & VT::TVTS::PLANNED::I )
                        v = para->get_tag_value_planned( m_p2tag, c );
                    else if( m_opt_int == VT::TVT::COMPL_PERCENTAGE::I )
                    {
                        v = para->get_tag_value( m_p2tag, c );
                        weight = para->get_tag_value_planned( m_p2tag, c );
                    }
                    else
                        v = para->get_tag_value( m_p2tag, c );

                    if( is_counting() )
                    {
                        if( m_p2filter_count && m_FC_count->filter( v ) )
                            value = 1;
                    }
                    else
                        value = v;
                }
                break;
            case TCT_SUBTAG:
                if( m_p2tag && is_counting() )
                {
                    DiaryElemTag* tag_sub{ nullptr };
                    switch( m_opt_int )
                    {
                        case VT::FIRST:   tag_sub = para->get_subtag_first( m_p2tag ); break;
                        case VT::LAST:    tag_sub = para->get_subtag_last( m_p2tag ); break;
                        case VT::LOWEST:  tag_sub = para->get_subtag_lowest( m_p2tag ); break;
                        case VT::HIGHEST: tag_sub = para->get_subtag_highest( m_p2tag ); break;
                        case VT::TCOS_ALL:
                        {
                            auto&& subtags{ para->get_sub_tags( m_p2tag ) };
                            if( m_p2filter_count )
                            {
                                int c{ 0 };
                                for( auto t : subtags ) { if( m_FC_count->filter( t ) ) c++; }
                                value = c;
                            }
                            else
                                value = subtags.size();
                        }
                        default:
                            break;
                    }
                    if( tag_sub )
                    {
                        if( !m_p2filter_count || m_FC_count->filter( tag_sub ) )
                            value = 1;
                    }
                }
                break;
            case TCT_REFERRERS:
                if( is_counting() )
                {
                    if( m_p2filter_count )
                    {
                        value = 0;
                        for( auto t : para->get_references() )
                        { if( m_FC_count->filter( t ) ) ++value; }
                    }
                    else
                        value = para->get_reference_count();
                            break;
                }
                break;
            case TCT_SCRIPT: // must never be handled here
            default:
                throw LoG::Error( "Illegal request!" );
        }
    }

    line->set_value_num( m_index, value, weight );
    format_number( line ); // calls set_col_v_txt based on numeric value set above
}

Value
TableColumn::calculate_weight( const Paragraph* para ) const
{
    if     ( m_source_elem < VT::SRC_ITSELF )
        return calculate_weight( para->m_host );
    // do not redirect when redirected from entry:
    else if( m_source_elem > VT::SRC_ITSELF && m_p2data->m_F_para_based )
        para = get_src_para_as_needed( para );

    return( ( para && m_type == TCT_COMPLETION ) ? para->get_workload() : 0.0 );
}

Ustring
TableColumn::calculate_value_txt( const Paragraph* para ) const
{
    if     ( m_source_elem < VT::SRC_ITSELF )
        return calculate_value_txt( para->m_host );
    // do not redirect when redirected from entry:
    else if( m_source_elem > VT::SRC_ITSELF && m_p2data->m_F_para_based )
        para = get_src_para_as_needed( para );
    if( !para ) return "";

    switch( m_type )
    {
        case TCT_TEXT:
            switch( m_opt_int & VT::SEQ_FILTER )
            {
                case VT::TCT_SRC_ANCESTRAL:
                    return( para->get_ancestry_path() + para->get_name() );
                case VT::TCT_SRC_URI:
                    return para->get_uri_broad();
                default:
                    return para->get_text_stripped( m_opt_int );
            }
        case TCT_THEME:
            return para->m_host->get_theme()->get_name();
        case TCT_SUBTAG:
            if( m_p2tag && !is_counting() )
            {
                DiaryElemTag* tag_sub { nullptr };
                switch( m_opt_int )
                {
                    case VT::FIRST:   tag_sub = para->get_subtag_first( m_p2tag ); break;
                    case VT::LAST:    tag_sub = para->get_subtag_last( m_p2tag ); break;
                    case VT::LOWEST:  tag_sub = para->get_subtag_lowest( m_p2tag ); break;
                    case VT::HIGHEST: tag_sub = para->get_subtag_highest( m_p2tag ); break;
                    case VT::TCOS_ALL:
                    {
                        auto&& subtags{ para->get_sub_tags( m_p2tag ) };
                        Ustring tag_names;
                        bool F_2nd_or_beyond{ false };
                        for( auto t : subtags )
                        {
                            if( F_2nd_or_beyond )
                                tag_names += "; ";
                            else
                                F_2nd_or_beyond = true;
                            tag_names += t->get_name();
                        }
                        return tag_names;
                    }
                    default:
                        break;
                }
                if( tag_sub )
                    return tag_sub->get_name();
            }
            // no break
        case TCT_REFERRERS:
            if( !is_counting() )
            {
                Ustring tag_names;
                bool    F_2nd_or_beyond { false };
                for( auto t : para->get_references() )
                {
                    if( F_2nd_or_beyond )
                        tag_names += "; ";
                    else
                        F_2nd_or_beyond = true;
                    tag_names += t->get_name();
                }
                return tag_names;
            }
            // no break
        default: // for other types this function should never be called in the first place
            return "internal error";
    }
}

DiaryElemTag*
TableColumn::calculate_value_tag( const Paragraph* para ) const
{
    if     ( m_source_elem < VT::SRC_ITSELF )
        return calculate_value_tag( para->m_host );
    else if( m_source_elem > VT::SRC_ITSELF && m_p2data->m_F_para_based )
        para = get_src_para_as_needed( para );

    if( para && m_type == TCT_SUBTAG && m_p2tag && !is_counting() )
    {
        switch( m_opt_int )
        {
            case VT::FIRST:   return para->get_subtag_first( m_p2tag ); break;
            case VT::LAST:    return para->get_subtag_last( m_p2tag ); break;
            case VT::LOWEST:  return para->get_subtag_lowest( m_p2tag ); break;
            case VT::HIGHEST: return para->get_subtag_highest( m_p2tag ); break;
            default:
                break;
        }
    }
    return nullptr;
}

DateV
TableColumn::calculate_value_date( const Paragraph* para ) const
{
    if     ( m_source_elem < VT::SRC_ITSELF )
        return calculate_value_date( para->m_host );
    // do not redirect when redirected from entry:
    else if( m_source_elem > VT::SRC_ITSELF && m_p2data->m_F_para_based )
        para = get_src_para_as_needed( para );

    if( para && m_type == TCT_DATE )
    {
        switch( m_opt_int & VT::BODY_FILTER )
        {
            case VT::DATE_CREATION: return para->get_date_created(); break;
            case VT::DATE_CHANGE:   return para->get_date_edited(); break;
            case VT::DATE_FINISH:   return para->get_date_finish_broad(); break;
            default:                return para->get_date_broad(); break;
        }
    }

    return Date::NOT_SET;
}

void
TableColumn::calculate_value_gantt( const Paragraph* para, TableLine* line ) const
{
    line->m_periods.push_back( { 0.0, 0.0,
                                 para->get_date_broad(),
                                 para->get_date_finish_broad() } );
    line->set_value_num( m_index, Date::get_secs_since_min( line->m_periods.front().dbgn ) );
}

Value
TableColumn::calculate_duration( const DiaryElemTag* elem, TableLine* line ) const
{
    DateV d_bgn{ Date::NOT_SET }, d_end{ Date::NOT_SET };

    switch( m_opt_int & VT::TCD_FILTER_BGN )
    {
        case VT::TCD_BGN_START:     d_bgn = elem->get_date(); break;
        case VT::TCD_BGN_FINISH:    d_bgn = elem->get_date_finish_calc(); break;
        case VT::TCD_BGN_CREATION:  d_bgn = elem->get_date_created(); break;
        case VT::TCD_BGN_CHANGE:    d_bgn = elem->get_date_edited(); break;
        case VT::TCD_BGN_NOW:       d_bgn = Date::get_now(); break;
        case VT::TCD_BGN_L1:
            d_bgn = ( m_index > 0 ? line->get_value_date( m_index - 1 ) : 0 ); break;
        case VT::TCD_BGN_L2:
            d_bgn = ( m_index > 1 ? line->get_value_date( m_index - 2 ) : 0 ); break;
    }

    switch( m_opt_int & VT::TCD_FILTER_END )
    {
        case VT::TCD_END_START:     d_end = elem->get_date(); break;
        case VT::TCD_END_FINISH:    d_end = elem->get_date_finish_calc(); break;
        case VT::TCD_END_CREATION:  d_end = elem->get_date_created(); break;
        case VT::TCD_END_CHANGE:    d_end = elem->get_date_edited(); break;
        case VT::TCD_END_NOW:       d_end = Date::get_now(); break;
        case VT::TCD_END_L1:
            d_end = ( m_index > 0 ? line->get_value_date( m_index - 1 ) : 0 ); break;
        case VT::TCD_END_L2:
            d_end = ( m_index > 1 ? line->get_value_date( m_index - 2 ) : 0 ); break;
    }

    if( !Date::is_set( d_bgn ) || !Date::is_set( d_end ) ) return 0;

    if( ( m_opt_int & VT::TCD_FILTER_TYPE ) == VT::TCD_TYPE_WORK_DAYS )
        return m_p2data->m_p2diary->calculate_work_days_between( d_bgn, d_end );
    else
        return Date::calculate_days_between( d_bgn, d_end );
}

Value
TableColumn::calculate_arithmetic( TableLine* line ) const
{
    Value vA { m_opt_double1 };
    Value vB { m_opt_double2 };
    if( m_opt_int1 > 0 )
    {
        auto col { m_p2data->get_column_by_id( m_opt_int1 ) };
        vA = ( col ? line->get_value_num( col->m_index ) : 0.0 );
        // the unary modifier function
        switch( m_opt_int & VT::TCAu::FILTER_A )
        {
            case VT::TCAu::MNS::I: vA = abs( vA ); break;
            case VT::TCAu::ABS::I: vA = -vA; break;
            case VT::TCAu::RCP::I: vA = ( 1.0 / vA ); break;
        }
    }
    if( m_opt_int2 > 0 )
    {
        auto col { m_p2data->get_column_by_id( m_opt_int2 ) };
        vB = ( col ? line->get_value_num( col->m_index ) : 0.0 );
        // the unary modifier function
        switch( ( m_opt_int & VT::TCAu::FILTER_B ) >> 4 )
        {
            case VT::TCAu::MNS::I: vB = abs( vB ); break;
            case VT::TCAu::ABS::I: vB = -vB; break;
            case VT::TCAu::RCP::I: vB = ( 1.0 / vB ); break;
        }
    }

    // TODO: 3.2: handle dates differently
    switch( m_opt_int & VT::TCAo::FILTER )
    {
        default:               return( vA + vB );
        case VT::TCAo::SUB::I: return( vA - vB );
        case VT::TCAo::MUL::I: return( vA * vB );
        case VT::TCAo::DIV::I: return( vA / vB );
        case VT::TCAo::POW::I: return( pow( vA, vB ) );
        case VT::TCAo::ROO::I: return( pow( vA, 1/vB ) );
        case VT::TCAo::MOD::I:
            if( vB == 0 ) return Constants::INFINITY_PLS;
            else          return( int( vA ) % int( vB ) );
    }
}

void
TableColumn::calculate_script( TableLine* line ) noexcept
{
#ifdef __ANDROID__
    line->set_value_txt( get_index(), "Android not supported" );
#else
    namespace py = pybind11;

    if( !m_opt_str.empty() )
    {
        TableColumn* tc { this }; // to get rid of constness
        auto obj = PyBindings::run_script_name_return< py::object,
                                                       TableColumn*&,
                                                       TableLine*& >( m_opt_str, "run", tc, line );

        if( py::isinstance< py::tuple >( obj ) )
        {
            py::tuple t { obj.cast< py::tuple >() };
            if( t.size() != 2 )
            {
                print_error( "Table modifier script should return a (float, str) tuple!" );
                return;
            }

            if( !py::isinstance< py::float_ >( t[ 0 ] ) )
            {
                print_error( "First element in table modifier script return tuple must be float!" );
                return;
            }

            if( !py::isinstance< py::str >( t[ 1 ] ) )
            {
                print_error( "Second element in table modifier script return tuple must be str!" );
                return;
            }

            line->set_value_num( get_index(), t[ 0 ].cast< float >() );
            line->set_value_txt( get_index(), t[ 1 ].cast< String >() );
        }
        else if( py::isinstance< py::str >( obj ) )
        {
            line->set_value_txt( get_index(), obj.cast< String >() );
        }
        else if( obj )
        {
            try
            {
                double value { obj.cast< double >() };
                line->set_value_num( get_index(), value );
                format_number( line );
            }
            catch( const py::cast_error& )
            {
                print_error( "Table modfifier script must return a number convertible to double!" );
            }
        }
    }
#endif
}

void
TableColumn::format_number( TableLine* line ) const
{
    String        formatted_str;
    const Value   num_value       { line->get_value_num( m_index ) };

    if     ( is_percentage() )
        formatted_str = STR::format_percentage( num_value / line->get_col_weight( m_index ) );
    else if( m_type == TCT_ARITHMETIC &&
             ( ( m_opt_int & VT::TCAf::FILTER ) == VT::TCAf::INT::I ) ) // format as integer
        formatted_str = STR::format_number( std::round( num_value ), 0 );
    else if( m_type == TCT_ARITHMETIC &&
             ( ( m_opt_int & VT::TCAf::FILTER ) == VT::TCAf::RE2::I ) ) // format as real .##
        formatted_str = STR::format_number( num_value, 2 );
    else
        formatted_str = STR::format_number( num_value );

    line->set_value_txt( m_index, formatted_str );
}

// TABLE LINE ======================================================================================
TableLine::TableLine( TableData* p2data, int depth, bool F_expanded )
:   m_p2data( p2data ), m_depth( depth ), m_sublines( p2data->m_columns_sort ),
    m_F_expanded( F_expanded ) { }

double
TableLine::get_col_v_num_by_cid( int cid ) const
{
    auto col{ m_p2data->m_columns_map[ cid ] };

    if( !col ) return 0.0;

    return get_value_num( col->get_index() );
}

Ustring
TableLine::get_col_v_txt_by_cid( int cid ) const
{
    auto col{ m_p2data->m_columns_map[ cid ] };

    if( !col ) return "";

    return get_value_txt( col->get_index() );
}

// this is costlier than other get_value_* functions as dates are not cached for now
DateV
TableLine::get_value_date( unsigned int ic ) const
{
    if( ic < 0 || ic >= m_p2data->m_columns.size() ) return Date::NOT_SET;

    auto&& col{ m_p2data->m_columns[ ic ] };

    if( col->get_type() == TableColumn::TCT_DATE )
    {
        if( m_p2elem->is_entry() )
            return col->calculate_value_date( dynamic_cast< Entry* >( m_p2elem ) );
        else
            return col->calculate_value_date( dynamic_cast< Paragraph* >( m_p2elem ) );
    }
    else
        return Date::NOT_SET;
}
DateV
TableLine::get_col_v_date_by_cid( int cid ) const
{
    auto col{ m_p2data->m_columns_map[ cid ] };

    if( !col ) return Date::NOT_SET;

    return get_value_date( col->get_index() );
}

Value
TableLine::get_value_num( int i ) const
{
    if( i >= int( m_values_num.size() ) )
        return 0.0;
    else
        return m_values_num[ i ];
}

Value
TableLine::get_col_weight( int i ) const
{
    if( i >= int( m_weights.size() ) )
        return 0.0;
    else
        return m_weights[ i ];
}

Ustring
TableLine::get_value_txt( int i ) const
{
    if( i >= int( m_values_txt.size() ) )
        return "";
    else
        return m_values_txt[ i ];
}

void
TableLine::set_value_num( const int i, Value v, Value w )
{
    for( int i2 = m_values_num.size(); i2 <= i; ++i2 )
        m_values_num.push_back( 0.0 );
    for( int i2 = m_weights.size(); i2 <= i; ++i2 )
        m_weights.push_back( 0.0 );

    m_values_num[ i ] = v;
    m_weights[ i ] = w;
}

void
TableLine::set_value_txt( int i, Ustring&& v )
{
    for( int i2 = m_values_txt.size(); i2 <= i; ++i2 )
        m_values_txt.push_back( "" );

    m_values_txt[ i ] = v;
}

void
TableLine::add_col_v_num( const int i, Value v, Value w )
{
    for( int i2 = m_values_num.size(); i2 <= i; ++i2 )
        m_values_num.push_back( 0.0 );
    for( int i2 = m_weights.size(); i2 <= i; ++i2 )
        m_weights.push_back( 0.0 );

    m_values_num[ i ] += v;
    m_weights[ i ] += w;
}

void
TableLine::add_col_v_txt( int i, const Ustring& v )
{
    for( int i2 = m_values_txt.size(); i2 <= i; ++i2 )
        m_values_txt.push_back( "" );

    if( m_values_txt[ i ] != v ) // do not repeatedly add same value
    {
        if( m_values_txt[ i ].empty() )
            m_values_txt[ i ] = v;
        else
            m_values_txt[ i ] += ( "; " + v );
    }
}

void
TableLine::add_col_v_gantt( int ic, TableLine* sl )
{
    for( const Period& period_new : sl->m_periods )
    {
        if( m_periods.empty() )
        {
            m_periods.push_back( period_new );
        }
        else
        {
            DateV        new_bgn { period_new.dbgn };
            const DateV  new_end { period_new.dend };
            bool         F_added { false };

            for( auto it_exist = m_periods.begin(); it_exist != m_periods.end(); )
            {
                const DateV existing_bgn { it_exist->dbgn };
                const DateV existing_end { it_exist->dend };

                // order is very significant in the below cases:
                // 1) not there yet:
                if( existing_end < new_bgn )
                {
                    ++it_exist;
                }
                // 2) the new segment comes before the first segment:
                else if( existing_bgn > new_end )
                {
                    m_periods.insert( it_exist, period_new );
                    F_added = true;
                    break;
                }
                // 3) the new segment is contained within the existing:
                else if( existing_bgn <= new_bgn && existing_end >= new_end )
                {
                    F_added = true; // actually ignored
                    break;
                }
                // 4) the existing segment is contained within the new:
                else if( existing_bgn >= new_bgn && existing_end <= new_end )
                {
                    it_exist = m_periods.erase( it_exist );
                }
                // 5) new segment shifts the start of existing back:
                else if( existing_bgn > new_bgn && existing_end > new_end )
                {
                    it_exist->dbgn = new_bgn;
                    F_added = true; // actually merged
                    break;
                }
                // 6) new segment shifts the end of the existing forward:
                else //if( existing_bgn < new_bgn && existing_end < new_end )
                {
                    new_bgn = existing_bgn;
                    it_exist = m_periods.erase( it_exist );
                }
            }

            if( !F_added )  // it comes after all existing segments
                m_periods.push_back( { 0.0, 0.0, new_bgn, new_end } );
        }
    }

    set_value_num( ic, m_periods.empty() ? 0 : Date::get_secs_since_min( m_periods.front().dbgn ) );
}

bool
TableLine::is_group_head() const
{
    return( m_depth <= m_p2data->m_grouping_depth );
}

void
TableLine::toggle_all_expanded()
{
    int count_expanded_lines    { 0 };
    int count_collapsed_lines   { 0 };

    for( auto sl : m_sublines )
    {
        if( sl->is_expanded() ) count_expanded_lines++;
        else                    count_collapsed_lines++;
    }

    bool F_expand{ count_expanded_lines == 0 ? true :
                   ( count_collapsed_lines == 0 ? false :
                     ( count_expanded_lines < count_collapsed_lines ) ) };

    for( auto sl : m_sublines )
        sl->m_F_expanded = F_expand;
}

bool
TableLine::has_str( const Ustring& str ) const
{
    for( unsigned int i = 0; i < m_values_txt.size(); ++i )
        if( STR::lowercase( m_values_txt[ i ] ).find( str ) != Ustring::npos )
            return true;

    return false;
}

TableLine*
TableLine::get_subline_by_id( LoGID id ) const
{
    for( auto& sl : m_sublines )
    {
        if( sl->is_group_head() )
        {
            auto ssl{ sl->get_subline_by_id( id ) };
            if( ssl ) return ssl;
        }
        else
        {
            LoGID id_sl{ sl->m_p2elem &&
                         sl->m_p2elem->get_type() == DiaryElement::ET_PARAGRAPH ?
                         dynamic_cast< Paragraph* >( sl->m_p2elem )->m_host->get_id() :
                         sl->m_p2elem->get_id() };
            if( id == id_sl ) return sl;
        }
    }

    return nullptr;
}

TableLine*
TableLine::find_subline_per_grouping( TableLine* line )
{
    auto        col_group { m_p2data->get_group_column( m_depth ) };
    const auto  ic        { col_group->get_index() };
    const auto  str       { line->get_value_txt( ic ) };
    if( col_group )
    {
        // if( m_subgroups_unsorted.empty() ) // this is used in single entry mode
            for( auto sl : m_sublines )
            {
                if( sl->get_value_txt( ic ) == str )
                    return sl;
            }
        // else
            for( auto sl : m_subgroups_unsorted )
            {
                if( sl->get_value_txt( ic ) == str )
                    return sl;
            }
    }

    return nullptr;
}

TableLine*
TableLine::create_group_line( TableLine* sl )
{
    TableLine* gl { new TableLine( m_p2data, m_depth + 1, sl->is_expanded() ) };
    gl->m_p2parent = this;

    // set text for grouping columns:
    for( int i = 0; i <= m_depth; ++i )
    {
        const auto ic { m_p2data->get_group_column( i )->get_index() };
        gl->set_value_txt( ic, sl->get_value_txt( ic ) );
    }

    m_subgroups_unsorted.push_back( gl );

    gl->insert_subline( sl );

    return gl;
}

void
TableLine::incorporate_subline_values( TableLine* sl )
{
    for( auto& col : m_p2data->m_columns )
    {
        const auto ic    { col->get_index() };
        const auto col_v { ic < int( sl->m_values_num.size() ) ? sl->m_values_num[ ic ] : 0 };
        // when the collumn is not numeric ic can be less than m_values_num.size

        if     ( col->is_numeric() )
            add_col_v_num( ic, col_v, sl->m_weights[ ic ] );
        // only for sorting:
        else if( col->get_type() == TableColumn::TCT_SUBTAG && col->get_opt_int() != VT::TCOS_ALL )
            set_value_num( ic, col_v );
        else if( col->get_type() == TableColumn::TCT_REFERRERS && col->is_counting() )
            add_col_v_num( ic, col_v );
        else if( col->get_type() == TableColumn::TCT_DATE )
        {
            if( int( m_values_num.size() ) <= ic )
                add_col_v_num( ic, col_v );
            else if( m_values_num[ ic ] > col_v ) // set to start date
                m_values_num[ ic ] = col_v;
        }
        else if( col->get_type() == TableColumn::TCT_TODO_STATUS )
        {
            if( int( m_values_num.size() ) <= ic )
                add_col_v_num( ic, col_v );
            else
            {
                const int cs{ int( col_v ) | int( m_values_num[ ic ] ) };
                // PROGRESSED is like 0 in multiplication:
                if     ( cs & ES::PROGRESSED )
                    m_values_num[ ic ] = ES::PROGRESSED;
                // CANCELED can only retain when not combined with anything else:
                else if( col_v == ES::CANCELED && ( cs & ~ES::CANCELED ) == 0 )
                    m_values_num[ ic ] = ES::CANCELED;
                else if( cs == ( ES::TODO|ES::DONE ) )
                    m_values_num[ ic ] = ES::PROGRESSED;
            }
        }
        else if( col->get_type() == TableColumn::TCT_BOOL )
        {
            if( int( m_values_num.size() ) <= ic )
                add_col_v_num( ic, col_v );
            else
            if( m_values_num[ ic ] != col_v && m_values_num[ ic ] != 0.5 )
                m_values_num[ ic ] = 0.5;
        }
        else if( col->get_type() == TableColumn::TCT_GANTT )
            add_col_v_gantt( ic, sl );
        else
            continue;

        col->m_values.insert( col_v );
    }
}

void
TableLine::subtract_line_from_total( TableLine* line )
{
    for( auto& col : m_p2data->m_columns )
    {
        if( !col->is_enumerable() ) continue;

        if( col->is_numeric() )
        {
            m_values_num[ col->m_index ] -= line->m_values_num[ col->m_index ];
            m_weights[ col->m_index ] -= line->m_weights[ col->m_index ];
        }
        else
            continue;

        // TODO: 3.2: the ones below require recalculation:
        // case TableColumn::TCT_TODO_STATUS:
        // case TableColumn::TCT_GANTT:
        // case TableColumn::TCT_BOOL:
        // case TableColumn::TCT_SUB:

        col->format_number( this ); // use set_total_value_strings?
    }

    if( m_p2parent )
        m_p2parent->subtract_line_from_total( line );
}

void
TableLine::set_total_value_strings()
{
    if( m_sublines.empty() ) return;

    for( auto line : m_sublines )
        line->set_total_value_strings();

    for( auto& col : m_p2data->m_columns )
    {
        const int ic{ col->get_index() };

        switch( col->m_summary_func )
        {
            case VT::SUMF::SUM::C:
                col->format_number( this );
                break;
            case VT::SUMF::AVG::C:
                set_value_txt( ic, STR::format_number( get_value_num( ic ) /
                                                       get_col_weight( ic ) ) );
                break;
            case VT::SUMF::MAX::C:
                set_value_txt( ic, STR::format_number( col->get_val_max() ) );
                break;
            case VT::SUMF::MIN::C:
                set_value_txt( ic, STR::format_number( col->get_val_min() ) );
                break;
            case VT::SUMF::DTC::C: // distinct value count
                set_value_txt( ic, STR::format_number( col->m_values.size() ) );
                break;
            case VT::SUMF::CIN::C: // compute independently
                if( col->get_type() == TableColumn::TCT_ARITHMETIC )
                {
                    set_value_num( ic, col->calculate_arithmetic( this ) );
                    col->format_number( this );
                }
                break;
            default:
                break;
        }
    }
}

void
TableLine::insert_subline( TableLine* line )
{
    incorporate_subline_values( line );

    // if out of folding depth, add as a child:
    if( m_depth >= std::min( m_p2data->m_grouping_depth, int( m_p2data->m_columns_sort.size() ) ) )
    {
        m_sublines.insert( line );
        line->m_p2parent = this;
        line->m_depth = ( m_depth + 1 );
        for( TableLine* tl = this; tl; tl = tl->m_p2parent )
            tl->m_group_size++;
    }
    else // add as a grandchild
    {
        auto existing_child{ find_subline_per_grouping( line ) };
        if( existing_child ) // only add the grandchild
            existing_child->insert_subline( line );
        else // create the child and add the grandchild
            create_group_line( line );
    }
}

bool
TableLine::remove_entry( Entry* entry )
{
    bool F_removed{ false };

    for( auto it = m_sublines.begin(); it != m_sublines.end(); )
    {
        TableLine* sl { *it };
        if( sl->m_p2elem )
        {
            if( ( m_p2data->m_F_para_based &&
                  dynamic_cast< Paragraph* >( sl->m_p2elem )->m_host == entry ) ||
                sl->m_p2elem->get_id() == entry->get_id() )
            {
                subtract_line_from_total( sl );
                it = m_sublines.erase( it );
                m_p2data->m_count_lines--;
                decrease_group_size_up_chain();
                delete sl;
                F_removed = true;
            }
            else
                ++it;
        }
        else
        if( sl->remove_entry( entry ) )
        {
            if( sl->m_sublines.empty() ) // remove the empty header
            {
                it = m_sublines.erase( it );
                decrease_group_size_up_chain();
                delete sl;
            }
            F_removed = true;
        }
        else
            ++it; // not in the for statement for the erase case
    }

    return F_removed;
}

// TABLE DATA ======================================================================================
void
TableData::clear()
{
    m_filter_entry      = nullptr;
    m_filter_para       = nullptr;
    m_F_para_based      = false;

    m_grouping_depth    = 0;
    m_p2col_gantt       = nullptr;

    m_master_header.clear_sublines( true );
    m_lines.clear();

    for( auto& col : m_columns ) delete col;
    m_columns.clear();
    m_columns_map.clear();
    m_columns_sort.clear();
}

String
TableData::get_as_string() const
{
    String table_def;

    for( auto& col : m_columns )
    {
        const auto vt{ col->get_opt_int() };
        const auto ct{ col->get_type() };
        table_def += STR::compose( "Mci", col->m_id, col->get_name(), '\n' );

        table_def += "Mco"; // options

        switch( ct )
        {
            case TableColumn::TCT_ARITHMETIC:
                table_def +=
                        STR::compose(
                                "A",
                                VT::get_v< VT::TCAo, char, int >( vt & VT::TCAo::FILTER ),
                                VT::get_v< VT::TCAu, char, int >( vt & VT::TCAu::FILTER_A ),
                                VT::get_v< VT::TCAu, char, int >( ( vt & VT::TCAu::FILTER_B ) >> 4 ),
                                VT::get_v< VT::TCAf, char, int >( vt & VT::TCAf::FILTER ),
                                "~~" );
                break;
            case TableColumn::TCT_BOOL:         table_def += "B~~~~~~"; break;
            case TableColumn::TCT_SCRIPT:       table_def += "C~~~~~~"; break;
            case TableColumn::TCT_DATE:
                switch( vt & VT::BODY_FILTER )
                {
                    case VT::DATE_CREATION:     table_def += "DC~~~~~"; break;
                    case VT::DATE_CHANGE:       table_def += "DG~~~~~"; break;
                    case VT::DATE_FINISH:       table_def += "DF~~~~~"; break;
                    default:                    table_def += "DS~~~~~"; break; // DATE_START
                }
                break;
            case TableColumn::TCT_REFERRERS:    table_def += "F~~~~~~"; break;
            case TableColumn::TCT_GANTT:        table_def += "G~~~~~~"; break;
            case TableColumn::TCT_THEME:        table_def += "H~~~~~~"; break;
            case TableColumn::TCT_PATH_LENGTH:  table_def += "L~~~~~~"; break;
            case TableColumn::TCT_NUMBER:       table_def += "M~~~~~~"; break;
            case TableColumn::TCT_ORDER:        table_def += "O~~~~~~"; break;
            case TableColumn::TCT_COMPLETION:   table_def += "P~~~~~~"; break;
            case TableColumn::TCT_DURATION:
                table_def += STR::compose( "R",
                                           VT::TCD_CHARS[ vt & VT::TCD_FILTER_BGN ],
                                           VT::TCD_CHARS[ ( vt & VT::TCD_FILTER_END ) >> 4 ],
                                           VT::TCD_CHARS[ ( vt & VT::TCD_FILTER_TYPE ) >> 8 ],
                                           "~~~");  // reserved (3)
                break;
            case TableColumn::TCT_SUBTAG:
                switch( vt & VT::BODY_FILTER )
                {
                    case VT::LAST:              table_def += "SL~~~~~"; break;
                    case VT::LOWEST:            table_def += "SO~~~~~"; break;
                    case VT::HIGHEST:           table_def += "SH~~~~~"; break;
                    case VT::TCOS_ALL:          table_def += "SA~~~~~"; break;
                    default:                    table_def += "SF~~~~~"; break;  // FIRST
                }
                break;
            case TableColumn::TCT_TAG_V:
                table_def += STR::compose( "T",
                                           VT::get_v< VT::TVT, char, int >( vt & VT::BODY_FILTER ),
                                           "~~~~~" );
                break;
            case TableColumn::TCT_TODO_STATUS:  table_def += "U~~~~~~"; break;
            case TableColumn::TCT_TEXT:         table_def += "X~~~~~~"; break;
            case TableColumn::TCT_SIZE:
                table_def += STR::compose( "Z", char( vt ), "~~~~~" ); // reserved (5)
                break;
        }

        table_def += ( col->is_combine_same() ? 'G' : '_' );

        table_def += ( col->is_delta() ? 'D' : ( col->is_counting() ? 'C' : '_' ) );

        table_def += VT::SRC_CHARS[ col->get_source_elem() ];

        table_def += HELPERS::get_underlying( col->get_coloring_scheme() );

        table_def += col->m_summary_func;

        table_def += "~~~~\n"; // end of options

        if     ( ct == TableColumn::TCT_TEXT )
        {
            table_def += "Mcx";
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_TAG        ) ? 'T' : '_';
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_DATE       ) ? 'D' : '_';
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_COMMENT    ) ? 'C' : '_';
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_INDENT     ) ? 'I' : '_';
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_PLAIN      ) ? 'P' : '_';
            table_def += ( vt & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_NUMBER     ) ? 'N' : '_';

            table_def += "~~~~~~~~"; // reserved (8)

            switch( vt & VT::SEQ_FILTER )
            {
                case VT::TCT_SRC_ANCESTRAL:     table_def += 'A'; break;
                case VT::TCT_SRC_DESCRIPTION:   table_def += 'D'; break;
                case VT::TCT_SRC_URI:           table_def += 'U'; break;
                default:                        table_def += 'T'; break; //VT::TCT_SRC_TITLE
            }

            table_def += '\n'; // end of text properties
        }
        else if( ct == TableColumn::TCT_BOOL )
        {
            table_def += STR::compose( "Mcbf", col->m_opt_int, '\n' ); // filter
        }
        else if( ct == TableColumn::TCT_DATE )
        {
            if( vt & VT::DATE_CUSTOM_FORMAT )
                table_def += STR::compose( "Mcdf", col->m_opt_str, '\n' );
            else
                table_def += "Mcd_\n";
        }
        else if( ct == TableColumn::TCT_ARITHMETIC )
        {
            if( col->m_opt_int1 == 0 ) // constant value case
                table_def += STR::compose( "McaA", col->m_opt_double1, '\n' );
            else // column id case
                table_def += STR::compose( "Mcaa", col->m_opt_int1, '\n' );
            if( col->m_opt_int2 == 0 ) // constant value case
                table_def += STR::compose( "McaB", col->m_opt_double2, '\n' );
            else // column id case
                table_def += STR::compose( "Mcab", col->m_opt_int2, '\n' );
        }
        else if( ct == TableColumn::TCT_SCRIPT )
            table_def += STR::compose( "Mcrn", col->m_opt_str, '\n' );

        if( ( col->get_source_elem() == VT::SRC_PARENT_FILTER ||
              col->get_source_elem() >= VT::SRC_FCHILD_FILTER ) && col->get_source_elem_filter() )
            table_def += STR::compose( "Mcgf", col->get_source_elem_filter()->get_id().get_raw(),
                                       '\n' );

        table_def += STR::compose( "Mcw", col->m_width, '\n' );

        if( col->m_sort_order > 0 )
            table_def += STR::compose( "Mcs", col->m_sort_desc ? 'D' : 'A',
                                              col->m_sort_order, '\n' );

        if( col->get_tag() )
            table_def += STR::compose( "Mct", col->get_tag()->get_id().get_raw(), '\n' );

        if( col->get_count_filter() != nullptr )
            table_def += STR::compose( "Mccf", col->get_count_filter()->get_id().get_raw(), '\n' );
    }

    if( m_filter_entry != nullptr )
        table_def += STR::compose( "Mf", m_filter_entry->get_id().get_raw(), '\n' );

    if( m_filter_para != nullptr )
        table_def += STR::compose( "Ml", m_filter_para->get_id().get_raw(), '\n' );

    table_def += STR::compose( "Mo", m_grouping_depth,
                                     m_F_para_based ? 'P' : 'E' );
    // this comes last to guarantee lack of \n at the end

    return table_def;
}

void
TableData::set_from_string( const Ustring& def )
{
    String          line;
    StringSize      line_offset  { 0 };
    TableColumn*    col          { nullptr };

    clear();

    while( STR::get_line( def, line_offset, line ) )
    {
        if( line.size() < 2 )   // should never occur
            continue;

        switch( line[ 1 ] )
        {
            case 'c':   // column
            {
                switch( line[ 2 ] )
                {
                    case 'i':   // id + name
                        col = add_column( -1, std::stoi( line.substr( 3, 3 ) ) );
                        col->set_name( line.substr( 6 ) );
                        break;
                    case 'n':   // name (incremental id version --only use for ephemeral tables)
                        col = add_column( -1, COL_ID_MIN + m_columns.size() );
                        col->set_name( line.substr( 3 ) );
                        break;
                    case 'o':   // options
                        switch( line[ 3 ] )
                        {
                            case 'A': col->set_type( TableColumn::TCT_ARITHMETIC );
                                // opreration:
                                col->m_opt_int =  VT::get_v< VT::TCAo, int, char >( line[ 4 ] );
                                col->m_opt_int |= VT::get_v< VT::TCAu, int, char >( line[ 5 ] );
                                col->m_opt_int |= ( VT::get_v< VT::TCAu, int, char >( line[ 6 ] ) << 4 );
                                col->m_opt_int |= VT::get_v< VT::TCAf, int, char >( line[ 7 ] );
                                break;
                            case 'B': col->set_type( TableColumn::TCT_BOOL ); break;
                            case 'C': col->set_type( TableColumn::TCT_SCRIPT ); break;
                            case 'D': col->set_type( TableColumn::TCT_DATE );
                                switch( line[ 4 ] )
                                {
                                    case 'C': col->set_opt_int( VT::DATE_CREATION ); break;
                                    case 'G': col->set_opt_int( VT::DATE_CHANGE ); break;
                                    case 'M':   // deprecated
                                    case 'F': col->set_opt_int( VT::DATE_FINISH ); break;
                                    default:  col->set_opt_int( VT::DATE_START ); break;
                                }
                                break;
                            case 'F': col->set_type( TableColumn::TCT_REFERRERS ); break;
                            case 'G': col->set_type( TableColumn::TCT_GANTT ); break;
                            case 'H': col->set_type( TableColumn::TCT_THEME ); break;
                            case 'L': col->set_type( TableColumn::TCT_PATH_LENGTH ); break;
                            case 'M': col->set_type( TableColumn::TCT_NUMBER ); break;
                            case 'N': // Name type dropped, use text instead:
                                col->set_type( TableColumn::TCT_TEXT );
                                col->set_opt_int( VT::TCT_SRC_TITLE|VT::TCT_FILTER_COMPONENT );
                                break;
                            case 'O': col->set_type( TableColumn::TCT_ORDER ); break;
                            case 'P': col->set_type( TableColumn::TCT_COMPLETION ); break;
                            case 'R': col->set_type( TableColumn::TCT_DURATION );
                                col->set_opt_int(
                                    ( STR::get_pos_c( VT::TCD_CHARS, line[ 4 ], 0 ) ) |
                                    ( STR::get_pos_c( VT::TCD_CHARS, line[ 5 ], 0 ) << 4 ) |
                                    ( STR::get_pos_c( VT::TCD_CHARS, line[ 6 ], 0 ) << 8 ) );
                                break;
                            case 'S': col->set_type( TableColumn::TCT_SUBTAG );
                                switch( line[ 4 ] )
                                {
                                    case 'L': col->set_opt_int( VT::LAST ); break;
                                    case 'O': col->set_opt_int( VT::LOWEST ); break;
                                    case 'H': col->set_opt_int( VT::HIGHEST ); break;
                                    case 'A': col->set_opt_int( VT::TCOS_ALL ); break;
                                    default:  col->set_opt_int( VT::FIRST ); break;
                                }
                                break;
                            case 'T': col->set_type( TableColumn::TCT_TAG_V );
                                col->set_opt_int( VT::get_v< VT::TVT, int, char >( line[ 4 ] ) );
                                break;
                            case 'U': col->set_type( TableColumn::TCT_TODO_STATUS ); break;
                            case 'X': col->set_type( TableColumn::TCT_TEXT ); break;
                            case 'Z': col->set_type( TableColumn::TCT_SIZE );
                                col->set_opt_int( line[ 4 ] );
                                break;
                        }

                        if( line.size() > 13 ) // check for TCT_ORDER case
                        {
                            col->set_combine_same( line[ 10 ] == 'G' );
                            col->m_show_as = line[ 11 ];
                            col->set_source_elem( STR::get_pos_c( VT::SRC_CHARS,
                                                                      line[ 12 ],
                                                                      0 ) );
                            col->set_coloring_scheme(
                                    HELPERS::set_from_underlying< TableColumn::Coloring >(
                                            line[ 13 ] ) );
                        }
                        if( line.size() > 14 )
                            col->m_summary_func = line[ 14 ];
                        break;
                    case 'a':   // arithmetic options
                        switch( line[ 3 ] )
                        {
                            // operand A and B constant values:
                            case 'A': col->m_opt_double1 = STR::get_d( line.substr( 4 ) ); break;
                            case 'B': col->m_opt_double2 = STR::get_d( line.substr( 4 ) ); break;
                            // operand A and B column ids:
                            case 'a': col->m_opt_int1 = std::stoi( line.substr( 4 ) ); break;
                            case 'b': col->m_opt_int2 = std::stoi( line.substr( 4 ) ); break;
                        }
                        break;
                    case 'b':   // bool options
                        col->m_opt_int = std::stol( line.substr( 4 ) );
                        break;
                    case 'c':   // conditions
                        switch( line[ 3 ] )
                        {
                            case 'f':
                                col->set_count_filter( m_p2diary->get_filter(
                                        D::DEID( line.substr( 4 ) ) ) );
                                break;
                           // NOTE: numeric limits were removed in favor of filters...
                           // ... these have to be set manually after update:
                           // case 'l':
                           // case 'h':
                        }
                        break;
                    case 'x':   // text options
                    {
                        int text_flags{ 0 };
                        if( line[ 3 ] == 'T' ) text_flags |= VT::TCT_CMPNT_TAG;
                        if( line[ 4 ] == 'D' ) text_flags |= VT::TCT_CMPNT_DATE;
                        if( line[ 5 ] == 'C' ) text_flags |= VT::TCT_CMPNT_COMMENT;
                        if( line[ 6 ] == 'I' ) text_flags |= VT::TCT_CMPNT_INDENT;
                        if( line[ 7 ] == 'P' ) text_flags |= VT::TCT_CMPNT_PLAIN;
                        if( line[ 8 ] == 'N' ) text_flags |= VT::TCT_CMPNT_NUMBER;

                        if( line.size() > 17 )
                        {
                            switch( line[ 17 ] )
                            {
                                case 'A': text_flags |= VT::TCT_SRC_ANCESTRAL; break;
                                case 'D': text_flags |= VT::TCT_SRC_DESCRIPTION; break;
                                case 'U': text_flags |= VT::TCT_SRC_URI; break;
                                default:  text_flags |= VT::TCT_SRC_TITLE; break;
                            }
                        }

                        col->set_opt_int( text_flags );
                        break;
                    }
                    case 'd':   // date
                        if( line[ 3 ] == 'f' )
                        {
                            col->m_opt_int |= VT::DATE_CUSTOM_FORMAT;
                            col->m_opt_str = line.substr( 4 ); // format string
                        }
                        else
                            col->m_opt_str = Date::get_format_str_default();
                        break;
                    case 'g':   // get from source
                        col->set_source_elem_filter(
                                m_p2diary->get_filter( D::DEID( line.substr( 4 ) ) ) );
                        break;
                    case 'r':   // script options
                        col->m_opt_str = line.substr( 4 ); // script name
                        break;
                    case 's':   // sorting
                    {
                        col->m_sort_desc = ( line[ 3 ] == 'D' );
                        col->m_sort_order = std::stoi( line.substr( 4 ) );
                        bool F_added{ false };
                        for( auto it = m_columns_sort.begin(); it != m_columns_sort.end(); it++ )
                            if( col->m_sort_order < ( *it )->m_sort_order )
                            {
                                m_columns_sort.insert( it, col );
                                F_added = true;
                                break;
                            }
                        if( !F_added )
                            m_columns_sort.push_back( col );
                        break;
                    }
                    case 't':   // column tag
                        col->set_tag(
                                m_p2diary->get_tag_by_id( D::DEID( line.substr( 3 ) ) ) );
                        break;
                    case 'w':   // column width
                        col->m_width = STR::get_d( line.substr( 3 ) );
                        break;
                }
                break;
            }
            case 'f':   // filter
                m_filter_entry = m_p2diary->get_filter( D::DEID( line.substr( 2 ) ) );
                break;
            case 'l':   // filter
                m_filter_para = m_p2diary->get_filter( D::DEID( line.substr( 2 ) ) );
                break;
            // NOTE: tag filters were removed in favor of generic filters...
            // ... these have to be set manually after update:
            // case 't':
            case 'o':
                if( line[ 2 ] == 'G' || line[ 2 ] == '_' ) // to support pre v2016
                    m_grouping_depth = 0;
                else
                    m_grouping_depth = ( line[ 2 ] - '0' );
                m_F_para_based = ( line[ 3 ] == 'P' );
                break;
            default:
                PRINT_DEBUG( "Unrecognized table string: ", line );
                break;
        }
    }

    // set a sort column if none is set:
    ensure_sort_column();
}

int
TableData::generate_new_id() const
{
    int id_new;

    do
    {
#ifndef _WIN32
        std::random_device rd;  //seed
        std::mt19937 gen( rd() ); // generator
#else
        std::mt19937 gen( time( NULL ) ); // generator
#endif
        std::uniform_int_distribution<> distrib( COL_ID_MIN, COL_ID_MAX );

        id_new = distrib( gen );
    }
    while( m_columns_map.find( id_new ) != m_columns_map.end() );

    PRINT_DEBUG( "New Column ID = ", id_new );

    return id_new;
}

TableColumn*
TableData::add_column( int i_column, int id )
// id == -1 means user action i.e. not set_from_string()
// id == -2 means order column
{
    if( m_columns.size() == COL_COUNT_MAX ) return nullptr;

    TableColumn* col_new{ new TableColumn( this, id < 0 ? generate_new_id() : id ) };

    if( m_columns.empty() && id < 0 )
    {
        col_new->m_sort_order = 1;
        m_columns_sort.push_back( col_new );
    }

    if( id == -2 )
        col_new->m_width = 0.1;
    else
        // always do this to cope with old diaries regardless of user action:
        col_new->m_width = 1.0 / ( m_columns.size() + 1 );

    if( id < 0 ) // redistribute widths
    {
        for( auto& col : m_columns )
            col->m_width -= ( col_new->m_width * col->m_width );
    }

    col_new->m_index = ( i_column < 0 ? m_columns.size() : i_column );

    if( col_new->m_index >= int( m_columns.size() ) )
        m_columns.push_back( col_new );
    else
    {
        auto it_insert{ m_columns.begin() + col_new->m_index };
        it_insert = m_columns.insert( it_insert, col_new );
        it_insert++;
        for( ; it_insert != m_columns.end(); ++it_insert )
            ( *it_insert )->m_index++;
    }

    m_columns_map[ col_new->m_id ] = col_new;

    return col_new;
}

void
TableData::dismiss_column( int i_col )
{
    if( i_col < 0 || i_col >= int( m_columns.size() ) )
        throw LoG::Error( "Column index out of bounds!" );

    auto&& iter{ m_columns.begin() + i_col };
    TableColumn* col_del{ *iter };

    m_columns.erase( iter );
    m_columns_map.erase( col_del->m_id );
    m_columns_sort.clear();

    // redistribute widths, indices, & sort orders
    for( auto& col : m_columns )
    {
        col->m_width /= ( 1.0 - col_del->m_width );

        if( col->m_sort_order > 0 )
            m_columns_sort.push_back( col );

        if( col_del->m_sort_order > 0 && col->m_sort_order > col_del->m_sort_order )
            col->m_sort_order--;

        if( col->m_index > col_del->m_index )
            col->m_index--;
    }

    ensure_sort_column();
    if( col_del->m_type == TableColumn::TCT_GANTT )
        m_p2col_gantt = nullptr;

    delete col_del;
}

void
TableData::move_column( int i_cur, int i_tgt )
{
    if( i_cur == i_tgt )
        return;

    if( i_cur < 0 || i_cur >= int( m_columns.size() ) )
        throw LoG::Error( "Column index out of bounds!" );

    if( i_tgt < 0 || i_tgt >= int( m_columns.size() ) )
        throw LoG::Error( "Column target index out of bounds!" );

    auto&& iter{ m_columns.begin() };
    std::advance( iter, i_cur );
    auto column{ *iter };
    m_columns.erase( iter );

    iter = m_columns.begin();
    std::advance( iter, i_tgt );
    m_columns.insert( iter, column );

    // update indices
    int i{ 0 };
    for( auto& col : m_columns )
    {
        col->m_index = i;
        i++;
    }

    // swap content
    auto swap_col_values = [ & ]( TableLine* line )
    {
        auto        v_txt   { line->get_value_txt( i_cur ) };
        const auto  v_num   { line->get_value_num( i_cur ) };
        const auto  weight  { line->get_col_weight( i_cur ) };

        line->set_value_txt( i_cur, line->get_value_txt( i_tgt ) );
        line->set_value_num( i_cur, line->get_value_num( i_tgt ), line->get_col_weight( i_tgt ) );
        line->set_value_txt( i_tgt, std::move( v_txt ) );
        line->set_value_num( i_tgt, v_num, weight );
    };

    swap_col_values( &m_master_header );
    for( auto& line : m_lines )
        swap_col_values( line );
}

void
TableData::set_show_order_column( bool F_show )
{
    if( F_show == has_order_column() )
        return;
    else if( F_show )
    {
        TableColumn* col{ add_column( 0, -2 ) };
        col->set_type( TableColumn::TCT_ORDER );
        col->set_name( "#" );
    }
    else
        dismiss_column( 0 );
}

void
TableData::set_column_sort_or_change_dir( int i_col ) // called on double click
{
    if( m_columns[ i_col ]->m_sort_order != 1 )
    {
        for( auto& col : m_columns_sort )
            col->clear_sorting();
        m_columns_sort.clear();

        m_columns[ i_col ]->m_sort_order = 1;
        m_columns_sort.push_back( m_columns[ i_col ] );
    }
    else
        m_columns[ i_col ]->m_sort_desc = !m_columns[ i_col ]->m_sort_desc;
}
// called on Po RB click & shift + double click:
void
TableData::set_column_sort_dir( int i_col, bool F_descending )
{
    if( m_columns[ i_col ]->m_sort_order < 1 )
    {
        m_columns_sort.push_back( m_columns[ i_col ] );
        m_columns[ i_col ]->m_sort_order = ( m_columns_sort.size() );
        m_columns[ i_col ]->m_sort_desc = F_descending;
    }
    else
        m_columns[ i_col ]->m_sort_desc = F_descending;
}
void
TableData::ensure_sort_column()
{
    const unsigned int i_col = ( has_order_column() ? 1 : 0 );
    if( m_columns_sort.empty() && m_columns.size() > i_col )
    {
        m_columns[ i_col ]->m_sort_order = 1;
        m_columns_sort.push_back( m_columns[ i_col ] );
    }
}

void
TableData::unset_column_sort( int i_col )
{
    if( m_columns[ i_col ]->m_sort_order > 0 )
    {
        m_columns_sort.clear();
        for( auto& col : m_columns )
        {
            if( col->m_sort_order > 0 &&
                col->m_sort_order != m_columns[ i_col ]->m_sort_order ) // check before order--
                m_columns_sort.push_back( col );
            if( col->m_sort_order > m_columns[ i_col ]->m_sort_order )
                col->m_sort_order--;
        }

        m_columns[ i_col ]->clear_sorting();
    }
}

bool
TableData::populate_lines( bool F_expanded, Entry* entry_single ) // use all entries if nullptr
{
    if( m_p2diary == nullptr || m_columns.empty() )
        return false;

    if( !entry_single )
    {
        m_master_header.clear_sublines( true );
        m_count_lines = 0;
    }

    FiltererContainer*  fc_entry    { nullptr };
    FiltererContainer*  fc_para     { nullptr };
    int                 n_added     { 0 };

    for( auto& col : m_columns )
    {
        // initialize filterer stacks
        col->allocate_filter_stacks();

        if( !entry_single )
            col->m_values.clear();
    }

    if( m_filter_entry )
        fc_entry = m_filter_entry->get_filterer_stack();
    if( m_filter_para )
        fc_para = m_filter_para->get_filterer_stack();

    for( Entry* entry = ( entry_single ? entry_single : m_p2diary->get_entry_1st() );
         entry;
         entry = ( entry_single ? nullptr : entry->get_next_straight() ) )
    {
        if( fc_entry && !fc_entry->filter( entry ) )
            continue;

        if( m_F_para_based )
        {
            for( Paragraph* para = entry->get_paragraph_1st(); para; para = para->m_p2next )
            {
                if( fc_para && fc_para->filter( para ) == false )
                    continue;

                m_master_header.insert_subline( create_line( para, 1, F_expanded ) );
                m_count_lines++;
                n_added++;
            }
        }
        else
        {
            m_master_header.insert_subline( create_line( entry, 1, F_expanded ) );
            m_count_lines++;
            n_added++;
        }

        // limit maximum number of lines to something manageable
        if( m_count_lines >= 10000 ) break;
    }

    if( m_grouping_depth > 0 )
        sort_group_lines( &m_master_header );

    if( m_count_lines > 0 )
        m_master_header.set_total_value_strings();

    if( m_p2col_gantt ) // is set up in a second iteration as all lines depend to each other
        set_gantt_column_coords();

    update_lines_cache();

    // delete filterer stacks
    delete fc_entry;
    delete fc_para;

    for( auto& col : m_columns )
        col->deallocate_filter_stacks();

    return( n_added > 0 );
}

void
TableData::set_gantt_column_coords()
{
    DateV   date_bgn  { Date::NOT_SET };
    DateV   date_end  { Date::NOT_SET };
    double  span      { 0 };

    std::function< void( TableLine* ) >
    calculate_span = [ & ]( TableLine* line )
    {
        for( auto sl : line->m_sublines )
        {
            if( sl->m_p2elem ) // not header
            {
                if( !Date::is_set( date_end ) || date_bgn > sl->m_periods.front().dbgn )
                    date_bgn = sl->m_periods.front().dbgn;

                if( !Date::is_set( date_end ) || date_end < sl->m_periods.back().dend )
                    date_end = sl->m_periods.back().dend;
            }

            if( !sl->m_sublines.empty() )
                calculate_span( sl );
        }
    };

    std::function< void( TableLine* ) >
    fill_subline_period_coords = [ & ]( TableLine* line )
    {
        for( auto sl : line->m_sublines )
        {
            for( auto& seg : sl-> m_periods )
            {
                seg.x = ( Date::calculate_days_between_abs( date_bgn, seg.dbgn ) / span );
                seg.w = ( ( Date::calculate_days_between_abs( seg.dbgn, seg.dend ) + 1 ) / span );
            }

            if( !sl->m_sublines.empty() )
                fill_subline_period_coords( sl );
        }
    };

    calculate_span( &m_master_header );
    Date::forward_days( date_end, 1 );  // move one day to to show the width of the last day
    span = Date::calculate_days_between_abs( date_bgn, date_end );

    fill_subline_period_coords( &m_master_header );
    m_date_bgn = date_bgn;
    m_master_header.set_value_num( m_p2col_gantt->m_index, 0.0, span );
}

void
TableData::update_lines_cache()
{
    std::function< void( TableLine* ) > add_sublines =
    [ & ]( TableLine* line )
    {
        for( auto sl : line->m_sublines )
        {
            if( m_search_str.empty() || sl->has_str( m_search_str ) )
                m_lines.push_back( sl );
            if( !sl->m_sublines.empty() && sl->is_expanded() )
                add_sublines( sl );
        }
    };

    m_lines.clear();      // ordered cache

    add_sublines( &m_master_header );

    if( has_order_column() )
        update_order_column();

    update_delta_columns();
}

void
TableData::update_delta_columns()
{
    const auto col_count { m_columns.size() };

    for( ListTableColumns::size_type ic = 0; ic < col_count; ic++ )
    {
        const auto col { m_columns[ ic ] };

        if( !col->is_delta() ) continue;

        for( int il = m_lines.size() - 1 ; il > 0; --il )
        {
            const auto v_num { get_value( il, ic ) - get_value( il - 1, ic ) };
            m_lines[ il ]->set_value_num( ic, v_num );
            col->format_number( m_lines[ il ] );
        }

        // first line:
        if( !m_lines.empty() )
        {
            m_lines[ 0 ]->set_value_num( ic, 0 );
            col->format_number( m_lines[ 0 ] );
        }
    }
}

void
TableData::update_order_column()
{
    std::vector< int > last_order_per_depth;
    int last_depth{ 1 };

    for( auto sl : m_lines )
    {
        if     ( sl->m_depth > int( last_order_per_depth.size() ) )
            last_order_per_depth.push_back( 1 );
        else if( sl->m_depth > last_depth )
            last_order_per_depth[ sl->m_depth - 1 ] = 1;
        else
            last_order_per_depth[ sl->m_depth - 1 ]++;

        String number { std::to_string( last_order_per_depth[ 0 ] ) };
        for( int i = 1; i < sl->m_depth; i++ )
            number += STR::compose( '.', last_order_per_depth[ i ] );

        sl->set_value_txt( 0, number );

        last_depth = sl->m_depth;
    }

    m_master_header.set_value_txt( 0, std::to_string( m_master_header.m_group_size ) );
}

void
TableData::resort_lines()
{
    std::vector< TableLine* > lines_backup;

    std::function< void( TableLine* ) > backup_sublines = [ & ]( TableLine* line ){
        for( auto sl : line->m_sublines )
        {
            if( sl->m_sublines.empty() )
                lines_backup.push_back( sl );
            else
                backup_sublines( sl );
        }
    };

    backup_sublines( &m_master_header );

    m_master_header.clear_sublines( false );

    for( auto& line : lines_backup )
        m_master_header.insert_subline( line );

    if( m_grouping_depth > 0 )
        sort_group_lines( &m_master_header );

    m_master_header.set_total_value_strings();

    if( m_p2col_gantt ) // is set up in a second iteration as all lines depend to each other
        set_gantt_column_coords();

    update_lines_cache();
}

// we need to sort the group header lines after their values are finalized
void
TableData::sort_group_lines( TableLine* line )
{
    for( auto sl : line->m_subgroups_unsorted )
    {
        line->m_sublines.insert( sl );
        if( !sl->m_subgroups_unsorted.empty() )
            sort_group_lines( sl );
    }

    line->m_subgroups_unsorted.clear();
}

double
TableData::get_value( unsigned int il, unsigned int ic ) const
{
    if( il >= m_lines.size() || ic >= m_columns.size() )
        throw( LoG::Error( "table index out of bounds" ) );

    auto&& line { m_lines[ il ] };
    auto&& col  { m_columns[ ic ] };

    if( col->is_enumerable( true ) )
        return( line->get_value_num( ic ) );
    else
        return( 0.0 );
}

double
TableData::get_value_pure( unsigned int il, unsigned int ic ) const
{
    if( il >= m_lines.size() || ic >= m_columns.size() )
        throw( LoG::Error( "table index out of bounds" ) );

    if( ic >= m_lines[ il ]->m_values_num.size() )
        return 0.0;

    return( m_lines[ il ]->m_values_num[ ic ] );
}

double
TableData::get_weight( unsigned int il, unsigned int ic ) const
{
    if( il >= m_lines.size() || ic >= m_columns.size() )
        throw( LoG::Error( "table index out of bounds" ) );

    return( m_lines[ il ]->get_col_weight( ic ) );
}

Ustring
TableData::get_value_str( unsigned int il, unsigned int ic ) const
{
    if( il >= m_lines.size() || ic >= m_columns.size() )
        throw( LoG::Error( "table index out of bounds" ) );

    return m_lines[ il ]->get_value_txt( ic );
}

Ustring
TableData::get_total_value_str( unsigned int ic ) const
{
    if( ic >= m_columns.size() )
        throw( LoG::Error( "table index out of bounds" ) );

    return m_master_header.get_value_txt( ic );
}

bool
TableData::is_col_value_same( unsigned int il, unsigned int ic ) const
{
    if( il == 0 || m_lines[ il - 1 ]->is_group_head() ) return false;

    return( get_value_str( il, ic ) == get_value_str( il - 1, ic ) );
}

void
TableData::set_para_based( bool F_para_based )
{
    m_F_para_based = F_para_based;

    for( auto col : m_columns )
    {
        if( !F_para_based && col->m_type == TableColumn::TCT_TEXT &&
            ( col->m_opt_int & VT::SEQ_FILTER ) == VT::TCT_SRC_URI )
            col->set_opt_int( VT::TCT_DEFAULT ); // uri does not apply to entries
    }
}

// TABLEELEM =======================================================================================
const Ustring TableElem::DEFINITION_DEFAULT{ STR::compose(
        "\nMci", TableData::COL_ID_MIN, get_sstr( CSTR::NEW_COLUMN ),
        "\nMcoX__~~~~~_S"
        "\nMcx____P_~~~~~~~~T"
        "\nMcw1.0" ) };

const Ustring TableElem::DEFINITION_REPORT{ STR::compose(
        "\nMci", TableData::COL_ID_MIN, "#",
        "\nMcoO__~~~~__SO",
        "\nMcw0.06",
        "\nMci", TableData::COL_ID_MIN + 1, _( "Date" ),
        "\nMcoDS_~~~~__SO",
        "\nMcd_",
        "\nMcw0.1",
        "\nMci", TableData::COL_ID_MIN + 2, _( "Entry" ),
        "\nMcoX__~~~~__SO",
        "\nMcx____P_~~~~~~~~T",
        "\nMcw0.6",
        "\nMcsA1",
        "\nMci", TableData::COL_ID_MIN + 3, _( "Size" ),
        "\nMcoZ__~~~~__SO",
        "\nMcw0.24",
        "\nMo0E" ) };

const R2Pixbuf&
TableElem::get_icon() const
{
    return Lifeograph::icons->table;
}
