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


#include "../lifeograph.hpp"
#include "parser_base.hpp"


using namespace LoG;

// TEXT FORMATTING
const ParserBase::Recipe::Contents
    ParserBase::m_rc_comment =
    { { Ch_SBB, &ParserBase::set_start },
      Ch_SBB,
      { CFC_ANY_BUT_NEWLINE|CM_MULTIPLE|CM_OPTIONAL, Ch_SBE, nullptr },
      Ch_SBE,
      { Ch_SBE, &ParserBase::apply_comment } },

    ParserBase::m_rc_tag_value_nmbr =
    { { Ch_EQUALS, &ParserBase::check_inline_tag_value_start },
      CF_SIGN|CM_OPTIONAL,  // + or -
      { CF_NUMERIC|CM_OPTIONAL|CM_MULTIPLE, &ParserBase::junction_number },
      { CFC_ANY_BUT_NUMERIC|CF_VALUE_SEPARATOR, &ParserBase::junction_tag_value_sep }, // slash
      { CF_NUMERIC|CM_MULTIPLE, &ParserBase::junction_number },
      { CFC_ANY_BUT_NUMERIC, &ParserBase::apply_inline_tag_value_nmbr } },

    ParserBase::m_rc_tag_value_date =
    { { Ch_EQUALS, &ParserBase::check_inline_tag_value_start },
      CF_DIGIT,
      CF_DIGIT,
      CF_DIGIT,
      CF_DIGIT,
      { CF_DATE_SEPARATOR, &ParserBase::junction_date_dotym },
      CF_DIGIT,
      CF_DIGIT,
      { CF_DATE_SEPARATOR, &ParserBase::junction_date_dotmd },
      CF_DIGIT,
      { CF_DIGIT, &ParserBase::check_inline_tag_value_date } },

    ParserBase::m_rc_time =
    { CFC_BLANK|CF_PUNCTUATION,
      { CF_DIGIT, &ParserBase::set_start },
      CF_DIGIT,
      { Ch_COLON, &ParserBase::junction_time_hm },
      CF_DIGIT,
      CF_DIGIT,
      { CFC_ANY, &ParserBase::junction_time_ms },
      CF_DIGIT,
      { CF_DIGIT, &ParserBase::check_time } };

// LINK
const ParserBase::Recipe::Contents
    ParserBase::m_rc_date =
    { CFC_BLANK|CF_PUNCTUATION,
      { CF_DIGIT, &ParserBase::set_start },
      CF_DIGIT,
      CF_DIGIT,
      CF_DIGIT,
      { CF_DATE_SEPARATOR, &ParserBase::junction_date_dotym },
      CF_DIGIT,
      CF_DIGIT,
      { CF_DATE_SEPARATOR, &ParserBase::junction_date_dotmd },
      CF_DIGIT,
      { CF_DIGIT, &ParserBase::check_date } },

    ParserBase::m_rc_colon =
    { { Ch_COLON, &ParserBase::junction_colon } },

    ParserBase::m_rc_at_email =
    { { Ch_AT, &ParserBase::junction_at },
      { CFC_EMAIL|CM_MULTIPLE, Ch_DOT, nullptr },
      Ch_DOT,
      CFC_EMAIL|CM_MULTIPLE,
      { CFC_BLANK|CF_PARENTHESIS, &ParserBase::junction_link } },

    ParserBase::m_rc_link_file =
    { Ch_SLASH,
      Ch_SLASH,
      CF_SPACE_CONDTNL|CFC_NONSPACE|CM_MULTIPLE,
      { CF_SPACE_CONDTNL|CFC_BLANK, &ParserBase::junction_link } },

    ParserBase::m_rc_link_email =
    { { CFC_EMAIL|CM_MULTIPLE, Ch_AT, nullptr },
      Ch_AT,
      { CFC_NONSPACE|CM_MULTIPLE, Ch_DOT, nullptr },
      Ch_DOT,
      CFC_EMAIL|CM_MULTIPLE,
      { CFC_BLANK|CF_PARENTHESIS, &ParserBase::junction_link } },

    ParserBase::m_rc_link_geo =
    { CFC_NONSPACE|CM_MULTIPLE,
      { CFC_BLANK|CF_PARENTHESIS, &ParserBase::junction_link } };

