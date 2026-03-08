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


#include "parser_upgrader.hpp"
#include "../diaryelements/diary.hpp"


using namespace LoG;

const ParserBase::Recipe::Contents
    ParserUpgrader::m_rc_link_hidden_end =
    { { Ch_TAB, &ParserBase::set_middle },
      { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, Ch_MORE, nullptr },
      { Ch_MORE, &ParserBase::apply_link_old } };

const ParserBase::Recipe::Contents
    ParserUpgrader::m_rc_markup =
    { CFC_BLANK|CF_PARENTHESIS,
      { CF_MARKUP, &ParserBase::junction_markup } },

    ParserUpgrader::m_rc_markup_b_end =
    { { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, Ch_ASTERISK, &ParserBase::junction_markup2 },
      { Ch_ASTERISK, &ParserBase::apply_bold } },

    ParserUpgrader::m_rc_markup_i_end =
    { { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, Ch_UNDERSCORE, &ParserBase::junction_markup2 },
      { Ch_UNDERSCORE, &ParserBase::apply_italic } },

    ParserUpgrader::m_rc_markup_h_end =
    { { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, Ch_HASH, &ParserBase::junction_markup2 },
      { Ch_HASH, &ParserBase::apply_highlight } },

    ParserUpgrader::m_rc_markup_s_end =
    { { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, Ch_EQUALS, &ParserBase::junction_markup2 },
      { Ch_EQUALS, &ParserBase::apply_strikethrough } },

    ParserUpgrader::m_rc_tag =
    { CFC_BLANK|CF_PARENTHESIS,
      { Ch_COLON, &ParserBase::set_start },
      CFC_NONSPACE,
      { CFC_ANY_BUT_NEWLINE|CM_OPTIONAL|CM_MULTIPLE, Ch_COLON, nullptr },
      Ch_COLON,
      { CFC_ANY, &ParserBase::junction_tag }, // equal sign
      CF_SIGN|CM_OPTIONAL,
      { CF_NUMERIC|CM_OPTIONAL|CM_MULTIPLE, &ParserBase::junction_number },
      { CFC_ANY_BUT_NUMERIC|CF_VALUE_SEPARATOR, &ParserBase::junction_tag2 }, // slash
      { CF_NUMERIC|CM_MULTIPLE, &ParserBase::junction_number },
      { CFC_ANY_BUT_NUMERIC, &ParserBase::apply_inline_tag_old } };


ParserUpgrader::ParserUpgrader()
{
    m_all_recipes.insert( new Recipe{ RID_MARKUP, this, &m_rc_markup, 0, 0 } );
    m_all_recipes.insert( new Recipe{ RID_TAG, this, &m_rc_tag, 0, 0 } );
}

void
ParserUpgrader::parse( Paragraph* p2para, int version_read )
{
    m_parser_p2para_cur = p2para;
    m_version_read = version_read;
    ParserBase::parse( 0, m_parser_p2para_cur->get_size() );
}

void
ParserUpgrader::junction_markup()
{
    set_start();
    m_recipe_cur->m_index = -1;    // as it will be ++
    m_recipe_cur->m_state |= Recipe::RS_BLOCK;

    switch( m_char_cur )
    {
        case '*':
            m_recipe_cur->m_id = RID_BOLD;
            m_recipe_cur->m_blocks_new = RID_BOLD;
            m_recipe_cur->m_contents = &m_rc_markup_b_end;
            break;
        case '_':
            m_recipe_cur->m_id = RID_ITALIC;
            m_recipe_cur->m_blocks_new = RID_ITALIC;
            m_recipe_cur->m_contents = &m_rc_markup_i_end;
            break;
        case '#':
            m_recipe_cur->m_id = RID_HIGHLIGHT;
            m_recipe_cur->m_blocks_new = RID_HIGHLIGHT;
            m_recipe_cur->m_contents = &m_rc_markup_h_end;
            break;
        case '=':
            m_recipe_cur->m_id = RID_STRIKETHROUGH;
            m_recipe_cur->m_blocks_new = RID_STRIKETHROUGH;
            m_recipe_cur->m_contents = &m_rc_markup_s_end;
            break;
    }
}

void
ParserUpgrader::junction_markup2()
{
    switch( m_recipe_cur->m_id )
    {
        case RID_BOLD:
            m_recipe_cur->m_id = RID_MARKUP_B_END;
            break;
        case RID_ITALIC:
            m_recipe_cur->m_id = RID_MARKUP_I_END;
            break;
        case RID_HIGHLIGHT:
            m_recipe_cur->m_id = RID_MARKUP_H_END;
            break;
        case RID_STRIKETHROUGH:
            m_recipe_cur->m_id = RID_MARKUP_S_END;
            break;
    }
}

void
ParserUpgrader::junction_number()
{
    // this is used to disregard the spaces which can be used in numbers...
    // ...as thousands separator per ISO 31-0 standard
    if( m_char_cur != ' ' )
        m_parser_pos_extra_2 = m_parser_pos_cur;
    // disallow spaces right after =
    else if( m_parser_pos_cur == m_recipe_cur->m_pos_mid + 1 )
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
    // disallow consecutive spaces
    else if( m_char_last == ' ' )
    {
        apply_inline_tag_old();
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
    }
}

void
ParserUpgrader::junction_tag()
{
    apply_inline_tag_old();
    if( m_char_cur == '=' )
        m_recipe_cur->m_pos_mid = m_parser_pos_cur;
    else
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
}

void
ParserUpgrader::junction_tag2()
{
    if( m_char_cur == '/' )
        m_parser_pos_extra_1 = m_parser_pos_cur;
    else
    if( m_cf_last & ( CF_DIGIT | CF_SPACE ) )
    {
        apply_inline_tag_old();
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
    }
    else
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
}

