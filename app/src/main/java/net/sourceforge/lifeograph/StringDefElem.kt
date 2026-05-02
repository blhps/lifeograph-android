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
abstract class StringDefElem protected constructor(nativePtr: Long) : DiaryElement(nativePtr) {
    override fun get_size(): Int {
        return 0
    }

    var definition: String
        get() = nativeGetDefinition(mNativePtr)
        set(def) = nativeSetDefinition(mNativePtr, def)

//    fun add_definition_line(line: String) {
//        if(!m_definition!!.isEmpty()) m_definition += '\n'
//        m_definition += line
//    }

    // NATIVE METHODS ==============================================================================
    private external fun nativeGetDefinition(ptr: Long): String
    private external fun nativeSetDefinition(ptr: Long, def: String)

}
