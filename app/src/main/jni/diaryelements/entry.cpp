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


#include "entry.hpp"
#include "diary.hpp"
#include "../lifeograph.hpp"
#include "../strings.hpp"
#include "../diaryelements/paragraph.hpp"
#include "../helpers.hpp"


using namespace LoG;


bool
UndoEdit::s_F_force_absorb { false };

UndoEdit::UndoEdit( UndoableType type, Entry* p2entry, Paragraph* p_prev,
                    int n_paras_orig, int offs_0, int offs_1 )
:   Undoable( type ), m_p2entry( p2entry ),
    m_id_para_before( p_prev ? p_prev->get_id() : DEID::UNSET ),
    m_n_paras_before( n_paras_orig ), m_n_paras_after( n_paras_orig ),
    // n_paras_to_remove is also initialized to n_paras_orig as this is the case for the...
    // ...operations that does not alter the char count
    m_offset_cursor_before( offs_0 ), // position to revert the cursor to on undo
    m_offset_cursor_after( offs_1 )  // position to revert the cursor to on redo
{
    // create the copy chain:
    Paragraph* p          { p_prev ? p_prev->m_p2next : p2entry->get_paragraph_1st() };
    Paragraph* p_new_prev { nullptr };
    for( int i = 0; i < n_paras_orig; ++i )
    {
        m_p2entry->get_diary()->set_force_id_allow_duplicate( p->get_id() );
        Paragraph* p_new { new Paragraph( p ) };

        p_new->m_order_in_host = p->m_order_in_host;
        p_new->m_p2prev = p_new_prev;
        if( p_new_prev )
            p_new_prev->m_p2next = p_new;
        else  // first para
            m_p2original_para_1st = p_new;

        p_new_prev = p_new;
        p = p->m_p2next;
    }
}

UndoEdit::~UndoEdit()
{
    if( !m_F_inhibit_para_deletion )
    {
        for( Paragraph* p = m_p2original_para_1st; p;  )
        {
            Paragraph* p_del { p };
            p = p->m_p2next;
            delete p_del;
        }
    }
}

bool
UndoEdit::can_absorb( const Undoable* action ) const
{
    if( s_F_force_absorb ) return true;

    // different type or more than 5 seconds have passed:
    if( action->get_type() != m_type || ( action->get_time() - m_time > 5 ) ) return false;

    auto edit_new { dynamic_cast< const UndoEdit* >( action ) };

    if( edit_new->m_id_para_before != m_id_para_before ||
        edit_new->m_offset_cursor_before != m_offset_cursor_after ) return false;

    // never absorb multi-para operations:
    if( m_n_paras_before > 1 || edit_new->m_n_paras_before > 1 ||
        m_n_paras_after  > 1 || edit_new->m_n_paras_after > 1 ) return false;

    return true;
}

void
UndoEdit::absorb( Undoable* action )
{
    auto edit_new { dynamic_cast< UndoEdit* >( action ) };
    m_n_paras_after = edit_new->m_n_paras_after;
    m_offset_cursor_after = edit_new->m_offset_cursor_after;
}

Undoable*
UndoEdit::execute()
{
    Paragraph*  para_before { Diary::d->get_element2< Paragraph >( m_id_para_before ) };
    Paragraph*  para_rm_bgn { m_n_paras_after > 0 ? ( para_before ? para_before->m_p2next
                                                                  : m_p2entry->get_paragraph_1st() )
                                                  : nullptr };
    Paragraph*  para_rm_end { para_rm_bgn ? para_rm_bgn->get_nth_next( m_n_paras_after - 1 )
                                          : nullptr };
    UndoEdit*   redo_item   { new UndoEdit( m_type, m_p2entry, para_before,
                                            0, // 0 prevents copying paras
                                            m_offset_cursor_after, m_offset_cursor_before ) };

    // copy over the para counts:
    redo_item->m_n_paras_after = m_n_paras_before;
    redo_item->m_n_paras_before = m_n_paras_after;

    // set the redo paragraphs  if any (ownership is transferred to redo):
    redo_item->m_p2original_para_1st = ( m_n_paras_after > 0 ? para_rm_bgn : nullptr );

    // remove the paragraph chain from the entry:
    if( m_n_paras_after > 0 )
        m_p2entry->remove_paragraphs( para_rm_bgn, para_rm_end );

    // add the original paragraphs (ownership is transferred to the entry):
    if( m_n_paras_before > 0 )
        m_p2entry->add_paragraphs_after( m_p2original_para_1st, para_before,
                                         ParaInhClass::SET_TEXT );

    // do not delete original paragraphs as their ownership has been transferred to the entry now:
    m_F_inhibit_para_deletion = true;

    return redo_item;
}

// ENTRY ===========================================================================================
Entry::Entry( Diary* const d, const DateV date, ElemStatus status )
:   DiaryElemTag( d, _( STRING::EMPTY_ENTRY_TITLE ), date, status )
{ }

SKVVec
Entry::get_as_skvvec() const
{
    SKVVec sv;
    sv.push_back( { CSTR::CREATION_DATE,  get_date_created_str() } );
    sv.push_back( { CSTR::EDIT_DATE,      get_date_edited_str() } );
    sv.push_back( { CSTR::TODO,           get_sstr( get_todo_status_id() ) } );
    sv.push_back( { CSTR::THEME,          is_theme_set() ? m_p2theme->get_name() : "" } );
    sv.push_back( { CSTR::TRASHED,        get_sstr_i( is_trashed() ? CSTR::YES : CSTR::NO ) } );
    sv.push_back( { CSTR::FAVORITE,       get_sstr_i( is_favorite() ? CSTR::YES : CSTR::NO ) } );
    sv.push_back( { CSTR::TITLE_STYLE,    VT::get_v< VT::ETS,
                                                     char const *,
                                                     int >( get_title_style() ) } );
    sv.push_back( { CSTR::COMMENT_STYLE,  VT::get_v< VT::CS,
                                                     char const *,
                                                     int >( get_comment_style() ) } );

    for( auto it_prop : m_properties )
        sv.push_back( { it_prop.first, PropertyStorage::get_variant_str( it_prop.second ) } );

    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        sv.push_back( { para->get_id(), para->get_text() } );

        for( auto it_prop : para->m_properties )
            sv.push_back( { LoGIDF( para->get_id(), it_prop.first ),
                            "    " + PropertyStorage::get_variant_str( it_prop.second ) } );

        for( auto format : para->m_formats )
            if( !( format->type & VT::HFT_F_ONTHEFLY ) )
                sv.push_back( { para->get_format_id_for_sync( format ),
                                "    " + format->get_as_human_readable_str() } );
    }

    return sv;
}

Entry*
Entry::get_parent_unfiltered( FiltererContainer* fc ) const
{
    for( Entry* ep = m_p2parent; ep; ep = ep->m_p2parent )
    {
        if( fc->filter( ep ) )
            return ep;
    }
    return nullptr;
}
Entry*
Entry::get_prev_unfiltered( FiltererContainer* fc ) const
{
    for( Entry* ep = m_p2prev; ep; ep = ep->m_p2prev )
    {
        if( fc->filter( ep ) )
            return ep;
    }
    return nullptr;
}

