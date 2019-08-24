/***********************************************************************************

    Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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


import android.graphics.Color;

public class Theme {

    public static class System extends Theme
    {
        private System() {
            font = "";
            color_base = Color.WHITE;
            color_text = Color.BLACK;
            color_heading = Color.parseColor( "#B72525" );
            color_subheading = Color.parseColor( "#963F3F" );
            color_highlight = Color.parseColor( "#FFBBBB" );
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

        private static System system = null;
    }

    public Theme() {
        font = "";
    }

    public Theme( Theme theme ) {
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
    protected int color_base;
    protected int color_text;
    protected int color_heading;
    protected int color_subheading;
    protected int color_highlight;

    // CONSTANT COLORS
    public static final int     s_color_match1 = Color.parseColor( "#33FF33" );
    public static final int     s_color_match2 = Color.parseColor( "#009900" );
    //public static final int     s_color_link1 = Color.parseColor( "#3333FF" ); LATER
    //public static final int     s_color_link2 = Color.parseColor( "#000099" );
    //public static final int     s_color_broken1 = Color.parseColor( "#FF3333" );
    //public static final int     s_color_broken2 = Color.parseColor( "#990000" );

    public static final int     s_color_todo = Color.parseColor( "#FF0000" );
    public static final int     s_color_progressed = Color.parseColor( "#FF8811" );
    public static final int     s_color_done = Color.parseColor( "#66BB00" );
    public static final int     s_color_done1 = Color.parseColor( "#77CC11" );
    public static final int     s_color_done2 = Color.parseColor( "#409000" );
    public static final int     s_color_canceled = Color.parseColor( "#AA8855" );

    private static int parse_color_sub( String color, int begin, int end ) {
        int ret_val = 0;

        for( int i = begin; i <= end; i++ ) {
            char c = color.charAt( i );
            if( c >= '0' && c <= '9' ) {
                ret_val *= 16;
                ret_val += ( c - '0' );
            }
            else if( c >= 'a' && c <= 'f' ) {
                ret_val *= 16;
                ret_val += ( c - 'a' + 10 );
            }
            else if( c >= 'A' && c <= 'F' ) {
                ret_val *= 16;
                ret_val += ( c - 'A' + 10 );
            }
        }

        return ret_val;
    }

    public static int parse_color( String color ) {
        return Color.rgb( parse_color_sub( color, 1, 2 ),
                          parse_color_sub( color, 5, 6 ),
                          parse_color_sub( color, 9, 10 ) );
    }
    public static String color2string( int i_color ) {
        return String.format( "#%02X%<02X%02X%<02X%02X%<02X",
                              Color.red( i_color ),
                              Color.green( i_color ),
                              Color.blue( i_color ) );
    }

    public static int midtone( int c1, int c2, float ratio ) {
        return Color.rgb(
                ( int ) ( ( Color.red( c1 ) * ratio ) + ( Color.red( c2 ) * ( 1.0 - ratio ) ) ),
                ( int ) ( ( Color.green( c1 ) * ratio ) + ( Color.green( c2 ) * ( 1.0 - ratio ) ) ),
                ( int ) ( ( Color.blue( c1 ) * ratio ) + ( Color.blue( c2 ) * ( 1.0 - ratio ) ) ) );
    }
    public static int contrast( int bg, int c1, int c2 ) {
        int dist1 = Math.abs( Color.red( bg ) - Color.red( c1 ) ) +
                    Math.abs( Color.green( bg ) - Color.green( c1 ) ) +
                    Math.abs( Color.blue( bg ) - Color.blue( c1 ) );

        int dist2 = Math.abs( Color.red( bg ) - Color.red( c2 ) ) +
                    Math.abs( Color.green( bg ) - Color.green( c2 ) ) +
                    Math.abs( Color.blue( bg ) - Color.blue( c2 ) );

        if( dist1 > dist2 )
            return c1;
        else
            return c2;
    }
}