// TEXT PARSER =====================================================================================
ParserBase::ParserBase()
{
    m_all_recipes.insert( new Recipe{ RID_COMMENT, this, &m_rc_comment, 0, 0 } );
    m_all_recipes.insert( new Recipe{ RID_DATE, this, &m_rc_date, 0, 0 } );
    m_all_recipes.insert( new Recipe{ RID_TIME, this, &m_rc_time, 0, RID_TIME } );
    m_all_recipes.insert( new Recipe{ RID_COLON, this, &m_rc_colon, 0, 0 } );
    m_all_recipes.insert( new Recipe{ RID_LINK_AT, this, &m_rc_at_email, 0, 0 } );
    m_all_recipes.insert( new Recipe{ RID_GENERIC, this, &m_rc_tag_value_nmbr, 0, 0 } );
    //m_all_recipes.insert( new Recipe{ RID_GENERIC, this, &m_rc_tag_value_date, 0, 0 } );

    //m_link_protocols.emplace( "deid", new LinkProtocol( RID_ID, &m_rc_link_id ) );
    m_link_protocols.emplace( "file", new LinkProtocol( RID_URI, &m_rc_link_file ) );
    m_link_protocols.emplace( "ftp", new LinkProtocol( RID_URI, &m_rc_link_file ) );
    m_link_protocols.emplace( "geo", new LinkProtocol( RID_URI, &m_rc_link_geo ) );
    m_link_protocols.emplace( "http", new LinkProtocol( RID_URI, &m_rc_link_file ) );
    m_link_protocols.emplace( "https", new LinkProtocol( RID_URI, &m_rc_link_file ) );
    m_link_protocols.emplace( "mailto", new LinkProtocol( RID_URI, &m_rc_link_email ) );
    m_link_protocols.emplace( "rel", new LinkProtocol( RID_URI, &m_rc_link_file ) );
}
ParserBase::~ParserBase()
{
    for( auto&& kv : m_enchant_dicts )
        enchant_broker_free_dict( Lifeograph::s_enchant_broker, kv.second );

    for( auto recipe : m_all_recipes )
        delete recipe;

    for( auto& kv_protocol : m_link_protocols )
        delete kv_protocol.second;
}

void
ParserBase::reset( UstringSize bgn, UstringSize end )
{
    m_pos_end = end;
    m_parser_pos_cur = m_parser_pos_para_bgn = m_parser_pos_extra_1 = m_parser_pos_extra_2 = bgn;
    m_parser_pos_blank = m_parser_pos_last_digit = ( bgn - 1 );
    m_F_spellcheck_exceptions_initialized = false;
    //m_parser_p2para_cur = nullptr; reset at the end to enable single para parsers

    m_cf_curr = Ch_NEWLINE|CF_NEWLINE;
    m_cf_last = Ch_NOT_SET;
    m_word_cur.clear();
    m_word_count = 0;
    m_int_last = 0;
    m_date_last = 0;

    for( auto r : m_active_recipes )
        delete r;
    m_active_recipes.clear();

    // start as if previous char is a new line
    for( Recipe* r : m_all_recipes )
    {
        r->m_index = 0;
        r->m_state = Recipe::RS_NOT_SET;
        if( r->process_char() == Recipe::RS_IN_PROGRESS )
            m_active_recipes.push_back( new Recipe( r ) );
    }
}

bool
ParserBase::is_pos_cur_in_spellchk_zone()
{
    if( !m_F_spellcheck_exceptions_initialized )
    {
        m_spellcheck_exceptions.clear();

        // fill in the spellcheck exceptions:
        for( auto f : m_parser_p2para_cur->m_formats )
        {
            if( f->type == VT::HFT_TAG || f->type == VT::HFT_LINK_EVAL )
            {
                if      ( m_spellcheck_exceptions.empty() ||
                          f->pos_bgn > m_spellcheck_exceptions.front().second )
                    m_spellcheck_exceptions.push_back( { f->pos_bgn, f->pos_end } );
                else if ( f->pos_end > m_spellcheck_exceptions.front().second )
                    m_spellcheck_exceptions.back().second = f->pos_end;
            }
        }
        m_F_spellcheck_exceptions_initialized = true;
    }

    while( !m_spellcheck_exceptions.empty() &&
           m_parser_pos_in_para > m_spellcheck_exceptions.front().second )
        m_spellcheck_exceptions.pop_front();

    return( m_spellcheck_exceptions.empty() ||
            m_parser_pos_in_para < m_spellcheck_exceptions.front().first ||
            m_parser_pos_in_para > m_spellcheck_exceptions.front().second );
}

