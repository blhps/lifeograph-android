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
package net.sourceforge.lifeograph

import java.util.LinkedList


class ChartData
internal constructor(diary: Diary) {
    var mNativePtr: Long

    @Throws(Throwable::class)
    protected fun finalize() {
        if(mNativePtr != 0L) {
            nativeDestroy(mNativePtr)
            mNativePtr = 0
        }
        //super.finalize()
    }

    fun clear() { nativeClear(mNativePtr) }
    fun set_from_string(chart_def: String?) { nativeSetFromString(mNativePtr, chart_def) }

    fun calculate_points() { nativeCalculatePoints(mNativePtr) }

    val type: Int get() { return nativeGetType(mNativePtr) }

    val style: Int get() { return nativeGetStyle(mNativePtr) }

    val span: Int get() { return nativeGetSpan(mNativePtr) }

    val period: Int get() { return nativeGetPeriod(mNativePtr) }

    val unit: String get() { return nativeGetUnit(mNativePtr) }

//    fun is_underlay_prev_year(): Boolean {
//        return nativeIsUnderlayPrevYear(mNativePtr)
//    }

    fun has_underlay(): Boolean { return nativeHasUnderlay(mNativePtr) }

    val v_min: Double get() { return nativeGetVMin(mNativePtr) }
    val v_max: Double get() { return nativeGetVMax(mNativePtr) }
    val v_grid_step: Double get() { return nativeGetVGridStep(mNativePtr) }
    val v_grid_min: Double get() { return nativeGetVGridMin(mNativePtr) }

    val values_num: LinkedHashMap<Double, YValues> get() { return nativeGetValuesNum(mNativePtr) }
    val values_date: LinkedHashMap<Long, YValues> get() { return nativeGetValuesDate(mNativePtr) }
    val values_str: LinkedHashMap<Int, YValues> get() { return nativeGetValuesStr(mNativePtr) }
    val values_index2str: LinkedHashMap<Int, String> get() { return nativeGetValuesIndex2Str(mNativePtr) }
    //var values_plan: LinkedList<Double> = LinkedList<Double>()
    //val dates: LinkedList<Long> get() { return nativeGetDates(mNativePtr) }

    //    final static int UNDERLAY_PREV_YEAR      = 0x100;
    //    final static int UNDERLAY_PLANNED        = 0x200;
    //    final static int UNDERLAY_MASK           = 0x300;
    init {
        mNativePtr = nativeCreate(diary.mNativePtr)
    }

    // NATIVE FUNCTIONS ============================================================================
    private external fun nativeCreate(ptr_diary: Long): Long
    private external fun nativeDestroy(ptr: Long)

    private external fun nativeClear(ptr: Long)
    private external fun nativeCalculatePoints(ptr: Long)
    private external fun nativeSetFromString(ptr: Long, str: String?)

    private external fun nativeGetType(ptr: Long): Int
    private external fun nativeGetStyle(ptr: Long): Int
    private external fun nativeGetSpan(ptr: Long): Int
    private external fun nativeGetPeriod(ptr: Long): Int

    private external fun nativeGetUnit(ptr: Long): String

    private external fun nativeIsUnderlayPrevYear(ptr: Long): Boolean
    private external fun nativeHasUnderlay(ptr: Long): Boolean

    private external fun nativeGetVMin(ptr: Long): Double
    private external fun nativeGetVMax(ptr: Long): Double
    private external fun nativeGetVGridStep(ptr: Long): Double
    private external fun nativeGetVGridMin(ptr: Long): Double

    private external fun nativeGetValuesNum(ptr: Long): LinkedHashMap<Double, YValues>
    private external fun nativeGetValuesDate(ptr: Long): LinkedHashMap<Long, YValues>
    private external fun nativeGetValuesStr(ptr: Long): LinkedHashMap<Int, YValues>
    private external fun nativeGetValuesIndex2Str(ptr: Long): LinkedHashMap<Int, String>

    companion object {
        val PERIOD_MONTHLY: Int get() = nativePERIOD_MONTHLY()
        val PERIOD_YEARLY: Int get() = nativePERIOD_YEARLY()
        @JvmStatic private external fun nativePERIOD_MONTHLY(): Int
        @JvmStatic private external fun nativePERIOD_YEARLY(): Int

        val NUM_Y_STEPS: Int get() = nativeNUM_Y_STEPS()
        @JvmStatic private external fun nativeNUM_Y_STEPS(): Int

        val STYLE_LINE: Int get() = nativeSTYLE_LINE()
        val STYLE_BARS: Int get() = nativeSTYLE_BARS()
        @JvmStatic private external fun nativeSTYLE_LINE(): Int
        @JvmStatic private external fun nativeSTYLE_BARS(): Int

        val TYPE_DATE: Int get() = nativeTYPE_DATE()
        val TYPE_STRING: Int get() = nativeTYPE_STRING()
        val TYPE_NUMBER: Int get() = nativeTYPE_NUMBER()
        @JvmStatic private external fun nativeTYPE_DATE(): Int
        @JvmStatic private external fun nativeTYPE_STRING(): Int
        @JvmStatic private external fun nativeTYPE_NUMBER(): Int
    }
}
