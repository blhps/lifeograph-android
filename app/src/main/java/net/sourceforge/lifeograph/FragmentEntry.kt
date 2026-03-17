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

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.text.style.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import net.sourceforge.lifeograph.ToDoAction.ToDoObject
import java.util.*

class FragmentEntry : FragmentDiaryEditor(), MenuProvider, ToDoObject, DialogInquireText
    .Listener  {
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
    var                  mFlagSetTextOperation = false
    var                  mFlagEntryChanged = false
    private var          mFlagDismissOnExit = false
    var                  mFlagSearchIsOpen = false
    private val          mBrowsingHistory = ArrayList<Int>()

    companion object {
        lateinit var mEntry: Entry
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ActivityMain.mViewCurrent = this
        if (mMenuId > 0) {
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        //Lifeograph.updateScreenSizes( this );

        mEditText = view.findViewById(R.id.editTextEntry)
        //mEditText.movementMethod = LinkMovementMethod.getInstance()
        //mKeyListener = mEditText.keyListener
        if(!Diary.getMain().is_in_edit_mode) {
            mEditText.setRawInputType( InputType.TYPE_NULL )
            //mEditText.isFocusable = false
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
                    if(count == 1 && s.subSequence(start, start + count)[0] == '\n') {
                        mFlagSetTextOperation = true
                        handleNewLine(s, start + count - 1)
                        mFlagSetTextOperation = false
                    }
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
                    mEntry.insert_text(start, s.toString())
                }
                updateTextFormatting(mEntry._paragraph_1st, mEntry._paragraph_last)
            }
        })

//        mEditText.setOnEditorActionListener { handleNewLine() } <- moved to onTextChanged

        // ----alternative way of showing the go button but does not work in read-only mode----
