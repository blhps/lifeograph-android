/* *********************************************************************************

    Copyright (C) 2012-2020 Ahmet Öztürk (aoz_2@yahoo.com)

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

public class TableElem extends StringDefElem
{
    public static final String DEFINITION_DEFAULT = "T";

    TableElem( Diary diary, String name, String definition ) {
        super( diary, name, definition );
    }

    @Override public
    Type get_type(){
        return Type.CHART;
    }
}