void
ParserBase::parse( const UstringSize bgn, const UstringSize end )
{
    reset( bgn, end );

    if( bgn == end ) // zero length
    {
        process_paragraph();
        return;
    }

    for( ; m_parser_pos_cur < m_pos_end && !m_F_stop; ++m_parser_pos_cur )
    {
        m_char_last = m_char_cur;
        m_char_cur = get_char_at( m_parser_pos_cur );
        m_parser_pos_in_para = ( m_parser_pos_cur - m_parser_pos_para_bgn );

        // MARKUP PARSING
        switch( m_char_cur )
        {
            //case 0:     // should never be the case
            case '\n':
            case '\r':
                m_cf_curr = Ch_NEWLINE|CF_NEWLINE;
                process_char();
                //if( m_pos_cur > bgn ) // skip the \n at the start of the parsing region...
                // ...no longer necessary as parsing regions should never start with \n any more
                process_paragraph();
                m_parser_pos_para_bgn = m_parser_pos_cur + 1;
                m_F_spellcheck_exceptions_initialized = false;
                continue;   // !!!!! CONTINUES TO SKIP process_char() BELOW !!!!!
            case ' ':
                m_cf_curr = Ch_SPACE|CF_SPACE|CF_TODO_STATUS|CF_NUMERIC;
                break;
            case '*': // PUNCTUATION
                m_cf_curr = Ch_ASTERISK|CF_PUNCTUATION|CF_MARKUP|CF_IDENTIFIER;
                break;
            case '_': // PUNCTUATION
                m_cf_curr = Ch_UNDERSCORE|CF_PUNCTUATION|CF_MARKUP|CF_IDENTIFIER;
                break;
            case '=': // PUNCTUATION
                m_cf_curr = Ch_EQUALS|CF_PUNCTUATION|CF_MARKUP;
                break;
            case '#': // PUNCTUATION
                m_cf_curr = Ch_HASH|CF_PUNCTUATION|CF_MARKUP;
                break;
            case '[': // PUNCTUATION
                m_cf_curr = Ch_SBB|CF_PUNCTUATION|CF_PARENTHESIS;
                break;
            case ']': // PUNCTUATION
                m_cf_curr = Ch_SBE|CF_PUNCTUATION|CF_PARENTHESIS;
                break;
            case '(': case ')':
            case '{': case '}': // parentheses
                m_cf_curr = CF_PUNCTUATION|CF_PARENTHESIS;
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                m_cf_curr = CF_DIGIT|CF_NUMERIC|CF_IDENTIFIER;
                process_number();   // calculates numeric value
                break;
            case '.': // PUNCTUATION
                m_cf_curr = Ch_DOT|CF_PUNCTUATION|CF_DATE_SEPARATOR|CF_NUMERIC|CF_IDENTIFIER;
                break;
            case ',': // PUNCTUATION
                m_cf_curr = Ch_COMMA|CF_PUNCTUATION|CF_NUMERIC;
                break;
            case '-': // PUNCTUATION - CF_SIGNSPELL does not seem to be necessary
                m_cf_curr = Ch_DASH|CF_PUNCTUATION|CF_DATE_SEPARATOR|CF_SIGN|CF_IDENTIFIER;
                break;
            case '/': // PUNCTUATION
                m_cf_curr = Ch_SLASH|CF_PUNCTUATION|CF_DATE_SEPARATOR|CF_VALUE_SEPARATOR;
                break;
            case ':': // PUNCTUATION
                m_cf_curr = Ch_COLON|CF_PUNCTUATION;
                break;
            case '@': // PUNCTUATION
                m_cf_curr = Ch_AT|CF_PUNCTUATION;
                break;
            case '<': // PUNCTUATION
                m_cf_curr = Ch_LESS|CF_PUNCTUATION;
                break;
            case '>': // PUNCTUATION
                m_cf_curr = Ch_MORE|CF_PUNCTUATION|CF_TODO_STATUS;
                break;
            case '|': // PUNCTUATION
                m_cf_curr = Ch_PIPE|CF_PUNCTUATION;
                break;
            case '\t':
                m_cf_curr = Ch_TAB|CF_TAB;
                break;
            // LIST CHARS
            case '~':
                m_cf_curr = Ch_TILDE|CF_PUNCTUATION|CF_TODO_STATUS;
                break;
            case '+':
                m_cf_curr = Ch_PLUS|CF_PUNCTUATION|CF_TODO_STATUS|CF_SIGN|CF_IDENTIFIER;
                break;
            case 'x':
            case 'X':
                m_cf_curr = Ch_X|CF_ALPHA|CF_SPELLCHECK|CF_TODO_STATUS|CF_IDENTIFIER;
                break;
            case '\'':
                m_cf_curr = CF_PUNCTUATION|CF_SPELLCHECK|CF_IDENTIFIER;
                break;
            default:
                m_cf_curr = Glib::Unicode::isalpha( m_char_cur ) ? CF_ALPHA|CF_SPELLCHECK|CF_IDENTIFIER
                                                                 : CF_PUNCTUATION;
                break;
        }
        process_char();
    }
    // end of the text -treated like a new line for all means and purposes
    // if( m_pos_end > 0 ) // only when finish is not forced
    // {
        m_char_last = m_char_cur;
        m_char_cur = '\n';
        m_cf_curr = Ch_NEWLINE|CF_NEWLINE;
        process_char();
        process_paragraph();
    // }
    m_parser_p2para_cur = nullptr;
}

