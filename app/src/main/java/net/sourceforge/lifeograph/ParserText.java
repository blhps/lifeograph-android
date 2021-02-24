/* *********************************************************************************

    Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)

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

package net.sourceforge.lifeograph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import androidx.annotation.NonNull;

public abstract class ParserText
{
    public static final int
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

    // FAMILIES (every char is defined by a Ch+CF but do not combine with Ch in recipes)
    CF_NOTHING          = 0x800,
    CF_ALPHA            = 0x1000,
    CF_DIGIT            = 0x2000,
    CF_PUNCTUATION      = 0x4000,
    CF_SPACE            = 0x8000,
    CF_TAB              = 0x10000,
    CF_NEWLINE          = 0x20000,

    // ADDITIONAL FAMILIES
    CF_NUMERIC          = 0x40000,      // digits, minus, dot, comma, and space (per ISO 31-0)
    CF_DATE_SEPARATOR   = 0x80000,
    CF_TODO_STATUS      = 0x100000,
    CF_MARKUP           = 0x200000,
    CF_SPELLCHECK       = 0x400000,
    CF_VALUE_SEPARATOR  = 0x800000,
    CF_NONNUMERIC_PUNCT = 0x1000000,
    CF_PARENTHESIS      = 0x2000000,
    CF_SPACE_CONDTNL    = 0x4000000,    // accept spaces based on recipe state
    // please note that its effect is reversed when used
    // together with CF_SPACE

    // MODIFIERS
    CM_MULTIPLE         = 0x20000000,   // char can occur multiple times

    // MULTIPLE FAMILY COMBINATIONS (Do not use for an individual char)
    CFC_BLANK           = CF_NOTHING|CF_SPACE|CF_TAB|CF_NEWLINE,
    CFC_NONSPACE        = CF_ALPHA|CF_DIGIT|CF_PUNCTUATION,
    CFC_ANY_BUT_NEWLINE = ( 0xFFFFF800 & ( ~CF_NEWLINE ) ),
    CFC_ANY             = CF_ALPHA|CF_DIGIT|CF_PUNCTUATION|CF_SPACE|CF_TAB|CF_NEWLINE,
    CFC_ANY_BUT_NUMERIC = CF_ALPHA|CF_TAB|CF_NEWLINE|CF_NONNUMERIC_PUNCT,

    // MASKS
    CFC_CHAR_MASK       = 0x7FF,
    CFC_FAMILY_MASK     = ( 0xFFFFF800 & ( ~CM_MULTIPLE ) );

    private static class AbsChar  // abstract char
    {
        AbsChar( int f, ParSel a, boolean j ) {
            flags = f;
            applier = a;
            junction = j;
        }
        AbsChar( int f, ParSel a ) {
            flags = f;
            applier = a;
            junction = false;
        }
        AbsChar( int f ) {
            flags = f;
            applier = ParSel.NULL;
            junction = false;
        }
        int     flags;
        ParSel  applier;
        boolean junction;
    }

    static class Recipe
    {
        /*typedef std::vector< AbsChar > Contents;
        typedef unsigned int Id;
        typedef unsigned int State;*/
        static final int
            RS_NOT_SET = 0x1, RS_IN_PROGRESS = 0x2, RS_REJECTED = 0x4,
            RS_ACCEPTED = 0x8, RS_BLOCK = 0x1000;

        static boolean
        cmp_chars( int set, int member )
        {
            // when set does not care about a specific char just check for family...
            // please note that a set must look for either the family or a specific...
            // char but not both at the same time
            if( ( set & CFC_CHAR_MASK ) == 0 )
                return( set & member ) != 0;
            else
                return( ( set & CFC_CHAR_MASK ) == ( member & CFC_CHAR_MASK ) );
        }

        // for creating main recipes
        Recipe( int id, ParserText parent, List< AbsChar > c, int b ) {
            m_id = id;
            m_parent = parent;
            m_contents = c;
            m_blocks = b;
        }

        // for copying main recipes to active
        Recipe( Recipe oth ) {
            m_id = oth.m_id;
            m_parent = oth.m_parent;
            m_contents = oth.m_contents;
            m_blocks = oth.m_blocks;
            m_index = oth.m_index;
            m_pos_bgn = oth.m_pos_bgn;
            m_pos_mid = oth.m_pos_mid;
            m_int_value = oth.m_int_value;
            m_flag_accept_spaces = oth.m_flag_accept_spaces;
            m_state = oth.m_state;
        }

        // for 2nd part recipes
        Recipe( int id, ParserText parent, List< AbsChar > c, int b, int ps, int pm ) {
            m_id = id; m_parent = parent;
            m_contents = c;
            m_blocks = b;
            m_index = 0;
            m_pos_bgn = ps;
            m_pos_mid = pm;
            m_int_value = m_parent.m_int_last;
        }

        int
        process_char(){
            if( ( m_parent.m_blocked_flags & m_id ) != 0 )
                return( m_state = RS_REJECTED );

            if( cmp_chars( get_char_class_at( m_index ), m_parent.m_cf_curr ) ) {
                if( m_contents.get( m_index ).applier != ParSel.NULL ) {
                    m_parent.m_recipe_cur = this;
                    // applier may set a value for m_state:
                    m_parent.selectParsingFunc( m_contents.get( m_index ).applier );
                }

                if( ( m_state & RS_IN_PROGRESS ) == 0 )
                    m_state = ( m_state & RS_BLOCK ) | RS_IN_PROGRESS;

                m_index++;
            }
            else
            if( m_index == 0 ||
                ( m_contents.get( m_index - 1 ).flags & CM_MULTIPLE ) == 0 ||
                !cmp_chars( get_char_class_at( m_index - 1 ), m_parent.m_cf_curr ) )
                m_state = RS_REJECTED;
            else
            if( m_contents.get( m_index - 1 ).applier != ParSel.NULL ) {
                // multiply occurring chars can have appliers, too
                m_parent.m_recipe_cur = this;
                // applier may set a value for m_state:
                m_parent.selectParsingFunc( m_contents.get( m_index- 1 ).applier );
            }

            if( m_index == m_contents.size() )
                m_state = ( m_state & RS_BLOCK ) | RS_ACCEPTED;

            return m_state;
        }

        // CharClass = int in Java
        int
        get_char_class_at( int i ) {
            int cc = m_contents.get( i ).flags;
            if( ( cc & CF_SPACE_CONDTNL ) != 0 ) {
                final boolean flag_accept_spaces_effective =
                        ( ( cc & CF_SPACE ) == 0 ) == m_flag_accept_spaces;
                if( flag_accept_spaces_effective )
                    return( cc|CF_SPACE );
                else
                    return( cc & ( ~CF_SPACE ) );
            }
            else
                return cc;
        }

        int                         m_id;
        ParserText                  m_parent;
        List< AbsChar >             m_contents;
        int                         m_blocks;
        int                         m_index = 0;
        int                         m_pos_bgn = 0;
        int                         m_pos_mid = 0;  // when needed
        int                         m_int_value = 0;   // when needed
        boolean                     m_flag_accept_spaces = false;
        int                         m_state = RS_NOT_SET;
    }

    static class LinkProtocol
    {
        LinkProtocol( int t, List< AbsChar > r ) {
            type = t;
            rc = r;
        }

        int             type;
        List< AbsChar > rc;
    }

    static final int RID_HEADING         = 0x1;
    static final int RID_SUBHEADING      = 0x2;
    static final int RID_MARKUP          = 0x4;
    static final int RID_BOLD            = 0x8;
    static final int RID_ITALIC          = 0x10;
    static final int RID_HIGHLIGHT       = 0x20;
    static final int RID_STRIKETHROUGH   = 0x40;
    static final int RID_MARKUP_B_END    = 0x80;
    static final int RID_MARKUP_I_END    = 0x100;
    static final int RID_MARKUP_H_END    = 0x200;
    static final int RID_MARKUP_S_END    = 0x400;
    static final int RID_COMMENT         = 0x800;
    static final int RID_IGNORE          = 0x1000;
    static final int RID_TODO            = 0x2000;
    static final int RID_DATE            = 0x4000;
    static final int RID_COLON           = 0x40000;
    static final int RID_LINK_AT         = 0x80000;
    static final int RID_GENERIC         = 0x100000;
    static final int RID_TAG             = 0x200000;

    static final int RID_URI             = 0x1000000;    // only in m_link_type_last
    static final int RID_ID              = 0x1000001;    // only in m_link_type_last
