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


@Suppress("PropertyName", "FunctionName")
class ChartData(val mNativePtr: Long) {

//    @Throws(Throwable::class)
//    protected fun finalize() {
//        if(mNativePtr != 0L) {
//            nativeDestroy(mNativePtr)
//            mNativePtr = 0
//        }
//        //super.finalize()
//    }

    fun clear() { nativeClear(mNativePtr) }
    fun set_from_string(chartDef: String?) { nativeSetFromString(mNativePtr, chartDef) }

    fun set_diary(diary: Diary) { nativeSetDiary(mNativePtr, diary.mNativePtr) }

    fun refresh_table() { nativeRefreshTable(mNativePtr) }

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

    // NATIVE FUNCTIONS ============================================================================
    //private external fun nativeCreate(ptr_diary: Long): Long
    //private external fun nativeDestroy(ptr: Long)

    private external fun nativeClear(ptr: Long)
    private external fun nativeSetDiary(ptr: Long, ptrDiary: Long)
    private external fun nativeRefreshTable(ptr: Long)
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
        val PERIOD_MONTHLY: Int = nativePERIOD_MONTHLY()
        val PERIOD_YEARLY: Int = nativePERIOD_YEARLY()
        @JvmStatic private external fun nativePERIOD_MONTHLY(): Int
        @JvmStatic private external fun nativePERIOD_YEARLY(): Int

        val NUM_Y_STEPS: Int = nativeNUM_Y_STEPS()
        @JvmStatic private external fun nativeNUM_Y_STEPS(): Int

        val STYLE_LINE: Int = nativeSTYLE_LINE()
        val STYLE_BARS: Int = nativeSTYLE_BARS()
        @JvmStatic private external fun nativeSTYLE_LINE(): Int
        @JvmStatic private external fun nativeSTYLE_BARS(): Int

        val TYPE_DATE: Int = nativeTYPE_DATE()
        val TYPE_STRING: Int = nativeTYPE_STRING()
        val TYPE_NUMBER: Int = nativeTYPE_NUMBER()
        @JvmStatic private external fun nativeTYPE_DATE(): Int
        @JvmStatic private external fun nativeTYPE_STRING(): Int
        @JvmStatic private external fun nativeTYPE_NUMBER(): Int
    }
}
