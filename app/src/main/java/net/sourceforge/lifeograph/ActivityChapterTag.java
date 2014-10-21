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


import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ActivityChapterTag extends Activity implements ToDoAction.ToDoObject,
        DialogInquireText.InquireListener, DialogCalendar.Listener, FragmentElemList.DiaryManager,
        FragmentElemList.ListOperations
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.chapter );

        // ELEMENT TO SHOW
        mElement = Diary.diary.get_element( getIntent().getIntExtra( "elem", 0 ) );
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

        // UI UPDATES (must come before listeners)
        //updateFilterWidgets( Diary.diary.m_filter_active.get_status() );

        // LISTENERS
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
        mActionBar = getActionBar();
        if( mActionBar != null ) {
            mActionBar.setDisplayHomeAsUpEnabled( true );
            mActionBar.setIcon( mElement.get_icon() );
            setTitle( mElement.get_list_str() );
            mActionBar.setSubtitle( mElement.get_info_str() );
        }

        Log.d( Lifeograph.TAG, "onCreate - ActivityChapterTag" );
    }

    @Override
    protected void onPause() {
        Lifeograph.sFlagForceUpdateOnResume = true;
        Log.d( Lifeograph.TAG, "onPause - ActivityChapterTag" );
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Lifeograph.sFlagLogoutOnPause = true;
        if( Lifeograph.sFlagForceUpdateOnResume )
            mFragmentList.updateList();
        Lifeograph.sFlagForceUpdateOnResume = false;
        Log.d( Lifeograph.TAG, "onResume - ActivityChapterTag" );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_chapter_tag, menu );

        MenuItem item = menu.findItem( R.id.change_todo_status );
        ToDoAction ToDoAction = ( ToDoAction ) item.getActionProvider();
        ToDoAction.mObject = this;

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

//  TODO WILL BE IMPLEMENTED IN 0.4
//        item = menu.findItem( R.id.change_sort_type );
//        item.setVisible( mParentElem != null );

        item = menu.findItem( R.id.add_entry );
        item.setVisible( ( type == DiaryElement.Type.TOPIC || type == DiaryElement.Type.GROUP ) &&
                         flagWritable );

        item = menu.findItem( R.id.dismiss );
        item.setVisible( !flagPseudoElement && flagWritable );

        item = menu.findItem( R.id.rename );
        item.setVisible( !flagPseudoElement && flagWritable );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
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
                Lifeograph.showElem( this, entry );
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

    private void dismiss_chapter() {
        Lifeograph.showConfirmationPrompt( this,
                                           R.string.chapter_dismiss_confirm,
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
        Lifeograph.showConfirmationPrompt( this,
                                           R.string.tag_dismiss_confirm, R.string.dismiss,
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
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
            case R.string.rename_tag:
                return !Diary.diary.m_tags.containsKey( s );
            case R.string.rename_chapter:
                return( mElement.m_name.compareTo( s ) != 0 );
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
                    mActionBar.setIcon( mElement.get_icon() );
                    return;
                default:
                    break;
            }
        }

        Log.w( Lifeograph.TAG, "cannot set todo status" );
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

    private FragmentElemList mFragmentList = null;
    private ActionBar mActionBar = null;
    private DrawerLayout mDrawerLayout = null;
    private DiaryElement mElement = null;
}
