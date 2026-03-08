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


#ifndef LIFEOGRAPH_PARSER_PARAGRAPH_HEADER
#define LIFEOGRAPH_PARSER_PARAGRAPH_HEADER


#include "parser_base.hpp"
#include "../diaryelements/paragraph.hpp"


namespace LoG
{


class ParserBackGround : public ParserBase
{
    public:
                                ParserBackGround( Diary* p2d ) : m_p2diary( p2d ) { }

        void                    parse( Paragraph* );
        void                    parse( const UstringSize, const UstringSize ) = delete;
        void                    parse_code( const UstringSize, const UstringSize );

        Wchar                   get_char_at( int i ) override
        { return m_parser_p2para_cur->get_char( i ); }

        Ustring                 get_substr( UstringSize begin, UstringSize end )
        { return m_parser_p2para_cur->get_text().substr( begin, end - begin ); }

        virtual void            process_paragraph() override;

        void                    apply_comment() override;
        void                    apply_time() override;
        void                    apply_link() override;
        void                    apply_inline_tag_value_nmbr() override;

    protected:
        void                    reset( UstringSize, UstringSize ) override;
        Diary*                  m_p2diary;
};

}  // end of namespace LoG

#endif
