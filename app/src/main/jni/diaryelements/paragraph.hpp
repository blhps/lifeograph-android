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


#ifndef LIFEOGRAPH_PARAGRAPH_HEADER
#define LIFEOGRAPH_PARAGRAPH_HEADER


#ifndef __ANDROID__
#include <gtkmm/treemodel.h>
#endif

#include <memory>
#include <set>
#include <vector>

#include "../helpers.hpp"  // i18n headers
#include "../settings.hpp"
#include "diarydata.hpp"

namespace LoG
{

using namespace HELPERS;

// FORWARD DECLARATIONS
class Entry;
class ParserBackGround;

struct HiddenFormat
{
    HiddenFormat( int t, const String& u, StringSize b, StringSize e )
    : type( t ), uri( u ), pos_bgn( b ), pos_end( e ) { }

    int         type;
    String      uri;
    uint64_t    ref_id { DEID::UNSET.get_raw() };
                // equals to link target id or host para id for eval links
    int64_t     var_i  { 0 }; // extra variable
    DateV       var_d  { 0 }; // extra variable
    StringSize  pos_bgn;
    StringSize  pos_end;

    static int  get_type_from_char( char c )
    {
        switch( c )
        {
            case 'B': return VT::HFT_BOLD;
            case 'I': return VT::HFT_ITALIC;
            case 'H': return VT::HFT_HIGHLIGHT;
            case 'S': return VT::HFT_STRIKETHRU;
            case 'U': return VT::HFT_UNDERLINE;
            case 'F': return VT::HFT_FADED;
            case 'C': return VT::HFT_SUBSCRIPT;
            case 'P': return VT::HFT_SUPERSCRIPT;
            case 'T': return VT::HFT_TAG;
            case 'L': return VT::HFT_LINK_URI;
            case 'E': return VT::HFT_LINK_EVAL;
            case 'D': return VT::HFT_LINK_ID;
        }
        return 0; // mostly to silence the compiler
    }

    Ustring   get_as_human_readable_str() const
    {
        Ustring str;

        switch( type )
        {
            case VT::HFT_BOLD:          str = _( "Bold" ); break;
            case VT::HFT_ITALIC:        str = _( "Italic" ); break;
            case VT::HFT_HIGHLIGHT:     str = _( "Highlight" ); break;
            case VT::HFT_STRIKETHRU:    str = _( "Strikethrough" ); break;
            case VT::HFT_UNDERLINE:     str = _( "Underline" ); break;
            case VT::HFT_SUBSCRIPT:     str = _( "Subscript" ); break;
            case VT::HFT_SUPERSCRIPT:   str = _( "Superscript" ); break;
            case VT::HFT_TAG:           str = STR::compose( _( "Tag to" ), ": ", ref_id ); break;
            case VT::HFT_LINK_URI:      str = STR::compose( _( "Link to URI" ), ": ", uri ); break;
            case VT::HFT_LINK_EVAL:     str = STR::compose( _( "Link to evaluated text" ) ); break;
            case VT::HFT_LINK_ID:       str = STR::compose( _( "Link to entry" ), ": ", ref_id );
                 break;
            default:                    str = "???"; break;
        }

        str += STR::compose( " @ ", pos_bgn, "..", pos_end );

        return str;
    }

    D::DEID
    get_id_lo() const { return D::DEIDF{ LoGID64( ref_id ) }.get_lo(); }
    D::DEID
    get_id_hi() const { return D::DEIDF{ LoGID64( ref_id ) }.get_hi(); }

    void
    set_id_lo( const LoGID& id ) { ref_id = LoGIDF{ id, get_id_hi() }.get_raw(); }
    void
    set_id_hi( const LoGID& id ) { ref_id = LoGIDF{ get_id_lo(), id }.get_raw(); }
};

struct FuncCmpFormats
{
    bool operator()( HiddenFormat* const& l, HiddenFormat* const& r )
    const { return( l->pos_bgn < r->pos_bgn ); }
};

using ListHiddenFormats = std::multiset< HiddenFormat*, FuncCmpFormats >;

struct FormattedText
{
    FormattedText()
    { }

