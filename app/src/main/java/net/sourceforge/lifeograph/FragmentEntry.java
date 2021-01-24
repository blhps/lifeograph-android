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

package net.sourceforge.lifeograph;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import static net.sourceforge.lifeograph.DiaryElement.ES_NOT_TODO;

public class FragmentEntry extends Fragment
        implements ToDoAction.ToDoObject, DialogInquireText.InquireListener,
        PopupMenu.OnMenuItemClickListener, Lifeograph.DiaryEditor
{
    // CHAR FLAGS
    public final int CF_NOT_SET = 0;
    public final int CF_NOTHING = 0x1;
    public final int CF_NEWLINE = 0x2;
    public final int CF_PUNCTUATION_RAW = 0x4;
    public final int CF_SPACE = 0x8; // space that will come eventually
    public final int CF_TAB = 0x10;
    public final int CF_IMMEDIATE = 0x20; // indicates contiguity of chars

    public final int CF_ASTERISK = 0x40; // bold
    public final int CF_UNDERSCORE = 0x80; // italic
    public final int CF_EQUALS = 0x100; // strikethrough
    public final int CF_HASH = 0x400; // highlight
    public final int CF_MARKUP = CF_ASTERISK | CF_UNDERSCORE | CF_EQUALS | CF_HASH;

    public final int CF_SLASH = 0x800;
    public final int CF_ALPHA = 0x1000;
    public final int CF_NUMBER = 0x2000;
    public final int CF_ALHANUM = CF_ALPHA | CF_NUMBER;
    public final int CF_AT = 0x4000; // email
    public final int CF_SPELLCHECK = 0x8000;

    public final int CF_DOTYM = 0x10000;
    public final int CF_DOTMD = 0x20000;
    public final int CF_DOTDATE = CF_DOTMD | CF_DOTYM;

    public final int CF_LESS = 0x80000; // tagging
    public final int CF_MORE = 0x100000;
    public final int CF_SBB = 0x200000; // square bracket begin: comments
    public final int CF_SBE = 0x400000; // square bracket end: comments

    public final int CF_TODO = 0x1000000; // ~,+,x

    public final int CF_IGNORE = 0x40000000;
    public final int CF_EOT = 0x80000000; // End of Text

    public final int CF_ANY = 0xFFFFFFFF;

    public final int CF_PUNCTUATION = CF_PUNCTUATION_RAW | CF_SLASH | CF_DOTDATE | CF_LESS
                                      | CF_MORE | CF_SBB | CF_SBE;
    public final int CF_FORMATCHAR = CF_ASTERISK | CF_UNDERSCORE | CF_EQUALS | CF_HASH | CF_SBB
                                     | CF_SBE;
    public final int CF_NUM_SLSH = CF_NUMBER | CF_SLASH;
    public final int CF_NUM_CKBX = CF_NUMBER | CF_TODO;
    public final int CF_NONSPACE = CF_PUNCTUATION | CF_MARKUP | CF_ALPHA | CF_NUMBER | CF_AT
                                   | CF_TODO;
    public final int CF_NONTAB = CF_NONSPACE | CF_SPACE;

    public final int CF_SEPARATOR = CF_SPACE|CF_TAB | CF_NEWLINE;
    public final int CF_NOT_SEPARATOR = CF_ANY ^ CF_SEPARATOR;

    //public final int CF_ALPHASPELL = CF_ALPHA | CF_SPELLCHECK;

    // PARSER SELECTOR (NEEDED DUE TO LACK OF FUNCTION POINTERS IN JAVA)
    private enum ParSel {
        NULL, TR_SUBH, TR_BOLD, TR_ITLC, TR_STRK, TR_HILT, TR_CMNT, TR_LINK, TR_LNAT,
        TR_LNKD, TR_LIST, TR_IGNR,
        JK_DDMD, JK_DDYM, JK_IGNR, JK_LNHT, JK_LNDT, JK_LIST, JK_LST2,
        AP_BOLD, AP_CMNT, AP_HEND, AP_HILT, AP_ITLC, AP_LINK, AP_LNDT, AP_LNID, AP_STRK, AP_SUBH,
        AP_CUNF, AP_CPRG, AP_CFIN, AP_CCCL
    }

    private enum LinkStatus {
        LS_OK, LS_ENTRY_UNAVAILABLE, LS_INVALID, // separator: to check a valid entry link:
                                                 // linkstatus < LS_INVALID
        LS_CYCLIC, LS_FILE_OK, LS_FILE_INVALID, LS_FILE_UNAVAILABLE, LS_FILE_UNKNOWN
    }

    static Entry        mEntry            = null;
    private ActionBar   mActionBar        = null;
    private Menu        mMenu             = null;
    private EditText    mEditText         = null;
    private KeyListener mKeyListener;
    private Button      mButtonHighlight;

    boolean mFlagSetTextOperation = false;
    boolean mFlagEditorActionInProgress = false;
    boolean mFlagEntryChanged = false;
    boolean mFlagDismissOnExit = false;
    boolean mFlagSearchIsOpen = false;

    private int mColorMid;
    private int mColorMatchBG;
    private final float sMarkupScale = 0.7f;

    @Override
    public void
    onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );
    }

    @Override
    public View
    onCreateView( @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstState ) {
        return inflater.inflate( R.layout.entry, container, false );
    }

    @Override
    public void
    onViewCreated( @NonNull View view, Bundle savedInstanceState ) {
        //Lifeograph.updateScreenSizes( this );

        mActionBar = ( ( AppCompatActivity ) requireActivity() ).getSupportActionBar();
        //assert mActionBar != null;
        //mActionBar.setDisplayHomeAsUpEnabled( true );

        //mDrawerLayout = ( DrawerLayout ) findViewById( R.id.drawer_layout );

        mEditText = view.findViewById( R.id.editTextEntry );
        //mEditText.setMovementMethod( LinkMovementMethod.getInstance() );
        mKeyListener = mEditText.getKeyListener();

        if( ! Diary.diary.is_in_edit_mode() ) {
            //mEditText.setInputType( InputType.TYPE_NULL ); does not seem necessary, besides
            // disables text wrap
            mEditText.setTextIsSelectable( true );
            mEditText.setKeyListener( null );

            view.findViewById( R.id.toolbar_text_edit ).setVisibility( View.GONE );
        }

        if( Lifeograph.getScreenHeight() >= Lifeograph.MIN_HEIGHT_FOR_NO_EXTRACT_UI )
            mEditText.setImeOptions( EditorInfo.IME_FLAG_NO_EXTRACT_UI );

        // set custom font as the default font may lack the necessary chars such as check marks:
        /*Typeface font = Typeface.createFromAsset( getAssets(), "OpenSans-Regular.ttf" );
        mEditText.setTypeface( font );*/

        mEditText.addTextChangedListener( new TextWatcher() {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                // if( mFlagSetTextOperation == false )
                {
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
                    mFlagEntryChanged = true;
                }
                parse( 0, mEditText.getText().length() );
            }
        } );

        mEditText.setOnEditorActionListener( ( v, actionId, event ) -> {
            if( mFlagEditorActionInProgress ) {
                mFlagEditorActionInProgress = false;
                return false;
            }

            int iter_end = v.getSelectionStart();
            int iter_start = v.getText().toString().lastIndexOf( '\n', iter_end - 1 );
            if( iter_start < 0 || iter_start == v.getText().length() - 1 )
                return false;

            iter_start++;   // get rid of the new line char
            int offset_start = iter_start;   // save for future

            if( v.getText().charAt( iter_start ) == '\t' ) {
                StringBuilder text = new StringBuilder( "\n\t" );
                int value = 0;
                char char_lf = '*';
                iter_start++;   // first tab is already handled, so skip it

                for( ; iter_start != iter_end; ++iter_start ) {
                    switch( v.getText().charAt( iter_start ) ) {
                        // BULLET LIST
                        case '•':
                            if( char_lf != '*' )
                                return false;
                            char_lf = ' ';
                            text.append( "• " );
                            break;
                        // CHECK LIST
                        case '[':
                            if( char_lf != '*' )
                                return false;
                            char_lf = 'c';
                            break;
                        case '~':
                        case '+':
                        case 'x':
                        case 'X':
                            if( char_lf != 'c' )
                                return false;
                            char_lf = ']';
                            break;
                        case ']':
                            if( char_lf != ']' )
                                return false;
                            char_lf = ' ';
                            text.append( "[ ] " );
                            break;
                        // NUMBERED LIST
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            if( char_lf != '*' && char_lf != '1' )
                                return false;
                            char_lf = '1';
                            value *= 10;
                            value += v.getText().charAt( iter_start ) - '0';
                            break;
                        case '-':
                            if( char_lf == '*' ) {
                                char_lf = ' ';
                                text.append(  "- " );
                                break;
                            }
                            // no break
                        case '.':
                        case ')':
                            if( char_lf != '1' )
                                return false;
                            char_lf = ' ';
                            text.append( ++value )
                                .append( v.getText().charAt( iter_start ) )
                                .append( ' ' );
                            break;
                        case '\t':
                            if( char_lf != '*' )
                                return false;
                            text.append( '\t' );
                            break;
                        case ' ':
                            if( char_lf == 'c' ) {
                                char_lf = ']';
                                break;
                            }
                            else if( char_lf != ' ' )
                                return false;
                            // remove the last bullet if no text follows it:
                            if( iter_start == iter_end - 1 ) {
                                iter_start = offset_start;
                                mFlagEditorActionInProgress = true;
                                mEditText.getText().delete( iter_start, iter_end );
                                mEditText.getText().insert( iter_start, "\n" );
                            }
                            else {
                                mFlagEditorActionInProgress = true;
                                mEditText.getText().insert( iter_end, text );
                                iter_start = iter_end + text.length();
                                if( value > 0 ) {
                                    iter_start++;
                                    while( ( iter_start = increment_numbered_line(
                                                iter_start, value++, v ) ) > 0 ) {
                                        iter_start++;
                                    }
                                }
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            }
            return false;
        } );

        mEditText.setCustomSelectionActionModeCallback( new ActionMode.Callback()
        {
            public boolean onPrepareActionMode( ActionMode mode, Menu menu ) {
                return true;
            }

            public void onDestroyActionMode( ActionMode mode ) {
            }

            public boolean onCreateActionMode( ActionMode mode, Menu menu ) {
                menu.add( Menu.NONE, R.id.visit_link, Menu.FIRST, R.string.go );
                return true;
            }

            public boolean onActionItemClicked( ActionMode mode, MenuItem item ) {
                if( item.getItemId() == R.id.visit_link ) {
                    final Editable buffer = mEditText.getEditableText();
                    final ClickableSpan[] link = buffer.getSpans( mEditText.getSelectionStart(),
                                                                  mEditText.getSelectionEnd(),
                                                                  ClickableSpan.class );
                    if( link.length > 0 )
                        link[ 0 ].onClick( mEditText );
                    else
                        Log.i( Lifeograph.TAG, "No link in the selection" );
                    return true;
                }
                return false;
            }
        } );

        Button mButtonBold = view.findViewById( R.id.buttonBold );
        mButtonBold.setOnClickListener( v -> toggleFormat( "*" ) );

        Button mButtonItalic = view.findViewById( R.id.buttonItalic );
        mButtonItalic.setOnClickListener( v -> toggleFormat( "_" ) );

        Button mButtonStrikethrough = view.findViewById( R.id.buttonStrikethrough );
        SpannableString spanStringS = new SpannableString( "S" );
        spanStringS.setSpan( new StrikethroughSpan(), 0, 1, 0 );
        mButtonStrikethrough.setText( spanStringS );
        mButtonStrikethrough.setOnClickListener( v -> toggleFormat( "=" ) );

        mButtonHighlight = view.findViewById( R.id.buttonHighlight );
        mButtonHighlight.setOnClickListener( v -> toggleFormat( "#" ) );

        Button mButtonIgnore = view.findViewById( R.id.button_ignore );
        mButtonIgnore.setOnClickListener( v -> toggleIgnoreParagraph() );

        Button mButtonComment = view.findViewById( R.id.button_comment );
        mButtonComment.setOnClickListener( v -> addComment() );

        assert mEntry != null;

        if( mEntry.get_size() > 0 ) {
            requireActivity().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN );
        }
        show( savedInstanceState == null );

        if( !Lifeograph.getAddFreeNotPurchased() ) {
            LinearLayout container = view.findViewById( R.id.main_container );
            View ad = view.findViewById( R.id.fragmentAd );
            container.removeView( ad );
        }
    }

    /*@Override
    protected void onPause() {
        super.onPause();

        Log.d( Lifeograph.TAG, "ActivityEntry.onPause()" );
    }*/

    @Override
    public void
    onResume() {
        super.onResume();

        Log.d( Lifeograph.TAG, "FragmentEntry.onResume()" );

        if( mMenu != null )
            updateMenuVisibilities();
    }

    @Override
    public void
    onStop() {
        super.onStop();

        Log.d( Lifeograph.TAG, "ActivityEntry.onStop()" );

        if( mFlagDismissOnExit )
            Diary.diary.dismiss_entry( mEntry, false );
        else
            sync();

        Diary.diary.writeLock();
    }

    @Override
    public void
    onCreateOptionsMenu( @NonNull Menu menu, MenuInflater inflater ) {
        inflater.inflate( R.menu.menu_entry, menu );

        super.onCreateOptionsMenu( menu, inflater );

        MenuItem item = menu.findItem( R.id.change_todo_status );
        ToDoAction ToDoAction = ( ToDoAction ) MenuItemCompat.getActionProvider( item );
        ToDoAction.mObject = this;

        item = menu.findItem( R.id.search_text );
        final SearchView searchView = ( SearchView ) item.getActionView();
        item.setOnActionExpandListener( new MenuItem.OnActionExpandListener()
        {
            public boolean onMenuItemActionExpand( MenuItem menuItem ) {
                searchView.setQuery( Diary.diary.get_search_text(), false );
                return true;
            }

            public boolean onMenuItemActionCollapse( MenuItem menuItem ) {
                return true;
            }
        } );

        mMenu = menu;

        searchView.setOnQueryTextListener( new SearchView.OnQueryTextListener()
        {
            public boolean onQueryTextSubmit( String s ) {
                return true;
            }

            public boolean onQueryTextChange( String s ) {
                if( mFlagSearchIsOpen ) {
                    Diary.diary.set_search_text( s.toLowerCase(), false );
                    parse( 0, mEditText.getText().length() );
                }
                return true;
            }
        } );
        searchView.setOnQueryTextFocusChangeListener( ( view, b ) -> mFlagSearchIsOpen = b );
        updateIcon();
    }

    @Override
    public void onPrepareOptionsMenu( @NonNull Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        updateMenuVisibilities();
    }

    @Override
    public boolean
    onOptionsItemSelected( MenuItem item ) {
        int itemId = item.getItemId();
        if( itemId == R.id.enable_edit ) {
            Lifeograph.enableEditing( this );
            return true;
        }
        else if( itemId == R.id.home ) {
            //NavUtils.navigateUpFromSameTask( this );
            //finish();
            return true;
        }
        else if( itemId == R.id.toggle_favorite ) {
            toggleFavorite();
            return true;
        }
        else if( itemId == R.id.change_todo_status ) {
            return false;
        }
        else if( itemId == R.id.edit_date ) {
            new DialogInquireText( getContext(),
                                   R.string.edit_date,
                                   mEntry.get_date().format_string(),
                                   R.string.apply,
                                   this ).show();
            return true;
        }
        else if( itemId == R.id.dismiss ) {
            dismiss();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    // POPUP MENU LISTENER
    public boolean
    onMenuItemClick( MenuItem item ) {
        int itemId = item.getItemId();
        if( itemId == R.id.button_list_none ) {
            set_list_item_mark( 'n' );
            return true;
        }
        else if( itemId == R.id.button_list_bullet ) {
            set_list_item_mark( '*' );
            return true;
        }
        else if( itemId == R.id.button_list_to_do ) {
            set_list_item_mark( ' ' );
            return true;
        }
        else if( itemId == R.id.button_list_progressed ) {
            set_list_item_mark( '~' );
            return true;
        }
        else if( itemId == R.id.button_list_done ) {
            set_list_item_mark( '+' );
            return true;
        }
        else if( itemId == R.id.button_list_canceled ) {
            set_list_item_mark( 'x' );
            return true;
        }
        else if( itemId == R.id.button_list_numbered ) {
            set_list_item_mark( '1' );
            return true;
        }
        return false;
    }

    private void updateMenuVisibilities(){
        boolean flagWritable = Diary.diary.is_in_edit_mode();

        mMenu.findItem( R.id.enable_edit ).setVisible( !flagWritable &&
                                                       Diary.diary.can_enter_edit_mode() );

        mMenu.findItem( R.id.change_todo_status ).setVisible( flagWritable );
        mMenu.findItem( R.id.toggle_favorite ).setVisible( flagWritable );
        mMenu.findItem( R.id.edit_date ).setVisible( flagWritable );
        mMenu.findItem( R.id.dismiss ).setVisible( flagWritable );
    }

    @Override
    public void
    enableEditing() {
        mMenu.findItem( R.id.enable_edit ).setVisible( false );

        mMenu.findItem( R.id.change_todo_status ).setVisible( true );
        mMenu.findItem( R.id.toggle_favorite ).setVisible( true );
        mMenu.findItem( R.id.edit_date ).setVisible( true );
        mMenu.findItem( R.id.dismiss ).setVisible( true );

        mEditText.setKeyListener( mKeyListener );
        // force soft keyboard to be shown:
        if( mEditText.requestFocus() ){
            InputMethodManager imm =
                    ( InputMethodManager ) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
            imm.showSoftInput( mEditText, InputMethodManager.SHOW_IMPLICIT );
        }

        requireActivity().findViewById( R.id.toolbar_text_edit ).setVisibility( View.VISIBLE );
    }

    void
    updateIcon() {
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


        if( mMenu != null ) {
            int icon = R.drawable.ic_action_not_todo;

            mMenu.findItem( R.id.toggle_favorite ).setIcon(
                    mEntry.is_favored() ? R.drawable.ic_action_favorite :
                    R.drawable.ic_action_not_favorite );

            switch( mEntry.get_todo_status_effective() ) {
                case Entry.ES_TODO:
                    icon = R.drawable.ic_action_todo_open;
                    break;
                case Entry.ES_PROGRESSED:
                    icon = R.drawable.ic_action_todo_progressed;
                    break;
                case Entry.ES_DONE:
                    icon = R.drawable.ic_action_todo_done;
                    break;
                case Entry.ES_CANCELED:
                    icon = R.drawable.ic_action_todo_canceled;
                    break;
            }
            mMenu.findItem( R.id.change_todo_status ).setIcon( icon );
        }
    }

    void updateTheme() {
        Theme theme = mEntry.get_theme();
        mEditText.setBackgroundColor( theme.color_base );
        mEditText.setTextColor( theme.color_text );

        mColorMid = Theme.midtone( theme.color_base, theme.color_text, 0.4f );

        //mColorRegionBG = Theme.midtone( theme.color_base, theme.color_text, 0.9f ); LATER
        mColorMatchBG = Theme.contrast2(
                theme.color_base, Theme.s_color_match1, Theme.s_color_match2 );

        //mColorLink = Theme.contrast(
        //theme.color_base, Theme.s_color_link1, Theme.s_color_link2 ); LATER
        //mColorLinkBroken = Theme.contrast(
        //theme.color_base, Theme.s_color_broken1, Theme.s_color_broken2 ); LATER

        mButtonHighlight.setTextColor( theme.color_text );
        SpannableString spanStringH = new SpannableString( "H" );
        spanStringH.setSpan( new BackgroundColorSpan( theme.color_highlight ), 0, 1, 0 );
        mButtonHighlight.setText( spanStringH );
    }

    void sync() {
        if( mFlagEntryChanged ) {
            mEntry.m_date_edited = ( int ) ( System.currentTimeMillis() / 1000L );
            mEntry.set_text( mEditText.getText().toString() );
            mFlagEntryChanged = false;
        }
    }

    void show( boolean flagParse ) {
        if( mEntry == null ) {
            Log.e( Lifeograph.TAG, "Empty entry passed to show" );
            return;
        }

        mFlagDismissOnExit = false;

        // THEME
        updateTheme();

        // PARSING
        m_pos_start = 0;
        m_pos_end = mEntry.get_text().length();

        // SETTING TEXT
        // mFlagSetTextOperation = true;
        if( flagParse )
            mEditText.setText( mEntry.get_text() );
        // mFlagSetTextOperation = false;

        // if( flagParse )
        // parse();

        mActionBar.setTitle( mEntry.get_title_str() );
        mActionBar.setSubtitle( mEntry.get_info_str() );
        updateIcon();
        //invalidateOptionsMenu(); // may be redundant here
    }

    private void toggleFavorite() {
        mEntry.toggle_favored();
        updateIcon();
    }

    private void dismiss() {
        Lifeograph.showConfirmationPrompt( getContext(),
                                           R.string.entry_dismiss_confirm,
                                           R.string.dismiss,
                                           ( dialog, id ) -> {
                                               mFlagDismissOnExit = true;
                                               // TODO: FragmentEntry.this.finish();
                                           } );
    }

    public void createListLineMenu( View v ) {
        PopupMenu popup = new PopupMenu( getContext(), v );
        popup.setOnMenuItemClickListener( this );

        popup.inflate( R.menu.menu_list_line );

        popup.show();
    }

    public void setTodoStatus( int s ) {
        mEntry.set_todo_status( s );
        mEntry.m_date_status = ( int ) ( System.currentTimeMillis() / 1000L );
        updateIcon();
    }

    // InquireListener methods
    public void onInquireAction( int id, String text ) {
        if( id == R.string.edit_date ) {
            Date date = new Date( text );
            if( date.m_date != Date.NOT_SET ) {
                if( !date.is_ordinal() )
                    date.set_order_3rd( 1 );
                try {
                    Diary.diary.set_entry_date( mEntry, date );
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
                mActionBar.setTitle( mEntry.get_title_str() );
                mActionBar.setSubtitle( mEntry.get_info_str() );
            }
        }
    }
    public boolean onInquireTextChanged( int id, String s ) {
        if( id == R.string.edit_date ) {
            long date = Date.parse_string( s );
            return ( date > 0 && date != mEntry.m_date.m_date );
        }
        return true;
    }

    private int increment_numbered_line( int iter, int expected_value, TextView v ) {
        if( iter >= v.getText().length() )
            return -1;

        int iter_start = iter;
        int iter_end = v.getText().toString().indexOf( '\n', iter );
        if( iter_end == -1 )
            iter_end = v.getText().length() - 1;

        StringBuilder text = new StringBuilder();
        int value = 0;
        char char_lf = 't';

        for( ; iter != iter_end; ++iter ) {
            switch( v.getText().charAt( iter ) ) {
                case '\t':
                    if( char_lf != 't' && char_lf != '1' )
                        return -1;
                    char_lf = '1';
                    text.append( '\t' );
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    if( char_lf != '1' && char_lf != '-' )
                        return -1;
                    char_lf = '-';
                    value *= 10;
                    value += v.getText().charAt( iter ) - '0';
                    break;
                case '-':
                case '.':
                case ')':
                    if( char_lf != '-' || value != expected_value )
                        return -1;
                    char_lf = ' ';
                    value++;
                    text.append( value ).append( v.getText().charAt( iter ) ).append( ' ' );
                    break;
                case ' ':
                    if( char_lf != ' ' )
                        return -1;
                    mFlagEditorActionInProgress = true;
                    mEditText.getText().delete( iter_start, iter + 1 );
                    mEditText.getText().insert( iter_start, text );
                    return( iter_end + text.length() - ( iter - iter_start + 1 ) );
                default:
                    return -1;
            }
        }
        return -1;
    }

    // FORMATTING BUTTONS ==========================================================================
    private boolean calculate_multi_para_bounds( int[] bounds ) {
        String str = mEditText.getText().toString();

        if( mEditText.hasSelection() ) {
            bounds[ 0 ] = mEditText.getSelectionStart();
            bounds[ 1 ] = mEditText.getSelectionEnd();
        }
        else {
            bounds[ 0 ] = bounds[ 1 ] = mEditText.getSelectionStart();
            if( bounds[ 0 ] == 0 )
                return true;
            if( str.charAt( bounds[ 0 ] - 1 ) == '\n' ) {
                if( bounds[ 0 ] == str.length() )
                    return false;
                if( str.charAt( bounds[ 0 ] ) == '\n' )
                    return false;
            }
        }

        bounds[ 0 ]--;

        if( str.lastIndexOf( '\n', bounds[ 0 ] ) == -1 ) {
            if( str.indexOf( '\n', bounds[ 0 ] ) == -1 )
                return true;
            else
                bounds[ 0 ] = str.indexOf( '\n', bounds[ 0 ] ) + 1;
        }
        else
            bounds[ 0 ] = str.lastIndexOf( '\n', bounds[ 0 ] ) + 1;

        if( str.indexOf( '\n', bounds[ 1 ] ) == -1 )
            bounds[ 1 ] = str.length() - 1;
        else
            bounds[ 1 ] = str.indexOf( '\n', bounds[ 1 ] ) - 1;

        return ( bounds[ 0 ] > bounds[ 1 ] );
    }

    private void toggleFormat( String markup ) {
        int p_start, p_end;
        if( mEditText.hasSelection() ) {
            int start = -2, end = -1;
            boolean properly_separated = false;

            p_start = mEditText.getSelectionStart();
            p_end = mEditText.getSelectionEnd() - 1;

            int p_firt_nl = mEditText.getText().toString().indexOf( '\n' );
            if( p_firt_nl == -1 ) // there is only heading
                return;
            else if( p_end <= p_firt_nl )
                return;
            else if( p_start > p_firt_nl )
                p_start--; // also evaluate the previous character
            else { // p_start <= p_first_nl
                p_start = p_firt_nl + 1;
                properly_separated = true;
                start = -1;
            }

            for( ;; p_start++ ) {
                AdvancedSpan theSpan = hasSpan( p_start, markup.charAt( 0 ) );
                if( theSpan.getType() == '*' || theSpan.getType() == '_' ||
                    theSpan.getType() == '#' || theSpan.getType() == '=' )
                    return;
                switch( mEditText.getText().charAt( p_start ) ) {
                    case '\n': // selection spreads over more than one
                        if( start >= 0 ) {
                            if( properly_separated ) {
                                mEditText.getText().insert( start, markup );
                                end += 2;
                                p_start += 2;
                                p_end += 2;
                            }
                            else {
                                mEditText.getText().insert( start, " " + markup );
                                end += 3;
                                p_start += 3;
                                p_end += 3;
                            }

                            mEditText.getText().insert( end, markup );

                            properly_separated = true;
                            start = -1;
                            break;
                        }
                        /* else no break */
                    case ' ':
                    case '\t':
                        if( start == -2 ) {
                            properly_separated = true;
                            start = -1;
                        }
                        break;
                        /* else no break */
                    default:
                        if( start == -2 )
                            start = -1;
                        else if( start == -1 )
                            start = p_start;
                        end = p_start;
                        break;
                }
                if( p_start == p_end )
                    break;
            }
            // add markup chars to the beginning and end:
            if( start >= 0 ) {
                if( properly_separated ) {
                    mEditText.getText().insert( start, markup );
                    end += 2;
                }
                else {
                    mEditText.getText().insert( start, " " + markup );
                    end += 3;
                }

                mEditText.getText().insert( end, markup );
                // TODO place_cursor( get_iter_at_offset( end ) );
            }
        }
        else { // no selection case
            p_start = p_end = mEditText.getSelectionStart();
            if( isSpace( p_start ) || p_start == mEditText.length() - 1 ) {
                if( startsLine( p_start ) )
                    return;
                p_start--;
                if( hasSpan( p_start, 'm' ).getType() == 'm' )
                    p_start--;
            }
            else if( hasSpan( p_start, 'm' ).getType() == 'm' ) {
                if( startsLine( p_start ) )
                    return;
                p_start--;
                if( isSpace( p_start ) )
                    p_start += 2;
            }

            AdvancedSpan theSpan = hasSpan( p_start, markup.charAt( 0 ) );

            // if already has the markup remove it
            if( theSpan.getType() == markup.charAt( 0 ) ) {
                p_start = mEditText.getText().getSpanStart( theSpan );
                p_end = mEditText.getText().getSpanEnd( theSpan );
                mEditText.getText().delete( p_start - 1, p_start );
                mEditText.getText().delete( p_end - 1, p_end );
            }
            else if( theSpan.getType() == ' ' ) {
                // find word boundaries:
                while( p_start > 0 ) {
                    char c = mEditText.getText().charAt( p_start );
                    if( c == '\n' || c == ' ' || c == '\t' ) {
                        p_start++;
                        break;
                    }

                    p_start--;
                }
                mEditText.getText().insert( p_start, markup );

                while( p_end < mEditText.getText().length() ) {
                    char c = mEditText.getText().charAt( p_end );
                    if( c == '\n' || c == ' ' || c == '\t' )
                        break;
                    p_end++;
                }
                mEditText.getText().insert( p_end, markup );
                // TODO (if necessary) place_cursor( offset );
            }
        }
    }

    public void set_list_item_mark( char target_item_type ) {
        int[] bounds = { 0, 0 };
        if( calculate_multi_para_bounds( bounds ) )
            return;

        int pos = bounds[ 0 ];

        if ( bounds[ 0 ] == bounds[ 1 ] ) { // empty line
            switch( target_item_type ) {
                case '*':
                    mEditText.getText().insert( pos, "\t• " );
                    break;
                case ' ':
                    mEditText.getText().insert( pos, "\t[ ] " );
                    break;
                case '~':
                    mEditText.getText().insert( pos, "\t[~] " );
                    break;
                case '+':
                    mEditText.getText().insert( pos, "\t[+] " );
                    break;
                case 'x':
                    mEditText.getText().insert( pos, "\t[x] " );
                    break;
                case '1':
                    mEditText.getText().insert( pos, "\t1- " );
                    break;
            }
            return;
        }

        int pos_end = bounds[ 1 ];
        int pos_erase_begin = pos;
        char item_type = 0;    // none
        char char_lf = 't';    // tab
        int value = 1; // for numeric lists

        while( pos <= pos_end ) {
            switch( mEditText.getText().toString().charAt( pos ) ) {
                case '\t':
                    if( char_lf == 't'  || char_lf == '[' ) {
                        char_lf = '[';  // opening bracket
                        pos_erase_begin = pos;
                    }
                    else
                        char_lf = 'n';
                    break;
                case '•':
                case '-':
                    char_lf = ( char_lf == '[' ? 's' : 'n' );
                    item_type = ( char_lf == 's' ? '*' : 0 );
                    break;
                case '[':
                    char_lf = ( char_lf == '[' ? 'c' : 'n' );
                    break;
                case ' ':
                    if( char_lf == 's' ) { // separator space
                        if( item_type != target_item_type ) {
                            mEditText.getText().delete( pos_erase_begin, pos + 1 );
                            int diff = ( pos + 1 - pos_erase_begin );
                            pos -= diff;
                            pos_end -= diff;
                            char_lf = 'a';
                        }
                        else {
                            char_lf = 'n';
                        }
                        break;
                    }
                    // no break: process like other check box chars:
                case '~':
                case '+':
                case 'x':
                case 'X':
                    char_lf = ( char_lf == 'c' ? ']' : 'n' );
                    item_type = mEditText.getText().toString().charAt( pos );
                    break;
                case ']':
                    char_lf = ( char_lf == ']' ? 's' : 'n' );
                    break;
                case '\n':
                    item_type = 0;
                    char_lf = 't';  // tab
                    break;
                case 0: // end
                default:
                    if( char_lf == 'a' || char_lf == 't' || char_lf == '[' ) {
                        switch( target_item_type ) {
                            case '*':
                                mEditText.getText().insert( pos, "\t• " );
                                pos += 3;
                                pos_end += 3;
                                break;
                            case ' ':
                                mEditText.getText().insert( pos, "\t[ ] " );
                                pos += 5;
                                pos_end += 5;
                                break;
                            case '~':
                                mEditText.getText().insert( pos, "\t[~] " );
                                pos += 5;
                                pos_end += 5;
                                break;
                            case '+':
                                mEditText.getText().insert( pos, "\t[+] " );
                                pos += 5;
                                pos_end += 5;
                                break;
                            case 'x':
                                mEditText.getText().insert( pos, "\t[x] " );
                                pos += 5;
                                pos_end += 5;
                                break;
                            case '1':
                                mEditText.getText().insert( pos, "\t" + value + "- " );
                                value++;
                                break;
                        }
                    }
                    char_lf = 'n';
                    break;
            }
            pos++;
        }
    }

    private void addComment() {
        if( mEditText.hasSelection() ) {
            int p_start, p_end;
            p_start = mEditText.getSelectionStart();
            p_end = mEditText.getSelectionEnd() - 1;

            mEditText.getText().insert( p_start, "[[" );
            mEditText.getText().insert( p_end + 2, "]]" );
        }
        else { // no selection case
            int p_start = mEditText.getSelectionStart();
            mEditText.getText().insert( p_start, "[[]]" );
            mEditText.setSelection( p_start + 2 );
        }
    }

    private void toggleIgnoreParagraph() {
        int[] bounds = { 0, 0 };
        if( calculate_multi_para_bounds( bounds ) )
            return;

        if ( bounds[ 0 ] == bounds[ 1 ] ) { // empty line
            mEditText.getText().insert( bounds[ 0 ], ".\t" );
            return;
        }

        int pos = bounds[ 0 ];
        int pos_end = bounds[ 1 ];
        int pos_erase_begin = pos;
        char char_lf = '.';

        while( pos <= pos_end ) {
            switch( mEditText.getText().toString().charAt( pos ) ) {
                case '.':
                    if( char_lf == '.' ) {
                        pos_erase_begin = pos;
                        char_lf = 't'; // tab
                    }
                    else
                        char_lf = 'n';
                    break;
                case '\n':
                    char_lf = '.';
                    break;
                case '\t':
                    if( char_lf == 't' ) {
                        mEditText.getText().delete( pos_erase_begin, pos + 1 );
                        int diff = ( pos + 1 - pos_erase_begin );
                        pos -= diff;
                        pos_end -= diff;
                    }
                case 0: // end
                default:
                    if( char_lf == '.' ) {
                        mEditText.getText().insert( pos, ".\t" );
                        pos += 2;
                        pos_end += 2;
                    }
                    char_lf = 'n';
                    break;
            }
            pos++;
        }
    }

    // PARSING VARIABLES ===========================================================================
    private int        m_pos_start, m_pos_end, m_pos_current;
    private int        pos_word, /*pos_regular,*/ pos_search, pos_tab;
    private char       m_char_current;
    private int        m_cf_last, m_cf_req = CF_ANY;
    private final StringBuilder word_last = new StringBuilder();
    private int        int_last;
    private int        id_last;
    private final Date date_last = new Date();
    protected boolean  m_flag_hidden_link;

    private final java.util.List< AbsChar > m_chars_looked_for = new ArrayList<>();
    private ParSel m_applier_nl;

    private static class AbsChar  // abstract char
    {
        AbsChar( int f, ParSel a, boolean j ) {
            flags = f;
            applier = a;
            junction = j;
        }
        AbsChar( int f, ParSel a ) {
            flags = f;
            applier = a;
            junction = false;
        }
        int             flags;
        ParSel          applier;
        boolean         junction;
    }

    // SPANS =======================================================================================
    private interface AdvancedSpan
    {
        char getType();
    }
    private static class SpanOther implements  AdvancedSpan
    {
        public char getType() {
            return 'O';
        }
    }
    private static class SpanNull implements  AdvancedSpan
    {
        public char getType() {
            return ' ';
        }
    }
    @SuppressLint( "ParcelCreator" )
    private static class SpanBold extends StyleSpan implements AdvancedSpan
    {
        SpanBold() {
            super( Typeface.BOLD );
        }
        public char getType() {
            return '*';
        }
    }
    @SuppressLint( "ParcelCreator" )
    private static class SpanItalic extends StyleSpan implements AdvancedSpan
    {
        SpanItalic() {
            super( Typeface.ITALIC );
        }
        public char getType() {
            return '_';
        }
    }
    @SuppressLint( "ParcelCreator" )
    private static class SpanHighlight extends BackgroundColorSpan implements AdvancedSpan
    {
        SpanHighlight() {
            super( mEntry.get_theme().color_highlight );
        }
        public char getType() {
            return '#';
        }
    }
    @SuppressLint( "ParcelCreator" )
    private static class SpanStrikethrough extends StrikethroughSpan implements AdvancedSpan
    {
        public char getType() {
            return '=';
        }
    }
    @SuppressLint( "ParcelCreator" )
    private class SpanMarkup extends ForegroundColorSpan implements AdvancedSpan
    {
        SpanMarkup() {
            super( mColorMid );
        }
        public char getType() {
            return 'm';
        }
    }

    private static class LinkDate extends ClickableSpan implements AdvancedSpan
    {
        LinkDate( long date ) {
            mDate = date;
        }

        @Override
        public void onClick( @NonNull View widget ) {
            Entry entry = Diary.diary.get_entry_by_date( mDate );

            if( entry == null )
                entry = Diary.diary.create_entry( mDate, "", false );

            Lifeograph.showElem( entry );
        }

        public char getType() {
            return 'd';
        }

        private final long mDate;
    }
    private class LinkUri extends ClickableSpan implements AdvancedSpan
    {
        LinkUri( String uri ) {
            mUri = uri;
        }

        @Override
        public void onClick( @NonNull View widget ) {
            Intent browserIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( mUri ) );
            startActivity( browserIntent );
        }

        public char getType() {
            return 'u';
        }

        private final String mUri;
    }
    private static class LinkID extends ClickableSpan implements AdvancedSpan
    {
        LinkID( int id ) {
            mId = id;
        }

        @Override
        public void onClick( @NonNull View widget ) {
            DiaryElement elem = Diary.diary.get_element( mId );
            if( elem != null ) {
                if( elem.get_type() != DiaryElement.Type.ENTRY )
                    Log.d( Lifeograph.TAG, "Target is not entry" );
                else
                    Lifeograph.showElem( elem );
            }
        }

        public char getType() {
            return 'i';
        }

        private final int mId;
    }

    private final java.util.Vector< Object > mSpans = new java.util.Vector<>();

    // PARSING =====================================================================================
    private void reset( int start, int end ) {
        m_pos_start = start;
        m_pos_end = end;
        m_pos_current = pos_word = /*pos_regular =*/ start;

        // TODO: only remove spans within the parsing boundaries...
        // mEditText.getText().clearSpans(); <-- problematic!!
        for( Object span : mSpans )
            mEditText.getText().removeSpan( span );
        mSpans.clear();

        m_cf_last = CF_NOT_SET;
        m_cf_req = CF_ANY;
        word_last.setLength( 0 );
        int_last = 0;
        date_last.set( 0 );
        id_last = 0;
        m_chars_looked_for.clear();

        if( start == 0 && end > 0 ) {
            // to prevent formatting within title:
            m_chars_looked_for.add( new AbsChar( CF_IGNORE, ParSel.NULL ) );
            m_applier_nl = ParSel.AP_HEND;
            apply_heading();
        }
        else {
            m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
            m_applier_nl = ParSel.NULL;
        }
    }

    void parse( int start, int end ) {
        mEntry.set_text( mEditText.getText().toString() );

        update_todo_status();

        // NOTE: everything below should go to Parser when there is one
        reset( start, end );

        // this part is different than in c++
        String search_text = Diary.diary.get_search_text();
        boolean flag_search_active = !search_text.isEmpty();
        int i_search = 0;
        int i_search_end = Diary.diary.get_search_text().length() - 1;

        for( ; m_pos_current < m_pos_end; ++m_pos_current ) {
            m_char_current = mEditText.getText().charAt( m_pos_current );

            if( flag_search_active ) {
                if( search_text.charAt( i_search ) == Character.toLowerCase( m_char_current ) ) {
                    if( i_search == 0 )
                        pos_search = m_pos_current;
                    if( i_search == i_search_end ) {
                        apply_match();
                        i_search = 0;
                    }
                    else
                        i_search++;
                }
                else
                    i_search = 0;
            }

            // MARKUP PARSING
            switch( m_char_current ) {
                case '\n':
                case '\r':
                    process_char( CF_NEWLINE, CF_NUM_CKBX | CF_ALPHA | CF_FORMATCHAR | CF_SLASH
                                              | CF_DOTDATE | CF_MORE | CF_TAB | CF_IGNORE, 0,
                                  ParSel.NULL );
                    break;
                case ' ':
                    process_char( CF_SPACE, CF_ALPHA | CF_NUMBER | CF_SLASH | CF_DOTDATE
                                            | CF_TODO, CF_NOTHING, ParSel.TR_SUBH );
                    break;
                case '*':
                    process_char( CF_ASTERISK, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE,
                                  CF_NOTHING, ParSel.TR_BOLD );
                    break;
                case '_':
                    process_char( CF_UNDERSCORE, CF_NUM_CKBX | CF_SLASH | CF_DOTDATE, CF_NOTHING,
                                  ParSel.TR_ITLC );
                    break;
                case '=':
                    process_char( CF_EQUALS, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE,
                                  CF_NOTHING, ParSel.TR_STRK );
                    break;
                case '#':
                    process_char( CF_HASH, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE,
                                  CF_NOTHING, ParSel.TR_HILT );
                    break;
                case '[':
                    process_char( CF_SBB, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE,
                                  CF_NOTHING, ParSel.TR_CMNT );
                    break;
                case ']':
                    process_char( CF_SBE, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE, 0,
                                  ParSel.NULL );
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    handle_number(); // calculates numeric value
                    process_char( CF_NUMBER, CF_SLASH | CF_ALPHA | CF_DOTDATE | CF_TODO,
                                  CF_NOTHING, ParSel.TR_LNKD );
                    break;
                case '.':
                    process_char( CF_DOTDATE, CF_NUM_CKBX | CF_ALPHA | CF_SLASH, CF_NOTHING,
                                  ParSel.TR_IGNR );
                    break;

                case '-':
                    process_char( CF_DOTDATE, CF_NUM_CKBX | CF_ALPHA | CF_SLASH, 0, ParSel.NULL );
                    break;
                case '/':
                    process_char( CF_SLASH | CF_DOTDATE, CF_NUM_CKBX | CF_ALPHA, 0, ParSel.NULL );
                    break;
                case ':':
                    process_char( CF_PUNCTUATION_RAW, CF_NUM_CKBX | CF_ALPHA | CF_SLASH
                                                      | CF_DOTDATE, CF_NOTHING, ParSel.TR_LINK );
                    break;
                case '@':
                    process_char( CF_AT, CF_NUM_CKBX | CF_ALPHA | CF_SLASH | CF_DOTDATE,
                                  CF_NOTHING, ParSel.TR_LNAT );
                    break;
                case '>':
                    // marks deferred when used in to do context
                    process_char( CF_TODO|CF_MORE, CF_NUM_CKBX | CF_ALPHA | CF_SLASH |
                                                   CF_DOTDATE, 0,
                                  ParSel.NULL );
                    break;
                case '\t':
                    process_char( CF_TAB, CF_NUM_SLSH | CF_ALPHA | CF_DOTDATE, CF_NOTHING,
                                  ParSel.TR_LIST );
                    break;
                // LIST CHARS
                case '~':
                case '+':
                    process_char( CF_TODO|CF_PUNCTUATION_RAW,
                                  CF_ALPHA|CF_NUM_CKBX|CF_DOTDATE|CF_SLASH,
                                  0, ParSel.NULL );
                    break;
                case 'x':
                case 'X':
                    process_char( CF_TODO|CF_ALPHA,
                                  CF_NUM_CKBX|CF_DOTDATE|CF_SLASH,
                                  0, ParSel.NULL );
                    break;
                default: // most probably alpha
                    process_char( CF_ALPHA, CF_NUM_CKBX | CF_DOTDATE | CF_SLASH, 0, ParSel.NULL ) ;
                    break;
            }
        }
        // end of the text -treated like new line
        process_char( CF_NEWLINE, CF_NUM_CKBX | CF_ALPHA | CF_FORMATCHAR | CF_SLASH | CF_DOTDATE
                | CF_MORE | CF_TAB, CF_EOT, ParSel.NULL );
    }

    // PARSING HELPER FUNCTIONS ====================================================================
    private void selectParsingFunc( ParSel ps ) {
        switch( ps ) {
            case TR_SUBH:
                trigger_subheading();
                break;
            case TR_BOLD:
                trigger_bold();
                break;
            case TR_ITLC:
                trigger_italic();
                break;
            case TR_STRK:
                trigger_strikethrough();
                break;
            case TR_HILT:
                trigger_highlight();
                break;
            case TR_CMNT:
                trigger_comment();
                break;
            case TR_IGNR:
                trigger_ignore();
                break;
            case TR_LINK:
                trigger_link();
                break;
            case TR_LNKD:
                trigger_link_date();
                break;
            case TR_LNAT:
                trigger_link_at();
                break;
            case TR_LIST:
                trigger_list();
                break;

            case AP_BOLD:
                apply_bold();
                break;
            case AP_CMNT:
                apply_comment();
                break;
            case AP_HILT:
                apply_highlight();
                break;
            case AP_ITLC:
                apply_italic();
                break;
            case AP_LINK:
                apply_link();
                break;
            case AP_LNDT:
                apply_link_date();
                break;
            case AP_LNID:
                apply_link_id();
                break;
            case AP_STRK:
                apply_strikethrough();
                break;
            case AP_SUBH:
                apply_subheading();
                break;
            case AP_CUNF:
                apply_check_unf();
                break;
            case AP_CPRG:
                apply_check_prg();
                break;
            case AP_CFIN:
                apply_check_fin();
                break;
            case AP_CCCL:
                apply_check_ccl();
                break;

            case JK_DDMD:
                junction_date_dotmd();
                break;
            case JK_DDYM:
                junction_date_dotym();
                break;
            case JK_IGNR:
                junction_ignore();
                break;
            case JK_LNHT:
                junction_link_hidden_tab();
                break;
            case JK_LNDT:
                junction_link_date();
                break;
            case JK_LIST:
                junction_list();
                break;
            case JK_LST2:
                junction_list2();
                break;
            default:
                break;
        }
    }

    private void addSpan( Object span, int start, int end, int styles ) {
        mSpans.add( span );
        mEditText.getText().setSpan( span, start, end, styles );
    }

    private boolean isSpace( int offset ) {
        switch( mEditText.getText().charAt( offset ) ) {
            case '\n':
            case '\t':
            case ' ':
                return true;
            default:
                return false;
        }
    }
    private boolean startsLine( int offset ) {
        return offset == 0 || ( mEditText.getText().charAt( offset - 1 ) == '\n' );
    }
    private AdvancedSpan hasSpan( int offset, char type ) {
        Object[] spans = mEditText.getText().getSpans( offset, offset, Object.class );
        boolean hasNoOtherSpan = true;
        for( Object span : spans ) {
            if( span instanceof AdvancedSpan ) {
                if( ( ( AdvancedSpan ) span ).getType() == type ) {
                    return ( AdvancedSpan ) span;
                }
                else
                    hasNoOtherSpan = false;
            }
        }
        return( hasNoOtherSpan ? new SpanNull() : new SpanOther() );
    }

    // PROCESS CHAR ================================================================================
    private void process_char( int char_flags, int breaks, int triggers_on, ParSel triggerer ) {
        int         cf = m_chars_looked_for.get( 0 ).flags;
        ParSel      applier = m_chars_looked_for.get( 0 ).applier;
        boolean     flag_clear_chars = false;
        boolean     flag_trigger = false;
        boolean     flag_apply = false;

        if( ( char_flags & cf ) != 0 ) {
            if( applier != ParSel.NULL ) {
                if( m_chars_looked_for.get( 0 ).junction )
                    flag_apply = true;
                else if( ( m_cf_last & m_cf_req ) != 0 ) { // not junction = final applier
                    flag_clear_chars = true;
                    flag_apply = true;
                }
            }
            else {
                m_chars_looked_for.remove( 0 );
                if( ( triggers_on & cf ) != 0 )
                    flag_trigger = true;
            }
        }
        else if( ( breaks & cf ) == cf || ( cf & CF_IMMEDIATE ) != 0 ) {
            flag_clear_chars = true;
            if( ( triggers_on & CF_NOTHING ) != 0 )
                flag_trigger = true;
        }
        else if( ( triggers_on & cf ) != 0 ) {
            flag_trigger = true;
        }

        if( ( char_flags & CF_NEWLINE ) != 0 ) {
            flag_clear_chars = true;

            if( m_applier_nl != ParSel.NULL ) {
                selectParsingFunc( m_applier_nl );
                m_applier_nl = ParSel.NULL;
            }
            else if( ( char_flags & CF_EOT ) != 0 && !flag_apply ) {
                m_pos_start = m_pos_current + 1;
                //apply_regular();
            }
        }

        // DO AS COLLECTED INFORMATION REQUIRES
        if( flag_clear_chars ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
        }
        if( flag_trigger )
            selectParsingFunc( triggerer );
        if( flag_apply )
            selectParsingFunc( applier );

        // UPDATE WORD LAST
        if( ( char_flags & CF_SEPARATOR ) != 0 )
            word_last.setLength( 0 );
        else {
            if( word_last.length() == 0 )
                pos_word = m_pos_current;
            word_last.append( m_char_current );
        }

        // UPDATE CHAR CLASS
        m_cf_last = char_flags;
    }

    // HANDLE NUMBER ===============================================================================
    private void handle_number() {
        if( m_cf_last == CF_NUMBER ) {
            int_last *= 10;
            int_last += ( m_char_current - '0' );
        }
        else
            int_last = ( m_char_current - '0' );
    }

    // PARSING TRIGGERERS ==========================================================================
    private void trigger_subheading() {
        if( m_cf_last == CF_NEWLINE ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NONSPACE, ParSel.AP_SUBH ) );
            m_cf_req = CF_ANY;
            m_pos_start = m_pos_current;
        }
    }

    private void trigger_markup( int lf, ParSel ps ) {
        if( ( m_cf_last & CF_NOT_SEPARATOR ) != 0 )
            return;

        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_NONSPACE - lf, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( lf, ps ) );
        m_cf_req = CF_NOT_SEPARATOR;
        m_pos_start = m_pos_current;
    }

    private void trigger_bold() {
        trigger_markup( CF_ASTERISK, ParSel.AP_BOLD );
    }

    private void trigger_italic() {
        trigger_markup( CF_UNDERSCORE, ParSel.AP_ITLC );
    }

    private void trigger_strikethrough() {
        trigger_markup( CF_EQUALS, ParSel.AP_STRK );
    }

    private void trigger_highlight() {
        trigger_markup( CF_HASH, ParSel.AP_HILT );
    }

    private void trigger_comment() {
        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_SBB | CF_IMMEDIATE, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_SBE, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_SBE | CF_IMMEDIATE, ParSel.AP_CMNT ) );
        m_cf_req = CF_ANY;
        m_pos_start = m_pos_current;
    }

    private void trigger_link() {
        if( word_last.toString().isEmpty() ) {
            Log.e( Lifeograph.TAG, "trigger_link() called for an empty word_last" );
            return;
            // this is a precaution against some error reports
        }
        m_flag_hidden_link = ( word_last.charAt( 0 ) == '<' );
        if( m_flag_hidden_link )
            word_last.deleteCharAt( 0 );

        m_cf_req = CF_ANY;

        String wl_str = word_last.toString();

        if( wl_str.equals( "http" ) || wl_str.equals( "https" ) || wl_str.equals( "ftp" )
            /*|| wl_str.equals( "file" )*/ ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_SLASH, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_SLASH, ParSel.NULL ) );
