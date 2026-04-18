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


#ifndef LIFEOGRAPH_HELPERS_HEADER
#define LIFEOGRAPH_HELPERS_HEADER


#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <iostream>
#include <fstream>
#include <sstream>
#include <set>
#include <type_traits>
#include <limits>
#include <vector>
#include <list>
#include <functional>
#include <cstring>
#include <gcrypt.h>

#ifdef __ANDROID__
#include "android_shim.hpp"
#else
#include <libintl.h>
#include <gtkmm.h>
#endif


// DEFINITIONS FOR LIBGETTEXT
#ifdef __ANDROID__
#define _(String)               (String)
#else
#define _(String)               gettext(String)
#endif
#define gettext_noop(String)    String
#define N_(String)              gettext_noop(String)
// END OF LIBGETTEXT DEFINITIONS

namespace HELPERS
{
// CONSTANTS
static constexpr double MI_TO_KM_RATIO{ 1.609344 };
static constexpr double PI = 3.141592653589793;

// TYPE ALIASES ====================================================================================
using date_t_old        = unsigned long;
using Value             = double;
using VecInts           = std::vector< int >;

using String            = std::string;
using StringSize        = std::string::size_type;
using VecStrings        = std::vector< String >;
using ListStrings       = std::list< String >;
using SetStrings        = std::set< String >;
using StrStream         = std::stringstream;

using Ustring           = Glib::ustring;
using UstringSize       = Glib::ustring::size_type;
using PairConstStrings  = std::pair< const Ustring, const Ustring >;
using VecUstrings       = std::vector< Ustring >;
using ListUstrings      = std::list< Ustring >;
using SetUstrings       = std::set< Ustring >;

using Wchar             = gunichar;
using R2Pixbuf          = Glib::RefPtr< Gdk::Pixbuf >;
using MapPathPixbufs    = std::unordered_map< String, R2Pixbuf >;
using Color             = Gdk::RGBA;

using FuncVoid          = std::function< void( void ) >;
using FuncVoidInt       = std::function< void( int ) >;
using FuncVoidString    = std::function< void( const String& ) >;
using FuncVoidUstring   = std::function< void( const Ustring& ) >;
using SignalVoid        = sigc::signal< void( void ) >;
using SignalVoidInt     = sigc::signal< void( int ) >;
using SignalVoidBool    = sigc::signal< void( bool ) >;
using SignalVoidString  = sigc::signal< void( const String& ) >;
using SignalVoidUstring = sigc::signal< void( const Ustring& ) >;
using SignalBoolUstring = sigc::signal< bool( const Ustring& ) >;
using SignalVoidColor   = sigc::signal< void( const Color& ) >;

enum class ArrayPosition { FIRST, INTERMEDIATE, LAST };

// ENUM HELPERS
template< typename T >
constexpr auto get_underlying( T const v )
    //-> typename std::underlying_type<Enumeration>::type
{
    return static_cast< typename std::underlying_type< T >::type >( v );
}
template< typename T >
constexpr auto set_from_underlying( typename std::underlying_type< T >::type v )
    //-> typename std::underlying_type<Enumeration>::type
{
    return static_cast< T >( v );
}

// ERROR for throw-ing
class Error
{
    public:
                        Error( const Ustring& );
    const Ustring&      what() { return description; }
    const Ustring       description;
};

// RESULT
enum Result
{
    OK,
    ABORTED,
    SUCCESS,
    FAILURE,
    COULD_NOT_START,
    COULD_NOT_FINISH,
    WRONG_PASSWORD,
    //APPARENTLY_ENCRYTED_FILE,
    //APPARENTLY_PLAIN_FILE,
    INCOMPATIBLE_FILE_OLD,
    INCOMPATIBLE_FILE_NEW,
    CORRUPT_FILE,
    // EMPTY_DATABASE, // not used anymore

    // RESULTS USED BY set_path():
    FILE_NOT_FOUND,
    FILE_NOT_READABLE,
    FILE_NOT_WRITABLE,
    FILE_LOCKED,
    FILE_READ_ONLY,

    // RESULTS USED BY Date
    OUT_OF_RANGE,
    INVALID
};

// CONSTANTS =======================================================================================
struct Constants
{
    static constexpr double INFINITY_PLS = std::numeric_limits< double >::infinity();
    static constexpr double INFINITY_MNS = -std::numeric_limits< double >::infinity();
};

// Restricted Variables
template< typename T, typename Owner >
class Restricted
{
public:
    Restricted( T v ) : value( v ) { }

    operator T() const { return value; }
    Restricted& operator=( const Restricted& ) = delete;
    Restricted& operator=( Restricted&& ) = delete;

private:
    T value;

