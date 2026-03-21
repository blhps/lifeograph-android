/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

    Parts of this file are loosely based on an example gcrypt program
    on http://punkroy.drque.net/

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

#include <iomanip>

#ifndef __ANDROID__
#include "helpers_gtk.hpp"
#endif

#include "logid.hpp"


namespace HELPERS
{

Error::Error( const Ustring& error_message )
: description( error_message )
{
    print_error( error_message );
}

#if LIFEOGRAPH_DEBUG_BUILD
unsigned Console::DEBUG_LINE_I( 0 );
#endif

// DATETIME ========================================================================================
String      Date::s_format_order        = "YMD";
char        Date::s_format_separator    = '.';
int         Date::s_week_start_day      = 1;

// MAKERS...........................................................................................
DateV
Date::make( const String& str_date )
{
    char c_cur;
    unsigned int num[ 4 ] = { 0, 0, 0, 0 };  // fourth int is for trailing spaces
    int i( 0 );

    for( unsigned j = 0; j < str_date.size(); j++ )
    {
        c_cur = str_date[ j ];
        switch( c_cur )
        {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                if( i > 2 )
                    return NOT_SET;
                num[ i ] *= 10;
                num[ i ] += ( c_cur - '0' );
                break;
            case ' ':
                if( num[ i ] > 0 )
                    i++;
                break;
            case '.':
            case '-':
            case '/':
                if( num[ i ] == 0 || i == 2 )
                    return NOT_SET;
                else
                    i++;
                break;
            default:
                return NOT_SET;
        }
    }

    if( num[ 2 ] ) // temporal
    {
        unsigned int year( 0 );
        unsigned int month( 0 );
        unsigned int day( 0 );

        if( num[ 0 ] > 31 && num[ 1 ] <= 12 && num[ 2 ] <= 31 ) // YMD
        {
            year = num[ 0 ];
            month = num[ 1 ];
            day = num[ 2 ];
        }
        else
        {
            if( num[ 0 ] <= 12 && num[ 1 ] <= 12 ) // both DMY and MDY possible
            {
                if( s_format_order[ 0 ] == 'M' )
                {
                    month = num[ 0 ];
                    day = num[ 1 ];
                }
                else
                {
                    day = num[ 0 ];
                    month = num[ 1 ];
                }
            }
            else if( num[ 0 ] <= 31 && num[ 1 ] <= 12 ) // DMY
            {
                month = num[ 1 ];
                day = num[ 0 ];
            }
            else if( num[ 0 ] <= 12 && num[ 1 ] <= 31 ) // MDY
            {
                month = num[ 1 ];
                day = num[ 0 ];
            }
            else
                return NOT_SET;

            year = num[ 2 ];

            if( year < 100 )
                year += ( year < 30 ? 2000 : 1900 );
        }

        if( year < YEAR_MIN || year > YEAR_MAX )
            return NOT_SET;

        DateV date_tmp{ make( year, month, day ) };
        if( ! is_valid( date_tmp ) ) // checks days in month
            return NOT_SET;
        else
            return date_tmp;
    }

    else
        return NOT_SET;
}

DateV
Date::make( const Glib::DateTime& gd )
{
    return make( gd.get_year(), gd.get_month(), gd.get_day_of_month(),
                 gd.get_hour(), gd.get_minute(), gd.get_second() );
}

// CHECKERS.........................................................................................
bool
Date::is_leap_year_gen( const unsigned year )
{
    if( ( year % 400 ) == 0 )
        return true;
    else if( ( year % 100 ) == 0 )
        return false;

    return( ( year % 4 ) == 0 );
}

// GETTERS..........................................................................................
// time_t
// DateTime::get_ctime( const DateTimeV d )
// {
//     time_t t;
//     time( &t );
//     struct tm* timeinfo = localtime( &t );
//     timeinfo->tm_year = get_year( d ) - 1900;
//     timeinfo->tm_mon = get_month( d ) - 1;
//     timeinfo->tm_mday = get_day( d );
//     timeinfo->tm_hour = get_hour( d );
//     timeinfo->tm_min = get_min( d );
//     timeinfo->tm_sec = get_sec( d );

//     return( mktime( timeinfo ) );
// }

Glib::Date::Month
Date::get_month_glib( DateV date )
{
    switch( get_month( date ) )
    {
        case 1:     return Glib::Date::Month::JANUARY;
        case 2:     return Glib::Date::Month::FEBRUARY;
        case 3:     return Glib::Date::Month::MARCH;
        case 4:     return Glib::Date::Month::APRIL;
        case 5:     return Glib::Date::Month::MAY;
        case 6:     return Glib::Date::Month::JUNE;
        case 7:     return Glib::Date::Month::JULY;
        case 8:     return Glib::Date::Month::AUGUST;
        case 9:     return Glib::Date::Month::SEPTEMBER;
        case 10:    return Glib::Date::Month::OCTOBER;
        case 11:    return Glib::Date::Month::NOVEMBER;
        case 12:    return Glib::Date::Month::DECEMBER;
        default:    return Glib::Date::Month::BAD_MONTH;
    }
}

unsigned
Date::get_week( const DateV d )
{
    int yearday = get_yearday( d );
    int wd_jan1 = get_weekday( make( get_year( d ), 1, 1 ) );
    int first_week_length = ( 7 - wd_jan1 + s_week_start_day ) % 7;

    int week_no = ( int ) ceil( ( yearday - first_week_length ) / 7.0 );

    if( first_week_length >= 4 )
        ++week_no;

    return week_no;
}

unsigned
Date::get_weekday( const DateV date ) // sunday = 0
{
    // from wikipedia: http://en.wikipedia.org/wiki/Calculating_the_day_of_the_week
    const unsigned int year = get_year( date );
    const unsigned int century = ( year - ( year % 100 ) ) / 100;
    int c = 2 * ( 3 - ( century % 4 ) );
    int y = year % 100;
    y = y + floor( y / 4 );

    static const int t_m[] = { 0, 3, 3, 6, 1, 4, 6, 2, 5, 0, 3, 5 };

    int m = get_month( date ) - 1;
    int d = ( c + y + t_m[ m ] + get_day( date ) );

    if( m < 2 && is_leap_year( date ) )  // leap year!
        d += 6;

    return( d % 7 );
}

unsigned
Date::get_yearday( const DateV date )
{
    int result{ 0 };
    DateV d_m{ make( get_year( date ), 1, 1 ) };

    for( unsigned i = 1; i < 12; i++ )
    {
        if( i < get_month( date ) )
            result += get_days_in_month( d_m );
        else
            break;

        forward_months( d_m, 1 );
    }

    result += get_day( date );

    return result;
}

unsigned
Date::get_days_in_month( const DateV d )
{
    switch( get_month( d ) )
    {
        case 4: case 6: case 9: case 11:
            return 30;
        case 2:
            return is_leap_year( d ) ? 29 : 28;
    }

    return 31;
}

String
Date::get_duration_str( const DateV ds, const DateV df )
{
    String      sf;

    if     ( ds == df )
        return( STR::compose( Date::format_string( ds ), "   (0)" ) );
    else if( Date::isolate_year( df ) != Date::isolate_year( ds ) )
        sf = Date::format_string( df );
    else if( Date::isolate_month( df ) != Date::isolate_month( ds ) )
        sf = Date::format_string( df, "MD" );
    else
        sf = Date::format_string( df, "D" );

    return STR::compose( calculate_days_between_abs( ds, df ), " ", _( "day(s)" ),
                         "  (", Date::format_string( ds ), "...", sf, ")" );
}

// SHIFTERS.........................................................................................
void
Date::forward_months( DateV& d, unsigned int months )
{
    months += get_month( d );
    d -= isolate_MD( d );
    const auto mod_months{ months % 12 };
    if( mod_months == 0 )
    {
        d += make_year( ( months / 12 ) - 1 );
        d += make_month( 12 );
    }
    else
    {
        d += make_year( months / 12 );
        d += make_month( mod_months );
    }

    d += make_day( 1 );
}

void
Date::backward_months( DateV& d, unsigned int months )
{
    const int m_diff{ int( months ) - int( get_month( d ) ) };
    d -= isolate_MD( d );
    if( m_diff < 0 )
        d += make_month( -m_diff );
    else
    {
        d -= make_year( ( m_diff / 12 ) + 1 );
        d += make_month( 12 - ( m_diff % 12 ) );
    }

    d += make_day( get_days_in_month( d ) );
}

void
Date::forward_days( DateV& date, unsigned int n_days )
{
    const auto day_new = get_day( date ) + n_days;
    const auto days_in_month{ get_days_in_month( date ) };

    if( day_new > days_in_month )
    {
        forward_months( date, 1 );    // sets day to 1, hence -1 below
        if( day_new > days_in_month + 1 )
            forward_days( date, day_new - days_in_month - 1 );
    }
    else
        set_day( date, day_new );
}

void
Date::backward_days( DateV& date, unsigned int n_days )
{
    const int day_new = get_day( date ) - n_days;

    if( day_new <= 0 )
    {
        backward_months( date, 1 );    // sets day to the last day of previous month
        if( day_new < 0 )
            backward_days( date, -day_new );
    }
    else
        set_day( date, day_new );
}

void
Date::backward_to_week_start( DateV& date )
{
    const auto wd{ get_weekday( date ) };
    if( int( wd ) != s_week_start_day )
        backward_days( date, ( wd + 7 - s_week_start_day ) % 7 );

    date = isolate_YMD( date ); // resets time to start
}

// CONVERSION TO STRING.............................................................................
String
Date::format_string( const DateV d, const String& format, const char separator )
{
    String result;

    auto get_YMD = [ &d ]( char c ) -> unsigned int
    {
        switch( c )
        {
            case 'Y':
                return get_year( d );
            case 'M':
                return get_month( d );
            case 'D':
            default: // no error checking for now
                return get_day( d );
        }
    };

    for( unsigned int i = 0; ; i++ )
    {
        const auto&& ymd{ get_YMD( format[ i ] ) };
        if( ymd < 10 ) result += '0';
        result += std::to_string( ymd );
        if( i == format.size() - 1 ) break;
        result += separator;
    }

    return result;
}

String
Date::format_string_adv( const DateV d, const String& format )
{
    String result;

    if( d == NOT_SET )
    {
        return "";
    }
    else
    {
        bool F_escape{ false };
        for( unsigned int i = 0; i < format.size(); i++ )
        {
            if( F_escape )
            {
                result += format[ i ];
                F_escape = false;
                continue;
            }
            switch( format[ i ] )
            {
                case 'Y':
                    result += std::to_string( get_year( d ) );
                    break;
                case 'M':
                    if( get_month( d ) < 10 ) result += '0';
                    result += std::to_string( get_month( d ) );
                    break;
                case 'n':
                    result += get_month_name( d );
                    break;
                case 'D':
                    if( get_day( d ) < 10 ) result += '0';
                    result += std::to_string( get_day( d ) );
                    break;
                case 'd':
                    result += get_day_name( get_weekday( d ) );
                    break;
                case 'W':
                {
                    auto&& weekno{ get_week( d ) };
                    if( weekno< 10 ) result += '0';
                    result += std::to_string( weekno );
                    break;
                }
                case 'h':
                    if( get_hours( d ) < 10 ) result += '0';
                    result += std::to_string( get_hours( d ) );
                    break;
                case 'm':
                    if( get_mins( d ) < 10 ) result += '0';
                    result += std::to_string( get_mins( d ) );
                    break;
                case 's':
                    if( get_secs( d ) < 10 ) result += '0';
                    result += std::to_string( get_secs( d ) );
                    break;
                case 'F':
                    result += format_string( d );
                    break;
                case 'T':
                    result += format_string_time( d );
                    break;
                case '\\':
                    F_escape = true;
                    break;
                default:
                    result += format[ i ];
                    break;
            }
        }
    }
    return result;
}

String
Date::get_format_str_default()
{
    String result;

    for( unsigned int i = 0; ; i++ )
    {
        result += s_format_order[ i ];
        if( i == s_format_order.size() - 1 ) break;
        result += s_format_separator;
    }

    return result;
}

Ustring
Date::get_weekday_str( const DateV d )
{
    return( get_day_name( get_weekday( d ) ) );
}

Ustring
Date::get_month_name( const DateV d )
{
    using namespace LoG;
    return( get_sstr_i( LoGID32( CSTR::JANUARY.get_raw() + get_month( d ) - 1 ) ) );
}

Ustring
Date::get_day_name( int i ) // sunday = 0
{
    using namespace LoG;
    return( get_sstr_i( LoGID32( CSTR::SUNDAY.get_raw() + ( i % 7 ) ) ) );
}

// CALCULATORS......................................................................................
uint64_t
Date::calculate_secs_between_abs( const DateV d1, const DateV d2 )
{
    DateV           date_former { d1 < d2 ? d1 : d2 };
    const DateV     date_latter { d1 < d2 ? d2 : d1 };
    uint64_t        dist        { calculate_days_between_abs( date_former, date_latter ) * 86400 };

    dist += ( ( Date::get_hours( date_latter ) - Date::get_hours( date_former ) ) * 3600 );
    dist += ( ( Date::get_mins( date_latter ) - Date::get_mins( date_former ) ) * 60 );
    dist += ( Date::get_secs( date_latter ) - Date::get_secs( date_former ) );

    return dist;
}

unsigned int
Date::calculate_days_between_abs( const DateV d1, const DateV d2 )
{
    unsigned int    dist        { 0 };
    DateV           date_former { d1 < d2 ? d1 : d2 };
    const DateV     date_latter { d1 < d2 ? d2 : d1 };

    if( Date::isolate_YM( date_latter ) == Date::isolate_YM( date_former ) )
    {
        return ( Date::get_day( date_latter ) - Date::get_day( date_former ) );
    }
    else
    {
        dist = ( Date::get_days_in_month( date_former ) - Date::get_day( date_former ) + 1 );
        Date::forward_months( date_former, 1 );
    }

    while( Date::isolate_YM( date_latter ) != Date::isolate_YM( date_former ) )
    {
        dist += ( Date::get_days_in_month( date_former ) );
        Date::forward_months( date_former, 1 );
    }

    dist += ( Date::get_day( date_latter ) - Date::get_day( date_former ) );
    return dist;
}

unsigned int
Date::calculate_weeks_between_abs( const DateV d1, const DateV d2 )
{
    unsigned int dist{ 0 };
    DateV date_former{ isolate_YMD( d1 < d2 ? d1 : d2 ) };
    DateV date_latter{ isolate_YMD( d1 < d2 ? d2 : d1 ) };

    backward_to_week_start( date_former );
    backward_to_week_start( date_latter );

    while( date_former < date_latter )
    {
        forward_days( date_former, 7 );
        dist++;
    }

    return dist;
}

int
Date::calculate_months_between( const DateV d1, const DateV d2 )
{
    int dist{ 12 * int( get_year( d2 ) - get_year( d1 ) ) };
    dist += ( get_month( d2 ) - get_month( d1 ) );

    return dist;
}

// COLOR OPERATIONS ================================================================================
Color
contrast2( const Color& bg, const Color& c1, const Color& c2 )
{
    const float dist1{ get_color_diff( bg, c1 ) };
    const float dist2{ get_color_diff( bg, c2 ) };

    if( dist1 > dist2 )
        return c1;
    else
        return c2;
}

Color
midtone( const Color& c1, const Color& c2 )
{
    Color midtone;
    midtone.set_red_u( ( c1.get_red_u() + c2.get_red_u() ) / 2.0 );
    midtone.set_green_u( ( c1.get_green_u() + c2.get_green_u() ) / 2.0 );
    midtone.set_blue_u( ( c1.get_blue_u() + c2.get_blue_u() ) / 2.0 );
    midtone.set_alpha( 1.0 );
    return midtone;
}

Color
midtone( const Color& c1, const Color& c2, double ratio, double alpha )
{
    Color midtone;
    midtone.set_red_u( ( c1.get_red_u() * ( 1.0 - ratio ) ) + ( c2.get_red_u() * ratio ) );
    midtone.set_green_u( ( c1.get_green_u() * ( 1.0 - ratio ) ) + ( c2.get_green_u() * ratio ) );
    midtone.set_blue_u( ( c1.get_blue_u() * ( 1.0 - ratio ) ) + ( c2.get_blue_u() * ratio ) );
    midtone.set_alpha( alpha );
    return midtone;
}

// FILE OPERATIONS =================================================================================
#ifdef _WIN32
String
get_exec_path()
{
    char*   buf   { g_win32_get_package_installation_directory_of_module( nullptr ) };
    String  path  { buf };
    g_free( buf );
    return path;
}
#endif

#ifndef __ANDROID__
std::ios::pos_type
get_file_size( std::ifstream& file )
{
   std::ios::pos_type size;
   const std::ios::pos_type currentPosition = file.tellg();

   file.seekg( 0, std::ios_base::end );
   size = file.tellg();
   file.seekg( currentPosition );

   return size;
}

bool
copy_file( const String& source_path, const String& target_path_root, bool F_unique )
{
    try
    {
        auto    file_src    { Gio::File::create_for_commandline_arg( source_path ) };
        int     index       { 0 };
        auto    file_dest   { Gio::File::create_for_commandline_arg( target_path_root ) };

        while( F_unique && file_dest->query_exists() )
        {
            file_dest = Gio::File::create_for_commandline_arg(
                    target_path_root + " (" + std::to_string( ++index ) + ")" );
        }

        file_src->copy( file_dest, F_unique ? Gio::File::CopyFlags::NONE
                                            : Gio::File::CopyFlags::OVERWRITE );

        return true;
    }
    catch( ... )
    {
        PRINT_DEBUG( "Copy file failed!" );
    }

    return false;
}

bool
copy_file_suffix( const String& source_uri, const String& suffix1, int suffix2, bool F_unique )
{
    try
    {
        auto          file_src    { Gio::File::create_for_uri( source_uri ) };
        const String  target_path { source_uri + Glib::uri_escape_string( suffix1 )
                                               + ( suffix2 >= 0 ? std::to_string( suffix2 )
                                                                : "" ) };
        int           index       { 0 };
        auto          file_dest   { Gio::File::create_for_uri( target_path ) };

        while( F_unique && file_dest->query_exists() )
        {
            file_dest = Gio::File::create_for_uri( target_path + "%20("
                                                               + std::to_string( ++index )
                                                               + ")"   );
        }

        file_src->copy( file_dest );
    }
    catch( ... )
    {
        return false;
    }
    return true;
}

bool
is_dir( const String& path )
{
    auto file { Gio::File::create_for_commandline_arg( path ) };
    return( file->query_file_type() == Gio::FileType::DIRECTORY );
}

bool
is_dir( const Glib::RefPtr< Gio::File >& file )
{
    return( file->query_file_type() == Gio::FileType::DIRECTORY );
}

bool
check_path_exists( const String& uri_or_path )
{
    auto file{ Gio::File::create_for_commandline_arg( uri_or_path ) };
    return file->query_exists();
}

bool
check_uri_writable( const Glib::RefPtr< Gio::File >& giofile )
{
    auto fileinf { giofile->query_info( "access::*", Gio::FileQueryInfoFlags::NONE ) };
    return( fileinf->get_attribute_boolean( G_FILE_ATTRIBUTE_ACCESS_CAN_WRITE ) );
}

String
evaluate_path( const String& path )
{
    if( path.size() < 3 )
        return _( "FAILED" );
    if     ( STR::begins_with( path, "file://" ) ||
             STR::begins_with( path, "http://" ) ||
             STR::begins_with( path, "https://" ) ||
             STR::begins_with( path, "ftp://" )
    )
        return path;
    else if( STR::begins_with( path, "~/" ) )
        return( "file://" + Glib::get_home_dir() + path.substr( 1 ) );
    else if( STR::begins_with( path, "\\\\" ) ||
             ( Glib::Ascii::isalpha( path[ 0 ] ) && path[ 1 ] == ':' && path[ 2 ] == '\\' ) ||
             path[ 0 ] == '/' )
        return( "file://" + path );
    else
        return( "http://" + path );
}

Ustring
sanitize_file_path( const Ustring& path )
{
    const Ustring invalid_chars { "<>:\"/\\|?*" };
    Ustring       result        { path };

    for( auto ch : result )
    {
        if( invalid_chars.find( ch ) != Ustring::npos || ch < 32 )
            ch = '_';
    }
    return result;
}

void
get_all_files_in_path( const String& path, SetStrings& list )
{
    Gio::init();
    try
    {
        Glib::RefPtr< Gio::File > directory = Gio::File::create_for_path( path );
        if( directory )
        {
            Glib::RefPtr< Gio::FileEnumerator > enumerator = directory->enumerate_children();
            if( enumerator )
            {
                Glib::RefPtr< Gio::FileInfo > file_info = enumerator->next_file();
                while( file_info )
                {
                    if( file_info->get_file_type() != Gio::FileType::DIRECTORY )
                    {
                        list.insert( Glib::build_filename( path, file_info->get_name() ) );
                    }
                    file_info = enumerator->next_file();
                }
            }
        }
    }
    catch( const Gio::Error& error )
    {
        print_error( error.what() );
    }
}

String
read_text_file( const String& path )
{
    std::ifstream file( path, std::ios::in | std::ios::binary );

    if( !file ) throw Error( "Cannot open file" );

    return String( ( std::istreambuf_iterator< char >( file ) ),
                     std::istreambuf_iterator< char >() );
}
#endif // __ANDROID__

#ifdef _WIN32
String
convert_filename_to_win32_locale( const String& fn )
{
    char* buf = g_win32_locale_filename_from_utf8( fn.c_str() );
    std::string path{ buf };
    g_free( buf );
    return path;
}
#endif

// TEXT OPERATIONS =================================================================================
String
STR::format_percentage( double percentage )
{
    std::stringstream str;
    str << std::fixed << std::setprecision( 1 ) << percentage * 100 << '%';
    // TODO: 3.2: add locale support

    return str.str();
}

String
STR::format_hex( int num )
{
    std::stringstream str;
    str << std::hex << "0x" << num;

    return str.str();
}

String
STR::format_number( double number, int decimal_cnt )
{
    char        result[ 32 ];
    snprintf( result, 32, "%f", number );

    const auto  pos_point { strcspn( result, ".," ) };
    const auto  size      { strlen( result ) };
    String      str;

    // full length mode:
    if( decimal_cnt < 0 )
    {
        decimal_cnt = ( size - pos_point - 1 );
        for( unsigned i = size - 1; i > pos_point; --i )
        {
            if( result[ i ] == '0' )
                decimal_cnt--;
            else
                break;
        }
    }

    for( auto p = pos_point; p > 0; p-- )
    {
        if( p != pos_point && pos_point > 4 && ( ( pos_point - p ) % 3 ) == 0 )
            str.insert( str.begin(), ' ' ); // thousands separator for 10 000 or bigger

        str.insert( str.begin(), result[ p - 1 ] );
    }

    if( decimal_cnt > 0 )
    {
        str += '.'; // decimals separator

        for( auto p = pos_point + 1; p <= pos_point + decimal_cnt; p++ )
        {
            if( p >= size )
                str += '0'; // zero padding
            else
                str += result[ p ];
        }
    }

    return str;
}

String
STR::format_number_roman( int num, bool F_lower )
{
    // array of roman numeral characters
    const String  u_ch[]  { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
    const String  l_ch[]  { "m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i" };
    const String* p2chars { F_lower ? l_ch : u_ch };
    // array of corresponding integer values
    const int     values[]{ 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };

    String        result;

    for( int i = 0; i < 13; i++ )
    {
        while( num >= values[ i ] )
        {
            result += p2chars[ i ];
            num -= values[i];
        }
    }

    return result;
}

bool
STR::begins_with ( const String& str, const String& bgn )
{
    return( str.rfind( bgn, 0 ) == 0 );
}

bool
STR::ends_with( const String& str, const String& end )
{
    if( str.length() > end.length() )
        return( str.compare( str.length() - end.length(), end.length(), end ) == 0 );
    else
        return false;
}
bool
STR::ends_with( const String& str, const char end )
{
    if( str.empty() )
        return false;
    else
        return( str.at( str.length() - 1 ) == end );
}
bool
STR::ends_with_trimmed( const Ustring& str, const gunichar end )
{
    for( int i = str.length() - 1; i >= 0; --i )
    {
        const auto ch { str[ i ] };
        if( ch == end )
            return true;
        else if( !is_char_space( ch ) )
            return false;
    }
    return false;
}

char
STR::get_end_char( const String& str )
{
    return( str.at( str.length() - 1 ) );
}

gunichar
STR::get_last_nonspace_char( const Ustring& str )
{
    for( int i = str.length() - 1; i >= 0; --i )
    {
        const auto ch { str[ i ] };
        if( ch != ' ' && ch != '\t' )
            return ch;
    }
    return 0;
}

bool
STR::get_line( const String& source, StringSize& o, String& line )
{
    if( source.empty() || o >= source.size() )
        return false;

    std::string::size_type o_end{ source.find( '\n', o ) };

    if( o_end == std::string::npos )
    {
        line = source.substr( o );
        o = source.size();
    }
    else
    {
        line = source.substr( o, o_end - o );
        o = o_end + 1; // make o ready for the subsequent search
    }

    return true;
}
bool
STR::get_line( const char* source, unsigned int& o, String& line )
{
    if( !source || o >= sizeof( source ) )
        return false;

    const unsigned int size { sizeof( o ) };

    while( o < size )
    {
        if( source[ o ] == '\n' )
        {
            o++; // make it ready for the subsequent search
            break;
        }

        line += source[ o ];
        o++;
    }

    return true;
}

int
STR::replace( String& txt, const String& s_search, const String& s_replace )
{
    int occurrences{ 0 };

    for( auto pos = txt.find( s_search );
         pos != std::string::npos;
         pos = txt.find( s_search, pos + s_replace.size() ) )
    {
        txt.replace( pos, s_search.size(), s_replace );
        occurrences++;
    }

    return occurrences;
}

String
STR::replace_spaces( const String& txt )
{
    std::string output;
    char ch;

    for( unsigned int pos = 0 ; pos < txt.size(); pos++ )
    {
        ch = txt[ pos ];
        if( ch == ' ' || ch == '\t' )
            output += '_';
        else
            output += ch;
    }

    return output;
}

int64_t
STR::get_i64( const String& line, int& i )
{
    int64_t result{ 0 };

    for( ; i < int( line.size() ) && int ( line[ i ] ) >= '0' && int ( line[ i ] ) <= '9'; i++ )
        result = ( result * 10 ) + int ( line[ i ] ) - '0';

    return result;
}

int32_t
STR::get_i32( const String& line, int& i )
{
    long result{ 0 };

    for( ; i < int( line.size() ) && int ( line[ i ] ) >= '0' && int ( line[ i ] ) <= '9'; i++ )
        result = ( result * 10 ) + int ( line[ i ] ) - '0';

    return result;
}
int32_t
STR::get_i32( const String& text )
{
    try
    {
        return std::stoul( text );
    }
    catch( std::exception& e )
    {
        print_error( e.what() );
        return 0U;
    }
}

double
STR::get_d( const String& text )
{
    //NOTE: this implementation may be a little bit more forgiving than good for health
    double value{ 0.0 };
    //char lf{ '=' }; // =, \, #, $(unit)
    int    divider{ 0 };
    bool   negative{ false };
    Wchar  c;

    for( Ustring::size_type i = 0; i < text.size(); i++ )
    {
        c = text[ i ];
        switch( c )
        {
            case ',':
            case '.':
                if( !divider ) // note that if divider
                    divider = 1;
                break;
            case '-':
                negative = true;
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                value *= 10;
                value += ( c - '0' );
                if( divider )
                    divider *= 10;
                break;
            default:
                break;
        }
    }

    if( divider > 1 )
        value /= divider;
    if( negative )
        value *= -1;

    return value;
}

double
STR::get_d( const String& line, int& i, double undef_val )
{
    double value      { 0.0 };
    int    divider    { 0 };
    bool   negative   { false };
    bool   f_continue { true };
    Wchar  c;

    for( ; i < int( line.size() ); ++i )
    {
        c = line[ i ];
        switch( c )
        {
            case ',':
            case '.':
                if( !divider ) // note that if divider
                    divider = 1;
                break;
            case '-':
                negative = true;
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                value *= 10;
                value += ( c - '0' );
                if( divider )
                    divider *= 10;
                break;
            case '_': // this is a special case for dealing with unset values in StrDefElems
                if( !divider && !negative && value == 0.0 )
                    return undef_val;
                // else no break:
            default:
                --i;
                f_continue = false; // end loop
                break;
        }

        if( !f_continue ) break; // break before incrementing i
    }

    if( divider > 1 )
        value /= divider;
    if( negative )
        value *= -1;

    return value;
}

VecUstrings
STR::make_substr_vector( const Ustring& str, int length )
{
    VecUstrings vec;

    for( int i = 0; i < int( str.length() ); i += length )
        vec.push_back( str.substr( i, length ) );

    return vec;
}

Ustring
STR::lowercase( const Ustring& str )
{
    // fixes the Glib problem with the Turkish capital i (İ)
    Ustring result;
    for( Ustring::size_type i = 0; i < str.length(); i++ )
    {
        if( str.at( i ) == L'İ' )
            result += 'i';
        else
            result += Glib::Unicode::tolower( str.at( i ) );
    }

    return result;
}
Ustring
STR::sentencecase( const Ustring& str )
{
    Ustring result;
    int     sentence_start { -1 }; // '.' -> 0, ' ' -> 1

    for( Ustring::size_type i = 0; i < str.length(); i++ )
    {
        const auto ch { str.at( i ) };

        if( ch == '.' || ch == '!' || ch == '?' )
        {
            sentence_start = 0;
            result += ch;
        }
        else if( is_char_space( ch ) )
        {
            if( sentence_start == 0 )
                sentence_start = 1;

            result += ch;
        }
        else
        {
            if( i == 0 || sentence_start == 1 )
                result += Glib::Unicode::toupper( ch );
            else
                result += ch; // do not mess up with the existing uppercase chars

            sentence_start = -1; // reset
        }
    }

    return result;
}
Ustring
STR::titlecase( const Ustring& str )
{
    Ustring result;
    bool    word_start{ false };

    for( Ustring::size_type i = 0; i < str.length(); i++ )
    {
        const auto ch   { str.at( i ) };

        if( is_char_space( ch ) )
        {
            if( !word_start )
                word_start = true;

            result += ch;
        }
        else
        {
            if( i == 0 || word_start )
                result += Glib::Unicode::toupper( ch );
            else
                result += Glib::Unicode::tolower( ch );

            word_start = false; // reset
        }
    }

    return result;
}

int
STR::find_sentence_start_backwards( const String& str, StringSize pos_bgn )
{
    int space_count{ 0 };

    while( true )
    {
        const auto ch{ str.at( pos_bgn ) };

        if( is_char_space( ch ) )
            space_count++;
        else if( ch == '.' && space_count > 0 && ( pos_bgn + space_count + 1 ) < str.size() )
            return( pos_bgn + space_count + 1 );
        else
            space_count = 0;

        if( pos_bgn == 0 )
            break;
        else
            --pos_bgn;
    }

    return pos_bgn;
}
int
STR::find_word_start_backwards( const String& str, StringSize pos_bgn )
{
    while( true )
    {
        if( is_char_space( str.at( pos_bgn ) ) && ( pos_bgn + 1 ) < str.size() )
            return( pos_bgn + 1 );

        if( pos_bgn == 0 )
            break;
        else
            --pos_bgn;
    }

    return pos_bgn;
}

String
get_env_lang()
{
    static String s_lang_env( "" );
    if( s_lang_env.empty() )
    {
        String lang = Glib::getenv( "LANG" );
        if( lang.empty() || lang == "C" || lang == "c" )
            s_lang_env = "en";
        else
            s_lang_env = lang;
    }
    return s_lang_env;
}

#ifdef _WIN32

wchar_t*
convert_utf8_to_16( const Ustring& str8 )
{
    //wchar_t* str16 = new wchar_t[ str8.size() + 1 ];
    //MultiByteToWideChar( CP_UTF8, 0, str8.c_str(), str8.size() + 1, str16, str8.size() + 1 );

    gunichar2*  gstr16  { g_utf8_to_utf16( str8.c_str(), -1, NULL, NULL, NULL ) };
    wchar_t*    str16   { reinterpret_cast< wchar_t* >( gstr16 ) };

    return str16;
}

Ustring
convert_utf16_to_8( const wchar_t* str16 )
{
    //char* str8{ nullptr };
    //int size = WideCharToMultiByte( CP_UTF8, 0, str16, -1, str8, 0, NULL, NULL );
    //str8 = new char[ size ];
    //WideCharToMultiByte( CP_UTF8, 0, str16, -1, str8, size, NULL, NULL );

    auto    gstr16  { reinterpret_cast< gunichar2 const* >( str16 ) };
    gchar*  str8    { g_utf16_to_utf8( gstr16, -1, NULL, NULL, NULL ) };
    Ustring ustr    { str8 };
    g_free( str8 );

    return ustr;
}

#endif

// ENCRYPTION ======================================================================================
bool
Cipher::init()
{
    // http://www.gnupg.org/documentation/manuals/gcrypt/Initializing-the-library.html

    // initialize subsystems:
    if( ! gcry_check_version( NULL ) )  // TODO: 3.2 check version
    {
        print_error( "Libgcrypt version mismatch" );
        return false;
    }

    // disable secure memory
    gcry_control( GCRYCTL_DISABLE_SECMEM, 0 );

    // MAYBE LATER:
    /*
    // suppress warnings
    gcry_control( GCRYCTL_SUSPEND_SECMEM_WARN );

    // allocate a pool of 16k secure memory. this makes the secure memory...
    // ...available and also drops privileges where needed
    gcry_control( GCRYCTL_INIT_SECMEM, 16384, 0 );

    // resume warnings
    gcry_control( GCRYCTL_RESUME_SECMEM_WARN );
    */

    // tell Libgcrypt that initialization has completed
    gcry_control( GCRYCTL_INITIALIZATION_FINISHED, 0 );

    return true;
}

void
Cipher::create_iv( unsigned char** iv )
{
    // (Allocate memory for and fill with strong random data)
    *iv = ( unsigned char* ) gcry_random_bytes( Cipher::cIV_SIZE, GCRY_STRONG_RANDOM );

    if( ! *iv )
        throw Error( "Unable to create IV" );
}

void
Cipher::expand_key( const char* passphrase,
                    const unsigned char* salt,
                    unsigned char** key )
{
    gcry_md_hd_t hash;
    gcry_error_t error = 0;
    int hashdigestsize;
    unsigned char* hashresult;

    // OPEN MESSAGE DIGEST ALGORITHM
    error = gcry_md_open( &hash, cHASH_ALGORITHM, 0 );
    if( error )
        throw Error( "Unable to open message digest algorithm: %s" ); //, gpg_strerror( Error ) );

    // RETRIVE DIGEST SIZE
    hashdigestsize = gcry_md_get_algo_dlen( cHASH_ALGORITHM );

    // ADD SALT TO HASH
    gcry_md_write( hash, salt, cSALT_SIZE );

    // ADD PASSPHRASE TO HASH
    gcry_md_write( hash , passphrase , strlen( passphrase ) );

    // FETCH DIGEST (THE EXPANDED KEY)
    hashresult = gcry_md_read( hash , cHASH_ALGORITHM );

    if( ! hashresult )
    {
        gcry_md_close( hash );
        throw Error( "Unable to finalize key" );
    }

    // ALLOCATE MEMORY FOR KEY
    // can't use the 'HashResult' because those resources are freed after the
    // hash is closed
    *key = new unsigned char[ cKEY_SIZE ];
    if( ! key )
    {
        gcry_md_close( hash );
        throw Error( "Unable to allocate memory for key" );
    }

    // DIGEST SIZE SMALLER THEN KEY SIZE?
    if( hashdigestsize < cKEY_SIZE )
    {
        // PAD KEY WITH '0' AT THE END
        memset( *key , 0 , cKEY_SIZE );

        // COPY EVERYTHING WE HAVE
        memcpy( *key , hashresult , hashdigestsize );
    }
    else
        // COPY ALL THE BYTES WE'RE USING
        memcpy( *key , hashresult , hashdigestsize );

    // FINISHED WITH HASH
    gcry_md_close( hash );
}

// create new expanded key
void
Cipher::create_new_key( char const * passphrase,
                        unsigned char** salt,
                        unsigned char** key )
{
    // ALLOCATE MEMORY FOR AND FILL WITH STRONG RANDOM DATA
    *salt = ( unsigned char* ) gcry_random_bytes( cSALT_SIZE, GCRY_STRONG_RANDOM );

    if( ! *salt )
        throw Error( "Unable to create salt value" );

    expand_key( passphrase, *salt, key );
}

void
Cipher::encrypt_buffer ( unsigned char* buffer,
                         size_t& size,
                         const unsigned char* key,
                         const unsigned char* iv )
{
    gcry_cipher_hd_t    cipher;
    gcry_error_t        error = 0;

    error = gcry_cipher_open( &cipher, cCIPHER_ALGORITHM, cCIPHER_MODE, 0 );

    if( error )
        throw Error( "unable to initialize cipher: " ); // + gpg_strerror( Error ) );

    // GET KEY LENGTH
    int cipherKeyLength = gcry_cipher_get_algo_keylen( cCIPHER_ALGORITHM );
    if( ! cipherKeyLength )
        throw Error( "gcry_cipher_get_algo_keylen failed" );

    // SET KEY
    error = gcry_cipher_setkey( cipher, key, cipherKeyLength );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Cipher key setup failed: %s" ); //, gpg_strerror( Error ) );
    }

    // SET INITILIZING VECTOR (IV)
    error = gcry_cipher_setiv( cipher, iv, cIV_SIZE );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Unable to setup cipher IV: %s" );// , gpg_strerror( Error ) );
    }

