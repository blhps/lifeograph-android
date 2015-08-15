/***********************************************************************************

 Copyright (C) 2012-2015 Ahmet Öztürk (aoz_2@yahoo.com)

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


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import yuku.ambilwarna.AmbilWarnaDialog;


public class DialogTheme extends Dialog
{
    public DialogTheme( Context context, Tag tag ) {
        super( context );
        mTag = tag;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.dialog_theme );

        setTitle( mTag.get_name() );

        mButtonTextColor = ( ImageButton ) findViewById( R.id.button_text_color );
        mButtonBaseColor = ( ImageButton ) findViewById( R.id.button_base_color );
        mButtonHeadingColor = ( ImageButton ) findViewById( R.id.button_heading_color );
        mButtonSubheadingColor = ( ImageButton ) findViewById( R.id.button_subheading_color );
        mButtonHighlightColor = ( ImageButton ) findViewById( R.id.button_highlight_color );

        mButtonReset = ( Button ) findViewById( R.id.button_theme_reset );

        updateButtonColors();

        mButtonTextColor.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                sIndex = 0;
                showColorDialog( mTag.get_theme().color_text + 0xff000000 );
            }
        } );
        mButtonBaseColor.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                sIndex = 1;
                showColorDialog( mTag.get_theme().color_base + 0xff000000 );
            }
        } );
        mButtonHeadingColor.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                sIndex = 2;
                showColorDialog( mTag.get_theme().color_heading + 0xff000000 );
            }
        } );
        mButtonSubheadingColor.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                sIndex = 3;
                showColorDialog( mTag.get_theme().color_subheading + 0xff000000 );
            }
        } );
        mButtonHighlightColor.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                sIndex = 3;
                showColorDialog( mTag.get_theme().color_highlight + 0xff000000 );
            }
        } );


        mButtonReset.setEnabled( mTag.get_has_own_theme() );
        mButtonReset.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                resetTheme();
            }
        } );
    }

    void showColorDialog( int prevColor ) {
        // create a new theme if there is not
        AmbilWarnaDialog dlg = new AmbilWarnaDialog( getContext(), prevColor,
                                                     new AmbilWarnaDialog.OnAmbilWarnaListener()
        {
            public void onOk( AmbilWarnaDialog dialog, int color ) {
                mButtonReset.setEnabled( true );
                if( !mTag.get_has_own_theme() )
                    mTag.create_own_theme_duplicating( Theme.System.get() );

                switch( sIndex ) {
                    case 0:
                        mTag.get_theme().color_text = color;
                        mButtonTextColor.setBackgroundColor( color );
                        break;
                    case 1:
                        mTag.get_theme().color_base = color;
                        mButtonBaseColor.setBackgroundColor( color );
                        break;
                    case 2:
                        mTag.get_theme().color_heading = color;
                        mButtonHeadingColor.setBackgroundColor( color );
                        break;
                    case 3:
                        mTag.get_theme().color_subheading = color;
                        mButtonSubheadingColor.setBackgroundColor( color );
                        break;
                    case 4:
                        mTag.get_theme().color_highlight = color;
                        mButtonHighlightColor.setBackgroundColor( color );
                        break;
                }
            }

            public void onCancel( AmbilWarnaDialog dialog ) {
             // cancel was selected by the user
            }
        } );

        dlg.show();
    }

    private void resetTheme() {
        mTag.reset_theme();
        mButtonReset.setEnabled( false );
        updateButtonColors();
    }

    private void updateButtonColors() {
        mButtonTextColor.setBackgroundColor( mTag.get_theme().color_text );
        mButtonBaseColor.setBackgroundColor( mTag.get_theme().color_base );
        mButtonHeadingColor.setBackgroundColor( mTag.get_theme().color_heading );
        mButtonSubheadingColor.setBackgroundColor( mTag.get_theme().color_subheading );
        mButtonHighlightColor.setBackgroundColor( mTag.get_theme().color_highlight );
    }

    private Tag mTag;
    public static int sIndex;

    ImageButton mButtonTextColor;
    ImageButton mButtonBaseColor;
    ImageButton mButtonHeadingColor;
    ImageButton mButtonSubheadingColor;
    ImageButton mButtonHighlightColor;
    Button mButtonReset;
}
