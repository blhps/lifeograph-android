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


#include "paragraph.hpp"
#include "diary.hpp"
#include "../settings.hpp"
#include "../parsers/parser_stripper.hpp"
#include "../parsers/parser_background.hpp"


using namespace LoG;


// PARAGRAPH =======================================================================================
bool
FuncCmpParagraphs::operator()( const Paragraph* l, const Paragraph* r ) const
{
    return( l->m_host == r->m_host ? l->get_para_no() > r->get_para_no()
                                   : l->m_host->get_date() > r->m_host->get_date() );
}


Paragraph::Paragraph( Entry* host, const Ustring& text, ParserBackGround* parser )
:   DiaryElemTag( host ? host->get_diary() : nullptr ),
    m_host( host ), m_text( text )
{
    if( parser ) parser->parse( this );
}

Paragraph::Paragraph( Paragraph* p, Diary* p2diary )
:   DiaryElemTag( p, p2diary ),
    m_style( p->m_style ), m_host( p->m_host ),
    m_text( p->m_text )
{
    for( auto& format : p->m_formats )
        if( !format->is_on_the_fly() )
            m_formats.insert( new HiddenFormat( *format ) );
}

Paragraph*
Paragraph::get_prev_visible() const
{
    Paragraph* prev = m_p2prev;

    while( prev && !prev->is_visible() )
    {
        prev = prev->m_p2prev;
    }

    return prev;
}
Paragraph*
Paragraph::get_prev_sibling() const
{
    for( auto prev = m_p2prev; prev; prev = prev->m_p2prev )
    {
        if( prev->get_indent_level() < get_indent_level() ||
            prev->get_heading_level() > get_heading_level() ||
            prev->is_empty_completely() )
            break;
        if( prev->get_indent_level() == get_indent_level() &&
            prev->get_heading_level() == get_heading_level() )
            return prev;
    }

    return nullptr;
}
Paragraph*
Paragraph::get_next_sibling() const
{
    for( auto next = m_p2next; next; next = next->m_p2next )
    {
        if( next->get_indent_level() < get_indent_level() ||
            next->get_heading_level() > get_heading_level() ||
            next->is_empty_completely() )
            break;
        if( next->get_indent_level() == get_indent_level() &&
            next->get_heading_level() == get_heading_level() )
            return next;
    }

    return nullptr;
}
DiaryElemTag*
Paragraph::get_sibling_tag_1st()
{
    Paragraph* first { defines_tag() ? this : nullptr };

    for( Paragraph* prev = get_prev_sibling(); prev; prev = prev->get_prev_sibling() )
        if( prev->defines_tag() )
            first = prev;

    return first;
}
DiaryElemTag*
Paragraph::get_sibling_tag_last()
{
    Paragraph* last { defines_tag() ? this : nullptr };

    for( Paragraph* next = get_next_sibling(); next; next = next->get_next_sibling() )
        if( next->defines_tag() )
            last = next;

    return last;
}

ListParagraphs
Paragraph::get_siblings() const
{
    ListParagraphs siblings{ const_cast< Paragraph* >( this ) };
    for( auto prev = get_prev_sibling(); prev; prev = prev->get_prev_sibling() )
        siblings.push_front( prev );
    for( auto next = get_next_sibling(); next; next = next->get_next_sibling() )
        siblings.push_back( next );

    return siblings;
}

int
Paragraph::get_sibling_order() const
{
    int order { 0 };
    for( Paragraph* p = get_prev_sibling(); p; p = p->get_prev_sibling() )
        ++order;
    return order;
}

bool
Paragraph::is_descendant_of( const DiaryElemTag* pp ) const

{
    if( pp->get_type() != ET_PARAGRAPH ) return false;
    const Paragraph* ppp    { dynamic_cast< const Paragraph* >( pp ) };
    const Paragraph* child  { this };
    if( ppp->m_host != m_host || ppp->m_order_in_host >= m_order_in_host ) return false;

    for( const Paragraph* prev = m_p2prev; prev; prev = prev->m_p2prev )
    {
        // entry title cannot be a parent:
        if( prev->is_title() ) break;

        if( prev->can_be_parent_of( child ) )
        {
            if( pp->get_id() == prev->get_id() ) return true;

            // since we have found a parent that is not pp, pp now needs to be a grand parent:
            child = prev;
        }
    }

    return false;
}

VecTags
Paragraph::get_descendant_tags() const
{
    VecTags descendants;

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
        if( can_be_parent_of( p ) )
            descendants.push_back( p );
        else
            break;

    return descendants;
}

Paragraph*
Paragraph::get_nth_next( int n )
{
    Paragraph* p { this };

    for( int i = 0; i < n && p; ++i ) p = p->m_p2next;

    return p;
}

Paragraph*
Paragraph::get_next_visible() const
{
    Paragraph* next = m_p2next;

    while( next && !next->is_visible() )
    {
        next = next->m_p2next;
    }

    return next;
}

Paragraph*
Paragraph::get_parent() const
{
    if( get_heading_level() < VT::PHS::LARGE::I ) // nothing can parent a large heading
    {
        for( Paragraph* prev = m_p2prev; prev; prev = prev->m_p2prev )
        {
            // TODO: 3.2: put this check into can_be_a_parent_of:
            if( prev->is_title() ) // entry title is not a parent
                break;

            if( prev->can_be_parent_of( this ) )
                return prev;
        }
    }

    return nullptr;
}

Paragraph*
Paragraph::get_last() const
{
    Paragraph* p_last { nullptr };
    for( Paragraph* p = m_p2next; p; p = p->m_p2next ) p_last = p;
    return p_last;
}

Paragraph*
Paragraph::get_sub_last() const
{
    Paragraph* p_sub{ const_cast< Paragraph* >( this ) };

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
    {
        if( this->can_be_parent_of( p ) )
            p_sub = p;
        else
            break;
    }

    return p_sub;
}

Paragraph*
Paragraph::get_sub_last_visible( bool F_force_1st_gen ) const
// Caution: returns itself in the absence of any visible sub
{
    Paragraph* p_sub{ const_cast< Paragraph* >( this ) };

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
    {
        if( this->can_be_parent_of( p ) )
        {
            if( p->is_visible() && ( !F_force_1st_gen || p->get_parent() == this ) )
                p_sub = p;
            else
                continue;
        }
        else
            break;
    }

    return p_sub;
}
Paragraph*
Paragraph::get_sub_last_invisible() const
// Caution: returns itself in the absence of any visible sub
{
    Paragraph* p_sub{ const_cast< Paragraph* >( this ) };

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
    {
        if( can_be_parent_of( p ) )
        {
            if( !p->is_visible() )
                p_sub = p;
            else
                continue;
        }
        else
            break;
    }

    return p_sub;
}

