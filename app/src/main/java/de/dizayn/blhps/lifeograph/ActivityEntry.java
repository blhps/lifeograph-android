/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

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

package de.dizayn.blhps.lifeograph;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityEntry extends Activity {
    // ENTRY PARSER ENUMS
    public final int LF_NOTHING = 0x1;
    public final int LF_NEWLINE = 0x2;
    public final int LF_PUNCTUATION_RAW = 0x4;
    public final int LF_SPACE = 0x8; // space that will come eventually
    public final int LF_TAB = 0x10;
    public final int LF_IMMEDIATE = 0x20; // indicates contiguity of chars

    public final int LF_ASTERISK = 0x40; // bold
    public final int LF_UNDERSCORE = 0x80; // italic
    public final int LF_EQUALS = 0x100; // strikethrough
    public final int LF_HASH = 0x400; // highlight
    public final int LF_MARKUP = LF_ASTERISK | LF_UNDERSCORE | LF_EQUALS | LF_HASH;

    public final int LF_SLASH = 0x800;
    public final int LF_ALPHA = 0x1000;
    public final int LF_NUMBER = 0x2000;
    public final int LF_AT = 0x4000; // email
    public final int LF_CHECKBOX = 0x8000;

    public final int LF_DOTYM = 0x10000;
    public final int LF_DOTMD = 0x20000;
    public final int LF_DOTDATE = 0x30000; // DOTMD | DOTYM

    public final int LF_LESS = 0x80000; // tagging
    public final int LF_MORE = 0x100000;
    public final int LF_SBB = 0x200000; // square bracket begin: comments
    public final int LF_SBE = 0x400000; // square bracket end: comments

    public final int LF_APPLY = 0x1000000;
    public final int LF_JUNCTION = 0x2000000;
    public final int LF_IGNORE = 0x40000000;
    public final int LF_EOT = 0x80000000; // End of Text

    public final int LF_PUNCTUATION = LF_PUNCTUATION_RAW | LF_SLASH | LF_DOTDATE | LF_LESS
                                      | LF_MORE | LF_SBB | LF_SBE;
    public final int LF_FORMATCHAR = LF_ASTERISK | LF_UNDERSCORE | LF_EQUALS | LF_HASH | LF_SBB
                                     | LF_SBE;
    public final int LF_NUM_SLSH = LF_NUMBER | LF_SLASH;
    public final int LF_NUM_CKBX = LF_NUMBER | LF_CHECKBOX;
    public final int LF_NONSPACE = LF_PUNCTUATION | LF_MARKUP | LF_ALPHA | LF_NUMBER | LF_AT
                                   | LF_CHECKBOX;

    public final int CC_NONE = 0;
    public final int CC_NUMBER = 0x10;
    public final int CC_ALPHA = 0x20;
    public final int CC_ALPHANUM = 0x30;
    public final int CC_SIGN = 0x40;

    public final int CC_SPACE = 0x100;
    public final int CC_TAB = 0x200;
    public final int CC_NEWLINE = 0x400;
    public final int CC_SEPARATOR = 0x700;
    public final int CC_NOT_SEPARATOR = 0xF8FF;

    public final int CC_ANY = 0xFFFF;

    // PARSER SELECTOR (NEEDED DUE TO LACK OF FUNCTION POINTERS IN JAVA)
    protected enum ParSel {
        NULL, TR_HEAD, TR_SUBH, TR_BOLD, TR_ITLC, TR_STRK, TR_HILT, TR_CMNT, TR_LINK, TR_LNAT,
        TR_LNKD, TR_LIST, TR_IGNR, AP_HEND, AP_SUBH, AP_BOLD, AP_ITLC, AP_HILT, AP_STRK, AP_CMNT,
        AP_LINK, JK_IGNR, JK_DDYM, JK_DDMD, JK_LNKD
    }

    protected enum LinkStatus {
        LS_OK, LS_ENTRY_UNAVAILABLE, LS_INVALID, // separator: to check a valid entry link:
                                                 // linkstatus < LS_INVALID
        LS_CYCLIC, LS_FILE_OK, LS_FILE_INVALID, LS_FILE_UNAVAILABLE, LS_FILE_UNKNOWN
    }

    protected ActionBar mActionBar = null;
    protected ImageView mImageEntry = null;
    protected TextView mTextViewElemSub = null;
    public EditText mEditText = null;
    public Button mButtonBold = null;
    public Button mButtonItalic = null;
    public Button mButtonStrikethrough = null;
    public Button mButtonHighlight = null;
    public Button mButtonTags = null;
    boolean m_flag_settextoperation = false;
    boolean m_flag_entry_changed = false;
    boolean mFlagDismissOnExit = false;
    protected ListView mListViewTags = null;
    public Entry m_ptr2entry = null;
    protected ArrayAdapter< String > m_adapter_tags;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        Lifeobase.activityEntry = this;
        setContentView( R.layout.entry );

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled( true );

        mImageEntry = ( ImageView ) findViewById( R.id.imageViewEntry );
        mEditText = ( EditText ) findViewById( R.id.editTextEntry );
        // mEditText.setMovementMethod( LinkMovementMethod.getInstance() );

        // set custom font as the default font may lack the necessary chars such as check
        // marks:
        Typeface font = Typeface.createFromAsset( getAssets(), "OpenSans-Regular.ttf" );
        mEditText.setTypeface( font );

        mEditText.addTextChangedListener( new TextWatcher() {
            public void afterTextChanged( Editable s ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                // if( m_flag_settextoperation == false )
                {
                    pos_start = 0;
                    pos_end = mEditText.getText().length();
                    // if( start > 0 ) {
                    // pos_start = mEditText.getText().toString().indexOf( '\n', start - 1 );
                    // if( pos_start == -1 )
                    // pos_start = 0;
                    // }
                    //
                    // if( start < pos_end ) {
                    // pos_end = mEditText.getText().toString().indexOf( '\n', start + count );
                    // if( pos_end == -1 )
                    // pos_end = mEditText.getText().length();
                    // }
                    m_flag_entry_changed = true;
                }
                parse_text();
            }
        } );

        mTextViewElemSub = ( TextView ) findViewById( R.id.textViewElemSub );
        mImageEntry.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                changeView();
            }
        } );

        mButtonBold = ( Button ) findViewById( R.id.buttonBold );
        SpannableString spanStringB = new SpannableString( "B" );
        spanStringB.setSpan( new StyleSpan( Typeface.BOLD ), 0, 1, 0 );
        mButtonBold.setText( spanStringB );
        mButtonBold.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                toggleFormat( "*" );
            }
        } );

        mButtonItalic = ( Button ) findViewById( R.id.buttonItalic );
        SpannableString spanStringI = new SpannableString( "I" );
        spanStringI.setSpan( new StyleSpan( Typeface.ITALIC ), 0, 1, 0 );
        mButtonItalic.setText( spanStringI );
        mButtonItalic.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                toggleFormat( "_" );
            }
        } );

        mButtonStrikethrough = ( Button ) findViewById( R.id.buttonStrikethrough );
        SpannableString spanStringS = new SpannableString( "S" );
        spanStringS.setSpan( new StrikethroughSpan(), 0, 1, 0 );
        mButtonStrikethrough.setText( spanStringS );
        mButtonStrikethrough.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                toggleFormat( "=" );
            }
        } );

        mButtonHighlight = ( Button ) findViewById( R.id.buttonHighlight );
        SpannableString spanStringH = new SpannableString( "H" );
        spanStringH.setSpan( new BackgroundColorSpan( Color.YELLOW ), 0, 1, 0 );
        mButtonHighlight.setText( spanStringH );
        mButtonHighlight.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                toggleFormat( "#" );
            }
        } );

        // mButtonFavorite = ( Button ) findViewById( R.id.buttonFavorite );
        // mButtonFavorite.setOnClickListener( new View.OnClickListener() {
        // public void onClick( View v ) {
        // toggleFavorite();
        // }
        // } );

        mButtonTags = ( Button ) findViewById( R.id.buttonTags );
        mButtonTags.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                showTagDialog();
            }
        } );
        m_adapter_tags =
                new ArrayAdapter< String >( this,
                                            android.R.layout.simple_list_item_multiple_choice,
                                            android.R.id.text1 );
        // if( savedInstanceState == null )
        show( ActivityDiary.entry_current, savedInstanceState == null );
        // flag_rotation = false;
    }

    // boolean flag_rotation = false;

    @Override
    public void onPause() {
        if( mFlagDismissOnExit )
            Diary.diary.dismiss_entry( m_ptr2entry );
        else
            sync();
        Lifeobase.activityDiary.update_entry_list();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_entry, menu );
        return true;
    }

    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask( this );
                finish();
                return true;
            case R.id.toggle_favorite:
                toggleFavorite();
                return true;
            case R.id.dismiss:
                dismiss();
                return true;
        }

        return super.onMenuItemSelected( featureId, item );
    }

    public void changeView() {
        finish();
    }

    public void sync() {
        if( m_flag_entry_changed ) {
            m_ptr2entry.m_date_changed = ( int ) ( System.currentTimeMillis() / 1000L );
            m_ptr2entry.m_text = mEditText.getText().toString();
            m_flag_entry_changed = false;
        }
    }

    /*
     * protected Dialog onCreateDialog(int id) { Dialog dialog; switch(id) { case 1: // do the
     * work to define the pause Dialog break; default: dialog = null; } return dialog; }
     */

    public void show( Entry entry, boolean flagParse ) {
        mFlagDismissOnExit = false;
        m_ptr2entry = entry;

        // PARSING
        pos_start = 0;
        pos_end = entry.get_text().length();

        // SETTING TEXT
        // m_flag_settextoperation = true;
        if( flagParse )
            mEditText.setText( entry.get_text() );
        // m_flag_settextoperation = false;

        // if( flagParse )
        // parse_text();

        setTitle( entry.getHeadStr() );
        mActionBar.setIcon( entry.get_icon() );
        mTextViewElemSub.setText( entry.getSubStr() );
        update_tag_button();
    }

    public void update_tag_button() {
        mButtonTags.setText( m_ptr2entry.m_tags.size() + " Tag(s)..." );
    }

    public void toggleFavorite() {
        m_ptr2entry.toggle_favored();
        mImageEntry.setImageResource( m_ptr2entry.get_icon() );
    }

    public void dismiss() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.entry_dismiss_confirm ).setCancelable( false )
               .setPositiveButton( R.string.dismiss, new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       mFlagDismissOnExit = true;
                       ActivityEntry.this.finish();
                   }
               } ).setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       dialog.cancel();
                   }
               } );
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showTagDialog() {
        Dialog dialog = new DialogTags( this );
        dialog.show();
    }

    // TAG DIALOG =================================================================================
    public class DialogTags extends Dialog {
        protected EditText editText;
        protected Button buttonAdd;

        public DialogTags( Context context ) {
            super( context );
        }

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            super.onCreate( savedInstanceState );

            setContentView( R.layout.dialog_tags );
            setTitle( "Edit Entry Tags" );
            setCancelable( true );
            setOnDismissListener( new android.content.DialogInterface.OnDismissListener() {
                public void onDismiss( android.content.DialogInterface dialog ) {
                    update_tag_button();
                }
            } );

            mListViewTags = ( ListView ) findViewById( R.id.listViewTags );
            mListViewTags.setAdapter( m_adapter_tags );
            mListViewTags.setItemsCanFocus( false );
            mListViewTags.setChoiceMode( ListView.CHOICE_MODE_MULTIPLE );
            mListViewTags.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                public void onItemClick( AdapterView< ? > parent, View view, int pos, long id ) {
                    Tag tag = Diary.diary.m_tags.get( m_adapter_tags.getItem( pos ) );
                    if( mListViewTags.isItemChecked( pos ) )
                        m_ptr2entry.add_tag( tag );
                    else
                        m_ptr2entry.remove_tag( tag );
                }
            } );

            buttonAdd = ( Button ) findViewById( R.id.buttonAddTag );
            buttonAdd.setOnClickListener( new View.OnClickListener() {
                public void onClick( View v ) {
                    create_tag();
                }
            } );
            buttonAdd.setEnabled( false );

            editText = ( EditText ) findViewById( R.id.editTextTag );
            editText.addTextChangedListener( new TextWatcher() {
                public void afterTextChanged( Editable s ) {
                }

                public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                }

                public void onTextChanged( CharSequence s, int start, int before, int count ) {
                    update_list( s );
                    if( s.length() > 0 )
                        buttonAdd.setEnabled( Diary.diary.m_tags.get( s.toString() ) == null );
                    else
                        buttonAdd.setEnabled( false );
                }
            } );
            editText.setOnEditorActionListener( new TextView.OnEditorActionListener() {
                public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                    if( v.getText().length() > 0 ) {
                        create_tag();
                        return true;
                    }
                    return false;
                }
            } );

            update_list( null );
        }

        private void create_tag() {
            Tag tag = Diary.diary.create_tag( editText.getText().toString(), null );
            m_ptr2entry.add_tag( tag );
            editText.setText( "" );
        }

        private void update_list( CharSequence filter ) {
            m_adapter_tags.clear();
            int i = 0;
            for( Tag t : Diary.diary.m_tags.values() ) {
                if( filter != null )
                    if( !t.get_name().contains( filter ) )
                        continue;
                m_adapter_tags.add( t.get_name() );
                mListViewTags.setItemChecked( i, m_ptr2entry.m_tags.contains( t ) );
                i++;
            }
        }

    }

    // FORMATTING BUTTONS
    public void toggleFormat( String markup ) {
        int iter_start, iter_end;
        if( mEditText.hasSelection() ) {
            int pos_start = -2, pos_end = -1;
            boolean properly_separated = false;

            iter_start = mEditText.getSelectionStart();
            iter_end = mEditText.getSelectionEnd();
            iter_end--;

            if( iter_start > 0 ) {
                --iter_start; // also evaluate the previous character
            }
            else {
                properly_separated = true;
                pos_start = -2;
            }

            for( ;; ++iter_start ) {
                if( is_marked_up_region( markup.charAt( 0 ), iter_start ) )
                    return;
                switch( mEditText.getText().charAt( iter_start ) ) {
                // do nothing if selection spreads over more than one line:
                    case '\n':
                        if( pos_start > -2 )
                            return;
                        /* else no break */
                    case ' ':
                    case '\t':
                        if( pos_start == -2 ) {
                            properly_separated = true;
                            pos_start = -1;
                        }
                        break;
                    // case '*':
                    // case '_':
                    // case '#':
                    // case '=':
                    // if( iter_start.get_char() == markup[ 0 ] )
                    // break;
                    /* else no break */
                    default:
                        if( pos_start == -2 )
                            pos_start = -1;
                        else if( pos_start == -1 )
                            pos_start = iter_start;
                        pos_end = iter_start;
                        break;
                }
                if( iter_start == iter_end )
                    break;
            }
            // add markup chars to the beginning and end:
            if( pos_start >= 0 ) {
                if( properly_separated ) {
                    mEditText.getText().insert( pos_start, markup );
                    pos_end += 2;
                }
                else {
                    mEditText.getText().insert( pos_start, " " + markup );
                    pos_end += 3;
                }

                mEditText.getText().insert( pos_end, markup );
                // TODO place_cursor( get_iter_at_offset( pos_end ) );
            }
        }
        else // no selection case
        {
            iter_start = iter_end = mEditText.getSelectionStart();
            if( iter_start == 0 )
                return;
            char ch_st = mEditText.getText().charAt( iter_start );
            if( ch_st == '\n' || ch_st == '\t' || ch_st == ' ' ) {
                if( iter_start == mEditText.length() - 1 )
                    return;
                ch_st = mEditText.getText().charAt( ++iter_start );
                if( ch_st == '\n' || ch_st == '\t' || ch_st == ' ' )
                    return;
            }
            if( is_marked_up_region( markup.charAt( 0 ), iter_start ) ) {
                // TODO (if necessary) m_flag_ongoingoperation = true;

                iter_start = mEditText.getText().toString().lastIndexOf( markup, iter_start );
                // iter_start.backward_to_tag_toggle( tag );
                if( iter_start == -1 )
                    return;
                mEditText.getText().delete( iter_start, iter_start + 1 );
                // backspace( iter_start );

                iter_end = mEditText.getText().toString().indexOf( markup, iter_start );

                // TODO (if necessary) m_flag_ongoingoperation = false;

                mEditText.getText().delete( iter_end, iter_end + 1 );
                // backspace( ++iter_end );
            }
            // nested tags are not supported atm:
            else if( mEditText.getText().getSpans( iter_start, iter_end, StyleSpan.class ).length == 0 ) {
                // find word boundaries:
                while( iter_start > 0 ) {
                    char c = mEditText.getText().charAt( iter_start );
                    if( c == '\n' || c == ' ' || c == '\t' ) {
                        iter_start++;
                        break;
                    }

                    iter_start--;
                }
                mEditText.getText().insert( iter_start, markup );

                while( iter_end < mEditText.getText().length() ) {
                    char c = mEditText.getText().charAt( iter_end );
                    if( c == '\n' || c == ' ' || c == '\t' )
                        break;
                    iter_end++;
                }
                mEditText.getText().insert( iter_end, markup );
                // TODO (if necessary) place_cursor( offset );
            }
        }
    }

    /*
     * XXX ANOTHER APPROACH --NOT VERY LIKELY TO YIELD ANY USEFUL RESULT THOUGH... public void
     * ttttt( char char_markup, int pos ) { char char_current; int word_start = -1; int
     * para_start = -1; int word_end = -1; int para_end = -1; int mark_start = -1; int mark_end
     * = -1;
     * 
     * boolean last_is_space = false; boolean nonspace_found = false; for( int i = pos; i!=0;
     * i-- ) { char_current = mEditText.getText().charAt( i ); switch( char_current ) { case
     * '\n': para_start = i; break; case ' ': case '\t': if( nonspace_found ) word_start = i +
     * 1; last_is_space = true; break; default: if( char_current == char_markup ) { if( !
     * last_is_space ) mark_start = i; } else last_is_space = false; } } }
     */

    // TODO: to be improved to handle corner cases
    protected int find_markup( char char_markup, int pos, int step, int limit ) {
        char char_current;
        boolean last_is_space = false;
        for( int i = pos; i != limit; i += step ) {
            char_current = mEditText.getText().charAt( i );
            if( char_current == '\n' )
                break;
            else if( char_current == ' ' || char_current == '\t' )
                last_is_space = true;
            else if( char_current == char_markup ) {
                if( !last_is_space )
                    return i;
            }
            else
                last_is_space = false;
        }

        return -1;
    }

    protected int find_markup_begin( char char_markup, int pos ) {
        return find_markup( char_markup, pos, -1, 0 );
    }

    protected int find_markup_end( char char_markup, int pos ) {
        return find_markup( char_markup, pos, 1, mEditText.length() );
    }

    protected boolean is_marked_up_region( char char_markup, int pos ) {
        return( find_markup( char_markup, pos, -1, 0 ) != -1 && find_markup( char_markup, pos, 1,
                                                                             mEditText.length() ) != -1 );
    }

    // PARSING VARIABLES ==========================================================================
    protected int pos_start, pos_current, pos_end, pos_word, pos_regular;
    protected char char_current;
    protected int char_last, char_req = CC_ANY;
    protected String word_last;
    protected long int_last, id_last;
    protected Date date_last = new Date();
    protected java.util.List< Integer > lookingfor = new ArrayList< Integer >();
    protected java.util.List< ParSel > m_appliers = new ArrayList< ParSel >();
    protected ParSel m_applier_nl;
    protected java.util.Vector< java.lang.Object > m_spans =
            new java.util.Vector< java.lang.Object >();

    protected void reset( int start, int end ) {
        // pos_start = start;
        // pos_end = end;
        pos_current = pos_word = pos_regular = start;

        // TODO: only remove spans within the parsing boundaries...
        // mEditText.getText().clearSpans(); <-- problematic!!
        for( java.lang.Object span : m_spans )
            mEditText.getText().removeSpan( span );
        m_spans.clear();

        char_last = CC_NONE;
        char_req = CC_ANY;
        word_last = "";
        int_last = 0;
        date_last.set( 0 );
        id_last = 0;
        lookingfor.clear();
        m_appliers.clear();
        if( start == 0 && end > 0 ) {
            lookingfor.add( LF_IGNORE ); // to prevent formatting within title
            m_applier_nl = ParSel.AP_HEND;
            apply_heading();
        }
        else {
            lookingfor.add( LF_NOTHING );
            m_applier_nl = ParSel.NULL;
        }
    }

    public void parse_text( /* int start, int end */) {
        // everything below should go to Parser when there is one (and there is nothing above
        // as of yet...)
        reset( pos_start, pos_end );

        for( ; pos_current < pos_end; ++pos_current ) {
            char_current = mEditText.getText().charAt( pos_current );

            /*
             * TODO if( m_search_str.size() > 0 ) { if( m_search_str[ i_search ] ==
             * Glib::Unicode::tolower( char_current ) ) { if( i_search == 0 ) pos_search =
             * pos_current; if( i_search == i_search_end ) { apply_match(); i_search = 0; }
             * else i_search++; } else { i_search = 0; } }
             */

            // MARKUP PARSING
            switch( char_current ) {
                case '\n':
                case '\r':
                    process_char( LF_NEWLINE, LF_NUM_CKBX | LF_ALPHA | LF_FORMATCHAR | LF_SLASH
                                              | LF_DOTDATE | LF_MORE | LF_TAB | LF_IGNORE, 0,
                                  ParSel.NULL, CC_NEWLINE );
                    break;
                case ' ':
                    process_char( LF_SPACE, LF_ALPHA | LF_NUMBER | LF_SLASH | LF_DOTDATE
                                            | LF_CHECKBOX, LF_NOTHING, ParSel.TR_SUBH, CC_SPACE );
                    break;
                case '*':
                    process_char( LF_ASTERISK, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE,
                                  LF_NOTHING, ParSel.TR_BOLD, CC_SIGN );
                    break;
                case '_':
                    process_char( LF_UNDERSCORE, LF_NUM_CKBX | LF_SLASH | LF_DOTDATE, LF_NOTHING,
                                  ParSel.TR_ITLC, CC_SIGN );
                    break;
                case '=':
                    process_char( LF_EQUALS, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE,
                                  LF_NOTHING, ParSel.TR_STRK, CC_SIGN );
                    break;
                case '#':
                    process_char( LF_HASH, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE,
                                  LF_NOTHING, ParSel.TR_HILT, CC_SIGN );
                    break;
                case '[':
                    process_char( LF_SBB, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE,
                                  LF_NOTHING, ParSel.TR_CMNT, CC_SIGN );
                    break;
                case ']':
                    process_char( LF_SBE, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE, 0,
                                  ParSel.NULL, CC_SIGN );
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
                    process_char( LF_NUMBER, LF_SLASH | LF_ALPHA | LF_DOTDATE | LF_CHECKBOX,
                                  LF_NOTHING, ParSel.TR_LNKD, CC_NUMBER );
                    break;
                case '.':
                    process_char( LF_DOTDATE, LF_NUM_CKBX | LF_ALPHA | LF_SLASH, LF_NOTHING,
                                  ParSel.TR_IGNR, CC_SIGN );
                    break;

                case '-':
                    process_char( LF_DOTDATE, LF_NUM_CKBX | LF_ALPHA | LF_SLASH, 0, ParSel.NULL,
                                  CC_SIGN );
                    break;
                case '/':
                    process_char( LF_SLASH | LF_DOTDATE, LF_NUM_CKBX | LF_ALPHA, 0, ParSel.NULL,
                                  CC_SIGN );
                    break;
                case ':':
                    process_char( LF_PUNCTUATION_RAW, LF_NUM_CKBX | LF_ALPHA | LF_SLASH
                                                      | LF_DOTDATE, LF_NOTHING, ParSel.TR_LINK,
                                  CC_SIGN );
                    break;
                case '@':
                    process_char( LF_AT, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE,
                                  LF_NOTHING, ParSel.TR_LNAT, CC_SIGN );
                    break;
                case '>':
                    process_char( LF_MORE, LF_NUM_CKBX | LF_ALPHA | LF_SLASH | LF_DOTDATE, 0,
                                  ParSel.NULL, CC_SIGN );
                    break;
                case '\t':
                    process_char( LF_TAB, LF_NUM_SLSH | LF_ALPHA | LF_DOTDATE, LF_NOTHING,
                                  ParSel.TR_LIST, CC_TAB );
                    break;
                // LIST CHARS
                case '☐':
                case '☑':
                case '☒':
                    process_char( LF_CHECKBOX, LF_NUM_SLSH | LF_ALPHA | LF_DOTDATE, 0, ParSel.NULL,
                                  CC_SIGN );
                    break;
                default:
                    process_char( LF_ALPHA, LF_NUM_CKBX | LF_DOTDATE | LF_SLASH, 0, ParSel.NULL,
                                  CC_ALPHA ); // most probably :)
                    break;
            }
        }
        // end of the text -treated like new line
        process_char( LF_NEWLINE, LF_NUM_CKBX | LF_ALPHA | LF_FORMATCHAR | LF_SLASH | LF_DOTDATE
                                  | LF_MORE | LF_TAB, LF_EOT, ParSel.NULL, CC_NEWLINE );
    }

    // SELECT PARSING FUNCTION ====================================================================
    protected void selectParsingFunc( ParSel ps ) {
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

            case AP_SUBH:
                apply_subheading();
                break;
            case AP_BOLD:
                apply_bold();
                break;
            case AP_ITLC:
                apply_italic();
                break;
            case AP_STRK:
                apply_strikethrough();
                break;
            case AP_HILT:
                apply_highlight();
                break;
            case AP_CMNT:
                apply_comment();
                break;
            case AP_LINK:
                apply_link();
                break;
            case JK_IGNR:
                junction_ignore();
                break;
            case JK_DDYM:
                junction_date_dotym();
                break;
            case JK_DDMD:
                junction_date_dotmd();
                break;
            case JK_LNKD:
                junction_link_date();
                break;
            default:
                break;
        }
    }

    // PROCESS CHAR ===============================================================================
    protected void process_char( int satisfies, int breaks, int triggers, ParSel ps, int cc ) {
        int lf = lookingfor.get( 0 );

        if( ( satisfies & LF_NEWLINE ) != 0 ) {
            lookingfor.clear();
            lookingfor.add( LF_NOTHING );

            process_newline();
        }

        if( ( lf & satisfies ) != 0 ) {
            if( ( lf & LF_APPLY ) != 0 && ( char_last & char_req ) != 0 ) {
                if( ( satisfies & LF_NEWLINE ) == 0 ) {
                    lookingfor.clear();
                    lookingfor.add( LF_NOTHING );
                }

                selectParsingFunc( m_appliers.get( 0 ) ); // lookingfor has to
                                                          // be
                                                          // cleared beforehand
            }
            else if( ( lf & LF_JUNCTION ) != 0 ) {
                selectParsingFunc( m_appliers.get( 0 ) );
            }
            else {
                if( ( lf & LF_APPLY ) == 0 )
                    lookingfor.remove( 0 );
                if( ( lf & triggers ) != 0 )
                    selectParsingFunc( ps );
            }
        }
        else if( ( triggers & LF_EOT ) != 0 ) {
            if( m_applier_nl == ParSel.NULL ) {
                pos_start = pos_current + 1;
                // apply_regular();
            }
            process_newline();
        }
        else if( ( lf & breaks ) != 0 || ( lf & LF_IMMEDIATE ) != 0 ) {
            if( ( satisfies & LF_NEWLINE ) == 0 ) {
                lookingfor.clear();
                lookingfor.add( LF_NOTHING );
            }
            if( ( triggers & LF_NOTHING ) != 0 )
                selectParsingFunc( ps );
        }
        else if( ( lf & triggers ) != 0 ) {
            selectParsingFunc( ps );
        }

        // SET NEW CHAR CLASS & ADJUST WORD_LAST ACOORDINGLY
        if( ( cc & CC_SEPARATOR ) != 0 )
            word_last = "";
        else {
            if( word_last.length() == 0 )
                pos_word = pos_current;
            word_last += char_current;
        }
        char_last = cc;
    }

    // PROCESS NEWLINE ============================================================================
    protected void process_newline() {
        if( m_applier_nl != ParSel.NULL ) {
            selectParsingFunc( m_applier_nl );
            m_applier_nl = ParSel.NULL;
        }
    }

    // HANDLE NUMBER ==============================================================================
    void handle_number() {
        if( char_last == CC_NUMBER ) {
            int_last *= 10;
            int_last += ( char_current - '0' );
        }
        else
            int_last = ( char_current - '0' );
    }

    // PARSING TRIGGERERS =========================================================================
    protected void trigger_subheading() {
        if( char_last == CC_NEWLINE ) {
            lookingfor.clear();
            lookingfor.add( LF_NONSPACE | LF_APPLY );
            char_req = CC_ANY;
            pos_start = pos_current;
            m_appliers.clear();
            m_appliers.add( ParSel.AP_SUBH );
        }
    }

    protected void trigger_markup( int lf, ParSel ps ) {
        if( ( char_last & CC_NOT_SEPARATOR ) != 0 )
            return;

        lookingfor.clear();
        lookingfor.add( LF_NONSPACE - lf );
        lookingfor.add( lf | LF_APPLY );
        char_req = CC_NOT_SEPARATOR;
        pos_start = pos_current;
        m_appliers.clear();
        m_appliers.add( ps );
    }

    protected void trigger_bold() {
        trigger_markup( LF_ASTERISK, ParSel.AP_BOLD );
    }

    protected void trigger_italic() {
        trigger_markup( LF_UNDERSCORE, ParSel.AP_ITLC );
    }

    protected void trigger_strikethrough() {
        trigger_markup( LF_EQUALS, ParSel.AP_STRK );
    }

    protected void trigger_highlight() {
        trigger_markup( LF_HASH, ParSel.AP_HILT );
    }

    protected void trigger_comment() {
        lookingfor.clear();
        lookingfor.add( LF_SBB | LF_IMMEDIATE );
        lookingfor.add( LF_SBE );
        lookingfor.add( LF_SBE | LF_IMMEDIATE | LF_APPLY );
        char_req = CC_ANY;
        pos_start = pos_current;
        m_appliers.clear();
        m_appliers.add( ParSel.AP_CMNT );
    }

    protected void trigger_link() {
        // TODO:
        // m_flag_hidden_link = word_last[ 0 ] == '<';
        // if( m_flag_hidden_link )
        // word_last.erase( 0, 1 );

        char_req = CC_ANY;

        if( word_last.equals( "http" ) || word_last.equals( "https" ) || word_last.equals( "ftp" )
            || word_last.equals( "file" ) ) {
            lookingfor.clear();
            lookingfor.add( LF_SLASH );
            lookingfor.add( LF_SLASH );
            if( word_last.equals( "file" ) ) {
                lookingfor.add( LF_SLASH );
                lookingfor.add( LF_NONSPACE );
            }
            else
                lookingfor.add( LF_ALPHA | LF_NUMBER ); // TODO: add dash
        }
        else if( word_last.equals( "mailto" ) ) {
            lookingfor.clear();
            lookingfor.add( LF_UNDERSCORE | LF_ALPHA | LF_NUMBER );
            lookingfor.add( LF_AT );
            lookingfor.add( LF_ALPHA | LF_NUMBER ); // TODO: add dash
        }
        // else
        // if( word_last == "deid" && m_flag_hidden_link )
        // {
        // lookingfor.clear();
        // lookingfor.add( LF_NUMBER );
        // lookingfor.add( LF_TAB|LF_JUNCTION );
        // lookingfor.add( LF_NONSPACE - LF_MORE );
        // lookingfor.add( LF_MORE|LF_APPLY );
        // pos_start = pos_word;
        // m_appliers.clear();
        // m_appliers.push_back( JK_LNHT ); // link hidden tab
        // m_appliers.push_back( AP_LNKH ); // link hidden
        // return;
        // }
        else
            return;

        // if( m_flag_hidden_link )
        // {
        // lookingfor.push_back( LF_TAB|LF_JUNCTION );
        // lookingfor.push_back( LF_NONSPACE - LF_MORE );
        // lookingfor.push_back( LF_MORE|LF_APPLY );
        // m_appliers.clear();
        // m_appliers.push_back( &EntryParser::junction_link_hidden_tab );
        // m_appliers.push_back( &EntryParser::apply_link );
        // }
        // else
        {
            lookingfor.add( LF_TAB | LF_NEWLINE | LF_SPACE | LF_APPLY );
            m_appliers.clear();
            m_appliers.add( ParSel.AP_LINK );
        }
        pos_start = pos_word;
    }

    protected void trigger_link_at() {
        if( ( char_last & CC_SEPARATOR ) != 0 )
            return;

        // m_flag_hidden_link = false;
        word_last = "mailto:" + word_last;
        lookingfor.clear();
        lookingfor.add( LF_ALPHA | LF_NUMBER ); // TODO: add dash
        lookingfor.add( LF_TAB | LF_NEWLINE | LF_SPACE | LF_APPLY );
        char_req = CC_ANY;
        pos_start = pos_word;
        m_appliers.clear();
        m_appliers.add( ParSel.AP_LINK );
    }

    protected void trigger_link_date() {
        char_req = CC_ANY;
        lookingfor.clear();
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_DOTYM | LF_JUNCTION );
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_DOTMD | LF_JUNCTION );
        lookingfor.add( LF_NUMBER );
        lookingfor.add( LF_NUMBER | LF_JUNCTION );

        m_appliers.clear();
        m_appliers.add( ParSel.JK_DDYM ); // junction_date_dotym
        m_appliers.add( ParSel.JK_DDMD ); // junction_date_dotmd
        m_appliers.add( ParSel.JK_LNKD ); // junction_link_date - checks validity of the date

        // TODO:
        // m_flag_hidden_link = ( word_last.equals( "<" ) );
        // if( m_flag_hidden_link )
        // {
        // lookingfor.push_back( LF_TAB|LF_JUNCTION );
        // lookingfor.push_back( LF_NONSPACE );
        // lookingfor.push_back( LF_MORE|LF_APPLY );
        // pos_start = pos_current - 1;
        // m_appliers.push_back( &EntryParser::junction_link_hidden_tab );
        // m_appliers.push_back( &EntryParser::apply_link_date );
        // }
        // else
        {
            pos_start = pos_current;
            // applier is called by junction_link_date() in this case
        }
    }

    protected void trigger_list() {

    }

    protected void trigger_ignore() {
        if( char_last == CC_NEWLINE ) {
            lookingfor.clear();
            lookingfor.add( LF_TAB | LF_IMMEDIATE | LF_JUNCTION );
            char_req = CC_ANY;
            pos_start = pos_current;
            m_appliers.clear();
            m_appliers.add( ParSel.JK_IGNR );
        }
    }

    protected void junction_link_hidden_tab() {

    }

    protected void junction_list() {

    }

    protected void junction_date_dotym() { // dot between year and month
        if( int_last >= Date.YEAR_MIN && int_last <= Date.YEAR_MAX ) {
            date_last.set_year( ( int ) int_last );
            lookingfor.remove( 0 );
            m_appliers.remove( 0 );
        }
        else {
            lookingfor.clear();
            lookingfor.add( LF_NOTHING );
        }
    }

    protected void junction_date_dotmd() { // dot between month and day
        if( int_last >= 1 && int_last <= 12
        // two separators must be the same:
            && char_current == word_last.charAt( word_last.length() - 3 ) ) {
            date_last.set_month( ( int ) int_last );
            lookingfor.remove( 0 );
            m_appliers.remove( 0 );
        }
        else {
            lookingfor.clear();
            lookingfor.add( LF_NOTHING );
        }
    }

    protected void junction_link_date() {
        date_last.set_day( ( int ) int_last );

        if( date_last.is_valid() ) {
            // if( m_flag_hidden_link )
            // {
            // lookingfor.pop_front();
            // m_appliers.pop_front();
            // return;
            // }
            // else
            apply_link_date();
        }

        lookingfor.clear();
        lookingfor.add( LF_NOTHING );
    }

    protected void junction_ignore() {
        lookingfor.clear();
        lookingfor.add( LF_IGNORE );
        m_appliers.clear();
        apply_ignore();
    }

    // APPLIERS ===============================================================
    protected void apply_heading() {
        int end = 0;
        if( mEditText.getText().charAt( 0 ) != '\n' )
            end = mEditText.getText().toString().indexOf( '\n' );
        if( end == -1 )
            end = mEditText.getText().length();

        // m_spans.add( new StyleSpan( Typeface.BOLD ) );
        // mEditText.getText().setSpan( m_spans.lastElement(), 0, end, 0 );
        // m_spans.add( new RelativeSizeSpan( 1.4f ) );
        // mEditText.getText().setSpan( m_spans.lastElement(), 0, end,
        // Spanned.SPAN_INTERMEDIATE );
        m_spans.add( new TextAppearanceSpan( this, R.style.headingSpan ) );
        mEditText.getText().setSpan( m_spans.lastElement(), 0, end, Spanned.SPAN_INTERMEDIATE );
        m_spans.add( new ForegroundColorSpan( Color.BLUE ) );
        mEditText.getText().setSpan( m_spans.lastElement(), 0, end, 0 );

        if( !m_flag_settextoperation ) {
            m_ptr2entry.m_name = mEditText.getText().toString().substring( 0, end );
            // TODO: handle_entry_title_changed( m_ptr2entry );
        }
    }

    protected void apply_subheading() {
        int end = mEditText.getText().toString().indexOf( '\n', pos_start );
        if( end != -1 ) {
            m_spans.add( new TextAppearanceSpan( this, R.style.subheadingSpan ) );
            mEditText.getText().setSpan( m_spans.lastElement(), pos_start, end,
                                         Spanned.SPAN_INTERMEDIATE );
            m_spans.add( new ForegroundColorSpan( Color.MAGENTA ) );
            mEditText.getText().setSpan( m_spans.lastElement(), pos_start, end, 0 );
        }
    }

    protected void apply_bold() {
        apply_markup( new StyleSpan( Typeface.BOLD ) );
    }

    protected void apply_italic() {
        apply_markup( new StyleSpan( Typeface.ITALIC ) );
    }

    protected void apply_strikethrough() {
        apply_markup( new StrikethroughSpan() );
    }

    protected void apply_highlight() {
        apply_markup( new BackgroundColorSpan( Color.YELLOW ) );
    }

    protected void apply_markup( Object span ) {
        m_spans.add( new TextAppearanceSpan( this, R.style.markupSpan ) );
        mEditText.getText().setSpan( m_spans.lastElement(), pos_start, pos_start + 1, 0 );

        m_spans.add( span );
        mEditText.getText().setSpan( span, pos_start + 1, pos_current, 0 );

        m_spans.add( new TextAppearanceSpan( this, R.style.markupSpan ) );
        mEditText.getText().setSpan( m_spans.lastElement(), pos_current, pos_current + 1, 0 );
    }

    protected void apply_comment() {
        m_spans.add( new TextAppearanceSpan( this, R.style.commentSpan ) );
        mEditText.getText().setSpan( m_spans.lastElement(), pos_start, pos_current + 1,
                                     Spanned.SPAN_INTERMEDIATE );
        m_spans.add( new ForegroundColorSpan( Color.GRAY ) );
        mEditText.getText().setSpan( m_spans.lastElement(), pos_start, pos_current + 1,
                                     Spanned.SPAN_INTERMEDIATE );
        m_spans.add( new SuperscriptSpan() );
        mEditText.getText().setSpan( m_spans.lastElement(), pos_start, pos_current + 1, 0 );
    }

    protected void apply_ignore() {
        int end = pos_current;
        if( mEditText.getText().charAt( end ) != '\n' )
            end = mEditText.getText().toString().indexOf( '\n' ) - 1;
        if( end < 0 )
            end = mEditText.getText().length() - 1;
        // TODO:
        // m_spans.add( new BackgroundColorSpan( Color.GRAY ) );
        // mEditText.getText().setSpan( m_spans.lastElement(), pos_start, end, 0 );
    }

    protected void apply_link() {
        // if( m_flag_hidden_link )
        // {
        // Gtk::TextIter iter_end( get_iter_at_offset( pos_current + 1 ) );
        // Gtk::TextIter iter_url_start( get_iter_at_offset( pos_start + 1 ) );
        // Gtk::TextIter iter_tab( get_iter_at_offset( pos_tab ) );
        // m_list_links.push_back( new LinkUri( create_mark( iter_tab ),
        // create_mark( iter_current ),
        // get_slice( iter_url_start, iter_tab ) ) );
        //
        // apply_hidden_link_tags( iter_end, m_tag_link );
        // }
        // else
        {
            m_spans.add( new ClickableSpan() {
                String uri = new String( word_last );

                @Override
                public void onClick( View widget ) {
                    Intent browserIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( uri ) );
                    startActivity( browserIntent );
                }
            } );

            mEditText.getText().setSpan( m_spans.lastElement(), pos_start, pos_current, 0 );
        }
    }

    void apply_link_date() {
        LinkStatus status = LinkStatus.LS_OK;
        Entry ptr2entry = Diary.diary.get_entry( date_last.m_date + 1 ); // + 1
                                                                         // fixes
                                                                         // order
        if( ptr2entry == null )
            status = LinkStatus.LS_ENTRY_UNAVAILABLE;
        else if( date_last.get_pure() == m_ptr2entry.m_date.get_pure() )
            status =
                    Diary.diary.get_day_has_multiple_entries( date_last ) ? LinkStatus.LS_OK
                                                                         : LinkStatus.LS_CYCLIC;

        if( status == LinkStatus.LS_OK || status == LinkStatus.LS_ENTRY_UNAVAILABLE ) {
            int end = pos_current + 1;
            // if hidden link:
            // if( char_current == '>' )
            // {
            // m_list_links.push_back( new LinkEntry( create_mark( iter_tab ),
            // create_mark( iter_current ),
            // date_last ) );
            // apply_hidden_link_tags( iter_end,
            // status == LS_OK ? m_tag_link : m_tag_link_broken );
            // }
            // else
            {
                m_spans.add( new ClickableSpan() {
                    // Entry entry = ActivityOpenDiary.diary.get( date_last );

                    @Override
                    public void onClick( View widget ) {
                        // TODO...

                    }
                } );

                mEditText.getText().setSpan( m_spans.lastElement(), pos_start, end, 0 );
            }
        }
    }
}