void
ParserUpgrader::junction_link()
{
    if( m_version_read < 2014 )
        if( m_recipe_cur->m_F_accept_spaces )
        {
            m_active_recipes.push_back(
                    new Recipe{ m_recipe_cur->m_id, this, &m_rc_link_hidden_end, 0,
                                m_recipe_cur->m_pos_bgn - 1, m_parser_pos_cur } );
        }
}

HiddenFormat*
ParserUpgrader::apply_markup( int type, UstringSize pos_mark_bgn, UstringSize pos_mark_end )
{
    const auto length_text { pos_mark_end - pos_mark_bgn - 1 };

    // shift existing formats
    for( auto format : m_parser_p2para_cur->m_formats )
    {
        if( pos_mark_bgn <= format->pos_bgn ) format->pos_bgn--;
        if( pos_mark_bgn <= format->pos_end ) format->pos_end--;
        if( pos_mark_end <= format->pos_bgn ) format->pos_bgn--;
        if( pos_mark_end <= format->pos_end ) format->pos_end--;
    }

    HiddenFormat* format = m_parser_p2para_cur->add_format( type, "",
                                                            pos_mark_bgn, pos_mark_end - 1 );
    Ustring text_new = m_parser_p2para_cur->m_text.substr( 0, pos_mark_bgn );
    m_parser_p2para_cur->m_text = ( m_parser_p2para_cur->m_text.substr( 0, pos_mark_bgn ) +
                                  m_parser_p2para_cur->m_text.substr( pos_mark_bgn + 1,
                                                                      length_text ) +
                                  ( int( pos_mark_end ) < m_parser_p2para_cur->get_size() ?
                                  m_parser_p2para_cur->m_text.substr( pos_mark_end + 1 ) : "" ) );

    m_pos_end = m_parser_p2para_cur->get_size();
    m_parser_pos_cur -= 2;

    return format;
}
void
ParserUpgrader::apply_bold()
{
    apply_markup( VT::HFT_BOLD, m_recipe_cur->m_pos_bgn, m_parser_pos_cur );
}
void
ParserUpgrader::apply_italic()
{
    apply_markup( VT::HFT_ITALIC, m_recipe_cur->m_pos_bgn, m_parser_pos_cur );
}
void
ParserUpgrader::apply_highlight()
{
    apply_markup( VT::HFT_HIGHLIGHT, m_recipe_cur->m_pos_bgn, m_parser_pos_cur );
}
void
ParserUpgrader::apply_strikethrough()
{
    apply_markup( VT::HFT_STRIKETHRU, m_recipe_cur->m_pos_bgn, m_parser_pos_cur );
}

void
ParserUpgrader::apply_inline_tag_old()
{
    auto pos_bgn{ m_recipe_cur->m_pos_mid > 0 ? m_recipe_cur->m_pos_bgn
                                              : m_recipe_cur->m_pos_bgn  + 1 };
    auto pos_end{ m_recipe_cur->m_pos_mid > 0 ? m_recipe_cur->m_pos_mid
                                              : m_parser_pos_cur - 1 };
    Entry* tag = m_parser_p2para_cur->m_host->get_diary()->get_entry_by_name(
            get_substr( pos_bgn, pos_end ) );

    if( !tag ) return;

    if( m_recipe_cur->m_pos_mid == 0 )
    {
        auto format = apply_markup( VT::HFT_TAG, pos_bgn - 1, pos_end );

        format->set_id_lo( tag->get_id() );
        m_parser_p2para_cur->set_tag( tag->get_id(), 1.0 );
    }
    else // value
    {
        if( m_parser_pos_extra_1 > pos_bgn ) // has planned value
        {
            const Value v_real{ STR::get_d( get_substr( m_recipe_cur->m_pos_mid + 1,
                                                        m_parser_pos_extra_1 ) ) };
            const Value v_plan{ STR::get_d( get_substr( m_parser_pos_extra_1 + 1,
                                                        m_parser_pos_extra_2 + 1 ) ) };
            m_parser_p2para_cur->set_tag( tag->get_id(), v_real, v_plan );
        }
        else
            m_parser_p2para_cur->set_tag( tag->get_id(),
                                          STR::get_d( get_substr( m_recipe_cur->m_pos_mid + 1,
                                                                  m_parser_pos_extra_2 + 1 ) ) );
    }
}

void
ParserUpgrader::apply_link_old()
{
    // if( m_recpe_cur == &m_rc_link_hidden_end ) --later
    const auto length_uri { m_recipe_cur->m_pos_mid - m_recipe_cur->m_pos_bgn - 1 };
    const auto length_lbl { m_parser_pos_cur - m_recipe_cur->m_pos_mid - 1 };
    const auto pos_rest   { m_parser_pos_cur + 1 };

    m_parser_p2para_cur->add_format(
            VT::HFT_LINK_URI,
            m_parser_p2para_cur->m_text.substr( m_recipe_cur->m_pos_bgn + 1, length_uri ),
            m_recipe_cur->m_pos_bgn, m_recipe_cur->m_pos_bgn + length_lbl );
    m_parser_p2para_cur->m_text =
            ( m_parser_p2para_cur->m_text.substr( 0, m_recipe_cur->m_pos_bgn ) +
            m_parser_p2para_cur->m_text.substr( m_recipe_cur->m_pos_mid + 1, length_lbl ) +
            m_parser_p2para_cur->m_text.substr( pos_rest, m_parser_p2para_cur->m_text.size() -
                                                pos_rest ) );

    m_pos_end = m_parser_p2para_cur->m_text.size();
    m_parser_pos_cur -= ( length_uri + 3 );
}
