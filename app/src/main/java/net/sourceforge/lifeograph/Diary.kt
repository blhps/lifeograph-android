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
import android.net.Uri
import android.util.Log
import net.sourceforge.lifeograph.Lifeograph.Companion.copyFile
import net.sourceforge.lifeograph.helpers.Date.Companion.format_string
import net.sourceforge.lifeograph.helpers.Date.Companion.get_today
import net.sourceforge.lifeograph.helpers.FileUtil
import net.sourceforge.lifeograph.helpers.Result
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Vector
import androidx.core.net.toUri
import net.sourceforge.lifeograph.helpers.DiaryDirectoryUtil

class Diary : DiaryElement {
//    enum class SetPathType {
//        NORMAL, READ_ONLY, NEW
//    }

    constructor() : super(nativeCreate())
    private constructor(nativePtr: Long) : super(nativePtr) // only for internal jobs

    val isMain: Boolean
        get() = mNativePtr == nativeGetMain()

    @Throws(Throwable::class)
    fun finalize() {
        if(mNativePtr != 0L && mNativePtr != nativeGetMain()) {
            nativeDestroy(mNativePtr)
            mNativePtr = 0
        }
    }

    // MAIN FUNCTIONALITY ==========================================================================
    fun get_uri(): String {
        return nativeGetUri(mNativePtr)
    }

    fun get_uri_unsaved(): String? {
        return nativeGetUriUnsaved(mNativePtr)
    }

    fun is_old(): Boolean {
        return nativeIsOld(mNativePtr)
    }

    fun is_encrypted(): Boolean {
        return nativeIsEncrypted(mNativePtr)
    }

    fun is_open(): Boolean {
        return nativeIsOpen(mNativePtr)
    }

    fun is_in_edit_mode(): Boolean {
        return nativeIsInEditMode(mNativePtr)
    }

    fun can_enter_edit_mode(): Boolean {
        return nativeCanEnterEditMode(mNativePtr)
    }

    // not part of c++
    fun is_virtual(): Boolean {
        return nativeGetUri(mNativePtr) == EXAMPLE_DIARY_PATH
    }

    fun init_new(ctx: Context, path: String): Result? {
        nativeInitNewPre(mNativePtr)

        var res = setPath(ctx, path)
        if(res != Result.SUCCESS) return res

        res = Result.entries[nativeInitNew(mNativePtr, path, "")]
        return if(res == Result.SUCCESS) write(ctx) else res
    }

    fun clear() {
        nativeClear(mNativePtr)
    }

    // DIARYELEMENT INHERITED FUNCTIONS ============================================================
    override fun get_type(): Type {
        return Type.DIARY
    }

    override fun get_size(): Int {
        return nativeGetSize(mNativePtr)
    }

    override fun getIcon(): Int {
        return R.drawable.ic_diary
    }

    override fun get_info_str(): String {
        return ("$_size entries")
    }

    // PASSPHRASE ==================================================================================
    fun set_passphrase(passphrase: String?): Boolean {
        return nativeSetPassphrase(mNativePtr, passphrase)
    }

    fun compare_passphrase(passphrase: String?): Boolean {
        return nativeGetPassphrase(mNativePtr) == passphrase
    }

    fun is_passphrase_Set(): Boolean {
        return (!nativeGetPassphrase(mNativePtr)!!.isEmpty())
    }

    // ID HANDLING =================================================================================
    fun get_element(id: Int): DiaryElement? {
        val ptr = nativeGetElement(mNativePtr, id)
        if(ptr == 0L) return null
        val elem = DiaryElement(ptr)
        return when(elem._type) {
            Type.ENTRY -> Entry(ptr)
            Type.PARAGRAPH -> Paragraph(ptr)
            Type.THEME -> Theme(ptr)
            Type.FILTER -> Filter(ptr)
            Type.CHART -> ChartElem(ptr)
            Type.TABLE -> TableElem(ptr)
            Type.DIARY -> Diary(ptr)
            else -> elem
        }
    }

    fun create_new_id(element: DiaryElement?): Int {
        return nativeCreateNewId(mNativePtr, element)
    }

    // OPTIONS =====================================================================================
    fun get_lang(): String? {
        return nativeGetLang(mNativePtr)
    }