    friend Owner;
};

// ENUM CLASS FLAGS ================================================================================
template< typename E > class EnumFlags
{
    static_assert( std::is_enum_v< E >, "Flags requires an enum type" );

public:
    using underlying = std::underlying_type_t< E >;

    constexpr EnumFlags() : m_value( 0 ) {}
    constexpr EnumFlags( E e ) : m_value( static_cast< underlying >( e ) ) {}
    constexpr explicit EnumFlags( underlying v ) : m_value( v ) {}

    // bitwise or
    constexpr EnumFlags
    operator|( EnumFlags other ) const
    { return EnumFlags( m_value | other.m_value ); }

    constexpr EnumFlags&
    operator|=( EnumFlags other )
    {
        m_value |= other.m_value;
        return *this;
    }

    // bitwise and
    constexpr EnumFlags
    operator&( EnumFlags other ) const
    { return EnumFlags( m_value & other.m_value ); }

    // check if flag exists
    constexpr bool
    contains( E flag ) const
    { return ( m_value & static_cast< underlying >( flag ) ) != 0; }

    // check if ay of flags exists
    constexpr bool
    contains( const EnumFlags< E >& flags ) const
    { return ( m_value & flags.m_value ) != 0; }

    constexpr bool
    is_empty() const
    { return m_value == 0; }

    constexpr underlying
    get_underlying_value() const
    { return m_value; }

private:
    underlying m_value;
};

template < typename E >
constexpr EnumFlags< E >
operator|( E a, E b )
{ return EnumFlags< E >( a ) | EnumFlags< E >( b ); }

// STRING OPERATIONS ===============================================================================
enum class LetterCase { CASE_SENTENCE, CASE_TITLE, CASE_LOWER, CASE_UPPER };

struct FuncCmpStrings
{
    bool operator()( const Ustring& l, const Ustring& r )
    const { return( l < r ); }
};

class STR
{
    private:
        static void             print_internal( String& ) {}

        template< typename Arg1, typename... Args >
        static void             print_internal( String& str, Arg1 arg1, Args... args )
        {
            str += format( arg1 );
            print_internal( str, args... );
        }
        static const char*      format( const char* str ){ return str; }
        static const String     format( const String& str ){ return str; }
        static const char       format( const char ch ){ return ch; }
        static const String     format( int num ){ return std::to_string( num ); }
        static const String     format( unsigned int num ){ return std::to_string( num ); }
        static const String     format( long num ){ return std::to_string( num ); }
        static const String     format( unsigned long num ){ return std::to_string( num ); }
        static const String     format( long long num ){ return std::to_string( num ); }
        static const String     format( unsigned long long num ){ return std::to_string( num ); }
        static const String     format( double num ){ return format_number( num ); }

    public:
        template< typename... Args >
        static Ustring          compose( Args... args )
        {
            String str;
            print_internal( str, args... );

            return str;
        }

        static String           format_percentage( double );
        static String           format_hex( int );
        static String           format_number( double, int = -1 );
        // to be able to use above in template functions:
        static const String&    format_number( const String& s ) { return s; }
        static String           format_number_no_inf( double num )
        {
        	return( num == Constants::INFINITY_MNS || num == Constants::INFINITY_PLS ?
        			"" : format_number( num ) );
        }
        static String           format_number_roman( int, bool = false );

        static bool             begins_with( const String&, const String& );
        static bool             ends_with( const String&, const String& );
        static bool             ends_with( const String&, const char );
        static bool             ends_with_trimmed( const Ustring&, const gunichar );
        static char             get_end_char( const String& );
        static gunichar         get_last_nonspace_char( const Ustring& );
        static bool             get_line( const String&, StringSize&, String& );
        static bool             get_line( const char*, unsigned int&, String& );
        static int              replace( String&, const String&, const String& );
        static String           replace_spaces( const String& );

        static Ustring          lowercase( const Ustring& );
        static Ustring          sentencecase( const Ustring& );
        static Ustring          titlecase( const Ustring& );

        static Ustring          get_substr_delim( const Ustring& line,
                                                  UstringSize& pos, char delimiter )
        {
            const UstringSize pos_bgn { pos };
            pos = line.find( delimiter, pos );
            return( pos == std::string::npos ? "" : line.substr( pos_bgn, pos - pos_bgn ) );
        }

        static String           strip_spaces( const String& s )
        {
            size_t start { 0 };
            while( start < s.size() && std::isspace( static_cast< unsigned char >( s[ start ] ) ) )
                ++start;

            size_t end { s.size() };
            while( end > start && std::isspace( static_cast< unsigned char >( s[ end - 1 ] ) ) )
                --end;

            return s.substr( start, end - start );
        }

        static int64_t          get_i64( const String&, int& i );
        static int32_t          get_i32( const String&, int& i );
        static int32_t          get_i32( const String& ); // guarded std::stol
        static double           get_d( const String& ); // std::stod creates locale issues
        static double           get_d( const String&, int& i, double = -1.0 ); // version 2
        // NOTE: in get_i32/64 i ends up pointing to the next char...
        // ...but in get_d it points to the last numeric char