    FormattedText( const Ustring& text, const ListHiddenFormats* formats )
    : m_text( text )
    {
        if( formats )
            for( auto& f : *formats ) m_formats.insert( new HiddenFormat( *f ) );
    }

    ~FormattedText()
    {
        for( auto& f : m_formats ) delete f;
    }

    // keep until we switch to C++20:
    static ListHiddenFormats::size_type erase_if( ListHiddenFormats& c,
                                                  std::function< bool( HiddenFormat* ) >&& pred )
    {
        auto old_size = c.size();
        for( auto first = c.begin(), last = c.end(); first != last; )
        {
            if ( pred( *first ) )
                first = c.erase( first );
            else
                ++first;
        }

        return old_size - c.size();
    }

    Ustring             m_text;
    ListHiddenFormats   m_formats;
};

// INHERITANCE CLASS ===============================================================================
enum class ParaInhClass : unsigned int
{
    QUOT_TYPE     = 1,
    LIST_TYPE     = 1 << 1,
    HEADING_LVL   = 1 << 2,
    INDENTATION   = 1 << 3,

    DEFAULT       = static_cast< unsigned int >( QUOT_TYPE ) |
                    static_cast< unsigned int >( LIST_TYPE ) |
                    static_cast< unsigned int >( INDENTATION ),

    // below are not inheritance classes, do not mix with others:
    NONE          = 1 << 20,
    SET_TEXT      = 1 << 21,
};

using ParaInhClasses = HELPERS::EnumFlags< ParaInhClass >;

// PARAGRAPH =======================================================================================
class Paragraph : public DiaryElemTag
{
    public:
        Paragraph( Entry*, const Ustring&, ParserBackGround* = nullptr );
        Paragraph( Paragraph*, Diary* = nullptr );
        ~Paragraph()
        {
            add_or_remove_ref_from_tags( false ); // has to come before clear_formats()
            clear_formats();
        }

        Paragraph*                  get_prev() const
        { return m_p2prev; }
        Paragraph*                  get_prev_visible() const;
        Paragraph*                  get_prev_sibling() const;
        Paragraph*                  get_next() const
        { return m_p2next; }
        Paragraph*                  get_next_visible() const;
        Paragraph*                  get_next_sibling() const;
        DiaryElemTag*               get_sibling_tag_prev( bool F_cyclic ) override
        {
            auto prev_sibl { get_prev_sibling() };
            if( !prev_sibl )
            {
                if( F_cyclic )
                {
                    auto sibl { get_sibling_tag_last() };
                    return( ( sibl && sibl != this ) ? sibl : nullptr );
                }
                else
                    return nullptr;
            }
            else
                return( prev_sibl->defines_tag() ? prev_sibl
                                                 : prev_sibl->get_sibling_tag_prev( F_cyclic ) );
        }
        DiaryElemTag*               get_sibling_tag_next( bool F_cyclic ) override
        {
            auto next_sibl { get_next_sibling() };
            if( !next_sibl )
            {
                if( F_cyclic )
                {
                    auto sibl { get_sibling_tag_1st() };
                    return( ( sibl && sibl != this ) ? sibl : nullptr );
                }
                else
                    return nullptr;
            }
            else
                return( next_sibl->defines_tag() ? next_sibl
                                                 : next_sibl->get_sibling_tag_next( F_cyclic ) );
        }

