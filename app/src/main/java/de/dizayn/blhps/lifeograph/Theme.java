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

public class Theme {

    public static class System extends Theme {
        public System()
        {
            // TODO
        }

        @Override
        public boolean is_system() {
            return true;
        }

        public static System get() {
            // initialize if not already initialized:
            if( system == null )
                system = new System();
            return system;
        }

        protected static System system = null;
    }

    public Theme() {
        font = new String();
        // color_base = new Color();
        // color_text = new Color();
        // color_heading = new Color();
        // color_subheading = new Color();
        // color_highlight = new Color();
    }

    public Theme( Theme theme )
    {
        font = theme.font;
        color_base = theme.color_base;
        color_text = theme.color_text;
        color_heading = theme.color_heading;
        color_subheading = theme.color_subheading;
        color_highlight = theme.color_highlight;
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
