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


#ifndef LIFEOGRAPH_ENTRY_PARSER_HEADER
#define LIFEOGRAPH_ENTRY_PARSER_HEADER


#include <mutex>

#ifndef __ANDROID__
#include <enchant.h>
#include <gtkmm.h>
#else
#include "../android_shim.hpp"
#endif

#include <deque>

#include "../helpers.hpp"
#include "../diaryelements/entry.hpp"


namespace LoG
{

using namespace HELPERS;

typedef unsigned int CharClass;

static const CharClass
    // INDIVIDUAL CHARS
    Ch_NOT_SET          = 0,
    Ch_NEWLINE          = 0x2,
    Ch_SPACE            = 0x8,          // space that will come eventually
    Ch_TAB              = 0x10,

    Ch_ASTERISK         = 0x40,         // *
    Ch_UNDERSCORE       = 0x41,         // _
    Ch_EQUALS           = 0x42,         // =
    Ch_HASH             = 0x43,         // #

    Ch_SLASH            = 0x60,         // /
    Ch_X                = 0x61,         // xX
    Ch_DASH             = 0x62,         // -
    Ch_PLUS             = 0x63,         // +
    Ch_TILDE            = 0x64,         // ~
    Ch_AT               = 0x65,         // @

    Ch_DOT              = 0x80,         // .
    Ch_COLON            = 0x81,         // :
    Ch_COMMA            = 0x83,         // ,

    Ch_LESS             = 0x90,         // <
    Ch_MORE             = 0x91,         // >
    Ch_SBB              = 0x92,         // [
    Ch_SBE              = 0x93,         // ]

    Ch_PIPE             = 0x94,         // |

    // FAMILIES (every char is defined by a Ch+CF but do not combine with Ch in recipes)
    CF_NOTHING          = 0x200,
    CF_ALPHA            = 0x400,
    CF_DIGIT            = 0x800,
    CF_PUNCTUATION      = 0x1000,
    CF_SPACE            = 0x2000,
    CF_TAB              = 0x4000,
    CF_NEWLINE          = 0x8000,
    CF_IDENTIFIER       = 0x10000, // markup chars that are acceptable in emails

    // ADDITIONAL FAMILIES
    CF_NUMERIC          = 0x20000,      // digits, dot, comma, and space (per ISO 31-0)
    CF_SIGN             = 0x40000,      // + or -
    CF_DATE_SEPARATOR   = 0x80000,
    CF_TODO_STATUS      = 0x100000,
    CF_MARKUP           = 0x200000,
    CF_SPELLCHECK       = 0x400000,
    CF_VALUE_SEPARATOR  = 0x800000,
    CF_PARENTHESIS      = 0x2000000,
    CF_SPACE_CONDTNL    = 0x4000000,    // accept spaces based on recipe state
                                        // please note that its effect is reversed when used
                                        // together with CF_SPACE

    // MODIFIERS
    CM_MULTIPLE         = 0x20000000,   // char can occur multiple times
    CM_OPTIONAL         = 0x40000000,   // char may or may not occur

    // MULTIPLE FAMILY COMBINATIONS (Do not use for an individual char)
    CFC_BLANK           = CF_NOTHING|CF_SPACE|CF_TAB|CF_NEWLINE,
    CFC_NONSPACE        = CF_ALPHA|CF_DIGIT|CF_PUNCTUATION,
    CFC_EMAIL           = CF_ALPHA|CF_DIGIT|CF_IDENTIFIER,
    CFC_ANY             = CF_ALPHA|CF_DIGIT|CF_PUNCTUATION|CF_SPACE|CF_TAB|CF_NEWLINE,
    CFC_ANY_BUT_NEWLINE = CF_ALPHA|CF_DIGIT|CF_PUNCTUATION|CF_SPACE|CF_TAB,
    CFC_ANY_BUT_NUMERIC = CF_ALPHA|CF_TAB|CF_NEWLINE|CF_PUNCTUATION,

    // MASKS
    CFC_CHAR_MASK       = 0xFF,
    CFC_FAMILY_MASK     = 0x1FFFFF00;   // EXCLUDING Chs and CMs


class ParserBase
{
    public:
        typedef void ( ParserBase::*FPtr_void )();

