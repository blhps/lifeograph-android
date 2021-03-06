/* *********************************************************************************

    Copyright (C) 2012-2020 Ahmet Öztürk (aoz_2@yahoo.com)

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class Lifeograph
{
    // CONSTANTS ===================================================================================
    //public static final String PROGRAM_NAME = "Lifeograph";
    public static final String LIFEOGRAPH_RELEASE_CODENAME =
            "one can enter the same data stream twice";

    static final String LANG_INHERIT_DIARY = "d";

    // LIFEOGRAPH APPLICATION-WIDE FUNCTIONALITY ===================================================
    private enum PurchaseStatus { PS_UNKNOWN, PURCHASED, NOT_PURCHASED }
    private static PurchaseStatus mAdFreePurchased = PurchaseStatus.PS_UNKNOWN;

    static void setAdFreePurchased( boolean purchased ) {
        mAdFreePurchased = purchased ? PurchaseStatus.PURCHASED : PurchaseStatus.NOT_PURCHASED;
    }

    static boolean getAddFreeNotPurchased() {
        return( mAdFreePurchased == PurchaseStatus.NOT_PURCHASED );
    }

    interface DiaryEditor{
        void enableEditing();
        Context getContext();
    }

    static void showElem( DiaryElement elem ) {
        if( elem != null ) {
            switch( elem.get_type() ) {
                case ENTRY: {
                    Intent i = new Intent( sContext, ActivityEntry.class );
                    i.putExtra( "entry", elem.get_date_t() );
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
                    sContext.startActivity( i );
                    break;
                }
            }
        }
        else
            Log.e( TAG, "null element passed to showElem" );
    }

    public static void enableEditing( DiaryEditor editor ) {
        // HANDLE OLD DIARY
        if( Diary.diary.is_old() ) {
            Lifeograph.showConfirmationPrompt(
                    editor.getContext(),
                    R.string.diary_upgrade_confirm,
                    R.string.upgrade_diary,
                    ( a, b ) -> enableEditing2( editor ) );
            return;
        }

        enableEditing2( editor );
    }
    private static void enableEditing2( DiaryEditor editor ) {
        if( !Diary.diary.can_enter_edit_mode() ) return;
        if( Diary.diary.enable_editing() != Result.SUCCESS ) return;

        editor.enableEditing();
    }
    // ANDROID & JAVA HELPERS ======================================================================
    public static final String TAG = "LFO";

    static Context sContext = null;
    private static float sScreenWidth;
    private static float sScreenHeight;
    static float sDPIX;
    static float sDPIY;
    static final float MIN_HEIGHT_FOR_NO_EXTRACT_UI = 6.0f;

    public static String getStr( int i ) {
        if( sContext == null )
            return "CONTEXT IS NOT READY. SOMETHING IS WRONG!";
        else
            return sContext.getString( i );
    }

    static void showConfirmationPrompt( Context context,
                                        int message,
                                        int positiveText,
                                        DialogInterface.OnClickListener posListener ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( context );
        builder.setMessage( message )
               .setPositiveButton( positiveText, posListener )
               .setNegativeButton( R.string.cancel, null );

        //AlertDialog alert = builder.create();
        builder.show();
    }
    static void showConfirmationPrompt( Context context,
                                        int message,
                                        int positiveText,
                                        DialogInterface.OnClickListener posListener,
                                        int negativeText,
                                        DialogInterface.OnClickListener negListener ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( context );
        builder.setMessage( message )
               .setPositiveButton( positiveText, posListener )
               .setNegativeButton( negativeText, negListener );

        builder.show();
    }

    static void showToast( String message ) {
        Toast.makeText( sContext, message, Toast.LENGTH_LONG ).show();
    }
    static void showToast( int message ) {
        Toast.makeText( sContext, message, Toast.LENGTH_LONG ).show();
    }

    static String joinPath( String p1, String p2) {
        File file1 = new File( p1 );
        return new File( file1, p2 ).getPath();
    }

    public static void copyFile( File src, File dst ) throws IOException {
        try( InputStream in = new FileInputStream( src ) ) {
            try( OutputStream out = new FileOutputStream( dst ) ) {
                // Transfer bytes from in to out
                byte[] buf = new byte[ 1024 ];
                int len;
                while( ( len = in.read( buf ) ) > 0 ) {
                    out.write( buf, 0, len );
                }
            }
        }
    }

//    public static String getEnvLang() {
//        return Locale.getDefault().getLanguage();
//    }

//    public static void initializeConstants( Context ctx ) {
//        sContext = ctx;
//        sIsLargeScreen = ( ctx.getResources().getConfiguration().screenLayout
//                & Configuration.SCREENLAYOUT_SIZE_MASK ) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
//    }

    static void updateScreenSizes( Context context ) {
        WindowManager wm = ( WindowManager ) context.getSystemService( Context.WINDOW_SERVICE );
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics( metrics );
        sScreenWidth = metrics.widthPixels / metrics.xdpi;
        sScreenHeight = metrics.heightPixels / metrics.ydpi;
        sDPIX = metrics.xdpi;
        sDPIY = metrics.ydpi;
        Log.d( TAG, "Updated the sizes: " + sScreenWidth + "x" + sScreenHeight );
    }

    static float getScreenShortEdge() {
        return( Math.min( sScreenHeight, sScreenWidth ) );
    }
    static float getScreenWidth() {
        return sScreenWidth;
    }
    static float getScreenHeight() {
        return sScreenHeight;
    }
//    public static boolean isLargeScreen() {
//        return sIsLargeScreen;
//    }

//    static boolean isExternalStorageWritable() {
//        String state = Environment.getExternalStorageState();
//        return Environment.MEDIA_MOUNTED.equals( state );
//    }

}