    // ENCRYPT BUFFER TO SELF
    error = gcry_cipher_encrypt( cipher, buffer, size, NULL, 0 );

    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Encrption failed: %s" ); // , gpg_strerror( Error ) );
    }

    gcry_cipher_close( cipher );
}

void
Cipher::decrypt_buffer ( unsigned char* buffer,
                         size_t size,
                         const unsigned char* key,
                         const unsigned char* iv )
{
    gcry_cipher_hd_t cipher;
    gcry_error_t error = 0;

    error = gcry_cipher_open( &cipher, cCIPHER_ALGORITHM, cCIPHER_MODE, 0 );

    if( error )
        throw Error( "Unable to initialize cipher: " ); // + gpg_strerror( Error ) );

    // GET KEY LENGTH
    int cipherKeyLength = gcry_cipher_get_algo_keylen( cCIPHER_ALGORITHM );
    if( ! cipherKeyLength )
        throw Error( "gcry_cipher_get_algo_keylen failed" );

    // SET KEY
    error = gcry_cipher_setkey( cipher, key, cipherKeyLength );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Cipher key setup failed: %s" ); //, gpg_strerror( Error ) );
    }

    // SET IV
    error = gcry_cipher_setiv( cipher, iv, cIV_SIZE );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Unable to setup cipher IV: %s" );// , gpg_strerror( Error ) );
    }

    // DECRYPT BUFFER TO SELF
    error = gcry_cipher_decrypt( cipher, buffer, size, NULL, 0 );

    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Encryption failed: %s" ); // , gpg_strerror( Error ) );
    }

    gcry_cipher_close( cipher );
}

