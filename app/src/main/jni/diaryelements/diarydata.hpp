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


#ifndef LIFEOGRAPH_DIARYDATA_HEADER
#define LIFEOGRAPH_DIARYDATA_HEADER


#include <set>
#include <map>
#include <vector>
#include <variant>
#ifndef __ANDROID__
#include <gtkmm/stringlist.h>
#endif

#include "../helpers.hpp"  // i18n headers
#include "../logid.hpp"


namespace LoG
{

// clashes with pur definition:
#ifdef _WIN32
#undef PASCAL
#endif

using namespace HELPERS;

// VALUE TYPES
namespace VT
{
    // REGISTRATION MACROS
    #define REGISTER_PROP_INTERNAL( INDEX, INDEX_PREV, PROPNAME, CHR, NUM, STR ) \
            struct PROP_##INDEX \
            { \
                static constexpr unsigned IND = INDEX; \
                static constexpr int      I   = NUM; \
                static constexpr char     C   = CHR; \
                static constexpr char     S[] = STR; \
                using PREV = PROP_##INDEX_PREV; \
                template< typename T > static T get(); \
            }; \
            using PROPNAME = PROP_##INDEX

    #define REG_PROP_GETTERS( PROPNAME ) \
        template< typename T > T PROPNAME::get() \
        { throw LoG::Error( "Bad cast in Property!" ); return T(); } \
        template<> int          PROPNAME::get< int >() { return I; } \
        template<> char         PROPNAME::get< char >() { return C; } \
        template<> char const * PROPNAME::get< char const * >() { return S; } \
        template<> unsigned     PROPNAME::get< unsigned >() { return IND; }

    // NOTE: the properties are iterated over in the reverse order as we only have PREV pointers
    #define REG_PROP_FRST( CLSNAME, CHR, NUM, PROPNAME, STR ) \
        struct CLSNAME { \
            REGISTER_PROP_INTERNAL( 0, 0, PROPNAME, CHR, NUM, STR )

    #define REG_PROP_01( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  1,  0, NAME, CH, NUM, STR )
    #define REG_PROP_02( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  2,  1, NAME, CH, NUM, STR )
    #define REG_PROP_03( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  3,  2, NAME, CH, NUM, STR )
    #define REG_PROP_04( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  4,  3, NAME, CH, NUM, STR )
    #define REG_PROP_05( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  5,  4, NAME, CH, NUM, STR )
    #define REG_PROP_06( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  6,  5, NAME, CH, NUM, STR )
    #define REG_PROP_07( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  7,  6, NAME, CH, NUM, STR )
    #define REG_PROP_08( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  8,  7, NAME, CH, NUM, STR )
    #define REG_PROP_09( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL(  9,  8, NAME, CH, NUM, STR )
    #define REG_PROP_10( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 10,  9, NAME, CH, NUM, STR )
    #define REG_PROP_11( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 11, 10, NAME, CH, NUM, STR )
    #define REG_PROP_12( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 12, 11, NAME, CH, NUM, STR )
    #define REG_PROP_13( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 13, 12, NAME, CH, NUM, STR )
    #define REG_PROP_14( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 14, 13, NAME, CH, NUM, STR )
    #define REG_PROP_15( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 15, 14, NAME, CH, NUM, STR )
    #define REG_PROP_16( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 16, 15, NAME, CH, NUM, STR )
    #define REG_PROP_17( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 17, 16, NAME, CH, NUM, STR )
    #define REG_PROP_18( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 18, 17, NAME, CH, NUM, STR )
    #define REG_PROP_19( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 19, 18, NAME, CH, NUM, STR )
    #define REG_PROP_20( CH, NUM, NAME, STR ) REGISTER_PROP_INTERNAL( 20, 19, NAME, CH, NUM, STR )

    #define REG_PROP_LAST( INDEX ) using LAST = PROP_##INDEX

    /* TODO: 3.2: an apparently futile attempt to make things more elegant:
    #define REGISTER_PROP_INTERNAL_P1( INDEX, PROPNAME, PROP_PREV ) \
            struct PROPNAME \
            { \
                static constexpr unsigned IND = INDEX; \
                using PREV = PROP_PREV

    #define REGISTER_PROP_INTERNAL_P2( CHR, NUM, STR ) \
                static constexpr int      VINT = NUM; \
                static constexpr char     VCHR = CHR; \
                static constexpr char     VSTR[] = STR; \
                template< typename T > \
                static T get() \
                { \
                    if( typeid( T ) == typeid( int ) )      return VINT; \
                    if( typeid( T ) == typeid( char ) )     return VCHR; \
                    if( typeid( T ) == typeid( unsigned ) ) return INDX; \
                    if( typeid( T ) == typeid( char* ) )    return VSTR; \
                    return T(); \
                } \
            }

    #define REGISTER_PROP_INTERNAL_RECURSIVE( PROPNAME, CHR, NUM, STR, PROPNAME_NEXT, ... ) \
        REGISTER_PROP_INTERNAL_P2( CHR, NUM, STR ); \
        REGISTER_PROP_INTERNAL_P1( PROPNAME::INDX + 1, PROPNAME_NEXT, PROPNAME ); \
        REGISTER_PROP_INTERNAL_RECURSIVE( PROPNAME_NEXT, __VA_ARGS__ )

    #define REG_PROPS( CLSNAME, PROPNAME, ... ) \
        struct CLSNAME { \
            REGISTER_PROP_INTERNAL_P1( 0, PROPNAME, PROPNAME );
            REGISTER_PROP_INTERNAL_RECURSIVE( PROPNAME, __VA_ARGS__ );
            using LAST = PROPNAME
    */

    // not ordered property:
    #define REG_PROP_XTRA( PROPNAME, NUM ) static constexpr int PROPNAME = NUM
    #define REG_PROP_ALIAS( PROPNAME, CLASS_ORIG ) \
            using PROPNAME = CLASS_ORIG

    #define REG_PROP_END } // enf of property CLSNAME


    template<typename A, typename B>
    bool are_equal( A const& a, B const& b ) { return a == b; }
    inline bool are_equal( char const* a, char const* b ) { return strcmp( a, b ) == 0; }

    template< typename PROP, typename RT, typename MT >
    RT get_prop_value_internal( MT v )
    {
        if( are_equal( PROP::template get< MT >(), v ) )
            return PROP::template get< RT >();
        else if( typeid( typename PROP::PREV ) == typeid( PROP ) ) // last item reached
        {
            print_error( "Failed property request: ", PROP::S, "::get< ",
                         typeid( RT ).name(), ", ",
                         typeid( MT ).name(), " >(); Value = ", v );
            // throw LoG::Error( STR::compose( "Failed property request: ", PROP::S, "::get< ",
            //                                   typeid( RT ).name(), ", ",
            //                                   typeid( MT ).name(), " >()" ) );
            return RT(); // to prevent compiler complaining
        }
        else
            return get_prop_value_internal< typename PROP::PREV, RT, MT >( v );
    }

    template< typename CLASS, typename RT, typename MT >
    RT get_v( MT v )
    { return get_prop_value_internal< typename CLASS::LAST, RT, MT >( v ); }

    template< typename PROP >
    void stringize_bitmap_internal( const int& bitmap, char* str )
    {
        str[ PROP::IND ] = ( ( bitmap & PROP::I ) ? PROP::C : '~' );

        if( typeid( typename PROP::PREV ) != typeid( PROP ) ) // last item not reached
            stringize_bitmap_internal< typename PROP::PREV >( bitmap, str );
    }

    template< typename CLASS >
    const String stringize_bitmap( const int& bitmap )
    {
        char str[ CLASS::LAST::IND + 2 ];
        str[ CLASS::LAST::IND + 1 ] = '\0'; // terminating zero
        stringize_bitmap_internal< typename CLASS::LAST >( bitmap, str );
        return str;
    }

    template< typename PROP >
    void bitmapize_string_internal( const String& str, int& bitmap )
    {
        if( str[ PROP::IND ] == PROP::C )
            bitmap |= PROP::I;

        if( typeid( typename PROP::PREV ) != typeid( PROP ) ) // last item not reached
            bitmapize_string_internal< typename PROP::PREV >( str, bitmap );
    }

    template< typename CLASS >
    int bitmapize_string( const String& str )
    {
        int bitmap { 0 };
        bitmapize_string_internal< typename CLASS::LAST >( str, bitmap );
        return bitmap;
    }

    template< typename PROP >
    VecUstrings vectorize_propspace( VecUstrings&& vs )
    {
        vs.push_back( PROP::S );

        if( PROP::IND > 0 )
            return vectorize_propspace< typename PROP::PREV >( std::move( vs ) );
        else
            std::reverse( vs.begin(), vs.end() );
        return vs;
    }
    template< typename PROP >
    VecUstrings vectorize_propspace()
    {
        VecUstrings vs = { PROP::S };

        if( PROP::IND > 0 )
            return vectorize_propspace< typename PROP::PREV >( std::move( vs ) );
        else
            return vs;
    }

#ifndef __ANDROID__
    template< typename PROP >
    auto strlistize_propspace()
    { return Gtk::StringList::create( vectorize_propspace< typename PROP::LAST >() ); }
#endif

    // CONSTANTS
    static constexpr int SEQ_FILTER = 0xF;
    static constexpr int ADD_PROPERTIES_FILTER = 0xFF000000;
    static constexpr int BODY_FILTER           = 0x00FFFFFF;

    // ARITHMETIC RELATIONS
    static constexpr char AR_CHARS[]      = "<=>";
    static constexpr int R_LESS           = 0,
                         R_EQUAL          = 1,
                         R_MORE           = 2;

