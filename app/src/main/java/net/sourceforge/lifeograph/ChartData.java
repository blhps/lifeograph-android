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
    final static int MAP_PATH_LENGTH         = 0x3000;
    final static int TAG_VALUE_ENTRY         = 0x4000;
    final static int TAG_VALUE_PARA          = 0x5000;
    final static int Y_AXIS_MASK             = 0xF000;

    final static int DEFAULT                 = MONTHLY|CUMULATIVE_CONTINUOUS;

    ChartData( Diary d, int t  ) {
        m_p2diary = d;
        type = t;
    }
    ChartData( Diary d ) {
        this( d, DEFAULT );
    }

    void
    clear() {
        type            = 0;
        tag             = null;
        para_filter_tag = null;
        filter          = null;
        unit            = "";
    }

    String
    get_as_string() {
        String chart_def = "";

        switch( type & Y_AXIS_MASK ) {
            case COUNT: // entry count
                chart_def += "Gyc\n";
                break;
            case TEXT_LENGTH: // text length
                chart_def += "Gyl\n";
                break;
            case MAP_PATH_LENGTH: // map path length
                chart_def += "Gym\n";
                break;
            case TAG_VALUE_ENTRY: // tag value
                chart_def += ( "Gyt" + ( tag != null ? tag.get_id() : DiaryElement.DEID_UNSET )
                                     + '\n' );
                chart_def += ( "Gp" + ( para_filter_tag != null ? para_filter_tag.get_id() :
                                                                  DiaryElement.DEID_UNSET )
                                    + '\n' );
                break;
            case TAG_VALUE_PARA: // tag value
                chart_def += ( "Gyp" + ( tag != null ? tag.get_id() : DiaryElement.DEID_UNSET )
                                     + '\n' ) ;
                chart_def += ( "Gp" + ( para_filter_tag != null ? para_filter_tag.get_id() :
                                                                  DiaryElement.DEID_UNSET )
                                    + '\n' );
                break;
        }

        if( filter != null )
            chart_def += ( "Gf" + filter.get_name() + '\n' );

        chart_def += ( "Go" + ( ( type & TAGGED_ONLY ) != 0 ? 'T' : '-' ) );

        switch( type & UNDERLAY_MASK ) {
            case UNDERLAY_PREV_YEAR:    chart_def += 'Y'; break;
            case UNDERLAY_PLANNED:      chart_def += 'P'; break;
            case 0:                     chart_def += '_'; break;
        }
        switch( type & PERIOD_MASK ) {
            case WEEKLY:                chart_def += 'W'; break;
            case MONTHLY:               chart_def += 'M'; break;
            case YEARLY:                chart_def += 'Y'; break;
        }
        switch( type & VALUE_TYPE_MASK ) {
            case CUMULATIVE_PERIODIC:   chart_def += 'P'; break;
            case CUMULATIVE_CONTINUOUS: chart_def += 'C'; break;
            case AVERAGE:               chart_def += 'A'; break;
        }

        return chart_def;
    }

    void
    set_from_string( String chart_def ) {
        Lifeograph.MutableString line        = new Lifeograph.MutableString();
        Lifeograph.MutableInt    line_offset = new Lifeograph.MutableInt();

        clear();

        while( Lifeograph.getLine( chart_def, line_offset, line ) ) {
            if( line.v.length() < 2 )   // should never occur
                continue;

            switch( line.v.charAt( 1 ) ) {
                case 'y':   // y axis
                {
                    switch( line.v.charAt( 2 ) ) {
                        case 'c':   // count
                            type |= COUNT;
                            break;
                        case 'l':   // text length
                            type |= TEXT_LENGTH;
                            break;
                        case 'm':   // map path length
                            type |= MAP_PATH_LENGTH;
                            break;
                        case 't':   // tag value
                            type |= TAG_VALUE_ENTRY;
                            tag = m_p2diary.get_entry_by_id(
                                    Integer.parseInt( line.v.substring( 3 ) ) );
                            break;
                        case 'p':   // tag value
                            type |= TAG_VALUE_PARA;
                            tag = m_p2diary.get_entry_by_id(
                                    Integer.parseInt( line.v.substring( 3 ) ) );
                            break;
                    }
                    break;
                }
                case 'p': // para filter tag
                    para_filter_tag = m_p2diary.get_entry_by_id(
                            Integer.parseInt( line.v.substring( 2 ) ) );
                    break;
                case 'f':   // filter
                    filter = m_p2diary.get_filter( line.v.substring( 2 ) );
                    break;
                case 'o':
                    if( line.v.charAt( 2 ) == 'T' )
                        type |= TAGGED_ONLY;

                    switch( line.v.charAt( 3 ) ) {
                        case 'Y':
                            type |= UNDERLAY_PREV_YEAR;
                            break;
                        case 'P':
                            type |= UNDERLAY_PLANNED;
                            break;
                    }
                    switch( line.v.charAt( 4 ) ) {
                        case 'W':
                            type |= WEEKLY;
                            break;
                        case 'M':
                            type |= MONTHLY;
                            break;
                        case 'Y':
                            type |= YEARLY;
                            break;
                    }
                    switch( line.v.charAt( 5 ) ) {
                        case 'P':
                            type |= CUMULATIVE_PERIODIC;
                            break;
                        case 'C':
                            type |= CUMULATIVE_CONTINUOUS;
                            break;
                        case 'A':
                            type |= AVERAGE;
                            break;
                    }
                    break;
                default:
                    Log.d( Lifeograph.TAG, "Unrecognized chart string: " + line );
                    break;
            }
        }

        refresh_unit();
    }

    void
    set_type_sub( int t ) {
        switch( t ) {
            case TAGGED_ONLY:
                type |= TAGGED_ONLY;
                break;
            case -TAGGED_ONLY:
                type &= ( ~TAGGED_ONLY );
                break;
            default:
                if( 0 == ( t & PERIOD_MASK ) )          t |= ( type & PERIOD_MASK );
                if( 0 == ( t & VALUE_TYPE_MASK ) )      t |= ( type & VALUE_TYPE_MASK );
                if( 0 == ( t & Y_AXIS_MASK ) )          t |= ( type & Y_AXIS_MASK );
                if( 0 == ( t & UNDERLAY_MASK ) )        t |= ( type & UNDERLAY_MASK );
                if( 0 == ( t & TAGGED_ONLY ) )          t |= ( type & TAGGED_ONLY );

                type = t;
                break;
        }

        refresh_unit();
    }

    void
    refresh_unit() {
        switch( type & Y_AXIS_MASK ) {
            case MAP_PATH_LENGTH:
                unit = ( Lifeograph.sOptImperialUnits ? "mi" : "km" );
                break;
            case TAG_VALUE_ENTRY:
            case TAG_VALUE_PARA:
                unit = ( tag != null ? tag.m_unit : "" );
                break;
            default:
                unit = "";
        }
    }

    int
    calculate_distance( long d1, long d2 ) {
        switch( type & PERIOD_MASK ) {
            case WEEKLY:
                return Date.calculate_weeks_between( d1, d2 );
            case MONTHLY:
                return Date.calculate_months_between( d1, d2 );
            case YEARLY:
            default:
                return abs( Date.get_year( d1 ) - Date.get_year( d2 ) );
        }
    }

    long
    forward_date( long date, int n ) {
        switch( type & PERIOD_MASK ) {
            case WEEKLY:
                date = Date.forward_days( date, 7 * n );
                break;
            case MONTHLY:
                date = Date.forward_months( date, n );
                break;
            case YEARLY:
                date = Date.forward_months( date, 12 * n );
                break;
        }

        return date;
    }

    void
    clear_points() {
        values.clear();
        values_plan.clear();
        dates.clear();
        counts.clear();
        chapters.clear();

        v_min      = Double.MAX_VALUE;
        v_max      = -Double.MAX_VALUE;
        v_plan_min = Double.MAX_VALUE;
        v_plan_max = -Double.MAX_VALUE;
    }

    void
    add_value( long date, double value, double value_plan ) {
        if( values.isEmpty() )
            push_back_value( value, value_plan );
        else {
            double v_last      = values.getLast();
            double v_plan_last = values_plan.getLast();

            if( date == dates.getLast() ) {
                switch( get_type() ) {
                    case CUMULATIVE_PERIODIC:
                    case CUMULATIVE_CONTINUOUS:
                        values.removeLast();
                        values_plan.removeLast();
                        push_back_value( v_last + value, v_plan_last + value_plan );
                        counts.set( counts.size() - 1, counts.getLast() + 1 );
                        break;
                    case AVERAGE:
                        v_last *= counts.getLast();
                        v_last += value;
                        v_plan_last *= counts.getLast();
                        v_plan_last += value_plan;
                        counts.set( counts.size() - 1, counts.getLast() + 1 );
                        values.removeLast();
                        values_plan.removeLast();
                        push_back_value( v_last / counts.getLast(), v_plan_last / counts.getLast() );
                        break;
                }
                return;
            }
            else {
                int    steps_between     = calculate_distance( date, dates.getLast() );
                double value_offset      = ( value - v_last ) / steps_between;
                double value_plan_offset = ( value_plan - v_plan_last ) / steps_between;
                long   date_inter        = dates.getLast();

                for( int i = 1; i < steps_between; i++ ) {
                    date_inter = forward_date( date_inter, 1 );
                    switch( get_type() ) {
                        case CUMULATIVE_PERIODIC:
                            push_back_value( 0.0, 0.0 );
                            break;
                        case CUMULATIVE_CONTINUOUS:
                            push_back_value( v_last, v_plan_last );
                            break;
                        case AVERAGE:
                            push_back_value( v_last + ( i * value_offset ),
                                             v_plan_last + ( i * value_plan_offset ) );
                            break;
                    }

                    dates.add( date_inter );
                    counts.add( 0 );
                }
            }
            if( get_type() == CUMULATIVE_CONTINUOUS )
                push_back_value( value + v_last, value_plan + v_plan_last );
            else
                push_back_value( value, value_plan );
        }

        dates.add( date );
        counts.add( 1 );
    }

    protected void
    push_back_value( double v_real, double v_plan ) {
        values.add( v_real );
        values_plan.add( v_plan );
    }
    protected void
    push_back_value( double v_real ) { push_back_value( v_real, 0.0 ); }

    void
    update_min_max() {
        for( double v : values ) {
            if( v < v_min )
                v_min = v;
            if( v > v_max )
                v_max = v;
        }

        for( double v : values_plan ) {
            if( v < v_plan_min )
                v_plan_min = v;
            if( v > v_plan_max )
                v_plan_max = v;
        }
    }

    int
    get_span() {
        return values.size();
    }

    int
    get_period() { return( type & PERIOD_MASK ); }
    boolean
    is_monthly() { return( ( type & PERIOD_MASK ) == MONTHLY ); }
    int
    get_type() { return( type & VALUE_TYPE_MASK ); }
    boolean
    is_average() { return( ( type & VALUE_TYPE_MASK ) == AVERAGE ); }
    boolean
    is_tagged_only() {
        return( ( type & TAGGED_ONLY ) != 0 &&
                ( ( type & Y_AXIS_MASK ) == TAG_VALUE_ENTRY ||
                  ( type & Y_AXIS_MASK ) == TAG_VALUE_PARA ) ); }
    int
    get_y_axis() { return( type & Y_AXIS_MASK ); }
    long
    get_start_date() { return( dates.isEmpty() ? Date.NOT_SET : dates.get( 0 ) ); }
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
    List< Pair< Double, Integer > > chapters = new ArrayList<>();

    String          unit = "";
    int             type;
    Entry           tag = null;
    Entry           para_filter_tag = null;
    Filter          filter = null;

    Diary           m_p2diary;
}
