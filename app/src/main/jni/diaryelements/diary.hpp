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


#ifndef LIFEOGRAPH_DIARY_HEADER
#define LIFEOGRAPH_DIARY_HEADER


#include <string_view>
#include <thread>
#include <mutex>
#include <random>

#ifndef __ANDROID__
#include "glibmm/regex.h"
#endif

#include "../helpers.hpp" // i18n headers
#include "diarydata.hpp"
#include "paragraph.hpp"
#include "tableelem.hpp"
#include "filter.hpp"
#include "../parsers/parser_background.hpp"


namespace LoG
{
    bool compare_matches( HiddenFormat*, HiddenFormat* );

    constexpr char  DB_FILE_HEADER[]    = "LIFEOGRAPHDB";
    constexpr char  LOCK_SUFFIX[]       = ".~LOCK~";

class Diary : public DiaryElement, public PropertyContainer
{
    public:
        enum SetPathType { SPT_NORMAL, SPT_READ_ONLY, SPT_NEW };

        static constexpr int                        DB_FILE_VERSION_INT     { 3010 };
        static constexpr int                        DB_FILE_VERSION_INT_MIN { 1020 };
        static constexpr std::string::size_type     PASSPHRASE_MIN_SIZE     { 4 };

        static constexpr int                        MAX_SEARRCH_RESULTS     { 200 };

        static constexpr int                        LOGGED_OUT              { 0 };
        static constexpr int                        LOGGED_TIME_OUT         { 1 };
        static constexpr int                        LOGGED_IN_RO            { 10 };
        static constexpr int                        LOGGED_IN_EDIT          { 20 };

        using SetMatches = std::set< HiddenFormat*, decltype( &compare_matches ) >;

                                Diary();
                                ~Diary();

        LoGID64                 get_id_full() const = delete;

        SKVVec                  get_as_skvvec() const override;

        // MAIN FUNCTIONALITY
        int                     get_version() const
        { return m_read_version; }
        bool                    is_old() const
        { return( m_read_version < DB_FILE_VERSION_INT ); }
        bool                    is_encrypted() const
        { return( ! m_passphrase.empty() ); }
        bool                    is_open()
        { return( m_login_status >= LOGGED_IN_RO ); }
        bool                    is_ready()
        { return( is_open() && m_thread_postread_operations == nullptr ); }
        bool                    is_in_edit_mode() const
        { return( m_login_status == LOGGED_IN_EDIT ); }
        bool                    can_enter_edit_mode() const
        { return( m_F_read_only == false ); }
        bool                    is_logged_time_out() const
        { return( m_login_status == LOGGED_TIME_OUT ); }
        void                    set_timed_out()
        { m_login_status = LOGGED_TIME_OUT; }

        Result                  init_new( const std::string&, const std::string& = "" );
        virtual void            clear();

        // DIARYELEMENT INHERITED FUNCTIONS
        int                     get_size() const override
        { return m_entries.size(); }
        Type                    get_type() const override
        { return ET_DIARY; }

        R2Pixbuf                get_image_file( const String&, int );
        R2Pixbuf                get_image_chart( const String&, int,
                                                 const Pango::FontDescription& );
        R2Pixbuf                get_image_table( const String&, int, const Pango::FontDescription&,
                                                 bool );
        void                    clear_chart_and_table_images();
        void                    clear_table_images( const String& );

        Ustring                 get_list_str() const override
        { return STR::compose( "<b>", Glib::Markup::escape_text( m_name ), "</b>" ); }

        int                     get_time_span() const;

        // PASSPHRASE
        bool                    set_passphrase( const String& );
        void                    clear_passphrase();
        const String&           get_passphrase() const;
        bool                    compare_passphrase( const String& ) const;
        bool                    is_passphrase_set() const;

        // ID HANDLING
        DiaryElement*           get_element( D::DEID ) const;
        template< class T >
        T*                      get_element2( const D::DEID& id ) const
        {
            DiaryElement* elem{ get_element( id ) };
            return dynamic_cast< T* >( elem );
        }

