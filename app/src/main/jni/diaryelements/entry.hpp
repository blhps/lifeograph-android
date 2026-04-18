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


#ifndef LIFEOGRAPH_ENTRY_HEADER
#define LIFEOGRAPH_ENTRY_HEADER


#include "../undo.hpp"
#include "diarydata.hpp"
#include "paragraph.hpp"


namespace LoG
{

using FuncVoidEntry             = std::function< void( LoG::Entry* ) >;
using SignalVoidEntry           = sigc::signal< void( LoG::Entry* ) >;
using SignalVoidTag             = sigc::signal< void( LoG::DiaryElemTag* ) >;
using SignalVoidEntryInt        = sigc::signal< void( LoG::Entry*, int ) >;
using EntryComparer             = std::function< int64_t( LoG::Entry*, LoG::Entry* ) > ;

class Entry;

// UNDO ============================================================================================
class UndoEdit : public Undoable
{
    public:
        static constexpr int    UNDO_MERGE_TIMEOUT = 3;

                                        UndoEdit( UndoableType, Entry*, Paragraph*, int, int, int );
                                        ~UndoEdit();

        bool                            can_absorb( const Undoable* ) const override;
        void                            absorb( Undoable* ) override;
        static bool                     s_F_force_absorb;

        void                            set_n_paras_after( int n_paras )
        { m_n_paras_after = n_paras; }

#ifdef LIFEOGRAPH_DEBUG_BUILD
        void                            print_debug_info() override
        {
            using namespace HELPERS;
            PRINT_DEBUG( "n: ", get_name(), " | time: ", m_time,
                         " | id_p_bfr: ", m_id_para_before.get_raw(),
                         " | orig_p_1st: ", m_p2original_para_1st->get_text(),
                         " | orig_p_id: ", m_p2original_para_1st->get_id().get_raw() ); }
#endif

        //Ustring                         m_name;
        Entry*                          m_p2entry;
        D::DEID                         m_id_para_before; // UNSET if the first para
        Paragraph*                      m_p2original_para_1st;  // chain of original paragraphs
        int                             m_n_paras_before; // length of the original para chain
        int                             m_n_paras_after           { -1 };
        int                             m_offset_cursor_before    { -1 };
        int                             m_offset_cursor_after     { -1 };
        bool                            m_F_inhibit_para_deletion { false };
        // NOTE: before/afters above mean before/after the operation the undo is about
        // NOT BEFORE/AFTER UNDO!

    protected:
        Undoable*                       execute() override;
};

// ENTRY ===========================================================================================
class Entry : public DiaryElemTag
{
    public:
#ifndef __ANDROID__
        using GValue = Glib::Value< LoG::Entry* >;
#endif

                                Entry( Diary* const,
                                       const DateV,
                                       ElemStatus = ES::ENTRY_DEFAULT );
                                ~Entry() { clear_text(); }

        SKVVec                  get_as_skvvec() const override;