bool
Paragraph::is_last_in_host() const
{
    return( m_host ? ( this == m_host->get_paragraph_last() ) : false );
}

int
Paragraph::get_bgn_offset_in_host() const
{
    int offset{ 0 };
    for( Paragraph* para = m_p2prev; para; para = para->m_p2prev )
    {
        if( para->is_visible() )
            offset += para->get_size() + 1; // +1 is for the /n
    }

    return offset;
}
int
Paragraph::get_bgn_offset_in_host_abs() const
{
    int offset{ 0 };
    for( Paragraph* para = m_p2prev; para; para = para->m_p2prev )
        offset += para->get_size() + 1; // +1 is for the /n

    return offset;
}
int
Paragraph::get_end_offset_in_host() const
{
    int offset{ get_size() };
    for( const Paragraph* para = m_p2prev; para; para = para->m_p2prev )
    {
        if( para->is_visible() )
            offset += para->get_size() + 1; // +1 is for the /n
    }

    return offset;
}

void
Paragraph::move_to_entry( Entry* p2new_host, Paragraph* para_before )
{
    if( m_host )
    {
        if( p2new_host != m_host ) // can also be used to move within the same entry
            m_host->get_undo_stack()->clear();
            // NOTE: clearing the undo stack of the source entry is a must as it is not possible...
            // ...to maintain integrity under all possible future undo/redo scenarios without...
            // ...duplicating paragraphs which is both not ideal and has the potential to break...
            // ...linking (if any)

        m_host->remove_paragraphs( this, nullptr, p2new_host == m_host ); // add undo if same entry
    }

    if( !para_before )
        para_before = p2new_host->get_paragraph_last();

    p2new_host->add_paragraphs_after( this, para_before );
}

D::DEIDF
Paragraph::get_id_full() const  // shadows DiaryElement implementation
{ return D::DEIDF( m_id, m_host ? m_host->get_id() : DEID::UNSET ); }

int
Paragraph::get_list_order() const
{
    int         order      { 0 };
    const int   indent_lvl { this->get_indent_level() };
    const int   headng_lvl { this->get_heading_level() };

    for( const Paragraph* para = this; para; para = para->m_p2prev )
    {
        const int indent_lvl_cur{ para->get_indent_level() };
        const int headng_lvl_cur{ para->get_heading_level() };
        if( indent_lvl_cur < indent_lvl || headng_lvl_cur < headng_lvl )
            break;
        else
        if( indent_lvl_cur == indent_lvl && headng_lvl_cur == headng_lvl &&
            para->get_para_type() == get_para_type() )
            ++order;
    }

    return order;
}
String
Paragraph::get_list_order_str( char separator, bool F_list_only ) const
{
    switch( get_list_type() )
    {
        default:
            if( F_list_only )
                return "";
            //else no break:
        case VT::PLS::NUMBER::I:
            return STR::compose( get_list_order(), separator );
        case VT::PLS::CLTTR::I:
            return STR::compose( char( 'A' + ( ( get_list_order() + 25 ) % 26 ) ), separator );
        case VT::PLS::SLTTR::I:
            return STR::compose( char( 'a' + ( ( get_list_order() + 25 ) % 26 ) ), separator );
        case VT::PLS::CROMAN::I:
            return STR::compose( STR::format_number_roman( get_list_order(), false ), separator );
        case VT::PLS::SROMAN::I:
            return STR::compose( STR::format_number_roman( get_list_order(), true ), separator );
    }
}
String
Paragraph::get_list_order_full() const
{
    String order_str{ get_list_order_str( '-', false ) };

    for( const Paragraph* p = get_parent(); p; p = p->get_parent() )
        order_str.insert( 0, p->get_list_order_str( '.', false ) );

    return order_str;
}

int
Paragraph::get_code_line_order() const
{
    const char  lang    { get_quot_type() };
    int         order   { lang == VT::QT::PYTHON::C ? 1 : 0 };
    // NOTE: order normally had to set to 0 but pybind11 somewhat prepends a line before execution,
    // ...hence we offset line numbers by 1

    for( const Paragraph* para = this;
         para &&  para->get_quot_type() == lang;
         para = para->m_p2prev )
    {
        ++order;
    }

    return order;
}

Ustring
Paragraph::get_ancestry_path() const
{
    Ustring path;

    for( Paragraph* pp = get_parent(); pp; pp = pp->get_parent() )
        path.insert( 0, pp->get_name() + "  ▶  " );

    if( m_host )
        path.insert( 0, m_host->get_name() + "  ▶  " );

    return path;
}

Ustring
Paragraph::get_name() const // repurposed to return the tag name:
{
    if( defines_tag() )
        return m_text.substr( 0, m_properties.get( PROP::TAG_END_POS, 0 ) );
    else
    {
        if( m_text.length() > 16 )
            return m_text.substr( 0, 16 ) + "...";
        else
            return m_text;
    }
}

void
Paragraph::set_tag_bound( int pos_end )
{
    if( pos_end < 1 )
        m_properties.remove( PROP::TAG_END_POS );
    else
    {
        m_properties.set( PROP::TAG_END_POS, pos_end );
        m_properties.set( PROP::TAG_END_POS_CHANGED, true );
        if( m_p2diary )
            m_p2diary->add_tag_to_cache( this, pos_end > 0 );
    }

    update_date_edited();
}
void
Paragraph::offset_tag_bound( int offset )
{
    const int pos { m_properties.get( PROP::TAG_END_POS, 0 ) + offset };

    if( pos > 0 )
        m_properties.set( PROP::TAG_END_POS, pos );
    else
        m_properties.remove( PROP::TAG_END_POS );

    m_properties.set( PROP::TAG_END_POS_CHANGED, true );

    update_date_edited();
}

int
Paragraph::get_chain_char_count() const
{
    auto length { m_text.length() };

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
        length += ( p->m_text.length() + 1 ); // +1 for the \n

    return length;
}
int
Paragraph::get_chain_para_count() const
{
    auto count { 1 };

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
        ++count;

    return count;
}

// TEXT
String
Paragraph::get_text_code() const
{
    String str { m_text };
    int offset { 0 };

    for( auto format : m_formats )
        if( format->type == VT::HFT_TAG )
        {
            DiaryElemTag* tag { Diary::d->get_tag_by_id( format->get_id_lo() ) };
            const String  repl_str
            { STR::compose( tag->is_entry() ? "Diary.get_main().get_tag_by_id("
                                            : "Diary.get_main().get_paragraph_by_id(",
                            format->ref_id, ")" ) };
            const auto    orig_size { format->pos_end - format->pos_bgn + 1 };
            str.replace( format->pos_bgn + offset, orig_size + offset, repl_str );
            offset += ( repl_str.size() - orig_size );
        }

    return str;
}
Ustring
Paragraph::get_text_stripped( int flags ) const
{
    ParserStripper parser;
    Ustring        str;

    if( flags & VT::TCT_CMPNT_INDENT )
        for( int i = 0; i < get_indent_level(); ++i ) str += "    ";

    str += parser.parse( this, flags );

    return str;
}

