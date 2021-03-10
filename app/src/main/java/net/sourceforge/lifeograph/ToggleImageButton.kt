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

// This Code is taken from
// http://stackoverflow.com/questions/2604599/android-imagebutton-with-a-selected-state

package net.sourceforge.lifeograph

import android.content.Context
import androidx.appcompat.widget.AppCompatImageButton
import android.widget.Checkable
import android.util.AttributeSet

class ToggleImageButton : AppCompatImageButton, Checkable {
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        setChecked(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
            context!!, attrs, defStyle) {
        setChecked(attrs)
    }

    private fun setChecked(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ToggleImageButton)
        isChecked = a.getBoolean(R.styleable.ToggleImageButton_android_checked, false)
        a.recycle()
    }

    override fun isChecked(): Boolean {
        return isSelected
    }

    override fun setChecked(checked: Boolean) {
        isSelected = checked
        if(onCheckedChangeListener != null) {
            onCheckedChangeListener!!.onCheckedChanged(this, checked)
        }
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(buttonView: ToggleImageButton?, isChecked: Boolean)
    }
}