        // HIERARCHY
        Entry*                  get_parent() const    { return m_p2parent; }
        DiaryElemTag*           get_parent_tag() const override { return m_p2parent; }
        Entry*                  get_parent_unfiltered( FiltererContainer* fc ) const;
        Entry*                  get_child_1st() const { return m_p2child_1st; }
        Entry*                  get_child_last() const
        { return( m_p2child_1st ? m_p2child_1st->get_sibling_last() : nullptr ); }
        Entry*                  get_prev() const      { return m_p2prev; }
        Entry*                  get_prev_or_up() const
        { return( m_p2prev ? m_p2prev : m_p2parent ); }
        Entry*                  get_next() const      { return m_p2next; }
        Entry*                  get_prev_unfiltered( FiltererContainer* fc ) const;
        Entry*                  get_prev_straight( bool = true ) const;
        Entry*                  get_next_straight( bool = true ) const;
        Entry*                  get_next_straight( const Entry*, bool = true ) const;
        Entry*                  get_sibling_tag_prev( bool F_cyclic ) override
        { return( m_p2prev ? m_p2prev : ( ( m_p2next && F_cyclic ) ? get_sibling_last()
                                                                   : nullptr ) ); }
        DiaryElemTag*           get_sibling_tag_next( bool F_cyclic ) override
        { return( m_p2next ? m_p2next : ( ( m_p2prev && F_cyclic ) ? get_sibling_1st()
                                                                   : nullptr ) ); }
        Entry*                  get_sibling_1st();
        DiaryElemTag*           get_sibling_tag_1st() override
        { return get_sibling_1st(); }
        DiaryElemTag*           get_sibling_tag_last() override
        { return get_sibling_last(); }
        Entry*                  get_sibling_last();
        bool                    is_descendant_of( const DiaryElemTag* ) const override;
        void                    set_parent( Entry* );
        void                    add_child_1st( Entry* );
        void                    add_child_last( Entry* );
        void                    add_sibling_before( Entry* );
        void                    add_sibling_after( Entry* );
        void                    add_sibling_chain_after( Entry* );
        void                    append_entry_as_paras( Entry* );
        static int64_t          compare_names( Entry* e1, Entry* e2 )
        { return( e1->m_name.compare( e2->m_name ) ); }
        static int64_t          compare_dates( Entry* e1, Entry* e2 )
        { return( e1->m_date - e2->m_date ); }
        static int64_t          compare_sizes( Entry* e1, Entry* e2 )
        { return( e1->get_size() - e2->get_size() ); }

        void                    do_for_each_descendant( const FuncVoidEntry& );

        bool                    has_children() const
        { return( m_p2child_1st != nullptr ); }
        int                     get_child_count() const;
        VecEntries              get_descendants() const;
        VecTags                 get_descendant_tags() const override;
        int                     get_generation() const;
        int                     get_descendant_depth() const;
        bool                    is_expanded() const override
        { return( m_p2child_1st && DiaryElemTag::is_expanded() ); }

        // if not under a collapsed parent:
        bool                    is_exposed_in_list() const
        {
            return( m_p2parent ? ( m_p2parent->is_expanded() ? m_p2parent->is_exposed_in_list()
                                                             : false )
                               : true );
        }
        // get the first exposed entry in the hierarchical chain upwards:
        const Entry*            get_first_exposed_upwards() const
        {
            for( const Entry* e = this; ; e = e->m_p2parent )
            {
                // if its parent is expanded or nonexistent it is exposed:
                if( !e->m_p2parent || e->m_p2parent->is_expanded() )
                    return e;
            }
        }
        // non-const version:
        Entry*                  get_first_exposed_upwards()
        {
            return const_cast< Entry* >(
                    const_cast< const Entry* >( this )->get_first_exposed_upwards() );
        }

        // FILTERING
        bool                    is_filtered_out() const
        { return( m_status & ES::FILTERED_OUT ); }
        char                    is_filtered_out_completely() const
        {
            // NOTE: the return value { h, o, _ } is directly used in the diary file
            if( m_status & ES::FILTERED_OUT )
                return( bool( m_status & ES::HAS_VSBL_DESCENDANT ) ? 'h' : 'o' );
            else
                return( '_' );
        }
        void                    set_filtered_out( bool filteredout )
        {
            // for the following to work always call this function in a parent-to-child order:
            m_status &= ~ES::HAS_VSBL_DESCENDANT;

            if( filteredout )
            {
                m_status |= ES::FILTERED_OUT;
            }
            else
            {
                for( auto parent = get_parent(); parent; parent = parent->get_parent() )
                {
                    if( parent->is_filtered_out_completely() == 'o' )
                        parent->m_status |= ES::HAS_VSBL_DESCENDANT;
                    else
                        break;
                }
                if( m_status & ES::FILTERED_OUT )
                    m_status &= ~ES::FILTERED_OUT;
            }
        }
        void                    set_filtered_out( bool filteredout, bool has_visible_children )
        {
            if( filteredout )
                m_status |= ES::FILTERED_OUT;
            else if( m_status & ES::FILTERED_OUT )
                m_status &= ~ES::FILTERED_OUT;

            if( has_visible_children )
                m_status |= ES::HAS_VSBL_DESCENDANT;
            else if( m_status & ES::HAS_VSBL_DESCENDANT )
                m_status &= ~ES::HAS_VSBL_DESCENDANT;
        }