Entry*
Entry::get_prev_straight( bool F_go_up ) const
{
    if ( m_p2prev )
    {
        auto&& descendants { m_p2prev->get_descendants() };
        return( descendants.empty() ? m_p2prev : descendants.back() );
    }
    else return m_p2parent;
}
Entry*
Entry::get_next_straight( bool F_go_down ) const
{
    if     ( F_go_down && m_p2child_1st ) return m_p2child_1st;
    else if( m_p2next )                   return m_p2next;
    else if( m_p2parent )                 return m_p2parent->get_next_straight( false );
    else                                  return nullptr;
}
Entry*
Entry::get_next_straight( const Entry* e_up_bound, bool F_go_down ) const
{
    if     ( F_go_down && m_p2child_1st ) return m_p2child_1st;
    else if( m_p2next )                   return m_p2next;
    else if( m_p2parent &&
             m_p2parent != e_up_bound )   return m_p2parent->get_next_straight( e_up_bound, false );
    else                                  return nullptr;
}

Entry*
Entry::get_sibling_1st()
{
    Entry* es = this;
    while( es->m_p2prev != nullptr ) es = es->m_p2prev;

    return es;
}

Entry*
Entry::get_sibling_last()
{
    Entry* es = this;
    while( es->m_p2next != nullptr ) es = es->m_p2next;

    return es;
}

bool
Entry::is_descendant_of( const DiaryElemTag* ep ) const
{
    if( !ep->is_entry() ) return false;

    for( Entry* e = m_p2parent; e; e = e->m_p2parent )
        if( ep->get_id() == e->get_id() )
            return true;

    return false;
}

//void
//Entry::set_parent( Entry* ep ) // not used now
//{
//    if( m_p2prev ) m_p2prev->m_p2next = m_p2next;
//    if( m_p2next ) m_p2next->m_p2prev = m_p2prev;
//    if( m_p2parent && m_p2parent->m_p2child_1st == this ) m_p2parent->m_p2child_1st = m_p2next;
//
//    ep->add_child_last( this );
//}

void
Entry::add_child_1st( Entry* e ) // e should be removed from hierarchy before
{
    if( m_p2child_1st )
    {
        m_p2child_1st->m_p2prev = e;
        e->m_p2next = m_p2child_1st;
    }

    e->m_p2parent = this;
    m_p2child_1st = e;

    e->update_sibling_orders();
}

void
Entry::add_child_last( Entry* e ) // e should be removed from hierarchy before
{
    if( m_p2child_1st )
    {
        Entry* last_child_cur = m_p2child_1st->get_sibling_last();

        last_child_cur->m_p2next = e;
        e->m_p2prev = last_child_cur;
    }
    else
        m_p2child_1st = e;

    e->m_p2parent = this;

    e->update_sibling_orders();
}

void
Entry::add_sibling_before( Entry* e )
{
    if( m_p2prev )
    {
        m_p2prev->m_p2next = e;
        e->m_p2prev = m_p2prev;
    }
    else
    {
        e->m_p2prev = nullptr;
        if( m_p2parent )
            m_p2parent->m_p2child_1st = e;
    }

    e->m_p2parent = m_p2parent;
    e->m_p2next = this;
    e->m_sibling_order = m_sibling_order;
    m_p2prev = e;

    if( m_p2diary && m_p2diary->get_entry_1st() == this )
        m_p2diary->set_entry_1st( e );

    update_sibling_orders();
}

void
Entry::add_sibling_after( Entry* e )
{
    if( m_p2next )
    {
        m_p2next->m_p2prev = e;
        e->m_p2next = m_p2next;
    }
    else
    {
        e->m_p2next = nullptr;
    }

    e->m_p2parent = m_p2parent;
    e->m_p2prev = this;
    m_p2next = e;

    e->update_sibling_orders();
}

void
Entry::add_sibling_chain_after( Entry* ec )
{
    // parents
    for( Entry* e = ec; e; e = e->m_p2next )
        e->m_p2parent = m_p2parent;

    Entry* e_sibl_last { ec->get_sibling_last() };
    if( m_p2next )
        m_p2next->m_p2prev = e_sibl_last;
    e_sibl_last->m_p2next = m_p2next;

    ec->m_p2prev = this;
    m_p2next = ec;

    ec->update_sibling_orders();
}

void
Entry::append_entry_as_paras( Entry* entry_r )
{
    if( m_p2para_last )
    {
        if( !m_p2para_last->is_empty() )
            add_paragraph_before( "", nullptr );

        if( entry_r->get_reference_count() > 0 )
        {
            const auto&   references  { entry_r->get_references() };
            Paragraph*    p2para_1st  { entry_r->m_p2para_1st };

            for( auto r : references )
            {
                if( r->get_type() == DiaryElement::ET_PARAGRAPH )
                {
                    Paragraph* p { dynamic_cast< Paragraph* >( r ) };

                    for( auto f : p->m_formats )
                        if( f->type == VT::HFT_TAG && f->ref_id == entry_r->get_id().get_raw() )
                        {
                            f->set_id_lo( p2para_1st->get_id() );
                            p2para_1st->set_tag_bound( p2para_1st->get_size() );
                        }
                }
            }
        }
        entry_r->m_p2para_1st->set_heading_level( VT::PHS::LARGE::I );
        // no issue as it's previous host will be dismissed

        // DATE UPDATES
        update_date_edited();
        update_inline_dates();
    }

    Paragraph* p2para_1st { entry_r->m_p2para_1st };
    entry_r->m_p2para_1st = entry_r->m_p2para_last = nullptr; // remove paragraphs from the entry
    add_paragraphs_after( p2para_1st, m_p2para_last );
}

void
Entry::do_for_each_descendant( const FuncVoidEntry& process_entry )
{
    for( Entry* e = m_p2child_1st; e; e = e->m_p2next )
    {
        process_entry( e );
        e->do_for_each_descendant( process_entry );
    }
}

int
Entry::get_child_count() const
{
    int count{ 0 };

    for( Entry* e = m_p2child_1st; e != nullptr; e = e->m_p2next )
        count++;

    return count;
}

VecEntries
Entry::get_descendants() const
{
    // NOTE: also returns grand-children
    VecEntries descendants;

    for( Entry* e = m_p2child_1st; e != nullptr; e = e->get_next_straight( this, true ) )
        descendants.push_back( e );

    return descendants;
}

VecTags
Entry::get_descendant_tags() const
{
    // NOTE: also returns grand-children
    VecTags descendants;

    for( Entry* e = m_p2child_1st; e != nullptr; e = e->get_next_straight( this, true ) )
        descendants.push_back( e );

    return descendants;
}

int
Entry::get_generation() const
{
    int gen{ 0 };
    for( Entry* e = m_p2parent; e != nullptr; e = e->m_p2parent )
        gen++;

    return gen;
}

int
Entry::get_descendant_depth() const
{
    // NOTE: not very efficient
    int depth{ 0 };
    for( Entry* e = get_next_straight( this ); e; e = e->get_next_straight( this, true ) )
    {
        int gen = e->get_generation();
        if( gen > depth )
            depth = gen;
    }

    return( depth - get_generation() );
}

UstringSize
Entry::translate_to_visible_pos( UstringSize pos_absolute ) const
{
    UstringSize pos_visible{ 0 };
    UstringSize pos_cur{ 0 };

    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        if( pos_absolute < pos_cur + p->get_size() )
            return( pos_visible + pos_absolute - pos_cur );

        pos_cur += ( p->get_size() + 1 );

        if( p->is_visible() )
            pos_visible += ( p->get_size() + 1 );
    }

    return pos_absolute;
}