// LATER   static final int RID_CHART           = 0x1000002;

    static final int RID_CUSTOM          = 0x80000000;
    static final int RID_ALL             = 0xFFFFFFFF;

    // RECIPES =====================================================================================
    final static List< AbsChar > m_rc_subheading = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_NEWLINE ),
            new AbsChar( Ch_SPACE, ParSel.ST_STRT ),
            new AbsChar( CFC_ANY_BUT_NEWLINE, ParSel.JK_SUBH ) ) );

    final static List< AbsChar > m_rc_markup = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_BLANK|CF_PARENTHESIS ),
            new AbsChar( CF_MARKUP, ParSel.JK_MKUP ) ) );

    final static List< AbsChar > m_rc_markup_b_end = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, ParSel.JK_MKP2 ),
            new AbsChar( Ch_ASTERISK, ParSel.AP_BOLD ) ) );

    final static List< AbsChar > m_rc_markup_i_end = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, ParSel.JK_MKP2 ),
            new AbsChar( Ch_UNDERSCORE, ParSel.AP_ITLC ) ) );

    final static List< AbsChar > m_rc_markup_h_end = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, ParSel.JK_MKP2 ),
            new AbsChar( Ch_HASH, ParSel.AP_HILT ) ) );

    final static List< AbsChar > m_rc_markup_s_end = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, ParSel.JK_MKP2 ),
            new AbsChar( Ch_EQUALS, ParSel.AP_STRK ) ) );

    final static List< AbsChar > m_rc_comment = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_SBB, ParSel.ST_STRT ),
            new AbsChar( Ch_SBB ),
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE ),
            new AbsChar( Ch_SBE ),
            new AbsChar( Ch_SBE, ParSel.AP_CMNT ) ) );

    final static List< AbsChar > m_rc_ignore = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_NEWLINE ),
            new AbsChar( Ch_DOT, ParSel.ST_STRT ),
            new AbsChar( Ch_TAB ),
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE, ParSel.AD_BLCK ),
            new AbsChar( Ch_NEWLINE, ParSel.AP_IGNR) ) );

    final static List< AbsChar > m_rc_todo = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_NEWLINE ),
            new AbsChar( Ch_TAB|CM_MULTIPLE ),
            new AbsChar( Ch_SBB, ParSel.ST_STRT ),
            new AbsChar( CF_TODO_STATUS ),
            new AbsChar( Ch_SBE ),
            new AbsChar( Ch_SPACE, ParSel.JK_TODO ) ) );

    final static List< AbsChar > m_rc_indent = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_NEWLINE, ParSel.ST_STRT ),
            new AbsChar( Ch_TAB|CM_MULTIPLE ),
            new AbsChar( CFC_NONSPACE, ParSel.AP_IDNT ) ) );

    final static List< AbsChar > m_rc_tag = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_BLANK|CF_PARENTHESIS ),
            new AbsChar( Ch_COLON, ParSel.ST_STRT ),
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE ),
            new AbsChar( Ch_COLON ),
            new AbsChar( CFC_ANY, ParSel.JK_ITAG ), // equal sign
            new AbsChar( CF_NUMERIC|CM_MULTIPLE, ParSel.JK_NMBR ),
            new AbsChar( CFC_ANY_BUT_NUMERIC|CF_VALUE_SEPARATOR, ParSel.JK_ITG2 ), // slash
            new AbsChar( CF_NUMERIC|CM_MULTIPLE, ParSel.JK_NMBR ),
            new AbsChar( CFC_ANY_BUT_NUMERIC, ParSel.AP_ITAG ) ) );

