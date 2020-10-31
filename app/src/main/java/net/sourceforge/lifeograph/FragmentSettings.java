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


import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;


public class FragmentSettings extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
        //super.onCreatePreferenes( savedInstanceState, rootKey );

        setPreferencesFromResource( R.xml.pref_general, rootKey );

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.

        bindPreferenceSummaryToValue(
                Objects.requireNonNull(
                        findPreference( getString( R.string.pref_DIARY_STORAGE_key ) ) ) );
        bindPreferenceSummaryToValue(
                Objects.requireNonNull(
                        findPreference( getString( R.string.pref_DIARY_PATH_key ) ) ) );
        bindPreferenceSummaryToValue(
                Objects.requireNonNull(
                        findPreference( getString( R.string.pref_DATE_FORMAT_ORDER_key ) ) ) );
        bindPreferenceSummaryToValue(
                Objects.requireNonNull(
                        findPreference( getString( R.string.pref_DATE_FORMAT_SEPARATOR_key ) ) ) );
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            ( pref, value ) -> {
                String stringValue = value.toString();

                if( pref.getKey().equals(
                        Lifeograph.getStr( R.string.pref_DATE_FORMAT_ORDER_key ) ) ) {
                    Date.s_format_order = stringValue;
                }
                else if( pref.getKey().equals(
                        Lifeograph.getStr( R.string.pref_DATE_FORMAT_SEPARATOR_key ) ) ) {
                    Date.s_format_separator = stringValue.charAt( 0 );
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
