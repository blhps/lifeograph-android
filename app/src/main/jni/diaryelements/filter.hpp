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


#ifndef LIFEOGRAPH_FILTERING_HEADER
#define LIFEOGRAPH_FILTERING_HEADER


#include <cassert>

#include "diarydata.hpp"
#include "entry.hpp"


namespace LoG
{

// FILTERED OBJECT CLASSES
namespace FOC
{
    static const int NOTHING        = 0x00000;
    static const int ENTRIES        = 0x00001;
    static const int PARAGRAPHS     = 0x00002;
    static const int DIARYELEMS     = 0x0000F;
    static const int NUMBERS        = 0x00010;
    static const int STRINGS        = 0x00020;
    static const int DATES          = 0x00040;
    static const int BASIC_DATA     = 0x0FFF0;
    static const int ALL_REAL       = 0x0FFFF;
    static const int SUBGROUPS      = 0x10000;
    static const int EVERYTHING     = 0xFFFFF;
    static const int FILTER_PANEL   = ENTRIES|SUBGROUPS;
}

// FORWARD DECLARATIONS
class FiltererContainer;
class FiltererContainerUI;
class Filter;

class Filterer
{
    public:
        Filterer( FiltererContainer* p2container )
        : m_p2container( p2container ) {}
        virtual ~Filterer() {}

        virtual bool            is_container() const    { return false; }
        bool                    is_not() const          { return m_F_not; }
        void                    set_not( bool F_not )   { m_F_not = F_not; }
        void                    toggle_not()            { m_F_not = !m_F_not; }

        virtual void            initialize_ui( FiltererContainerUI* ) = 0;

        virtual bool            filter( const Entry* ) const { return true; }
        virtual bool            filter( const Paragraph* ) const { return true; }
        virtual bool            filter( const DiaryElemTag* ) const { return true; }
        virtual bool            filter( const double ) const { return true; }
        virtual int             get_obj_classes() const = 0;
        virtual void            get_as_string( Ustring& ) const = 0;
        virtual Ustring         get_as_human_readable_str() const = 0;

        FiltererContainer*      m_p2container;

    // protected:
        bool                    m_F_not     { false };

};

class FiltererStatus : public Filterer
{
    public:
        static const ElemStatus STATUS_DEFAULT
                                { ES::SHOW_NOT_TODO|ES::SHOW_TODO|ES::SHOW_PROGRESSED };

        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererStatus( FiltererContainer* ctr, ElemStatus es = STATUS_DEFAULT )
        : Filterer( ctr ), m_included_statuses( es ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    //protected:
        ElemStatus              m_included_statuses;
};

class FiltererSize : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererSize( FiltererContainer* ctr, char type = VT::SO::CHAR_COUNT::C,
                      double range_b = -1, bool F_incl_b = false,
                      double range_e = -1, bool F_incl_e = false )
        : Filterer( ctr ), m_type( type ),
          m_range_b( range_b ), m_F_incl_b( F_incl_b ),
          m_range_e( range_e ), m_F_incl_e( F_incl_e ){}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    //protected:
        bool                    filter_v( int ) const;

        char                    m_type;
        int                     m_range_b;
        bool                    m_F_incl_b; // ( or [
        int                     m_range_e;
        bool                    m_F_incl_e; // ) or ]
};

class FiltererFavorite : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererFavorite( FiltererContainer* ctr )
        : Filterer( ctr ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
};

class FiltererTrashed : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererTrashed( FiltererContainer* ctr )
        : Filterer( ctr ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
};

class FiltererUnit : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererUnit( FiltererContainer* ctr, const Ustring& unit = "" )
        : Filterer( ctr ), m_unit( unit ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
        Ustring                 m_unit;
};

class FiltererIs : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererIs( FiltererContainer* ctr,
                    D::DEID id = DEID::UNSET,
                    int depth = VT::OP_DEPTH::DEFAULT::I );

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter_common( const DiaryElemTag* ) const;
        bool                    filter( const Entry* e ) const override
        { return filter_common( e ); }
        bool                    filter( const Paragraph* p ) const override
        { return filter_common( p ); }
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        DiaryElemTag*           m_p2tag       { nullptr };
        int                     m_depth       { VT::OP_DEPTH::DEFAULT::I };
};