// LINK
    final static List< AbsChar > m_rc_date = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_BLANK|CF_PUNCTUATION ),
            new AbsChar( CF_DIGIT, ParSel.ST_STRT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DATE_SEPARATOR, ParSel.JK_DDYM ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DATE_SEPARATOR, ParSel.JK_DDMD ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT, ParSel.CH_DATE ) ) );

    final static List< AbsChar > m_rc_colon = new ArrayList<>( Collections.singletonList(
            new AbsChar( Ch_COLON, ParSel.JK_COLN ) ) );

    final static List< AbsChar > m_rc_at_email = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_AT, ParSel.JK_ATSG ),
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( Ch_DOT ),
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( CFC_BLANK, ParSel.AP_LINK ) ) );

    final static List< AbsChar > m_rc_link_file = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_SLASH ),
            new AbsChar( Ch_SLASH ),
            new AbsChar( CF_SPACE_CONDTNL|CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( CF_SPACE_CONDTNL|CFC_BLANK, ParSel.JK_LINK ) ) );

    final static List< AbsChar > m_rc_link_email = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( Ch_AT ),
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( Ch_DOT ),
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( CFC_BLANK, ParSel.JK_LINK ) ) );

    final static List< AbsChar > m_rc_link_geo = new ArrayList<>( Arrays.asList(
            new AbsChar( CFC_NONSPACE|CM_MULTIPLE ),
            new AbsChar( CFC_BLANK, ParSel.JK_LINK ) ) );

    final static List< AbsChar > m_rc_link_id = new ArrayList<>( Arrays.asList(
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT ),
            new AbsChar( CF_DIGIT|CM_MULTIPLE ),
            new AbsChar( CFC_BLANK, ParSel.JK_LINK ) ) );