#ifndef __ANDROID__
// MARKED UP MENU ITEM =============================================================================
// Gtk::MenuItem*
// create_menuitem_markup( const Glib::ustring& str_markup,
//                         const Glib::SignalProxy0< void >::SlotType& handler )
// {
//     // thanks to GNote for showing the way
//     Gtk::MenuItem* menuitem = Gtk::manage( new Gtk::MenuItem( str_markup ) );
//     Gtk::Label* label = dynamic_cast< Gtk::Label* >( menuitem->get_child() );
//     if( label )
//         label->set_use_markup( true );
//     menuitem->signal_activate().connect( handler );
//     return menuitem;
// }

// CHILDREN REMOVAL ================================================================================
void remove_all_children_from_Bx( Gtk::Box* w )
{
    while( auto cw = w->get_last_child() ) w->remove( *cw );
}
void remove_all_children_from_LBx( Gtk::ListBox* w )
{
    while( auto cw = w->get_row_at_index( 0 ) ) w->remove( *cw );
}

void remove_all_children_from_FBx( Gtk::FlowBox* w )
{
    while( auto cw = w->get_child_at_index( 0 ) ) w->remove( *cw );
}

// ENTRYCLEAR ======================================================================================
void
EntryClear::init()
{
    try // may not work!
    {
        set_icon_from_icon_name( "edit-clear-symbolic", Gtk::Entry::IconPosition::SECONDARY );
    }
    catch( ... )
    {
        set_icon_from_icon_name( "edit-clear", Gtk::Entry::IconPosition::SECONDARY );
    }

    signal_icon_press().connect( sigc::mem_fun( *this, &EntryClear::handle_icon_press ) );
    signal_icon_release().connect( sigc::mem_fun( *this, &EntryClear::handle_icon_release ) );

    // EVENT CONTROLLERS
    auto event_controller_key{ Gtk::EventControllerKey::create() };
    event_controller_key->signal_key_released().connect(
            sigc::mem_fun( *this, &EntryClear::on_key_release_event ), false );
    add_controller( event_controller_key );
}

