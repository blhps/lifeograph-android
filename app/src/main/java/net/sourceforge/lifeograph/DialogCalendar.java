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


import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.NumberPicker;


class DialogCalendar extends Dialog
{
    DialogCalendar( Listener listener, boolean allowCreation ) {
        super( listener.getActivity(), R.style.FullHeightDialog );
        mListener = listener;
        mAllowEntryCreation = allowCreation;
        mAllowChapterCreation = allowCreation;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.calendar );

        setTitle( R.string.calendar );

        mDate = new Date( Date.get_today( 0 ) );

        GridView gridCalendar = ( GridView ) this.findViewById( R.id.gridViewCalendar );
        mAdapter = new GridCalAdapter( Lifeograph.sContext, mDate );
        mNumberPickerMonth = ( NumberPicker ) findViewById( R.id.numberPickerMonth );
        mNumberPickerYear = ( NumberPicker ) findViewById( R.id.numberPickerYear );
        Button buttonCreateEntry = ( Button ) findViewById( R.id.buttonCreateEntry );
        mButtonCreateChapter = ( Button ) findViewById( R.id.buttonCreateChapter );

        mAdapter.notifyDataSetChanged();
        gridCalendar.setAdapter( mAdapter );
        gridCalendar.setOnItemClickListener( new OnItemClickListener()
        {
            public void onItemClick( AdapterView< ? > parent, View v, int pos, long id ) {
                handleDayClicked( pos );
            }
        } );

        mNumberPickerMonth.setOnValueChangedListener(
                new NumberPicker.OnValueChangeListener() {
                    public void onValueChange( NumberPicker picker, int old, int n ) {
                        mDate.set_month( n );
                        if( mDate.get_day() > mDate.get_days_in_month() )
                            mDate.set_day( mDate.get_days_in_month() );
                        handleDayChanged();
                    }
                } );
        mNumberPickerYear.setOnValueChangedListener(
                new NumberPicker.OnValueChangeListener() {
                    public void onValueChange( NumberPicker picker, int old, int n ) {
                        mDate.set_year( n );
                        mDate.set_day( mDate.get_days_in_month() );
                        handleDayChanged();
                    }
                } );
        mNumberPickerMonth.setMinValue( 1 );
        mNumberPickerMonth.setMaxValue( 12 );
        mNumberPickerYear.setMinValue( ( int ) Date.YEAR_MIN );
        mNumberPickerYear.setMaxValue( ( int ) Date.YEAR_MAX );

        mNumberPickerMonth.setValue( mDate.get_month() );
        mNumberPickerYear.setValue( mDate.get_year() );

        buttonCreateEntry.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                createEntry();
            }
        } );
        buttonCreateEntry.setVisibility( mAllowEntryCreation ? View.VISIBLE : View.INVISIBLE );

        mButtonCreateChapter.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                createChapter();
            }
        } );
        mButtonCreateChapter.setEnabled(
                !Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey( mDate.m_date ) );
        mButtonCreateChapter.setVisibility( mAllowChapterCreation ? View.VISIBLE : View.INVISIBLE );

        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN );
    }

    private void createEntry() {
        Entry e = Diary.diary.create_entry( mAdapter.mDateCurrent, "", false );
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
                !Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey( mDate.m_date ) );
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
        Activity getActivity();
    }
}