//    final static List< AbsChar > m_rc_chart = new ArrayList<>( Arrays.asList(
//            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE ),
//            new AbsChar( Ch_NEWLINE, ParSel.AP_CHRT ) ) );

    final static List< AbsChar > m_rc_link_hidden_end = new ArrayList<>( Arrays.asList(
            new AbsChar( Ch_TAB, ParSel.ST_MIDL ),
            new AbsChar( CFC_ANY_BUT_NEWLINE|CM_MULTIPLE ),
            new AbsChar( Ch_MORE, ParSel.AP_LNHD ) ) );

    // METHODS =====================================================================================
    ParserText() {
        m_all_recipes.add( new Recipe( RID_SUBHEADING, this, m_rc_subheading, 0 ) );
        m_all_recipes.add( new Recipe( RID_MARKUP, this, m_rc_markup, 0 ) );
        m_all_recipes.add( new Recipe( RID_COMMENT, this, m_rc_comment, 0 ) );
        m_all_recipes.add( new Recipe( RID_IGNORE, this, m_rc_ignore, RID_ALL ) );
        m_all_recipes.add( new Recipe( RID_TODO, this, m_rc_todo, 0 ) );
        m_all_recipes.add( new Recipe( RID_DATE, this, m_rc_date, 0 ) );
        m_all_recipes.add( new Recipe( RID_COLON, this, m_rc_colon, 0 ) );
        m_all_recipes.add( new Recipe( RID_LINK_AT, this, m_rc_at_email, 0 ) );
        m_all_recipes.add( new Recipe( RID_GENERIC, this, m_rc_indent, 0 ) );
        m_all_recipes.add( new Recipe( RID_GENERIC, this, m_rc_tag, 0 ) );

        m_link_protocols.put( "deid", new LinkProtocol( RID_ID, m_rc_link_id ) );
        m_link_protocols.put( "file", new LinkProtocol( RID_URI, m_rc_link_file ) );
        m_link_protocols.put( "ftp", new LinkProtocol( RID_URI, m_rc_link_file ) );
        m_link_protocols.put( "geo", new LinkProtocol( RID_URI, m_rc_link_geo ) );
        m_link_protocols.put( "http", new LinkProtocol( RID_URI, m_rc_link_file ) );
        m_link_protocols.put( "https", new LinkProtocol( RID_URI, m_rc_link_file ) );
        m_link_protocols.put( "mailto", new LinkProtocol( RID_URI, m_rc_link_email ) );
        m_link_protocols.put( "rel", new LinkProtocol( RID_URI, m_rc_link_file ) );
// LATER:        m_link_protocols.put( "chart", new LinkProtocol( RID_CHART, m_rc_chart ) );
    }

    private boolean
    check_search_char() {
        if( m_search_str.charAt( i_search ) == Character.toLowerCase( m_char_cur ) ) {
            if( i_search == 0 )
                m_pos_search = m_pos_cur;
            if( i_search == i_search_end ) {
                apply_match();
                i_search = 0;
            }
            else
                i_search++;

            return true;
        }
        else
            return false;
    }

    void
    parse( int bgn, int end ) {
        reset( bgn, end );

        if( bgn == 0 && end > 0 ) // not empty
            apply_heading();

        if( bgn == end ) // zero length
            return;

        for( ; m_pos_cur < m_pos_end; ++m_pos_cur ) {
            m_char_last = m_char_cur;
            m_char_cur = get_char_at( m_pos_cur );

            if( !m_search_str.isEmpty() ) {
                if( !check_search_char() && i_search > 0 ) {
                    i_search = 0;
                    check_search_char();
                }
            }

            // MARKUP PARSING
            switch( m_char_cur ) {
                case 0:     // should never be the case
                case '\n':
                case '\r':
                    m_cf_curr = Ch_NEWLINE | CF_NEWLINE;
                    process_char();
                    if( m_pos_cur > bgn ) // skip the \n at the start of the parsing region
                        process_paragraph();
                    m_pos_para_bgn = m_pos_cur + 1;
                    continue;   // !!!!! CONTINUES TO SKIP process_char() BELOW !!!!!
                case ' ':
                    m_cf_curr = Ch_SPACE | CF_SPACE | CF_TODO_STATUS | CF_NUMERIC;
                    break;
                case '*': // SIGN
                    m_cf_curr = Ch_ASTERISK | CF_PUNCTUATION | CF_MARKUP | CF_NONNUMERIC_PUNCT;
                    break;
                case '_': // SIGN
                    m_cf_curr = Ch_UNDERSCORE | CF_PUNCTUATION | CF_MARKUP | CF_NONNUMERIC_PUNCT;
                    break;
                case '=': // SIGN
                    m_cf_curr = Ch_EQUALS | CF_PUNCTUATION | CF_MARKUP | CF_NONNUMERIC_PUNCT;
                    break;
                case '#': // SIGN
                    m_cf_curr = Ch_HASH | CF_PUNCTUATION | CF_MARKUP | CF_NONNUMERIC_PUNCT;
                    break;
                case '[': // SIGN
                    m_cf_curr = Ch_SBB | CF_PUNCTUATION | CF_NONNUMERIC_PUNCT | CF_PARENTHESIS;
                    break;
                case ']': // SIGN
                    m_cf_curr = Ch_SBE | CF_PUNCTUATION | CF_NONNUMERIC_PUNCT | CF_PARENTHESIS;
                    break;
                case '(':
                case ')':
                case '{':
                case '}': // parentheses
                    m_cf_curr = CF_PUNCTUATION | CF_NONNUMERIC_PUNCT | CF_PARENTHESIS;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    m_cf_curr = CF_DIGIT | CF_NUMERIC;
                    process_number();   // calculates numeric value
                    break;
                case '.': // SIGN
                    m_cf_curr = Ch_DOT | CF_PUNCTUATION | CF_DATE_SEPARATOR | CF_NUMERIC;
                    break;
                case ',': // SIGN
                    m_cf_curr = Ch_COMMA | CF_PUNCTUATION | CF_NUMERIC;
                    break;
                case '-': // SIGN - CF_SIGNSPELL does not seem to be necessary
                    m_cf_curr = Ch_DASH | CF_PUNCTUATION | CF_DATE_SEPARATOR | CF_NUMERIC;
                    break;
                case '/': // SIGN
                    m_cf_curr = Ch_SLASH | CF_PUNCTUATION | CF_DATE_SEPARATOR |
                                CF_VALUE_SEPARATOR | CF_NONNUMERIC_PUNCT;
                    break;
                case ':': // SIGN
                    m_cf_curr = Ch_COLON | CF_PUNCTUATION | CF_NONNUMERIC_PUNCT;
                    break;
                case '@': // SIGN
                    m_cf_curr = Ch_AT | CF_PUNCTUATION | CF_NONNUMERIC_PUNCT;
                    break;
                case '<': // SIGN
                    m_cf_curr = Ch_LESS | CF_PUNCTUATION | CF_NONNUMERIC_PUNCT;
                    break;
                case '>': // SIGN
                    m_cf_curr = Ch_MORE | CF_PUNCTUATION | CF_TODO_STATUS | CF_NONNUMERIC_PUNCT;
                    break;
                case '\t':
                    m_cf_curr = Ch_TAB | CF_TAB;
                    break;
                // LIST CHARS
                case '~':
                    m_cf_curr = Ch_TILDE | CF_PUNCTUATION | CF_TODO_STATUS | CF_NONNUMERIC_PUNCT;
                    break;
                case '+':
                    m_cf_curr = Ch_PLUS | CF_PUNCTUATION | CF_TODO_STATUS | CF_NUMERIC;
                    break;
                case 'x':
                case 'X':
                    m_cf_curr =
                            Ch_X | CF_ALPHA | CF_SPELLCHECK | CF_TODO_STATUS | CF_NONNUMERIC_PUNCT;
                    break;
                case '\'':
                    m_cf_curr = CF_PUNCTUATION | CF_SPELLCHECK | CF_NONNUMERIC_PUNCT;
                    break;
                default:
                    m_cf_curr = Character.isLetter( m_char_cur ) ? CF_ALPHA | CF_SPELLCHECK :
                                CF_PUNCTUATION | CF_NONNUMERIC_PUNCT;
                    break;
            }
            process_char();
        }
        // end of the text -treated like a new line for all means and purposes
        if( m_pos_end > 0 ) // only when finish is not forced
        {
            m_char_last = m_char_cur;
            m_char_cur = '\n';
            m_cf_curr = Ch_NEWLINE | CF_NEWLINE;
            process_char();
            process_paragraph();
        }
    }

    void
    set_search_str( @NonNull String str ) {
        m_search_str = str;
        i_search = 0;
        i_search_end = str.length() - 1;
    }

    // EXTENDIBILITY
    void
    add_link_protocol( String name, int id, Vector< AbsChar > rc ) {
        m_link_protocols.put( name, new LinkProtocol( id, rc ) );
    }

    void // for addt'l recipes in derived classes
    apply_custom1() { }

    // HELPERS
    void
    set_start() {
        m_recipe_cur.m_pos_bgn = m_pos_cur;
    }
    void
    set_middle() {
        m_recipe_cur.m_pos_mid = m_pos_cur;
    }
    void
    add_block() {
        m_recipe_cur.m_state |= Recipe.RS_BLOCK;
    }

    protected abstract char
    get_char_at( int i );

    void
    process_char() {
        m_blocked_flags = 0;

        // UPDATE WORD LAST
        if( ( m_cf_curr & CF_SPELLCHECK ) != 0 ) {
            if( ( m_cf_last & CF_SPELLCHECK ) == 0 ) {
                m_word_cur = "";
                m_word_count++;
            }

            m_word_cur += m_char_cur;
        }
        else {
            if( ( m_cf_curr & CFC_BLANK ) != 0 )
                m_pos_blank = m_pos_cur;
            if( m_flag_check_word && !m_word_cur.isEmpty() && ( m_cf_last & CF_SPELLCHECK ) != 0 )
                check_word();
        }

        // FIRST CHECK ACTIVE RECIPES
        for( int i = 0; i < m_active_recipes.size(); ) {
            Recipe r = m_active_recipes.get( i );
            if( ( r.process_char() & Recipe.RS_IN_PROGRESS ) == 0 ) {
                m_active_recipes.remove( i );
            }
            else {
                if( ( r.m_state & Recipe.RS_BLOCK ) != 0 )
                    m_blocked_flags |= r.m_blocks;
                i++;
            }
        }

        // THEN CHECK IF IT TRIGGERS ANY OTHER RECIPE
        for( Recipe r : m_all_recipes ) {
            r.m_index = 0;
            r.m_state = Recipe.RS_NOT_SET;
            if( r.process_char() == Recipe.RS_IN_PROGRESS )
                m_active_recipes.add( new Recipe( r ) );
        }

        m_cf_last = m_cf_curr;
    }

    void
    process_paragraph() { }

    void
    process_number() {
        if( ( m_cf_last & CF_DIGIT ) != 0 ) {
            m_int_last *= 10;
            m_int_last += ( m_char_cur - '0' );
        }
        else
            m_int_last = ( m_char_cur - '0' );
    }

    // JUNCTIONS & CHECKS
    void
    check_date() {
        m_date_last.set_day( m_int_last );

        if( m_date_last.is_valid() )
            apply_link();
    }
    void
    junction_subheading() {
        if( m_char_cur == ' ' )
            apply_subsubheading();
        else
            apply_subheading();
    }
    void
    junction_markup() {
        set_start();
        m_recipe_cur.m_index = 0;    // as it will be ++
        m_recipe_cur.m_state |= Recipe.RS_BLOCK;

        switch( m_char_cur ) {
            case '*':
                m_recipe_cur.m_id = RID_BOLD;
                m_recipe_cur.m_blocks = RID_BOLD;
                m_recipe_cur.m_contents = m_rc_markup_b_end;
                break;
            case '_':
                m_recipe_cur.m_id = RID_ITALIC;
                m_recipe_cur.m_blocks = RID_ITALIC;
                m_recipe_cur.m_contents = m_rc_markup_i_end;
                break;
            case '#':
                m_recipe_cur.m_id = RID_HIGHLIGHT;
                m_recipe_cur.m_blocks = RID_HIGHLIGHT;
                m_recipe_cur.m_contents = m_rc_markup_h_end;
                break;
            case '=':
                m_recipe_cur.m_id = RID_STRIKETHROUGH;
                m_recipe_cur.m_blocks = RID_STRIKETHROUGH;
                m_recipe_cur.m_contents = m_rc_markup_s_end;
                break;
        }
    }
    void
    junction_markup2() {
        switch( m_recipe_cur.m_id ) {
            case RID_BOLD:
                m_recipe_cur.m_id = RID_MARKUP_B_END;
                break;
            case RID_ITALIC:
                m_recipe_cur.m_id = RID_MARKUP_I_END;
                break;
            case RID_HIGHLIGHT:
                m_recipe_cur.m_id = RID_MARKUP_H_END;
                break;
            case RID_STRIKETHROUGH:
                m_recipe_cur.m_id = RID_MARKUP_S_END;
                break;
        }
    }
    void
    junction_date_dotym() { // dot between year and month
        if( m_int_last >= Date.YEAR_MIN && m_int_last <= Date.YEAR_MAX )
            m_date_last.set_year( m_int_last );
        else
            m_recipe_cur.m_state = Recipe.RS_REJECTED;
    }
    void
    junction_date_dotmd() { // dot between month and day
        if( m_int_last >= 1 && m_int_last <= 12 &&
            // two separators must be the same:
            get_char_at( m_pos_cur - 3 ) == m_char_cur ) {
            m_date_last.set_month( m_int_last );
        }
        else
            m_recipe_cur.m_state = Recipe.RS_REJECTED;
    }
    void
    junction_todo() {
        switch( get_char_at( m_pos_cur - 2 ) ) {
            case ' ':
                apply_check_unf();
                break;
            case '~':
                apply_check_prg();
                break;
            case '+':
                apply_check_fin();
                break;
            case 'x':
            case 'X':
            case '>': // extra sign for distinguishing deferred items
                apply_check_ccl();
                break;
            default:
                break;
        }
    }
    void
    junction_colon() {
        LinkProtocol protocol = m_link_protocols.get( m_word_cur );
        if( protocol != null ) {
            Recipe lastRecipe = new Recipe( protocol.type, this, protocol.rc, RID_LINK_AT,
                                            m_pos_cur - m_word_cur.length(), m_pos_cur + 1 );
            m_active_recipes.add( lastRecipe );

            // pos_middle is only used by chart

            if( m_pos_cur - m_word_cur.length() > 0 &&
                get_char_at( m_pos_cur - m_word_cur.length() - 1 ) == '<' )
                lastRecipe.m_flag_accept_spaces = true;

            lastRecipe.m_state |= Recipe.RS_BLOCK;
        }
    }
    void
    junction_at() {
        m_recipe_cur.m_pos_bgn = m_pos_blank + 1;
    }
    void
    junction_link() {
        //if( m_recipe_cur->m_pos_bgn > 0 && get_char_at( m_recipe_cur->m_pos_bgn - 1 ) == '<' )
        if( m_recipe_cur.m_flag_accept_spaces ) {
            m_active_recipes.add(
                    new Recipe( m_recipe_cur.m_id, this, m_rc_link_hidden_end, 0,
                                m_recipe_cur.m_pos_bgn - 1, m_pos_cur ) );
        }
        else {
            if( m_recipe_cur.m_id == RID_ID )
                m_recipe_cur.m_int_value = m_int_last;
            apply_link();
        }
    }
    void
    junction_number() {
        // this is used to disregard the spaces which can be used in numbers...
        // ...as thousands separator per ISO 31-0 standard
        if( m_char_cur != ' ' )
            m_pos_extra_2 = m_pos_cur;
    }
    void
    junction_tag() {
        apply_inline_tag();
        if( m_char_cur == '=' )
            m_recipe_cur.m_pos_mid = m_pos_cur;
        else // do not continue with this recipe:
            m_recipe_cur.m_index = ( m_recipe_cur.m_contents.size() - 1 );
    }
    void
    junction_tag2() {
        if( m_char_cur == '/' )
            m_pos_extra_1 = m_pos_cur;
        else {
            apply_inline_tag();
            // do not continue with this recipe:
            m_recipe_cur.m_index = ( m_recipe_cur.m_contents.size() - 1 );
        }
    }

    // APPLIERS (TO BE OVERRIDEN)
    void                apply_heading() { }
    void                apply_subheading() { }
    void                apply_subsubheading() { }
    void                apply_bold() { }
    void                apply_italic() { }
    void                apply_strikethrough() { }
    void                apply_highlight() { }
    void                apply_comment() { }
    void                apply_ignore() { }
    void                apply_link() { }
    void                apply_link_hidden() { }
    void                apply_check_unf() { }
    void                apply_check_prg() { }
    void                apply_check_fin() { }
    void                apply_check_ccl() { }
    void                apply_inline_tag() { }
