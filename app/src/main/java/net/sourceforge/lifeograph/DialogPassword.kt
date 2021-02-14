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
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

class DialogPassword(context: Context?, 
                     private val mDiary: Diary,
                     private val mAction: DPAction,
                     private val mListener: Listener) : Dialog(context!!) {
    enum class DPAction {
        DPA_LOGIN, DPA_AUTHENTICATE, DPA_ADD, DPAR_AUTH_FAILED // just returned as a result
    }

    private var mInput1: EditText? = null
    private var mInput2: EditText? = null
    private var mImage1: ImageView? = null
    private var mImage2: ImageView? = null
    private var mButtonOk: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_password)
        setCancelable(true)

        // mButtonOk must come before mInput as it is referenced there
        mButtonOk = findViewById(R.id.passwordButtonPositive)
        mButtonOk!!.setOnClickListener { go() }
        mInput1 = findViewById(R.id.edit_password_1)
        mInput2 = findViewById(R.id.edit_password_2)
        mImage1 = findViewById(R.id.image_password_1)
        mImage2 = findViewById(R.id.image_password_2)
        when(mAction) {
            DPAction.DPA_LOGIN -> {
                setTitle("Enter password for " + mDiary._name)
                mButtonOk!!.text = Lifeograph.getStr(R.string.open)
                mInput2!!.visibility = View.GONE
                mImage1!!.visibility = View.GONE
                mImage2!!.visibility = View.GONE
            }
            DPAction.DPA_AUTHENTICATE -> {
                setTitle("Enter the current password")
                mButtonOk!!.text = Lifeograph.getStr(R.string.authenticate)
                mInput2!!.visibility = View.GONE
                mImage1!!.visibility = View.GONE
                mImage2!!.visibility = View.GONE
            }
            DPAction.DPA_ADD -> {
                setTitle("Enter the new password")
                mButtonOk!!.text = Lifeograph.getStr(R.string.set_password)
            }
            DPAction.DPAR_AUTH_FAILED -> Log.d(Lifeograph.TAG, "Auth Failed")
        }
        if(Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mInput1!!.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        mInput1!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                check()
            }
        })
        mInput1!!.setOnEditorActionListener { _: TextView?, _: Int,
                                              _: KeyEvent? ->
            if(mAction != DPAction.DPA_ADD &&
                    mInput1!!.text.length >= Diary.PASSPHRASE_MIN_SIZE) {
                go()
                true
            }
            else
                false
        }
        mInput2!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                check()
            }
        })
        val buttonNegative = findViewById<Button>(R.id.passwordButtonNegative)
        buttonNegative.setOnClickListener { dismiss() }
    }

    private fun check() {
        val passphrase = mInput1!!.text.toString()
        val passphrase2 = mInput2!!.text.toString()
        if(passphrase.length >= Diary.PASSPHRASE_MIN_SIZE) {
            mImage1!!.setImageResource(R.mipmap.ic_todo_done)
            when {
                mAction != DPAction.DPA_ADD -> {
                    mButtonOk!!.isEnabled = true
                }
                passphrase == passphrase2 -> {
                    mImage2!!.setImageResource(R.drawable.ic_check)
                    mButtonOk!!.isEnabled = true
                }
                else -> {
                    mImage2!!.setImageResource(
                            if(passphrase2.isEmpty()) R.mipmap.ic_todo_open
                            else R.mipmap.ic_todo_canceled)
                    mButtonOk!!.isEnabled = false
                }
            }
        }
        else {
            mImage1!!.setImageResource(if(passphrase.isEmpty()) R.mipmap.ic_todo_open 
                                       else R.mipmap.ic_todo_canceled)
            mImage2!!.setImageResource(if(passphrase2.isEmpty()) R.mipmap.ic_todo_open 
                                       else R.mipmap.ic_todo_canceled)
            mButtonOk!!.isEnabled = false
        }
    }

    private fun go() {
        when(mAction) {
            DPAction.DPA_LOGIN, DPAction.DPA_ADD -> {
                mDiary.set_passphrase(mInput1!!.text.toString())
                mListener.onDPAction(mAction)
            }
            DPAction.DPA_AUTHENTICATE -> 
                if(mDiary.compare_passphrase(mInput1!!.text.toString())) 
                    mListener.onDPAction(mAction) 
                else 
                    mListener.onDPAction(DPAction.DPAR_AUTH_FAILED)
            else ->
                Log.d( Lifeograph.TAG, "Unhandled return" )
        }
        dismiss()
    }

    interface Listener {
        fun onDPAction(action: DPAction)
    }
}
