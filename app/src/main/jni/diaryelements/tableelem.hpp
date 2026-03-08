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


#ifndef LIFEOGRAPH_TABLEELEM_HEADER
#define LIFEOGRAPH_TABLEELEM_HEADER


#include "../helpers.hpp"
#include "diarydata.hpp"


namespace LoG
{

// FORWARD DECLARATION
class TableData;
class TableLine;

// TABLE COLUMN ====================================================================================
class TableColumn : public Named
{
    public:
        enum Type { TCT_TEXT = 0,        TCT_DATE = 1,  TCT_DURATION = 2,   TCT_SIZE = 3,
                    TCT_NUMBER = 4,      TCT_BOOL = 5,  TCT_TAG_V = 6,      TCT_SUBTAG = 7,
                    TCT_REFERRERS = 8,   TCT_COMPLETION = 9,                TCT_TODO_STATUS = 10,
                    TCT_GANTT = 11,      TCT_PATH_LENGTH = 12,              TCT_THEME = 13,
                    TCT_ARITHMETIC = 14, TCT_SCRIPT = 15,
                    TCT_ORDER = 50 };

        enum class Coloring : char { COLOR_OFF = 'O',
                                     NEG_RED = 'N', BIG_GREEN = 'G', BIG_RED = 'R' };

        TableColumn( TableData* p2data, int id, Type t = TCT_TEXT );

        bool                        set_type( Type );
        Type                        get_type() const
        { return m_type; }

        void                        set_opt_int( int32_t value )
        { m_opt_int = value; }
        void                        set_opt_int_field( int32_t filter, int32_t value )
        { m_opt_int = ( ( m_opt_int & ~filter ) | value ); }
        int32_t                     get_opt_int() const
        { return m_opt_int; }

        void                        set_tag( DiaryElemTag* tag )
        { m_p2tag = tag; }
        DiaryElemTag*               get_tag() const
        { return m_p2tag; }

        void                        allocate_filter_stacks();
        void                        deallocate_filter_stacks();

        bool                        is_source_elem_para() const;
        void                        set_source_elem( int src )
        {
            m_source_elem = src;
            // reset value type when necessary:
            if( is_source_elem_para() && get_type() == TCT_TEXT )
                m_opt_int = ( ( m_opt_int & ~VT::SEQ_FILTER ) | VT::TCT_SRC_TITLE );
        }
        int                         get_source_elem() const
        { return m_source_elem; }
        void                        set_source_elem_filter( const Filter* filter )
        { m_filter_source_elem = filter; }
        const Filter*               get_source_elem_filter() const
        { return m_filter_source_elem; }

        bool                        is_delta() const
        { return( m_show_as == VT::SAS::DELTA::C ); }

        bool                        is_counting() const
        { return( m_show_as == VT::SAS::COUNT::C ); }

        void                        set_combine_same( bool F_group_same )
        { m_F_combine_same = F_group_same; }
        bool                        is_combine_same() const
        { return m_F_combine_same; }

        void                        set_count_filter( const Filter* filter )
        { m_p2filter_count = filter; }
        const Filter*               get_count_filter() const
        { return m_p2filter_count; }

        bool                        is_numeric() const;
        bool                        is_enumerable( bool = false ) const;
//        bool                        is_fraction() const;
        bool                        is_percentage() const;
        bool                        is_sort_desc() const
        { return m_sort_desc; }
        void                        clear_sorting()
        {
            m_sort_desc = false;
            m_sort_order = 0;
            m_F_combine_same = false; // combining only makes sense for sorted columns
        }