const R2Pixbuf&
Entry::get_icon() const
{
    if( m_status & ES::HAS_VSBL_DESCENDANT )
        return Lifeograph::icons->filter;
    else if( m_status & ES::FILTER_TODO_PURE )
        return Lifeograph::get_todo_icon( m_status & ES::FILTER_TODO_PURE );
    else if( get_title_style() == VT::ETS::MILESTONE::I )
        return Lifeograph::icons->milestone_16;
    else if( registers_scripts() )
        return Lifeograph::icons->entry_script_16;
    else
        return( !has_property( PROP::UNIT ) ?
                ( m_p2child_1st ?
                  Lifeograph::icons->entry_parent_16 : Lifeograph::icons->entry_16 ) :
                Lifeograph::icons->tag_16 );
}
const R2Pixbuf&
Entry::get_icon32() const
{
    if( m_status & ES::FILTER_TODO_PURE )
        return Lifeograph::get_todo_icon32( m_status & ES::FILTER_TODO_PURE );
    else if( get_title_style() == VT::ETS::MILESTONE::I )
        return Lifeograph::icons->milestone_32;
    else if( registers_scripts() )
        return Lifeograph::icons->entry_script_32;
    else
        return( has_property( PROP::UNIT )
                ? Lifeograph::icons->tag_32
                : ( m_p2child_1st ? Lifeograph::icons->entry_parent_32
                                  : Lifeograph::icons->entry_32 ) );
}

const R2Pixbuf&
Entry::get_icon_lock() const
{
    if( is_trashed() )
        return Lifeograph::icons->trash_16;
    return Lifeograph::icons->lock;
}

Ustring
Entry::get_list_str() const
{
    static const std::string completion_colors[] =
            { "BB0000", "C06000", "B69300", "90B000", "449000", "00B000" };
    const int                child_count{ get_child_count() };
    Ustring                  str;

    // BOLD
    if( ( m_status & ES::FAVORED ) || get_title_style() == VT::ETS::MILESTONE::I )
        str += "<b>";

    // NUMBER
    switch( get_title_style() )
    {
        case VT::ETS::MILESTONE::I:
            str += "<span color=\"#666666\" bgcolor=\"";
            str += get_color_no_fail();
            str += "\"> – ";
            str += Date::format_string( m_date );
            str += " – </span>  ";
            break;
        case VT::ETS::DATE_AND_NAME::I:
            str += Date::format_string( m_date );
            str += " -  ";
            break;
        case VT::ETS::NUMBER_AND_NAME::I:
            str += get_number_str();
            str += " -  ";
            break;
    }

    // CUT
    if( m_status & ES::CUT )
        str += "<i>";

    // STRIKE-THROUGH
    if( m_status & ES::CANCELED )
        str += "<s>";

    // NAME ITSELF
    if( m_name.empty() == false )
        str += Glib::Markup::escape_text( m_name );

    // DESCRIPTION
    if( get_title_style() == VT::ETS::NAME_AND_DESCRIPT::I )
        str += ( " -  <i>" + Glib::Markup::escape_text( get_description() ) + "</i>" );

    // STRIKE-THROUGH
    if( m_status & ES::CANCELED )
        str += "</s>";

    // CUT
    if( m_status & ES::CUT )
        str += "</i>";

    // BOLD
    if( ( m_status & ES::FAVORED ) || get_title_style() == VT::ETS::MILESTONE::I )
        str += "</b>";

    // EXTRA INFO
    // COMPLETION
    if( get_workload() > 0.0 )
    {
        double completion{ get_completion() };
        str += "  <span color=\"#FFFFFF\" bgcolor=\"#";
        str += completion_colors[ std::clamp( int( completion * 5 ), 0, 5 ) ];
        str += "\"> ";
        str += STR::format_percentage( completion );
        str += " </span>";
    }
    // CHILD COUNT
    if( child_count > 0 )
    {
        str += "  <span color=\"";
        str += Lifeograph::get_color_insensitive();
        str += "\">(";
        str += std::to_string( child_count );
        str += ")</span>";
    }

    return str;
}

Ustring
Entry::get_ancestry_path() const
{
    Ustring path;

    for( Entry* pe = m_p2parent; pe != nullptr; pe = pe->m_p2parent )
        path.insert( 0, pe->m_name + "  ▶  " );

    return path;
}

void
Entry::toggle_favored()
{
    m_status ^= ES::FILTER_FAVORED;
    update_date_edited();
}

void
Entry::update_name()
{
    if( is_empty() )
        m_name = _( STRING::EMPTY_ENTRY_TITLE );
    else
        m_name = m_p2para_1st->get_text();
}

inline bool
is_status_ready( const ElemStatus& s )
{
    return( ( s & ES::PROGRESSED ) || ( ( s & ES::TODO ) && ( s & ES::DONE ) ) );
}
inline ElemStatus
convert_status( const ElemStatus& s )
{
    if( is_status_ready( s ) )
        return( ES::NOT_TODO | ES::PROGRESSED );

    switch( s & ~ES::NOT_TODO )
    {
        case ES::CANCELED:
            return( ES::NOT_TODO|ES::CANCELED );
        case ES::TODO:
        case ES::TODO|ES::CANCELED:
            return( ES::NOT_TODO|ES::TODO );
        case ES::DONE:
        case ES::DONE|ES::CANCELED:
            return( ES::NOT_TODO|ES::DONE );
        default:
            return ES::NOT_TODO;
    }
}

bool
Entry::update_todo_status()
{
    // NOTE: for low level operations. does not update Entry's edit date (also on-the-fly)
    ElemStatus es{ 0 };

    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        es |= para->get_todo_status();

        if( is_status_ready( es ) )
            break;
    }
    es = convert_status( es );

    if( es != get_todo_status() )
    {
        set_todo_status( es );
        return true;
    }

    return false;
}

double
Entry::get_completion() const
{
    const double wl{ get_workload() };

    if( wl == 0.0 )
        return 1.0;

    return( std::min( get_completed() / wl, 1.0 ) );
}

String
Entry::get_completion_str() const
{
    const auto workload { get_workload() };

    if( workload )
        return STR::compose( STR::format_percentage( get_completion() ),
                             " (", get_completed(), "/", workload, ")" );
    else
        return "";
}

double
Entry::get_completed() const
{
    DiaryElemTag* tag_comp { m_p2diary ? m_p2diary->get_completion_tag() : nullptr };

    if( tag_comp == nullptr )
        return 0.0;

    return get_tag_value( tag_comp, false );
}

double
Entry::get_workload() const
{
    DiaryElemTag* tag_comp { m_p2diary ? m_p2diary->get_completion_tag() : nullptr };

    if( tag_comp == nullptr )
        return 0.0;

    return get_tag_value_planned( tag_comp, false );
}

Ustring
Entry::get_text() const
{
    Ustring text;
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        text += para->get_text();
        text += '\n';
    }

    if( text.empty() == false )
        text.erase( text.length() - 1, 1 );

    return text;
}

Ustring
Entry::get_text_visible( bool F_reset_visibilities ) const
{
    Ustring text;
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        if( F_reset_visibilities )
            para->reset_visibility(); // sets visibilities per expansion states of parents

        if( para->is_visible() )
        {
            text += para->get_text();
            text += '\n';
        }
    }

    if( text.empty() )
        text = "\n";

    return text;
}

