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


#include "../helpers.hpp"
#include "diarydata.hpp"
#include "diary.hpp"
#include "tableelem.hpp"
#include "../lifeograph.hpp"


namespace LoG
{

// CONSTANTS =======================================================================================
REG_PROP_GETTERS( VT::SO::PROP_0 )
REG_PROP_GETTERS( VT::SO::PROP_1 )

REG_PROP_GETTERS( VT::TVTC::PROP_0 )
REG_PROP_GETTERS( VT::TVTC::PROP_1 )

REG_PROP_GETTERS( VT::TVTS::PROP_0 )
REG_PROP_GETTERS( VT::TVTS::PROP_1 )
REG_PROP_GETTERS( VT::TVTS::PROP_2 )

REG_PROP_GETTERS( VT::TVT::PROP_0 )
REG_PROP_GETTERS( VT::TVT::PROP_1 )
REG_PROP_GETTERS( VT::TVT::PROP_2 )
REG_PROP_GETTERS( VT::TVT::PROP_3 )
REG_PROP_GETTERS( VT::TVT::PROP_4 )
REG_PROP_GETTERS( VT::TVT::PROP_5 )
REG_PROP_GETTERS( VT::TVT::PROP_6 )

REG_PROP_GETTERS( VT::SAS::PROP_0 )
REG_PROP_GETTERS( VT::SAS::PROP_1 )
REG_PROP_GETTERS( VT::SAS::PROP_2 )

REG_PROP_GETTERS( VT::SUMF::PROP_0 )
REG_PROP_GETTERS( VT::SUMF::PROP_1 )
REG_PROP_GETTERS( VT::SUMF::PROP_2 )
REG_PROP_GETTERS( VT::SUMF::PROP_3 )
REG_PROP_GETTERS( VT::SUMF::PROP_4 )
REG_PROP_GETTERS( VT::SUMF::PROP_5 )
REG_PROP_GETTERS( VT::SUMF::PROP_6 )

REG_PROP_GETTERS( VT::OP_DEPTH::PROP_0 )
REG_PROP_GETTERS( VT::OP_DEPTH::PROP_1 )
REG_PROP_GETTERS( VT::OP_DEPTH::PROP_2 )

REG_PROP_GETTERS( VT::ETS::PROP_0 )
REG_PROP_GETTERS( VT::ETS::PROP_1 )
REG_PROP_GETTERS( VT::ETS::PROP_2 )
REG_PROP_GETTERS( VT::ETS::PROP_3 )
REG_PROP_GETTERS( VT::ETS::PROP_4 )
REG_PROP_GETTERS( VT::ETS::PROP_5 )

REG_PROP_GETTERS( VT::CS::PROP_0 )
REG_PROP_GETTERS( VT::CS::PROP_1 )
REG_PROP_GETTERS( VT::CS::PROP_2 )

REG_PROP_GETTERS( VT::DT::PROP_0 )
REG_PROP_GETTERS( VT::DT::PROP_1 )
REG_PROP_GETTERS( VT::DT::PROP_2 )
REG_PROP_GETTERS( VT::DT::PROP_3 )

REG_PROP_GETTERS( VT::PHS::PROP_0 )
REG_PROP_GETTERS( VT::PHS::PROP_1 )
REG_PROP_GETTERS( VT::PHS::PROP_2 )
REG_PROP_GETTERS( VT::PHS::PROP_3 )

REG_PROP_GETTERS( VT::PLS::PROP_0 )
REG_PROP_GETTERS( VT::PLS::PROP_1 )
REG_PROP_GETTERS( VT::PLS::PROP_2 )
REG_PROP_GETTERS( VT::PLS::PROP_3 )
REG_PROP_GETTERS( VT::PLS::PROP_4 )
REG_PROP_GETTERS( VT::PLS::PROP_5 )
REG_PROP_GETTERS( VT::PLS::PROP_6 )
REG_PROP_GETTERS( VT::PLS::PROP_7 )
REG_PROP_GETTERS( VT::PLS::PROP_8 )
REG_PROP_GETTERS( VT::PLS::PROP_9 )
REG_PROP_GETTERS( VT::PLS::PROP_10 )

REG_PROP_GETTERS( VT::PA::PROP_0 )
REG_PROP_GETTERS( VT::PA::PROP_1 )
REG_PROP_GETTERS( VT::PA::PROP_2 )

REG_PROP_GETTERS( VT::TCAo::PROP_0 )
REG_PROP_GETTERS( VT::TCAo::PROP_1 )
REG_PROP_GETTERS( VT::TCAo::PROP_2 )
REG_PROP_GETTERS( VT::TCAo::PROP_3 )
REG_PROP_GETTERS( VT::TCAo::PROP_4 )
REG_PROP_GETTERS( VT::TCAo::PROP_5 )
REG_PROP_GETTERS( VT::TCAo::PROP_6 )

REG_PROP_GETTERS( VT::TCAf::PROP_0 )
REG_PROP_GETTERS( VT::TCAf::PROP_1 )
REG_PROP_GETTERS( VT::TCAf::PROP_2 )
REG_PROP_GETTERS( VT::TCAf::PROP_3 )

REG_PROP_GETTERS( VT::TCAu::PROP_0 )
REG_PROP_GETTERS( VT::TCAu::PROP_1 )
REG_PROP_GETTERS( VT::TCAu::PROP_2 )
REG_PROP_GETTERS( VT::TCAu::PROP_3 )

REG_PROP_GETTERS( VT::FMT::PROP_0 )
REG_PROP_GETTERS( VT::FMT::PROP_1 )
REG_PROP_GETTERS( VT::FMT::PROP_2 )
REG_PROP_GETTERS( VT::FMT::PROP_3 )
REG_PROP_GETTERS( VT::FMT::PROP_4 )
REG_PROP_GETTERS( VT::FMT::PROP_5 )
REG_PROP_GETTERS( VT::FMT::PROP_6 )
REG_PROP_GETTERS( VT::FMT::PROP_7 )
REG_PROP_GETTERS( VT::FMT::PROP_8 )
REG_PROP_GETTERS( VT::FMT::PROP_9 )
REG_PROP_GETTERS( VT::FMT::PROP_10 )
REG_PROP_GETTERS( VT::FMT::PROP_11 )
REG_PROP_GETTERS( VT::FMT::PROP_12 )
REG_PROP_GETTERS( VT::FMT::PROP_13 )
REG_PROP_GETTERS( VT::FMT::PROP_14 )
REG_PROP_GETTERS( VT::FMT::PROP_15 )
REG_PROP_GETTERS( VT::FMT::PROP_16 )
REG_PROP_GETTERS( VT::FMT::PROP_17 )
REG_PROP_GETTERS( VT::FMT::PROP_18 )
REG_PROP_GETTERS( VT::FMT::PROP_19 )
REG_PROP_GETTERS( VT::FMT::PROP_20 )

REG_PROP_GETTERS( VT::QT::PROP_0 )
REG_PROP_GETTERS( VT::QT::PROP_1 )
REG_PROP_GETTERS( VT::QT::PROP_2 )
REG_PROP_GETTERS( VT::QT::PROP_3 )
REG_PROP_GETTERS( VT::QT::PROP_4 )
REG_PROP_GETTERS( VT::QT::PROP_5 )
REG_PROP_GETTERS( VT::QT::PROP_6 )
REG_PROP_GETTERS( VT::QT::PROP_7 )
REG_PROP_GETTERS( VT::QT::PROP_8 )
REG_PROP_GETTERS( VT::QT::PROP_9 )
REG_PROP_GETTERS( VT::QT::PROP_10 )
REG_PROP_GETTERS( VT::QT::PROP_11 )
REG_PROP_GETTERS( VT::QT::PROP_12 )
REG_PROP_GETTERS( VT::QT::PROP_13 )
REG_PROP_GETTERS( VT::QT::PROP_14 )
REG_PROP_GETTERS( VT::QT::PROP_15 )
REG_PROP_GETTERS( VT::QT::PROP_16 )
REG_PROP_GETTERS( VT::QT::PROP_17 )
REG_PROP_GETTERS( VT::QT::PROP_18 )
REG_PROP_GETTERS( VT::QT::PROP_19 )
REG_PROP_GETTERS( VT::QT::PROP_20 )

// DIARYELEMENT ====================================================================================
// STATIC MEMBERS
const R2Pixbuf                      DiaryElement::s_pixbuf_null;
const Ustring                       DiaryElement::s_type_names[] =
{
    "", "Chapter Ctg (not used anymore)", _( "Theme" ), _( "Filter" ), _( "Chart" ), _( "Table" ),
    _( "Paragraph" ), _( "Diary" ), "Multiple Entries", _( "Entry" ), "Chapter (not used anymore)",
    "Header"
};

DiaryElement::DiaryElement() : Named( "" ), m_status( ES::EXPANDED ) {}

DiaryElement::DiaryElement( Diary* const ptr2diary,
                            const Ustring& name,
                            ElemStatus status )
:   Named( name ), m_p2diary( ptr2diary ),
    m_status( status ),
    m_id( ptr2diary ? ptr2diary->create_new_id( this ) : DEID::UNSET )
{
}

DiaryElement::~DiaryElement()
{
    // only remove if it is the right copy (e.g. not undo storage copy):
    if( m_p2diary != nullptr && m_p2diary->get_element( m_id ) == this )
        m_p2diary->erase_id( m_id );
}

D::DEIDF
DiaryElement::get_id_full() const  // Paragaph reimplements this
{ return D::DEIDF( m_id, m_p2diary ? m_p2diary->get_id() : DEID::UNSET ); }

// DIARYELEMENT DATA SOURCE (Entries and Paragraphs) ===============================================
Ustring
DiaryElemTag::get_title_ancestral() const
{
    return( "<small>" + Glib::Markup::escape_text( get_ancestry_path() )
                      + "</small><b>" + Glib::Markup::escape_text( get_name() )
                      + "</b>" );
}

void
DiaryElemTag::set_todo_status( ElemStatus s )
{
    m_status -= ( m_status & ES::FILTER_TODO );
    m_status |= s;
}

D::CSTR
DiaryElemTag::get_todo_status_id() const
{
    switch( get_todo_status() )
    {
        case ES::TODO:
            return CSTR::TODO;
        case ES::PROGRESSED:
            return CSTR::PROGRESSED;
        case ES::DONE:
            return CSTR::DONE;
        case ES::CANCELED:
            return CSTR::CANCELED;
        case ( ES::NOT_TODO | ES::TODO ):
        case ( ES::NOT_TODO | ES::PROGRESSED ):
        case ( ES::NOT_TODO | ES::DONE ):
        case ( ES::NOT_TODO | ES::CANCELED ):
            return CSTR::AUTO;
        case ES::NOT_TODO:
        default:    // should never be the case
            return CSTR::_VOID_;
    }
}

// COORDS ==========================================================================================
double
Coords::get_distance( const Coords& p1, const Coords& p2 )
{
#if __GNUC__ > 9
    const static double D{ 6371 * 2 } ;      // mean diameter of Earth in kilometers
    const static double to_rad{ HELPERS::PI/180 }; // degree to radian conversion multiplier
    const double φ1{ p1.latitude * to_rad };  // in radians
    const double φ2{ p2.latitude * to_rad };  // in radians
    const double Δφ{ φ2 - φ1 };
    const double Δλ{ ( p2.longitude - p1.longitude ) * to_rad };

    const double a = pow( sin( Δφ / 2 ), 2 ) + cos( φ1 ) * cos( φ2 ) * pow( sin( Δλ / 2 ), 2 );

    return( D * atan2( sqrt( a ), sqrt( 1 - a ) ) );
    // per Wikipedia article about haversine formula, asin( sqrt( a ) ) should also work
#else // Unicode identifier support was added in GCC 10!
    const static double D{ 6371 * 2 } ;
    const static double to_rad{ HELPERS::PI/180 };
    const double phi1{ p1.latitude * to_rad };
    const double phi2{ p2.latitude * to_rad };
    const double dp{ phi2 - phi1 };
    const double dl{ ( p2.longitude - p1.longitude ) * to_rad };

    const double a = pow( sin( dp / 2 ), 2 ) + cos( phi1 ) * cos( phi2 ) * pow( sin( dl / 2 ), 2 );

    return( D * atan2( sqrt( a ), sqrt( 1 - a ) ) );
#endif
}

// NAME AND VALUE ==================================================================================
NameAndValue
NameAndValue::parse( const Ustring& text )
{
    NameAndValue nav;
    char lf{ '=' }; // =, \, #, $(unit)
    int divider{ 0 };
    int trim_length{ 0 };
    int trim_length_unit{ 0 };
    bool negative{ false };
    Wchar c;

    for( Ustring::size_type i = 0; i < text.size(); i++ )
    {
        c = text.at( i );
        switch( c )
        {
            case '\\':
                if( lf == '#' || lf == '$' )
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                    lf = '$';
                }
                else if( lf == '\\' )
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '=';
                }
                else // i.e. ( lf == '=' )
                    lf = '\\';
                break;
            case '=':
                if( nav.name.empty() || lf == '\\' )
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '=';
                }
                else if( lf == '#' || lf == '$' )
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                    lf = '$';
                }
                else // i.e. ( lf == '=' )
                {
                    nav.status |= NameAndValue::HAS_EQUAL;
                    lf = '#';
                }
                break;
            case ' ':
            case '\t':
                // if( lf == '#' ) just ignore
                if( lf == '=' || lf == '\\' )
                {
                    if( !nav.name.empty() ) // else ignore
                    {
                        nav.name += c;
                        trim_length++;
                    }
                }
                else if( lf == '$' )
                {
                    nav.unit += c;
                    trim_length_unit++;
                }
                break;
            case ',':
            case '.':
                if( divider || lf == '$' ) // note that if divider, lf must be #
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                    lf = '$';
                }
                else if( lf == '#' )
                    divider = 1;
                else
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '=';
                }
                break;
            case '-':
                if( negative || lf == '$' ) // note that if negative, lf must be #
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                    lf = '$';
                }
                else if( lf == '#' )
                    negative = true;
                else
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '=';
                }
                break;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                if( lf == '#' )
                {
                    nav.status |= NameAndValue::HAS_VALUE;
                    nav.value *= 10;
                    nav.value += ( c - '0' );
                    if( divider )
                        divider *= 10;
                }
                else if( lf == '$' )
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                }
                else
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '='; // reset ( lf == \ ) case
                }
                break;
            default:
                if( lf == '#' || lf == '$' )
                {
                    nav.unit += c;
                    trim_length_unit = 0;
                    lf = '$';
                }
                else
                {
                    nav.name += c;
                    trim_length = 0;
                    lf = '=';
                }
                break;
        }
    }

    if( lf == '$' )
        nav.status |= ( NameAndValue::HAS_NAME | NameAndValue::HAS_UNIT );
    else if( ! nav.name.empty() )
        nav.status |= NameAndValue::HAS_NAME;

    if( trim_length )
        nav.name.erase( nav.name.size() - trim_length, trim_length );
    if( trim_length_unit )
        nav.unit.erase( nav.unit.size() - trim_length_unit, trim_length_unit );

    if( lf == '=' && ! nav.name.empty() ) // implicit boolean tag
        nav.value = 1;
    else
    {
        if( divider > 1 )
            nav.value /= divider;
        if( negative )
            nav.value *= -1;
    }

    PRINT_DEBUG( "tag parsed | name: ", nav.name, "; value: ", nav.value, "; unit: ", nav.unit );

    return nav;
}

