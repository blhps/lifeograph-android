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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

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

    static final String SUFFIX_LOCK = ".~LOCK~";
    static final String SUFFIX_UNSAVED = ".~unsaved~";

    static final String sExampleDiaryPath = "*/E/X/A/M/P/L/E/D/I/A/R/Y/*";
    static final String sExampleDiaryName = "*** Example Diary ***";

    static final int PASSPHRASE_MIN_SIZE = 4; // TODO get from C++?

    public enum SetPathType { NORMAL, READ_ONLY, NEW }

    public Diary() { super( nativeCreate() ); }
    private Diary(long nativePtr) { super( nativePtr ); } // only for internal jobs

    static public void
    initMain() {
        if( nativeGetMain() == 0 )
            nativeCreateMain();
    }

    boolean
    isMain() { return mNativePtr == nativeGetMain(); }

    static Diary
    getMain() { return new Diary( nativeGetMain() ); }


    @Override
    protected void finalize() throws Throwable {
        if (mNativePtr != 0 && mNativePtr != nativeGetMain()) {
            nativeDestroy(mNativePtr);
            mNativePtr = 0;
        }
        super.finalize();
    }

    // MAIN FUNCTIONALITY ==========================================================================
    String
    get_uri() { return nativeGetUri(mNativePtr); }

    String
    get_uri_unsaved() { return nativeGetUriUnsaved(mNativePtr); }

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
        return nativeGetUri( mNativePtr ).equals( sExampleDiaryPath );
    }

    Result
    init_new( Context ctx, String path ) {
        nativeInitNewPre(mNativePtr);

        Result res = setPath( ctx, path );
        if( res != Result.SUCCESS ) return res;

        res = Result.values()[nativeInitNew(mNativePtr, path, "")];
        if( res == Result.SUCCESS )
            return write( ctx );
        else
            return res;
    }

    void
    clear() {
        nativeClear(mNativePtr);
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
    getIcon() {
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
        if( ptr == 0 ) return null;
        DiaryElement elem = new DiaryElement( ptr );
        switch( elem.get_type() ) {
            case ENTRY:     return new Entry( ptr );
            case PARAGRAPH: return new Paragraph( ptr );
            case THEME:     return new Theme( ptr );
            case FILTER:    return new Filter( ptr );
            case CHART:     return new ChartElem( ptr );
            case TABLE:     return new TableElem( ptr );
            case DIARY:     return new Diary( ptr );
            default:        return elem;
        }
    }

    int
    create_new_id( DiaryElement element ) { return nativeCreateNewId(mNativePtr, element); }

    // OPTIONS =====================================================================================
    String
    get_lang() { return nativeGetLang(mNativePtr); }

    void
    set_lang( String lang ) { nativeSetLang(mNativePtr, lang); }

    // ENTRIES =====================================================================================
    Entry
    get_entry_1st() {
        long ptr = nativeGetEntry1st( mNativePtr );
        return ptr == 0 ? null : new Entry( ptr );
    }

    Entry
    get_entry_most_current() {
        long ptr = nativeGetEntryMostCurrent( mNativePtr );
        return ptr == 0 ? null : new Entry( ptr );
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
        return ptr == 0 ? null : new Entry( ptr );
    }

    Entry
    get_entry_by_date( long date ) {
        long ptr = nativeGetEntryByDate(mNativePtr, date);
        return ptr == 0 ? null : new Entry( ptr );
    }

    Entry
    get_entry_by_name( String name ) {
        long ptr = nativeGetEntryByName(mNativePtr, name);
        return ptr != 0 ? new Entry( ptr ) : null;
    }

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
        return ptr == 0 ? null : new Entry(ptr);
    }

    Entry
    duplicate_entry( Entry source ) {
        long ptr = nativeDuplicateEntry(mNativePtr, source.mNativePtr);
        return ptr == 0 ? null : new Entry(ptr);
    }

    // adds a new entry to today even if there is already one or more:
    Entry
    addToday() {
        long ptr = nativeCreateEntryDated( mNativePtr, 0, Date.get_today(), false );
        return new Entry( ptr );
    }

    Entry
    dismiss_entry( @NonNull Entry entry ) {
        long ptr = nativeDismissEntry(mNativePtr, entry);
        return ptr == 0 ? null : new Entry(ptr);
    }

    // TAGS ========================================================================================
    DiaryElemTag
    get_completion_tag() {
        long ptr = nativeGetCompletionTag(mNativePtr);
        if( ptr == 0 ) return null;
        DiaryElemTag tag = new DiaryElemTag( ptr );
        switch( tag.get_type() ) {
            case ENTRY:     return new Entry( ptr );
            case PARAGRAPH: return new Paragraph( ptr );
            default:        return tag;
        }
    }

