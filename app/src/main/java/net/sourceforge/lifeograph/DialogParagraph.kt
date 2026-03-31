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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.button.MaterialButtonToggleGroup

// ABOUT DIALOG ====================================================================================
class DialogParagraph(ctx: Context, private val listener: Listener) : Dialog(ctx) {
    // VARIABLES ===================================================================================

    interface Listener {
        fun onApplyParaAction(action: (Paragraph) -> Unit)
    }

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_paragraph)
        setCancelable(true)
        setTitle(R.string.todo_auto)


        val tgCheckboxes = findViewById<MaterialButtonToggleGroup>(R.id.tg_checkboxes)
        val tgBullets = findViewById<MaterialButtonToggleGroup>(R.id.tg_bullets)

        tgCheckboxes.addOnButtonCheckedListener { group, checkedId, isChecked ->
            // when a checkbox is selected, clear the bullet/numbering row
            if (isChecked) tgBullets.clearChecked()

            val action: (Paragraph) -> Unit = when (checkedId) {
                R.id.bt_para_not_list -> { para -> para._list_type = '_' }
                R.id.bt_para_open -> { para -> para._list_type = 'O' }
                R.id.bt_para_progressed -> { para -> para._list_type = '~' }
                R.id.bt_para_done -> { para -> para._list_type = '+' }
                R.id.bt_para_canceled -> { para -> para._list_type = 'X' }
                else -> { _ -> }
            }

            listener.onApplyParaAction(action)
            dismiss()
        }

        tgBullets.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) tgCheckboxes.clearChecked()

            val action: (Paragraph) -> Unit = when (checkedId) {
                R.id.bt_para_bullet -> { para -> para._list_type = '-' }
                R.id.bt_para_number -> { para -> para._list_type = '1' }
                R.id.bt_para_letter -> { para -> para._list_type = 'A' }
                R.id.bt_para_roman -> { para -> para._list_type = 'R' }
                R.id.bt_para_capital -> { para -> para._list_type = 'a' }
                // TODO: 2.0: implement capital variants properly
                else -> { _ -> }
            }

            listener.onApplyParaAction(action)
            dismiss()
        }


    }
}
