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


#include <cmath>

#include "chart_common.hpp"


using namespace LoG;

// CHART ===========================================================================================
inline DateV
ChartCommon::get_period_date( DateV d )
{
    switch( m_data.m_properties & ChartData::PERIOD_MASK )
    {
        case ChartData::PERIOD_WEEKLY:  Date::backward_to_week_start( d ); break;
        case ChartData::PERIOD_MONTHLY: Date::backward_to_month_start( d ); break;
        case ChartData::PERIOD_YEARLY:  Date::backward_to_year_start( d ); break;
    }

    return d;
}

Ustring
ChartCommon::get_date_str( DateV date ) const
{
    switch( m_data.m_properties & ChartData::PERIOD_MASK )
    {
        case ChartData::PERIOD_MONTHLY: return Date::format_string( date, "YM" );
        case ChartData::PERIOD_YEARLY:  return Date::get_year_str( date );
        // weekly & daily:
        default:                        return Date::format_string( date, "YMD" );
    }
}

void
ChartCommon::set_zoom( float level )
{
    if( level != m_zoom_level )
        m_zoom_level = ( level > 1.0f ? 1.0f : ( level < 0.0f ) ? 0.0f : level );

    if( m_width > 0 ) // if on_size_allocate is executed before
    {
        update_col_geom( false );
        refresh();
    }
}

bool
ChartCommon::is_zoom_possible() const
{
    return( !( m_step_count == m_data.m_span && m_step_x >= m_width_col_min ) &&
            m_data.get_type() == ChartData::TYPE_DATE );
}

double
find_nearest_grid( double base, double amplitude, int exp )
{
    if( amplitude == 0 ) return amplitude;

    const double step_5         { 5 * pow( 10, exp - 1 ) };
    const double step_10        { pow( 10, exp ) };
    const double rounded_out    { abs( std::fmod( base, step_10 ) ) };
    const double nearest_grid_0 { abs( rounded_out ) };
    const double nearest_grid_5 { abs( step_5 - rounded_out ) };
    const double nearest_grid_10{ abs( step_10 - rounded_out ) };

    double nearest_grid{ std::min( std::min( nearest_grid_0, nearest_grid_5 ),
                                   nearest_grid_10 ) };

    if( nearest_grid == nearest_grid_0 )
        nearest_grid = ( base - rounded_out );
    else if( nearest_grid == nearest_grid_10 )
        nearest_grid = ( base - rounded_out + step_10 );
    else
        nearest_grid = ( base - rounded_out + step_5 );

    if( abs( nearest_grid - base ) < amplitude / 10 )
        return nearest_grid;

    return find_nearest_grid( base, amplitude, exp - 1 );
}

void
ChartCommon::update_col_geom( bool F_new )
{
    // 100% zoom:
    const bool F_style_line  { m_data.get_style() == ChartData::STYLE_LINE };
    const auto step_count_nom{ unsigned( m_length / m_width_col_min ) + ( F_style_line ? 1U
                                                                                       : 0U ) };
    const auto step_count_min{ m_data.m_span > step_count_nom ? step_count_nom : m_data.m_span };

    m_step_count = ( m_zoom_level * ( m_data.m_span - step_count_min ) ) + step_count_min;
    m_step_x = m_length / ( m_step_count - ( F_style_line ? 1 : 0 ) );

    m_ov_height = ( m_step_count < m_data.m_span ? ( m_height * 0.1 ) : 0.0 );

    m_y_max = m_height - m_h_x_values - m_ov_height;

    if( m_data.v_min < 0 )
        m_y_max -= m_label_height;

    m_ampli_y = m_y_max - m_y_min;

    const unsigned int col_start_max{ m_data.m_span - m_step_count };
    if( F_new || m_step_start > col_start_max )
        m_step_start = col_start_max;

    if( m_data.v_max != Constants::INFINITY_MNS ) // i.e. was initialized

        m_ampli_v = m_data.v_grid_step * ( ChartData::NUM_Y_STEPS - 1 );
    else
        m_ampli_v = 1;

    m_coefficient = m_ampli_y / m_ampli_v;
    m_y_0 = ( m_data.v_grid_min < 0 ? ( m_y_max + m_data.v_grid_min * m_coefficient ) : m_y_max );

    // OVERVIEW PARAMETERS
    m_ampli_ov = m_ov_height - 2 * m_border_label;
    m_coeff_ov = m_ampli_ov / m_ampli_v;
    m_step_x_ov = m_width - 2 * m_border_label;
    if( m_data.m_span > 1 )
        m_step_x_ov /= m_data.m_span - 1;
}

void
ChartCommon::resize( int w, int h )
{
    const bool F_first{ m_width < 0 };

    m_width = w;
    m_height = h;

    update_dims( F_first );
}

void
ChartCommon::update_dims( const bool F_first )
{
    if( !update_label_size() ) return; // implemented on each platform

    m_border_curve    = m_width * 0.06;
    m_border_label    = m_label_size * 0.075;

    m_width_col_min   = m_border_curve * 0.8;
    m_label_height    = m_label_size + 2 * m_border_label;

    m_x_min           = m_border_curve + m_border_label;
    m_x_min_bar       = m_x_min + m_width_col_min / 2;
    m_y_min           = m_label_height + m_label_size; // m_label_size is for margin

    m_x_max           = m_width - m_border_curve;
    m_length          = m_x_max - m_x_min;

    m_unit_line_thk   = ( m_label_size / 25 );

    update_h_x_values();

    update_col_geom( F_first );
}

void
ChartCommon::update_h_x_values()
{
    if( m_data.get_type() == ChartData::TYPE_DATE )
    {
        if( ( m_data.m_properties & ChartData::PERIOD_MASK ) == ChartData::PERIOD_YEARLY )
            m_h_x_values = m_label_height * 1.5;
        else
            m_h_x_values = m_label_height * 3;
    }
    else
        m_h_x_values = m_label_height * 5;
}

void
ChartCommon::update_pre_and_post_steps()
{
    m_pre_steps = ceil( m_x_min / m_step_x );
    if( m_pre_steps > m_step_start )
        m_pre_steps = m_step_start;

    m_post_steps = ceil( m_border_curve / m_step_x );
    if( m_post_steps > m_data.m_span - m_step_count - m_step_start )
        m_post_steps = m_data.m_span - m_step_count - m_step_start;
}

void
ChartCommon::scroll( int offset )
{
    //if( m_points )
    {
        if     ( offset < 0 && m_step_start > 0 )
            m_step_start--;
        else if( offset > 0 && m_step_start < ( m_data.m_span - m_step_count ) )
            m_step_start++;
        else
            return;

        refresh();
    }
}

