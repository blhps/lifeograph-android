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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

public class ActivityDiary extends Activity
        implements DialogInquireText.InquireListener, FragmentElemList.DiaryManager,
        DialogCalendar.Listener, FragmentElemList.ListOperations, PopupMenu.OnMenuItemClickListener,
        DialogTags.DialogTagsHost, ActionMode.Callback, DialogPassword.Listener
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Log.d( Lifeograph.TAG, "ActivityDiary.onCreate()" );

        Lifeograph.sContext = this;
        Lifeograph.updateScreenSizes();
        Lifeograph.sNumberOfDiaryEditingActivities++;

        // PICKING UP THE APPROPRIATE LAYOUT
        if( Lifeograph.getScreenWidth() >= 4.0 ) {
            setContentView( R.layout.diary_wide );

            mButtonCalendar = ( Button ) findViewById( R.id.button_calendar );
            mButtonCalendar.setOnClickListener( new View.OnClickListener()
            {
                public void onClick( View view ) {
                    new DialogCalendar( ActivityDiary.this, !Diary.diary.is_read_only() ).show();
                }
            } );

            ViewPager pagerCalendar = ( ViewPager ) findViewById( R.id.pager_calendar );
            mCalPagerAdapter = new PagerAdapterCalendar( pagerCalendar );
        }
        else {
            mCalPagerAdapter = null;
            setContentView( R.layout.diary );
        }

        // FILLING WIDGETS
        mDrawerLayout = ( DrawerLayout ) findViewById( R.id.drawer_layout );
        //mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        // LISTENERS
        mDrawerLayout.setDrawerListener( new DrawerLayout.DrawerListener()
        {
            public void onDrawerSlide( View view, float v ) { }

            public void onDrawerOpened( View view ) {

                for( FragmentElemList fragment : mDiaryFragments ) {
                    if( fragment.isVisible() )
                        fragment.getListView().setEnabled( false );
                }

                // alternative way:
//                for( int i = 0; i < 3; i++ ) {
//                    FragmentElemList fragment = ( FragmentElemList ) getFragmentManager()
//                            .findFragmentByTag( TabsAdapter.makeFragmentName( i ) );
//                    if( fragment != null )
//                        if( fragment.isVisible() )
//                            fragment.getListView().setEnabled( false );
//                }
            }

            public void onDrawerClosed( View view ) {
                for( FragmentElemList fragment : mDiaryFragments ) {
                    if( fragment.isVisible() )
                        fragment.getListView().setEnabled( true );
                }
            }

            public void onDrawerStateChanged( int i ) { }
        } );

        // ACTIONBAR
        mActionBar = getActionBar();
        if( mActionBar != null ) {
            mActionBar.setDisplayHomeAsUpEnabled( true );
            mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
            mActionBar.setIcon( R.drawable.ic_diary );
            setTitle( Diary.diary.get_title_str() );
            mActionBar.setSubtitle( Diary.diary.get_info_str() );
        }

        mPager = ( ViewPager ) findViewById( R.id.pager );
        mTabsAdapter = new TabsAdapter( this, mPager );

        Bundle args = new Bundle();
        args.putInt( "tab", 0 );
        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.all_entries ),
                             FragmentElemList.class, args );
        args = new Bundle();
        args.putInt( "tab", 1 );
        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.chapters ),
                             FragmentElemList.class, args );
        args = new Bundle();
        args.putInt( "tab", 2 );
        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.tags ),
                             FragmentElemList.class, args );

        if( savedInstanceState != null ) {
            mActionBar.setSelectedNavigationItem( savedInstanceState.getInt( "tab", 0 ) );
        }

        if( !Lifeograph.getAddFreeNotPurchased() ) {
            LinearLayout container = ( LinearLayout ) findViewById( R.id.main_container );
            View ad = findViewById( R.id.fragmentAd );
            container.removeView( ad );
        }

        Lifeograph.sLoginStatus = Lifeograph.LoginStatus.LOGGED_IN;
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "ActivityDiary.onPause()" );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d( Lifeograph.TAG, "ActivityDiary.onDestroy()" );

        Lifeograph.handleDiaryEditingActivityDestroyed();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d( Lifeograph.TAG, "ActivityDiary.onResume()" );

        Lifeograph.sContext = this;

        Lifeograph.sSaveDiaryOnLogout = true;
    }

    @Override
    protected void onSaveInstanceState( @NonNull Bundle outState ) {
        super.onSaveInstanceState( outState );
        outState.putInt( "tab", getActionBar().getSelectedNavigationIndex() );
    }

