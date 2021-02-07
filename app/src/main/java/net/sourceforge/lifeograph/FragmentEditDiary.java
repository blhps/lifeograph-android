/* *********************************************************************************

    Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)

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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

public class FragmentEditDiary extends Fragment
        implements DialogInquireText.InquireListener,
        DialogCalendar.Listener, FragmentEntryList.ListOperations, PopupMenu.OnMenuItemClickListener,
        ActionMode.Callback, Lifeograph.DiaryEditor
{
    @Override
    //protected void onCreate( Bundle savedInstanceState ) {
    public View
    onCreateView( @NonNull LayoutInflater inflater,
                  ViewGroup container,
                  Bundle savedInstanceState ) {
        ViewGroup rootView;

        // PICKING UP THE APPROPRIATE LAYOUT
        if( Lifeograph.getScreenWidth() >= 4.0 ) {
            rootView = ( ViewGroup ) inflater.inflate( R.layout.diary_wide, container, false );

            mButtonCalendar = rootView.findViewById( R.id.button_calendar );
            //mButtonCalendar.setOnClickListener( view -> new DialogCalendar( FragmentEditDiary
            // .this, Diary.diary.is_in_edit_mode() ).show() );

            ViewPager pagerCalendar = rootView.findViewById( R.id.pager_calendar );
            mCalPagerAdapter = new PagerAdapterCalendar( pagerCalendar );
        }
        else {
            mCalPagerAdapter = null;
            rootView = ( ViewGroup ) inflater.inflate( R.layout.diary, container, false );
        }

        return rootView;
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState) {
        Log.d( Lifeograph.TAG, "ActivityDiary.onCreate()" );

        Lifeograph.updateScreenSizes( getContext() );

        // FILLING WIDGETS
        mDrawerLayout = view.findViewById( R.id.drawer_layout );
        //mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        // LISTENERS
//        mDrawerLayout.addDrawerListener( new DrawerLayout.DrawerListener()
//        {
//            public void onDrawerSlide( @NonNull View view, float v ) { }
//
//            public void onDrawerOpened( @NonNull View view ) {
//
//                for( FragmentElemList fragment : mDiaryFragments ) {
//                    if( fragment.isVisible() )
//                        fragment.getListView().setEnabled( false );
//                }

                // alternative way:
//                for( int i = 0; i < 3; i++ ) {
//                    FragmentElemList fragment = ( FragmentElemList ) getFragmentManager()
//                            .findFragmentByTag( TabsAdapter.makeFragmentName( i ) );
//                    if( fragment != null )
//                        if( fragment.isVisible() )
//                            fragment.getListView().setEnabled( false );
//                }
//            }
//
//            public void onDrawerClosed( @NonNull View view ) {
//                for( FragmentElemList fragment : mDiaryFragments ) {
//                    if( fragment.isVisible() )
//                        fragment.getListView().setEnabled( true );
//                }
//            }
//
//            public void onDrawerStateChanged( int i ) { }
//        } );

        // ACTIONBAR
//        ActionBar mActionBar = getSupportActionBar();
//        if( mActionBar != null ) {
//            mActionBar.setDisplayHomeAsUpEnabled( true );
//            mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
//            //mActionBar.setIcon( R.drawable.ic_diary );
//            setTitle( Diary.diary.get_title_str() );
//            // TODO mActionBar.setSubtitle( Diary.diary.get_info_str() );
//        }

//        mPager = view.findViewById( R.id.pager );
//        TabsAdapter mTabsAdapter = new TabsAdapter( this, mPager );
//
//        Bundle args = new Bundle();
//        args.putInt( "tab", 0 );
//        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.all_entries ),
//                             args );
//        args = new Bundle();
//        args.putInt( "tab", 1 );
//        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.chapters ),
//                             args );
//        args = new Bundle();
//        args.putInt( "tab", 2 );
//        mTabsAdapter.addTab( mActionBar.newTab().setText( R.string.tags ),
//                             args );

        // CHART
//        mViewChart = findViewById( R.id.chart_view_diary );
//        mViewChart.set_points( Diary.diary.create_chart_data(), 1f );
//        mViewChart.setListener( type -> {
//            Diary.diary.set_chart_type( type );
//            mViewChart.set_points( Diary.diary.create_chart_data(), 1f );
//        } );

//        if( savedInstanceState != null ) {
//            mActionBar.setSelectedNavigationItem( savedInstanceState.getInt( "tab", 0 ) );
//        }
//
//        if( !Lifeograph.getAddFreeNotPurchased() ) {
//            LinearLayout container = view.findViewById( R.id.main_container );
//            View ad = view.findViewById( R.id.fragmentAd );
//            container.removeView( ad );
//        }
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "ActivityDiary.onPause()" );
    }*/

    @Override
    public void onStop() {
        super.onStop();

        Log.d( Lifeograph.TAG, "ActivityDiary.onStop()" );

        Diary.diary.writeLock();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d( Lifeograph.TAG, "ActivityDiary.onResume()" );

        Diary.diary.setSavingEnabled( true );

        if( mMenu != null )
            updateMenuVisibilities();
    }

