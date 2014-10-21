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
        Entry e = Diary.diary.m_entries.get( mListDays.get( pos ) + 1 );
        if( e != null ) {
            dismiss();
            Lifeograph.showElem( e );
        }
        else {
            Date d = new Date( mListDays.get( pos ) );
            mDatePicker.updateDate( d.get_year(), d.get_month() - 1, d.get_day() );
        }
    }

    private class GridCalAdapter extends BaseAdapter {
        final Context mContext;
        int mDaysInMonth;
        Date mDateCurrent;

        // Days in Current Month
        public GridCalAdapter( Context context, Date date ) {
            super();
            this.mContext = context;
            mListDays = new ArrayList< Long >();

            showMonth( date );
        }

        // @Override
        public String getItem( int position ) {
            return mListDays.get( position ).toString();
        }

        // @Override
        public int getCount() {
            return mListDays.size();
        }

        protected void showMonth( Date date ) {
            boolean nothing2show = false;
            if( mDateCurrent != null )
                if( date.get_yearmonth() != mDateCurrent.get_yearmonth() )
                    nothing2show = true;
            mDateCurrent = new Date( date.m_date );

            if( nothing2show )
                return;

            mListDays.clear();
            notifyDataSetChanged();

            // HEADER
            for( int i = 0; i < 7; i++ ) {
                mListDays.add( 0L );
            }

            mDaysInMonth = date.get_days_in_month();
            Date date2 = new Date( date.m_date );
            date2.set_day( 1 );
            final int numSlotBefore = date2.get_weekday();

            Date prevMonth = new Date( date2.m_date );
            prevMonth.backward_month();
            int prevMonthLength = prevMonth.get_days_in_month();
            prevMonth.set_day( prevMonthLength - numSlotBefore );

            Date nextMonth = new Date( date2.m_date );
            nextMonth.forward_month();
            nextMonth.set_day( 0 );

            // Prev Month days
            for( int i = 1; i <= numSlotBefore; i++ ) {
                mListDays.add( prevMonth.m_date + Date.make_day( i ) );
            }

            // Current Month Days
            for( int i = 0; i < mDaysInMonth; i++ ) {
                mListDays.add( date2.m_date + Date.make_day( i ) );
            }

            // Next Month days
            //final int numSlotAfter = 7 - ( ( numSlotBefore + mDaysInMonth ) % 7 );
            // always use 6 rows:
            final int numSlotAfter = 42 - ( numSlotBefore + mDaysInMonth );
            for( int i = 1; i <= numSlotAfter; i++ ) {
                mListDays.add( nextMonth.m_date + Date.make_day( i ) );
            }
        }

        public long getItemId( int position ) {
            return position;
        }

        // @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            View row = convertView;
            if( row == null ) {
                LayoutInflater inflater =
                        ( LayoutInflater ) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                row = inflater.inflate( R.layout.cal_day, parent, false );
            }

            TextView tvDayNo = ( TextView ) row.findViewById( R.id.calendar_day_gridcell );
            //TextView num_events_per_day = ( TextView ) row.findViewById( R.id.num_events_per_day );
            //num_events_per_day.setTextColor( Color.GREEN );

            if( position < 7 ) {
                tvDayNo.setText( Date.WEEKDAYSSHORT[ position+1 ] );
                tvDayNo.setTextAppearance( mContext, R.style.boldText );
                tvDayNo.setTextColor( Color.DKGRAY );
                tvDayNo.setTextScaleX( 0.6f );
            }
            else {
                Date date = new Date( mListDays.get( position ) );
                date.m_date += 1; // fixes order

//  TODO EXTRA WIDGET WILL BE INVESTIGATED IN 0.4
//                if( Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey( date.get_pure() ) )
//                    num_events_per_day.setText( "C" );
//                else
//                    num_events_per_day.setText( "" );

                tvDayNo.setText( String.valueOf( date.get_day() ) );

                boolean within_month = ( date.get_month() == mDateCurrent.get_month() );

                if( Diary.diary.m_entries.containsKey( date.m_date ) ) {
                    tvDayNo.setTextAppearance( mContext, R.style.boldText );
                    tvDayNo.setTextColor( within_month ? Color.BLUE : Color.GRAY );
                }
                else {
                    tvDayNo.setTextAppearance( mContext, R.style.normalText );
                    tvDayNo.setTextColor( within_month ? Color.BLACK : Color.GRAY );
                }
                // holidays:
                // if( date.get_weekday() == 0 )
                // tvDayNo.setBackgroundColor( Color.LTGRAY );
            }
            return row;
        }
    }

    private GridCalAdapter mAdapter = null;
    private DatePicker mDatePicker = null;
    private Button mButtonCreateChapter = null;
    private List< Long > mListDays;

    private boolean mAllowEntryCreation;
    private boolean mAllowChapterCreation;

    private Listener mListener;

    public interface Listener
    {
        public void createChapter( long date );
        public Activity getActivity();
    }
}
