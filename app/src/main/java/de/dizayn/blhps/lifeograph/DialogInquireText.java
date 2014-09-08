/***********************************************************************************

 Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DialogInquireText extends Dialog
{
    public DialogInquireText( Context context, int resTitle, String resDefName, int resActName,
                              InquireListener listener ) {
        super( context );

        //mContext = context;
        mListener = listener;
        mTitle = resTitle;
        mDefName = resDefName;
        mActName = resActName;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.dialog_inquire_text );
        setCancelable( true );

        setTitle( mTitle );
        // setMessage( mMessage );

        // mButtonOk must come before mInput as it is referenced there
        mButtonOk = ( Button ) findViewById( R.id.inquireTextButtonPositive );
        mButtonOk.setText( mActName );
        mButtonOk.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) { go(); }
        } );

        mInput = ( EditText ) findViewById( R.id.inquireTextEdit );
        mInput.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                mButtonOk.setEnabled( s.length() > 0 &&
                        mListener.onInquireTextChanged( mTitle, mInput.getText().toString() ) );
            }
        } );
        mInput.setOnEditorActionListener( new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                if( v.getText().length() > 0 ) {
                    go();
                    return true;
                }
                return false;
            }
        } );
        mInput.setText( mDefName );
        mInput.selectAll();

        Button buttonNegative = ( Button ) findViewById( R.id.inquireTextButtonNegative );
        buttonNegative.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                dismiss();
            }
        } );
    }

    private void go() {
        mListener.onInquireAction( mTitle, mInput.getText().toString() );
        dismiss();
    }

    public interface InquireListener
    {
        public void onInquireAction( int id, String text );
        public boolean onInquireTextChanged( int id, String text );
    }

    //private Context mContext;
    private EditText mInput;
    private Button mButtonOk;
    private InquireListener mListener;
    private int mTitle;
    private String mDefName;
    private int mActName;
}
