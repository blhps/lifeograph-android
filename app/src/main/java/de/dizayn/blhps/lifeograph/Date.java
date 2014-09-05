/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.util.Log;

public class Date {
    public static final long    NOTSET           = 0xFFFFFFFFL;
    public static final long    DATE_MAX         = 0xFFFFFFFFL;
    public static final long    YEAR_MAX         = 2199L;
    public static final long    YEAR_MIN         = 1900L;

    public static final long    ORDER_FILTER     =      0x3FFL;
    public static final long    DAY_FILTER       =     0x7C00L;
    public static final long    MONTH_FILTER     =    0x78000L;
    public static final long    YEAR_FILTER      = 0x7FF80000L;
    public static final long    ORDER_FILTER_INV = DATE_MAX ^ ORDER_FILTER;
    public static final long    DAY_FILTER_INV   = DATE_MAX ^ DAY_FILTER;
    public static final long    MONTH_FILTER_INV = DATE_MAX ^ MONTH_FILTER;
    public static final long    YEAR_FILTER_INV  = DATE_MAX ^ YEAR_FILTER;
    public static final long    YEARMONTH_FILTER = YEAR_FILTER|MONTH_FILTER;
    public static final long    PURE_FILTER      = DATE_MAX ^ ORDER_FILTER;

    // hidden elements are custom sorted and their sequence numbers are not shown
    public static final long    VISIBLE_FLAG     = 0x40000000L;  // only for ordinal items

    public static final long    ORDINAL_STEP     = 0x400L;
    public static final long    ORDINAL_FLAG     = 0x80000000L;
    public static final long    ORDINAL_FILTER   = 0x1FFFFC00L;
    public static final long    TOPIC_MAX        = ORDINAL_FILTER;
    public static final long    TOPIC_NO_FLAGS_FILTER   = ORDINAL_FILTER|ORDER_FILTER;

    public static final long    TOPIC_MIN        = VISIBLE_FLAG|ORDINAL_FLAG;
    public static final long    SORTED_MIN       = ORDINAL_FLAG;

    // private DateFormatSymbols symbols = new DateFormatSymbols();
    public static final String[] WEEKDAYS = ( new DateFormatSymbols() ).getWeekdays();
    public static final String[] WEEKDAYSSHORT = ( new DateFormatSymbols() ).getShortWeekdays();
    public static final String[] MONTHS = ( new DateFormatSymbols() ).getMonths();
    // { "January", "February", "March", "April", "May", "June",
    // "July", "August", "September", "October", "November",
    // "December" };
    public static final int[] MONTHLENGHTS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    protected static final int[] tm = { 0, 3, 3, 6, 1, 4, 6, 2, 5, 0, 3, 5 };

    // int that holds the real value
    public long m_date;

    public Date( long d ) {
        m_date = d;
    }

    public Date() {
        m_date = NOTSET;
    }

