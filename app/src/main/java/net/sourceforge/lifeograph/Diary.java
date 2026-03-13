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

import java.util.Vector;
import android.content.Context;
import androidx.annotation.NonNull;
import net.sourceforge.lifeograph.helpers.*;


public class Diary extends DiaryElement
{
    static {
        System.loadLibrary( "gpg-error" );
        System.loadLibrary( "gcrypt" );
        System.loadLibrary( "lifeograph_core" );

        initCipher();
    }

//    static final int SoCr_DATE          = 0x1;
//    static final int SoCr_SIZE_C        = 0x2;  // size (char count)
//    static final int SoCr_CHANGE        = 0x3;  // last change date
//    static final int SoCr_NAME          = 0x4;  // name
//    static final int SoCr_FILTER_CRTR   = 0xF;
//    static final int SoCr_DESCENDING    = 0x10;
//    static final int SoCr_ASCENDING     = 0x20;
//    static final int SoCr_FILTER_DIR    = 0xF0;
//    static final int SoCr_INVERSE       = 0x100; // (<2000) inverse dir for ordinals
//    static final int SoCr_DESCENDING_T  = 0x100; // temporal
//    static final int SoCr_ASCENDING_T   = 0x200; // temporal
//    static final int SoCr_FILTER_DIR_T  = 0xF00; // temporal
//    static final int SoCr_DEFAULT       = SoCr_DATE|SoCr_ASCENDING|SoCr_DESCENDING_T;

//    static final String DB_FILE_HEADER = "LIFEOGRAPHDB";
//    static final int    DB_FILE_VERSION_INT = 3010;
//    static final int    DB_FILE_VERSION_INT_MIN = 1020;
    static final String LOCK_SUFFIX = ".~LOCK~"; // TODO get from C++?

    static final String sExampleDiaryPath = "*/E/X/A/M/P/L/E/D/I/A/R/Y/*";
    static final String sExampleDiaryName = "*** Example Diary ***";

    static final int PASSPHRASE_MIN_SIZE = 4; // TODO get from C++?

    public enum SetPathType { NORMAL, READ_ONLY, NEW }

    public Diary() {
        super( d.nativeCreate() );
    }

    @Override
    protected void finalize() throws Throwable {
        if (mNativePtr != 0) {
            nativeDestroy(mNativePtr);
            mNativePtr = 0;
        }
        super.finalize();
    }

    // MAIN FUNCTIONALITY ==========================================================================
    String
    get_uri() { return nativeGetUri(mNativePtr); }

    boolean
    is_old() {
        return nativeIsOld(mNativePtr);
    }

    boolean
    is_encrypted() {
        return nativeIsEncrypted(mNativePtr);
    }

    boolean
    is_open() {
        return nativeIsOpen(mNativePtr);
    }

    boolean
    is_in_edit_mode() {
        return nativeIsInEditMode(mNativePtr);
    }

    boolean
    can_enter_edit_mode() {
        return nativeCanEnterEditMode(mNativePtr);
    }

    // not part of c++
    boolean
    is_virtual() {
        return m_path.equals( sExampleDiaryPath );
    }

    Result
    init_new( Context ctx, String path ) {
        int res = nativeInitNew(mNativePtr, path, "");
        return Result.values()[res];
    }

    void
    clear() {
        nativeClear(mNativePtr);
        m_path = "";
    }

    // DIARYELEMENT INHERITED FUNCTIONS ============================================================
    @Override public DiaryElement.Type
    get_type() {
        return DiaryElement.Type.DIARY;
    }

    @Override public int
    get_size() {
        return nativeGetSize(mNativePtr);
    }

    @Override public int
    get_icon() {
        return R.drawable.ic_diary;
    }

    public String
    get_info_str() {
        return( get_size() + " entries" );
    }

    // PASSPHRASE ==================================================================================
    @SuppressWarnings( "UnusedReturnValue" )
    boolean
    set_passphrase( String passphrase ) {
        return nativeSetPassphrase(mNativePtr, passphrase);
    }

    boolean
    compare_passphrase( String passphrase ) {
        return nativeGetPassphrase(mNativePtr).equals( passphrase );
    }

    boolean
    is_passphrase_Set() {
        return( !nativeGetPassphrase(mNativePtr).isEmpty() );
    }

    // ID HANDLING =================================================================================
    DiaryElement
    get_element( int id ) {
        long ptr = nativeGetElement(mNativePtr, id);
        return new DiaryElement(ptr);
    }

    int
    create_new_id( DiaryElement element ) {
        return nativeCreateNewId(mNativePtr, element);
    }

//    void
//    erase_id( long id ) {
//        m_ids.remove( id );
//    }

//    @SuppressWarnings( "UnusedReturnValue" )
//    boolean
//    set_force_id( long id ) {
//        if( m_ids.get( id ) != null || id <= DiaryElement.DEID_MIN )
//            return false;
//        m_force_id = id;
//        return true;
//    }