void
EntryClear::handle_icon_press( Gtk::Entry::IconPosition pos )
{
    if( pos == Gtk::Entry::IconPosition::SECONDARY )
        m_F_pressed = true;
}

void
EntryClear::handle_icon_release( Gtk::Entry::IconPosition pos )
{
    if( pos == Gtk::Entry::IconPosition::SECONDARY )
    {
        if( m_F_pressed && !is_empty() )
            set_text( "" );
        m_F_pressed = false;
    }
}

void
EntryClear::on_changed()
{
    // be watchful: setting icon of an entry hosted in a popopver used to creates problems in gtkmm4
    if( get_text().empty() )
    {
        unset_icon( Gtk::Entry::IconPosition::SECONDARY );
        set_icon_activatable( false, Gtk::Entry::IconPosition::SECONDARY );
    }
    else
    {
        try // may not work!
        {
            set_icon_from_icon_name( "edit-clear-symbolic", Gtk::Entry::IconPosition::SECONDARY );
        }
        catch( ... )
        {
            set_icon_from_icon_name( "edit-clear", Gtk::Entry::IconPosition::SECONDARY );
        }
        set_icon_activatable( true, Gtk::Entry::IconPosition::SECONDARY );
    }

    Gtk::Entry::on_changed();
}

void
EntryClear::on_key_release_event( guint keyval, guint, Gdk::ModifierType state )
{
    if( keyval == GDK_KEY_Escape )
        set_text( "" );
}

