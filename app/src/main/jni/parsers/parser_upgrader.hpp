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


#ifndef LIFEOGRAPH_PARSER_UPGRADER_HEADER
#define LIFEOGRAPH_PARSER_UPGRADER_HEADER


#include "parser_base.hpp"


namespace LoG
{

class ParserUpgrader : public ParserBase
{
    public:
        ParserUpgrader();

        void                    parse( Paragraph*, int );

        Wchar                   get_char_at( int i ) override
        { return m_parser_p2para_cur->m_text.at( i ); }

        Ustring                 get_substr( UstringSize begin, UstringSize end )
        { return m_parser_p2para_cur->m_text.substr( begin, end - begin ); }

        void                    junction_markup() override;
        void                    junction_markup2() override;
        void                    junction_link() override;
        void                    junction_tag() override;
        void                    junction_tag2() override;
        void                    junction_number() override;

        void                    apply_bold() override;
        void                    apply_italic() override;
        void                    apply_strikethrough() override;
        void                    apply_highlight() override;

        void                    apply_inline_tag_old() override;

        void                    apply_link_old() override;

    protected:
        HiddenFormat*           apply_markup( int, UstringSize, UstringSize );

        int                     m_version_read;
        const static Recipe::Contents   m_rc_link_hidden_end;
        const static Recipe::Contents   m_rc_markup;
        const static Recipe::Contents   m_rc_markup_b_end;
        const static Recipe::Contents   m_rc_markup_i_end;
        const static Recipe::Contents   m_rc_markup_h_end;
        const static Recipe::Contents   m_rc_markup_s_end;
        const static Recipe::Contents   m_rc_tag;

        static const Recipe::Id RID_MARKUP          = 0x4;
        static const Recipe::Id RID_BOLD            = 0x8;
        static const Recipe::Id RID_ITALIC          = 0x10;
        static const Recipe::Id RID_HIGHLIGHT       = 0x20;
        static const Recipe::Id RID_STRIKETHROUGH   = 0x40;
        static const Recipe::Id RID_MARKUP_B_END    = 0x80;
        static const Recipe::Id RID_MARKUP_I_END    = 0x100;
        static const Recipe::Id RID_MARKUP_H_END    = 0x200;
        static const Recipe::Id RID_MARKUP_S_END    = 0x400;
};

}  // end of namespace LoG

#endif