class FiltererTaggedBy : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererTaggedBy( FiltererContainer* ctr, DiaryElemTag* tag = nullptr,
                          bool F_consider_parents = false )
        : Filterer( ctr ), m_tag( tag ), m_F_consider_parents( F_consider_parents ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        DiaryElemTag*           m_tag;
        bool                    m_F_consider_parents;
};

class FiltererSubtaggedBy : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererSubtaggedBy( FiltererContainer* ctr,
                             DiaryElemTag* tag_parent = nullptr, DiaryElemTag* tag_child = nullptr,
                             char rel = '=', int type = VT::LAST )
        : Filterer( ctr ), m_tag_parent( tag_parent ), m_tag_child( tag_child ),
                              m_relation( rel ), m_type( type ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        bool                    filter_common( DiaryElemTag* ) const;

        DiaryElemTag*           m_tag_parent  { nullptr };
        DiaryElemTag*           m_tag_child   { nullptr };
        char                    m_relation;
        int                     m_type;
};

class FiltererDefinesTag : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererDefinesTag( FiltererContainer* ctr )
        : Filterer( ctr ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::PARAGRAPHS };
};

class FiltererTagValue : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererTagValue( FiltererContainer* ctr, DiaryElemTag* tag = nullptr,
                          int type = VT::TVTS::REALIZED::I,
                          double range_b = Constants::INFINITY_MNS, bool F_incl_b = false,
                          double range_e = Constants::INFINITY_PLS, bool F_incl_e = false )
        : Filterer( ctr ), m_tag( tag ), m_type( type ),
          m_range_b( range_b ), m_F_incl_b( F_incl_b ),
          m_range_e( range_e ), m_F_incl_e( F_incl_e ){}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        bool                    filter_v( double ) const;

        DiaryElemTag*           m_tag         { nullptr };
        int                     m_type;
        double                  m_range_b;
        bool                    m_F_incl_b; // ( or [
        double                  m_range_e;
        bool                    m_F_incl_e; // ) or ]
};

class FiltererTheme : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererTheme( FiltererContainer* ctr, Theme* theme = nullptr )
        : Filterer( ctr ), m_theme( theme ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
        Theme*                  m_theme       { nullptr };
};

class FiltererBetweenDates : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererBetweenDates( FiltererContainer* ctr,
                              DateV date_b = Date::NOT_SET, bool f_incl_b = false,
                              DateV date_e = Date::NOT_SET, bool f_incl_e = false )
        : Filterer( ctr ), m_date_b( std::min( date_b, date_e ) ), m_F_incl_b( f_incl_b ),
                              m_date_e( std::max( date_b, date_e ) ), m_F_incl_e( f_incl_e ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS | FOC::DATES };

    // protected:
        bool                    filter_v( DateV ) const;

        DateV                   m_date_b;
        bool                    m_F_incl_b; // ( or [
        DateV                   m_date_e;
        bool                    m_F_incl_e; // ) or ]
};

class FiltererBetweenDateOffsets : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererBetweenDateOffsets( FiltererContainer* ctr, char date_type = VT::DT::START::C,
                                    int offset_bgn = 0, int offset_end = 5 )
        : Filterer( ctr ), m_date_type( date_type ),
                           m_offset_bgn( std::min( offset_bgn, offset_end ) ),
                           m_offset_end( std::max( offset_bgn, offset_end ) ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS | FOC::DATES };

    // protected:
        bool                    filter_v( DateV ) const;

        char                    m_date_type;
        int                     m_offset_bgn;
        int                     m_offset_end;
};

class FiltererBetweenEntries : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererBetweenEntries( FiltererContainer* ctr,
                                Entry* entry_b = nullptr, bool f_incl_b = false,
                                Entry* entry_e = nullptr, bool f_incl_e = false )
        : Filterer( ctr ), m_entry_b( entry_b ), m_F_incl_b( f_incl_b ),
                              m_entry_e( entry_e ), m_F_incl_e( f_incl_e ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
        Entry*                  m_entry_b;
        bool                    m_F_incl_b; // ( or [
        Entry*                  m_entry_e;
        bool                    m_F_incl_e; // ) or ]
};