Ustring
Paragraph::get_text_decorated() const
{
    Ustring str;

    for( int i = get_indent_level(); i > 0; --i ) str += '\t';

    switch( get_list_type() )
    {
        case VT::PLS::BULLET::I:   str += "* "; break;
        case VT::PLS::NUMBER::I:
        case VT::PLS::CLTTR::I:
        case VT::PLS::SLTTR::I:
        case VT::PLS::CROMAN::I:
        case VT::PLS::SROMAN::I:   str += ( get_list_order_str() + ' ' ); break;
        case VT::PLS::TODO::I:
        case VT::PLS::PROGRS::I:
        case VT::PLS::DONE::I:
        case VT::PLS::CANCLD::I:   str += get_todo_status_as_text(); break;
    }

    str += m_text;

    return str;
}

Paragraph*
Paragraph::get_sub( UstringSize bgn, UstringSize end ) const
{
    // disable addition of the sub to diary:
    if( m_host->get_diary() ) m_host->get_diary()->set_force_id_allow_duplicate( DEID::OMIT );
    Paragraph* para_sub { new Paragraph( m_host, m_text.substr( bgn, end - bgn ) ) };
    para_sub->m_style = m_style;

    for( auto format : m_formats )
    {
        // format at least intersects with the range:
        if( format->pos_bgn < end && format->pos_end > bgn )
        {
            auto format_new{ new HiddenFormat( *format ) };
            format_new->pos_bgn = format->pos_bgn > bgn ? format->pos_bgn - bgn : 0ul;
            format_new->pos_end = format->pos_end > end ? end - bgn : format->pos_end - bgn;
            para_sub->insert_format( format_new );
        }
    }

    return para_sub;
}

void
Paragraph::set_text( const Ustring& text, ParserBackGround* parser, bool F_add_undo )
{
    if( F_add_undo && m_host )
        m_host->add_undo_action( UndoableType::MODIFY_TEXT, this, 1,
                                 m_text.length(),
                                 text.length() );

    m_text = text;
    if( parser ) parser->parse( this );

    update_date_edited();
}

void
Paragraph::append( Paragraph* para, ParserBackGround* parser )
{
    const auto size_prev { m_text.size() };

    m_text += para->m_text;
    for( auto format : para->m_formats )
        add_format( format, size_prev );

    if( parser ) parser->parse( this );

    update_date_edited();
}
void
Paragraph::append( const Ustring& text, ParserBackGround* parser )
{
    m_text += text;
    if( parser ) parser->parse( this );

    update_date_edited();
}
HiddenFormat*
Paragraph::append( const Ustring& text, int type, const String& uri )
{
    const auto pos = m_text.size();
    m_text += text;
    update_date_edited();

    return add_format( type, uri, pos, pos + text.length() );
}

void
Paragraph::insert_text( UstringSize pos, Paragraph* para, ParserBackGround* parser )
{
    insert_text( pos, para->m_text, nullptr );

    for( auto format : para->m_formats )
        add_format( format, pos );

    if( parser ) parser->parse( this );

    update_date_edited();
}
void
Paragraph::insert_text( UstringSize pos, const Ustring& text, ParserBackGround* parser,
                        bool F_add_undo )
{
    const auto length { text.length() };

    if( F_add_undo && m_host )
        m_host->add_undo_action( UndoableType::INSERT_TEXT, this, 1, pos, pos + length );

    m_text.insert( pos, text );
    update_formats( pos, 0, length );

    if( pos < get_tag_bound() )
        offset_tag_bound( length );

    if( parser ) parser->parse( this );

    update_date_edited();
}
std::tuple< UstringSize, UstringSize, UstringSize >
Paragraph::insert_text_with_spaces( UstringSize pos, Ustring text, ParserBackGround* parser,
                                    bool F_space_bgn_punct, bool F_space_end_punct )
{
    auto        c         { Wchar( m_text[ pos ] ) };
    auto        ct        { Wchar( text[ text.size() - 1 ] ) };
    UstringSize space_bgn { 0 };
    UstringSize space_end { 0 };

    if( !STR::is_char_space( ct ) && ( F_space_end_punct ? !STR::is_char_space( c )
                                                         : Glib::Unicode::isalnum( c ) ) )
    {
        text += " ";
        space_end = 1;
    }

    if( pos > 0 )
    {
        c = m_text[ pos - 1 ];
        ct = text[ 0 ];
        if( !STR::is_char_space( ct ) && ( F_space_bgn_punct ? !STR::is_char_space( c )
                                                             : Glib::Unicode::isalnum( c ) ) )
        {
            text.insert( 0, " " );
            space_bgn = 1;
        }
    }

    insert_text( pos, text, parser );
    return std::make_tuple( space_bgn, space_end, text.length() );
}
void
Paragraph::erase_text( UstringSize pos, UstringSize length, ParserBackGround* parser,
                       bool F_add_undo )
{
    if( F_add_undo && m_host )
        m_host->add_undo_action( UndoableType::ERASE_TEXT, this, 1, pos + length, pos );

    m_text.erase( pos, length );
    update_formats( pos, length, 0 );

    const auto tag_boundary { get_tag_bound() };
    if( pos < tag_boundary )
    {
        if( pos + length > tag_boundary )
            offset_tag_bound( pos - tag_boundary  );
        else
            offset_tag_bound( -length );
    }

    if( parser ) parser->parse( this );

    update_date_edited();
}
void
Paragraph::replace_text( UstringSize pos, UstringSize size, const Ustring& text,
                         ParserBackGround* parser, bool F_add_undo )
{
    if( F_add_undo && m_host )
        m_host->add_undo_action( UndoableType::MODIFY_TEXT, this, 1,
                                    pos + size,
                                    pos + text.length() );

    m_text.erase( pos, size );
    m_text.insert( pos, text );
    update_formats( pos, size, text.size() ); // erase + insert

    const auto tag_boundary { get_tag_bound() };
    if( pos + size <= tag_boundary ) // contained case
        offset_tag_bound( text.size() - size );
    else if( pos < tag_boundary ) // intersection case
        set_tag_bound( pos );

    if( parser ) parser->parse( this );

    if( m_host && is_title() ) m_host->update_name();

    update_date_edited();
}

