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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import net.sourceforge.lifeograph.inappbilling.util.IabBroadcastReceiver;
import net.sourceforge.lifeograph.inappbilling.util.IabHelper;
import net.sourceforge.lifeograph.inappbilling.util.IabResult;
import net.sourceforge.lifeograph.inappbilling.util.Inventory;
import net.sourceforge.lifeograph.inappbilling.util.Purchase;

public class ActivityLogin extends AppCompatActivity
        implements IabBroadcastReceiver.IabBroadcastListener, FragmentHost
{
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Lifeograph.mActivityLogin = this;
        Lifeograph.updateScreenSizes( this );

        if( Diary.diary == null )
            Diary.diary = new Diary();

        // IN APP BILLING
        mIabHelper = new IabHelper( this, IDs.base64EncodedPublicKey );
        //mIabHelper.enableDebugLogging( true );

        mIabHelper.startSetup( result -> {
            if( !result.isSuccess() ) {
                Log.d( Lifeograph.TAG, "IAB setup failed: " + result );
                return;
            }
            if( mIabHelper == null )
                return;
            mIabBroadcastReceiver = new IabBroadcastReceiver( ActivityLogin.this );
            IntentFilter broadcastFilter = new IntentFilter( IabBroadcastReceiver.ACTION );
            registerReceiver( mIabBroadcastReceiver, broadcastFilter );
            Log.d( Lifeograph.TAG, "IAB setup successful" );

            try {
                mIabHelper.queryInventoryAsync( mGotInventoryListener );
            }
            catch( IabHelper.IabAsyncInProgressException e ) {
                Log.e( Lifeograph.TAG,
                       "Error querying inventory. Another async operation in progress." );
            }
        } );

        // PREFERENCES
        PreferenceManager.setDefaultValues( getApplicationContext(), R.xml.pref_general, false );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext() );
        FragmentDiaryList.sExternalStorage = prefs.getString(
                Lifeograph.getStr( R.string.pref_DIARY_STORAGE_key ), "N/A" );
        FragmentDiaryList.sDiaryPath = prefs.getString(
                Lifeograph.getStr( R.string.pref_DIARY_PATH_key ), "N/A" );

        Date.s_format_order = prefs.getString(
                Lifeograph.getStr( R.string.pref_DATE_FORMAT_ORDER_key ), "N/A" );
        Date.s_format_separator = prefs.getString(
                Lifeograph.getStr( R.string.pref_DATE_FORMAT_SEPARATOR_key ), "." ).charAt( 0 );

        Lifeograph.sOptImperialUnits = prefs.getString(
                Lifeograph.getStr( R.string.pref_UNIT_TYPE_key ), "M" ).equals( "I" );

        // CONTENT
        setContentView( R.layout.home );

        Toolbar toolbar = findViewById( R.id.toolbar_main );
        setSupportActionBar( toolbar );

        DrawerLayout   drawerLayout   = findViewById( R.id.drawer_layout );
        NavigationView navigationView = findViewById( R.id.nav_view );
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_diaries, R.id.nav_settings, R.id.nav_about )
                .setDrawerLayout( drawerLayout )
                .build();

        NavHostFragment navHostFragment = ( NavHostFragment )
                getSupportFragmentManager().findFragmentById( R.id.nav_host_fragment );
        assert navHostFragment != null;
        mNavController = navHostFragment.getNavController();

//        AppBarConfiguration appBarConfiguration =
//                new AppBarConfiguration.Builder( navController.getGraph() )
//                        .setOpenableLayout( drawerLayout )
//                        .build();