// ADVANCED MESSAGE DIALOG =========================================================================
std::vector< DialogMessage* > DialogMessage::s_dialog_store;

DialogMessage::DialogMessage( const Ustring& message, const Ustring& clabel )
{
    Pango::AttrList   attrlist;
    auto&&            attweight { Pango::Attribute::create_attr_weight( Pango::Weight::BOLD ) };
    auto              Bx_main   { Gtk::make_managed< Gtk::Box >( Gtk::Orientation::VERTICAL, 0 ) };

    m_Bx_content  = Gtk::make_managed< Gtk::Box >( Gtk::Orientation::VERTICAL, 10 );
    m_Bx_buttons  = Gtk::make_managed< Gtk::Box >();
    m_L_msg       = Gtk::make_managed< Gtk::Label >( message );
    m_B_cancel    = Gtk::make_managed< Gtk::Button >( clabel, true );

    set_titlebar( *Gtk::make_managed< Gtk::Box >() ); // empty titlebar
    add_css_class( "dialog" );
    add_css_class( "message" );
    set_modal( true );
    set_resizable( false );

    m_L_msg->set_wrap( true );
    m_L_msg->set_wrap_mode( Pango::WrapMode::WORD );
    m_L_msg->set_max_width_chars( 60 );

    attrlist.insert( attweight );
    m_L_msg->set_attributes( attrlist );

    m_Bx_buttons->add_css_class( "dialog-action-box" );
    m_Bx_buttons->add_css_class( "linked" );
    m_Bx_buttons->append( *m_B_cancel );

    m_Bx_content->set_margin_start( 30 );
    m_Bx_content->set_margin_end( 30 );
    m_Bx_content->append( *m_L_msg );

    Bx_main->add_css_class( "dialog-vbox" );
    Bx_main->append( *m_Bx_content );
    Bx_main->append( *m_Bx_buttons );

    m_B_cancel->set_hexpand( true );
    m_B_cancel->add_css_class( "dlg-msg" );
    m_B_cancel->signal_clicked().connect( std::bind( &DialogMessage::hide, this ) );

    set_child( *Bx_main );
}