        static int              get_pos_c( const char* str, char c, int def_value = -1 )
        {
            const char* str_sub = strchr( str, c );
            if( !str_sub ) return def_value;
            else           return( int( str_sub - str ) );
        }

        static int              get_utf8_char_count( const String& s )
        {
            int count = 0;
            for( unsigned char c : s )
            {
                if( ( c & 0b11000000 ) != 0b10000000 )
                    ++count; // Only count leading bytes (start of new character)
            }
            return count;
        }
        static int              get_utf8_pos_from_byte_i( const String& s, size_t bi )
        {
            int char_count = 0;
            for( size_t i = 0; i < bi && i < s.size(); ++i )
            {
                unsigned char c = static_cast< unsigned char >( s[ i ] );
                if( ( c & 0b11000000 ) != 0b10000000 )
                    ++char_count; // New character starts here
            }
            return char_count;
        }

        static VecUstrings      make_substr_vector( const Ustring&, int = 1 );
        static int              find_sentence_start_backwards( const String&, StringSize );
        static int              find_word_start_backwards( const String&, StringSize );

        static bool             is_char_space( Wchar c )
        { return( c == ' ' || c == '\t' || c == '\n' ); }
        static bool             is_char_name( Wchar c )
        { return( c == '_' || c == '-' || Glib::Unicode::isalnum( c ) ); }
        static bool             is_char_not_name( Wchar c )
        { return( !is_char_name( c ) ); }

        static Ustring          create_unique_name( const Ustring& name0,
                std::function< bool( const Ustring& ) >&& is_taken )
        {
            Ustring name = name0;
            for( int i = 0; is_taken( name ); i++ )
            {
                name = compose( name0, "_", i );
            }

            return name;
        }
};

// OTHER STRING OPERATIONS =========================================================================
String              get_env_lang();
#ifdef _WIN32
wchar_t*            convert_utf8_to_16( const Ustring& );
Ustring             convert_utf16_to_8( const wchar_t* );
#endif

// SOME BASICS =====================================================================================
template< typename T >
bool is_value_in_range_excl( const T& value, const T& low, const T& high )
{
    return( ( value > low ) && ( value < high ) );
}

// OLD DATE ========================================================================================
// order: 10 bits
// day:    5 bits
// month:  4 bits
// year:  12 bits
// ordinal flag:  1 bit (32nd bit)

class DateOld
{
    public:
        static const unsigned long  NOT_SET              = 0xFFFFFFFF;
        static const unsigned long  DATE_MAX             = 0xFFFFFFFF;

        static const unsigned int   YEAR_MIN             = 1900;
        static const unsigned int   YEAR_MAX             = 2199;
        static const unsigned long  ORDER_MAX            = 1023;
        static const unsigned long  ORDER_1ST_MAX        = 0x3FF00000; // bits 21..30
        static const unsigned long  ORDER_2ND_MAX        =    0xFFC00; // bits 11..20
        static const unsigned int   ORDER_3RD_MAX        =      0x3FF; // bits 01..10

        static const unsigned long  ORDER_1ST_STEP       =   0x100000; // bit 21
        static const unsigned long  ORDER_2ND_STEP       =      0x400; // bit 11

        static const unsigned long  FILTER_DAY           =     0x7C00; // bits 11..15
        static const unsigned long  FILTER_MONTH         =    0x78000; // bits 16..19
        static const unsigned long  FILTER_YEAR          = 0x7FF80000; // bits 20..31
        static const unsigned long  FILTER_YEARMONTH     = FILTER_YEAR|FILTER_MONTH;
        static const unsigned long  FILTER_ORDER_1ST_INV = DATE_MAX ^ ORDER_1ST_MAX;
        static const unsigned long  FILTER_ORDER_2ND_INV = DATE_MAX ^ ORDER_2ND_MAX;
        static const unsigned long  FILTER_ORDER_3RD_INV = DATE_MAX ^ ORDER_3RD_MAX; // FILTER_PURE
        static const unsigned long  FILTER_DAY_INV       = DATE_MAX ^ FILTER_DAY;
        static const unsigned long  FILTER_MONTH_INV     = DATE_MAX ^ FILTER_MONTH;
        static const unsigned long  FILTER_YEAR_INV      = DATE_MAX ^ FILTER_YEAR;

        // hidden elements' sequence numbers are not shown
        static const unsigned long  FLAG_VISIBLE         = 0x40000000; // only for ordinal items
        static const unsigned long  FLAG_ORDINAL         = 0x80000000; // 32nd bit

        static const unsigned long  NUMBERED_MIN         = FLAG_VISIBLE|FLAG_ORDINAL;
        static const unsigned long  FREE_MIN             = FLAG_ORDINAL;

        explicit                    DateOld( date_t_old date )
            :   m_date( date ) {}
        explicit                    DateOld( unsigned int y, unsigned int m, unsigned int d,
                                          unsigned int o = 0 )
            :   m_date( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) | o ) {}
        // ORDINAL C'TOR
        explicit                    DateOld( bool f_n,
                                          unsigned int o1, unsigned int o2, unsigned int o3 )
            : m_date( ( f_n ? NUMBERED_MIN : FREE_MIN ) | ( o1 << 20 ) | ( o2 << 10 ) | o3 ) {}