// THEMES ==========================================================================================
// STATIC MEMBERS
const Color Theme::s_color_match1( "#33FF33" );
const Color Theme::s_color_match2( "#009900" );
const Color Theme::s_color_link1( "#6666FF" );
const Color Theme::s_color_link2( "#000099" );
const Color Theme::s_color_broken1( "#FF3333" );
const Color Theme::s_color_broken2( "#990000" );

const Color Theme::s_color_todo( "#FF0000" );
const Color Theme::s_color_done( "#66BB00" );
const Color Theme::s_color_canceled( "#AA8855" );

Theme::Theme( Diary* const d, const Ustring& name, const Ustring& )
// last Ustring is a dummy to mimic StringDefElem constructors
:   DiaryElement( d, name )
{
}

Theme::Theme( Diary* const d,
              const Ustring& name,
              const Ustring& str_font,
              const std::string& str_base,
              const std::string& str_text,
              const std::string& str_heading,
              const std::string& str_subheading,
              const std::string& str_highlight )
:   DiaryElement( d, name ),
    font( str_font ), color_base( str_base ), color_text( str_text ),
    color_title( str_heading ), color_heading_L( str_subheading ),
    color_highlight( str_highlight )
{
    calculate_derived_colors();
}

Theme::Theme( Diary* const d, const Ustring& name, const Theme* theme )
:   DiaryElement( d, name ),
    font( theme->font ),
    color_base( theme->color_base ),
    color_text( theme->color_text ),
    color_title( theme->color_title ),
    color_heading_L( theme->color_heading_L ),
    color_highlight( theme->color_highlight )
{
    calculate_derived_colors();
}