        DiaryElemTag*               get_sibling_tag_1st() override;
        DiaryElemTag*               get_sibling_tag_last() override;
        std::list< Paragraph* >     get_siblings() const;
        int                         get_sibling_order() const override;
        bool                        is_descendant_of( const DiaryElemTag* ) const override;
        VecTags                     get_descendant_tags() const override;
        Paragraph*                  get_parent() const;
        DiaryElemTag*               get_parent_tag() const override { return get_parent(); }
        Paragraph*                  get_nth_next( int );
        Paragraph*                  get_last() const;
        Paragraph*                  get_sub_last() const;
        Paragraph*                  get_sub_last_visible( bool = false ) const;
        Paragraph*                  get_sub_last_invisible() const;
        bool                        can_be_parent_of( const Paragraph* p_sub ) const
        {
            if( p_sub )
            {
                const auto ths_heading_lvl  { this->get_heading_level() };
                const auto sub_heading_lvl  { p_sub->get_heading_level() };

                // quotations cannot be parent of non-quptation paras:
                if( get_quot_type() != VT::QT::OFF::C &&
                    p_sub->get_quot_type() == VT::QT::OFF::C )    return false;
                if( sub_heading_lvl < ths_heading_lvl )           return true;
                if( sub_heading_lvl > ths_heading_lvl )           return false;
                // empty paras cannot be parent by virtue of indentation:
                if( STR::strip_spaces( m_text ).empty() )         return false;
                // empty paras are always sub unless they are the last one:
                if( STR::strip_spaces( p_sub->m_text ).empty() )  
                    return can_be_parent_of( p_sub->m_p2next );

                const auto ths_indent_lvl  { this->get_indentation_any() };
                const auto sub_indent_lvl  { p_sub->get_indentation_any() };
                if( sub_indent_lvl > ths_indent_lvl )             return true;
                // bulleted paras are assumed to be parents of plain paras of same indent level:
                if( sub_indent_lvl == ths_indent_lvl &&
                    p_sub->get_list_type() == 0 &&
                    this->get_list_type() == VT::PLS::BULLET::I ) return true;
            }

            return false;
        }
        bool                        has_subs() const
        { return( !is_title() && can_be_parent_of( m_p2next ) ); }

        int                         get_para_no() const
        { return m_order_in_host; }
        int                         get_para_no_visible() const
        { return( m_p2prev ? m_p2prev->get_para_no_visible() +
                             ( m_p2prev->is_visible() ? 1 : 0 ) : 0 ); }
        bool                        is_last_in_host() const;
        int                         get_bgn_offset_in_host() const; // visible only
        int                         get_bgn_offset_in_host_abs() const; // absolute
        int                         get_end_offset_in_host() const;
        void                        move_to_entry( Entry*, Paragraph* = nullptr );

        D::DEIDF                    get_id_full() const; // shadows DiaryElement implementation

        int                         get_list_order() const;
        String                      get_list_order_str( char = '-', bool = true ) const;
        String                      get_list_order_full() const;

        int                         get_code_line_order() const;

        Ustring                     get_description() const override
        { return STR::strip_spaces( m_text.substr( get_tag_bound() ) ); }
        Ustring                     get_ancestry_path() const override;
        Ustring                     get_name() const override;
        bool                        defines_tag() const
        { return has_property( PROP::TAG_END_POS ); }
        UstringSize                 get_tag_bound() const
        { return m_properties.get( PROP::TAG_END_POS, 0 ); }
        void                        set_tag_bound( int );
        void                        offset_tag_bound( int );
        bool                        get_tag_bound_changed()
        { return m_properties.has( PROP::TAG_END_POS_CHANGED ); }
        void                        unset_tag_bound_changed()
        { return m_properties.remove( PROP::TAG_END_POS_CHANGED ); }

        int                         get_size() const override
        { return m_text.length(); }
        int                         get_size_adv( char type ) const
        {
            switch( type )
            {
                default: return m_text.length();
                // case VT::SO::WORD_COUNT::C: return 0; // TODO: 3.2 or later
                case VT::SO::PARA_COUNT::C: return 1;
            }
        }
        int                         get_chain_char_count() const;
        int                         get_chain_para_count() const;
        DiaryElement::Type          get_type() const override
        { return ET_PARAGRAPH; }
        int                         get_chain_para_count_to( const Paragraph* other )
        { return( abs( other->m_order_in_host - m_order_in_host ) + 1 ); }