Ustring
Entry::get_text_partial( Paragraph* p_bgn, Paragraph* p_end, bool F_visible_only ) const
{
    Ustring text;
    for( Paragraph* p = p_bgn; p; p = p->m_p2next )
    {
        if( !F_visible_only || p->is_visible() )
        {
            text += p->get_text();
            text += '\n';
        }

        if( !p_end || p == p_end )
            break;
    }

    return text;
}
String
Entry::get_text_partial_code( Paragraph* p_bgn, Paragraph* p_end ) const
{
    String text;
    for( Paragraph* p = p_bgn; p; p = p->m_p2next )
    {
        text += p->get_text_code();
        text += '\n';

        if( !p_end || p == p_end )
            break;
    }

    return text;
}
Ustring
Entry::get_text_partial( UstringSize pos_bgn, UstringSize pos_end, bool F_decorated ) const
{
    Ustring     text;
    UstringSize pos_p_bgn{ 0 };

    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        if( !p->is_visible() ) continue;

        const auto pos_p_end{ pos_p_bgn + p->get_size() };

        if( pos_p_end > pos_bgn )
        {
            if( pos_bgn <= pos_p_bgn && pos_end >= pos_p_end )
                text += ( F_decorated ? p->get_text_decorated() : p->get_text() );
            else
            {
                const auto pb { pos_bgn < pos_p_bgn ? 0ul : pos_bgn - pos_p_bgn };
                text += p->get_text().substr( pb, pos_end > pos_p_end ? pos_p_end - pos_p_bgn - pb
                                                                      : pos_end - pos_p_bgn - pb );
                // no decoration except at the beginning, so no decorated version
            }

            if( pos_end > pos_p_end )
                text += '\n';
            else
                break;
        }

        pos_p_bgn = ( pos_p_end + 1 ); // +1 is for \n
    }

    return text;
}
Paragraph*
Entry::get_text_partial_paras( const UstringSize pos_bgn, const UstringSize pos_end ) const
{
    Paragraph*    p2para_1st  { nullptr };
    Paragraph*    p2para_prev { nullptr };
    Paragraph*    p2para_new  { nullptr };
    UstringSize   pos_p_bgn   { 0 };

    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        if( !p->is_visible() ) continue;

        const auto pos_p_end{ pos_p_bgn + p->get_size() };

        if( pos_p_end >= pos_bgn ) // pos_p_end == pos_bgn when the para is empty
        {
            if( pos_bgn <= pos_p_bgn && pos_end >= pos_p_end )
            {
                // disable addition of the paragraph into the Diary:
                if( m_p2diary ) m_p2diary->set_force_id_allow_duplicate( DEID::OMIT );
                p2para_new = new Paragraph( p );
            }
            else
                p2para_new = p->get_sub( pos_bgn < pos_p_bgn ? 0 : pos_bgn - pos_p_bgn,
                                         pos_end > pos_p_end ? pos_p_end - pos_p_bgn
                                                             : pos_end - pos_p_bgn );

            if( p2para_prev )
            {
                p2para_prev->m_p2next = p2para_new;
                p2para_new->m_p2prev = p2para_prev;
            }
            else  // first round
                p2para_1st = p2para_new;

            if( pos_end > pos_p_end )
                p2para_prev = p2para_new;
            else
                break;
        }

        pos_p_bgn = ( pos_p_end + 1 ); // +1 is for \n
    }

    return p2para_1st;
}

FormattedText
Entry::get_formatted_text( UstringSize pos_bgn, UstringSize pos_end ) const
{
    FormattedText   ft;
    UstringSize     pos_p_bgn { 0 };

    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        const auto p_size{ p->get_size() };

        if( ( pos_p_bgn + p_size ) >= pos_bgn )
        {
            if( pos_bgn <= pos_p_bgn && pos_end >= ( pos_p_bgn + p_size ) )
                ft.m_text += p->get_text();
            else
            {
                ft.m_text += p->get_substr( pos_bgn > pos_p_bgn ? pos_bgn - pos_p_bgn : 0ul,
                                            pos_end >= ( pos_p_bgn + p_size )
                                            ? p_size
                                            : pos_end - pos_p_bgn );
            }

            for( auto& f : p->m_formats )
            {
                if( ( f->pos_bgn + pos_p_bgn ) < pos_end && ( f->pos_end + pos_p_bgn ) >= pos_bgn )
                {
                    auto fn{ new HiddenFormat( *f ) };
                    // shift it from para offsets to region offsets
                    fn->pos_bgn = ( f->pos_bgn + pos_p_bgn > pos_bgn
                                    ? f->pos_bgn + pos_p_bgn - pos_bgn
                                    : 0ul );
                    fn->pos_end = ( f->pos_end + pos_p_bgn < pos_end
                                    ? f->pos_end + pos_p_bgn - pos_bgn
                                    : pos_end - pos_bgn );
                    ft.m_formats.insert( fn );
                }
            }

            if( pos_end > ( pos_p_bgn + p_size ) )
                ft.m_text += '\n';
            else
                break;
        }

        pos_p_bgn += ( p_size + 1 ); // +1 is for \n
    }

    return ft;
}

void
Entry::clear_text() // not undoable
{
    // NOTE: for low level operations. does not update Entry's edit date
    for( Paragraph* para = m_p2para_1st; para; )
    {
        Paragraph* p_del { para };
        para = para->m_p2next;
        delete p_del;
    }
    m_p2para_1st = m_p2para_last = nullptr;
}

void
Entry::set_text( const Ustring& text, ParserBackGround* parser )
{
    // NOTE: for low level operations. does not update Entry's edit date nor adds undo
    clear_text();
    digest_text( text, parser );

    update_name();
    update_todo_status();

    if( m_p2diary )
        m_p2diary->update_tag_refs( get_id(), get_name_pure() );
}

void
Entry::insert_text( UstringSize pos, const Ustring& new_text, const ListHiddenFormats* formats,
                    ParaInhClasses ic, bool F_add_undo )
{
    Paragraph*  para            { nullptr };
    Paragraph*  para_inherit    { nullptr };
    UstringSize pos_in_para     { 0 };
    UstringSize pos_split       { 0 };
    Ustring     para_text;
    ListHiddenFormats::const_iterator
                it_format;
    UstringSize pos_para_bgn    { 0 };
    UndoEdit*   p2undo          { nullptr };
    int         n_paras_changed { 1 };  // 1 is minimum even when no new para was added
    // use the diary standard parser as insert is not a bg job:
    ParserBackGround*
                parser          { m_p2diary ? &m_p2diary->m_parser_bg : nullptr };

    // DETECT THE PARAGRAPH
    if( new_text.empty() )
        return;
    else if( pos == Ustring::npos  ) // append text mode
    {
        para_inherit = para = add_paragraph_before( "", nullptr, nullptr, ic );
        pos = get_size();
    }
    else if( !m_p2para_1st ) // entry is empty
    {
        para_inherit = para = add_paragraph_before( "", nullptr );
        pos = 0;
    }
    else if( get_paragraph( pos, para, pos_para_bgn, true ) )
    {
        pos_in_para = pos_split = ( pos - pos_para_bgn );
        para_inherit = para;
        // we had some pproblems with the solution below in the past, so be careful:
        if( new_text[ 0 ] == '\n' && !para->is_expanded() && para->has_subs() &&
            // do not append to end when inserting in the middle of para:
            int( pos_split ) == para->get_size() )
        {
            para_inherit = para;
            para = para->get_sub_last();
            pos_split = para->get_size();
        }
    }
    else
        throw LoG::Error( "Text cannot be inserted!" );

    // UNDO STACK
    if( F_add_undo )
    {
        p2undo = add_undo_action( UndoableType::INSERT_TEXT, para, 1,
                                  pos, pos + new_text.length() );
    }

    // INTERNAL FUNCTION TO INSERT WITH FORMATS
    auto insert_text_to_para = [ & ]()
    {
        para->insert_text( pos_in_para, para_text, parser );

        while( formats && it_format != formats->end() )
        {
            if( ( *it_format )->pos_bgn + pos < ( pos_para_bgn + para->get_size() ) )
            {
                para->add_format( *it_format, pos - pos_para_bgn );
                ++it_format;
            }
            else
                break;
        }
    };

    // INIT FORMATS ITER
    if( formats )
        it_format = formats->begin();

    // ADD THE CHARS ONE BY ONE
    for( auto ch : new_text )
    {
        if( ch == '\n' )
        {
            if( !para_text.empty() )
                insert_text_to_para();

            // please note that when pos==end, split_at just creates an empty new paragraph
            para = add_paragraphs_after( para->split_at( pos_split + para_text.length(), parser ),
                                         para,
                                         !para_inherit ? ic : ParaInhClass::NONE );
                                         // if para inherit is set, inheritance is applied below
            pos_split = 0;

            if( !ic.is_empty() && para_inherit )
            {
                ParaInhClasses ic_final { ic };
                if( !para_inherit->is_title() && para_inherit->is_empty() && !para->is_empty() )
                    ic_final |= ParaInhClass::HEADING_LVL;
                if( n_paras_changed == 1 )
                    ic_final |= ParaInhClass::INDENTATION;
                para->inherit_style_from( para_inherit, ic_final );
            }

            // remove heading and hrule properties from empty paragraphs:
            if( para_inherit->is_empty() )
            {
                para_inherit->set_hrule( false );
                para_inherit->set_heading_level( VT::PHS::NORMAL::I );
            }

            pos_para_bgn += ( para->get_size() + 1 );   // +1 for the \n
            pos_in_para = 0;
            para_text.clear();
            ++n_paras_changed;
        }
        else
        {
            para_text += ch;
        }
    }

    if( !para_text.empty() )
        insert_text_to_para();
    else
        parser->parse( para );

    // UNDO
    if( F_add_undo )
        p2undo->set_n_paras_after( n_paras_changed );

    // OTHER UPDATES
    if( !ic.contains( ParaInhClass::SET_TEXT ) )
    {
        update_date_edited();
        update_inline_dates();
    }

    if( pos <= ( unsigned ) m_p2para_1st->get_size() )
        update_name();
}