//    void
//    set_completion_tag( long id ) {
//        m_completion_tag_id = id;
//    }

    public DiaryElemTag
    get_tag_by_id( int id ) {
        long ptr = nativeGetTagById(mNativePtr, id);
        if( ptr == 0 ) return null;
        DiaryElemTag tag = new DiaryElemTag( ptr );
        switch( tag.get_type() ) {
            case ENTRY:     return new Entry( ptr );
            case PARAGRAPH: return new Paragraph( ptr );
            default:        return tag;
        }
    }

    // SEARCHING ===================================================================================
    String
    get_search_str() { return nativeGetSearchStr(mNativePtr); }

    void
    set_search_str( @NonNull String text ) { nativeSetSearchStr(mNativePtr, text); }

    void
    start_search() { nativeStartSearch(mNativePtr); }
    void
    stop_search() { nativeStopSearch(mNativePtr); }

    boolean
    is_search_in_progress() { return nativeIsSearchInProgress(mNativePtr); }

    int
    get_match_count()  { return nativeGetMatchCount(mNativePtr); }

    Vector<HiddenFormat>
    get_matches() {
        long[] ptrs = nativeGetMatches( mNativePtr );
        Vector< HiddenFormat > matches = new Vector<>( ptrs.length );
        for( long ptr : ptrs ) matches.add( new HiddenFormat( ptr ) );
        return matches;
    }

    void
    replace_all_matches( String newtext ) {
        // TODO: 2.1: delegate to C++
    }

    // this version does not disturb the active search and is case-sensitive
    void
    replace_all_matches( String oldtext, String newtext ) {
        // TODO: 2.1: delegate to C++
    }

    void
    clear_matches() {
        // TODO: 2.1: delegate to C++
    }

    // FILTERS =====================================================================================
    Filter
    get_filter( String name ) {
        // TODO: delegate to C++
        return null;
    }

    Vector<Filter>
    get_filters() {
        long[] ptrs = nativeGetFilters(mNativePtr);
        Vector<Filter> filters = new Vector<>(ptrs.length);
        for (long ptr : ptrs) {
            if (ptr != 0) {
                filters.add(new Filter(ptr));
            }
        }
        return filters;
    }

    Filter // Non-null?
    get_filter_nontrashed() {
        long ptr = nativeGetFilterNonTrashed(mNativePtr);
        return ptr == 0 ? null : new Filter( ptr );
    }

    Filter // Non-null?
    get_filter_trashed() {
        long ptr = nativeGetFilterTrashed(mNativePtr);
        return ptr == 0 ? null : new Filter( ptr );
    }

    Filter
    get_filter_list() {
        long ptr = nativeGetFilterEntryList( mNativePtr );
        return ptr == 0 ? null : new Filter( ptr );
    }

    void
    set_filter_list( Filter filter ) { nativeSetFilterEntryList( mNativePtr, filter.mNativePtr); }

    int
    update_all_entries_filter_status() { return nativeUpdateAllEntriesFilterStatus(mNativePtr); }

    boolean
    rename_filter( Filter filter, String new_name ) {
        return nativeRenameFilter(mNativePtr, filter, new_name);
    }

    Filter
    create_filter( String name0 ) {
        long ptr = nativeCreateFilter(mNativePtr, name0);
        return ptr == 0 ? null : new Filter(ptr);
    }

    boolean
    dismiss_filter( String name ) {
        // TODO: 2.1: nativeDismissFilter(mNativePtr, name);
        return false;
    }

    Filter
    duplicate_filter( String name ) {
        //long ptr = nativeDuplicateFilter(mNativePtr, name);
        //return ptr == 0 ? null : new Filter(ptr);
        return null; // TODO: 2.1
    }

    // CHARTS ======================================================================================
    ChartElem
    create_chart( String name0, String definition ) {
        // TODO: 2.2: delegate to C++
        return null;
    }

    ChartElem
    get_chart( String name ) {
        // TODO: delegate to C++
        return null;
    }

    Vector<ChartElem>
    get_charts() {
        long[] ptrs = nativeGetCharts(mNativePtr);
        Vector<ChartElem> charts = new Vector<>(ptrs.length);
        for (long ptr : ptrs) {
            if (ptr != 0) {
                charts.add(new ChartElem(ptr));
            }
        }
        return charts;
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
        return ptr == 0 ? null : new Theme(ptr);
    }

    Theme
    get_theme( @NonNull String name ) {
        long ptr =  nativeGetTheme(mNativePtr, name);
        return ptr == 0 ? null : new Theme(ptr);
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
    setPath( Context ctx, String uriStr/*, SetPathType type*/ ) {
        String name = FileUtil.getFileName( Uri.parse(uriStr), ctx );
        removeLockIfNecessary(ctx);
        nativeSetUri(mNativePtr, uriStr);
        nativeSetName(mNativePtr, name);
        return isLocked(ctx) ? Result.FILE_LOCKED : Result.SUCCESS;
    }

    protected Result
    read_header(android.content.Context context) {
        android.net.Uri uri = android.net.Uri.parse(get_uri());
        try (java.io.InputStream inputStream =
                     context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return Result.FILE_NOT_FOUND;

            // Read all bytes from the stream
            byte[] bytes = getBytesFromInputStream(inputStream);

            // Call the native method
            int result = nativeReadHeader(mNativePtr, bytes, uri.toString());
            return Result.values()[result];
        } catch (Exception e) {
            return Result.FAILURE;
        }
    }
    // Helper method to convert InputStream to byte[]
    private byte[] getBytesFromInputStream(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    protected Result
    read_body() {
        int result = nativeReadBody(mNativePtr);
        return Result.values()[result];
    }

    Result
    enableEditing( android.content.Context ctx ) {
//        if( nativeIsReadOnly(mNativePtr) ) {
//            Log.e( Lifeograph.TAG, "Diary: editing cannot be enabled. Diary is read-only" );
//            return Result.FILE_LOCKED;
//        }

        Result result = writeLock( ctx );

        if( result == Result.SUCCESS ) {
            nativeSetLoggedInEdit(mNativePtr);
            return Result.SUCCESS;
        }
        else return result;
    }

    // WRITING =====================================================================================
    // we reimplement in Android as it is not possible to deal with content:// uris in C++
    Result
    write(android.content.Context context) {
        Uri uri = Uri.parse( nativeGetUri( mNativePtr ) );
        ContentResolver resolver = context.getContentResolver();

        try {
            InputStream istream = resolver.openInputStream( uri );
            if (istream != null) {
                // TODO: 2.1: BACKUP FOR THE LAST VERSION BEFORE UPGRADE
//                if( m_read_version != DB_FILE_VERSION_INT ) {
//                    DocumentFile backupOld = getNeighborFileDocument( context,
//                                                                      uri,
//                                                                      "." +
//                                                                      nativeGetReadVersion()
//                }

                // BACKUP THE PREVIOUS VERSION
                DocumentFile backupDoc = getNeighborFileDocument( context,
                                                                  uri,
                                                                  ".previousversion~" );
                if (backupDoc != null) {
                    try ( OutputStream ostream = resolver.openOutputStream( backupDoc.getUri())) {
                        Lifeograph.copyFile(istream, ostream); // Overload copyFile for Streams
                    }
                }
                else { // fallback to app's folder
                    File backupFile = new File( context.getFilesDir(), get_name() + ".~previousversion~" );
                    Lifeograph.copyFile( istream, backupFile );
                }
                istream.close();
            }
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Could not save backup file: " + ex.getMessage() );
            return Result.FILE_NOT_WRITABLE;
        }

        // WRITE THE FILE
        Result result = writeStreamed( context, uri);

        // DAILY BACKUP SAVES
        if( result == Result.SUCCESS && Lifeograph.sSaveDailyBackups ) {
            try {
                InputStream istream = resolver.openInputStream( uri );
                if(istream != null ) {
                    String name = get_name();
                    String baseName =
                            name.endsWith( ".diary" ) ? name.substring( 0, name.length() - 6 ) :
                            name;
                    File backupFile = new File( context.getFilesDir(),
                                                baseName + "_(" + get_id() + ")_" +
                                                Date.format_string( Date.get_today(), "YMD", '-' ) +
                                                ".diary" );
                    Lifeograph.copyFile( istream, backupFile );
                    istream.close();
                }
            }
            catch( IOException ex ) {
                Log.e( Lifeograph.TAG, "Could not save daily backup file: " + ex.getMessage() );
            }
        }

        return result;
    }

    Result
    writeStreamed( android.content.Context context, Uri uri ) {
        if( nativeWriteUri(mNativePtr, uri.toString()) == Result.SUCCESS.ordinal() )
            try {
                ContentResolver resolver = context.getContentResolver();

                try (OutputStream ostream = resolver.openOutputStream(uri, "wt")) {
                    if (ostream == null) return Result.FAILURE;

                    // get the raw bytes (encrypted or plain) from C++
                    byte[] data = nativeGetStrStream(mNativePtr);
                    if (data != null) {
                        ostream.write(data);
                        ostream.flush();
                        return Result.SUCCESS;
                    }
                }
            }
            catch( IOException ex ) {
                Log.e( Lifeograph.TAG, "Failed to save diary: " + ex.getMessage() );
            }
        return Result.FAILURE;
    }

    Result
    write_txt( @NonNull String path, Filter filter ) {
        return Result.values()[nativeWriteTxt(mNativePtr, path,
                                              filter == null ? 0 : filter.mNativePtr)];
    }

    DocumentFile
    getNeighborFileDocument(android.content.Context context, Uri uri, String suffix) {
        try {
            DocumentFile sourceFile = DocumentFile.fromSingleUri( context, uri);
            DocumentFile parentDir = sourceFile.getParentFile();

            if (parentDir != null && parentDir.isDirectory()) {
                String backupName = sourceFile.getName() + suffix;

                // look for existing backup or create new one
                DocumentFile neighborDoc = parentDir.findFile(backupName);
                if (neighborDoc == null) {
                    neighborDoc = parentDir.createFile("application/octet-stream", backupName);
                }

                return neighborDoc;
            }
        }
        catch( Exception ex ) {
            Log.e( Lifeograph.TAG, "Error getting neighbor file: " + ex.getMessage() );
        }
        return null;
    }

    Result
    writeLock(android.content.Context context) {
        Uri uri = Uri.parse( nativeGetUri( mNativePtr ) );
        ContentResolver resolver = context.getContentResolver();

        try {
            InputStream istream = resolver.openInputStream( uri );
            if (istream != null) {
                DocumentFile lockDoc = getNeighborFileDocument( context, uri, SUFFIX_LOCK );
                if (lockDoc != null) {
                    writeStreamed( context, lockDoc.getUri() );
                }
                else { // fallback to app's folder
                    File lockFile = new File( context.getFilesDir(), get_name() + SUFFIX_LOCK );
                    writeStreamed( context, Uri.fromFile( lockFile ) );
                }
                istream.close();
            }
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Could not save backup file: " + ex.getMessage() );
            return Result.FILE_NOT_WRITABLE;
        }
        return Result.SUCCESS;
    }

    boolean
    removeLockIfNecessary( Context ctx ) {
        if( !is_in_edit_mode() )
            return false;

        Uri uri = Uri.parse( nativeGetUri( mNativePtr ) );
        ContentResolver resolver = ctx.getContentResolver();

        try {
            DocumentFile lockDoc = getNeighborFileDocument( ctx, uri, SUFFIX_LOCK );
            if (lockDoc != null) {
                return lockDoc.delete();
            }
            else { // fallback to app's folder
                File lockFile = new File( ctx.getFilesDir(), get_name() + SUFFIX_LOCK );
                return lockFile.delete();
            }
        }
        catch( Exception ex ) {
            Log.e( Lifeograph.TAG, "Could not save backup file: " + ex.getMessage() );
        }
        return false;
    }

    void
    set_continue_from_lock() { nativeSetContinueFromLock(mNativePtr); }

    boolean
    isLocked(android.content.Context context) {
        String uriString = nativeGetUri(mNativePtr);
        if (uriString == null || uriString.isEmpty()) return false;

        Uri uri = Uri.parse(uriString);

        // 1. Check for neighboring lock file (for SAF/Content URIs)
        try {
            DocumentFile sourceFile = DocumentFile.fromSingleUri(context, uri);
            DocumentFile parentDir = sourceFile.getParentFile();

            if (parentDir != null && parentDir.isDirectory()) {
                String lockName = sourceFile.getName() + SUFFIX_LOCK;
                DocumentFile lockDoc = parentDir.findFile(lockName);
                if (lockDoc != null && lockDoc.exists()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Logged silently as it might fail for simple file paths
            Log.d(Lifeograph.TAG, "Could not check neighbor lock: " + e.getMessage());
        }

        // 2. Check for fallback lock file in app's internal filesDir
        File internalLockFile = new File(context.getFilesDir(), get_name() + SUFFIX_LOCK);
        return internalLockFile.exists();
    }

    Result
    writeUnsaved(android.content.Context context) {
        Uri uri = Uri.parse( nativeGetUri( mNativePtr ) );
        ContentResolver resolver = context.getContentResolver();

        try {
            InputStream istream = resolver.openInputStream( uri );
            if (istream != null) {
                DocumentFile locUnsvd = getNeighborFileDocument( context, uri, SUFFIX_UNSAVED);
                if (locUnsvd != null) {
                    writeStreamed( context, locUnsvd.getUri() );
                }
                else { // fallback to app's folder
                    File fileUnsvd = new File( context.getFilesDir(), get_name() + SUFFIX_UNSAVED );
                    writeStreamed( context, Uri.fromFile( fileUnsvd ) );
                }
                istream.close();
            }
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Could not backup unsaved changes: " + ex.getMessage() );
            return Result.FILE_NOT_WRITABLE;
        }
        return Result.SUCCESS;
    }

    // C++ BASED METHODS WITH A DIFFERENT USAGE ====================================================
//    static native void
//    set_date_format_order( String format ) { nativeSetDateFormatSeparator(format); }
//    static native void
//    set_date_format_separator( String format ) { nativeSetDateFormatSeparator(format); }

    // NATIVE METHODS ==============================================================================
    private static native boolean initCipher();
    private static native long nativeCreate();
    static native void nativeCreateMain();
    static native long nativeGetMain();

    private native void nativeDestroy(long ptr);
    private native void nativeInitNewPre(long ptr);
    private native int nativeInitNew(long ptr, String path, String pw);
    private native void nativeClear(long ptr);
    private native int nativeReadHeader(long ptr, byte[] data, String uri);
    private native int nativeReadBody(long ptr);
    private native void nativeSetName(long ptr, String name);
    private native String nativeGetPassphrase(long ptr);
    private native boolean nativeSetPassphrase(long ptr, String pw);
    //private native int nativeGetReadVersion(long ptr);
    private native String nativeGetUri(long ptr);
    private native void nativeSetUri(long ptr, String uri);
    private native String nativeGetUriUnsaved(long ptr);
    private native int nativeGetSize(long ptr);
    private native boolean nativeIsOld(long ptr);
    private native boolean nativeIsEncrypted(long ptr);
    private native boolean nativeIsOpen(long ptr);
    private native boolean nativeIsInEditMode(long ptr);
    private native boolean nativeCanEnterEditMode(long ptr);
    private native void nativeSetLoggedInEdit(long ptr);
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
    private native long nativeGetEntryByName(long ptr, String name);
    private native long[] nativeGetEntriesByFilter(long ptr, long ptr_filter);
    private native int nativeGetEntryCountOnDay(long ptr, long date);
    private native void nativeSetEntryName(long ptr, Entry entry, String name);
    private native void nativeSetEntryDate(long ptr, Entry entry, long date);
    private native long nativeCreateEntry(long ptr, Entry entry_rel, boolean fParent, long date,
                                          String content);
    private native long nativeCreateEntryDated(long ptr, long ptr_entry_rel, long date,
                                               boolean fMileStone);
    private native long nativeDuplicateEntry(long ptr, long ptr_entry);
    private native long nativeDismissEntry(long ptr, Entry entry);
    private native long nativeGetCompletionTag(long mNativePtr);
    private native long nativeGetTagById(long mNativePtr, int id);

    private native long[] nativeGetFilters(long mNativePtr);
    private native long nativeGetFilterNonTrashed( long mNativePtr);
    private native long nativeGetFilterTrashed( long mNativePtr);
    private native long nativeGetFilterEntryList( long mNativePtr);
    private native void nativeSetFilterEntryList( long mNativePtr, long ptr_filter);
    private native int nativeUpdateAllEntriesFilterStatus(long mNativePtr);
    private native boolean nativeRenameFilter(long mNativePtr, Filter filter, String name);
    private native long nativeCreateFilter( long mNativePtr, String name0);

    private native long[] nativeGetCharts(long mNativePtr);

    private native long nativeCreateTheme(long mNativePtr, String name0);
    private native long nativeGetTheme(long mNativePtr, String name);
    private native long[] nativeGetThemes(long mNativePtr);


    private native int nativeWriteUri(long mNativePtr, String uri);
    private native byte[] nativeGetStrStream(long mNativePtr);
    private native int nativeWriteTxt(long mNativePtr, String path, long ptr_filter);
    private native void nativeSetContinueFromLock(long ptr);

    private native String nativeGetSearchStr(long mNativePtr);
    private native void nativeSetSearchStr(long mNativePtr, String str);
    private native void nativeStartSearch(long mNativePtr);
    private native void nativeStopSearch(long mNativePtr);
    private native void nativeDestroySearchThreads(long mNativePtr);
    private native boolean nativeIsSearchInProgress(long mNativePtr);
    private native int nativeGetMatchCount(long mNativePtr);
    private native long[] nativeGetMatches(long mNativePtr);

    // HELPER FUNCTIONS ============================================================================

}