        struct AbsChar  // abstract char
        {
            AbsChar( CharClass c, CharClass e, FPtr_void a )
            : flags( c ), exception( e ), applier( a ) {}
            AbsChar( CharClass c, FPtr_void a ) : flags( c ), applier( a ) {}
            AbsChar( CharClass c ) : flags( c ) {}

            CharClass       flags;
            CharClass       exception{ Ch_NOT_SET }; // only Ch_s are allowed here
            FPtr_void       applier{ nullptr };
        };

        class Recipe
        {
            public:
                typedef std::vector< AbsChar > Contents;
                typedef unsigned int Id;
                typedef unsigned int State;
                static const State
                        RS_NOT_SET = 0x1, RS_IN_PROGRESS = 0x2, RS_REJECTED = 0x4,
                        RS_ACCEPTED = 0x8, RS_FINISHED = 0x10, RS_BLOCK = 0x1000;

                static bool                 cmp_chars( CharClass set, CharClass member )
                {
                    // when set does not care about a specific char just check for family...
                    // please note that a set must look for either the family or a specific...
                    // char but not both at the same time
                    if( not( set & CFC_CHAR_MASK ) )
                        return( set & member );
                    else
                        return( ( set & CFC_CHAR_MASK ) == ( member & CFC_CHAR_MASK ) );
                }

                // for creating main recipes
                Recipe( Id id, ParserBase* parent, const Contents* c, Id blocks, Id blocks_new )
                : m_id( id ), m_parent( parent ), m_contents( c ),
                  m_blocks( blocks ), m_blocks_new( blocks_new ) { }

                // for copying main recipes to active
                Recipe( const Recipe* oth )
                : m_id( oth->m_id ), m_parent( oth->m_parent ),
                  m_contents( oth->m_contents ),
                  m_blocks( oth->m_blocks ), m_blocks_new( oth->m_blocks_new ),
                  m_index( oth->m_index ), m_pos_bgn( oth->m_pos_bgn ),
                  m_pos_mid( oth->m_pos_mid ), m_int_value( oth->m_int_value ),
                  m_F_accept_spaces( oth->m_F_accept_spaces ),
                  m_state( oth->m_state ) { }

                // for 2nd part recipes
                Recipe( Id id, ParserBase* parent, const Contents* c, Id b,
                        Ustring::size_type ps, Ustring::size_type pm )
                : m_id( id ), m_parent( parent ), m_contents( c ),
                  m_blocks( b ), m_index( 0 ), m_pos_bgn( ps ), m_pos_mid( pm ),
                  m_int_value( m_parent->m_int_last ) { }

                State                       process_char();
                CharClass                   get_char_class_at( int i ) const
                {
                    CharClass cc{ m_contents->at( i ).flags };
                    if( ( cc & CF_SPACE_CONDTNL ) )
                    {
                        const bool flag_accept_spaces_effective{ ( cc & CF_SPACE ) ?
                                                                 !m_F_accept_spaces :
                                                                 m_F_accept_spaces };
                        if( flag_accept_spaces_effective )
                            return( cc|CF_SPACE );
                        else
                            return( cc & ( ~CF_SPACE ) );
                    }
                    else
                        return cc;
                }

            //private:
                Id                          m_id{ 0 };
                ParserBase* const           m_parent;
                const Contents*             m_contents;
                Id                          m_blocks;           // blocks retroactively
                Id                          m_blocks_new{ 0 };  // blocks addition of new
                unsigned int                m_index{ 0 };
                UstringSize                 m_pos_bgn{ Ustring::npos };
                UstringSize                 m_pos_mid{ 0 };  // when needed
                unsigned int                m_int_value{ 0 };   // when needed
                bool                        m_F_accept_spaces{ false };
                bool                        m_F_multiple_char_matched{ false };
                State                       m_state{ RS_NOT_SET };

        };
        typedef std::set< Recipe* > RecipeSet;
        typedef std::list< Recipe* > RecipeList;