        // TEXTUAL CONTENTS
        bool                        is_empty() const
        { return m_text.empty(); }
        bool                        is_empty_completely() const
        { return( m_text.empty() && !is_list() ); }
        gunichar                    get_char( UstringSize i ) const
        { return m_text[ i ]; }
        const Ustring&              get_text() const
        { return m_text; }
        const std::string           get_text_std() const // for file output
        { return m_text; }
        String                      get_text_code() const;
        Ustring                     get_text_stripped( int ) const;
        Ustring                     get_text_decorated() const;
        Ustring                     get_substr( UstringSize i ) const
        { return m_text.substr( i ); }
        Ustring                     get_substr( UstringSize bgn, UstringSize end ) const
        { return( end > bgn ? m_text.substr( bgn, end - bgn ) : ( end == bgn ? "" : "XXX" ) ); }
        Paragraph*                  get_sub( UstringSize bgn, UstringSize end ) const;
        Ustring                     get_info_str() const
        {
            return STR::compose( _( "Created" ),  ":    <b>", get_date_created_str(), "</b>\n",
                                 _( "Edited" ),   ":    <b>", get_date_edited_str(), "</b>" );
        }

        void                        set_text( const Ustring& text, ParserBackGround*,
                                              bool F_add_undo = false );
        void                        append( Paragraph*, ParserBackGround* );
        void                        append( const Ustring& text, ParserBackGround* );
        HiddenFormat*               append( const Ustring& text, int, const String& uri );
        void                        insert_text( UstringSize, Paragraph*, ParserBackGround* );
        void                        insert_text( UstringSize, const Ustring&, ParserBackGround*,
                                                 bool F_add_undo = false );
        std::tuple< UstringSize, UstringSize, UstringSize >
                                    insert_text_with_spaces( UstringSize, Ustring,
                                                             ParserBackGround*,
                                                             bool = true, bool = false );
        void                        erase_text( UstringSize pos,
                                                UstringSize size,
                                                ParserBackGround* = nullptr,
                                                bool F_add_undo = false );
        void                        replace_text( UstringSize pos,
                                                  UstringSize size,
                                                  const Ustring& text,
                                                  ParserBackGround* = nullptr,
                                                  bool F_add_undo = false );

        void                        change_letter_cases( int, int, LetterCase );

        int                         predict_list_style_from_text();
        int                         predict_indent_from_text();

        void                        join_with_next();
        Paragraph*                  split_at( UstringSize, ParserBackGround* = nullptr );

        // STYLE
        void                        inherit_style_from( const Paragraph*, ParaInhClasses );

        // FOLDING
        void                        set_expanded( bool ) override;
        bool                        is_visible() const
        { return( m_style & VT::PS_VISIBLE ); }
        bool                        is_visible_recalculate() const;
        void                        reset_visibility()
        { set_visible( is_visible_recalculate() ); }
        void                        set_visible( bool F_force )
        {
            if( F_force ) m_style |= VT::PS_VISIBLE;
            else          m_style &= ~VT::PS_VISIBLE;
        }
        void                        make_accessible(); // expands collapsed parents recursively
        bool                        is_foldable() const
        {
            return( !m_text.empty() && has_subs() );
        }

        // COMMENT CHECKS
        bool                        has_hidden_comment() const;
        bool                        has_comment_to_bgn() const; // used in textview
        bool                        has_comment_to_end() const; // used in textview

        // HEADING
        int                         get_heading_level() const
        { return( is_title() ? VT::PHS::TITLE::I : ( m_style & VT::PHS::FILTER ) ); }
        void                        clear_heading_level()
        {
            m_style &= ~VT::PHS::FILTER;
            update_date_edited();
        }
        void                        set_heading_level( int type )
        {
            m_style = ( ( m_style & ~( VT::PHS::FILTER ) ) | ( type & VT::PHS::FILTER ) );
            update_date_edited();
        }
        void                        change_heading_level()
        {
            switch( get_heading_level() )
            {
                case VT::PHS::LARGE::I:   set_heading_level( VT::PHS::MEDIUM::I ); break;
                case VT::PHS::MEDIUM::I:  clear_heading_level(); break;
                default:                  set_heading_level( VT::PHS::LARGE::I ); break;
            }
        }
        bool                        is_title() const
        { return( !m_p2prev && !( m_style & VT::PS_REORDERED ) ); }
        bool                        is_heading() const // titles are excluded here
        { return( m_p2prev && ( m_style & VT::PHS::FILTER ) ); }

