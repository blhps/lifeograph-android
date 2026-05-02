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

class FiltererContainer(nativePtr: Long) : Filterer( nativePtr) {
    // FILTERERSTATUS ==============================================================================
    class FiltererStatus internal constructor(nativePtr: Long) : Filterer( nativePtr) {
        var included_statuses: Int
            get() { return nativeGetIncludedStatuses(mNativePtr) }
            set(value) { nativeSetIncludedStatuses(mNativePtr, value) }

        private external fun nativeGetIncludedStatuses(ptr: Long): Int
        private external fun nativeSetIncludedStatuses(ptr: Long, value: Int)
    }

    // FILTERERFAVORITE ============================================================================
    class FiltererFavorite internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERTRASHED =============================================================================
    class FiltererTrashed internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERIS ==================================================================================
    class FiltererIs internal constructor(nativePtr: Long) : Filterer( nativePtr) {
        var id: Int
            get() { return nativeGetId(mNativePtr) }
            set(value) { nativeSetId(mNativePtr, value) }

        private external fun nativeGetId(ptr: Long): Int
        private external fun nativeSetId(ptr: Long, value: Int)
    }

    // FILTERERHASTAG ==============================================================================
    class FiltererHasTag internal constructor(nativePtr: Long) : Filterer( nativePtr) {
        var id: Int
            get() { return nativeGetId(mNativePtr) }
            set(value) { nativeSetId(mNativePtr, value) }

        private external fun nativeGetId(ptr: Long): Int
        private external fun nativeSetId(ptr: Long, value: Int)
    }

    // FILTERERTHEME ===============================================================================
    class FiltererTheme internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERBETWEENDATES ========================================================================
    class FiltererBetweenDates internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERBETWEENENTRIES ======================================================================
    class FiltererBetweenEntries internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERCOMPLETION ==========================================================================
    class FiltererCompletion internal constructor(nativePtr: Long) : Filterer( nativePtr)

    // FILTERERUNSUPPORTED (for yet to be supported filters in Android) ============================
    class FiltererUnsupported internal constructor(nativePtr: Long) : Filterer( nativePtr) {
    }

    // FILTERERCONTAINER ===========================================================================
    protected val flag_or: Boolean get() { return nativeIsOr(mNativePtr) }

    val pipeline: List<Filterer> get() {
        val pointers = nativeGetPipeline(mNativePtr)
        return pointers.map { ptr ->
            // instantiate the actual types:
            when (nativeGetFiltererType(ptr)) {
                Filter.FT_STATUS -> FiltererStatus(ptr)
                Filter.FT_FAVORITE -> FiltererFavorite(ptr)
                Filter.FT_TRASHED -> FiltererTrashed(ptr)
                Filter.FT_IS -> FiltererIs(ptr)
                Filter.FT_HAS_TAG -> FiltererHasTag(ptr)
                else -> FiltererUnsupported(ptr)
            }
        }
    }

    override fun isContainer(): Boolean { return true }

    fun update_state() {}

    fun get_as_string(): String { return nativeGetAsString(mNativePtr) }

    fun set_from_string(string: String) {
        nativeSetFromString(mNativePtr, string)
        update_logic_label()
    }

    fun toggle_logic() { nativeToggleLogic(mNativePtr) }

    fun update_logic_label() {}

    fun add_filterer_status() { nativeAddFiltererStatus(mNativePtr) }
    fun add_filterer_favorite() { nativeAddFiltererFavorite(mNativePtr) }
    fun add_filterer_trashed() { nativeAddFiltererTrashed(mNativePtr) }
    fun add_filterer_is() { nativeAddFiltererIs(mNativePtr) }
    fun add_filterer_has_tag() { nativeAddFiltererHasTag(mNativePtr) }

//    fun add_filterer_theme(theme: Theme?, f_has: Boolean) {
//        nativeAddFiltererTheme(mNativePtr, theme, f_has)
//    }
//
//    fun add_filterer_between_dates(
//        date_b: Long,
//        f_incl_b: Boolean,
//        date_e: Long,
//        f_incl_e: Boolean
//                                  ) {
//        m_pipeline.add(
//            FiltererBetweenDates(m_p2diary, this, date_b, f_incl_b, date_e, f_incl_e)
//                      )
//    }
//
//    fun add_filterer_between_entries(
//        entry_b: Entry?,
//        f_inc_b: Boolean,
//        entry_e: Entry?,
//        f_inc_e: Boolean
//                                    ) {
//        m_pipeline.add(
//            FiltererBetweenEntries(m_p2diary, this, entry_b, f_inc_b, entry_e, f_inc_e)
//                      )
//    }
//
//    fun add_filterer_completion(compl_b: Double, compl_e: Double) {
//        m_pipeline.add(FiltererCompletion(m_p2diary, this, compl_b, compl_e))
//    }
//
//    fun add_filterer_subgroup(): FiltererContainer {
//        val container =
//            FiltererContainer(
//                m_p2diary, this,
//                m_p2container != null && !(m_p2container.m_flag_or)
//                             )
//        m_pipeline.add(container)
//        return container
//    }

    fun remove_filterer(filterer: Filterer) {
        if(nativeRemoveFilterer(mNativePtr, filterer.mNativePtr))
            update_state()
    }

    //protected Vector< Filterer > m_pipeline = new Vector<>();


    // NATIVE METHODS ==============================================================================
    //private native long nativeCreate(long ptr_diary);
    //private native void nativeDestroy(long ptr);
    private external fun nativeGetAsString(ptr: Long): String
    private external fun nativeSetFromString(ptr: Long, str: String?)

    private external fun nativeToggleLogic(ptr: Long)
    private external fun nativeIsOr(ptr: Long): Boolean

    private external fun nativeGetPipeline(ptr: Long): LongArray
    private external fun nativeGetFiltererType(ptr: Long): Char

    private external fun nativeAddFiltererFavorite(ptr: Long)
    private external fun nativeAddFiltererStatus(ptr: Long)
    private external fun nativeAddFiltererTrashed(ptr: Long)
    private external fun nativeAddFiltererIs(ptr: Long)
    private external fun nativeAddFiltererHasTag(ptr: Long)

    private external fun nativeRemoveFilterer(ptr: Long, ptr_filterer: Long): Boolean
}
