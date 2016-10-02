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

        invalidate();
    }

    void update() {
        invalidate();
    }

    void add_item( Canvas canvas, Tag tag ) {
        TagItem titem = new TagItem();
        String label = tag.get_name_and_value( m_ptr2entry, false, true );

        float text_width = mPaint.measureText( label );
        if( m_pos_x + ICON_SIZE + LABEL_OFFSET + text_width + MARGIN > m_width ) {
            m_pos_x = MARGIN;
            m_pos_y += ( ICON_SIZE + VSPACING );
        }

        titem.tag = tag;
        titem.xl = m_pos_x - ITEM_BORDER;
        titem.xr = m_pos_x + text_width + HALF_HEIGHT;
        titem.yl = m_pos_y - ITEM_BORDER;
        titem.yr = titem.yl + ITEM_HEIGHT;

        if( tag == m_add_tag_item )
            titem.xr += ( ICON_SIZE + LABEL_OFFSET );

        m_items.add( titem );

        mPath.reset(); // reset path

        // BACKGROUND
        if( !( tag != m_hovered_tag && tag == m_add_tag_item ) ) {
            float width = ( titem.xr - titem.xl - HALF_HEIGHT );

            mPath.moveTo( titem.xl, titem.yl );
            mPath.rLineTo( width, 0.0f );
            mPath.rLineTo( HALF_HEIGHT, HALF_HEIGHT );
            mPath.rLineTo( HALF_HEIGHT * -1, HALF_HEIGHT );
            mPath.rLineTo( -width, 0 );
            mPath.close();

            if( tag == m_hovered_tag && m_flag_editable )
                mPaint.setStrokeWidth( 4.0f );

            if( tag.get_has_own_theme() ) {
                mPaint.setColor( tag.get_theme().color_base );
                canvas.drawPath( mPath, mPaint );

                mPaint.setColor( tag.get_theme().color_highlight );
                mPaint.setStyle( Paint.Style.STROKE );
                canvas.drawPath( mPath, mPaint );

                mPaint.setPathEffect( new DashPathEffect( new float[] { 5, 10 }, 0 ) );
                mPaint.setColor( tag.get_theme().color_heading );
                canvas.drawPath( mPath, mPaint );
                mPaint.setPathEffect( null );
            }
            else {
                mPaint.setStyle( Paint.Style.STROKE );
                mPaint.setColor( m_color_text_default );
                canvas.drawPath( mPath, mPaint );
            }
            mPaint.setStyle( Paint.Style.FILL );

            if( tag == m_hovered_tag && m_flag_editable )
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
        if( tag.get_has_own_theme() )
            mPaint.setColor( tag.get_theme().color_text );
        else
            mPaint.setColor( m_color_text_default );

        canvas.drawText( label, m_pos_x, m_pos_y + TEXT_HEIGHT, mPaint );

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
        m_items.clear();
        mPaint.setTextSize( TEXT_HEIGHT );
        mPaint.setStyle( Paint.Style.STROKE );
        mPaint.setStrokeWidth( 2.0f );

        java.util.List< Tag > tags = m_ptr2entry.get_tags();

        if( tags.isEmpty() && !m_flag_editable )
        {
            mPaint.setColor( m_color_text_default );
            canvas.drawText( "Not tagged", m_pos_x, m_pos_y + TEXT_HEIGHT, mPaint );
        }

        for( Tag tag : tags )
            add_item( canvas, tag );

        if( m_flag_editable )
            add_item( canvas, m_add_tag_item );

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
    private Entry m_ptr2entry = null;
    private int m_color_text_default;

    class TagItem
    {
        Tag tag;
        float xl, xr, yl, yr;
    }

    class AddTagItem extends Tag
    {
        public AddTagItem()  {
            super( null, "Add Tag", null );
        }
    }

    private java.util.List< TagItem > m_items = new java.util.ArrayList< TagItem >();
    private AddTagItem m_add_tag_item = new AddTagItem();
    private Tag m_hovered_tag = null;

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