//    @Override
//    protected void onSaveInstanceState( @NonNull Bundle outState ) {
//        super.onSaveInstanceState( outState );
//        //outState.putInt( "tab", getSupportActionBar().getSelectedNavigationIndex() );
//
//        Log.d( Lifeograph.TAG, "ActivityDiary.onSaveInstanceState()" );
//    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//    }


//


    // POPUP MENU LISTENER
    public boolean onMenuItemClick( MenuItem item ) {
        switch( item.getItemId() ) {
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

        popup.inflate( R.menu.menu_chapter_ctg );

        popup.show();
    }

    private void updateMenuVisibilities(){
        boolean flagWritable = Diary.diary.is_in_edit_mode();
        boolean flagEncrypted = Diary.diary.is_encrypted();

        mMenu.findItem( R.id.enable_edit ).setVisible( !flagWritable &&
                                                       Diary.diary.can_enter_edit_mode() );

        mMenu.findItem( R.id.export_plain_text ).setVisible( !Diary.diary.is_virtual() );

        mMenu.findItem( R.id.add_password ).setVisible( flagWritable && !flagEncrypted );
        mMenu.findItem( R.id.change_password ).setVisible( flagWritable && flagEncrypted );

        mMenu.findItem( R.id.logout_wo_save ).setVisible( flagWritable );
    }

    @Override
    public void enableEditing(){
        boolean flagEncrypted = Diary.diary.is_encrypted();

        mMenu.findItem( R.id.enable_edit ).setVisible( false );

        mMenu.findItem( R.id.add_password ).setVisible( !flagEncrypted );
        mMenu.findItem( R.id.change_password ).setVisible( flagEncrypted );

        mMenu.findItem( R.id.logout_wo_save ).setVisible( true );
    }

    public void createChapter( long date ) {
        mDateLast = date;

        DialogInquireText dlg = new DialogInquireText( getContext(),
                                                       R.string.create_chapter,
                                                       Lifeograph.getStr( R.string.new_chapter ),
                                                       R.string.create,
                                                       this );
        dlg.show();
    }

    void renameCtg( int id ) {
        DialogInquireText dlg = new DialogInquireText( getContext(),
                                                       id,
                                                       mElemMenu.m_name,
                                                       R.string.rename,
                                                       this );
        dlg.show();
    }

    void dismissChapterCtg() {
        Lifeograph.showConfirmationPrompt( getContext(),
                                           R.string.chapter_ctg_dismiss_confirm,
                                           R.string.dismiss,
                                           ( dialog, id ) -> {
                                               Diary.diary.dismiss_chapter_ctg(
                                                       ( Chapter.Category ) mElemMenu );
                                               updateList();
                                           }
        );
    }

    //  TODO WILL BE IMPLEMENTED IN 0.8+
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
                Chapter chapter = Diary.diary.m_p2chapter_ctg_cur.create_chapter( mDateLast,
                                                                                  false,
                                                                                  false,
                                                                                  true );
                chapter.set_text( text );
                Diary.diary.update_entries_in_chapters();
                Lifeograph.showElem( chapter );
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
            case R.string.rename_chapter_ctg:
                return !Diary.diary.m_chapter_categories.containsKey( s );
            default:
                return true;
        }
    }

    // DiaryManager INTERFACE METHODS
