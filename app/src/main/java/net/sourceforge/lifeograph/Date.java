/***********************************************************************************

    Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.util.Log;

public class Date {
    public static final long    NOT_APPLICABLE   = 0x0L;
    public static final long    NOT_SET          = 0xFFFFFFFFL;
    public static final long    DATE_MAX         = 0xFFFFFFFFL;

    public static final long    YEAR_MIN         = 1900L;
    public static final long    YEAR_MAX         = 2199L;
    public static final long    CHAPTER_MAX      = 1024L;

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

    // hidden elements' sequence numbers are not shown
    public static final long    VISIBLE_FLAG     = 0x40000000L;  // only for ordinal items

    public static final long    ORDINAL_STEP     = 0x400L;
    public static final long    ORDINAL_FLAG     = 0x80000000L;
    public static final long    ORDINAL_FILTER   = ORDINAL_STEP * ( CHAPTER_MAX - 1 );
    public static final long    ORDINAL_LAST     = ORDINAL_FILTER;

    public static final long    ORDER_MAX        = ORDER_FILTER;

    public static final long    TOPIC_NO_FLAGS_FILTER   = ORDINAL_FILTER|ORDER_FILTER;

    public static final long    TOPIC_MIN        = VISIBLE_FLAG|ORDINAL_FLAG;
    public static final long    GROUP_MIN        = ORDINAL_FLAG;

    // private DateFormatSymbols symbols = new DateFormatSymbols();
    public static final String[] WEEKDAYS = ( new DateFormatSymbols() ).getWeekdays();
    public static final String[] WEEKDAYSSHORT = ( new DateFormatSymbols() ).getShortWeekdays();
    public static final String[] MONTHS = ( new DateFormatSymbols() ).getMonths();

    public static final int[] MONTHLENGHTS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    protected static final int[] tm = { 0, 3, 3, 6, 1, 4, 6, 2, 5, 0, 3, 5 };

    public static String s_format_order;
    public static char s_format_separator;

    // int that holds the real value
    public long m_date;

    public Date( long d ) {
        m_date = d;
    }

    public Date() {
        m_date = NOT_SET;
    }

    public Date( int y, int m, int d ) {
        m_date = ( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) );
    }

    // ORDINAL C'TOR
    public Date( int o1, int o2 ) {
        m_date = ( ORDINAL_FLAG | ( o1 << 10 ) | o2 );
    }

    // STRING C'TOR
    public Date( String str_date ) {
        m_date = parse_string( str_date );
        if( m_date == 0 )
            m_date = NOT_SET;
    }

    public static long get_today( int order ) {
        Calendar cal = Calendar.getInstance();

        return make_date( cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH ) + 1,
                          cal.get( Calendar.DAY_OF_MONTH ), order );
    }

    public void set( long date ) {
        m_date = date;
    }

    // signature is different than in c++:
    public static long parse_string( String str_date ) {
        char c_cur;
        int[] num = { 0, 0, 0, 0 };  // fourth int is for trailing spaces
        int i = 0;
        long date;

        for( int j = 0; j < str_date.length(); j++ ) {
            c_cur = str_date.charAt( j );
            switch( c_cur ) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    if( i > 2 )
                        return 0; //INVALID;
                    num[ i ] *= 10;
                    num[ i ] += ( c_cur - '0' );
                    break;
                case ' ':
                    if( num[ i ] > 0 )
                        i++;
                    break;
                case '.':
                case '-':
                case '/':
                    if( num[ i ] == 0 || i == 2 )
                        return 0; //INVALID;
                    else
                        i++;
                    break;
                default:
                    return 0; //INVALID;
            }
        }

        // TEMPORAL
        if( num[ 2 ] != 0 ) {
            int year;
            int month;
            int day;

            // YMD
            if( num[ 0 ] > 31 && num[ 1 ] <= 12 && num[ 2 ] <= 31 ) {
                year = num[ 0 ];
                month = num[ 1 ];
                day = num[ 2 ];
            }
            else {
                // BOTH DMY AND MDY POSSIBLE
                if( num[ 0 ] <= 12 && num[ 1 ] <= 12 ) {
                    if( s_format_order.charAt( 0 ) == 'M' ) {
                        month = num[ 0 ];
                        day = num[ 1 ];
                    }
                    else {
                        month = num[ 1 ];
                        day = num[ 0 ];
                    }
                }
                // DMY
                else if( num[ 0 ] <= 31 && num[ 1 ] <= 12 ) {
                    month = num[ 1 ];
                    day = num[ 0 ];
                }
                // MDY
                else if( num[ 0 ] <= 12 && num[ 1 ] <= 31 ) {
                    month = num[ 1 ];
                    day = num[ 0 ];
                }
                else
                    return 0; //INVALID;

                year = num[ 2 ];

                if( year < 100 )
                    year += ( year < 30 ? 2000 : 1900 );
            }

            if( year < YEAR_MIN || year > YEAR_MAX )
                return 0; //OUT_OF_RANGE;

            Date date_tmp = new Date( year, month, day );
            if( ! date_tmp.is_valid() ) // checks days in month
                return 0; //INVALID;

            date = date_tmp.m_date;

        }
        // ORDINAL
        else if( num[ 1 ] != 0 ) {
            if( num[ 0 ] > CHAPTER_MAX || num[ 1 ] > ORDER_MAX )
                return 0; //OUT_OF_RANGE;

            date = make_date( num[ 0 ], num[ 1 ] );
        }
        else
            return 0; //INVALID;

        return date; //OK;
    }

    public static String format_string( long d, String format, char separator ) {
        StringBuilder result = new StringBuilder();

        if( ( d & ORDINAL_FLAG ) != 0 ) {
            result.append( get_ordinal_order( d ) + 1 );
            if( get_order( d ) != 0 )
                result.append( "." ).append( get_order( d ) );
        }
        else {
            for( int i = 0; i < format.length(); i++ ) {
                result.append( String.format( "%02d", get_YMD( d, format.charAt( i ) ) ) );
                if( i != format.length() - 1 )
                    result.append( separator );
            }
        }

        return result.toString();
    }
    public String format_string() {
        return format_string( m_date, s_format_order, s_format_separator );
    }
    public String format_string( String format ) {
        return format_string( m_date, format, s_format_separator );
    }
    public String format_string( String format, char separator ) {
        return format_string( m_date, format, separator );
    }

    // does not exist in C++
    public String format_string_ym() {
        return String.format( "%s, %02d", MONTHS[ get_month() - 1 ], get_year() );
    }

    public static String format_string_dt( long d ) {
        java.util.Date date = new java.util.Date( d * 1000L );
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        return String.format( "%02d.%02d.%02d, %2d:%2d", cal.get( Calendar.YEAR ),
                              cal.get( Calendar.MONTH ) + 1, cal.get( Calendar.DAY_OF_MONTH ),
                              cal.get( Calendar.HOUR ), cal.get( Calendar.MINUTE ) );
    }
    public static String format_string_d( long d ) {
        java.util.Date date = new java.util.Date( d * 1000L );
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        return String.format( "%02d.%02d.%02d", cal.get( Calendar.YEAR ),
                              cal.get( Calendar.MONTH ) + 1, cal.get( Calendar.DAY_OF_MONTH ) );
    }

    public int get_day() {
        return (int) ( ( m_date & DAY_FILTER ) >> 10 );
    }
    public static int get_day( long d ) {
        return (int) ( ( d & DAY_FILTER ) >> 10 );
    }

    public int get_month() {
        return (int) ( ( m_date & MONTH_FILTER ) >> 15 );
    }
    public static int get_month( long d ) {
        return (int) ( ( d & MONTH_FILTER ) >> 15 );
    }

    public int get_year() {
        return (int) ( ( m_date & YEAR_FILTER ) >> 19 );
    }
    public static int get_year( long d ) {
        return (int) ( ( d & YEAR_FILTER ) >> 19 );
    }

    // helper function for format_string() (this is a lambda in C++ version)
    public static int get_YMD( long d, char c ) {
        switch( c ) {
            case 'Y':
                return get_year( d );
            case 'M':
                return get_month( d );
            case 'D':
            default: // no error checking for now
                return get_day( d );
        }
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
    public static long get_order( long d ) {
        return( d & ORDER_FILTER );
    }

    public int get_ordinal_order() {
        return (int) ( ( m_date & ORDINAL_FILTER ) >> 10 );
    }
    public static int get_ordinal_order( long d ) {
        return (int) ( ( d & ORDINAL_FILTER ) >> 10 );
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

    public static long make_date( int c, int o ) {
        return( TOPIC_MIN | ( ( c - 1 ) * ORDINAL_STEP ) | o );
    }

    public void backward_ordinal_order() {
        if( get_ordinal_order() > 0 )
            m_date -= ORDINAL_STEP;
    }

    public void forward_ordinal_order() {
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

    public void forward_months( int months ) {
        months += ( ( m_date & MONTH_FILTER )>>15 ); // get month
        m_date &= YEAR_FILTER;   // isolate year
        int mod_months = months % 12;
        if( mod_months == 0 ) {
            m_date += make_year( ( months / 12 ) - 1 );
            m_date |= 0x60000;  // make month 12
        }
        else {
            m_date += make_year( months / 12 );
            m_date |= make_month( mod_months );
        }
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

    public String get_weekday_str() {
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
        else return ( year % 4 ) == 0;
    }

    public int calculate_days_between( Date date2 ) {
        // TODO: STUB!
        Log.e( Lifeograph.TAG, "STUB! STUB! STUB!" );
        return( 0 );
    }

    public int calculate_months_between( long date2 ) {
        return calculate_months_between( m_date, date2 );
    }

    public static int calculate_months_between( long date1, long date2 ) {
        int dist = 12 * ( get_year( date2 ) - get_year( date1 ) );
        dist += ( get_month( date2 ) - get_month( date1 ) );

        return ( dist < 0 ? -dist : dist );
    }

    public static int calculate_months_between_neg( long date1, long date2 ) {
        int dist = 12 * ( get_year( date2 ) - get_year( date1 ) );
        dist += ( get_month( date2 ) - get_month( date1 ) );

        return dist;
    }
}