        bool                        has_coloring() const
        { return( is_enumerable() && m_coloring_scheme != Coloring::COLOR_OFF ); }
        Coloring                    get_coloring_scheme() const
        { return m_coloring_scheme; }
        void                        set_coloring_scheme( Coloring cs )
        {
            m_coloring_scheme = cs;
            switch( cs )
            {
                case Coloring::COLOR_OFF:
                case Coloring::NEG_RED:
                    m_color_small.set( 0.0, 0.0, 0.0 );
                    m_color_large.set( 0.0, 0.0, 0.0 );
                    break;
                case Coloring::BIG_GREEN:
                    m_color_small.set( 1.0, 0.0, 0.0 );
                    m_color_large.set( 0.0, 0.8, 0.0 );
                    break;
                case Coloring::BIG_RED:
                    m_color_small.set( 0.0, 0.8, 0.0 );
                    m_color_large.set( 1.0, 0.0, 0.0 );
                    break;
            }
        }
        ColorBasic                  get_color( double value ) const
        {
            if( m_coloring_scheme == Coloring::NEG_RED )
                return ( value < 0 ? ColorBasic( 1, 0, 0 ) : ColorBasic( 0, 0, 0 ) );
            else
            {
                const double c { ( value - get_val_min() ) / ( get_val_max() - get_val_min() ) };
                return ColorBasic( m_color_small.r + ( m_color_large.r - m_color_small.r ) * c,
                                   m_color_small.g + ( m_color_large.g - m_color_small.g ) * c,
                                   m_color_small.b + ( m_color_large.b - m_color_small.b ) * c );
            }
        }

        Ustring                     get_unit() const;

        Pango::Alignment            get_alignment() const;

        int                         get_index() const
        { return m_index; }

        void                        calculate_value_num( const Entry*, TableLine* ) const;
        Value                       calculate_weight( const Entry* ) const;
        Ustring                     calculate_value_txt( const Entry* ) const;
        DiaryElemTag*               calculate_value_tag( const Entry* ) const;
        DateV                       calculate_value_date( const Entry* ) const;
        void                        calculate_value_gantt( const Entry*, TableLine* ) const;

        void                        calculate_value_num( const Paragraph*, TableLine* ) const;
        Value                       calculate_weight( const Paragraph* ) const;
        Ustring                     calculate_value_txt( const Paragraph* ) const;
        DiaryElemTag*               calculate_value_tag( const Paragraph* ) const;
        DateV                       calculate_value_date( const Paragraph* ) const;
        void                        calculate_value_gantt( const Paragraph*, TableLine* ) const;

        Value                       calculate_duration( const DiaryElemTag*, TableLine* ) const;
        Value                       calculate_arithmetic( TableLine* ) const;
        void                        calculate_script( TableLine* ) noexcept;

        void                        format_number( TableLine* ) const;

        const int                   m_id;

    protected:
        const Entry*                get_src_entry_as_needed( const Entry* ) const;
        const Paragraph*            get_src_para_as_needed( const Entry* ) const;
        const Paragraph*            get_src_para_as_needed( const Paragraph* ) const;

        TableData*                  m_p2data            { nullptr };
        int                         m_index             { -1 };
        Type                        m_type;
        DiaryElemTag*               m_p2tag             { nullptr };
        int32_t                     m_opt_int           { VT::TVTC::TOTAL::I };
        int64_t                     m_opt_int1          { 0 };  // extra options
        int64_t                     m_opt_int2          { 0 };
        double                      m_opt_double1       { 0.0 };
        double                      m_opt_double2       { 0.0 };
        String                      m_opt_str;
        FiltererContainer*          m_FC_generic        { nullptr }; // used by boolen columns now
        int                         m_source_elem       { VT::SRC_ITSELF };
        const Filter*               m_filter_source_elem{ nullptr };
        FiltererContainer*          m_FC_source_elem    { nullptr };
        char                        m_show_as           { VT::SAS::DEFAULT::C };
        bool                        m_F_combine_same    { false };
        const Filter*               m_p2filter_count    { nullptr };
        FiltererContainer*          m_FC_count          { nullptr };
        char                        m_summary_func      { VT::SUMF::DEFAULT::C };
        double                      m_width             { 1.0 };
        int                         m_sort_order        { 0 };
        bool                        m_sort_desc         { false };
        Coloring                    m_coloring_scheme   { Coloring::COLOR_OFF };
        ColorBasic                  m_color_small;
        ColorBasic                  m_color_large;

        std::set< double >          m_values;
        double                      get_val_min() const
        { return( m_values.empty() ? Constants::INFINITY_PLS : *m_values.begin() ); }
        double                      get_val_max() const
        { return( m_values.empty() ? Constants::INFINITY_MNS : *m_values.rbegin() ); }

