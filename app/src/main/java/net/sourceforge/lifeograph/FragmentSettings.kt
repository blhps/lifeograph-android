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

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.preference.*
import net.sourceforge.lifeograph.helpers.FileUtil.getFullPathFromTreeUri

class FragmentSettings : PreferenceFragmentCompat() {
    // VARIABLES ===================================================================================
    private val sPickFolder = 2

    // METHODS =====================================================================================
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //super.onCreatePreferenes( savedInstanceState, rootKey );
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_DIARY_STORAGE_key))!!)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_DIARY_PATH_key))!!)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_DATE_FORMAT_ORDER_key))!!)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_DATE_FORMAT_SEPARATOR_key))!!)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_UNIT_TYPE_key))!!)

        val filePicker: Preference? = findPreference(getString(R.string.pref_DIARY_PATH_key))
        filePicker!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //preference: Preference ->
                openFile()
                true
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view!!.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.t_darker))
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if(requestCode == sPickFolder && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the directory that the user selected
            val path = getFullPathFromTreeUri(resultData?.data, requireContext())
            Log.d(Lifeograph.TAG, "Path: $path")

            val prefs: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor: Editor = prefs.edit()
            editor.putString(getString(R.string.pref_DIARY_PATH_key), path)
            editor.apply()
            val pref : Preference = findPreference(getString(R.string.pref_DIARY_PATH_key))!!
            sBindPreferenceChangeListener.onPreferenceChange(pref, path)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, sPickFolder)
    }


    companion object {
        private val sBindPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { pref: Preference, value: Any ->
            val stringValue = value.toString()
            when(pref.key) {
                Lifeograph.getStr(R.string.pref_DATE_FORMAT_ORDER_key) -> {
                    Date.s_format_order = stringValue
                }
                Lifeograph.getStr(R.string.pref_DATE_FORMAT_SEPARATOR_key) -> {
                    Date.s_format_separator = stringValue[0]
                }
                Lifeograph.getStr(R.string.pref_DIARY_STORAGE_key) -> {
                    FragmentListDiaries.sStoragePref = stringValue

                    val prefDiaryPath: Preference = pref.preferenceManager.findPreference(
                            Lifeograph.getStr(R.string.pref_DIARY_PATH_key))!!
                    prefDiaryPath.isVisible = stringValue != "I"
                }
                Lifeograph.getStr(R.string.pref_DIARY_PATH_key) -> {
                    FragmentListDiaries.sDiaryPath = stringValue
                }
            }
            if(pref is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = pref.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                pref.setSummary(if(index >= 0) pref.entries[index] else null)
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                pref.summary = stringValue
            }
            true
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceChangeListener

            // Trigger the listener immediately with the preference's current value
            sBindPreferenceChangeListener.onPreferenceChange(
                    preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
