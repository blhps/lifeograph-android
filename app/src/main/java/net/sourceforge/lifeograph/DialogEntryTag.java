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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class DialogEntryTag extends Dialog
{
    DialogEntryTag( Context context, Entry tag, Entry entry, Listener listener ) {
        super( context );

        mListener = listener;
        mTag = tag;
        mEntry = entry;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.dialog_entry_tags );
        setCancelable( true );

        // mButtonAction must come before mInput as it is referenced there
        mButtonAction = findViewById( R.id.entry_tag_action );
        mButtonAction.setOnClickListener( v -> go() );

        mButtonTheme = findViewById( R.id.entry_set_theme );
        mButtonTheme.setOnClickListener( v -> {
            mListener.onTagsChanged();
            dismiss();
        } );

        mInput1 = findViewById( R.id.entry_tag_edit );
//        if( mTag != null ) // add new tag case
//            mInput1.setText( mTag.get_name_and_value( mEntry, true, true ) );

        String[] tags = new String[ Diary.d.m_entries.size() ];
        int i = 0;
        for( Entry tag : Diary.d.m_entries.values() ) {
            tags[ i++ ] = tag.m_name;
        }
        ArrayAdapter< String > adapter_tags = new ArrayAdapter<>
                ( getContext(), android.R.layout.simple_dropdown_item_1line, tags );
        mInput1.setAdapter( adapter_tags );

        /*if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mInput1.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );*/

        // show all suggestions w/o entering text:
        mInput1.setOnClickListener( view -> mInput1.showDropDown() );

        mInput1.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                handleNameEdited( s, count - before );
            }
        } );
        mInput1.setOnEditorActionListener( ( v, actionId, event ) -> {
            go();
            return true;
        } );

        handleNameEdited( mInput1.getText(), 1000 ); // 1000 just means positive direction here
    }

    private void handleNameEdited( CharSequence text, int direction ) {
        Entry tag = null;

        mNAV = NameAndValue.parse( text.toString() );

        // empty
        if( ( mNAV.status & NameAndValue.HAS_NAME ) == 0 ) {
            mAction = TagOperation.TO_NONE;
        }
        else {
            tag = Diary.d.get_entry_by_name( mNAV.name );
            if( tag == null ) {
                if( mNAV.value == 1 )
                    mAction = TagOperation.TO_CREATE_BOOLEAN;
                else
                    mAction = TagOperation.TO_CREATE_CUMULATIVE;
            }
            else
            {
                if( ( mNAV.status & NameAndValue.HAS_EQUAL ) == 0 &&
                    direction > 0 ) { // we don't want this to engage on erase
                    String txt = tag.m_name + " = ";

                    mInput1.setText( txt );
                    mInput1.setSelection( txt.length() );
                }
                if( mEntry.get_tags().contains( tag ) ) {
//                    if( tag.get_value( mEntry ) != mNAV.value )
//                        mAction = TagOperation.TO_CHANGE_VALUE;
//                    else
                        mAction = TagOperation.TO_REMOVE;
                }
                else {
                    mAction = TagOperation.TO_ADD;
                }
            }
        }

        mTag = tag;

        if( mAction == TagOperation.TO_INVALID )
            mInput1.setError( "Invalid expression" );

        updateButtons();
    }

    private void updateButtons() {
        mButtonTheme.setVisibility( View.GONE );
        mButtonAction.setVisibility( View.VISIBLE );

        switch( mAction ) {
            case TO_INVALID:
            case TO_NONE:
                mButtonAction.setVisibility( View.GONE );
                break;
            case TO_CREATE_BOOLEAN:
            case TO_CREATE_CUMULATIVE:
                mButtonAction.setText( Lifeograph.getStr( R.string.create_tag ) );
                break;
            case TO_REMOVE:
                mButtonAction.setText( Lifeograph.getStr( R.string.remove_tag ) );
                break;
            case TO_ADD:
                mButtonAction.setText( Lifeograph.getStr( R.string.add_tag ) );
                break;
            case TO_CHANGE_VALUE:
                mButtonAction.setText( Lifeograph.getStr( R.string.change_value ) );
                break;
            default:
                break;
        }
    }

    private void go() {
        Entry tag;

        switch( mAction ) {
            case TO_NONE:
            case TO_INVALID:
                break; // don't even clear
            case TO_REMOVE:
                break;
            case TO_CREATE_BOOLEAN:
            case TO_CREATE_CUMULATIVE:
//                if( mAction == TagOperation.TO_CREATE_CUMULATIVE )
//                    tag = Diary.diary.create_entry( mNAV.name, null,
//                                                  ChartData.MONTHLY | ChartData.CUMULATIVE );
//                else
//                    tag = Diary.diary.create_entry( mNAV.name, false );

//                mEntry.add_tag( tag, mNAV.value );
                break;
//            case TO_ADD:
//                tag = Diary.diary.m_tags.get( mNAV.name );
//                assert tag != null;
//                mEntry.add_tag( tag, mNAV.value );
//                break;
//            case TO_CHANGE_VALUE:
//                tag = Diary.diary.m_tags.get( mNAV.name );
//                mEntry.remove_tag( tag );
//                assert tag != null;
//                mEntry.add_tag( tag, mNAV.value );
//                break;
        }

        mInput1.setText( "" ); // why bother?

        mListener.onTagsChanged();

        dismiss();
    }

    public interface Listener
    {
        void onTagsChanged();
    }

    private enum TagOperation { TO_NONE, TO_INVALID, TO_ADD, TO_REMOVE, TO_CHANGE_VALUE,
                                TO_CREATE_BOOLEAN, TO_CREATE_CUMULATIVE }

    private AutoCompleteTextView mInput1;
    private Button mButtonAction;
    private Button mButtonTheme;
    private Listener mListener;
    private Entry mTag;
    private Entry mEntry;
    private NameAndValue mNAV;
    private TagOperation mAction;
}
