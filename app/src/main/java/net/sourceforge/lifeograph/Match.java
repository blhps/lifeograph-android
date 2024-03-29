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

public class Match
{
    Match( Paragraph p, int p_entry, int p_para ) {
        para = p;
        pos_entry = p_entry;
        pos_para = p_para;

        p.add_reference( this.para );
    }

// TODO: in java we need to remove the reference for it to be deleted
//    ~Match() {
//        if( para != null )
//            para.remove_reference( this.para );
//    }

    boolean
    is_equal_to( Match other ) {
        return( other.para == para && other.pos_para == pos_para );
    }

    Paragraph para;
    int       pos_entry;
    int       pos_para;
    boolean   valid = true;
}