        // TEMPORAL METHODS
        static unsigned int         get_year( const date_t_old d )
        { return ( ( d & FILTER_YEAR ) >> 19 ); }

        static unsigned int         get_month( const date_t_old d )
        { return( ( d & FILTER_MONTH ) >> 15 ); }

        static unsigned int         get_day( const date_t_old d )
        { return( ( d & FILTER_DAY ) >> 10 ); }

        // ORDINAL METHODS
        static unsigned int         get_order_3rd( const date_t_old d )
        { return( d & ORDER_3RD_MAX ); }
        static unsigned int         get_order_2nd( const date_t_old d )
        { return( ( d & ORDER_2ND_MAX ) >> 10 ); }
        static unsigned int         get_order_1st( const date_t_old d )
        { return( ( d & ORDER_1ST_MAX ) >> 20 ); }

        static bool                 is_1st_level( const date_t_old d )
        { return( ( d & ( ORDER_2ND_MAX | ORDER_3RD_MAX ) ) == 0 ); }
        static bool                 is_2nd_level( const date_t_old d )
        { return( ( d & ORDER_2ND_MAX ) != 0 && ( d & ORDER_3RD_MAX ) == 0 ); }
        static bool                 is_3rd_level( const date_t_old d )
        { return( ( d & ORDER_2ND_MAX ) != 0 && ( d & ORDER_3RD_MAX ) != 0 ); }

        static int                  get_level( const date_t_old d )
        {
            if( not( is_ordinal( d ) ) ) return 4; // temporal
            if( is_1st_level( d ) ) return 1;
            if( is_2nd_level( d ) ) return 2;
            return 3;
        }

        // RELATIONSHIP METHODS
        static date_t_old           get_parent( const date_t_old d )
        {
            switch( get_level( d ) )
            {
                case 1: return( DATE_MAX );
                case 2: return( d & FILTER_ORDER_2ND_INV & FILTER_ORDER_3RD_INV );
                default: return( d & FILTER_ORDER_3RD_INV );
            }
        }

        static bool                 is_set( const date_t_old date )
        { return( date != NOT_SET ); }

        static bool                 is_ordinal( const date_t_old d )
        { return( d & FLAG_ORDINAL ); }

        static bool                 is_hidden( const date_t_old d )
        { return( is_ordinal( d ) && !( d & FLAG_VISIBLE ) ); }

        static bool                 is_sibling( const date_t_old d1, const date_t_old d2 )
        { return( is_ordinal( d1 ) && get_parent( d1 ) == get_parent( d2 ) ); }
        static bool                 is_child_of( const date_t_old dc, const date_t_old dp )
        {
            if( dc == dp ) return false;
            if( dp == DATE_MAX ) return true; // DATE_MAX means all dates
            if( ( dc & FLAG_VISIBLE ) != ( dp & FLAG_VISIBLE ) ) return false;
            if( is_1st_level( dp ) ) return( get_order_1st( dc ) == get_order_1st( dp ) );
            if( is_2nd_level( dp ) ) return( get_order_1st( dc ) == get_order_1st( dp ) &&
                                             get_order_2nd( dc ) == get_order_2nd( dp ) );
            return false;
        }

        // MAKE METHODS
        static date_t_old           make( unsigned int y, unsigned int m, unsigned int d,
                                          unsigned int o = 0 )
        { return( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) | o ); }
        static date_t_old           make_ordinal( bool f_num,
                                                  unsigned int o1,
                                                  unsigned int o2,
                                                  unsigned int o3 = 0 )
        { return( ( f_num ? NUMBERED_MIN : FREE_MIN ) | ( o1 << 20 ) | ( o2 << 10 ) | o3 ); }

        date_t_old                  m_date{ 0 };
};

// DATE & TIME =====================================================================================
using DateV                 = int64_t;
using SignalVoidDate        = sigc::signal< void( DateV ) > ;
using SignalVoidDateUstring = sigc::signal< void( DateV, const Ustring& ) > ;
namespace Date
{
    extern String   s_format_order;
    extern char     s_format_separator;
    extern int      s_week_start_day;

    // CONSTANTS
    static const unsigned int  TIME_MAX = 235959;
    static const unsigned int  YEAR_MIN = 1800;
    static const unsigned int  YEAR_MAX = 2299;

    static constexpr DateV NOT_SET = 0x0;
    static constexpr DateV LATEST  =  229912310235959;
    static constexpr DateV MIN_1ST =              100;
    static constexpr DateV HOR_1ST =            10000;
    static constexpr DateV TIM_SEP =          1000000; // time separator
    static constexpr DateV DAY_1ST =         10000000;
    static constexpr DateV MNT_1ST =       1000000000;
    static constexpr DateV YER_1ST =     100000000000;
    static constexpr DateV DAT_SEP = 1000000000000000; // date separator

