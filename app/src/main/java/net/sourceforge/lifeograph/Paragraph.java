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

public class Paragraph extends DiaryElemTag
{
//    final static char JT_LEFT = '<';
//    final static char JT_CENTER = '|';
//    final static char JT_RIGHT = '>';

    protected
    Paragraph(long nativePtr) { super(nativePtr); }
    public Paragraph
    get_prev() {
        long ptr = nativeGetPrev( mNativePtr );
        return ptr == 0 ? null : new Paragraph( ptr ); }
    public Paragraph
    get_next() {
        long ptr = nativeGetNext( mNativePtr );
        return ptr == 0 ? null : new Paragraph( ptr ); }
    public Paragraph
    get_next_visible() {
        long ptr = nativeGetNextVisible( mNativePtr );
        return ptr == 0 ? null : new Paragraph( nativeGetNextVisible( mNativePtr ) ); }

    public Entry
    get_host() {
        long ptr = nativeGetHost( mNativePtr );
        return ptr == 0 ? null : new Entry(nativeGetHost( mNativePtr ) ); }

    public boolean
    is_empty() { return nativeIsEmpty(mNativePtr); }

    public String
    get_text() { return nativeGetText(mNativePtr); }

//    public void set_text( String text ) {
//        m_text = text;
//        update_per_text();
//    }
//    public void append( String text ) {
//        m_text += text;
//        update_per_text();
//    }
//    public void insert_text( int pos, String text ){
//        StringBuilder str_bld = new StringBuilder( m_text );
//        str_bld.insert( pos, text );
//        m_text = str_bld.toString();
//        update_per_text();
//    }
//    public void erase_text( int pos, int size ) {
//        StringBuilder str_bld = new StringBuilder( m_text );
//        str_bld.delete( pos, pos + size );
//        m_text = str_bld.toString();
//        update_per_text();
//    }
//    public void replace_text( int pos, int size, String text ) {
//        StringBuilder str_bld = new StringBuilder( m_text );
//        str_bld.delete( pos, pos + size );
//        str_bld.insert( pos, text );
//        m_text = str_bld.toString();
//        update_per_text();
//    }
//
//    public void clear_tags() {
//        m_tags.clear();
//        m_tags_planned.clear();
//        m_tags_in_order.clear();
//    }

    public boolean
    is_title() { return nativeIsTitle(mNativePtr); }
    public char
    get_heading_level() { return nativeGetHeadingLevel(mNativePtr); }
    public void
    set_heading_level( char level ) {  nativeSetHeadingLevel(mNativePtr, level); }

    public char
    get_alignment() { return nativeGetAlignment(mNativePtr); }
    public void
    set_alignment( char alignment ) { nativeSetAlignment( mNativePtr, alignment ); }

    public char
    get_list_type() { return nativeGetListType(mNativePtr); }
    public void
    set_list_type( char type ) { nativeSetListType( mNativePtr, type ); }
    public void
    clear_list_type() { nativeClearListType( mNativePtr ); }
    public boolean
    isList() { return nativeGetListType(mNativePtr) != '_'; }

    public String
    get_list_order_str() { return nativeGetListOrderStr(mNativePtr); }

    public int
    get_indent_level() { return nativeGetIndentLevel(mNativePtr); }
    public void
    set_indent_level( int level ) { nativeSetIndentLevel(mNativePtr, level); }
    public void
    indent() { nativeIndent(mNativePtr); }
    public void
    unindent() { nativeUnindent(mNativePtr); }

    public char
    get_quot_type() { return nativeGetQuotType(mNativePtr); }
    public void
    set_quot_type( char quot_type ) { nativeSetQuotType( mNativePtr, quot_type ); }
    public boolean
    is_quote() { return nativeIsQuote(mNativePtr); }
    public boolean
    is_code() { return nativeIsCode(mNativePtr); }

    public boolean
    has_hrule() { return nativeHasHRule(mNativePtr); }
    public void
    set_hrule( boolean fHasHRule ) { nativeSetHRule( mNativePtr, fHasHRule ); }

    public boolean
    is_foldable() { return nativeIsFoldable(mNativePtr); }

    public int
    get_bgn_offset_in_host() { return nativeGetBgnOffsetInHost(mNativePtr); }

    public HiddenFormat
    get_format_at( char type, int pos) {
        long ptr = nativeGetFormatAt(mNativePtr, type, pos);
        return ptr == 0 ? null : new HiddenFormat(ptr);
    }
    public HiddenFormat
    get_format_oneof_at( char type, int pos) {
        long ptr = nativeGetFormatOneOfAt(mNativePtr, type, pos);
        return ptr == 0 ? null : new HiddenFormat(ptr);
    }

    public Vector<HiddenFormat>
    get_formats() {
        long[] ptrs = nativeGetFormats(mNativePtr);
        Vector<HiddenFormat> formats = new Vector<>(ptrs.length);
        for (long ptr : ptrs) formats.add(new HiddenFormat(ptr));
        return formats;
    }

    public void
    toggle_format(char type, int startPos, int endPos, boolean fAlready) {
        nativeToggleFormat(mNativePtr, type, startPos, endPos, fAlready);
    }

    // NATIVE FUNCTIONS ============================================================================
    private native long nativeGetPrev(long ptr);
    private native long nativeGetNext(long ptr);
    private native long nativeGetNextVisible(long ptr);
    private native long nativeGetHost(long ptr);
    private native boolean nativeIsEmpty(long ptr);
    private native String nativeGetText(long ptr);
    private native boolean nativeIsTitle(long ptr);
    private native char nativeGetHeadingLevel(long ptr);
    private native void nativeSetHeadingLevel(long ptr, char level);
    private native char nativeGetAlignment(long ptr);
    private native void nativeSetAlignment(long ptr, char alignment);
    private native int nativeGetIndentLevel(long ptr);
    private native void nativeSetIndentLevel(long ptr, int level);
    private native void nativeIndent(long ptr);
    private native void nativeUnindent(long ptr);
    private native char nativeGetListType(long ptr);
    private native void nativeSetListType(long ptr, char type);
    private native void nativeClearListType(long ptr);
    private native String nativeGetListOrderStr(long ptr);
    private native char nativeGetQuotType(long ptr);
    private native void nativeSetQuotType(long ptr, char quot_type);
    private native boolean nativeIsQuote(long ptr);
    private native boolean nativeIsCode(long ptr);
    private native boolean nativeHasHRule(long ptr);
    private native void nativeSetHRule(long ptr, boolean fHasHRule);
    private native boolean nativeIsFoldable(long ptr);
    private native int nativeGetBgnOffsetInHost(long ptr);
    private native int nativeGetEndOffsetInHost(long ptr);
    private native long nativeGetFormatAt(long ptr, char type, int pos);
    private native long nativeGetFormatOneOfAt(long ptr, char type, int pos);
    private native long[] nativeGetFormats(long ptr);
    private native void nativeToggleFormat(long ptr, char type, int startPos, int endPos,
                                           boolean fAlready);

}