    friend class Table;
    friend class TableLine;
    friend class TableData;
    friend class TableColSortComparator;
    friend class TableLineComparator;
    friend class WidgetTable;
};

using ListTableColumns    = std::list< TableColumn* >;
using VectorTableColumns  = std::vector< TableColumn* >;
using MapIdsTableColumns  = std::map< int, TableColumn* >;

// TABLE LINE ======================================================================================
class TableLine
{
    public:
        struct Period
        {
            double x; // x coordinate
            double w; // width
            DateV  dbgn;
            DateV  dend;
        };

        struct TableLineComparator
        {
            TableLineComparator( const ListTableColumns& cols )
            : m_r2sort_cols( cols ) {}

            template< typename T >
            bool evaluate( const T& item_L, const T& item_R,
                    ListTableColumns::const_iterator it_col, TableLine* l, TableLine* r ) const
            {
                const auto& col{ *it_col };
                if( item_L != item_R ) return( col->is_sort_desc() == ( item_L > item_R ) );
                if( ++it_col != m_r2sort_cols.end() ) return compare( it_col, l, r );
                return col->is_sort_desc();
            }

            bool compare( ListTableColumns::const_iterator it_col,
                          TableLine* l, TableLine* r ) const
            {
                const TableColumn* col { *it_col };
                const auto         ic  { col->get_index() };

                if( col->is_percentage() )
                {
                    double v_l{ l->m_values_num[ ic ] == 0 ? 0.0 :
                                ( l->m_weights[ ic ] == 0 ? 1.0 :
                                l->m_values_num[ ic ] / l->m_weights[ ic ] ) };
                    double v_r{ r->m_values_num[ ic ] == 0 ? 0.0 :
                                ( r->m_weights[ ic ] == 0 ? 1.0 :
                                r->m_values_num[ ic ] / r->m_weights[ ic ] ) };
                    return evaluate( v_l, v_r, it_col, l, r );
                }
                else if( col->is_enumerable( true ) )
                    return evaluate( l->m_values_num[ ic ],
                                     r->m_values_num[ ic ],
                                     it_col, l, r );
                else
                    return evaluate( l->m_values_txt[ ic ],
                                     r->m_values_txt[ ic ],
                                     it_col, l, r );
            }

            bool operator()( TableLine* l, TableLine* r ) const
            {
                if( m_r2sort_cols.empty() ) return false;
                return compare( m_r2sort_cols.begin(), l, r );
            }

            const ListTableColumns& m_r2sort_cols;
        };

        TableLine( TableData* p2data, int depth, bool F_expanded );

        using SetSublines   = std::multiset< TableLine*, TableLineComparator >;

        double                      get_col_v_num_by_cid( int ) const; // by column id
        Ustring                     get_col_v_txt_by_cid( int ) const; // by column id
        DateV                       get_col_v_date_by_cid( int ) const; // by column id

        Value                       get_value_num( int ) const;
        Value                       get_col_weight( int ) const;
        Ustring                     get_value_txt( int ) const;
        DateV                       get_value_date( unsigned int ) const;

        void                        set_value_num( const int, Value, Value = 1.0  );
        void                        set_value_txt( int, Ustring&& );

        void                        add_col_v_num( const int, Value, Value = 1.0 );
        // default value==1 enables calculating average when needed
        void                        add_col_v_txt( int, const Ustring& );
        void                        add_col_v_gantt( int, TableLine* );

        bool is_group_head() const;
        bool is_expanded() const { return m_F_expanded; }

        void toggle_expanded() { m_F_expanded = !m_F_expanded; }
        void toggle_all_expanded();

        bool has_str( const Ustring& ) const;

        void clear_sublines( bool F_delete )
        {
            for( auto line : m_sublines )
            {
                line->clear_sublines( F_delete );
                if( F_delete )
                    delete line;
                m_group_size = 0; // no need to go up-chain as clearing always starts from masterh.
            }

            if( !m_sublines.empty() ) // only for headers
            {
                m_values_txt.clear();
                m_values_num.clear();
                m_weights.clear();
            }
            m_sublines.clear();
        }

        TableLine* get_subline_by_id( LoGID id ) const;

        TableLine* get_subline_by_row_i( const int i_target ) const
        {
            int i{ 0 };
            return get_subline_by_row_i( i_target, i );
        }