    static constexpr DateV BCK_MON = 10000001000000000; // backwards month
    static constexpr DateV FWD_MON = 10000002000000000; // backwards month
    static constexpr DateV THS_MON = 10000003000000000; // this month

    // MAKERS
    inline DateV make_min( int m )      { return( m * MIN_1ST ); }
    inline DateV make_hour( int h )     { return( h * HOR_1ST ); }
    inline DateV make_year( int y )     { return( y * YER_1ST ); }
    inline DateV make_month( int m )    { return( m * MNT_1ST ); }
    inline DateV make_day( int d )      { return( d * DAY_1ST ); }
    inline DateV make( unsigned y, unsigned m, unsigned d )
    { return( make_year( y ) + make_month( m ) + make_day( d ) ); }
    inline DateV make( unsigned y, unsigned M, unsigned d, unsigned h, unsigned m, unsigned s )
    { return( make_year( y ) + make_month( M ) + make_day( d ) +
              make_hour( h ) + make_min( m ) + s ); }
    DateV make( const String& );
    DateV make( const Glib::DateTime& );
    inline DateV make_from_ctime( const tm* ti )
    {
        return make( ti->tm_year + 1900, ti->tm_mon + 1, ti->tm_mday,
                     ti->tm_hour, ti->tm_min, ti->tm_sec );
    }
    inline DateV make_from_ctime( const time_t t )
    { return make_from_ctime( localtime( &t ) ); }

    // ISOLATORS
    inline DateV isolate_mins( DateV d )  { return( d % HOR_1ST - d % MIN_1ST ); }
    inline DateV isolate_hours( DateV d ) { return( d % TIM_SEP - d % HOR_1ST ); }
    inline DateV isolate_day( DateV d )   { return( d % MNT_1ST - d % DAY_1ST ); }
    inline DateV isolate_month( DateV d ) { return( d % YER_1ST - d % MNT_1ST ); }
    inline DateV isolate_year( DateV d )  { return( d % DAT_SEP - d % YER_1ST ); }
    inline DateV isolate_MD( DateV d )    { return( d % YER_1ST - d % DAY_1ST ); }
    inline DateV isolate_YM( DateV d )    { return( d % DAT_SEP - d % MNT_1ST ); }
    inline DateV isolate_YMD( DateV d )   { return( d % DAT_SEP - d % DAY_1ST ); }

    // GETTERS
    inline unsigned get_secs( DateV d )  { return(   d % MIN_1ST ); } // same as isolating
    inline unsigned get_mins( DateV d )  { return( ( d % HOR_1ST - d % MIN_1ST ) / MIN_1ST ); }
    inline unsigned get_hours( DateV d ) { return( ( d % TIM_SEP - d % HOR_1ST ) / HOR_1ST ); }
    inline unsigned get_time( DateV d )  { return(   d % TIM_SEP ); } // same as isolating
    inline unsigned get_day( DateV d )   { return( ( d % MNT_1ST - d % DAY_1ST ) / DAY_1ST ); }
    inline unsigned get_month( DateV d ) { return( ( d % YER_1ST - d % MNT_1ST ) / MNT_1ST ); }
    inline unsigned get_year( DateV d )  { return( ( d % DAT_SEP - d % YER_1ST ) / YER_1ST ); }

    bool is_leap_year_gen( const unsigned );
    inline bool is_leap_year( const DateV d ) { return is_leap_year_gen( get_year( d ) ); }

    Glib::Date::Month get_month_glib( const DateV d );
    unsigned get_week( const DateV );
    unsigned get_weekday( const DateV );
    unsigned get_yearday( const DateV );
    unsigned get_days_in_month( const DateV );
    inline unsigned get_days_in_year( const DateV d )
    { return( is_leap_year( d ) ? 366 : 365 ); }
    inline unsigned get_days_since_min( const DateV d )
    {
        unsigned    result { get_yearday( d ) };
        auto        year   { get_year( d ) };
        for( auto y = YEAR_MIN; y < year; ++y )
            result += ( is_leap_year_gen( y ) ? 366 : 365 );
        return result;
    }
    inline int64_t get_secs_since_min( const DateV d )
    { return( get_secs( d ) + 60 * get_mins( d )
                            + 3600 * get_hours( d )
                            + 86400 * int64_t( get_days_since_min( d ) ) ); }

    String get_duration_str( const DateV, const DateV );

    inline Glib::Date get_glib( const DateV d )
    { return Glib::Date( get_day( d ), get_month_glib( d ), get_year( d ) ); }

    inline DateV get_today()
    {
        time_t t = time( NULL );
        struct tm* ti = localtime( &t );
        return make( ti->tm_year + 1900, ti->tm_mon + 1, ti->tm_mday );
    }
    inline DateV get_now()
    {
        time_t t = time( NULL );
        struct tm* ti = localtime( &t );
        return make_from_ctime( ti );
    }