        // LOCKEDNESS
        bool                    is_locked_raw() const
        { return m_properties.has( PROP::LOCKED ); }
        bool                    is_locked() const
        { return( is_trashed() || m_properties.has( PROP::LOCKED ) ); }
        void                    set_locked( bool F_locked )
        { m_properties.set( PROP::LOCKED, F_locked ); }

        // POPUP NOTE
        bool                    is_popup_note() const
        { return m_properties.has( PROP::POPUP_NOTE ); }
        void                    set_popup_note( bool F_popup )
        { m_properties.set( PROP::POPUP_NOTE, F_popup ); }
        void                    get_popup_size( int& w, int& h ) const
        {
            w = m_properties.get< int >( PROP::WIDTH, -1 );
            h = m_properties.get< int >( PROP::HEIGHT, -1 );
        }
        void                    store_popup_size( int w, int h )
        {
            m_properties.set< int >( PROP::WIDTH, w );
            m_properties.set< int >( PROP::HEIGHT, h );
        }

        // TEXTUAL CONTENT
        UstringSize             translate_to_visible_pos( UstringSize ) const;

        bool                    is_empty() const
        {
            if( !m_p2para_1st )
                return true;
            else if( !m_p2para_1st->m_p2next && m_p2para_1st->is_empty() )
                return true;
            else
                return false;
        }
        Ustring                 get_text() const;
        Ustring                 get_text_visible( bool = true ) const;
        Ustring                 get_text_partial( Paragraph*, Paragraph*, bool = false ) const;
        String                  get_text_partial_code( Paragraph*, Paragraph* ) const;
        Ustring                 get_text_partial( const UstringSize, UstringSize,
                                                  bool = false ) const; // bool: decorated
        Paragraph*              get_text_partial_paras( const UstringSize,
                                                        const UstringSize ) const;
        FormattedText           get_formatted_text( UstringSize, UstringSize ) const;
        void                    clear_text();
        void                    set_text( const Ustring&, ParserBackGround* );
        void                    insert_text( UstringSize, const Ustring&, const ListHiddenFormats*,
                                             ParaInhClasses, bool );
        void                    insert_text( UstringSize pos, const Ustring& text,
                                             ParaInhClasses ic )
        { insert_text( pos, text, nullptr, ic, false ); }
        void                    erase_text( const UstringSize, const UstringSize, bool );
        void                    replace_text_with_styles( UstringSize, UstringSize, Paragraph* );
        bool                    parse( ParserBackGround* );
        Paragraph*              beautify_text( Paragraph*, Paragraph*, ParserBackGround* );