        // rather than erasing the id from the map, we keep it to avoid the id being assigned...
        // ...to a new elem which may lead to an id clash on undo:
        void                    shelve_id( D::DEID id )
        { m_ids[ id ] = nullptr; }
        void                    reclaim_id_for_elem( DiaryElement* );
        // DiaryElement*           get_element_shelved( LoGID ) const;
        // void                    shelve_element( LoGID );
        // void                    unshelve_element( LoGID );

        D::DEID                 create_new_id( DiaryElement* element );
        void                    erase_id( D::DEID id )
        {
            m_ids.erase( id );
            // if( m_ids_shelved.find( id ) != m_ids_shelved.end() )
            //     m_ids_shelved.erase( id );
        }
        bool                    try_force_id( LoGID id )
        {
            if( m_ids.find( id ) != m_ids.end() ) return false;
            m_force_id = id;
            return true;
        }
        void                    set_force_id_allow_duplicate( D::DEID id )
        { m_force_id = id; }
        void                    update_id_elem( D::DEID id, DiaryElement* elem )
        { m_ids[ id ] = elem; }

        // OPTIONS
        bool                    get_boolean_option( int opt ) const
        { return( m_options & opt); }
        void                    set_boolean_option( int opt, bool F_true )
        { if( F_true ) m_options |= opt; else m_options &= ~opt; }
        void                    toggle_boolean_option( int opt )
        { set_boolean_option( opt, !get_boolean_option( opt ) ); }
        int                     get_opt_ext_panel_cur() const
        { return m_opt_ext_panel_cur; }
        void                    set_opt_ext_panel_cur( int opt )
        { m_opt_ext_panel_cur = opt; }

        String                  get_lang() const { return m_language; }
        void                    set_lang( const String& lang ) { m_language = lang; }

        // COMMON NETHODS FOR ALL ELEMENT TYPES
        template< class T >
        T*                      get_strdef_elem( const std::map< Ustring, T*, FuncCmpStrings >& map,
                                                 const Ustring& name ) const
        {
            auto&& it_filter{ map.find( name ) };
            return( it_filter ==  map.end() ? nullptr : it_filter->second );
        }
        template< class T >
        T*                      create_strdef_elem( MapUstringDiaryElem< T >& map,
                                                    const Ustring& name0,
                                                    const Ustring& def )
        {
            Ustring name{ create_unique_name_for_map( map, name0 ) };
            T* str_def_elem{ new T( this, name, def ) };
            map.emplace( name, str_def_elem );

            return str_def_elem;
        }
        template< class T >
        T*                      create_stock_strdef_elem( MapUstringDiaryElem< T >& map,
                                                          const D::DEID id,
                                                          const Ustring& name,
                                                          const Ustring& def )
        {
            m_force_id = DEID::STOCK_TABLE;
            T* str_def_elem { new T( this, name, def ) };
            map.emplace( name, str_def_elem );
            str_def_elem->set_status_flag( ES::STOCK, true );

            return str_def_elem;
        }

        template< class T >
        T*
        duplicate_strdef_elem( MapUstringDiaryElem< T >& map, const Ustring& name )
        {
            T* existing_elem{ get_strdef_elem( map, name ) };

            if( !existing_elem ) return nullptr;

            return create_strdef_elem( map, name, existing_elem->get_definition() );
        }

