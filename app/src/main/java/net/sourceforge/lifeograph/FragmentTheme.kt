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

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener

class FragmentTheme: Fragment(), Lifeograph.DiaryEditor {
// VARIABLES =======================================================================================
    private lateinit var mButtonTextColor: Button
    private lateinit var mButtonBaseColor: Button
    private lateinit var mButtonHeadingColor: Button
    private lateinit var mButtonSubheadingColor: Button
    private lateinit var mButtonHighlightColor: Button
    private lateinit var mButtonReset: Button

    companion object {
        lateinit var mTheme: Theme
        private var sIndex = 0

        // from: http://stackoverflow.com/questions/4672271/reverse-opposing-colors
        private fun getContrastColor(color: Int): Int {
            val y = ((299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(
                    color)) / 1000).toDouble()
            return if(y >= 128) Color.BLACK else Color.WHITE
        }
    }

// METHODS =========================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_theme, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mButtonTextColor = view.findViewById(R.id.button_text_color)
        mButtonBaseColor = view.findViewById(R.id.button_base_color)
        mButtonHeadingColor = view.findViewById(R.id.button_heading_color)
        mButtonSubheadingColor = view.findViewById(R.id.button_subheading_color)
        mButtonHighlightColor = view.findViewById(R.id.button_highlight_color)
        mButtonReset = view.findViewById(R.id.button_theme_reset)

        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        actionBar!!.title = mTheme._title_str
        actionBar.subtitle = ""

        updateButtonColors()
        mButtonTextColor.setOnClickListener {
            sIndex = 0
            showColorDialog(mTheme.color_text + -0x1000000)
        }
        mButtonBaseColor.setOnClickListener {
            sIndex = 1
            showColorDialog(mTheme.color_base + -0x1000000)
        }
        mButtonHeadingColor.setOnClickListener {
            sIndex = 2
            showColorDialog(mTheme.color_heading + -0x1000000)
        }
        mButtonSubheadingColor.setOnClickListener {
            sIndex = 3
            showColorDialog(mTheme.color_subheading + -0x1000000)
        }
        mButtonHighlightColor.setOnClickListener {
            sIndex = 4
            showColorDialog(mTheme.color_highlight + -0x1000000)
        }
        mButtonReset.setOnClickListener { resetTheme() }
    }

    public override fun onStop() {
        super.onStop()
    }

    private fun showColorDialog(prevColor: Int) {
        // create a new theme if there is not
        val dlg = AmbilWarnaDialog(requireContext(), prevColor,
                                   object : OnAmbilWarnaListener {
                                       override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                                           mButtonReset.isEnabled = true
                                           when(sIndex) {
                                               0 -> mTheme.color_text = color
                                               1 -> mTheme.color_base = color
                                               2 -> mTheme.color_heading = color
                                               3 -> mTheme.color_subheading = color
                                               4 -> mTheme.color_highlight = color
                                           }
                                           updateButtonColors()
                                       }

                                       override fun onCancel(dialog: AmbilWarnaDialog) {
                                           // cancel was selected by the user
                                       }
                                   })
        dlg.show()
    }

    private fun resetTheme() {
        // TODO mTheme.reset();
        mButtonReset.isEnabled = false
        updateButtonColors()
    }

    private fun updateButtonColors() {
        val theme = mTheme
        mButtonTextColor.setBackgroundColor(theme.color_text)
        mButtonBaseColor.setBackgroundColor(theme.color_base)
        mButtonHeadingColor.setBackgroundColor(theme.color_heading)
        mButtonSubheadingColor.setBackgroundColor(theme.color_subheading)
        mButtonHighlightColor.setBackgroundColor(theme.color_highlight)
        mButtonTextColor.setTextColor(getContrastColor(theme.color_text))
        mButtonBaseColor.setTextColor(getContrastColor(theme.color_base))
        mButtonHeadingColor.setTextColor(getContrastColor(theme.color_heading))
        mButtonSubheadingColor.setTextColor(getContrastColor(theme.color_subheading))
        mButtonHighlightColor.setTextColor(getContrastColor(theme.color_highlight))
    }

    override fun enableEditing() {
        TODO("Not yet implemented")
    }

    override fun handleBack(): Boolean {
        return false
    }
}
