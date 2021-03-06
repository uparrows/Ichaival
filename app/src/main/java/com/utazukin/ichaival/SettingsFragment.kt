/*
 * Ichaival - Android client for LANraragi https://github.com/Utazukin/Ichaival/
 * Copyright (C) 2020 Utazukin
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.utazukin.ichaival

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val pref: Preference? = findPreference(getString(R.string.server_address_preference))
        binPrefSummaryNotify(pref)

        val apiPref: Preference? = findPreference(getString(R.string.api_key_pref))
        bindPreferenceSummaryToValue(apiPref)

        val licensePref: Preference? = findPreference(getString(R.string.license_key))
        licensePref?.setOnPreferenceClickListener {
            startWebActivity("file:////android_asset/licenses.html")
            true
        }

        val gplPref: Preference? = findPreference(getString(R.string.gpl_key))
        gplPref?.setOnPreferenceClickListener {
            startWebActivity("file:////android_asset/license.txt")
            true
        }

        val gitPref: Preference? = findPreference(getString(R.string.git_key))
        gitPref?.setOnPreferenceClickListener {
            val webpage = Uri.parse("https://github.com/Utazukin/Ichaival")
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            startActivity(intent)
            true
        }

        val thumbPref: Preference? = findPreference(getString(R.string.thumbnail_pref))
        thumbPref?.setOnPreferenceClickListener {
            DatabaseReader.clearThumbnails(requireActivity().filesDir)
            Toast.makeText(activity, getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()
            true
        }

        val tempPref: Preference? = findPreference(getString(R.string.temp_folder_pref))
        tempPref?.setOnPreferenceClickListener {
            (activity as CoroutineScope).launch(Dispatchers.IO) { WebHandler.clearTempFolder() }
            true
        }
    }

    private fun startWebActivity(url: String) {
        val intent = Intent(activity, WebViewActivity::class.java)
        val bundle = Bundle().apply { putString(WebViewActivity.URL_KEY, url) }
        intent.putExtras(bundle)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            startActivity(Intent(activity, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                    if (index >= 0)
                        preference.entries[index]
                    else
                        null
                )
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        private val bindAndNotifyPreferenceListener = Preference.OnPreferenceChangeListener { pref, value ->
            if (WebHandler.onPreferenceChange(pref, value))
                sBindPreferenceSummaryToValueListener.onPreferenceChange(pref, value)
            else
                false
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference?) {
            preference?.let {
                // Set the listener to watch for value changes.
                it.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

                // Trigger the listener immediately with the preference's
                // current value.
                sBindPreferenceSummaryToValueListener.onPreferenceChange(
                    it,
                    PreferenceManager
                        .getDefaultSharedPreferences(it.context)
                        .getString(it.key, "")
                )
            }
        }

        private fun binPrefSummaryNotify(preference: Preference?) {
            preference?.let {
                it.onPreferenceChangeListener = bindAndNotifyPreferenceListener
                bindAndNotifyPreferenceListener.onPreferenceChange(
                    it,
                    PreferenceManager
                        .getDefaultSharedPreferences(it.context)
                        .getString(it.key, "")
                )
            }
        }
    }
}