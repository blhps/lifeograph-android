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

package net.sourceforge.lifeograph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import net.sourceforge.lifeograph.DiaryElement.Type;

public class ActivityDiary extends ListActivity
        implements ToDoAction.ToDoObject, DialogInquireText.InquireListener,
        ActionBar.TabListener
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.diary );

        // FILLING WIDGETS
        mDrawerLayout = ( DrawerLayout ) findViewById( R.id.drawer_layout );
        mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        mEditSearch = ( EditText ) findViewById( R.id.editTextSearch );
        mButtonSearchTextClear = ( Button ) findViewById( R.id.buttonClearText );

        mButtonShowTodoNot = ( ToggleImageButton ) findViewById( R.id.show_todo_not );
        mButtonShowTodoOpen = ( ToggleImageButton ) findViewById( R.id.show_todo_open );
        mButtonShowTodoProgressed = ( ToggleImageButton ) findViewById( R.id.show_todo_progressed );
        mButtonShowTodoDone = ( ToggleImageButton ) findViewById( R.id.show_todo_done );
        mButtonShowTodoCanceled = ( ToggleImageButton ) findViewById( R.id.show_todo_canceled );
        mSpinnerShowFavorite = ( Spinner ) findViewById( R.id.spinnerFavorites );

        // UI UPDATES (must come before listeners)
        updateFilterWidgets( Diary.diary.m_filter_active.get_status() );

        // LISTENERS
        mDrawerLayout.setDrawerListener( new DrawerLayout.DrawerListener()
        {
            public void onDrawerSlide( View view, float v ) { }

            public void onDrawerOpened( View view ) {
                ActivityDiary.this.getListView().setEnabled( false );
            }

            public void onDrawerClosed( View view ) {
                ActivityDiary.this.getListView().setEnabled( true );
            }

            public void onDrawerStateChanged( int i ) { }
        } );

        mEditSearch.addTextChangedListener( new TextWatcher() {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                handleSearchTextChanged( s.toString() );
                mButtonSearchTextClear.setVisibility(
                        s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
            }
        } );
        mButtonSearchTextClear.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                mEditSearch.setText( "" );
            }
        } );

        mButtonShowTodoNot.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );
        mButtonShowTodoOpen.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );
        mButtonShowTodoProgressed.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );
        mButtonShowTodoDone.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );
        mButtonShowTodoCanceled.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                handleFilterTodoChanged();
            }
        } );

        mSpinnerShowFavorite.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected( AdapterView< ? > pv, View v, int pos, long id ) {
                // onItemSelected() is fired unnecessarily during initialization, so:
                if( initialized )
                    handleFilterFavoriteChanged( pos );
                else
                    initialized = true;
            }

            public void onNothingSelected( AdapterView< ? > arg0 ) {
                Log.d( Lifeograph.TAG, "Filter Favorites onNothingSelected" );
            }

            private boolean initialized = false;
        } );

        Button buttonFilterReset = ( Button ) findViewById( R.id.buttonFilterReset );
        buttonFilterReset.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                resetFilter();
            }
        } );
        Button buttonFilterSave = ( Button ) findViewById( R.id.buttonFilterSave );
        buttonFilterSave.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v ) {
                saveFilter();
            }
        } );

        // LIST ADAPTER
        m_adapter_entries = new DiaryElemAdapter( this, R.layout.list_item_element, R.id.title, m_elems );
        this.setListAdapter( m_adapter_entries );

        mActionBar = getActionBar();
        if( mActionBar != null ) {
            mActionBar.setDisplayHomeAsUpEnabled( true );
            getActionBar().setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
            getActionBar().addTab( getActionBar().newTab()
                                                 .setText( R.string.all_entries )
                                                 .setTabListener( this ) );
            getActionBar().addTab( getActionBar().newTab()
                                                 .setText( R.string.chapters )
                                                 .setTabListener( this ) );
            getActionBar().addTab( getActionBar().newTab()
                                                 .setText( R.string.tags )
                                                 .setTabListener( this ) );
        }

        //update_entry_list(); --tab listener updates the entry list

        Log.d( Lifeograph.TAG, "onCreate - ActivityDiary" );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if( mFlagLogoutOnPause && mFlagDiaryIsOpen )
            finishEditing( true );
        Log.d( Lifeograph.TAG, "onPause - ActivityDiary" );
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFlagLogoutOnPause = true;
        if( flag_force_update_on_resume )
            update_entry_list();
        flag_force_update_on_resume = false;
        Log.d( Lifeograph.TAG, "onResume - ActivityDiary" );
    }

    @Override
    public void onBackPressed() {
        if( mParentElem == Diary.diary ) {
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
            case TAG:
            case UNTAGGED:
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

        MenuItem item = menu.findItem( R.id.add_elem );
        AddElemAction addElemAction = ( AddElemAction ) item.getActionProvider();
        addElemAction.mParent = this;

        item = menu.findItem( R.id.change_todo_status );
        ToDoAction ToDoAction = ( ToDoAction ) item.getActionProvider();
        ToDoAction.mObject = this;

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        boolean flagPseudoELement = ( mParentElem == Diary.diary.m_orphans );
        DiaryElement.Type type = mParentElem.get_type();

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
        item.setVisible( type != Type.DIARY && !flagPseudoELement );

        item = menu.findItem( R.id.rename );
        item.setVisible( type != Type.DIARY && !flagPseudoELement );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() )
        {
            case android.R.id.home:
                if( mParentElem == Diary.diary ) {
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
                if( mDrawerLayout.isDrawerOpen( Gravity.RIGHT ) )
                    mDrawerLayout.closeDrawer( Gravity.RIGHT );
                else
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
                                logout( false );

                            }
                        }, null );
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

    private void finishEditing( boolean opt_save ) {
        Log.d( Lifeograph.TAG, "ActivityDiary.finishEditing()" );
        // SAVING
        // sync_entry();

        // Diary.diary.m_last_elem = get_cur_elem()->get_id();

        if( opt_save && !Diary.diary.is_read_only() ) {
            if( Diary.diary.write() == Result.SUCCESS ) {
                Lifeograph.showToast( this, "Diary saved successfully" );
                // TODO: try to save backup
            }
            else
                Lifeograph.showToast( this, "Cannot write back changes" );
        }
        else
            Log.d( Lifeograph.TAG, "Logged out without saving" );

        mFlagDiaryIsOpen = false;
    }

    public void logout( boolean opt_save ) {
        finishEditing( opt_save );
        ActivityDiary.this.finish();
    }

    void showEntry( Entry entry ) {
        if( entry != null ) {
            Intent i = new Intent( this, ActivityEntry.class );
            i.putExtra( "entry", entry.get_date_t() );
            mFlagLogoutOnPause = false; // we are just showing an entry from the diary
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

    void handleSearchTextChanged( String text ) {
        Diary.diary.set_search_text( text.toLowerCase() );
        update_entry_list();
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

    void resetFilter() {
        updateFilterWidgets( Diary.diary.m_filter_default.get_status() );
        Diary.diary.m_filter_active.set_status_outstanding();
        update_entry_list();
    }

    void saveFilter() {
        Lifeograph.showToast( this, R.string.filter_saved );
        Diary.diary.m_filter_default.set( Diary.diary.m_filter_active );
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

        Log.w( Lifeograph.TAG, "cannot set todo status" );
    }

    void update_entry_list() {
        m_adapter_entries.clear();
        m_elems.clear();

        switch( mParentElem.get_type() ) {
            case DIARY:
                mActionBar.setIcon( R.drawable.ic_diary );
                setTitle( Diary.diary.get_name() );
                mActionBar.setSubtitle( "Diary with " + Diary.diary.m_entries.size() + " Entries" );

                // ALL ENTRIES
                if( mCurTabIndex == 0 ) {
                    for( Entry e : Diary.diary.m_entries.values() ) {
                        if( !e.get_filtered_out() )
                            m_elems.add( e );
                    }

                    Collections.sort( m_elems, compare_elems );
                }

                // CHAPTERS
                else if( mCurTabIndex == 1 ) {
                    // FREE CHAPTERS
                    if( !Diary.diary.m_groups.mMap.isEmpty() ) {
                        m_elems.add( new HeaderElem( R.string.free_chapters ) );
                        for( Chapter c : Diary.diary.m_groups.mMap.descendingMap().values() ) {
                            m_elems.add( c );
                        }
                    }
                    // NUMBERED CHAPTERS
                    if( !Diary.diary.m_topics.mMap.isEmpty() ) {
                        m_elems.add( new HeaderElem( R.string.numbered_chapters ) );
                        for( Chapter c : Diary.diary.m_topics.mMap.descendingMap().values() ) {
                            m_elems.add( c );
                        }
                    }
                    // DATED CHAPTERS
                    for( Chapter.Category cc : Diary.diary.m_chapter_categories.values() ) {
                        m_elems.add( new Tag.Category( null, cc.get_name() ) );

                        if( cc == Diary.diary.m_ptr2chapter_ctg_cur ) {
                            for( Chapter c : cc.mMap.values() ) {
                                m_elems.add( c );
                            }

                            if( Diary.diary.m_orphans.get_size() > 0 )
                                m_elems.add( Diary.diary.m_orphans );
                        }
                    }
                }

                // TAGS
                else if( mCurTabIndex == 2 ) {
                    // ROOT TAGS
                    for( Tag t : Diary.diary.m_tags.values() ) {
                        if( t.get_category() == null )
                            m_elems.add( t );
                    }
                    // CATEGORIES
                    for( Tag.Category c : Diary.diary.m_tag_categories.values() ) {
                        m_elems.add( c );
                        if( c.get_expanded() ) {
                            for( Tag t : c.mTags )
                                m_elems.add( t );
                        }
                    }
                    // UNTAGGED META TAG
                    if( Diary.diary.m_untagged.get_size() > 0 ) {
                        m_elems.add( new HeaderElem( R.string._empty_ ) );
                        m_elems.add( Diary.diary.m_untagged );
                    }
                }
                break;
            case TAG:
            case UNTAGGED:
                mActionBar.setIcon( mParentElem.get_icon() );
                setTitle( mParentElem.get_list_str() );
                mActionBar.setSubtitle( mParentElem.get_info_str() );

                Tag t = ( Tag ) mParentElem;
                for( Entry e : t.mEntries ) {
                    if( !e.get_filtered_out() )
                        m_elems.add( e );
                }

                Collections.sort( m_elems, compare_elems );
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

                Collections.sort( m_elems, compare_elems );
                break;
            default:
                break;
        }

        // force menu update
        invalidateOptionsMenu();
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

    // TAB LISTENER INTERFACE ======================================================================
    public void onTabSelected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction ) {
        mCurTabIndex = tab.getPosition();
        mParentElem = Diary.diary;
        update_entry_list();
    }

    public void onTabUnselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction ) {

    }

    public void onTabReselected( ActionBar.Tab tab, FragmentTransaction fragmentTransaction ) {
        if( mParentElem != Diary.diary )
            onTabSelected( tab, fragmentTransaction );
    }

// TODO WILL BE IMPLEMENTED IN 0.4
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

    // VARIABLES ===================================================================================
    static boolean flag_force_update_on_resume = false;

    private DiaryElement mParentElem = Diary.diary;
    private int mCurTabIndex = 0;

    private java.util.List< DiaryElement > m_elems = new ArrayList< DiaryElement >();
    private DiaryElemAdapter m_adapter_entries = null;

    private LayoutInflater mInflater;
    private ActionBar mActionBar = null;
    private DrawerLayout mDrawerLayout = null;
    private EditText mEditSearch = null;
    private Button mButtonSearchTextClear = null;
    private ToggleImageButton mButtonShowTodoNot = null;
    private ToggleImageButton mButtonShowTodoOpen = null;
    private ToggleImageButton mButtonShowTodoProgressed = null;
    private ToggleImageButton mButtonShowTodoDone = null;
    private ToggleImageButton mButtonShowTodoCanceled = null;
    private Spinner mSpinnerShowFavorite = null;

    private boolean mFlagDiaryIsOpen = true;
    private boolean mFlagLogoutOnPause = false;

    private long mDateLast;

    // COMPARATOR ==================================================================================
    static class CompareListElems implements Comparator< DiaryElement >
    {
        public int compare( DiaryElement elem_l, DiaryElement elem_r ) {
            // NOTE: this function assumes only similar elements are listed at a time

            // SORT BY NAME
            if( elem_l.get_date_t() == Date.NOT_APPLICABLE ) {
                return 0;
            }
            // SORT BY DATE
            else {
                int direction =
                        ( elem_l.get_date().is_ordinal() && elem_r.get_date().is_ordinal() ) ?
                        -1 : 1;

                if( elem_l.get_date_t() > elem_r.get_date_t() )
                    return -direction;
                else if( elem_l.get_date_t() < elem_r.get_date_t() )
                    return direction;
                else
                    return 0;
            }
        }
    }

    static final CompareListElems compare_elems = new CompareListElems();

    // HEADER PSEUDO ELEMENT CLASS =================================================================
    class HeaderElem extends DiaryElement {

        public HeaderElem( int nameRsc ) {
            super( null, Lifeograph.getStr( nameRsc ), ES_VOID );
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
        public Type get_type() {
            return Type.HEADER;
        }

        @Override
        public int get_size() {
            return 0;
        }
    }

    // ADAPTER CLASS ===============================================================================
    private class DiaryElemAdapter extends ArrayAdapter< DiaryElement >
    {
        public DiaryElemAdapter( Context context, int resource, int textViewResourceId,
                                 java.util.List< DiaryElement > objects ) {
            super( context, resource, textViewResourceId, objects );
        }

        private ViewHolder setHolder( DiaryElement elem, ViewGroup par ) {
            View view;
            ViewHolder holder;

            switch( elem.get_type() ) {
                case TAG_CTG:
                    view = mInflater.inflate( R.layout.list_section_tag_ctg, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_TAG_CTG );
                    break;
                case HEADER:
                    view = mInflater.inflate( R.layout.list_section_simple, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.HEADER_SIMPLE );
                    break;
                default:
                    view = mInflater.inflate( R.layout.list_item_element, par, false );
                    holder = new ViewHolder( view, DiaryElement.LayoutType.ELEMENT );
                    break;
            }

            view.setTag( holder );
            return holder;
        }

        public void handleCollapse( DiaryElement elem ) {
            Log.d( Lifeograph.TAG, "handle collapse " + elem.get_name() );
            switch( elem.get_type().layout_type ) {
                case HEADER_TAG_CTG:
                    Tag.Category tc = (Tag.Category ) elem;
                    tc.set_expanded( !tc.get_expanded() );
                    break;
                case HEADER_CHAPTER_CTG:
                    break;
            }

            update_entry_list();
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            ViewHolder holder;
            TextView title;
            final DiaryElement elem = getItem( position );

            if( convertView == null ) {
                holder = setHolder( elem, parent );
                convertView = holder.getView();
            }
            else {
                holder = ( ViewHolder ) convertView.getTag();
            }

            if( holder.getLayoutType() != elem.get_type().layout_type ) {
                holder = setHolder( elem, parent );
                convertView = holder.getView();
            }

            title = holder.getName();
            title.setText( elem.get_list_str() );

            switch( holder.getLayoutType() ) {
                case HEADER_SIMPLE:
                    break;
                case HEADER_TAG_CTG:
                    Tag.Category tc = ( Tag.Category ) elem;
                    ImageButton iconCollapse = holder.getIconCollapse();
                    iconCollapse.setImageResource(
                            tc.get_expanded() ? R.drawable.ic_expanded : R.drawable.ic_collapsed );
                    iconCollapse.setOnClickListener( new View.OnClickListener() {
                        public void onClick( View v ) {
                            handleCollapse( elem );
                        }
                    } );
                    break;
                case ELEMENT:
                    TextView detail = holder.getDetail();
                    detail.setText( elem.getListStrSecondary() );

                    ImageView icon = holder.getIcon();
                    icon.setImageResource( elem.get_icon() );

                    ImageView icon2 = holder.getIcon2();
                    icon2.setImageResource( R.drawable.ic_favorite );
                    icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );
                    break;
                default:
                    break;
            }

            return convertView;
        }

        private class ViewHolder {
            private View mRow;
            private TextView mTitle = null;
            private TextView mDetail = null;
            private ImageView mIcon = null;
            private ImageView mIcon2 = null;

            private ImageButton mIconCollapse = null;

            private DiaryElement.LayoutType mLayoutType;

            public ViewHolder( View row, DiaryElement.LayoutType layoutType ) {
                mRow = row;
                mLayoutType = layoutType;
            }

            public DiaryElement.LayoutType getLayoutType() {
                return mLayoutType;
            }

            public View getView() {
                return mRow;
            }

            public TextView getName() {
                if( null == mTitle ) {
                    mTitle = ( TextView ) mRow.findViewById( R.id.title );
                }
                return mTitle;
            }

            public TextView getDetail() {
                if( null == mDetail ) {
                    mDetail = ( TextView ) mRow.findViewById( R.id.detail );
                }
                return mDetail;
            }

            public ImageView getIcon() {
                if( null == mIcon ) {
                    mIcon = ( ImageView ) mRow.findViewById( R.id.icon );
                }
                return mIcon;
            }

            public ImageView getIcon2() {
                if( null == mIcon2 ) {
                    mIcon2 = ( ImageView ) mRow.findViewById( R.id.icon2 );
                }
                return mIcon2;
            }

            public ImageButton getIconCollapse() {
                if( null == mIconCollapse ) {
                    mIconCollapse = ( ImageButton ) mRow.findViewById( R.id.icon_collapse );
                }
                return mIconCollapse;
            }
        }
    }
}