        template< class T, class M >
        bool
        rename_strdef_elem( M& map, T* elem, const Ustring& new_name )
        {
            if( new_name.empty() )
                return false;
            if( map.find( new_name ) != map.end() )
                return false;

            const Ustring old_name{ elem->get_name() };

            auto&& iter{ map.find( old_name ) };
            if( iter == map.end() || iter->second != elem )
                return false;

            elem->set_name( new_name );

            map.erase( old_name );
            map.emplace( new_name, elem );

            return true;
        }
        template< class T, class M >
        bool                    set_active_strdef_elem( const M& map, T*& ptr, const Ustring& name )
        {
            auto&& iter{ map.find( name ) };
            if( iter == map.end() )
                return false;

            ptr = iter->second;
            return true;
        }
        template< class T >
        bool                    dismiss_strdef_elem( MapUstringDiaryElem< T >& map,
                                                     MapUstringDiaryElem< T >* map_stock,
                                                     T*& ptr,
                                                     const Ustring& name )
        {
            auto&& iter{ map.find( name ) };

            if( iter == map.end() )
                return false;

            T* elem_to_delete{ iter->second };

            map.erase( name );

            if( name == ptr->get_name() )
                ptr = ( map_stock ? map_stock->begin()->second : map.begin()->second );

            delete elem_to_delete;

            return true;
        }
        template< class T, class M >
        bool                    import_strdef_elem( M& map, const T* elem_r, bool F_add )
        {
            if( F_add && !get_element( elem_r->get_id() ) )
                m_force_id = elem_r->get_id();

            if( F_add )
            {
                create_strdef_elem( map, elem_r->get_name(), elem_r->get_definition() );
            }
            else
            {
                auto elem_l{ dynamic_cast< T* >( get_element( elem_r->get_id() ) ) };

                if( elem_l == nullptr ) return false; // should never occur

                rename_strdef_elem( map, elem_l, elem_r->get_name() );
                elem_l->set_definition( elem_r->get_definition() );
            }

            return true;
        }

        // ENTRIES
        Entry*                  get_entry_1st() const
        { return m_p2entry_1st; }
        void                    set_entry_1st( Entry* e )
        { m_p2entry_1st = e; }
        // Entry*                  get_entry_last() const not needed for now
        // {
        //     Entry* entry_last = m_p2entry_1st;
        //     for( Entry* e = entry_last; e; e = e->get_next_straight() )
        //         entry_last = e;
        //     return entry_last;
        // }
        const PoolEntries&      get_entries() const
        { return m_entries; }
        const MapLoGIDTag&      get_tags() const
        { return m_cache_tags; }
        Entry*                  get_entry_today() const;
        Entry*                  get_startup_entry( bool = true ) const;
        Entry*                  get_home_entry() const
        { return get_entry_by_id( m_startup_entry_id ); }
        void                    set_startup_entry( const Entry* entry )
        { m_startup_entry_id = ( entry ? entry->get_id() : DEID::MOST_CURRENT_ENTRY ); }
        Entry*                  get_entry_most_current() const;
        Entry*                  get_prev_session_entry() const
        { return dynamic_cast< Entry* >( get_element( m_last_entry_id ) ); }
        void                    set_current_entry( const Entry* entry )
        { m_current_entry_id = entry->get_id(); }
        Entry*                  get_entry_by_id( const D::DEID id ) const
        { return get_element2< Entry >( id ); }
        Entry*                  get_entry_by_date( const DateV, bool=false ) const;
        VecEntries              get_entries_by_date( DateV, bool=false ) const;
        Entry*                  get_entry_by_name( const Ustring& ) const;
        Entry*                  get_entry_by_name_fuzzy( const Ustring& ) const;
        VecEntries              get_entries_by_filter( const Filter* ) const;
        VecEntries              get_entries_by_filter( const Ustring& ) const;
        unsigned int            get_entry_count_on_day( const DateV ) const;
        // Entry*                  get_entry_next_in_day( const Date& ) const;
        Entry*                  get_entry_first_untrashed() const;
        Entry*                  get_entry_latest() const; // get last temporal entry
        Ustring                 get_entry_name( D::DEID ) const;
        void                    set_entry_date( Entry*, DateV );
        void                    update_tag_refs( D::DEID, const Ustring& );
        void                    add_tag_to_cache( DiaryElemTag*, bool = true );
        void                    sort_entry_siblings( Entry*, EntryComparer&&, int );
        Entry*                  get_milestone_before( const DateV ) const;
        Entry*                  create_entry( Entry*, bool, DateV, const Ustring& = "",
                                              int = VT::ETS::DATE_AND_NAME::I );
        Entry*                  create_entry( DateV, bool, bool, bool );
        Entry*                  duplicate_entry( Entry* );
        // adds a new entry to today even if there is already one or more:
        Entry*                  create_entry_dated( Entry*, DateV, bool = false );
        Entry*                  create_entry_dummy();
        Entry*                  remove_entry_from_hierarchy( Entry* );
        void                    remove_entry_from_hierarchy_with_descendants( Entry* );
        void                    move_entry( Entry*, Entry*, const DropPosition& );
        void                    move_entries( const EntrySelection*, Entry*, const DropPosition& );
        Entry*                  dismiss_entry( Entry* );