        TableLine* get_subline_by_row_i( const int i_target, int& i ) const
        {
            for( auto& sl : m_sublines )
            {
                if( i == i_target ) return sl;

                i++;

                if( sl->is_group_head() && sl->is_expanded() )
                {
                    auto ssl{ sl->get_subline_by_row_i( i_target, i ) };
                    if( ssl ) return ssl;
                }
            }

            return nullptr;
        }

        TableLine*                  find_subline_per_grouping( TableLine* line );
        TableLine*                  create_group_line( TableLine* sl );

        void                        incorporate_subline_values( TableLine* line );
        void                        subtract_line_from_total( TableLine* line );
        void                        set_total_value_strings();
        void                        insert_subline( TableLine* );
        bool                        remove_entry( Entry* );
        void                        increase_group_size_up_chain()
        { for( TableLine* tl = this; tl; tl = tl->m_p2parent ) tl->m_group_size++; }
        void                        decrease_group_size_up_chain()
        { for( TableLine* tl = this; tl; tl = tl->m_p2parent ) tl->m_group_size--; }

        std::vector< Ustring >      m_values_txt;
        std::vector< Value >        m_values_num;
        std::vector< Value >        m_weights;
        std::list< Period >         m_periods; // for gantt charts

        TableData*                  m_p2data        { nullptr };
        DiaryElemTag*               m_p2elem        { nullptr };
        int                         m_depth         { 0 };
        TableLine*                  m_p2parent      { nullptr };
        SetSublines                 m_sublines;
        std::vector< TableLine* >   m_subgroups_unsorted;    // groupheads are sorted last
        int                         m_group_size    { 0 };

        bool                        m_F_expanded;
};

using VectorTableLines  = std::vector< TableLine* >;

// TABLE DATA ======================================================================================
class TableData
{
    public:
        static constexpr int        COL_ID_MIN      = 100;
        static constexpr int        COL_ID_MAX      = 999;
        static constexpr int        COL_COUNT_MAX   = 32;

        TableData( Diary* d = nullptr ) : m_p2diary( d ) {}
        ~TableData() { clear(); }

        void                        set_diary( Diary* diary )
        { m_p2diary = diary; }
        Diary*                      get_diary() const
        { return m_p2diary; }

        void                        clear();

        std::string                 get_as_string() const;
        void                        set_from_string( const Ustring& );

        TableColumn*                add_column( int, int = -1 );
        void                        dismiss_column( int );
        void                        move_column( int, int );
        void                        set_show_order_column( bool );
        bool                        has_order_column() const
        { return( !m_columns.empty() && m_columns.front()->m_type == TableColumn::TCT_ORDER ); }

        void                        set_column_sort_or_change_dir( int );
        void                        set_column_sort_dir( int, bool );
        void                        ensure_sort_column();
        void                        unset_column_sort( int );

        TableColumn*                get_column_by_id( const int id )
        {
            auto&& iter( m_columns_map.find( id ) );
            return( iter == m_columns_map.end() ? nullptr : iter->second );
        }

        TableColumn*                get_group_column( int depth )
        {
            if( depth > int( m_columns_sort.size() ) || depth > m_grouping_depth )
                return nullptr;
            else
                return( *std::next( m_columns_sort.begin(), depth ) );
        }

        bool                        populate_lines( bool, Entry* = nullptr );
        void                        update_lines_cache();
        void                        update_delta_columns();
        void                        update_order_column();
        void                        resort_lines();
        void                        sort_group_lines( TableLine* );

        // TODO: 3.2: consider moving the below methods to TableLine:
        double                      get_value( unsigned int, unsigned int ) const;
        double                      get_value_pure( unsigned int, unsigned int ) const;
        double                      get_weight( unsigned int, unsigned int ) const;
        Ustring                     get_value_str( unsigned int, unsigned int ) const;
        Ustring                     get_total_value_str( unsigned int ) const;

        bool                        is_col_value_same( unsigned int, unsigned int ) const;

        void                        set_para_based( bool );

    protected:
        int                         generate_new_id() const;
        void                        set_gantt_column_coords();

