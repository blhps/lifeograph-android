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

public class Theme extends DiaryElement {

    private String colorToHex(int color) { return String.format("#%06X", (0xFFFFFF & color)); }

    protected Theme(long nativePtr) {
        super(nativePtr);
    }

    @Override
    public int getIcon() { return R.drawable.ic_theme; }

    boolean
    is_system() { return nativeIsSystem(mNativePtr); }

    void
    copy_to( Theme target ) { nativeCopyTo(mNativePtr, target.mNativePtr); }

    int
    get_color_base() { return Color.parseColor(nativeGetColorBase(mNativePtr)); }
    int
    get_color_text() { return Color.parseColor(nativeGetColorText(mNativePtr)); }
    int
    get_color_title() { return Color.parseColor(nativeGetColorTitle(mNativePtr)); }
    int
    get_color_heading_L() { return Color.parseColor(nativeGetColorHeadingL(mNativePtr)); }
    int
    get_color_highlight() { return Color.parseColor(nativeGetColorHighlight(mNativePtr)); }
    int
    get_color_heading_M() { return Color.parseColor(nativeGetColorHeadingM(mNativePtr)); }
    int
    get_color_mid() { return Color.parseColor(nativeGetColorMid(mNativePtr)); }
    int
    get_color_match_bg() { return Color.parseColor(nativeGetColorMatchBG(mNativePtr)); }
    int
    get_color_inline_tag() { return Color.parseColor(nativeGetColorInlineTag(mNativePtr)); }
    int
    get_color_open() { return Color.parseColor(nativeGetColorOpen(mNativePtr)); }
    int
    get_color_done() { return Color.parseColor(nativeGetColorDone(mNativePtr)); }
    int
    get_color_done_bg() { return Color.parseColor(nativeGetColorDoneBG(mNativePtr)); }

    void
    set_color_base( int color ) { nativeSetColorBase(mNativePtr, colorToHex( color ) ); }
    void
    set_color_text( int color ) { nativeSetColorText(mNativePtr, colorToHex( color ) ); }
    void
    set_color_title( int color ) { nativeSetColorTitle(mNativePtr, colorToHex( color ) ); }
    void
    set_color_heading_L( int color ) { nativeSetColorHeadingL(mNativePtr, colorToHex( color ) ); }
    void
    set_color_highlight( int color ) { nativeSetColorHighlight(mNativePtr, colorToHex( color ) ); }

    // NATIVE FUNCTIONS ============================================================================
    private native boolean nativeIsSystem(long ptr);
    private native void nativeCopyTo(long ptr, long ptr_theme);
    private native String nativeGetColorBase(long ptr);
    private native String nativeGetColorText(long ptr);
    private native String nativeGetColorTitle(long ptr);
    private native String nativeGetColorHeadingL(long ptr);
    private native String nativeGetColorHighlight(long ptr);
    private native String nativeGetColorHeadingM(long ptr);
    private native String nativeGetColorMid(long ptr);
    private native String nativeGetColorMatchBG(long ptr);
    private native String nativeGetColorInlineTag(long ptr);
    private native String nativeGetColorOpen(long ptr);
    private native String nativeGetColorDone(long ptr);
    private native String nativeGetColorDoneBG(long ptr);

    private native void nativeSetColorBase(long ptr, String color);
    private native void nativeSetColorText(long ptr, String color);
    private native void nativeSetColorTitle(long ptr, String color);
    private native void nativeSetColorHeadingL(long ptr, String color);
    private native void nativeSetColorHighlight(long ptr, String color);
}
