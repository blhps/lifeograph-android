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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import net.sourceforge.lifeograph.FragmentEntry.ParserEditText.SpanNull
import net.sourceforge.lifeograph.FragmentEntry.ParserEditText.SpanOther
import net.sourceforge.lifeograph.ToDoAction.ToDoObject
import java.util.*
import kotlin.collections.ArrayList

class FragmentEntry : FragmentDiaryEditor(), ToDoObject, DialogInquireText.Listener  {
//    private enum class LinkStatus {
//        LS_OK, LS_ENTRY_UNAVAILABLE, LS_INVALID,  // separator: to check a valid entry link:
//
//        // linkstatus < LS_INVALID
//        LS_CYCLIC, LS_FILE_OK, LS_FILE_INVALID, LS_FILE_UNAVAILABLE, LS_FILE_UNKNOWN
//    }

    // VARIABLES ===================================================================================
    override val mLayoutId: Int = R.layout.fragment_entry
    override val mMenuId: Int   = R.menu.menu_entry

    private lateinit var mEditText: EditText
    private lateinit var mButtonHighlight: Button
    private val          mParser = ParserEditText(this)
    var                  mFlagSetTextOperation = false
    private var          mFlagEditorActionInProgress = false
    var                  mFlagEntryChanged = false
    private var          mFlagDismissOnExit = false
    var                  mFlagSearchIsOpen = false
    private val          mBrowsingHistory = ArrayList<Int>()

    companion object {
        lateinit var mEntry: Entry
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ActivityMain.mViewCurrent = this
        //Lifeograph.updateScreenSizes( this );

        mEditText = view.findViewById(R.id.editTextEntry)
        mEditText.movementMethod = LinkMovementMethod.getInstance()
        //mKeyListener = mEditText.keyListener
        if(!Diary.d.is_in_edit_mode) {
            mEditText.setRawInputType( InputType.TYPE_NULL )
            mEditText.isFocusable = false
            //mEditText.setTextIsSelectable(true) --above seems to work better
            //mEditText.keyListener = null

            view.findViewById<View>(R.id.toolbar_text_edit).visibility = View.GONE
        }
        if(Lifeograph.screenHeight >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mEditText.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        // set custom font as the default font may lack the necessary chars such as check marks:
        /*Typeface font = Typeface.createFromAsset( getAssets(), "OpenSans-Regular.ttf" );
        mEditText.setTypeface( font );*/
        mEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if(!mFlagSetTextOperation) {
                    // if( start > 0 ) {
                    // m_pos_start = mEditText.getText().toString().indexOf( '\n', start - 1 );
                    // if( m_pos_start == -1 )
                    // m_pos_start = 0;
                    // }
                    //
                    // if( start < m_pos_end ) {
                    // m_pos_end = mEditText.getText().toString().indexOf( '\n', start + count );
                    // if( m_pos_end == -1 )
                    // m_pos_end = mEditText.getText().length();
                    // }
                    mFlagEntryChanged = true
                    mEntry._text = mEditText.text.toString()
                }
                reparse()
            }
        })

        mEditText.setOnEditorActionListener { v: TextView, _: Int, _: KeyEvent? ->
            if(mFlagEditorActionInProgress) {
                mFlagEditorActionInProgress = false
                return@setOnEditorActionListener false
            }
            val iterEnd = v.selectionStart
            var iterStart = v.text.toString().lastIndexOf('\n', iterEnd - 1)
            if(iterStart < 0 || iterStart == v.text.length - 1)
                return@setOnEditorActionListener false
            iterStart++ // get rid of the new line char
            val offsetStart = iterStart // save for future
            if(v.text[iterStart] == '\t') {
                val text = StringBuilder("\n\t")
                var value = 0
                var charLf = '*'
                iterStart++ // first tab is already handled, so skip it
                while(iterStart != iterEnd) {
                    when(v.text[iterStart]) {
                        '•' -> {
                            if(charLf != '*') return@setOnEditorActionListener false
                            charLf = ' '
                            text.append("• ")
                        }
                        '[' -> {
                            if(charLf != '*') return@setOnEditorActionListener false
                            charLf = 'c'
                        }
                        '~', '+', 'x', 'X' -> {
                            if(charLf != 'c') return@setOnEditorActionListener false
                            charLf = ']'
                        }
                        ']' -> {
                            if(charLf != ']') return@setOnEditorActionListener false
                            charLf = ' '
                            text.append("[ ] ")
                        }
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            if(charLf != '*' && charLf != '1')
                                return@setOnEditorActionListener false
                            charLf = '1'
                            value *= 10
                            value += v.text[iterStart] - '0'
                        }
                        '-' -> {
                            if(charLf == '*') {
                                text.append("- ")
                                break
                            }
                            if(charLf != '1') return@setOnEditorActionListener false
                            charLf = ' '
                            text.append(++value)
                                    .append(v.text[iterStart])
                                    .append(' ')
                        }
                        '.', ')' -> {
                            if(charLf != '1') return@setOnEditorActionListener false
                            charLf = ' '
                            text.append(++value)
                                    .append(v.text[iterStart])
                                    .append(' ')
                        }
                        '\t' -> {
                            if(charLf != '*') return@setOnEditorActionListener false
                            text.append('\t')
                        }
                        ' ' -> {
                            if(charLf == 'c') {
                                break
                            }
                            else if(charLf != ' ') return@setOnEditorActionListener false
                            // remove the last bullet if no text follows it:
                            if(iterStart == iterEnd - 1) {
                                iterStart = offsetStart
                                mFlagEditorActionInProgress = true
                                mEditText.text.delete(iterStart, iterEnd)
                                mEditText.text.insert(iterStart, "\n")
                            }
                            else {
                                mFlagEditorActionInProgress = true
                                mEditText.text.insert(iterEnd, text)
                                iterStart = iterEnd + text.length
                                if(value > 0) {
                                    iterStart++
                                    while(incrementNumberedLine(
                                                    iterStart, value++, v).also { iterStart = it } > 0) {
                                        iterStart++
                                    }
                                }
                            }
                            return@setOnEditorActionListener true
                        }
                        else -> return@setOnEditorActionListener false
                    }
                    ++iterStart
                }
            }
            false
        }

