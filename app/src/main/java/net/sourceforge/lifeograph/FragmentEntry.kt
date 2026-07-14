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

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.*
import android.text.style.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.MenuItemCompat
import net.sourceforge.lifeograph.ToDoAction.ToDoObject
import net.sourceforge.lifeograph.helpers.STR
import java.util.*

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

    private lateinit var mEditText: EditTextEntry
    private lateinit var mButtonHighlight: Button
    var                  mFlagSetTextOperation = false
    var                  mFlagBlockFormatter = false
    private var          mFlagDismissOnExit = false
    var                  mFlagSearchIsOpen = false
    private var          mFRestartSearch = false
    private val          mBrowsingHistory = ArrayList<Int>()
    private var          mFlagAdjustingSelection = false

    companion object {
        lateinit var mEntry: Entry
        const val INDENT_UNIT_WIDTH = 80

        const val CHAR_EMPTY_PARA_ANCHOR = '\u200B' // zero-width space, view-only
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Lifeograph.updateScreenSizes( this );

        mEditText = view.findViewById(R.id.editTextEntry)
        if(!Diary.main.is_in_edit_mode()) {
            // allows the user to move the cursor and select text,
            // but prevents the keyboard from popping up and changing text
            mEditText.showSoftInputOnFocus = false
            mEditText.isFocusable = true
            mEditText.isFocusableInTouchMode = true
            mEditText.setTextIsSelectable(true)
            mEditText.keyListener = null

            view.findViewById<View>(R.id.toolbar_text_edit).visibility = View.GONE
        }
        if(Lifeograph.screenHeight >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI)
            mEditText.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

        // set custom font as the default font may lack the necessary chars such as check marks:
        /*Typeface font = Typeface.createFromAsset( getAssets(), "OpenSans-Regular.ttf" );
        mEditText.setTypeface( font );*/
        mEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if(!mFlagBlockFormatter)
                    updateTextFormatting(s, mEntry._paragraph_1st, mEntry._paragraph_last)
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if(!mFlagSetTextOperation && !mFlagBlockFormatter) {
                    if( before > 0 ) { // deletion
                        mEntry.erase_text(start, start + before)
                    }

                    if( count > 0 ) { // insertion
                        val addedText = s.subSequence(start, start + count).toString()
                        mEntry.insert_text(start, addedText)
                        // this method always inherits on Andro, and it may be an issue on paste
                    }
                }
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

        // keep the cursor before the dummy invisible last char:
        mEditText.mOnSelectionChanged = { selStart, selEnd -> handleSelectionChanged(selStart, selEnd) }

        // the below method is much more reliable for links than LinkMovementMethod
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

        mEditText.setOnTouchListener { v, event ->
            if(event.action == MotionEvent.ACTION_UP) {
                val x = event.x.toInt()
                val y = event.y.toInt()

                if(x < 100) { // TODO: adjust threshold based on your gapWidth + padding
                    val layout = mEditText.layout
                    if (layout != null) {
                        val line = mEditText.layout.getLineForVertical(y)
                        val offset = mEditText.layout.getLineStart(line)

                        // Find the paragraph at this offset
                        val para = mEntry.get_paragraph(offset, true)

                        if(para != null && para.is_foldable) {
                            v.performClick()
                            para.is_expanded = !para.is_expanded
                            show(true)
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false
        }

        val buttonBold = view.findViewById<Button>(R.id.buttonBold)
        buttonBold.setOnClickListener { toggleFormat('B') }

        val buttonItalic = view.findViewById<Button>(R.id.buttonItalic)
        buttonItalic.setOnClickListener { toggleFormat('I') }

        val buttonUnderline = view.findViewById<Button>(R.id.buttonUnderline)
        val spanUnderline = SpannableString(getString(R.string.underline))
        spanUnderline.setSpan(UnderlineSpan(), 0, 1, 0)
        buttonUnderline.text = spanUnderline
        buttonUnderline.setOnClickListener { toggleFormat('U') }

        val buttonStrikethrough = view.findViewById<Button>(R.id.buttonStrikethrough)
        val spanStringS = SpannableString(getString(R.string.strikethrough))
        spanStringS.setSpan(StrikethroughSpan(), 0, 1, 0)
        buttonStrikethrough.text = spanStringS
        buttonStrikethrough.setOnClickListener { toggleFormat('S') }

        mButtonHighlight = view.findViewById(R.id.buttonHighlight)
        mButtonHighlight.setOnClickListener { toggleFormat('H') }

        val buttonFaded = view.findViewById<Button>(R.id.buttonFaded)
        buttonFaded.setOnClickListener { toggleFormat('F') }


        val mButtonPara = view.findViewById<Button>(R.id.button_para)
        mButtonPara.setOnClickListener { showParaDlg() }

        val mButtonComment = view.findViewById<Button>(R.id.button_comment)
        mButtonComment.setOnClickListener { addComment() }

        if(mEntry._size > 0) {
            requireActivity().window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }
        show(savedInstanceState == null) // non-null on rotation
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "ActivityEntry.onPause()" );
    }*/

    override fun onStop() {
        super.onStop()
        Log.d(Lifeograph.TAG, "ActivityEntry.onStop()")
        val dm = Diary.main
        if(mFlagDismissOnExit) dm.dismiss_entry(mEntry)
        if(dm.is_in_edit_mode())
            Diary.main.writeLock(requireContext())
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        var item = menu.findItem(R.id.search_text)
        val searchView = item.actionView as SearchView
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                searchView.setQuery(Diary.main.get_search_str(), false)
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
                if(mFlagSearchIsOpen) handleSearchTextChanged(s)
                return true
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _: View?, b: Boolean -> mFlagSearchIsOpen = b }

        item = menu.findItem(R.id.change_todo_status)
        val toDoAction = MenuItemCompat.getActionProvider(item) as ToDoAction
        toDoAction.mObject = this

        updateIcons()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
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
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun updateMenuVisibilities() {
        super.updateMenuVisibilities()

        val dm = Diary.main
        val flagWritable = dm.is_in_edit_mode()
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

        mEditText.showSoftInputOnFocus = true // Restore keyboard behavior
        mEditText.keyListener = android.text.method.TextKeyListener.getInstance()
        mEditText.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        mEditText.isFocusable = true
        mEditText.isFocusableInTouchMode = true

        requireActivity().findViewById<View>(R.id.toolbar_text_edit).visibility = View.VISIBLE
        reparse()
    }

    /**
     * The trailing CHAR_EMPTY_PARA_ANCHOR (see ensureTrailingEmptyParaAnchor) is a view-only
     * character with no counterpart in the core model. If the cursor is ever allowed to sit
     * at text.length() (i.e. past the anchor), any call site that forwards
     * mEditText.selectionStart/End straight to mEntry.get_paragraph() etc. passes an offset
     * one past the core text's valid range, and the C++ core throws.
     *
     * Mirrors the Desktop guard: whenever the cursor would land on/after the anchor, snap it
     * back to just before the anchor instead.
     */
    private fun handleSelectionChanged(selStart: Int, selEnd: Int) {
        if (mFlagAdjustingSelection || mFlagSetTextOperation) return

        val text = mEditText.text ?: return
        val len = text.length
        if (len == 0 || text[len - 1] != CHAR_EMPTY_PARA_ANCHOR) return

        val maxPos = len - 1 // position immediately before the anchor == valid end-of-text for core
        val newStart = selStart.coerceAtMost(maxPos)
        val newEnd = selEnd.coerceAtMost(maxPos)

        if (newStart != selStart || newEnd != selEnd) {
            mFlagAdjustingSelection = true
            mEditText.setSelection(newStart, newEnd)
            mFlagAdjustingSelection = false
        }
    }

    override fun handleBack(): Boolean {
        if(!mBrowsingHistory.isEmpty())
            mBrowsingHistory.removeAt(mBrowsingHistory.lastIndex)
        if(mBrowsingHistory.isEmpty()) {
            return false
        }
        else {
            val entry = Diary.main.get_entry_by_id(mBrowsingHistory.last())
            if(entry != null) {
                mEntry = entry
                show(true)
            }
        }
        return true
    }

    private fun updateIcons() {
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
                when(mEntry._todo_status) {
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
        mEditText.setBackgroundColor(theme.color_base)
        mEditText.setTextColor(theme.color_text)
        mButtonHighlight.setTextColor(theme.color_text)
        val spanStringH = SpannableString("H")
        spanStringH.setSpan(BackgroundColorSpan(theme.color_highlight), 0, 1, 0)
        mButtonHighlight.text = spanStringH
    }

    fun show(fReset: Boolean) {
        mFlagDismissOnExit = false

        // THEME
        updateTheme()

        // SETTING TEXT
        mFlagSetTextOperation = true
        if(fReset)
            mEditText.setText(mEntry._text_visible)
        mFlagSetTextOperation = false

        Lifeograph.getActionBar().subtitle = mEntry._name // TODO: _title_str
        //invalidateOptionsMenu(); // may be redundant here

        // BROWSING HISTORY
        if(mBrowsingHistory.isEmpty() || mEntry._id != mBrowsingHistory.last()) // not going back
            mBrowsingHistory.add(mEntry._id)
    }

    private fun toggleFavorite() {
        mEntry.toggle_favorite()
        updateIcons()
    }

    private fun dismiss() {
        Lifeograph.showConfirmationPrompt(
            requireContext(),
            R.string.entry_dismiss_confirm,
            R.string.dismiss
        ) { _: DialogInterface?, _: Int -> mFlagDismissOnExit = true }
    }

    private fun handleSearchTextChanged(text: String) {
        val searchStr = text.lowercase(Locale.ROOT)
        val dm = Diary.main
        dm.set_search_str(searchStr)
        if( dm.is_search_in_progress()) {
            mFRestartSearch = true
            dm.stop_search()
        }
        else {
            dm.start_search()
        }
    }

    fun handleSearchFinished() {
        if( mFRestartSearch ) {
            mFRestartSearch = false
            Diary.main.start_search()
        }
        else {
            reparse()
        }
    }

//    fun showStatusPickerDlg() {
//        DialogPicker(requireContext(),
//                     object: DialogPicker.Listener{
//                               override fun onItemClick(item: RViewAdapterBasic.Item) {
//                                   setListItemType(item.mId[0])
//                               }
//
//                               override fun populateItems(list: RVBasicList) {
//                                   list.clear()
//
//                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.bullet),
//                                                                   "*",
//                                                                   R.drawable.ic_bullet))
//
//                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_open),
//                                                                   " ",
//                                                                   R.drawable.ic_todo_open))
//                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_progressed),
//                                                                   "~",
//                                                                   R.drawable.ic_todo_progressed))
//                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_done),
//                                                                   "+",
//                                                                   R.drawable.ic_todo_done))
//                                   list.add(RViewAdapterBasic.Item(Lifeograph.getStr(R.string.todo_canceled),
//                                                                   "x",
//                                                                   R.drawable.ic_todo_canceled))
//                               }
//                           }).show()
//    }

    private fun showThemePickerDlg() {
        DialogPicker(requireContext(),
                     object: DialogPicker.Listener{
                         override fun onItemClick(item: RViewAdapterBasic.Item) {
                             val theme = Diary.main.get_theme(item.mId)
                             mEntry._theme = theme
                             updateTheme()
                             reparse()
                         }

                         override fun populateItems(list: RVBasicList) {
                             list.clear()

                             for(theme in Diary.main.get_themes())
                                 list.add(RViewAdapterBasic.Item(theme._name,
                                                                 theme._name,
                                                                 R.drawable.ic_theme))
                         }
                     }).show()
    }

    private fun showParaDlg() {
        DialogParagraph(requireContext(), object : DialogParagraph.Listener {
            override fun onApplyParaAction(action: (Paragraph) -> Unit, fRefreshFully: Boolean) {
                doForEachSelPara(action, false)

                mEntry.update_todo_status()
                updateIcons()
                if( fRefreshFully )
                    show(true)
                else
                    reparse()
            }
            override fun getParagraph(): Paragraph {
                val selectionStart = mEditText.selectionStart
                val para: Paragraph = mEntry.get_paragraph(selectionStart, true)
                return para
            }
        }).show()
    }

    override fun setTodoStatus(s: Int) {
        mEntry._todo_status = s
        updateIcons()
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

    // FORMATTING BUTTONS ==========================================================================
    private fun calculateTokenBounds(bounds: IntArray, type: Char, fCategorical: Boolean = false): HiddenFormat? {
        if(mEditText.hasSelection()) {
            bounds[0] = mEditText.selectionStart
            bounds[1] = mEditText.selectionEnd
            return null
        }
        else// if( m_para_sel_bgn )
        {
            val posCursor = mEditText.selectionStart
            val paraBgn = mEntry.get_paragraph(posCursor, true)
            val posParaBgn = paraBgn._bgn_offset_in_host
            val posCursorInParaBgn = posCursor - posParaBgn
            val format = if( fCategorical ) paraBgn.get_format_oneof_at(type, posCursorInParaBgn)
                         else paraBgn.get_format_at(type, posCursorInParaBgn)
            if( format != null )
            {
                val posParaBgn = paraBgn._bgn_offset_in_host
                bounds[0] = posParaBgn + format.posBgn
                bounds[1] = posParaBgn + format.posEnd
                return format
            }
        }

        // if no selection and no existing tag:
        val str = mEditText.text.toString()
        var bgn = mEditText.selectionStart
        var end = mEditText.selectionStart

        // find word boundaries:
        // backward_find_char logic
        while(bgn > 0) {
            val charBefore = str[bgn - 1]
            val shouldStop = if(type == 'T') !STR.is_char_name(charBefore)
                             else STR.is_char_space(charBefore)
            if(shouldStop) break
            bgn--
        }
        // forward_find_char logic
        while(end < str.length) {
            val charAfter = str[end]
            val shouldStop = if(type == 'T') !STR.is_char_name(charAfter)
                             else STR.is_char_space(charAfter)
            if(shouldStop) break
            end++
        }

        // if omitting punctuation did not end well:
        if(type == 'T' && bgn == end) {
            bgn = mEditText.selectionStart
            while(bgn > 0 && !STR.is_char_space(str[bgn - 1])) {
                bgn--
            }

            end = mEditText.selectionStart
            while(end < str.length && !STR.is_char_space(str[end])) {
                end++
            }
        }

        bounds[0] = bgn
        bounds[1] = end

        return null
    }

    private fun toggleFormat(type: Char, fCheckOnly: Boolean = false) {
        val bounds = intArrayOf(0, 0)
        calculateTokenBounds( bounds, type )

        val paraBgn = mEntry.get_paragraph(bounds[0], true)
        val paraEnd = mEntry.get_paragraph(bounds[1], true)
        val posBgn  = bounds[0] - paraBgn._bgn_offset_in_host
        val posEnd  = bounds[1] - paraEnd._bgn_offset_in_host
        val fAlready : Boolean = paraBgn.get_format_at(type, posBgn) != null

        if( !fCheckOnly ) {
            var p: Paragraph? = paraBgn
            while( p != null ) {
                val pid = p._id
                val startPos = if (pid == paraBgn._id) posBgn else 0
                val endPos = if (pid == paraEnd._id) posEnd else p._size

                p.toggle_format( type, startPos, endPos, fAlready )

                if(pid == paraEnd._id) break
                p = p.get_next()
            }

            mEditText.text?.let { updateTextFormatting(it, paraBgn, paraEnd) }
        }
    }

    private fun doForEachSelPara(action: (Paragraph) -> Unit, fRecursive: Boolean) {
        val selectionStart = mEditText.selectionStart
        val selectionEnd = mEditText.selectionEnd

        // get the start and end paragraphs based on the selection offsets
        val paraBgn: Paragraph = mEntry.get_paragraph(selectionStart, true)
        val paraEnd: Paragraph =
            mEntry.get_paragraph(selectionEnd.coerceAtLeast(selectionStart), true)

        var p: Paragraph? = paraBgn
        while(p != null) {
            // execute the lambda passed as an argument
            action(p)

            if(p._id == paraEnd._id) break
            p = p.get_next()
        }

//        if(fRecursive) {
//            // TODO: 2.1 or later
//        }
    }

    private fun addComment() {
        val pStart: Int = mEditText.selectionStart

        if(pStart>=0)
            return
        mEditText.text?.let { edt ->
            if(mEditText.hasSelection()) {
                val pEnd: Int = mEditText.selectionEnd - 1
                edt.insert(pStart, "[[")
                edt.insert(pEnd + 2, "]]")
            } else { // no selection case
                edt.insert(pStart, "[[]]")
                mEditText.setSelection(pStart + 2)
            }
        }
    }

    private fun toggleQuotation() {
        doForEachSelPara({ para ->
                             if(para._quot_type == '_') // off
                                 para._quot_type = '*' // generic
                             else
                                 para._quot_type = '_'
                         }, false)
    }

    // PARSING =====================================================================================
    private fun processParagraph(edt: Editable, p: Paragraph, offset: Int, offsetEnd: Int) {
        val textLength = edt.length
        // We use offsetEnd for paragraph-level styles to keep them strictly within paragraph boundaries.
        // For an empty paragraph, offset == offsetEnd, which is still a valid (zero-length) range.
        val styleEnd = offsetEnd.coerceAtMost(textLength)
        val styleStart = offset.coerceAtMost(styleEnd)

        val isAnchoredTrailingEmptyPara = p._size == 0 &&
                p._id == mEntry._paragraph_last._id &&
                styleEnd < textLength && edt[styleEnd] == CHAR_EMPTY_PARA_ANCHOR
        val marginEnd = if (isAnchoredTrailingEmptyPara) styleEnd + 1 else styleEnd

        val theme = mEntry._theme

        // 1. ALIGNMENT
        val alignment = when(p._alignment) {
            '<' -> Layout.Alignment.ALIGN_NORMAL
            '|' -> Layout.Alignment.ALIGN_CENTER
            '>' -> Layout.Alignment.ALIGN_OPPOSITE
            else -> null
        }
        alignment?.let {
            edt.setSpan(AlignmentSpan.Standard(it), styleStart, marginEnd, Spanned
                .SPAN_INCLUSIVE_INCLUSIVE)
        }

        // 2. HEADING TYPE
        when(p._heading_level) {
            'T' -> { // TITLE
                edt.setSpan(TextAppearanceSpan(requireContext(), R.style.headingSpan),
                            styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                edt.setSpan(ForegroundColorSpan(mEntry._theme.color_title), styleStart,
                            marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                // TODO: handle_title_edited logic would go here if needed for Android UI
            }

            'S' -> { // LARGE
                edt.setSpan(TextAppearanceSpan(requireContext(), R.style.subheadingSpan),
                            styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                edt.setSpan(ForegroundColorSpan(mEntry._theme.color_heading_L), styleStart,
                            marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            'B' -> { // MEDIUM
                edt.setSpan(StyleSpan(Typeface.BOLD), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }

        // 3. LIST ITEM TYPE & SPECIAL STYLES (Exempt for title)
        if(!p.is_title) {
            val listType = p._list_type
            if (listType != 0.toChar()) {
                // For numbered lists, you might need to get the actual order/number from the paragraph
                val label = if ("1AaRr".contains(listType)) p._list_order_str else null

                edt.setSpan(
                    SpanList(requireContext(), p, label, theme),
                    styleStart,
                    marginEnd,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE
                           )
            }

            when(listType) {
                'O' -> { // open to-do
                    edt.setSpan(ForegroundColorSpan(theme.color_open), styleStart,
                                marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }

//                    '~' -> { // in progress: no special format
//                    }

                '+' -> { // done
                    edt.setSpan(ForegroundColorSpan(theme.color_done), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    edt.setSpan(BackgroundColorSpan(theme.color_done_bg), styleStart,
                                marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }

                'X' -> { // canceled
                    edt.setSpan(StrikethroughSpan(), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }

            if(p.is_code) {
                edt.setSpan(TypefaceSpan("monospace"), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else if(p.is_quote) {
                edt.setSpan(QuoteSpan(), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                edt.setSpan(StyleSpan(Typeface.ITALIC), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // 4. INDENTATION
            val indentLevel = p._indent_level
            if(indentLevel > 0 && listType == 0.toChar()) {
                val margin = indentLevel * INDENT_UNIT_WIDTH
                edt.setSpan(LeadingMarginSpan.Standard(margin), styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }

        // 5. FOLDING
        if( p.is_foldable ) { //&& !p.is_expanded )
            val spanFolding = SpanFolding(
                p.is_expanded,
                if (p._heading_level == 'S') theme.color_heading_L else theme.color_heading_M,
                p._indent_level
                                         )

            // We use a specific range (e.g., the first char or a hidden char) to attach the span
            edt.setSpan(spanFolding, styleStart, marginEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        // 6. HIDDEN FORMATS (Inline Spans)
        for(format in p._formats) {
            val fStart = offset + format.posBgn
            val fEnd = offset + format.posEnd

            // Ensure boundaries are valid
            if(fStart < 0 || fEnd > textLength || fStart > fEnd) continue

            val flags = if (fStart == fEnd) Spanned.SPAN_INCLUSIVE_INCLUSIVE else 0
            val colorMid = theme.color_mid

            when(format.type) {
                'B' -> edt.setSpan(StyleSpan(Typeface.BOLD), fStart, fEnd, flags)
                'I' -> edt.setSpan(StyleSpan(Typeface.ITALIC), fStart, fEnd, flags)
                'H' -> edt.setSpan(BackgroundColorSpan(theme.color_highlight), fStart, fEnd, flags)
                'S' -> edt.setSpan(StrikethroughSpan(), fStart, fEnd, flags)
                'U' -> edt.setSpan(UnderlineSpan(), fStart, fEnd, flags)
                'F' -> edt.setSpan(ForegroundColorSpan(colorMid), fStart, fEnd, flags)
                'C' -> {
                    edt.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.subscriptSpan),
                        fStart, fEnd, flags ) // general span
                }
                'P' -> {
                    edt.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.superscriptSpan),
                        fStart, fEnd, flags ) // general span
                }
                'T' -> { // TAG
                    edt.setSpan(
                        BackgroundColorSpan(theme.color_inline_tag), fStart, fEnd, flags)
                    edt.setSpan(
                        LinkID(format.refId.toInt()), fStart, fEnd, flags)
                }
                'L' -> { // Link: URI
                    edt.setSpan( LinkUri(format.uri), fStart, fEnd, flags)
                    // You already have LinkUri class defined in ParserEditText
                    // edt.setSpan(LinkUri(format.uri), fStart, fEnd, flags)
                }
                'D' -> { // Link: ID
                    val element = Diary.main.get_tag_by_id(format.refId.toInt())
                    val span = if(element != null)
                            LinkID(format.refId.toInt())
                        else  // indicate dead links
                            ForegroundColorSpan(Color.RED)
                    edt.setSpan(span, fStart, fEnd, flags)
                }
                'v' -> { // TAG VALUE
                    edt.setSpan(
                        BackgroundColorSpan(theme.color_inline_tag), fStart, fEnd, flags)
                }
                'c' -> { // COMMENT / MARKUP
                    edt.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.commentSpan),
                        fStart, fEnd, flags ) // general span
                    edt.setSpan( ForegroundColorSpan(colorMid), fStart,
                                 fStart + 2, flags ) // [[
                    if(format.posBgn < format.posEnd - 4) {
                        edt.setSpan(
                            ForegroundColorSpan(colorMid), fStart + 2, fEnd - 2, flags)
                    }
                    edt.setSpan(ForegroundColorSpan(colorMid), fEnd - 2, fEnd, flags) // ]]
                }
                'd' -> { // DATE
                    edt.setSpan(LinkDate(format.varD), fStart, fEnd, flags)
                }
                'm' -> { // MATCH
                    edt.setSpan(
                        BackgroundColorSpan(theme.color_match_bg),
                        fStart, fEnd, Spanned.SPAN_INTERMEDIATE or flags )
                    edt.setSpan(
                        ForegroundColorSpan(theme.color_base),
                        fStart, fEnd, Spanned.SPAN_INTERMEDIATE or flags )
                }
                'k' -> { // KEYWORD
                    edt.setSpan(StyleSpan(Typeface.BOLD), fStart, fEnd, flags)
                    edt.setSpan(ForegroundColorSpan(theme.color_title), fStart, fEnd, flags)
                }
                '#' -> { // CODE COMMENT
                    edt.setSpan(ForegroundColorSpan(colorMid), fStart, fEnd, flags)
                }
                'g' -> { // CODE STRING
                    edt.setSpan(ForegroundColorSpan(theme.color_match_bg), fStart, fEnd, flags)
                }
                // Add other types (Date, ID, etc.) based on your ParserEditText.AdvancedSpan types
            }
        }
    }

    private fun updateTextFormatting(edt: Editable, paraBgn: Paragraph, paraEnd: Paragraph) {
        ensureTrailingEmptyParaAnchor(edt)

        val startOffset = paraBgn._bgn_offset_in_host
        var endOffset = paraEnd._bgn_offset_in_host + paraEnd._size
        if (endOffset < edt.length && (edt[endOffset] == '\n' || edt[endOffset] == CHAR_EMPTY_PARA_ANCHOR))
            endOffset++

        // 1. Clear all relevant spans in the entire range first to avoid boundary overlaps
        val classesToRemove = arrayOf(
            CharacterStyle::class.java,
            ParagraphStyle::class.java,
            SpanList::class.java,
            AdvancedSpan::class.java
        )
        for (clazz in classesToRemove) {
            val spans = edt.getSpans(startOffset, endOffset, clazz)
            for (span in spans) {
                if (span is Selection) continue
                edt.removeSpan(span)
            }
        }

        // 2. Process each paragraph
        var offset = startOffset
        var p: Paragraph? = paraBgn
        while(p != null) {
            val pSize = p._size
            processParagraph(edt, p, offset, offset + pSize)

            if(p._id == paraEnd._id) break

            // Move to next paragraph
            offset += pSize + 1
            p = p._next_visible
        }

        // to force the UI to refresh on format toggles without losing cursor position:
        if( mFlagSetTextOperation ) return
        val selectionStart = mEditText.selectionStart
        val selectionEnd = mEditText.selectionEnd
        // restore selection
        if( selectionStart >= 0 && selectionEnd >= 0 ) {
            mEditText.setSelection(selectionStart, selectionEnd)
        }
    }
    fun reparse() {
        mEditText.text?.let {  updateTextFormatting(it,
                                                    mEntry._paragraph_1st,
                                                    mEntry._paragraph_last) }
    }

    override fun onInquireAction(id: Int, text: String) {
        TODO("Not yet implemented")
    }

    // PARSING HELPER FUNCTIONS ====================================================================
    /**
     * Android's Layout.getParagraphSpans() drops any ParagraphStyle span whose end offset
     * equals the start of an empty line (start == end). That's normally correct - it stops a
     * span from a preceding non-empty paragraph leaking onto a following blank one - but it
     * also silently drops our own list/fold margin span on a genuinely empty *last* paragraph,
     * since there's no character after it to give the span non-zero length.
     *
     * Fix: append one invisible anchor char after such a paragraph so its span range is never
     * zero-length. Added/removed under mFlagSetTextOperation + mFlagBlockFormatter so this is
     * never seen by the model or re-triggers formatting recursively.
     */
    private fun ensureTrailingEmptyParaAnchor(edt: Editable) {
        val last = mEntry._paragraph_last
        val needsAnchor = last._size == 0 &&
                (last._list_type != 0.toChar() || last.is_foldable)
        val hasAnchor = edt.isNotEmpty() && edt[edt.length - 1] == CHAR_EMPTY_PARA_ANCHOR

        if (needsAnchor == hasAnchor) return

        val prevBlock = mFlagBlockFormatter
        mFlagBlockFormatter = true   // suppress recursive updateTextFormatting()
        mFlagSetTextOperation = true // suppress model sync in onTextChanged()
        if (needsAnchor)
            edt.append(CHAR_EMPTY_PARA_ANCHOR)
        else
            edt.delete(edt.length - 1, edt.length)
        mFlagSetTextOperation = false
        mFlagBlockFormatter = prevBlock
    }

//    private fun startsLine(offset: Int): Boolean {
//        if(offset < 0 || offset >= mEditText.text.length)
//            return false
//        return offset == 0 || mEditText.text[offset - 1] == '\n'
//    }

//    private fun hasSpan(offset: Int, type: Char): AdvancedSpan {
//        val spans = mEditText.text.getSpans(offset, offset, Any::class.java)
//        var hasNoOtherSpan = true
//        for(span in spans) {
//            if(span is AdvancedSpan) {
//                hasNoOtherSpan = if(span.type == type) {
//                    return span
//                }
//                else false
//            }
//        }
//        return if(hasNoOtherSpan) SpanNull() else SpanOther()
//    }

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
            val dm = Diary.main
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
            Lifeograph.mActivityMain?.startActivity(browserIntent)
        }

        override val type: Char
            get() = 'u'
    }

    private class LinkID(private val mId: Int) : ClickableSpan(), AdvancedSpan {
        override fun onClick(widget: View) {
            Log.d( Lifeograph.TAG, "Clicked on ID link")
            val elem = Diary.main.get_element(mId)
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

//    private class LinkCheck(val mHost: FragmentEntry) :
//        ClickableSpan(), AdvancedSpan {
//        override fun onClick(widget: View) {
//            mHost.showStatusPickerDlg()
//        }
//
//        override val type: Char
//            get() = 'c'
//    }

    private class SpanList(
        private val context: Context,
        private val para: Paragraph,
        private val label: String?, // For numbered lists
        private val theme: Theme,
        private val gapWidth: Int = ( INDENT_UNIT_WIDTH * .7f ).toInt()
                          ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int =
            (para._indent_level * INDENT_UNIT_WIDTH) + gapWidth

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, layout: Layout
                                      ) {
            if(!first) return // Only draw on the first line of the paragraph

            val oldColor = p.color
            val style = p.style

            // x is the current margin start. If this is the only LeadingMarginSpan, x is 0.
            val drawX = (x + para._indent_level * INDENT_UNIT_WIDTH).toFloat()
            val centerY = (top + bottom) / 2f
            val size = p.textSize * 0.6f

            when(val type = para._list_type) {
                '-' -> { // Bullet
                    p.color = theme.color_text
                    c.drawCircle(drawX + gapWidth / 2f, centerY, size / 3f, p)
                }

                'O', '~', '+', 'X' -> { // Checkboxes
                    val resId = when(type) {
                        'O' -> R.drawable.ic_todo_open
                        '~' -> R.drawable.ic_todo_progressed
                        '+' -> R.drawable.ic_todo_done
                        'X' -> R.drawable.ic_todo_canceled
                        else -> 0
                    }

                    if(resId != 0) {
                        AppCompatResources.getDrawable(context, resId)?.let { drawable ->
//                            val color = when(type) {
//                                '+' -> theme.color_done
//                                'O' -> theme.color_open
//                                else -> theme.color_text
//                            }
//                            drawable.setTint(color)
                            val iconSize = (size * 1.3f).toInt()
                            val left = drawX.toInt()
                            val topIcon = (centerY - iconSize / 2f).toInt()
                            drawable.setBounds(left, topIcon, left + iconSize, topIcon + iconSize)
                            drawable.draw(c)
                        }
                    } else {
//                        p.color = when(type) {
//                            '+' -> theme.color_done
//                            'O' -> theme.color_open
//                            else -> theme.color_text
//                        }
//                        val rect = RectF(drawX, centerY - size / 2, drawX + size, centerY + size / 2)
//                        p.style = Paint.Style.STROKE
//                        c.drawRect(rect, p)
//
//                        // Draw internal mark if needed (e.g., 'x' or '+')
//                        if(type == 'x' || type == 'X' || type == '+') {
//                            c.drawLine(rect.left, rect.top, rect.right, rect.bottom, p)
//                            c.drawLine(rect.left, rect.bottom, rect.right, rect.top, p)
//                        } else if(type == '~') {
//                            c.drawLine(rect.left, centerY, rect.right, centerY, p)
//                        }
                    }
                }

                '1' -> { // Numbered list
                    p.color = theme.color_text
                    label?.let { c.drawText(it, drawX, baseline.toFloat(), p) }
                }
            }

            p.color = oldColor
            p.style = style
        }
    }

    private class SpanFolding(
        private val isExpanded: Boolean,
        private val color: Int,
        //private val indentLevel: Int,
        private val gapWidth: Int = 0
                             ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int = gapWidth

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, layout: Layout
                                      ) {
            if(!first) return // Only draw the arrow on the first line

            val oldColor = p.color
            val oldStyle = p.style

            p.color = color
            p.style = Paint.Style.FILL
            p.isAntiAlias = true

            // x is the current margin start, dir is layout direction
            val indentation = 0//indentLevel * INDENT_UNIT_WIDTH
            val drawX = x + dir + indentation + 20f - INDENT_UNIT_WIDTH
            val centerY = (top + bottom) / 2f
            val size = 32f

            val path = Path()
            if(isExpanded) {
                // Downward arrow
                path.moveTo(drawX - size / 2, centerY - size / 4)
                path.lineTo(drawX + size / 2, centerY - size / 4)
                path.lineTo(drawX, centerY + size / 4)
            } else {
                // Rightward arrow
                path.moveTo(drawX - size / 4, centerY - size / 2)
                path.lineTo(drawX - size / 4, centerY + size / 2)
                path.lineTo(drawX + size / 4, centerY)
            }
            path.close()
            c.drawPath(path, p)

            p.color = oldColor
            p.style = oldStyle
        }
    }


}