    // OPTIONS =====================================================================================
    String
    get_lang() {
        return nativeGetLang(mNativePtr);
    }

    void
    set_lang( String lang ) {
        nativeSetLang(mNativePtr, lang);
    }

    // ENTRIES =====================================================================================
    Entry
    get_entry_1st() { return new Entry( nativeGetEntry1st( mNativePtr ) ); }

    Entry
    get_entry_most_current() {
        long ptr = nativeGetEntryMostCurrent( mNativePtr );
        return new Entry( ptr );
    }

    Entry
    get_entry_by_id( int id ) {
        long ptr = nativeGetEntryById(mNativePtr, id);
        return ptr == 0 ? null : new Entry( ptr );
    }
    Paragraph
    get_paragraph_by_id( int id ) {
        long ptr = nativeGetParagraphById(mNativePtr, id);
        return ptr == 0 ? null : new Paragraph( ptr );
    }

    Entry
    get_entry_today() {
        long ptr = nativeGetEntryToday(mNativePtr);
        return new Entry( ptr );
    }

    Entry
    get_entry_by_date( long date ) {
        long ptr = nativeGetEntryByDate(mNativePtr, date);
        return new Entry( ptr );
    }

//    Entry
//    get_entry_by_name( String name ) {
//        // TODO: delegate to C++
//        return null;
//    }

//    List< Entry >
//    get_entries_by_name( String name ) {
//        // TODO: delegate to C++
//        return null;
//    }

    Vector< Entry >
    get_entries_by_filter( @NonNull Filter filter ) {
        long[] ptrs = nativeGetEntriesByFilter(mNativePtr, filter.mNativePtr);
        Vector<Entry> entries = new Vector<>(ptrs.length);
        for (long ptr : ptrs) {
            if (ptr != 0) {
                entries.add(new Entry(ptr));
            }
        }
        return entries;
    }

    int
    get_entry_count_on_day( long date ) {
        return nativeGetEntryCountOnDay(mNativePtr, date);
    }

//    Entry
//    get_entry_next_in_day( Date date ) {
//        // TODO: delegate to C++
//        return null;
//    }

//    Entry
//    get_entry_first_untrashed() {
//        // TODO: delegate to C++
//        return null;
//    }

//    Entry
//    get_entry_latest() {
//        // TODO: delegate to C++
//        return null;
//    }

    void
    set_entry_name( @NonNull Entry entry, String name ) {
        nativeSetEntryName(mNativePtr, entry, name);
    }

    void
    set_entry_date( @NonNull Entry entry, long date ) {
        nativeSetEntryDate(mNativePtr, entry, date);
    }

    Entry
    create_entry( long date, String content ) {
        long ptr =  nativeCreateEntry(mNativePtr, null, false, date, content);
        return new Entry(ptr);
    }

    // adds a new entry to today even if there is already one or more:
    Entry
    add_today() {
        // TODO: delegate to C++
        return null;
    }

    Entry
    dismiss_entry( @NonNull Entry entry ) {
        long ptr = nativeDismissEntry(mNativePtr, entry);
        return new Entry(ptr);
    }

    // TAGS ========================================================================================
    DiaryElemTag
    get_completion_tag() {
        long ptr = nativeGetCompletionTag(mNativePtr);
        return new DiaryElemTag(ptr);
    }

//    void
//    set_completion_tag( long id ) {
//        m_completion_tag_id = id;
//    }

    public DiaryElemTag
    get_tag_by_id( int id ) {
        long ptr = nativeGetTagById(mNativePtr, id);
        return new DiaryElemTag(ptr);
    }

    // SEARCHING ===================================================================================
    String
    get_search_str() {
        // return nativeGetSearchText(mNativePtr);
        return ""; // TODO does not exist in C++ right now...
    }

    void
    set_search_str( @NonNull String text ) { nativeSetSearchStr(mNativePtr, text); }

    boolean
    is_search_in_progress() { return nativeIsSearchInProgress(mNativePtr); }

    Vector<HiddenFormat>
    get_matches() {
        long[] ptrs = nativeGetMatches( mNativePtr );
        Vector< HiddenFormat > matches = new Vector<>( ptrs.length );
        for( long ptr : ptrs ) matches.add( new HiddenFormat( ptr ) );
        return matches;
    }

    void
    replace_all_matches( String newtext ) {
        // TODO: delegate to C++
    }

    // this version does not disturb the active search and is case-sensitive
    void
    replace_all_matches( String oldtext, String newtext ) {
        // TODO: delegate to C++
    }