void
Paragraph::change_letter_cases( int pb, int pe, LetterCase lc )
{
    if( pb != 0 )
    {
        if( lc == LetterCase::CASE_SENTENCE )
            pb = STR::find_sentence_start_backwards( m_text, pb );
        else
        if( lc == LetterCase::CASE_TITLE )
            pb = STR::find_word_start_backwards( m_text, pb );
    }

    const auto  size_old  { ( pe == -1 ? m_text.length() : pe ) - pb };
    Ustring     substring { m_text.substr( pb, size_old ) };

    switch( lc )
    {
        case LetterCase::CASE_SENTENCE: substring = STR::sentencecase( substring );  break;
        case LetterCase::CASE_TITLE:    substring = STR::titlecase( substring );     break;
        case LetterCase::CASE_LOWER:    substring = STR::lowercase( substring );     break;
        case LetterCase::CASE_UPPER:    substring = substring.uppercase();           break;
    }

    m_text.erase( pb, size_old );
    m_text.insert( pb, substring );
    update_formats( pb, size_old, substring.size() ); // erase + insert

    update_date_edited();
}

void
Paragraph::inherit_style_from( const Paragraph* para, ParaInhClasses ic )
{
    const auto quot_type { para->get_quot_type() };

    if( ic.contains( ParaInhClass::LIST_TYPE ) )
    {
        if( para->m_style & VT::PS_TODO_GEN )
            set_list_type( VT::PLS::TODO::I );
        else if( para->m_style & VT::PS_LIST_GEN )
            set_list_type( para->get_list_type() );
    }

    if( ic.contains( ParaInhClass::QUOT_TYPE ) )
    {
        if( quot_type != VT::QT::OFF::C )
            set_quot_type( quot_type );
    }

    if( ic.contains( ParaInhClass::INDENTATION ) )
    {
        if( para->is_code() )
        {
            if( quot_type == VT::QT::PYTHON::C && STR::ends_with_trimmed( para->m_text, ':' ) )
                set_space_indent( para->get_space_indent() + INDENT_SPACE_COUNT );
            else
                set_space_indent( para->get_space_indent() );
        }
        else
            set_indent_level( para->get_indent_level() );
    }

    if( ic.contains( ParaInhClass::HEADING_LVL ) )
    {
        set_hrule( para->has_hrule() );
        set_heading_level( para->get_heading_level() );
    }
}

void
Paragraph::set_expanded( bool F_expanded )
{
    DiaryElemTag::set_expanded( F_expanded );

    for( Paragraph* p = m_p2next; p; p = p->m_p2next )
    {
        if( can_be_parent_of( p ) )
            p->reset_visibility();
        else
            break;
    }
}

bool
Paragraph::is_visible_recalculate() const
{
    Paragraph* parent = get_parent();

    if( !parent )
        return true;

    if( parent->is_expanded() )
        return parent->is_visible();
    else
        return false;
}

void
Paragraph::make_accessible()
{
    Paragraph* parent = get_parent();

    set_visible( true );

    if( !parent )
        return;

    if( !parent->is_visible() )
        parent->set_visible( true );

    parent->make_accessible();
}

bool
Paragraph::has_hidden_comment() const
{
    if( is_expanded() ) return false;

    for( Paragraph* p = m_p2next; p && p->is_descendant_of( this ); p = p->m_p2next )
        for( auto f : p->m_formats )
            if( f->type == VT::HFT_COMMENT ) return true;

    return false;
}
bool
Paragraph::has_comment_to_bgn() const
{
    for( const Paragraph* p = this; p; p = p->m_p2prev )
        for( auto f : p->m_formats )
            if( f->type == VT::HFT_COMMENT ) return true;

    return false;
}
bool
Paragraph::has_comment_to_end() const
{
    for( const Paragraph* p = this; p; p = p->m_p2next )
    {
        // PRINT_DEBUG( "if para has tag=", p->get_text() );
        for( auto f : p->m_formats )
        if( f->type == VT::HFT_COMMENT ) return true;
    }

    return false;
}

void
Paragraph::set_tag( const D::DEID& id, Value value ) // NOTE: does not update edit date
{
    m_tags[ id ] = value;
    m_tags_in_order.push_back( id );
}
void
Paragraph::set_tag( const D::DEID& id, Value v_real, Value v_planned )
// NOTE: does not update edit date
{
    m_tags_planned[ id ] = v_planned;
    set_tag( id, v_real );
}

bool
Paragraph::has_tag( const DiaryElemTag* tag ) const
{
    return( m_tags.find( tag->get_id() ) != m_tags.end() );
}

bool
Paragraph::has_tag_planned( const DiaryElemTag* tag ) const
{
    return( m_tags_planned.find( tag->get_id() ) != m_tags_planned.end() );
}

bool
Paragraph::has_tag_broad( const DiaryElemTag* tag, bool F_consider_parents ) const
{
    if( has_tag( tag ) ) return true;

    VecTags                       descendant_tags { tag->get_descendant_tags() };
    std::list< const Paragraph* > paragraphs;

    if( F_consider_parents )
        for( const Paragraph* para = this; para; para = para->get_parent() )
            paragraphs.push_back( para );
    else
        paragraphs.push_back( this );

    for( DiaryElemTag* t : descendant_tags )
        for( const Paragraph* para : paragraphs )
            if( para->has_tag( t ) ) return true;

    return false;
}

Value
Paragraph::get_tag_value( const DiaryElemTag* tag, int& count ) const
{
    if( !tag || !has_tag( tag ) )
        return 0.0;
    else
    {
        count++;
        return m_tags.get_value_for_tag( tag->get_id() );
    }
}

Value
Paragraph::get_tag_value_planned( const DiaryElemTag* tag, int& count ) const
{
    if( !tag || !has_tag( tag ) )
        return 0.0;
    else if( !has_tag_planned( tag ) )
        // return realized value as planned when planned value is omitted
        return m_tags.get_value_for_tag( tag->get_id() );
    else
    {
        count++;
        return m_tags_planned.get_value_for_tag( tag->get_id() );
    }
}

Value
Paragraph::get_tag_value_remaining( const DiaryElemTag* tag, int& count ) const
{
    if( !tag || !has_tag_planned( tag ) )
        return 0.0;
    else
    {
        count++;
        return( m_tags_planned.get_value_for_tag( tag->get_id() ) -
                m_tags.get_value_for_tag( tag->get_id() ) );
    }
}

// double
// Paragraph::get_tag_value_completion( const DiaryElemTag* tag ) const
// {
//     if( !m_host || !tag ) return 0.0;

//     int c{ 0 }; // dummy
//     double vp{ get_tag_value_planned( tag, c ) };

//     if( vp == 0.0 )
//         return 0.0;

//     return( get_tag_value( tag, c ) / vp );
// }

DiaryElemTag*
Paragraph::get_subtag_first( const DiaryElemTag* parent_tag ) const
{
    for( auto& tag_id : m_tags_in_order )
    {
        auto tag { m_p2diary ? m_p2diary->get_tag_by_id( tag_id ) : nullptr };

        if( tag && tag->is_descendant_of( parent_tag ) )
            return tag;
    }

    Paragraph* parent_para { get_parent() };
    return( parent_para ? parent_para->get_subtag_first( parent_tag ) : nullptr );
}