    // DATES
    static constexpr int DATE_START         = 0,
                         DATE_CREATION      = 1,
                         DATE_CHANGE        = 2,
                         DATE_FINISH        = 3;
    static constexpr int DATE_CUSTOM_FORMAT = 0x1000000;    // addtl property

    // DIARY OPTIONS ...............................................................................
    REG_PROP_FRST( DO, 'A', 0x0010, SHOW_ALL_ENTRY_LOCATIONS, N_( "Show All Entries in Map" ) );
    REG_PROP_01(       'C', 0x0200, SEARCH_MATCH_CASE,        N_( "Match Case in Searches" ) );
    REG_PROP_02(       'R', 0x0400, SEARCH_USE_REGEX,         N_( "Use Regular Expressions" ) );
    REG_PROP_03(       'W', 0x1000, SHOW_WEEK_NOS,            N_( "Show Week Nos" ) );
    REG_PROP_LAST( 3 );
    REG_PROP_XTRA( DEFAULT, 0x0 );
    REG_PROP_END;

    // TAG VALUE TYPE / COMBINATION ................................................................
    REG_PROP_FRST( TVTC, 'A', 0x010,  TOTAL,    N_( "Total" ) );
    REG_PROP_01(         'C', 0x020,  AVERAGE,  N_( "Average" ) );
    REG_PROP_LAST( 1 );
    REG_PROP_END;

    // TAG VALUE TYPE / STATUS .....................................................................
    REG_PROP_FRST( TVTS, 'A', 0x040,  PLANNED,    N_( "Planned" ) );
    REG_PROP_01(         'C', 0x080,  REALIZED,   N_( "Realized" ) );
    REG_PROP_02(         'C', 0x100,  REMAINING,  N_( "Remaining" ) );
    REG_PROP_LAST( 2 );
    REG_PROP_END;

    // TAG VALUE TYPE ..............................................................................
    REG_PROP_FRST( TVT,  'P', 0x050,  TOTAL_PLANNED,      N_( "Total Planned" ) );
    REG_PROP_01(         'R', 0x091,  TOTAL_REALIZED,     N_( "Total Realized" ) );
    REG_PROP_02(         'B', 0x112,  TOTAL_REMAINING,    N_( "Total Remaining" ) );
    REG_PROP_03(         'p', 0x063,  AVERAGE_PLANNED,    N_( "Average Planned" ) );
    REG_PROP_04(         'r', 0x0A4,  AVERAGE_REALIZED,   N_( "Average Realized" ) );
    REG_PROP_05(         'b', 0x125,  AVERAGE_REMAINING,  N_( "Average Remaining" ) );
    REG_PROP_06(         'C', 0x006,  COMPL_PERCENTAGE,   N_( "Completion Percentage" ) );
    REG_PROP_LAST( 6 );
    REG_PROP_END;
    // TOTAL_PLANNED    = TOTAL   + PLANNED
    // TOTAL_REALIZED   = TOTAL   + REALIZE  + 1
    // TOTAL_REMAINING  = TOTAL   + REMAINING + 2
    // AVG_PLANNED      = AVERAGE + PLANNED   + 3
    // AVG_REALIZED     = AVERAGE + REALIZED  + 4
    // AVG_REMAINING    = AVERAGE + REMAINING + 5

    // TABLE COLUMN OPTIONS: GENERAL
    static constexpr int TCO_NONE               = 0x0;

    // TABLE COLUMN OPTIONS: SUBTAGS
    static constexpr int FIRST                  = 0,
                         LAST                   = 1,
                         LOWEST                 = 2,
                         HIGHEST                = 3,
                         TCOS_ALL               = 4;

    // TABLE COLUMN OPTIONS: TEXT
    static constexpr int TCT_FILTER_COMPONENT   = 0x0FFF0;
    static constexpr int TCT_CMPNT_PLAIN        = 0x10,
                         TCT_CMPNT_TAG          = 0x20,
                         TCT_CMPNT_DATE         = 0x40,
                         TCT_CMPNT_COMMENT      = 0x80,
                         TCT_CMPNT_INDENT       = 0x100,
                         TCT_CMPNT_NUMBER       = 0x200;
    // source fields:
    static constexpr int TCT_SRC_TITLE          = 0x0, // means text for paragraphs
                         TCT_SRC_ANCESTRAL      = 0x1,
                         TCT_SRC_DESCRIPTION    = 0x2,
                         TCT_SRC_URI            = 0x3;
    static constexpr int TCT_DEFAULT            = TCT_CMPNT_PLAIN|TCT_SRC_TITLE;

    // SHOW AS OPTIONS .............................................................................
    REG_PROP_FRST( SAS, '_', 0x00, NORMAL,   N_( "Normal" ) );
    REG_PROP_01(        'D', 0x01, DELTA,    N_( "Delta" ) );
    REG_PROP_02(        'C', 0x02, COUNT,    N_( "Count" ) );
    REG_PROP_LAST( 2 );
    REG_PROP_ALIAS( DEFAULT, NORMAL );
    REG_PROP_END;

    // SUMMARY FUNCTIONS ...........................................................................
    REG_PROP_FRST( SUMF, '_', 0x00, NONE,   N_( "None" ) );
    REG_PROP_01(         'S', 0x01, SUM,    N_( "Summate" ) );
    REG_PROP_02(         'A', 0x02, AVG,    N_( "Average" ) );
    REG_PROP_03(         'X', 0x03, MAX,    N_( "Max" ) );
    REG_PROP_04(         'N', 0x04, MIN,    N_( "Min" ) );
    REG_PROP_05(         'D', 0x05, DTC,    N_( "Distinct Count" ) );
    REG_PROP_06(         'I', 0x06, CIN,    N_( "Compute Independently" ) );
    REG_PROP_LAST( 6 );
    REG_PROP_ALIAS( DEFAULT, NONE );
    REG_PROP_END;

    // SIZE OPTIONS ................................................................................
    REG_PROP_FRST( SO, 'C', 0x00, CHAR_COUNT, N_( "Char Count" ) );
    REG_PROP_01(       'P', 0x01, PARA_COUNT, N_( "Paragraph Count" ) );
    REG_PROP_LAST( 1 );
    REG_PROP_ALIAS( DEFAULT, CHAR_COUNT );
    REG_PROP_XTRA( FILTER, 0x0F );
    REG_PROP_END;
    // TODO: 3.2 or later:  REG_PROP_INTR( 1, 0, 'W', 0x02, WORD_COUNT, N_( "Word Count" ) );

    // SUMMARY FUNCTIONS ...........................................................................
    REG_PROP_FRST( OP_DEPTH, 'I', 0x01, ITSELF,            N_( "Itself Only " ) );
    REG_PROP_01(             'A', 0x03, ITSELF_AND_DESCS,  N_( "Itself & Descendants" ) );
    REG_PROP_02(             'D', 0x02, DESCENDANTS,       N_( "Descendants Only" ) );
    REG_PROP_LAST( 2 );
    REG_PROP_ALIAS( DEFAULT, ITSELF );
    REG_PROP_END;

    // TABLE COLUMN OPTIONS: DURATION
    static constexpr char TCD_CHARS[]           = "_SCHFN12";
    static constexpr int TCD_FILTER_BGN         = 0x0F;
    static constexpr int TCD_FILTER_END         = 0xF0;
    static constexpr int TCD_FILTER_TYPE        = 0xF00;
    static constexpr int TCD_BGN_START          = 0x01,
                         TCD_BGN_CREATION       = 0x02,
                         TCD_BGN_CHANGE         = 0x03,
                         TCD_BGN_FINISH         = 0x04,
                         TCD_BGN_NOW            = 0x05,
                         TCD_BGN_L1             = 0x06,
                         TCD_BGN_L2             = 0x07,
                         TCD_END_START          = 0x10,
                         TCD_END_CREATION       = 0x20,
                         TCD_END_CHANGE         = 0x30,
                         TCD_END_FINISH         = 0x40,
                         TCD_END_NOW            = 0x50,
                         TCD_END_L1             = 0x60,
                         TCD_END_L2             = 0x70,
                         TCD_TYPE_WORK_DAYS     = 0x100;  // reuses the date chars for economy
    static constexpr int TCD_DEFAULT            = TCD_BGN_START|TCD_END_FINISH;