class FiltererCompletion : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererCompletion( FiltererContainer* ctr,
                            double compl_b = 0.0, double compl_e = 100.0 )
        : Filterer( ctr ), m_compl_b( std::min( compl_b, compl_e ) ),
                              m_compl_e( std::max( compl_b, compl_e ) ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
        double                  m_compl_b;
        double                  m_compl_e;
};

class FiltererContainsText : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererContainsText( FiltererContainer* ctr,
                              const Ustring& text = "",
                              bool case_sensitive = false, bool use_regex = false,
                              bool name_only = false )
        : Filterer( ctr ), m_text( text ), m_case_sensitive( case_sensitive ),
          m_use_regex( use_regex ), m_name_only( name_only )
        { update_regex(); }

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        void                    update_regex();
        Ustring                 m_text;
        bool                    m_case_sensitive;
        bool                    m_use_regex;
        bool                    m_name_only;

        Glib::RefPtr< Glib::Regex >
                                m_regex;
};

class FiltererIsCurrentEntry : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererIsCurrentEntry( FiltererContainer* ctr, bool F_include_descendants = false )
        : Filterer( ctr ), m_F_include_descendants( F_include_descendants ) {}

        void                    initialize_ui( FiltererContainerUI* ) override { /*has no UI*/ }
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        bool                    m_F_include_descendants;
};

class FiltererChildFilter : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererChildFilter( FiltererContainer*, Filter* = nullptr );

        ~FiltererChildFilter();

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
        Filter*                 m_p2filter;
        FiltererContainer*      m_FC_stack    { nullptr };
};

class FiltererHasImage : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererHasImage( FiltererContainer* ctr )
        : Filterer( ctr ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::PARAGRAPHS };

    // protected:
};

class FiltererHasCoords : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererHasCoords( FiltererContainer* ctr )
        : Filterer( ctr ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS };

    // protected:
};

class FiltererTitleStyle : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererTitleStyle( FiltererContainer* ctr, char style = VT::ETS::NAME_ONLY::C )
        : Filterer( ctr ), m_title_style( style ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES };

    // protected:
        char                    m_title_style;
};

class FiltererHeadingLevel : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererHeadingLevel( FiltererContainer* ctr, String H_levels = "" )
        : Filterer( ctr ), m_H_levels( H_levels ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        bool                    accepts( char heading_type ) const
        { return( m_H_levels.find( heading_type ) != String::npos ); }

        const static int        m_obj_classes { FOC::PARAGRAPHS };

    // protected:
        String                  m_H_levels;
};

class FiltererListType : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererListType( FiltererContainer* ctr, int type = VT::PLS::PLAIN::I )
        : Filterer( ctr ), m_type( type ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::PARAGRAPHS };

    // protected:
        int                     m_type;
};

class FiltererIndentationLevel : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererIndentationLevel( FiltererContainer* ctr, char rel = '=', int indentation = 1 )
        : Filterer( ctr ), m_relation( rel ), m_indentation( indentation ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::PARAGRAPHS };

    // protected:
        char                    m_relation;
        int                     m_indentation;
};

class FiltererHasFormat : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererHasFormat( FiltererContainer* ctr, char format = VT::FMT::DEFAULT::C )
        : Filterer( ctr ), m_format( VT::get_v< VT::FMT, int, char >( format ) ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Paragraph* ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::PARAGRAPHS };

    // protected:
        int                     m_format;
};

// BASIC FILTERERS =================================================================================
class FiltererEquals : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererEquals( FiltererContainer* ctr, char relation = '=', double value = 0.0 )
        : Filterer( ctr ), m_relation( relation ), m_value( value ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const double ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::NUMBERS };

    // protected:
        char                    m_relation;
        double                  m_value;
};

