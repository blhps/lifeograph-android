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

// This Code is taken from
// http://stackoverflow.com/questions/2604599/android-imagebutton-with-a-selected-state

package net.sourceforge.lifeograph;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

public class ToggleImageButton extends ImageButton implements Checkable
{
    private OnCheckedChangeListener onCheckedChangeListener;

    public ToggleImageButton( Context context ) {
        super( context );
    }

    public ToggleImageButton( Context context, AttributeSet attrs ) {
        super( context, attrs );
        setChecked( attrs );
    }

    public ToggleImageButton( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );
        setChecked( attrs );
    }

    private void setChecked( AttributeSet attrs ) {
        TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.ToggleImageButton );
        setChecked( a.getBoolean( R.styleable.ToggleImageButton_android_checked, false ) );
        a.recycle();
    }

    //@Override
    public boolean isChecked() {
        return isSelected();
    }

    //@Override
    public void setChecked( boolean checked ) {
        setSelected( checked );

        if( onCheckedChangeListener != null ) {
            onCheckedChangeListener.onCheckedChanged( this, checked );
        }
    }

    //@Override
    public void toggle() {
        setChecked( !isChecked() );
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    public OnCheckedChangeListener getOnCheckedChangeListener() {
        return onCheckedChangeListener;
    }

    public void setOnCheckedChangeListener( OnCheckedChangeListener onCheckedChangeListener ) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    public static interface OnCheckedChangeListener
    {
        public void onCheckedChanged( ToggleImageButton buttonView, boolean isChecked );
    }
}