ParserBase::Recipe::State
ParserBase::Recipe::process_char()
{
    if( m_parent->m_blocked_flags & m_id )
        return( m_state = RS_REJECTED );

    bool flag_loop{ true };

    while( flag_loop )
    {
        const AbsChar& absch{ m_contents->at( m_index ) };

        if( ( ! absch.exception || ( m_parent->m_cf_curr & CFC_CHAR_MASK ) != absch.exception ) &&
            cmp_chars( get_char_class_at( m_index ), m_parent->m_cf_curr ) )
        {
            if( absch.applier )
            {
                m_parent->m_recipe_cur = this;
                ( m_parent->*m_contents->at( m_index ).applier )(); // may modify m_state
            }

            if( !( absch.flags & CM_MULTIPLE ) )
                m_index++;
            else
                m_F_multiple_char_matched = true;
            flag_loop = false;
        }
        else
        if( ( absch.flags & CM_OPTIONAL ) ||
            ( ( absch.flags & CM_MULTIPLE ) && m_F_multiple_char_matched ) )
        {
            m_index++;
            m_F_multiple_char_matched = false;
        }
// The old way. It was unpleasant as it involved a backwards iteration
//        else
//        if( m_index > 0 &&
//            ( m_contents->at( m_index - 1 ).flags & ( CM_MULTIPLE|CM_OPTIONAL ) ) == CM_MULTIPLE )
//            m_index--;
        else
        {
            m_state = RS_REJECTED;
            flag_loop = false;
        }
    }

    if( m_state != RS_REJECTED )
    {
        if( ( m_state & RS_FINISHED ) || m_index == m_contents->size() )
            m_state = ( m_state & RS_BLOCK ) | RS_ACCEPTED;
        else
            m_state = ( m_state & RS_BLOCK ) | RS_IN_PROGRESS;
    }

    return m_state;
}