        unsigned int            get_paragraph_count() const
        { return( m_p2para_last ? m_p2para_last->m_order_in_host + 1 : 0 ); }
        bool                    get_paragraph( UstringSize, Paragraph*&, UstringSize&, bool ) const;
        Paragraph*              get_paragraph( UstringSize, bool = false ) const;
        Paragraph*              get_paragraph_by_no( unsigned int ) const;
        Paragraph*              get_paragraph_1st() const
        { return m_p2para_1st; }
        void                    set_paragraph_1st( Paragraph* );
        Paragraph*              get_paragraph_last() const
        { return m_p2para_last; }
        Paragraph*              add_paragraph_before_set( const Ustring& text,
                                                          Paragraph* p_after = nullptr )
        { return add_paragraph_before( text, p_after, nullptr, ParaInhClass::SET_TEXT ); }
        Paragraph*              add_paragraph_before( const Ustring&, Paragraph*,
                                                      ParserBackGround* = nullptr,
                                                      ParaInhClasses = ParaInhClass::NONE );
        // NOTE: not sure if this is really useful
        // std::pair< Paragraph*, Paragraph* >
        //                         add_paragraph_before_multi( const Ustring&, Paragraph*,
        //                                                     ParserBackGround* = nullptr,
        //                                                     ParaInhClasses = ParaInhClass::NONE );
        Paragraph*              add_paragraphs_after( Paragraph* const, Paragraph* const,
                                                      ParaInhClasses = ParaInhClass::NONE );
        void                    remove_paragraphs( Paragraph*, Paragraph* = nullptr,
                                                   bool F_add_undo = false );
        void                    do_for_each_para( const FuncParagraph& );
        void                    do_for_each_para( const FuncParagraph& ) const;
        Paragraph*              move_paras_up( Paragraph*, Paragraph* );
        Paragraph*              move_paras_down( Paragraph*, Paragraph* );
        void                    move_para_before( Paragraph*, Paragraph* );

        Paragraph*              get_description_para() const // returns 2nd paragraph
        { return( m_p2para_1st ? m_p2para_1st->m_p2next : nullptr ); }
        Ustring                 get_description() const override // returns 2nd paragraph
        {
            Paragraph* pd{ get_description_para() };
            return( pd ? pd->get_text() : "" );
        }

        Ustring                 get_info_str() const;

        //Ustring                 get_date_status_str() const;
        void                    update_inline_dates( Paragraph* = nullptr, Paragraph* = nullptr );

        String                  get_number_str() const
        {
            String&& number{ std::to_string( m_sibling_order ) };
            for( Entry* ep = m_p2parent; ep != nullptr; ep = ep->m_p2parent )
            {
                number.insert( 0, STR::compose( ep->m_sibling_order, '.' ) );
            }
            return number;
        }
        std::list< int >        get_number_array() const
        {
            std::list< int > number;
            for( auto ep = this; ep != nullptr; ep = ep->m_p2parent )
            {
                number.push_front( ep->m_sibling_order );
            }
            return number;
        }
        void                    update_sibling_orders();
        int                     get_sibling_order() const override
        { return m_sibling_order; }

        int                     get_size() const override
        {
            int size{ 0 };
            for( Paragraph* para = m_p2para_1st; para; para = para->m_p2next )
                size += ( para->get_size() + 1 );

            return( size > 0 ? size - 1 : 0 );
        }
        int                     get_size_adv( char type ) const
        {
            switch( type )
            {
                default: return get_size();
                // case VT::SO::WORD_COUNT::C: return 0; // TODO: 3.2 or later
                case VT::SO::PARA_COUNT::C:
                    return( m_p2para_last ? ( m_p2para_last->m_order_in_host + 1 ) : 0 );
            }
        }

        virtual Type            get_type() const override
        { return ET_ENTRY; }

        virtual const R2Pixbuf& get_icon() const override;
        virtual const R2Pixbuf& get_icon32() const override;

        const R2Pixbuf&         get_icon_lock() const;

        bool                    has_name() const
        { return( m_p2para_1st && not( m_p2para_1st->is_empty() ) ); }
        Ustring                 get_name_pure() const
        { return( m_p2para_1st ? m_p2para_1st->get_text() : "" ); }
        void                    update_name();

        // NOTE: for some reason Glib::ustring::at() is rather slow
        // so, use std::string wherever possible

        bool                    update_todo_status();
        double                  get_completion() const;
        String                  get_completion_str() const;
        double                  get_completed() const;
        double                  get_workload() const;

        Ustring                 get_list_str() const override;
        Ustring                 get_ancestry_path() const override;

        bool                    is_favorite() const { return( m_status & ES::FAVORED ); }
        // void                    set_favored( bool favored )
        // {
        //     m_status -= ( m_status & ES::FILTER_FAVORED );
        //     m_status |= ( favored ? ES::FAVORED : ES::NOT_FAVORED );
        // }
        void                    toggle_favored();

