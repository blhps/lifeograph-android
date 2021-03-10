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

package net.sourceforge.lifeograph

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import net.sourceforge.lifeograph.helpers.Result
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class Lifeograph : Application() {
    override fun onCreate() {
        mInstance = this
        super.onCreate()
    }

    class MutableBool {
        constructor() {
            v = false
        }

        constructor(v0: Boolean) {
            v = v0
        }

        var v: Boolean
    }

    class MutableInt {
        constructor() {
            v = 0
        }

        constructor(v0: Int) {
            v = v0
        }

        @JvmField
        var v: Int
    }

    class MutableString {
        constructor() {
            v = ""
        }

        constructor(v0: String) {
            v = v0
        }

        @JvmField
        var v: String
    }

    companion object {
        // CONSTANTS ===============================================================================
        //public static final String PROGRAM_NAME = "Lifeograph";
        const val LIFEOGRAPH_RELEASE_CODENAME = "the spring of goliath"
        const val LANG_INHERIT_DIARY          = "d"
        const val MI_TO_KM_RATIO              = 1.609344
        lateinit var mActivityMain: ActivityMain
        @JvmStatic
        val context: Context
            get() = mInstance!!

        fun getActionBar(): ActionBar {
            return mActivityMain.mActionBar!!
        }

        // VARIABLES ===============================================================================
        @JvmField
        var sOptImperialUnits = false

        // LIFEOGRAPH APPLICATION-WIDE FUNCTIONALITY ===============================================
        @JvmStatic
        fun getTodoIcon(es: Int): Int {
            return when(es) {
                DiaryElement.ES_PROGRESSED -> R.drawable.ic_todo_progressed
                DiaryElement.ES_DONE -> R.drawable.ic_todo_done
                DiaryElement.ES_CANCELED -> R.drawable.ic_todo_canceled
                //DiaryElement.ES_TODO -> R.drawable.ic_todo_open
                else -> R.drawable.ic_todo_open
            }
        }

        @JvmStatic
        fun goToToday() {
            if(BuildConfig.DEBUG && !Diary.d.is_open) {
                throw AssertionError("Assertion failed")
            }
            var entry = Diary.d._entry_today
            if(entry == null) // add new entry if no entry exists on selected date
                entry = Diary.d.add_today()
            showElem(entry)
        }

        @JvmStatic
        fun addEntry(date0: Long, text: String?) {
            var date = date0
            if(BuildConfig.DEBUG && !Diary.d.is_in_edit_mode) {
                throw AssertionError("Assertion failed")
            }
            if(!Date.is_ordinal(date) && Date.get_order_3rd(date) == 0) date++ // fix order
            val entry = Diary.d.create_entry(date, text)
            if(entry != null) showElem(entry)
        }

        @JvmStatic
        fun duplicateEntry(entrySrc: Entry, title: String): Entry {
            val entry = Diary.d.create_entry(entrySrc._date_t, title)
            for((i, para) in entrySrc.m_paragraphs.withIndex())
                if(i > 0)
                    entry.add_paragraph(para.m_text)

            return entry
        }

        @JvmStatic
        fun showElem(elem: DiaryElement) {
            if(BuildConfig.DEBUG && !Diary.d.is_open) {
                throw AssertionError("Assertion failed")
            }
            mActivityMain.showElem(elem)
        }

        fun enableEditing(editor: FragmentDiaryEditor) {
            // HANDLE OLD DIARY
            if(Diary.d.is_old) {
                showConfirmationPrompt(
                        editor.getContext(),
                        R.string.diary_upgrade_confirm,
                        R.string.upgrade_diary
                                      ) { _: DialogInterface?, _: Int -> enableEditing2(editor) }
                return
            }
            enableEditing2(editor)
        }

        private fun enableEditing2(editor: FragmentDiaryEditor) {
            if(!Diary.d.can_enter_edit_mode()) return
            if(Diary.d.enable_editing() != Result.SUCCESS) return
            editor.enableEditing()
        }

        fun logoutWithoutSaving(view: View) {
            if(Diary.d.is_open) {
                showConfirmationPrompt(view.context,
                                       R.string.logoutwosaving_confirm,
                                       R.string.logoutwosaving
                                      ) { _: DialogInterface?, _: Int ->
                    // unlike desktop version Android version
                    // does not back up changes
                    Diary.d.setSavingEnabled(false)
                    Navigation.findNavController(view)
                            .navigate(R.id.nav_diaries)
                }
            }
        }

        // ANDROID & JAVA HELPERS ======================================================================
        const val TAG = "LFO"
        private var mInstance: Lifeograph? = null
        @JvmField
        var screenWidth = 0f
        @JvmField
        var screenHeight = 0f
        @JvmField
        var sDPIX = 0f
        private var sDPIY = 0f
        const val MIN_HEIGHT_FOR_NO_EXTRACT_UI = 6.0f

        @JvmStatic
        fun getStr(i: Int): String {
            if(BuildConfig.DEBUG && mInstance == null) {
                error("Assertion failed")
            }
            return mInstance!!.getString(i)
        }

        @JvmStatic
        fun showConfirmationPrompt(context: Context?,
                                   message: Int,
                                   positiveText: Int,
                                   posListener: DialogInterface.OnClickListener?) {
            val builder = AlertDialog.Builder(context, R.style.LifeoAlertDlgTheme)
            builder.setMessage(message)
                    .setPositiveButton(positiveText, posListener)
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        @JvmStatic
        fun showConfirmationPrompt(context: Context?,
                                   message: Int,
                                   positiveText: Int,
                                   posListener: DialogInterface.OnClickListener?,
                                   negativeText: Int,
                                   negListener: DialogInterface.OnClickListener?) {
            val builder = AlertDialog.Builder(context, R.style.LifeoAlertDlgTheme)
            builder.setMessage(message)
                    .setPositiveButton(positiveText, posListener)
                    .setNegativeButton(negativeText, negListener)
                    .show()
        }

        fun showSnack(view: View?, message: String?) {
            Snackbar.make(view!!, message!!, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        @JvmStatic
        fun showToast(message: String?) {
            Toast.makeText(mInstance, message, Toast.LENGTH_LONG).show()
        }

        @JvmStatic
        fun showToast(message: Int) {
            Toast.makeText(mInstance, message, Toast.LENGTH_LONG).show()
        }

        @JvmStatic
        fun joinPath(p1: String, p2: String): String {
            val file1 = File(p1)
            return File(file1, p2).path
        }

        @JvmStatic
        @Throws(IOException::class)
        fun copyFile(src: File?, dst: File?) {
            FileInputStream(src).use { `in` ->
                FileOutputStream(dst).use { out ->
                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len: Int
                    while(`in`.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
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
        fun updateScreenSizes(context: Context) {
            val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels / metrics.xdpi
            screenHeight = metrics.heightPixels / metrics.ydpi
            sDPIX = metrics.xdpi
            sDPIY = metrics.ydpi
            Log.d(TAG, "Updated the sizes: " + screenWidth + "x" + screenHeight)
        }

        // instead of below, use dimens.xml as much as possible:
        //    static int
        //    getPixelsAsDP( int pixels ) {
        //        final float scale = mActivityMain.getResources().getDisplayMetrics().density;
        //        return (int) ( pixels * scale + 0.5f );
        //    }
        @JvmStatic
        val screenShortEdge: Float
            get() = screenHeight.coerceAtMost(screenWidth)

        //    public static boolean isLargeScreen() {
        //        return sIsLargeScreen;
        //    }
        //    static boolean isExternalStorageWritable() {
        //        String state = Environment.getExternalStorageState();
        //        return Environment.MEDIA_MOUNTED.equals( state );
        //    }
        @JvmStatic
        fun getLine(source: String, o: MutableInt, line: MutableString): Boolean {
            if(source.isEmpty() || o.v >= source.length) return false
            val oEnd = source.indexOf('\n', o.v)
            if(oEnd == -1) {
                line.v = source.substring(o.v)
                o.v = source.length
            }
            else {
                line.v = source.substring(o.v, oEnd)
                o.v = oEnd + 1
            }
            return true
        }

        @JvmStatic
        fun getLong(line: String, i: MutableInt): Long {
            var result: Long = 0
            while(i.v < line.length && line[i.v] >= '0' && line[i.v] <= '9') {
                result = result * 10 + line[i.v].toLong() - '0'.toLong()
                i.v++
            }
            return result
        }

        @JvmStatic
        fun getInt(line: String, i: MutableInt): Int {
            var result = 0
            while(i.v < line.length && line[i.v] >= '0' && line[i.v] <= '9') {
                result = result * 10 + line[i.v].toInt() - '0'.toInt()
                i.v++
            }
            return result
        }

        @JvmStatic
        fun getDouble(text: String): Double {
            //NOTE: this implementation may be a little bit more forgiving than good for health
            var value = 0.0
            //char lf{ '=' }; // =, \, #, $(unit)
            var divider = 0
            var negative = false
            var c: Char
            for(element in text) {
                c = element
                when(c) {
                    ',', '.' -> if(divider == 0) // note that if divider
                        divider = 1
                    '-' -> negative = true
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        value *= 10.0
                        value += (c - '0').toDouble()
                        if(divider != 0) divider *= 10
                    }
                    else -> {
                    }
                }
            }
            if(divider > 1) value /= divider.toDouble()
            if(negative) value *= -1.0
            return value
        }

        @JvmStatic
        fun getDouble(line: String, i: MutableInt): Double {
            var value = 0.0
            var divider = 0
            var negative = false
            var fContinue = true
            var c: Char
            while(i.v < line.length && fContinue) {
                c = line[i.v]
                when(c) {
                    ',', '.' -> if(divider == 0) // note that if divider
                        divider = 1
                    '-' -> negative = true
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        value *= 10.0
                        value += (c - '0').toDouble()
                        if(divider != 0) divider *= 10
                    }
                    else -> {
                        i.v--
                        fContinue = false // end loop
                    }
                }
                i.v++
            }
            if(divider > 1) value /= divider.toDouble()
            if(negative) value *= -1.0
            return value
        } //  TODO WILL BE IMPLEMENTED LATER
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
    }
}