    // TABLE COLUMN OPTIONS: ARITHMETIC: OPERATION .................................................
    REG_PROP_FRST( TCAo, 'A', 0x0000000, ADD, N_( "Add" ) );
    REG_PROP_01(         'S', 0x0000100, SUB, N_( "Subtract" ) );
    REG_PROP_02(         'M', 0x0000200, MUL, N_( "Multiply" ) );
    REG_PROP_03(         'D', 0x0000300, DIV, N_( "Divide" ) );
    REG_PROP_04(         'P', 0x0000400, POW, N_( "Power" ) );
    REG_PROP_05(         'R', 0x0000500, ROO, N_( "Root" ) );
    REG_PROP_06(         'O', 0x0000600, MOD, N_( "Modulus" ) );
    REG_PROP_LAST( 6 );
    REG_PROP_XTRA( FILTER,    0x0000F00 );
    REG_PROP_END;
    // TABLE COLUMN OPTIONS: ARITHMETIC: FORMATTING ................................................
    REG_PROP_FRST( TCAf, 'I', 0x0000000, INT, N_( "Integer" ) );
    REG_PROP_01(         'R', 0x0010000, REA, N_( "Real Number" ) );
    REG_PROP_02(         '2', 0x0020000, RE2, N_( "Real Number (.##)" ) );
    REG_PROP_03(         'P', 0x0030000, PTG, N_( "Percentage" ) );
    REG_PROP_LAST( 3 );
    REG_PROP_XTRA( FILTER,    0x00F0000 );
    REG_PROP_XTRA( SHIFT,     16 );
    REG_PROP_END;
    // TABLE COLUMN OPTIONS: ARITHMETIC: UNARY MODIFIERS ...........................................
    REG_PROP_FRST( TCAu, '_', 0x0000000, NON, "" );
    REG_PROP_01(         '-', 0x0100000, MNS, "(-)" );
    REG_PROP_02(         'R', 0x0200000, RCP, "1/" );
    REG_PROP_03(         'A', 0x0300000, ABS, "ABS" );
    REG_PROP_LAST( 3 );
    REG_PROP_XTRA( FILTER_A,  0x0F00000 );
    REG_PROP_XTRA( FILTER_B,  0xF000000 );
    REG_PROP_XTRA( SHIFT_A,   20 );
    REG_PROP_XTRA( SHIFT_B,   24 );
    REG_PROP_END;
    // TABLE COLUMN OPTIONS: ARITHMETIC: COMMON ....................................................
    static constexpr int TCA_DEFAULT            = TCAo::ADD::I|TCAf::INT::I|TCAu::NON::I;
    static constexpr int TCA_FLAG_OPD_A         = 0x01000;
    static constexpr int TCA_FLAG_OPD_B         = 0x02000;
    static constexpr int TCA_FLAG_CONST_A       = 0x03000;
    static constexpr int TCA_FLAG_CONST_B       = 0x04000;

    // SOURCES
    static constexpr char SRC_CHARS[]    = "FGPSCL";
    static constexpr int SRC_PARENT_FILTER  = 0,
                         SRC_GRANDPARENT    = 1,
                         SRC_PARENT         = 2,
                         SRC_ITSELF         = 3,
                         SRC_FCHILD_FILTER  = 4, // in para mode it means prev para
                         SRC_LCHILD_FILTER  = 5; // in para mode it means next para

    // ENTRY TITLE STYLES ..........................................................................
    REG_PROP_FRST( ETS, 'N', 0x0, NAME_ONLY,          N_( "Name only" ) );
    REG_PROP_01(        'S', 0x1, NAME_AND_DESCRIPT,  N_( "Name and Description" ) );
    REG_PROP_02(        'B', 0x2, NUMBER_AND_NAME,    N_( "Name and Number" ) );
    REG_PROP_03(        'D', 0x3, DATE_AND_NAME,      N_( "Date and Name" ) );
    REG_PROP_04(        'M', 0x4, MILESTONE,          N_( "Milestone" ) );
    REG_PROP_05(        'I', 0x5, INHERIT,            N_( "Inherit" ) );
    REG_PROP_LAST( 5 );
    REG_PROP_XTRA( FILTER, 0xF );
    REG_PROP_END;

    // COMMENT STYLES --stored in the same variable as ETS .........................................
    REG_PROP_FRST( CS, 'H', 0x00, HIDDEN,     N_( "Hidden" ) );
    REG_PROP_01(       'N', 0x10, NORMAL,     N_( "Normal" ) );
    REG_PROP_02(       'L', 0x20, HILITED,    N_( "Highlighted" ) );
    REG_PROP_LAST( 2 );
    REG_PROP_XTRA( FILTER, 0xF0 );
    REG_PROP_END;

    // DATE TYPES ..................................................................................
    REG_PROP_FRST( DT, 'S', 0x01, START,      N_( "Start Date" ) );
    REG_PROP_01(       'F', 0x02, FINISH,     N_( "Finish Date" ) );
    REG_PROP_02(       'C', 0x03, CREATION,   N_( "Creation Date" ) );
    REG_PROP_03(       'E', 0x04, LAST_EDIT,  N_( "Edit Date" ) );
    REG_PROP_LAST( 3 );
    REG_PROP_END;

    // ENTRY FLAGS --stored in the same variable as ETS*
    //static constexpr int EF_DATE_START_IS_EXPLICIT  = 0x1000;

    // PARAGRAPH STYLES
    static constexpr int PS_PLAIN       = 0,

                         PS_HEADER_GEN  = 0x100,
                         // PS_NOTHDR      = PS_HEADER_GEN | 0x4,  // not header
                         // PS_HEADER_M    = PS_HEADER_GEN | 0x3,  // subsubheader
                         // PS_HEADER_L    = PS_HEADER_GEN | 0x2,  // subheader
                         // PS_TITLE       = PS_HEADER_GEN | 0x1,  // title
                         // PS_FLT_HEADER  = 0x107,

                         PS_LIST_GEN    = 0x200,
                         PS_TODO_GEN    = 0x400,
                         PS_ORDERED_GEN = 0x800,
                         //PS_FLT_LIST    = PS_LIST_GEN | PS_TODO_GEN | PS_ORDERED_GEN | 0xF0,

                         PS_HRULE_0     =      0x1000,    // basic horizontal rule
                         PS_SEPRTR_P    =      0x2000,    // used in search view
                         PS_SEPRTR_E    =      0x2001,    // used in search view
                         PS_FLT_TYPE    =      0xFFFF,
                         // ATTRIBUTES
                         PS_IMAGE       =   0x10'0000,
                         PS_IMAGE_FILE  = PS_IMAGE | 0x1'0000,
                         PS_IMAGE_CHART = PS_IMAGE | 0x2'0000,
                         PS_IMAGE_TABLE = PS_IMAGE | 0x3'0000,
                         PS_FLT_IMAGE   = PS_IMAGE | 0x7'0000,
                         PS_REORDERED   =   0x40'0000,  // in an ad-hoc entry in an arbitrary order
                         PS_IMAGE_EXPND =   0x80'0000,
                         // INDENTATION
                         PS_FLT_INDENT  =  0xF00'0000,
                         PS_INDENT_1    =  0x100'0000,
                         PS_INDENT_2    =  0x200'0000,
                         PS_INDENT_3    =  0x300'0000,
                         PS_INDENT_4    =  0x400'0000,
                         PS_INDENT_5    =  0x500'0000,
                         PS_INDENT_6    =  0x600'0000,
                         PS_INDENT_MAX  = 6,
                         // ALIGNMENT
                         // PS_FLT_ALIGN   = 0x3000'0000,
                         PS_ALIGN_L     = 0x1000'0000,
                         // PS_ALIGN_C     = 0x2000'0000,
                         // PS_ALIGN_R     = 0x3000'0000,
                         // STATUSES
                         PS_TODO_FORCED = 0x4000'0000,
                         PS_VISIBLE     = 0x8000'0000,

                         PS_DEFAULT     = PS_PLAIN | PS_ALIGN_L | PS_VISIBLE;

    // PARAGRAPH HEADER SIZES ......................................................................
    REG_PROP_FRST( PHS, '_', 0x0,                   NORMAL,   N_( "Normal" ) );
    REG_PROP_01(        'T', PS_HEADER_GEN | 0x7,   TITLE,    N_( "Title" ) );
    REG_PROP_02(        'S', PS_HEADER_GEN | 0x5,   LARGE,    N_( "Large" ) );
    REG_PROP_03(        'B', PS_HEADER_GEN | 0x4,   MEDIUM,   N_( "Medium" ) );
    // REG_PROP_04(        's', PS_HEADER_GEN | 0x3,   SMALL,    N_( "Small" ,) );
    REG_PROP_LAST( 3 );
    REG_PROP_XTRA( FILTER,   PS_HEADER_GEN | 0x7 );
    REG_PROP_END;

    // PARAGRAPH LIST STYLES .......................................................................
    REG_PROP_FRST( PLS, '_', 0x00,                                PLAIN,  N_( "Not List" ) );
    REG_PROP_01(        'O', PS_LIST_GEN | PS_TODO_GEN | 0x10,    TODO,   N_( "Todo" ) );
    REG_PROP_02(        '~', PS_LIST_GEN | PS_TODO_GEN | 0x20,    PROGRS, N_( "Progressed" ) );
    REG_PROP_03(        '+', PS_LIST_GEN | PS_TODO_GEN | 0x30,    DONE,   N_( "Done" ) );
    REG_PROP_04(        'X', PS_LIST_GEN | PS_TODO_GEN | 0x40,    CANCLD, N_( "Canceled" ) );
    REG_PROP_05(        '-', PS_LIST_GEN | 0x50,                  BULLET, N_( "Bullet" ) );
    REG_PROP_06(        '1', PS_LIST_GEN | PS_ORDERED_GEN | 0x60, NUMBER, N_( "Number" ) );
    REG_PROP_07(        'A', PS_LIST_GEN | PS_ORDERED_GEN | 0x70, CLTTR,  N_( "Capital Letter" ) );
    REG_PROP_08(        'a', PS_LIST_GEN | PS_ORDERED_GEN | 0x80, SLTTR,  N_( "Small Letter" ) );
    REG_PROP_09(        'R', PS_LIST_GEN | PS_ORDERED_GEN | 0x90, CROMAN, N_( "Capital Roman" ) );
    REG_PROP_10(        'r', PS_LIST_GEN | PS_ORDERED_GEN | 0xA0, SROMAN, N_( "Small Roman" ) );
    REG_PROP_LAST( 10 );
    REG_PROP_XTRA( FILTER,   PS_LIST_GEN | PS_TODO_GEN | PS_ORDERED_GEN | 0xF0 );
    REG_PROP_END;

