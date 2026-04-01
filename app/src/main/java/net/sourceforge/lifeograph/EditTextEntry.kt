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

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText

// NOTE: this class is added to silence performClick() not overridden warnings in SetOnTouchListener
// but in the long run it can come handy for other uses
// the constructor with defStyleAttr ensures it looks for R.attr.editTextStyle
class EditTextEntry @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle // this links to the XML style
                                              ) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    override fun performClick(): Boolean {
        return super.performClick()
    }
}