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
import android.widget.EditText
import android.widget.TextView
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

    private lateinit var mEditText: EditText
    private lateinit var mButtonHighlight: Button
    var                  mFlagSetTextOperation = false
    var                  mFlagBlockFormatter = false
    var                  mFlagEntryChanged = false
    private var          mFlagDismissOnExit = false
    var                  mFlagSearchIsOpen = false
    private val          mBrowsingHistory = ArrayList<Int>()

    companion object {
        lateinit var mEntry: Entry
        const val INDENT_UNIT_WIDTH = 60
    }

    // METHODS =====================================================================================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            override fun afterTextChanged(s: Editable) {
                if(!mFlagBlockFormatter)
                    updateTextFormatting(mEntry._paragraph_1st, mEntry._paragraph_last)
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if(!mFlagSetTextOperation && !mFlagBlockFormatter) {
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
        mButtonBold.setOnClickListener { toggleFormat('B') }

        val mButtonItalic = view.findViewById<Button>(R.id.buttonItalic)
        mButtonItalic.setOnClickListener { toggleFormat('I') }

        val mButtonUnderline = view.findViewById<Button>(R.id.buttonUnderline)
        val spanUnderline = SpannableString(getString(R.string.underline))
        spanUnderline.setSpan(UnderlineSpan(), 0, 1, 0)
        mButtonUnderline.text = spanUnderline
        mButtonUnderline.setOnClickListener { toggleFormat('U') }

        val mButtonStrikethrough = view.findViewById<Button>(R.id.buttonStrikethrough)
        val spanStringS = SpannableString(getString(R.string.strikethrough))
        spanStringS.setSpan(StrikethroughSpan(), 0, 1, 0)
        mButtonStrikethrough.text = spanStringS
        mButtonStrikethrough.setOnClickListener { toggleFormat('S') }

        mButtonHighlight = view.findViewById(R.id.buttonHighlight)
        mButtonHighlight.setOnClickListener { toggleFormat('H') }

        val mButtonPara = view.findViewById<Button>(R.id.button_para)
        mButtonPara.setOnClickListener { showParaDlg() }

        val mButtonComment = view.findViewById<Button>(R.id.button_comment)
        mButtonComment.setOnClickListener { addComment() }

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
        val dm = Diary.getMain()
        if(mFlagDismissOnExit) dm.dismiss_entry(mEntry) else sync()
        if(dm.is_in_edit_mode)
            Diary.getMain().writeLock(context)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

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
            mEditText.setText(mEntry._text_visible)
        mFlagSetTextOperation = false

        // if( flagParse )
        // parse();
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

    private fun showParaDlg() {
        DialogParagraph(requireContext(), object : DialogParagraph.Listener {
            override fun onApplyParaAction(action: (Paragraph) -> Unit) {
                doForEachSelPara(action, false)

                mEntry.update_todo_status()
                updateIcons()
                reparse()
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

            updateTextFormatting(paraBgn, paraEnd)
        }
    }

    private fun doForEachSelPara(action: (Paragraph) -> Unit, fRecursive: Boolean) {
        val selectionStart = mEditText.selectionStart
        val selectionEnd = mEditText.selectionEnd

        // Get the start and end paragraphs based on the selection offsets
        val paraBgn: Paragraph = mEntry.get_paragraph(selectionStart, true)
        val paraEnd: Paragraph =
            mEntry.get_paragraph(selectionEnd.coerceAtLeast(selectionStart), true)

        var p: Paragraph? = paraBgn
        while(p != null) {
            // Execute the lambda passed as an argument
            action(p)

            if(p === paraEnd) break
            p = p.get_next()
        }

        if(fRecursive) {
            // TODO: 2.1 or later
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

    private fun toggleQuotation() {
        doForEachSelPara({ para ->
                 if(para._quot_type == '_') // off
                     para._quot_type = '*' // generic
                 else
                     para._quot_type = '_'
             }, false)
    }

    // PARSING =====================================================================================
    private fun processParagraph(p: Paragraph, rawOffset: Int, rawOffsetEnd: Int) {
        val textLength = mEditText.text.length
        val offset = rawOffset.coerceIn(0, textLength)
        val offsetEnd = rawOffsetEnd.coerceIn(0, textLength)

        if (offset >= offsetEnd) return

        val theme = mEntry._theme

        // remove existing spans in this paragraph's range before re-applying
        val spans = mEditText.text.getSpans(offset, offsetEnd, Any::class.java)
        for(span in spans) {
            mEditText.text.removeSpan(span)
        }

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
            val listType = p._list_type
            if (listType != 0.toChar()) {
                // For numbered lists, you might need to get the actual order/number from the paragraph
                val label = if ("1AaRr".contains(listType)) p._list_order_str else null

                mEditText.text.setSpan(
                    ListSpan(requireContext(), p, label, theme),
                    offset,
                    offsetEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                      )
            }

            when(p._list_type) {
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

            if(p.is_code) {
                mEditText.text.setSpan(TypefaceSpan("monospace"), offset, offsetEnd, 0)
            } else if(p.is_quote) {
                mEditText.text.setSpan(QuoteSpan(), offset, offsetEnd, 0)
                mEditText.text.setSpan(StyleSpan(Typeface.ITALIC), offset, offsetEnd, 0)
            }

            // 4. INDENTATION
            val indentLevel = p._indent_level
            if(indentLevel > 0) {
                val margin = indentLevel * INDENT_UNIT_WIDTH
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
                'C' -> {
                    mEditText.text.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.subscriptSpan),
                        fStart, fEnd, 0 ) // general span
                }
                'P' -> {
                    mEditText.text.setSpan(
                        TextAppearanceSpan(requireContext(), R.style.superscriptSpan),
                        fStart, fEnd, 0 ) // general span
                }
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

            if(p._id == paraEnd._id) { break }

            // Move to next paragraph
            offset = offsetEnd + 1
            p = p._next_visible
        }

        // to force the UI to refresh on format toggles without losing cursor position:
        if( mFlagSetTextOperation ) return
        val selectionStart = mEditText.selectionStart
        val selectionEnd = mEditText.selectionEnd
        // nudge the EditText to re-draw spans
        mFlagBlockFormatter = true
        mEditText.setText(mEditText.text, TextView.BufferType.EDITABLE)
        // restore selection
        if( selectionStart >= 0 && selectionEnd >= 0 ) {
            mEditText.setSelection(selectionStart, selectionEnd)
        }
        mFlagBlockFormatter = false

   }
    fun reparse() {
        updateTextFormatting(mEntry._paragraph_1st, mEntry._paragraph_last)
    }

    override fun onInquireAction(id: Int, text: String) {
        TODO("Not yet implemented")
    }

    // PARSING HELPER FUNCTIONS ====================================================================
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

//    private class LinkCheck(val mHost: FragmentEntry) :
//        ClickableSpan(), AdvancedSpan {
//        override fun onClick(widget: View) {
//            mHost.showStatusPickerDlg()
//        }
//
//        override val type: Char
//            get() = 'c'
//    }

    private class ListSpan(
        private val context: Context,
        private val para: Paragraph,
        private val label: String?, // For numbered lists
        private val theme: Theme,
        private val gapWidth: Int = 40
                          ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int = gapWidth

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, layout: Layout
                                      ) {
            if(!first) return // Only draw on the first line of the paragraph

            val oldColor = p.color
            val style = p.style

            // Adjust x for direction
            val drawX = (x + dir + para._indent_level * FragmentEntry.INDENT_UNIT_WIDTH).toFloat()
            val centerY = (top + bottom) / 2f
            val size = p.textSize * 0.6f

            when(val type = para._list_type) {
                '-' -> { // Bullet
                    p.color = theme._color_text
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
//                                '+' -> theme._color_done
//                                'O' -> theme._color_open
//                                else -> theme._color_text
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
//                            '+' -> theme._color_done
//                            'O' -> theme._color_open
//                            else -> theme._color_text
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
                    p.color = theme._color_text
                    label?.let { c.drawText(it, drawX, baseline.toFloat(), p) }
                }
            }

            p.color = oldColor
            p.style = style
        }
    }