/***********************************************************************************
 * Copyright (C) 2012-2016 Ahmet Öztürk (aoz_2@yahoo.com)
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class ViewChart extends View
{
    // CONSTANTS
    static final float      border_curve = 30f;
    static final float      border_label = 10f;
    static final float      offset_label = 5f;
    static final float      label_height = 20f; // different in Android
    static final float      bar_height = 20f;
    static final float      OVERVIEW_COEFFICIENT = 15f;
    static final float      COLUMN_WIDTH_MIN= 45f;
    // CALCULATED CONSTANTS
    static final float      label_y = offset_label + label_height;
    static final float      s_x_min = border_curve + border_label;
    static final float      s_y_min = border_curve;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    Context context;
    private Paint mPaint;
    private float mX, mY;
    private static final float TOLERANCE = 5;

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
        mPaint.setStrokeWidth( 4f );
    }

    public void set_points( ChartPoints points, float zoom_level ) {
        m_points = points;
        m_zoom_level = zoom_level;
        m_span = points != null ? points.get_span() : 0;

        if( m_width > 0 ) { // if on_size_allocate is executed before
            update_col_geom( true );
            invalidate();
        }
    }

    void update_col_geom( boolean flag_new ) {
        if( m_points != null ) {
            // 100% zoom:
            final int step_count_nominal = ( int ) ( m_length / COLUMN_WIDTH_MIN ) + 1;
            final int step_count_min = m_span > step_count_nominal ? step_count_nominal : m_span;

            m_step_count = ( int ) ( m_zoom_level * ( m_span - step_count_min ) ) + step_count_min;
            m_step_x = ( m_step_count < 3 ? m_length : m_length / ( m_step_count - 1 ) );

            m_ov_height = m_step_count < m_span ?
                    ( float ) Math.log10( m_height ) * OVERVIEW_COEFFICIENT : 0f;

            int mltp = ( m_points.type & ChartPoints.PERIOD_MASK ) == ChartPoints.YEARLY ? 1 : 2;
            m_y_max = m_height - mltp * bar_height - m_ov_height;
            m_y_mid = ( m_y_max + s_y_min ) / 2;
            m_amplitude = m_y_max - s_y_min;
            m_coefficient = m_points.value_max.equals( m_points.value_min ) ? 0f :
                    m_amplitude / ( float ) ( m_points.value_max - m_points.value_min );

            final int col_start_max = m_span - m_step_count;
            if( flag_new || m_step_start > col_start_max )
                m_step_start = col_start_max;

            // OVERVIEW PARAMETERS
            m_ampli_ov = m_ov_height - 2 * offset_label;
            m_coeff_ov = m_points.value_max.equals( m_points.value_min ) ? 0.5f :
                    m_ampli_ov / ( float ) ( m_points.value_max - m_points.value_min );
            m_step_x_ov = m_width - 2 * offset_label;
            if( m_span > 1 )
                m_step_x_ov /= m_span - 1;
        }
    }

    // override onSizeChanged
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
        super.onSizeChanged( w, h, oldw, oldh );

        // your Canvas will draw onto the defined Bitmap
        mBitmap = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888 );
        mCanvas = new Canvas( mBitmap );

        boolean flag_first = ( m_width < 0 );

        m_width = w;
        m_height = h;

        m_x_max = m_width - border_curve;
        m_length = m_x_max - s_x_min;

        update_col_geom( flag_first );
    }

    // override onDraw
    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw( canvas );

/* TODO
        // FONT FACE
        cr.set_font_face( m_font_main );

        // BACKGROUND
        cr.rectangle( 0.0, 0.0, m_width, m_height );
        cr.set_source_rgb( 1.0, 1.0, 1.0 );
        cr.fill();*/

        // HANDLE THERE-IS-TOO-FEW-ENTRIES-CASE SPECIALLY
        if( m_points == null || m_span < 2 ) {
            /*cr.set_font_size( 1.5 * label_height );
            cr.set_source_rgb( 0.0, 0.0, 0.0 );
            Cairo::TextExtents te;
            cr.get_text_extents( _( "INSUFFICIENT DATA" ), te );
            cr.move_to( ( m_width - te.width ) / 2 , m_height / 2 );
            cr.show_text( _( "INSUFFICIENT DATA" ) );*/
            return;
        }

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
        mPaint.setColor( Color.LTGRAY ); // TODO: ( 0.85, 0.85, 0.85 );
        int period = m_points.type & ChartPoints.PERIOD_MASK;
        if( period != ChartPoints.YEARLY )
            canvas.drawRect( 0f, m_y_max, m_width, bar_height * 2, mPaint );
        else
            canvas.drawRect( 0f, m_y_max, m_width, bar_height, mPaint );

        // VERTICAL LINES
        float cumulative_width = 0f;
        boolean flag_print_label = false;

        mPaint.setColor( Color.DKGRAY ); // TODO: ( 0.6, 0.6, 0.6 );
        mPaint.setStrokeWidth( 1.0f );
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
        mPaint.setColor( Color.RED ); // TODO: ( 0.9, 0.3, 0.3 );
        mPaint.setStrokeWidth( 4.0f );

        mPath.moveTo( s_x_min - m_step_x * pre_steps,
                      m_y_max - m_coefficient *
                                ( float ) ( m_points.values.get( m_step_start - pre_steps ) -
                                            m_points.value_min ) );

        for( int i = 1; i < m_step_count + pre_steps + post_steps; i++ ) {
            mPath.lineTo( s_x_min + m_step_x * ( i - pre_steps ),
                          m_y_max - m_coefficient *
                                    ( float )
                                            ( m_points.values.get( i + m_step_start - pre_steps ) -
                                              m_points.value_min ) );
        }
        canvas.drawPath( mPath, mPaint );

        // YEAR & MONTH LABELS
        mPaint.setColor( Color.BLACK );
        mPaint.setTextSize( label_height );
        mPaint.setStrokeWidth( 0f );

        mLabelDate.m_date = m_points.start_date;
        if( period == ChartPoints.MONTHLY )
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

            if( period == ChartPoints.MONTHLY ) {
                if( flag_print_label ) {
                    canvas.drawText( mLabelDate.format_string( "M" ),
                                     s_x_min + m_step_x * i + offset_label,
                                     m_y_max + label_y,
                                     mPaint );

                    if( i == 0 || year_last != mLabelDate.get_year() ) {
                        canvas.drawText( mLabelDate.format_string( "Y" ),
                                         s_x_min + m_step_x * i + offset_label,
                                         m_y_max + bar_height + label_y / 1.5f,
                                         mPaint );
                        year_last = mLabelDate.get_year();
                    }
                }

                mLabelDate.forward_month();
            }
            else { // YEARLY
                if( flag_print_label ) {
                    canvas.drawText( mLabelDate.format_string( "Y" ),
                                     s_x_min + m_step_x * i + offset_label, m_y_max  + label_y,
                                     mPaint );
                }
                mLabelDate.forward_year();
            }
        }

        // y LABELS
        canvas.drawText( m_points.value_max.toString() + " " + m_points.unit, border_label,
                         s_y_min - offset_label, mPaint );
        canvas.drawText( m_points.value_min.toString() + " " + m_points.unit, border_label,
                         m_y_max - offset_label, mPaint );