        String                  get_lang_final() const override;

        bool                    is_trashed() const { return( m_status & ES::TRASHED ); }
        void                    set_trashed( bool trashed )
        {
            m_status -= ( m_status & ES::FILTER_TRASHED );
            m_status |= ( trashed ? ES::TRASHED : ES::NOT_TRASHED );
        }

        // TAGS
        bool                    has_tag( const DiaryElemTag* ) const;
        bool                    has_tag_broad( const DiaryElemTag*, bool ) const;
        Value                   get_tag_value( const DiaryElemTag*, bool ) const;
        Value                   get_tag_value_planned( const DiaryElemTag*, bool ) const;
        Value                   get_tag_value_remaining( const DiaryElemTag*, bool ) const;
        DiaryElemTag*           get_sub_tag_first( const DiaryElemTag* ) const;
        DiaryElemTag*           get_sub_tag_last( const DiaryElemTag* ) const;
        DiaryElemTag*           get_sub_tag_lowest( const DiaryElemTag* ) const;
        DiaryElemTag*           get_sub_tag_highest( const DiaryElemTag* ) const;
        ListTags                get_sub_tags( const DiaryElemTag* ) const override;
        void                    add_tag( DiaryElemTag*, Value = 1.0 );

        // THEMES
        void                    set_theme( const Theme* theme )
        { m_p2theme = theme; }
        const Theme*            get_theme() const;
        bool                    is_theme_set() const
        { return( m_p2theme != nullptr ); }
        bool                    has_theme( const Ustring& name ) const
        { return( get_theme()->get_name() == name ); }

        // OTHER OPTIONS
        int                     get_title_style() const
        { return( m_style & VT::ETS::FILTER ); } // no separate index func needed
        void                    set_title_style( const int ts )
        { m_style = ( ( m_style & ~VT::ETS::FILTER ) | ts ); }

        int                     get_comment_style() const
        { return( m_style & VT::CS::FILTER ); }
        void                    set_comment_style( const int cs )
        { m_style = ( ( m_style & ~VT::CS::FILTER ) | cs ); }

        // SCRIPTING
        String                  get_script_bounds( Paragraph*&, Paragraph*& ) const;
        bool                    registers_scripts() const
        { return( !is_trashed() && m_properties.has( PROP::REGISTER_SCRIPTS ) ); }
        bool                    registers_scripts_raw() const
        { return( m_properties.has( PROP::REGISTER_SCRIPTS ) ); }

        // LOCATION
        bool                    is_map_path_old() const
        { return m_F_map_path_old; }
        void                    update_map_path() const;
        bool                    has_location() const
        {
            if( m_F_map_path_old ) update_map_path();

            return( !m_map_path.empty() );
        }
        Coords*                 get_location() const
        {
            if( m_F_map_path_old ) update_map_path();

            if( m_map_path.empty() )
                return nullptr;
            else
                return m_map_path.front()->get_location();
        }
        Paragraph*              get_location_para() const
        {
            if( m_F_map_path_old ) update_map_path();

            if( m_map_path.empty() )
                return nullptr;
            else
                return m_map_path.front();
        }
        const ListLocations&    get_map_path() const
        {
            if( m_F_map_path_old ) update_map_path();

            return m_map_path;
        }
        void                    clear_map_path();
        Paragraph*              add_map_path_point( double, double, Paragraph*, bool = true );
        void                    remove_map_path_point( Paragraph* );
        double                  get_map_path_length() const;

        // REMEMBERING POSITIONS
        // double                  get_scroll_pos() const        { return m_scroll_pos; }
        // void                    set_scroll_pos( double pos )  { m_scroll_pos = pos; }
        int                     get_cursor_pos() const     { return m_cursor_pos; }
        void                    set_cursor_pos( int pos )  { m_cursor_pos = pos; }