void
Entry::erase_text( const UstringSize pos_bgn, const UstringSize pos_end, bool F_full_procedure )
{
    UstringSize para_offset_bgn { 0 };
    Paragraph*  para_bgn        { nullptr };
    if( !get_paragraph( pos_bgn, para_bgn, para_offset_bgn, true ) ) return;

    Paragraph*  para_end        { nullptr };
    UstringSize para_offset_end { 0 };
    if( !get_paragraph( pos_end, para_end, para_offset_end, true ) ) return;

    // TODO: 3.2: backspace at the beginning of a para after a collapsed para deletes
    // ...tthe unexposed paras silently

    const int   n_deleted_paras { para_end->m_order_in_host - para_bgn->m_order_in_host };
    // use the diary standard parser as erase is not a bg job:
    ParserBackGround*
                parser          { m_p2diary ? &m_p2diary->m_parser_bg : nullptr };

    // undo stack:
    if( F_full_procedure )
    {
        add_undo_action( UndoableType::ERASE_TEXT, para_bgn, n_deleted_paras + 1,
                         pos_end, pos_bgn )
                ->set_n_paras_after( 1 );
    }

    // merge paragraphs into the first one, if necessary:
    if( ( para_bgn->m_order_in_host + n_deleted_paras ) >= m_p2para_last->m_order_in_host )
        m_p2para_last = para_bgn;
    for( int i = 0; i < n_deleted_paras; ++i )
    {
        para_bgn->join_with_next();
    }
    // update host orders:
    for( Paragraph* p = para_bgn->m_p2next; p; p = p->m_p2next )
        p->m_order_in_host = ( p->m_p2prev->m_order_in_host + 1 );

    // actually erase the text from para_bgn:
    para_bgn->erase_text( pos_bgn - para_offset_bgn, pos_end - pos_bgn - n_deleted_paras, parser );

    if( F_full_procedure )
    {
        update_date_edited();
        update_inline_dates();

        if( /*!m_p2para_1st ||*/ pos_bgn <= ( unsigned ) m_p2para_1st->get_size() )
            update_name();
    }
}

void
Entry::replace_text_with_styles( UstringSize pos_bgn, UstringSize pos_end, Paragraph* para_new_1st )
{
    Paragraph*  para            { nullptr };
    UstringSize pos_split       { 0 };
    UstringSize pos_para_bgn    { 0 };
    Paragraph*  para_split      { nullptr };
    UndoEdit*   p2undo          { nullptr };
    int         n_paras_changed { 1 };  // 1 is minimum even when no new para was added

    // ERASE THE TEXT
    if( pos_end > pos_bgn )
    {
        UstringSize para_offset_bgn { 0 };
        Paragraph*  para_bgn        { nullptr };
        if( !get_paragraph( pos_bgn, para_bgn, para_offset_bgn, true ) ) return;

        Paragraph*  para_end        { nullptr };
        UstringSize para_offset_end { 0 };
        if( !get_paragraph( pos_end, para_end, para_offset_end, true ) ) return;

        const int   n_deleted_paras { para_end->m_order_in_host - para_bgn->m_order_in_host };

        // undo stack:
        p2undo = new UndoEdit( UndoableType::INSERT_TEXT, this, para_bgn->m_p2prev,
                               n_deleted_paras + 1, pos_end, pos_bgn );

        // merge paragraphs into the first one, if necessary:
        if( ( para_bgn->m_order_in_host + n_deleted_paras ) >= m_p2para_last->m_order_in_host )
            m_p2para_last = para_bgn;
        for( int i = 0; i < n_deleted_paras; ++i )
        {
            para_bgn->join_with_next();
        }

        // actually erase the text from para_bgn:
        para_bgn->erase_text( pos_bgn - para_offset_bgn, pos_end - pos_bgn - n_deleted_paras );
    }

    // DETECT THE PARAGRAPH
    if( !para_new_1st )
        return;
    else if( !m_p2para_1st ) // entry is empty
    {
        para = add_paragraph_before( "", nullptr, nullptr, ParaInhClass::NONE );
        pos_bgn = 0;
    }
    else if( get_paragraph( pos_bgn, para, pos_para_bgn, true ) )
    {
        pos_split = ( pos_bgn - pos_para_bgn );
        if( m_p2para_1st->is_empty() && para->has_subs() && !para->is_expanded() &&
            // do not append to end when inserting in the middle of para:
            int( pos_split ) == para->get_size() )
        {
            para = para->get_sub_last();
        }
    }
    else
        throw LoG::Error( "Text cannot be inserted!" );

    // UNDO
    if( !p2undo )
        p2undo = new UndoEdit( UndoableType::INSERT_TEXT, this, para->m_p2prev,
                               1, pos_end, pos_bgn + para_new_1st->get_chain_char_count() );

    // INSERT THE NEW TEXT
    if( pos_split >= 0 )
        para_split = para->split_at( pos_split );
    para->insert_text( pos_split, para_new_1st, nullptr );  // first para is inserted
    // we normally don't access the parser directly, but here it makes sense:
    m_p2diary->m_parser_bg.parse( para );

    for( Paragraph* p = para_new_1st->m_p2next; p; p = p->m_p2next )
    {
        ++n_paras_changed;
        para = add_paragraphs_after( new Paragraph( p ), para, ParaInhClass::NONE );
        m_p2diary->m_parser_bg.parse( para );
    }

    if( para_split && !para_split->is_empty() )
    {
        para->append( para_split, nullptr );
        m_p2diary->m_parser_bg.parse( para );
    }

    // UNDO
    p2undo->set_n_paras_after( n_paras_changed );
    m_session_edits.add_action( p2undo );

    // OTHER UPDATES
    update_date_edited();
    update_inline_dates();

    if( pos_bgn <= ( unsigned ) m_p2para_1st->get_size() )
        update_name();
}

