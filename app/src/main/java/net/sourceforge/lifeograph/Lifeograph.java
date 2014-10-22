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
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

public class Lifeograph
{
    // CONSTANTS ===================================================================================
    //public static final String PROGRAM_NAME = "Lifeograph";

    public static final String LANG_INHERIT_DIARY = "d";

    // LIFEOGRAPH APPLICATION-WIDE FUNCTIONALITY ===================================================
    protected static boolean sFlagLogoutOnPause = false;
    protected static boolean sSaveDiaryOnLogout = true;
    protected static boolean sFlagUpdateListOnResume = false;

    enum LoginStatus { LOGGED_OUT, LOGGED_IN, LOGGED_TIME_OUT }
    protected static LoginStatus sLoginStatus = LoginStatus.LOGGED_OUT;

    public static void showElem( DiaryElement elem ) {
        if( elem != null ) {
            switch( elem.get_type() ) {
                case ENTRY: {
                    Intent i = new Intent( sContext, ActivityEntry.class );
                    i.putExtra( "entry", elem.get_date_t() );
                    sFlagLogoutOnPause = false; // not logging out but going deeper
                    sContext.startActivity( i );
                    break;
                }
                case TAG:
                case UNTAGGED:
                case CHAPTER:
                case TOPIC:
                case GROUP: {
                    Intent i = new Intent( sContext, ActivityChapterTag.class );
                    i.putExtra( "elem", elem.get_id() );
                    i.putExtra( "type", elem.get_type().i );
                    sFlagLogoutOnPause = false; // not logging out but going deeper
                    sContext.startActivity( i );
                    break;
                }
            }
        }
        else
            Log.e( TAG, "null element passed to showElem" );
    }

    public static void logout() {
        Log.d( Lifeograph.TAG, "Lifeograph.logout()" );
        // SAVING
        // sync_entry();

        // Diary.diary.m_last_elem = get_cur_elem()->get_id();

        if( sSaveDiaryOnLogout && !Diary.diary.is_read_only() ) {
            if( Diary.diary.write() == Result.SUCCESS ) {
                Lifeograph.showToast( "Diary saved successfully" );
                // TODO: try to save backup
            }
            else
                Lifeograph.showToast( "Cannot write back changes" );
        }
        else
            Log.d( Lifeograph.TAG, "Logged out without saving" );
    }

    // ANDROID & JAVA HELPERS ======================================================================
    public static final String TAG = "LFO";

    protected static Context sContext = null;
    private static boolean sIsLargeScreen = false;

    public static String getStr( int i ) {
        if( sContext == null )
            return "CONTEXT IS NOT READY. SOMETHING IS WRONG!";
        else
            return sContext.getString( i );
    }

    public static void showConfirmationPrompt( int message,
                                               int positiveText,
                                               DialogInterface.OnClickListener posListener,
                                               DialogInterface.OnClickListener negListener ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( sContext );
        builder.setMessage( message )
               .setPositiveButton( positiveText, posListener )
               .setNegativeButton( R.string.cancel, negListener );

        //AlertDialog alert = builder.create();
        builder.show();
    }

    public static void showToast( String message ) {
        Toast.makeText( sContext, message, Toast.LENGTH_LONG ).show();
    }
    public static void showToast( int message ) {
        Toast.makeText( sContext, message, Toast.LENGTH_LONG ).show();
    }

    public static String joinPath( String p1, String p2) {
        File file1 = new File( p1 );
        return new File( file1, p2 ).getPath();
    }

    public static String getEnvLang() {
        return Locale.getDefault().getLanguage();
    }

    public static void initializeConstants( Context ctx ) {
        sContext = ctx;
        sIsLargeScreen = ( ctx.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK ) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isLargeScreen() {
        return sIsLargeScreen;
    }
}
