/***************************************************************************************************
 Copyright (C) 2026 Ahmet Öztürk (aoz_2@yahoo.com)

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

package net.sourceforge.lifeograph.helpers


class Date(var mDate: Long = 0) {
    // In Android implementation we added mDate. In the future, we will probably do the same on C++

    constructor(y: Int, m: Int, d: Int) : this(nativeMake(y, m, d))
    constructor(strDate: String) : this(nativeMakeStr(strDate).let { if (it == 0L) 0 else it })

    fun set(date: Long) {
        mDate = date
    }

    fun get_day(): Int = nativeGetDay(mDate)
    fun get_month(): Int = nativeGetMonth(mDate)
    fun get_year(): Int = nativeGetYear(mDate)

    fun isolate_YM(): Long = nativeIsolateYM(mDate)
    fun isolate_YMD(): Long = nativeIsolateYMD(mDate)

    fun set_year(y: Int) { mDate = nativeSetYear(mDate, y) }
    fun set_month(m: Int) { mDate = nativeSetMonth(mDate, m) }
    fun set_day(d: Int)  { mDate = nativeSetDay(mDate, d) }

    fun forward_months(months: Int) { mDate = nativeForwardMonths(mDate, months) }
    fun backward_months(months: Int) { mDate = nativeBackwardMonths(mDate, months) }
    fun forward_days(days: Int) { mDate = nativeForwardDays(mDate, days) }
    fun backward_days(days: Int) { mDate = nativeBackwardDays(mDate, days) }

    fun backward_to_week_start() { mDate = nativeBackwardToWeekStart(mDate) }
    fun backward_to_month_start() { mDate = nativeBackwardToMonthStart(mDate) }
    fun backward_to_year_start() { mDate = nativeBackwardToYearStart(mDate) }

    fun get_weekday(): Int = nativeGetWeekday(mDate)
    fun get_yearday(): Int = nativeGetYearday(mDate)
    fun get_days_in_month(): Int = nativeGetDaysInMonth(mDate)
    fun get_days_in_year(): Int = nativeGetDaysInYear(mDate)

    fun is_leap_year(): Boolean = nativeIsLeapYear(mDate)

    fun is_valid(): Boolean = nativeIsValid(mDate)
    fun is_set(): Boolean = nativeIsSet(mDate)

    fun format_string(format: String): String = nativeFormatStringCustom(mDate, format)
    fun format_string(): String = nativeFormatString(mDate)

    fun get_weekday_str(): String = nativeGetWeekDayStr(mDate)
    fun get_month_str(): String = nativeGetMonthStr(mDate)

    //fun format_string_ym(): String = String.format(Locale.US, "%s, %02d", get_month_str(), get_year())

    override fun toString(): String = format_string()

    companion object {

//        @JvmField val WEEKDAYS: Array<String> = DateFormatSymbols().weekdays
//        @JvmField val WEEKDAYSSHORT: Array<String> = DateFormatSymbols().shortWeekdays
//        @JvmField val MONTHS: Array<String> = DateFormatSymbols().months

        @JvmStatic fun is_valid(d: Long): Boolean = nativeIsValid(d)
        @JvmStatic fun is_set(d: Long): Boolean = nativeIsSet(d)

        @JvmStatic fun get_today(): Long = nativeGetToday()
        @JvmStatic fun get_day(d: Long): Int = nativeGetDay(d)
        @JvmStatic fun get_month(d: Long): Int = nativeGetMonth(d)
        @JvmStatic fun get_year(d: Long): Int = nativeGetYear(d)
        @JvmStatic fun set_day(date: Long, d: Int): Long = nativeSetDay(date, d)
        @JvmStatic fun set_month(date: Long, m: Int): Long = nativeSetMonth(date, m)
        @JvmStatic fun set_year(date: Long, y: Int): Long = nativeSetYear(date, y)
        @JvmStatic fun isolate_YM(d: Long): Long = nativeIsolateYM(d)
        @JvmStatic fun isolate_YMD(d: Long): Long = nativeIsolateYMD(d)

        @JvmStatic fun forward_months(d: Long, months: Int): Long = nativeForwardMonths(d, months)
        @JvmStatic fun backward_months(d: Long, months: Int): Long = nativeBackwardMonths(d, months)
        @JvmStatic fun forward_days(d: Long, days: Int): Long = nativeForwardDays(d, days)
        @JvmStatic fun backward_days(d: Long, days: Int): Long = nativeBackwardDays(d, days)


        @JvmStatic fun backward_to_week_start(d: Long): Long = nativeBackwardToWeekStart(d)
        @JvmStatic fun backward_to_month_start(d: Long): Long = nativeBackwardToMonthStart(d)
        @JvmStatic fun backward_to_year_start(d: Long): Long = nativeBackwardToYearStart(d)

        @JvmStatic fun get_weekday(d: Long): Int = nativeGetWeekday(d)
        @JvmStatic fun get_yearday(d: Long): Int = nativeGetYearday(d)
        @JvmStatic fun get_days_in_month(d: Long): Int = nativeGetDaysInMonth(d)
        @JvmStatic fun get_days_in_year(d: Long): Int = nativeGetDaysInYear(d)
        @JvmStatic fun is_leap_year(d: Long): Boolean = nativeIsLeapYear(d)

        @JvmStatic fun make(strDate: String): Long = nativeMakeStr(strDate)
        @JvmStatic fun make(y: Int, m: Int, d: Int): Long = nativeMake(y, m, d)
        @JvmStatic fun format_string(d: Long, format: String):
                String = nativeFormatStringCustom(d, format)
        @JvmStatic fun format_string(d: Long): String = nativeFormatString(d)

        @JvmStatic fun get_day_name(no: Int): String = nativeGetDayName(no)

        @JvmStatic fun calculate_days_between(d1: Long, d2: Long): Int = nativeCalculateDaysBetween(d1, d2)

        // NATIVE DECLARATIONS =====================================================================
        @JvmStatic private external fun nativeIsValid(date: Long): Boolean
        @JvmStatic private external fun nativeIsSet(date: Long): Boolean

        @JvmStatic private external fun nativeMake(y: Int, m: Int, d: Int): Long
        @JvmStatic private external fun nativeMakeStr(str: String): Long

        @JvmStatic private external fun nativeGetToday(): Long
        @JvmStatic private external fun nativeGetNow(): Long
        @JvmStatic private external fun nativeGetYear(d: Long): Int
        @JvmStatic private external fun nativeGetMonth(d: Long): Int
        @JvmStatic private external fun nativeGetDay(d: Long): Int
        @JvmStatic private external fun nativeSetYear(date: Long, y: Int): Long
        @JvmStatic private external fun nativeSetMonth(date: Long, m: Int): Long
        @JvmStatic private external fun nativeSetDay(date: Long, d: Int): Long
        @JvmStatic private external fun nativeIsolateYM(d: Long): Long
        @JvmStatic private external fun nativeIsolateYMD(d: Long): Long
        @JvmStatic private external fun nativeGetHours(d: Long): Int
        @JvmStatic private external fun nativeGetMins(d: Long): Int
        @JvmStatic private external fun nativeGetSecs(d: Long): Int
        @JvmStatic private external fun nativeForwardMonths(date: Long, months: Int): Long
        @JvmStatic private external fun nativeBackwardMonths(date: Long, months: Int): Long
        @JvmStatic private external fun nativeForwardDays(date: Long, days: Int): Long
        @JvmStatic private external fun nativeBackwardDays(date: Long, days: Int): Long

        @JvmStatic private external fun nativeBackwardToWeekStart(date: Long): Long
        @JvmStatic private external fun nativeBackwardToMonthStart(date: Long): Long
        @JvmStatic private external fun nativeBackwardToYearStart(date: Long): Long


        @JvmStatic private external fun nativeIsLeapYear(d: Long): Boolean
        @JvmStatic private external fun nativeGetDaysInMonth(d: Long): Int
        @JvmStatic private external fun nativeGetDaysInYear(d: Long): Int
        @JvmStatic private external fun nativeGetWeekday(d: Long): Int
        @JvmStatic private external fun nativeGetYearday(d: Long): Int
        @JvmStatic private external fun nativeFormatStringCustom(d: Long, format: String): String
        @JvmStatic private external fun nativeFormatString(d: Long): String
        @JvmStatic private external fun nativeGetMonthStr(d: Long): String
        @JvmStatic private external fun nativeGetWeekDayStr(d: Long): String
        @JvmStatic private external fun nativeGetDayName(no: Int): String
        @JvmStatic private external fun nativeCalculateDaysBetween(d1: Long, d2: Long): Int
        @JvmStatic private external fun nativeSetFormat(order: String, separator: Char)

        init {
            System.loadLibrary("lifeograph_core")
        }
    }
}