bool
Entry::parse( ParserBackGround* parser )
{
    // NOTE: does not actually manipulate contents but may correct dates
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        parser->parse( para );
        if( para->has_date() )
            update_inline_dates( para, para );
    }

    return true;
}

void
Entry::digest_text( const Ustring& text, ParserBackGround* parser )
{
    // NOTE: does manipulation through other functions. so, does not update edit date
    if( text.empty() )
    {
        add_paragraph_before( "", nullptr, nullptr, ParaInhClass::SET_TEXT );
        return;
    }

    UstringSize pt_bgn{ 0 }, pt_end{ 0 };
    bool flag_terminate_loop{ false };

    while( true )
    {
        pt_end = text.find( '\n', pt_bgn );
        if( pt_end == Ustring::npos )
        {
            pt_end = text.size();
            flag_terminate_loop = true;
        }

        add_paragraph_before( text.substr( pt_bgn, pt_end - pt_bgn), nullptr, parser,
                              ParaInhClass::SET_TEXT );

        if( flag_terminate_loop )
            break; // end of while( true )

        pt_bgn = pt_end + 1;
    }
}

Paragraph*
Entry::beautify_text( Paragraph* p_bgn, Paragraph* p_end, ParserBackGround* parser )
{
    bool      F_prev_para_is_blank  { false };
    int       i_para                { p_bgn->m_order_in_host };
    const int i_para_end            { p_end->m_order_in_host };
    int       n_removed_paras       { 0 };
    auto      p2undo                { add_undo_action( UndoableType::MODIFY_TEXT,
                                                       p_bgn,
                                                       p_bgn->get_chain_para_count_to( p_end ),
                                                       p_bgn->get_bgn_offset_in_host() ) };

    while( p_bgn )
    {
        int l_trim_bgn  { 0 };
        int l_trim_end  { 0 };
        int l_indent    { 0 };
        int l_para      { p_bgn->get_size() };

        while( l_trim_bgn < l_para )
        {
            if     ( p_bgn->get_char( l_trim_bgn ) == ' ' )   l_indent++;
            else if( p_bgn->get_char( l_trim_bgn ) == '\t' )  l_indent+=4;
            else break;
            l_trim_bgn++;
        }
        while( l_trim_end < l_para &&
               STR::is_char_space( p_bgn->get_char( l_para - l_trim_end - 1 ) ) )
            l_trim_end++;

        if( l_trim_end >= l_para )
        {
            if( F_prev_para_is_blank )
            {
                Paragraph* p2prev = p_bgn->m_p2prev;
                remove_paragraphs( p_bgn );
                ++n_removed_paras;
                p_bgn = p2prev;
            }
            else
                F_prev_para_is_blank = true;

            continue;
        }
        else if( ( l_trim_bgn + l_trim_end ) > 0 )
        {
            p_bgn->set_text( p_bgn->get_substr( l_trim_bgn, l_para - l_trim_end ), parser );
            p_bgn->set_indent_level( l_indent / 4 );
        }

        if( p_bgn->get_list_type() == 0 )
            if( p_bgn->predict_list_style_from_text() != VT::PS_PLAIN )
                p_bgn->predict_indent_from_text();

        F_prev_para_is_blank = false;

        if( ++i_para <= i_para_end )  // do not move the p_bgn if the end is reached
            p_bgn = p_bgn->m_p2next;
        else
            break;
    }

    // finishing undo values:
    p2undo->m_n_paras_after = p2undo->m_n_paras_before - n_removed_paras;

    // DATE UPDATE
    update_date_edited();

    return p_bgn;
}

bool
Entry::get_paragraph( UstringSize pos, Paragraph*& para, UstringSize& para_offset,
                      bool F_ignore_hidden ) const
{
    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        if( !p->is_visible() && F_ignore_hidden )
            continue;
        if( pos <= para_offset + p->get_size() )
        {
            para = p;
            return true;
        }
        else
            para_offset += ( p->get_size() + 1 );  // +1 is for \n
    }

    return false;
}
Paragraph*
Entry::get_paragraph( UstringSize pos, bool F_ignore_hidden ) const
{
    UstringSize offset_total{ 0 };

    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        if( !para->is_visible() && F_ignore_hidden )
            continue;
        else
        {
            offset_total += ( para->get_size() + 1 );  // +1 is for \n
            if( pos < offset_total )
                return para;
        }
    }

    return m_p2para_last;
    // not sure if above is a good idea, but we need to take care of offset past the end
}
Paragraph*
Entry::get_paragraph_by_no( unsigned int no ) const
{
    unsigned int i{ 0 };

    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        if( i == no ) return para;
        else          i++;

    return nullptr;
}

void
Entry::set_paragraph_1st( Paragraph* para )
{
    // NOTE: for low level operations. does not update Entry's edit date
    if( para )
    {
        para->clear_list_type();
        para->clear_heading_level(); // main heading level comes from position
    }

    m_p2para_1st = para;

    update_name();
}

Paragraph*
Entry::add_paragraph_before( const Ustring& text, Paragraph* para_after, ParserBackGround* parser,
                             ParaInhClasses ic )
{
    Paragraph*  para        { new Paragraph( this, text, parser ) };
    Paragraph*  para_before { para_after ? para_after->m_p2prev : m_p2para_last };
    auto        ic_final    { ic.contains( ParaInhClass::SET_TEXT ) ||
                              ( para_before && para_before->is_visible() ) ?
                              ic : ParaInhClass::NONE };

    add_paragraphs_after( para, para_before, ic_final );
    // NOTE: do not inherit from collapsed paras

    return para;
}

Paragraph*
Entry::add_paragraphs_after( Paragraph* const para_chain_bgn, Paragraph* const para_before,
                             ParaInhClasses ic )
{
    // calculate chain end and reassign to ids:
    Paragraph*  para_chain_end    { para_chain_bgn };
    int         chain_char_count  { 0 };
    int         chain_para_count  { 0 };
    for( ; ; para_chain_end = para_chain_end->m_p2next )
    {
        // change the id in the map to point to the new para:
        if( m_p2diary )
            m_p2diary->reclaim_id_for_elem( para_chain_end );

        chain_char_count += ( para_chain_end->get_size() + 1 ); // +1 for \n
        chain_para_count += 1;

        if( !para_chain_end->m_p2next ) break;
    }

    // weld the chain into the place part-1:
    para_chain_bgn->m_p2prev = para_before;
    para_chain_end->m_p2next = ( para_before ? para_before->m_p2next : m_p2para_1st );

    // update indices of the paragraphs:
    int i { para_before ? para_before->m_order_in_host + 1 : 0 };
    for( Paragraph* p = para_chain_bgn; p; p = p->m_p2next )
    {
        p->m_host = this;
        p->m_order_in_host = i;
        p->add_or_remove_ref_from_tags( true );
        ++i;
    }

    // weld the chain into the place part-2:
    if( para_before )
        para_before->m_p2next = para_chain_bgn;

    if( para_chain_end->m_p2next )
        para_chain_end->m_p2next->m_p2prev = para_chain_end;

    if( m_p2para_last == para_before )
        m_p2para_last = para_chain_end;
    // else if( m_p2para_last == para_chain_end ) <- how can this be? delete?
    //     m_p2para_last = para_chain_end->m_p2next;

    // update the name when the entry was empty at the time the second para is added:
    if( para_chain_bgn->m_order_in_host == 1 && m_p2para_1st->is_empty() )
        update_name();
    else if( para_chain_bgn->m_order_in_host == 0 )
    {
        if( ic.contains( ParaInhClass::SET_TEXT ) ) // on set_text do not reset anything
        {
            m_p2para_1st = para_chain_bgn;
            update_name();
        }
        else
            set_paragraph_1st( para_chain_bgn );
    }

    if( para_before && !para_before->is_empty() )
        para_chain_bgn->inherit_style_from( para_before, ic );

    // undo except for SET_TEXT operations (on add operations undo is added after the operation):
    if( !ic.contains( ParaInhClass::SET_TEXT ) )
    {
        const int offset { para_before ? para_before->get_end_offset_in_host() : 0 };

        add_undo_action( UndoableType::INSERT_TEXT,
                         para_chain_bgn, 0,
                         offset,
                         offset + chain_char_count )
            ->m_n_paras_after = chain_para_count;
    }

    // DATE UPDATES
    if( !ic.contains( ParaInhClass::SET_TEXT ) )
    {
        update_date_edited();
        update_inline_dates( para_chain_bgn, para_chain_end );
    }

    return para_chain_bgn;
}

