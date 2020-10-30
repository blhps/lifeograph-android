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


public class ChartPoints
{
    ChartPoints() {
        this( MONTHLY|CUMULATIVE );
    }
    ChartPoints( int t  ) {
        type = t;
    }

    final static int MONTHLY = 0x1;
    final static int YEARLY = 0x2;
    final static int PERIOD_MASK = 0xf;

    final static int BOOLEAN = 0x10;
    final static int CUMULATIVE = 0x20;
    final static int AVERAGE = 0x30;
    final static int VALUE_TYPE_MASK = 0xf0;

    final static int UNDERLAY_PREV_MONTH = 0x100;

    int calculate_distance( Date d1, Date d2 ) {
        switch( type & PERIOD_MASK ) {
            case MONTHLY:
                return d1.calculate_months_between( d2.m_date );
            case YEARLY:
                return Math.abs( d1.get_year() - d2.get_year() );
        }

        return 0; // just to silence the compiler warning
    }

    int calculate_distance_neg( Date d1, Date d2 )
    {
        switch( type & PERIOD_MASK )
        {
            case MONTHLY:
                return Date.calculate_months_between_neg( d1.m_date, d2.m_date );
            case YEARLY:
                return( d2.get_year() - d1.get_year() );
        }

        return 0; // just to silence the compiler warning
    }

    void push_back( Double v ) {
        values.addLast( v );
        if( v < value_min )
            value_min = v;
        if( v > value_max )
            value_max = v;
    }

    void add( int limit, boolean flag_sustain, Double a, Double b ) {
        for( int i = 1; i < limit; i++ ) {
            if( flag_sustain ) // interpolation
                push_back( a + ( i * ( ( b - a ) / limit ) ) );
            else
                push_back( 0.0 );
        }

        push_back( b );
    }

    void add_plain( Date d_last, Date d ) {
        if( d.is_ordinal() )
            return;

        if( start_date == 0 )
            start_date = d.m_date;

        if( values.isEmpty() ) // first value is being entered i.e. v_before is not set
            push_back( 1.0 );
        else if( calculate_distance( d, d_last ) > 0 )
            add( calculate_distance( d, d_last ), false, 0.0, 1.0 );
        else {
            Double v = values.getLast() + 1;
            values.set( values.size() - 1, v );
            if( v < value_min )
                value_min = v;
            if( v > value_max )
                value_max = v;
        }

        d_last.m_date = d.m_date;
    }

    int get_span() {
        return values.size();
    }

    Double value_min = Double.POSITIVE_INFINITY;
    Double value_max = Double.NEGATIVE_INFINITY;

    java.util.LinkedList< Double > values = new java.util.LinkedList<>();
    int type;
    long start_date = 0;

    java.util.List< java.util.Map.Entry< Double, Integer > > chapters;
    String unit = "";
}