void
Theme::copy_to( Theme* target ) const
{
    target->font = font;
    target->font_literary = font_literary;
    target->font_monospace = font_monospace;
    target->image_bg = image_bg;
    target->color_base = color_base;
    target->color_base2 = color_base2;
    target->color_text = color_text;
    target->color_title = color_title;
    target->color_heading_L = color_heading_L;
    target->color_highlight = color_highlight;

    target->calculate_derived_colors();
}

void
Theme::calculate_derived_colors()
{
    color_heading_M =     midtone( color_text, color_heading_L, 0.5 );
    color_inline_tag =    midtone( color_base, color_highlight, 0.2 );
    color_mid_dark =      midtone( color_base, color_text, 0.65 );
    color_mid =           midtone( color_base, color_text, 0.42 );
    color_pale =          midtone( color_base, color_text, 0.28 );
    color_region_bg =     midtone( color_base, color_text, 0.1, 0.6 ); // alpha = 0.6
    color_match_bg =      contrast2( color_base, s_color_match1, s_color_match2 );
    color_link =          contrast2( color_base, s_color_link1, s_color_link2 );
    color_link_broken =   contrast2( color_base, s_color_broken1, s_color_broken2 );

    // TODO: 3.2: we may change the coefficients below depending on the difference between the...
    // ... contrasting colors using get_color_diff( Theme::s_color_done, theme->color_base )...
    // ... generally, when get_color_diff is < 1.0 contrast is not satisfactory
    color_open =          midtone( s_color_todo, color_text );
    // color_open_bg =       midtone( s_color_todo, color_base, 0.7 );

    color_done =          midtone( s_color_done, color_text, 0.7 );
    color_done_bg =       midtone( s_color_done, color_base, 0.7 );

    color_canceled =      midtone( s_color_canceled, color_text );
    color_canceled_bg =   midtone( s_color_canceled, color_base, 0.7 );
}