    // PARAGRAPH ALIGNMENT .........................................................................
    REG_PROP_FRST( PA,  '<', 0x1000'0000,   LEFT,   N_( "Left" ) );
    REG_PROP_01(        '|', 0x2000'0000,   CENTER, N_( "Center" ) );
    REG_PROP_02(        '>', 0x3000'0000,   RIGHT,  N_( "Right" ) );
    REG_PROP_LAST( 2 );
    REG_PROP_XTRA( FILTER, 0x3000'0000 );
    REG_PROP_END;

    // HIDDEN FORMAT TYPES
    static constexpr int HFT_FILTER_CHARS   =   0x0'007F;  // to cover non-on-the-fly ones
    static constexpr int HFT_F_ONTHEFLY     =   0x1'0000;
    static constexpr int HFT_F_LINK         =   0x4'0000;
    static constexpr int HFT_F_LINK_MANUAL  =   0x8'0000;
    static constexpr int HFT_F_ALWAYS       =  0x10'0000;
    static constexpr int HFT_F_R_CLICK_TOO  =  0x20'0000;
    static constexpr int HFT_F_V_POS        =  0x40'0000;
    static constexpr int HFT_F_REFERENCE    =  0x80'0000;
    static constexpr int HFT_F_NUMERIC      = 0x100'0000;
    static constexpr int HFT_BOLD           = 0,
                         HFT_ITALIC         = 1,
                         HFT_HIGHLIGHT      = 2,
                         HFT_STRIKETHRU     = 3,
                         HFT_UNDERLINE      = 4,
                         HFT_FADED          = 5,
                         HFT_SUBSCRIPT      = 6  | HFT_F_V_POS,
                         HFT_SUPERSCRIPT    = 7  | HFT_F_V_POS,
                         HFT_TAG            = 8  | HFT_F_LINK | HFT_F_REFERENCE,
                         HFT_LINK_URI       = 9  | HFT_F_LINK_MANUAL | HFT_F_LINK,
                         HFT_LINK_EVAL      = 10 | HFT_F_LINK_MANUAL | HFT_F_LINK,
                         HFT_LINK_ID        = 11 | HFT_F_LINK_MANUAL | HFT_F_LINK | HFT_F_REFERENCE,
                         HFT_LINK_ONTHEFLY  = 12 | HFT_F_LINK | HFT_F_ONTHEFLY,
                         HFT_LINK_BROKEN    = 13 | HFT_F_ONTHEFLY,
                         HFT_TAG_VALUE      = 14 | HFT_F_ONTHEFLY | HFT_F_NUMERIC,
                         HFT_COMMENT        = 15 | HFT_F_ONTHEFLY,
                         HFT_TIME           = 16 | HFT_F_ONTHEFLY | HFT_F_NUMERIC,
                         // minute or second (smaller):
                         HFT_TIME_MS        = 17 | HFT_F_ONTHEFLY | HFT_F_NUMERIC,
                         HFT_MISSPELLED     = 18 | HFT_F_ONTHEFLY,
                         HFT_DATE           = 19 | HFT_F_LINK | HFT_F_ONTHEFLY | HFT_F_NUMERIC,
                         HFT_LINK_THEME     = 20 | HFT_F_LINK | HFT_F_ONTHEFLY,
                         HFT_MATCH          = 21 | HFT_F_ONTHEFLY,
                         HFT_IMAGE          = 22 | HFT_F_LINK | HFT_F_ONTHEFLY,
                         HFT_ENTRY_MENU     = 23 | HFT_F_LINK | HFT_F_ALWAYS | HFT_F_ONTHEFLY
                                                 | HFT_F_R_CLICK_TOO,
                         HFT_REFS_MENU      = 24 | HFT_F_LINK | HFT_F_ALWAYS | HFT_F_ONTHEFLY
                                                 | HFT_F_R_CLICK_TOO,
                         HFT_FOLD_EXPANDED  = 25 | HFT_F_LINK | HFT_F_ALWAYS | HFT_F_ONTHEFLY,
                         HFT_FOLD_COLLAPSED = 26 | HFT_F_LINK | HFT_F_ALWAYS | HFT_F_ONTHEFLY,
                         HFT_DATE_ELLIPSIS  = 61 | HFT_F_ONTHEFLY,
                         // code snippets
                         HFT_CODE_KEYWORD   = 81 | HFT_F_ONTHEFLY,
                         HFT_CODE_COMMENT   = 82 | HFT_F_ONTHEFLY,
                         HFT_CODE_STRING    = 83 | HFT_F_ONTHEFLY,

                         HFT_UNSET          = 99;

     // FORMAT TYPES ...............................................................................
    REG_PROP_FRST( FMT, 'B',  HFT_BOLD,           BOLD,           "Bold" );
    REG_PROP_01(        'I',  HFT_ITALIC,         ITALIC,         "Italic" );
    REG_PROP_02(        'H',  HFT_HIGHLIGHT,      HIGHLIGHT,      "Highlight" );
    REG_PROP_03(        'S',  HFT_STRIKETHRU,     STRIKETHRU,     "Strikethrough" );
    REG_PROP_04(        'U',  HFT_UNDERLINE,      UNDERLINE,      "Underline" );
    REG_PROP_05(        'F',  HFT_FADED,          FADED,          "Faded" );
    REG_PROP_06(        'C',  HFT_SUBSCRIPT,      SUBSCRIPT,      "Subscript" );
    REG_PROP_07(        'P',  HFT_SUPERSCRIPT,    SUPERSCRIPT,    "Superscript" );
    REG_PROP_08(        'T',  HFT_TAG,            TAG,            "Tag" );
    REG_PROP_09(        'L',  HFT_LINK_URI,       LINK_URI,       "Link Uri" );
    REG_PROP_10(        'E',  HFT_LINK_EVAL,      LINK_EVAL,      "Link Eval" );
    REG_PROP_11(        'D',  HFT_LINK_ID,        LINK_ID,        "Link ID" );
    // below are on-the-fly formats, char representations are not stored but are used in Android:
    REG_PROP_12(        'v',  HFT_TAG_VALUE,      TAG_VALUE,      "Tag Value" );
    REG_PROP_13(        'c',  HFT_COMMENT,        COMMENT,        "Comment" );
    REG_PROP_14(        't',  HFT_TIME,           TIME,           "Time" );
    REG_PROP_15(        's',  HFT_TIME_MS,        TIME_MS,        "Time M or S" );
    REG_PROP_16(        'd',  HFT_DATE,           DATE,           "Date" );
    REG_PROP_17(        'm',  HFT_MATCH,          MATCH,          "Match" );
    REG_PROP_18(        'k',  HFT_CODE_KEYWORD,   CODE_KEYWORD,   "Code Keyword" );
    REG_PROP_19(        '#',  HFT_CODE_COMMENT,   CODE_COMMENT,   "Code Comment" );
    REG_PROP_20(        'g',  HFT_CODE_STRING,    CODE_STRING,    "Code String" );
    REG_PROP_LAST( 20 );
    REG_PROP_ALIAS( DEFAULT, BOLD ); // used in filter_has_format
    REG_PROP_XTRA( F_LINK_MANUAL,  HFT_F_LINK_MANUAL );
    REG_PROP_END;

     // QUOT TYPE (LITERARY / CODE) ................................................................
    REG_PROP_FRST( QT, '_',      -1, OFF,        "Off" );
    REG_PROP_01(       '"',       0, LITERARY,   "Literary Quote" );
    REG_PROP_02(       '*',   0x100, GENERIC,    "Generic Code" );
    REG_PROP_03(       'P',   0x200, PYTHON,     "Python ⚡" );
    REG_PROP_04(       'B',   0x300, BASH,       "Bash" );
    REG_PROP_05(       'C',   0x400, CPP,        "C/C++" );
    REG_PROP_06(       'F',   0x500, FORTRAN,    "Fortran" );
    REG_PROP_07(       'G',   0x600, GO,         "Go" );
    REG_PROP_08(       'H',   0x700, HASKELL,    "Haskell" );
    REG_PROP_09(       '<',   0x800, HTML,       "HTML" );
    REG_PROP_10(       'J',   0x900, JAVA,       "Java" );
    REG_PROP_11(       'V',   0xA00, JAVASCRIPT, "Javascript" );
    REG_PROP_12(       'K',   0xB00, KOTLIN,     "Kotlin" );
    REG_PROP_13(       '(',   0xC00, LISP,       "Lisp" );
    REG_PROP_14(       'U',   0xD00, LUA,        "Lua" );
    REG_PROP_15(       'L',   0xE00, PASCAL,     "Pascal" );
    REG_PROP_16(       '$',   0xF00, PERL,       "Perl" );
    REG_PROP_17(       'R',   0x000, RUBY,       "Ruby" );
    REG_PROP_18(       'T',  0x1100, RUST,       "Rust" );
    REG_PROP_19(       'S',  0x1200, SCALA,      "Scala" );
    REG_PROP_20(       'Q',  0x1300, SQL,        "SQL" );
    REG_PROP_LAST( 20 );
    REG_PROP_XTRA( KEYWORDS, 1 );
    REG_PROP_XTRA( COMMENTS, 2 );
    REG_PROP_XTRA( STRINGS,  3 );
    REG_PROP_END;

constexpr const char*
get_code_comment_token( char lang )
{
    switch( lang )
    {
        default:
            return "";
        case QT::BASH::C:
        case QT::PERL::C:
        case QT::PYTHON::C:
        case QT::RUBY::C:
            return "#";
        case QT::CPP::C:
        case QT::GO::C:
        case QT::JAVA::C:
        case QT::JAVASCRIPT::C:
        case QT::KOTLIN::C:
        case QT::RUST::C:
        case QT::SCALA::C:
            return "//";
        case QT::FORTRAN::C:
            return "!";
        case QT::HASKELL::C:
        case QT::LUA::C:
        case QT::SQL::C:
            return "--";
        case QT::LISP::C:
            return ";";
    }
}

} // end of namespace VT

typedef int SortCriterion;
static const SortCriterion SoCr_DATE          = 0x1;
static const SortCriterion SoCr_SIZE_C        = 0x2;  // size (char count)
static const SortCriterion SoCr_CHANGE        = 0x3;  // last change date
static const SortCriterion SoCr_NAME          = 0x4;  // name
static const SortCriterion SoCr_CREATION      = 0x5;  // creation date
static const SortCriterion SoCr_FILTER_CRTR   = 0xF;
static const SortCriterion SoCr_DESCENDING    = 0x10;
static const SortCriterion SoCr_ASCENDING     = 0x20;
static const SortCriterion SoCr_FILTER_DIR    = 0xF0;
static const SortCriterion SoCr_INVERSE       = 0x100; // (for v<2000) inverse dir for ordinals
static const SortCriterion SoCr_DESCENDING_T  = 0x100; // temporal
static const SortCriterion SoCr_ASCENDING_T   = 0x200; // temporal
static const SortCriterion SoCr_FILTER_DIR_T  = 0xF00; // temporal
static const SortCriterion SoCr_DEFAULT       = SoCr_DATE|SoCr_ASCENDING|SoCr_DESCENDING_T;
static const SortCriterion SoCr_DEFAULT_REV   = SoCr_DATE|SoCr_DESCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_DATE_ASC      = SoCr_DATE|SoCr_ASCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_DATE_DSC      = SoCr_DATE|SoCr_DESCENDING|SoCr_DESCENDING_T;
static const SortCriterion SoCr_SIZE_C_ASC    = SoCr_SIZE_C|SoCr_ASCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_SIZE_C_DSC    = SoCr_SIZE_C|SoCr_DESCENDING|SoCr_DESCENDING_T;
static const SortCriterion SoCr_CHANGE_ASC    = SoCr_CHANGE|SoCr_ASCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_CHANGE_DSC    = SoCr_CHANGE|SoCr_DESCENDING|SoCr_DESCENDING_T;
static const SortCriterion SoCr_NAME_ASC      = SoCr_NAME|SoCr_ASCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_NAME_DSC      = SoCr_NAME|SoCr_DESCENDING|SoCr_DESCENDING_T;
static const SortCriterion SoCr_CREATION_ASC  = SoCr_CREATION|SoCr_ASCENDING|SoCr_ASCENDING_T;
static const SortCriterion SoCr_CREATION_DSC  = SoCr_CREATION|SoCr_DESCENDING|SoCr_DESCENDING_T;

typedef unsigned long ElemStatus;
namespace ES
{
    static const ElemStatus _VOID_           = 0x0;
    static const ElemStatus EXPANDED         = 0x40;

    static const ElemStatus NOT_FAVORED      = 0x100;
    static const ElemStatus FAVORED          = 0x200;
    static const ElemStatus FILTER_FAVORED   = NOT_FAVORED|FAVORED;
    static const ElemStatus NOT_TRASHED      = 0x400;
    static const ElemStatus TRASHED          = 0x800;
    static const ElemStatus FILTER_TRASHED   = NOT_TRASHED|TRASHED;
    static const ElemStatus NOT_TODO         = 0x1000;
    // NOTE: NOT_TODO means AUTO when used together with other to do statuses
    static const ElemStatus TODO             = 0x2000;
    static const ElemStatus PROGRESSED       = 0x4000;
    static const ElemStatus DONE             = 0x8000;
    static const ElemStatus CANCELED         = 0x10000;
    static const ElemStatus FILTER_TODO      = NOT_TODO|TODO|PROGRESSED|DONE|CANCELED;
    static const ElemStatus FILTER_TODO_PURE = TODO|PROGRESSED|DONE|CANCELED;
    static const ElemStatus FLT_OPEN_OR_PRGR = TODO|PROGRESSED;

    static const ElemStatus ENTRY_DEFAULT       = NOT_FAVORED|NOT_TRASHED|NOT_TODO;
    static const ElemStatus ENTRY_DEFAULT_FAV   = FAVORED|NOT_TRASHED|NOT_TODO;
    static const ElemStatus CHAPTER_DEFAULT     = ENTRY_DEFAULT; // same as entry now

    // FILTER RELATED CONSTANTS AND ALIASES
    static const ElemStatus SHOW_NOT_FAVORED    = NOT_FAVORED;
    static const ElemStatus SHOW_FAVORED        = FAVORED;
    static const ElemStatus SHOW_NOT_TRASHED    = NOT_TRASHED;
    static const ElemStatus SHOW_TRASHED        = TRASHED;
    static const ElemStatus SHOW_NOT_TODO       = NOT_TODO;
    static const ElemStatus SHOW_TODO           = TODO;
    static const ElemStatus SHOW_PROGRESSED     = PROGRESSED;
    static const ElemStatus SHOW_DONE           = DONE;
    static const ElemStatus SHOW_CANCELED       = CANCELED;

    static const ElemStatus STOCK               = 0x100000;
    static const ElemStatus HAS_VSBL_DESCENDANT = 0x1000000;  // used in filtering
    static const ElemStatus CUT                 = 0x20000000;
    static const ElemStatus FILTERED_OUT        = 0x40000000;

    static const ElemStatus FILTER_MAX          = 0x7FFFFFFF;
    // to go parallel with Java version 0x7FFFFFFF is the max
}

// NEW PROPERTY STORAGE IMPLEMENTATION =============================================================
struct PropValueObj
{
    virtual ~PropValueObj() = default;
    virtual Ustring to_string() const = 0;
    // virtual std::unique_ptr< PropValueObjBase > clone() const = 0;
};

// template< typename Derived >
// struct PropValueObj : public PropValueObjBase {
//     std::unique_ptr< PropValueObjBase > clone() const override
//     { return std::make_unique< Derived >( static_cast< const Derived& >( *this ) ); }
// };
// PropertyValue holds one of the supported types
using PropertyValue = std::variant< bool, char, int, double,
                                    D::DEID,
                                    String,
                                    std::shared_ptr< PropValueObj > >;

// PropertyStorage holds a map from handle to value
class PropertyStorage : public std::unordered_map< LoGID, PropertyValue, LoGIDHasher >
{
public:
    // Set a property
    template< typename T >
    void set( const LoGID& key, T value )
    { ( *this )[ key ] = std::move( value ); }

    template< typename T >
    T get( const LoGID& key, const T& def_val  ) const
    { return get_internal< T >( key ).value_or( def_val ); }

    template< typename T >
    T* get_or_create( const LoGID& key )
    {
        static_assert( std::is_base_of_v< PropValueObj, T >,
                       "T must derive from BaseCloneable");
        static_assert( std::is_default_constructible_v< T >, "T must be default-constructible" );

        // Try to find existing value
        auto it { find( key ) };
        if( it != end() )
        {
            auto* base_ptr = std::get_if< std::shared_ptr< PropValueObj > >( &it->second );
            if( base_ptr && *base_ptr )
            {
                if( auto* casted = dynamic_cast< T* >( base_ptr->get() ) )
                    return casted;
            }
            // If present but not correct type, replace it
        }

        // Create new object and insert
        auto ptr { std::make_shared< T >() };
        T* raw_ptr = ptr.get();  // Save raw pointer before moving

        ( *this )[ key ] = std::move( ptr );

        return raw_ptr;
    }

    // check if a property exists
    bool has( const LoGID& key ) const
    { return( count( key ) > 0 ); }

    // remove a property
    void remove( const LoGID& key )
    { erase( key ); }

    static String
    get_variant_str( const PropertyValue& v )
    {
        return std::visit(
            []( const auto& val ) -> String
            {
                using T = std::decay_t< decltype( val ) >;
                if constexpr     ( std::is_same_v< T, String > )  return val;
                else if constexpr( std::is_same_v< T, bool > )    return val ? "true" : "false";
                else if constexpr( std::is_same_v< T, char > )    return String( 1, val );
                else if constexpr( std::is_same_v< T, std::shared_ptr< PropertyValue > > )
                {
                    if( val )                                     return val->to_string();
                    else                                          return "null";
                }
                else if constexpr( std::is_same_v< T, double > ||
                                   std::is_same_v< T, int > )     return std::to_string( val );
                else if constexpr( std::is_same_v< T, D::DEID > ) return "ID"; // TODO: 3.1: v.get_str( );
                else                                              return "";
            },
            v );
    }

protected:
    // Get a property, returns std::optional<T>
    template< typename T >
    std::optional< T > get_internal( const LoGID& key ) const
    {
        // static_assert( IsSupportedType< T >::value, "Unsupported property type" );

        auto it { find( key ) };
        if( it == end() ) return std::nullopt;

        if( auto val = std::get_if< T >( &it->second ) )
            return *val;
        else
            return std::nullopt;
    }
};

// explicit specialization should be outside the class definition:
template<>
inline void PropertyStorage::set< bool >( const LoGID& key, bool value )
{
    if( value )
        ( *this )[ key ] = value;
    else if( has( key ) )
        remove( key );
}

// ATTRIBUTE CLASS FOR PROPERTIES
class PropertyContainer
{
public:
    PropertyContainer() {}

    PropertyContainer( PropertyContainer* other ) : m_properties( other->m_properties ) {}

    // PropertyStorage&
    // get_properties() noexcept { return m_properties; }
    //
    // const PropertyStorage&
    // get_properties() const noexcept { return m_properties; }

    void
    clear_properties() noexcept { m_properties.clear(); }

    bool
    has_property( const D::PROP& prop ) const noexcept { return m_properties.has( prop ); }

    template< typename T >
    T
    get_property( const D::PROP& prop, T value_on_fail ) const noexcept
    { return m_properties.get< T >( prop, value_on_fail ); }

    template< typename T >
    void
    set_property( const D::PROP& prop, T value ) { return m_properties.set< T >( prop, value ); }

protected:
    PropertyStorage     m_properties;
};

// FORWARD DECLARATIONS
class DiaryElement;
class Paragraph;
class Entry;
class Diary;
class ChartData;
class Filter;
class FiltererContainer;
class TableData; // used in chart data

// NAMED ATTRIBUTE CLASS ===========================================================================
class Named
{
    public:
                                Named() : m_name( "" ) {}
                                Named( const Ustring& name ) : m_name( name ) {}
        virtual                 ~Named() {}  //needed because of virtual methods

        virtual Ustring         get_name() const
        { return m_name; }
        std::string             get_name_std() const  // std::string version
        { return m_name; }
        virtual void            set_name( const Ustring& name )
        { m_name = name; }

    protected:
        Ustring                 m_name;
};

// DIARYELEMENT ====================================================================================
class DiaryElement : public Named
{
    public:
        static const R2Pixbuf   s_pixbuf_null;
        static const Ustring    s_type_names[];

        enum Type
        {   // CAUTION: order is significant and shouldn't be changed!
            // s_type_names above should follow the same order to work
            ET_NONE, ET_CHAPTER_CTG, ET_THEME, ET_FILTER, ET_CHART, ET_TABLE, ET_PARAGRAPH,
            // entry list elements (elements after ENTRY need to be its derivatives):
            ET_DIARY, ET_ENTRY
        };
                                DiaryElement(); // only for pseudo elements
                                DiaryElement( Diary* const, const Ustring&,
                                              ElemStatus = ES::EXPANDED );
        virtual                 ~DiaryElement();

        virtual SKVVec          get_as_skvvec() const
        {
            SKVVec sv;
            sv.push_back( { CSTR::TYPE_NAME, get_type_name() } );
            return sv;
        }

        virtual Type            get_type() const = 0;
        const Ustring           get_type_name() const
        { return s_type_names[ get_type() ]; }
        bool                    is_entry() const
        { return get_type() == DiaryElement::ET_ENTRY; }

        virtual int             get_size() const = 0;

        virtual const R2Pixbuf& get_icon() const
        { return( s_pixbuf_null ); }
        virtual const R2Pixbuf& get_icon32() const
        { return( s_pixbuf_null ); }

        virtual Ustring         get_list_str() const
        { return Glib::Markup::escape_text( m_name ); }

        const D::DEID&          get_id() const { return m_id; }
        D::DEIDF                get_id_full() const;

        bool                    is_equal_to( const DiaryElement* other ) const
        { return( other->m_id == this->m_id ); }

        bool                    is_stock() const
        { return( m_status & ES::STOCK ); }

        Diary*                  get_diary() const
        { return m_p2diary; }

        void                    set_status_flag( ElemStatus status, bool F_add )
        {
            if( F_add )
                m_status |= status;
            else if( m_status & status )
                m_status -= status;
        }

    protected:
        void                    set_id( const D::DEID& id )
        { m_id = id; }

        Diary* const            m_p2diary { nullptr };
        ElemStatus              m_status  { ES::_VOID_ };
        D::DEID                 m_id      { DEID::UNSET };
};

// GET ID FROM ELEM POINTER
inline uint32_t
get_id_raw_failsafe( DiaryElement* elem )
{ return ( elem ? elem->get_id().get_raw() : DEID::UNSET.get_raw() ); }


// COMPARATOR OBJECTS
struct FuncCmpDiaryElemById
{
    bool operator()( DiaryElement* l, DiaryElement* r ) const
    { return( l->get_id() < r->get_id() ); }
};

struct FuncCmpNamedElemByName
{
    bool operator()( Named* l, Named* r ) const
    { return( l->get_name() < r->get_name() ); }
};
struct FuncCmpDiaryElemByNameAndId
{
    bool operator()( DiaryElement* l, DiaryElement* r ) const
    {
        const int cmp { l->get_name().compare( r->get_name() ) };
        return( cmp == 0 ? ( l->get_id() < r->get_id() ) : ( cmp < 0 ) );
    }
};

class DiaryElemTag; // forward declaration
using VecTags                   = std::vector< DiaryElemTag* >;
using ListTags                  = std::list< DiaryElemTag* >;
using SetTagsByNameID           = std::set< DiaryElemTag*, FuncCmpDiaryElemByNameAndId >;
using SetTagByID                = std::set< DiaryElemTag*, FuncCmpDiaryElemById >;

// DIARYELEMENT DATA SOURCE (Entries and Paragraphs) ===============================================
class DiaryElemTag : public DiaryElement, public PropertyContainer
{
    public:
        struct FuncCmpTagByDate
        {
            bool operator()( DiaryElemTag* l, DiaryElemTag* r ) const
            { return( l->get_date() > r->get_date() ); }
        };

        DiaryElemTag( Diary* const d , const Ustring& s, DateV date, ElemStatus es = ES::EXPANDED )
        :   DiaryElement( d, s, es ), m_date( date ), m_date_finish( date ),
            m_date_created( Date::get_now() ), m_date_edited( m_date_created ) {}
        DiaryElemTag( Diary* const d, ElemStatus es = ES::EXPANDED )
        :   DiaryElement( d, "", es ),
            m_date_created( Date::get_now() ), m_date_edited( m_date_created ) {}
        DiaryElemTag( DiaryElemTag* t, Diary* d = nullptr )
        :   DiaryElement( d ? d : t->m_p2diary, t->m_name, t->m_status ), PropertyContainer( t ),
            m_date_created( Date::get_now() ), m_date_edited( m_date_created ) {}

        // DATE
        DateV                   get_date() const        { return m_date; }
        DateV                   get_date_finish() const
        { return( Date::is_set( m_date_finish ) ? m_date_finish : m_date ); }
        DateV                   get_date_finish_calc() const
        {
            return( ( get_todo_status() & ES::FLT_OPEN_OR_PRGR )
                            ? std::max( Date::get_now(), get_date_finish() )
                            : get_date_finish() );
        }
        void                    set_date_finish( DateV date )  { m_date_finish = date; }
        DateV                   get_date_created() const { return m_date_created; }
        DateV                   get_date_edited() const { return m_date_edited; }
        void                    set_date_edited( DateV d ) { m_date_edited.value = d; }
        inline thread_local static bool s_F_edit_dates_updateable { false };
        struct ContextDateEditability
        {
            public:
                ContextDateEditability( bool v ): prev( s_F_edit_dates_updateable )
                { s_F_edit_dates_updateable = v; }
                ~ContextDateEditability()
                { s_F_edit_dates_updateable = prev; }
            private:
                bool prev;
        };
        void                    update_date_edited()
        {
            if( s_F_edit_dates_updateable )
                m_date_edited.value = Date::get_now();
    }
        Ustring                 get_date_created_str() const
        { return Date::format_string_adv( m_date_created, "F,  h:m" ); }
        Ustring                 get_date_edited_str() const
        { return Date::format_string_adv( m_date_edited, "F,  h:m" ); }

        //HIERARCHY
        Ustring                 get_title_ancestral() const;
        virtual Ustring         get_ancestry_path() const = 0;
        virtual Ustring         get_description() const = 0;

        virtual DiaryElemTag*   get_sibling_tag_prev( bool = false ) = 0;
        virtual DiaryElemTag*   get_sibling_tag_next( bool = false ) = 0;
        virtual int             get_sibling_order() const = 0;
        virtual DiaryElemTag*   get_sibling_tag_1st() = 0;
        virtual DiaryElemTag*   get_sibling_tag_last() = 0;
        virtual VecTags         get_descendant_tags() const = 0;
        virtual bool            is_descendant_of( const DiaryElemTag* ) const = 0;

        virtual ListTags        get_sub_tags( const DiaryElemTag* ) const = 0;
        virtual DiaryElemTag*   get_parent_tag() const = 0;


        int
        get_duration() const
        { return Date::calculate_days_between( m_date, get_date_finish_calc() ); }

        // TODO STATUS
        virtual ElemStatus      get_todo_status() const
        {
            return( m_status & ES::FILTER_TODO );
        }
        ElemStatus              get_todo_status_effective() const
        {
            const ElemStatus s{ m_status & ES::FILTER_TODO_PURE };
            return( s ? s : ES::NOT_TODO );
        }
        String                  get_todo_status_as_text() const
        {
            switch( get_todo_status() )
            {
                case ES::TODO:        return "[ ] ";
                case ES::PROGRESSED:  return "[~] ";
                case ES::DONE:        return "[+] ";
                case ES::CANCELED:    return "[X] ";
                default:              return "";
            }
        }
        D::CSTR                 get_todo_status_id() const;
        void                    set_todo_status( ElemStatus );

        // EXPANSION
        virtual bool            is_expanded() const
        { return( m_status & ES::EXPANDED ); }
        virtual void            set_expanded( bool flag_expanded )
        { set_status_flag( ES::EXPANDED, flag_expanded ); }
        void                    toggle_expanded()
        { set_expanded( !is_expanded() ); }

        // UNITS
        String                  get_unit() const
        { return m_properties.get< String >( PROP::UNIT, "" ); }
        void                    set_unit( String unit )
        {
            if( unit.empty() )
                m_properties.remove( PROP::UNIT );
            else
                m_properties.set( PROP::UNIT, unit );
        }

        // COLOR
        bool                    has_color() const
        { return m_properties.has( PROP::COLOR ); }
        String                  get_color( const String& def_color = "" ) const
        { return m_properties.get< String >( PROP::COLOR, def_color ); }
        String                  get_color_no_fail() const
        { return m_properties.get< String >( PROP::COLOR, "#FFFFFF" ); }
        void                    set_color( const String& color )
        { m_properties.set( PROP::COLOR, color ); }

        // LANGUAGE
        String                  get_lang() const
        { return m_properties.get< String >( PROP::LANGUAGE, "" ); }
        void                    set_lang( const String& lang )
        {
            if( lang.empty() )
                m_properties.remove( PROP::LANGUAGE );
            else
                m_properties.set( PROP::LANGUAGE, lang );
        }
        virtual String          get_lang_final() const = 0;

        // REFERENCES
        const SetTagsByNameID&
        get_references() const { return m_referring_elems; }

        int
        get_reference_count() const { return int( m_referring_elems.size() ); }
        //void                    update_reference_count();

        void
        clear_references() { m_referring_elems.clear(); }

        void
        add_referring_elem( DiaryElemTag* elem ) { m_referring_elems.insert( elem ); }
        void
        remove_referring_elem( DiaryElemTag* elem )
        {
            // checking existence is crucial for the cases when there are multiple instances of
            // the same tag and removal has already been carried out before
            if( m_referring_elems.find( elem ) != m_referring_elems.end() )
                m_referring_elems.erase( elem );
        }

        DateV                   m_date          { Date::NOT_SET }; // is always set for entries
        DateV                   m_date_finish   { Date::NOT_SET }; // is always set for entries
        DateV                   m_date_created;
        Restricted< DateV, DiaryElemTag >
                                m_date_edited;
    protected:
        SetTagsByNameID         m_referring_elems;
};

// TYPEDEFS
using MapUstringUstring         = std::map< Ustring, Ustring, FuncCmpStrings >;
using ListDiaryElems            = std::list< DiaryElement* >;
using MapLoGIDTag               = std::unordered_map< LoGID, DiaryElemTag*, LoGIDHasher >;
using VecDiaryElems             = std::vector< DiaryElement* >;
using ListEntries               = std::list< Entry* >;
using VecEntries                = std::vector< Entry* >;
using VecEntriesIter            = std::vector< Entry* >::iterator;
using VecConstEntries           = std::vector< const Entry* >;
using PoolLoGIDs                = std::unordered_map< D::DEID, DiaryElement*, LoGIDHasher >;

using SignalVoidLoGID           = sigc::signal< void( LoGID ) >;
using SignalVoidElem            = sigc::signal< void( LoG::DiaryElement* ) >;
using SignalVoidTag             = sigc::signal< void( LoG::DiaryElemTag* ) >;
using SignalVoidElemstatus      = sigc::signal< void( LoG::ElemStatus ) >;
using FuncVoidElem              = std::function< void( DiaryElement* ) >;

template< class T >
class MapUstringDiaryElem : public std::map< Ustring, T*, FuncCmpStrings >
{
public:
    ~MapUstringDiaryElem() { clear(); }

    void
    clear()
    {
        for( auto kv : *this )
            delete kv.second;

        std::map< Ustring, T*, FuncCmpStrings >::clear();
    }
};

template< class T >
Ustring
create_unique_name_for_map( const std::map< Ustring, T, FuncCmpStrings >& map,
                            const Ustring& name0 )
{
    Ustring name = name0;
    for( int i = 1; map.find( name ) != map.end(); i++ )
    {
        name = STR::compose( name0, " ", i );
    }

    return name;
}
template< class T >
Ustring
create_unique_name_for_map( const std::multimap< Ustring, T >& map, const Ustring& name0 )
{
    Ustring name = name0;
    for( int i = 1; map.find( name ) != map.end(); i++ )
    {
        name = STR::compose( name0, " ", i );
    }

    return name;
}

// STRING DEFINITION ELEMENT =======================================================================
class StringDefElem : public DiaryElement
{
    public:
        StringDefElem( Diary* const diary, const Ustring& name, const Ustring& definition )
        : DiaryElement( diary, name ), m_definition( definition ) {}

        int                     get_size() const override
        { return 0; }
        const Ustring&          get_definition() const
        { return m_definition; }
        void                    set_definition( const Ustring& definition )
        { m_definition = definition; }

        void                    add_definition_line( const Ustring& line )
        {
            if( not( m_definition.empty() ) ) m_definition += '\n';
            m_definition += line;
        }

        virtual SKVVec          get_as_skvvec() const override
        {
            SKVVec sv;
            sv.push_back( { CSTR::TYPE_NAME,    get_type_name() } );
            sv.push_back( { CSTR::NAME,         get_name() } );
            sv.push_back( { CSTR::DEFINITION,   m_definition } );

            return sv;
        }

    protected:
        Ustring                 m_definition;

};

// TAGGING =========================================================================================
struct NameAndValue
{
    static constexpr int HAS_NAME = 0x1;
    static constexpr int HAS_VALUE = 0x2;
    static constexpr int HAS_UNIT = 0x4;
    static constexpr int HAS_EQUAL = 0x10;

    NameAndValue() {}
    NameAndValue( const Ustring& n, const Value& v ) : name( n ), value( v ) {}

    Ustring name{ "" };
    Value value{ 0.0 };
    Ustring unit{ "" };
    int status{ 0 };

    static NameAndValue parse( const Ustring& );
};

class MapTags : public std::unordered_map< D::DEID, Value, LoGIDHasher >
{
    public:
        Value                   get_value_for_tag( D::DEID id ) const
        {
            auto&& kv{ find( id ) };
            if( kv == end() )
                return 0;
            else
                return kv->second;
        }

    protected:

};

// COORDS ==========================================================================================
struct Coords : public PropValueObj
{
    static constexpr double UNSET{ Constants::INFINITY_PLS };

    Coords() : latitude( UNSET ), longitude( UNSET ) {}
    Coords( double lat, double lon ) : latitude( lat ), longitude( lon ) {}
    double latitude;
    double longitude;
    Ustring to_string() const { return STR::compose( latitude, ", ", longitude ); }
    bool is_set() const { return( latitude != UNSET ); }
    void unset(){ latitude = UNSET; longitude = UNSET; }
    bool is_equal_to( const Coords& pt2 ) const
    { return( pt2.latitude == latitude && pt2.longitude == longitude ); }

    static double get_distance( const Coords& p1, const Coords& p2 );
};

struct FuncCmpCoords
{
    bool operator()( const Coords& l, const Coords& r ) const
    { return( ( l.latitude * 1000.0 + l.longitude ) < ( r.latitude * 1000.0 + r.longitude ) ); }
};

using ListLocations = std::list< Paragraph* >;

// THEME ===========================================================================================
class Theme : public DiaryElement
{
    public:
#ifndef __ANDROID__
        using GValue = Glib::Value< const LoG::Theme* >;
#endif

                                    Theme( Diary* const, const Ustring&, const Ustring& = "" );
                                    Theme( Diary* const, const Ustring&,
                                           const Ustring&,
                                           const std::string&,
                                           const std::string&,
                                           const std::string&,
                                           const std::string&,
                                           const std::string& );
        // duplicates an existing theme, works like a copy constructor
                                    Theme( Diary* const, const Ustring&, const Theme* );
        virtual                     ~Theme() {}

        DiaryElement::Type          get_type() const override
        { return ET_THEME; }
        int                         get_size() const override
        { return 0; }   // redundant

        virtual bool                is_system() const
        { return false; }

        SKVVec                      get_as_skvvec() const override
        {
            SKVVec sv;
            sv.push_back( { CSTR::TYPE_NAME, get_type_name() } );
            sv.push_back( { CSTR::NAME, get_name() } );
            sv.push_back( { CSTR::THEME_FONT, font.to_string() } );
            sv.push_back( { CSTR::BACKGROUND_IMAGE, image_bg } );
            sv.push_back( { CSTR::THEME_BASE,
                            convert_gdkrgba_to_html( color_base ) } );
            sv.push_back( { CSTR::THEME_BASE,
                            convert_gdkrgba_to_html( color_base2 ) } );
            sv.push_back( { CSTR::THEME_TEXT,
                            convert_gdkrgba_to_html( color_text ) } );
            sv.push_back( { CSTR::THEME_HEADING,
                            convert_gdkrgba_to_html( color_heading ) } );
            sv.push_back( { CSTR::THEME_SUBHEADING,
                            convert_gdkrgba_to_html( color_subheading ) } );
            sv.push_back( { CSTR::THEME_HIGHLIGHT,
                            convert_gdkrgba_to_html( color_highlight ) } );

            return sv;
        }

        void                        copy_to( Theme* ) const;

        void                        calculate_derived_colors();

        String                      get_css_class_name() const
        { return( "t" + get_id_full().get_str() ); }
        String                      get_css_class_def() const;

        bool                        has_font_literary() const
        { return bool( font_literary.get_set_fields() & Pango::FontMask::FAMILY ); }
        bool                        has_font_monospace() const
        { return bool( font_monospace.get_set_fields() & Pango::FontMask::FAMILY ); }

        Pango::FontDescription      font;
        Pango::FontDescription      font_literary;
        Pango::FontDescription      font_monospace;
        String                      image_bg;

        // USER DEFINED COLORS
        Color                       color_base;
        Color                       color_base2; // for gradient
        Color                       color_text;
        Color                       color_heading;
        Color                       color_subheading;
        Color                       color_highlight;

        // DERIVED COLORS
        Color                       color_subsubheading;
        Color                       color_inline_tag;
        Color                       color_mid_dark;
        Color                       color_mid;
        Color                       color_pale;
        Color                       color_region_bg;
        Color                       color_match_bg;
        Color                       color_link;
        Color                       color_link_broken;

        Color                       color_open;
        Color                       color_open_bg;
        Color                       color_done;
        Color                       color_done_text;   // for the text rather than checkbox
        Color                       color_done_bg;
        Color                       color_canceled;
        Color                       color_canceled_bg;

        // CONSTANT COLORS
        static const Color          s_color_match1;
        static const Color          s_color_match2;
        static const Color          s_color_link1;
        static const Color          s_color_link2;
        static const Color          s_color_broken1;
        static const Color          s_color_broken2;

        static const Color          s_color_todo;
        static const Color          s_color_done;
        static const Color          s_color_canceled;
};

class ThemeSystem : public Theme
{
    public:
        static ThemeSystem*         get();
        bool                        is_system() const
        { return true; }

    protected:
                                    ThemeSystem( const Ustring&,
                                                 const std::string&,
                                                 const std::string&,
                                                 const std::string&,
                                                 const std::string&,
                                                 const std::string& );
};

using PoolThemes            = MapUstringDiaryElem< Theme >;
using PoolThemesIter        = PoolThemes::iterator;

// CHARTPOINTS =====================================================================================
// TODO: don't forget to update when VT::SRC_ITSELF changes:
// TODO: 3.2: add localization
static const char           TABLE_DEF_ALL_ENTRIES[] =
R"(McnDate
McoDS_~~~~~_S
Mcd_
McnName
McoX__~~~~~_S
Mcx____P~~~~~~~~~T
McnSize
McoZ__~~~~~_S
Mo0E)";

static const char           TABLE_DEF_ALL_PARAGRAPHS[] =
R"(McnDate
McoDS_~~~~~_S
Mcd_
McnName
McoX__~~~~~_S
Mcx____P~~~~~~~~~T
McnSize
McoZ__~~~~~_S
Mo0P)";

class ChartData
{
    public:
        typedef std::pair< float, Color > PairMilestone;

        struct YValues
        {
            Value v;
            Value u;
            int c;
            SetTagsByNameID elems;

            void add_cumulative( Value vn, Value un, DiaryElemTag* t )
            {
                v += vn;
                u += un;
                c++;
                elems.insert( t );
            }
            void add_average( Value vn, Value un, DiaryElemTag* t )
            {
                v = ( v * c + vn ) / ( c + 1 );
                u = ( u * c + un ) / ( c + 1 );
                c++;
                elems.insert( t );
            }
        };

        // NOTE: change in these values will break compatibility with older diary versions
        static constexpr int    DEL_PERIOD_MONTHLY      = 0x1;  // old value
        static constexpr int    DEL_PERIOD_YEARLY       = 0x2;  // old value
        static constexpr int    DEL_PERIOD_WEEKLY       = 0x3;  // old value
        static constexpr int    PERIOD_DAILY            = 0x1;
        static constexpr int    PERIOD_WEEKLY           = 0x2;
        static constexpr int    PERIOD_MONTHLY          = 0x3;
        static constexpr int    PERIOD_YEARLY           = 0x4;
        static constexpr int    PERIOD_MASK             = 0xF;

        static constexpr int    DEL_BOOLEAN             = 0x10; // not used any more
        static constexpr int    DEL_CUMULATIVE_PERIODIC = 0x20; // not used any more
        static constexpr int    DEL_AVERAGE             = 0x30; // not used any more
        static constexpr int    DEL_CUMULATIVE_CONT     = 0x40; // not used any more
        static constexpr int    COMBINE_CUMULATIVE_PERIODIC     = 0x10; // also the old cumulative
        static constexpr int    COMBINE_CUMULATIVE_CONTINUOUS   = 0x20;
        static constexpr int    COMBINE_AVERAGE                 = 0x30;
        static constexpr int    COMBINE_MASK                    = 0xF0;
        static constexpr int    COMBINE_SHIFT                   = 4;

        static constexpr int    UNDERLAY_PREV_YEAR      = 0x100;
        static constexpr int    UNDERLAY_PLANNED        = 0x200;
        static constexpr int    UNDERLAY_MASK           = 0x300;
        static constexpr int    UNDERLAY_NONE           = 0x300; // same as mask to save bits

        static constexpr int    STYLE_LINE              = 0x1000;
        static constexpr int    STYLE_BARS              = 0x2000;
        static constexpr int    STYLE_PIE               = 0x3000;
        static constexpr int    STYLE_MASK              = 0xF000;

        static constexpr int    TYPE_NUMBER             = 0x10000;
        static constexpr int    TYPE_STRING             = 0x20000;
        static constexpr int    TYPE_DATE               = 0x30000;
        static constexpr int    TYPE_MASK               = 0xF0000;

        static constexpr int    COLUMN_NONE             = 0x100000; // auto column NONE
        static constexpr int    COLUMN_COUNT            = 0x100000; // auto column to count
        static constexpr int    COLUMN_PREV_YEAR        = 0x200000; // auto column to underlay

        static constexpr int    PROPERTIES_DEFAULT      = STYLE_LINE|COMBINE_CUMULATIVE_CONTINUOUS|
                                                          PERIOD_MONTHLY;

        ChartData( Diary* d, int t = PROPERTIES_DEFAULT );
        ~ChartData();

        void                        clear();

        void                        set_diary( Diary* );

        String                      get_as_string() const;
        void                        set_from_string( const Ustring& );
        void                        set_from_string_old( const Ustring& );

        void                        set_properties( int );

        void                        set_table( const D::DEID& );
        void                        refresh_table();

        void                        refresh_type();
        void                        refresh_unit();

        unsigned int                calculate_distance( const DateV, const DateV ) const;

        void                        forward_date( DateV& ) const;

        void                        clear_points();
        void                        calculate_points();

        void                        add_value_date( DateV, const Value, const Value,
                                                    DiaryElemTag* );
        void                        add_value_num( const Value, const Value, const Value,
                                                   DiaryElemTag* );
        void                        add_value_str( const Ustring&, const Value, const Value,
                                                   DiaryElemTag* );
        void                        fill_in_intermediate_date_values();
        void                        fill_in_date_underlays();

        int                         get_type() const
        { return( m_properties & TYPE_MASK ); }
        int                         get_style() const
        { return( m_properties & STYLE_MASK ); }
        int                         get_period() const
        { return( m_properties & PERIOD_MASK ); }
        int                         get_combining() const
        { return( m_properties & COMBINE_MASK ); }
        bool                        is_monthly() const
        { return( ( m_properties & PERIOD_MASK ) == PERIOD_MONTHLY ); }
        bool                        is_average() const
        { return( ( m_properties & COMBINE_MASK ) == COMBINE_AVERAGE ); }
        int                         get_y_axis() const
        { return m_tcidy; }

        Value                       v_min       { Constants::INFINITY_PLS };
        Value                       v_max       { Constants::INFINITY_MNS };

        std::map< DateV, YValues >  values_date;
        std::map< Ustring, int >    values_str2index;
        std::map< int, Ustring >    values_index2str;
        std::map< int, YValues >    values_str;
        std::map< Value, YValues >  values_num;
        unsigned int                m_span      { 0 };

        Ustring                     m_unit;
        int                         m_properties;

        D::DEID                     m_table_id  { DEID::TABLE_ID_ALL_ENTRIES };
        TableData*                  m_td;
        int                         m_tcidx     { COLUMN_NONE };   // table column id x-axis
        int                         m_tcidy     { COLUMN_COUNT };  // table column id y-axis
        int                         m_tcidu     { COLUMN_NONE };   // table column id y-underlay
        int                         m_tcidf     { COLUMN_NONE };   // table column id filter
        Ustring                     m_filter_v;

    protected:
        void                        update_span();
        void                        update_min_max();

        template< class T >
        void update_min_max( T& values )
        {
            for( auto& kv : values )
            {
                const auto vmin_new { m_tcidu < COLUMN_NONE ? std::min( kv.second.v, kv.second.u )
                                                            : kv.second.v };
                const auto vmax_new { m_tcidu < COLUMN_NONE ? std::max( kv.second.v, kv.second.u )
                                                            : kv.second.v };
                if( vmin_new  < v_min )
                    v_min = vmin_new;
                if( vmax_new > v_max )
                    v_max = vmax_new;
            }
        }

        Diary*                      m_p2diary;

    friend class Chart;
    friend class WidgetChart;
};

class ChartElem : public StringDefElem
{
    public:
#ifndef __ANDROID__
        using GValue = Glib::Value< ChartElem* >;
#endif

        static const Ustring DEFINITION_DEFAULT;
        static const Ustring DEFINITION_DEFAULT_Y; // temporary: needed for diary v1050 support

        ChartElem( Diary* const diary, const Ustring& name, const Ustring& definition )
        : StringDefElem( diary, name, definition ) {}

        DiaryElement::Type      get_type() const override
        { return ET_CHART; }

        const R2Pixbuf&         get_icon() const override;
};

using MapUstringChartElem = MapUstringDiaryElem< ChartElem >;

} // end of namespace LoG

#endif