        static const Recipe::Id RID_HEADING         = 0x1;
        static const Recipe::Id RID_COMMENT         = 0x800;
        static const Recipe::Id RID_IGNORE          = 0x1000;
        static const Recipe::Id RID_DATE            = 0x4000;
        static const Recipe::Id RID_COLON           = 0x40000;
        static const Recipe::Id RID_LINK_AT         = 0x80000;
        static const Recipe::Id RID_LINK_NAME       = 0x100000;
        static const Recipe::Id RID_GENERIC         = 0x200000;
        static const Recipe::Id RID_TAG             = 0x400000;
        static const Recipe::Id RID_TIME            = 0x800000;

        static const Recipe::Id RID_URI             = 0x1000000;    // only in m_link_type_last

        static const Recipe::Id RID_CUSTOM          = 0x80000000;
        static const Recipe::Id RID_ALL             = 0xFFFFFFFF;

        struct LinkProtocol
        {
            LinkProtocol( Recipe::Id t, const Recipe::Contents* r ) : type( t ), rc( r ) {}

            Recipe::Id              type;
            const Recipe::Contents* rc;
        };
        using MapLinkProtocols = std::unordered_map< std::string, LinkProtocol* >;

                                    ParserBase();
        virtual                     ~ParserBase();

        void                        parse( const UstringSize, const UstringSize );

        // EXTENDIBILITY
        void                        add_link_protocol( const std::string&,
                                                       Recipe::Id,
                                                       Recipe::Contents*);
        virtual void                junction_link();
        virtual void                junction_markup() {}
        virtual void                junction_markup2() {}
        virtual void                apply_bold() { }
        virtual void                apply_italic() { }
        virtual void                apply_strikethrough() { }
        virtual void                apply_highlight() { }

        virtual void                apply_custom1() { } // for addt'l recipes in derived classes

        // HELPERS
        void                        set_start()
        { m_recipe_cur->m_pos_bgn = m_parser_pos_cur; }
        void                        set_start_for_multi();
        void                        set_middle()
        { m_recipe_cur->m_pos_mid = m_parser_pos_cur; }
        void                        add_block()
        { m_recipe_cur->m_state |= Recipe::RS_BLOCK; }

        virtual void                apply_link_old() { }    // deprecated
        virtual void                apply_inline_tag_old() { }  // deprecated
        virtual void                junction_tag() { }      // deprecated
        virtual void                junction_tag2() { }     // deprecated
        virtual void                junction_number();

        EnchantDict*                get_dict( const String& lang )
        {
            auto kv { m_enchant_dicts.find( lang ) };
            return ( kv != m_enchant_dicts.end() ? kv->second : nullptr );
        }

        bool                        m_F_spellchk_enabled { false };

    protected:
        virtual Wchar               get_char_at( int ) = 0;

        void                        process_char();
        virtual void                process_paragraph() { }
        void                        process_number();

        // JUNCTIONS & CHECKS
        void                        check_date();
        void                        check_time();
        void                        check_inline_tag_value_start();
        void                        check_inline_tag_value_date();
        void                        junction_date_dotym();   // dot between year and month
        void                        junction_date_dotmd();   // dot between month and day
        void                        junction_time_hm();      // : between hours and minutes
        void                        junction_time_ms();      // : between minutes and seconds
        void                        junction_colon();
        void                        junction_at();
        void                        junction_tag_value_sep();

        // APPLIERS (TO BE OVERRIDEN)
        virtual void                apply_comment() { }
        virtual void                apply_time() { }
        virtual void                apply_link() { }

        virtual void                apply_inline_tag_value_nmbr() { }
        virtual void                apply_inline_tag_value_date() { }

        virtual void                reset( UstringSize, UstringSize );

        bool                        is_pos_cur_in_spellchk_zone();

        UstringSize                 m_pos_end{ 0 };         // position of last char (constant)
        UstringSize                 m_parser_pos_para_bgn{ 0 };    // position of curr para's begin
        UstringSize                 m_parser_pos_cur{ 0 };
        UstringSize                 m_parser_pos_in_para{ 0 }; // = pos_cur - pos_para_bgn
        UstringSize                 m_parser_pos_extra_1{ 0 }; // for storing exta positions
        UstringSize                 m_parser_pos_extra_2{ 0 }; // for storing exta positions
        UstringSize                 m_parser_pos_blank{ 0 };
        UstringSize                 m_parser_pos_last_digit{ 0 };
        int                         m_pos_search{ 0 };
        std::deque< std::pair< unsigned, unsigned > >
                                    m_spellcheck_exceptions;
        std::map< String, EnchantDict* >
                                    m_enchant_dicts;
        bool                        m_F_spellcheck_exceptions_initialized{ false };
        std::atomic< bool >         m_F_stop { false };