String
Theme::get_css_class_def() const
{
    const auto& name    { get_css_class_name() };
    const auto  size    { ( font.get_size_is_absolute()
                            ? font.get_size()
                            : ( font.get_size() / static_cast< double >( PANGO_SCALE ) ) ) * 4/3 };
                        // * 4/3 is for dpi adjustment (=96.0/72.0)
    const auto& c_text  { color_text.to_string() };
    const auto& c_base  { color_base.to_string() };
    const auto& font_f  { font.get_family().empty() ? "sans" : font.get_family() };
    const auto& i_bg    { image_bg.empty()
                          ? "background-image: none"
                          : ( image_bg == "#"
                              ? STR::compose( "background: linear-gradient(to bottom, ",
                                              c_base, ", ", color_base2.to_string(), ")" )
                              : STR::compose( "background-image: url(\"", image_bg, "\")" ) ) };

    return( STR::compose(
            "textview#", name, " { "
                "color: ",              c_text, "; "
                "font-family: ",        font_f, "; "
                "font-size: ",          size,   "px; "
                "caret-color: ",        c_text, "; }\n"
            "textview#", name, " > text selection { color: ", c_base, "; "
                                  "background: ", color_title.to_string(), "; }\n"
            "textview#", name, " > text:selected { color: ", c_base, "; "
                                  "background: ",  color_title.to_string(), "; }\n"
            "box#", name, " { background-color: ", c_base, "; "
                             "background-size: cover; background-repeat: no-repeat; ",
                              i_bg, "; }\n"
            "window#", name, " { background-color: ", c_base, "; "
                                "background-size: cover; background-repeat: no-repeat; ",
                                 i_bg, "; }\n"
            "button.", name, " { background-color: ", c_base, "; ", i_bg, "; }\n"
            "label#", name, " { color: ", c_text, "; background: transparent; }\n"
            "label#", name, ":backdrop { color: ", color_mid.to_string(), "; }\n"
            "image#", name, " { color: ", c_text, "; }\n"
            "image#", name, ":backdrop { color: ", color_mid.to_string(), "; }\n"
 ) );
    // noncustom specialization is to revert children of the main textview to defaults
}

// THEMESYSTEM =====================================================================================
ThemeSystem::ThemeSystem( const Ustring& f,
                          const std::string& cb,
                          const std::string& ct,
                          const std::string& ch,
                          const std::string& csh,
                          const std::string& chl )
:   Theme( nullptr, "Lifeograph", f, cb, ct, ch, csh, chl )
{
}

ThemeSystem*
ThemeSystem::get()
{
    static ThemeSystem *s_theme{ nullptr };

    if( s_theme == nullptr )
        s_theme = new ThemeSystem( "Sans 10", "#FFFFFF", "#000000", "#B72525", "#963F3F",
                                   "#FFBBBB" );

    return s_theme;
}

// CHARTS ==========================================================================================
ChartData::ChartData( Diary* d, int t )
: m_properties( t ), m_td( new TableData( d ) ), m_p2diary( d )
{ }

ChartData::~ChartData()
{
    delete m_td;
}

void
ChartData::clear()
{
    m_properties = 0;
    m_unit.clear();
    clear_points();
}

void
ChartData::set_diary( Diary* diary )
{
    m_p2diary = diary;
    m_td->set_diary( diary );
}

String
ChartData::get_as_string() const
{
    String chart_def;

    // table:
    chart_def += STR::compose( "Gt", m_table_id.get_raw(), '\n' );

    // columns:
    chart_def += STR::compose( "Gx", m_tcidx, '\n' );
    chart_def += STR::compose( "Gy", m_tcidy, '\n' );
    chart_def += STR::compose( "Gu", m_tcidu, '\n' );
    chart_def += STR::compose( "Gf", m_tcidf, '\n' );
    chart_def += STR::compose( "Gv", m_filter_v, '\n' );

    // options:
    chart_def += "Go_"; // _ was for TAGGED_ONLY but it is no longer in use

    switch( m_properties & STYLE_MASK )
    {
        default:                chart_def += 'L'; break;
        case STYLE_BARS:        chart_def += 'B'; break;
        case STYLE_PIE:         chart_def += 'P'; break;
    }
    switch( m_properties & PERIOD_MASK )
    {
        default:                chart_def += 'D'; break; // daily
        case PERIOD_WEEKLY:     chart_def += 'W'; break;
        case PERIOD_MONTHLY:    chart_def += 'M'; break;
        case PERIOD_YEARLY:     chart_def += 'Y'; break;
    }
    switch( m_properties & COMBINE_MASK )
    {
        default:                            chart_def += 'P'; break;    // cumulative periodic
        case COMBINE_CUMULATIVE_CONTINUOUS: chart_def += 'C'; break;
        case COMBINE_AVERAGE:               chart_def += 'A'; break;
    }

    return chart_def;
}