        // LISTS
        bool                        is_list() const
        { return( m_style & VT::PS_LIST_GEN ); }
        int                         get_list_type() const
        { return( m_style & VT::PS_FLT_LIST ); }
        void                        clear_list_type()
        {   m_style &= ~VT::PS_FLT_LIST;
            update_date_edited();
        }
        void                        set_list_type( int );

        // ALIGNMENT
        int                         get_alignment() const
        { return( m_style & VT::PA::FILTER ); }
        void                        set_alignment( int align )
        {
            m_style = ( ( m_style & ~VT::PA::FILTER ) | ( align & VT::PA::FILTER ) );
            update_date_edited();
        }
        Pango::Alignment            get_pango_alignment() const
        {
            switch( m_style & VT::PA::FILTER )
            {
                case VT::PA::CENTER::I: return Pango::Alignment::CENTER;
                case VT::PA::RIGHT::I:  return Pango::Alignment::RIGHT;
                //case VT::PA::LEFT::I:
                default:                return Pango::Alignment::LEFT;
            }
        }

        // TO-DO STATUS
        ElemStatus                  get_todo_status() const override
        {
            switch( m_style & VT::PS_FLT_LIST )
            {
                case VT::PLS::TODO::I:    return ES::TODO;
                case VT::PLS::PROGRS::I:  return ES::PROGRESSED;
                case VT::PLS::DONE::I:    return ES::DONE;
                case VT::PLS::CANCLD::I:  return ES::CANCELED;
                default:                  return ES::NOT_TODO;
            }
        }
        int                         get_todo_status_ps() const
        {
            const auto style { m_style & VT::PS_FLT_LIST };
            return( ( style & VT::PS_TODO_GEN ) ? style : 0 );
        }
        bool                        is_todo_status_forced() const
        { return( m_style & VT::PS_TODO_FORCED ); }
        void                        set_todo_status_forced( bool F_forced )
        {
            m_style = ( F_forced ? ( m_style | VT::PS_TODO_FORCED )
                                 : ( m_style & ~VT::PS_TODO_FORCED ) );
        }
        ElemStatus                  get_todo_status_effective() const = delete;
        void                        set_todo_status( ElemStatus ) = delete;

        // LANGUAGE
        String                      get_lang_final() const override;

        // HORIZONTAL RULE
        bool                        has_hrule() const
        { return( m_style & VT::PS_HRULE_0 ); }
        void                        set_hrule( bool F_hrule )
        {
            if( F_hrule ) m_style |= VT::PS_HRULE_0;
            else          m_style &= ~VT::PS_HRULE_0;
            update_date_edited();
        }

        // QUOTE
        char                        get_quot_type() const
        { return m_properties.get< char >( PROP::QUOT_TYPE, VT::QT::OFF::C ); }
        void                        set_quot_type( char );
        // below methods are specifically for literary quotes:
        bool                        is_quote() const
        { return( get_quot_type() == VT::QT::LITERARY::C ); }
        void                        set_quote( bool F_quote )
        {
            set_quot_type( F_quote ? VT::QT::LITERARY::C : VT::QT::OFF::C );
            update_date_edited();
        }
        // below methods are specifically for code quotes:
        bool                        is_code() const
        { return( VT::get_v< VT::QT, int, char >( get_quot_type() ) >= VT::QT::GENERIC::I ); }
        void                        get_code_block( Paragraph*&, Paragraph*& );

        int                         get_code_line_comment_bgn() const;
        void                        set_code_line_commented( bool );

        // OTHER PROPERTIES
        // URI (used by image paragraphs)
        String                      get_uri() const
        { return m_properties.get< String >( PROP::URI, "" ); }
        String                      get_uri_broad() const
        {
            Ustring uri { get_uri() };
            for( auto f : m_formats )
            {
                if( f->type == VT::HFT_LINK_URI )
                {
                    if( !uri.empty() ) uri += "; ";
                    uri += f->uri;
                }
            }
            return uri;
        }
        // String                      get_uri_unrel() const
        // { return m_p2diary->convert_rel_uri( m_uri ); }
        void                        set_uri( const String& uri )
        {
            if( uri.empty() )
                m_properties.remove( PROP::URI );
            else
                m_properties.set( PROP::URI, uri );
            update_date_edited();
        }