//        mEditText.customSelectionActionModeCallback = object : ActionMode.Callback {
//            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
//                return true
//            }
//
//            override fun onDestroyActionMode(mode: ActionMode) {}
//            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
//                menu.add(Menu.NONE, R.id.visit_link, Menu.FIRST, R.string.go)
//                return true
//            }
//
//            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//                if(item.itemId == R.id.visit_link) {
//                    val buffer = mEditText.editableText
//                    val link = buffer.getSpans(mEditText.selectionStart,
//                            mEditText.selectionEnd,
//                            ClickableSpan::class.java)
//                    if(link.isNotEmpty())
//                        link[0].onClick(mEditText)
//                    else
//                        Log.i(Lifeograph.TAG, "No link in the selection")
//                    return true
//                }
//                return false
//            }
//        }
        val mButtonBold = view.findViewById<Button>(R.id.buttonBold)
        mButtonBold.setOnClickListener { toggleFormat("*") }
        val mButtonItalic = view.findViewById<Button>(R.id.buttonItalic)
        mButtonItalic.setOnClickListener { toggleFormat("_") }
        val mButtonStrikethrough = view.findViewById<Button>(R.id.buttonStrikethrough)
        val spanStringS = SpannableString("S")
        spanStringS.setSpan(StrikethroughSpan(), 0, 1, 0)
        mButtonStrikethrough.text = spanStringS
        mButtonStrikethrough.setOnClickListener { toggleFormat("=") }
        mButtonHighlight = view.findViewById(R.id.buttonHighlight)
        mButtonHighlight.setOnClickListener { toggleFormat("#") }
        val mButtonIgnore = view.findViewById<Button>(R.id.button_ignore)
        mButtonIgnore.setOnClickListener { toggleIgnoreParagraph() }
        val mButtonComment = view.findViewById<Button>(R.id.button_comment)
        mButtonComment.setOnClickListener { addComment() }
        val mButtonList = view.findViewById<Button>(R.id.button_list)
        mButtonList.setOnClickListener { showStatusPickerDlg() }

        if(mEntry._size > 0) {
            requireActivity().window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }
        show(savedInstanceState == null)
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "ActivityEntry.onPause()" );
    }*/

    override fun onStop() {
        super.onStop()
        Log.d(Lifeograph.TAG, "ActivityEntry.onStop()")
        if(mFlagDismissOnExit) Diary.d.dismiss_entry(mEntry, false) else sync()
        Diary.d.writeLock()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        var item = menu.findItem(R.id.search_text)
        val searchView = item.actionView as SearchView
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                searchView.setQuery(Diary.d._search_text, false)
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                if(mFlagSearchIsOpen) {
                    Diary.d.set_search_text(s.toLowerCase(Locale.ROOT), false)
                    reparse()
                }
                return true
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _: View?, b: Boolean -> mFlagSearchIsOpen = b }

        item = menu.findItem(R.id.change_todo_status)
        val toDoAction = MenuItemCompat.getActionProvider(item) as ToDoAction
        toDoAction.mObject = this
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.enable_edit -> {
                Lifeograph.enableEditing(this)
                return true
            }
            R.id.home -> {
                //NavUtils.navigateUpFromSameTask( this );
                //finish();
                return true
            }
            R.id.toggle_favorite -> {
                toggleFavorite()
                return true
            }
            R.id.change_todo_status -> {
                return false
            }
            R.id.set_theme -> {
                showThemePickerDlg()
                return true
            }
            R.id.edit_date -> {
                DialogInquireText(requireContext(),
                                  R.string.edit_date,
                                  mEntry._date.format_string(),
                                  R.string.apply,
                                  this).show()
                return true
            }
            R.id.dismiss -> {
                dismiss()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val flagWritable = Diary.d.is_in_edit_mode
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                Diary.d.can_enter_edit_mode()
        mMenu.findItem(R.id.change_todo_status).isVisible = flagWritable
        mMenu.findItem(R.id.toggle_favorite).isVisible = flagWritable
        mMenu.findItem(R.id.edit_date).isVisible = flagWritable
        mMenu.findItem(R.id.set_theme).isVisible = flagWritable
        mMenu.findItem(R.id.dismiss).isVisible = flagWritable
    }

    // DiaryEditor interface methods
    override fun enableEditing() {
        super.enableEditing()

        mEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        mEditText.isFocusable = true
        // force soft keyboard to be shown:
//        if(mEditText.requestFocus()) {
//            val imm = requireContext().getSystemService(
//                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)
//        }
        requireActivity().findViewById<View>(R.id.toolbar_text_edit).visibility = View.VISIBLE
        reparse()
    }

    override fun handleBack(): Boolean {
        mBrowsingHistory.removeLast()
        if(mBrowsingHistory.isEmpty()) {
            return false
        }
        else {
            val entry = Diary.d.get_entry_by_id(mBrowsingHistory.last())
            if(entry != null) {
                mEntry = entry
                show(true)
            }
        }
        return true
    }

    private fun updateIcon() {
        /*if( m_ptr2entry.is_favored() ) {
            Bitmap bmp = BitmapFactory.decodeResource(
                    getResources(), m_ptr2entry.get_icon() )
                            .copy( Bitmap.Config.ARGB_8888, true ); // make the bitmap mutable

            Canvas canvas = new Canvas( bmp );

            Bitmap bmp2 = BitmapFactory.decodeResource( getResources(), R.drawable.ic_action_favorite );

            Rect rectDest = new Rect(
                    bmp.getWidth()/2, bmp.getHeight()/2,
                    bmp.getWidth()-1, bmp.getHeight()-1 );

            canvas.drawBitmap( bmp2, null, rectDest, null );

            BitmapDrawable bd =  new BitmapDrawable( bmp );
            bd.setTargetDensity( getResources().getDisplayMetrics().densityDpi );

            mActionBar.setIcon( bd );
        }
        else
            mActionBar.setIcon( m_ptr2entry.get_icon() );*/
        if(isMenuInitialized) {
            mMenu.findItem(R.id.toggle_favorite).setIcon(
                    if(mEntry.is_favored) R.drawable.ic_favorite_active
                    else R.drawable.ic_favorite_inactive)

            mMenu.findItem(R.id.change_todo_status).setIcon(
                    when(mEntry._todo_status_effective) {
                        Entry.ES_TODO ->       R.drawable.ic_todo_open_inactive
                        Entry.ES_PROGRESSED -> R.drawable.ic_todo_progressed_inactive
                        Entry.ES_DONE ->       R.drawable.ic_todo_done_inactive
                        Entry.ES_CANCELED ->   R.drawable.ic_todo_canceled_inactive
                        else ->                R.drawable.ic_todo_auto_inactive
                    } )
        }
    }

    private fun updateTheme() {
        mParser.mP2Theme = mEntry._theme
        mEditText.setBackgroundColor(mParser.mP2Theme.color_base)
        mEditText.setTextColor(mParser.mP2Theme.color_text)
        mButtonHighlight.setTextColor(mParser.mP2Theme.color_text)
        val spanStringH = SpannableString("H")
        spanStringH.setSpan(BackgroundColorSpan(mParser.mP2Theme.color_highlight), 0, 1, 0)
        mButtonHighlight.text = spanStringH
    }

    private fun sync() {
        if(mFlagEntryChanged) {
            mEntry.m_date_edited = (System.currentTimeMillis() / 1000L)
            mEntry._text = mEditText.text.toString()
            mFlagEntryChanged = false
        }
    }

    fun show(flagParse: Boolean) {
        mFlagDismissOnExit = false

        // THEME
        updateTheme()

        // SETTING TEXT
        // mFlagSetTextOperation = true;
        if(flagParse)
            mEditText.setText(mEntry._text)
        // mFlagSetTextOperation = false;

        // if( flagParse )
        // parse();
        Lifeograph.getActionBar().subtitle = mEntry._title_str
        updateIcon()
        //invalidateOptionsMenu(); // may be redundant here

        // BROWSING HISTORY
        if(mBrowsingHistory.isEmpty() || mEntry.m_id != mBrowsingHistory.last()) // not going back
            mBrowsingHistory.add(mEntry.m_id)
    }

    private fun toggleFavorite() {
        mEntry.toggle_favored()
        updateIcon()
    }

    private fun dismiss() {
        Lifeograph.showConfirmationPrompt(
                requireContext(),
                R.string.entry_dismiss_confirm,
                R.string.dismiss
        ) { _: DialogInterface?, _: Int -> mFlagDismissOnExit = true }
    }

    fun showStatusPickerDlg() {
        DialogPicker(requireContext(),
                     object: DialogPicker.Listener{
                               override fun onItemClick(item: RViewAdapterBasic.Item) {
                                   setListItemMark( item.mId[0])
                               }

                               override fun populateItems(list: RVBasicList) {
                                   list.clear()

                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.bullet),
                                                                   "*",
                                                                   R.drawable.ic_bullet))

                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_open),
                                                                   " ",
                                                                   R.drawable.ic_todo_open))
                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_progressed),
                                                                   "~",
                                                                   R.drawable.ic_todo_progressed))
                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_done),
                                                                   "+",
                                                                   R.drawable.ic_todo_done))
                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_canceled),
                                                                   "x",
                                                                   R.drawable.ic_todo_canceled))
                               }
                           }).show()
    }

    private fun showThemePickerDlg() {
        DialogPicker(requireContext(),
                     object: DialogPicker.Listener{
                         override fun onItemClick(item: RViewAdapterBasic.Item) {
                             val theme = Diary.d.get_theme(item.mId)
                             mEntry._theme = theme
                             updateTheme()
                             reparse()
                         }

                         override fun populateItems(list: RVBasicList) {
                             list.clear()

                             for(theme in Diary.d.m_themes.values)
                                 list.add(RViewAdapterBasic.Item(theme.m_name,
                                                                 theme.m_name,
                                                                 R.drawable.ic_theme))
                         }
                     }).show()
    }

    override fun setTodoStatus(s: Int) {
        mEntry._todo_status = s
        mEntry.m_date_status = (System.currentTimeMillis() / 1000L)
        updateIcon()
    }

    // InquireListener methods
    override fun onInquireAction(id: Int, text: String) {
        if(id == R.string.edit_date) {
            val date = Date(text)
            if(date.m_date != Date.NOT_SET) {
                if(!date.is_ordinal) date._order_3rd = 1
                try {
                    Diary.d.set_entry_date(mEntry, date)
                }
                catch(e: Exception) {
                    e.printStackTrace()
                }
                Lifeograph.getActionBar().subtitle = mEntry._info_str
            }
        }
    }

    override fun onInquireTextChanged(id: Int, text: String): Boolean {
        if(id == R.string.edit_date) {
            val date = Date.parse_string(text)
            return date > 0 && date != mEntry.m_date.m_date
        }
        return true
    }

    private fun incrementNumberedLine(pos_bgn: Int, expected_value: Int, v: TextView): Int {
        var pos = pos_bgn
        if(pos >= v.text.length) return -1
        var iterEnd = v.text.toString().indexOf('\n', pos)
        if(iterEnd == -1) iterEnd = v.text.length - 1
        val text = StringBuilder()
        var value = 0
        var charLf = 't'
        while(pos != iterEnd) {
            when(v.text[pos]) {
                '\t' -> {
                    if(charLf != 't' && charLf != '1') return -1
                    charLf = '1'
                    text.append('\t')
                }
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    if(charLf != '1' && charLf != '-') return -1
                    charLf = '-'
                    value *= 10
                    value += v.text[pos] - '0'
                }
                '-', '.', ')' -> {
                    if(charLf != '-' || value != expected_value) return -1
                    charLf = ' '
                    value++
                    text.append(value).append(v.text[pos]).append(' ')
                }
                ' ' -> {
                    if(charLf != ' ') return -1
                    mFlagEditorActionInProgress = true
                    mEditText.text.delete(pos_bgn, pos + 1)
                    mEditText.text.insert(pos_bgn, text)
                    return iterEnd + text.length - (pos - pos_bgn + 1)
                }
                else -> return -1
            }
            ++pos
        }
        return -1
    }

    // FORMATTING BUTTONS ==========================================================================
    private fun calculateMultiParaBounds(bounds: IntArray): Boolean {
        val str = mEditText.text.toString()
        if(mEditText.hasSelection()) {
            bounds[0] = mEditText.selectionStart
            bounds[1] = mEditText.selectionEnd
        }
        else {
            bounds[1] = mEditText.selectionStart
            bounds[0] = bounds[1]
            if(bounds[0] <= 0) return true
            if(str[bounds[0] - 1] == '\n') {
                if(bounds[0] == str.length) return false
                if(str[bounds[0]] == '\n') return false
            }
        }
        bounds[0]--
        if(str.lastIndexOf('\n', bounds[0]) == -1) {
            if(str.indexOf('\n', bounds[0]) == -1)
                return true
            else
                bounds[0] = str.indexOf('\n', bounds[0]) + 1
        }
        else
            bounds[0] = str.lastIndexOf('\n', bounds[0]) + 1
        if(str.indexOf('\n', bounds[1]) == -1)
            bounds[1] = str.length - 1
        else
            bounds[1] = str.indexOf('\n', bounds[1]) - 1
        return bounds[0] > bounds[1]
    }

    private fun toggleFormat(markup: String) {
        var pStart: Int
        var pEnd: Int
        if(mEditText.hasSelection()) {
            var start = -2
            var end = -1
            var properlySeparated = false
            pStart = mEditText.selectionStart
            pEnd = mEditText.selectionEnd - 1
            val pFirstNl = mEditText.text.toString().indexOf('\n')
            when {
                pFirstNl == -1 -> // there is only heading
                    return
                pEnd <= pFirstNl ->
                    return
                pStart > pFirstNl ->
                    pStart-- // also evaluate the previous character
                else -> { // p_start <= p_first_nl
                    pStart = pFirstNl + 1
                    properlySeparated = true
                    start = -1
                }
            }
            while(true) {
                val theSpan = hasSpan(pStart, markup[0])
                if(theSpan.type == '*' || theSpan.type == '_' || theSpan.type == '#' || theSpan.type == '=') 
                    return
                when(mEditText.text[pStart]) {
                    '\n' -> {
                        if(start >= 0) {
                            if(properlySeparated) {
                                mEditText.text.insert(start, markup)
                                end += 2
                                pStart += 2
                                pEnd += 2
                            }
                            else {
                                mEditText.text.insert(start, " $markup")
                                end += 3
                                pStart += 3
                                pEnd += 3
                            }
                            mEditText.text.insert(end, markup)
                            properlySeparated = true
                            start = -1
                            break
                        }
                        if(start == -2) {
                            properlySeparated = true
                            start = -1
                        }
                    }
                    ' ', '\t' -> if(start == -2) {
                        properlySeparated = true
                        start = -1
                    }
                    else -> {
                        if(start == -2) start = -1 else if(start == -1) start = pStart
                        end = pStart
                    }
                }
                if(pStart == pEnd) break
                pStart++
            }
            // add markup chars to the beginning and end:
            if(start >= 0) {
                end += if(properlySeparated) {
                    mEditText.text.insert(start, markup)
                    2
                }
                else {
                    mEditText.text.insert(start, " $markup")
                    3
                }
                mEditText.text.insert(end, markup)
                // TODO place_cursor( get_iter_at_offset( end ) );
            }
        }
        else { // no selection case
            pEnd = mEditText.selectionStart
            pStart = pEnd
            if(isSpace(pStart) || pStart == mEditText.length() - 1) {
                if(startsLine(pStart)) return
                pStart--
                if(hasSpan(pStart, 'm').type == 'm') pStart--
            }
            else if(hasSpan(pStart, 'm').type == 'm') {
                if(startsLine(pStart)) return
                pStart--
                if(isSpace(pStart)) pStart += 2
            }
            val theSpan = hasSpan(pStart, markup[0])

            // if already has the markup remove it
            if(theSpan.type == markup[0]) {
                pStart = mEditText.text.getSpanStart(theSpan)
                pEnd = mEditText.text.getSpanEnd(theSpan)
                mEditText.text.delete(pStart - 1, pStart)
                mEditText.text.delete(pEnd - 1, pEnd)
            }
            else if(theSpan.type == ' ') {
                // find word boundaries:
                while(pStart > 0) {
                    val c = mEditText.text[pStart]
                    if(c == '\n' || c == ' ' || c == '\t') {
                        pStart++
                        break
                    }
                    pStart--
                }
                mEditText.text.insert(pStart, markup)
                while(pEnd < mEditText.text.length) {
                    val c = mEditText.text[pEnd]
                    if(c == '\n' || c == ' ' || c == '\t') break
                    pEnd++
                }
                mEditText.text.insert(pEnd, markup)
                // TODO (if necessary) place_cursor( offset );
            }
        }
    }

    private fun setListItemMark(target_item_type: Char) {
        val bounds = intArrayOf(0, 0)
        if(calculateMultiParaBounds(bounds)) return
        var pos = bounds[0]
        if(bounds[0] == bounds[1]) { // empty line
            when(target_item_type) {
                '*' -> mEditText.text.insert(pos, "\t• ")
                ' ' -> mEditText.text.insert(pos, "\t[ ] ")
                '~' -> mEditText.text.insert(pos, "\t[~] ")
                '+' -> mEditText.text.insert(pos, "\t[+] ")
                'x' -> mEditText.text.insert(pos, "\t[x] ")
                '1' -> mEditText.text.insert(pos, "\t1- ")
            }
            return
        }
        var posEnd = bounds[1]
        var posEraseBegin = pos
        var itemType = 0.toChar() // none
        var charLf = 't' // tab
        var value = 1 // for numeric lists
        mainloop@ while(pos <= posEnd) {
            when(mEditText.text.toString()[pos]) {
                '\t' -> if(charLf == 't' || charLf == '[') {
                    charLf = '[' // opening bracket
                    posEraseBegin = pos
                }
                else charLf = 'n'
                '•', '-' -> {
                    charLf = if(charLf == '[') 's' else 'n'
                    itemType = if(charLf == 's') '*' else 0.toChar()
                }
                '[' -> charLf = (if(charLf == '[') 'c' else 'n')
                ' ' -> {
                    if(charLf == 's') { // separator space
                        if(itemType != target_item_type) {
                            mEditText.text.delete(posEraseBegin, pos + 1)
                            val diff = pos + 1 - posEraseBegin
                            pos -= diff
                            posEnd -= diff
                            charLf = 'a'
                            continue@mainloop
                        }
                        else {
                            charLf ='n'
                        }
                    }
                    else {
                        charLf = if(charLf == 'c') ']' else 'n'
                        itemType = mEditText.text.toString()[pos]
                        // same as below. unfortunately no fallthrough in Kotlin
                    }
                }
                '~', '+', 'x', 'X' -> {
                    charLf = if(charLf == 'c') ']' else 'n'
                    itemType = mEditText.text.toString()[pos]
                }
                ']' -> charLf = (if(charLf == ']') 's' else 'n')
                '\n' -> {
                    itemType = 0.toChar()
                    charLf = 't' // tab
                }
                else -> {
                    if(charLf == 'a' || charLf == 't' || charLf == '[') {
                        when(target_item_type) {
                            '*' -> {
                                mEditText.text.insert(pos, "\t• ")
                                pos += 3
                                posEnd += 3
                            }
                            ' ' -> {
                                mEditText.text.insert(pos, "\t[ ] ")
                                pos += 5
                                posEnd += 5
                            }
                            '~' -> {
                                mEditText.text.insert(pos, "\t[~] ")
                                pos += 5
                                posEnd += 5
                            }
                            '+' -> {
                                mEditText.text.insert(pos, "\t[+] ")
                                pos += 5
                                posEnd += 5
                            }
                            'x' -> {
                                mEditText.text.insert(pos, "\t[x] ")
                                pos += 5
                                posEnd += 5
                            }
                            '1' -> {
                                mEditText.text.insert(pos, "\t$value- ")
                                value++
                            }
                        }
                    }
                    charLf = 'n'
                }
            }
            pos++
        }
    }

    private fun addComment() {
        val pStart: Int = mEditText.selectionStart

        if(pStart>=0)
            return
        if(mEditText.hasSelection()) {
            val pEnd: Int = mEditText.selectionEnd - 1
            mEditText.text.insert(pStart, "[[")
            mEditText.text.insert(pEnd + 2, "]]")
        }
        else { // no selection case
            mEditText.text.insert(pStart, "[[]]")
            mEditText.setSelection(pStart + 2)
        }
    }

    private fun toggleIgnoreParagraph() {
        val bounds = intArrayOf(0, 0)
        if(calculateMultiParaBounds(bounds)) return
        if(bounds[0] == bounds[1]) { // empty line
            mEditText.text.insert(bounds[0], ".\t")
            return
        }
        var pos = bounds[0]
        var posEnd = bounds[1]
        var posEraseBegin = pos
        var charLf = '.'
        while(pos <= posEnd) {
            when(mEditText.text.toString()[pos]) {
                '.' -> if(charLf == '.') {
                    posEraseBegin = pos
                    charLf = 't' // tab
                }
                else charLf = 'n'
                '\n' -> charLf = '.'
                '\t' -> {
                    if(charLf == 't') {
                        mEditText.text.delete(posEraseBegin, pos + 1)
                        val diff = pos + 1 - posEraseBegin
                        pos -= diff
                        posEnd -= diff
                    }
                    if(charLf == '.') {
                        mEditText.text.insert(pos, ".\t")
                        pos += 2
                        posEnd += 2
                    }
                    charLf = 'n'
                }
                0.toChar() -> {
                    if(charLf == '.') {
                        mEditText.text.insert(pos, ".\t")
                        pos += 2
                        posEnd += 2
                    }
                    charLf = 'n'
                }
                else -> {
                    if(charLf == '.') {
                        mEditText.text.insert(pos, ".\t")
                        pos += 2
                        posEnd += 2
                    }
                    charLf = 'n'
                }
            }
            pos++
        }
    }

    // PARSING =====================================================================================
    fun reparse() {
        mParser.parse(0, mEditText.text.length)
    }

    private interface AdvancedSpan {
        val type: Char
    }

    internal class ParserEditText(private val mHost: FragmentEntry) : ParserText() {
        lateinit var mP2Theme: Theme
        private val sMarkupScale = 0.7f
        private val mSpans = Vector<Any?>()

        override fun get_char_at(i: Int): Char {
            return mHost.mEditText.text[i]
        }

        private fun getSlice(bgn: Int, end: Int): String {
            return mHost.mEditText.text.subSequence(bgn, end).toString()
        }

        private fun addSpan(span: Any?, start: Int, end: Int, styles: Int) {
            mSpans.add(span)
            mHost.mEditText.text.setSpan(span, start, end, styles)
        }

        override fun reset(bgn: Int, end: Int){
            super.reset(bgn, end)

            // COMPLETELY CLEAR THE PARSING REGION
            // TODO: only remove spans within the parsing boundaries...
            // mEditText.getText().clearSpans(); <-- problematic!!
            for(span in mSpans)
                mHost.mEditText.text.removeSpan(span)
            mSpans.clear()

            // Following must come after reset as m_pos_para_bgn is reset there
            // when bgn != 0, 1 added to the m_pos_para_bgn to skip the \n at the beginning
            m_p2para_cur = mEntry.get_paragraph(
                    if( m_pos_para_bgn > 0 ) m_pos_para_bgn + 1 else 0 )
            mEntry.clear_paragraph_data( m_pos_para_bgn, end )
        }

        public override fun parse(bgn: Int, end: Int) {
            mHost.updateTodoStatus()
            super.parse(bgn, end)
        }

        public override fun process_paragraph() {
            if(m_p2para_cur == null) return
            when(m_p2para_cur.m_justification) {
                Paragraph.JT_LEFT -> addSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                                             m_pos_para_bgn, m_pos_cur, 0)
                Paragraph.JT_CENTER -> addSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                               m_pos_para_bgn, m_pos_cur, 0)
                Paragraph.JT_RIGHT -> addSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                                              m_pos_para_bgn, m_pos_cur, 0)
            }
            m_p2para_cur = m_p2para_cur._next
        }

        // SPANS ===================================================================================
        class SpanOther : AdvancedSpan {
            override val type: Char
                get() = 'O'
        }

        class SpanNull : AdvancedSpan {
            override val type: Char
                get() = ' '
        }

        @SuppressLint("ParcelCreator")
        private class SpanBold : StyleSpan(Typeface.BOLD), AdvancedSpan {
            override val type: Char
                get() = '*'
        }

        @SuppressLint("ParcelCreator")
        private class SpanItalic : StyleSpan(Typeface.ITALIC), AdvancedSpan {
            override val type: Char
                get() = '_'
        }

        @SuppressLint("ParcelCreator")
        private inner class SpanHighlight : BackgroundColorSpan(mP2Theme.color_highlight),
                AdvancedSpan {
            override val type: Char
                get() = '#'
        }

        @SuppressLint("ParcelCreator")
        private class SpanStrikethrough : StrikethroughSpan(), AdvancedSpan {
            override val type: Char
                get() = '='
        }

        @SuppressLint("ParcelCreator")
        private inner class SpanMarkup : ForegroundColorSpan(mP2Theme.m_color_mid), AdvancedSpan {
            override val type: Char
                get() = 'm'
        }

        private class LinkDate(private val mDate: Long) : ClickableSpan(), AdvancedSpan {
            override fun onClick(widget: View) {
                Log.d( Lifeograph.TAG, "Clicked on Date link")
                var entry = Diary.d.get_entry_by_date(mDate)
                if(entry == null)
                    entry = Diary.d.create_entry(mDate, "")
                Lifeograph.showElem(entry!!)
            }

            override val type: Char
                get() = 'd'
        }

        private class LinkUri(private val mUri: String) : ClickableSpan(), AdvancedSpan {
            override fun onClick(widget: View) {
                Log.d( Lifeograph.TAG, "Clicked on Uri link")
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mUri))
                Lifeograph.mActivityMain.startActivity(browserIntent)
            }

            override val type: Char
                get() = 'u'
        }

        private class LinkID(private val mId: Int) : ClickableSpan(), AdvancedSpan {
            override fun onClick(widget: View) {
                Log.d( Lifeograph.TAG, "Clicked on ID link")
                val elem = Diary.d.get_element(mId)
                if(elem != null) {
                    if(elem._type != DiaryElement.Type.ENTRY)
                        Log.d(Lifeograph.TAG, "Target is not entry")
                    else
                        Lifeograph.showElem(elem)
                }
            }

            override val type: Char
                get() = 'i'
        }

        private class LinkCheck(val mHost: FragmentEntry) :
                ClickableSpan(), AdvancedSpan {
            override fun onClick(widget: View) {
                mHost.showStatusPickerDlg()
            }

            override val type: Char
                get() = 'c'
        }

        // APPLIERS ====================================================================================
        public override fun apply_heading() {
            var end = 0
            if(mHost.mEditText.text[0] != '\n') end = mHost.mEditText.text.toString().indexOf('\n')
            if(end == -1) end = mHost.mEditText.text.length
            addSpan(TextAppearanceSpan(mHost.requireContext(), R.style.headingSpan), 0, end,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(ForegroundColorSpan(mEntry._theme.color_heading), 0, end, 0)
            if(!mHost.mFlagSetTextOperation) {
                mEntry.m_name = mHost.mEditText.text.toString().substring(0, end)
                // handle_entry_title_changed() will not be used here in Android
            }
        }

        public override fun apply_subheading() {
            val bgn = m_recipe_cur.m_pos_bgn
            var end = mHost.mEditText.text.toString().indexOf('\n', bgn)
            if(end == -1)
                end = mHost.mEditText.text.length
            addSpan(TextAppearanceSpan(mHost.requireContext(), R.style.subheadingSpan),
                    bgn, end, Spanned.SPAN_INTERMEDIATE)
            addSpan(ForegroundColorSpan(mEntry._theme.color_subheading),
                    bgn, end, 0)
            if(m_p2para_cur != null)
                m_p2para_cur.m_heading_level = 2
        }

        public override fun apply_bold() {
            applyMarkup(SpanBold())
        }

        public override fun apply_italic() {
            applyMarkup(SpanItalic())
        }

        public override fun apply_strikethrough() {
            applyMarkup(SpanStrikethrough())
        }

        public override fun apply_highlight() {
            applyMarkup(SpanHighlight())
        }

        private fun applyMarkup(span: Any) {
            val bgn = m_recipe_cur.m_pos_bgn
            val mid = bgn + 1
            val cur = m_pos_cur
            addSpan(RelativeSizeSpan(sMarkupScale), bgn, mid,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(SpanMarkup(), bgn, mid, 0)
            addSpan(span, mid, cur, 0)
            addSpan(RelativeSizeSpan(sMarkupScale), cur, cur + 1,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(SpanMarkup(), cur, cur + 1, 0)
        }

        public override fun apply_comment() {
            addSpan(TextAppearanceSpan(mHost.requireContext(), R.style.commentSpan),
                    m_recipe_cur.m_pos_bgn, m_pos_cur + 1,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(ForegroundColorSpan(mP2Theme.m_color_mid),
                    m_recipe_cur.m_pos_bgn, m_pos_cur + 1, Spanned.SPAN_INTERMEDIATE)
            addSpan(SuperscriptSpan(),
                    m_recipe_cur.m_pos_bgn, m_pos_cur + 1, 0)
        }

        public override fun apply_ignore() {
            var end = mHost.mEditText.text.toString().indexOf('\n', m_recipe_cur.m_pos_bgn)
            if(end < 0) end = mHost.mEditText.text.length
            addSpan(ForegroundColorSpan(mP2Theme.m_color_mid),
                    m_recipe_cur.m_pos_bgn, end, 0)
        }

        private fun applyHiddenLinkTags(end: Int, spanLink: Any?) {
            addSpan(RelativeSizeSpan(sMarkupScale),
                    m_recipe_cur.m_pos_bgn, m_recipe_cur.m_pos_mid,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(SpanMarkup(), m_recipe_cur.m_pos_bgn, m_recipe_cur.m_pos_mid, 0)
            addSpan(RelativeSizeSpan(sMarkupScale), m_pos_cur, end,
                    Spanned.SPAN_INTERMEDIATE)
            addSpan(SpanMarkup(), m_pos_cur, end, 0)
            addSpan(spanLink, m_recipe_cur.m_pos_mid + 1, m_pos_cur, 0)
        }

        public override fun apply_link_hidden() {
            //remove_tag( m_tag_misspelled, it_uri_bgn, it_tab );
            var span: Any? = null
            when(m_recipe_cur.m_id) {
                RID_URI -> span = LinkUri(getSlice(m_recipe_cur.m_pos_bgn + 1,
                        m_recipe_cur.m_pos_mid))
                RID_ID -> {
                    val element = Diary.d.get_element(m_recipe_cur.m_int_value)
                    span = if(element != null && element._type == DiaryElement.Type.ENTRY) LinkID(m_recipe_cur.m_int_value)
                    else  // indicate dead links
                        ForegroundColorSpan(Color.RED)
                }
            }
            applyHiddenLinkTags(m_pos_cur + 1, span)
        }

        public override fun apply_link() {
            //remove_tag( m_tag_misspelled, it_bgn, it_cur );
            when(m_recipe_cur.m_id) {
                RID_DATE -> applyDate()
                RID_LINK_AT -> {
                    val uri = "mailto:" + getSlice(m_recipe_cur.m_pos_bgn, m_pos_cur)
                    addSpan(LinkUri(uri), m_recipe_cur.m_pos_bgn, m_pos_cur, 0)
                }
                RID_URI -> {
                    val uri = getSlice(m_recipe_cur.m_pos_bgn, m_pos_cur)
                    addSpan(LinkUri(uri), m_recipe_cur.m_pos_bgn, m_pos_cur, 0)
                }
                RID_ID -> {
                    Log.d(Lifeograph.TAG, "********** m_int_value: " + m_recipe_cur.m_int_value)
                    val element = Diary.d.get_element(m_recipe_cur.m_int_value)
                    val span: Any
                    span =
                            if(element != null && element._type == DiaryElement.Type.ENTRY)
                                LinkID(element.m_id)
                            else  // indicate dead links
                                ForegroundColorSpan(Color.RED)
                    addSpan(span, m_recipe_cur.m_pos_bgn, m_pos_cur, 0)
                }
            }
        }

        private fun applyDate() {
            // DO NOT FORGET TO COPY UPDATES HERE TO TextbufferDiarySearch::apply_date()
            addSpan(LinkDate(m_date_last.m_date), m_recipe_cur.m_pos_bgn, m_pos_cur + 1, 0)
            if(m_p2para_cur != null)
                m_p2para_cur.m_date = m_date_last.m_date
        }

        private fun applyCheck(tag_box: Any, tag: Any?) {
            val posBgn = m_pos_cur - 3
            val posBox = m_pos_cur
            var posEnd = mHost.mEditText.text.toString().indexOf('\n', m_pos_cur)
            if(posEnd == -1)
                posEnd = mHost.mEditText.text.length
            if(Diary.d.is_in_edit_mode)
                addSpan(LinkCheck(mHost), posBgn, posBox, Spanned.SPAN_INTERMEDIATE)
            addSpan(tag_box, posBgn, posBox, Spanned.SPAN_INTERMEDIATE)
            addSpan(TypefaceSpan("monospace"), posBgn, posBox, 0)
            if(tag != null)
                addSpan(tag, posBox + 1, posEnd, 0) // ++ to skip separating space char
        }

        public override fun apply_check_unf() {
            applyCheck(ForegroundColorSpan(Theme.s_color_todo), SpanBold())
        }

        public override fun apply_check_prg() {
            applyCheck(ForegroundColorSpan(Theme.s_color_progressed), null)
        }

        public override fun apply_check_fin() {
            applyCheck(ForegroundColorSpan(Theme.s_color_done),
                       BackgroundColorSpan(Theme.s_color_done))
        }

        public override fun apply_check_ccl() {
            applyCheck(ForegroundColorSpan(Theme.s_color_canceled), SpanStrikethrough())
        }

        public override fun apply_match() {
            addSpan(BackgroundColorSpan(mP2Theme.m_color_match_bg),
                    m_pos_search, m_pos_cur + 1, Spanned.SPAN_INTERMEDIATE)
            addSpan(ForegroundColorSpan(mP2Theme.color_base),
                    m_pos_search, m_pos_cur + 1, 0)
        }

        public override fun apply_inline_tag() {
            // m_pos_mid is used to determine if a value is assigned to the tag
            var pEnd = if(m_recipe_cur.m_pos_mid > 0) m_recipe_cur.m_pos_mid else m_pos_cur
            val pNameBgn = m_recipe_cur.m_pos_bgn + 1
            val pNameEnd = pEnd - 1
            val tagName = getSlice(pNameBgn, pNameEnd)
            val entries = Diary.d.get_entries_by_name(tagName)
            if(entries == null || entries.isEmpty())
                addSpan(ForegroundColorSpan(Color.RED), m_recipe_cur.m_pos_bgn, pEnd, 0)
            else {
                if(m_recipe_cur.m_pos_mid == 0) {
                    addSpan(SpanMarkup(), m_recipe_cur.m_pos_bgn, pNameBgn, 0)
                    addSpan(BackgroundColorSpan(mP2Theme.m_color_inline_tag),
                            m_recipe_cur.m_pos_bgn, pEnd, 0)
                    addSpan(LinkID(entries[0]._id),
                            pNameBgn, pNameEnd, 0)
                    addSpan(SpanMarkup(), pNameEnd, pEnd, 0)
                    if(m_p2para_cur != null)
                        m_p2para_cur.set_tag(tagName, 1.0)
                }
                else {
                    val pBgn = m_recipe_cur.m_pos_mid + 1
                    pEnd = m_pos_extra_2 + 1
                    addSpan(BackgroundColorSpan(mP2Theme.m_color_inline_tag), pBgn, pEnd, 0)
                    if(m_p2para_cur == null) return
                    if(m_pos_extra_1 > m_recipe_cur.m_pos_bgn) { // has planned value
                        val vReal = Lifeograph.getDouble(getSlice(pBgn, m_pos_extra_1))
                        val vPlan = Lifeograph.getDouble(getSlice(m_pos_extra_1 + 1, pEnd))
                        m_p2para_cur.set_tag(tagName, vReal, vPlan)
                    }
                    else
                        m_p2para_cur.set_tag(tagName, Lifeograph.getDouble(getSlice(pBgn, pEnd)))
                }
            }
        }
    }

    // PARSING HELPER FUNCTIONS ====================================================================
    private fun isSpace(offset: Int): Boolean {
        if(offset < 0 || offset >= mEditText.text.length)
            return false
        return when(mEditText.text[offset]) {
            '\n', '\t', ' ' -> true
            else -> false
        }
    }

    private fun startsLine(offset: Int): Boolean {
        if(offset < 0 || offset >= mEditText.text.length)
            return false
        return offset == 0 || mEditText.text[offset - 1] == '\n'
    }

    private fun hasSpan(offset: Int, type: Char): AdvancedSpan {
        val spans = mEditText.text.getSpans(offset, offset, Any::class.java)
        var hasNoOtherSpan = true
        for(span in spans) {
            if(span is AdvancedSpan) {
                hasNoOtherSpan = if(span.type == type) {
                    return span
                }
                else false
            }
        }
        return if(hasNoOtherSpan) SpanNull() else SpanOther()
    }

    private fun updateTodoStatus() {
        if(mEntry._status and DiaryElement.ES_NOT_TODO != 0) {
            Entry.calculate_todo_status(mEntry._text)
            updateIcon()
            // Not relevant now: panel_diary->handle_elem_changed( m_ptr2entry );
        }
    }
}
