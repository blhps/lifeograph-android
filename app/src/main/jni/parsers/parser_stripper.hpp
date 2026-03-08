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


#ifndef LIFEOGRAPH_PARSER_STRIPPER_HEADER
#define LIFEOGRAPH_PARSER_STRIPPER_HEADER


#include "parser_base.hpp"


namespace LoG
{

class ParserStripper
{
    public:
        Ustring                 parse( const Paragraph*, int );

    protected:
        void                    add_substr_to_stripped( UstringSize pos_bgn,
                                                        UstringSize pos_end,
                                                        int component )
        {
            // if any, add the plain text remnants first:
            if( m_flags & VT::TCT_FILTER_COMPONENT & VT::TCT_CMPNT_PLAIN )
            {
                int offset
                { ( m_stripped_text.empty() ||
                    STR::is_char_space( m_stripped_text[ m_stripped_text.size() - 1 ] ) ) &&
                  STR::is_char_space( m_p2para->get_char( m_i_last ) ) ? 1 : 0 };
                m_stripped_text += m_p2para->get_substr( m_i_last + offset, pos_bgn );
            }

            if( m_flags & VT::TCT_FILTER_COMPONENT & component )
                m_stripped_text += m_p2para->get_substr( pos_bgn, pos_end );

            m_i_last = pos_end;
        }

        const Paragraph*        m_p2para;
        Ustring                 m_stripped_text;
        int                     m_flags;
        int                     m_i_last;
};

}  // end of namespace LoG

#endif