// LATER:    void                apply_chart() { }

    void                apply_indent() { }
    void                apply_match() { }

    void                check_word() { } // for spell-checking

    void
    reset( int bgn, int end ) {
        m_pos_end = end;
        m_pos_cur = m_pos_blank = m_pos_para_bgn = m_pos_extra_1 = m_pos_extra_2 = bgn;

        m_cf_curr = CF_NOTHING;
        m_cf_last = Ch_NOT_SET;
        m_word_cur = "";
        m_word_count = 0;
        m_int_last = 0;
        m_date_last.m_date = 0;

        m_active_recipes.clear();

        // start as if previous char is a new line
        for( Recipe r : m_all_recipes ) {
            r.m_index = 0;
            r.m_state = Recipe.RS_NOT_SET;
            if( r.process_char() == Recipe.RS_IN_PROGRESS )
                m_active_recipes.add( new Recipe( r ) );
        }
    }

    // PARSER SELECTOR (NEEDED DUE TO LACK OF FUNCTION POINTERS IN JAVA)
    private enum ParSel {
        NULL, ST_STRT, ST_MIDL, AD_BLCK,
        JK_ATSG, JK_COLN, JK_DDMD, JK_DDYM, JK_ITAG, JK_ITG2, JK_LINK, JK_MKUP, JK_MKP2, JK_NMBR,
        JK_SUBH, JK_TODO,
        AP_BOLD, AP_CCCL, AP_CFIN, AP_CMNT, AP_CPRG, AP_CUNF, AP_HILT, AP_IDNT, AP_IGNR, AP_ITAG,
        AP_ITLC, AP_LINK, AP_LNHD, AP_SSBH, AP_STRK, AP_SUBH,
        CH_DATE
    }
    private void
    selectParsingFunc( ParSel ps ) {
        switch( ps ) {
            case ST_STRT: set_start(); break;
            case ST_MIDL: set_middle(); break;
            case AD_BLCK: add_block(); break;

            case AP_BOLD: apply_bold(); break;
            case AP_CCCL: apply_check_ccl(); break;
            case AP_CFIN: apply_check_fin(); break;
            case AP_CMNT: apply_comment(); break;
            case AP_CPRG: apply_check_prg(); break;
            case AP_CUNF: apply_check_unf(); break;
            case AP_HILT: apply_highlight(); break;
            case AP_IDNT: apply_indent(); break;
            case AP_IGNR: apply_ignore(); break;
            case AP_ITAG: apply_inline_tag(); break;
            case AP_ITLC: apply_italic(); break;
            case AP_LINK: apply_link(); break;
            case AP_LNHD: apply_link_hidden(); break;
            case AP_STRK: apply_strikethrough(); break;
            case AP_SUBH: apply_subheading(); break;
            case AP_SSBH: apply_subsubheading(); break;

            case CH_DATE: check_date(); break;

            case JK_ATSG: junction_at(); break; // @ sign
            case JK_COLN: junction_colon(); break;
            case JK_DDMD: junction_date_dotmd(); break;
            case JK_DDYM: junction_date_dotym(); break;
            case JK_ITAG: junction_tag(); break;
            case JK_ITG2: junction_tag2(); break;
            case JK_LINK: junction_link(); break;
            case JK_MKUP: junction_markup(); break;
            case JK_MKP2: junction_markup2(); break;
            case JK_NMBR: junction_number(); break;
            case JK_SUBH: junction_subheading(); break;
            case JK_TODO: junction_todo(); break;
            default:
                break;
        }
    }

    int                         m_pos_end = 0;         // position of last char (constant)
    int                         m_pos_para_bgn = 0;    // position of curr para's begin
    int                         m_pos_cur = 0;
    int                         m_pos_extra_1 = 0; // for more articulated parsing like tags
    int                         m_pos_extra_2 = 0; // for more articulated parsing like tags
    int                         m_pos_blank = 0;
    int                         m_pos_search = 0;

    // from new to old: curr, last
    int                         m_cf_curr = Ch_NOT_SET;
    int                         m_cf_last = Ch_NOT_SET;

    char                        m_char_cur = 0;
    char                        m_char_last = 0;

    String                      m_word_cur;    // last word consisting purely of letters

    Paragraph                   m_p2para_cur = null;

    Map< String, LinkProtocol > m_link_protocols = new TreeMap<>();
    Recipe                      m_recipe_cur = null;
    int                         m_word_count = 0;
    int                         m_int_last = 0;
    Date                        m_date_last = new Date( 0 );
    boolean                     m_flag_check_word = false;

    String                      m_search_str = "";

    Set< Recipe >               m_all_recipes = new HashSet<>();
    List< Recipe >              m_active_recipes = new ArrayList<>();
    int                         m_blocked_flags = 0;

    private int                 i_search = 0;
    private int                 i_search_end = 0;
}
