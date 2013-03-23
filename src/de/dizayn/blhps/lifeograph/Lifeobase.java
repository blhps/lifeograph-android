package de.dizayn.blhps.lifeograph;

import java.util.Locale;

import android.content.Context;

public class Lifeobase {
    public static ActivityOpenDiary activityOpenDiary = null;
    public static ActivityDiary activityDiary = null;
    public static ActivityEntry activityEntry = null;

    public static Context context = null;

    public static final String PROGRAM_NAME = "Lifeograph";

    public static final String LANG_INHERIT_DIARY = "d";

    public static String getStr( int i ) {
        if( context == null )
            return "n/a";
        else
            return context.getString( i );
    }

    public static String get_env_lang() {
        return Locale.getDefault().getLanguage();
    }
}