    void
    clear_matches() {
        // TODO: delegate to C++
    }

    // FILTERS =====================================================================================
    Filter
    create_filter( String name0, String definition ) {
        // TODO: delegate to C++
        return null;
    }

    Filter
    get_filter( String name ) {
        // TODO: delegate to C++
        return null;
    }

//    boolean
//    set_filter_active( String name ) {
//        // TODO: delegate to C++
//        return false;
//    }

//    String
//    get_filter_active_name() {
//        // TODO: delegate to C++
//        return "";
//    }

    boolean
    rename_filter( Filter filter, String new_name ) {
        return nativeRenameFilter(mNativePtr, filter, new_name);
    }

    boolean
    dismiss_filter( String name ) {
        // TODO: delegate to C++
        return false;
    }

//    boolean
//    dismiss_filter_active() {
//        // TODO: delegate to C++
//        return false;
//    }

    void // Android only
    updateAllEntriesFilterStatus() {
        // TODO: delegate to C++
    }

    // CHARTS ======================================================================================
    ChartElem
    create_chart( String name0, String definition ) {
        // TODO: delegate to C++
        return null;
    }

    ChartElem
    get_chart( String name ) {
        // TODO: delegate to C++
        return null;
    }

    boolean
    set_chart_active( String name ) {
        // TODO: delegate to C++
        return false;
    }

    String
    get_chart_active_name() {
        // TODO: delegate to C++
        return "";
    }

    boolean
    rename_chart( String old_name, String new_name ) {
        // TODO: delegate to C++
        return false;
    }

    boolean
    dismiss_chart( String name ) {
        // TODO: delegate to C++
        return false;
    }

    void
    fill_up_chart_data( ChartData cd ) {
        // TODO: delegate to C++
    }

    // THEMES ======================================================================================
    Theme
    create_theme( String name0 ) {
        long ptr =  nativeCreateTheme(mNativePtr, name0);
        return new Theme(ptr);
    }

    Theme
    get_theme( @NonNull String name ) {
        long ptr =  nativeGetTheme(mNativePtr, name);
        return new Theme(ptr);
    }

    Vector<Theme>
    get_themes() {
        long[] ptrs = nativeGetThemes(mNativePtr);
        Vector<Theme> themes = new Vector<>(ptrs.length);
        for (long ptr : ptrs) {
            if (ptr != 0) {
                themes.add(new Theme(ptr));
            }
        }
        return themes;
    }

    void
    dismiss_theme( @NonNull Theme theme ) {
        // TODO....
    }

    void
    rename_theme( @NonNull Theme theme, String new_name ) {
        // TODO ...
    }

    // READING =====================================================================================
    Result
    set_path(String uristr, SetPathType type ) {
        int result = nativeSetPath(mNativePtr, uristr, type.ordinal());
        return Result.values()[result];
    }

    protected Result
    read_header() {
        int result = nativeReadHeader(mNativePtr);
        return Result.values()[result];
    }

    protected Result
    read_body() {
        int result = nativeReadBody(mNativePtr);
        return Result.values()[result];
    }

    Result
    enable_editing() {
        int result = nativeEnableEditing(mNativePtr);
        return Result.values()[result];
    }

    // WRITING =====================================================================================
    Result
    write() {
        return nativeWrite(mNativePtr);
    }

    Result
    write_lock() {
        return nativeWriteLock(mNativePtr);
    }

    Result
    write_txt( @NonNull String path, Filter filter ) {
        return nativeWriteTxt(mNativePtr, path, filter.mNativePtr);
    }

    boolean
    remove_lock_if_necessary() { return nativeRemoveLockIfNecessary(mNativePtr); }

    void
    set_continue_from_lock() { nativeSetContinueFromLock(mNativePtr); }

    // NATIVE METHODS ==============================================================================
    private static native boolean initCipher();
    private native long nativeCreate();
    private native void nativeDestroy(long ptr);
    private native int nativeInitNew(long ptr, String path, String pw);
    private native void nativeClear(long ptr);
    private native int nativeSetPath(long ptr, String path, int type);
    private native int nativeReadHeader(long ptr);
    private native int nativeReadBody(long ptr);
    private native int nativeEnableEditing(long ptr);
    private native String nativeGetPassphrase(long ptr);
    private native boolean nativeSetPassphrase(long ptr, String pw);
    //private native int nativeGetReadVersion(long ptr);
    private native String nativeGetUri(long ptr);
    private native int nativeGetSize(long ptr);
    private native boolean nativeIsOld(long ptr);
    private native boolean nativeIsEncrypted(long ptr);
    private native boolean nativeIsOpen(long ptr);
    private native boolean nativeIsInEditMode(long ptr);
    private native boolean nativeCanEnterEditMode(long ptr);
    private native String nativeGetLang(long ptr);
    private native void nativeSetLang(long ptr, String lang);