    // SETTERS
    inline bool set_sec( DateV& date, const unsigned s )
    {
        if( s > 59 ) return false;
        date = ( date - ( date % MIN_1ST ) + s );
        return true;
    }
    inline bool set_min( DateV& date, const unsigned m )
    {
        if( m > 59 ) return false;
        date = ( date - ( date % HOR_1ST ) + ( date % MIN_1ST ) + make_min( m ) );
        return true;
    }
    inline bool set_hour( DateV& date, const unsigned h )
    {
        if( h > 23 ) return false;
        date = ( date - ( date % TIM_SEP ) + ( date % HOR_1ST ) + make_hour( h ) );
        return true;
    }

    inline void set_time( DateV& date, unsigned t )
    { if( t <= TIME_MAX ) date = ( date - ( date % TIM_SEP ) + ( t % TIM_SEP ) ); }

    inline void set_day( DateV& date, unsigned d )
    { if( d < 32 ) date = ( date - ( date % MNT_1ST - date % DAY_1ST ) + make_day( d ) ); }

    inline void set_month( DateV& date, unsigned m )
    { if( m < 13 ) date = ( date - ( date % YER_1ST - date % MNT_1ST ) + make_month( m ) ); }

    inline void set_year( DateV& date, unsigned y )
    { if( y >= YEAR_MIN && y <= YEAR_MAX ) date = ( ( date % YER_1ST ) + make_year( y ) ); }

    // CHECKERS
    inline bool is_set( const DateV date ) { return( date != NOT_SET ); }
    inline bool is_valid( const DateV d )
    {
        const auto day    { get_day( d ) };
        const auto month  { get_month( d ) };
        return( day > 0 && month > 0 && month < 13 && day <= get_days_in_month( d ) );
    }
    inline bool is_legit( const DateV d )
    { return( is_valid( d ) && get_year( d ) <= YEAR_MAX && get_year( d ) >= YEAR_MIN ); }

    // SHIFTERS
    void forward_months( DateV&, unsigned int );
    void forward_days( DateV&, unsigned int );
    void backward_months( DateV&, unsigned int );
    void backward_days( DateV&, unsigned int );
    void backward_to_week_start( DateV& );
    inline void backward_to_month_start( DateV& date )
    { date = ( isolate_year( date ) + isolate_month( date ) + make_day( 1 ) ); }
    inline void backward_to_year_start( DateV& date )
    { date = ( isolate_year( date ) + make_month( 1 ) + make_day( 1 ) ); }
    inline void offset_days( DateV& d, int offset )
    {
        if      ( offset < 0 ) backward_days( d, -offset );
        else if ( offset > 0 ) forward_days( d, offset );
    }

    // CONVERSION TO STRING
    String format_string( const DateV, const String&, const char = s_format_separator );
    inline String format_string( const DateV date )
    { return format_string( date, s_format_order, s_format_separator ); }
    String format_string_adv( const DateV, const String& );
    inline String format_string_time( const DateV time )
    {
        char buffer[ 9 ] = "";

        if( time >= 0 )
            snprintf( buffer, 9, "%02d:%02d:%02d", get_hours( time ),
                                                   get_mins( time ),
                                                   get_secs( time ) );
        return buffer;
    }
    String  get_format_str_default();
    //Ustring format_string_ctime();
    Ustring get_weekday_str( const DateV );
    Ustring get_month_name( const DateV );
    inline String get_month_str( const DateV d ) { return std::to_string( get_month( d ) ); };
    inline String get_year_str( const DateV d ) { return std::to_string( get_year( d ) ); };
    Ustring get_day_name( int );

    // CALCULATORS
    uint64_t     calculate_secs_between_abs( const DateV, const DateV );
    unsigned int calculate_days_between_abs( const DateV, const DateV );
    inline int   calculate_days_between( const DateV d1, const DateV d2 )
    {
        const auto dist_abs{ calculate_days_between_abs( d1, d2 ) };
        return( d1 > d2 ? -dist_abs : dist_abs );
    }
    unsigned int calculate_weeks_between_abs( const DateV, const DateV );
    int          calculate_months_between( const DateV, const DateV );
    inline unsigned int calculate_months_between_abs( const DateV d1, const DateV d2 )
    { return( d1 > d2 ? -calculate_months_between( d1, d2 )
                      : calculate_months_between( d1, d2 ) ); }

} // end of namespace DateTime

typedef bool( *FuncCompareDates )( const DateV&, const DateV& ) ;

inline bool
compare_dates( const DateV& date_l, const DateV& date_r )
{
    return( date_l > date_r );
}