    fun set_lang(lang: String?) {
        nativeSetLang(mNativePtr, lang)
    }

    fun is_date_holiday(date: Long): Boolean { return nativeIsDayHoliday(mNativePtr, date) }
    fun is_date_workday(date: Long): Boolean { return nativeIsDayWorkday(mNativePtr, date) }

    // ENTRIES =====================================================================================
    fun get_entry_1st(): Entry? {
        val ptr = nativeGetEntry1st(mNativePtr)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun get_entry_most_current(): Entry? {
        val ptr = nativeGetEntryMostCurrent(mNativePtr)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun get_entry_by_id(id: Int): Entry? {
        val ptr = nativeGetEntryById(mNativePtr, id)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun get_paragraph_by_id(id: Int): Paragraph? {
        val ptr = nativeGetParagraphById(mNativePtr, id)
        return if(ptr == 0L) null else Paragraph(ptr)
    }

    fun get_entry_today(): Entry? {
        val ptr = nativeGetEntryToday(mNativePtr)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun get_entry_by_date(date: Long): Entry? {
        val ptr = nativeGetEntryByDate(mNativePtr, date)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun get_entry_by_name(name: String?): Entry? {
        val ptr = nativeGetEntryByName(mNativePtr, name)
        return if(ptr != 0L) Entry(ptr) else null
    }

    //    List< Entry >
    //    get_entries_by_name( String name ) {
    //        // TODO: delegate to C++
    //        return null;
    //    }
    fun get_entries_by_filter(filter: Filter): Vector<Entry?> {
        val ptrs = nativeGetEntriesByFilter(mNativePtr, filter.mNativePtr)
        val entries = Vector<Entry?>(ptrs.size)
        for(ptr in ptrs) {
            if(ptr != 0L) {
                entries.add(Entry(ptr))
            }
        }
        return entries
    }

    fun get_entry_count_on_day(date: Long): Int {
        return nativeGetEntryCountOnDay(mNativePtr, date)
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
    fun set_entry_name(entry: Entry, name: String?) {
        nativeSetEntryName(mNativePtr, entry, name)
    }

    fun set_entry_date(entry: Entry, date: Long) {
        nativeSetEntryDate(mNativePtr, entry, date)
    }

    fun create_entry(date: Long, content: String?): Entry? {
        val ptr = nativeCreateEntry(mNativePtr, null, true, date, content)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun create_entry(entryRel: Entry?, fParent: Boolean, date: Long, content: String?): Entry? {
        val ptr = nativeCreateEntry(mNativePtr, entryRel, fParent, date, content)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun create_entry_child(entryParent: Entry?, date: Long, content: String?): Entry? {
        val ptr = nativeCreateEntryChild(mNativePtr, entryParent, date, content)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun create_entry_parent(entries: Array<Entry>, date: Long, content: String?): Entry? {
        val ptr = nativeCreateEntryParent(mNativePtr, entries, date, content)
        return if(ptr == 0L) null else Entry(ptr)
    }

    fun duplicate_entry(source: Entry): Entry? {
        val ptr = nativeDuplicateEntry(mNativePtr, source.mNativePtr)
        return if(ptr == 0L) null else Entry(ptr)
    }

    // adds a new entry to today even if there is already one or more:
    fun addToday(): Entry {
        val ptr = nativeCreateEntryDated(mNativePtr, 0, get_today(), false)
        return Entry(ptr)
    }

    fun dismiss_entry(entry: Entry): Entry? {
        val ptr = nativeDismissEntry(mNativePtr, entry)
        return if(ptr == 0L) null else Entry(ptr)
    }

    // TAGS ========================================================================================
    fun get_completion_tag(): DiaryElemTag? {
        val ptr = nativeGetCompletionTag(mNativePtr)
        if(ptr == 0L) return null
        val tag = DiaryElemTag(ptr)
        return when(tag._type) {
            Type.ENTRY -> Entry(ptr)
            Type.PARAGRAPH -> Paragraph(ptr)
            else -> tag
        }
    }

    //    void
    //    set_completion_tag( long id ) {
    //        m_completion_tag_id = id;
    //    }
    fun get_tag_by_id(id: Int): DiaryElemTag? {
        val ptr = nativeGetTagById(mNativePtr, id)
        if(ptr == 0L) return null
        val tag = DiaryElemTag(ptr)
        return when(tag._type) {
            Type.ENTRY -> Entry(ptr)
            Type.PARAGRAPH -> Paragraph(ptr)
            else -> tag
        }
    }

    // SEARCHING ===================================================================================
    fun get_search_str(): String? {
        return nativeGetSearchStr(mNativePtr)
    }

    fun set_search_str(text: String) {
        nativeSetSearchStr(mNativePtr, text)
    }

    fun start_search() {
        nativeStartSearch(mNativePtr)
    }

    fun stop_search() {
        nativeStopSearch(mNativePtr)
    }

    fun is_search_in_progress(): Boolean {
        return nativeIsSearchInProgress(mNativePtr)
    }

    fun get_match_count(): Int {
        return nativeGetMatchCount(mNativePtr)
    }

    fun get_matches(): Vector<HiddenFormat> {
        val ptrs = nativeGetMatches(mNativePtr)
        val matches = Vector<HiddenFormat>(ptrs.size)
        for(ptr in ptrs) if(ptr!=0L) matches.add(HiddenFormat(ptr))
        return matches
    }

    fun replace_all_matches(newtext: String?) {
        // TODO: 2.1: delegate to C++
    }

    // this version does not disturb the active search and is case-sensitive
    fun replace_all_matches(oldtext: String?, newtext: String?) {
        // TODO: 2.1: delegate to C++
    }

    fun clear_matches() {
        // TODO: 2.1: delegate to C++
    }

    // FILTERS =====================================================================================
    fun get_filter(name: String?): Filter? {
        // TODO: delegate to C++
        return null
    }

    fun get_filters(): Vector<Filter> {
        val ptrs = nativeGetFilters(mNativePtr)
        val filters = Vector<Filter>(ptrs.size)
        for(ptr in ptrs) {
            if(ptr != 0L) {
                filters.add(Filter(ptr))
            }
        }
        return filters
    }

    fun  // Non-null?
            get_filter_nontrashed(): Filter? {
        val ptr = nativeGetFilterNonTrashed(mNativePtr)
        return if(ptr == 0L) null else Filter(ptr)
    }

    fun  // Non-null?
            get_filter_trashed(): Filter? {
        val ptr = nativeGetFilterTrashed(mNativePtr)
        return if(ptr == 0L) null else Filter(ptr)
    }

    fun get_filter_list(): Filter? {
        val ptr = nativeGetFilterEntryList(mNativePtr)
        return if(ptr == 0L) null else Filter(ptr)
    }

    fun set_filter_list(filter: Filter) {
        nativeSetFilterEntryList(mNativePtr, filter.mNativePtr)
    }

    fun update_all_entries_filter_status(): Int {
        return nativeUpdateAllEntriesFilterStatus(mNativePtr)
    }

    fun rename_filter(filter: Filter?, new_name: String?): Boolean {
        return nativeRenameFilter(mNativePtr, filter, new_name)
    }

    fun create_filter(name0: String?): Filter? {
        val ptr = nativeCreateFilter(mNativePtr, name0)
        return if(ptr == 0L) null else Filter(ptr)
    }

    fun dismiss_filter(name: String?): Boolean {
        // TODO: 2.1: nativeDismissFilter(mNativePtr, name);
        return false
    }

    fun duplicate_filter(name: String?): Filter? {
        //long ptr = nativeDuplicateFilter(mNativePtr, name);
        //return ptr == 0 ? null : new Filter(ptr);
        return null // TODO: 2.1
    }

    // CHARTS ======================================================================================
    fun create_chart(name0: String?, definition: String?): ChartElem? {
        // TODO: 2.2: delegate to C++
        return null
    }

    fun get_chart(name: String?): ChartElem? {
        // TODO: delegate to C++
        return null
    }

    fun get_charts(): Vector<ChartElem> {
        val ptrs = nativeGetCharts(mNativePtr)
        val charts = Vector<ChartElem>(ptrs.size)
        for(ptr in ptrs) {
            if(ptr != 0L) {
                charts.add(ChartElem(ptr))
            }
        }
        return charts
    }

    fun set_chart_active(name: String?): Boolean {
        // TODO: delegate to C++
        return false
    }

    fun get_chart_active_name(): String {
        // TODO: delegate to C++
        return ""
    }

    fun rename_chart(old_name: String?, new_name: String?): Boolean {
        // TODO: delegate to C++
        return false
    }

    fun dismiss_chart(name: String?): Boolean {
        // TODO: delegate to C++
        return false
    }

    fun fill_up_chart_data(cd: ChartData?) {
        // TODO: delegate to C++
    }

    // THEMES ======================================================================================
    fun create_theme(name0: String?): Theme? {
        val ptr = nativeCreateTheme(mNativePtr, name0)
        return if(ptr == 0L) null else Theme(ptr)
    }

    fun get_theme(name: String): Theme? {
        val ptr = nativeGetTheme(mNativePtr, name)
        return if(ptr == 0L) null else Theme(ptr)
    }

    fun get_themes(): Vector<Theme> {
        val ptrs = nativeGetThemes(mNativePtr)
        val themes = Vector<Theme>(ptrs.size)
        for(ptr in ptrs) {
            if(ptr != 0L) {
                themes.add(Theme(ptr))
            }
        }
        return themes
    }

    fun dismiss_theme(theme: Theme) {
        // TODO....
    }

    fun rename_theme(theme: Theme, new_name: String?) {
        // TODO ...
    }

    // READING =====================================================================================
    /** reimplements C++'s Diary::set_path()  */
    fun setPath(ctx: Context, uriStr: String /*, SetPathType type*/): Result {
        removeLockIfNecessary(ctx)

        nativeSetUri(mNativePtr, uriStr)

        if(uriStr == EXAMPLE_DIARY_PATH) {
            nativeSetName(mNativePtr, EXAMPLE_DIARY_NAME)
            nativeSetReadOnly(mNativePtr)
            return Result.SUCCESS
        }

        nativeSetName(mNativePtr, FileUtil.getFileName(uriStr.toUri(), ctx))
        return if(isLocked(ctx)) Result.FILE_LOCKED else Result.SUCCESS
    }

    fun read_header(ctx: Context): Result {
        val uriStr = nativeGetUri(mNativePtr)

        // 1. Determine which stream to open
        val inputStream: InputStream? = try {
            when {
                uriStr == EXAMPLE_DIARY_PATH -> ctx.assets.open("example.diary")
                nativeGetContinueFromLock(mNativePtr) -> getLockStream(ctx)
                else -> ctx.contentResolver.openInputStream(uriStr.toUri())
            }
        } catch(e: Exception) {
            Log.e(Lifeograph.TAG, "Failed to open diary stream: ${e.message}")
            null
        }

        // 2. Validate stream
        if(inputStream == null) return Result.FILE_NOT_FOUND

        // 3. Process the stream
        return try {
            inputStream.use { stream ->
                val bytes = getBytesFromInputStream(stream)
                val resultIndex = nativeReadHeader(mNativePtr, bytes)
                Result.entries[resultIndex]
            }
        } catch(e: Exception) {
            Log.e(Lifeograph.TAG, "Error reading diary header: ${e.message}")
            Result.FAILURE
        }
    }

    // Helper method to convert InputStream to byte[]
    @Throws(IOException::class)
    private fun getBytesFromInputStream(`is`: InputStream): ByteArray {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(0xFFFF)
        var len = `is`.read(buffer)
        while(len != -1) {
            os.write(buffer, 0, len)
            len = `is`.read(buffer)
        }
        return os.toByteArray()
    }

    fun read_body(): Result {
        val result = nativeReadBody(mNativePtr)
        return Result.entries[result]
    }

    fun enableEditing(ctx: Context): Result {
//        if( nativeIsReadOnly(mNativePtr) ) {
//            Log.e( Lifeograph.TAG, "Diary: editing cannot be enabled. Diary is read-only" );
//            return Result.FILE_LOCKED;
//        }

        val result = writeLock(ctx)

        if(result == Result.SUCCESS) {
            nativeSetLoggedInEdit(mNativePtr)
            return Result.SUCCESS
        } else return result
    }

    // WRITING =====================================================================================
    // we reimplement in Android as it is not possible to deal with content:// uris in C++
    fun write(context: Context): Result {
        val uri = nativeGetUri(mNativePtr).toUri()
        val resolver = context.contentResolver

        // BACKUP THE PREVIOUS VERSION
        try {
            val prevDoc = DiaryDirectoryUtil.createLockFile(context, uri, SUFFIX_PREVVER)
            if(prevDoc != null) {
                writeStreamed(context, prevDoc.uri)
            }
        } catch(ex: IOException) {
            Log.e(Lifeograph.TAG, "Could not save previous version file: " + ex.message)
            // return Result.FILE_NOT_WRITABLE
        }

        // WRITE THE FILE
        val result = writeStreamed(context, uri)

        // DAILY BACKUP SAVES
        if(result == Result.SUCCESS && Lifeograph.sSaveDailyBackups) {
            try {
                val istream = resolver.openInputStream(uri)
                if(istream != null) {
                    val name = _name
                    val baseName =
                        if(name.endsWith(".diary")) name.substring(0, name.length - 6) else name
                    val backupFile = File(
                        context.filesDir,
                        baseName + "_(" + _id + ")_" +
                                format_string(get_today(), "YMD", '-') +
                                ".diary"
                                         )
                    copyFile(istream, backupFile)
                    istream.close()
                }
            } catch(ex: IOException) {
                Log.e(Lifeograph.TAG, "Could not save daily backup file: " + ex.message)
            }
        }

        return result
    }

    fun writeStreamed(context: Context, uri: Uri): Result {
        if(nativeWriteUri(mNativePtr, uri.toString()) == Result.SUCCESS.ordinal) try {
            val resolver = context.contentResolver

            resolver.openOutputStream(uri, "wt").use { ostream ->
                if(ostream == null) return Result.FAILURE
                // get the raw bytes (encrypted or plain) from C++
                val data = nativeGetStrStream(mNativePtr)
                if(data != null) {
                    ostream.write(data)
                    ostream.flush()
                    return Result.SUCCESS
                }
            }
        } catch(ex: IOException) {
            Log.e(Lifeograph.TAG, "Failed to save diary: " + ex.message)
        }
        return Result.FAILURE
    }

    fun write_txt(path: String, filter: Filter?): Result {
        return Result.entries[nativeWriteTxt(
            mNativePtr, path,
            filter?.mNativePtr ?: 0
                                            )]
    }

//    fun getNeighborFileDocument(context: Context, uri: Uri, suffix: String): DocumentFile? {
//        try {
//            val sourceFile: DocumentFile = DocumentFile.fromSingleUri(context, uri)!!
//            val parentDir = sourceFile.parentFile
//
//            if(parentDir != null && parentDir.isDirectory) {
//                val backupName = sourceFile.name + suffix
//
//                // look for existing backup or create new one
//                var neighborDoc = parentDir.findFile(backupName)
//                if(neighborDoc == null) {
//                    neighborDoc = parentDir.createFile("application/octet-stream", backupName)
//                }
//
//                return neighborDoc
//            }
//        } catch(ex: Exception) {
//            Log.e(Lifeograph.TAG, "Error getting neighbor file: " + ex.message)
//        }
//        return null
//    }

    fun writeLock(context: Context): Result {
        try {
            val uri = nativeGetUri(mNativePtr).toUri()
            val lockDoc = DiaryDirectoryUtil.createLockFile( context, uri, SUFFIX_LOCK)
            if(lockDoc != null) {
                writeStreamed(context, lockDoc.uri)
                return Result.SUCCESS
            }
        } catch(ex: IOException) {
            ex.message?.let { Log.e(Lifeograph.TAG, it) }
        }
        Log.e(Lifeograph.TAG, "Could not save backup file!")
        return Result.FILE_NOT_WRITABLE
    }

    fun getLockStream(ctx: Context): InputStream? {
        try {
            val resolver = ctx.contentResolver
            val uri = nativeGetUri(mNativePtr).toUri()
            val lockDoc = DiaryDirectoryUtil.createLockFile( ctx, uri, SUFFIX_LOCK)
            if(lockDoc != null) {
                return resolver.openInputStream(lockDoc.uri)
            }
        } catch(ex: IOException) {
            Log.e(Lifeograph.TAG, "Could not open lock file for reading: " + ex.message)
        }
        return null
    }

    fun removeLockIfNecessary(ctx: Context): Boolean {
        if(!is_in_edit_mode()) return false
        val uri = nativeGetUri(mNativePtr).toUri()
        return DiaryDirectoryUtil.deleteLockFile(ctx, uri, SUFFIX_LOCK)
    }

    fun set_continue_from_lock() {
        nativeSetContinueFromLock(mNativePtr)
    }

    fun isLocked(context: Context): Boolean {
        val uri = nativeGetUri(mNativePtr).toUri()
        return DiaryDirectoryUtil.isLocked( context, uri, SUFFIX_LOCK )
    }

    fun writeUnsaved(ctx: Context): Result {
        try {
            val uri = nativeGetUri(mNativePtr).toUri()
            val unsavedDoc = DiaryDirectoryUtil.createLockFile(ctx, uri, SUFFIX_UNSAVED)
            if(unsavedDoc != null) {
                writeStreamed(ctx, unsavedDoc.uri)
                return Result.SUCCESS
            }
        } catch(ex: IOException) {
            ex.message?.let { Log.e(Lifeograph.TAG, it) }
        }
        Log.e(Lifeograph.TAG, "Could not backup unsaved changes!")
        return Result.FILE_NOT_WRITABLE
    }

    // NATIVE METHODS ==============================================================================
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeInitNewPre(ptr: Long)
    private external fun nativeInitNew(ptr: Long, path: String?, pw: String?): Int
    private external fun nativeClear(ptr: Long)
    private external fun nativeReadHeader(ptr: Long, data: ByteArray?): Int
    private external fun nativeReadBody(ptr: Long): Int
    private external fun nativeSetName(ptr: Long, name: String?)
    private external fun nativeSetReadOnly(ptr: Long)
    private external fun nativeGetPassphrase(ptr: Long): String?
    private external fun nativeSetPassphrase(ptr: Long, pw: String?): Boolean

    //private native int nativeGetReadVersion(long ptr);
    private external fun nativeGetUri(ptr: Long): String
    private external fun nativeSetUri(ptr: Long, uri: String?)
    private external fun nativeGetUriUnsaved(ptr: Long): String?
    private external fun nativeGetSize(ptr: Long): Int
    private external fun nativeIsOld(ptr: Long): Boolean
    private external fun nativeIsEncrypted(ptr: Long): Boolean
    private external fun nativeIsOpen(ptr: Long): Boolean
    private external fun nativeIsInEditMode(ptr: Long): Boolean
    private external fun nativeCanEnterEditMode(ptr: Long): Boolean
    private external fun nativeSetLoggedInEdit(ptr: Long)
    private external fun nativeGetLang(ptr: Long): String?
    private external fun nativeSetLang(ptr: Long, lang: String?)
    private external fun nativeIsDayHoliday(ptr: Long, date: Long): Boolean
    private external fun nativeIsDayWorkday(ptr: Long, date: Long): Boolean

    private external fun nativeCreateNewId(mNativePtr: Long, element: DiaryElement?): Int
    private external fun nativeGetElement(ptr: Long, id: Int): Long
    private external fun nativeGetEntry1st(ptr: Long): Long
    private external fun nativeGetEntryMostCurrent(ptr: Long): Long
    private external fun nativeGetEntryById(ptr: Long, id: Int): Long
    private external fun nativeGetParagraphById(ptr: Long, id: Int): Long
    private external fun nativeGetEntryToday(ptr: Long): Long
    private external fun nativeGetEntryByDate(ptr: Long, date: Long): Long
    private external fun nativeGetEntryByName(ptr: Long, name: String?): Long
    private external fun nativeGetEntriesByFilter(ptr: Long, ptr_filter: Long): LongArray
    private external fun nativeGetEntryCountOnDay(ptr: Long, date: Long): Int
    private external fun nativeSetEntryName(ptr: Long, entry: Entry?, name: String?)
    private external fun nativeSetEntryDate(ptr: Long, entry: Entry?, date: Long)
    private external fun nativeCreateEntry(
        ptr: Long, entry_rel: Entry?, fParent: Boolean, date: Long,
        content: String?
                                          ): Long

    private external fun nativeCreateEntryChild(
        ptr: Long, entry_rel: Entry?, date: Long,
        content: String?
                                               ): Long

    private external fun nativeCreateEntryParent(
        ptr: Long, entries: Array<Entry>, date: Long,
        content: String?
                                                ): Long

    private external fun nativeCreateEntryDated(
        ptr: Long, ptr_entry_rel: Long, date: Long,
        fMileStone: Boolean
                                               ): Long

    private external fun nativeDuplicateEntry(ptr: Long, ptr_entry: Long): Long
    private external fun nativeDismissEntry(ptr: Long, entry: Entry?): Long
    private external fun nativeGetCompletionTag(mNativePtr: Long): Long
    private external fun nativeGetTagById(mNativePtr: Long, id: Int): Long

    private external fun nativeGetFilters(mNativePtr: Long): LongArray
    private external fun nativeGetFilterNonTrashed(mNativePtr: Long): Long
    private external fun nativeGetFilterTrashed(mNativePtr: Long): Long
    private external fun nativeGetFilterEntryList(mNativePtr: Long): Long
    private external fun nativeSetFilterEntryList(mNativePtr: Long, ptr_filter: Long)
    private external fun nativeUpdateAllEntriesFilterStatus(mNativePtr: Long): Int
    private external fun nativeRenameFilter(mNativePtr: Long, filter: Filter?,
                                            name: String? ): Boolean

    private external fun nativeCreateFilter(mNativePtr: Long, name0: String?): Long

    private external fun nativeGetCharts(mNativePtr: Long): LongArray

    private external fun nativeCreateTheme(mNativePtr: Long, name0: String?): Long
    private external fun nativeGetTheme(mNativePtr: Long, name: String?): Long
    private external fun nativeGetThemes(mNativePtr: Long): LongArray

    private external fun nativeWriteUri(mNativePtr: Long, uri: String?): Int
    private external fun nativeGetStrStream(mNativePtr: Long): ByteArray?
    private external fun nativeWriteTxt(mNativePtr: Long, path: String?, ptr_filter: Long): Int
    private external fun nativeGetContinueFromLock(ptr: Long): Boolean
    private external fun nativeSetContinueFromLock(ptr: Long)

    private external fun nativeGetSearchStr(mNativePtr: Long): String?
    private external fun nativeSetSearchStr(mNativePtr: Long, str: String?)
    private external fun nativeStartSearch(mNativePtr: Long)
    private external fun nativeStopSearch(mNativePtr: Long)
    private external fun nativeDestroySearchThreads(mNativePtr: Long)
    private external fun nativeIsSearchInProgress(mNativePtr: Long): Boolean
    private external fun nativeGetMatchCount(mNativePtr: Long): Int
    private external fun nativeGetMatches(mNativePtr: Long): LongArray // HELPER FUNCTIONS ============================================================================

    companion object {
        init {
            System.loadLibrary("gpg-error")
            System.loadLibrary("gcrypt")
            System.loadLibrary("lifeograph_core")

            initCipher()
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
        const val SUFFIX_LOCK: String = ".~LOCK~"
        const val SUFFIX_UNSAVED: String = ".~unsaved~"
        const val SUFFIX_PREVVER: String = ".~previousversion~"

        const val EXAMPLE_DIARY_PATH: String = "*/E/X/A/M/P/L/E/D/I/A/R/Y/*"
        const val EXAMPLE_DIARY_NAME: String = "*** Example Diary ***"

        const val PASSPHRASE_MIN_SIZE: Int = 4 // TODO get from C++?

        fun initMain() {
            if(nativeGetMain() == 0L) nativeCreateMain()
        }

        val main: Diary
            get() = Diary(nativeGetMain())


        // C++ BASED METHODS WITH A DIFFERENT USAGE ====================================================
        //    static native void
        //    set_date_format_order( String format ) { nativeSetDateFormatSeparator(format); }
        //    static native void
        //    set_date_format_separator( String format ) { nativeSetDateFormatSeparator(format); }
        // NATIVE METHODS ==============================================================================
        @JvmStatic
        private external fun initCipher(): Boolean
        @JvmStatic
        private external fun nativeCreate(): Long
        @JvmStatic
        external fun nativeCreateMain()
        @JvmStatic
        external fun nativeGetMain(): Long
    }
}
