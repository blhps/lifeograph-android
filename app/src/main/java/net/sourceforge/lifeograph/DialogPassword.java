/***********************************************************************************

 Copyright (C) 2012-2016 Ahmet Öztürk (aoz_2@yahoo.com)

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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


class DialogPassword extends Dialog
{
    enum DPAction {
        DPA_LOGIN, DPA_AUTHENTICATE, DPA_ADD,
        DPAR_AUTH_FAILED // just returned as a result
    }

    DialogPassword( Context context, Diary diary, DPAction action, Listener listener ) {
        super( context );

        mListener = listener;
        mAction = action;
        mDiary = diary;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.dialog_password );
        setCancelable( true );

        // mButtonOk must come before mInput as it is referenced there
        mButtonOk = ( Button ) findViewById( R.id.passwordButtonPositive );
        mButtonOk.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) { go(); }
        } );

        mInput1 = ( EditText ) findViewById( R.id.edit_password_1 );
        mInput2 = ( EditText ) findViewById( R.id.edit_password_2 );

        mImage1 = ( ImageView ) findViewById( R.id.image_password_1 );
        mImage2 = ( ImageView ) findViewById( R.id.image_password_2 );

        switch( mAction ) {
            case DPA_LOGIN:
                setTitle( "Enter password for " + mDiary.get_name() );
                mButtonOk.setText( Lifeograph.getStr( R.string.open ) );
                mInput2.setVisibility( View.GONE );
                mImage1.setVisibility( View.GONE );
                mImage2.setVisibility( View.GONE );
                break;
            case DPA_AUTHENTICATE:
                setTitle( "Enter the current password" );
                mButtonOk.setText( Lifeograph.getStr( R.string.authenticate ));
                mInput2.setVisibility( View.GONE );
                mImage1.setVisibility( View.GONE );
                mImage2.setVisibility( View.GONE );
                break;
            case DPA_ADD:
                setTitle( "Enter the new password" );
                mButtonOk.setText( Lifeograph.getStr( R.string.set_password ) );
                break;
        }

        if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mInput1.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );

        mInput1.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                check();
            }
        } );
        mInput1.setOnEditorActionListener( new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                if( mAction != DPAction.DPA_ADD &&
                    mInput1.getText().length() >= Diary.PASSPHRASE_MIN_SIZE ) {
                    go();
                    return true;
                }
                return false;
            }
        } );
        mInput2.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                check();
            }
        } );

        Button buttonNegative = ( Button ) findViewById( R.id.passwordButtonNegative );
        buttonNegative.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                dismiss();
            }
        } );
    }

    private void check() {
        String passphrase = mInput1.getText().toString();
        String passphrase2 = mInput2.getText().toString();

        if( passphrase.length() >= Diary.PASSPHRASE_MIN_SIZE ) {
            mImage1.setImageResource( R.drawable.ic_todo_done );
            if( mAction != DPAction.DPA_ADD ) {
                mButtonOk.setEnabled( true );
            }
            else if( passphrase.equals( passphrase2 ) ) {
                mImage2.setImageResource( R.drawable.ic_todo_done );
                mButtonOk.setEnabled( true );
            }
            else {
                mImage2.setImageResource( passphrase2.isEmpty() ?
                                                  R.drawable.ic_todo_open :
                                                  R.drawable.ic_todo_canceled );
                mButtonOk.setEnabled( false );
            }
        }
        else {
            mImage1.setImageResource( passphrase.isEmpty() ?
                                              R.drawable.ic_todo_open :
                                              R.drawable.ic_todo_canceled );
            mImage2.setImageResource( passphrase2.isEmpty() ?
                                              R.drawable.ic_todo_open :
                                              R.drawable.ic_todo_canceled );
            mButtonOk.setEnabled( false );
        }
    }

    private void go() {
        switch( mAction ) {
            case DPA_LOGIN:
            case DPA_ADD:
                mDiary.set_passphrase( mInput1.getText().toString() );
                mListener.onDPAction( mAction );
                break;
            case DPA_AUTHENTICATE:
                if( mDiary.compare_passphrase( mInput1.getText().toString() ) )
                    mListener.onDPAction( mAction );
                else
                    mListener.onDPAction( DPAction.DPAR_AUTH_FAILED );
                break;
        }
        dismiss();
    }

    public interface Listener
    {
        void onDPAction( DPAction action );
    }

    private EditText mInput1;
    private EditText mInput2;
    private ImageView mImage1;
    private ImageView mImage2;
    private Button mButtonOk;
    private Listener mListener;
    private DPAction mAction;
    private Diary mDiary;
}