// CUSTOM CONTAINER OR POLLING OBJECTS =============================================================
template< typename KeyType >
class PollMap
{
    public:
        // Insert or update
        void
        set( const KeyType& key, const int value )
        {
            auto it { key_to_value.find( key ) };
            if( it != key_to_value.end() )
            {
                int   old_value { it->second };
                auto  range     { value_to_key.equal_range( old_value ) };
                for( auto v_it = range.first; v_it != range.second; ++v_it )
                {
                    if( v_it->second == key )
                    {
                        value_to_key.erase( v_it );
                        break;
                    }
                }
            }
            key_to_value[ key ] = value;
            value_to_key.insert( { value, key } );
        }

        void
        increase( const KeyType& key )
        {
            auto  it    { key_to_value.find( key ) };
            int   value { 0 };
            if( it != key_to_value.end() )
            {
                value = it->second;
                auto range { value_to_key.equal_range( value ) };
                for( auto v_it = range.first; v_it != range.second; ++v_it )
                {
                    if( v_it->second == key )
                    {
                        value_to_key.erase( v_it );
                        break;
                    }
                }
            }
            ++value;
            key_to_value[ key ] = value;
            value_to_key.insert( { value, key } );
        }

        // Get value by key
        int
        get_value( const KeyType& key ) const
        {
            auto it { key_to_value.find( key ) };
            if( it == key_to_value.end() )
                throw std::out_of_range( "Key not found" );
            return it->second;
        }

        // Get the key with the lowest value
        KeyType
        get_key_min() const
        {
            if( value_to_key.empty() )
                throw std::runtime_error( "Map is empty" );
            return value_to_key.begin()->second;
        }
        KeyType
        get_key_max() const
        {
            if( value_to_key.empty() )
                throw std::runtime_error( "Map is empty" );
            return value_to_key.rbegin()->second;
        }

        // Remove a key
        void
        remove( const KeyType& key )
        {
            auto it { key_to_value.find( key ) };
            if( it != key_to_value.end() )
            {
                int   value { it->second };
                auto  range { value_to_key.equal_range(value) };
                for( auto v_it = range.first; v_it != range.second; ++v_it )
                {
                    if( v_it->second == key )
                    {
                        value_to_key.erase( v_it );
                        break;
                    }
                }
                key_to_value.erase( it );
            }
        }

        // Size of the container
        size_t size() const { return key_to_value.size(); }
        // Check if empty
        bool empty() const { return key_to_value.empty(); }

    private:
        std::map< KeyType, int >      key_to_value;
        std::multimap< int, KeyType > value_to_key;
};


// CONSOLE MESSAGES ================================================================================
class Console
{
    private:
        static void print( std::ostream& os )
        {
            os << std::endl;
        }

        template< typename Arg1, typename... Args >
        static void print( std::ostream& os, Arg1 arg1, Args... args )
        {
            try{ os << arg1; } // this is necessary for Windows iconv exceptions
            catch( Glib::Error& er )
            { os << "Error printing text:" << er.what() << std::endl; }

            print( os, args... );
        }

#if LIFEOGRAPH_DEBUG_BUILD
        static unsigned DEBUG_LINE_I;
#endif

    template< typename... Args >
    friend void print_error( Args... );

    template< typename... Args >
    friend void print_info( Args... );

    template< typename... Args >
    friend void PRINT_DEBUG( Args... );
};

template< typename... Args >
void print_info( Args... args )
{
    Console::print( std::cout, "INFO: ", args... );
}

template< typename... Args >
void print_error( Args... args )
{
    Console::print( std::cerr, "ERROR: ", args... );
}

#if LIFEOGRAPH_DEBUG_BUILD
    template< typename... Args >
    void PRINT_DEBUG( Args... args )
    {
        Console::print( std::cout, "* DBG:", Console::DEBUG_LINE_I++, " * ", args... );
    }
#else
#define PRINT_DEBUG( ... ) ;
#endif

// COLOR OPERATIONS ================================================================================
struct ColorBasic
{
    ColorBasic() { }
    ColorBasic( double rn, double gn, double bn ) : r( rn ), g( gn ), b( bn ) { }

    double r = -1.0;
    double g = -1.0;
    double b = -1.0;

    // bool is_set() const
    // { return( r >= 0.0 && g >= 0.0 && b >= 0.0 ); }

    void set( double rn, double gn, double bn )
    {
        r = rn;
        g = gn;
        b = bn;
    }

    // void unset()
    // {
    //     r = g = b = -1.0;
    // }
};

inline float
get_color_diff( const Color& c1, const Color c2 )
{
    return( fabs( c2.get_red() - c1.get_red() ) +
            fabs( c2.get_green() - c1.get_green() ) +
            fabs( c2.get_blue() - c1.get_blue() ) );
}

inline Ustring
convert_gdkcolor_to_html( const Color& gdkcolor )
{
    // this function's source of inspiration is Geany
    char buffer[ 8 ];

    snprintf( buffer, 8, "#%02X%02X%02X",
            gdkcolor.get_red_u() >> 8,
            gdkcolor.get_green_u() >> 8,
            gdkcolor.get_blue_u() >> 8 );
    return buffer;
}