DialogMessage*
DialogMessage::init( Gtk::Window* parent, const Ustring& message, const Ustring& clabel )
{
    auto dlg{ new DialogMessage( message, clabel ) };
    s_dialog_store.push_back( dlg );

    dlg->set_transient_for( *parent );

    return dlg;
}

DialogMessage*
DialogMessage::add_extra_info( const Ustring& extra_info, bool F_use_markup )
{
    auto L_extra { Gtk::make_managed< Gtk::Label >( extra_info ) };

    L_extra->set_wrap( true );
    L_extra->set_wrap_mode( Pango::WrapMode::WORD );
    L_extra->set_max_width_chars( 75 );
    if( F_use_markup )
        L_extra->set_use_markup( true );

    m_Bx_content->insert_child_after( *L_extra, *m_L_msg );

    return this;
}

DialogMessage*
DialogMessage::add_extra_widget( Gtk::Widget* W_extra )
{
    m_Bx_content->insert_child_after( *W_extra, *m_L_msg );
    return this;
}

Gtk::Button*
DialogMessage::add_button_b( const Ustring& label, const FuncVoid& handle_click )
{
    auto B_new { Gtk::make_managed< Gtk::Button >( label, true ) };
    B_new->set_hexpand( true );
    B_new->add_css_class( "dlg-msg" );
    B_new->signal_clicked().connect( handle_click );
    B_new->signal_clicked().connect( std::bind( &DialogMessage::hide, this ) );
    m_Bx_buttons->append( *B_new );

    return B_new;
}

