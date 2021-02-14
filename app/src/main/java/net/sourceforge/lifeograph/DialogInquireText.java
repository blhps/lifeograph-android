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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

public class DialogInquireText extends Dialog
{
    DialogInquireText( Context context, int resTitle, String resDefName, int resActName,
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
        mButtonOk = findViewById( R.id.inquireTextButtonPositive );
        mButtonOk.setText( mActName );
        mButtonOk.setOnClickListener( v -> go() );

        mInput = findViewById( R.id.inquireTextEdit );

        if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mInput.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );

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
        mInput.setOnEditorActionListener( ( v, actionId, event ) -> {
            if( v.getText().length() > 0 ) {
                go();
                return true;
            }
            return false;
        } );
        mInput.setText( mDefName );
        mInput.selectAll();

        Button buttonNegative = findViewById( R.id.inquireTextButtonNegative );
        buttonNegative.setOnClickListener( v -> dismiss() );
    }

    private void go() {
        mListener.onInquireAction( mTitle, mInput.getText().toString() );
        dismiss();
    }

    interface InquireListener
    {
        void onInquireAction( int id, @NonNull String text );
        boolean onInquireTextChanged( int id, @NonNull String text );
    }

    //private Context mContext;
    private EditText mInput;
    private Button mButtonOk;
    private InquireListener mListener;
    private int mTitle;
    private String mDefName;
    private int mActName;
}