void
ChartData::set_from_string( const Ustring& chart_def )
{
    String      line;
    StringSize  line_offset{ 0 };

    clear();

    while( STR::get_line( chart_def, line_offset, line ) )
    {
        if( line.size() < 2 )   // should never occur
            continue;

        switch( line[ 1 ] )
        {
            case 't':   // table
                set_table( D::DEID( line.substr( 2 ) ) );
                break;
            case 'x':   // x-axis
                m_tcidx = std::stoul( line.substr( 2 ) );
                if( Diary::d->get_version() < 2018 && m_tcidx < TableData::COL_ID_MAX )
                    m_tcidx += TableData::COL_ID_MIN; // convert from index to id
                break;
            case 'y':   // y-axis
                m_tcidy = std::stoul( line.substr( 2 ) );
                if( Diary::d->get_version() < 2018 && m_tcidy < TableData::COL_ID_MAX )
                    m_tcidy += TableData::COL_ID_MIN; // convert from index to id
                break;
            case 'u':   // underlay
                m_tcidu = std::stoul( line.substr( 2 ) );
                if( Diary::d->get_version() < 2018 && m_tcidu < TableData::COL_ID_MAX )
                    m_tcidu += TableData::COL_ID_MIN; // convert from index to id
                break;
            case 'f':   // filter
                m_tcidf = std::stoul( line.substr( 2 ) );
                if( Diary::d->get_version() < 2018 && m_tcidf < TableData::COL_ID_MAX )
                    m_tcidf += TableData::COL_ID_MIN; // convert from index to id
                break;
            case 'v':   // filter value
                m_filter_v = line.substr( 2 );
                break;
            case 'o':
                switch( line[ 3 ] )
                {
                    case 'L': m_properties |= STYLE_LINE; break;
                    case 'B': m_properties |= STYLE_BARS; break;
                    case 'P': m_properties |= STYLE_PIE; break;
                }
                switch( line[ 4 ] )
                {
                    case 'D': m_properties |= PERIOD_DAILY; break;
                    case 'W': m_properties |= PERIOD_WEEKLY; break;
                    case 'M': m_properties |= PERIOD_MONTHLY; break;
                    case 'Y': m_properties |= PERIOD_YEARLY; break;
                }
                switch( line[ 5 ] )
                {
                    case 'P': m_properties |= COMBINE_CUMULATIVE_PERIODIC; break;
                    case 'C': m_properties |= COMBINE_CUMULATIVE_CONTINUOUS; break;
                    case 'A': m_properties |= COMBINE_AVERAGE; break;
                }
                break;
            default:
                PRINT_DEBUG( "Unrecognized chart string: ", line );
                break;
        }
    }

    refresh_type();
    refresh_unit();
}

void
ChartData::set_from_string_old( const Ustring& chart_def )
{
    // TODO: could have been better but it may not worth to effort to implement upgrading...
    // ...all sorts of older charts
    String      line;
    StringSize  line_offset{ 0 };

    m_properties = 0;
    m_table_id = DEID::TABLE_ID_ALL_ENTRIES;
    m_tcidx = TableData::COL_ID_MIN; // date column
    m_tcidy = COLUMN_COUNT;
    m_tcidu = COLUMN_NONE;

    while( STR::get_line( chart_def, line_offset, line ) )
    {
        if( line.size() < 2 )   // should never occur
            continue;

        switch( line[ 1 ] )
        {
            case 'y':   // y axis
            {
                switch( line[ 2 ] )
                {
                    // case 'c':   // count: alrready set
                    //     break;
                    case 'l':   // text length
                        m_tcidx = ( TableData::COL_ID_MIN + 2 ); // size column
                        break;
                    case 'm':   // map path length
                        break;
                    case 't':   // tag value <- hard to support
                        break;
                    case 'p':   // tag value for para <-hard to support
                        m_table_id = DEID::TABLE_ID_ALL_PARAGRAPHS;
                        break;
                }
                break;
            }
            //case 'p': // para filter tag DROPPED
            /*case 'f':   // filter
                if( Diary::d->is_old() )
                    filter_entry = m_p2diary->get_filter( line.substr( 2 ) );
                else
                {
                    if( line[ 2 ] == 'e' )
                        filter_entry = m_p2diary->get_filter( std::stoul( line.substr( 3 ) ) );
                    else
                    if( line[ 2 ] == 'p' )
                        filter_para = m_p2diary->get_filter( std::stoul( line.substr( 3 ) ) );
                }
                break;*/
            case 'o':
                switch( line[ 3 ] )
                {
                    case 'Y': m_tcidu = COLUMN_PREV_YEAR; break;
                    case 'P':  break; // planned - TODO: probably never....
                }
                switch( line[ 4 ] )
                {
                    case 'W': m_properties |= PERIOD_WEEKLY; break;
                    case 'M': m_properties |= PERIOD_MONTHLY; break;
                    case 'Y': m_properties |= PERIOD_YEARLY; break;
                }
                switch( line[ 5 ] )
                {
                    case 'P': m_properties |= COMBINE_CUMULATIVE_PERIODIC|STYLE_BARS; break;
                    case 'C': m_properties |= COMBINE_CUMULATIVE_CONTINUOUS|STYLE_BARS; break;
                    case 'A': m_properties |= COMBINE_AVERAGE|STYLE_LINE; break;
                }
                break;
        }
    }
}

