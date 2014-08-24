/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

public class Theme extends DiaryElement {

    public static class System extends Theme {
        public System() {
            super( null, NAME ); // null prevents taking an ID
        }

        public boolean is_system() {
            return true;
        }

        public static System get() {
            // initialize if not already initialized:
            if( system == null )
                system = new System();
            return system;
        }

        public static System system = null;
        public static final String NAME = new String( "[ - 0 - ]" );
    }

    public Theme( Diary diary, String name ) {
        super( diary, name );
        font = new String();
        // color_base = new Color();
        // color_text = new Color();
        // color_heading = new Color();
        // color_subheading = new Color();
        // color_highlight = new Color();
    }

    @Override
    public String getSubStr() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int get_icon() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Type get_type() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int get_size() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean is_system() {
        return false;
    }

    protected String font;
    protected String color_base;
    protected String color_text;
    protected String color_heading;
    protected String color_subheading;
    protected String color_highlight;
}