//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // Show and Manage the Drawer and Back Icon
        NavigationUI.setupActionBarWithNavController( this, mNavController, mAppBarConfiguration );

        // Handle Navigation item clicks
        // This works with no further action on your part if the menu and destination id’s match.
        NavigationUI.setupWithNavController( navigationView, mNavController );
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d( Lifeograph.TAG, "ActivityLogin.onResume()" );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // very important:
        if( mIabBroadcastReceiver != null ) {
            unregisterReceiver( mIabBroadcastReceiver );
        }
        if( mIabHelper != null ) {
            mIabHelper.disposeWhenFinished();
            mIabHelper = null;
        }

        Log.d( Lifeograph.TAG, "ActivityLogin.onDestroy()" );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( mIabHelper == null )
            return;
        if( !mIabHelper.handleActivityResult( requestCode, resultCode, data ) ) {
            super.onActivityResult( requestCode, resultCode, data );
        }
        else {
            Log.d( Lifeograph.TAG, "onActivityResult handled by IABUtil." );
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );
        //getMenuInflater().inflate( R.menu.menu_login, menu );
        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu( Menu menu ) {
//        super.onPrepareOptionsMenu( menu );
//
//        menu.findItem( R.id.purchase ).setVisible( Lifeograph.getAddFreeNotPurchased() );
//
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController( this, R.id.nav_host_fragment );
        return NavigationUI.navigateUp( navController, mAppBarConfiguration )
               || super.onSupportNavigateUp();
    }

    /*@Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
//            case R.id.new_diary:
//                mFragmentDiaryList.createNewDiary();
//                return true;
//            case R.id.settings:
//                launchSettings();
//                return true;
//            case R.id.about:
//                DialogAbout dialog = new DialogAbout( this );
//                dialog.show();
//                return true;
            case R.id.purchase:
                start_purchase();
                return true;
        }

        return super.onOptionsItemSelected( item );
    }*/

    @Override
    public void
    updateDrawerMenu( int curFragId ) {
        NavigationView navigationView = findViewById( R.id.nav_view );
        MenuItem item_entries = navigationView.getMenu().findItem( R.id.nav_entries );
        MenuItem item_charts = navigationView.getMenu().findItem( R.id.nav_charts );

        if( curFragId == R.id.nav_diaries ) {
            item_entries.setEnabled( false );
            item_charts.setEnabled( false );
        }
        else {
            item_entries.setEnabled( true );
            item_charts.setEnabled( true );
        }
    }

    public void
    showElem( DiaryElement elem ) {
        switch( elem.get_type() ) {
            case ENTRY:
            case CHAPTER:
            {
                FragmentEntry.mEntry = ( Entry ) elem;
//                NavHostFragment navHostFrag = ( NavHostFragment )
//                        getSupportFragmentManager().findFragmentById( R.id.nav_host_fragment );
//                Navigation.findNavController( findViewById( R.id.drawer_layout ) )
                mNavController.navigate( R.id.nav_entry_editor );
                break;
            }
            case THEME:
            {
//                Intent i = new Intent( sContext, ActivityChapterTag.class );
//                i.putExtra( "elem", elem.get_id() );
//                i.putExtra( "type", elem.get_type().i );
//                sContext.startActivity( i );
                break;
            }
            case FILTER:
            case CHART:
                break;
        }
    }

    // IN APP BILLING
    public void start_purchase() {
        try {
            mIabHelper.launchPurchaseFlow( this, SKU_ADFREE, 10001,
                                           mPurchaseFinishedListener, IDs.devPayload );
        }
        catch( IabHelper.IabAsyncInProgressException e ) {
            Log.e( Lifeograph.TAG,
                   "Error launching purchase flow. Another async operation in progress." );
        }
    }

    //@Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d( Lifeograph.TAG, "Received broadcast notification. Querying inventory." );
        try {
            mIabHelper.queryInventoryAsync( mGotInventoryListener );
        }
        catch( IabHelper.IabAsyncInProgressException e ) {
            Log.e( Lifeograph.TAG,
                   "Error querying inventory. Another async operation in progress." );
        }
    }

    // VARIABLES ===================================================================================
    FragmentDiaryList mFragmentDiaryList;
    NavController     mNavController;
    //private ArrayAdapter< String > mAdapterDiaries;
    //private DiaryAdapter mAdapterDiaries; MAYBE LATER
    private boolean m_flag_open_ready = false;
    private int m_password_attempt_no = 0;

    // DIARY ELEMENT ADAPTER CLASS =================================================================
//    class DiaryAdapter extends ArrayAdapter< Diary >
//    {
//        public DiaryAdapter( Context context,
//                             int resource,
//                             int textViewResourceId,
//                             java.util.List< Diary > objects,
//                             LayoutInflater inflater ) {
//            super( context, resource, textViewResourceId, objects );
//            mInflater = inflater;
//        }
//
//        @Override
//        public View getView( int position, View convertView, ViewGroup parent ) {
//            ViewHolder holder;
//            final Diary diary = getItem( position );
//
//            if( convertView == null ) {
//                View view = mInflater.inflate( R.layout.list_item_diary, parent, false );
//                holder = new ViewHolder( view );
//
//                convertView = holder.getView();
//
//                view.setTag( holder );
//            }
//            else {
//                holder = ( ViewHolder ) convertView.getTag();
//            }
//
//            TextView title = holder.getName();
//            title.setText( diary.get_list_str() );
//
//            TextView detail = holder.getDetail();
//            detail.setText( diary.getListStrSecondary() );
//
//            return convertView;
//        }
//
//        private LayoutInflater mInflater;
//
//        // VIEW HOLDER
// =============================================================================
//        private class ViewHolder
//        {
//            private View mRow;
//            private TextView mTitle = null;
//            private TextView mDetail = null;
//            private ImageView mIcon = null;
//
//            public ViewHolder( View row ) {
//                mRow = row;
//            }
//
//            public View getView() {
//                return mRow;
//            }
//
//            public TextView getName() {
//                if( null == mTitle ) {
//                    mTitle = ( TextView ) mRow.findViewById( R.id.title );
//                }
//                return mTitle;
//            }
//
//            public TextView getDetail() {
//                if( null == mDetail ) {
//                    mDetail = ( TextView ) mRow.findViewById( R.id.detail );
//                }
//                return mDetail;
//            }
//
//            public ImageView getIcon() {
//                if( null == mIcon ) {
//                    mIcon = ( ImageView ) mRow.findViewById( R.id.icon );
//                }
//                return mIcon;
//            }
//        }
//    }

    private IabHelper mIabHelper;
    IabBroadcastReceiver mIabBroadcastReceiver;
    static final String SKU_ADFREE = "adfree.one_time";
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener()
            {
                public void onIabPurchaseFinished( IabResult result, Purchase purchase ) {
                    if( mIabHelper == null )
                        return;

                    if( result.isFailure() ) {
                        Lifeograph.showToast( "Purchase failed" );
                    }
                    else if( purchase.getSku().equals( SKU_ADFREE ) &&
                             purchase.getDeveloperPayload().equals( IDs.devPayload ) ) {
                        Log.d( Lifeograph.TAG, "Purchase successful" );
                        Lifeograph.setAdFreePurchased( true );
                    }
                }
            };
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
            new IabHelper.QueryInventoryFinishedListener()
            {
                public void onQueryInventoryFinished( IabResult result, Inventory inventory ) {
                    if( mIabHelper == null )
                        return;

                    if( result.isFailure() ) {
                        Lifeograph.showToast( "Failed to query purchases!" );
                        Lifeograph.setAdFreePurchased( false );
                    }
                    else {
                        Lifeograph.setAdFreePurchased( inventory.hasPurchase( SKU_ADFREE ) );
                    }
                }
            };
}
