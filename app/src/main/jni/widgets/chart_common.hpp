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


#pragma once


#include "../helpers.hpp"
#include "../diaryelements/diarydata.hpp"
#include "../diaryelements/diary.hpp"


namespace LoG
{

using namespace HELPERS;


class FiltererContainer; // forward declaration


class ChartCommon
{
    public:
                                    ChartCommon( bool F_printing_mode )
                                    :   m_F_printing_mode( F_printing_mode ) {}
        virtual                     ~ChartCommon() {}

        void                        set_diary( Diary* diary )
        { m_data.set_diary( diary ); }
        Diary*                      get_diary()
        { return m_data.m_p2diary; }

       ChartData&                   get_chart_data()
       { return m_data; }

        bool                        is_zoom_possible() const;
        void                        set_zoom( float );

        int                         get_width() const { return m_width; }
        int                         get_height() const { return m_height; }

        void                        resize( int, int );
        void                        update_dims( const bool );
        void                        update_h_x_values();
        void                        update_pre_and_post_steps();
        void                        scroll( int );

        Value
        get_value_at( int i )
        {
            switch( m_data.get_type() )
            {
                case ChartData::TYPE_STRING: return get_yvalue_at( m_data.values_str, i ).v;
                case ChartData::TYPE_NUMBER: return get_yvalue_at( m_data.values_num, i ).v;
                default:                     return get_yvalue_at( m_data.values_date, i ).v;
            }
        }

    private:
#ifndef __ANDROID__
        virtual bool                update_label_size() = 0;
#else
        bool                        update_label_size()
        {
            m_label_size = Glib::get_chart_label_size_from_android();
            return true;
        }
#endif
        virtual void                refresh() {}

    protected:
        void                        update_col_geom( bool = false );

        const SetTagsByNameID&
        get_elems_at( int i )
        {
            switch( m_data.get_type() )
            {
                case ChartData::TYPE_STRING: return get_yvalue_at( m_data.values_str, i ).elems;
                case ChartData::TYPE_NUMBER: return get_yvalue_at( m_data.values_num, i ).elems;
                default:                     return get_yvalue_at( m_data.values_date, i ).elems;
            }
        }
        template< class T >
        const ChartData::YValues&
        get_yvalue_at( const T& values, int i )
        {
            if( abs( i ) >= values.size() )
                throw Error( STR::compose( "YValue (", i, ") out of bounds" ) );

            auto&& iter{ i < 0 ? values.end() : values.begin() };
            std::advance( iter, i < 0 ? i - 1 : i );
            return iter->second;
        }

        Ustring
        get_x_label_at( int i )
        {
            switch( m_data.get_type() )
            {
                case ChartData::TYPE_STRING:
                    return m_data.values_index2str[ i ];
                case ChartData::TYPE_NUMBER:
                {
                    auto&& iter{ m_data.values_num.begin() };
                    std::advance( iter, i );
                    return STR::format_number( iter->first );
                }
                default:
                {
                    auto&& iter{ m_data.values_date.begin() };
                    std::advance( iter, i );
                    return Date::format_string( iter->first );
                }
            }
        }

        DateV                       get_period_date( DateV );
        Ustring                     get_date_str( DateV ) const;

        // DATA
#ifdef __ANDROID__
    public: // to allow access from Android
#endif
        ChartData                   m_data          { nullptr };

        // GEOMETRY
        static constexpr int        Y_DIVISIONS_MAX { 4 };
        double                      m_x_offset      { 0.0 };
        double                      m_y_offset      { 0.0 };
        int                         m_width         { -1 };
        int                         m_height        { -1 };
        int                         m_step_count    { 0 };
        int                         m_step_start    { 0 };
        int                         m_pre_steps;
        int                         m_post_steps;
        double                      m_zoom_level    { 1.0 };

        double                      m_border_curve;
        double                      m_border_label;
        double                      m_label_size;
        double                      m_label_height;
        double                      m_h_x_values;
        double                      m_width_col_min;

        double                      m_x_min;
        double                      m_x_min_bar;
        double                      m_y_min;

        double                      m_x_max{ 0.0 }, m_y_max{ 0.0 }, m_y_0{ 0.0 };
        double                      m_ampli_y{ 0.0 }, m_length{ 0.0 };
        double                      m_ampli_v{ 0.0 };
        double                      m_step_x{ 0.0 }, m_coefficient{ 0.0 };
        double                      m_ov_height{ 0.0 };
        double                      m_step_x_ov{ 0.0 }, m_ampli_ov{ 0.0 }, m_coeff_ov{ 0.0 };
        double                      m_unit_line_thk{ 1.0 };

        int                         m_hovered_step{ 0 };
        int                         m_warning_w{ 0 };

        // FLAGS
        bool                        m_F_button_pressed   { false };
        bool                        m_F_overview_hovered { false };
        bool                        m_F_widget_hovered   { false };
        bool                        m_F_printing_mode    { false };
};

} // end of namespace LoG

