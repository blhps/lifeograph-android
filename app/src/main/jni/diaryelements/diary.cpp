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


#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <cstdio>   // for file operations
#include <string>
#include <sstream>
#include <iostream>
#include <gcrypt.h>
#include <chrono>

#include "diary.hpp"
#include "../strings.hpp"
#include "../helpers.hpp"
#include "../fuzzyfinder.hpp"
#include "../lifeograph.hpp"
#include "../parsers/parser_upgrader.hpp"
#ifndef __ANDROID__
#include "../widgets/chart_surface.hpp"
#include "../widgets/table_surface.hpp"
#endif


using namespace LoG;


// STATIC MEMBERS
Diary*                  Diary::d;
//bool                    Diary::s_flag_ignore_locks{ false };

// PARSING HELPERS
DateV
get_db_line_date( const Ustring& line )
{
    DateV date{ 0 };

    for( unsigned int i = 2;
         i < line.size() && i < 12 && int ( line[ i ] ) >= '0' && int ( line[ i ] ) <= '9';
         i++ )
    {
        date = ( date * 10 ) + int ( line[ i ] ) - '0';
    }

    return date;
}

Ustring
get_db_line_name( const Ustring& line )
{
    Ustring::size_type begin( line.find( '\t' ) );
    if( begin == std::string::npos )
        begin = 2;
    else
        begin++;

    return( line.substr( begin ) );
}

// MATCHES COMPARATOR
bool
LoG::compare_matches( HiddenFormat* l, HiddenFormat* r )
{
    if( l->var_d == r->var_d )
    {
        if( ( l->ref_id & 0xFFFFFFFF ) == ( r->ref_id & 0xFFFFFFFF ) ) // same paragraph
            return( l->pos_bgn < r->pos_bgn ); // earlier in para first
        else if( ( l->get_id_hi() ) == ( r->get_id_hi() ) ) // same entry
            return( l->var_i < r->var_i ); // earlier in entry first
        else
            return( l->ref_id < r->ref_id ); // if everything fails, sort randomly (in essence)
    }
    else
        return( l->var_d > r->var_d ); // latest in diary first
}

// DIARY ===========================================================================================
Diary::Diary()
:   m_random_gen( m_random_device() ), m_parser_bg( this ), m_matches( &compare_matches )
{
    m_dispatcher_postread_operations.connect(
            sigc::mem_fun( *this, &Diary::handle_postread_operations_finished ) );

    // consider adding these into a m_filters_stock using create_stock_strdef_elem():
    m_force_id = DEID::FILTER_ID_NONTRASHED;
    m_filter_nontrashed     = new Filter( this, STR::compose( "<", get_sstr_i( CSTR::NONTRASHED ),
                                                              ">" ),
                                          Filter::DEFINITION_NONTRASHED );
    m_force_id = DEID::FILTER_ID_TRASHED;
    m_filter_trashed        = new Filter( this, STR::compose( "<", get_sstr_i( CSTR::TRASHED ),
                                                              ">" ),
                                          Filter::DEFINITION_TRASHED );
    m_force_id = DEID::FILTER_ID_CUR_ENTRY;
    m_filter_cur_entry      = new Filter( this, STR::compose( "<",
                                                              get_sstr_i( CSTR::CURRENT_ENTRY ),
                                                              ">" ),
                                          Filter::DEFINITION_CUR_ENTRY + '_' );
    m_force_id = DEID::FILTER_ID_CUR_ENTRY_TREE;
    m_filter_cur_entry_tree = new Filter( this,
                                          STR::compose( "<", get_sstr_i( CSTR::CURRENT_ENTRY_TREE ),
                                                        ">" ),
                                          Filter::DEFINITION_CUR_ENTRY + 'D' );

    // stock elements:
    create_stock_strdef_elem( m_tables_stock, DEID::STOCK_TABLE,
                              _( STRING::DIARY_REPORT ), TableElem::DEFINITION_REPORT);
}

Diary::~Diary()
{
    //remove_lock_if_necessary();
}

SKVVec
Diary::get_as_skvvec() const
{
    SKVVec sv;

    // TODO: make options more human readable
    sv.push_back( { CSTR::LANGUAGE,       m_language } );
    sv.push_back( { CSTR::OPTIONS,        VT::stringize_bitmap< VT::DO >( m_options ) } );
    sv.push_back( { CSTR::FILTER_LIST,    m_p2filter_list ? m_p2filter_list->get_name()
                                                          : get_sstr( CSTR::NA ) } );
    sv.push_back( { CSTR::FILTER_SEARCH,  m_p2filter_search ? m_p2filter_search->get_name()
                                                            : get_sstr( CSTR::NA ) } );
    sv.push_back( { CSTR::STARTUP_ENTRY,  get_entry_name( m_startup_entry_id ) } );
    sv.push_back( { CSTR::COMPLETION_TAG, get_entry_name( m_completion_tag_id ) } );

    return sv;
}

#ifdef __ANDROID__
void
Diary::init_new_pre() {
    clear();
    set_id(create_new_id(this)); // adds itself to the ID pool with a unique ID
    m_read_version = DB_FILE_VERSION_INT;
}
#endif
Result
Diary::init_new( const std::string& path, const std::string& pw )
{
#ifndef __ANDROID__
    clear();

    set_id( create_new_id( this ) ); // adds itself to the ID pool with a unique ID

    m_read_version = DB_FILE_VERSION_INT;
    Result result{ set_path( path, SPT_NEW ) };

    if( result != LoG::SUCCESS )
    {
        clear();
        return result;
    }
#endif

    m_p2chart_active = create_chart( _( STRING::DEFAULT ), ChartElem::DEFINITION_DEFAULT );
    m_p2table_active = m_tables_stock.begin()->second;

    m_force_id = DEID::THEME_SYSTEM;
    ThemeSystem::get()->copy_to( create_theme( ThemeSystem::get()->get_name() ) );

    parse_themes( R"(
ID300103
T ~Artemisia
TfSerif 10
TqSans 10
Tb#FFFFEEEEEEEE
Tt#000000000000
Th#CCCC00000000
Ts#A0A000000000
Tl#F1F1BBBBC4C4
Tg#F6F6F5F5F4F4

ID300107
T ~Dark
TfSans 10
TqSerif 10
Tb#111111111111
Tt#CCCCCCCCCCCC
Th#FFFF66666666
Ts#DDDD33336666
Tl#666611113333

ID300113
T ~Urgent
TfSans 10
TqSerif 10
Tb#808000000000
Tt#FCFCD4D49D9D
Th#FFFFAAAA3333
Ts#E5E5A2A24444
Tl#B5B540400000
Tg#3D3D02020A0A

ID300117
T ~Well Noted
TfSans 11
TqSerif 10
Tb#BBBBEEEEEEEE
Tt#000000000000
Th#555533336666
Ts#222244448888
Tl#9090E0E03333
Tg#F6F6F5F5F4F4
)" );

    m_options = VT::DO::DEFAULT;

    create_entry_dummy(); // must come after m_ptr2chapter_ctg_cur is set
    set_passphrase( pw );

    m_login_status = LOGGED_IN_RO;

#ifndef __ANDROID__
    return write();
#else
    return LoG::SUCCESS;
#endif
}

void
Diary::clear()
{
    if( m_thread_postread_operations )
    {
        {
            std::lock_guard< std::mutex > lock( m_mutex_postread_operations );
            m_F_stop_postread_operations = true;
        }

        while( m_F_stop_postread_operations )
            std::this_thread::sleep_for( std::chrono::milliseconds( 100 ) );
    }

    if( m_parser_bg_postread )
    {
        delete m_parser_bg_postread;
        m_parser_bg_postread = nullptr;
    }

    if( m_sstream )
    {
        delete m_sstream;
        m_sstream = nullptr;
    }
    m_size_bytes = 0;

    m_uri.clear();

    m_read_version = 0;

    set_id( DEID::UNSET );
    m_force_id = DEID::UNSET;
    for( auto it_id = m_ids.begin(); it_id != m_ids.end(); )
    {
        if( !it_id->first.is_stock() )
            it_id = m_ids.erase( it_id );
        else
            ++it_id;
    }

    m_entries.clear();
    m_cache_tags.clear();
    m_p2entry_1st = nullptr;

    m_search_text.clear();
    if( m_fc_search )
    {
        m_p2filter_search = nullptr;
        delete m_fc_search;
        m_fc_search = nullptr;
    }
    m_matches.clear();

    m_filters.clear();
    m_p2filter_list = nullptr;

    m_charts.clear();
    m_p2chart_active = nullptr;

    m_tables.clear();
    m_p2table_active = nullptr;

    m_themes.clear();

    m_weekends[ 0 ] = true;
    m_weekends[ 6 ] = true;
    for( int i = 1; i < 6; i++ ) m_weekends[ i ] = false;
    m_holidays.clear();

    m_startup_entry_id = DEID::MOST_CURRENT_ENTRY;
    m_last_entry_id = DEID::UNSET;
    m_completion_tag_id = DEID::UNSET;

    m_passphrase.clear();

    // NOTE: only reset body options here:
    m_language.clear();
    m_options = 0;
    m_opt_ext_panel_cur = 1;

    m_parser_bg.m_F_spellchk_enabled = false;

    m_F_read_only = false;
    m_F_continue_from_lock = false;
    m_login_status = LOGGED_OUT;
}

R2Pixbuf
Diary::get_image_file( const String& uri, int width )
{
    R2Pixbuf      buf;
    const auto&&  rc_name { STR::compose( uri, "/", width ) };
    auto&&        iter    { m_map_images_f.find( rc_name ) };

    if( iter == m_map_images_f.end() )
    {
        auto file { Gio::File::create_for_uri( convert_rel_uri( uri ) ) };

        if( !file->query_exists() )
        {
            print_error( "Image could not be found: ", uri );
            return buf;
        }

        try
        {
            buf = Gdk::Pixbuf::create_from_file( file->get_path() );
// #ifndef _WIN32
//             buf = Gdk::Pixbuf::create_from_file(
//                     Glib::filename_from_uri( convert_rel_uri( uri ) ) );
// #else
//             const auto&& pth = convert_rel_uri( uri );
//             buf = Gdk::Pixbuf::create_from_file( PATH( pth.substr( 7, pth.length() - 7 ) ) );
// #endif
        }
        catch( Gdk::PixbufError& er )
        {
            print_error( "File is not an image" );
        }
        catch( ... )
        {
            print_error( "Image could not be created: ", uri );
        }

        if( buf )
        {
            if( buf->get_width() > width )
                buf = buf->scale_simple( width, ( buf->get_height() *  width ) / buf->get_width(),
                                         Gdk::InterpType::BILINEAR );

            m_map_images_f[ rc_name ] = buf;
        }
    }
    else
        buf = iter->second;

    return buf;
}
R2Pixbuf
Diary::get_image_chart( const String& uri, int width, const Pango::FontDescription& fd )
{
    R2Pixbuf     buf;
    const auto&& rc_name { STR::compose( uri, "/", width ) };
    auto&&       iter    { m_map_images_c.find( rc_name ) };

    if( iter == m_map_images_c.end() )
    {
        auto chart{ get_element2< ChartElem >( D::DEID( uri ) ) };

        if( !chart ) throw LoG::Error( "Chart not found" );

        buf = ChartSurface::create_pixbuf( chart, width, fd );

        m_map_images_c[ rc_name ] = buf;
    }
    else
        buf = iter->second;

    return buf;
}
R2Pixbuf
Diary::get_image_table( const String& uri, int width, const Pango::FontDescription& fd,
                        bool F_expanded )
{
    R2Pixbuf     buf;
    const auto&& rc_name { STR::compose( uri, "/", width ) };
    auto&&       iter    { m_map_images_t.find( rc_name ) };

    if( iter == m_map_images_t.end() )
    {
        auto table{ get_element2< TableElem >( D::DEID( uri ) ) };

        if( !table ) throw LoG::Error( "Table not found" );

        buf = TableSurface::create_pixbuf( table, width, fd, F_expanded );

        m_map_images_t[ rc_name ] = buf;
    }
    else
        buf = iter->second;

    return buf;
}

void
Diary::clear_chart_and_table_images()
{
    m_map_images_c.clear();
    m_map_images_t.clear();
}
void
Diary::clear_table_images( const String& uri )
{
    for( auto&& it = m_map_images_t.begin(); it != m_map_images_t.end(); )
    {
        if( STR::begins_with( it->first, uri ) )
            it = m_map_images_t.erase( it );
        else
            ++it;
    }
}

#ifndef __ANDROID__
LoG::Result
Diary::set_path( const String& path0, SetPathType type )
{
    auto    file   { Gio::File::create_for_commandline_arg( path0 ) };
    String  uri    { file->get_uri() };

    // CHECK FILE SYSTEM PERMISSIONS
    if( type != SPT_NEW )
    {
        if( !file->query_exists() )
        {
            PRINT_DEBUG( "File is not found" );
            return LoG::FILE_NOT_FOUND;
        }

        auto finfo { file->query_info() };

        if( !finfo->get_attribute_boolean( G_FILE_ATTRIBUTE_ACCESS_CAN_READ ) )
        {
            PRINT_DEBUG( "File is not readable" );
            return LoG::FILE_NOT_READABLE;
        }
        // else if( type != SPT_READ_ONLY &&
        //          !finfo->get_attribute_boolean( G_FILE_ATTRIBUTE_ACCESS_CAN_WRITE ) )
        // {
        //     PRINT_DEBUG( "File is not writable" );
        //     return LoG::FILE_NOT_WRITABLE;
        // }

        // RESOLVE SYMBOLIC LINK
        if( finfo->get_file_type() == Gio::FileType::SYMBOLIC_LINK )
        {
            uri = finfo->get_symlink_target(); // TODO: 3.2: is this necessary with Gio::File?
            print_info( "Symbolic link resolved to path: ", uri );
        }
    }

    // REMOVE PREVIOUS PATH'S LOCK IF ANY
    remove_lock_if_necessary();

    // ACCEPT PATH
    m_uri = uri;
    m_name = get_filename_base( uri );
    m_F_read_only = ( type == SPT_READ_ONLY );

    // CHECK IF LOCKED
    if( !m_F_read_only && is_locked() )
        return LoG::FILE_LOCKED;
    else
        return LoG::SUCCESS;
}
#endif // __ANDROID__

void
Diary::set_continue_from_lock()
{
    m_F_continue_from_lock = true;
}

String
Diary::convert_rel_uri( String uri ) const
{
    if( uri.find( "rel://" ) == 0 )
    {
        auto file { Gio::File::create_for_uri( m_uri ) };
        uri.replace( 0, 5, file->get_parent()->get_uri() );
    }

    return uri;
}
String
Diary::relativize_uri( const String& full_uri ) const
{
    auto          file        { Gio::File::create_for_uri( m_uri ) };
    const String  common_path { file->get_parent()->get_uri() };

    if( STR::begins_with( full_uri, common_path ) )
        return( "rel://" + full_uri.substr( common_path.length() + 1 ) );
        // +1 is to get rid of the / (or backslash in case of windows)
    else
        return full_uri;
}

#ifndef __ANDROID__
LoG::Result
Diary::enable_editing()
{
    if( m_F_read_only )
    {
        PRINT_DEBUG( "Diary: editing cannot be enabled. Diary is read-only" );
        return LoG::FILE_READ_ONLY;
    }

    if( !check_uri_writable( m_uri ) )
    {
        PRINT_DEBUG( "File is not writable" );
        return LoG::FILE_NOT_WRITABLE;
    }

    // Check and "touch" the new lock:
    try
    {
        auto file_lock { Gio::File::create_for_uri( m_uri + LOCK_SUFFIX ) };

        if     ( !file_lock->query_exists() )
            file_lock->create_file()->close();
        else if( !m_F_continue_from_lock )
        {
            auto file_dst { Gio::File::create_for_uri( m_uri + LOCK_SUFFIX +
                                                       Date::format_string_adv( Date::get_now(),
                                                                                ".YMD_hms" ) ) };

            file_lock->copy( file_dst );
        }
    }
    catch( const Glib::Error& err )
    {
        print_error( err.what() );
        return LoG::FAILURE;
    }

    m_parser_bg.m_F_spellchk_enabled = true;

    m_login_status = LOGGED_IN_EDIT;

    return LoG::SUCCESS;
}
#endif // __ANDROID__

bool
Diary::set_passphrase( const std::string& passphrase )
{
    if( passphrase.size() >= PASSPHRASE_MIN_SIZE )
    {
        m_passphrase = passphrase;
        return true;
    }
    else
        return false;
}

void
Diary::clear_passphrase()
{
    m_passphrase.clear();
}

const std::string&
Diary::get_passphrase() const
{
    return m_passphrase;
}

bool
Diary::compare_passphrase( const std::string& passphrase ) const
{
    return( m_passphrase == passphrase );
}

bool
Diary::is_passphrase_set() const
{
    return( ( bool ) m_passphrase.size() );
}

// PARSING HELPERS
inline void
parse_todo_status( Entry* p2entry, char c )
{
    switch( c )
    {
        case 't': p2entry->set_todo_status( ES::TODO ); break;
        case 'T': p2entry->set_todo_status( ES::NOT_TODO | ES::TODO ); break;
        case 'p': p2entry->set_todo_status( ES::PROGRESSED ); break;
        case 'P': p2entry->set_todo_status( ES::NOT_TODO | ES::PROGRESSED ); break;
        case 'd': p2entry->set_todo_status( ES::DONE ); break;
        case 'D': p2entry->set_todo_status( ES::NOT_TODO | ES::DONE ); break;
        case 'c': p2entry->set_todo_status( ES::CANCELED ); break;
        case 'C': p2entry->set_todo_status( ES::NOT_TODO | ES::CANCELED ); break;
    }
}

