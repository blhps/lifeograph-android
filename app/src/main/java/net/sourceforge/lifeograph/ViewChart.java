/* *********************************************************************************
 * Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)
 * <p/>
 * This file is part of Lifeograph.
 * <p/>
 * Lifeograph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Lifeograph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.
 ***********************************************************************************/

package net.sourceforge.lifeograph;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ViewChart extends View implements GestureDetector.OnGestureListener
{
    // CONSTANTS
    final float border_curve = Lifeograph.getScreenShortEdge() * Lifeograph.sDPIX / 25f;
    final float border_label = border_curve / 3f;
    final float offset_label = border_curve / 6f;
    final float LABEL_HEIGHT = border_curve / 1.75f;
    final float OVERVIEW_COEFFICIENT = border_curve / 2f;
    final float COLUMN_WIDTH_MIN = border_curve * 2f;
    final float STROKE_WIDTH = border_curve / 30f;
    private final float BAR_HEIGHT = LABEL_HEIGHT + offset_label;
    final float label_y = LABEL_HEIGHT;
    final float s_x_min = border_curve + border_label;
    final float s_y_min = border_curve;

    public ViewChart( Context c, AttributeSet attrs ) {
        super( c, attrs );
        context = c;

        // we set a new Path
        mPath = new Path();

        // and we set a new Paint with the desired attributes
        mPaint = new Paint();
        mPaint.setAntiAlias( true );
        mPaint.setColor( Color.BLACK );
        mPaint.setStyle( Paint.Style.STROKE );
        mPaint.setStrokeJoin( Paint.Join.ROUND );
        mPaint.setStrokeWidth( 4 * STROKE_WIDTH );

        mGestureDetector = new GestureDetector( c, this );
    }

    public void setListener( Listener listener ) {
        mListener = listener;
    }

    public void set_points( ChartData points, float zoom_level ) {
        m_data = points;
        m_zoom_level = zoom_level;
        m_span = points != null ? points.get_span() : 0;

        if( m_width > 0 ) { // if on_size_allocate is executed before
            update_col_geom( true );
            invalidate();
        }
    }

    void update_col_geom( boolean flag_new ) {
        if( m_data != null ) {
            // 100% zoom:
            final int step_count_nominal = ( int ) ( m_length / COLUMN_WIDTH_MIN ) + 1;
            final int step_count_min = Math.min( m_span, step_count_nominal );

            m_step_count = ( int ) ( m_zoom_level * ( m_span - step_count_min ) ) + step_count_min;
            m_step_x = ( m_step_count < 3 ? m_length : m_length / ( m_step_count - 1 ) );

            m_ov_height = m_step_count < m_span ?
                    ( float ) Math.log10( m_height ) * OVERVIEW_COEFFICIENT : 0f;

            int mltp = ( m_data.type & ChartData.PERIOD_MASK ) == ChartData.YEARLY ? 1 : 2;
            m_y_max = m_height - mltp * BAR_HEIGHT - m_ov_height;
            m_y_mid = ( m_y_max + s_y_min ) / 2;
            m_amplitude = m_y_max - s_y_min;
            m_coefficient = ( m_data.v_max == m_data.v_min ) ? 0f :
                    m_amplitude / ( float ) ( m_data.v_max - m_data.v_min );

            final int col_start_max = m_span - m_step_count;
            if( flag_new || m_step_start > col_start_max )
                m_step_start = col_start_max;

            // OVERVIEW PARAMETERS
            m_ampli_ov = m_ov_height - 2 * offset_label;
            m_coeff_ov = ( m_data.v_max == m_data.v_min ) ? 0.5f :
                    m_ampli_ov / ( float ) ( m_data.v_max - m_data.v_min );
            m_step_x_ov = m_width - 2 * offset_label;
            if( m_span > 1 )
                m_step_x_ov /= m_span - 1;
        }
    }

    FiltererContainer
    get_filterer_stack() {
        if( m_data.filter != null )
            return m_data.filter.get_filterer_stack();

        return null;
    }

    protected long
    get_period_date( long date ) {
        switch( m_data.type & ChartData.PERIOD_MASK ) {
            case ChartData.WEEKLY:
                Date.backward_to_week_start( date );
                return Date.get_pure( date );
            case ChartData.MONTHLY:
                return( Date.get_yearmonth( date ) + Date.make_day( 1 ) );
            case ChartData.YEARLY:
                return( ( date & Date.FILTER_YEAR ) + Date.make_month( 1 ) + Date.make_day( 1 ) );
        }

        return 0;
    }

    void
    calculate_points( double zoom_level ) {
        m_data.clear_points();

        FiltererContainer   fc = get_filterer_stack();
        Collection< Entry > entries = Diary.diary.m_entries.descendingMap().values();
        double      v = 1.0;
        double      v_plan = 0.0;
        final int   y_axis = m_data.get_y_axis();
        class       Values{
            final double v; final double p;
            public Values( double v, double p ){ this.v = v; this.p = p; } }
        Map< Long, List< Values > > map_values = new TreeMap<>(); // multimap

        for( Entry entry : entries ) {
            if( y_axis == ChartData.TAG_VALUE_PARA ) {
                if( m_data.tag != null && !entry.has_tag( m_data.tag ) )
                    continue;
            }
            else {
                if( entry.is_ordinal() )
                    continue;

                if( m_data.tag != null && m_data.is_tagged_only() && !entry.has_tag( m_data.tag ) )
                    continue;
            }

            if( fc != null && !fc.filter( entry ) )
                continue;

            switch( y_axis ) {
                case ChartData.COUNT:
                    break;
                case ChartData.TEXT_LENGTH:
                    v = entry.get_size();
                    break;
                case ChartData.MAP_PATH_LENGTH:
                    v = entry.get_map_path_length();
                    break;
                case ChartData.TAG_VALUE_ENTRY:
                    if( m_data.tag != null ) {
                        v = entry.get_value_for_tag( m_data );
                        v_plan = entry.get_value_planned_for_tag( m_data );
                    }
                    break;
                case ChartData.TAG_VALUE_PARA:
                    if( m_data.tag != null ) {
                        // first sort the values by date:
                        for( Paragraph para : entry.m_paragraphs ) {
                            long date = para.get_date_broad();
                            if( date == Date.NOT_SET || Date.is_ordinal( date ) )
                                continue;
                            if( !para.has_tag( m_data.tag ) )
                                continue;

                            Lifeograph.MutableInt c = new Lifeograph.MutableInt(); // dummy
                            v = para.get_value_for_tag( m_data, c );
                            v_plan = para.get_value_planned_for_tag( m_data, c );

                            final long periodDate = get_period_date( date );

                            List< Values > list = map_values.get( periodDate );
                            if( list != null )
                                list.add( new Values( v, v_plan ) );
                            else {
                                list = new ArrayList<>();
                                list.add( new Values( v, v_plan ) );
                                map_values.put( periodDate, list );
                            }
                        }
                    }
                    break;
            }

            if( y_axis != ChartData.TAG_VALUE_PARA ) // para values are added in their case
                m_data.add_value( get_period_date( entry.get_date_t() ), v, v_plan );
        }

        if( y_axis == ChartData.TAG_VALUE_PARA ) {
            // feed the values in order:
            for( Map.Entry< Long, List< Values > > kv : map_values.entrySet() ) {
                List< Values > list = kv.getValue();
                for( Values values : list )
                    m_data.add_value( kv.getKey(), values.v, values.p );
            }
        }

        m_data.update_min_max();

        Diary.diary.fill_up_chart_data( m_data );

        if( zoom_level >= 0.0 )
            m_zoom_level = zoom_level;

        m_span = m_data.get_span();

        if( m_width > 0 ) { // if on_size_allocate is executed before
            update_col_geom( zoom_level >= 0.0 );
            // TODO refresh();
        }
    }

    // override onSizeChanged
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
        super.onSizeChanged( w, h, oldw, oldh );

        boolean flag_first = ( m_width < 0 );

        m_width = w;
        m_height = h;

        m_x_max = m_width - border_curve;
        m_length = m_x_max - s_x_min;

        update_col_geom( flag_first );
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw( canvas );

        // reset path
        mPath.reset();

        // BACKGROUND COLOR (contrary to Linux version, background is not white in Android)
        canvas.drawColor( getResources().getColor( R.color.t_lightest ) );

        // HANDLE THERE-IS-TOO-FEW-ENTRIES-CASE SPECIALLY
        if( m_data == null || m_span < 2 ) {
            this.setVisibility( View.GONE );
            return;
        }
        else
            this.setVisibility( View.VISIBLE );

        // NUMBER OF STEPS IN THE PRE AND POST BORDERS
        int pre_steps = ( int ) Math.ceil( s_x_min / m_step_x );
        if( pre_steps > m_step_start )
            pre_steps = m_step_start;

        int post_steps = ( int ) Math.ceil( border_curve / m_step_x );
        if( post_steps > m_span - m_step_count - m_step_start )
            post_steps = m_span - m_step_count - m_step_start;

/* TODO
        // CHAPTER BACKGROUNDS
        double pos_chapter_last = -FLT_MAX;
        double pos_chapter_new = 0.0;
        Color chapter_color_last = "#FFFFFF";
        for(  pc_chapter : m_points.chapters )
        {
            pos_chapter_new = s_x_min + m_step_x * ( pc_chapter.first - m_step_start );
            if( pos_chapter_last != -FLT_MAX )
            {
                if( pos_chapter_new > 0 )
                {
                    if( pos_chapter_new > m_width )
                        pos_chapter_new = m_width;

                    Gdk::Cairo::set_source_rgba( cr, chapter_color_last );
                    cr.rectangle( pos_chapter_last, 0.0, pos_chapter_new - pos_chapter_last, m_y_max );
                    cr.fill();
                }

                if( pos_chapter_new >= m_width )
                    break;
            }

            pos_chapter_last = pos_chapter_new;
            chapter_color_last = pc_chapter.second;
        }
*/

        // YEAR & MONTH BAR
        mPaint.setColor( getResources().getColor( R.color.t_dark ) );
        mPaint.setStyle( Paint.Style.FILL );
        int period = m_data.type & ChartData.PERIOD_MASK;
        canvas.drawRect( 0f, m_y_max, m_width,
                         m_y_max + ( period == ChartData.YEARLY ?
                                     BAR_HEIGHT : BAR_HEIGHT * 2 ),
                         mPaint );

        // VERTICAL LINES
        float cumulative_width = 0f;
        boolean flag_print_label;

        mPaint.setColor( Color.BLACK );
        mPaint.setStrokeWidth( STROKE_WIDTH );
        mPaint.setStyle( Paint.Style.STROKE );
        for( int i = 0; i < m_step_count; ++i ) {
            flag_print_label = ( cumulative_width == 0 );
            cumulative_width += m_step_x;
            if( cumulative_width >= COLUMN_WIDTH_MIN )
                cumulative_width = 0; // reset for the next round

            if( flag_print_label ) {
                mPath.moveTo( s_x_min + m_step_x * i, m_y_max + label_y );
                mPath.lineTo( s_x_min + m_step_x * i, 0.0f );
            }
        }

        // HORIZONTAL LINES
        mPath.moveTo( 0.0f, s_y_min );
        mPath.lineTo( m_width, s_y_min );
        mPath.moveTo( 0.0f, m_y_mid );
        mPath.lineTo( m_width, m_y_mid );

        canvas.drawPath( mPath, mPaint ); // draws both vertical and horizontal lines
        mPath.reset();

        // GRAPH LINE
        mPaint.setColor( getResources().getColor( R.color.t_darker ) );
        mPaint.setStrokeWidth( 4 * STROKE_WIDTH );

        mPath.moveTo( s_x_min - m_step_x * pre_steps,
                      m_y_max - m_coefficient *
                                ( float ) ( m_data.values.get( m_step_start - pre_steps ) -
                                            m_data.v_min ) );

        for( int i = 1; i < m_step_count + pre_steps + post_steps; i++ ) {
            mPath.lineTo( s_x_min + m_step_x * ( i - pre_steps ),
                          m_y_max - m_coefficient *
                                    ( float )
                                            ( m_data.values.get( i + m_step_start - pre_steps ) -
                                              m_data.v_min ) );
        }
        canvas.drawPath( mPath, mPaint );

        // YEAR & MONTH LABELS
        mPaint.setColor( Color.WHITE );
        mPaint.setTextSize( LABEL_HEIGHT );
        mPaint.setStyle( Paint.Style.FILL );

        //mLabelDate.m_date = m_points.start_date;
        if( period == ChartData.MONTHLY )
            mLabelDate.forward_months( m_step_start );
        else
            mLabelDate.set_year( mLabelDate.get_year() + m_step_start );

        int year_last = 0;
        cumulative_width = 0;

        for( int i = 0; i < m_step_count; ++i ) {
            flag_print_label = ( cumulative_width == 0 );
            cumulative_width += m_step_x;
            if( cumulative_width >= COLUMN_WIDTH_MIN )
                cumulative_width = 0; // reset for the next round

            if( period == ChartData.MONTHLY ) {
                if( flag_print_label ) {
                    canvas.drawText( mLabelDate.format_string( "M" ),
                                     s_x_min + m_step_x * i + offset_label,
                                     m_y_max + label_y,
                                     mPaint );

                    if( i == 0 || year_last != mLabelDate.get_year() ) {
                        canvas.drawText( mLabelDate.format_string( "Y" ),
                                         s_x_min + m_step_x * i + offset_label,
                                         m_y_max + BAR_HEIGHT + label_y,
                                         mPaint );
                        year_last = mLabelDate.get_year();
                    }
                }

                mLabelDate.forward_months( 1 );
            }
            else { // YEARLY
                if( flag_print_label ) {
                    canvas.drawText( mLabelDate.format_string( "Y" ),
                                     s_x_min + m_step_x * i + offset_label, m_y_max  + label_y,
                                     mPaint );
                }
                mLabelDate.forward_years( 1 );
            }
        }

        // y LABELS
        mPaint.setColor( Color.BLACK );
        canvas.drawText( m_data.v_max + " " + m_data.unit, border_label,
                         s_y_min - offset_label, mPaint );
        canvas.drawText( m_data.v_min + " " + m_data.unit, border_label,
                         m_y_max - offset_label, mPaint );
    }

    //override the onTouchEvent
    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        this.mGestureDetector.onTouchEvent( event );
        // Be sure to call the superclass implementation
        return super.onTouchEvent( event );
    }

    // GestureDetector.OnGestureListener INTERFACE METHODS
    public boolean onDown( MotionEvent event ) {
        return true;
    }

    public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
        return true;
    }

    public void onLongPress( MotionEvent event ) {
        if( m_data != null && mListener != null ) {
            if( ( m_data.type & ChartData.PERIOD_MASK ) == ChartData.YEARLY )
                mListener.onTypeChanged( ChartData.MONTHLY );
            else
                mListener.onTypeChanged( ChartData.YEARLY );
        }
    }

    public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
        return true;
    }

    public void onShowPress( MotionEvent event ) {
    }

    public boolean onSingleTapUp( MotionEvent event ) {
        return true;
    }

    // INTERFACE
    public interface Listener
    {
        void onTypeChanged( int type );
    }

    // DATA
    Context context;
    private Paint mPaint;
    private Path mPath;

    ChartData m_data = null;
    private Date mLabelDate = new Date(); // this is local in C++

    // GEOMETRICAL VARIABLES
    private int m_width = -1;
    private int m_height = -1;
    private int m_span = 0;
    private int m_step_count = 0;
    private int m_step_start = 0;
    private double m_zoom_level = 1.0;
    private float m_x_max = 0.0f, m_y_max = 0.0f, m_y_mid = 0.0f;
    private float m_amplitude = 0.0f, m_length = 0.0f;
    private float m_step_x = 0.0f, m_coefficient = 0.0f;
    private float m_ov_height = 0.0f;
    private float m_step_x_ov = 0.0f, m_ampli_ov = 0.0f, m_coeff_ov = 0.0f;

    private GestureDetector mGestureDetector;
    private Listener mListener = null;
}
