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


#include "parser_background.hpp"
#include "../diaryelements/diary.hpp"
#include "../helpers.hpp"
#include "../python/code_languages.hpp"


using namespace LoG;

void
ParserBackGround::reset( UstringSize bgn, UstringSize end )
{
    Paragraph* para_cur = m_parser_p2para_cur;
    ParserBase::reset( bgn, end );
    m_parser_p2para_cur = para_cur; // restore after ParserBase::reset()

    m_parser_p2para_cur->set_todo_status_forced( false );
    m_parser_p2para_cur->clear_tags();
    m_parser_p2para_cur->remove_onthefly_formats();
    m_parser_p2para_cur->add_or_remove_ref_from_tags( false );
    m_parser_p2para_cur->set_date( Date::NOT_SET );
    m_parser_p2para_cur->set_date_finish( Date::NOT_SET );
}

void
ParserBackGround::parse( Paragraph* para )
{
    DiaryElemTag::ContextDateEditability date_editability( false );
    m_parser_p2para_cur = para;

    if( para->is_code() )
        parse_code( 0, para->get_size() );
    else
        ParserBase::parse( 0, para->get_size() );
}

void
ParserBackGround::parse_code( const UstringSize bgn, const UstringSize end )
{
    Glib::MatchInfo match_info;
    int             pos_bgn_match, pos_end_match;
    const Ustring   text { m_parser_p2para_cur->get_text() };
    std::vector< std::pair< int,int > >
                    protected_ranges;

    auto inside_protected = [ & ]( int pos )
    {
        for( auto& r : protected_ranges )
            if( pos >= r.first && pos < r.second )
                return true;
        return false;
    };

    auto process_regex_group = [ & ]( const Glib::RefPtr< Glib::Regex >& regex, int format )
    {
        for( bool F_matches = regex->match( text, match_info );
             F_matches;
             F_matches = match_info.next() )
        {
            match_info.fetch_pos( 0, pos_bgn_match, pos_end_match );
            pos_bgn_match = STR::get_utf8_pos_from_byte_i( text, pos_bgn_match );
            pos_end_match = STR::get_utf8_pos_from_byte_i( text, pos_end_match );

            if( inside_protected( pos_bgn_match ) )  // match is inside a claimed region → skip
                continue;

            m_parser_p2para_cur->add_format( format, "", pos_bgn_match, pos_end_match );
            protected_ranges.emplace_back( pos_bgn_match, pos_end_match );
        }
    };

    reset( bgn, end );

    const int lc { VT::get_v<VT::QT, int, char >( m_parser_p2para_cur->get_quot_type() ) };

    if( lc > VT::QT::GENERIC::I )
    {
        process_regex_group( get_code_lang_regex( lc | VT::QT::COMMENTS ), VT::HFT_CODE_COMMENT );
        process_regex_group( get_code_lang_regex( lc | VT::QT::STRINGS ), VT::HFT_CODE_STRING );
        process_regex_group( get_code_lang_regex( lc | VT::QT::KEYWORDS ), VT::HFT_CODE_KEYWORD );
    }
}

void
ParserBackGround::process_paragraph()
{
    const auto&& F_todo_para{ bool( m_parser_p2para_cur->get_para_type() & VT::PS_TODO_GEN ) };

    // HIDDEN FORMATS
    for( auto format : m_parser_p2para_cur->m_formats )
    {
        switch( format->type )
        {
            case VT::HFT_TAG:
            {
                auto tag            { m_p2diary->get_tag_by_id( format->get_id_lo() ) };
                auto host_theme     { m_parser_p2para_cur->m_host->get_theme() };
                if( !tag ||
                    tag->get_name() != m_parser_p2para_cur->get_substr( format->pos_bgn,
                                                                        format->pos_end ) )
                {
                    format->uri = host_theme->color_link_broken.to_string();
                    format->set_id_lo( DEID::UNSET );
                }
                else
                {
                    format->uri = contrast3( host_theme->color_base,
                                             host_theme->image_bg == "#" ? host_theme->color_base2
                                                                         : host_theme->color_base,
                                             Color( tag->get_color() ) ).to_string();

                    m_parser_p2para_cur->set_tag( tag->get_id(), 1.0 );
                    m_p2format_tag_cur = format;

                    m_parser_p2para_cur->add_ref_to_tag( format->get_id_lo() );
                }
                break;
            }
            case VT::HFT_TAG_VALUE:
            {
            // BEAWARE that the last reference overrides previous ones within a paragraph:
                if( format->var_i >= int( format->pos_bgn ) ) // has planned value
                {
                    const Value v_real{ STR::get_d( get_substr( format->pos_bgn,
                                                                format->var_i ) ) };
                    const Value v_plan{ STR::get_d( get_substr( format->var_i + 1,
                                                                format->pos_end ) ) };
                    m_parser_p2para_cur->set_tag( m_p2format_tag_cur->get_id_lo(), v_real, v_plan );
                }
                else
                {
                    const Value v_real{ STR::get_d( get_substr( format->pos_bgn,
                                                                format->pos_end ) ) };
                    m_parser_p2para_cur->set_tag( m_p2format_tag_cur->get_id_lo(), v_real );
                }

                auto completion_tag { m_p2diary->get_completion_tag() };
                if( completion_tag && completion_tag->get_id() == m_p2format_tag_cur->get_id_lo() )
                {
                    if( F_todo_para )
                    {
                        const auto c{ m_parser_p2para_cur->get_completion() };
                        if( c == 1.0 )
                            m_parser_p2para_cur->set_list_type( VT::PLS::DONE::I );
                        else
                        if( c == 0.0 )
                            m_parser_p2para_cur->set_list_type( VT::PLS::TODO::I );
                        else
                            m_parser_p2para_cur->set_list_type( VT::PLS::PROGRS::I );
                    }

                    m_parser_p2para_cur->set_todo_status_forced( true );
                }
                break;
            }
            case VT::HFT_LINK_ID:
                m_parser_p2para_cur->add_ref_to_tag( format->get_id_lo() );
                break;
            case VT::HFT_DATE:
                // use date only when not in comment:
                if( !m_parser_p2para_cur->get_format_at( VT::HFT_COMMENT, format->pos_bgn ) )
                {
                    if( format->var_i == -1 ) // explicitly end date
                        m_parser_p2para_cur->set_date_finish( format->ref_id );
                    else
                        m_parser_p2para_cur->add_date( format->ref_id );
                }
                break;
        }
    }
}