//    public void addFragment( FragmentElemList fragment ) {
//        mDiaryFragments.add( fragment );
//    }
//    public void removeFragment( FragmentElemList fragment ) {
//        mDiaryFragments.remove( fragment );
//    }
    public DiaryElement getElement() {
        return Diary.diary;
    }

    // DialogCalendar.Listener INTERFACE METHODS
    public Activity
    getRelatedActivity() {
        return getActivity();
    }

    // FragmentElemList.ListOperations INTERFACE METHODS
    public void updateList() {
//        for( FragmentElemList fragment : mDiaryFragments )
//            fragment.updateList();
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
                !Diary.diary.m_p2chapter_ctg_cur.mMap.containsKey(
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
                        mCalPagerAdapter.getSelectedDate().m_date, "", false );
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

    // TAG DIALOG HOST METHODS =====================================================================
//    public void onDialogTagsClose() {
//        updateList();
//    }
//    public List< Entry > getTags() {
//        return( ( Tag.Category ) mElemMenu ).mTags;
//    }
//    public void addTag( Entry t ) {
//        t.set_category( ( Tag.Category ) mElemMenu );
//    }
//    public void removeTag( Tag t ) {
//        t.set_category( null );
//    }

    // VARIABLES ===================================================================================
    //private LayoutInflater mInflater;
    protected ViewPager mPager;
    private PagerAdapterCalendar mCalPagerAdapter = null;
    //private List< FragmentElemList > mDiaryFragments = new java.util.ArrayList<>();

    private Menu mMenu = null;

    private Button mButtonCalendar;

    private DrawerLayout mDrawerLayout = null;

    private ActionMode mActionMode;

    private ViewChart mViewChart;

    private long mDateLast;
    private DiaryElement mElemMenu;

    // TABS ADAPTER ================================================================================
    // partly based on Support Library FragmentPagerAdapter implementation
    public static class TabsAdapter extends PagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener
    {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList< TabInfo > mTabs = new ArrayList<>();
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

        TabsAdapter( AppCompatActivity activity, ViewPager pager ) {
            mContext = activity;
            mActionBar = activity.getSupportActionBar();
            mFragMan = activity.getSupportFragmentManager();
            mViewPager = pager;
            mViewPager.setAdapter( this );
            mViewPager.addOnPageChangeListener( this );
        }

        @Override
        public boolean isViewFromObject( @NonNull View view, @NonNull Object object ) {
            return( ( Fragment ) object).getView() == view;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @NonNull
        @Override
        public Object instantiateItem( @NonNull ViewGroup container, int position ) {
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
        public void destroyItem( @NonNull ViewGroup container, int pos, @NonNull Object object ) {
            if( mCurTransaction == null ) {
                mCurTransaction = mFragMan.beginTransaction();
            }
            Log.d( Lifeograph.TAG, "Detaching item #" + pos + ": f=" + object
                    + " v=" + ( ( Fragment ) object ).getView() );
            mCurTransaction.detach( ( Fragment ) object );
        }

        @Override
        public void setPrimaryItem( @NonNull ViewGroup container, int pos,
                                    @NonNull Object object ) {
            Fragment fragment = ( Fragment ) object;
            if( fragment != mCurrentPrimaryItem ) {
                if( mCurrentPrimaryItem != null ) {
                    mCurrentPrimaryItem.setMenuVisibility( false );
                }
                fragment.setMenuVisibility( true );
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public void startUpdate( @NonNull ViewGroup container ) {
        }

        @Override
        public void finishUpdate( @NonNull ViewGroup container ) {
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

        static String makeFragmentName( int index ) {
            return "DiaryTabs.fragment" + index;
        }

        void addTab( ActionBar.Tab tab, Bundle args ) {
            TabInfo info = new TabInfo( FragmentEntryList.class, args );
            tab.setTag( info );
            tab.setTabListener( this );
            mTabs.add( info );
            mActionBar.addTab( tab );
            notifyDataSetChanged();
        }

        Fragment getItem( int position ) {
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
        PagerAdapterCalendar( ViewPager pager ) {
            mViewPager = pager;
            mViewPager.setAdapter( this );
            mViewPager.setOnPageChangeListener( this );

            mViewPager.setCurrentItem( 1, false );

            initGVs();
            updateGVs();
        }

        private void initGVs() {
            for( int i = 0; i < 3; i++ ) {
                mGVs[ i ] = new GridView( FragmentEditDiary.this.getContext() );
                mGridAdapters[ i ] = new GridCalAdapter( FragmentEditDiary.this.getContext() );
                mGVs[ i ].setAdapter( mGridAdapters[ i ] );
                mGVs[ i ].setNumColumns( 7 );
                mGVs[ i ].setVerticalSpacing( 5 );
                mGVs[ i ].setStretchMode( GridView.STRETCH_COLUMN_WIDTH );
                mGVs[ i ].setSelector( R.drawable.themed_selector );
            }

            mGVs[ 1 ].setOnItemLongClickListener( ( arg0, view, pos, arg3 ) -> {
                mGridAdapters[ 1 ].mDateCurrent =
                        new Date( mGridAdapters[ 1 ].mListDays.get( pos ) );
                view.setSelected( true );

                if( mActionMode != null )
                    mActionMode.finish();

                //mActionMode =
                  //      FragmentEntryList.this.startActionMode( FragmentEntryList.this
                //      .getContext() );

                return false;
            } );
        }

        private void updateGVs() {
            Date datePrev = new Date( mDateCur.m_date );
            datePrev.backward_months( 1 );
            Date dateNext = new Date( mDateCur.m_date );
            dateNext.forward_months( 1 );

            mGridAdapters[ 0 ].showMonth( datePrev );
            mGridAdapters[ 1 ].showMonth( mDateCur );
            mGridAdapters[ 2 ].showMonth( dateNext );

            mButtonCalendar.setText( mDateCur.format_string_ym() );
        }

        Date getSelectedDate() {
            return mGridAdapters[ 1 ].mDateCurrent;
        }

        @NonNull
        @Override
        public Object instantiateItem( ViewGroup container, int position ) {
            Log.d( Lifeograph.TAG, "pager adapter calendar instantiate item: " + position );

            container.addView( mGVs[ position ] );

            return mGVs[ position ];
        }

        @Override
        public void destroyItem( ViewGroup container, int pos, @NonNull Object object ) {
            container.removeView( ( View ) object );
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject( @NonNull View view, @NonNull Object object ) {
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
                    mDateCur.backward_months( 1 );
                // go forward:
                else if( mPosCur == 2 )
                    mDateCur.forward_months( 1 );

                updateGVs();

                mViewPager.setCurrentItem( 1, false );
            }
        }

        final ViewPager mViewPager;
        Date mDateCur = new Date( Date.get_today( 0 ) );
        int mPosCur = 1;

        GridView[] mGVs = new GridView[ 3 ];
        GridCalAdapter[] mGridAdapters = new GridCalAdapter[ 3 ];
    }
}