// SPELL CHECKING BY DIRECT UTILIZATION OF ENCHANT (code partly copied from GtkSpell library)
static void
set_lang_from_dict_cb( const char* const lang_tag, const char* const provider_name,
                       const char* const provider_desc, const char* const provider_file,
                       void* user_data )
{
    String* language = ( String* ) user_data;
    ( *language ) = lang_tag;
}
inline void
ParserBase::process_char()
{
    m_blocked_flags = 0;
    m_blocked_flags_new = 0;

    if( m_parser_p2para_cur && m_parser_p2para_cur->is_code() )
        goto skip_recipes;

    // UPDATE WORD LAST
    if( m_cf_curr & CF_SPELLCHECK )
    {
        if( not( m_cf_last & CF_SPELLCHECK ) )
        {
            m_word_cur.clear();
            m_word_count++;
        }

        m_word_cur += m_char_cur;
    }
    else
    {
        if     ( m_cf_curr & CFC_BLANK )
            m_parser_pos_blank = m_parser_pos_cur;
        else if( m_cf_curr & CF_DIGIT )
            m_parser_pos_last_digit = m_parser_pos_cur;

        if( m_F_spellchk_enabled && m_parser_p2para_cur && !m_word_cur.empty() &&
            ( m_cf_last & CF_SPELLCHECK ) && is_pos_cur_in_spellchk_zone() )
        {
            // SPELL CHECKING LANGUAGE
            String&& lang { m_parser_p2para_cur->get_lang_final() };
            if( !lang.empty() )
            {
                auto dict = get_dict( lang );
                if( !dict )
                {
                    dict = enchant_broker_request_dict( Lifeograph::s_enchant_broker,
                                                        lang.c_str() );
                    if( dict )
                    {
                        enchant_dict_describe( dict, set_lang_from_dict_cb, &lang );
                        m_enchant_dicts[ lang ] = dict;
                    }
                    else
                        print_error( "Enchant error for language: ", lang );
                }
                if( dict && enchant_dict_check( dict, m_word_cur.c_str(), m_word_cur.length() ) )
                    m_parser_p2para_cur->add_format( VT::HFT_MISSPELLED, "",
                                                     m_parser_pos_cur - int( m_word_cur.length() ),
                                                     m_parser_pos_cur );
            }
        }
    }

    // FIRST CHECK ACTIVE RECIPES
    for( auto&& it = m_active_recipes.begin(); it != m_active_recipes.end(); )
    {
        Recipe* r{ *it };
        if( !( r->process_char() & Recipe::RS_IN_PROGRESS ) )
        {
            it = m_active_recipes.erase( it );
            delete r;
        }
        else
        {
            it++;
            if( r->m_state & Recipe::RS_BLOCK )
            {
                m_blocked_flags |= r->m_blocks;
                m_blocked_flags_new |= r->m_blocks_new;
            }
        }
    }
    // CHECK FOR BLOCKS ONCE MORE
    m_active_recipes.remove_if(
            [] ( Recipe*& r )
            {
                if( r->m_parent->m_blocked_flags & r->m_id )
                {
                    delete r;
                    return true;
                }
                else
                    return false;
            } );

    // THEN CHECK IF IT TRIGGERS ANY OTHER RECIPE
    if( m_parser_pos_cur < m_pos_end )
        for( Recipe* r : m_all_recipes )
        {
            if( m_blocked_flags_new & r->m_id ) continue;
            r->m_index = 0;
            r->m_state = Recipe::RS_NOT_SET;
            if( r->process_char() == Recipe::RS_IN_PROGRESS )
                m_active_recipes.push_back( new Recipe( r ) );
        }

skip_recipes:
    m_cf_last = m_cf_curr;
}

void
ParserBase::add_link_protocol( const std::string& name, Recipe::Id id, Recipe::Contents* rc )
{
    m_link_protocols.emplace( name, new LinkProtocol( id, rc ) );
}

// JUNCTIONS =======================================================================================
void
ParserBase::check_date()
{
    Date::set_day( m_date_last, m_int_last );

    if( Date::is_valid( m_date_last ) )
        apply_link();
}

void
ParserBase::check_inline_tag_value_start()
{
    if( !m_parser_p2para_cur )
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
    else
    {
        m_p2format_tag_cur = m_parser_p2para_cur->get_format_at(
                VT::HFT_TAG, m_parser_pos_in_para - 1 );

        if( !m_p2format_tag_cur )
            m_recipe_cur->m_state = Recipe::RS_REJECTED;
        else
            m_recipe_cur->m_pos_bgn = m_parser_pos_cur + 1; // set start to past the '='
    }
}

void
ParserBase::check_inline_tag_value_date()
{
    Date::set_day( m_date_last, m_int_last );

    if( Date::is_valid( m_date_last ) )
        apply_link();
    // TODO 3.1 or later...
}

void
ParserBase::check_time()
{
    if( m_int_last < 60 )
    {
        Date::set_sec( m_date_last, m_int_last );
        m_recipe_cur->m_pos_mid = m_parser_pos_cur + 1; // pos_mid is used as pos_end for time
        apply_time();
    }
}