void
Diary::parse_themes( const std::string_view data )
{
    size_t pos_bgn  { 0 };
    size_t pos_end  { data.find( '\n' ) };
    Theme* theme    { nullptr };

    while( pos_end != std::string_view::npos )
    {
        if( pos_bgn != pos_end )
        {
            const auto line { String( data.substr( pos_bgn, pos_end - pos_bgn ) ) };
            if( STR::begins_with( line, "ID" ) )
                m_force_id = D::DEID( line.substr( 2 ) );
            else
                parse_theme_line( theme, line );
        }

        pos_bgn = pos_end + 1;
        pos_end = data.find( '\n', pos_bgn );
    }

    // handle the last line if there's no trailing newline
    if( pos_bgn < data.size() )
        parse_theme_line( theme, String( data.substr( pos_bgn ) ) );
}

void
Diary::parse_theme_line( Theme*& ptr2theme, const String& line )
{
    switch( line[ 1 ] )
    {
        case ' ':   // declaration line
            ptr2theme = create_theme( line.substr( 3 ) );
            break;
        case 'f':   // font
            ptr2theme->font = Pango::FontDescription( line.substr( 2 ) );
            break;
        case 'q':   // litreary quote font
            ptr2theme->font_literary = Pango::FontDescription( line.substr( 2 ) );
            break;
        case 'm':   // monospace font
            ptr2theme->font_monospace = Pango::FontDescription( line.substr( 2 ) );
            break;
        case 'b':   // base color
            ptr2theme->color_base.set( line.substr( 2 ) );
            break;
        case 't':   // text color
            ptr2theme->color_text.set( line.substr( 2 ) );
            break;
        case 'h':   // heading color
            ptr2theme->color_title.set( line.substr( 2 ) );
            break;
        case 's':   // subheading color
            ptr2theme->color_heading_L.set( line.substr( 2 ) );
            break;
        case 'l':   // highlight color
            ptr2theme->color_highlight.set( line.substr( 2 ) );
            break;
        case 'g':   // gradient color
            ptr2theme->color_base2.set( line.substr( 2 ) );
            ptr2theme->image_bg = "#";
            break;
        case 'i':   // background image
            ptr2theme->image_bg = convert_rel_uri( line.substr( 2 ) );
            break;
    }
}

void
Diary::tmp_upgrade_ordinal_date_to_2000( DateV& old_date )
{
    if( DateOld::is_ordinal( old_date ) == false )
        return;

    old_date = DateOld::make_ordinal( !DateOld::is_hidden( old_date ),
                                      DateOld::get_order_2nd( old_date ),
                                      DateOld::get_order_3rd( old_date ),
                                      0 );
}

void
Diary::tmp_upgrade_date_to_3000( DateV& date )
{
    date = Date::make( DateOld::get_year( date ),
                       DateOld::get_month( date ),
                       DateOld::get_day( date ) );
}

inline void
tmp_add_tags_as_paragraph( Entry* entry_new,
                           std::unordered_map< Entry*, Value >& entry_tags )
{
    if( entry_new )
    {
        for( auto& kv_tag : entry_tags )
        {
            entry_new->add_tag( kv_tag.first, kv_tag.second );
        }

        entry_tags.clear();
    }
}

inline void
tmp_create_chart_from_tag( Entry* tag, long type, Diary* diary )
{
    auto o2{ ( type & ChartData::UNDERLAY_MASK ) == ChartData::UNDERLAY_PREV_YEAR ? "Y" : "-" };
    auto o3{ ( type & ChartData::PERIOD_MASK )   == ChartData::DEL_PERIOD_MONTHLY ? "M" : "Y" };
    auto o4{ ( type & ChartData::COMBINE_MASK )  == ChartData::DEL_AVERAGE        ? "A" : "P" };
    // create predefined charts for non-boolean tags
    if( tag && ( type & ChartData::COMBINE_MASK ) != ChartData::DEL_BOOLEAN )
    {
        diary->create_chart( tag->get_name(),
                             STR::compose( "Gyt", tag->get_id().get_raw(), "\nGoT", o2, o3, o4 ) );
    }
}

void
Diary::tmp_upgrade_table_defs()
{
    for( auto& kv_table : m_tables )
    {
        auto                def             { kv_table.second->get_definition() };
        String              line;
        String::size_type   line_offset     { 0 };
        int                 col_count       { 0 };

        while( STR::get_line( def, line_offset, line ) )
            if( line[ 1 ] == 'c' && line[ 2 ] == 'n' ) col_count++;

        if( col_count == 0 )
            continue;

        line_offset = 0;

        while( STR::get_line( def, line_offset, line ) )
            if( line[ 1 ] == 'c' && line[ 2 ] == 'o' )
                def.insert( line_offset, STR::compose( "Mcw", 1.0/col_count, '\n' ) );

        kv_table.second->set_definition( def );
    }
}

void
Diary::upgrade_to_1030()
{
    // initialize the status dates:
    for( auto& kv_entry : m_entries )
        kv_entry.second->m_date_finish = kv_entry.second->get_date_created();

    // replace old to-do boxes:
    replace_all_matches( "☐", "[ ]" );
    replace_all_matches( "☑", "[+]" );
    replace_all_matches( "☒", "[x]" );
}

void
Diary::upgrade_to_1050()
{
    for( auto& kv_entry : m_entries )
    {
        Entry* entry{ kv_entry.second };

        if( entry->get_todo_status() == ES::NOT_TODO )
            entry->update_todo_status();
    }
}

void prnt_siblings( Entry* e1st, int indent_lvl )
{
    String indent;
    for( int i= 0; i < indent_lvl; i++ ) indent += "  ";

    for( Entry* esib = e1st; esib; esib = esib->get_next() )
    {
        PRINT_DEBUG( "       ", indent, "* ",  esib->get_name(),
                     "(par=", esib->get_parent() ? esib->get_parent()->get_name() :  "0x0", ")" );
        if( esib->get_child_1st() )
            prnt_siblings( esib->get_child_1st(), indent_lvl + 1 );
    }
}

bool
Diary::upgrade_to_3000()
{
    // startup entry:
    if( m_startup_entry_id == DEID::MIN ) // this was used in diaries <2000
        m_startup_entry_id = DEID::MOST_CURRENT_ENTRY;

    // fill up the hierarchy:
    if( m_read_version < 2011 && !m_entries.empty() )
    {
        Entry* e_last_l1        { nullptr };
        Entry* e_last_l2        { nullptr };
        Entry* e_last_l3        { nullptr };
        Entry* e_first_temporal { nullptr };
        Entry* e_last_temporal  { nullptr };

        auto add_children_to_parent_entry = []( Entry* es1, Entry* ep )
        {
            Entry* e_last_sibling = es1;
            while( e_last_sibling->m_p2next != nullptr )
            {
                e_last_sibling = e_last_sibling->get_next();
                e_last_sibling->m_p2parent = ep;
            }

            if( ep->m_p2child_1st )
            {
                ep->m_p2child_1st->m_p2prev = e_last_sibling;
                e_last_sibling->m_p2next = ep->m_p2child_1st;
            }

            es1->m_p2parent = ep;
            es1->m_p2prev = nullptr;
            ep->m_p2child_1st = es1;
        };

        for( auto&& iter = m_entries.begin(); iter != m_entries.end(); ++iter )
        {
            Entry* e { iter->second };

            print_info( "Upgrading: ", e->get_name() );

            switch( DateOld::get_level( e->get_date() ) )
            {
                case 1:
                    if( e_last_l1 )
                        e_last_l1->add_sibling_before( e );
                    if( e_last_l2 )
                    {
                        add_children_to_parent_entry( e_last_l2, e );
                        e_last_l2 = nullptr;
                    }
                    e_last_l1 = e;
                    break;
                case 2:
                    if( e_last_l2 )
                        e_last_l2->add_sibling_before( e ); // as the map is ordered descendingly
                    if( e_last_l3 )
                    {
                        add_children_to_parent_entry( e_last_l3, e );
                        e_last_l3 = nullptr;
                    }
                    e_last_l2 = e;
                    break;
                case 3:
                    if( e_last_l3 )
                        e_last_l3->add_sibling_before( e ); // as the map is ordered descendingly
                    e_last_l3 = e;
                    break;
                case 4:
                    if( e_last_temporal )
                        e_last_temporal->add_sibling_after( e );
                    else if( !e_first_temporal )
                        e_first_temporal = e;
                    e_last_temporal = e;
                    break;
            }
        }

        if( e_last_l1 )
        {
            m_p2entry_1st = e_last_l1;
            if( e_first_temporal )
                m_p2entry_1st->get_sibling_last()->add_sibling_chain_after( e_first_temporal );
        }
        else if( e_first_temporal )
        {
            m_p2entry_1st = e_first_temporal;
        }
        else
        {
            print_error( "Upgrade error: Diary does not have a first level entry!" );
            return false;
        }

        // take care of orphan entries:
        if( e_last_l2 )
        {
            add_children_to_parent_entry( e_last_l2, e_last_l1 );
            int sorder{ 0 };
            for( Entry* e2o = e_last_l1->m_p2child_1st; e2o; e2o = e2o->m_p2next )
                e2o->m_sibling_order = ++sorder;
        }
        if( e_last_l3 )
        {
            add_children_to_parent_entry( e_last_l3, e_last_l1 );
            int sorder{ 0 };
            for( Entry* e2o = e_last_l1->m_p2child_1st; e2o; e2o = e2o->m_p2next )
                e2o->m_sibling_order = ++sorder;
        }

        // check if anything was left out:
        unsigned n_hierarchical_entries { 0 };
        for( Entry* eh = m_p2entry_1st; eh; eh = eh->get_next_straight() )
            ++n_hierarchical_entries;
        if( n_hierarchical_entries != m_entries.size() )
        {
            print_error( "Upgrade error: ", m_entries.size() - n_hierarchical_entries,
                         " entries could not be upgraded!" );
            return false;
        }

        // assign date to ordinal entries + set title style:
        for( auto&& kv_e : m_entries )
        {
            Entry* e{ kv_e.second };

            if( DateOld::is_ordinal( e->m_date ) )
            {
                if( DateOld::is_hidden( e->m_date ) )
                    e->set_title_style( VT::ETS::NAME_ONLY::I );
                else
                    e->set_title_style( VT::ETS::NUMBER_AND_NAME::I );

                // all entries are assigned to a date now:
                if( e->get_date_created() < DateV( DateOld::DATE_MAX ) )
                    e->m_date = Date::make_from_ctime( e->get_date_created() );
                else
                    e->m_date = e->get_date_created();
            }
            // if not already set as milestone:
            else if( e->get_title_style() != VT::ETS::MILESTONE::I )
            {
                e->set_title_style( VT::ETS::DATE_AND_NAME::I );
            }
        }
    }

    // update the dates:
    if( m_read_version < 2016 )
    {
        m_entries.clear_but_keep();

        for( Entry* e = m_p2entry_1st; e; e = e->get_next_straight() )
        {
            if( e->m_date < DateV( DateOld::DATE_MAX ) ) // if not already upgraded
                tmp_upgrade_date_to_3000( e->m_date );

            if( e->m_date_created < DateV( DateOld::DATE_MAX ) ) // if not already upgraded
                e->m_date_created = Date::make_from_ctime( e->m_date_created );

            if( e->m_date_edited < DateV( DateOld::DATE_MAX ) ) // if not already upgraded
                e->set_date_edited( Date::make_from_ctime( e->m_date_edited ) );

            if( e->m_date_finish < DateV( DateOld::DATE_MAX ) ) // if not already upgraded
            {
                if( m_read_version < 2014 )
                    e->m_date_finish = Date::make_from_ctime( e->m_date_finish );
                else
                    tmp_upgrade_date_to_3000( e->m_date_finish );
            }

            m_entries.emplace( e->m_date, e );

            if( e->get_title_style() == VT::ETS::DATE_AND_NAME::I &&
                e->m_date != e->get_date_created() )
            {
                if( e->m_p2para_1st->m_p2next )
                    e->m_p2para_1st->m_p2next->insert_text_with_spaces(
                            0, Date::format_string( e->m_date ), nullptr );
                else
                    e->add_paragraph_before( Date::format_string( e->m_date ), nullptr, nullptr,
                                             ParaInhClass::SET_TEXT );
            }
        }
        for( auto kv_filter : m_filters )
        {
            auto        def{ kv_filter.second->get_definition() };
            Ustring     def_new;
            String      line;
            UstringSize line_offset{ 0 };
            bool        F_replace_def;

            while( STR::get_line( def, line_offset, line ) )
            {
                if( STR::begins_with( line, "Fd" ) )
                {
                    int       i_date{ 3 };
                    DateV     date_b{ STR::get_i64( line, i_date ) };
                    const int i_f_icl_e{ i_date };
                    DateV     date_e{ STR::get_i64( line, ++i_date ) };

                    date_b = Date::make( DateOld::get_year( date_b ),
                                            DateOld::get_month( date_b ),
                                            DateOld::get_day( date_b ) );
                    date_e = Date::make( DateOld::get_year( date_e ),
                                            DateOld::get_month( date_e ),
                                            DateOld::get_day( date_e ) );

                    def_new += STR::compose( "\nFd", line[ 2 ], date_b, line[ i_f_icl_e ], date_e );
                    F_replace_def = true;
                }
                else if( line_offset > 3 )
                    def_new += ( "\n" + line );
                else
                    def_new = line;
            }

            if( F_replace_def )
                kv_filter.second->set_definition( def_new );
        }
    }

    // convert old paragraphs to 2013:
    if( m_read_version < 2013 )
    {
        for( Entry* e = m_p2entry_1st; e; e = e->get_next_straight() )
        {
            for( Paragraph* para = e->m_p2para_1st; para; para = para->m_p2next )
            {
                if( para->m_text.empty() ) continue;

                if( ( STR::begins_with( para->m_text, "chart:" ) ||
                      STR::begins_with( para->m_text, "file:" )  ||
                      STR::begins_with( para->m_text, "rel:" ) ) &&
                    para->m_text.find( ' ' ) == Ustring::npos &&
                    m_read_version < 2013 )
                {
                    if( para->m_text[ 0 ] == 'c' )
                    {
                        para->set_image_type( VT::PS_IMAGE_CHART );
                        ChartElem* chart { get_chart( para->m_text.substr( 6 ) ) };
                        if( chart )
                        {
                            para->set_uri( chart->get_id().get_str() );
                            para->m_text.clear();
                        }
                    }
                    else
                    {
                        para->set_image_type( VT::PS_IMAGE_FILE );
                        para->set_uri( para->m_text );
                        para->m_text.clear();
                    }
                }
                else
                {
                    ParserUpgrader parser_upg;
                    parser_upg.parse( para, m_read_version );
                }

                if( m_read_version > 2011 ) continue; // 2012 changes above

                unsigned int indentation_level{ 0 };
                for( unsigned int i = 0; i < para->m_text.length(); i++ )
                {
                    if( para->m_text[ i ] == '\t' ) indentation_level++;
                    else if( para->m_text[ i ] != '.' || i != 0 ) break;
                }

                // SUBHEADER
                if( para->m_text[ 0 ] == ' ' && para->m_text[ 1 ] != ' ' )
                {
                    para->set_heading_level( VT::PHS::LARGE::I );
                    para->m_text = para->m_text.substr( 1 );
                }
                // SUBSUBHEADER
                else if( STR::begins_with( para->m_text, "  " ) && para->m_text[ 3 ] != ' ' )
                {
                    para->set_heading_level( VT::PHS::MEDIUM::I );
                    para->m_text = para->m_text.substr( 2 );
                }
                // IGNORE
                else if( STR::begins_with( para->m_text, ".\t" ) )
                {
                    para->set_quot_type( VT::QT::GENERIC::C );
                    para->m_style |= ( VT::PS_INDENT_1 * indentation_level );
                    para->m_text = para->m_text.substr( indentation_level + 1 );
                }
                // TO-DO
                else if( indentation_level > 0 && para->m_text.length() >= indentation_level + 4 &&
                        para->m_text[ indentation_level ] == '[' &&
                        para->m_text[ indentation_level + 2 ] == ']' )
                {
                    switch( para->m_text[ indentation_level + 1 ] )
                    {
                        case ' ':
                            para->set_list_type( VT::PLS::TODO::I );
                            break;
                        case '~':
                            para->set_list_type( VT::PLS::PROGRS::I );
                            break;
                        case '+':
                            para->set_list_type( VT::PLS::DONE::I );
                            break;
                        case 'x':
                        case 'X':
                        case '>':
                            para->set_list_type( VT::PLS::CANCLD::I );
                            break;
                    }
                    for( auto format : para->m_formats )
                    {
                        format->pos_bgn -= 4;
                        format->pos_end -= 4;
                    }

                    para->m_style |= ( VT::PS_INDENT_1 * indentation_level );
                    para->m_text = para->m_text.substr( indentation_level + 4 );
                }
                // INDENTATION (if not already processed above)
                if( indentation_level > 0 && para->get_indent_level() == 0 )
                {
                    para->m_style |= ( VT::PS_INDENT_1 * indentation_level );
                    para->m_text = para->m_text.substr( indentation_level );
                    para->predict_list_style_from_text();
                }

                if( indentation_level > 0 )
                {
                    for( auto format : para->m_formats )
                    {
                        format->pos_bgn -= indentation_level;
                        format->pos_end -= indentation_level;
                    }
                }
            }
        }
    }

    if( m_read_version < 2017 )
    {
        for( auto kv_chart : m_charts )
        {
            ChartData cd( this );
            cd.set_from_string_old( kv_chart.second->get_definition() );
            kv_chart.second->set_definition( cd.get_as_string() );
        }

        for( auto& kv_table : m_tables )
        {
            const String&   def_old{ kv_table.second->get_definition() };
            String          def_new;
            String          line;
            StringSize      line_offset{ 0 };

            while( STR::get_line( def_old, line_offset, line ) )
            {
                if( STR::begins_with( line, "McoR" ) )
                    def_new += ( "McoRSF~~~~~" + line.substr( 5 ) + '\n' );
                else
                if( STR::begins_with( line, "Mco" ) )
                    def_new += ( line.substr( 0, 5 ) + "_~~~~~" + line.substr( 5 ) + '\n' );
                else
                    def_new += ( line + '\n' );
            }
            kv_table.second->set_definition( def_new );
        }
    }

    // convert filter refs by name
    if( m_read_version < 2018 )
    {
        for( auto kv_table : m_tables )
        {
            const Ustring   def         { kv_table.second->get_definition() };
            Ustring         def_new;
            String          line;
            StringSize      line_offset { 0 };

            while( STR::get_line( def, line_offset, line ) )
            {
                if( line.substr( 0, 4 ) == "Mccf" )
                {
                    auto filter{ get_filter( line.substr( 4 ) ) };
                    if( filter )
                        def_new += STR::compose( "Mccf",
                                                 filter->get_id().get_raw(),
                                                 '\n' );
                }
                else
                if( line.substr( 0, 2 ) == "Mf" )
                {
                    auto filter{ get_filter( line.substr( 2 ) ) };
                    if( filter )
                        def_new += STR::compose( "Mf",
                                                 get_filter( line.substr( 2 ) )->get_id().get_raw(),
                                                 '\n' );
                }
                else
                if( line.substr( 0, 2 ) == "Ml" )
                {
                    auto filter{ get_filter( line.substr( 2 ) ) };
                    if( filter )
                        def_new += STR::compose( "Ml",
                                                 get_filter( line.substr( 2 ) )->get_id().get_raw(),
                                                 '\n' );
                }
                else
                    def_new += ( line + '\n' );
            }

            kv_table.second->set_definition( def_new );
        }
    }

    //prnt_siblings( e_last_l1, 0 );

    return true;
}

