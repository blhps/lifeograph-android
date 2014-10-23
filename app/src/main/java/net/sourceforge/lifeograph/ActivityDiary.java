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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class ActivityDiary extends Activity
        implements DialogInquireText.InquireListener, FragmentElemList.DiaryManager,
        DialogCalendar.Listener, FragmentElemList.ListOperations
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Log.d( Lifeograph.TAG, "ActivityDiary.onCreate()" );

        Lifeograph.updateScreenWidth();
        Lifeograph.sNumberOfDiaryEditingActivities++;

        setContentView( R.layout.diary );

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

        if( Lifeograph.sFlagUpdateListOnResume )
            updateList();
        Lifeograph.sFlagUpdateListOnResume = false;
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
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

        MenuItem item = menu.findItem( R.id.add_elem );
        item.setVisible( flagWritable );

//  TODO WILL BE IMPLEMENTED IN 0.4
//        item = menu.findItem( R.id.change_sort_type );
//        item.setVisible( mParentElem != null );

        item = menu.findItem( R.id.export_plain_text );
        item.setVisible( !Diary.diary.is_virtual() );

        item = menu.findItem( R.id.logout_wo_save );
        item.setVisible( flagWritable );

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
//  TODO WILL BE IMPLEMENTED IN 0.4
//            case R.id.import_sms:
//                import_messages();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
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

    void goToToday() {
        Entry entry = Diary.diary.get_entry_today();

        if( entry == null ) // add new entry if no entry exists on selected date
            entry = Diary.diary.add_today();

        Lifeograph.showElem( entry );
    }

    public void createChapter( long date ) {
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

//  TODO WILL BE IMPLEMENTED IN 0.4
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
    //private LayoutInflater mInflater;
    private ActionBar mActionBar = null;
    private ViewPager mPager;
    private TabsAdapter mTabsAdapter;
    private List< FragmentElemList > mDiaryFragments = new java.util.ArrayList< FragmentElemList >();

    private DrawerLayout mDrawerLayout = null;

    private long mDateLast;

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

}