//  overriding onBackPressed is no longer necessary
//    @Override
//    public void onBackPressed() {
//        if( mParentElem == Diary.diary ) {
//            super.onBackPressed();
//        }
//        else {
//            mParentElem = Diary.diary;
//            update_entry_list();
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_diary, menu );

        MenuItem item = menu.findItem( R.id.add_elem );
        AddElemAction addElemAction = ( AddElemAction ) item.getActionProvider();
        addElemAction.mParent = this;

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        boolean flagWritable = !Diary.diary.is_read_only();
        boolean flagEncrypted = Diary.diary.is_encrypted();

        menu.findItem( R.id.add_elem ).setVisible( flagWritable );

        menu.findItem( R.id.calendar ).setVisible( mCalPagerAdapter == null );

//  TODO WILL BE IMPLEMENTED IN 0.5
//        menu.findItem( R.id.change_sort_type ).setVisible( mParentElem != null );

        menu.findItem( R.id.export_plain_text ).setVisible( !Diary.diary.is_virtual() );

        menu.findItem( R.id.add_password ).setVisible( flagWritable && !flagEncrypted );
        menu.findItem( R.id.change_password ).setVisible( flagWritable && flagEncrypted );

        menu.findItem( R.id.logout_wo_save ).setVisible( flagWritable );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.calendar:
                new DialogCalendar( this, !Diary.diary.is_read_only() ).show();
                return true;
            case R.id.filter:
                if( mDrawerLayout.isDrawerOpen( Gravity.RIGHT ) )
                    mDrawerLayout.closeDrawer( Gravity.RIGHT );
                else
                    mDrawerLayout.openDrawer( Gravity.RIGHT );
                return true;
            case R.id.add_password:
                new DialogPassword( this,
                                    Diary.diary,
                                    DialogPassword.DPAction.DPA_ADD,
                                    this ).show();
                return true;
            case R.id.change_password:
                new DialogPassword( this,
                                    Diary.diary,
                                    DialogPassword.DPAction.DPA_AUTHENTICATE,
                                    this ).show();
                return true;
            case R.id.export_plain_text:
                if( Diary.diary.write_txt() == Result.SUCCESS )
                    Lifeograph.showToast( R.string.text_export_success );
                else
                    Lifeograph.showToast( R.string.text_export_fail );
                return true;
            case R.id.logout_wo_save:
                Lifeograph.showConfirmationPrompt( R.string.logoutwosaving_confirm,
                                                   R.string.logoutwosaving,
                                                   new DialogInterface.OnClickListener()
                                                   {
                                                       public void onClick( DialogInterface
                                                                                    dialog,
                                                                            int id ) {
                                                           // unlike desktop version Android version
                                                           // does not back up changes
                                                           Lifeograph.sSaveDiaryOnLogout = false;
                                                           finish();
                                                       }
                                                   }, null );
                return true;
