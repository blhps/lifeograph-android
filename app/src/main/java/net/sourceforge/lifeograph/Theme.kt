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
package net.sourceforge.lifeograph

import android.graphics.Color
import androidx.core.graphics.toColorInt


class Theme(nativePtr: Long) : DiaryElement(nativePtr) {
    private fun colorToHex(color: Int): String {
        return String.format("#%06X", (0xFFFFFF and color))
    }

    override fun getIcon(): Int {
        return R.drawable.ic_theme
    }

    fun is_system(): Boolean { return nativeIsSystem(mNativePtr) }

    fun copy_to(target: Theme) {
        nativeCopyTo(mNativePtr, target.mNativePtr)
    }

    fun resetToSystem() {
        get_system_theme().copy_to(this)
    }

    var color_base: Int
        get() { return nativeGetColorBase(mNativePtr).toColorInt() }
        set(color: Int) { nativeSetColorBase(mNativePtr, colorToHex(color)) }

    var color_text: Int
        get() { return nativeGetColorText(mNativePtr).toColorInt() }
        set(color: Int) { nativeSetColorText(mNativePtr, colorToHex(color)) }

    var color_title: Int
        get() { return  nativeGetColorTitle(mNativePtr).toColorInt() }
        set(color: Int) { nativeSetColorTitle(mNativePtr, colorToHex(color)) }

    var color_heading_L: Int
        get() { return nativeGetColorHeadingL(mNativePtr).toColorInt() }
        set(color: Int) { nativeSetColorHeadingL(mNativePtr, colorToHex(color)) }

    var color_highlight: Int
        get() { return nativeGetColorHighlight(mNativePtr).toColorInt() }
        set(color: Int) { nativeSetColorHighlight(mNativePtr, colorToHex(color)) }

    val color_heading_M: Int get() { return nativeGetColorHeadingM(mNativePtr).toColorInt() }

    val color_mid: Int get() { return nativeGetColorMid(mNativePtr).toColorInt() }

    val color_match_bg: Int get() { return nativeGetColorMatchBG(mNativePtr).toColorInt() }

    val color_inline_tag: Int get() { return nativeGetColorInlineTag(mNativePtr).toColorInt() }

    val color_open: Int get() { return nativeGetColorOpen(mNativePtr).toColorInt() }

    val color_done: Int get() { return nativeGetColorDone(mNativePtr).toColorInt() }

    val color_done_bg: Int get() { return nativeGetColorDoneBG(mNativePtr).toColorInt() }

    // NATIVE FUNCTIONS ============================================================================
    private external fun nativeIsSystem(ptr: Long): Boolean
    private external fun nativeCopyTo(ptr: Long, ptr_theme: Long)
    private external fun nativeGetColorBase(ptr: Long): String
    private external fun nativeGetColorText(ptr: Long): String
    private external fun nativeGetColorTitle(ptr: Long): String
    private external fun nativeGetColorHeadingL(ptr: Long): String
    private external fun nativeGetColorHighlight(ptr: Long): String
    private external fun nativeGetColorHeadingM(ptr: Long): String
    private external fun nativeGetColorMid(ptr: Long): String
    private external fun nativeGetColorMatchBG(ptr: Long): String
    private external fun nativeGetColorInlineTag(ptr: Long): String
    private external fun nativeGetColorOpen(ptr: Long): String
    private external fun nativeGetColorDone(ptr: Long): String
    private external fun nativeGetColorDoneBG(ptr: Long): String

    private external fun nativeSetColorBase(ptr: Long, color: String)
    private external fun nativeSetColorText(ptr: Long, color: String)
    private external fun nativeSetColorTitle(ptr: Long, color: String)
    private external fun nativeSetColorHeadingL(ptr: Long, color: String)
    private external fun nativeSetColorHighlight(ptr: Long, color: String)


    companion object {
        fun get_system_theme(): Theme { return Theme( nativeGetSystemTheme() ) }

        @JvmStatic private external fun nativeGetSystemTheme(): Long
    }
}
