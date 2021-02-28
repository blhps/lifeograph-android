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
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class DialogInquireText(context: Context,
                        private val mTitle: Int,
                        private val mDefName: String,
                        private val mActName: Int,
                        private val mListener: InquireListener) : Dialog(context) {

    // VARIABLES ===================================================================================
    private lateinit var mInput: EditText
    private lateinit var mButtonOk: Button

    // METHODS =====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_inquire_text)
        setCancelable(true)
        setTitle(mTitle)
        // setMessage( mMessage );

        // mButtonOk must come before mInput as it is referenced there
        mButtonOk = findViewById(R.id.inquireTextButtonPositive)
        mButtonOk.setText(mActName)
        mButtonOk.setOnClickListener { go() }
        mInput = findViewById(R.id.inquireTextEdit)
        if(Lifeograph.screenHeight >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mInput.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        mInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                mButtonOk.isEnabled = s.isNotEmpty() &&
                        mListener.onInquireTextChanged(mTitle, mInput.text.toString())
            }
        })
        mInput.setOnEditorActionListener { v: TextView, _: Int, _: KeyEvent? ->
            if(v.text.isNotEmpty()) {
                go()
                return@setOnEditorActionListener true
            }
            false
        }
        mInput.setText(mDefName)
        mInput.selectAll()
        val buttonNegative = findViewById<Button>(R.id.inquireTextButtonNegative)
        buttonNegative.setOnClickListener { dismiss() }
    }

    private fun go() {
        mListener.onInquireAction(mTitle, mInput.text.toString())
        dismiss()
    }

    interface InquireListener {
        fun onInquireAction(id: Int, text: String)
        fun onInquireTextChanged(id: Int, text: String): Boolean
    }
}