DialogMessage*
DialogMessage::add_cancel_handler( const FuncVoid& handle_click )
{
    m_B_cancel->signal_clicked().connect( handle_click );
    return this;
}

// DIALOGEVENT =====================================================================================
// the rest of the DialogEvent needs to be defined within the user application
void
DialogEvent::handle_logout()
{
    hide();
}

// FILECHOOSERRBUTTON ==============================================================================
void
FileChooserButton::init()
{
    m_dlg = Gtk::FileDialog::create();
    //m_dlg->set_title(  );
    m_dlg->set_modal( true );

    auto box  { Gtk::make_managed< Gtk::Box >( Gtk::Orientation::HORIZONTAL, 5 ) };

    m_icon = Gtk::make_managed< Gtk::Image >();
    m_icon->set_from_icon_name( "document-open-symbolic" );

    m_label = Gtk::make_managed< Gtk::Label >();
    m_label->set_ellipsize( Pango::EllipsizeMode::START );
    m_label->set_hexpand( true );
    m_label->set_halign( Gtk::Align::START );

    box->append( *m_icon );
    box->append( *m_label );
    set_child( *box );

    signal_clicked().connect( sigc::mem_fun( *this, &FileChooserButton::handle_click ) );
}

void
FileChooserButton::handle_click()
{
    if( !m_uri.empty() )
    {
        auto file { Gio::File::create_for_uri( m_uri ) };
        m_dlg->set_initial_folder( file->get_parent() );
    }

    if( m_F_folder_mode )
        m_dlg->select_folder(   *dynamic_cast< Gtk::Window* >( get_root() ),
                                [ this ]( Glib::RefPtr< Gio::AsyncResult >& result )
                                {
                                    auto file{ m_dlg->select_folder_finish( result ) };
                                    m_uri = file->get_uri();
                                    m_signal_file_set.emit( m_uri );
                                    m_label->set_text( file->get_parse_name() );
                                } );
    else
        m_dlg->open(    *dynamic_cast< Gtk::Window* >( get_root() ),
                        [ this ]( Glib::RefPtr< Gio::AsyncResult >& result )
                        {
                            auto file{ m_dlg->open_finish( result ) };
                            m_uri = file->get_uri();
                            m_signal_file_set.emit( m_uri );
                            m_label->set_text( file->get_parse_name() );
                        } );
}

void
FileChooserButton::add_file_filters( Glib::RefPtr< Gtk::FileDialog >& fd,
                                     const Ustring& type_name,
                                     const Ustring& pattern,
                                     const Ustring& mime )
{
    auto filter_any     { Gtk::FileFilter::create() };
    auto filter_custom  { Gtk::FileFilter::create() };

    filter_any->set_name( _( "All Files" ) );
    filter_any->add_pattern( "*" );
    filter_custom->set_name( type_name );
#ifndef __linux__
    filter_custom->add_pattern( pattern );
#else
    filter_custom->add_mime_type( mime );
#endif

    auto filters = Gio::ListStore< Gtk::FileFilter >::create();
    filters->append( filter_any );
    filters->append( filter_custom );

    fd->set_filters( filters );
    fd->set_default_filter( filter_custom );
}

void
FileChooserButton::add_image_file_filters( Glib::RefPtr< Gtk::FileDialog >& fd )
{
    auto filter_any = Gtk::FileFilter::create();
    auto filter_img = Gtk::FileFilter::create();

    filter_any->set_name( _( "All Files" ) );
    filter_any->add_pattern( "*" );
    filter_img->set_name( _( "Image Files" ) );
    filter_img->add_pixbuf_formats();
// #ifdef _WIN32
//     filter_img->add_pattern( "*.jpg" );
//     filter_img->add_pattern( "*.png" );
// #else
//     filter_img->add_mime_type( "image/jpeg" );
//     filter_img->add_mime_type( "image/png" );
// #endif

    auto filters = Gio::ListStore< Gtk::FileFilter >::create();
    filters->append( filter_any );
    filters->append( filter_img );

    fd->set_filters( filters );
    fd->set_default_filter( filter_img );
}