    public Date( int y, int m, int d ) {
        m_date = ( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) );
    }

    // ORDINAL C'TOR
    public Date( int o1, int o2 ) {
        m_date = ( ORDINAL_FLAG | ( o1 << 10 ) | o2 );
    }

    static long get_today( int order ) {
        Calendar cal = Calendar.getInstance();

        return make_date( cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH ) + 1,
                          cal.get( Calendar.DAY_OF_MONTH ), order );
    }

    public void set( int date ) {
        m_date = date;
    }

    public String format_string( boolean weekDay ) {
        if( ( m_date & ORDINAL_FLAG ) != 0 ) {
            return String.format( get_order() != 0 ? "%d.%d" : "%d", get_ordinal_order() + 1,
                                  get_order() );
        }
        else if( weekDay ) {
            return String.format( "%02d.%02d.%02d, %s", get_year(), get_month(), get_day(),
                                  getWeekdayStr() );
        }
        else {
            return String.format( "%02d.%02d.%02d", get_year(), get_month(), get_day() );
        }
    }

    public static String format_string_do( long d ) {
        java.util.Date date = new java.util.Date( d * 1000L );
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        return String.format( "%02d.%02d.%02d", cal.get( Calendar.YEAR ),
                              cal.get( Calendar.MONTH ) + 1, cal.get( Calendar.DAY_OF_MONTH ) );
    }

    public int get_day() {
        return (int) ( ( m_date & DAY_FILTER ) >> 10 );
    }

    public int get_month() {
        return (int) ( ( m_date & MONTH_FILTER ) >> 15 );
    }

    public int get_year() {
        return (int) ( ( m_date & YEAR_FILTER ) >> 19 );
    }

    public long get_yearmonth() {
        return( m_date & YEARMONTH_FILTER );
    }

    public long get_pure() {
        return( m_date & PURE_FILTER );
    }

    public long get_order() {
        return( m_date & ORDER_FILTER );
    }

    public int get_ordinal_order() {
        return (int) ( ( m_date & ORDINAL_FILTER ) >> 10 );
    }

    public boolean is_ordinal() {
        return( ( m_date & ORDINAL_FLAG ) != 0 );
    }
    public static boolean is_ordinal( long d ) {
        return( ( d & ORDINAL_FLAG ) != 0 );
    }

    public boolean is_hidden() {
        return( is_ordinal() && ( m_date & VISIBLE_FLAG ) == 0 );
    }

    public boolean is_valid() {
        return( get_day() > 0 && get_day() <= get_days_in_month() );
    }

    public void set_year( int y ) {
        if( y >= YEAR_MIN && y <= YEAR_MAX ) {
            m_date &= YEAR_FILTER_INV;
            m_date |= ( y << 19 );
        }
    }

    public void set_month( int m ) {
        if( m < 13 ) {
            m_date &= MONTH_FILTER_INV;
            m_date |= ( m << 15 );
        }
    }

    public void set_day( int d ) {
        if( d < 32 ) {
            m_date &= DAY_FILTER_INV;
            m_date |= ( d << 10 );
        }
    }

    public void reset_order_0() {
        m_date &= ORDER_FILTER_INV;
    }

    public void reset_order_1() {
        m_date &= ORDER_FILTER_INV;
        m_date |= 0x1;
    }

    public static long reset_order_1( long d ) {
        return( ( d | 0x1 ) & ORDER_FILTER_INV );
    }

    public static long make_year( int y ) {
        return( y << 19 );
    }

    public static long make_month( int m ) {
        return( m << 15 );
    }

    public static long make_day( int d ) {
        return( d << 10 );
    }

    public static long make_date( int y, int m, int d, int o ) {
        return( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) | o );
    }

    void backward_ordinal_order() {
        if( get_ordinal_order() > 0 )
            m_date -= ORDINAL_STEP;
    }

    void forward_ordinal_order() {
        m_date += ORDINAL_STEP;
    }

    public void backward_month() {
        int day = get_day();
        int month = ( ( get_month() + 10 ) % 12 ) + 1;
        int year = get_year();
        if( month == 12 )
            year--;
        m_date = make_year( year ) | make_month( month );

        if( day > get_days_in_month() )
            day = get_days_in_month();

        m_date |= make_day( day );
    }

    // IMPROVED METHODS WRT C++ COUNTERPARTS
    public void forward_month() {
        int day = get_day();
        int month = ( get_month() % 12 ) + 1;
        int year = get_year();
        if( month == 1 )
            year++;
        m_date = make_year( year ) | make_month( month );

        if( day > get_days_in_month() )
            day = get_days_in_month();

        m_date |= make_day( day );
    }

    public int get_weekday() {
        // from wikipedia: http://en.wikipedia.org/wiki/Calculating_the_day_of_the_week
        int year = get_year();
        int century = ( year - ( year % 100 ) ) / 100;
        int c = 2 * ( 3 - ( century % 4 ) );
        int y = year % 100;
        y = y + ( int ) java.lang.Math.floor( y / 4 );

        int m = get_month() - 1;
        int d = ( c + y + tm[ m ] + get_day() );

        if( m < 2 && is_leap_year() ) // leap year!
            d += 6;

        return( d % 7 );
    }

    public String getWeekdayStr() {
        return WEEKDAYS[ get_weekday() + 1 ];
    }

    // EOF IMPR.

    public int get_days_in_month() {
        int length = MONTHLENGHTS[ get_month() - 1 ];
        if( get_month() == 2 )
            if( is_leap_year() )
                length++;
        return length;
    }

    public boolean is_leap_year() {
        int year = get_year();
        if( ( year % 400 ) == 0 )
            return true;
        else if( ( year % 100 ) == 0 )
            return false;
        else if( ( year % 4 ) == 0 )
            return true;
        else
            return false;
    }

    int calculate_days_between( Date date2 ) {
        // TODO: STUB!
        Log.w( Lifeograph.TAG, "STUB! STUB! STUB!" );
        return( 0 );
    }
}
