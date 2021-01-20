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

import net.sourceforge.lifeograph.helpers.Result;

import androidx.annotation.NonNull;

public class Lifeograph
{
    // CONSTANTS ===================================================================================
    //public static final String PROGRAM_NAME = "Lifeograph";
    public static final String LIFEOGRAPH_RELEASE_CODENAME =
            "one can enter the same data stream twice";

    static final String LANG_INHERIT_DIARY = "d";

    static final double MI_TO_KM_RATIO = 1.609344;

    // LIFEOGRAPH APPLICATION-WIDE FUNCTIONALITY ===================================================
    private enum PurchaseStatus { PS_UNKNOWN, PURCHASED, NOT_PURCHASED }
    private static PurchaseStatus mAdFreePurchased = PurchaseStatus.PS_UNKNOWN;

    static void setAdFreePurchased( boolean purchased ) {
        mAdFreePurchased = purchased ? PurchaseStatus.PURCHASED : PurchaseStatus.NOT_PURCHASED;
    }

    static boolean getAddFreeNotPurchased() {
        return( mAdFreePurchased == PurchaseStatus.NOT_PURCHASED );
    }

    static int
    get_todo_icon( int es ) {
        switch( es ) {
            case DiaryElement.ES_PROGRESSED:
                return R.mipmap.ic_todo_progressed;
            case DiaryElement.ES_DONE:
                return R.mipmap.ic_todo_done;
            case DiaryElement.ES_CANCELED:
                return R.mipmap.ic_todo_canceled;
            case DiaryElement.ES_TODO:
            default:
                return R.mipmap.ic_todo_open;
        }
    }

    interface DiaryEditor{
        void enableEditing();
        Context getContext();
    }

    static void
    showElem( @NonNull DiaryElement elem ) {
        switch( elem.get_type() ) {
            case ENTRY:
            case CHAPTER:
            {
                Intent i = new Intent( sContext, ActivityEntry.class );
                i.putExtra( "entry", elem.get_date_t() );
                sContext.startActivity( i );
                break;
            }
//                case THEME:
//                {
//                    Intent i = new Intent( sContext, ActivityChapterTag.class );
//                    i.putExtra( "elem", elem.get_id() );
//                    i.putExtra( "type", elem.get_type().i );
//                    sContext.startActivity( i );
//                    break;
//                }
        }
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

    static boolean sOptImperialUnits = false;

    // ANDROID & JAVA HELPERS ======================================================================
    public static final String TAG = "LFO";

    static Context sContext = null;
    private static float sScreenWidth;
    private static float sScreenHeight;
    static float         sDPIX;
    static float         sDPIY;
    static final float   MIN_HEIGHT_FOR_NO_EXTRACT_UI = 6.0f;

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

    static class MutableBool
    {
        public MutableBool()             { v = false; }
        public MutableBool( boolean v0 ) { v = v0; }
        public boolean v;
    }
    static class MutableInt
    {
        public MutableInt()         { v = 0; }
        public MutableInt( int v0 ) { v = v0; }
        public int v;
    }
    static class MutableString
    {
        public MutableString()            { v = ""; }
        public MutableString( String v0 ) { v = v0; }
        public String v;
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

    static boolean
    get_line( String source, MutableInt o, MutableString line ) {
        if( source.isEmpty() || o.v >= source.length() )
            return false;

        int o_end = source.indexOf( '\n', o.v );

        if( o_end == -1 ) {
            line.v = source.substring( o.v );
            o.v = source.length();
        }
        else {
            line.v = source.substring( o.v, o_end );
            o.v = ( o_end + 1 );
        }

        return true;
    }

    static long
    get_long( @NonNull String line, @NonNull MutableInt i ) {
        long result = 0;

        for( ; i.v < line.length() && line.charAt( i.v ) >= '0' && line.charAt( i.v ) <= '9';
             i.v++ )
        result = ( ( result * 10 ) + line.charAt( i.v ) - '0' );

        return result;
    }

    static int
    get_int( @NonNull String line, @NonNull MutableInt i ) {
        int result = 0;

        for( ; i.v < line.length() && line.charAt( i.v ) >= '0' && line.charAt( i.v ) <= '9';
             i.v++ )
        result = ( ( result * 10 ) + line.charAt( i.v ) - '0' );

        return result;
    }

    static double
    get_double( @NonNull String text ) {
        //NOTE: this implementation may be a little bit more forgiving than good for health
        double  value = 0.0;
        //char lf{ '=' }; // =, \, #, $(unit)
        int     divider = 0;
        boolean negative = false;
        char    c;

        for( int i = 0; i < text.length(); i++ ) {
            c = text.charAt( i );
            switch( c ) {
                case ',':
                case '.':
                    if( divider == 0 ) // note that if divider
                        divider = 1;
                    break;
                case '-':
                    negative = true;
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    value *= 10;
                    value += ( c - '0' );
                    if( divider != 0 )
                        divider *= 10;
                    break;
                default:
                    break;
            }
        }

        if( divider > 1 )
            value /= divider;
        if( negative )
            value *= -1;

        return value;
    }
    static double
    get_double( @NonNull String line, @NonNull MutableInt i ) {
        double  value      = 0.0;
        int     divider    = 0;
        boolean negative   = false;
        boolean f_continue = true;
        char    c;

        for( ; i.v < line.length() && f_continue; i.v++ )
        {
            c = line.charAt( i.v );
            switch( c ) {
                case ',':
                case '.':
                    if( divider == 0 ) // note that if divider
                        divider = 1;
                    break;
                case '-':
                    negative = true;
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                value *= 10;
                value += ( c - '0' );
                if( divider != 0 )
                    divider *= 10;
                break;
                default:
                    i.v--;
                    f_continue = false; // end loop
                    break;
            }
        }

        if( divider > 1 )
            value /= divider;
        if( negative )
            value *= -1;

        return value;
    }
}