        template< class T >
        TableLine* create_line( T* elem, int depth, bool F_expanded )
        {
            TableLine* line { new TableLine( this, depth, F_expanded ) };

            line->m_p2elem = elem;

            for( auto& col : m_columns )
            {
                switch( col->get_type() )
                {
                    case TableColumn::TCT_NUMBER:
                    case TableColumn::TCT_BOOL:
                    case TableColumn::TCT_COMPLETION:
                    case TableColumn::TCT_SIZE:
                    case TableColumn::TCT_DURATION:
                    case TableColumn::TCT_TAG_V:
                    case TableColumn::TCT_PATH_LENGTH:
                    case TableColumn::TCT_ARITHMETIC:
                        col->calculate_value_num( elem, line );
                        break;
                    case TableColumn::TCT_TODO_STATUS:
                        col->calculate_value_num( elem, line );
                        line->set_value_txt( col->m_index, elem->get_todo_status_as_text() );
                        break;
                    case TableColumn::TCT_GANTT:
                        col->calculate_value_gantt( elem, line );
                        break;
                    case TableColumn::TCT_DATE:
                    {
                        const DateV date{ col->calculate_value_date( elem ) };
                        line->set_value_num( col->m_index, Date::get_secs_since_min( date ) );
                        line->set_value_txt( col->m_index,
                                             Date::format_string_adv( date, col->m_opt_str ) );
                        break;
                    }
                    case TableColumn::TCT_ORDER: // ignore, will be calculated later on
                        break;
                    case TableColumn::TCT_SCRIPT:
                        col->calculate_script( line );
                        // we cannot rely on script author to set values properly:
                        if( int( line->m_values_num.size() ) <= col->get_index() )
                            line->set_value_num( col->m_index, 0 );
                        if( int( line->m_values_txt.size() ) <= col->get_index() )
                            line->set_value_txt( col->m_index, "" );
                        break;
                    case TableColumn::TCT_REFERRERS:
                        if( col->is_counting() )
                            col->calculate_value_num( elem, line );
                        else
                            line->set_value_txt( col->m_index, col->calculate_value_txt( elem ) );
                        break;
                    case TableColumn::TCT_SUBTAG:
                        if( col->is_counting() )
                        {
                            col->calculate_value_num( elem, line );
                            break;
                        }
                        else
                        if( col->get_opt_int() != VT::TCOS_ALL )
                        {
                            auto tag{ col->calculate_value_tag( elem ) };
                            line->set_value_num( col->m_index,
                                                 tag ? tag->get_sibling_order()
                                                     : -1 );
                            line->set_value_txt( col->m_index, tag ? tag->get_name() : "" );
                            break;
                        }
                        // else no break
                    default:
                        line->set_value_txt( col->m_index, col->calculate_value_txt( elem ) );
                        break;
                }
            }

            return line;
        }

        const Filter*               m_filter_entry      { nullptr };
        const Filter*               m_filter_para       { nullptr };
        Ustring                     m_search_str;
        VectorTableColumns          m_columns;
        ListTableColumns            m_columns_sort;
        MapIdsTableColumns          m_columns_map;
        int                         m_grouping_depth    { 0 };
        int                         m_count_lines       { 0 };
        TableColumn*                m_p2col_gantt       { nullptr };
        DateV                       m_date_bgn          { 0 }; // begin date of gantt chart
        TableLine                   m_master_header     { this, 0, true };
        VectorTableLines            m_lines;      // basic cache of lines in sorted order
        bool                        m_F_para_based      { false };
        Diary*                      m_p2diary;
        bool                        m_F_under_construction  { false };

    friend class Table;
    friend class TableColumn;
    friend class TableLine;
    friend class TableSurface;
    friend class WidgetTable;
    friend class Chart;
    friend class ChartData;
    friend class WidgetChart;
};

// TABLE ELEM ======================================================================================
class TableElem : public StringDefElem
{
    public:
#ifndef __ANDROID__
        using GValue = Glib::Value< TableElem* >;
#endif

        static const Ustring DEFINITION_DEFAULT;
        static const Ustring DEFINITION_REPORT;

        TableElem( Diary* const diary, const Ustring& name, const Ustring& definition )
        : StringDefElem( diary, name, definition ) {}

        [[nodiscard]] DiaryElement::Type
        get_type() const override { return ET_TABLE; }

        [[nodiscard]] const R2Pixbuf&
        get_icon() const override;

        bool                    is_stock() const
        { return( get_id() == DEID::STOCK_TABLE ); }
};

using MapUstringTableElem = MapUstringDiaryElem< TableElem >;

} // end of namespace LoG

#endif
