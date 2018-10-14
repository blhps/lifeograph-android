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


import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class ActivityChapterTag extends AppCompatActivity
        implements ToDoAction.ToDoObject,
        DialogInquireText.InquireListener, DialogCalendar.Listener, FragmentElemList.DiaryManager,
        FragmentElemList.ListOperations, DialogTheme.DialogThemeHost, Spinner.OnItemSelectedListener
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Log.d( Lifeograph.TAG, "onCreate - ActivityChapterTag" );

        setContentView( R.layout.chapter );

        Lifeograph.sContext = this;
        Lifeograph.updateScreenSizes();

        // ELEMENT TO SHOW
        mElement = ( DiaryElementChart ) Diary.diary.get_element(
                getIntent().getIntExtra( "elem", 0 ) );
        if( mElement == null ) {
            int type = getIntent().getIntExtra( "type", 0 );
            if( type == DiaryElement.Type.UNTAGGED.i )
                mElement = Diary.diary.get_untagged();
            else if( type == DiaryElement.Type.CHAPTER.i )
                mElement = Diary.diary.m_orphans;
            else
                Log.e( Lifeograph.TAG, "Element not found in the diary" );
        }

        // FILLING WIDGETS
        mDrawerLayout = ( DrawerLayout ) findViewById( R.id.drawer_layout );
        //mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        LinearLayout layoutTagProperties = ( LinearLayout ) findViewById( R.id.tag_properties );
        Spinner spinnerTagType = ( Spinner ) findViewById( R.id.tag_type );
        mAtvTagUnit = ( AutoCompleteTextView ) findViewById( R.id.tag_unit );

        mViewChart = ( ViewChart ) findViewById( R.id.chart_view_tag );

        // UI UPDATES (must come before listeners)
        if( mElement != null )
            switch( mElement.get_type() ) {
                case TOPIC:
                case GROUP:
                    mViewChart.setVisibility( View.GONE );
                    layoutTagProperties.setVisibility( View.GONE );
                    break;
                case CHAPTER:
                case UNTAGGED:
                    mViewChart.setVisibility( View.VISIBLE );
                    mViewChart.set_points( mElement.create_chart_data(), 1f );
                    layoutTagProperties.setVisibility( View.GONE );
                    break;
                case TAG:
                    mViewChart.setVisibility( View.VISIBLE );
                    mViewChart.set_points( mElement.create_chart_data(), 1f );
                    layoutTagProperties.setVisibility( View.VISIBLE );
                    switch( mElement.get_chart_type() & ChartPoints.VALUE_TYPE_MASK ) {
                        case ChartPoints.BOOLEAN:
                            spinnerTagType.setSelection( 0 );
                            mAtvTagUnit.setVisibility( View.GONE );
                            break;
                        case ChartPoints.CUMULATIVE:
                            spinnerTagType.setSelection( 1 );
                            mAtvTagUnit.setVisibility( View.VISIBLE );
                            mAtvTagUnit.setText( ( ( Tag ) mElement ).get_unit() );
                            break;
                        default:
                            spinnerTagType.setSelection( 2 );
                            mAtvTagUnit.setVisibility( View.VISIBLE );
                            mAtvTagUnit.setText( ( ( Tag ) mElement ).get_unit() );
                            break;
                    }
                    break;
            }

        //updateFilterWidgets( Diary.diary.m_filter_active.get_status() );

        // LISTENERS
        spinnerTagType.setOnItemSelectedListener( this );

        String[] units = getResources().getStringArray( R.array.array_tag_units );
        ArrayAdapter< String > adapter_units = new ArrayAdapter< String >
                ( this, android.R.layout.simple_dropdown_item_1line, units );
        mAtvTagUnit.setAdapter( adapter_units );
        // show all suggestions w/o entering text:
        mAtvTagUnit.setOnClickListener( new AutoCompleteTextView.OnClickListener() {
                                            public void onClick( View view ) {
                                                mAtvTagUnit.showDropDown();
                                            }
                                        }
        );
        mAtvTagUnit.addTextChangedListener( new TextWatcher()
        {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                ( ( Tag ) mElement ).set_unit( s.toString() );
                mViewChart.set_points( mElement.create_chart_data(), 1f );
            }
        } );

        mViewChart.setListener( new ViewChart.Listener()
        {
            public void onTypeChanged( int type ) {
                mElement.set_chart_type( type );
                mViewChart.set_points( mElement.create_chart_data(), 1f );
            }
        } );

        mDrawerLayout.setDrawerListener( new DrawerLayout.DrawerListener()
        {
            public void onDrawerSlide( View view, float v ) { }

            public void onDrawerOpened( View view ) {
                if( mFragmentList != null )
                    mFragmentList.getListView().setEnabled( false );
            }

            public void onDrawerClosed( View view ) {
                if( mFragmentList != null )
                    mFragmentList.getListView().setEnabled( true );
            }

            public void onDrawerStateChanged( int i ) { }
        } );

        // ACTIONBAR
        mActionBar = getSupportActionBar();
        if( mActionBar != null ) {
            mActionBar.setDisplayHomeAsUpEnabled( true );
            // mActionBar.setIcon( mElement.get_icon() );
            setTitle( mElement.get_title_str() );
            mActionBar.setSubtitle( mElement.get_info_str() );
        }
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "onPause - ActivityChapterTag" );
    }*/

    @Override
    protected void onStop() {
        super.onStop();

        Log.d( Lifeograph.TAG, "ActivityChapterTag.onStop()" );

        Lifeograph.handleDiaryEditingActivityDestroyed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d( Lifeograph.TAG, "onResume - ActivityChapterTag" );

        Lifeograph.sContext = this;

        updateList();
    }

    @Override
    public void onBackPressed() {
        Lifeograph.sFlagStartingDiaryEditingActivity = true;
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_chapter_tag, menu );

        MenuItem item = menu.findItem( R.id.change_todo_status );
        ToDoAction ToDoAction = ( ToDoAction ) MenuItemCompat.getActionProvider( item );
        ToDoAction.mObject = this;

        mMenu = menu;
        updateIcon();

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        boolean flagPseudoElement = ( mElement == Diary.diary.m_orphans );
        boolean flagWritable = !Diary.diary.is_read_only();
        DiaryElement.Type type = mElement.get_type();

        MenuItem item = menu.findItem( R.id.change_todo_status );
        item.setVisible( type != DiaryElement.Type.UNTAGGED &&
                         !flagPseudoElement &&
                         flagWritable );

