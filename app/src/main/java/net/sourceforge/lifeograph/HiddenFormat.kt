/***************************************************************************************************
Copyright (C) 2026. Ahmet Öztürk (aoz_2@yahoo.com)

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
 **************************************************************************************************/

package net.sourceforge.lifeograph

class HiddenFormat(val mNativePtr: Long) {
    var type: Char
        get() = nativeGetType(mNativePtr)
        set(value) = nativeSetType(mNativePtr, value)

    var uri: String
        get() = nativeGetUri(mNativePtr)
        set(value) = nativeSetUri(mNativePtr, value)

    var posBgn: Int
        get() = nativeGetPosBgn(mNativePtr)
        set(value) = nativeSetPosBgn(mNativePtr, value)

    var posEnd: Int
        get() = nativeGetPosEnd(mNativePtr)
        set(value) = nativeSetPosEnd(mNativePtr, value)

    var refId: Long
        get() = nativeGetRefId(mNativePtr)
        set(value) = nativeSetRefId(mNativePtr, value)

    fun get_id_lo(): Int = nativeGetIdLo(mNativePtr)
    fun get_id_hi(): Int = nativeGetIdHi(mNativePtr)


    var varI: Long
        get() = nativeGetVarI(mNativePtr)
        set(value) = nativeSetVarI(mNativePtr, value)

    var varD: Long
        get() = nativeGetVarD(mNativePtr)
        set(value) = nativeSetVarD(mNativePtr, value)

    // NATIVE METHODS ==============================================================================
    private external fun nativeGetType(ptr: Long): Char
    private external fun nativeSetType(ptr: Long, value: Char)
    private external fun nativeGetUri(ptr: Long): String
    private external fun nativeSetUri(ptr: Long, value: String)
    private external fun nativeGetPosBgn(ptr: Long): Int
    private external fun nativeSetPosBgn(ptr: Long, value: Int)
    private external fun nativeGetPosEnd(ptr: Long): Int
    private external fun nativeSetPosEnd(ptr: Long, value: Int)
    private external fun nativeGetRefId(ptr: Long): Long
    private external fun nativeSetRefId(ptr: Long, value: Long)
    private external fun nativeGetIdLo(ptr: Long): Int
    private external fun nativeGetIdHi(ptr: Long): Int
    private external fun nativeGetVarI(ptr: Long): Long
    private external fun nativeSetVarI(ptr: Long, value: Long)
    private external fun nativeGetVarD(ptr: Long): Long
    private external fun nativeSetVarD(ptr: Long, value: Long)
}