    private native int nativeCreateNewId( long mNativePtr, DiaryElement element );
    private native long nativeGetElement(long ptr, int id);
    private native long nativeGetEntry1st(long ptr);
    private native long nativeGetEntryMostCurrent(long ptr);
    private native long nativeGetEntryById(long ptr, int id);
    private native long nativeGetParagraphById(long ptr, int id);
    private native long nativeGetEntryToday(long ptr);
    private native long nativeGetEntryByDate(long ptr, long date);
    private native long[] nativeGetEntriesByFilter(long ptr, long ptr_filter);
    private native int nativeGetEntryCountOnDay(long ptr, long date);
    private native void nativeSetEntryName(long ptr, Entry entry, String name);
    private native void nativeSetEntryDate(long ptr, Entry entry, long date);
    private native long nativeCreateEntry(long ptr, Entry entry_rel, boolean fParent, long date,
                                          String content);
    private native long nativeDismissEntry(long ptr, Entry entry);
    private native long nativeGetCompletionTag(long mNativePtr);
    private native long nativeGetTagById(long mNativePtr, int id);

    private native boolean nativeRenameFilter(long mNativePtr, Filter filter, String name);

    private native long nativeCreateTheme(long mNativePtr, String name0);
    private native long nativeGetTheme(long mNativePtr, String name);
    private native long[] nativeGetThemes(long mNativePtr);


    private native Result nativeWrite(long mNativePtr);
    private native Result nativeWriteLock(long mNativePtr);
    private native Result nativeWriteTxt(long mNativePtr, String path, long ptr_filter);
    private native boolean nativeRemoveLockIfNecessary(long mNativePtr);
    private native void nativeSetContinueFromLock(long mNativePtr);

    private native void nativeSetSearchStr(long mNativePtr, String str);
    private native boolean nativeIsSearchInProgress(long mNativePtr);
    private native long[] nativeGetMatches(long mNativePtr);

    // HELPER FUNCTIONS ============================================================================

    // VARIABLES ===================================================================================
    static Diary d = null;

    private String m_path = "";
    //private String mDiaryPathBackup = "";
    //private String mLockFilePath;

    //ids (DEID)
    //private final TreeMap< Long, DiaryElement > m_ids = new TreeMap<>();
    //private long m_force_id          = DEID_UNSET;
    //private long m_startup_entry_id  = HOME_CURRENT_ENTRY;
    //private long m_last_entry_id     = DEID_UNSET;
    //private long m_completion_tag_id = DEID_UNSET;

    // CONTENT
    //TreeMap< Long, Entry >    m_entries = new TreeMap<>( DiaryElement.compare_dates );
//    Map< String, List< Entry > >
//                              m_entry_names = new TreeMap<>();
//
//    TreeMap< String, Theme >  m_themes = new TreeMap<>();
//    Theme                     m_theme_default = null;
//
//    TreeMap< String, Chapter.Category >
//                              m_chapter_categories = new TreeMap<>( DiaryElement.compare_names );
//    Chapter.Category          m_p2chapter_ctg_cur = null;
//
//    TreeMap< String, Filter > m_filters = new TreeMap<>();
//    Filter                    m_filter_active = null;
//
//    TreeMap< String, ChartElem >
//                              m_charts = new TreeMap<>();
//    ChartElem                 m_chart_active = null;
//
//    TreeMap< String, TableElem >
//                              m_tables = new TreeMap<>();
//    TableElem                 m_table_active = null;
//
//    // OPTIONS
//    protected String  m_language;
//
//    protected int     m_read_version;

    // options & flags
    //protected boolean m_opt_show_all_entry_locations = false;
    //protected int     m_opt_ext_panel_cur = 1;
    //protected boolean m_flag_read_only = false;
    //protected boolean m_flag_ignore_locks = false;
    //protected boolean m_flag_skip_old_check = false;
    //protected boolean m_flag_save_enabled = true;

//    public enum LoginStatus{ LOGGED_OUT, LOGGED_TIME_OUT, LOGGED_IN_RO, LOGGED_IN_EDIT }
//    protected LoginStatus m_login_status = LoginStatus.LOGGED_OUT;

    // i/o
//    protected ContentResolver   mResolver;
//    protected byte[]            mBytes;
//    protected BufferedReader    mBufferedReader  = null;
//    protected BufferedWriter    mBufferedWriter  = null;
//    protected int               mHeaderLineCount = 0;

//    protected static final int cIV_SIZE   = 16; // = 128 bits
//    protected static final int cSALT_SIZE = 16; // = 128 bits
}
