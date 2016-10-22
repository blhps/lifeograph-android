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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class DialogEntryTag extends Dialog
{
    public DialogEntryTag( Context context, Tag tag, Entry entry, Listener listener ) {
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
        mButtonAction = ( Button ) findViewById( R.id.entry_tag_action );
        mButtonAction.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) { go(); }
        } );

        mButtonTheme = ( Button ) findViewById( R.id.entry_set_theme );
        mButtonTheme.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                if( mTag.get_has_own_theme() )
                    mEntry.set_theme_tag( mTag );
                mListener.onTagsChanged();
                dismiss();
            }
        } );

        mInput1 = ( AutoCompleteTextView ) findViewById( R.id.entry_tag_edit );
        if( mTag != null ) // add new tag case
            mInput1.setText( mTag.get_name_and_value( mEntry, true, true ) );

        String[] tags = new String[ Diary.diary.m_tags.size() ];
        int i = 0;
        for( String tag : Diary.diary.m_tags.keySet() ) {
            tags[ i++ ] = Tag.escape_name( tag );
        }
        ArrayAdapter< String > adapter_tags = new ArrayAdapter< String >
                ( getContext(), android.R.layout.simple_dropdown_item_1line, tags );
        mInput1.setAdapter( adapter_tags );

        /*if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mInput1.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );*/

        // show all suggestions w/o entering text:
        mInput1.setOnClickListener( new AutoCompleteTextView.OnClickListener()
                                    {
                                        public void onClick( View view ) {
                                            mInput1.showDropDown();
                                        }
                                    }
        );

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
        mInput1.setOnEditorActionListener( new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                go();
                return true;
            }
        } );

        handleNameEdited( mInput1.getText(), 1000 ); // 1000 just means positive direction here
    }

    private void handleNameEdited( CharSequence text, int direction ) {
        Tag tag = null;

        mNAV = NameAndValue.parse( text.toString() );

        // empty
        if( ( mNAV.status & NameAndValue.HAS_NAME ) == 0 ) {
            mAction = TagOperation.TO_NONE;
        }
        else {
            tag = Diary.diary.m_tags.get( mNAV.name );
            if( tag == null ) {
                if( mNAV.value == 1 )
                    mAction = TagOperation.TO_CREATE_BOOLEAN;
                else
                    mAction = TagOperation.TO_CREATE_CUMULATIVE;
            }
            else if( tag.is_boolean() && mNAV.value != 1 ) {
                    tag = null;
                    mAction = TagOperation.TO_INVALID;
            }
            else
            {
                if( ! tag.is_boolean() && ( mNAV.status & NameAndValue.HAS_EQUAL ) == 0 &&
                    direction > 0 ) { // we don't want this to engage on erase
                    String txt = Tag.escape_name( tag.get_name() ) + " = ";

                    mInput1.setText( txt );
                    mInput1.setSelection( txt.length() );
                }
                if( mEntry.get_tags().contains( tag ) ) {
                    if( tag.get_value( mEntry ) != mNAV.value )
                        mAction = TagOperation.TO_CHANGE_VALUE;
                    else
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
                mButtonAction.setText( "Create Tag" );
                break;
            case TO_REMOVE:
                mButtonAction.setText( "Remove Tag" );

                if( mTag.get_has_own_theme() && mEntry.get_theme_tag() != mTag )
                    mButtonTheme.setVisibility( View.VISIBLE );
                break;
            case TO_ADD:
                mButtonAction.setText( "Add Tag" );
                break;
            case TO_CHANGE_VALUE:
                mButtonAction.setText( "Change Value" );
                break;
            default:
                break;
        }
    }

    private void go() {
        Tag tag;

        switch( mAction ) {
            case TO_NONE:
            case TO_INVALID:
                break; // don't even clear
            case TO_REMOVE:
                tag = Diary.diary.m_tags.get( mNAV.name );
                mEntry.remove_tag( tag );
                break;
            case TO_CREATE_BOOLEAN:
            case TO_CREATE_CUMULATIVE:
                if( mAction == TagOperation.TO_CREATE_CUMULATIVE )
                    tag = Diary.diary.create_tag( mNAV.name, null,
                                                  ChartPoints.MONTHLY | ChartPoints.CUMULATIVE );
                else
                    tag = Diary.diary.create_tag( mNAV.name, null );

                mEntry.add_tag( tag, mNAV.value );
                break;
            case TO_ADD:
                tag = Diary.diary.m_tags.get( mNAV.name );
                mEntry.add_tag( tag, mNAV.value );
                break;
            case TO_CHANGE_VALUE:
                tag = Diary.diary.m_tags.get( mNAV.name );
                mEntry.remove_tag( tag );
                mEntry.add_tag( tag, mNAV.value );
                break;
        }

        mInput1.setText( "" ); // why bother?

        mListener.onTagsChanged();

        dismiss();
    }

    public interface Listener
    {
        void onTagsChanged();
    }

    public enum TagOperation { TO_NONE, TO_INVALID, TO_ADD, TO_REMOVE, TO_CHANGE_VALUE,
                               TO_CREATE_BOOLEAN, TO_CREATE_CUMULATIVE }

    private AutoCompleteTextView mInput1;
    private Button mButtonAction;
    private Button mButtonTheme;
    private Listener mListener;
    private Tag mTag;
    private Entry mEntry;
    private NameAndValue mNAV;
    private TagOperation mAction;
}
