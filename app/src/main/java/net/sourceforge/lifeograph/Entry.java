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

import android.text.Spanned;
import java.util.Vector;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

public class Entry extends DiaryElemTag {
    protected Entry(long nativePtr) {
        super(nativePtr);
    }

    @Override
    public int
    getIcon() { // Java specific
        if( ( get_todo_status() & ES_FILTER_TODO_PURE ) != 0 )
            return Lifeograph.getTodoIcon( get_todo_status() & ES_FILTER_TODO_PURE );
        else
            return( get_unit().isEmpty() ?
                    ( has_children() ? R.drawable.ic_entry_parent : R.drawable.ic_entry ) :
                    R.drawable.ic_tag );
    }

    void
    update_name() {
        nativeUpdateName(mNativePtr);
    }

    public Spanned get_styled_title() {
        String rawTitle = get_list_str(); // This contains your HTML-like markup
        // FROM_HTML_MODE_LEGACY is the standard flag for basic HTML rendering
        return HtmlCompat.fromHtml(rawTitle, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public String
    get_list_str() { return nativeGetListStr(mNativePtr); }

    @Override
    public String
    get_info_str() { return nativeGetInfoStr(mNativePtr); }

    public boolean
    is_filtered_out() { return nativeIsFilteredOut(mNativePtr); }

    // HIERARCHY ===================================================================================
    Entry
    get_parent(){
        long ptr = nativeGetParent(mNativePtr);
        return ptr != 0 ? new Entry(ptr) : null;
    }

    int
    get_generation() { return nativeGetGeneration(mNativePtr); }

    Entry
    get_next(){
        long ptr = nativeGetNext(mNativePtr);
        return ptr != 0 ? new Entry(ptr) : null;
    }

    Entry
    get_next_stright(){
        long ptr = nativeGetNextStraight(mNativePtr);
        return ptr != 0 ? new Entry(ptr) : null;
    }

    public boolean
    has_children() { return get_child_count() > 0; }

    int
    get_child_count() { return nativeGetChildCount(mNativePtr); }

    Entry
    get_child_1st(){
        long ptr = nativeGetChild1st(mNativePtr);
        return ptr != 0 ? new Entry(ptr) : null;
    }

    Vector< Entry >
    get_descendants() {
        // This might be expensive to create all Java objects
        long[] ptrs = nativeGetDescendants(mNativePtr);
        Vector<Entry> descendants = new Vector<>(ptrs.length);
        for (long ptr : ptrs) descendants.add(new Entry(ptr));
        return descendants;
    }

    int
    get_descendant_depth() {
        return nativeGetDescendantDepth(mNativePtr);
    }

    // TEXTUAL CONTENTS ============================================================================
    boolean
    is_empty() { return nativeIsEmpty(mNativePtr); }

    String
    get_text() { return nativeGetText(mNativePtr); } // does not have a redundant \n
    String
    get_text_visible() {
        String text = nativeGetTextVisible( mNativePtr );
        return text.substring( 0, text.length() - 1 ); // text always ends with a redundant \n
    }

    void
    clear_text() {
        nativeClearText(mNativePtr);
    }

    void
    set_text( String text ) {
        nativeSetText(mNativePtr, text);
    }

    void
    insert_text( int pos, final String text ) {
        nativeInsertText(mNativePtr, pos, text);
    }

    void
    erase_text( int pos_bgn, int pos_end ) {
        nativeEraseText(mNativePtr, pos_bgn, pos_end);
    }

    Paragraph
    get_paragraph( int pos, boolean flag_visible_only ) {
        long ptr = nativeGetParagraphAtPos(mNativePtr, pos, flag_visible_only);
        return ptr != 0 ? new Paragraph(ptr) : null;
    }
    Paragraph
    get_paragraph_1st() {
        long ptr = nativeGetParagraph1st(mNativePtr);
        return ptr != 0 ? new Paragraph(ptr) : null;
    }
    Paragraph
    get_paragraph_last() {
        long ptr = nativeGetParagraphLast(mNativePtr);
        return ptr != 0 ? new Paragraph(ptr) : null;
    }

    Paragraph
    add_paragraph_before( String text, Paragraph para_after ) {
        long ptr = nativeAddParagraphBefore(mNativePtr, text, para_after.mNativePtr);
        return ptr != 0 ? new Paragraph(ptr) : null;
    }

    Paragraph
    add_paragraphs_after( Paragraph para_bgn, Paragraph para_before ) {
        long ptr = nativeAddParagraphsAfter(mNativePtr, para_bgn.mNativePtr,
                                            para_before.mNativePtr);
        return ptr != 0 ? new Paragraph(ptr) : null;
    }

//    void
//    clear_paragraph_data( int pos_b, int pos_e ) {
//        if (mNativePtr != 0) {
//            // nativeClearParagraphData(mNativePtr, pos_b, pos_e);
//            return;
//        }
//        int para_offset = 0;
//
//        for( Paragraph para : m_paragraphs ) {
//            if( pos_b <= para_offset ) {
//                if( pos_e >= ( para_offset + para.get_size() ) ) {
//                    para.clear_tags();
//                    para.m_date = Date.NOT_SET;
//                }
//                else
//                    break;
//            }
//            para_offset += ( para.get_size() + 1 );  // +1 is for \n
//        }
//    }

    String
    get_description() { // returns 2nd non-empty paragraph
        return nativeGetDescription(mNativePtr);
    }

    // FAVOREDNESS =================================================================================
    boolean
    is_favorite() {
        return nativeIsFavorite(mNativePtr);
    }

    void
    toggle_favorite() {
        nativeToggleFavorite(mNativePtr);
    }

    // LANGUAGE ====================================================================================

    // TRASH FUNCTIONALITY =========================================================================
    boolean
    is_trashed() { return nativeIsTrashed(mNativePtr); }

    void
    set_trashed( boolean trashed ) { nativeSetTrashed(mNativePtr, trashed); }

    // TAGS ========================================================================================
    boolean
    has_tag( Entry tag ) { return nativeHasTag(mNativePtr, tag.mNativePtr); }

    // THEME =======================================================================================
    Theme
    get_theme() {
        long ptr = nativeGetTheme(mNativePtr);
        return ptr != 0 ? new Theme(ptr) : null;
    }

    void
    set_theme( Theme theme ){
        nativeSetTheme(mNativePtr, theme.mNativePtr);
    }

    boolean
    is_theme_set() { return nativeIsThemeSet(mNativePtr); }

    // TO-DO STATUS ================================================================================
    protected static boolean
    is_status_ready( int s ) {
        return( ( s & ES_PROGRESSED ) != 0 || ( ( s & ES_TODO ) != 0 && ( s & ES_DONE ) != 0 ) );
    }

    boolean
    update_todo_status() { return nativeUpdateTodoStatus(mNativePtr); }

    double
    get_completion() { return nativeGetCompletion(mNativePtr); }

    double
    get_completed() { return nativeGetCompleted(mNativePtr); }

    double
    get_workload() { return nativeGetWorkload(mNativePtr); }

    // LOCATION ====================================================================================
//    void
//    set_location( double lat, double lon ) {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        m_location.latitude = lat;
//        m_location.longitude = lon;
//    }
//    void
//    remove_location() {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        m_location.unset();
//    }
//    boolean
//    is_location_set() {
//        if (mNativePtr != 0) {
//            return nativeHasLocation(mNativePtr);
//        }
//        return m_location.is_set();
//    }
//
//    boolean
//    is_map_path_set() {
//        if (mNativePtr != 0) {
//            return !nativeGetMapPath(mNativePtr).isEmpty();
//        }
//        return( !m_map_path.isEmpty() );
//    }
//    void
//    clear_map_path() {
//        nativeClearMapPath(mNativePtr);
//    }
//
//    void
//    add_map_path_point( double lat, double lon ) {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        m_map_path.add( new Coords( lat, lon ) );
//    }
//    void
//    remove_last_map_path_point() {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        m_map_path.remove( m_map_path.size() - 1 );
//    }
//
//    void
//    remove_map_path_point( Coords pt_other ) {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        int i= 0;
//        for( Coords pt : m_map_path ) {
//            if( pt.is_equal_to( pt_other ) ) {
//                m_map_path.remove( i );
//                return;
//            }
//            i++;
//        }
//    }
//
//    Coords
//    get_map_path_end() {
//        if (mNativePtr != 0) {
//            // TODO
//        }
//        return m_map_path.get( m_map_path.size() - 1 );
//    }

    double
    get_map_path_length() {
        return nativeGetMapPathLength(mNativePtr);
    }

    // SUB CLASSES =================================================================================
    public static class Coords
    {
        public Coords() {
            latitude = -0.1;
            longitude = -0.1;
        }
        public Coords( double lat, double lon ) {
            latitude = lat;
            longitude = lon;
        }

        public String
        to_string() {
            return( latitude + ", " + longitude );
        }

        public boolean
        is_set() {
            return( latitude != -0.1 );
        }

        public void
        unset() {
            latitude = -0.1;
            longitude = -0.1;
        }

        public boolean
        is_equal_to( @NonNull Coords pt2 ) {
            return( pt2.latitude == latitude && pt2.longitude == longitude );
        }

        public double latitude;
        public double longitude;
    }

    // NATIVE FUNCTIONS ============================================================================
    private native long nativeGetId(long ptr);
    private native long nativeGetParent(long ptr);
    private native int nativeGetGeneration(long ptr);
    private native long nativeGetNext(long ptr);
    private native long nativeGetNextStraight(long ptr);
    private native long nativeGetChild1st(long ptr);
    private native int nativeGetChildCount(long ptr);
    private native long[] nativeGetDescendants(long ptr);
    private native int nativeGetDescendantDepth(long ptr);
    private native int nativeGetSize(long ptr);
    private native long nativeGetDate(long ptr);
    private native void nativeSetDate(long ptr, long date);
    private native boolean nativeHasName(long ptr);
    private native String nativeGetName(long ptr);
    private native void nativeUpdateName(long ptr);
    private native String nativeGetListStr(long ptr);
    private native String nativeGetInfoStr(long ptr);
    private native boolean nativeIsFilteredOut(long ptr);
    private native boolean nativeIsEmpty(long ptr);
    private native String nativeGetText(long ptr);
    private native String nativeGetTextVisible(long ptr);
    private native void nativeClearText(long ptr);
    private native void nativeSetText(long ptr, String text);
    private native void nativeInsertText(long ptr, int pos, String text);
    private native void nativeEraseText(long ptr, int pos_bgn, int pos_end);
    private native long nativeGetParagraphAtPos(long ptr, int pos, boolean flag_visible_only);
    private native long nativeGetParagraph1st(long ptr);
    private native long nativeGetParagraphLast(long ptr);
    private native long nativeAddParagraphBefore(long ptr, String text, long ptr_para_after);
    private native long nativeAddParagraphsAfter(long ptr, long ptr_para_bgn, long ptr_para_before);
    private native String nativeGetDescription(long ptr);
    private native boolean nativeIsFavorite(long ptr);
    private native void nativeToggleFavorite(long ptr);
    private native String nativeGetLangFinal(long ptr);
    private native boolean nativeIsTrashed(long ptr);
    private native void nativeSetTrashed(long ptr, boolean trashed);
    private native boolean nativeHasTag(long ptr, long tagPtr);
    private native boolean nativeHasTagBroad(long ptr, long tagPtr);
    private native double nativeGetTagValue(long ptr, long tagPtr, boolean average);
    private native double nativeGetTagValuePlanned(long ptr, long tagPtr, boolean average);
    private native double nativeGetTagValueRemaining(long ptr, long tagPtr, boolean average);
    private native long nativeGetSubTagFirst(long ptr, long tagPtr);
    private native long nativeGetSubTagLast(long ptr, long tagPtr);
    private native long nativeGetSubTagLowest(long ptr, long tagPtr);
    private native long nativeGetSubTagHighest(long ptr, long tagPtr);
    private native void nativeAddTag(long ptr, long tagPtr, double value);
    private native long nativeGetTheme(long ptr);
    private native boolean nativeSetTheme(long ptr, long ptr_theme);
    private native boolean nativeIsThemeSet(long ptr);
    private native boolean nativeUpdateTodoStatus(long ptr);
    private native double nativeGetCompletion(long ptr);
    private native double nativeGetCompleted(long ptr);
    private native double nativeGetWorkload(long ptr);
    private native boolean nativeHasLocation(long ptr);
    private native double nativeGetMapPathLength(long ptr);
}
