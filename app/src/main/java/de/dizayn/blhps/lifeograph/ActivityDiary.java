/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import de.dizayn.blhps.lifeograph.DiaryElement.Type;

public class ActivityDiary extends ListActivity
        implements ToDoAction.ToDoObject, DialogInquireText.InquireListener
{
    public static Entry entry_current = null;

    DiaryElement mParentElem = Diary.diary;

    private LayoutInflater mInflater;
    private ActionBar mActionBar = null;
    private DrawerLayout mDrawerLayout = null;
    private ToggleImageButton mButtonShowTodoNot = null;
    private ToggleImageButton mButtonShowTodoOpen = null;
    private ToggleImageButton mButtonShowTodoProgressed = null;
    private ToggleImageButton mButtonShowTodoDone = null;
    private ToggleImageButton mButtonShowTodoCanceled = null;
    private Spinner mSpinnerShowFavorite = null;

    private ElemListAllEntries mElemAllEntries = null;

    private boolean mFlagSaveOnLogOut = true;
    private boolean mFlagLogoutOnPause = false;

    private long mDateLast;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        Lifeograph.activityDiary = this;

        if( Diary.diary == null )
            Diary.diary = new Diary();

        setContentView( R.layout.diary );

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled( true );

        mDrawerLayout = ( DrawerLayout ) findViewById( R.id.drawer_layout );

        mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        // FILTERING
        mButtonShowTodoNot = ( ToggleImageButton ) findViewById( R.id.show_todo_not );
        mButtonShowTodoNot.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mButtonShowTodoOpen = ( ToggleImageButton ) findViewById( R.id.show_todo_open );
        mButtonShowTodoOpen.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mButtonShowTodoProgressed = ( ToggleImageButton ) findViewById( R.id.show_todo_progressed );
        mButtonShowTodoProgressed.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mButtonShowTodoDone = ( ToggleImageButton ) findViewById( R.id.show_todo_done );
        mButtonShowTodoDone.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mButtonShowTodoCanceled = ( ToggleImageButton ) findViewById( R.id.show_todo_canceled );
        mButtonShowTodoCanceled.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mSpinnerShowFavorite = ( Spinner ) findViewById( R.id.spinnerFavorites );
        mSpinnerShowFavorite.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            public void onItemSelected( AdapterView<?> pv, View v, int pos, long id ) {
                handleFilterFavoriteChanged( pos );
            }
            public void onNothingSelected( AdapterView<?> arg0 ) {
                Log.d( Lifeograph.TAG, "Filter Favorites onNothingSelected" );
            }
        } );

        // LIST ADAPTER
        m_adapter_entries = new DiaryElemAdapter( this, R.layout.imagelist, R.id.title, m_elems );
        this.setListAdapter( m_adapter_entries );
        update_entry_list();

        mFlagSaveOnLogOut = true;
        Log.d( Lifeograph.TAG, "RESET MFLAGSAVEONLOGOUT" );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if( mFlagLogoutOnPause )
            handleLogout(); // TODO: save backup if not successful
        Log.d( Lifeograph.TAG, "onPause - ActivityDiary" );
    }

    /* onDestroy is called after next Activity gets started so this is not useful
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d( Lifeograph.TAG, "onDestroy - ActivityDiary" );
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        mFlagLogoutOnPause = false;
        //update_entry_list();
        Log.i( "L", "onResume - ActivityDiary" );
    }

    @Override
    public void onBackPressed() {
        if( mParentElem == Diary.diary ) {
            mFlagLogoutOnPause = true;
            super.onBackPressed();
        }
        else {
            mParentElem = Diary.diary;
            update_entry_list();
        }
    }

    @Override
    protected void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );
        switch( m_elems.get( pos ).get_type() ) {
            case ENTRY:
                showEntry( ( Entry ) m_elems.get( pos ) );
                break;
            case ALL_ENTRIES:
                mParentElem = mElemAllEntries;
                update_entry_list();
                break;
            case TAG:
            case CHAPTER:
            case TOPIC:
            case GROUP:
                mParentElem = m_elems.get( pos );
                update_entry_list();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_diary, menu );

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        DiaryElement.Type type = ( mParentElem == Diary.diary.m_orphans ?
                 Type.ALL_ENTRIES : mParentElem.get_type() );
        // orphans chapter is treated as if all_entries pseudo element
        // this is OK as they both are pseudo elements

        MenuItem item = menu.findItem( R.id.add_elem );
        item.setVisible( type == Type.DIARY );

        item = menu.findItem( R.id.change_todo_status );
        item.setVisible( type == Type.TOPIC || type == Type.GROUP || type == Type.CHAPTER );

        item = menu.findItem( R.id.calendar );
        item.setVisible( type == Type.DIARY || type == Type.CHAPTER );

//  TODO WILL BE IMPLEMENTED IN 0.3
//        item = menu.findItem( R.id.change_sort_type );
//        item.setVisible( mParentElem != null );

        item = menu.findItem( R.id.add_entry );
        item.setVisible( type == Type.TOPIC || type == Type.GROUP );

        item = menu.findItem( R.id.dismiss );
        item.setVisible( type != Type.DIARY && type != Type.ALL_ENTRIES );

        item = menu.findItem( R.id.rename );
        item.setVisible( type != Type.DIARY && type != Type.ALL_ENTRIES );

        ToDoAction.mObject = this;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() )
        {
            case android.R.id.home:
                if( mParentElem == Diary.diary ) {
                    mFlagLogoutOnPause = true;
                    //NavUtils.navigateUpFromSameTask( this );
                    finish();
                }
                else {
                    mParentElem = Diary.diary;
                    update_entry_list();
                }
                return true;
            case R.id.calendar:
                showCalendar();
                return true;
            case R.id.filter:
                mDrawerLayout.openDrawer( Gravity.RIGHT );
                return true;
            case R.id.add_entry:
                showEntry( Diary.diary.create_entry( ( ( Chapter ) mParentElem ).get_free_order(),
                        "", false ) );
                return true;
            case R.id.rename:
                switch( mParentElem.get_type() ) {
                    case TAG:
                        rename_tag();
                        break;
                    case CHAPTER:
                    case TOPIC:
                    case GROUP:
                        rename_chapter();
                        break;
                    default:
                        break;
                }
                return true;
            case R.id.dismiss:
                switch( mParentElem.get_type() ) {
                    case TAG:
                        dismiss_tag();
                        break;
                    case CHAPTER:
                    case TOPIC:
                    case GROUP:
                        dismiss_chapter();
                        break;
                    default:
                        break;
                }
                return true;
            case R.id.logout_wo_save:
                Lifeograph.showConfirmationPromt( this,
                        R.string.logoutwosaving_confirm,
                        R.string.logoutwosaving,
                        new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int id ) {
                                // unlike desktop version Android version
                                // does not back up changes
                                mFlagSaveOnLogOut = false;
                                ActivityDiary.this.finish();
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int id ) {
                                mFlagSaveOnLogOut = true;
                            }
                        } );
                return true;
//  TODO WILL BE IMPLEMENTED IN 0.3
//            case R.id.import_sms:
//                import_messages();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // InquireListener methods
    public void onInquireAction( int id, String text ) {
        switch( id ) {
            case R.string.create_chapter:
                mParentElem = Diary.diary.m_ptr2chapter_ctg_cur.create_chapter( text, mDateLast );
                Diary.diary.update_entries_in_chapters();
                update_entry_list();
                break;
            case R.string.create_topic:
                mParentElem = Diary.diary.m_topics.create_chapter_ordinal( text );
                Diary.diary.update_entries_in_chapters();
                update_entry_list();
                break;
            case R.string.create_group:
                mParentElem = Diary.diary.m_groups.create_chapter_ordinal( text );
                Diary.diary.update_entries_in_chapters();
                update_entry_list();
                break;
            case R.string.rename_tag:
                Diary.diary.rename_tag( ( Tag ) mParentElem, text );
                setTitle( mParentElem.m_name );
                break;
            case R.string.rename_chapter:
                mParentElem.m_name = text;
                setTitle( mParentElem.get_list_str() );
                break;
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
            case R.string.rename_tag:
                return !Diary.diary.m_tags.containsKey( s );
            case R.string.rename_chapter:
                return( mParentElem.m_name.compareTo( s ) != 0 );
            default:
                return true;
        }
    }

    private boolean handleLogout() {
        // SAVING
        // sync_entry();

        // Diary.diary.m_last_elem = get_cur_elem()->get_id();

        if( mFlagSaveOnLogOut ) {
            if( Diary.diary.write() != Result.SUCCESS ) {
                Toast.makeText( Lifeograph.activityLogin, "Cannot write back changes",
                        Toast.LENGTH_LONG ).show();
                return false;
            }
            else {
                // ActivityDiary.this.finish();
                Toast.makeText( Lifeograph.activityLogin, "Diary saved successfully",
                        Toast.LENGTH_LONG ).show();
                return true;
            }
        }
        else {
            Log.d( Lifeograph.TAG, "Logged out without saving" );
            return true;
        }
    }

    void showEntry( Entry entry ) {
        if( entry != null ) {
            entry_current = entry;
            Intent i = new Intent( this, ActivityEntry.class );
            startActivity( i );
        }
    }

    void goToToday() {
        Entry entry = Diary.diary.get_entry_today();

        if( entry == null ) // add new entry if no entry exists on selected date
        {
            entry = Diary.diary.add_today();
            // update_entry_list();
        }

        showEntry( entry );
    }

    private void showCalendar() {
        // Intent i = new Intent( this, ActivityCalendar.class );
        // startActivityForResult( i, ActivityCalendar.REQC_OPEN_ENTRY );
        DialogCalendar dialog = new DialogCalendar( this );
        dialog.show();
    }

    // @Override
    // protected void onActivityResult( int reqCode, int resultCode, Intent data ) {
    // super.onActivityResult( reqCode, resultCode, data );
    // if( resultCode == ActivityCalendar.REQC_OPEN_ENTRY ) {
    // changeView();
    // }
    // else {
    // Toast.makeText( this, "Fail", Toast.LENGTH_LONG ).show();
    // }
    // }

    void handleFilterTodoChanged() {
        Diary.diary.m_filter_active.set_todo(
                mButtonShowTodoNot.isChecked(),
                mButtonShowTodoOpen.isChecked(),
                mButtonShowTodoProgressed.isChecked(),
                mButtonShowTodoDone.isChecked(),
                mButtonShowTodoCanceled.isChecked() );

        update_entry_list();
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

        Diary.diary.m_filter_active.set_favorites( showFav, showNotFav );

        update_entry_list();
    }

    public void set_todo_status( int s ) {
        if( mParentElem != null ) {
            switch( mParentElem.get_type() ) {
                case CHAPTER:
                case TOPIC:
                case GROUP:
                    Chapter chapter = ( Chapter ) mParentElem;
                    chapter.set_todo_status( s );
                    mActionBar.setIcon( mParentElem.get_icon() );
                    return;
                default:
                    break;
            }
        }

        Log.d( Lifeograph.TAG, "Cannot set todo status" );
    }

    void update_entry_list() {
        m_adapter_entries.clear();
        m_elems.clear();

        switch( mParentElem.get_type() ) {
            case DIARY:
                mActionBar.setIcon( R.drawable.ic_diary );
                setTitle( Diary.diary.get_name() );
                mActionBar.setSubtitle( "Diary with " + Diary.diary.m_entries.size() + " Entries" );

                if( mElemAllEntries == null ) {
                    mElemAllEntries = new ElemListAllEntries( Diary.diary );
                }
                m_elems.add( mElemAllEntries );

                for( Chapter c : Diary.diary.m_groups.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Chapter c : Diary.diary.m_topics.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Chapter c : Diary.diary.m_ptr2chapter_ctg_cur.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Tag t : Diary.diary.m_tags.values() ) {
                    m_elems.add( t );
                }
                if( Diary.diary.m_groups.empty() &&
                    Diary.diary.m_topics.empty() &&
                    Diary.diary.m_ptr2chapter_ctg_cur.get_size() == 0 ) {
                    for( Entry e : Diary.diary.m_orphans.mEntries )
                        m_elems.add( e );
                }
                else if( Diary.diary.m_orphans.get_size() > 0 )
                    m_elems.add( Diary.diary.m_orphans );
                break;
            case TAG:
                mActionBar.setIcon( mParentElem.get_icon() );
                setTitle( mParentElem.get_list_str() );
                mActionBar.setSubtitle( mParentElem.get_info_str() );

                Tag t = ( Tag ) mParentElem;
                for( Entry e : t.mEntries ) {
                    if( !e.get_filtered_out() )
                        m_elems.add( e );
                }
                break;
            case CHAPTER:
            case TOPIC:
            case GROUP:
                mActionBar.setIcon( mParentElem.get_icon() );
                setTitle( mParentElem.get_list_str() );
                mActionBar.setSubtitle( mParentElem.get_info_str() );

                Chapter c = ( Chapter ) mParentElem;
                for( Entry e : c.mEntries ) {
                    if( !e.get_filtered_out() )
                        m_elems.add( e );
                }
                break;
            case ALL_ENTRIES:
                for( Entry e : Diary.diary.m_entries.values() ) {
                    if( !e.get_filtered_out() )
                        m_elems.add( e );
                }
                break;
            default:
                break;
        }

        // force menu update
        invalidateOptionsMenu();
        Collections.sort( m_elems, compare_elems );
    }

    void createChapter( long date ) {
        mDateLast = date;

        DialogInquireText dlg = new DialogInquireText( this, R.string.create_chapter,
                Lifeograph.getStr( R.string.new_chapter ), R.string.create, this );
        dlg.show();
    }
    void createTopic() {
        DialogInquireText dlg = new DialogInquireText( this, R.string.create_topic,
                Lifeograph.getStr( R.string.new_chapter ), R.string.create, this );
        dlg.show();
    }
    void createGroup() {
        DialogInquireText dlg = new DialogInquireText( this, R.string.create_group,
                Lifeograph.getStr( R.string.new_chapter ), R.string.create, this );
        dlg.show();
    }

    private void rename_tag() {
        DialogInquireText dlg = new DialogInquireText( this, R.string.rename_tag,
                mParentElem.m_name, R.string.rename, this );
        dlg.show();
    }

    private void rename_chapter() {
        DialogInquireText dlg = new DialogInquireText( this, R.string.rename_chapter,
                mParentElem.m_name, R.string.rename, this );
        dlg.show();
    }

    private void dismiss_chapter() {
        Lifeograph.showConfirmationPromt( this,
                R.string.chapter_dismiss_confirm, R.string.dismiss,
                new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        Diary.diary.dismiss_chapter( ( Chapter ) mParentElem );
                        // go up:
                        mParentElem = Diary.diary;
                        Diary.diary.update_entries_in_chapters();
                        update_entry_list();
                    }
                }, null );
    }

    private void dismiss_tag() {
        Lifeograph.showConfirmationPromt( this,
                R.string.tag_dismiss_confirm, R.string.dismiss,
                new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int id ) {
                        Diary.diary.dismiss_tag( ( Tag ) mParentElem );
                        // go up:
                        mParentElem = Diary.diary;
                        update_entry_list();
                    }
                }, null );
    }

// TODO WILL BE IMPLEMENTED IN 0.3
//    protected void import_messages() {
//        Cursor cursor =
//                getContentResolver().query( Uri.parse( "content://sms/inbox" ), null, null, null,
//                                            null );
//        cursor.moveToFirst();
//
//        do {
//            String body = new String();
//            Calendar cal = Calendar.getInstance();
//
//            for( int idx = 0; idx < cursor.getColumnCount(); idx++ ) {
//                String msgData = cursor.getColumnName( idx );
//
//                if( msgData.compareTo( "body" ) == 0 )
//                    body = cursor.getString( idx );
//                else if( msgData.compareTo( "date" ) == 0 )
//                    cal.setTimeInMillis( cursor.getLong( idx ) );
//            }
//
//            Diary.diary.create_entry( new Date( cal.get( Calendar.YEAR ),
//                                                cal.get( Calendar.MONTH ) + 1,
//                                                cal.get( Calendar.DAY_OF_MONTH ) ), body, false );
//        }
//        while( cursor.moveToNext() );
//
//    }

    // ALL ENTRIES PSEUDO ELEMENT CLASS ============================================================
    class ElemListAllEntries extends DiaryElement {

        public ElemListAllEntries( Diary diary ) {
            super( diary, getString( R.string.all_entries ), ES_VOID );
        }

        @Override
        public String get_info_str() {
            return "";
        }

        @Override
        public int get_icon() {
            return R.drawable.ic_diary;
        }

        @Override
        public Date get_date() {
            return new Date( 0x100000000L );
        }

        @Override
        public Type get_type() {
            return Type.ALL_ENTRIES;
        }

        @Override
        public int get_size() {
            return mSize;
        }

        int mSize = 0;
    }

    private java.util.List< DiaryElement > m_elems = new ArrayList< DiaryElement >();
    private DiaryElemAdapter m_adapter_entries;

    // COMPARATOR ==================================================================================
    static class CompareListElems implements Comparator< DiaryElement >
    {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {

            // SORT BY DATE (ONLY DESCENDINGLY FOR NOW)
            if( elem_l.get_type() == Type.DIARY )
                return -1;

            int direction = ( elem_l.get_date().is_ordinal() && elem_r.get_date().is_ordinal() ) ?
                            -1 : 1;

            if( elem_l.get_date().m_date > elem_r.get_date().m_date )
                return -direction;
            else
            if( elem_l.get_date().m_date < elem_r.get_date().m_date )
                return direction;
            else
                return 0;
        }
    }

    static final CompareListElems compare_elems = new CompareListElems();

    // ADAPTER CLASS ===============================================================================
    private class DiaryElemAdapter extends ArrayAdapter< DiaryElement > {

        public DiaryElemAdapter( Context context, int resource, int textViewResourceId,
                                 java.util.List< DiaryElement > objects ) {
            super( context, resource, textViewResourceId, objects );
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            ViewHolder holder;
            TextView title;
            TextView detail;
            ImageView icon;
            ImageView icon2;
            DiaryElement elem = getItem( position );

            if( convertView == null ) {
                convertView = mInflater.inflate( R.layout.imagelist, null );
                holder = new ViewHolder( convertView );
                convertView.setTag( holder );
            }
            holder = ( ViewHolder ) convertView.getTag();

            title = holder.getName();
            title.setText( elem.get_list_str() );

            detail = holder.getDetail();
            detail.setText( elem.getListStrSecondary() );

            icon = holder.getIcon();
            icon.setImageResource( elem.get_icon() );

            icon2 = holder.getIcon2();
            icon2.setImageResource( R.drawable.ic_favorite );
            icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );

            return convertView;
        }

        private class ViewHolder {
            private View mRow;
            private TextView title = null;
            private TextView detail = null;
            private ImageView icon = null;
            private ImageView icon2 = null;

            public ViewHolder( View row ) {
                mRow = row;
            }

            public TextView getName() {
                if( null == title ) {
                    title = ( TextView ) mRow.findViewById( R.id.title );
                }
                return title;
            }

            public TextView getDetail() {
                if( null == detail ) {
                    detail = ( TextView ) mRow.findViewById( R.id.detail );
                }
                return detail;
            }

            public ImageView getIcon() {
                if( null == icon ) {
                    icon = ( ImageView ) mRow.findViewById( R.id.icon );
                }
                return icon;
            }

            public ImageView getIcon2() {
                if( null == icon2 ) {
                    icon2 = ( ImageView ) mRow.findViewById( R.id.icon2 );
                }
                return icon2;
            }
        }
    }
}
