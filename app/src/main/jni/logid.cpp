/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

    Parts of this file are loosely based on an example gcrypt program
    on http://punkroy.drque.net/

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


#include "logid.hpp"


// string getter
const char*
LoG::get_sstr( const LoGID& id )
{
    switch( id.get_raw() )
    {
#define XS( type, name, number, text ) \
        case type::name.get_raw(): \
            return text;

#define XE( type, name, number ) /* no string */

#include "stock_ids.dict"

#undef XS
#undef XE
        default:
            return "*ERROR*"; // should never be the case
    }
}
