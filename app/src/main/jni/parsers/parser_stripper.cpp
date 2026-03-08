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


#include "parser_stripper.hpp"


using namespace LoG;


Ustring
ParserStripper::parse( const Paragraph* para, int flags )
{
    m_p2para = para;
    m_i_last = 0;
    m_flags = flags;
    m_stripped_text.clear();

    if( m_flags & VT::TCT_CMPNT_NUMBER )
        m_stripped_text = ( m_p2para->get_list_order_full() + ' ' );

    for( auto format : para->m_formats )
    {
        // ignore formats already in a processed area (for formats within comments):
        if( format->pos_bgn < unsigned( m_i_last ) ) continue;

        switch( format->type )
        {
            case VT::HFT_COMMENT:
                if( m_flags & VT::TCT_CMPNT_PLAIN )
                    add_substr_to_stripped( format->pos_bgn, format->pos_end,
                                            VT::TCT_CMPNT_COMMENT );
                else // do not include the [[ and ]] when plain text is excluded
                    add_substr_to_stripped( format->pos_bgn + 2, format->pos_end - 2,
                                            VT::TCT_CMPNT_COMMENT );
                break;
            case VT::HFT_DATE:
            case VT::HFT_DATE_ELLIPSIS:
            case VT::HFT_TIME:
                add_substr_to_stripped( format->pos_bgn, format->pos_end, VT::TCT_CMPNT_DATE );
                break;
            case VT::HFT_TAG:
                add_substr_to_stripped( format->pos_bgn, format->pos_end, VT::TCT_CMPNT_TAG );
                break;
            case VT::HFT_TAG_VALUE:
                add_substr_to_stripped( format->pos_bgn - 1, format->pos_end, VT::TCT_CMPNT_TAG );
                break;
        }
    }

    add_substr_to_stripped( para->get_size(), 0, 0 );

    return m_stripped_text;
}
