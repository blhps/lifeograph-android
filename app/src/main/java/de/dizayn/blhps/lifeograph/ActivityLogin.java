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

import java.io.File;
import java.util.ArrayList;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityLogin extends ListActivity {
    protected java.util.List< String > m_paths = new ArrayList< String >();
    protected ArrayAdapter< String > m_adapter_diaries;

    // Called when the activity is first created
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Lifeograph.activityLogin = this;

        if( Diary.diary == null )
            Diary.diary = new Diary();

        setContentView( R.layout.open_diary );
        // setTitle( "Diaries" );

        Lifeograph.context = getApplicationContext();

        m_adapter_diaries =
                new ArrayAdapter< String >( this, android.R.layout.simple_list_item_1,
                                            android.R.id.text1 );
        this.setListAdapter( m_adapter_diaries );

        registerForContextMenu( getListView() );

        populate_diaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        populate_diaries();
        Log.w( "LFO", "onResume ActivityOpenDairy" );
    }

    public void createNewDiary() {
        // ask for name
        DialogNewDiary dialog = new DialogNewDiary( this );
        dialog.show();
        // AlertDialog.Builder alert = new AlertDialog.Builder( this );
        // alert.setTitle( "Create New Diary" );
        // alert.setMessage( "Enter name of the diay file:" );

        // Set an EditText view to get user input
        // final EditText input = new EditText( this );
        // input.setInputType( InputType.TYPE_TEXT_FLAG_MULTI_LINE );
        // alert.setView( input );
        //
        // alert.setPositiveButton( "Ok", new DialogInterface.OnClickListener() {
        // public void onClick( DialogInterface dialog, int whichButton ) {
        // String name = new String( input.getText().toString() );
        //
        // if( name.length() > 0 ) {
        // diary.init_new( "/mnt/sdcard/Diaries/" + name );
        // Intent i = new Intent( ActivityLogin.this, ActivityDiary.class );
        // startActivityForResult( i, 0 );
        // }
        // }
        // } );
        // alert.setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
        // public void onClick( DialogInterface dialog, int which ) {
        // diary.init_new( "" );
        // }
        // } );
        // alert.show();
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
            case R.id.about:
                DialogAbout dialog = new DialogAbout( this );
                dialog.show();
                return true;
            case R.id.new_diary:
                createNewDiary();
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
    protected void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );

        boolean flag_open_ready = false;

        Diary.diary.clear();

        switch( Diary.diary.set_path( m_paths.get( pos ), Diary.SetPathType.NORMAL ) ) {
            case SUCCESS:
                flag_open_ready = true;
                break;
            case FILE_NOT_FOUND:
                Toast.makeText( this, "File is not found", Toast.LENGTH_LONG ).show();
                break;
            case FILE_NOT_READABLE:
                Toast.makeText( this, "File is not readable", Toast.LENGTH_LONG ).show();
                break;
            case FILE_LOCKED:
                Toast.makeText( this, "File is locked", Toast.LENGTH_LONG ).show();
                break;
            default:
                Toast.makeText( this, "Failed to open the diary", Toast.LENGTH_SHORT ).show();
                break;
        }

        if( flag_open_ready ) {
            flag_open_ready = false;
            switch( Diary.diary.read_header() ) {
                case SUCCESS:
                    flag_open_ready = true;
                    break;
                case INCOMPATIBLE_FILE:
                    // TODO: show info
                    break;
                case CORRUPT_FILE:
                    Toast.makeText( this, "Corrupt File", Toast.LENGTH_SHORT ).show();
                    break;
                default:
                    // TODO: show info
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
                    Diary.diary.clear(); // clear partially read content if any
                    break;
                default:
                    break;
            }
        }
    }

    protected boolean mExternalStorageAvailable = false;
    protected boolean mExternalStorageWriteable = false;

    protected void populate_diaries() {
        m_adapter_diaries.clear();
        String state = Environment.getExternalStorageState();

        if( Environment.MEDIA_MOUNTED.equals( state ) ) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else {
            // Something else is wrong. It may be one of many other states, but
            // all we need to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if( mExternalStorageAvailable && mExternalStorageWriteable ) {
            File dir = new File( Environment.getExternalStorageDirectory(), "Diaries/" );
            Log.i( "LFO", dir.getPath() );
            if( !dir.exists() ) {
                dir.mkdir();
            }
            else {
                File[] dirs = dir.listFiles();
                for( File ff : dirs ) {
                    if( !ff.isDirectory() ) {
                        m_adapter_diaries.add( ff.getName() );
                        m_paths.add( ff.getPath() );
                    }
                }
            }
        }
        else
            Toast.makeText( this, R.string.storage_not_available, Toast.LENGTH_LONG ).show();
    }

    // ABOUT DIALOG ================================================================================
    public class DialogAbout extends Dialog {
        protected Button buttonClose;

        public DialogAbout( Context context ) {
            super( context );
        }

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );

            setContentView( R.layout.dialog_about );
            // setTitle( "About..." );
            setCancelable( true );

            buttonClose = ( Button ) findViewById( R.id.buttonAboutClose );
            buttonClose.setOnClickListener( new View.OnClickListener() {
                public void onClick( View v ) {
                    dismiss();
                }
            } );

            // TODO:
            // textView.setText( getPackageManager().getPackageInfo(getPackageName(),
            // 0).versionName );

        }
    }

    // CREATE DIARY DIALOG =========================================================================
    public class DialogNewDiary extends Dialog {
        protected EditText eTextName;
        protected Button buttonCreate, buttonCancel;

        public DialogNewDiary( Context context ) {
            super( context );
        }

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );

            setContentView( R.layout.dialog_new_diary );
            setTitle( getResources().getText( R.string.new_diary_name ) );
            setCancelable( true );

            buttonCreate = ( Button ) findViewById( R.id.buttonCreateDiary );
            buttonCreate.setOnClickListener( new View.OnClickListener() {
                public void onClick( View v ) {
                    create_diary();
                }
            } );

            buttonCancel = ( Button ) findViewById( R.id.buttonCancelNewDiary );
            buttonCancel.setOnClickListener( new View.OnClickListener() {
                public void onClick( View v ) {
                    dismiss();
                }
            } );

            eTextName = ( EditText ) findViewById( R.id.editTextNewDiary );
            eTextName.addTextChangedListener( new TextWatcher() {
                public void afterTextChanged( Editable s ) {
                }

                public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                }

                public void onTextChanged( CharSequence s, int start, int before, int count ) {
                    buttonCreate.setEnabled( s.length() > 0 );
                }
            } );
            eTextName.setOnEditorActionListener( new TextView.OnEditorActionListener() {
                public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                    if( v.getText().length() > 0 ) {
                        create_diary();
                        return true;
                    }
                    return false;
                }
            } );
        }

        private void create_diary() {
            String name = new String( eTextName.getText().toString() );
            dismiss();

            if( name.length() > 0 )
            {
                if( Diary.diary.init_new( "/mnt/sdcard/Diaries/" + name ) == Result.SUCCESS )
                {
                    Intent i = new Intent( ActivityLogin.this, ActivityDiary.class );
                    startActivityForResult( i, 0 );
                }
                // TODO else inform the user about the problem
            }
        }
    }

}