        // bool                    is_first( const Entry* const entry ) const
        // { return( entry->is_equal_to( m_entries.begin()->second ) ); }
        // bool                    is_last( const Entry* const entry ) const
        // { return( entry->is_equal_to( m_entries.rbegin()->second ) ); }

        Paragraph*              get_paragraph_by_id( D::DEID id )
        { return get_element2< Paragraph >( id ); }

        DiaryElemTag*           get_tag_by_name( const Ustring& ) const;
        DiaryElemTag*           get_tag_by_name_fuzzy( const Ustring& ) const;
        DiaryElemTag*           get_tag_by_id( const D::DEID& id ) const
        { return get_element2< DiaryElemTag >( id ); }
        // TODO: 3.2: consider using the tag cache for this purpose

        bool                    is_trash_empty() const;

        bool                    has_completion_tag() const
        { return( m_completion_tag_id != DEID::UNSET ); }
        DiaryElemTag*           get_completion_tag() const
        { return get_tag_by_id( m_completion_tag_id ); }
        void                    set_completion_tag( D::DEID id )
        { m_completion_tag_id = id; }

        // SEARCHING
        void                    set_search_str( const Ustring& );
        void                    set_search_filter( const Filter* );
        const Filter*           get_search_filter() const { return m_p2filter_search; }
        void                    start_search( const int );
        void                    search_internal( int, int );
        void                    search_internal_entry( const Entry* );
        void                    stop_search()
        {
            std::lock_guard< std::mutex > lock( m_mutex_search );
            m_F_stop_search_thread = true;
        }
        void                    destroy_search_threads()
        {
            if( m_threads_search.empty() )
                return;

            for( auto thread : m_threads_search )
            {
                if( thread->joinable() )
                    thread->join();
                delete thread;
            }
            m_threads_search.clear();
        }
        void                    remove_entry_from_search( const Entry* );
        void                    update_search_for_entry( const Entry* );
        Ustring                 get_search_text() const
        { return m_search_text; }
        bool                    is_search_needed() const
        { return( !m_search_text.empty() ); }
        bool                    is_search_in_progress() const
        { return( !m_threads_search.empty() ); }

        void                    replace_match( const HiddenFormat*, const Ustring& );
        void                    replace_all_matches( const Ustring& );
        void                    replace_all_matches( const Ustring&, const Ustring& );
        SetMatches*             get_matches()
        {
            std::lock_guard< std::mutex > lock( m_mutex_search );
            return &m_matches;
        }
        unsigned                get_match_count() const
        { return m_matches.size(); }
        HiddenFormat*           get_match_at( int );
        void                    clear_matches();

        // FILTERS
        Filter*                 create_filter( const Ustring& name0,
                                               const Ustring& def = Filter::DEFINITION_MINIMAL )
        { return create_strdef_elem( m_filters, name0, def ); }
        Filter*                 duplicate_filter( const Ustring& name )
        { return duplicate_strdef_elem( m_filters, name ); }
        Filter*                 get_filter( const Ustring& name )
        { return get_strdef_elem( m_filters, name ); }
        Filter*                 get_filter( D::DEID id ) const
        { return get_element2< Filter >( id ); }
        const Filter*           get_filter_list() const
        { return m_p2filter_list; }
        FiltererContainer*      get_filter_list_stack() const
        { return( m_p2filter_list ? m_p2filter_list->get_filterer_stack() : nullptr ); }
        void                    set_filter_list( const Filter* filter )
        { m_p2filter_list = filter; }

        Filter*                 get_filter_nontrashed()
        { return m_filter_nontrashed; }
        Filter*                 get_filter_trashed()
        { return m_filter_trashed; }
        Filter*                 get_filter_cur_entry()
        { return m_filter_cur_entry; }
        Filter*                 get_filter_cur_entry_tree()
        { return m_filter_cur_entry_tree; }

