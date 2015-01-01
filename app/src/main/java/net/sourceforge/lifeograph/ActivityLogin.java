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

import java.io.File;
import java.util.ArrayList;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.sourceforge.lifeograph.inappbilling.util.IabHelper;
import net.sourceforge.lifeograph.inappbilling.util.IabResult;
import net.sourceforge.lifeograph.inappbilling.util.Inventory;
import net.sourceforge.lifeograph.inappbilling.util.Purchase;

public class ActivityLogin extends ListActivity
        implements DialogInquireText.InquireListener, DialogPassword.Listener
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Lifeograph.sContext = this;
        Lifeograph.updateScreenSizes();
        Lifeograph.sNumberOfDiaryEditingActivities++;

        if( Diary.diary == null )
            Diary.diary = new Diary();

        // IN APP BILLING
        mIabHelper = new IabHelper( this, IDs.base64EncodedPublicKey );

        mIabHelper.startSetup( new IabHelper.OnIabSetupFinishedListener()
        {
            public void onIabSetupFinished( IabResult result ) {
                if( !result.isSuccess() ) {
                    Log.d( Lifeograph.TAG, "IAB setup failed: " + result );
                    Lifeograph.setAdFreePurchased( false );
                }
                else {
                    Log.d( Lifeograph.TAG, "IAB setup successful" );
                    mIabHelper.queryInventoryAsync( mGotInventoryListener );
                }
            }
        } );

        // PREFERENCES
        PreferenceManager.setDefaultValues( getApplicationContext(), R.xml.pref_general, false );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext() );
        sDiaryPath = prefs.getString(
                Lifeograph.getStr( R.string.pref_DIARY_PATH_key ), "N/A" );
        Date.s_format_order = prefs.getString(
                Lifeograph.getStr( R.string.pref_DATE_FORMAT_ORDER_key ), "N/A" );
        Date.s_format_separator = prefs.getString(
                Lifeograph.getStr( R.string.pref_DATE_FORMAT_SEPARATOR_key ), "N/A" );

        setContentView( R.layout.login );

        mAdapterDiaries = new ArrayAdapter< String >( this,
                                                      R.layout.list_item_diary,
                                                      R.id.title );
        this.setListAdapter( mAdapterDiaries );

        registerForContextMenu( getListView() );

        populate_diaries();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Lifeograph.sContext = this;

        if( Lifeograph.sLoginStatus == Lifeograph.LoginStatus.LOGGED_IN ) {
            Lifeograph.logout();

            Lifeograph.sLoginStatus = Lifeograph.LoginStatus.LOGGED_OUT;
        }

        populate_diaries(); // this also helps with the changes in the diary path
        Log.d( Lifeograph.TAG, "ActivityLogin.onResume()" );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mIabHelper != null ) {
            mIabHelper.dispose();
            mIabHelper = null;
        }

        Log.d( Lifeograph.TAG, "ActivityLogin.onDestroy()" );

        Lifeograph.handleDiaryEditingActivityDestroyed();
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( !mIabHelper.handleActivityResult( requestCode, resultCode, data ) ) {
            super.onActivityResult( requestCode, resultCode, data );
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_login, menu );

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        menu.findItem( R.id.purchase ).setVisible( Lifeograph.getAddFreeNotPurchased() );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.new_diary:
                createNewDiary();
                return true;
            case R.id.settings:
                launchSettings();
                return true;
            case R.id.about:
                DialogAbout dialog = new DialogAbout( this );
                dialog.show();
                return true;
            case R.id.purchase:
                start_purchase();
                return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );

        boolean flag_open_ready = false;

        Diary.diary.clear();

        switch( Diary.diary.set_path( mPaths.get( pos ), Diary.SetPathType.NORMAL ) ) {
            case SUCCESS:
                flag_open_ready = true;
                break;
            case FILE_NOT_FOUND:
                Lifeograph.showToast( "File is not found" );
                break;
            case FILE_NOT_READABLE:
                Lifeograph.showToast( "File is not readable" );
                break;
            case FILE_LOCKED:
                Lifeograph.showToast( "File is locked" );
                break;
            default:
                Lifeograph.showToast( "Failed to open the diary" );
                break;
        }

        if( flag_open_ready ) {
            flag_open_ready = false;
            switch( Diary.diary.read_header( getAssets() ) ) {
                case SUCCESS:
                    flag_open_ready = true;
                    break;
                case INCOMPATIBLE_FILE:
                    Lifeograph.showToast( "Incompatible diary version" );
                    break;
                case CORRUPT_FILE:
                    Lifeograph.showToast( "Corrupt file" );
                    break;
                default:
                    Log.e( Lifeograph.TAG, "Unprocessed return value from read_header" );
                    break;
            }
        }

        /*
         * TODO: if( flag_open_ready && ( ! flag_encrypted ) && m_flag_open_directly ) {
         * handle_button_opendb_clicked(); return; }
         */

        if( flag_open_ready ) {
            if( Diary.diary.is_encrypted() )
                askPassword();
            else
                readBody();
        }
    }

    private void askPassword() {
        DialogPassword dlg = new DialogPassword( this,
                                                 Diary.diary,
                                                 DialogPassword.DPAction.DPA_LOGIN,
                                                 this );
        dlg.show();
    }

    private void readBody() {
        switch( Diary.diary.read_body() ) {
            case SUCCESS:
                Intent i = new Intent( this, ActivityDiary.class );
                startActivity( i );
                break;
            case WRONG_PASSWORD:
                Lifeograph.showToast( R.string.wrong_password );
                break;
            case CORRUPT_FILE:
                Lifeograph.showToast( "Corrupt file" );
                break;
            default:
                break;
        }
    }

    File getDiariesDir() {
        return new File( Environment.getExternalStorageDirectory(), sDiaryPath );
    }

    // InquireListener INTERFACE METHODS
    public void onInquireAction( int id, String text ) {
        switch( id ) {
            case R.string.create_diary:
                if( Diary.diary.init_new( Lifeograph.joinPath( getDiariesDir().getPath(), text ) )
                        == Result.SUCCESS ) {
                    Intent i = new Intent( ActivityLogin.this, ActivityDiary.class );
                    startActivity( i );
                }
                // TODO else inform the user about the problem
                break;
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        switch( id ) {
            case R.string.create_diary:
                File fp = new File( getDiariesDir().getPath(), s );
                return( !fp.exists() );
        }
        return true;
    }

    // DialogPassword INTERFACE METHODS
    public void onDPAction( DialogPassword.DPAction action ) {
        if( action == DialogPassword.DPAction.DPA_LOGIN )
            readBody();
    }

    void createNewDiary() {
        // ask for name
        DialogInquireText dlg = new DialogInquireText( this, R.string.create_diary,
                Lifeograph.getStr( R.string.new_diary ), R.string.create, this );
        dlg.show();
    }

    void populate_diaries() {
        boolean externalStorageAvailable;
        boolean externalStorageWritable;

        mAdapterDiaries.clear();
        mPaths.clear();

        String state = Environment.getExternalStorageState();

        if( Environment.MEDIA_MOUNTED.equals( state ) ) {
            // We can read and write the media
            externalStorageAvailable = externalStorageWritable = true;
        }
        else if( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            // We can only read the media
            externalStorageAvailable = true;
            externalStorageWritable = false;
        }
        else {
            // Something else is wrong. It may be one of many other states, but
            // all we need to know is we can neither read nor write
            externalStorageAvailable = externalStorageWritable = false;
        }

        if( externalStorageAvailable && externalStorageWritable ) {
            File dir = getDiariesDir();
            Log.d( Lifeograph.TAG, dir.getPath() );
            if( !dir.exists() ) {
                if( !dir.mkdirs() )
                    Lifeograph.showToast( "Failed to create the diary folder" );
            }
            else {
                File[] dirs = dir.listFiles();
                for( File ff : dirs ) {
                    if( !ff.isDirectory() ) {
                        mAdapterDiaries.add( ff.getName() );
                        mPaths.add( ff.getPath() );
                    }
                }
            }
        }
        else
            Lifeograph.showToast( R.string.storage_not_available );

        mPaths.add( Diary.sExampleDiaryPath );
        mAdapterDiaries.add( Diary.sExampleDiaryName );
    }

    void launchSettings() {
        Intent i = new Intent( this, ActivitySettings.class );
        startActivity( i );
    }

    // IN APP BILLING
    public void start_purchase() {
        mIabHelper.launchPurchaseFlow( this, SKU_ADDFREE, 10001,
                                       mPurchaseFinishedListener, IDs.devPayload );
    }

    // VARIABLES
    public static String sDiaryPath;
    private java.util.List< String > mPaths = new ArrayList< String >();
    private ArrayAdapter< String > mAdapterDiaries;
    //private DiaryAdapter mAdapterDiaries; MAYBE LATER

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
//        // VIEW HOLDER =============================================================================
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
    static final String SKU_ADDFREE = "lifeograph.addfree.purchased";
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener()
            {
                public void onIabPurchaseFinished( IabResult result, Purchase purchase ) {
                    if( result.isFailure() ) {
                        Lifeograph.showToast( "Purchase failed" );
                    }
                    else if( purchase.getSku().equals( SKU_ADDFREE ) &&
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
                    if( result.isFailure() ) {
                        Lifeograph.showToast( "Failed to query purchases!" );
                        Lifeograph.setAdFreePurchased( false );
                    }
                    else {
                        Lifeograph.setAdFreePurchased( inventory.hasPurchase( SKU_ADDFREE ) );
                    }
                }
            };

    // ABOUT DIALOG ================================================================================
    public class DialogAbout extends Dialog
    {
        public DialogAbout( Context context ) {
            super( context );
        }

        @Override
        protected void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );

            setContentView( R.layout.dialog_about );
            setTitle( R.string.program_name );
            setCancelable( true );

            TextView tv = ( TextView ) findViewById( R.id.textViewWebsite );
            tv.setMovementMethod( LinkMovementMethod.getInstance() );

            tv = ( TextView ) findViewById( R.id.textViewVersion );
            tv.setText( BuildConfig.VERSION_NAME );
        }
    }
}
