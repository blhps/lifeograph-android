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

public class Filter extends StringDefElem
{
    public static final String DEFINITION_EMPTY = "F&";
    public static final String DEFINITION_DEFAULT = "F&\nFtn\nFsNOPdc";

    Filter( Diary d, String name, String definition ) {
        super( d, name, definition );
    }

    @Override
    public Type get_type() {
        return Type.FILTER;
    }

    @Override
    public int get_size() {
        return 0;
    }

    @Override
    public int get_icon() {
        return R.drawable.ic_filter;
    }

    FiltererContainer
    get_filterer_stack() {
        if( m_definition.isEmpty() )
            return null;

        FiltererContainer fc = new FiltererContainer( m_p2diary, null );

        fc.set_from_string( m_definition );

        return fc;
    }
}
