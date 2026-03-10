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


import android.graphics.Color;

import static java.lang.Math.abs;

public class Theme extends DiaryElement {

    public static class System extends Theme
    {
        private System( String f,
                        String cb,
                        String ct,
                        String ch,
                        String csh,
                        String chl ) {
            super( null, "Lifeograph", f, cb, ct, ch, csh, chl );
        }

        @Override
        public boolean
        is_system() {
            return true;
        }

        public static
        System get() {
            // initialize if not already initialized:
            if( system == null )
                system = new System( "Sans 10", "#FFFFFF", "#000000", "#B72525", "#963F3F",
                                     "#FFBBBB" );
            return system;
        }

        protected static System system = null;
    }

//    Theme( Diary d, String name ) {
//        super( d, name, ES_VOID );
//    }

    protected Theme(long nativePtr) {
        super(nativePtr);
    }

//    Theme( Diary d,
//           String name,
//           String str_font,
//           String str_base,
//           String str_text,
//           String str_heading,
//           String str_subheading,
//           String str_highlight ) {
//        super( d, name, ES_VOID );
//        font             = str_font;
//        color_base       = Color.parseColor( str_base );
//        color_text       = Color.parseColor( str_text );
//        color_heading    = Color.parseColor( str_heading );
//        color_subheading = Color.parseColor( str_subheading );
//        color_highlight  = Color.parseColor( str_highlight );
//    }

    @Override
    public int get_icon() {
        return R.drawable.ic_theme;
    }

    boolean
    is_system() {
        return false;
    }

    void
    copy_to( Theme target ) {
        nativeCopyTo(mNativePtr, target);
    }

    protected String font;
    protected String image_bg = "";

    protected int color_base;
    protected int color_text;
    protected int color_heading;
    protected int color_subheading;
    protected int color_highlight;

    // DERIVED COLORS
    int m_color_subsubheading;
    int m_color_inline_tag;
    int m_color_mid;
    int m_color_region_bg;
    int m_color_match_bg;
    int m_color_link;
    int m_color_link_broken;


    // CONSTANT COLORS

    static final int s_color_todo       = Color.parseColor( "#FF0000" );
    static final int s_color_progressed = Color.parseColor( "#FF8811" );
    static final int s_color_done       = Color.parseColor( "#66BB00" );
    static final int s_color_done1      = Color.parseColor( "#77CC11" );
    static final int s_color_done2      = Color.parseColor( "#409000" );
    static final int s_color_canceled   = Color.parseColor( "#AA8855" );

    // COLOR HELPER METHODS
    private static int
    parse_color_sub( String color, int begin, int end ) {
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
    public static int
    parse_color( String color ) {
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

    static int
    midtone( int c1, int c2, double ratio ) {
        return Color.rgb(
                ( int ) ( ( Color.red( c1 ) * ratio ) + ( Color.red( c2 ) * ( 1.0 - ratio ) ) ),
                ( int ) ( ( Color.green( c1 ) * ratio ) + ( Color.green( c2 ) * ( 1.0 - ratio ) ) ),
                ( int ) ( ( Color.blue( c1 ) * ratio ) + ( Color.blue( c2 ) * ( 1.0 - ratio ) ) ) );
    }
    static int
    midtone( int c1, int c2 ) {
        return Color.rgb( ( Color.red( c1 ) + Color.red( c2 ) ) / 2,
                          ( Color.green( c1 ) + Color.green( c2 ) ) / 2,
                          ( Color.blue( c1 ) + Color.blue( c2 ) ) / 2 );
    }

    static int
    contrast2( int bg, int c1, int c2 ) {
        double dist1 = get_color_diff( bg, c1 );
        double dist2 = get_color_diff( bg, c2 );

        if( dist1 > dist2 )
            return c1;
        else
            return c2;
    }

    static double
    get_color_diff( int c1, int c2 ) {
        return( abs( Color.red( c2 ) - Color.red( c1 ) ) +
                abs( Color.green( c2 ) - Color.green( c1 ) ) +
                abs( Color.blue( c2 ) - Color.blue( c1 ) ) );
    }
}