//        mEditText.accessibilityDelegate = object : View.AccessibilityDelegate() {
//            override fun sendAccessibilityEvent(host: View, eventType: Int) {
//                super.sendAccessibilityEvent(host, eventType)
//                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED){
//                    val buffer = mEditText.editableText
//                    val link = buffer.getSpans(mEditText.selectionStart,
//                                               mEditText.selectionEnd,
//                                               ClickableSpan::class.java)
//                    if(link.isNotEmpty())
//                        view.findViewById<View>(R.id.visit_link).visibility = View.GONE
//                    else
//                        view.findViewById<View>(R.id.visit_link).visibility = View.VISIBLE
//                }
//            }
//        }

        mEditText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                if( mEditText.editableText.getSpans(mEditText.selectionStart,
                                                    mEditText.selectionEnd,
                                                    ClickableSpan::class.java).isNotEmpty())
                    menu.add(Menu.NONE, R.id.visit_link, Menu.FIRST, R.string.go)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if(item.itemId == R.id.visit_link) {
                    val buffer = mEditText.editableText
                    val link = buffer.getSpans(mEditText.selectionStart,
                            mEditText.selectionEnd,
                            ClickableSpan::class.java)
                    if(link.isNotEmpty())
                        link[0].onClick(mEditText)
                    else
                        Log.i(Lifeograph.TAG, "No link in the selection")
                    return true
                }
                return false
            }
        }
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
        if(mFlagDismissOnExit) Diary.getMain().dismiss_entry(mEntry) else sync()
        Diary.getMain().write_lock()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if(mMenuId > 0)
            menuInflater.inflate(mMenuId, menu)
        mMenu = menu
    }

    override fun onPrepareMenu(menu: Menu) {
        var item = menu.findItem(R.id.search_text)
        val searchView = item.actionView as SearchView
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                searchView.setQuery(Diary.getMain()._search_str, false)
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
                    Diary.getMain()._search_str = s
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

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.enable_edit -> {
                Lifeograph.enableEditing(this)
                true
            }
//            android.R.id.home -> {
//                handleBack() // onSupportNavigateUp() in the ActivityMain is called in case of false
//            }
            R.id.toggle_favorite -> {
                toggleFavorite()
                true
            }
            R.id.change_todo_status -> {
                false
            }
            R.id.set_theme -> {
                showThemePickerDlg()
                true
            }
            R.id.dismiss -> {
                dismiss()
                true
            }
            else -> false
        }
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val dm = Diary.getMain()
        val flagWritable = dm.is_in_edit_mode
        mMenu.findItem(R.id.enable_edit).isVisible = !flagWritable &&
                dm.can_enter_edit_mode()
        mMenu.findItem(R.id.change_todo_status).isVisible = flagWritable
        mMenu.findItem(R.id.toggle_favorite).isVisible = flagWritable
        mMenu.findItem(R.id.set_theme).isVisible = flagWritable
        mMenu.findItem(R.id.dismiss).isVisible = flagWritable
    }

    // DiaryEditor interface methods
    override fun enableEditing() {
        super.enableEditing()

        mEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        //mEditText.isFocusable = true
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
        if(!mBrowsingHistory.isEmpty())
            mBrowsingHistory.removeAt(mBrowsingHistory.lastIndex)
        if(mBrowsingHistory.isEmpty()) {
            return false
        }
        else {
            val entry = Diary.getMain().get_entry_by_id(mBrowsingHistory.last())
            if(entry != null) {
                mEntry = entry
                show(true)
            }
        }
        return true
    }

    private fun handleNewLine(fullText: CharSequence, iterEnd: Int): Boolean {
        // iterStart is the start of the previous line
        var iterStart = fullText.toString().lastIndexOf('\n', iterEnd - 1)
        if(iterStart < 0 || iterStart == fullText.length - 1)
            return false
        iterStart++ // get rid of the new line char
        val offsetStart = iterStart // save for future
        if(fullText[iterStart] == '\t') {
            val text = StringBuilder("\t")
            var value = 0
            var charLf = '*'
            iterStart++ // first tab is already handled, so skip it
            while(iterStart != iterEnd) {
                when(fullText[iterStart]) {
                    '•' -> {
                        if(charLf != '*') return false
                        charLf = ' '
                        text.append("• ")
                    }
                    '[' -> {
                        if(charLf != '*') return false
                        charLf = 'c'
                    }
                    '~', '+', 'x', 'X' -> {
                        if(charLf != 'c') return false
                        charLf = ']'
                    }
                    ']' -> {
                        if(charLf != ']') return false
                        charLf = ' '
                        text.append("[ ] ")
                    }
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        if(charLf != '*' && charLf != '1')
                            return false
                        charLf = '1'
                        value *= 10
                        value += fullText[iterStart] - '0'
                    }
                    '-' -> {
                        if(charLf == '*') {
                            text.append("- ")
                            break
                        }
                        if(charLf != '1') return false
                        charLf = ' '
                        text.append(++value)
                            .append(fullText[iterStart])
                            .append(' ')
                    }
                    '.', ')' -> {
                        if(charLf != '1') return false
                        charLf = ' '
                        text.append(++value)
                            .append(fullText[iterStart])
                            .append(' ')
                    }
                    '\t' -> {
                        if(charLf != '*') return false
                        text.append('\t')
                    }
                    ' ' -> {
                        if(charLf == 'c')
                            charLf = ']'
                        else {
                            if(charLf != ' ') return false
                            // remove the last bullet if no text follows it:
                            if(iterStart == iterEnd - 1) {
                                iterStart = offsetStart
                                mEditText.text.delete(iterStart, iterEnd)
                                mEditText.text.insert(iterStart, "\n")
                            }
                            else {
                                mEditText.text.insert(iterEnd+1, text)
                                iterStart = iterEnd + text.length
                                if(value > 0) {
                                    iterStart++
                                    while(incrementNumberedLine(fullText,
                                                                iterStart,
                                                                value++).also {
                                            iterStart = it } > 0) {
                                        iterStart++
                                    }
                                }
                            }
                            return true
                        }
                    }
                    else -> return false
                }
                ++iterStart
            }
        }
        return false
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
                    if(mEntry.is_favorite) R.drawable.ic_favorite_active
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
        val theme = mEntry._theme
        mEditText.setBackgroundColor(theme._color_base)
        mEditText.setTextColor(theme._color_text)
        mButtonHighlight.setTextColor(theme._color_text)
        val spanStringH = SpannableString("H")
        spanStringH.setSpan(BackgroundColorSpan(theme._color_highlight), 0, 1, 0)
        mButtonHighlight.text = spanStringH
    }

    private fun sync() {
        if(mFlagEntryChanged) {
            mEntry._text = mEditText.text.toString()
            mFlagEntryChanged = false
        }
    }

    fun show(flagParse: Boolean) {
        mFlagDismissOnExit = false

        // THEME
        updateTheme()

        // SETTING TEXT
        mFlagSetTextOperation = true
        if(flagParse)
            mEditText.setText(mEntry._text)
        mFlagSetTextOperation = false

        // if( flagParse )
        // parse();
        Lifeograph.getActionBar().subtitle = mEntry._name // TODO: _title_str
        updateIcon()
        //invalidateOptionsMenu(); // may be redundant here

        // BROWSING HISTORY
        if(mBrowsingHistory.isEmpty() || mEntry._id != mBrowsingHistory.last()) // not going back
            mBrowsingHistory.add(mEntry._id)
    }

    private fun toggleFavorite() {
        mEntry.toggle_favorite()
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
                             val theme = Diary.getMain().get_theme(item.mId)
                             mEntry._theme = theme
                             updateTheme()
                             reparse()
                         }

                         override fun populateItems(list: RVBasicList) {
                             list.clear()

                             for(theme in Diary.getMain()._themes)
                                 list.add(RViewAdapterBasic.Item(theme._name,
                                                                 theme._name,
                                                                 R.drawable.ic_theme))
                         }
                     }).show()
    }

    override fun setTodoStatus(s: Int) {
        mEntry._todo_status = s
        updateIcon()
    }

    // InquireListener methods