unsigned int
ChartData::calculate_distance( const DateV d1, const DateV d2 ) const
{
    switch( m_properties & PERIOD_MASK )
    {
        case PERIOD_DAILY:
            return Date::calculate_days_between_abs( d1, d2 );
        case PERIOD_WEEKLY:
            return Date::calculate_weeks_between_abs( d1, d2 );
        case PERIOD_MONTHLY:
            return Date::calculate_months_between_abs( d1, d2 );
        case PERIOD_YEARLY:
        default:
            return labs( int( Date::get_year( d1 ) ) - int( Date::get_year( d2 ) ) );
    }
}

void
ChartData::forward_date( DateV& date ) const
{
    switch( m_properties & PERIOD_MASK )
    {
        case PERIOD_DAILY:      Date::forward_days( date, 1 ); break;
        case PERIOD_WEEKLY:     Date::forward_days( date, 7 ); break;
        case PERIOD_MONTHLY:
            if( Date::get_month( date ) == 12 )
                date = Date::make( Date::get_year( date ) + 1, 1, 1 );
            else
                date = Date::make( Date::get_year( date ), Date::get_month( date ) + 1, 1 );
            break;
        case PERIOD_YEARLY:
            date = Date::make( Date::get_year( date ) + 1, 1, 1 );
            break;
    }
}

void
ChartData::clear_points()
{
    values_date.clear();
    values_str.clear();
    values_str2index.clear();
    values_index2str.clear();
    values_num.clear();

    v_min = Constants::INFINITY_PLS;
    v_max = Constants::INFINITY_MNS;

    m_span = 0;
}

void
ChartData::add_value_date( DateV date, const Value vy, const Value vu, DiaryElemTag* elem )
{
    switch( get_period() )
    {
        case PERIOD_DAILY:      date = Date::isolate_YMD( date ); break;
        case PERIOD_WEEKLY:     Date::backward_to_week_start( date ); break;
        case PERIOD_MONTHLY:    Date::backward_to_month_start( date ); break;
        case PERIOD_YEARLY:     Date::backward_to_year_start( date ); break;
    }

    if( values_date.empty() )
    {
        values_date[ date ] = { vy, vu, 1, { elem } };
        return;
    }
    else if( values_date.find( date ) == values_date.end() ) // insert the intermediate values
    {
        DateV           d;
        unsigned int    steps_between;

        if( date < values_date.begin()->first )
        {
            d = date;
            steps_between = calculate_distance( d, values_date.begin()->first );
        }
        else
        {
            d = values_date.rbegin()->first;
            steps_between = calculate_distance( d, date );
        }

        for( unsigned int i = 1; i < steps_between; i++ )
        {
            forward_date( d );
            values_date[ d ] = { 0.0, 0.0, 0, {} };
        }
    }

    switch( get_combining() )
    {
        case COMBINE_CUMULATIVE_PERIODIC:
        case COMBINE_CUMULATIVE_CONTINUOUS:
            values_date[ date ].add_cumulative( vy, vu, elem );
            break;
        case ChartData::COMBINE_AVERAGE:
            values_date[ date ].add_average( vy, vu, elem );

            break;
    }
}

void
ChartData::fill_in_intermediate_date_values()
{
    auto get_next_real = [ this ]( DateV d ) -> MapDateValues::iterator
    {
        auto it { values_date.find( d ) };
        for( ; it != values_date.end(); ++it )
            if( it->second.c > 0 )
                break;

        return it;
    };

    switch( get_combining() )
    {
        case ChartData::COMBINE_CUMULATIVE_PERIODIC: // do nothing
            break;
        case ChartData::COMBINE_CUMULATIVE_CONTINUOUS:
        {
            Value vp{ 0.0 }, up{ 0.0 };

            for( auto& vd : values_date )
            {
                vd.second.v += vp;
                vd.second.u += up;

                vp = vd.second.v;
                up = vd.second.u;
            }
            break;
        }
        case ChartData::COMBINE_AVERAGE:
        {
            DateV d_last_real { Date::NOT_SET };
            Value vy_last_real { 0.0 }, vu_last_real { 0.0 };
            Value vy_step { 0.0 }, vu_step { 0.0 };
            int   i_step { 0 };

            for( auto& vd : values_date )
            {
                if( vd.second.c == 0 )
                {
                    if( i_step == 0 )
                    {
                        const auto vd_next_real { get_next_real( vd.first ) };
                        if( vd_next_real == values_date.end() ) break;
                        const auto steps_between { calculate_distance( d_last_real,
                                                                       vd_next_real->first ) };
                        vy_step = ( vd_next_real->second.v - vy_last_real ) / steps_between;
                        vu_step = ( vd_next_real->second.u - vu_last_real ) / steps_between;
                        i_step = 1;

                    }
                    vd.second.v = vy_last_real + vy_step * i_step;
                    vd.second.u = vu_last_real + vu_step * i_step;
                    ++i_step;
                }
                else
                {
                    i_step = 0;
                    vy_last_real = vd.second.v;
                    vu_last_real = vd.second.u;
                    d_last_real = vd.first;
                }
            }
        }
    }
}

void
ChartData::fill_in_date_underlays()
{
    auto        it_vs{ values_date.begin() }; // source
    unsigned    offset;

    switch( get_period() )
    {
        case ChartData::PERIOD_DAILY:
            //offset = Date::get_days_in_year( Date::get_year( d ) - 1 );
            offset = 365; // TODO: 3.2: incorporate leap years
            break;
        case ChartData::PERIOD_WEEKLY: offset = 52; break;
        // case ChartData::PERIOD_MONTHLY:
        default:                       offset = 12; break;
    }

    if( values_date.size() > offset )
    {
        auto it_vt{ values_date.begin() }; // target
        std::advance( it_vt, offset );

        while( it_vt != values_date.end() )
        {
            it_vt->second.u = it_vs->second.v;
            ++it_vs;
            ++it_vt;
        }
    }
}