void
FileChooserButton::add_diary_file_filters( Glib::RefPtr< Gtk::FileDialog >& fd )
{
    add_file_filters( fd, _( "Diary files" ), "*.diary", LoG::MIME_LOG_FILE );
}

void
FileChooserButton::add_file_filters( const Ustring& type_name,
                                     const Ustring& pattern,
                                     const Ustring& mime )
{
    add_file_filters( m_dlg, type_name, pattern, mime );
}

void
FileChooserButton::add_image_file_filters()
{
    add_image_file_filters( m_dlg );
    m_icon->set_from_icon_name( "image-x-generic-symbolic" );
}

void
FileChooserButton::add_diary_file_filters()
{
    add_diary_file_filters( m_dlg );
    m_icon->set_from_icon_name( "diary-16-symbolic" );
}

void
FileChooserButton::set_icon( const String& icon_name )
{
    m_icon->set_from_icon_name( icon_name );
}

void
FileChooserButton::set_uri( const String& uri )
{
    m_uri = uri;

    if( uri.empty() )
    {
        m_label->set_text( m_empty_text );
    }
    else
{
        auto file { Gio::File::create_for_path( uri ) };

        m_dlg->set_initial_folder( file );
        m_label->set_text( file->get_parse_name() );
    }
}

void
FileChooserButton::set_info_text( const String& text )
{
    m_empty_text = text;

    if( m_uri.empty() )
        m_label->set_text( text );
}
// FRAME (for printing) ============================================================================
// Gtk::Frame*
// create_frame( const Glib::ustring& str_label, Gtk::Widget& content )
// {
//     Gtk::Frame* frame = Gtk::manage( new Gtk::Frame );
//     Gtk::Label* label = Gtk::manage( new Gtk::Label );

//     Gtk::Alignment* alignment = Gtk::manage( new Gtk::Alignment );

//     label->set_markup( Glib::ustring::compose( "<b>%1</b>", str_label ) );
//     frame->set_shadow_type( Gtk::SHADOW_NONE );
//     frame->set_label_widget( *label );
//     alignment->set_padding( 0, 0, 12, 0 );
//     alignment->add( content );
//     frame->add( *alignment );

//     return frame;
// }

// TREEVIEW ========================================================================================
bool is_treepath_less( const Gtk::TreePath& p, const Gtk::TreePath& pb )
{
    for( unsigned int i = 0; i < p.size(); i++ )
    {
        if( i >= pb.size() )
            return false;

        if( p[i] < pb[ i ] )
            return true;

        if( p[i] > pb[ i ] )
            return false;
    }

    return true; // if pb has more members apart from the shared ones, it is more
}

bool is_treepath_more( const Gtk::TreePath& p, const Gtk::TreePath& pe )
{
    for( unsigned int i = 0; i < p.size(); i++ )
    {
        if( i >= pe.size() )
            return true;

        if( p[i] > pe[ i ] )
            return true;

        if( p[i] < pe[ i ] )
            return false;
    }

    return false; // less or equal
}

// OTHER GTK+ ======================================================================================
void
flush_gtk_event_queue()
{
    // while( Gtk::Main::events_pending() )
    //     Gtk::Main::iteration();
    PRINT_DEBUG( "flush_gtk_event_queue() is DISABLED in gtkmm4" );
}

// what a shame on Gtk!
int
get_LB_item_count( Gtk::ListBox* LB )
{
    int count{ 0 };
    while( LB->get_row_at_index( count ) ) count++;
    return count;
}

void
scroll_to_selected_LB_row( Gtk::ListBox* LB_ )
{
    Glib::signal_idle().connect_once(
            sigc::bind(
                    // the following hackish solution was found on the internet:
                    []( Gtk::ListBox* LB )
                    {
                        auto    row { LB->get_selected_row() };
                        auto    adj { LB->get_adjustment() };
                        double  x, y;

                        // if there's selection
                        if( row && adj )
                        {
                            // convert the row's Y coordinate into the list box's coordinate
                            if( row->translate_coordinates( *LB, 0.0, 0.0, x, y ) )
                            {
                                // Scroll the vertical adjustment to center the row in the viewport
                                auto rowHeight = row->get_preferred_size().minimum.get_height();
                                adj->set_value( y - ( adj->get_page_size() - rowHeight ) / 2 );
                            }
                        }
                    },
                    LB_ ) );
}

void
select_LB_item_prev( Gtk::ListBox* LB )
{
    auto row{ LB->get_selected_row() };
    if( row )
    {
        auto prev{ row->get_prev_sibling() };
        if( prev )
        {
            LB->select_row( * dynamic_cast< Gtk::ListBoxRow* >( prev ) );
            return; // to prevent the default action
        }
    }

    // default action (is also used to wrap around):
    const int item_c{ get_LB_item_count( LB ) };
    if( item_c > 0 )
        LB->select_row( * LB->get_row_at_index( item_c - 1 ) );
}
void
select_LB_item_next( Gtk::ListBox* LB )
{
    auto row{ LB->get_selected_row() };
    if( row )
    {
        auto next{ row->get_next_sibling() };
        if( next )
        {
            LB->select_row( * dynamic_cast< Gtk::ListBoxRow* >( next ) );
            return; // to prevent the default action
        }
    }

    // default action (is also used to wrap around):
    if( LB->get_first_child() )
        LB->select_row( * LB->get_row_at_index( 0 ) );
}

Ustring
get_DD_selected_string( const Gtk::DropDown* DD )
{
    auto selected_item = DD->get_selected_item();

    if( selected_item )
    {
        auto string_obj { std::dynamic_pointer_cast< const Gtk::StringObject >( selected_item ) };
        if( string_obj )
            return string_obj->get_string();
    }
    return "";
}
bool
set_DD_selected_by_string( Gtk::DropDown* DD, const Ustring& target )
{
    auto model { DD->get_model() };
    if( !model ) return false;

    const guint n_items { model->get_n_items() };

    for( guint i = 0; i < n_items; ++i )
    {
        auto item       { model->get_object( i ) };
        auto string_obj { std::dynamic_pointer_cast<Gtk::StringObject>( item ) };

        if( string_obj && string_obj->get_string() == target )
        {
            DD->set_selected( i );
            return true;
        }
    }
    return false;
};

R2Pixbuf
lookup_theme_icon_pixbuf( const Glib::RefPtr< Gtk::IconTheme >& theme, const Ustring& name,
                          int size )
{
    auto icon { theme->lookup_icon( name, size ) };
    auto file { icon->get_file() };
    if( !file ) return {};

    try
    {
        auto stream { file->read() }; // open Gio::InputStream from resource or file
        return Gdk::Pixbuf::create_from_stream( stream );
    }
    catch( const Glib::Error& ex )
    {
        print_error( "Failed to load icon pixbuf: ", ex.what() );
        return {};
    }
}

#endif // ifndef __ANDROID__

} // end of name space
