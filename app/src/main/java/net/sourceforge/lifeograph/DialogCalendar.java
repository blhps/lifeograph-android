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


import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.NumberPicker;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


public class DialogCalendar extends DialogFragment
{
    public DialogCalendar() {
        mAllowEntryCreation = mAllowChapterCreation = Diary.diary.is_in_edit_mode();
    }

//    DialogCalendar( Listener listener, boolean allowCreation ) {
//        mListener = listener;
//        mAllowEntryCreation = allowCreation;
//        mAllowChapterCreation = allowCreation;
//    }

    @Override
    public View
    onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                  Bundle savedInstanceState ) {
        return inflater.inflate( R.layout.calendar, container );
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );

        Objects.requireNonNull( getDialog() ).setTitle( R.string.calendar );

        mDate = new Date( Date.get_today( 0 ) );

        GridView gridCalendar = view.findViewById( R.id.gridViewCalendar );
        mAdapter = new GridCalAdapter( Lifeograph.getContext(), mDate );
        mNumberPickerMonth = view.findViewById( R.id.numberPickerMonth );
        mNumberPickerYear = view.findViewById( R.id.numberPickerYear );
        Button buttonCreateEntry = view.findViewById( R.id.buttonCreateEntry );
        mButtonCreateChapter = view.findViewById( R.id.buttonCreateChapter );

        mAdapter.notifyDataSetChanged();
        gridCalendar.setAdapter( mAdapter );
        gridCalendar.setOnItemClickListener( ( parent, v, pos, id ) -> handleDayClicked( pos ) );

        mNumberPickerMonth.setOnValueChangedListener(
                ( picker, old, n ) -> {
                    mDate.set_month( n );
                    if( mDate.get_day() > mDate.get_days_in_month() )
                        mDate.set_day( mDate.get_days_in_month() );
                    handleDayChanged();
                } );
        mNumberPickerYear.setOnValueChangedListener(
                ( picker, old, n ) -> {
                    mDate.set_year( n );
                    mDate.set_day( mDate.get_days_in_month() );
                    handleDayChanged();
                } );
        mNumberPickerMonth.setMinValue( 1 );
        mNumberPickerMonth.setMaxValue( 12 );
        mNumberPickerYear.setMinValue( ( int ) Date.YEAR_MIN );
        mNumberPickerYear.setMaxValue( ( int ) Date.YEAR_MAX );

        mNumberPickerMonth.setValue( mDate.get_month() );
        mNumberPickerYear.setValue( mDate.get_year() );

        buttonCreateEntry.setOnClickListener( v -> createEntry() );
        buttonCreateEntry.setVisibility( mAllowEntryCreation ? View.VISIBLE : View.INVISIBLE );

        mButtonCreateChapter.setOnClickListener( v -> createChapter() );
        mButtonCreateChapter.setEnabled(
                mAllowChapterCreation &&
                !Diary.diary.m_p2chapter_ctg_cur.mMap.containsKey( mDate.m_date ) );
        mButtonCreateChapter.setVisibility( mAllowChapterCreation ? View.VISIBLE : View.INVISIBLE );

        Objects.requireNonNull( getDialog() ).getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN );
    }

    private void createEntry() {
        Entry e = Diary.diary.create_entry( mAdapter.mDateCurrent.m_date, "", false );
        dismiss();
        Lifeograph.showElem( e );
    }

    private void createChapter() {
        dismiss();
        mListener.createChapter( mAdapter.mDateCurrent.m_date );
    }

    private void handleDayChanged() {
        mAdapter.showMonth( mDate );
        mButtonCreateChapter.setEnabled(
                !Diary.diary.m_p2chapter_ctg_cur.mMap.containsKey( mDate.m_date ) );
    }

    private void handleDayClicked( int pos ) {
        if( pos < 7 )
            return;
        Entry e = Diary.diary.m_entries.get( mAdapter.mListDays.get( pos ) + 1 );
        if( e != null ) {
            dismiss();
            Lifeograph.showElem( e );
        }
        else {
            mDate.m_date = mAdapter.mListDays.get( pos );
            mNumberPickerMonth.setValue( mDate.get_month() );
            mNumberPickerYear.setValue( mDate.get_year() );
            handleDayChanged();
        }
    }

    private GridCalAdapter mAdapter = null;
    private Date mDate;
    private NumberPicker mNumberPickerMonth = null;
    private NumberPicker mNumberPickerYear = null;
    private Button mButtonCreateChapter = null;

    private boolean mAllowEntryCreation;
    private boolean mAllowChapterCreation;

    private Listener mListener;

    public interface Listener
    {
        void createChapter( long date );
        Activity getRelatedActivity();
    }
}