//  TODO WILL BE IMPLEMENTED IN 0.7+
//        item = menu.findItem( R.id.change_sort_type );
//        item.setVisible( mParentElem != null );

        item = menu.findItem( R.id.add_entry );
        item.setVisible( ( type == DiaryElement.Type.TOPIC || type == DiaryElement.Type.GROUP ) &&
                         flagWritable );

        item = menu.findItem( R.id.dismiss );
        item.setVisible( !flagPseudoElement && flagWritable );

        item = menu.findItem( R.id.rename );
        item.setVisible( !flagPseudoElement && flagWritable );

        item = menu.findItem( R.id.edit_date );
        item.setVisible( type == DiaryElement.Type.CHAPTER && !flagPseudoElement && flagWritable );

        item = menu.findItem( R.id.edit_theme );
        item.setVisible( type == DiaryElement.Type.TAG && !flagPseudoElement && flagWritable );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
                Lifeograph.sFlagStartingDiaryEditingActivity = true;
                finish();
                return true;
            case R.id.filter:
                if( mDrawerLayout.isDrawerOpen( Gravity.RIGHT ) )
                    mDrawerLayout.closeDrawer( Gravity.RIGHT );
                else
                    mDrawerLayout.openDrawer( Gravity.RIGHT );
                return true;
            case R.id.add_entry: {
                Entry entry = Diary.diary.create_entry(
                        ( ( Chapter ) mElement ).get_free_order(), "", false );
                Lifeograph.showElem( entry );
                return true;
            }
            case R.id.rename:
                switch( mElement.get_type() ) {
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
            case R.id.edit_theme:
                showThemeDialog();
                return true;
            case R.id.edit_date:
                new DialogInquireText( this,
                                       R.string.edit_date,
                                       mElement.get_date().format_string(),
                                       R.string.apply,
                                       this ).show();
                return true;
            case R.id.dismiss:
                switch( mElement.get_type() ) {
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
        }
        return super.onOptionsItemSelected( item );
    }

    private void rename_tag() {
        DialogInquireText dlg = new DialogInquireText( this,
                                                       R.string.rename_tag,
                                                       mElement.m_name,
                                                       R.string.rename,
                                                       this );
        dlg.show();
    }

    private void rename_chapter() {
        DialogInquireText dlg = new DialogInquireText( this,
                                                       R.string.rename_chapter,
                                                       mElement.m_name,
                                                       R.string.rename,
                                                       this );
        dlg.show();
    }


    private void showThemeDialog() {
        Dialog dialog = new DialogTheme( this, ( Tag ) mElement, this );
        dialog.show();
    }

    private void dismiss_chapter() {
        Lifeograph.showConfirmationPrompt( R.string.chapter_dismiss_confirm,
                                           R.string.dismiss,
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick( DialogInterface dialog,
                                                                    int id ) {
                                                   Diary.diary.dismiss_chapter(
                                                           ( Chapter ) mElement );
                                                   // go up:
                                                   finish();
                                               }
                                           },
                                           null );
    }

    private void dismiss_tag() {
        Lifeograph.showConfirmationPrompt( R.string.tag_dismiss_confirm, R.string.dismiss,
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick( DialogInterface dialog,
                                                                    int id ) {
                                                   Diary.diary.dismiss_tag(
                                                           ( Tag ) mElement );
                                                   // go up:
                                                   finish();
                                               }
                                           }, null );
    }

    // InquireListener INTERFACE METHODS
    public void onInquireAction( int id, String text ) {
        switch( id ) {
            case R.string.rename_tag:
                Diary.diary.rename_tag( ( Tag ) mElement, text );
                setTitle( mElement.m_name );
                break;
            case R.string.rename_chapter:
                mElement.m_name = text;
                setTitle( mElement.get_list_str() );
                break;
            case R.string.edit_date:
                Date date = new Date( text );
                if( date.m_date != Date.NOT_SET ) {
                    if( date.is_ordinal() )
                        break;

                    date.reset_order_0();
                    Diary.diary.m_ptr2chapter_ctg_cur.set_chapter_date( ( Chapter ) mElement,
                                                                        date.m_date );
                    Diary.diary.update_entries_in_chapters();
                    setTitle( mElement.get_title_str() );
                    mActionBar.setSubtitle( mElement.get_info_str() );
                    updateList();
                }
                break;
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
            case R.string.rename_tag:
                return !Diary.diary.m_tags.containsKey( s );
            case R.string.rename_chapter:
                return( mElement.m_name.compareTo( s ) != 0 );
            case R.string.edit_date:
                Date date = new Date( s );
                if( date.m_date == Date.NOT_SET )
                    return false;
                else if( date.is_ordinal() )
                    return false;
                return !Diary.diary.m_ptr2chapter_ctg_cur.getMap().containsKey( date.m_date );
            default:
                return true;
        }
    }

    // ToDoObject INTERFACE METHODS
    public void setTodoStatus( int s ) {
        if( mElement != null ) {
            switch( mElement.get_type() ) {
                case CHAPTER:
                case TOPIC:
                case GROUP:
                    Chapter chapter = ( Chapter ) mElement;
                    chapter.set_todo_status( s );
                    updateIcon();
                    return;
                default:
                    break;
            }
        }

        Log.w( Lifeograph.TAG, "cannot set todo status" );
    }

    void updateIcon() {
        if( mMenu != null ) {
            int icon = R.drawable.ic_action_not_todo;

            switch( mElement.get_todo_status() ) {
                case Entry.ES_TODO:
                    icon = R.drawable.ic_action_todo_open;
                    break;
                case Entry.ES_PROGRESSED:
                    icon = R.drawable.ic_action_todo_progressed;
                    break;
                case Entry.ES_DONE:
                    icon = R.drawable.ic_action_todo_done;
                    break;
                case Entry.ES_CANCELED:
                    icon = R.drawable.ic_action_todo_canceled;
                    break;
            }
            mMenu.findItem( R.id.change_todo_status ).setIcon( icon );
        }
    }

    // DialogCalendar.Listener INTERFACE METHODS
    public Activity getActivity() {
        return this;
    }
    public void createChapter( long date ) { } // dummy

    // DiaryManager INTERFACE METHODS
    public void addFragment( FragmentElemList fragment ) {
        mFragmentList = fragment;
    }
    public void removeFragment( FragmentElemList fragment ) {
        mFragmentList = null;
    }
    public DiaryElement getElement() {
        return mElement;
    }
    public int getTabIndex() { // dummy
        return 0;
    }

    // ListOperations INTERFACE METHODS
    public void updateList() {
        if( mFragmentList != null )
            mFragmentList.updateList();
    }

    // DialogThemeHost INTERFACE METHODS
    public void onDialogThemeClose() {
        //mActionBar.setIcon( mElement.get_icon() );
    }

    // Spinner INTERFACE METHODS
    public void onItemSelected( AdapterView<?> parent, View view, int pos, long id ) {
        switch( pos ) {
            case 0:
                mElement.set_chart_type( ChartPoints.BOOLEAN );
                mAtvTagUnit.setVisibility( View.GONE );
                break;
            case 1:
                mElement.set_chart_type( ChartPoints.CUMULATIVE );
                mAtvTagUnit.setVisibility( View.VISIBLE );
                break;
            case 2:
                mElement.set_chart_type( ChartPoints.AVERAGE );
                mAtvTagUnit.setVisibility( View.VISIBLE );
                break;
        }
        mViewChart.set_points( mElement.create_chart_data(), 1f );
    }
    public void onNothingSelected( AdapterView<?> parent ) {
        // do nothing?
    }

    private FragmentElemList mFragmentList = null;
    private ActionBar mActionBar = null;
    private Menu mMenu = null;
    private DrawerLayout mDrawerLayout = null;
    private ViewChart mViewChart;
    private AutoCompleteTextView mAtvTagUnit;

    private DiaryElementChart mElement = null;
}