void
ChartData::add_value_str( const Ustring& str, const Value vy, const Value vu, DiaryElemTag* elem )
{
    auto&& it{ values_str2index.find( str ) };

    if( it == values_str2index.end() )
    {
        const auto iv{ values_str.size() };
        values_str2index[ str ] = iv;
        values_index2str[ iv ] = str;
        values_str[ iv ] = { vy, vu, 1, { elem } };
    }
    else
    {
        switch( get_combining() )
        {
            case ChartData::COMBINE_CUMULATIVE_PERIODIC:
            case ChartData::COMBINE_CUMULATIVE_CONTINUOUS:
                values_str[ ( *it ).second ].add_cumulative( vy, vu, elem );
                break;
            case ChartData::COMBINE_AVERAGE:
                values_str[ ( *it ).second ].add_average( vy, vu, elem );
                break;
        }
    }
}

void
ChartData::add_value_num( const Value vx, const Value vy, const Value vu, DiaryElemTag* elem )
{
    if( values_num.find( vx ) == values_num.end() )
        values_num[ vx ] = { vy, vu, 1, { elem } };
    else
    {
        switch( get_combining() )
        {
            case ChartData::COMBINE_CUMULATIVE_PERIODIC:
            case ChartData::COMBINE_CUMULATIVE_CONTINUOUS:
                values_num[ vx ].add_cumulative( vy, vu, elem );
                break;
            case ChartData::COMBINE_AVERAGE:
                values_num[ vx ].add_average( vy, vu, elem );
                break;
        }
    }
}

void
ChartData::update_span()
{
    switch( get_type() )
    {
        case TYPE_DATE:     m_span = values_date.size();    break;
        case TYPE_NUMBER:   m_span = values_num.size();     break;
        default:            m_span = values_str.size();     break;
    }
}

// helper func: rounds a value down to the nearest multiple of 'step':
static double
floor_to( double value, double step )
{
    double result { std::floor( value / step ) * step };
    // snap near-zero results to exactly 0 to avoid float drift
    return( ( std::abs( result ) < step * 1e-9 ) ? 0.0 : result );
}

// helper func: returns the "nicest" step size >= raw_step by snapping to the nearest value...
// ...of the form mantissa * 10^exponent:
static double
nice_step( double raw_step )
{
    const double exponent = std::floor( std::log10( raw_step ) );
    const double magnitude = std::pow( 10.0, exponent );
    const double fraction = raw_step / magnitude; // in [1, 10)

    for( double m : ChartData::NICE_MANTISSAS )
        if( fraction <= m + 1e-9 ) return m * magnitude;

    // fallback: next power of ten
    return 10.0 * magnitude;
}

// calculates optimum axis parameters for a y-axis chart. guarantees:
// - min_round_value <= value_min
// - min_round_value + NUM_DIVISIONS * step_size >= value_max
// - both values are "nicely" rounded
// - if value_min <= 0 <= value_max, zero is exactly on a grid line
void
ChartData::calculate_grid()
{
    // if( v_min > v_max ) throw std::invalid_argument( "value_min must be <= value_max" );

    // handle degenerate flat-line case:
    if( v_min == v_max )
    {
        double center = v_max;
        v_grid_step = ( center == 0.0 ) ? 1.0
                                        : nice_step( std::abs( center ) / ChartData::NUM_Y_STEPS );
        v_grid_min = floor_to( center, v_grid_step ) - v_grid_step;
    }

    const double range = v_max - v_min;
    const double raw_step = range / ChartData::NUM_Y_STEPS;
    double step = nice_step( raw_step );

    // if the range spans zero, shift the grid so that zero lands exactly on a line:
    if( v_min <= 0.0 && v_max >= 0.0 )
    {
        // with this step size, find the lowest grid line <= value_min that
        // keeps 0.0 as an exact multiple of step.
        double min_round = floor_to( v_min, step ); // already a multiple of step

        // verify upper bound; if not satisfied, bump step up one notch and retry
        while( min_round + ChartData::NUM_Y_STEPS * step < v_max )
        {
            // Advance to next nice step size
            const double exponent = std::floor( std::log10( step ) );
            const double magnitude = std::pow( 10.0, exponent );
            const double fraction = step / magnitude;

            double next_fraction = 10.0; // fallback
            for( double m : ChartData::NICE_MANTISSAS )
                if( m > fraction + 1e-9 )
                {
                    next_fraction = m;
                    break;
                }

            step = next_fraction * magnitude;
            min_round = floor_to( v_min, step );
        }

        v_grid_step = step;
        v_grid_min = min_round;
        return;
    }

    // normal (no zero-crossing) case:
    double best_min = std::numeric_limits< double >::max();
    double best_step = step;
    double best_score = std::numeric_limits< double >::max();

    // explore a small neighbourhood of nice steps around the computed one
    const double exponent = std::floor( std::log10( raw_step ) );
    const double magnitude = std::pow( 10.0, exponent );

    for( double scale : { magnitude * 0.1, magnitude, magnitude * 10.0 } )
    {
        for( double m : ChartData::NICE_MANTISSAS )
        {
            double candidate = m * scale;
            if( candidate < raw_step - 1e-9 ) continue; // too small

            double min_round = floor_to( v_min, candidate );
            if( min_round + ChartData::NUM_Y_STEPS * candidate < v_max ) continue; // doesn't cover

            // score: prefer smallest total span
            double score = min_round + ChartData::NUM_Y_STEPS * candidate - v_min;
            if( score < best_score - 1e-9 )
            {
                best_score = score;
                best_step = candidate;
                best_min = min_round;
            }
        }
    }

    v_grid_step = best_step;
    v_grid_min = best_min;
}