void
Entry::remove_paragraphs( Paragraph* para, Paragraph* para_last, bool F_add_undo )
{
     // single para mode:
    if( !para_last ) para_last = para;

    Paragraph*  p2para_next   { para_last->m_p2next };
    const int   delta         { para->get_chain_para_count_to( para_last ) };

    if( F_add_undo )
        add_undo_action( UndoableType::ERASE_TEXT,
                         para,
                         p2para_next ? ( delta + 1 ) : delta,
                         para->get_end_offset_in_host(),
                         para->get_bgn_offset_in_host() )
                ->m_n_paras_after = ( p2para_next ? 1 : 0 );

    for( Paragraph* p = para; p && p!= para_last->m_p2next; p = p->m_p2next )
    {
        // if-less approach:
        m_F_map_path_old = ( m_F_map_path_old || p->has_property( PROP::LOCATION ) );
        if( m_p2diary ) m_p2diary->shelve_id( p->get_id() );
        p->add_or_remove_ref_from_tags( false );
    }

    for( Paragraph* p = para_last->m_p2next; p; p = p->m_p2next )
        p->m_order_in_host -= delta;
    // para->m_order_in_host = -1; // is it necessary to do this?

    if( para->m_p2prev )
        para->m_p2prev->m_p2next = para_last->m_p2next;
    if( para_last->m_p2next )
        para_last->m_p2next->m_p2prev = para->m_p2prev;
    if( para == m_p2para_1st )
        set_paragraph_1st( para_last->m_p2next );
    if( para_last == m_p2para_last )
        m_p2para_last = para->m_p2prev;

    para->m_p2prev = nullptr;
    para_last->m_p2next = nullptr;

    if( !m_p2para_1st )
        add_paragraph_before( "", nullptr );
    else // add_paragraph_before() updates the dates
    {
        update_date_edited();
        update_inline_dates();
    }
}

void
Entry::do_for_each_para( const FuncParagraph& process_para )
{
    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
        process_para( p );
}

void
Entry::do_for_each_para( const FuncParagraph& process_para ) const
{
    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
        process_para( p );
}

Paragraph*
Entry::move_paras_up( Paragraph* para_bgn, Paragraph* para_end )
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    Paragraph* para_prev{ para_bgn->get_prev_visible() };

    if( !para_prev ) return nullptr;

    for( Paragraph* p = para_bgn; p; p = p->m_p2next )
    {
        p->set_visible( true );
        m_F_map_path_old = ( m_F_map_path_old || p->has_property( PROP::LOCATION ) );
        if( p == para_end ) break;
    }

    // move collapsed paragraphs together with their children:
    if( !para_end->is_expanded() )
        para_end = para_end->get_sub_last_invisible();

    remove_paragraphs( para_bgn, para_end );
    add_paragraphs_after( para_bgn, para_prev->m_p2prev );

    return para_prev->m_p2prev;
}

Paragraph*
Entry::move_paras_down( Paragraph* para_bgn, Paragraph* para_end )
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    Paragraph* para_next{ para_end->get_next_visible() };

    if( !para_next ) return nullptr;

    for( Paragraph* p = para_bgn; p; p = p->m_p2next )
    {
        p->set_visible( true );
        m_F_map_path_old = ( m_F_map_path_old || p->has_property( PROP::LOCATION ) );
        if( p == para_end ) break;
    }

    // move collapsed paragraphs together with their children:
    if( !para_end->is_expanded() )
        para_end = para_end->get_sub_last_invisible();

    // skip hidden sub-paragraphs:
    if( !para_next->is_expanded() && para_next->has_subs() )
        para_next = para_next->get_sub_last();

    remove_paragraphs( para_bgn, para_end );
    add_paragraphs_after( para_bgn, para_next );

    return para_next;
}

void
Entry::move_para_before( Paragraph* para2move, Paragraph* para_after )
{
    // remove the paragraph:
    if( para2move->m_p2prev )
        para2move->m_p2prev->m_p2next = para2move->m_p2next;
    if( para2move->m_p2next )
        para2move->m_p2next->m_p2prev = para2move->m_p2prev;

    // re-add the paragraph in the new location:
    if( para_after->m_p2prev )
    {
        para_after->m_p2prev->m_p2next = para2move;
        para2move->m_p2prev = para_after->m_p2prev;
    }
    else
    {
        m_p2para_1st = para2move;
        para2move->m_p2prev = nullptr;
    }
    para_after->m_p2prev = para2move;
    para2move->m_p2next = para_after;

    m_F_map_path_old = ( m_F_map_path_old || para2move->has_property( PROP::LOCATION ) );

    // DATE UPDATES
    update_date_edited();
}

Ustring
Entry::get_info_str() const
{
    Ustring str { STR::compose( _( "Created" ),  ":    <b>", get_date_created_str(), "</b>\n",
                                _( "Edited" ),   ":    <b>", get_date_edited_str(), "</b>" ) };
    if( m_date != m_date_finish )
        str += STR::compose( "\n", _( "Duration" ), ":    <b>",
                             Date::get_duration_str( m_date, m_date_finish ),
                             "</b>" );

    return str;
}

void
Entry::update_inline_dates( Paragraph* p_bgn, Paragraph* p_end )
{
    auto date_prev{ Date::isolate_YMD( m_date ) };

    if( !p_end ) p_end = ( p_bgn ? p_bgn : m_p2para_last );
    if( !p_bgn ) p_bgn = m_p2para_1st;

    m_date          = Date::LATEST;
    m_date_finish   = Date::NOT_SET;

    for( Paragraph* p = p_bgn; p; p = p->m_p2next )
    {
        if( p->has_date() )
        {
            if( p->get_date() < m_date )        m_date = p->get_date();
            if( p->get_date() > m_date_finish ) m_date_finish = p->get_date();
        }
        if( p->has_date_finish() )
        {
            // shouldn't be necessary as the end date cannot be earlier than begin date:
            //if( p->get_date_finish() < m_date )        m_date = p->get_date_finish();
            if( p->get_date_finish() > m_date_finish ) m_date_finish = p->get_date_finish();
        }

        if( p == p_end ) break;
    }

    if( m_date == Date::LATEST )
        m_date = m_date_created;

    if( m_date_finish == Date::NOT_SET )
        m_date_finish = m_date;

    // DEID_OMIT is used for temoorary entries that are not part of the diary
    if( m_id != DEID::OMIT && Date::isolate_YMD( m_date ) != date_prev && m_p2diary )
        m_p2diary->set_entry_date( this, m_date );
}

