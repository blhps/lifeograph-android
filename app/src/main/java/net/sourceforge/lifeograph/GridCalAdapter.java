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


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;


class GridCalAdapter extends BaseAdapter
{
    final Context mContext;
    int mDaysInMonth;
    Date mDateCurrent;
    public List< Long > mListDays;

    // Days in Current Month
    public GridCalAdapter( Context context, Date date ) {
        super();
        this.mContext = context;
        mListDays = new ArrayList< Long >();

        showMonth( date );
    }
    public GridCalAdapter( Context context ) {
        super();
        this.mContext = context;
        mListDays = new ArrayList< Long >();
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
        mDateCurrent = new Date( date.m_date );

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
            tvDayNo.setTextColor( mContext.getResources().getColor( R.color.t_mid ) );
            tvDayNo.setTextScaleX( 0.65f );
        }
        else {
            Date date = new Date( mListDays.get( position ) + 1 );

            tvDayNo.setText( String.valueOf( date.get_day() ) );

            boolean flagWithinMonth = ( date.get_month() == mDateCurrent.get_month() );
            boolean flagWeekDay = ( date.get_weekday() > 0 );

            if( Diary.diary.m_entries.containsKey( date.m_date ) ) {
                tvDayNo.setTextAppearance( mContext, R.style.boldText );

                tvDayNo.setTextColor( flagWithinMonth ?
                            mContext.getResources().getColor( R.color.t_dark ) : Color.DKGRAY );
            }
            else {
                tvDayNo.setTextAppearance( mContext, R.style.normalText );
                if( flagWithinMonth && flagWeekDay ) // weekdays within month
                    tvDayNo.setTextColor( mContext.getResources().getColor( R.color.t_mid ) );
                else if( flagWithinMonth ) // weekends within month
                    tvDayNo.setTextColor( mContext.getResources().getColor( R.color.t_light ) );
                else
                    tvDayNo.setTextColor( Color.GRAY );
            }

            if( date.get_pure() == mDateCurrent.get_pure() )
                tvDayNo.setBackgroundColor(
                        mContext.getResources().getColor( R.color.t_lighter ) );
            else if( Diary.diary.m_ptr2chapter_ctg_cur.getMap().containsKey( date.get_pure() ) )
                tvDayNo.setBackgroundColor(
                        mContext.getResources().getColor( R.color.t_lightest ) );
            else
                tvDayNo.setBackgroundColor( Color.WHITE );
        }
        return row;
    }
}