//    override fun onInquireAction(id: Int, text: String) {
//        if(id == R.string.edit_date) {
//            val date = Date(text)
//            if(date.m_date != Date.NOT_SET) {
//                if(!date.is_ordinal) date._order_3rd = 1
//                try {
//                    Diary.d.set_entry_date(mEntry, date)
//                }
//                catch(e: Exception) {
//                    e.printStackTrace()
//                }
//                Lifeograph.getActionBar().subtitle = mEntry._info_str
//            }
//        }
//    }

//    override fun onInquireTextChanged(id: Int, text: String): Boolean {
//        if(id == R.string.edit_date) {
//            val date = Date.parse_string(text)
//            return date > 0 && date != mEntry.m_date.m_date
//        }
//        return true
//    }

    private fun incrementNumberedLine(fullText: CharSequence,
                                      pos_bgn: Int,
                                      expected_value: Int): Int {
        var pos = pos_bgn
        if(pos >= fullText.length) return -1
        var iterEnd = fullText.toString().indexOf('\n', pos)
        if(iterEnd == -1) iterEnd = fullText.length - 1
        val text = StringBuilder()
        var value = 0
        var charLf = 't'
        while(pos != iterEnd) {
            when(fullText[pos]) {
                '\t' -> {
                    if(charLf != 't' && charLf != '1') return -1
                    charLf = '1'
                    text.append('\t')
                }
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    if(charLf != '1' && charLf != '-') return -1
                    charLf = '-'
                    value *= 10
                    value += fullText[pos] - '0'
                }
                '-', '.', ')' -> {
                    if(charLf != '-' || value != expected_value) return -1
                    charLf = ' '
                    value++
                    text.append(value).append(fullText[pos]).append(' ')
                }
                ' ' -> {
                    if(charLf != ' ') return -1
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

    private fun setListItemMark(targetItemType: Char) {
        val bounds = intArrayOf(0, 0)
        if(calculateMultiParaBounds(bounds)) return
        var pos = bounds[0]
        if(bounds[0] == bounds[1]) { // empty line
            when(targetItemType) {
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
                        if(itemType != targetItemType) {
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
                        when(targetItemType) {
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
        val para_bgn : Paragraph = mEntry.get_paragraph( bounds[0] )
        val para_end : Paragraph = mEntry.get_paragraph( bounds[1] )
        if(para_bgn._quot_type == '_') // off
            para_bgn._quot_type = '*' // generic
        else
            para_bgn._quot_type = '_'
    }

    // PARSING =====================================================================================

    private fun processParagraph(p: Paragraph, offset: Int, offsetEnd: Int) {
        val theme = mEntry._theme
        // In Android, we don't necessarily "remove all tags" manually like GTK if we are
        // using the ParserEditText's reset() logic, but for parity:
        // (Assuming mEditText.text is a Spannable)

        // 1. ALIGNMENT
        val alignment = when(p._alignment) {
            '<' -> Layout.Alignment.ALIGN_NORMAL
            '|' -> Layout.Alignment.ALIGN_CENTER
            '>' -> Layout.Alignment.ALIGN_OPPOSITE
            else -> null
        }
        alignment?.let {
            mEditText.text.setSpan(AlignmentSpan.Standard(it), offset, offsetEnd, 0)
        }

        // 2. HEADING TYPE
        when(p._heading_level) {
            'T' -> { // TITLE
//                mEditText.text.setSpan(RelativeSizeSpan(1.5f), offset, offsetEnd, 0)
//                mEditText.text.setSpan(StyleSpan(Typeface.BOLD), offset, offsetEnd, 0)
                mEditText.text.setSpan(TextAppearanceSpan(requireContext(), R.style.headingSpan),
                                       offset, offsetEnd, Spanned.SPAN_INTERMEDIATE)
                mEditText.text.setSpan(ForegroundColorSpan(mEntry._theme._color_title), offset,
                                       offsetEnd, 0)
                // TODO: handle_title_edited logic would go here if needed for Android UI
            }

            'S' -> { // LARGE
//                mEditText.text.setSpan(RelativeSizeSpan(1.2f), offset, offsetEnd, 0)
//                mEditText.text.setSpan(StyleSpan(Typeface.BOLD), offset, offsetEnd, 0)
                mEditText.text.setSpan(TextAppearanceSpan(requireContext(), R.style.subheadingSpan),
                                       offset, offsetEnd, Spanned.SPAN_INTERMEDIATE)
                mEditText.text.setSpan(ForegroundColorSpan(mEntry._theme._color_heading_L), offset,
                                       offsetEnd, 0)
            }

            'B' -> { // MEDIUM
                mEditText.text.setSpan(StyleSpan(Typeface.BOLD), offset, offsetEnd, 0)
            }
        }

        // 3. LIST ITEM TYPE & SPECIAL STYLES (Exempt for title)
        if(!p.is_title) {
            if(offset != offsetEnd) {
                when(p._list_type) {
                    '-' -> { // bullet
                    }

                    'O' -> { // open to-do
                        mEditText.text.setSpan(ForegroundColorSpan(theme._color_open), offset,
                                               offsetEnd, 0)
                    }

//                    '~' -> { // in progress: no special format
//                    }

                    '+' -> { // done
                        mEditText.text.setSpan(ForegroundColorSpan(theme._color_done), offset, offsetEnd, 0)
                        mEditText.text.setSpan(BackgroundColorSpan(theme._color_done_bg), offset,
                                               offsetEnd, 0)
                    }

                    'X' -> { // canceled
                        mEditText.text.setSpan(StrikethroughSpan(), offset, offsetEnd, 0)
                    }
                }
            }

            if(p.is_code) {
                mEditText.text.setSpan(TypefaceSpan("monospace"), offset, offsetEnd, 0)
            } else if(p.is_quote) {
                mEditText.text.setSpan(QuoteSpan(), offset, offsetEnd, 0)
                mEditText.text.setSpan(StyleSpan(Typeface.ITALIC), offset, offsetEnd, 0)
            }

            // 4. INDENTATION
            val indentLevel = p._indent_level
            if(indentLevel > 0) {
                val margin = indentLevel * 60
                mEditText.text.setSpan(LeadingMarginSpan.Standard(margin), offset, offsetEnd, 0)
            }
        }

        // 5. HIDDEN FORMATS (Inline Spans)
        for(format in p._formats) {
            val fStart = offset + format.posBgn
            val fEnd = offset + format.posEnd

            // Ensure boundaries are valid
            if(fStart < 0 || fEnd > mEditText.text.length || fStart >= fEnd) continue

            val colorMid = theme._color_mid

            when(format.type) {
                'B' -> mEditText.text.setSpan(StyleSpan(Typeface.BOLD), fStart, fEnd, 0)
                'I' -> mEditText.text.setSpan(StyleSpan(Typeface.ITALIC), fStart, fEnd, 0)
                'H' -> mEditText.text.setSpan(
                    BackgroundColorSpan(theme._color_highlight), fStart, fEnd, 0 )
                'S' -> mEditText.text.setSpan(StrikethroughSpan(), fStart, fEnd, 0)
                'U' -> mEditText.text.setSpan(UnderlineSpan(), fStart, fEnd, 0)
                'F' -> mEditText.text.setSpan(ForegroundColorSpan(colorMid), fStart, fEnd, 0)
                'C' -> mEditText.text.setSpan(SubscriptSpan(), fStart, fEnd, 0)
                'P' -> mEditText.text.setSpan(SuperscriptSpan(), fStart, fEnd, 0)
                'T' -> { // TAG
                    mEditText.text.setSpan(
                        BackgroundColorSpan(theme._color_inline_tag), fStart, fEnd, 0)
                    mEditText.text.setSpan(
                        LinkID(format.refId.toInt()), fStart, fEnd, 0)
                }
                'L' -> { // Link: URI
                    mEditText.text.setSpan( LinkUri(format.uri), fStart, fEnd, 0)
                    // You already have LinkUri class defined in ParserEditText
                    // mEditText.text.setSpan(LinkUri(format.uri), fStart, fEnd, 0)
                }
                'D' -> { // Link: ID
                    val element = Diary.getMain().get_tag_by_id(format.refId.toInt())
                    val span = if(element != null)
                            LinkID(format.refId.toInt())
                        else  // indicate dead links
                            ForegroundColorSpan(Color.RED)
                    mEditText.text.setSpan(span, fStart, fEnd, 0)
                }
                'v' -> { // TAG VALUE
                    mEditText.text.setSpan(
                        BackgroundColorSpan(theme._color_inline_tag), fStart, fEnd, 0)
                }
                'c' -> { // COMMENT / MARKUP
                    mEditText.text.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.commentSpan),
                        fStart, fEnd, 0 ) // general span
                    mEditText.text.setSpan( ForegroundColorSpan(colorMid), fStart,
                                            fStart + 2, 0 ) // [[
                    if(format.posBgn < format.posEnd - 4) {
                        mEditText.text.setSpan(
                            ForegroundColorSpan(colorMid), fStart + 2, fEnd - 2, 0)
                    }
                    mEditText.text.setSpan(ForegroundColorSpan(colorMid), fEnd - 2, fEnd, 0) // ]]
                }
                'd' -> { // DATE
                    mEditText.text.setSpan(LinkDate(format.varD), fStart, fEnd, 0)
                }
                'm' -> { // MATCH
                    mEditText.text.setSpan(
                        BackgroundColorSpan(theme._color_match_bg),
                        fStart, fEnd, Spanned.SPAN_INTERMEDIATE )
                    mEditText.text.setSpan(
                        ForegroundColorSpan(theme._color_base),
                        fStart, fEnd, Spanned.SPAN_INTERMEDIATE )
                }
                // Add other types (Date, ID, etc.) based on your ParserEditText.AdvancedSpan types
            }
        }
    }

//    private fun updateTextFormatting(bgn: Int, end: Int) {
//
//    }
    private fun updateTextFormatting(paraBgn: Paragraph, paraEnd: Paragraph) {
        var offset = paraBgn._bgn_offset_in_host
        var offsetEnd: Int

        var p: Paragraph? = paraBgn
        while(p != null) {
            offsetEnd = offset + p._size

            processParagraph(p, offset, offsetEnd)

            if(p === paraEnd) { break }

            // Move to next paragraph
            offset = offsetEnd + 1
            p = p._next_visible
        }
   }
    fun reparse() {
        updateTextFormatting(mEntry._paragraph_1st, mEntry._paragraph_last)
    }

    override fun onInquireAction(id: Int, text: String) {
        TODO("Not yet implemented")
    }

//        fun reset(bgn: Int, end: Int){
//            super.reset(bgn, end)
//
//            // COMPLETELY CLEAR THE PARSING REGION
//            // TODO: only remove spans within the parsing boundaries...
//            // mEditText.getText().clearSpans(); <-- problematic!!
//            for(span in mSpans)
//                mHost.mEditText.text.removeSpan(span)
//        }

        // APPLIERS ====================================================================================
//        private fun applyCheck(tag_box: Any, tag: Any?) {
//            val posBgn = m_pos_cur - 3
//            val posBox = m_pos_cur
//            var posEnd = mHost.mEditText.text.toString().indexOf('\n', m_pos_cur)
//            if(posEnd == -1)
//                posEnd = mHost.mEditText.text.length
//            if(Diary.d.is_in_edit_mode)
//                addSpan(LinkCheck(mHost), posBgn, posBox, Spanned.SPAN_INTERMEDIATE)
//            addSpan(tag_box, posBgn, posBox, Spanned.SPAN_INTERMEDIATE)
//            addSpan(TypefaceSpan("monospace"), posBgn, posBox, 0)
//            if(tag != null)
//                addSpan(tag, posBox + 1, posEnd, 0) // ++ to skip separating space char
//        }

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
    }

    // SPANS =======================================================================================
    interface AdvancedSpan {
        val type: Char
    }

    class SpanOther : AdvancedSpan {
        override val type: Char
            get() = 'O'
    }

    class SpanNull : AdvancedSpan {
        override val type: Char
            get() = ' '
    }
    private class LinkDate(private val mDate: Long) : ClickableSpan(), AdvancedSpan {
        override fun onClick(widget: View) {
            Log.d( Lifeograph.TAG, "Clicked on Date link")
            val dm = Diary.getMain()
            var entry = dm.get_entry_by_date(mDate)
            if(entry == null)
                entry = dm.create_entry(mDate, "")
            Lifeograph.showElem(entry!!)
        }

        override val type: Char
            get() = 'd'
    }

    private class LinkUri(private val mUri: String) : ClickableSpan(), AdvancedSpan {
        override fun onClick(widget: View) {
            Log.d( Lifeograph.TAG, "Clicked on Uri link")
            val browserIntent = Intent(Intent.ACTION_VIEW, mUri.toUri())
            Lifeograph.mActivityMain.startActivity(browserIntent)
        }

        override val type: Char
            get() = 'u'
    }

    private class LinkID(private val mId: Int) : ClickableSpan(), AdvancedSpan {
        override fun onClick(widget: View) {
            Log.d( Lifeograph.TAG, "Clicked on ID link")
            val elem = Diary.getMain().get_element(mId)
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
