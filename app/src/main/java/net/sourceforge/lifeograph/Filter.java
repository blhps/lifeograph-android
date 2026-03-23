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

public class Filter extends StringDefElem
{
    public static final String DEFINITION_EMPTY = "F&";
    //public static final String DEFINITION_DEFAULT = "F&\nFtn\nFsNOPdc";

//    Filter( Diary d, String name, String definition ) {
//        super( d, name, definition );
//    }

    protected Filter(long nativePtr) {
        super(nativePtr);
    }

//    @Override
//    protected void finalize() throws Throwable {
//        if (mNativePtr != 0) {
//            nativeDestroy(mNativePtr);
//            mNativePtr = 0;
//        }
//        super.finalize();
//    }

    @Override
    public int getIcon() { return R.drawable.ic_filter; } // Java specific

    FiltererContainer
    get_filterer_stack() {
        long ptr = nativeGetFiltererStack( mNativePtr );
        return ptr != 0 ? new FiltererContainer( ptr ) : null;
    }

    // NATIVE METHODS ==============================================================================
    //private native long nativeCreate(long ptr_diary);
    //private native void nativeDestroy(long ptr);
    private native long nativeGetFiltererStack( long ptr );
}