inline Ustring
convert_gdkrgba_to_html( const Color& gdkcolor )
{
    char buffer[ 14 ];
    snprintf( buffer, 14, "#%04X%04X%04X",
            int( gdkcolor.get_red() * 0xFFFF ),
            int( gdkcolor.get_green() * 0xFFFF ),
            int( gdkcolor.get_blue() * 0xFFFF ) );
            //int( gdkcolor.get_alpha() * 0xFFFF ) );
    return buffer;
}

// converts a hex color string to uint32_t.
// accepts both "#RRGGBB" and "RRGGBB" formats.
uint32_t            convert_colorstr_to_uint32( const String& hex );

// converts a uint32_t color to a "#RRGGBB" hex string.
inline String
convert_uint32_to_html( uint32_t color )
{
    static constexpr char hex[] = "0123456789ABCDEF";
    char buf[ 8 ];
    buf[ 0 ] = '#';
    buf[ 1 ] = hex[ ( color >> 20 ) & 0xF ];
    buf[ 2 ] = hex[ ( color >> 16 ) & 0xF ];
    buf[ 3 ] = hex[ ( color >> 12 ) & 0xF ];
    buf[ 4 ] = hex[ ( color >> 8 ) & 0xF ];
    buf[ 5 ] = hex[ ( color >> 4 ) & 0xF ];
    buf[ 6 ] = hex[ ( color ) & 0xF ];
    buf[ 7 ] = '\0';
    return String( buf, 7 ); // known length, no strlen scan
}

uint32_t            get_contrasting_color( uint32_t );
Color               contrast2( const Color&, const Color&, const Color& );
Color               midtone( const Color&, const Color& );
Color               midtone( const Color&, const Color&, double, double = 1.0 );

// FILE OPERATIONS =================================================================================
#ifdef _WIN32
String              get_exec_path();
#endif

std::ios::pos_type  get_file_size( std::ifstream& );

bool                copy_file( const String&, const String&, bool );
bool                copy_file_suffix( const String&, const String&, int, bool );

bool                is_dir( const String& );
bool                is_dir( const Glib::RefPtr< Gio::File >& );

bool                check_path_exists( const String& );
bool                check_uri_writable( const Glib::RefPtr< Gio::File >& );
inline bool         check_uri_writable( const String& uri )
{ return check_uri_writable( Gio::File::create_for_uri( uri ) ); }

String              evaluate_path( const String& );

inline String
get_filename_base( const String& path )
{
    auto file { Gio::File::create_for_commandline_arg( path ) };
    return file->get_basename();
}

Ustring             sanitize_file_path( const Ustring& );

#ifdef _WIN32

String
convert_filename_to_win32_locale( const String& );

inline String
convert_locale_to_utf8( const String& str )
{
    return Glib::locale_to_utf8( str );
}

#define PATH( A ) convert_filename_to_win32_locale( A )
#define PATH2( A ) convert_utf8_to_16( A )
#else
#define PATH( A ) ( A )
#define PATH2( A ) ( A )
#define convert_locale_to_utf8( A ) ( A )
#endif

// the following is not used right now but may be helpful in the future
void                get_all_files_in_path( const String&, SetStrings& );

String              read_text_file( const String& );

// ENCRYPTION ======================================================================================
class Cipher
{
    public:
        static const int    cCIPHER_ALGORITHM   = GCRY_CIPHER_AES256;
        static const int    cCIPHER_MODE        = GCRY_CIPHER_MODE_CFB;
        static const int    cIV_SIZE            = 16; // = 128 bits
        static const int    cSALT_SIZE          = 16; // = 128 bits
        static const int    cKEY_SIZE           = 32; // = 256 bits
        static const int    cHASH_ALGORITHM     = GCRY_MD_SHA256;

        static bool         init();

        static void         create_iv( unsigned char** );
        static void         expand_key( char const*,
                                        const unsigned char*,
                                        unsigned char** );
        static void         create_new_key( char const*,
                                            unsigned char**,
                                            unsigned char** );
        static void         encrypt_buffer( unsigned char*,
                                            size_t&,
                                            const unsigned char*,
                                            const unsigned char* );
        static void         decrypt_buffer( unsigned char*,
                                            size_t,
                                            const unsigned char*,
                                            const unsigned char* );

    protected:

    private:

};

struct CipherBuffers
{
    CipherBuffers()
    :   buffer( NULL ), salt( NULL ), iv( NULL ), key( NULL ) {}

    unsigned char* buffer;
    unsigned char* salt;
    unsigned char* iv;
    unsigned char* key;

    void clear()
    {
        if( buffer ) delete[] buffer;
        if( salt ) delete[] salt;
        if( iv ) delete[] iv;
        if( key ) delete[] key;
    }
};

// DROP POSITIONS ==================================================================================
enum class DropPosition { NONE, BEFORE, INTO, AFTER };
// AFTER can be regarded as INTO under certain circumstances
// but AFTER_ABSOLUTE cannot

} // end of namespace HELPERS

#endif