        const MapUstringFilter&
                                get_filters() const
        { return m_filters; }
        MapUstringFilter*       get_p2filters()
        { return &m_filters; }

        bool                    rename_filter( Filter* elem, const Ustring& new_name )
        { return rename_strdef_elem( m_filters, elem, new_name ); }
        bool                    dismiss_filter( const Ustring& );
        void                    remove_entry_from_filters( Entry* );
        bool                    update_entry_filter_status( Entry* );
        int                     update_all_entries_filter_status();
        void                    update_filter_user_counts();

        // CHARTS
        ChartElem*              create_chart( const Ustring& name0, const Ustring& def )
        { return create_strdef_elem( m_charts, name0, def ); }
        ChartElem*              duplicate_chart( const Ustring& name )
        { return duplicate_strdef_elem( m_charts, name ); }
        ChartElem*              get_chart( const Ustring& name ) const
        { return get_strdef_elem( m_charts, name ); }
        ChartElem*              get_chart( D::DEID id ) const
        { return get_element2< ChartElem >( id ); }
        ChartElem*              get_chart_active() const
        { return m_p2chart_active; }
        bool                    set_chart_active( const Ustring& name )
        { return set_active_strdef_elem( m_charts, m_p2chart_active, name ); }
        ChartElem* const *      get_p2chart_active() const
        { return &m_p2chart_active; }
        const MapUstringChartElem&
                                get_charts() const
        { return m_charts; }
        MapUstringChartElem*    get_p2charts()
        { return &m_charts; }

        bool                    rename_chart( ChartElem* elem, const Ustring& new_name )
        { return rename_strdef_elem( m_charts, elem, new_name ); }
        bool                    dismiss_chart( const Ustring& name )
        { return dismiss_strdef_elem< ChartElem >( m_charts, nullptr, m_p2chart_active, name ); }

        // TABLES
        TableElem*              create_table( const Ustring& name0, const Ustring& def )
        { return create_strdef_elem( m_tables, name0, def ); }
        TableElem*              duplicate_table( const Ustring& name )
        { return duplicate_strdef_elem( m_tables, name ); }
        TableElem*              get_table( const Ustring& name ) const
        { return get_strdef_elem( m_tables, name ); }
        TableElem*              get_table( D::DEID id ) const
        { return get_element2< TableElem >( id ); }
        TableElem*              get_table_active() const
        { return m_p2table_active; }
        bool                    set_table_active( TableElem* p2table )
        {
            if( p2table && p2table->get_diary() == this )
            {
                m_p2table_active = p2table;
                return true;
            }
            else
                return false;
        }
        TableElem* const *      get_p2table_active() const
        { return &m_p2table_active; }
        const MapUstringTableElem&
                                get_tables() const
        { return m_tables; }
        MapUstringTableElem*    get_p2tables()
        { return &m_tables; }
        MapUstringTableElem*    get_p2tables_stock()
        { return &m_tables_stock; }

        bool                    rename_table( TableElem* elem, const Ustring& new_name )
        { return rename_strdef_elem( m_tables, elem, new_name ); }
        bool                    dismiss_table( TableElem* p2table )
        { return dismiss_strdef_elem( m_tables, &m_tables_stock, m_p2table_active,
                                      p2table->get_name() ); }

        // THEMES
        Theme*                  create_theme( const Ustring& name0 )
        { return create_strdef_elem( m_themes, name0, "" ); }
        Theme*                  get_theme( const Ustring& name )
        { return get_strdef_elem( m_themes, name ); }
        PoolThemes*             get_p2themes()
        { return &m_themes; }
        const PoolThemes*       get_p2themes() const
        { return &m_themes; }
        bool                    rename_theme( Theme* elem, const Ustring& new_name )
        { return rename_strdef_elem( m_themes, elem, new_name ); }
        void                    dismiss_theme( Theme* );