//  TODO WILL BE IMPLEMENTED IN 0.5
//            case R.id.import_sms:
//                import_messages();
//                return true;
        }
        return super.onOptionsItemSelected( item );
    }

    // POPUP MENU LISTENER
    public boolean onMenuItemClick( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.edit_tag_ctg:
                ( new DialogTags( this, this ) ).show();
                return true;
            case R.id.rename_tag_ctg:
                renameCtg( R.string.rename_tag_ctg );
                return true;
            case R.id.dismiss_tag_ctg:
                dismissTagCtg( false );
                return true;
            case R.id.dismiss_tag_ctg_and_tags:
                dismissTagCtg( true );
                return true;
            case R.id.rename_chapter_ctg:
                renameCtg( R.string.rename_chapter_ctg );
                return true;
            case R.id.dismiss_chapter_ctg:
                dismissChapterCtg();
                return true;
            default:
                return false;
        }
    }

    public void CreateCtgMenu( View v ) {
        mElemMenu = ( DiaryElement ) v.getTag();

        PopupMenu popup = new PopupMenu( getActivity(), v );
        popup.setOnMenuItemClickListener( this );

        if( mElemMenu.get_type() == DiaryElement.Type.TAG_CTG )
            popup.inflate( R.menu.menu_tag_ctg );
        else
            popup.inflate( R.menu.menu_chapter_ctg );

        popup.show();
    }

    void goToToday() {
        Entry entry = Diary.diary.get_entry_today();

        if( entry == null ) // add new entry if no entry exists on selected date
            entry = Diary.diary.add_today();

        Lifeograph.showElem( entry );
    }

    public void createChapter( long date ) {
        mDateLast = date;

        DialogInquireText dlg = new DialogInquireText( this,
                                                       R.string.create_chapter,
                                                       Lifeograph.getStr( R.string.new_chapter ),
                                                       R.string.create,
                                                       this );
        dlg.show();
    }
    void createTopic() {
        DialogInquireText dlg = new DialogInquireText( this,
                                                       R.string.create_topic,
                                                       Lifeograph.getStr( R.string.new_chapter ),
                                                       R.string.create,
                                                       this );
        dlg.show();
    }
    void createGroup() {
        DialogInquireText dlg = new DialogInquireText( this,
                                                       R.string.create_group,
                                                       Lifeograph.getStr( R.string.new_chapter ),
                                                       R.string.create,
                                                       this );
        dlg.show();
    }
    void createTagCtg() {
        DialogInquireText dlg = new DialogInquireText(
                this,
                R.string.create_tag_ctg,
                Diary.diary.create_unique_name_for_map(
                        Diary.diary.m_tag_categories,
                        Lifeograph.getStr( R.string.new_tag_ctg ) ),
                R.string.create,
                this );
        dlg.show();
    }

    void renameCtg( int id ) {
        DialogInquireText dlg = new DialogInquireText( this,
                                                       id,
                                                       mElemMenu.m_name,
                                                       R.string.rename,
                                                       this );
        dlg.show();
    }

    void dismissTagCtg( final boolean tags_too ) {
        Lifeograph.showConfirmationPrompt( tags_too ?
                                                   R.string.tag_ctg_dismiss_with_entries_confirm :
                                                   R.string.tag_ctg_dismiss_confirm,
                                           R.string.dismiss,
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick( DialogInterface dialog,
                                                                    int id ) {
                                                   Diary.diary.dismiss_tag_ctg(
                                                           ( Tag.Category ) mElemMenu, tags_too );
                                                   updateList();
                                               }
                                           },
                                           null );
    }
    void dismissChapterCtg() {
        Lifeograph.showConfirmationPrompt( R.string.chapter_ctg_dismiss_confirm,
                                           R.string.dismiss,
                                           new DialogInterface.OnClickListener()
                                           {
                                               public void onClick( DialogInterface dialog,
                                                                    int id ) {
                                                   Diary.diary.dismiss_chapter_ctg(
                                                           ( Chapter.Category ) mElemMenu );
                                                   updateList();
                                               }
                                           },
                                           null );
    }

    //  TODO WILL BE IMPLEMENTED IN 0.5
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

    // InquireListener INTERFACE METHODS
    public void onInquireAction( int id, String text ) {
        switch( id ) {
            case R.string.create_chapter: {
                Chapter chapter = Diary.diary.m_ptr2chapter_ctg_cur.create_chapter( text,
                                                                                    mDateLast );
                Diary.diary.update_entries_in_chapters();
                Lifeograph.showElem( chapter );
                break;
            }
            case R.string.create_topic: {
                Chapter chapter = Diary.diary.m_topics.create_chapter_ordinal( text );
                Diary.diary.update_entries_in_chapters();
                Lifeograph.showElem( chapter );
                break;
            }
            case R.string.create_group: {
                Chapter chapter = Diary.diary.m_groups.create_chapter_ordinal( text );
                Diary.diary.update_entries_in_chapters();
                Lifeograph.showElem( chapter );
                break;
            }
            case R.string.create_tag_ctg: {
                Diary.diary.create_tag_ctg( text );
                updateList();
                break;
            }
            case R.string.rename_tag_ctg: {
                Diary.diary.rename_tag_ctg( ( Tag.Category ) mElemMenu, text );
                updateList();
                break;
            }
            case R.string.rename_chapter_ctg: {
                Diary.diary.rename_chapter_ctg( ( Chapter.Category ) mElemMenu, text );
                updateList();
                break;
            }
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
            case R.string.create_tag_ctg:
            case R.string.rename_tag_ctg:
                return !Diary.diary.m_tag_categories.containsKey( s );
            case R.string.rename_chapter_ctg:
                return !Diary.diary.m_chapter_categories.containsKey( s );
            default:
                return true;
        }
    }

    // DiaryManager INTERFACE METHODS
    public void addFragment( FragmentElemList fragment ) {
        mDiaryFragments.add( fragment );
    }
    public void removeFragment( FragmentElemList fragment ) {
        mDiaryFragments.remove( fragment );
    }
    public DiaryElement getElement() {
        return Diary.diary;
    }

    // DialogCalendar.Listener INTERFACE METHODS
    public Activity getActivity() {
        return this;
    }

    // FragmentElemList.ListOperations INTERFACE METHODS
    public void updateList() {
        for( FragmentElemList fragment : mDiaryFragments )
            fragment.updateList();
    }

    // ActionMode.Callback INTERFACE METHODS
    public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate( R.menu.menu_calendar_contextual, menu );

        menu.findItem( R.id.open_entry ).setVisible(
                Diary.diary.m_entries.containsKey(
                        mCalPagerAdapter.getSelectedDate().m_date + 1 ) );

        menu.findItem( R.id.create_chapter ).setVisible(
                !Diary.diary.m_ptr2chapter_ctg_cur.mMap.containsKey(
                        mCalPagerAdapter.getSelectedDate().m_date ) );

        return true;
    }

    public boolean onActionItemClicked( ActionMode mode, MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.open_entry: {
                Entry e = Diary.diary.m_entries.get(
                        mCalPagerAdapter.getSelectedDate().m_date + 1 );
                Lifeograph.showElem( e );
                return true;
            }
            case R.id.create_entry: {
                Log.d( Lifeograph.TAG, "create entry" );
                mode.finish(); // Action picked, so close the CAB
                Entry e = Diary.diary.create_entry(
                        mCalPagerAdapter.getSelectedDate(), "", false );
                Lifeograph.showElem( e );
                return true;
            }
            case R.id.create_chapter:
                mode.finish(); // Action picked, so close the CAB
                createChapter( mCalPagerAdapter.getSelectedDate().m_date );
                return true;
            default:
                return false;
        }
    }

    public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
        return false; // Return false if nothing is done
    }

    public void onDestroyActionMode( ActionMode mode ) {
        mActionMode = null;
    }

    // DialogPassword INTERFACE METHODS
    public void onDPAction( DialogPassword.DPAction action ) {
        switch( action ) {
            case DPA_AUTHENTICATE:
                new DialogPassword( this, Diary.diary,
                                    DialogPassword.DPAction.DPA_ADD,
                                    this ).show();
                break;
            case DPAR_AUTH_FAILED:
                Lifeograph.showToast( R.string.wrong_password );
                break;
        }
    }

    // TAG DIALOG HOST METHODS =====================================================================
    public void onDialogTagsClose() {
        updateList();
    }
    public Entry getEntry() {
        return null;
    }
    public List< Tag > getTags() {
        return( ( Tag.Category ) mElemMenu ).mTags;
    }
    public void addTag( Tag t ) {
        t.set_category( ( Tag.Category ) mElemMenu );
    }
    public void removeTag( Tag t ) {
        t.set_category( null );
    }

    // VARIABLES ===================================================================================
    //private LayoutInflater mInflater;
    private ActionBar mActionBar = null;
    protected ViewPager mPager;
    private TabsAdapter mTabsAdapter;
    private PagerAdapterCalendar mCalPagerAdapter = null;
    private List< FragmentElemList > mDiaryFragments = new java.util.ArrayList< FragmentElemList >();

    private Button mButtonCalendar;

    private DrawerLayout mDrawerLayout = null;

    private ActionMode mActionMode;

    private long mDateLast;
    private DiaryElement mElemMenu;

    // TABS ADAPTER ================================================================================
    /* partly based on Support Library FragmentPagerAdapter implementation */
    public static class TabsAdapter extends PagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener
    {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList< TabInfo > mTabs = new ArrayList< TabInfo >();
        private final FragmentManager mFragMan;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem = null;

        static final class TabInfo
        {
            private final Class< ? > clss;
            private final Bundle args;

            TabInfo( Class< ? > _class, Bundle _args ) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter( Activity activity, ViewPager pager ) {
            mContext = activity;
            mActionBar = activity.getActionBar();
            mFragMan = activity.getFragmentManager();
            mViewPager = pager;
            mViewPager.setAdapter( this );
            mViewPager.setOnPageChangeListener( this );
        }

        @Override
        public boolean isViewFromObject( View view, Object object ) {
            return( ( Fragment ) object).getView() == view;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Object instantiateItem( ViewGroup container, int position ) {
            if( mCurTransaction == null )
                mCurTransaction = mFragMan.beginTransaction();

            String name = makeFragmentName( position );
            Fragment fragment = mFragMan.findFragmentByTag( name );
            if( fragment != null ) {
                Log.d( Lifeograph.TAG, "Attaching item #" + position + ": f=" + fragment );
                mCurTransaction.attach( fragment );
            }
            else {
                fragment = getItem( position );
                Log.d( Lifeograph.TAG, "Adding item #" + position + ": f=" + fragment );
                mCurTransaction.add( container.getId(), fragment,
                                     makeFragmentName( position ) );
            }
            if( fragment != mCurrentPrimaryItem ) {
                fragment.setMenuVisibility( false );
            }

            return fragment;
        }

        @Override
        public void destroyItem( ViewGroup container, int position, Object object ) {
            if( mCurTransaction == null ) {
                mCurTransaction = mFragMan.beginTransaction();
            }
            Log.d( Lifeograph.TAG, "Detaching item #" + position + ": f=" + object
                    + " v=" + ( ( Fragment ) object ).getView() );
            mCurTransaction.detach( ( Fragment ) object );
        }

        @Override
        public void setPrimaryItem( View container, int position, Object object ) {
            Fragment fragment = ( Fragment ) object;
            if( fragment != mCurrentPrimaryItem ) {
                if( mCurrentPrimaryItem != null ) {
                    mCurrentPrimaryItem.setMenuVisibility( false );
                }
                if( fragment != null ) {
                    fragment.setMenuVisibility( true );
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public void startUpdate( View container ) {
        }

        @Override
        public void finishUpdate( View container ) {
            if( mCurTransaction != null ) {
                Log.d( Lifeograph.TAG, "Commiting item transactions" );
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragMan.executePendingTransactions();
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState( Parcelable state, ClassLoader loader ) {
        }

        public static String makeFragmentName( int index ) {
            return "DiaryTabs.fragment" + index;
        }

        public void addTab( ActionBar.Tab tab, Class< ? > clss, Bundle args ) {
            TabInfo info = new TabInfo( clss, args );
            tab.setTag( info );
            tab.setTabListener( this );
            mTabs.add( info );
            mActionBar.addTab( tab );
            notifyDataSetChanged();
        }

        public Fragment getItem( int position ) {
            TabInfo info = mTabs.get( position );
            return Fragment.instantiate( mContext, info.clss.getName(), info.args );
        }

        public void onPageScrolled( int position, float positionOffset, int positionOffsetPixels ) {
        }

        public void onPageSelected( int position ) {
            mActionBar.setSelectedNavigationItem( position );
        }

        public void onPageScrollStateChanged( int state ) {
        }

        public void onTabSelected( ActionBar.Tab tab, FragmentTransaction ft ) {
            Object tag = tab.getTag();
            for( int i = 0; i < mTabs.size(); i++ ) {
                if( mTabs.get( i ) == tag ) {
                    mViewPager.setCurrentItem( i );
                }
            }
        }

        public void onTabUnselected( ActionBar.Tab tab, FragmentTransaction ft ) {
        }

        public void onTabReselected( ActionBar.Tab tab, FragmentTransaction ft ) {
        }
    }

    // CALENDAR PAGER ADAPTER ======================================================================
    public class PagerAdapterCalendar extends PagerAdapter
            implements ViewPager.OnPageChangeListener
    {
        public PagerAdapterCalendar( ViewPager pager ) {
            mViewPager = pager;
            mViewPager.setAdapter( this );
            mViewPager.setOnPageChangeListener( this );

            mViewPager.setCurrentItem( 1, false );

            initGVs();
            updateGVs();
        }

        private void initGVs() {
            for( int i = 0; i < 3; i++ ) {
                mGVs[ i ] = new GridView( ActivityDiary.this );
                mGridAdapters[ i ] = new GridCalAdapter( ActivityDiary.this );
                mGVs[ i ].setAdapter( mGridAdapters[ i ] );
                mGVs[ i ].setNumColumns( 7 );
                mGVs[ i ].setVerticalSpacing( 5 );
                mGVs[ i ].setStretchMode( GridView.STRETCH_COLUMN_WIDTH );
                mGVs[ i ].setSelector( R.drawable.themed_selector );
            }

            mGVs[ 1 ].setOnItemLongClickListener( new AdapterView.OnItemLongClickListener()
            {
                public boolean onItemLongClick( AdapterView< ? > arg0, View view,
                                                int pos, long arg3 ) {
                    mGridAdapters[ 1 ].mDateCurrent =
                            new Date( mGridAdapters[ 1 ].mListDays.get( pos ) );
                    view.setSelected( true );

                    if( mActionMode != null )
                        mActionMode.finish();

                    mActionMode = ActivityDiary.this.startActionMode( ActivityDiary.this );

                    return false;
                }
            } );
        }

        private void updateGVs() {
            Date datePrev = new Date( mDateCur.m_date );
            datePrev.backward_month();
            Date dateNext = new Date( mDateCur.m_date );
            dateNext.forward_month();

            mGridAdapters[ 0 ].showMonth( datePrev );
            mGridAdapters[ 1 ].showMonth( mDateCur );
            mGridAdapters[ 2 ].showMonth( dateNext );

            mButtonCalendar.setText( mDateCur.format_string_ym() );
        }

        public Date getSelectedDate() {
            return mGridAdapters[ 1 ].mDateCurrent;
        }

        @Override
        public Object instantiateItem( ViewGroup container, int position ) {
            Log.d( Lifeograph.TAG, "pager adapter calendar instantiate item: " + position );

            container.addView( mGVs[ position ] );

            return mGVs[ position ];
        }

        @Override
        public void destroyItem( ViewGroup container, int position, Object object ) {
            container.removeView( ( View ) object );
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject( View view, Object object ) {
            return( object == view );
        }

        // ViewPager.OnPageChangeListener INTERFACE METHODS
        public void onPageScrolled( int position, float posOffset, int posOffsetPixels ) { }
        public void onPageSelected( int position ) {
            mPosCur = position;
        }
        public void onPageScrollStateChanged( int state ) {
            if( state == ViewPager.SCROLL_STATE_IDLE ) {
                Log.d( Lifeograph.TAG, "PagerAdapterCalendar.onPageScrollStateChanged()" );

                // go back:
                if( mPosCur == 0 )
                    mDateCur.backward_month();
                // go forward:
                else if( mPosCur == 2 )
                    mDateCur.forward_month();

                updateGVs();

                mViewPager.setCurrentItem( 1, false );
            }
        }

        protected final ViewPager mViewPager;
        Date mDateCur = new Date( Date.get_today( 0 ) );
        int mPosCur = 1;

        GridView[] mGVs = new GridView[ 3 ];
        GridCalAdapter[] mGridAdapters = new GridCalAdapter[ 3 ];
    }
}
