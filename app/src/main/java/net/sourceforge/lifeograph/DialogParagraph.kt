/* *********************************************************************************

    Copyright (C) 2012-2026 Ahmet Öztürk (aoz_2@yahoo.com)

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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch

// ABOUT DIALOG ====================================================================================
class DialogParagraph(ctx: Context, private val listener: Listener) : Dialog(ctx) {
    // VARIABLES ===================================================================================

    interface Listener {
        fun onApplyParaAction(action: (Paragraph) -> Unit, fRefreshFully: Boolean = false)
        fun onDuplicateParagraphs()
        fun onEraseParagraphs()
        fun getParagraph(): Paragraph
    }

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_paragraph)
        setCancelable(true)
        setTitle(R.string.todo_auto)

        val para = listener.getParagraph()

        val btIndentMore = findViewById<ImageButton>(R.id.bt_indent_more)
        val btIndentLess = findViewById<ImageButton>(R.id.bt_indent_less)
        val tgHeadingType = findViewById<MaterialButtonToggleGroup>(R.id.tg_heading_type)
        val tgTodoStatus = findViewById<MaterialButtonToggleGroup>(R.id.tg_checkboxes)
        val tgBullets = findViewById<MaterialButtonToggleGroup>(R.id.tg_bullets)
        val btListLetter = findViewById<Button>(R.id.bt_para_letter)
        val btListRoman = findViewById<Button>(R.id.bt_para_roman)
        val llSmallLetters = findViewById<LinearLayout>(R.id.ll_small_letters)
        val swSmallLetters = findViewById<MaterialSwitch>(R.id.sw_small_letters)
        val swHorzRule = findViewById<MaterialSwitch>(R.id.sw_horizontal_rule)
        val swExpanded = findViewById<MaterialSwitch>(R.id.sw_para_expanded)

        // SETTING CURRENT VALUES ==================================================================
        when( para._heading_level ) {
            '_' -> tgHeadingType.check(R.id.bt_para_normal)
            'B' -> tgHeadingType.check(R.id.bt_para_heading_M)
            'S' -> tgHeadingType.check(R.id.bt_para_heading_L)
        }
        when( para._list_type ) {
            '_' -> tgTodoStatus.check(R.id.bt_para_not_list)
            'O' -> tgTodoStatus.check(R.id.bt_para_open)
            '~' -> tgTodoStatus.check(R.id.bt_para_progressed)
            '+' -> tgTodoStatus.check(R.id.bt_para_done)
            'X' -> tgTodoStatus.check(R.id.bt_para_canceled)
            '-' -> tgBullets.check( R.id.bt_para_bullet )
            '1' -> tgBullets.check( R.id.bt_para_number )
            'A' -> { tgBullets.check( R.id.bt_para_letter )
                     swSmallLetters.isChecked = false }
            'a' -> { tgBullets.check( R.id.bt_para_letter )
                     swSmallLetters.isChecked = true }
            'R' -> { tgBullets.check( R.id.bt_para_roman )
                     swSmallLetters.isChecked = false }
            'r' -> { tgBullets.check( R.id.bt_para_roman )
                     swSmallLetters.isChecked = true }
        }
        when( para._list_type ) {
            'A', 'a', 'R', 'r' -> llSmallLetters.visibility = View.VISIBLE
            else -> llSmallLetters.visibility = View.GONE
        }
        btListLetter.text = if(swSmallLetters.isChecked) "a-" else "A-"
        btListRoman.text = if(swSmallLetters.isChecked) "i-" else "I-"

        swExpanded.isChecked = para.is_expanded
        swHorzRule.isChecked = para.has_hrule()

        // ASSIGNING LISTENERS =====================================================================
        btIndentMore.setOnClickListener { listener.onApplyParaAction( { p -> p.indent() } ) }
        btIndentLess.setOnClickListener { listener.onApplyParaAction( { p -> p.unindent() } ) }
        // NOTE: do not dismiss to allow repeated action

        tgHeadingType.addOnButtonCheckedListener { _, checkedId, _ ->
            val action: (Paragraph) -> Unit = when (checkedId) {
                R.id.bt_para_normal -> { para -> para._heading_level = '_' }
                R.id.bt_para_heading_M -> { para -> para._heading_level = 'B' }
                R.id.bt_para_heading_L -> { para -> para._heading_level = 'S' }
                else -> { _ -> }
            }

            listener.onApplyParaAction(action)
            dismiss()
        }

        tgTodoStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            // when a checkbox is selected, clear the bullet/numbering row
            if (isChecked) {
                tgBullets.clearChecked()
                llSmallLetters.visibility = View.GONE

                val action: (Paragraph) -> Unit = when(checkedId) {
                    R.id.bt_para_not_list -> { para -> para.clear_list_type() }
                    R.id.bt_para_open -> { para -> para._list_type = 'O' }
                    R.id.bt_para_progressed -> { para -> para._list_type = '~' }
                    R.id.bt_para_done -> { para -> para._list_type = '+' }
                    R.id.bt_para_canceled -> { para -> para._list_type = 'X' }
                    else -> { _ -> }
                }

                listener.onApplyParaAction(action)
                dismiss()
            }
        }

        tgBullets.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                tgTodoStatus.clearChecked()
                llSmallLetters.visibility = View.VISIBLE
                val fSmall = swSmallLetters.isChecked

                val action: (Paragraph) -> Unit = when(checkedId) {
                    R.id.bt_para_bullet -> { para -> para._list_type = '-' }
                    R.id.bt_para_number -> { para -> para._list_type = '1' }
                    R.id.bt_para_letter -> { para -> para._list_type = if(fSmall) 'a' else 'A' }
                    R.id.bt_para_roman -> { para -> para._list_type = if(fSmall) 'r' else 'R' }
                    else -> { _ -> }
                }

                listener.onApplyParaAction(action)
                dismiss()
            }
            else
                llSmallLetters.visibility = View.GONE
        }

        swSmallLetters.setOnCheckedChangeListener { _, isChecked ->
            val listType = tgBullets.checkedButtonId

            val action: (Paragraph) -> Unit = when(listType) {
                R.id.bt_para_letter -> { para -> para._list_type = if(isChecked) 'a' else 'A' }
                R.id.bt_para_roman -> { para -> para._list_type = if(isChecked) 'r' else 'R' }
                else -> { _ -> }
            }

            btListLetter.text = if(swSmallLetters.isChecked) "a-" else "A-"
            btListRoman.text = if(swSmallLetters.isChecked) "i-" else "I-"

            listener.onApplyParaAction(action, true)
        }

        swExpanded.setOnCheckedChangeListener { _, isChecked ->
            val action: (Paragraph) -> Unit = { p -> p.is_expanded = isChecked }
            listener.onApplyParaAction(action, true)
            dismiss()
        }

        swHorzRule.setOnCheckedChangeListener { _, isChecked ->
            val action: (Paragraph) -> Unit = { p -> p.set_hrule( isChecked ) }
            listener.onApplyParaAction(action)
            dismiss()
        }

        findViewById<android.widget.Button>(R.id.bt_duplicate).setOnClickListener {
            listener.onDuplicateParagraphs()
            dismiss()
        }

        findViewById<android.widget.Button>(R.id.bt_erase).setOnClickListener {
            listener.onEraseParagraphs()
            dismiss()
        }
    }
}
