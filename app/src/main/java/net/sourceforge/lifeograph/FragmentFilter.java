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


import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


public class FragmentFilter extends Fragment
{
    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState ) {
        Log.d( Lifeograph.TAG, "FragmentFilter.onCreateView()" );

        ViewGroup rootView = ( ViewGroup ) inflater.inflate(
                R.layout.fragment_filter, container, false );

        // FILLING WIDGETS
        mEditSearch = rootView.findViewById( R.id.editTextSearch );
        mButtonSearchTextClear = rootView.findViewById( R.id.buttonClearText );

        mButtonShowTodoNot = rootView.findViewById( R.id.show_todo_not );
        mButtonShowTodoOpen = rootView.findViewById( R.id.show_todo_open );
        mButtonShowTodoProgressed = rootView.findViewById( R.id.show_todo_progressed );
        mButtonShowTodoDone = rootView.findViewById( R.id.show_todo_done );
        mButtonShowTodoCanceled = rootView.findViewById( R.id.show_todo_canceled );
        mSpinnerShowFavorite = rootView.findViewById( R.id.spinnerFavorites );

        if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mEditSearch.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );

        // UI UPDATES (must come before listeners)
        updateFilterWidgets( Diary.diary.m_filter_active.get_status() );
        if( Diary.diary.is_search_active() ) {
            mEditSearch.setText( Diary.diary.get_search_text() );
            mButtonSearchTextClear.setVisibility( View.VISIBLE );
        }

        // LISTENERS
        mEditSearch.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) { }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                if( mTextInitialized ) {
                    handleSearchTextChanged( s.toString() );
                    mButtonSearchTextClear.setVisibility(
                            s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
                }
                else
                    mTextInitialized = true;
            }
        } );
        mButtonSearchTextClear.setOnClickListener( v -> mEditSearch.setText( "" ) );

        mButtonShowTodoNot.setOnClickListener( v -> handleFilterTodoChanged() );
        mButtonShowTodoOpen.setOnClickListener( v -> handleFilterTodoChanged() );
        mButtonShowTodoProgressed.setOnClickListener( v -> handleFilterTodoChanged() );
        mButtonShowTodoDone.setOnClickListener( v -> handleFilterTodoChanged() );
        mButtonShowTodoCanceled.setOnClickListener( v -> handleFilterTodoChanged() );

        mSpinnerShowFavorite.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected( AdapterView< ? > pv, View v, int pos, long id ) {
                // onItemSelected() is fired unnecessarily during initialization, so:
                if( mInitialized )
                    handleFilterFavoriteChanged( pos );
                else
                    mInitialized = true;
            }

            public void onNothingSelected( AdapterView< ? > arg0 ) {
                Log.d( Lifeograph.TAG, "Filter Favorites onNothingSelected" );
            }

            private boolean mInitialized = false;
        } );

        Button buttonFilterReset = rootView.findViewById( R.id.buttonFilterReset );
        buttonFilterReset.setOnClickListener( v -> resetFilter() );
        Button buttonFilterSave = rootView.findViewById( R.id.buttonFilterSave );
        buttonFilterSave.setOnClickListener( v -> saveFilter() );

        return rootView;
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach( context );

        Log.d( Lifeograph.TAG, "FragmentFilter.onAttach() - " + context.toString() );

        if( context instanceof FragmentEntryList.ListOperations )
            mListOperations = ( FragmentEntryList.ListOperations ) context;
        else
            throw new ClassCastException( context.toString() + " must implement ListOperations" );
    }

    @Override
    public void onResume() {
        super.onResume();

        mTextInitialized = false;
        mEditSearch.setText( Diary.diary.get_search_text() );
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.d( Lifeograph.TAG, "FragmentFilter.onDetach() - " + this.toString() );

        mListOperations = null;
    }

    void handleSearchTextChanged( String text ) {
        Diary.diary.set_search_text( text.toLowerCase(), false );
        mListOperations.updateList();
    }

    void updateFilterWidgets( int fs ) {
        mButtonShowTodoNot.setChecked( ( fs & DiaryElement.ES_SHOW_NOT_TODO ) != 0 );
        mButtonShowTodoOpen.setChecked( ( fs & DiaryElement.ES_SHOW_TODO ) != 0 );
        mButtonShowTodoProgressed.setChecked( ( fs & DiaryElement.ES_SHOW_PROGRESSED ) != 0 );
        mButtonShowTodoDone.setChecked( ( fs & DiaryElement.ES_SHOW_DONE ) != 0 );
        mButtonShowTodoCanceled.setChecked( ( fs & DiaryElement.ES_SHOW_CANCELED ) != 0 );

        switch( fs & DiaryElement.ES_FILTER_FAVORED ) {
            case DiaryElement.ES_SHOW_FAVORED:
                mSpinnerShowFavorite.setSelection( 2 );
                break;
            case DiaryElement.ES_SHOW_NOT_FAVORED:
                mSpinnerShowFavorite.setSelection( 1 );
                break;
            case DiaryElement.ES_FILTER_FAVORED:
                mSpinnerShowFavorite.setSelection( 0 );
                break;
        }
    }

    void handleFilterTodoChanged() {
//        Diary.diary.m_filter_active.set_todo(
//                mButtonShowTodoNot.isChecked(),
//                mButtonShowTodoOpen.isChecked(),
//                mButtonShowTodoProgressed.isChecked(),
//                mButtonShowTodoDone.isChecked(),
//                mButtonShowTodoCanceled.isChecked() );
//
//        mListOperations.updateList();
    }

    void handleFilterFavoriteChanged( int i ) {
        boolean showFav = true;
        boolean showNotFav = true;

        switch( i ) {
            case 0:
                showFav = true;
                showNotFav = true;
                break;
            case 1:
                showFav = false;
                showNotFav = true;
                break;
            case 2:
                showFav = true;
                showNotFav = false;
                break;
        }

//        Diary.diary.m_filter_active.set_favorites( showFav, showNotFav );

        mListOperations.updateList();
    }

    void resetFilter() {
//        updateFilterWidgets( Diary.diary.m_filter_default.get_status() );
//        Diary.diary.m_filter_active.set_status_outstanding();
        mListOperations.updateList();
    }

    void saveFilter() {
        Lifeograph.showToast( R.string.filter_saved );
//        Diary.diary.m_filter_default.set( Diary.diary.m_filter_active );
    }

    private EditText mEditSearch = null;
    private Button mButtonSearchTextClear = null;
    private ToggleImageButton mButtonShowTodoNot = null;
    private ToggleImageButton mButtonShowTodoOpen = null;
    private ToggleImageButton mButtonShowTodoProgressed = null;
    private ToggleImageButton mButtonShowTodoDone = null;
    private ToggleImageButton mButtonShowTodoCanceled = null;
    private Spinner mSpinnerShowFavorite = null;

    private FragmentEntryList.ListOperations mListOperations;


    protected boolean mTextInitialized = false;
}
