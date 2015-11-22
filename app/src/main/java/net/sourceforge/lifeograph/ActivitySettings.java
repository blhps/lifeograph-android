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

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class ActivitySettings extends PreferenceActivity
{
    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        addPreferencesFromResource( R.xml.pref_general );

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.

        bindPreferenceSummaryToValue(
                findPreference( Lifeograph.getStr( R.string.pref_DIARY_STORAGE_key ) ) );
        bindPreferenceSummaryToValue(
                findPreference( Lifeograph.getStr( R.string.pref_DIARY_PATH_key ) ) );
        bindPreferenceSummaryToValue(
                findPreference( Lifeograph.getStr( R.string.pref_DATE_FORMAT_ORDER_key ) ) );
        bindPreferenceSummaryToValue(
                findPreference( Lifeograph.getStr( R.string.pref_DATE_FORMAT_SEPARATOR_key ) ) );
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener()
    {
        public boolean onPreferenceChange( Preference pref, Object value ) {
            String stringValue = value.toString();

            if( pref.getKey().equals(
                    Lifeograph.getStr( R.string.pref_DATE_FORMAT_ORDER_key ) ) ) {
                Date.s_format_order = stringValue;
            }
            else if( pref.getKey().equals(
                    Lifeograph.getStr( R.string.pref_DATE_FORMAT_SEPARATOR_key ) ) ) {
                Date.s_format_separator = stringValue;
            }
            else if( pref.getKey().equals(
                    Lifeograph.getStr( R.string.pref_DIARY_STORAGE_key ) ) ) {
                ActivityLogin.sExternalStorage = stringValue;
            }
            else if( pref.getKey().equals(
                    Lifeograph.getStr( R.string.pref_DIARY_PATH_key ) ) ) {
                ActivityLogin.sDiaryPath = stringValue;
            }

            if( pref instanceof ListPreference ) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = ( ListPreference ) pref;
                int index = listPreference.findIndexOfValue( stringValue );

                // Set the summary to reflect the new value.
                pref.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[ index ]
                                : null );

            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                pref.setSummary( stringValue );
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue( Preference preference ) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener( sBindPreferenceSummaryToValueListener );

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences( preference.getContext() )
                                 .getString( preference.getKey(), "" ) );
    }
}