//            if( word_last.equals( "file" ) ) {
//                m_chars_looked_for.add( new AbsChar( CF_SLASH, ParSel.NULL ) );
//                m_chars_looked_for.add( new AbsChar( CF_NONSPACE, ParSel.NULL ) );
//            }
//            else
                m_chars_looked_for.add( new AbsChar( CF_ALPHA | CF_NUMBER,
                                                     ParSel.NULL ) ); // TODO: add dash
        }
        else if( wl_str.equals( "mailto" ) ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_UNDERSCORE | CF_ALPHA | CF_NUMBER,
                                                 ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_AT, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_ALPHA | CF_NUMBER, ParSel.NULL ) ); // TODO: add dash
        }
        else if( wl_str.equals( "deid" ) && m_flag_hidden_link ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_TAB, ParSel.JK_LNHT, true ) );
            m_chars_looked_for.add( new AbsChar( CF_NONSPACE - CF_MORE, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_MORE, ParSel.AP_LNID ) );
            m_pos_start = pos_word;
            return;
        }
        else
            return;

        if( m_flag_hidden_link ) {
            m_chars_looked_for.add( new AbsChar( CF_TAB, ParSel.JK_LNHT, true ) );
            m_chars_looked_for.add( new AbsChar( CF_NONSPACE - CF_MORE, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_MORE, ParSel.AP_LINK ) );
        }
        else {
            m_chars_looked_for.add( new AbsChar( CF_TAB | CF_NEWLINE | CF_SPACE, ParSel.AP_LINK ) );
        }
        m_pos_start = pos_word;
    }

    private void trigger_link_at() {
        if( ( m_cf_last & CF_SEPARATOR ) != 0 )
            return;

        m_flag_hidden_link = false;
        word_last.insert( 0, "mailto:" );
        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_ALPHA | CF_NUMBER, ParSel.NULL ) ); // TODO: add dash
        m_chars_looked_for.add( new AbsChar( CF_TAB | CF_NEWLINE | CF_SPACE, ParSel.AP_LINK ) );
        m_cf_req = CF_ANY;
        m_pos_start = pos_word;
    }

    private void trigger_link_date() {
        m_cf_req = CF_ANY;
        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_DOTYM, ParSel.JK_DDYM, true ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_DOTMD, ParSel.JK_DDMD, true ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.NULL ) );
        m_chars_looked_for.add( new AbsChar( CF_NUMBER, ParSel.JK_LNDT, true ) );

        m_flag_hidden_link = ( word_last.toString().equals( "<" ) );
        if( m_flag_hidden_link ) {
            m_chars_looked_for.add( new AbsChar( CF_TAB, ParSel.JK_LNHT, true ) );
            m_chars_looked_for.add( new AbsChar( CF_NONSPACE, ParSel.NULL ) );
            m_chars_looked_for.add( new AbsChar( CF_MORE, ParSel.AP_LNDT ) );
            m_pos_start = m_pos_current - 1;
        }
        else {
            m_pos_start = m_pos_current;
            // applier is called by junction_link_date() in this case
        }
    }

    private void trigger_list() {
        if( m_cf_last != CF_NEWLINE )
            return;

        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_NONTAB, ParSel.JK_LIST, true ) );
        m_cf_req = CF_ANY;
        m_pos_start = m_pos_current;
    }

    private void trigger_ignore() {
        if( m_cf_last == CF_NEWLINE ) {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_TAB | CF_IMMEDIATE, ParSel.JK_IGNR, true ) );
            m_cf_req = CF_ANY;
            m_pos_start = m_pos_current;
        }
    }

    private void junction_link_date() {
        date_last.set_day( int_last );

        if( date_last.is_valid() ) {
            if( m_flag_hidden_link ) {
                m_chars_looked_for.remove( 0 );
                return;
            }
            else
                apply_link_date();
        }

        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
    }

    private void junction_link_hidden_tab() {
        m_chars_looked_for.remove( 0 );
        pos_tab = m_pos_current + 1;
        id_last = int_last;     // if not id link assignment is in vain
    }

    private void junction_list() {
        //apply_indent();
        m_cf_req = CF_ANY;

        if( m_char_current == '[' ) {
            m_chars_looked_for.remove( 0 );
            m_chars_looked_for.add( new AbsChar( CF_SPACE | CF_TODO | CF_IMMEDIATE,
                                                 ParSel.JK_LST2, true ) );

            m_chars_looked_for.add( new AbsChar( CF_SBE | CF_IMMEDIATE, ParSel.NULL ) );
        }
        else {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
        }
    }

    private void junction_list2() {
        m_cf_req = CF_ANY;

        switch( m_char_current )
        {
            case ' ':
                m_chars_looked_for.remove( 0 );
                m_chars_looked_for.add( new AbsChar( CF_SPACE | CF_IMMEDIATE, ParSel.AP_CUNF ) );
                break;
            case '~':
                m_chars_looked_for.remove( 0 );
                m_chars_looked_for.add( new AbsChar( CF_SPACE | CF_IMMEDIATE, ParSel.AP_CPRG ) );
                break;
            case '+':
                m_chars_looked_for.remove( 0 );
                m_chars_looked_for.add( new AbsChar( CF_SPACE | CF_IMMEDIATE, ParSel.AP_CFIN ) );
                break;
            case 'x':
            case 'X':
            case '>':
                m_chars_looked_for.remove( 0 );
                m_chars_looked_for.add( new AbsChar( CF_SPACE | CF_IMMEDIATE, ParSel.AP_CCCL ) );
                break;
            default:
                m_chars_looked_for.clear();
                m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
                break;
        }
    }

    private void junction_date_dotym() { // dot between year and month
        if( int_last >= Date.YEAR_MIN && int_last <= Date.YEAR_MAX ) {
            date_last.set_year( int_last );
            m_chars_looked_for.remove( 0 );
        }
        else {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
        }
    }

    private void junction_date_dotmd() { // dot between month and day
        if( int_last >= 1 && int_last <= 12
            // two separators must be the same:
            && m_char_current == word_last.charAt( word_last.length() - 3 ) ) {
            date_last.set_month( int_last );
            m_chars_looked_for.remove( 0 );
        }
        else {
            m_chars_looked_for.clear();
            m_chars_looked_for.add( new AbsChar( CF_NOTHING, ParSel.NULL ) );
        }
    }

    private void junction_ignore() {
        m_chars_looked_for.clear();
        m_chars_looked_for.add( new AbsChar( CF_IGNORE, ParSel.NULL ) );
        apply_ignore();
    }

    // APPLIERS ====================================================================================
    private void apply_heading() {
        int end = 0;
        if( mEditText.getText().charAt( 0 ) != '\n' )
            end = mEditText.getText().toString().indexOf( '\n' );
        if( end == -1 )
            end = mEditText.getText().length();

        addSpan( new TextAppearanceSpan( getContext(), R.style.headingSpan ), 0, end,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new ForegroundColorSpan( mEntry.get_theme().color_heading ), 0, end, 0 );

        if( !mFlagSetTextOperation ) {
            mEntry.m_name = mEditText.getText().toString().substring( 0, end );
            // handle_entry_title_changed() will not be used here in Android
        }
    }

    private void apply_subheading() {
        int end = mEditText.getText().toString().indexOf( '\n', m_pos_start );
        if( end == -1 )
            end = mEditText.getText().length();

        addSpan( new TextAppearanceSpan( getContext(), R.style.subheadingSpan ), m_pos_start, end,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new ForegroundColorSpan( mEntry.get_theme().color_subheading ),
                 m_pos_start, end, 0 );
    }

    private void apply_bold() {
        apply_markup( new SpanBold() );
    }

    private void apply_italic() {
        apply_markup( new SpanItalic() );
    }

    private void apply_strikethrough() {
        apply_markup( new SpanStrikethrough() );
    }

    private void apply_highlight() {
        apply_markup( new SpanHighlight() );
    }

    private void apply_markup( Object span ) {
        addSpan( new RelativeSizeSpan( sMarkupScale ), m_pos_start, m_pos_start + 1,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new SpanMarkup(), m_pos_start, m_pos_start + 1, 0 );

        addSpan( span, m_pos_start + 1, m_pos_current, 0 );

        addSpan( new RelativeSizeSpan( sMarkupScale ), m_pos_current, m_pos_current + 1,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new SpanMarkup(), m_pos_current, m_pos_current + 1, 0 );
    }

    private void apply_comment() {
        addSpan( new TextAppearanceSpan( getContext(), R.style.commentSpan ), m_pos_start,
                 m_pos_current + 1,
                 Spanned.SPAN_INTERMEDIATE );

        addSpan( new ForegroundColorSpan( mColorMid ), m_pos_start, m_pos_current + 1,
                 Spanned.SPAN_INTERMEDIATE );

        addSpan( new SuperscriptSpan(), m_pos_start, m_pos_current + 1, 0 );
    }

    private void apply_ignore() {
        int end = m_pos_current;
        if( mEditText.getText().charAt( end ) != '\n' )
            end = mEditText.getText().toString().indexOf( '\n', end );
        if( end < 0 )
            end = mEditText.getText().length();
        addSpan( new ForegroundColorSpan( mColorMid ), m_pos_start, end, 0 );
    }

    private void apply_hidden_link_tags( int end, Object spanLink ) {
        addSpan( new RelativeSizeSpan( sMarkupScale ), m_pos_start, pos_tab,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new SpanMarkup(), m_pos_start, pos_tab, 0 );
        addSpan( new RelativeSizeSpan( sMarkupScale ), m_pos_current, end,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new SpanMarkup(), m_pos_current, end, 0 );

        addSpan( spanLink, pos_tab, m_pos_current, 0 );
    }

    private void apply_link() {
        if( m_flag_hidden_link )
            apply_hidden_link_tags( m_pos_current + 1, new LinkUri( word_last.toString() ) );
        else
            addSpan( new LinkUri( word_last.toString() ), m_pos_start, m_pos_current, 0 );
    }

    private void apply_link_id() {
        DiaryElement element = Diary.diary.get_element( id_last );

        if( element != null ) {
            if( element.get_type() == DiaryElement.Type.ENTRY ) {
                apply_hidden_link_tags( m_pos_current + 1, new LinkID( id_last ) );
                //return;
            }
        }
        // TODO: indicate dead links
    }

    private void apply_link_date() {
        LinkStatus status = LinkStatus.LS_OK;
        Entry ptr2entry = Diary.diary.get_entry_by_date( date_last.m_date + 1 ); // + 1 fixes order
        if( ptr2entry == null ) {
            if( ! Diary.diary.can_enter_edit_mode() )
                return;
            status = LinkStatus.LS_ENTRY_UNAVAILABLE;
        }
        else if( date_last.get_pure() == mEntry.m_date.get_pure() )
            status = ( Diary.diary.get_entry_count_on_day( date_last ) > 1 ) ?
                     LinkStatus.LS_OK : LinkStatus.LS_CYCLIC;

        if( status == LinkStatus.LS_OK || status == LinkStatus.LS_ENTRY_UNAVAILABLE ) {
            int end = m_pos_current + 1;

            if( m_flag_hidden_link )
                apply_hidden_link_tags( end, new LinkDate( date_last.m_date ) );
            else
                addSpan( new LinkDate( date_last.m_date ), m_pos_start, end, 0 );
        }
    }

    private void apply_check( Object tag_box, Object tag/*, int c*/ ) {
        int pos_start = m_pos_current - 3;
        int pos_box = m_pos_current;
        int pos_end = mEditText.getText().toString().indexOf( '\n', m_pos_current );
        if( pos_end == -1 )
            pos_end = mEditText.getText().length();
        /*if( ! Diary.diary.is_read_only() )
            m_list_links.push_back( new LinkCheck( create_mark( iter_start ),
                                                   create_mark( iter_box ),
                                                   c ) );*/

        addSpan( tag_box, pos_start, pos_box, 0 );
        addSpan( new TypefaceSpan( "monospace" ), pos_start, pos_box, 0 );
        if( tag != null )
            addSpan( tag, pos_box + 1, pos_end, 0 ); // ++ to skip separating space char
    }

    private void apply_check_unf() {
        apply_check( new ForegroundColorSpan( Theme.s_color_todo ), new SpanBold() );
    }

    private void apply_check_prg() {
        apply_check( new ForegroundColorSpan( Theme.s_color_progressed ), null );
    }

    private void apply_check_fin() {
        apply_check( new ForegroundColorSpan( Theme.s_color_done ),
                     new BackgroundColorSpan( Theme.s_color_done ) );
    }

    private void apply_check_ccl() {
        apply_check( new ForegroundColorSpan( Theme.s_color_canceled ),
                     new SpanStrikethrough() );
    }

    private void apply_match() {
        addSpan( new BackgroundColorSpan( mColorMatchBG ), pos_search, m_pos_current + 1,
                 Spanned.SPAN_INTERMEDIATE );
        addSpan( new ForegroundColorSpan( mEntry.get_theme().color_base ),
                 pos_search, m_pos_current + 1, 0 );
    }

    private void update_todo_status() {
        if( ( mEntry.get_status() & ES_NOT_TODO ) != 0 ) {
            Entry.calculate_todo_status( mEntry.get_text() );
            updateIcon();
            // Not relevant now: panel_diary->handle_elem_changed( m_ptr2entry );
        }
    }
}
