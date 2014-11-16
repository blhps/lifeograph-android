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

package net.sourceforge.lifeograph;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.TextView;

public class DialogCalendar extends Dialog
{
    public DialogCalendar( Listener listener,
                           boolean allowCreation ) {
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

        Date date_today = new Date( Date.get_today( 0 ) );

        GridView gridCalendar = ( GridView ) this.findViewById( R.id.gridViewCalendar );
        mAdapter = new GridCalAdapter( Lifeograph.sContext, date_today );
        mDatePicker = ( DatePicker ) findViewById( R.id.datePickerCalendar );
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
        mDatePicker.init( date_today.get_year(), date_today.get_month() - 1, date_today.get_day(),
                          new DatePicker.OnDateChangedListener() {
                              public void onDateChanged( DatePicker view, int y, int m, int d ) {
                                  handleDayChanged( new Date( y, m + 1, d ) );
                              }
                          } );

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
                !Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey( date_today.m_date ) );
        mButtonCreateChapter.setVisibility( mAllowChapterCreation ? View.VISIBLE : View.INVISIBLE );
    }

    void createEntry() {
        Entry e = Diary.diary.create_entry( mAdapter.mDateCurrent, "", false );
        dismiss();
        Lifeograph.showElem( e );
    }

    void createChapter() {
        dismiss();
        mListener.createChapter( mAdapter.mDateCurrent.m_date );
    }

    private void handleDayChanged( Date date ) {
        mAdapter.showMonth( date );
        mButtonCreateChapter.setEnabled(
                !Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey( date.m_date ) );
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
            Date d = new Date( mAdapter.mListDays.get( pos ) );
            mDatePicker.updateDate( d.get_year(), d.get_month() - 1, d.get_day() );
        }
    }

    private GridCalAdapter mAdapter = null;
    private DatePicker mDatePicker = null;
    private Button mButtonCreateChapter = null;

    private boolean mAllowEntryCreation;
    private boolean mAllowChapterCreation;

    private Listener mListener;

    public interface Listener
    {
        public void createChapter( long date );
        public Activity getActivity();
    }
}
