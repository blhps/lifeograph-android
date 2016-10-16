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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


public class ViewEntryTags extends View implements GestureDetector.OnGestureListener
{
    // CONSTANTS
    static final float      MARGIN = 18f;
    static final float      HSPACING = 15f;
    static final float      VSPACING = 14f;
    static final float      LABEL_OFFSET = 2f;
    static final float      TEXT_HEIGHT = 25f; // different in Android
    static final float      ITEM_BORDER = 4f;  // must be < MARGIN
    static final int        ICON_SIZE = 16;
    static final float      ITEM_HEIGHT =  40f;
    static final float      HALF_HEIGHT = 20f;

    public ViewEntryTags( Context c, AttributeSet attrs ) {
        super( c, attrs );
        context = c;

        // we set a new Path
        mPath = new Path();

        // and we set a new Paint with the desired attributes
        mPaint = new Paint();
        mPaint.setAntiAlias( true );
        mPaint.setColor( Color.BLACK );
        mPaint.setStyle( Paint.Style.FILL );
        mPaint.setStrokeJoin( Paint.Join.ROUND );
        mPaint.setStrokeWidth( 4f );

        mGestureDetector = new GestureDetector( c, this );

        m_color_text_default = Color.BLACK;
    }

    public void setListener( Listener listener ) {
        mListener = listener;
    }

    public void set_entry( Entry entry ) {
        m_ptr2entry = entry;
        m_pos_x = MARGIN;

        m_items.clear();
        java.util.List< Tag > tags = m_ptr2entry.get_tags();
        for( Tag tag : tags ) {
            TagItem ti = new TagItem( tag.get_name_and_value( entry, false, true ) );
            ti.tag = tag;

            m_items.add( ti );
        }

        if( m_flag_editable ) {
            TagItem ti_add = new TagItem( "Add Tag" );
            m_items.add( ti_add );
        }

        invalidate();
    }

    void update() {
        invalidate();
    }

    void add_item( Canvas canvas, TagItem ti ) {
        float text_width = mPaint.measureText( ti.label );
        if( m_pos_x + ICON_SIZE + LABEL_OFFSET + text_width + MARGIN > m_width ) {
            m_pos_x = MARGIN;
            m_pos_y += ( ICON_SIZE + VSPACING );
        }

        ti.xl = m_pos_x - ITEM_BORDER;
        ti.xr = m_pos_x + text_width + HALF_HEIGHT;
        ti.yl = m_pos_y - ITEM_BORDER;
        ti.yr = ti.yl + ITEM_HEIGHT;

        if( ti.tag == null )
            ti.xr += ( ICON_SIZE + LABEL_OFFSET );

        mPath.reset(); // reset path

        // BACKGROUND
        if( ti.hovered || ti.tag != null ) {
            float width = ( ti.xr - ti.xl - HALF_HEIGHT );

            mPath.moveTo( ti.xl, ti.yl );
            mPath.rLineTo( width, 0.0f );
            mPath.rLineTo( HALF_HEIGHT, HALF_HEIGHT );
            mPath.rLineTo( HALF_HEIGHT * -1, HALF_HEIGHT );
            mPath.rLineTo( -width, 0 );
            mPath.close();

            if( ti.hovered && m_flag_editable )
                mPaint.setStrokeWidth( 5.0f );

            if( ti.tag != null && ti.tag.get_has_own_theme() ) {
                mPaint.setColor( ti.tag.get_theme().color_base );
                canvas.drawPath( mPath, mPaint );

                mPaint.setColor( ti.tag.get_theme().color_highlight );
                mPaint.setStyle( Paint.Style.STROKE );
                canvas.drawPath( mPath, mPaint );

                mPaint.setPathEffect( new DashPathEffect( new float[] { 5, 10 }, 0 ) );
                mPaint.setColor( ti.tag.get_theme().color_heading );
                canvas.drawPath( mPath, mPaint );
                mPaint.setPathEffect( null );
            }
            else {
                mPaint.setStyle( Paint.Style.STROKE );
                mPaint.setColor( m_color_text_default );
                canvas.drawPath( mPath, mPaint );
            }
            mPaint.setStyle( Paint.Style.FILL );

            if( ti.hovered && m_flag_editable )
                mPaint.setStrokeWidth( 2.0f );  // restore the stroke width
        }

        // ICON
//        if( tag == m_add_tag_item ) {
//            cr->set_source( m_image_surface_add, m_pos_x, m_pos_y );
//            cr->rectangle( m_pos_x, m_pos_y, ICON_SIZE, ICON_SIZE );
//            cr->clip();
//            cr->paint();
//            cr->reset_clip();
//
//            m_pos_x += ( ICON_SIZE + LABEL_OFFSET );
//        }

        // LABEL
        if( ti.tag != null && ti.tag.get_has_own_theme() )
            mPaint.setColor( ti.tag.get_theme().color_text );
        else
            mPaint.setColor( m_color_text_default );

        canvas.drawText( ti.label, m_pos_x, m_pos_y + TEXT_HEIGHT, mPaint );

        m_pos_x += ( text_width + HALF_HEIGHT + HSPACING );
    }

    // override onSizeChanged
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
        super.onSizeChanged( w, h, oldw, oldh );

        m_width = w;
        m_height = h;
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw( canvas );

        if( m_ptr2entry == null )
            return;

        m_pos_x = m_pos_y = MARGIN;
        mPaint.setTextSize( TEXT_HEIGHT );
        mPaint.setStyle( Paint.Style.FILL );
        mPaint.setStrokeWidth( 2.0f );

        if( m_items.isEmpty() && !m_flag_editable ) {
            mPaint.setColor( m_color_text_default );
            canvas.drawText( "Not tagged", m_pos_x, m_pos_y + TEXT_HEIGHT, mPaint );
        }

        for( TagItem ti : m_items )
            add_item( canvas, ti );

        // TODO: set_size_request( -1, m_pos_y + TEXT_HEIGHT + MARGIN );
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
        for( TagItem ti : m_items ) {
            if( event.getX() > ti.xl && event.getX() < ti.xr ) {
                ti.hovered = true;
                update();
                break;
            }
        }
        return true;
    }

    public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
        return true;
    }

    public void onLongPress( MotionEvent event ) {
    }

    public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
        return true;
    }

    public void onShowPress( MotionEvent event ) {
        Tag tag = null;
        for( TagItem ti : m_items ) {
            if( ti.hovered ) {
                ti.hovered = false;
                tag = ti.tag;
            }
        }
        update();
        mListener.onTagSelected( tag );
    }

    public boolean onSingleTapUp( MotionEvent event ) {
        return true;
    }

    // INTERFACE
    public interface Listener
    {
        void onTagSelected( Tag tag );
    }

    // DATA
    private Entry m_ptr2entry = null;
    private int m_color_text_default;

    class TagItem
    {
        TagItem( String l ) {
            label = l;
        }

        Tag tag = null;
        String label = "";
        float xl, xr, yl, yr;
        boolean hovered = false;
    }

    private java.util.List< TagItem > m_items = new java.util.ArrayList< TagItem >();

    // GEOMETRICAL VARIABLES
    private int m_width = 0;
    private int m_height = 0;
    float m_pos_x = MARGIN;
    float m_pos_y = MARGIN;

    Context context;
    private Paint mPaint;
    private Path mPath;

    boolean m_flag_editable = true;    // not read-only

    private GestureDetector mGestureDetector;
    private Listener mListener = null;
}