void
ParserBase::junction_date_dotym()
{
    if( m_int_last >= Date::YEAR_MIN && m_int_last <= Date::YEAR_MAX )
        Date::set_year( m_date_last, m_int_last );
    else
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
}

void
ParserBase::junction_date_dotmd()
{
    if( m_int_last >= 1 && m_int_last <= 12 &&
        // two separators must be the same:
        get_char_at( m_parser_pos_cur - 3 ) == m_char_cur )
    {
        Date::set_month( m_date_last, m_int_last );
    }
    else
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
}

void
ParserBase::junction_time_hm()
{
    Date::set_time( m_date_last, 0 );

    if( ! Date::set_hour( m_date_last, m_int_last ) )
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
    else
        m_recipe_cur->m_state = Recipe::RS_BLOCK; // activate blocking
}

void
ParserBase::junction_time_ms()
{
    if( ! Date::set_min( m_date_last, m_int_last ) )
        m_recipe_cur->m_state = Recipe::RS_REJECTED;

    else if( m_char_cur != ':' )
    {
        m_recipe_cur->m_pos_mid = m_parser_pos_cur; // pos_mid is used as pos_end for time

        apply_time();
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
    }
}

void
ParserBase::junction_colon()
{
    auto kv_protocol = m_link_protocols.find( m_word_cur );
    if( kv_protocol != m_link_protocols.end() )
    {
        Recipe* r{ new Recipe( kv_protocol->second->type, this, kv_protocol->second->rc,
                               RID_LINK_AT | RID_LINK_NAME,
                               m_parser_pos_cur - m_word_cur.length(), m_parser_pos_cur + 1 ) };
        m_active_recipes.push_back( r );

        if( m_parser_pos_cur - m_word_cur.length() > 0 &&
            get_char_at( m_parser_pos_cur - m_word_cur.length() - 1 ) == '<' )
            r->m_F_accept_spaces = true;

        r->m_state |= Recipe::RS_BLOCK;
    }
}

void
ParserBase::junction_at()
{
    m_recipe_cur->m_pos_bgn = m_parser_pos_blank + 1;
}

void
ParserBase::junction_link()
{
    // sorry but spellcheck cannot be blocked beforehand for on the fly links, so remove afterwards
    if( m_parser_p2para_cur )
        m_parser_p2para_cur->remove_format( VT::HFT_MISSPELLED,
                                            m_recipe_cur->m_pos_bgn,
                                            m_parser_pos_cur );
    apply_link();
}

void
ParserBase::junction_tag_value_sep()
{
    if( m_char_cur == '/' )
        m_parser_pos_extra_1 = m_parser_pos_cur;
    else
    if( ( m_cf_last & ( CF_DIGIT | CF_SPACE ) ) &&
        m_parser_pos_last_digit >= m_recipe_cur->m_pos_bgn )
    {
        apply_inline_tag_value_nmbr();
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
    }
    else
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
}

void
ParserBase::junction_number()
{
    // this is used to disregard the spaces which can be used in numbers...
    // ...as thousands separator per ISO 31-0 standard
    if( m_char_cur != ' ' )
        m_parser_pos_extra_2 = m_parser_pos_cur;
    // disallow spaces right after =
    else if( m_char_last == '=' )
        m_recipe_cur->m_state = Recipe::RS_REJECTED;
    // disallow consecutive spaces
    else if( m_char_last == ' ' )
    {
        apply_inline_tag_value_nmbr();
        m_recipe_cur->m_state |= Recipe::RS_FINISHED;
    }
}

// HELPERS =========================================================================================
inline void
ParserBase::set_start_for_multi()
{
    // checks for and prevents repetitive assignment when used on multiply occurring recipes
    if( m_recipe_cur->m_pos_bgn == Ustring::npos )
        m_recipe_cur->m_pos_bgn = m_parser_pos_cur;
}

inline void
ParserBase::process_number()
{
    if( m_cf_last & CF_DIGIT )
    {
        m_int_last *= 10;
        m_int_last += ( m_char_cur - '0' );
    }
    else
        m_int_last = ( m_char_cur - '0' );
}
