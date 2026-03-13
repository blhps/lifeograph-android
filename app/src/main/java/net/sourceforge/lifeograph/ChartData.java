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


import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import kotlin.Pair;

import static java.lang.Math.abs;

public class ChartData
{
    public long mNativePtr;

    final static int MONTHLY                 = 0x1;
    final static int YEARLY                  = 0x2;
    final static int WEEKLY                  = 0x3;
    final static int PERIOD_MASK             = 0xF;

    final static int BOOLEAN                 = 0x10; // not used any more
    final static int CUMULATIVE_PERIODIC     = 0x20; // corresponds to the old cumulative
    final static int AVERAGE                 = 0x30;
    final static int CUMULATIVE_CONTINUOUS   = 0x40;
    final static int VALUE_TYPE_MASK         = 0xF0;

    final static int UNDERLAY_PREV_YEAR      = 0x100;
    final static int UNDERLAY_PLANNED        = 0x200;
    final static int UNDERLAY_MASK           = 0x300;
    final static int UNDERLAY_NONE           = 0x300; // same as mask to save bits

    final static int TAGGED_ONLY             = 0x800;

    final static int COUNT                   = 0x1000;
    final static int TEXT_LENGTH             = 0x2000;

    ChartData(long nativePtr) { mNativePtr = nativePtr; }

    void
    clear() { nativeClear( mNativePtr ); }

    void
    set_from_string( String chart_def ) { nativeSetFromString( mNativePtr, chart_def ); }

    void
    calculate_points() { nativeCalculatePoints( mNativePtr ); }

    int
    get_span() {
        return values.size();
    }

    int
    get_period() { return( type & PERIOD_MASK ); }
    boolean
    is_underlay_prev_year() {
        return( ( type & UNDERLAY_MASK ) == UNDERLAY_PREV_YEAR &&
                ( type & PERIOD_MASK ) != YEARLY ); }
    boolean
    is_underlay_planned() { return( ( type & UNDERLAY_MASK ) == UNDERLAY_PLANNED ); }


    double v_min      = Double.MAX_VALUE;
    double v_max      = -Double.MAX_VALUE;
    double v_plan_min = Double.MAX_VALUE;
    double v_plan_max = -Double.MAX_VALUE;

    // linked lists are used in Java as opposed to vector in C++ to get getLast()
    LinkedList< Double >  values      = new LinkedList<>();
    LinkedList< Double >  values_plan = new LinkedList<>();
    LinkedList< Integer > counts      = new LinkedList<>();
    LinkedList< Long >    dates       = new LinkedList<>();

    String          unit = "";
    int             type;
    Filter          filter = null;

    // NATIVE FUNCTIONS ============================================================================
    private native void nativeClear(long ptr);
    private native void nativeCalculatePoints(long ptr);
    private native void nativeSetFromString(long ptr, String str);
}