        // WORK DAYS
        void                    set_day_weeekend( int i_day, bool flag_is )
        { m_weekends[ i_day % 7 ] = flag_is; }
        bool                    is_day_weekend( int i_day ) const
        { return m_weekends[ i_day % 7 ]; }
        void                    set_day_holiday( DateV date, bool flag_is )
        {
            if( flag_is )
                m_holidays.insert( Date::isolate_MD( date ) );
            else
                m_holidays.erase( Date::isolate_MD( date ) );
        }
        bool                    is_day_holiday( DateV date ) const
        { return( m_holidays.find( Date::isolate_MD( date ) ) != m_holidays.end() ); }
        bool                    is_day_workday( DateV date ) const
        { return( m_weekends[ Date::get_weekday( date ) ] == 0 &&
                  m_holidays.find( Date::isolate_MD( date ) ) == m_holidays.end() ); }
        int                     calculate_work_days_between( const DateV, const DateV );

        // DISK I/O
        LoG::Result             set_path( const std::string&, SetPathType );
        const String&           get_uri() const
        { return m_uri; }
        String                  convert_rel_uri( String ) const;
        String                  relativize_uri( const String& ) const;
        String                  get_rel_folder_name() const
        { return( "[" + sanitize_file_path( get_name() ) + "]" ); }
        String                  get_rel_folder_path() const
        { return Glib::filename_from_uri( convert_rel_uri( "rel://" + get_rel_folder_name() ) ); }

        LoG::Result             enable_editing();

        LoG::Result             read_body();
        LoG::Result             read_header();

        LoG::Result             write();
        LoG::Result             write( const String& );
        LoG::Result             write_copy( const String&, const String&, const Filter* );
        LoG::Result             write_txt( const String&, const Filter* );

        bool                    remove_lock_if_necessary();
        bool                    is_locked() const;
        void                    set_continue_from_lock();

        // IMPORTING
        void                    synchronize_options( const Diary* );
        bool                    import_entry( const Entry*, Entry*, bool );
        bool                    import_theme( const Theme*, bool );
        bool                    import_filter( const Filter* elem, bool F_add )
        { return import_strdef_elem( m_filters, elem, F_add ); }
        bool                    import_chart( const ChartElem* elem, bool F_add )
        { return import_strdef_elem( m_charts, elem, F_add ); }
        bool                    import_table( const TableElem* elem, bool F_add )
        { return import_strdef_elem( m_tables, elem, F_add ); }

        const DiaryElement*     get_corresponding_elem( const DiaryElement* ) const;
        D::CSTR                 compare_foreign_elem( const DiaryElement*,
                                                      const DiaryElement*& ) const;

        static Diary*           d;

        std::random_device      m_random_device;  //seed
        std::mt19937            m_random_gen;

        Glib::Dispatcher        m_dispatcher_search;
        Glib::Dispatcher        m_dispatcher_postread_operations;

        ParserBackGround        m_parser_bg;
        // bg jobs need to have their own parsers:
        ParserBackGround*       m_parser_bg_postread    { nullptr };

    // ITERATOR IMPLEMENTATION TODO: 3.2: we may consider adding a const version
    class iterator
    {
            Entry* current;

        public:
            iterator( Entry* e ) : current( e ) {}

            Entry* operator*() const { return current; }

            iterator& operator++()
            {
                if( current )
                    current = current->get_next_straight();
                return *this;
            }

            bool operator!=( const iterator& other ) const { return current != other.current; }
    };

        iterator begin() const { return iterator( m_p2entry_1st ); }
        iterator end() const   { return iterator( nullptr ); }
        // NOTE: these do not exactly behave like const functions

    protected:
        // IDS (must be first)
        PoolLoGIDs              m_ids;
        // PoolDEIDs               m_ids_shelved;
        D::DEID                 m_force_id{ DEID::UNSET };
        // PROPERTIES / OPTIONS
        String                  m_uri;
        String                  m_passphrase; // not synced
        String                  m_language;
        D::DEID                 m_startup_entry_id              { DEID::MOST_CURRENT_ENTRY };
        D::DEID                 m_last_entry_id                 { DEID::UNSET }; // not synced
        D::DEID                 m_current_entry_id              { DEID::UNSET }; // not synced
        D::DEID                 m_completion_tag_id             { DEID::UNSET };
        int                     m_options                       { 0 };
        int                     m_opt_ext_panel_cur             { 1 }; // not synced
        // CONTENT
        Entry*                  m_p2entry_1st                   { nullptr };
        PoolEntries             m_entries;  // this is a multimap
        //PoolEntryNames          m_entry_names;