        // IMAGE
        bool                        is_image( int subtype = VT::PS_IMAGE ) const
        {
            return( bool( m_style & VT::PS_IMAGE ) &&
                    ( subtype == VT::PS_IMAGE || ( m_style & VT::PS_FLT_IMAGE ) == subtype ) );
        }
        void                        set_image_type( int type )
        {
            m_style &= ~VT::PS_FLT_IMAGE;
            m_style |= ( type & VT::PS_FLT_IMAGE ); // prevent setting any other style through this
            update_date_edited();
        }
        int                         get_image_size() const
        { return m_properties.get< int >( PROP::IMG_SIZE, 3 ); }
        void                        set_image_size( int size )
        {
            size = std::clamp( size, 0, 3 );
            m_properties.set( PROP::IMG_SIZE, size );
            update_date_edited();
        }
        R2Pixbuf                    get_image( int, const Pango::FontDescription& ) noexcept;

        // MAP LOCATION
        Coords*                     get_location()
        { return m_properties.get_or_create< Coords >( PROP::LOCATION ); }
        void                        set_location( double lat, double lon )
        {
            m_properties.set( PROP::LOCATION, std::make_unique< Coords >( lat, lon ) );
            update_date_edited();
        }

        // FORMATS
        void                        add_ref_to_tag( const D::DEID& );
        void                        remove_ref_from_tag( const D::DEID& );
        void                        add_or_remove_ref_from_tags( bool );

        void                        clear_formats();
        HiddenFormat*               add_format( int, const String&, UstringSize, UstringSize );
        HiddenFormat*               add_format( HiddenFormat*, int );
        void                        insert_format( HiddenFormat* );
        HiddenFormat*               add_link( LoGID id, UstringSize b, UstringSize e )
        {
            auto&& format{ add_format( VT::HFT_LINK_ID, "", b, e ) };
            format->set_id_lo( id );
            return format;
        }
        HiddenFormat*               add_link( const String& uri, UstringSize b, UstringSize e )
        {
            auto f { add_format( uri.empty() ? VT::HFT_LINK_EVAL : VT::HFT_LINK_URI,
                                        uri, b, e ) };
            if( f->type == VT::HFT_LINK_EVAL ) f->set_id_lo( m_id ); // store host id
            return f;
        }
        HiddenFormat*               add_format_tag( const DiaryElemTag*, UstringSize );
        void                        remove_format( int, UstringSize, UstringSize );
        void                        remove_format( const HiddenFormat* );
        void                        remove_onthefly_formats();
        void                        remove_formats_of_type( int );
        HiddenFormat*               get_format_at( int, UstringSize, UstringSize ) const;
        HiddenFormat*               get_format_at( int type, UstringSize pos ) const
        { return get_format_at( type, pos, pos + 1 ); }
        HiddenFormat*               get_format_oneof_at( const std::vector< int >&,
                                                         UstringSize, UstringSize ) const;
        HiddenFormat*               get_format_oneof_at( int, UstringSize ) const;
        LoGIDF                      get_format_id_for_sync( HiddenFormat* f )
        { return LoGIDF( m_id, LoGID32( DEID::FORMAT_BASE.get_raw()
                                        + ( f->pos_bgn << 7 ) // to get out of FILTER_CHARS range
                                        + ( f->type & VT::HFT_FILTER_CHARS ) ) );
        }

