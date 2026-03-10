/* *********************************************************************************

    Copyright (C) 2012-2026 Ahmet Öztürk (aoz_2@yahoo.com)

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

import java.util.Comparator;

public abstract class DiaryElemTag extends DiaryElement
{
    protected DiaryElemTag( long nativePtr) {
        super(nativePtr);
    }

    public long
    get_date() {
        return nativeGetDate(mNativePtr);
    }

    public long
    get_date_edited() {
        return nativeGetDateEdited(mNativePtr);
    }

    // STRING METHODS

    public String
    get_info_str() {
        return "";
    }

    public int get_todo_status() {
        return nativeGetTodoStatus(mNativePtr);
    }
    public int get_todo_status_effective() {
        return nativeGetTodoStatusEffective(mNativePtr);
    }
    void
    set_todo_status( int s ) {
        nativeSetTodoStatus(mNativePtr, s);
    }

    public boolean
    is_expanded(){
        return nativeIsExpanded(mNativePtr);
    }
    public void
    set_expanded( boolean flag_expanded ) {
        nativeSetExpanded(mNativePtr, flag_expanded);
    }

    public String
    get_unit() {
        return nativeGetUnit(mNativePtr);
    }

    public long mNativePtr = 0;

    public static class CompareElemsByDate implements Comparator< DiaryElemTag > {
        public int compare( DiaryElemTag elem_l, DiaryElemTag elem_r ) {
            final long diff = ( elem_r.get_date() - elem_l.get_date() );
            if( diff == 0 )
                return 0;
            else if( diff > 0 )
                return 1;
            else return -1;
        }
    }

    //static final CompareElemsByName compare_elems_by_name = new CompareElemsByName();
    public static final CompareElemsByDate compare_elems_by_date = new CompareElemsByDate();

    // NATIVE METHODS ==============================================================================
    private native long nativeGetDate(long ptr);
    private native long nativeGetDateEdited(long ptr);
    private native boolean nativeIsExpanded(long ptr);
    private native void nativeSetExpanded(long ptr, boolean flag_expanded);
    private native int nativeGetTodoStatus(long ptr);
    private native int nativeGetTodoStatusEffective(long ptr);
    private native void nativeSetTodoStatus(long ptr, int s);
    private native String nativeGetUnit(long ptr);
}