void
ParserBackGround::apply_comment()
{
    m_parser_p2para_cur->add_format( VT::HFT_COMMENT, "", m_recipe_cur->m_pos_bgn,
                                                          m_parser_pos_cur + 1 );
}

void
ParserBackGround::apply_time()
{
    m_parser_p2para_cur->add_time( m_date_last );
    m_parser_p2para_cur->add_format( VT::HFT_TIME, "", m_recipe_cur->m_pos_bgn,
                                                       m_recipe_cur->m_pos_mid );
    m_parser_p2para_cur->add_format( VT::HFT_TIME_MS, "", m_recipe_cur->m_pos_mid - 3,
                                                          m_recipe_cur->m_pos_mid );
}

void
ParserBackGround::apply_link()
{
    switch( m_recipe_cur->m_id )
    {
        case RID_DATE:
        {
            auto format{ m_parser_p2para_cur->add_format( VT::HFT_DATE,
                                                          "",
                                                          m_recipe_cur->m_pos_bgn,
                                                          m_parser_pos_cur + 1 ) };
            format->ref_id = m_date_last;
            if( m_recipe_cur->m_pos_bgn > 2 &&
                get_char_at( m_recipe_cur->m_pos_bgn - 1 ) == '.' &&
                get_char_at( m_recipe_cur->m_pos_bgn - 2 ) == '.' &&
                get_char_at( m_recipe_cur->m_pos_bgn - 3 ) == '.' )
                {
                    format->var_i = -1; // end mark
                    m_parser_p2para_cur->add_format( VT::HFT_DATE_ELLIPSIS,
                                                     "",
                                                     m_recipe_cur->m_pos_bgn - 3,
                                                     m_recipe_cur->m_pos_bgn );
                }
            else
            if( int( m_parser_pos_cur ) < m_parser_p2para_cur->get_size() &&
                get_char_at( m_parser_pos_cur + 1 ) == '.' &&
                get_char_at( m_parser_pos_cur + 2 ) == '.' &&
                get_char_at( m_parser_pos_cur + 3 ) == '.' )
                {
                    m_parser_p2para_cur->add_format( VT::HFT_DATE_ELLIPSIS,
                                                     "",
                                                     m_parser_pos_cur + 1,
                                                     m_parser_pos_cur + 4 );
                }
        break;
        }
        case RID_LINK_AT:
        {
            const auto&& uri{ "mailto:" + m_parser_p2para_cur->get_substr( m_recipe_cur->m_pos_bgn,
                                                                           m_parser_pos_cur ) };
            m_parser_p2para_cur->add_format( VT::HFT_LINK_ONTHEFLY,
                                             uri,
                                             m_recipe_cur->m_pos_bgn,
                                             m_parser_pos_cur );
            break;
        }
        case RID_URI:
        {
            const auto&& uri{ m_parser_p2para_cur->get_substr( m_recipe_cur->m_pos_bgn,
                                                               m_parser_pos_cur ) };
            m_parser_p2para_cur->add_format( VT::HFT_LINK_ONTHEFLY,
                                             uri,
                                             m_recipe_cur->m_pos_bgn,
                                             m_parser_pos_cur );
            break;
        }
    }
}

void
ParserBackGround::apply_inline_tag_value_nmbr()
{
    if( m_p2format_tag_cur )
    {
        m_parser_p2para_cur->add_format( VT::HFT_TAG_VALUE,
                                         "",
                                         m_recipe_cur->m_pos_bgn,
                                         m_parser_pos_last_digit + 1 )->
                var_i = m_parser_pos_extra_1;
    }
}