// SCRIPT FILTERER =================================================================================
class FiltererScript : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }

        FiltererScript( FiltererContainer* ctr, const String& script = "" )
        : Filterer( ctr ), m_script( script ) {}

        void                    initialize_ui( FiltererContainerUI* ) override;
        bool                    filter( const Entry* ) const override;
        bool                    filter( const Paragraph* ) const override;
        bool                    filter( const double ) const override;
        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;

        const static int        m_obj_classes { FOC::ENTRIES | FOC::PARAGRAPHS | FOC::NUMBERS };

    // protected:
        String                  m_script;
};

typedef std::vector< Filterer* > VecFilterers;

// FILTERER CONTAINER ==============================================================================
class FiltererContainer final : public Filterer
{
    public:
        int                     get_obj_classes() const override { return m_obj_classes; }
        // this is used by parent dialogs:
        int                     calculate_obj_classes() const
        {
            int obj_classes { 0 };

            for( auto& filterer : m_pipeline )
                obj_classes |= filterer->get_obj_classes();

            // return obj_classes;
            return( obj_classes & FOC::ALL_REAL );
        }

        FiltererContainer( FiltererContainer* ctr )
        : Filterer( ctr ), m_p2diary( ctr->m_p2diary ), m_F_or( !ctr->m_F_or ) {}

        FiltererContainer( Diary* p2diary, const Ustring& def )
        : Filterer( nullptr ), m_p2diary( p2diary )
        { set_from_string( def ); }

        ~FiltererContainer()
        { clear_pipeline(); }

        void                    clear_pipeline();

        void                    initialize_ui( FiltererContainerUI* ) override;

        bool                    filter( const Entry* e ) const override
        { return filter_internal( e ); }
        bool                    filter( const Paragraph* p ) const override
        { return filter_internal( p ); }
        bool                    filter( const DiaryElemTag* t ) const override
        { return filter_internal( t ); }
        bool                    filter( const double d ) const override
        { return filter_internal( d ); }

        bool                    is_container() const override { return true; }
        bool                    is_or() const
        { return m_F_or; }

        void                    get_as_string( Ustring& ) const override;
        Ustring                 get_as_human_readable_str() const override;
        void                    set_from_string( const Ustring& );

        void                    toggle_logic()
        {
            m_F_or = !m_F_or;
            // recursion
            for( auto& filterer : m_pipeline )
            if( filterer->is_container() )
                dynamic_cast< FiltererContainer* >( filterer )->toggle_logic();
        }

        template< class T, typename... Args >
        T*                      add( Args... args )
        {
            T* filterer { new T( this, args... ) };
            m_pipeline.push_back( filterer );
            return filterer;
        }
        void                    remove_filterer( Filterer* );

        void                    set_last_filterer_not()
        { m_pipeline.back()->set_not( true ); }

        const static int        m_obj_classes { FOC::EVERYTHING };

    // protected:
        template< typename T >
        bool                    filter_internal( const T item ) const
        {
            if( m_pipeline.empty() )
                return true;

            for( auto& filterer : m_pipeline )
            {
                if( filterer->filter( item ) != filterer->is_not() )
                {
                    if( m_F_or )
                        return true;

                }
                else
                if( ! m_F_or )
                    return false;
            }

            return( ! m_F_or );
        }

        Diary*                  m_p2diary   { nullptr };

        VecFilterers            m_pipeline;

        bool                    m_F_or;

    friend class Filter;
};

class Filter : public StringDefElem
{
    public:
        static const Ustring    DEFINITION_DEFAULT;
        static const Ustring    DEFINITION_MINIMAL;
        static const Ustring    DEFINITION_CUR_ENTRY;
        static const Ustring    DEFINITION_NONTRASHED;
        static const Ustring    DEFINITION_TRASHED;

        Filter( Diary* const diary, const Ustring& name, const Ustring& definition )
        : StringDefElem( diary, name, definition ) {}

        DiaryElement::Type      get_type() const override
        { return ET_FILTER; }

        const R2Pixbuf&         get_icon() const override;

        FiltererContainer*      get_filterer_stack() const;

        bool                    can_filter_class( int ) const;

        SKVVec                  get_as_skvvec() const override;

        int                     m_num_users{ 0 };
};

using MapUstringFilter = MapUstringDiaryElem< Filter >;

} // end of namespace LoG

#endif