DiaryElemTag*
Paragraph::get_subtag_last( const DiaryElemTag* parent_tag ) const
{
    for( auto iter = m_tags_in_order.rbegin(); iter != m_tags_in_order.rend(); iter++ )
    {
        auto tag { m_p2diary ? m_p2diary->get_tag_by_id( *iter ) : nullptr };

        if( tag && tag->is_descendant_of( parent_tag ) )
            return tag;
    }

    Paragraph* parent_para { get_parent() };
    return( parent_para ? parent_para->get_subtag_last( parent_tag ) : nullptr );

    return nullptr;
}

DiaryElemTag*
Paragraph::get_subtag_lowest( const DiaryElemTag* parent_tag ) const
{
    DiaryElemTag* subtag_lowest { nullptr };

    if( parent_tag && m_p2diary )
    {
        for( auto& tag_id : m_tags_in_order )
        {
            auto sub_tag { m_p2diary->get_tag_by_id( tag_id ) };
            if( sub_tag && sub_tag->get_parent_tag() == parent_tag )
            {
                if( subtag_lowest )
                {
                    if( sub_tag->get_sibling_order() < subtag_lowest->get_sibling_order() )
                        subtag_lowest = sub_tag;
                }
                else
                    subtag_lowest = sub_tag;
            }
        }
    }

    if( !subtag_lowest )
    {
        Paragraph* parent_para { get_parent() };
        return( parent_para ? parent_para->get_subtag_lowest( parent_tag ) : nullptr );
    }
    else
        return subtag_lowest;
}

DiaryElemTag*
Paragraph::get_subtag_highest( const DiaryElemTag* parent_tag ) const
{
    DiaryElemTag* subtag_highest { nullptr };

    if( parent_tag && m_p2diary )
    {
        for( auto& tag_id : m_tags_in_order )
        {
            auto sub_tag { m_p2diary->get_tag_by_id( tag_id ) };
            if( sub_tag && sub_tag->is_descendant_of( parent_tag ) )
            {
                if( subtag_highest )
                {
                    if( sub_tag->get_sibling_order() > subtag_highest->get_sibling_order() )
                        subtag_highest = sub_tag;
                }
                else
                    subtag_highest = sub_tag;
            }
        }
    }

    if( !subtag_highest )
    {
        Paragraph* parent_para { get_parent() };
        return( parent_para ? parent_para->get_subtag_highest( parent_tag ) : nullptr );
    }
    else
        return subtag_highest;
}

ListTags
Paragraph::get_sub_tags( const DiaryElemTag* parent ) const
{
    ListTags sub_tags;

    if( parent && m_p2diary )
    {
        for( auto& tag_id : m_tags_in_order )
        {
            auto sub_tag { m_p2diary->get_tag_by_id( tag_id ) };
            if( sub_tag && sub_tag->is_descendant_of( parent ) )
                sub_tags.push_back( sub_tag );
        }
    }

    return sub_tags;
}

double
Paragraph::get_completion() const
{
    if( !m_host ) return 0.0;

    const double wl     { get_workload() };
    const double cmpltd { get_completed() };

    if( wl == 0.0 )
        return( cmpltd > 0.0 ? 1.0 : 0.0 );
    else
        return( cmpltd / wl );
}

double
Paragraph::get_completed() const
{
    if( m_host == nullptr ) return 0.0;

    int c{ 0 }; // dummy
    DiaryElemTag* tag_comp{ m_p2diary ? m_p2diary->get_completion_tag() : nullptr };

    if( tag_comp == nullptr )
        return 0.0;

    return get_tag_value( tag_comp, c );
}

double
Paragraph::get_workload() const
{
    if( m_host == nullptr ) return 0.0;

    int c{ 0 }; // dummy
    DiaryElemTag* tag_comp{ m_p2diary ? m_p2diary->get_completion_tag() : nullptr };

    if( tag_comp == nullptr )
        return 0.0;

    return get_tag_value_planned( tag_comp, c );
}

DateV
Paragraph::get_date_broad( bool F_explicit ) const
{
    if( m_date != Date::NOT_SET )
        return m_date;

    Paragraph* pp{ get_parent() };
    if( pp )
    {
        const auto d { pp->get_date_broad( true ) };
        if( Date::is_set( d ) )
            return d;
    }

    if( !F_explicit && m_host )
        return m_host->get_date();

    return Date::NOT_SET;
}
DateV
Paragraph::get_date_finish_broad( bool F_explicit ) const
{
    if( Date::is_set( m_date_finish ) )
        return m_date_finish;

    Paragraph* pp{ get_parent() };
    if( pp )
    {
        const auto d { pp->get_date_finish_broad( true ) };
        if( Date::is_set( d ) )
            return d;
    }

    if( !F_explicit )
    {
        if( Date::is_set( m_date ) )
            return m_date;

        if( m_host )
            return m_host->get_date_finish();
    }

    return Date::NOT_SET;
}

void
Paragraph::set_list_type( int type )
{
    if( ( m_style & VT::PS_TODO_GEN ) && ( type & VT::PS_TODO_GEN ) &&
        m_p2diary && m_p2diary->has_completion_tag() &&
        has_tag( m_p2diary->get_completion_tag() ) )
    {
        const auto v{ get_completion() };
        if( type == VT::PLS::CANCLD::I ||
            ( v == 0.0 && type != VT::PLS::TODO::I ) ||
            ( v == 1.0 && type != VT::PLS::DONE::I ) ||
            ( is_value_in_range_excl( v, 0.0,  1.0 ) && type != VT::PLS::PROGRS::I ) )
            return;
    }

    m_style = ( ( m_style & ~( VT::PLS::FILTER ) ) | ( type & VT::PLS::FILTER ) );
}

void
Paragraph::set_para_type2( int type )
{
    if( type & VT::PS_LIST_GEN )
    {
        if( type == VT::PS_LIST_GEN )
            clear_list_type();
        else
        {
            const bool was_list { is_list() };
            set_list_type( type );
            if( !was_list && get_indent_level() == 0 &&
                get_heading_level() != VT::PHS::LARGE::I && !is_code() )
                // there are many reasons not to indent
            {
                set_indent_level( 1 );
                //set_visible( true );
            }
        }
    }

    if( type & VT::PS_HEADER_GEN )
    {
        if( type == VT::PS_HEADER_GEN )
            clear_heading_level();
        else
            set_heading_level( type );
    }

    update_date_edited();
}

bool
Paragraph::set_indent_level( unsigned lvl )
{
    if( lvl > VT::PS_INDENT_MAX )   lvl = VT::PS_INDENT_MAX;

    m_style = ( ( m_style & ~( VT::PS_FLT_INDENT ) ) |
                ( ( lvl << 24 ) & VT::PS_FLT_INDENT ) );

    update_date_edited();

    return true;
}