void
Diary::do_standard_checks_after_read()
{
    if( m_properties.has( PROP::SYNC_ONLY ) ) return;

    // initialize derived theme colors
    for( auto& kv_theme : m_themes )
        kv_theme.second->calculate_derived_colors();

    if( this == d ) // i.e. omit for extra diaries such as manual or sync targets
    {
        if( m_themes.empty() )
            ThemeSystem::get()->copy_to( create_theme( ThemeSystem::get()->get_name() ) );

        // DEFAULT CHART AND TABLE
        if( !m_p2chart_active )
            m_p2chart_active = create_chart( _( STRING::DEFAULT ), ChartElem::DEFINITION_DEFAULT );
        if( !m_p2table_active )
            m_p2table_active = m_tables_stock.begin()->second;

        if( m_startup_entry_id > DEID::MIN )
        {
            if( !get_element( m_startup_entry_id ) )
            {
                print_error( "Startup element ", m_startup_entry_id.get_raw(),
                             " cannot be found in db" );
                m_startup_entry_id = DEID::MOST_CURRENT_ENTRY;
            }
        }

        if( m_p2filter_search )
            m_fc_search = m_p2filter_search->get_filterer_stack();

        if( m_entries.empty() )
        {
            print_info( "A dummy entry added to the diary" );
            create_entry_dummy();
        }
    }

    m_thread_postread_operations = new std::thread( &Diary::do_postread_operations, this );
}

void
Diary::do_postread_operations()
{
    {
        std::lock_guard< std::mutex > lock( m_mutex_postread_operations );
        m_F_stop_postread_operations = false;
        m_parser_bg_postread = new ParserBackGround( this );
    }

    for( auto e = m_p2entry_1st; e; e = e->get_next_straight() )
    {
        if( m_F_stop_postread_operations ) break;

        e->parse( m_parser_bg_postread );

        if( m_F_stop_postread_operations ) break;
    }

#if LIFEOGRAPH_DEBUG_BUILD
    if( m_F_stop_postread_operations )
        PRINT_DEBUG( "--- INTERRUPTED POST-READ OPERATIONS ---" );
    else
        PRINT_DEBUG( "--- FINISHED POST-READ OPERATIONS ---" );
#endif

    if( m_F_stop_postread_operations )
    {
        std::lock_guard< std::mutex > lock( m_mutex_postread_operations );
        m_F_stop_postread_operations = false;
    }
    else if( this == d ) // only for the main diary, not for manual or sync diaries
        m_dispatcher_postread_operations.emit();
}

void
Diary::handle_postread_operations_finished()
{
    if( !m_thread_postread_operations )
        return;

    if( m_thread_postread_operations->joinable() )
        m_thread_postread_operations->join();

    delete m_thread_postread_operations;
    m_thread_postread_operations = nullptr;

    delete m_parser_bg_postread;
    m_parser_bg_postread = nullptr;

    clear_chart_and_table_images();
}

// PARSING FUNCTIONS
inline LoG::Result
Diary::parse_db_body_text()
{
    if     ( m_read_version > 2000 && m_read_version <= DB_FILE_VERSION_INT )
        return parse_db_body_text_3000();
    else if( m_read_version > 1050 && m_read_version <= 2000 )
        return parse_db_body_text_2000();
    else if( m_read_version >= DB_FILE_VERSION_INT_MIN && m_read_version <= 1050 )
        return parse_db_body_text_1050();
    else
        return LoG::FAILURE;
}

