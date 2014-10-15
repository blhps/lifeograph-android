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

public class ActivityLogin extends ListActivity implements DialogInquireText.InquireListener
{
    public static String sDiaryPath;
    private java.util.List< String > m_paths = new ArrayList< String >();
    private ArrayAdapter< String > mAdapterDiaries;

    // Called when the activity is first created
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Lifeograph.context = getApplicationContext();   // must be first

        if( Diary.diary == null )
            Diary.diary = new Diary();

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

        mAdapterDiaries = new ArrayAdapter< String >( this, android.R.layout.simple_list_item_1,
                                                      android.R.id.text1 );
        this.setListAdapter( mAdapterDiaries );

        registerForContextMenu( getListView() );

        populate_diaries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populate_diaries(); // this also helps with the changes in the diary path
        Log.d( Lifeograph.TAG, "onResume - ActivityLogin" );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_login, menu );

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
        }

        return super.onOptionsItemSelected( item );
    }

    /*
     * @Override public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo
     * menuInfo ) { super.onCreateContextMenu( menu, v, menuInfo ); menu.add(0,
     * ID_BROWSE_DIARY, 0, R.string.browse_diary ); }
     */

    @Override
    public void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );

        boolean flag_open_ready = false;

        Diary.diary.clear();

        switch( Diary.diary.set_path( m_paths.get( pos ), Diary.SetPathType.NORMAL ) ) {
            case SUCCESS:
                flag_open_ready = true;
                break;
            case FILE_NOT_FOUND:
                Lifeograph.showToast( this, "File is not found" );
                break;
            case FILE_NOT_READABLE:
                Lifeograph.showToast( this, "File is not readable" );
                break;
            case FILE_LOCKED:
                Lifeograph.showToast( this, "File is locked" );
                break;
            default:
                Lifeograph.showToast( this, "Failed to open the diary" );
                break;
        }

        if( flag_open_ready ) {
            flag_open_ready = false;
            switch( Diary.diary.read_header( getAssets() ) ) {
                case SUCCESS:
                    flag_open_ready = true;
                    break;
                case INCOMPATIBLE_FILE:
                    Lifeograph.showToast( this, "Incompatible diary version" );
                    break;
                case CORRUPT_FILE:
                    Lifeograph.showToast( this, "Corrupt file" );
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
            if( Diary.diary.is_encrypted() ) {
                String passphrase = ""/* ask_passphrase() */;
                Diary.diary.set_passphrase( passphrase );
            }

            switch( Diary.diary.read_body() ) {
                case SUCCESS:
                    Intent i = new Intent( this, ActivityDiary.class );
                    startActivityForResult( i, 0 );
                    break;
                case WRONG_PASSWORD:
                    // TODO: show info
                    break;
                case CORRUPT_FILE:
                    // TODO: show info
                    break;
                default:
                    break;
            }
        }
    }

    File getDiariesDir() {
        return new File( Environment.getExternalStorageDirectory(), sDiaryPath );
    }

    // InquireListener method
    public void onInquireAction( int id, String text ) {
        switch( id ) {
            case R.string.create_diary:
                if( Diary.diary.init_new( Lifeograph.joinPath( getDiariesDir().getPath(), text ) )
                        == Result.SUCCESS ) {
                    Intent i = new Intent( ActivityLogin.this, ActivityDiary.class );
                    startActivityForResult( i, 0 );
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
        m_paths.clear();

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
                    Lifeograph.showToast( this, "Failed to create the diary folder" );
            }
            else {
                File[] dirs = dir.listFiles();
                for( File ff : dirs ) {
                    if( !ff.isDirectory() ) {
                        mAdapterDiaries.add( ff.getName() );
                        m_paths.add( ff.getPath() );
                    }
                }
            }
        }
        else
            Lifeograph.showToast( this, R.string.storage_not_available );

        m_paths.add( Diary.sExampleDiaryPath );
        mAdapterDiaries.add( Diary.sExampleDiaryName );
    }

    void launchSettings() {
        Intent i = new Intent( this, ActivitySettings.class );
        startActivity( i );
    }

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