void
update_para_style_after_after_indentation_change( Paragraph* p )
{
    if( ( p->m_style & VT::PS_LIST_GEN ) && !( p->m_style & VT::PS_TODO_GEN ) )
    {
        Paragraph* ps { p->get_prev_sibling() };
        Paragraph* ns { p->get_next_sibling() };

        if( ps && ( ps->m_style & VT::PS_LIST_GEN ) )
        {
            if( ps->m_style & VT::PS_TODO_GEN )
                p->set_list_type( VT::PLS::TODO::I );
            else
                p->set_list_type( ps->get_list_type() );
        }
        else if( ns && ( ns->m_style & VT::PS_LIST_GEN ) )
        {
            if( ns->m_style & VT::PS_TODO_GEN )
                p->set_list_type( VT::PLS::TODO::I );
            else
                p->set_list_type( ns->get_list_type() );
        }
    }
}
bool
Paragraph::indent()
{
    if( is_code() )
    {
        const auto cur_level { get_space_indent() };
        set_space_indent( cur_level + INDENT_SPACE_COUNT - ( cur_level % INDENT_SPACE_COUNT )  );
        return true;
    }
    else if( set_indent_level( get_indent_level() + 1 ) )
    {
        update_para_style_after_after_indentation_change( this );
        return true;
    }
    return false;
}
bool
Paragraph::unindent()
{
    const auto cur_level { is_code() ? get_space_indent() : get_indent_level() };
    if( cur_level == 0 ) return false;

    if( is_code() )
    {
        const auto misalignment { cur_level % INDENT_SPACE_COUNT };
        set_space_indent( cur_level - ( misalignment ? misalignment : INDENT_SPACE_COUNT ) );
        return true;
    }
    else if( set_indent_level( cur_level - 1 ) )
    {
        update_para_style_after_after_indentation_change( this );
        return true;
    }
    return false;
}

unsigned
Paragraph::get_space_indent() const
{
    unsigned depth { 0 };

    while( depth < m_text.length() && m_text[ depth ] == ' ' )
        depth++;

    return depth;
}
void
Paragraph::set_space_indent( const unsigned depth )
{
    const int difference { int( depth ) - int( get_space_indent() ) };

    if( difference > 0 )
        for( int i = 0; i < difference; ++i )
            m_text.insert( 0, " " );
    else if( difference < 0 )
        m_text.erase( 0, -difference );

    for( auto f : m_formats )
    {
        f->pos_bgn += difference;
        f->pos_end += difference;
    }

    update_date_edited();
}

void
Paragraph::convert_indentation_type( char new_quot_t )
{
    // convert indenetation type based on the quot type:
    if( is_code() && ( new_quot_t == VT::QT::OFF::C || new_quot_t == VT::QT::LITERARY::C ) )
    {
        set_indent_level( get_space_indent() / INDENT_SPACE_COUNT );
        set_space_indent( 0 );
    }
    else if( !is_code() && new_quot_t != VT::QT::OFF::C && new_quot_t != VT::QT::LITERARY::C )
    {
        set_space_indent( get_indent_level() * INDENT_SPACE_COUNT );
        set_indent_level( 0 );
    }
}

void
Paragraph::set_quot_type( char quot_type )
{
    if( quot_type == VT::QT::OFF::C )
        m_properties.remove( PROP::QUOT_TYPE );
    else
        m_properties.set( PROP::QUOT_TYPE, quot_type );

    update_date_edited();
}

void
Paragraph::get_code_block( Paragraph*& p_bgn, Paragraph*& p_end )
{
    const int lang_cur { get_quot_type() };

    // expands the value to all adjacent paragraphs:
    for( Paragraph* p = this; true; p = p->m_p2prev )
    {
        p_bgn = p;

        if( !p->m_p2prev || !p->m_p2prev->is_code() || p->m_p2prev->get_quot_type() != lang_cur )
            break;
    }
    for( Paragraph* p = this; true; p = p->m_p2next )
    {
        p_end = p;

        if( !p->m_p2next || !p->m_p2next->is_code() || p->m_p2next->get_quot_type() != lang_cur )
            break;
    }
}

int
Paragraph::get_code_line_comment_bgn() const
{
    if( !is_code() ) return false;

    const auto regex_comment
    { Glib::Regex::create(
            STR::compose( "^\\s*(", VT::get_code_comment_token( get_quot_type() ), ")" ) ) };
    Glib::MatchInfo match_info;

    if( regex_comment->match( m_text.c_str(), match_info ) )
    {
        int pos_bgn, pos_end;
        match_info.fetch_pos( 1, pos_bgn, pos_end );
        return pos_bgn;
    }
    else
        return -1;
}
void
Paragraph::set_code_line_commented( bool F_set )
{
    if( !is_code() ) return;

    const int comment_pos { get_code_line_comment_bgn() };
    if( F_set == ( comment_pos >= 0 ) ) return;
    const String token { VT::get_code_comment_token( get_quot_type() ) };
    if( F_set )
    {
        const auto      regex_first_nonsp { Glib::Regex::create( "^\\s*(\\S)" ) };
        Glib::MatchInfo match_info;
        int             pos_bgn { 0 }, pos_end;

        if( regex_first_nonsp->match( m_text.c_str(), match_info ) )
            match_info.fetch_pos( 1, pos_bgn, pos_end );
        insert_text( pos_bgn, token, nullptr );
    }
    else
        erase_text( comment_pos, token.length() );
}

void
Paragraph::update_formats( StringSize pos, int s_del, int s_ins )
{
    const int        delta      { s_ins - s_del };
    const StringSize pos_end_d  { pos + s_del };
    const StringSize pos_end_i  { pos + s_ins };

    if( delta > 0 ) // insertion
    {
        for( auto format : m_formats )
        {
            if( pos_end_d <= format->pos_bgn ) format->pos_bgn += delta;
            if( pos_end_d < format->pos_end ||
                ( pos_end_d == format->pos_end && s_del > 0 ) ) format->pos_end += delta;
            // "pos_end_d == format->pos_end" makes the last format bleed into newly entered text
            // it is used on replace operations but not on inserts
        }
    }
    else
    if( delta < 0 ) // erasure
    {
        if( s_ins == 0 ) // only do this on erase (but not on replace)
            FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* format )
            {
                if( pos <= format->pos_bgn && pos_end_d >= format->pos_end )
                {
                    delete format;
                    return true;
                }
                return false;
            } );

        for( auto format : m_formats )
        {
            if( pos < format->pos_end && pos_end_i < format->pos_end )
            {
                if( pos_end_d >= format->pos_end ) format->pos_end = pos_end_i;
                else                               format->pos_end += delta;
               // if( pos < format->pos_end )
            }

            if( pos < format->pos_bgn && pos_end_i < format->pos_bgn )
            {
                if( pos_end_d <= format->pos_bgn ) format->pos_bgn += delta;
                else                               format->pos_bgn = pos_end_i;
            }
        }
    }
}

