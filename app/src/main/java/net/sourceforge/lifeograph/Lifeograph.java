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
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

public class Lifeograph
{
    public static Context context = null;

    public static final String PROGRAM_NAME = "Lifeograph";
    public static final String TAG = "LFO";

    public static final String LANG_INHERIT_DIARY = "d";

    public static String getStr( int i ) {
        if( context == null )
            return "CONTEXT IS NOT READY. SOMETHING IS WRONG!";
        else
            return context.getString( i );
    }

    public static void showConfirmationPrompt( Context ctx, int message, int positiveText,
                                               DialogInterface.OnClickListener posListener,
                                               DialogInterface.OnClickListener negListener ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( ctx );
        builder.setMessage( message )
                .setCancelable( false )
                .setPositiveButton( positiveText, posListener )
                .setNegativeButton( R.string.cancel, negListener );

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void showToast( Context ctx, String message ) {
        Toast.makeText( ctx, message, Toast.LENGTH_LONG ).show();
    }
    public static void showToast( Context ctx, int message ) {
        Toast.makeText( ctx, message, Toast.LENGTH_LONG ).show();
    }

    public static String joinPath( String p1, String p2) {
        File file1 = new File( p1 );
        return new File( file1, p2 ).getPath();
    }

    public static String get_env_lang() {
        return Locale.getDefault().getLanguage();
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
}