        // TAGS
        void                        clear_tags() // NOTE: does not update edit date
        {
            m_tags.clear(); m_tags_planned.clear(); m_tags_in_order.clear();
        }
        // const MapTags&              get_tags() const
        // { return m_tags; }
        void                        set_tag( const D::DEID&, Value );
        void                        set_tag( const D::DEID&, Value, Value );
        bool                        has_tag( const DiaryElemTag* ) const;
        bool                        has_tag_planned( const DiaryElemTag* ) const;
        bool                        has_tag_broad( const DiaryElemTag*, bool ) const; // in broad sense
        Value                       get_tag_value( const DiaryElemTag*, int& ) const;
        Value                       get_tag_value_planned( const DiaryElemTag*, int& ) const;
        Value                       get_tag_value_remaining( const DiaryElemTag*, int& ) const;
        //Value                       get_tag_value_completion( const Entry* ) const;
        // return which sub tag of a parent tag is present in the map
        DiaryElemTag*               get_subtag_first( const DiaryElemTag* ) const;
        DiaryElemTag*               get_subtag_last( const DiaryElemTag* ) const;
        DiaryElemTag*               get_subtag_lowest( const DiaryElemTag* ) const;
        DiaryElemTag*               get_subtag_highest( const DiaryElemTag* ) const;
        ListTags                    get_sub_tags( const DiaryElemTag* ) const override;

        // COMPLETION
        double                      get_completion() const;
        double                      get_completed() const;
        double                      get_workload() const;

        // DATE
        void                        set_date( DateV date )  { m_date = date; }
        DateV                       get_date_broad( bool = false ) const; // in broad sense
        DateV                       get_date_finish_broad( bool = false ) const; // in broad sense
        bool                        has_date() const
        { return( Date::isolate_YMD( m_date ) != 0 ); }
        bool                        has_date_finish() const
        { return( Date::isolate_YMD( m_date_finish ) != 0 ); }
        void                        add_date( DateV date )
        {
            if( Date::is_set( m_date ) && date > m_date && date > m_date_finish )
                m_date_finish = date;
            else if( !Date::is_set( m_date ) || date < m_date )
                m_date = date;
        }
        void                        add_time( DateV time )
        {
            if( Date::get_time( m_date ) ) // corner case: no way to distinguish 00:00 from NOT_SET
                Date::set_time( m_date_finish, time );
            else
                Date::set_time( m_date, time );
        }

        // PARA TYPE
        int                         get_para_type() const
        { return( is_title() ? VT::PHS::TITLE::I : ( m_style & VT::PS_FLT_TYPE ) ); }
        void                        reset_para()
        {
            clear_list_type();
            clear_heading_level();
            set_indent_level( 0 );
            update_date_edited();
        }
        void                        set_para_type_raw( int type )
        {
            m_style = ( ( m_style & ~VT::PS_FLT_TYPE ) | ( type & VT::PS_FLT_TYPE ) );
            update_date_edited();
        }
        void                        set_para_type2( int type );

        // INDENTATION
        int                         get_indent_level() const
        { return( m_style & VT::PS_FLT_INDENT ) >> 24; }

        bool                        set_indent_level( unsigned );
        bool                        indent();
        bool                        unindent();
        unsigned                    get_space_indent() const;
        void                        set_space_indent( const unsigned );
        int                         get_indentation_any() const
        { return( is_code() ? ( get_space_indent() / INDENT_SPACE_COUNT ) : get_indent_level() ); }
        void                        convert_indentation_type( char );

        int                         m_style{ VT::PS_DEFAULT };
        Entry*                      m_host;
        Paragraph*                  m_p2prev        { nullptr };
        Paragraph*                  m_p2next        { nullptr };
        int                         m_order_in_host { -1 };
        ListHiddenFormats           m_formats;

    protected:
        void                        update_formats( StringSize, int, int );

        Ustring                     m_text;
        MapTags                     m_tags;
        MapTags                     m_tags_planned;
        VecDEIDs                    m_tags_in_order;

    friend class Diary;
    friend class Entry;
    friend class ParserUpgrader;
};

struct FuncCmpParagraphs
{
    bool operator()( const Paragraph* l, const Paragraph* r ) const;
};

using SetParagraphs   = std::set< Paragraph*, FuncCmpParagraphs >;
using VecParagraphs   = std::vector< Paragraph* >;
using ListParagraphs  = std::list< Paragraph* >;
using FuncParagraph   = std::function< void( Paragraph* ) >;
using FuncParagraphs  = std::function< void( const ListParagraphs& ) >;

} // end of namespace LoG

#endif
