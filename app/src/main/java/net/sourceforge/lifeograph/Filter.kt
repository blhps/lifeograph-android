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

open class Filter(nativePtr: Long) : StringDefElem(nativePtr) {

    class All() : Filter(0) {
        override fun get_name(): String { return "&lt;All&gt;" }
        override fun get_list_str(): String { return get_name() }
        override fun get_type(): Type { return Type.FILTER } // native method fails
        override fun can_filter_entries(): Boolean { return true }
        override fun can_filter_paragraphs(): Boolean { return true }
    }

    companion object {
        val ALL : All = All()
    }

    override fun
    getIcon(): Int { return R.drawable.ic_filter } // Java specific

    override fun
    get_info_str(): String {
        val parts = mutableListOf<String>()
        if(can_filter_entries()) parts.add("Entries")
        if(can_filter_paragraphs()) parts.add("Paragraphs")

        return "Filters: ${parts.joinToString(", ")}"
    }

    open fun can_filter_entries(): Boolean { return nativeCanFilterEntries(mNativePtr) }
    open fun can_filter_paragraphs(): Boolean { return nativeCanFilterParagraphs(mNativePtr) }

    fun get_filterer_stack(): FiltererContainer? {
        val ptr = nativeGetFiltererStack(mNativePtr)
        return if(ptr != 0L) FiltererContainer(ptr) else null
    }

    // NATIVE METHODS ==============================================================================
    //private native long nativeCreate(long ptr_diary);
    //private native void nativeDestroy(long ptr);
    private external fun nativeGetFiltererStack(ptr: Long): Long
    private external fun nativeCanFilterEntries(ptr: Long): Boolean
    private external fun nativeCanFilterParagraphs(ptr: Long): Boolean
}