void
ChartData::update_min_max()
{
    switch( get_type() )
    {
        case TYPE_DATE:   update_min_max( values_date ); break;
        case TYPE_NUMBER: update_min_max( values_num ); break;
        case TYPE_STRING: update_min_max( values_str ); break;
    }

    // try to improve the min of the graph to optimize between screen real estate usage and...
    // ...giving a better idea of the actual ratios:
    if( v_min > 0 /* MAYBE: && get_style() == STYLE_BARS */ )
    {
        if( v_min <= ( v_max - v_min ) )
            v_min = 0.0;
        else
            v_min -= ( v_max - v_min );
    }

    // otimum grid line coordinates on y-axis:
    calculate_grid();
}

void
ChartData::calculate_points()
{
    if( !m_p2diary ) return;

    clear_points();

    auto col{ m_td->m_columns_map[ m_tcidx ] };

    if( !col ) return;

    col->allocate_filter_stacks();

    for( auto& line : m_td->m_lines )
    {
        if( m_tcidf != ChartData::COLUMN_NONE && !m_filter_v.empty() &&
            line->get_col_v_txt_by_cid( m_tcidf ) != m_filter_v )
            continue;

        const Value vy{ m_tcidy == ChartData::COLUMN_COUNT
                        ? 1.0
                        : line->get_col_v_num_by_cid( m_tcidy ) };
        const Value vu{ m_tcidu < ChartData::COLUMN_NONE
                        ? line->get_col_v_num_by_cid( m_tcidu )
                        : 0.0 };
        const auto  ic{ col->get_index() };

        switch( get_type() )
        {
            case ChartData::TYPE_DATE:
                if( col->get_type() == TableColumn::TCT_GANTT )
                {
                    // TODO: 3.2: subdivide the value when necessary
                    // if( m_tcidy != ChartData::COLUMN_COUNT )
                    // {
                    //     const Value vy_sub{ vy / period };
                    //     const Value vu_sub{ vu / period };
                    // }

                    for( auto segment : line->m_periods )
                        for( auto vx = segment.dbgn; vx < segment.dend; forward_date( vx ) )
                            add_value_date( vx, vy, vu, line->m_p2elem );
                }
                else
                {
                    const auto& vx{ line->get_value_date( ic ) };
                    if( vx != Date::NOT_SET )
                        add_value_date( vx, vy, vu, line->m_p2elem );
                }
                break;
            case ChartData::TYPE_NUMBER:
            {
                const auto& vx{ line->get_value_num( ic ) };
                add_value_num( vx, vy, vu, line->m_p2elem );
                break;
            }
            default:
            {
                const auto& vx{ line->get_value_txt( ic ) };
                add_value_str( vx, vy, vu, line->m_p2elem );
                break;
            }
        }
    }

    col->deallocate_filter_stacks();

    fill_in_intermediate_date_values();

    if( get_type() == ChartData::TYPE_DATE &&
        get_period() != ChartData::PERIOD_YEARLY &&
        m_tcidu == ChartData::COLUMN_PREV_YEAR )
        fill_in_date_underlays();

    update_min_max();
    update_span();
}

void
ChartData::set_properties( int t )
{
    if( 0 == ( t & STYLE_MASK ) )       t |= ( m_properties & STYLE_MASK );
    if( 0 == ( t & TYPE_MASK ) )        t |= ( m_properties & TYPE_MASK );
    if( 0 == ( t & PERIOD_MASK ) )      t |= ( m_properties & PERIOD_MASK );
    if( 0 == ( t & COMBINE_MASK ) )     t |= ( m_properties & COMBINE_MASK );

    m_properties = t;
}

void
ChartData::set_table( const D::DEID& id )
{
    m_table_id = id;
    m_tcidx = -1;
    m_tcidy = ChartData::COLUMN_COUNT;
    m_tcidu = ChartData::COLUMN_NONE;
    m_tcidf = ChartData::COLUMN_NONE;
    m_filter_v.clear();

    refresh_table();
    refresh_type();
    refresh_unit();
}

void
ChartData::refresh_table()
{
    m_td->clear();

    switch( m_table_id )
    {
        case DEID::TABLE_ID_ALL_ENTRIES:
            m_td->set_from_string( TABLE_DEF_ALL_ENTRIES );
            break;
        case DEID::TABLE_ID_ALL_PARAGRAPHS:
            m_td->set_from_string( TABLE_DEF_ALL_PARAGRAPHS );
            break;
        default:
        {
            auto table { m_p2diary->get_table( m_table_id ) };
            if( !table ) return;
            m_td->set_from_string( table->get_definition() );
            break;
        }
    }

    m_td->m_grouping_depth = 0; // disable grouping again
    // TODO: 3.2: (MAYBE): a special table populator may be created for charts as they disregard grouping
    m_td->populate_lines( false );
}

void
ChartData::refresh_type()
{
    auto col{ m_td->m_columns_map[ m_tcidx ] };

    if( !col ) return;

    if( col->get_type() == TableColumn::TCT_DATE || col->get_type() == TableColumn::TCT_GANTT )
        set_properties( TYPE_DATE );
    else
    if( col->is_numeric() )
        set_properties( TYPE_NUMBER );
    else
        set_properties( TYPE_STRING );
}

void
ChartData::refresh_unit()
{
    if( m_tcidy != COLUMN_COUNT )
    {
        auto col{ m_td->m_columns_map[ m_tcidy ] };
        if( col )
        {
            m_unit = col->get_unit();
            return;
        }
    }

    m_unit = "";
}

const Ustring ChartElem::DEFINITION_DEFAULT
{ STR::compose( "Gt", DEID::TABLE_ID_ALL_ENTRIES.get_raw(), "\nGo_LMP" ) };
const Ustring ChartElem::DEFINITION_DEFAULT_Y
{ STR::compose( "Gt", DEID::TABLE_ID_ALL_ENTRIES.get_raw(), "\nGo_LYP" ) };

const R2Pixbuf&
ChartElem::get_icon() const
{
    return Lifeograph::icons->chart;
}

} // end of namespace LoG