// IMAGE
R2Pixbuf
Paragraph::get_image( int max_w, const Pango::FontDescription& fd ) noexcept
{
    try
    {
        if( !m_p2diary ) throw LoG::Error( "get_image() called on an orphan paragraph" );

        const auto uri  { get_uri() };
        const auto size { get_image_size() };

        switch( m_style & VT::PS_FLT_IMAGE )
        {
            case VT::PS_IMAGE_FILE:
                return m_p2diary->get_image_file( uri,
                                                  max_w * ( size + 1 ) * 0.25 );
            case VT::PS_IMAGE_CHART:
                return m_p2diary->get_image_chart( uri,
                                                   max_w * ( size + 1 ) * 0.25,
                                                   fd );
            case VT::PS_IMAGE_TABLE:
                return m_p2diary->get_image_table( uri,
                                                   max_w,
                                                   fd,
                                                   m_style & VT::PS_IMAGE_EXPND );
            default:
                throw LoG::Error( "get_image() called on a non-image paragraph" );
        }
    }
    catch( Glib::FileError& er )  { print_error( "Link target not found" ); }
    catch( LoG::Error& er )       { } // already prints the problem during throw
    catch( ... )                  { }

    return R2Pixbuf{};
}

String
Paragraph::get_lang_final() const
{
    const auto lang { m_properties.get< String >( PROP::LANGUAGE, "!" ) };
    if( lang == get_sstr( CSTR::OFF ) )
        return "";
    else if( lang == "!" ) // inherit
    {
        if( m_host )
            return m_host->get_lang_final();
        else if( m_p2diary )
            return m_p2diary->get_lang();
        else
            return "";
    }
    else
        return lang;
}

// FORMATS
void
Paragraph::add_ref_to_tag( const D::DEID& id )
{
    if( m_p2diary )
    {
        auto tag { m_p2diary->get_tag_by_id( id ) };

        if( tag )
            tag->add_referring_elem( this );
    }
}

void
Paragraph::remove_ref_from_tag( const D::DEID& id )
{
    if( m_p2diary )
    {
        auto tag { m_p2diary->get_tag_by_id( id ) };

        if( tag )
            tag->remove_referring_elem( this );
    }
}
void
Paragraph::add_or_remove_ref_from_tags( bool F_add )
{
    if( m_p2diary )
    {
        for( auto& f : m_formats )
        {
            if( f->type & VT::HFT_F_REFERENCE )
            {
                auto tag { m_p2diary->get_tag_by_id( f->get_id_lo() ) };
                if( tag )
                {
                    if( F_add )
                        tag->add_referring_elem( this );
                    else
                        tag->remove_referring_elem( this );
                }
            }
        }
    }
}

void
Paragraph::clear_formats() // NOTE: does not update edit date
{
    for( auto& format : m_formats )
        delete format;

    m_formats.clear();
    clear_tags();
}

HiddenFormat*
Paragraph::add_format( int type, const String& uri, UstringSize pos_bgn, UstringSize pos_end )
{
    UstringSize p_bgn_final{ pos_bgn }, p_end_final{ pos_end };

    if( type != VT::HFT_MATCH )
    {
        FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* format )
        {
            if( format->type == type &&
                format->pos_bgn <= pos_end && format->pos_end >= ( pos_bgn ) )
            {
                if( format->pos_bgn < p_bgn_final ) p_bgn_final = format->pos_bgn;
                if( format->pos_end > p_end_final ) p_end_final = format->pos_end;
                delete format;
                return true;
            }
            return false;

        } );
    }

    auto format_new{ new HiddenFormat( type, uri, p_bgn_final, p_end_final ) };
    insert_format( format_new );

    return format_new;
}

HiddenFormat*
Paragraph::add_format( HiddenFormat* format, int offset )
{
    auto format_new { new HiddenFormat( *format ) };
    format_new->pos_bgn += offset;
    format_new->pos_end += offset;

    if( format->type != VT::HFT_MATCH )
    {
        FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* f )
        {
            if( f->type == format_new->type &&
                f->pos_bgn <= format_new->pos_end &&
                f->pos_end >= ( format_new->pos_bgn ) )
            {
                if( f->pos_bgn < format_new->pos_bgn ) format_new->pos_bgn = f->pos_bgn;
                if( f->pos_end > format_new->pos_end ) format_new->pos_end = f->pos_end;
                delete f;
                return true;
            }
            return false;
        } );
    }

    insert_format( format_new );

    return format_new;
}

void
Paragraph::insert_format( HiddenFormat* f )
{
    for( auto&& it = m_formats.begin(); it != m_formats.end(); ++it )
    {
        if( ( *it )->pos_bgn > f->pos_bgn )
        {
            m_formats.insert( it, f );
            return;
        }
    }
    m_formats.insert( f );

    // set the ref_id for eval links:
    if( f->type == VT::HFT_LINK_EVAL )
        f->set_id_lo( m_id );

    if( !f->is_on_the_fly() )
        update_date_edited();
}

HiddenFormat*
Paragraph::add_format_tag( const DiaryElemTag* tag, UstringSize pos_bgn )
{
    auto&& format{ add_format( VT::HFT_TAG, "", pos_bgn, pos_bgn + tag->get_name().length() ) };
    format->set_id_lo( tag->get_id() );
    return format;
}

void
Paragraph::remove_format( int type, UstringSize pos_bgn, UstringSize pos_end )
// works both for individual formats & format classes per flags
{
    FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* format )
    {
        if( ( format->type == type ||
              // flag mode:
              ( !( type & VT::HFT_FILTER_CHARS ) && ( format->type & type ) ) ) &&
            format->pos_bgn < pos_end && format->pos_end > pos_bgn )
        {
            // if format extends in both directions, split:
            if( format->pos_bgn < pos_bgn && format->pos_end > pos_end )
            {
                insert_format( new HiddenFormat( type, format->uri, pos_end, format->pos_end ) );
                format->pos_end = pos_bgn;
            }
            // if rormat extends in one direction, trim:
            else if( format->pos_bgn < pos_bgn ) format->pos_end = pos_bgn;
            else if( format->pos_end > pos_end ) format->pos_bgn = pos_end;
            // if contained within the erase region, delete:
            else
            {
                if( !( type & VT::HFT_F_ONTHEFLY ) )
                    update_date_edited();

                delete format;
                return true;
            }
        }
        return false;
    } );
}

void
Paragraph::remove_format( const HiddenFormat* format )
{
    FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* f )
    {
        if( f->type == format->type && f->pos_bgn == format->pos_bgn )
        {
            if( !f->is_on_the_fly() )
                update_date_edited();
            delete f;
            return true;
        }
        return false;
    } );
}