        // from new to old: curr, last
        CharClass                   m_cf_curr{ Ch_NOT_SET };
        CharClass                   m_cf_last{ Ch_NOT_SET };

        Wchar                       m_char_cur{ 0 };
        Wchar                       m_char_last{ 0 };

        Ustring                     m_word_cur;    // last word consisting purely of letters

        Paragraph*                  m_parser_p2para_cur{ nullptr };
        HiddenFormat*               m_p2format_tag_cur{ nullptr };

        MapLinkProtocols            m_link_protocols;
        Recipe*                     m_recipe_cur{ nullptr };
        unsigned int                m_word_count{ 0 };
        unsigned int                m_int_last{ 0 };
        DateV                       m_date_last{ 0 };

        RecipeSet                   m_all_recipes;
        RecipeList                  m_active_recipes;
        Recipe::Id                  m_blocked_flags{ 0 };
        Recipe::Id                  m_blocked_flags_new{ 0 };
        // i.e. blocked from being added as a new recipe but not removed from active recipes

        const static Recipe::Contents   m_rc_comment;
        const static Recipe::Contents   m_rc_date;
        const static Recipe::Contents   m_rc_time;
        const static Recipe::Contents   m_rc_link_file;
        const static Recipe::Contents   m_rc_link_email;
        const static Recipe::Contents   m_rc_link_geo;
        //const static Recipe::Contents   m_rc_link_id;
        const static Recipe::Contents   m_rc_colon;
        const static Recipe::Contents   m_rc_at_email;
        const static Recipe::Contents   m_rc_tag_value_nmbr;
        const static Recipe::Contents   m_rc_tag_value_date;

    friend class TextbufferDiaryEdit;
    friend class TextviewDiaryEdit;
};

// TEXT PARSER FOR ENTRY OBJECT ====================================================================
// This is to be used when parsing is done without a GUI editor e.g. while printing
class EntryParser : public ParserBase
{
    public:
        void                        set_entry( const Entry* ptr2entry )
        { m_ptr2entry = ptr2entry; }

        void                        reparse()
        { if( m_ptr2entry ) parse( 0, m_ptr2entry->get_size() ); }

        Wchar                       get_char_at( int i ) override
        { return m_ptr2entry->get_text().at( i ); }

        Ustring                     get_substr( UstringSize begin, UstringSize end )
        { return m_ptr2entry->get_text().substr( begin, end - begin ); }

        UstringSize                 get_para_end( UstringSize begin )
        {
            auto end{ m_ptr2entry->get_text().find_first_of( '\n', begin ) };
            return( end == Ustring::npos ? end = m_ptr2entry->get_text().length() : end );
        }

    protected:
        const Entry*                m_ptr2entry{ nullptr };
};

class ParserString : public ParserBase
{
    public:
        void                        set_text( const Ustring& text )
        {
            std::lock_guard< std::mutex > lock( m_mutex );
            m_text = text;
        }

        void                        parse( Glib::Dispatcher& dispatcher )
        {
            std::lock_guard< std::mutex > lock( m_mutex );
            m_F_stop = false;
            ParserBase::parse( 0, m_text.length() );

            if( !m_F_stop ) // do not emit signal when stopped forcefully
                dispatcher.emit();
        }
        void                        stop()
        {
            m_F_stop = true;
            std::lock_guard< std::mutex > lock( m_mutex ); // wait until it actually stops
        }

        unsigned int                get_word_count() const
        {
            std::lock_guard< std::mutex > lock( m_mutex );
            return m_word_count;
        }

    protected:
        Wchar                       get_char_at( int i ) override
        {
            return m_text[ i ];
        }

        Ustring                     m_text;

        mutable std::mutex          m_mutex;
};

}   // end of namespace LoG

#endif