void
Entry::update_sibling_orders()
{
    m_sibling_order = ( m_p2prev ? m_p2prev->m_sibling_order + 1 : 1 );
    for( Entry* e = m_p2next; e; e = e->m_p2next )
        e->m_sibling_order = ( e->m_p2prev->m_sibling_order + 1 );
}

String
Entry::get_lang_final() const
{
    const auto lang { m_properties.get< String >( PROP::LANGUAGE, "!" ) };
    if( lang == get_sstr( CSTR::OFF ) ) return "";
    else if( lang == "!" )              return( m_p2diary ? m_p2diary->get_lang() : "" );
    else                                return lang;
}

const Theme*
Entry::get_theme() const
{
    return( m_p2theme ? m_p2theme : ThemeSystem::get() );
}

bool
Entry::has_tag( const DiaryElemTag* tag ) const
{
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
    {
        if( para->has_tag( tag ) )
            return true;
    }

    return false;
}

bool
Entry::has_tag_broad( const DiaryElemTag* tag, bool F_consider_parents ) const
{
    for( const Entry* e = this; e; e = e->get_parent() )
    {
        for( Paragraph* para = e->m_p2para_1st; para; para = para->m_p2next )
        {
            if( para->has_tag_broad( tag, false ) )
                return true;
        }

        if( !F_consider_parents ) break; // only run for this and break
    }

    return false;
}

Value
Entry::get_tag_value( const DiaryElemTag* tag, bool f_average ) const
{
    Value   value{ 0.0 };
    int     count{ 0 };
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        value += para->get_tag_value( tag, count );

    return( f_average ? value/count : value );
}

Value
Entry::get_tag_value_planned( const DiaryElemTag* tag, bool f_average ) const
{
    Value   value{ 0.0 };
    int     count{ 0 };
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        value += para->get_tag_value_planned( tag, count );

    return( f_average ? value/count : value );
}

Value
Entry::get_tag_value_remaining( const DiaryElemTag* tag, bool f_average ) const
{
    Value   value{ 0.0 };
    int     count{ 0 };
    for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        value += para->get_tag_value_remaining( tag, count );

    return( f_average ? value/count : value );
}

DiaryElemTag*
Entry::get_sub_tag_first( const DiaryElemTag* tag ) const
{
    if( tag != nullptr )
    {
        for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        {
            auto sub_tag{ para->get_subtag_first( tag ) };
            if( sub_tag )
                return sub_tag;
        }
    }

    return nullptr;
}

DiaryElemTag*
Entry::get_sub_tag_last( const DiaryElemTag* tag ) const
{
    if( tag != nullptr )
    {
        for( Paragraph* para = m_p2para_last; para; para = para->m_p2prev )
        {
            auto sub_tag{ para->get_subtag_last( tag ) };
            if( sub_tag )
                return sub_tag;
        }
    }

    return nullptr;
}

DiaryElemTag*
Entry::get_sub_tag_lowest( const DiaryElemTag* tag ) const
{
    DiaryElemTag* sub_tag_lowest{ nullptr };

    if( tag != nullptr )
    {
        for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        {
            auto sub_tag{ para->get_subtag_lowest( tag ) };
            if( sub_tag )
            {
                if( sub_tag_lowest )
                {
                    if( sub_tag->get_sibling_order() < sub_tag_lowest->get_sibling_order() )
                        sub_tag_lowest = sub_tag;
                }
                else
                    sub_tag_lowest = sub_tag;
            }
        }
    }

    return sub_tag_lowest;
}

DiaryElemTag*
Entry::get_sub_tag_highest( const DiaryElemTag* tag ) const
{
    DiaryElemTag* sub_tag_highest{ nullptr };

    if( tag != nullptr )
    {
        for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        {
            auto sub_tag{ para->get_subtag_highest( tag ) };
            if( sub_tag )
            {
                if( sub_tag_highest )
                {
                    if( sub_tag->get_sibling_order() > sub_tag_highest->get_sibling_order() )
                        sub_tag_highest = sub_tag;
                }
                else
                    sub_tag_highest = sub_tag;
            }
        }
    }

    return sub_tag_highest;
}

ListTags
Entry::get_sub_tags( const DiaryElemTag* tag ) const
{
    ListTags sub_tags;

    if( tag != nullptr )
    {
        for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
        {
            auto&& para_tags{ para->get_sub_tags( tag ) };
            sub_tags.splice( sub_tags.end(), para_tags );
        }
    }

    return sub_tags;
}

void
Entry::add_tag( DiaryElemTag* tag, Value value )
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    if( is_empty() )
        add_paragraph_before( "", nullptr );

    add_paragraph_before( tag->get_name(), nullptr )->add_format_tag( tag, 0 );

    if( value != 1.0 )
        m_p2para_last->append( STR::compose( value ),
                               m_p2diary ? &m_p2diary->m_parser_bg : nullptr );
}

String
Entry::get_script_bounds( Paragraph*& p_bgn, Paragraph*& p_end ) const
{
    // starts from p_bgn if set; sets p_bgn  and p_end to point to boundaries:
    Paragraph* p{ p_bgn ? p_bgn : m_p2para_1st };

    p_bgn = p_end = nullptr;

    for( ; p; p = p->m_p2next )
    {
        if( p->get_quot_type() == VT::QT::PYTHON::C )
        {
            if( !p_bgn ) p_bgn = p;
        }
        else if( p_bgn )
            break;
    }
    if( p_bgn )
    {
        p_end = ( p ? p->m_p2prev : m_p2para_last );
        return get_text_partial_code( p_bgn, p_end );
    }
    else
        return "";
}

// LOCATION
void
Entry::update_map_path() const
{
    m_map_path.clear();

    for( Paragraph* p = m_p2para_1st; p; p = p->m_p2next )
    {
        if( p->has_property( PROP::LOCATION ) )
            m_map_path.push_back( p );
    }

    m_F_map_path_old = false;
}

double
Entry::get_map_path_length() const
{
    double dist{ 0.0 };
    Coords pt_prev;
    bool   F_after_first{ false };

    if( m_F_map_path_old ) update_map_path();

    for( auto& para : m_map_path )
    {
        if( F_after_first )
            dist += Coords::get_distance( pt_prev, *para->get_location() );
        else
            F_after_first = true;

        pt_prev = *para->get_location();
    }

    return( Lifeograph::settings.use_imperial_units ? dist / LoG::MI_TO_KM_RATIO : dist );
}

void
Entry::clear_map_path()
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    if( m_F_map_path_old ) update_map_path();

    for( Paragraph* para : m_map_path )
        remove_paragraphs( para );

    m_map_path.clear();
}

Paragraph*
Entry::add_map_path_point( double lat, double lon, Paragraph* para_ref, bool F_after )
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    Paragraph* para { nullptr };

    if( !para_ref && has_location() )
        para_ref = m_map_path.back();

    if( para_ref && F_after )
        para_ref = para_ref->m_p2next;

    para = add_paragraph_before( "", para_ref );
    para->set_location( lat, lon );

    // update_map_path();
    m_F_map_path_old = true;

    return para;
}

void
Entry::remove_map_path_point( Paragraph* para )
{
    // NOTE: does manipulation through other functions. so, does not update edit date

    remove_paragraphs( para );
    m_F_map_path_old = true;
}

// ENTRY SET =======================================================================================
PoolEntries::~PoolEntries()
{
    for( iterator iter = begin(); iter != end(); ++iter )
        delete iter->second;
}

void
PoolEntries::clear()
{
    for( iterator iter = begin(); iter != end(); ++iter )
        delete iter->second;

    std::multimap< DateV, Entry*, FuncCompareDates >::clear();
}