/* TODO
        // OVERVIEW
        if( m_step_count < m_span )
        {
            // OVERVIEW REGION
            set_source_rgb( 0.7, 0.7, 0.7 );
            rectangle( 0.0, m_height - m_ov_height, m_width, m_ov_height );
            cr.fill();

            if( m_flag_pointer_hovered )
                cr.set_source_rgb( 1.0, 1.0, 1.0 );
            else
                cr.set_source_rgb( 0.95, 0.95, 0.95 );
            cr.rectangle( offset_label + m_step_start * m_step_x_ov, m_height - m_ov_height,
                           ( m_step_count - 1 ) * m_step_x_ov, m_ov_height );
            cr.fill();
            //cr.restore();

            // OVERVIEW LINE
            cr.set_source_rgb( 0.9, 0.3, 0.3 );
            cr.set_line_join( Cairo::LINE_JOIN_BEVEL );
            cr.set_line_width( 2.0 );

            //date.m_date = m_points.begin().first;
            cr.move_to( offset_label, m_height - offset_label - m_coeff_ov *
                    ( m_points.values[ 0 ] - m_points.value_min ) );
            for( int i = 1; i < m_span; ++i )
            {
                //date.forward_month();
                cr.line_to( offset_label + m_step_x_ov * i, m_height - offset_label - m_coeff_ov *
                        ( m_points.values[ i ] - m_points.value_min ) );
            }
            cr.stroke();

            // DIVIDER
            if( m_flag_pointer_hovered )
                cr.set_source_rgb( 0.2, 0.2, 0.2 );
            else
                cr.set_source_rgb( 0.45, 0.45, 0.45 );
            cr.rectangle( 1.0, m_height - m_ov_height, m_width - 2.0, m_ov_height - 1.0 );
            cr.stroke();
        }
*/

    }

    // when ACTION_DOWN start touch according to the x,y values
    private void startTouch( float x, float y ) {
        mPath.moveTo( x, y );
        mX = x;
        mY = y;
    }

    // when ACTION_MOVE move touch according to the x,y values
    private void moveTouch( float x, float y ) {
        float dx = Math.abs( x - mX );
        float dy = Math.abs( y - mY );
        if( dx >= TOLERANCE || dy >= TOLERANCE ) {
            mPath.quadTo( mX, mY, ( x + mX ) / 2, ( y + mY ) / 2 );
            mX = x;
            mY = y;
        }
    }

    public void clearCanvas() {
        mPath.reset();
        invalidate();
    }

    // when ACTION_UP stop touch
    private void upTouch() {
        mPath.lineTo( mX, mY );
    }

    //override the onTouchEvent
    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        float x = event.getX();
        float y = event.getY();

        switch( event.getAction() ) {
            case MotionEvent.ACTION_DOWN:
                startTouch( x, y );
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                moveTouch( x, y );
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                upTouch();
                invalidate();
                break;
        }
        return true;
    }

    // DATA
    private ChartPoints m_points = null;
    private Date mLabelDate = new Date(); // this is local in C++

    // GEOMETRY
    private int m_width = -1;
    private int m_height = -1;
    private int m_span = 0;
    private int m_step_count = 0;
    private int m_step_start = 0;
    private float m_zoom_level = 1.0f;

    float m_x_max = 0.0f, m_y_max = 0.0f, m_y_mid = 0.0f;
    float m_amplitude = 0.0f, m_length = 0.0f;
    float m_step_x = 0.0f, m_coefficient = 0.0f;
    float m_ov_height = 0.0f;
    float m_step_x_ov = 0.0f, m_ampli_ov = 0.0f, m_coeff_ov = 0.0f;
}