LoG::Result
Diary::parse_db_body_text_3000()
{
    String        line;
    Theme*        p2theme       { nullptr };
    Filter*       p2filter      { nullptr };
    ChartElem*    p2chart       { nullptr };
    TableElem*    p2table       { nullptr };
    Entry*        p2entry       { nullptr };
    Paragraph*    p2para        { nullptr };
    DateV         date_c        { Date::NOT_SET };
    DateV         date_e        { Date::NOT_SET };
    Paragraph*    p2para_after  { nullptr };  // for upgrade

    while( getline( *m_sstream, line ) )
    {
        if( line.size() < 2 )
            continue;

        switch( line[ 0 ] )
        {
            // DIARY OPTION
            case 'D':
                switch( line[ 1 ] )
                {
                    case 'o':   // options
                        if( m_read_version > 2000 )
                            m_options |= VT::bitmapize_string< VT::DO >( line.substr( 2 ) );
                        break;
                    case 'p':   // spell checking language
                        m_opt_ext_panel_cur = std::stoi( line.substr( 2 ) );
                        break;
                    case 's':   // spell checking language
                        m_language = line.substr( 2 );
                        break;
                    case 'f':   // first entry to show
                        m_startup_entry_id = D::DEID( line.substr( 2 ) );
                        break;
                    case 'l':   // last entry shown in the previous session
                        m_last_entry_id = D::DEID( line.substr( 2 ) );
                        break;
                    case 'c':   // completion tag
                        m_completion_tag_id = D::DEID( line.substr( 2 ) );
                        break;
                }
                break;
            // ID (START OF A NEW ELEMENT)
            case 'I':   // id
                m_force_id = D::DEID( line.substr( 2 ) );
                break;
            case 'N':   // date + id (new gen)
                date_c = std::stoull( line.substr( 2, 15 ) );
                date_e = std::stoull( line.substr( 18, 15 ) );
                m_force_id = D::DEID( line.substr( 34 ) );
                break;
            // THEME
            case 'T':
                parse_theme_line( p2theme, line );
                break;
            // FILTER
            case 'F':
                if( line[ 1 ] == ' ' ) // declaration
                {
                    p2filter = create_filter( line.substr( 4 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2filter_list = p2filter;
                    if( line[ 3 ] == 'S' )
                        m_p2filter_search = p2filter;
                }
                else
                {
                    if( m_read_version == 3000 && line[ 1 ] == 'x' )
                        line.insert( 2, "~~~_" );
                    p2filter->add_definition_line( line );
                }
                break;
            // CHART
            case 'G':
                if( line[ 1 ] == ' ' )  // declaration
                {
                    p2chart = create_chart( line.substr( 3 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2chart_active = p2chart;
                }
                else
                    p2chart->add_definition_line( line );
                break;
            // TABLE (MATRIX)
            case 'M':
                if( line[ 1 ] == ' ' )  // declaration
                {
                    p2table = create_table( line.substr( 3 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2table_active = p2table;
                }
                else
                    p2table->add_definition_line( line );
                break;
            case 'W':
                for( int i = 0; i < 7; i++ ) m_weekends[ i ] = line[ 2 + i ] == '1';
                break;
            case 'H':
                m_holidays.insert( std::stoull( line.substr( 2 ) ) );
                break;
            // ENTRY
            case 'E':
                switch( line[ 1 ] )
                {
                    case ' ':   // declaration
                    {
                        p2entry = create_entry( std::stoull( line.substr( 17 ) ),
                                                line[ 2 ] == 'F',
                                                line[ 3 ] == 'T',
                                                line[ 5 ] == 'E' );

                        p2para_after = nullptr;

                        parse_todo_status( p2entry, line[ 4 ] );
                        p2entry->set_title_style( VT::get_v< VT::ETS,
                                                             int,
                                                             char >( line[ 6 ] ) );
                        p2entry->set_comment_style( VT::get_v< VT::CS,
                                                               int,
                                                               char >( line[ 7 ] ) );
                        // cached value:
                        p2entry->set_filtered_out( line[ 8 ] != '_', line[ 8 ] == 'h' );
                        p2entry->set_property< bool >( PROP::REGISTER_SCRIPTS, line[ 9 ] == 'S' );
                        p2entry->set_locked( line[ 10 ] == 'L' );
                        break;
                    }
                    case 'b':   // bg color
                        p2entry->set_color( line.substr( 2 ) );
                        break;
                    case 'c':
                        p2entry->m_date_created = date_c = std::stoull( line.substr( 2 ) );
                        // date_c is set to fallback to inherit for old diaries
                        break;
                    case 'e':
                        p2entry->set_date_edited( date_e = std::stoull( line.substr( 2 ) ) );
                        // date_e is set to fallback to inherit for old diaries
                        break;
                    case 't':   // finish date (cached)
                        // on v2000 this meant status change date
                        p2entry->m_date_finish = std::stoull( line.substr( 2 ) );
                        break;
                    case 'h':   // hierarchy
                        if( line[ 2 ] == 'a' ) // after
                        {
                            auto esb = get_entry_by_id( D::DEID( line.substr( 3 ) ) );
                            if( esb ) esb->add_sibling_after( p2entry );
                            // should never be the case but is here to prevent data loss when things go astray:
                            else m_p2entry_1st->add_sibling_after( p2entry );
                        }
                        else
                        if( line[ 2 ] == 'u' ) // under
                        {
                            auto ep = get_entry_by_id( D::DEID( line.substr( 3 ) ) );
                            if( ep ) ep->add_child_1st( p2entry );
                            // should never be the case but is here to prevent data loss when things go astray:
                            else m_p2entry_1st->add_sibling_after( p2entry );
                        }
                        else // 'r' i.e. root
                            m_p2entry_1st = p2entry;
                        break;
                    case 'm':
                        p2entry->set_theme( m_themes[ line.substr( 2 ) ] );
                        break;
                    case 's':   // spell checking language
                        p2entry->set_lang( line.substr( 2 ) );
                        break;
                    case 'u':   // unit
                        p2entry->set_unit( line.substr( 2 ) );
                        break;
                    case 'p':   // paragraph
                        switch( line[ 2 ] )
                        {
                            case 'c':
                                p2para->set_color( line.substr( 3 ) );
                                break;
                            case 'g':
                                p2para->set_lang( line.substr( 3 ) );
                                break;
                            case 'l':
                            {
                                auto loc { p2para->get_location() };
                                if( line[ 3 ] == 'a' )
                                    loc->latitude = STR::get_d( line.substr( 4 ) );
                                else if(  line[ 3 ] == 'o' )
                                    loc->longitude = STR::get_d( line.substr( 4 ) );
                                break;
                            }
                            case 'r': // uri for images
                                p2para->set_uri( line.substr( 3 ) );
                                break;
                            case 't':   // tag definition
                                p2para->set_tag_bound( std::stoi( line.substr( 4 ) ) );
                                break;
                            case 'u':
                                p2para->set_unit( line.substr( 3 ) );
                                break;
                            default:
                                p2para = p2entry->add_paragraph_before(
                                        line.substr( line[ 2 ] == 'D' ? 3 : 18 ),
                                        p2para_after, // non-null only in old entries
                                        nullptr, ParaInhClass::SET_TEXT );

                                p2para->m_date_created = date_c;
                                p2para->set_date_edited( date_e );

                                if( line[ 2 ] == 'D' ) break;

                                p2para->set_alignment( VT::get_v< VT::PA, int, char >(
                                                           line[ 2 ] ) );
                                p2para->set_heading_level( VT::get_v< VT::PHS, int, char >(
                                                           line[ 3 ] ) );
                                p2para->set_list_type( VT::get_v< VT::PLS, int, char >(
                                                           line[ 4 ] ) );
                                p2para->set_expanded( line[ 5 ] == 'E' );
                                if( line[ 6 ] == 'Q' )
                                    p2para->set_quote( true ); // legacy
                                p2para->set_indent_level( std::stoi( line.substr( 7, 1 ) ) );

                                switch( line[ 8 ] )
                                {
                                    case 'I': p2para->set_image_type( VT::PS_IMAGE_FILE ); break;
                                    case 'C': p2para->set_image_type( VT::PS_IMAGE_CHART ); break;
                                    case 'T': p2para->set_image_type( VT::PS_IMAGE_TABLE ); break;
                                }
                                if( p2para->is_image() )
                                {
                                    p2para->set_image_size( std::stoi( line.substr( 9, 1 ) ) );
                                    if( m_read_version <= 3005 )
                                    {
                                        p2para->set_uri( p2para->m_text );
                                        p2para->m_text.clear();
                                    }
                                }

                                if( line[ 10 ] == 'X' )
                                    p2para->m_style |= VT::PS_IMAGE_EXPND;
                                if( m_read_version <= 3000 )
                                    p2para->set_quot_type( line[ 11 ] == 'C' ? VT::QT::GENERIC::C
                                                                             : VT::QT::OFF::C );
                                else
                                    p2para->set_quot_type( line[ 11 ] );
                                if( line[ 12 ] == 'R' )
                                    p2para->set_hrule( true );
                                break;
                        }
                        break;
                    case 'f':   // formatting
                    {
                        int        index        { 3 };
                        int        format_type  { VT::HFT_LINK_URI };
                        const auto pos_bgn      { STR::get_i32( line, index ) };
                        const auto pos_end      { STR::get_i32( line, ++index ) };

                        format_type = HiddenFormat::get_type_from_char( line[ 2 ] );

                        auto&& format = p2para->add_format( format_type, line.substr( ++index ),
                                                            pos_bgn, pos_end );

                        // temporary fix for diaries of development versions
                        if( STR::begins_with( format->uri, "deid:" ) )
                        {
                            format->ref_id = std::stoul( format->uri.substr( 5 ) );
                            format->uri = "";
                            if( format->type != VT::HFT_TAG )
                                format->type = VT::HFT_LINK_ID;
                        }
                        else if( format_type == VT::HFT_TAG || format_type == VT::HFT_LINK_ID )
                        {
                            format->ref_id = std::stoul( format->uri );
                            format->uri = "";
                        }

                        break;
                    }
                }
                break;
            default:
                print_error( "Unrecognized line: [", line, "]" );
                clear();
                return LoG::CORRUPT_FILE;
        }
    }

    do_standard_checks_after_read();

    return LoG::SUCCESS;
}

LoG::Result
Diary::parse_db_body_text_2000()
{
    String        line;
    Theme*        p2theme       { nullptr };
    Filter*       p2filter      { nullptr };
    ChartElem*    p2chart       { nullptr };
    TableElem*    p2table       { nullptr };
    Entry*        p2entry       { nullptr };
    Paragraph*    p2para        { nullptr };
    Paragraph*    p2para_coord  { nullptr };  // for upgrade
    Paragraph*    p2para_after  { nullptr };  // for upgrade

    while( getline( *m_sstream, line ) )
    {
        if( line.size() < 2 )
            continue;

        switch( line[ 0 ] )
        {
            // DIARY OPTION
            case 'D':
                switch( line[ 1 ] )
                {
                    case 'o':   // options
                        // NOTE: sorting criteria is omitted in the upgrade
                        if( line[ 2 ] == 'A' ) m_options |= VT::DO::SHOW_ALL_ENTRY_LOCATIONS::I;
                        if( line.size() > 6 )
                            m_opt_ext_panel_cur = std::stoi( line.substr( 6, 1 ) );
                        break;
                    case 's':   // spell checking language
                        m_language = line.substr( 2 );
                        break;
                    case 'f':   // first entry to show
                        m_startup_entry_id = D::DEID( line.substr( 2 ) );
                        break;
                    case 'l':   // last entry shown in the previous session
                        m_last_entry_id = D::DEID( line.substr( 2 ) );
                        break;
                    case 'c':   // completion tag
                        m_completion_tag_id = D::DEID( line.substr( 2 ) );
                        break;
                }
                break;
            // ID (START OF A NEW ELEMENT)
            case 'I':   // id
                m_force_id = D::DEID( line.substr( 2 ) );
                break;
            // THEME
            case 'T':
                // if( line[ 2 ] == 'D' ) // ignore, no longer relevant
                //     m_p2theme_default = p2theme;
                parse_theme_line( p2theme, line );
                break;
            // FILTER
            case 'F':
                if( line[ 1 ] == ' ' ) // declaration
                {
                    p2filter = create_filter( line.substr( 3 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2filter_list = p2filter;
                }
                else
                    p2filter->add_definition_line( line );
                break;
            // CHART
            case 'G':
                if( line[ 1 ] == ' ' )  // declaration
                {
                    p2chart = create_chart( line.substr( 3 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2chart_active = p2chart;
                }
                else
                    p2chart->add_definition_line( line );
                break;
            // TABLE (MATRIX)
            case 'M':
                if( line[ 1 ] == ' ' )  // declaration
                {
                    p2table = create_table( line.substr( 3 ), "" );
                    if( line[ 2 ] == 'A' )
                        m_p2table_active = p2table;
                }
                else
                    p2table->add_definition_line( line );
                break;
            // CHAPTER CATEGORY
            case 'C':
                p2filter = create_filter( _( "Chapter Category:" ) + line.substr( 3 ), "F|" );
//                if( line[ 2 ] == 'A' ) // ignored
//                    m_p2chapter_ctg_cur = ptr2chapter_ctg;
                break;
            // ENTRY / CHAPTER
            case 'E':
                switch( line[ 1 ] )
                {
                    case ' ':   // declaration
                    case '+':   // chapter declaration ( deprecated )
                    {
                        p2entry = create_entry( std::stoull( line.substr( 6 ) ),
                                                line[ 2 ] == 'F',
                                                line[ 3 ] == 'T',
                                                line[ 5 ] == 'E' );

                        p2para_after = nullptr;

                        parse_todo_status( p2entry, line[ 4 ] );
                        if( line[ 1 ] == '+' )
                        {
                            p2entry->set_title_style( VT::ETS::MILESTONE::I );
                            p2filter->add_definition_line(
                                    STR::compose( "FiT", p2entry->get_id().get_raw() ) );
                        }
                        break;
                    }
                    case 'c':
                        p2entry->m_date_created = std::stoull( line.substr( 2 ) );
                        break;
                    case 'e':
                        p2entry->set_date_edited( std::stoull( line.substr( 2 ) ) );
                        break;
                    case 't':   // finish date (cached)
                        // on v2000 this meant status change date
                        p2entry->m_date_finish = std::stoull( line.substr( 2 ) );
                        break;
                    case 'm':
                        p2entry->set_theme( m_themes[ line.substr( 2 ) ] );
                        break;
                    case 's':   // spell checking language
                        p2entry->set_lang( line.substr( 2 ) );
                        break;
                    case 'u':   // unit
                        p2entry->set_unit( line.substr( 2 ) );
                        break;
                    case 'l':   // location -v2000
                    case 'r':   // path (route) -v2000
                        if( line[ 2 ] == 'a' )
                        {
                            p2para_coord =
                                    p2entry->add_map_path_point( STR::get_d( line.substr( 3 ) ),
                                                                 0.0, nullptr );
                            if( !p2para_after )
                                p2para_after = p2para_coord;
                        }
                        else if( line[ 2 ] == 'o' )
                        {
                            auto loc{ p2para_coord->get_location() };
                            loc->longitude = STR::get_d( line.substr( 3 ) );
                        }
                        break;
                    case 'p':   // paragraph
                        p2para = p2entry->add_paragraph_before( line.substr( 3 ),
                                                                p2para_after, // only in old entries
                                                                nullptr,
                                                                ParaInhClass::SET_TEXT );
                        p2para->set_alignment( VT::get_v< VT::PA, int, char >( line[ 2 ] ) );
                        break;
                    case 'b':   // bg color
                        p2entry->set_color( line.substr( 2 ) );
                        break;
                }
                break;
            default:
                print_error( "Unrecognized line: [", line, "]" );
                clear();
                return LoG::CORRUPT_FILE;
        }
    }

    tmp_upgrade_table_defs();

    if( !upgrade_to_3000() )
        return LoG::FAILURE;

    do_standard_checks_after_read();

    return LoG::SUCCESS;
}

LoG::Result
Diary::parse_db_body_text_1050()
{
    std::string         read_buffer;
    std::string         line;
    std::string::size_type line_offset{ 0 };
    Entry*              p2entry{ nullptr };
    unsigned int        tag_o1{ 0 };
    unsigned int        tag_o2{ 0 };
    unsigned int        tag_o3{ 0 };
    Filter*             p2filter{ nullptr };
    Theme*              p2theme{ nullptr }, * p2theme_untagged{ nullptr };
    bool                flag_in_tag_ctg{ false };
    Ustring             filter_def{ "F&" };
    std::unordered_map< Entry*, Value >     entry_tags;
    Ustring             chart_def_default{ ChartElem::DEFINITION_DEFAULT };
    //SortCriterion       sorting_criteria{ SoCr_DEFAULT };

    // PREPROCESSING TO DETERMINE FIRST AVAILABLE ORDER
    while( getline( *m_sstream, line ) )
    {
        read_buffer += ( line + '\n' );

        if( line[ 0 ] == 'C' && line[ 1 ] == 'G' )
            tag_o1++;
    }

    // TAGS TOP LEVEL
    p2entry = new Entry( this, DateOld::make_ordinal( false, ++tag_o1, 0 ) );
    p2entry->m_date_finish = p2entry->m_date_created;
    m_entries.emplace( p2entry->m_date, p2entry );
    m_cache_tags[ p2entry->get_id() ] = p2entry;
    p2entry->add_paragraph_before( "[###>", nullptr, nullptr, ParaInhClass::SET_TEXT );
    p2entry->set_expanded( true );

    // TAG DEFINITIONS & CHAPTERS
    while( STR::get_line( read_buffer, line_offset, line ) )
    {
        if( line.empty() )    // end of section
            break;
        else if( line.size() >= 3 )
        {
            switch( line[ 0 ] )
            {
                case 'I':   // id
                    m_force_id = D::DEID( line.substr( 2 ) );
                break;
                // TAGS
                case 'T':   // tag category
                    p2entry = new Entry( this, DateOld::make_ordinal( false, tag_o1, ++tag_o2 ) );
                    p2entry->m_date_finish = p2entry->m_date_created;
                    m_entries.emplace( p2entry->m_date, p2entry );
                    m_cache_tags[ p2entry->get_id() ] = p2entry;
                    p2entry->add_paragraph_before( line.substr( 2 ), nullptr, nullptr,
                                                   ParaInhClass::SET_TEXT );
                    p2entry->set_expanded( line[ 1 ] == 'e' );
                    flag_in_tag_ctg = true;
                    tag_o3 = 0;
                    break;
                case 't':   // tag
                    switch( line[ 1 ] )
                    {
                        case ' ':
                            p2entry = new Entry(
                                    this,
                                    flag_in_tag_ctg ?
                                            DateOld::make_ordinal( false, tag_o1, tag_o2,
                                                                                  ++tag_o3 ) :
                                            DateOld::make_ordinal( false, tag_o1, ++tag_o2 ) );
                            m_entries.emplace( p2entry->m_date, p2entry );
                            m_cache_tags[ p2entry->get_id() ] = p2entry;
                            p2entry->add_paragraph_before( line.substr( 2 ), nullptr, nullptr,
                                                           ParaInhClass::SET_TEXT );
                            p2theme = nullptr;
                            break;
                        case 'c': // not used in 1010
                            tmp_create_chart_from_tag(
                                    p2entry, std::stol( line.substr( 2 ) ), this );
                            break;
                        case 'u': // not used in 1010
                            if( p2entry )
                                p2entry->set_unit( line.substr( 2 ) );
                            break;
                    }
                    break;
                case 'u':
                    if( !p2theme_untagged && line[ 1 ] != 'c' ) // chart is ignored
                        p2theme_untagged = create_theme( ThemeSystem::get()->get_name() );
                    parse_theme_line( p2theme_untagged, line );
                    break;
                case 'm':
                    if( p2theme == nullptr )
                        p2theme = create_theme( p2entry->get_name() );
                    parse_theme_line( p2theme, line );
                    break;
                // DEFAULT FILTER
                case 'f':
                    switch( line[ 1 ] )
                    {
                        case 's':   // status
                            filter_def += "\nFs";
                            filter_def += ( line[ 6 ] == 'N' ) ? 'N' : 'n';
                            filter_def += ( line[ 7 ] == 'T' ) ? 'O' : 'o';
                            filter_def += ( line[ 8 ] == 'P' ) ? 'P' : 'p';
                            filter_def += ( line[ 9 ] == 'D' ) ? 'D' : 'd';
                            filter_def += ( line[ 10 ] == 'C' ) ? 'C' : 'c';

                            if( line[ 2 ] == 'T' && line[ 3 ] != 't' ) filter_def += "\nFty";
                            else if( line[ 2 ] != 'T' && line[ 3 ] == 't' ) filter_def += "\nFtn";
                            if( line[ 4 ] == 'F' && line[ 5 ] != 'f' ) filter_def += "\nFfy";
                            else if( line[ 4 ] != 'F' && line[ 5 ] == 'f' ) filter_def += "\nFfn";
                            break;
                        case 't':   // tag
                        {
                            auto e_tag{ get_entry_by_name( line.substr( 2 ) ) };
                            if( e_tag )
                                filter_def += STR::compose( "\nFt", e_tag->get_id().get_raw() );
                            else
                                print_error( "Reference to undefined tag: ", line.substr( 2 ) );
                            break;
                        }
                        case 'b':   // begin date: in the new system this is an after filter
                            filter_def += STR::compose( "\nFa", line.substr( 2 ) );
                            break;
                        case 'e':   // end date: in the new system this is a before filter
                            filter_def += STR::compose( "\nFb", line.substr( 2 ) );
                            break;
                    }
                    break;
                // CHAPTERS
                case 'o':   // ordinal chapter (topic) (<1020)
                    p2entry = new Entry( this, get_db_line_date( line ) );
                    tmp_upgrade_ordinal_date_to_2000( p2entry->m_date );
                    m_entries.emplace( p2entry->m_date, p2entry );
                    m_cache_tags[ p2entry->get_id() ] = p2entry;
                    p2entry->set_text( get_db_line_name( line ), nullptr );
                    p2entry->set_expanded( line[ 1 ] == 'e' );
                    break;
                case 'd':   // to-do group (<1020)
                    if( line[ 1 ] == ':' ) // declaration
                    {
                        p2entry = new Entry( this, get_db_line_date( line ) );
                        tmp_upgrade_ordinal_date_to_2000( p2entry->m_date );
                        m_entries.emplace( p2entry->m_date, p2entry );
                        m_cache_tags[ p2entry->get_id() ] = p2entry;
                        p2entry->set_text( get_db_line_name( line ), nullptr );
                    }
                    else // options
                    {
                        p2entry->set_expanded( line[ 2 ] == 'e' );
                        if( line[ 3 ] == 'd' )
                            p2entry->set_todo_status( ES::DONE );
                        else if( line[ 3 ] == 'c' )
                            p2entry->set_todo_status( ES::CANCELED );
                        else
                            p2entry->set_todo_status( ES::TODO );
                    }
                    break;
                case 'c':   // temporal chapter (<1020)
                    if( p2filter )
                    {
                        p2entry = create_entry( get_db_line_date( line ), false, false, true );
                        p2entry->set_text( get_db_line_name( line ), nullptr );
                        p2filter->add_definition_line( STR::compose( "FiT",
                                                                     p2entry->get_id().get_raw() ) );
                        p2entry->set_title_style( VT::ETS::MILESTONE::I );
                    }
                    else
                        print_error( "No chapter category defined" );
                    break;
                case 'C':
                    switch( line[ 1 ] ) // any chapter item based on line[1] (>=1020)
                    {
                        case 'C':   // chapter category
                            p2filter = create_filter( _( "Chapter Category:" ) + line.substr( 3 ),
                                                      "F|" );
                            break;
                        case 'c':   // chapter color
                            p2entry->set_color( line.substr( 2 ) );
                            break;
                        case 'T':   // temporal chapter
                            p2entry = create_entry( get_db_line_date( line ), false, false, true );
                            p2entry->set_text( get_db_line_name( line ), nullptr );
                            p2filter->add_definition_line( STR::compose( "FiT",
                                                                         p2entry->get_id().get_raw() ) );
                            p2entry->set_title_style( VT::ETS::MILESTONE::I );
                            break;
                        case 'O':   // ordinal chapter (used to be called topic)
                        case 'G':   // free chapter (replaced todo_group in v1020)
                            p2entry = new Entry( this, get_db_line_date( line ) );
                            tmp_upgrade_ordinal_date_to_2000( p2entry->m_date );
                            m_entries.emplace( p2entry->m_date, p2entry );
                            m_cache_tags[ p2entry->get_id() ] = p2entry;
                            p2entry->set_text( get_db_line_name( line ), nullptr );
                            break;
                        case 'p':   // chapter preferences
                            p2entry->set_expanded( line[ 2 ] == 'e' );
                            parse_todo_status( p2entry, line[ 3 ] );
                            //line[ 4 ] (Y) is ignored as we no longer create charts for chapters
                            break;
                    }
                    break;
                case 'O':   // options
//                    switch( line[ 2 ] )
//                    {
//                        case 'd': sorting_criteria = SoCr_DATE_ASC; break;
//                        case 's': sorting_criteria = SoCr_SIZE_C_ASC; break;
//                        case 'c': sorting_criteria = SoCr_CHANGE_ASC; break;
//                    }
                      // NOTE: It is not worth the effort to incorporate the sorting criteria
                    if( m_read_version == 1050 )
                    {
                        //m_sorting_criteria |= ( line[ 3 ] == 'd' ? SoCr_DESCENDING : SoCr_ASCENDING );
                        // NOTE: It is not worth the effort to incorporate the sorting direction
                        if( line.size() > 4 && line[ 4 ] == 'Y' )
                            chart_def_default = ChartElem::DEFINITION_DEFAULT_Y;
                    }
                    else if( m_read_version == 1040 )
                    {
                        if( line.size() > 3 && line[ 3 ] == 'Y' )
                            chart_def_default = ChartElem::DEFINITION_DEFAULT_Y;
                    }
                    break;
                case 'l':   // language
                    m_language = line.substr( 2 );
                    break;
                case 'S':   // startup action
                    m_startup_entry_id = D::DEID( line.substr( 2 ) );
                    break;
                case 'L':
                    m_last_entry_id = D::DEID( line.substr( 2 ) );
                    break;
                default:
                    print_error( "Unrecognized line:\n", line );
                    clear();
                    return LoG::CORRUPT_FILE;
            }
        }
    }

    // ENTRIES
    p2entry = nullptr;
    while( STR::get_line( read_buffer, line_offset, line ) )
    {
        if( line.size() < 2 )
            continue;
        else if( line[ 0 ] != 'I' && line[ 0 ] != 'E' && line[ 0 ] != 'e' && p2entry == nullptr )
        {
            print_error( "No entry declared for the attribute" );
            continue;
        }

        switch( line[ 0 ] )
        {
            case 'I':
                // add tags as inline tags
                tmp_add_tags_as_paragraph( p2entry, entry_tags );
                // it is important to ensure setting force ID is directly followed
                // by new Entry() so paragraphs have to be added before setting it

                m_force_id = D::DEID( line.substr( 2 ) );
                break;
            case 'E':   // new entry
            case 'e':   // trashed
                if( line.size() < 5 )
                    continue;

                p2entry = new Entry( this, std::stoul( line.substr( 4 ) ),
                                     line[ 1 ] == 'f' ? ES::ENTRY_DEFAULT_FAV : ES::ENTRY_DEFAULT );
                tmp_upgrade_ordinal_date_to_2000( p2entry->m_date );
                m_entries.emplace( p2entry->m_date, p2entry );
                m_cache_tags[ p2entry->get_id() ] = p2entry;

                if( line[ 0 ] == 'e' )
                    p2entry->set_trashed( true );
                if( line[ 2 ] == 'h' )
                    filter_def += STR::compose( "\nFn", p2entry->get_id().get_raw() );

                parse_todo_status( p2entry, line[ 3 ] );
                break;
            case 'D':   // creation & change dates (optional)
                switch( line[ 1 ] )
                {
                    case 'r':
                        p2entry->m_date_created = std::stoul( line.substr( 2 ) );
                        break;
                    case 'h':
                        p2entry->set_date_edited( std::stoul( line.substr( 2 ) ) );
                        break;
                    case 's':
                        p2entry->m_date_finish = std::stoul( line.substr( 2 ) );
                        //p2entry->set_date_finish_explicit( true );
                        break;
                }
                break;
            case 'T':   // tag
            {
                NameAndValue&& nav{ NameAndValue::parse( line.substr( 2 ) ) };
                Entry* e_tag{ get_entry_by_name( nav.name ) };
                if( e_tag )
                {
                    entry_tags.emplace( e_tag, nav.value );
                    if( line[ 1 ] == 'T' )
                        p2entry->set_theme( m_themes[ nav.name ] );
                }
                else
                    print_error( "Reference to undefined tag: ", nav.name );
                break;
            }
            case 'l':   // language
                p2entry->set_lang( line.substr( 2 ) );
                break;
            case 'P':    // paragraph
                p2entry->add_paragraph_before( line.substr( 2 ), nullptr, nullptr,
                                               ParaInhClass::SET_TEXT );
                break;
            default:
                print_error( "Unrecognized line:\n", line );
                clear();
                return LoG::CORRUPT_FILE;
        }
    }

    // add tags to the last entry as inline tags
    tmp_add_tags_as_paragraph( p2entry, entry_tags );

    if( m_read_version < 1030 )
        upgrade_to_1030();

    if( !upgrade_to_3000() )
        return LoG::FAILURE;

    // this has to be done after 2*** update as it relies on paragraph statuses to be set:
    if( m_read_version < 1050 )
        upgrade_to_1050();

    // DEFAULT FILTER AND CHART
    m_p2filter_list = create_filter( _( STRING::DEFAULT ), filter_def );
    m_p2chart_active = create_chart( _( STRING::DEFAULT ), chart_def_default );

    do_standard_checks_after_read();

    return LoG::SUCCESS;
}

#ifdef __ANDROID__
// Andorid does not use the Gio::File system and fills the m_sstream itself
LoG::Result
Diary::read_header( const char* buffer, size_t size )
{
    m_size_bytes = size;

    if( m_sstream ) delete m_sstream;
    m_sstream = new std::stringstream();

    // copy the buffer into the stream
    m_sstream->write( buffer, size );

    return read_header();
}
#endif

// READING
LoG::Result
Diary::read_header()
{
#ifndef __ANDROID__ // Andoid reads the file itself
    m_sstream = new std::stringstream;

    try
    {
        auto  giofile   { Gio::File::create_for_uri( m_F_continue_from_lock ? m_uri + LOCK_SUFFIX
                                                                            : m_uri ) };
        auto  ifstream  { giofile->read() };
        auto  finfo     { giofile->query_info() };
        gsize bytes_read;

        m_size_bytes = finfo->get_size();
        auto  buffer    { new char[ m_size_bytes ] };

        if( !ifstream->read_all( buffer, m_size_bytes, bytes_read ) )
        {
            throw LoG::Error( "Couldn't read all!" );
        }

        m_sstream->write( buffer, bytes_read );

        delete[] buffer;
        ifstream->close();
    }
    catch( Glib::Error& error )
    {
        print_error( "Failed to open diary file: ", m_uri, "\n", error.what() );
        clear();
        return LoG::COULD_NOT_START;
    }
#endif // !__ANDROID__

    String line;
    getline( *m_sstream, line );

    if( line != DB_FILE_HEADER )
    {
        clear();
        return LoG::CORRUPT_FILE;
    }

    while( getline( *m_sstream, line ) )
    {
        switch( line[ 0 ] )
        {
            case 'V':
                m_read_version = std::stoi( line.substr( 2 ) );
                if( m_read_version < DB_FILE_VERSION_INT_MIN )
                {
                    clear();
                    return LoG::INCOMPATIBLE_FILE_OLD;
                }
                else if( m_read_version > DB_FILE_VERSION_INT )
                {
                    clear();
                    return LoG::INCOMPATIBLE_FILE_NEW;
                }
                break;
            case 'E':
                // passphrase is set to a dummy value to indicate that diary
                // is an encrypted one until user enters the real passphrase
                m_passphrase = ( line[ 2 ] == 'y' ? " " : "" );
                break;
            case 'I':
                m_force_id = D::DEID( line.substr( 2 ) );
                break;
            case 0:
                set_id( create_new_id( this ) );
                return( m_read_version ? LoG::SUCCESS : LoG::CORRUPT_FILE );
            default:
                print_error( "Unrecognized header line: ", line );
                break;
        }
    }

    clear();
    return LoG::CORRUPT_FILE;
}

LoG::Result
Diary::read_body()
{
    Result res { SUCCESS };
    DiaryElemTag::ContextDateEditability date_editability( false );

    if( !m_passphrase.empty() )
        res = decrypt_buffer();

    if( res == SUCCESS )
        res = parse_db_body_text() ;

    if( m_sstream )
    {
        delete m_sstream;
        m_sstream = nullptr;
    }

    if( res == SUCCESS )
        m_login_status = LOGGED_IN_RO;

    return res;
}

LoG::Result
Diary::decrypt_buffer()
{
    CipherBuffers buf;

    try
    {
        // allocate memory for salt
        buf.salt = new unsigned char[ LoG::Cipher::cSALT_SIZE ];
        // read salt value
        m_sstream->read( ( char* ) buf.salt, LoG::Cipher::cSALT_SIZE );

        buf.iv = new unsigned char[ LoG::Cipher::cIV_SIZE ];
        // read IV
        m_sstream->read( ( char* ) buf.iv, LoG::Cipher::cIV_SIZE );

        LoG::Cipher::expand_key( m_passphrase.c_str(), buf.salt, &buf.key );

        // calculate bytes of data in file
        const auto size { m_size_bytes - m_sstream->tellg() };
        if( size <= 3 )
        {
            buf.clear();
            clear();
            return LoG::CORRUPT_FILE;
        }
        buf.buffer = new unsigned char[ size ];
        if( ! buf.buffer )
            throw LoG::Error( "Unable to allocate memory for buffer" );

        m_sstream->read( ( char* ) buf.buffer, size );
        LoG::Cipher::decrypt_buffer( buf.buffer, size, buf.key, buf.iv );

        // passphrase check
        if( buf.buffer[ 0 ] != m_passphrase[ 0 ] || buf.buffer[ 1 ] != '\n' )
        {
            buf.clear();
            // do not clear the diary as it will be reused
            return LoG::WRONG_PASSWORD;
        }
    }
    catch( ... )
    {
        buf.clear();
        clear();
        return LoG::COULD_NOT_START;
    }

    delete m_sstream;
    m_sstream = new std::stringstream;
    buf.buffer += 2;          // ignore first two chars which are for passphrase checking
    *m_sstream << buf.buffer; // pass the decrypted contents form the temp buffer to the sstream
    buf.buffer -= 2;          // restore pointer to the start of the buffer before deletion
    buf.clear();

    return LoG::SUCCESS;
}

// WRITING
inline char
get_entry_todo_status_char( const Entry* p2entry )
{
    switch( p2entry->get_todo_status() )
    {
        case ES::TODO:                          return 't';
        case ( ES::NOT_TODO | ES::TODO ):       return 'T';
        case ES::PROGRESSED:                    return 'p';
        case ( ES::NOT_TODO | ES::PROGRESSED ): return 'P';
        case ES::DONE:                          return 'd';
        case ( ES::NOT_TODO | ES::DONE ):       return 'D';
        case ES::CANCELED:                      return 'c';
        case ( ES::NOT_TODO | ES::CANCELED ):   return 'C';
        case ES::NOT_TODO:
        default:    /* should never occur */    return 'n';
    }
}
inline char
get_para_alignment_char( const int style )
{
    return VT::get_v< VT::PA, char, int >( style & VT::PA::FILTER );
}
inline char
get_para_list_type_char( const int style )
{
    return VT::get_v< VT::PLS, char, int >( style & VT::PLS::FILTER );
}
inline String
get_para_image_chars( const Paragraph* para )
{
    char t;
    switch( para->m_style & VT::PS_FLT_IMAGE )
    {
        case VT::PS_IMAGE_FILE:     t = 'I'; break;
        case VT::PS_IMAGE_CHART:    t = 'C'; break;
        case VT::PS_IMAGE_TABLE:    t = 'T'; break;
        default:                    return "__";
    }

    return STR::compose( t, para->get_image_size() );
}

inline void
Diary::create_db_entry_text( const Entry* entry, FiltererContainer* fc_save, StrStream& sstream )
{
    // ENTRY DATE
    sstream << "\n\nID" << entry->get_id().get_raw()
            << "\nE " << ( entry->is_favorite() ? 'F' : '_' )
                      << ( entry->is_trashed() ? 'T' : '_' )
                      << get_entry_todo_status_char( entry )
                      << ( entry->is_expanded() ? 'E' : '_' )
                      << VT::get_v< VT::ETS, char, int >( entry->get_title_style() )
                      << VT::get_v< VT::CS, char, int >( entry->get_comment_style() )
                      << entry->is_filtered_out_completely()  // cached value
                      << ( entry->registers_scripts_raw() ? 'S' : '_' )
                      << ( entry->is_locked_raw() ? 'L' : '_' )
                      << "~~~~~~"    // reserved (6)
                      << entry->m_date;

    sstream << "\nEc" << entry->get_date_created();
    sstream << "\nEe" << entry->m_date_edited;
    if( entry->m_date_finish != entry->m_date )
        sstream << "\nEt" << entry->m_date_finish;

    // HIERARCHY
    if( fc_save )
    {
        if( entry->get_prev_unfiltered( fc_save ) )
            sstream << "\nEha" << entry->m_p2prev->get_id().get_raw();
        else if( entry->get_parent_unfiltered( fc_save ) )
            sstream << "\nEhu" << entry->m_p2parent->get_id().get_raw();
        else
        {
            if( m_p2top_entry_last )
                sstream << "\nEha" << m_p2top_entry_last->get_id().get_raw();
            else
                sstream << "\nEhr";
            m_p2top_entry_last = entry;
        }
    }
    else
    {
        if( entry->m_p2prev )
        sstream << "\nEha" << entry->m_p2prev->get_id().get_raw();
        else
        if( entry->m_p2parent )
            sstream << "\nEhu" << entry->m_p2parent->get_id().get_raw();
        else
            sstream << "\nEhr";
    }

    // THEME
    if( entry->is_theme_set() )
        sstream << "\nEm" << entry->get_theme()->get_name_std();

    // SPELLCHECKING LANGUAGE
    if( entry->has_property( PROP::LANGUAGE ) )
        sstream << "\nEs" << entry->get_lang();

    // UNIT
    if( entry->has_property( PROP::UNIT ) )
        sstream << "\nEu" << entry->get_unit();

    if( entry->has_property( PROP::COLOR ) )
        sstream << "\nEb" << entry->get_color();

    // PARAGRAPHS
    for( Paragraph* para = entry->m_p2para_1st; para; para = para->m_p2next )
    {
        sstream << "\nN " << para->m_date_created << '|' << para->m_date_edited
                                                  << '|' << para->get_id().get_raw();

        if( para->m_style == VT::PS_DEFAULT && para->get_quot_type() == VT::QT::OFF::C &&
            para->is_expanded() )
            sstream << "\nEpD";
        else
            sstream << "\nEp" << get_para_alignment_char( para->m_style )
                                 << VT::get_v< VT::PHS, char, int >( para->get_heading_level() )
                                 << get_para_list_type_char( para->m_style )
                                 << ( para->is_expanded() ? 'E' : 'C' )
                                 << '~' // quotation merged into code_lang in 3.1
                                 << ( para->get_indent_level() )
                                 << get_para_image_chars( para )
                                 << ( ( para->m_style & VT::PS_IMAGE_EXPND ) ? 'X' : '_' )
                                 << ( para->get_quot_type() )
                                 << ( ( para->has_hrule() ) ? 'R' : '_' )
                                 << "~~~~~"; // reserved (5)
        sstream << para->get_text_std();

        if( para->has_property( PROP::LOCATION ) )
        {
            const auto loc { para->get_location() };
            sstream << "\nEpla" << loc->latitude << "\nEplo" << loc->longitude;
        }
        if( para->has_property( PROP::COLOR ) )
            sstream << "\nEpc" << para->get_color();
        if( para->has_property( PROP::LANGUAGE ) )
            sstream << "\nEpg" << para->get_lang();
        if( para->defines_tag() )
            sstream << "\nEpt " << para->get_tag_bound();
        if( para->has_property( PROP::UNIT ) )
            sstream << "\nEpu" << para->get_unit();
        if( para->has_property( PROP::URI ) ) // TODO: 3.2: check saving uri for non-image paras
            sstream << "\nEpr" << para->get_uri();

        for( auto format : para->m_formats )
            if( !format->is_on_the_fly() )
                sstream << "\nEf" << VT::get_v< VT::FMT, char, int >( format->type )
                                  << format->pos_bgn << "|"
                                  << format->pos_end << "|"
                                  << ( ( format->type == VT::HFT_LINK_ID ||
                                         format->type == VT::HFT_TAG ) ?
                                       std::to_string( format->ref_id ) : format->uri );
    }
}

void
Diary::create_db_header_text( bool encrypted )
{
    *m_sstream << DB_FILE_HEADER;
    *m_sstream << "\nV " << DB_FILE_VERSION_INT;
    *m_sstream << "\nE " << ( encrypted ? 'y' : 'n' );
    *m_sstream << "\nId" << get_id().get_raw(); // diary id
    *m_sstream << "\n\n"; // end of header
}

bool
Diary::create_db_body_text( StrStream& sstream )
{
    // DIARY OPTIONS
    sstream << "Do" << VT::stringize_bitmap< VT::DO >( m_options );

    sstream << "\nDp" << m_opt_ext_panel_cur;

    // DEFAULT SPELLCHECKING LANGUAGE
    if( !m_language.empty() )
        sstream << "\nDs" << m_language;

    // FIRST ENTRY TO SHOW AT STARTUP (HOME ITEM) & LAST ENTRY SHOWN IN PREVIOUS SESSION
    sstream << "\nDf" << m_startup_entry_id.get_raw();
    sstream << "\nDl" << m_current_entry_id.get_raw();

    // COMPLETION TAG
    if( m_completion_tag_id != DEID::UNSET )
        sstream << "\nDc" << m_completion_tag_id.get_raw();

    // THEMES
    for( auto& kv_theme : m_themes )
    {
        Theme* theme{ kv_theme.second };
        sstream << "\n\nID" << theme->get_id().get_raw();
        sstream << "\nT ~" << theme->get_name_std(); // 1 reserved, remnant from default theme
        sstream << "\nTf" << theme->font.to_string().c_str();
        if( theme->has_font_literary() )
            sstream << "\nTq" << theme->font_literary.to_string().c_str();
        if( theme->has_font_monospace() )
            sstream << "\nTm" << theme->font_monospace.to_string().c_str();
        sstream << "\nTb" << convert_gdkrgba_to_html( theme->color_base );
        sstream << "\nTt" << convert_gdkrgba_to_html( theme->color_text );
        sstream << "\nTh" << convert_gdkrgba_to_html( theme->color_title );
        sstream << "\nTs" << convert_gdkrgba_to_html( theme->color_heading_L );
        sstream << "\nTl" << convert_gdkrgba_to_html( theme->color_highlight );
        if( theme->image_bg == "#" ) // gradient
            sstream << "\nTg" << convert_gdkrgba_to_html( theme->color_base2 );
        else if( !theme->image_bg.empty() )
            sstream << "\nTi" << relativize_uri( theme->image_bg );
    }

    // FILTERS
    for( auto& kv_filter : m_filters )
    {
        sstream << "\n\nID" << kv_filter.second->get_id().get_raw();
        sstream << "\nF " << ( kv_filter.second == m_p2filter_list ? 'A' : '_' )
                          << ( kv_filter.second == m_p2filter_search ? 'S' : '_' )
                          << kv_filter.first.c_str();
        sstream << '\n' << kv_filter.second->get_definition().c_str();
    }

    // CHARTS
    for( auto& kv_chart : m_charts )
    {
        sstream << "\n\nID" << kv_chart.second->get_id().get_raw();
        sstream << "\nG " << ( kv_chart.second == m_p2chart_active ? 'A' : '_' )
                          << kv_chart.first.c_str();
        sstream << '\n' << kv_chart.second->get_definition().c_str();
    }

    // TABLES
    for( auto& kv_table : m_tables )
    {
        sstream << "\n\nID" << kv_table.second->get_id().get_raw();
        sstream << "\nM " << ( kv_table.second == m_p2table_active ? 'A' : '_' )
                          << kv_table.first.c_str();
        sstream << '\n' << kv_table.second->get_definition().c_str();
    }

    // HOLIDAYS
    sstream << "\n\nWe" << m_weekends[ 0 ] << m_weekends[ 1 ] << m_weekends[ 2 ]
                        << m_weekends[ 3 ] << m_weekends[ 4 ] << m_weekends[ 5 ]
                        << m_weekends[ 6 ];

    for( auto& holiday : m_holidays ) sstream << "\nHo" << holiday;

    auto fc_save{ m_p2filter_save ? m_p2filter_save->get_filterer_stack() : nullptr };
    m_p2top_entry_last = nullptr; // reset it before start

    // ENTRIES
    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
    {
        // optionally only save filtered entries:
        if( fc_save && !fc_save->filter( entry ) ) continue;

        create_db_entry_text( entry, fc_save, sstream );
    }

    if( fc_save ) delete fc_save;

    return true;
}

#ifndef __ANDROID__
LoG::Result
Diary::write()
{
    if( m_F_read_only ) throw LoG::Error( "Trying to save read-only diary!" );

    auto file_diary { Gio::File::create_for_uri( m_uri ) };
    if( file_diary->query_exists() )
    {
        if( !check_uri_writable( m_uri ) ) return LoG::FILE_NOT_WRITABLE;

        // BACKUP FOR THE LAST VERSION BEFORE UPGRADE
        if( m_read_version != DB_FILE_VERSION_INT )
            copy_file_suffix( m_uri, ".", m_read_version, true );

        // BACKUP THE PREVIOUS VERSION
        auto file_prev { Gio::File::create_for_uri( m_uri + ".~previousversion~" ) };
        file_diary->move( file_prev, Gio::File::CopyFlags::OVERWRITE );
    }

    // WRITE THE FILE
    const Result result{ write( m_uri ) };

    // DAILY BACKUP SAVES
    if( Lifeograph::settings.save_backups && !Lifeograph::settings.backup_folder_uri.empty() )
    {
        const auto&&  name_strp
        { STR::ends_with( m_name, ".diary" ) ? m_name.substr( 0, m_name.size() - 6 ) : m_name };
        auto          folder_backup
        { Gio::File::create_for_uri( Lifeograph::settings.backup_folder_uri ) };
        auto          file_backup
        { folder_backup->get_child( name_strp + "_(" + get_id().get_str() + ")_"
                                              + Date::format_string( Date::get_today(), "YMD", '-' )
                                              + ".diary" ) };

        file_diary->copy( file_backup, Gio::File::CopyFlags::OVERWRITE );

        print_info( "Daily backup saved" );
    }

    return result;
}

LoG::Result
Diary::write_copy( const String& uri, const String& passphrase, const Filter* filter )
{
    m_p2filter_save = filter;

    String passphrase_actual = m_passphrase;
    m_passphrase = passphrase;
    Result result{ write( uri ) };
    m_passphrase = passphrase_actual;

    m_p2filter_save = nullptr;

    return result;
}
#endif // __ANDROID__

LoG::Result
Diary::write_txt( const String& uri, const Filter* filter )
{
    StrStream ss;

    ss.imbue( std::locale( "C" ) ); // to prevent thousands separators

    // HELPERS
    const String separator         = "---------------------------------------------\n";
    const String separator_favored = "+++++++++++++++++++++++++++++++++++++++++++++\n";
    const String separator_thick   = "=============================================\n";

    // DIARY TITLE
    ss << separator_thick << m_name << '\n' << separator_thick << "\n\n";

    // ENTRIES
    auto fc_save { filter ? filter->get_filterer_stack() : nullptr };

    for( EntryIterReverse it_entry = m_entries.rbegin(); it_entry != m_entries.rend(); ++it_entry )
    {
        Entry* entry{ it_entry->second };

        // PURGE EMPTY OR FILTERED OUT ENTRIES
        if( entry->is_empty() || ( fc_save && !fc_save->filter( entry ) ) )
            continue;

        // FAVOREDNESS AND DATE
        ss << ( entry->is_favorite() ? separator_favored : separator );
        if( entry->get_title_style() == VT::ETS::DATE_AND_NAME::I )
            ss << Date::format_string( entry->get_date() ) << '\n';
        else if( entry->get_title_style() == VT::ETS::NUMBER_AND_NAME::I )
            ss << entry->get_number_str() << '\n';

        // TO-DO STATUS
        ss << entry->get_todo_status_as_text();

        // CONTENT
        bool F_first_para { true };

        entry->do_for_each_para(
            [ & ]( Paragraph* para )
            {
                ss << para->get_text_decorated().c_str() << '\n';
                if( F_first_para )
                {
                    ss << ( entry->is_favorite() ? separator_favored : separator );
                    F_first_para = false;
                }
            } );

        ss << "\n\n";
    }

    ss << '\n';

#ifndef __ANDROID__
    auto  file_txt  { Gio::File::create_for_uri( uri ) }; // is PATH() is necessary on win?
    auto  ofstream  { file_txt->query_exists() ? file_txt->replace() : file_txt->create_file() };
    gsize bytes_written;
    ofstream->write_all( ss.str(), bytes_written );
    ofstream->close();
#endif

    if( fc_save ) delete fc_save;

    return SUCCESS;
}

LoG::Result
Diary::write( const String& uri )
{
#ifndef __ANDROID__
    Glib::RefPtr< Gio::FileOutputStream > ofstream;

    try
    {
        auto file { Gio::File::create_for_uri( uri ) };

        if( file->query_exists() )
        {
            if( !check_uri_writable( file ) ) return LoG::FILE_NOT_WRITABLE;
            ofstream = file->replace();
        }
        else
            ofstream = file->create_file();
    }
    catch ( const Glib::Error& err )
    {
        print_error( err.what() );
        return LoG::FAILURE;
    }
#endif

    // WRITING THE HEADER
    m_sstream = new std::stringstream;
    m_sstream->imbue( std::locale( "C" ) ); // to prevent thousands separators

    // WRITING THE HEADER
    create_db_header_text( is_encrypted() );

    // WRITING THE BODY into a temporary stream
    if( is_encrypted() )
    {
        CipherBuffers       buf;
        std::stringstream   body_stream;
        body_stream.imbue( std::locale( "C" ) );

        body_stream << m_passphrase[ 0 ] << '\n'; // first char of passphrase for validity checking
        create_db_body_text( body_stream );

        const String        body_str { body_stream.str() };

        try
        {
            size_t size { body_str.size() + 1 };
            LoG::Cipher::create_new_key( m_passphrase.c_str(), &buf.salt, &buf.key );
            LoG::Cipher::create_iv( &buf.iv );
            buf.buffer = new unsigned char[ size ];
            memcpy( buf.buffer, body_str.c_str(), size );
            LoG::Cipher::encrypt_buffer( buf.buffer, size, buf.key, buf.iv );

            // Append salt + iv + encrypted body into the saved (header) stream
            m_sstream->write( reinterpret_cast< const char* >( buf.salt ),
                              LoG::Cipher::cSALT_SIZE );
            m_sstream->write( reinterpret_cast< const char* >( buf.iv ), LoG::Cipher::cIV_SIZE );
            m_sstream->write( reinterpret_cast< const char* >( buf.buffer ), size );

            buf.clear();
        }
        catch( const Glib::Error& err )
        {
            print_error( err.what() );
            buf.clear();
            return LoG::FAILURE;
        }
    }
    else
    {
        create_db_body_text( *m_sstream );
    }

#ifndef __ANDROID__
    gsize bytes_written;
    try
    {
        // ALL data is in m_sstream — write it in one go:
        ofstream->write_all( m_sstream->str(), bytes_written );
        ofstream->close();
    }
    catch( const Glib::Error& err )
    {
        print_error( err.what() );
        return LoG::FAILURE;
    }
#endif

    return LoG::SUCCESS;
}

#ifndef __ANDROID__
bool
Diary::remove_lock_if_necessary()
{
    if( m_login_status != LOGGED_IN_EDIT || m_uri.empty() )
        return false;

    auto file_lock { Gio::File::create_for_uri( m_uri + LOCK_SUFFIX ) };
    if( file_lock->query_exists() )
        file_lock->remove();

    return true;
}

bool
Diary::is_locked() const
{
    if( m_uri.empty() )
        return false;

    try
    {
        auto file_lock  { Gio::File::create_for_uri( m_uri + LOCK_SUFFIX ) };
#if LIFEOGRAPH_DEBUG_BUILD
        // the vleowcheck is redundant but the exception creates hassles during debug
        if( !file_lock->query_exists() ) return false;
#endif
        auto finfo      { file_lock->query_info( "standard::*" ) };
        return( finfo->get_size() > 0 );
    }
    catch( ... )
    {
        PRINT_DEBUG( "Lock file size query failed!" );
    }

    return false;
}
#endif // __ANDROID__

// ELEMENTS
D::DEID
Diary::create_new_id( DiaryElement* element )
{
    D::DEID retval;
    if( m_force_id == DEID::UNSET )
    {
        std::uniform_int_distribution<> dis0( 10'000'000, 99'999'999 );

        do { retval.set( dis0( m_random_gen ) ); }
        while( m_ids.find( retval ) != m_ids.end() );

        m_ids[ retval ] = element;
    }
    else
    {
        // NOTE: add the id to map if not already there. id sharing is used in undo copies
        if( m_force_id != DEID::OMIT && m_ids.find( m_force_id ) == m_ids.end() )
            m_ids[ m_force_id ] = element;

        retval = m_force_id;
        m_force_id = DEID::UNSET; // consume the forced id
    }

    return retval;
}

DiaryElement*
Diary::get_element( D::DEID id ) const
{
    PoolLoGIDs::const_iterator iter { m_ids.find( id ) };
    return( iter == m_ids.end() ? nullptr : iter->second );
}
void
Diary::reclaim_id_for_elem( DiaryElement* new_owner )
{
    if( m_ids.find( new_owner->get_id() ) != m_ids.end() )
        m_ids[ new_owner->get_id() ] = new_owner;
}

// DiaryElement*
// Diary::get_element_shelved( DEID id ) const
// {
//     PoolDEIDs::const_iterator iter { m_ids_shelved.find( id ) };
//     return( iter == m_ids_shelved.end() ? nullptr : iter->second );
// }
// void
// Diary::shelve_element( DEID id )
// {
//     auto iter { m_ids.find( id ) };
//     if( iter != m_ids.end() && iter->second )
//     {
//         m_ids_shelved[ id ] = iter->second;
//         m_ids[ id ] = nullptr; // keep the id reserved for the shelved
//     }
// }
// void
// Diary::unshelve_element( DEID id )
// {
//     auto iter { m_ids_shelved.find( id ) };
//     if( iter != m_ids_shelved.end() )
//     {
//         m_ids[ id ] = iter->second;
//         m_ids_shelved.erase( id );
//     }
// }

Entry*
Diary::get_startup_entry( bool F_failsafe ) const
{
    Entry* entry{ nullptr };

    switch( m_startup_entry_id )
    {
        case DEID::MOST_CURRENT_ENTRY:
            entry = get_entry_most_current();
            break;
        case DEID::LAST_ENTRY:
            entry = get_entry_by_id( m_last_entry_id );
            break;
        case DEID::UNSET:
            break;
        default:
            entry = get_entry_by_id( m_startup_entry_id );
            break;
    }

    if( F_failsafe && !entry )
    {
        entry = m_p2entry_1st;
        print_info( "Failed to detect a startup entry. Will show the first entry.");
    }

    return entry;
}

Entry*
Diary::get_entry_most_current() const
{
    DateV   date        { Date::get_today() };
    DateV   diff_final  { Date::LATEST };
    DateV   diff_cur;
    Entry*  e_most_curr { nullptr };

    for( auto& kv_entry : m_entries )
    {
        Entry* entry { kv_entry.second };

        if( ! entry->is_filtered_out() )
        {
            diff_cur = ( entry->get_date() < date ? date - entry->get_date()
                                                  : entry->get_date() - date );
            if( diff_cur < diff_final )
            {
                diff_final = diff_cur;
                e_most_curr = entry;
            }
            else
                break;
        }
    }

    return e_most_curr;
}

// ENTRIES
Entry*
Diary::get_entry_today() const
{
    for( auto e : get_entries_by_date( Date::get_today() ) )
        if( e->get_title_style() == VT::ETS::DATE_AND_NAME::I ) // only return dated entries
            return e;
    return nullptr;
}

Entry*
Diary::get_entry_by_date( const DateV date, bool filtered_too ) const
{
    EntryIterConst iter( m_entries.find( Date::isolate_YMD( date ) ) );
    if( iter != m_entries.end() )
        if( filtered_too || iter->second->is_filtered_out() == false )
            return iter->second;

    return nullptr;
}

VecEntries
Diary::get_entries_by_date( DateV date, bool filtered_too ) const // takes pure date
{
    VecEntries ev;
    auto       range{ m_entries.equal_range( Date::isolate_YMD( date ) ) };

    for( auto& iter = range.first; iter!=range.second; ++iter )
        if( filtered_too || iter->second->is_filtered_out() == false )
            ev.push_back( iter->second );

    return ev;
}

Entry*
Diary::get_entry_by_name( const Ustring& name ) const
{
    // iterating using m_p2entry_1st is not an option here because during db upgrade
    // m_p2entry_1st is not ready when this function is called
    for( auto kv_entry : m_entries )
    {
        if( kv_entry.second->m_name == name )
            return kv_entry.second;
    }

    return nullptr;
}
Entry*
Diary::get_entry_by_name_fuzzy( const Ustring& name ) const
{
    VecUstrings entry_names;
    VecEntries  entries;

    for( auto e : *this )
    {
        entry_names.push_back( e->get_name() );
        entries.push_back( e );
    }

    auto match { FuzzyFinder::search_simple( name, entry_names ) };

    return( match.idx >= 0 ? entries[ match.idx ] : nullptr );
}

DiaryElemTag*
Diary::get_tag_by_name( const Ustring& name ) const
{
    for( auto kv_tag : m_cache_tags )
    {
        if( kv_tag.second->get_name() == name )
            return kv_tag.second;
    }

    return nullptr;
}
DiaryElemTag*
Diary::get_tag_by_name_fuzzy( const Ustring& name ) const
{
    VecUstrings tag_names;
    VecTags     tags;

    for( auto kv_tag : m_cache_tags )
    {
        tag_names.push_back( kv_tag.second->get_name() );
        tags.push_back( kv_tag.second );
    }

    auto match { FuzzyFinder::search_simple( name, tag_names ) };

    return( match.idx >= 0 ? tags[ match.idx ] : nullptr );
}

VecEntries
Diary::get_entries_by_filter( const Filter* filter ) const
{
    VecEntries          ev;
    FiltererContainer*  fc { filter ? filter->get_filterer_stack() : nullptr };

    for( auto& kv_entry : m_entries )
        if( !fc || fc->filter( kv_entry.second ) )
            ev.push_back( kv_entry.second );

    if( fc )
        delete fc;

    return ev;
}

unsigned int
Diary::get_entry_count_on_day( const DateV date ) const
{
    return m_entries.count( date );
}

// Entry*
// Diary::get_entry_next_in_day( const Date& date ) const
// {
//     if( date.is_ordinal() )
//         return nullptr;

//     for( auto iter = m_entries.rbegin(); iter != m_entries.rend(); iter++ )
//     {
//         if( date.get_day() == Date::get_day( iter->first ) &&
//             date.get_order_3rd() < Date::get_order_3rd( iter->first ) )
//             return( iter->second );
//     }

//     return nullptr;
// }

Entry*
Diary::get_entry_first_untrashed() const
{
    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
    {
        if( ! entry->is_trashed() )
            return entry;
    }
    return nullptr;
}

Entry*
Diary::get_entry_latest() const
{
    return( m_entries.rbegin()->second );
}

Ustring
Diary::get_entry_name( D::DEID id ) const
{
    if( id >= DEID::MIN_ACTUAL )
    {
        auto entry { get_entry_by_id( id ) };
        return( entry ? entry->get_name() : get_sstr_i( CSTR::NA ) );
    }
    else
        return get_sstr_i( id ); // for stock entries...
}

void
Diary::set_entry_date( Entry* entry, DateV date )
{
    for( auto&& iter = m_entries.begin(); iter != m_entries.end(); ++iter )
    {
        if( iter->second == entry )
        {
            m_entries.erase( iter );
            break;
        }
    }

    entry->m_date = date;
    m_entries.emplace( Date::isolate_YMD( date ), entry );
}

void
Diary::update_tag_refs( D::DEID id, const Ustring& new_name )
{
    bool F_replacement_done { false };

    for( Entry* e = m_p2entry_1st; e; e = e->get_next_straight() )
    {
        for( Paragraph* para = e->m_p2para_1st; para; para = para->m_p2next )
        {
            for( auto&& format : para->m_formats )
            {
                if( format->type == VT::HFT_TAG && format->get_id_lo() == id )
                {
                    para->replace_text( format->pos_bgn,
                                        format->pos_end - format->pos_bgn,
                                        new_name, nullptr );
                    F_replacement_done = true;
                }
            }
        }
        if( F_replacement_done )
        {
            update_search_for_entry( e ); // replacements may have affected the matches
            F_replacement_done = false;
        }
    }
}

void
Diary::add_tag_to_cache( DiaryElemTag* elem, bool F_add )
{
    if( F_add )
        m_cache_tags[ elem->get_id() ] = elem;
    else
        m_cache_tags.erase( elem->get_id() );
}

void
Diary::sort_entry_siblings( Entry* entry, EntryComparer&& compare, int direction )
{
    auto swap_siblings = []( Entry* ep, Entry* en )
    {
        Entry* epp = ep->m_p2prev;
        Entry* enn = en->m_p2next;
        en->m_p2prev = epp;
        en->m_p2next = ep;
        ep->m_p2prev = en;
        ep->m_p2next = enn;
        if( epp ) epp->m_p2next = en;
        if( enn ) enn->m_p2prev = ep;
    };

    auto normalize = []( int64_t i  ) -> int
    {
        if( i < 0 ) return -1;
        if( i > 0 ) return 1;
        return 0;
    };

    Entry* entry_bgn = entry->get_sibling_1st();
    Entry* entry_end = entry->get_sibling_last();
    do
    {
        Entry* entry_bgn_new = entry_end;
        Entry* entry_end_new = entry_bgn;

        // forward loop
        for( Entry* e = entry_bgn; e != entry_end; )
        {
            if( normalize( compare( e, e->m_p2next ) ) == direction )
            {
                swap_siblings( e, e->m_p2next );
                if( e == entry_bgn )
                    entry_bgn = e->m_p2prev; // e->m_p2prev is the former e->m_p2next
                if( e->m_p2prev == entry_end )
                {
                    entry_end_new = e->m_p2prev;
                    break;
                }
                else
                    entry_end_new = e;
            }
            else
                e = e->m_p2next;
        }

        entry_end = entry_end_new;

        // backward loop
        for( Entry* e = entry_end; e != entry_bgn; )
        {
            if( normalize( compare( e->m_p2prev, e ) ) == direction )
            {
                swap_siblings( e->m_p2prev, e ); // note that order is different here than above
                if( e == entry_end )
                    entry_end = e->m_p2next;
                if( e->m_p2next == entry_bgn )
                {
                    entry_bgn_new = e->m_p2next;
                    break;
                }
                else
                    entry_bgn_new = e;
            }
            else
                e = e->m_p2prev;
        }

        entry_bgn = entry_bgn_new;
    }
    while( entry_bgn->get_sibling_order() < entry_end->get_sibling_order() );

    if( entry->m_p2parent )
        entry->m_p2parent->m_p2child_1st = entry->get_sibling_1st();
    else
        m_p2entry_1st = entry->get_sibling_1st();
}

Entry*
Diary::get_milestone_before( const DateV date ) const
{
    // TODO: 3.2: add a generic get_colsest entry to the toolset...
    // ...it will be nice to have in python as well
    // if( m_entries.empty() ) return nullptr;

    auto it { m_entries.lower_bound( date ) };

    // TODO: the following is a closest entry implementation
    // if( it != m_entries.begin() )
    // {
    //     auto prev { std::prev( it ) };
    //     if( it == m_entries.end() || ( prev->first - date ) <= ( date - it->first ) )
    //         it = prev;
    // }

    for( ; it != m_entries.end(); ++it )
    {
        Entry* p2entry { it->second };
        if( p2entry->get_date() <= date &&
            p2entry->is_filtered_out() == false &&
            p2entry->get_title_style() == VT::ETS::MILESTONE::I ) return p2entry;
    }

    return nullptr;
}

/** creates a new entry in a relative position
 *  @entry_rel  if is null adds a top level entry at the beginning or end depending
 *              the value of @F_parent (false=beginning, true=end)
 *  @F_parent   whether @entry_rel will be regarded as parent or sibling
 *              when true entry will be added as the first child to @entry_rel
 */
Entry*
Diary::create_entry( Entry* entry_rel, bool F_parent, DateV date, const Ustring& content,
                     int style )
{
    Entry* entry { new Entry( this, date, ES::ENTRY_DEFAULT ) };

    entry->set_text( content, &m_parser_bg );

    if( !entry_rel )
    {
        if( F_parent && m_p2entry_1st )
            m_p2entry_1st->get_sibling_last()->add_sibling_after( entry );
        else if( !m_p2entry_1st )
            m_p2entry_1st = entry;
        else
            m_p2entry_1st->add_sibling_before( entry );
    }
    else if( F_parent )
        entry_rel->add_child_1st( entry );
    else
        entry_rel->add_sibling_after( entry );

    // Entry* entry_stylistic_src{ entry->m_p2prev ? entry->m_p2prev :
    //                                ( entry->m_p2next ? entry->m_p2next : entry->m_p2parent ) };

    if( style == VT::ETS::INHERIT::I ) // must never be the case when entry_rel = NULL
    {
        if( entry_rel && entry_rel->get_title_style() != VT::ETS::MILESTONE::I )
            entry->set_title_style( entry_rel->get_title_style() );
        else
            entry->set_title_style( VT::ETS::DATE_AND_NAME::I );
    }
    else
        entry->set_title_style( style );

    if( entry_rel && entry_rel->is_theme_set() )
    {
        entry->set_theme( entry_rel->get_theme() );
    }

    m_entries.emplace( Date::isolate_YMD( entry->m_date ), entry );
    m_cache_tags[ entry->get_id() ] = entry;

    return( entry );
}

Entry*
Diary::duplicate_entry( Entry* entry_rel )
{
    Entry* entry = new Entry( this, entry_rel->get_date(), entry_rel->get_todo_status() );

    for( Paragraph* p = entry_rel->get_paragraph_1st(); p; p = p->m_p2next )
        entry->add_paragraphs_after( new Paragraph( p ), entry->get_paragraph_last() );

    if( entry->get_paragraph_1st() )
    {
        entry->get_paragraph_1st()->append( " (*)", nullptr );
        entry->update_name();
    }

    entry_rel->add_sibling_after( entry );

    entry->set_title_style( entry_rel->get_title_style() );
    entry->set_comment_style( entry_rel->get_comment_style() );

    if( entry_rel->is_theme_set() )
        entry->set_theme( entry_rel->get_theme() );

    m_entries.emplace( Date::isolate_YMD( entry->m_date ), entry );
    m_cache_tags[ entry->get_id() ] = entry;

    return( entry );
}

// THIS VERSION IS USED DURING DIARY FILE READ ONLY:
Entry*
Diary::create_entry( DateV date, bool flag_favorite, bool flag_trashed, bool flag_expanded )
{
    ElemStatus status{ ES::NOT_TODO |
                       ( flag_favorite ? ES::FAVORED : ES::NOT_FAVORED ) |
                       ( flag_trashed ? ES::TRASHED : ES::NOT_TRASHED ) };
    if( flag_expanded ) status |= ES::EXPANDED;

    Entry* entry = new Entry( this, date, status );

    m_entries.emplace( Date::isolate_YMD( date ), entry );
    m_cache_tags[ entry->get_id() ] = entry;

    return( entry );
}

Entry*
Diary::create_entry_dummy()
{
    return create_entry( nullptr, true, Date::get_today(), _( "New Entry" ),
                         VT::ETS::NAME_ONLY::I );
}
Entry*
Diary::create_entry_dated( Entry* entry_prev, DateV date, bool F_milestone )
{
    // FIRST DETECT THE IDEAL POSITION FOR THE ENTRY
    Entry*  entry_bgn   { entry_prev ? entry_prev : m_p2entry_1st };
    Entry*  entry_sibl1 { nullptr };
    Entry*  entry_sibl2 { nullptr };
    Entry*  entry_rel   { entry_prev };
    bool    F_parent    { false };

    // find two dated siblings to figure out the order:
    for( Entry* e = entry_bgn; e; e = e->m_p2next )
    {
        if( e->get_title_style() == VT::ETS::DATE_AND_NAME::I )
        {
            if( !entry_sibl1 )
                entry_sibl1 = e;
            else if( e->get_date() != entry_sibl1->get_date() )
            {
                entry_sibl2 = e;
                break;
            }
        }
    }

    // we got what we need to figure out the order:
    if( entry_sibl1 && entry_sibl2 )
    {
        const bool F_ascending { entry_sibl1->get_date() < entry_sibl2->get_date() };


        for( Entry* e = entry_bgn; e; )
        {
            if( F_ascending )
            {
                if( date >= e->get_date() )
                    // && e->get_title_style() == VT::ETS::DATE_AND_NAME::I <- should we?
                {
                    if( !e->m_p2next || date < e->m_p2next->get_date() )
                    {
                        entry_rel = e; // insert after e
                        break;
                    }
                    e = e->m_p2next;
                }
                else // date < e->get_date()
                {
                    if( !e->m_p2prev )
                    {
                        entry_rel = e->get_parent(); // parent == nullptr if top level
                        F_parent = entry_rel; // add as the first entry if parent == nullptr
                        break;
                    }
                    else if( date >= e->m_p2prev->get_date() )
                    {
                        entry_rel = e->m_p2prev; // insert after e
                        break;
                    }
                    e = e->m_p2prev;
                }
            }
            else // descending
            {
                if( date <= e->get_date() )
                    // && e->get_title_style() == VT::ETS::DATE_AND_NAME::I <- should we?
                {
                    if( !e->m_p2next || date > e->m_p2next->get_date() )
                    {
                        entry_rel = e; // insert after e
                        break;
                    }
                    e = e->m_p2next;
                }
                else // date > e->get_Date()
                {
                    if( !e->m_p2prev )
                    {
                        entry_rel = e->get_parent(); // parent == nullptr if top level
                        F_parent = entry_rel; // add as the first entry if parent == nullptr
                        break;
                    }
                    else if( date <= e->m_p2prev->get_date() )
                    {
                        entry_rel = e->m_p2prev; // insert after e
                        break;
                    }
                    e = e->m_p2prev;
                }
            }
        }
    }

    // CREATE THE ENTRY
    Entry* entry { create_entry( entry_rel,
                                 F_parent,
                                 date,
                                 "\n" + Date::format_string( date ),
                                 F_milestone ? VT::ETS::MILESTONE::I
                                             : VT::ETS::DATE_AND_NAME::I ) };

    return entry;
}

Entry*
Diary::remove_entry_from_hierarchy( Entry* entry )
{
    const Entry* parent { entry->get_parent() };

    if( entry->m_p2prev )
    {
        if( entry->m_p2child_1st ) // if has children
        {
            entry->m_p2prev->m_p2next = entry->m_p2child_1st;
            entry->m_p2child_1st->m_p2prev = entry->m_p2prev;
        }
        else
            entry->m_p2prev->m_p2next = entry->m_p2next;
    }

    if( entry->m_p2next )
    {
        if( entry->m_p2child_1st ) // if has children
        {
            Entry* child_last = entry->m_p2child_1st->get_sibling_last();
            entry->m_p2next->m_p2prev = child_last;
            child_last->m_p2next = entry->m_p2next;
        }
        else
            entry->m_p2next->m_p2prev = entry->m_p2prev;
    }

    if( entry->m_p2parent && entry->m_p2parent->m_p2child_1st == entry )
    {
        if( entry->m_p2child_1st )
        {
            entry->m_p2parent->m_p2child_1st = entry->m_p2child_1st;
        }
        else
            entry->m_p2parent->m_p2child_1st = entry->m_p2next;
    }

    // raise parent links for the children one level up:
    for( Entry* e = entry->m_p2child_1st; e != nullptr; e = e->m_p2next )
         e->m_p2parent = entry->m_p2parent;

    // the entry that will replace this after removal
    Entry* e_replacing = ( entry->m_p2child_1st ? entry->m_p2child_1st
                                : ( entry->m_p2next ? entry->m_p2next
                                        : ( entry->m_p2prev ? entry->m_p2prev
                                                : entry->m_p2parent ) ) );

    // fix Diary's 1st pointer if needed
    if( m_p2entry_1st == entry )
        m_p2entry_1st = e_replacing;

    entry->m_p2parent = nullptr;
    entry->m_p2prev = nullptr;
    entry->m_p2next = nullptr;

    // update the sibling orders:
    if( parent )
    {
        if( parent->get_child_1st() )
            parent->get_child_1st()->update_sibling_orders();
    }
    else
        m_p2entry_1st->update_sibling_orders();

    return e_replacing;
}

void
Diary::remove_entry_from_hierarchy_with_descendants( Entry* entry )
{
    if( entry->m_p2prev ) entry->m_p2prev->m_p2next = entry->m_p2next;
    if( entry->m_p2next ) entry->m_p2next->m_p2prev = entry->m_p2prev;
    if( entry->m_p2parent && entry->m_p2parent->m_p2child_1st == entry )
        entry->m_p2parent->m_p2child_1st = entry->m_p2next;

    if( m_p2entry_1st == entry )
        m_p2entry_1st = entry->m_p2next;

    if( entry->m_p2next )
        entry->m_p2next->update_sibling_orders();

    entry->m_p2parent = nullptr;
    entry->m_p2prev = nullptr;
    entry->m_p2next = nullptr;
}

void
Diary::move_entry( Entry* p2entry2move, Entry* p2entry_target, const DropPosition& position )
{
    remove_entry_from_hierarchy_with_descendants( p2entry2move );

    switch( position )
    {
        default: // BEFORE
            p2entry_target->add_sibling_before( p2entry2move );
            break;
        case DropPosition::INTO:
            if( !p2entry_target->has_children() ) // ensure that the child will be visible
                p2entry_target->set_expanded( true );
            p2entry_target->add_child_last( p2entry2move );
            break;
        case DropPosition::AFTER:
            p2entry_target->add_sibling_after( p2entry2move );
            break;
    }
}

void
Diary::move_entries( const EntrySelection* p2entries2move,
                     Entry* p2e_target,
                     const DropPosition& dp )
{
    // CAUTION: tampers with the entry list order. list orders have to be updated after this func
    Entry* e_prev { nullptr };
    for( Entry* e2m : *p2entries2move )
    {
        if( e_prev == nullptr )
            move_entry( e2m, p2e_target, dp );
        else
            move_entry( e2m, e_prev, DropPosition::AFTER );

        e_prev = e2m;
    }
}

Entry*
Diary::dismiss_entry( Entry* entry )
{
    // fix startup element:
    if( m_startup_entry_id == entry->get_id() )
        m_startup_entry_id = DEID::MOST_CURRENT_ENTRY;

    // remove from filters:
    remove_entry_from_filters( entry );

    // remove from map:
    for( auto iter = m_entries.begin(); iter!=m_entries.end(); ++iter )
        if( iter->second->is_equal_to( entry ) )
        {
            m_entries.erase( iter );
            break;
        }

    m_cache_tags.erase( entry->get_id() );

    // remove from hierarchy:
    Entry* entry_next = remove_entry_from_hierarchy( entry );

    // an open diary must always contain at least one entry
    if( entry_next == nullptr )
        entry_next = create_entry_dummy();

    delete entry;

    return entry_next;
}

bool
Diary::is_trash_empty() const
{
    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
        if( entry->is_trashed() )
            return false;

    return true;
}

int
Diary::get_time_span() const
{
    DateV dfd{ Date::NOT_SET };

    for( auto kv_e : m_entries )
    {
        const auto dfe{ kv_e.second->get_date_finish() };
        if( dfe > dfd )
            dfd = dfe;
    }

    return Date::calculate_days_between_abs( m_entries.rbegin()->first, dfd );
}

// SEARCHING
void
Diary::set_search_str( const Ustring& text )
{
    std::lock_guard< std::mutex > lock( m_mutex_search );
    m_search_text = text;
}
void
Diary::set_search_filter( const Filter* filter )
{
    //std::lock_guard< std::mutex > lock( m_mutex_search );
    m_p2filter_search = filter;
    if( m_fc_search ) delete m_fc_search;
    m_fc_search = ( filter ? filter->get_filterer_stack() : nullptr );
}

void
Diary::start_search( const int thread_count )
{
    const int entries_per_thread { int( ceil( double( Diary::d->get_size() ) / thread_count ) ) };

    clear_matches();

    if( m_options & VT::DO::SEARCH_USE_REGEX::I )
    {
        try
        {
            if( m_options & VT::DO::SEARCH_MATCH_CASE::I )
                m_search_regex = Glib::Regex::create( m_search_text,
                                                      Glib::Regex::CompileFlags::DEFAULT );
            else
                m_search_regex = Glib::Regex::create( m_search_text,
                                                      Glib::Regex::CompileFlags::DEFAULT |
                                                      Glib::Regex::CompileFlags::CASELESS );
        }
        catch ( const Glib::Error& e )
        {
            print_error( "Regex error:", e.what() );
            return;
        }
    }

    m_active_search_thread_count = thread_count;

    for( int i = 0; i < thread_count; i++ )
    {
        PRINT_DEBUG( "Adding thread #", i );
        const int i_bgn{ i * entries_per_thread };
        m_threads_search.push_back( new std::thread( &Diary::search_internal,
                                                     this,
                                                     i_bgn,
                                                     i_bgn + entries_per_thread ) );
    }
}

void
Diary::search_internal( int i_bgn, int i_end )
{
    if( !m_search_text.empty() )
    {
        // std::lock_guard< std::mutex > lock( m_mutex_search );

        FiltererContainer*  fc { m_p2filter_search ? m_p2filter_search->get_filterer_stack()
                                                   : nullptr };
        int                 i  { -1 };

        for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
        {
            if( ++i < i_bgn ) continue;
            else if( i >= i_end ) break;

            if( m_F_stop_search_thread )
            { PRINT_DEBUG( "--- INTERRUPTED SEARCH AT 1 ---" ); break; }

            if( fc && !fc->filter( entry ) )
                continue;

            search_internal_entry( entry );
        }
    }

    {
        std::lock_guard< std::mutex > lock( m_mutex_search );
        m_active_search_thread_count--;
        PRINT_DEBUG( "Searchng in entries: ", i_bgn, "...", i_end, " FINISHED" );
    }

    if( m_active_search_thread_count == 0 )
    {
        m_dispatcher_search.emit();
    }
}

inline void
Diary::search_internal_entry( const Entry* entry )
{
    const bool      F_match_case  { bool( m_options & VT::DO::SEARCH_MATCH_CASE::I ) };
    const auto      length        { m_search_text.length() }; // shouldn't be used in regex
    UstringSize     pos;

    for( Paragraph* para = entry->m_p2para_1st; para; para = para->m_p2next )
    {
        if( m_F_stop_search_thread )
        { PRINT_DEBUG( "--- INTERRUPTED SEARCH AT 2 ---" ); return; }

        if( m_options & VT::DO::SEARCH_USE_REGEX::I )
        {
            Glib::MatchInfo       match_info;

            for( bool F_matches = m_search_regex->match( para->get_text(), match_info );
                 F_matches;
                 F_matches = match_info.next() )
            {
                if( m_F_stop_search_thread )
                { PRINT_DEBUG( "--- INTERRUPTED SEARCH AT 3 ---" ); return; }

                int pos_bgn, pos_end;
                match_info.fetch_pos( 0, pos_bgn, pos_end );
                pos_bgn = STR::get_utf8_pos_from_byte_i( para->get_text(), pos_bgn );
                pos_end = STR::get_utf8_pos_from_byte_i( para->get_text(), pos_end );

                {
                    std::lock_guard< std::mutex > lock( m_mutex_search );
                    auto f { para->add_format( VT::HFT_MATCH, "", pos_bgn, pos_end ) };
                    f->ref_id = para->get_id_full().get_raw();
                    f->var_i = para->m_order_in_host;
                    f->var_d = entry->get_date(); // used for sorting
                    m_matches.insert( new HiddenFormat( *f ) );
                }
            }
        }
        else
        {
            const Ustring&& para_text  { F_match_case ? para->get_text()
                                                      : STR::lowercase( para->get_text() ) };

            pos = 0;
            while( ( pos = para_text.find( m_search_text, pos ) ) != Ustring::npos )
            {
                if( m_F_stop_search_thread )
                { PRINT_DEBUG( "--- INTERRUPTED SEARCH AT 3 ---" ); return; }

                {
                    std::lock_guard< std::mutex > lock( m_mutex_search );
                    auto f { para->add_format( VT::HFT_MATCH, "", pos, pos + length ) };
                    f->ref_id = para->get_id_full().get_raw();
                    f->var_i = para->m_order_in_host;
                    f->var_d = entry->get_date(); // used for sorting
                    m_matches.insert( new HiddenFormat( *f ) );
                }

                pos += length;
            }
        }
    }
}

void
Diary::remove_entry_from_search( const Entry* entry )
{
    if( m_search_text.empty() || !entry ) return;

    for( auto&& it_match = m_matches.begin(); it_match != m_matches.end(); )
    {
        if( ( *it_match )->get_id_hi() == entry->get_id() )
        {
            delete( *it_match );
            it_match = m_matches.erase( it_match );
        }
        else
            ++it_match;
    }

    for( Paragraph* para = entry->m_p2para_1st; para; para = para->m_p2next )
    {
        para->remove_formats_of_type( VT::HFT_MATCH );
    }
}

void
Diary::update_search_for_entry( const Entry* entry )
{
    if( m_search_text.empty() || !entry ) return;
    if( m_p2filter_search && !m_fc_search->filter( entry ) ) return;

    remove_entry_from_search( entry );
    search_internal_entry( entry );
}

void
Diary::replace_match( const HiddenFormat* match, const Ustring& text_new )
{
    Paragraph*  para      { get_paragraph_by_id( match->get_id_lo() ) };
    Entry*      e_host    { para->m_host };
    const auto  ref_id    { match->ref_id };
    const auto  pos_bgn_p { match->pos_bgn };
    const auto  pos_bgn_e { para->get_bgn_offset_in_host() + pos_bgn_p };
    const auto  l_match   { match->pos_end - match->pos_bgn };

    e_host->add_undo_action( UndoableType::MODIFY_TEXT, para, 1,
                             pos_bgn_e + l_match,
                             pos_bgn_e + text_new.length() );

    para->remove_format( match ); // deletes the original match
    para->replace_text( pos_bgn_p, l_match, text_new, &m_parser_bg );

    for( auto it{ m_matches.begin() }, end{ m_matches.end() }; it != end; )
    {
        HiddenFormat* f { *it };
        if( f->ref_id == ref_id && f->pos_bgn == pos_bgn_p )
        {
            delete f; // deletes the copy in the diary
            it = m_matches.erase( it );
        }
        else
            ++it;
    }
}

void
Diary::replace_all_matches( const Ustring& text_new )
{
    // TODO: v3.1: add undo support
    for( Entry* e = m_p2entry_1st; e; e = e->get_next_straight() )
    {
        for( Paragraph* p = e->m_p2para_1st; p; p = p->m_p2next )
        {
            for( auto it = p->m_formats.begin(); it != p->m_formats.end(); )
            {
                HiddenFormat* f { *it };

                if( f->type == VT::HFT_MATCH )
                {
                    const auto fmt_pos_bgn { f->pos_bgn };
                    const auto fmt_pos_end { f->pos_end };
                    // delete the match format in advance to prevent double deletion:
                    delete f;
                    it = p->m_formats.erase( it );
                    p->replace_text( fmt_pos_bgn, fmt_pos_end - fmt_pos_bgn, text_new );
                }
                else
                    ++it;
            }
        }
    }

    for( auto f : m_matches ) delete f;
    m_matches.clear();
}

// this version does not disturb the active search and is case-sensitive
void
Diary::replace_all_matches( const Ustring& oldtext, const Ustring& newtext )
{
    if( oldtext.empty() )
        return;

    UstringSize pos_para;
    // UstringSize pos_entry;
    const auto  length_old { oldtext.length() };
    const int   delta      { ( int ) newtext.length() - ( int ) length_old };
    int         delta_cum  { 0 };
    Paragraph*  para_cur   { nullptr };
    std::vector< std::pair< Paragraph*, int > >  matches;

    // FIND
    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
    {
        // pos_entry = 0;

        for( Paragraph* para = entry->m_p2para_1st; para; para = para->m_p2next )
        {
            const auto& para_text{ para->get_text() };
            pos_para = 0;

            while( ( pos_para = para_text.find( oldtext, pos_para ) ) != Ustring::npos )
            {
                matches.push_back( { para, pos_para } );
                pos_para += length_old;
            }

            // pos_entry += ( para_text.length() + 1 );
        }
    }

    // REPLACE
    for( auto&& m : matches )
    {
        if( m.first != para_cur )
        {
            delta_cum = 0;
            para_cur = m.first;
        }
        else
            m.second += delta_cum;

        m.first->erase_text( m.second, length_old, nullptr );
        m.first->insert_text( m.second, newtext, &m_parser_bg );

        delta_cum += delta;
    }
}

HiddenFormat*
Diary::get_match_at( int i )
{
    std::lock_guard< std::mutex > lock( m_mutex_search );

    if( m_matches.empty() || i >= int( m_matches.size() ) )
        return nullptr;

    auto&& iter{ m_matches.begin() };
    std::advance( iter, i );

    return( *iter );
}

void
Diary::clear_matches()
{
    std::lock_guard< std::mutex > lock( m_mutex_search );

    for( auto f : m_matches )
    {
        Paragraph* p { get_paragraph_by_id( f->get_id_lo() ) };
        if( p )
            p->remove_format( f ); // deletes the original in the paragraph
        else
            print_error( "Match cannot be found in the entry. This is unexpected!" );

        delete f; // deletes the copy in the diary
    }

    m_matches.clear();
    m_F_stop_search_thread = false;
}

// FILTERS
bool
Diary::dismiss_filter( const Ustring& name )
{
    if( m_filters.size() < 2 )
        return false;

    auto&& it_filter{ m_filters.find( name ) };

    if( it_filter == m_filters.end() )
        return false;

    Filter* filter_to_delete{ it_filter->second };

    if( m_p2filter_list && filter_to_delete->get_id() == m_p2filter_list->get_id() )
    {
        Filter* filter_active_new{ nullptr };

        for( auto& kv_flt : m_filters )
            if( kv_flt.second->can_filter_class( FOC::ENTRIES ) &&
                kv_flt.second != filter_to_delete )
            {
                filter_active_new = kv_flt.second;
                break;
            }

        if( !filter_active_new )
        {
            print_info( "Last entry filter cannot be dismissed." );
            return false;
        }
        else
            m_p2filter_list = filter_active_new;
    }

    m_filters.erase( name );

    delete filter_to_delete;

    return true;
}

void
Diary::remove_entry_from_filters( Entry* entry )
{
    const Ustring     rep_str    { STR::compose( "Fr", DEID::UNSET.get_raw() ) };

    for( auto& kv_filter : m_filters )
    {
        Filter*       filter     { kv_filter.second };
        Ustring       definition { filter->get_definition() };
        const Ustring ref_str    { STR::compose( "Fr", entry->get_id().get_raw() ) };

        while( true )
        {
            auto pos{ definition.find( ref_str ) };
            if( pos != Ustring::npos )
                definition.replace( pos, ref_str.size(), rep_str );
            else
                break;
        }

        filter->set_definition( definition );
    }
}

bool
Diary::update_entry_filter_status( Entry* entry )
{
    auto       fc         { get_filter_list_stack() };
    const bool old_status { entry->is_filtered_out() };
    const bool new_status { fc ? !fc->filter( entry ) : false };

    entry->set_filtered_out( new_status );

    if( fc )
        delete fc;

    return ( old_status != new_status );
}
int
Diary::update_all_entries_filter_status()
{
    auto fc    { m_p2filter_list ? m_p2filter_list->get_filterer_stack() : nullptr };
    int  count { 0 };

    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
    {
        const bool F_filtered_in { fc ? fc->filter( entry ) : true };
        entry->set_filtered_out( ! F_filtered_in );
        if( F_filtered_in ) count++;
    }

    if( fc )
        delete fc;

    return count;
}

void
Diary::update_filter_user_counts()
{
    for( auto& kv_filter : m_filters )
        kv_filter.second->m_num_users = 0;

    // OTHER FILTERS
    for( const auto& kv_filter : m_filters )
    {
        const String&   def    { kv_filter.second->get_definition() };
        String          line;
        StringSize      line_o { 0 };

        while( STR::get_line( def, line_o, line ) )
        {
            if( STR::begins_with( line, "Fl" ) )
            {
                Filter* p2f{ get_filter( D::DEID( line.substr( 2 ) ) ) };

                if( !p2f )
                    print_info( "Dangling reference to Filter: ", line );
                else
                    p2f->m_num_users++;
            }
        }
    }
    // TODO: 3.2: if filter referring to the other filter is itself unused we can detect it...
    // ...and reset the user count here

    // CHARTS (not now)
    // for( const auto& kv_chart : m_charts )
    // {

    // }

    // TABLES
    for( const auto& kv_table : m_tables )
    {
        const String&   def    { kv_table.second->get_definition() };
        String          line;
        StringSize      line_o { 0 };

        while( STR::get_line( def, line_o, line ) )
        {
            Filter* p2f{ ( Filter* ) 0x1 };

            if     ( STR::begins_with( line, "Mcgf" ) )
                p2f = get_filter( D::DEID( line.substr( 4 ) ) );
            else if( STR::begins_with( line, "Mccf" ) )
                p2f = get_filter( D::DEID( line.substr( 4 ) ) );
            else if( STR::begins_with( line, "Mf" ) )
                p2f = get_filter( D::DEID( line.substr( 2 ) ) );
            else if( STR::begins_with( line, "Ml" ) )
                p2f = get_filter( D::DEID( line.substr( 2 ) ) );

            if( !p2f )
                print_info( "Dangling reference to Filter: ", line );
            else
            if( p2f != ( Filter* ) 0x1 )
                p2f->m_num_users++;
        }
    }

    if( m_p2filter_search ) const_cast< Filter* >( m_p2filter_search )->m_num_users++;
    if( m_p2filter_list ) const_cast< Filter* >( m_p2filter_list )->m_num_users++;
    // an alternative way of dropping constness:
    // get_filter( m_p2filter_active->get_id() )->m_num_users++;
}

// CHARTS

// TABLES

// THEMES
void
Diary::dismiss_theme( Theme* theme )
{
    if( m_themes.size() == 1 ) throw LoG::Error( "Trying to delete the last theme" );

    for( Entry* entry = m_p2entry_1st; entry; entry = entry->get_next_straight() )
    {
        if( entry->get_theme() == theme )
            entry->set_theme( nullptr );
    }

    // remove from associated filters
    for( auto& kv_filter : m_filters )
    {
        Ustring def{ kv_filter.second->get_definition() };
        auto    pos{ def.find( "\nFh" + theme->get_name() ) };
        if( pos != Ustring::npos )
        {
            def.erase( pos, theme->get_name().length() + 3 );
            kv_filter.second->set_definition( def );
        }
    }

    m_themes.erase( theme->get_name() );
    delete theme;
}

// WORK DAYS
int
Diary::calculate_work_days_between( const DateV d1, const DateV d2 )
{
    // NOTE: also check Date::calculate_days_between
    unsigned int    dist        { 0 };
    DateV           date_former { Date::isolate_YMD( d1 < d2 ? d1 : d2 ) };
    const DateV     date_latter { Date::isolate_YMD( d1 < d2 ? d2 : d1 ) };

    while( date_former != date_latter )
    {
        if( is_day_workday( date_former ) ) dist++;

        Date::forward_days( date_former, 1 );
    }

    return dist;
}

// IMPORTING
void
Diary::synchronize_options( const Diary* diary_r )
{
    m_language = diary_r->m_language;
    m_options = diary_r->m_options;

    if( diary_r->m_p2filter_list )
    {
        auto p2filter { get_element2< Filter >( diary_r->m_p2filter_list->get_id() ) };
        if( p2filter )
            m_p2filter_list = p2filter;
    }
    else
        m_p2filter_list = nullptr;

    if( diary_r->m_p2filter_search )
    {
        auto p2filter { get_element2< Filter >( diary_r->m_p2filter_search->get_id() ) };
        if( p2filter )
            m_p2filter_search = p2filter;
    }
    else
        m_p2filter_search = nullptr;

    if( diary_r->m_startup_entry_id < DEID::MIN ||
        get_entry_by_id( diary_r->m_startup_entry_id ) )
    {
        m_startup_entry_id = diary_r->m_startup_entry_id;
    }

    if( diary_r->m_completion_tag_id < DEID::MIN ||
        get_tag_by_id( diary_r->m_completion_tag_id ) )
    {
        m_completion_tag_id = diary_r->m_completion_tag_id;
    }
}

bool
Diary::import_entry( const Entry* entry_r, Entry* tag_for_imported, bool F_add )
{
    if( F_add && !get_element( entry_r->get_id() ) ) // if id is available
        m_force_id = entry_r->get_id();

    Entry* entry_l { F_add ? create_entry( nullptr, true, entry_r->m_date, "" )
                           : get_entry_by_id( entry_r->get_id() ) };

    if( !entry_l ) return false;


    if( !F_add )
    {
        entry_l->clear_text();
        entry_l->clear_properties();
    }

    // copy the text:
    Paragraph* p_new { nullptr };
    for( auto p = entry_r->m_p2para_1st; p; p = p->m_p2next )
    {
        try_force_id( p->get_id() );
        p_new = entry_l->add_paragraphs_after( new Paragraph( p, this ), p_new,
                                                              ParaInhClass::SET_TEXT );
    }

    // preserve the theme:
    if( entry_r->is_theme_set() )
    {
        auto theme_l { get_element2< Theme >( entry_r->get_theme()->get_id() ) };
        if( theme_l )
            entry_l->set_theme( theme_l );
    }

    if( tag_for_imported )
        entry_l->add_tag( tag_for_imported );

    // copy other data:
    entry_l->m_date_created = entry_r->m_date_created;
    entry_l->set_date_edited( entry_r->m_date_edited );
    // entry_l->m_date_finish = entry_r->m_date_finish; can be calculated
    entry_l->m_status = entry_r->m_status;
    entry_l->m_style = entry_r->m_style;

    entry_l->m_F_map_path_old = true; // force update

    for( auto& kv_prop : entry_r->m_properties )
        entry_l->m_properties.emplace( kv_prop.first, kv_prop.second );

    // update per the filtering configuration of the new host diary:
    update_entry_filter_status( entry_l );

    return true;
}

bool
Diary::import_theme( const Theme* theme_r, bool F_add )
{
    if( F_add && !get_element( theme_r->get_id() ) )
        m_force_id = theme_r->get_id();

    Theme* theme_l{ F_add ?
            create_theme( create_unique_name_for_map( m_themes, theme_r->get_name() ) ) :
            dynamic_cast< Theme* >( get_element( theme_r->get_id() ) ) };

    if( theme_l == nullptr )
        return false;

    if( !F_add )
        rename_theme( theme_l, theme_r->get_name() );

    theme_r->copy_to( theme_l );

    return true;
}

const DiaryElement*
Diary::get_corresponding_elem( const DiaryElement* elem_r ) const
{
    DiaryElement* elem_l{ get_element( elem_r->get_id() ) };
    if( elem_l && elem_r->get_type() == elem_l->get_type() )
        return elem_l;
    else
        return nullptr;
}

D::CSTR
Diary::compare_foreign_elem( const DiaryElement* elem_r,
                             const DiaryElement*& elem_l ) const
{
    elem_l = get_corresponding_elem( elem_r );

    if( elem_l == nullptr )
        return CSTR::NEW;
    else if( elem_r->get_as_skvvec() == elem_l->get_as_skvvec() )
        return CSTR::INTACT;

    //else
    return CSTR::CHANGED;
}