        // UNDO/REDO
        UndoStack*              get_undo_stack()
        { return &m_session_edits; }
        UndoEdit*               add_undo_action( UndoableType ut, Paragraph* p_bgn, int n_paras,
                                                 int pos_before, int pos_after = -1 )
        {
            auto p2undo { new UndoEdit( ut, this, p_bgn ? p_bgn->m_p2prev : nullptr,
                                        n_paras,
                                        pos_before,
                                        pos_after >= 0 ? pos_after : pos_before ) };
            m_session_edits.add_action( p2undo );
            return p2undo;
        }

        // ON-THE-FLY VALUES
        double                  m_scroll_pos    { 0.0 };

    protected:
        void                    digest_text( const Ustring&, ParserBackGround* );

        int                     m_sibling_order { 1 };  // starts from 1
        Paragraph*              m_p2para_1st    { nullptr };
        Paragraph*              m_p2para_last   { nullptr };
        mutable ListLocations   m_map_path;
        mutable bool            m_F_map_path_old{ true };
        const Theme*            m_p2theme       { nullptr }; // nullptr means theme is not set
        int                     m_style         { VT::ETS::DATE_AND_NAME::I |
                                                  VT::CS::NORMAL::I };

        // HIERARCHY
        Entry*                  m_p2parent      { nullptr };
        Entry*                  m_p2child_1st   { nullptr };
        Entry*                  m_p2prev        { nullptr };
        Entry*                  m_p2next        { nullptr };

        // ON-THE-FLY VALUES
        int                     m_cursor_pos    { 0 };
        UndoStack               m_session_edits;

    private:
        void                    set_name( const Ustring& ) override
        { throw LoG::Error( "Illegal call: Entry::Set_name()"); }

    friend class Diary;
};

// MAIN ENTRY POOL =================================================================================
class PoolEntries : public std::multimap< DateV, Entry*, FuncCompareDates >
{
    public:
                                PoolEntries()
    :   std::multimap< DateV, Entry*, FuncCompareDates >( compare_dates ) {}
                                ~PoolEntries();

        void                    clear();
        void                    clear_but_keep()
        { std::multimap< DateV, Entry*, FuncCompareDates >::clear(); }
};

typedef PoolEntries::iterator               EntryIter;
typedef PoolEntries::reverse_iterator       EntryIterReverse;
typedef PoolEntries::const_iterator         EntryIterConst;
typedef PoolEntries::const_reverse_iterator EntryIterConstRev;

typedef std::multimap< Ustring, Entry*, FuncCmpStrings > PoolEntryNames;

// A SUBSET OF ENTRIES =============================================================================
class SetEntries : public std::set< Entry*, FuncCmpDiaryElemById >
{
    public:
        SetEntries() {}
        SetEntries( std::initializer_list< Entry* > list )
        : std::set< Entry*, FuncCmpDiaryElemById >( list ) {}

#ifndef __ANDROID__
        using GValue = Glib::Value< LoG::SetEntries* >;
#endif

        bool                has_entry( Entry* entry ) const
        { return( find( entry ) != end() ); }
};

using SetEntriestIter = SetEntries::iterator;

struct FuncCmpEntriesByOrder
{
    bool operator()( Entry* const& l, Entry* const& r ) const
    { return( l->get_number_str() < r->get_number_str() ); }
};

class EntrySelection : public std::set< Entry*, FuncCmpEntriesByOrder >
{
    public:
        EntrySelection() {}
        EntrySelection( std::initializer_list< Entry* > list )
        : std::set< Entry*, FuncCmpEntriesByOrder >( list ) {}

#ifndef __ANDROID__
        using GValue = Glib::Value< LoG::EntrySelection* >;
#endif

        bool                has_entry( Entry* entry ) const
        { return( find( entry ) != end() ); }
};

using EntrySelectionIter = EntrySelection::iterator;

} // end of namespace LoG

#endif