void
Paragraph::remove_onthefly_formats()
{
    FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* f )
    {
        if( f->is_on_the_fly() )
        {
            delete f;
            return true;
        }
        return false;
    } );
}

void
Paragraph::remove_formats_of_type( int t )
{
    FormattedText::erase_if( m_formats, [ & ]( HiddenFormat* format )
    {
        if( format->type == t )
        {
            delete format;
            return true;
        }
        return false;
    } );

    if( !( t & VT::HFT_F_ONTHEFLY ) )
        update_date_edited();
}

void
Paragraph::toggle_format( int type, UstringSize pos_bgn, UstringSize pos_end, bool F_already )
{
    if( type & VT::HFT_F_V_POS ) // remove both sub and sup as they cannot coexist:
        remove_format( VT::HFT_F_V_POS, pos_bgn, pos_end );
    if( F_already )
        remove_format( type, pos_bgn, pos_end );
    else
        add_format( type, "", pos_bgn, pos_end );
}

HiddenFormat*
Paragraph::get_format_at( int type, UstringSize pos_bgn, UstringSize pos_end ) const
{
    for( auto& format : m_formats )
    {
        if( format->type == type && format->pos_bgn < pos_end && format->pos_end >= pos_bgn )
            return format;
    }
    return nullptr;
}

HiddenFormat*
Paragraph::get_format_oneof_at( const std::vector< int >& types,
                                UstringSize pos_bgn, UstringSize pos_end ) const
{
    for( auto& type : types )
    {
        for( auto format : m_formats )
            if( format->type == type && format->pos_bgn < pos_end && format->pos_end > pos_bgn )
                return format;
    }
    return nullptr;
}
HiddenFormat*
Paragraph::get_format_oneof_at( int flags, UstringSize pos ) const
{
    for( auto& format : m_formats )
    {
        if( ( format->type & flags ) && format->pos_bgn <= pos && format->pos_end >= pos )
            return format;
    }
    return nullptr;
}

int
Paragraph::predict_list_style_from_text()
{
    int ps{ VT::PS_PLAIN };
    bool F_has_mark{ false };
    for( UstringSize i = 0; i < m_text.length(); i++ )
    {
        const auto ch { Wchar( m_text[ i ] ) };
        if( i == 0 && Glib::Unicode::isalpha( ch ) )
            ps = ( Glib::Unicode::isupper( ch ) ? VT::PLS::CLTTR::I : VT::PLS::SLTTR::I );
        else
        if( ( i == 0 || ps == VT::PLS::NUMBER::I ) && !F_has_mark && Glib::Unicode::isdigit( ch ) )
            ps = VT::PLS::NUMBER::I;
        else
        if( i == 0 && ( ch == L'•' || ch == '*' || ch == '-' ) )
        {
            ps = VT::PLS::BULLET::I;
            F_has_mark = true;
        }
        else
        if( !F_has_mark && ps != VT::PS_PLAIN && ( ch == '-' || ch == '.' || ch == ')' ) )
            F_has_mark = true;
        else
        if( F_has_mark && STR::is_char_space( ch ) )
        {
            set_list_type( ps );
            m_text = m_text.substr( ++i );

            for( auto format : m_formats )
            {
                format->pos_bgn -= i;
                format->pos_end -= i;
            }

            return ps;
        }
        else
            break;
    }
    return VT::PS_PLAIN;
}

int
Paragraph::predict_indent_from_text()
{
    if( !m_host ) return 0;

    int indent_level{ 0 };

    // match the indent level from previous paras of the same type:
    for( Paragraph* p = m_p2prev; p && p->m_order_in_host < m_order_in_host; p = p->m_p2prev )
    {
        if( p->get_para_type() == get_para_type() )
        {
            indent_level = p->get_indent_level();
            break;
        }
        // indent if different than the previous list paragraph
        // TODO: 3.2: we may as well need to outdent based on the parents of p
        else
        if( p->is_list() && p->get_indent_level() >= indent_level )
            indent_level = ( p->get_indent_level() + 1 );
    }

    // it is not beautiful the lists to hace 0 indent (questionable):
    if( indent_level == 0 && is_list() )
        indent_level = 1;

    if( indent_level != get_indent_level() )
        set_indent_level( indent_level );

    return indent_level;
}

void
Paragraph::join_with_next()
{
    if( !m_p2next ) return;

    // if this para is not the title paragraph:
    if( m_p2prev )
    {
        if( is_empty() ) // if this para is empty, inherit the next pararaph's style:
        {
            set_heading_level( m_p2next->get_heading_level() );

            if( !( m_style & VT::PLS::FILTER ) )
            {
                set_list_type( m_p2next->get_list_type() );
                set_indent_level( m_p2next->get_indent_level() );
            }

            set_alignment( m_p2next->get_alignment() );
            set_hrule( m_p2next->has_hrule() );

            if( !has_property( PROP::URI ) && m_p2next->has_property( PROP::URI ) )
                set_uri( m_p2next->get_uri() );

            if( !has_property( PROP::IMG_SIZE ) &&
                m_p2next->has_property( PROP::IMG_SIZE ) )
                set_image_size( m_p2next->get_image_size() );
        }
        else if( m_p2next->has_hrule() )
            set_hrule( true );
    }

    for( auto& format : m_p2next->m_formats )
    {
        auto format_copy = new HiddenFormat( *format );
        format_copy->pos_bgn += m_text.length();
        format_copy->pos_end += m_text.length();
        // not insert_format() as format_copy is guaranteed to come after the last existing format:
        m_formats.insert( format_copy );
    }

    set_expanded( m_p2next->is_expanded() );

    m_text += m_p2next->m_text;

    // Paragraph* p_del { m_p2next };

    if( m_p2next->m_p2next )
        m_p2next->m_p2next->m_p2prev = this;
    m_p2next = m_p2next->m_p2next;

    // do not delete, only shelve to allow undo to work
    // if( m_p2diary )
    //     m_p2diary->shelve_element( p_del->get_id() );

    update_date_edited();
}
Paragraph*
Paragraph::split_at( UstringSize pos, ParserBackGround* parser )
{
    if( pos >= m_text.length() )
        return( new Paragraph( m_host, "" ) );

    Paragraph* para_new = new Paragraph( m_host, m_text.substr( pos ) );

    for( auto& format : m_formats )
    {
        if( format->pos_end > pos )
        {
            para_new->add_format( format->type,
                                  format->uri,
                                  ( format->pos_bgn > pos ? format->pos_bgn - pos : 0 ),
                                  format->pos_end - pos )->ref_id = format->ref_id;
        }
    }

    erase_text( pos, m_text.length() - pos, parser );

    return para_new;
}