        PoolThemes              m_themes;

        MapUstringFilter        m_filters;
        Filter*                 m_filter_nontrashed     { nullptr };
        Filter*                 m_filter_trashed        { nullptr };
        Filter*                 m_filter_cur_entry      { nullptr };
        Filter*                 m_filter_cur_entry_tree { nullptr };
        const Filter*           m_p2filter_list         { nullptr };  // synced
        const Filter*           m_p2filter_save         { nullptr };

        MapUstringChartElem     m_charts;
        ChartElem*              m_p2chart_active{ nullptr };

        MapUstringTableElem     m_tables;
        MapUstringTableElem     m_tables_stock;
        TableElem*              m_p2table_active{ nullptr };

        // TODO: 3.2: sync the following, too:
        bool                    m_weekends[ 7 ] = { true, false, false, false, false, false, true };
        std::set< DateV >       m_holidays;

        // FLAGS
        int                     m_read_version                  { 0 };
        bool                    m_F_read_only                   { false };
        bool                    m_F_continue_from_lock          { false };
        // SEARCHING
        Ustring                 m_search_text;
        Glib::RefPtr< Glib::Regex >
                                m_search_regex;
        const Filter*           m_p2filter_search               { nullptr };  // synced
        FiltererContainer*      m_fc_search                     { nullptr };
        std::vector< std::thread* > m_threads_search;
        int                     m_active_search_thread_count    { 0 };

        SetMatches              m_matches;
        bool                    m_F_stop_search_thread          { false };
        mutable std::mutex      m_mutex_search;

        LoG::Result             decrypt_buffer();
        LoG::Result             parse_db_body_text();
        LoG::Result             parse_db_body_text_3000();
        LoG::Result             parse_db_body_text_2000();
        LoG::Result             parse_db_body_text_1050();

        void                    upgrade_to_1030();
        void                    upgrade_to_1050();
        bool                    upgrade_to_3000();
        void                    tmp_upgrade_ordinal_date_to_2000( DateV& );
        void                    tmp_upgrade_date_to_3000( DateV& );
        void                    tmp_upgrade_table_defs();

        void                    parse_themes( const std::string_view );
        void                    parse_theme_line( Theme*&, const String& );

        void                    do_standard_checks_after_read();
        void                    do_postread_operations();
        void                    stop_postread_operations()
        {
            std::lock_guard< std::mutex > lock( m_mutex_postread_operations );
            m_F_stop_postread_operations = true;
        }
        void                    handle_postread_operations_finished();
        bool                    m_F_stop_postread_operations{ false };
                                // 0: stopped, 1: to-be stopped, 2: in progress
        mutable std::mutex      m_mutex_postread_operations;
        std::thread*            m_thread_postread_operations{ nullptr };

        void                    create_db_entry_text( const Entry*, FiltererContainer* ); // helper
        void                    create_db_header_text( bool );
        bool                    create_db_body_text();

        // RUN_TIME POOL OF IMAGES FOR EMBEDS INTO ENTRIES
        MapPathPixbufs          m_map_images_f;
        MapPathPixbufs          m_map_images_c;
        MapPathPixbufs          m_map_images_t;
        // runtime pool of tags:
        MapLoGIDTag             m_cache_tags;

    private:
        size_t                  m_size_bytes      { 0 };
        StrStream*              m_sstream         { nullptr };
        // last top level entry to add siblings to when saving filetered entries only
        const Entry*            m_p2top_entry_last;

        int                     m_login_status    { LOGGED_OUT };

    friend class UIDiary;
    friend class DialogSync;
};

} // end of namespace LoG

#endif
