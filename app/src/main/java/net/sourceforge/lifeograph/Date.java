/* *********************************************************************************

    Copyright (C) 2012-2020 Ahmet Öztürk (aoz_2@yahoo.com)

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
    public static final long NOT_APPLICABLE         =        0x0L;
    public static final long NOT_SET                = 0xFFFFFFFFL;
    public static final long DATE_MAX               = 0xFFFFFFFFL;

    public static final long YEAR_MIN               = 1900L;
    public static final long YEAR_MAX               = 2199L;
    public static final long ORDER_MAX              = 1023L;
    public static final long ORDER_1ST_MAX          = 0x3FF00000L; // bits 21..30
    public static final long ORDER_2ND_MAX          =    0xFFC00L; // bits 11..20
    public static final long ORDER_3RD_MAX          =      0x3FFL; // bits 01..10

    public static final long ORDER_1ST_STEP         =   0x100000L; // bit 21
    public static final long ORDER_2ND_STEP         =      0x400L; // bit 11

    public static final long FILTER_DAY             =     0x7C00L;
    public static final long FILTER_MONTH           =    0x78000L;
    public static final long FILTER_YEAR            = 0x7FF80000L;
    public static final long FILTER_YEARMONTH       = FILTER_YEAR|FILTER_MONTH;
    public static final long FILTER_PURE            = DATE_MAX ^ ORDER_3RD_MAX;
    public static final long FILTER_ORDER_1ST_INV   = DATE_MAX ^ ORDER_1ST_MAX;
    public static final long FILTER_ORDER_2ND_INV   = DATE_MAX ^ ORDER_2ND_MAX;
    public static final long FILTER_ORDER_3RD_INV   = DATE_MAX ^ ORDER_3RD_MAX; // FILTER_PURE
    public static final long FILTER_DAY_INV         = DATE_MAX ^ FILTER_DAY;
    public static final long FILTER_MONTH_INV       = DATE_MAX ^ FILTER_MONTH;
    public static final long FILTER_YEAR_INV        = DATE_MAX ^ FILTER_YEAR;

    // hidden elements' sequence numbers are not shown
    public static final long FLAG_VISIBLE           = 0x40000000L; // only for ordinal items
    public static final long FLAG_ORDINAL           = 0x80000000L; // 32nd bit

    public static final long NUMBERED_MIN           = FLAG_VISIBLE | FLAG_ORDINAL;
    public static final long FREE_MIN               = FLAG_ORDINAL;

    // private DateFormatSymbols symbols = new DateFormatSymbols();
    public static final String[] WEEKDAYS = ( new DateFormatSymbols() ).getWeekdays();
    public static final String[] WEEKDAYSSHORT = ( new DateFormatSymbols() ).getShortWeekdays();
    public static final String[] MONTHS = ( new DateFormatSymbols() ).getMonths();

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
        m_date = ( FLAG_ORDINAL | ( o1<<10 ) | o2 );
    }

    // STRING C'TOR
    public Date( String str_date ) {
        m_date = parse_string( str_date );
        if( m_date == 0 )
            m_date = NOT_SET;
    }

    // TEMPORAL METHODS ============================================================================
    public static long get_today( int order ) {
        Calendar cal = Calendar.getInstance();

        return make( cal.get( Calendar.YEAR ), cal.get( Calendar.MONTH ) + 1,
                     cal.get( Calendar.DAY_OF_MONTH ), order );
    }

    public void set( long date ) {
        m_date = date;
    }

    public int get_day() {
        return (int) ( ( m_date & FILTER_DAY ) >> 10 );
    }
    public static int get_day( long d ) {
        return (int) ( ( d & FILTER_DAY ) >> 10 );
    }

    public int get_month() {
        return (int) ( ( m_date & FILTER_MONTH ) >> 15 );
    }
    public static int get_month( long d ) {
        return (int) ( ( d & FILTER_MONTH ) >> 15 );
    }

    public int get_year() {
        return (int) ( ( m_date & FILTER_YEAR ) >> 19 );
    }
    public static int get_year( long d ) {
        return (int) ( ( d & FILTER_YEAR ) >> 19 );
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

    public long get_pure() {
        return( m_date & FILTER_PURE );
    }
    public static long get_pure( long d ) {
        return( d & FILTER_PURE );
    }

    public long get_yearmonth() {
        return( m_date & FILTER_YEARMONTH );
    }

    public void set_year( int y ) {
        if( y >= YEAR_MIN && y <= YEAR_MAX ) {
            m_date &= FILTER_YEAR_INV;
            m_date |= ( y << 19 );
        }
    }

    public void set_month( int m ) {
        if( m < 13 ) {
            m_date &= FILTER_MONTH_INV;
            m_date |= ( m << 15 );
        }
    }

    public static long
    set_day( long date, int d ) {
        if( d < 32 ) {
            date &= FILTER_DAY_INV;
            date |= ( d << 10 );
        }

        return date;
    }
    public void
    set_day( int d ) {
        m_date = set_day( m_date, d );
    }

    public void forward_years( int years ) {
        m_date += make_year( years );
    }

    public static long
    forward_months( long date, int months ) {
        months += get_month( date );
        date &= FILTER_YEAR;   // isolate year
        int mod_months = months % 12;
        if( mod_months == 0 ) {
            date += make_year( ( months / 12 ) - 1 );
            date |= 0x60000;  // make month 12
        }
        else {
            date += make_year( months / 12 );
            date |= make_month( mod_months );
        }

        return date;
    }
    public void
    forward_months( int months ) {
        m_date = forward_months( m_date, months );
    }

    public static long
    backward_months( long d, int months ) {
        int m_diff = ( months - get_month( d ) );
        d &= FILTER_YEAR;   // isolate year
        if( m_diff < 0 )
            d |= make_month( -m_diff );
        else
        {
            d -= make_year( ( m_diff / 12 ) + 1 );
            d |= make_month( 12 - ( m_diff % 12 ) );
        }

        d |= make_day( get_days_in_month( d ) );

        return d;
    }
    public void
    backward_months( int months ) {
        m_date = backward_months( m_date, months );
    }

    public static long
    forward_days( long date, int n_days ) {
        int day_new       = ( get_day( date ) + n_days );
        int days_in_month = get_days_in_month( date );

        if( day_new > days_in_month ) {
            date = forward_months( date, 1 );    // sets day to 1, hence -1 below
            if( day_new > days_in_month + 1 )
                forward_days( date, day_new - days_in_month - 1 );
        }
        else
            date = set_day( date, day_new );

        return date;
    }

    public static long
    backward_days( long date, int n_days ) {
        int day_new = get_day( date ) - n_days;

        if( day_new <= 0 ) {
            backward_months( date, 1 );    // sets day to the last day of previous month
            if( day_new < 0 )
                backward_days( date, -day_new );
        }
        else
            date = set_day( date, day_new );

        return date;
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

    public static int
    get_days_in_month( long d ) {
        switch( get_month( d ) ) {
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                return( is_leap_year( d ) ? 29 : 28 );
        }

        return 31;
    }
    public int
    get_days_in_month() {
        return get_days_in_month( m_date );
    }

    public static boolean
    is_leap_year( long d ) {
        int year = get_year( d );
        if( ( year % 400 ) == 0 )
            return true;
        else if( ( year % 100 ) == 0 )
            return false;
        else return ( year % 4 ) == 0;
    }
    public boolean
    is_leap_year() {
        return is_leap_year( m_date );
    }

    // ORDINAL METHODS =============================================================================
    public static long get_lowest_ordinal( boolean flag_free ) {
        return( flag_free ? FREE_MIN : NUMBERED_MIN );
    }

    public void set_order_1st( int o ){
        m_date &= FILTER_ORDER_1ST_INV; m_date |= ( o << 20 );
    }
    public void set_order_2nd( int o ){
        m_date &= FILTER_ORDER_2ND_INV; m_date |= ( o << 10 );
    }
    public void set_order_3rd( int o ){
        m_date &= FILTER_ORDER_3RD_INV; m_date |= o;
    }

    // the below methods are related to get_pure methods
    public void reset_order_3rd_0(){
        m_date &= FILTER_ORDER_3RD_INV;
    }
    public void reset_order_3rd_1(){
        m_date &= FILTER_ORDER_3RD_INV; m_date |= 0x1;
    }
    public static long get_as_order_3rd_1( long date ){
        return( ( date & FILTER_ORDER_3RD_INV ) | 0x1 );
    }

    public int get_order_3rd(){
        return ( int ) ( m_date & ORDER_3RD_MAX );
    }
    public static int get_order_3rd( long d ){
        return ( int ) ( d & ORDER_3RD_MAX );
    }

    public int get_order_2nd(){
        return ( int ) ( ( m_date & ORDER_2ND_MAX ) >> 10 );
    }
    public static int get_order_2nd( long d ){
        return ( int ) ( ( d & ORDER_2ND_MAX ) >> 10 );
    }

    public int get_order_1st(){
        return ( int ) ( ( m_date & ORDER_1ST_MAX ) >> 20 );
    }
    public static int get_order_1st( long d ){
        return ( int ) ( ( d & ORDER_1ST_MAX ) >> 20 );
    }

    public void set_order( int l, int o ) {
        switch( l )
        {
            case 1: set_order_1st( o ); break;
            case 2: set_order_2nd( o ); break;
            case 3:
            case 4: set_order_3rd( o ); break; // 4: temporal order
        }
    }
    public long get_order( int level ){
        switch( level )
        {
            case 1: return get_order_1st();
            case 2: return get_order_2nd();
            case 3: return get_order_3rd();
        }
        return NOT_SET;
    }

    // right order (least significant order)
    public static long get_order_r( long d ) {
        switch( get_level( d ) )
        {
            case 1: return get_order_1st( d );
            case 2: return get_order_2nd( d );
            case 3:
            case 4: return get_order_3rd( d );
        }
        return NOT_SET;
    }
    public long get_order_r(){
        return get_order_r( m_date );
    }

    public boolean is_1st_level(){
        return( ( m_date & ( ORDER_2ND_MAX | ORDER_3RD_MAX ) ) == 0 );
    }
    public static boolean is_1st_level( long d ){
        return( ( d & ( ORDER_2ND_MAX | ORDER_3RD_MAX ) ) == 0 );
    }

    public boolean is_2nd_level(){
        return( ( m_date & ORDER_2ND_MAX ) != 0 && ( m_date & ORDER_3RD_MAX ) == 0 );
    }
    public static boolean is_2nd_level( long d ){
        return( ( d & ORDER_2ND_MAX ) != 0 && ( d & ORDER_3RD_MAX ) == 0 );
    }

    public boolean is_3rd_level(){
        return( ( m_date & ORDER_2ND_MAX ) != 0 && ( m_date & ORDER_3RD_MAX ) != 0 );
    }
    public static boolean is_3rd_level( long d ){
        return( ( d & ORDER_2ND_MAX ) != 0 && ( d & ORDER_3RD_MAX ) != 0 );
    }

    public static int get_level( long d ) {
        if( !is_ordinal( d ) )  return 4; // temporal
        if( is_1st_level( d ) ) return 1;
        if( is_2nd_level( d ) ) return 2;
        return 3;
    }
    public int get_level(){
        return get_level( m_date );
    }

    public void backward_order_1st(){
        if( get_order_1st() > 0 ) m_date -= ORDER_1ST_STEP;
    }
    public void forward_order_1st(){
        if( ( m_date & ORDER_1ST_MAX ) < ORDER_1ST_MAX ) m_date += ORDER_1ST_STEP;
    }

    public void backward_order_2nd(){
        if( get_order_2nd() > 0 ) m_date -= ORDER_2ND_STEP;
    }
    public void forward_order_2nd(){
        if( ( m_date & ORDER_2ND_MAX ) < ORDER_2ND_MAX ) m_date += ORDER_2ND_STEP;
    }

    public void backward_order( int level ) {
        if     ( level == 1 ) backward_order_1st();
        else if( level == 2 ) backward_order_2nd();
        else if( level == 3 ) m_date--;
    }

    public void forward_order( int level ) {
        if     ( level == 1 ) forward_order_1st();
        else if( level == 2 ) forward_order_2nd();
        else if( level == 3 ) m_date++;
    }

    // RELATIONSHIP METHODS ========================================================================
    public static long
    get_parent( long d ) {
        switch( get_level( d ) )
        {
            case 1: return( DATE_MAX );
            case 2: return( d & FILTER_ORDER_2ND_INV & FILTER_ORDER_3RD_INV );
            default: return( d & FILTER_ORDER_3RD_INV );
        }
    }
    public long
    get_parent(){
        return get_parent( m_date );
    }

    public long get_next_date() {
        Date d = new Date( m_date );
        if( d.get_order_r() < ORDER_MAX )
            d.set_order( d.get_level(), ( int ) d.get_order_r() + 1 );

        return d.m_date;
    }

    public long get_prev_date(){
        Date d = new Date( m_date );
        if( d.get_order_r() > 0 )
            d.set_order( d.get_level(), ( int ) d.get_order_r() - 1 );

        return d.m_date;
    }

    public boolean is_valid() {
        return( get_day() > 0 && get_day() <= get_days_in_month() );
    }
    public boolean is_set() {
        return( m_date != NOT_SET );
    }

    public boolean is_ordinal() {
        return( ( m_date & FLAG_ORDINAL ) != 0 );
    }
    public static boolean is_ordinal( long d ) {
        return( ( d & FLAG_ORDINAL ) != 0 );
    }

    public boolean is_hidden() {
        return( is_ordinal() && ( m_date & FLAG_VISIBLE ) == 0 );
    }
    public static boolean is_hidden( long d ) {
        return( is_ordinal( d ) && ( d & FLAG_VISIBLE ) == 0 );
    }

    public static boolean is_same_kind( long d1, long d2 ) {
        return( ( d1 & ( FLAG_VISIBLE|FLAG_ORDINAL ) ) ==
                ( d2 & ( FLAG_VISIBLE|FLAG_ORDINAL ) ) );
    }
    public static boolean is_sibling( long d1, long d2 ){
        return( is_ordinal( d1 ) && get_parent( d1 ) == get_parent( d2 ) );
    }
    public static boolean is_child_of( long dc, long dp ){
        return( is_ordinal( dc ) && get_parent( dc ) == dp );
    }
    public static boolean is_descendant_of( long dc, long dp ){
        if( dc == dp ) return false;
        if( dp == DATE_MAX ) return true; // DATE_MAX means all dates
        if( ( dc & FLAG_VISIBLE ) != ( dp & FLAG_VISIBLE ) ) return false;
        if( is_1st_level( dp ) ) return( get_order_1st( dc ) == get_order_1st( dp ) );
        if( is_2nd_level( dp ) ) return( get_order_1st( dc ) == get_order_1st( dp ) &&
                                         get_order_2nd( dc ) == get_order_2nd( dp ) );
        return false;
    }

    // MAKE METHODS ================================================================================
    public static long make_year( int y ) {
        return( y << 19 );
    }

    public static long make_month( int m ) {
        return( m << 15 );
    }

    public static long make_day( int d ) {
        return( d << 10 );
    }

    public static long make( int y, int m, int d, int o ) {
        return( ( y << 19 ) | ( m << 15 ) | ( d << 10 ) | o );
    }

    public static long
    make_ordinal( boolean f_num, int o1, int o2, int o3 ) {
        return( ( f_num ? NUMBERED_MIN : FREE_MIN ) | ( o1 << 20 ) | ( o2 << 10 ) | o3 );
    }
    public static long
    make_ordinal( boolean f_num, int o1, int o2 ) {
        return make_ordinal( f_num, o1, o2, 0 );
    }

    // CALCULATE METHODS ===========================================================================
    public int calculate_days_between( Date date2 ) {
        // TODO: STUB!
        Log.e( Lifeograph.TAG, "STUB! STUB! STUB!" );
        return( 0 );
    }

    public static int
    calculate_weeks_between( long date1, long date2 ) {
        int dist = 0;
        long date_former = get_pure( Math.min( date1, date2 ) );
        long date_latter = get_pure( Math.max( date1, date2 ) );

        backward_to_week_start( date_former );
        backward_to_week_start( date_latter );

        while( date_former < date_latter ) {
            forward_days( date_former, 7 );
            dist++;
        }

        return dist;
    }

    public int
    calculate_months_between( long date2 ) {
        return calculate_months_between( m_date, date2 );
    }
    public static int
    calculate_months_between( long date1, long date2 ) {
        int dist = 12 * ( get_year( date2 ) - get_year( date1 ) );
        dist += ( get_month( date2 ) - get_month( date1 ) );

        return ( dist < 0 ? -dist : dist );
    }

    public static int
    calculate_months_between_neg( long date1, long date2 ) {
        int dist = 12 * ( get_year( date2 ) - get_year( date1 ) );
        dist += ( get_month( date2 ) - get_month( date1 ) );

        return dist;
    }

    // STRING METHODS ==============================================================================
    public String get_weekday_str() {
        return WEEKDAYS[ get_weekday() + 1 ];
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
        else
            return 0; //INVALID;

        return date; //OK;
    }

    public static String format_string( long d, String format, char separator ) {
        StringBuilder result = new StringBuilder();

        if( ( d & FLAG_ORDINAL ) != 0 ) {
            result.append( get_order_1st( d ) + 1 );
            if( get_order_2nd( d ) != 0 )
                result.append( "." ).append( get_order_2nd( d ) );
            if( get_order_3rd( d ) != 0 )
                result.append( ".").append( get_order_3rd( d ) );
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
}
