/* *********************************************************************************

    Copyright (C) 2012-2020 Ahmet Öztürk (aoz_2@yahoo.com)

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.util.Log;

enum Result {
    OK, ABORTED, SUCCESS, FAILURE, COULD_NOT_START, /*COULD_NOT_FINISH,*/ WRONG_PASSWORD,
    /*APPARENTLY_ENCRYTED_FILE, APPARENTLY_PLAIN_FILE,*/
    INCOMPATIBLE_FILE_OLD, INCOMPATIBLE_FILE_NEW, CORRUPT_FILE,
    FILE_NOT_FOUND, FILE_NOT_READABLE, FILE_NOT_WRITABLE, FILE_LOCKED
}

public class Diary extends DiaryElementChart
{
    static {
        System.loadLibrary( "gpg-error" );
        System.loadLibrary( "gcrypt" );
        System.loadLibrary( "lifeocrypt" );

        initCipher();
    }

    static final char SC_DATE = 'd';
    static final char SC_SIZE_C = 's';
    static final char SC_CHANGE = 's';
    static final char SD_DESCENDING = 'd';
    static final char SD_ASCENDING = 'a';

    final static String DB_FILE_HEADER = "LIFEOGRAPHDB";
    final static int DB_FILE_VERSION_INT = 1050;
    final static int DB_FILE_VERSION_INT_MIN = 1010;
    static final String LOCK_SUFFIX = ".~LOCK~";

    static final String sExampleDiaryPath = "*/E/X/A/M/P/L/E/D/I/A/R/Y/*";
    static final String sExampleDiaryName = "*** Example Diary ***";

    final static int PASSPHRASE_MIN_SIZE = 4;

    public static Diary diary = null;

    enum SetPathType { NORMAL, READ_ONLY, NEW }

    public Diary() {
        super( null, DiaryElement.DEID_DIARY, ES_VOID );
        m_current_id = DiaryElement.DEID_FIRST;
        m_force_id = DiaryElement.DEID_UNSET;
    }

    Result init_new( String path ) {
        clear();

        m_read_version = DB_FILE_VERSION_INT;
        Result result = set_path( path, SetPathType.NEW );

        if( result != Result.SUCCESS ) {
            clear();
            return result;
        }

        // every diary must at least have one chapter category:
        m_ptr2chapter_ctg_cur = create_chapter_ctg( "Default" );

        add_today(); // must come after m_ptr2chapter_ctg_cur is set

        Theme theme = Diary.diary.create_tag( "calm", null ).get_own_theme();
        theme.font = "Sans 11";
        theme.color_base = Color.parseColor( "#D1E188" );
        theme.color_text = Color.parseColor( "#001100" );
        theme.color_heading =  Color.parseColor( "#D24242" );
        theme.color_subheading = Color.parseColor( "#793E4C" );
        theme.color_highlight = Color.parseColor( "#8CCB4D" );

        theme = Diary.diary.create_tag( "lovely", null ).get_own_theme();
        theme.font = "Sans 11";
        theme.color_base = Color.parseColor( "#F9F0F0" );
        theme.color_text = Color.parseColor( "#330909" );
        theme.color_heading =  Color.parseColor( "#E13333" );
        theme.color_subheading = Color.parseColor( "#AA4747" );
        theme.color_highlight = Color.parseColor( "#E69595" );

        theme = Diary.diary.create_tag( "dark", null ).get_own_theme();
        theme.font = "Sans 11";
        theme.color_base = Color.parseColor( "#111111" );
        theme.color_text = Color.parseColor( "#BBBBBB" );
        theme.color_heading =  Color.parseColor( "#3874AC" );
        theme.color_subheading = Color.parseColor( "#23538B" );
        theme.color_highlight = Color.parseColor( "#173353" );

        m_login_status = LoginStatus.LOGGED_IN_RO;

        return write();
    }

    public void clear() {
        close_file();

        m_path = "";
        m_read_version = 0;

        m_current_id = DiaryElement.DEID_FIRST;
        m_force_id = DiaryElement.DEID_UNSET;
        m_ids.clear();
        m_ids.put( DiaryElement.DEID_DIARY, this );

        m_entries.clear();
        m_tags.clear();
        m_tag_categories.clear();
        m_untagged.reset();

        m_ptr2chapter_ctg_cur = null;
        m_chapter_categories.clear();
        m_topics.mMap.clear();
        m_groups.mMap.clear();
        m_orphans.clear();
        m_orphans.set_date( Date.DATE_MAX );

        m_startup_elem_id = DiaryElement.HOME_CURRENT_ELEM;
        m_last_elem_id = DiaryElement.DEID_DIARY;
        m_chart_type = DEFAULT_CHART_TYPE;

        m_passphrase = "";

        m_search_text = "";
        m_filter_active.reset();
        m_filter_default.reset();

        // NOTE: only reset body options here:
        m_language = "";
        m_option_sorting_criteria = SC_DATE;
        m_option_sorting_dir = SD_DESCENDING;

        //m_flag_changed = false;
        m_flag_read_only = false;
        m_flag_ignore_locks = false;
        m_flag_skip_old_check = false;
        m_login_status =LoginStatus.LOGGED_OUT;

        // java specific:
        mBufferedReader = null;
    }

    @Override
    public DiaryElement.Type get_type() {
        return DiaryElement.Type.DIARY;
    }

    @Override
    public int get_size() {
        return m_entries.size();
    }

    @Override
    public int get_icon() {
        return R.mipmap.ic_diary;
    }

    @Override
    public String get_info_str() {
        return( get_size() + " entries" );
    }

    @Override
    public ChartPoints create_chart_data() {
        if( m_entries.isEmpty() )
            return null;

        ChartPoints cp = new ChartPoints( m_chart_type );
        Date d_last = new Date( Date.NOT_SET );

        for( Entry entry : m_entries.descendingMap().values() )
            cp.add_plain( d_last, entry.get_date() );

        // TODO: fill_up_chart_points( cp );

        return cp;
    }

    // ID HANDLING =================================================================================
    public int create_new_id( DiaryElement element ) {
        int retval;
        if( m_force_id == DiaryElement.DEID_UNSET ) {
            Random random = new Random();
            do {
                retval = 1000000 + random.nextInt( 9000000 );
            }
            while( m_ids.containsKey( retval ) );
        }
        else {
            retval = m_force_id;
            m_force_id = DiaryElement.DEID_UNSET;
        }
        m_ids.put( retval, element );

        return retval;
    }

    public boolean set_force_id( int id ) {
        if( m_ids.get( id ) != null || id <= DiaryElement.DEID_DIARY )
            return false;
        m_force_id = id;
        return true;
    }

    public void make_free_entry_order( Date date ) {
        date.reset_order_1();
        while( m_entries.get( date.m_date ) != null )
            date.m_date += 1;
    }

    public String create_unique_name_for_map( TreeMap map, String name0 ) {
        String name = name0;
        for( int i = 1; map.containsKey( name ); i++ ) {
            name = name0 + " " + i;
        }

        return name;
    }

    // MEMBER RELATED FUNCS ========================================================================
    public boolean set_passphrase( String passphrase ) {
        if( passphrase.length() >= PASSPHRASE_MIN_SIZE ) {
            m_passphrase = passphrase;
            return true;
        }
        else
            return false;
    }

    public boolean compare_passphrase( String passphrase ) {
        return m_passphrase.equals( passphrase );
    }

    public boolean is_old() {
        return( m_read_version < DB_FILE_VERSION_INT );
    }

    public boolean is_encrypted() {
        return( !m_passphrase.isEmpty() );
    }

    public boolean is_open() {
        return( m_login_status == LoginStatus.LOGGED_IN_RO ||
                m_login_status == LoginStatus.LOGGED_IN_EDIT );
    }

    public boolean is_in_edit_mode() {
        return( m_login_status == LoginStatus.LOGGED_IN_EDIT );
    }

    public boolean can_enter_edit_mode() {
        return( !m_flag_read_only );
    }

    // not part of c++
    public boolean is_virtual() {
        return m_path.equals( sExampleDiaryPath );
    }

    public String get_lang() {
        return m_language;
    }

//    public void set_lang( String lang ) {
//        m_language = lang;
//    }

    public DiaryElement get_element( int id ) {
        return m_ids.get( id );
    }

// NOT USED NOW
//    public DiaryElement get_startup_elem() {
//        return null;
//    }
//
//    public DiaryElement get_most_current_elem() {
//        return null;
//    }
//
//    public DiaryElement get_prev_session_elem() {
//        return null;
//    }

//    public char get_sorting_criteria() {
//        return m_option_sorting_criteria;
//    }

//    public void set_sorting_criteria( char sc ) {
//        m_option_sorting_criteria = sc;
//    }

    // FILTERING ===================================================================================
    public void set_search_text( String text ) {
        m_search_text = text;
        m_filter_active.set_status_outstanding();
    }

    public String get_search_text() {
        return m_search_text;
    }

    public boolean is_search_active() {
        return( !m_search_text.isEmpty() );
    }

    public Filter get_filter() {
        return m_filter_active;
    }

    // ENTRIES =====================================================================================
    public Entry get_entry( long date ) {
        Entry entry = m_entries.get( date );
        if( entry != null )
            if( !entry.get_filtered_out() )
                return entry;

        return null;
    }

    public Entry get_entry_today() {
        return m_entries.get( Date.get_today( 1 ) ); // 1 is the order
    }

    public Entry create_entry( Date dateObj, String content, boolean flag_favorite ) {
        // make it the last entry of its day:
        dateObj.reset_order_1();
        long date = dateObj.m_date;
        while( m_entries.get( date ) != null )
            ++date;

        Entry entry = new Entry( this, date, content, flag_favorite );

        m_entries.put( date, entry );

        add_entry_to_related_chapter( entry );

        m_untagged.add_entry( entry );

        return( entry );
    }

    // adds a new entry to today even if there is already one or more:
    public Entry add_today() {
        Date date = new Date( Date.get_today( 0 ) );
        return create_entry( date, "", false );
    }

    public void dismiss_entry( Entry entry ) {
        long date = entry.m_date.m_date;

        // fix startup element
        if( m_startup_elem_id == entry.get_id() )
            m_startup_elem_id = DiaryElement.DEID_DIARY;

        // remove from tags:
        for( Tag tag : entry.m_tags )
            tag.mEntries.remove( entry );

        // remove from untagged:
        m_untagged.remove_entry( entry );

        // remove from chapters:
        remove_entry_from_chapters( entry );

        // remove from filters:
        if( m_filter_active.is_entry_filtered( entry ) )
            m_filter_active.remove_entry( entry );
        if( m_filter_default.is_entry_filtered( entry ) )
            m_filter_default.remove_entry( entry );

        // erase entry from map:
        m_entries.remove( date );

        // fix entry order:
        int i = 1;
        for( Entry e = m_entries.get( date + i ); e != null; e = m_entries.get( date + i ) ) {
            m_entries.remove( e.m_date.m_date );
            e.m_date.m_date--;
            m_entries.put( e.m_date.m_date, e );
            ++i;
        }
    }

    public void set_entry_date( Entry entry, Date date ) {
        long d = entry.m_date.m_date;
        m_entries.remove( d );

        for( Entry entry_shift = m_entries.get( ++d );
             entry_shift != null;
             entry_shift = m_entries.get( ++d ) ) {
            m_entries.remove( d );
            entry_shift.set_date( d-1 );
            m_entries.put( d - 1, entry_shift );
        }

        // find the last entry in the date/order
        d = date.m_date;
        boolean flag_replace = false;
        while( m_entries.get( d ) != null ) {
            flag_replace = true;
            d++;
        }

        if( flag_replace ) {
            for( Entry entry_shift = m_entries.get( --d );
                 d >= date.m_date;
                 entry_shift = m_entries.get( --d ) ) {
                assert entry_shift != null;
                m_entries.remove( d );
                entry_shift.set_date( d + 1 );
                m_entries.put( d + 1, entry_shift );
            }
        }

        entry.set_date( date.m_date );
        m_entries.put( date.m_date, entry );
        update_entries_in_chapters(); // date changes require full update
    }

    public boolean get_day_has_multiple_entries( Date date_impure ) {
        long date = date_impure.get_pure();
        return( m_entries.get( date + 2 ) != null );
        // TODO:
        // if( iter == null || iter == m_entries.begin() )
        // return false;
        //
        // do
        // {
        // --iter;
        // if( iter->second->get_date().get_pure() == date )
        // {
        // if( iter->second->get_filtered_out() == false )
        // return true;
        // }
        // else
        // break;
        // }
        // while( iter != m_entries.begin() );
        //
        // return false;
    }

    // TAGS ========================================================================================
//    public java.util.Map< String, Tag > get_tags() {
//        return m_tags;
//    }

    public Untagged get_untagged()
    {
        return m_untagged;
    }

//    public Tag.Category create_tag_ctg() {
//        String name = create_unique_chapter_ctg_name( "New category" );
//        Tag.Category new_category = new Tag.Category( this, name );
//        m_tag_categories.put( name, new_category );
//
//        return new_category;
//    }

    public Tag.Category create_tag_ctg( String name ) {
        Tag.Category new_category = new Tag.Category( this, name );
        m_tag_categories.put( name, new_category );

        return new_category;
    }

    // in C++ this method is in PoolTagCategories class which does not exist in Java
    public boolean rename_tag_ctg( Tag.Category ctg, String new_name ) {
        m_tag_categories.remove( ctg.m_name );
        ctg.m_name = new_name;
        return( m_tag_categories.put( new_name, ctg ) != null );
    }

    public void dismiss_tag_ctg( Tag.Category ctg, boolean flag_dismiss_contained ) {
        // fix or dismiss contained tags
        // we work on copies to prevent ConcurrentModificationException
        if( flag_dismiss_contained ) {
            for( Tag tag : ctg.mTags.toArray( new Tag[ 0 ] ) )
                dismiss_tag( tag );
        }
        else {
            for( Tag tag : ctg.mTags.toArray( new Tag[ 0 ] ) )
                tag.set_category( null );
        }

        // remove from the list
        m_tag_categories.remove( ctg.m_name );
    }

    public Tag create_tag( String name, Tag.Category ctg, int chart_type ) {
        Tag tag = m_tags.get( name );
        if( tag != null ) {
            Log.e( Lifeograph.TAG, "Tag already exists: " + name );
            return( tag );
        }
        tag = new Tag( this, name, ctg, chart_type );
        m_tags.put( name, tag );
        return tag;
    }

    public Tag create_tag( String name, Tag.Category ctg ) {
        return create_tag( name, ctg, DEFAULT_CHART_TYPE );
    }

    public void dismiss_tag( Tag tag ) {
        // remove from entries: (work on a copy to prevent ConcurrentModificationException)
        for( Entry e : tag.mEntries.keySet().toArray( new Entry[ 0 ] ) )
            e.remove_tag( tag );

        // remove from category if any:
        if( tag.get_category() != null )
            tag.get_category().remove( tag );

        // clear filters if necessary:
        if( tag == m_filter_active.get_tag() )
            m_filter_active.set_tag( null );
        if( tag == m_filter_default.get_tag() )
            m_filter_default.set_tag( null );

        m_tags.remove( tag.get_name() );
    }

    public boolean rename_tag( Tag tag, String new_name ) {
        m_tags.remove( tag.m_name );
        tag.m_name = new_name;
        return( m_tags.put( new_name, tag ) == null );
    }

    // CHAPTERS ====================================================================================
    //public Chapter.Category get_current_chapter_ctg() { return m_ptr2chapter_ctg_cur; }
    public void set_current_chapter_ctg( Chapter.Category ctg ) {
        m_ptr2chapter_ctg_cur = ctg;
        update_entries_in_chapters();
    }

    //CategoryChapters create_chapter_ctg() {}
    public Chapter.Category create_chapter_ctg( String name ) {
        Chapter.Category category = new Chapter.Category( this, name );
        m_chapter_categories.put( name, category );
        return category;
    }

    public void dismiss_chapter_ctg( Chapter.Category ctg ) {
        if( m_chapter_categories.size() < 2 )
            return;

        if( ctg == m_ptr2chapter_ctg_cur )
            m_ptr2chapter_ctg_cur = m_chapter_categories.firstEntry().getValue();

        m_chapter_categories.remove( ctg.m_name );
    }

    public boolean rename_chapter_ctg( Chapter.Category ctg, String new_name ) {
        if( m_chapter_categories.containsKey( new_name ) )
            return false;

        m_chapter_categories.remove( ctg.m_name );
        ctg.m_name = new_name;
        m_chapter_categories.put( new_name, ctg );

        return true;
    }

    public void dismiss_chapter( Chapter chapter ) {
        // BEWARE: higher means the earlier and lower means the later here!
        if( chapter.is_ordinal() ) // topic or group
        {
            Chapter.Category ptr2ctg = ( chapter.get_date().is_hidden() ? m_groups : m_topics );

            if( !ptr2ctg.mMap.containsKey( chapter.m_date_begin.m_date ) )
            {
                Log.e( Lifeograph.TAG, "Chapter could not be found in assumed category" );
                return;
            }

            Chapter chapter_next = ( ptr2ctg.mMap.lowerKey( chapter.m_date_begin.m_date ) != null ?
                    ptr2ctg.mMap.lowerEntry( chapter.m_date_begin.m_date ).getValue() : null );

            final boolean flag_erasing_oldest_cpt = (
                    chapter_next == null &&
                    ptr2ctg.mMap.higherKey( chapter.m_date_begin.m_date ) != null );

            // CALCULATE THE LAST ENTRY DATE
            long last_entry_date;
            // last entry date is taken from previous chapter's last entry when
            // the chapter to be deleted is the last chapter
            if( flag_erasing_oldest_cpt )
            {
                Chapter chapter_p = ptr2ctg.mMap.higherEntry( chapter.m_date_begin.m_date )
                                               .getValue();

                // use the chapters date if it does not contain any entry
                last_entry_date = chapter_p.mEntries.isEmpty() ? chapter_p.get_date_t()
                        : chapter_p.mEntries.first().get_date_t();
            }
            else
            {
                last_entry_date = chapter.mEntries.isEmpty() ? chapter.get_date_t()
                        : chapter.mEntries.first().get_date_t();
            }

            // SHIFT ENTRY DATES
            boolean flag_first = true;
            boolean flag_dismiss_contained = false; // TODO WILL BE ADDED IN 0.5+
            for( Chapter chpt = chapter; chpt != null; )
            {
                boolean flag_neighbor = ( chpt.get_date_t() == chapter.get_date_t()
                    + Date.ORDINAL_STEP  );

                for( Entry entry : chpt.mEntries )
                {
                    if( flag_first && flag_dismiss_contained )
                        dismiss_entry( entry );
                    else if( !flag_first || flag_erasing_oldest_cpt )
                    {
                        m_entries.remove( entry.get_date_t() );
                        if( !flag_dismiss_contained && ( flag_neighbor || flag_erasing_oldest_cpt ) )
                            entry.set_date( entry.get_date().get_order() + last_entry_date );
                        else
                            entry.set_date( entry.get_date_t() - Date.ORDINAL_STEP );
                        m_entries.put( entry.get_date_t(), entry );
                    }
                }
                flag_first = false;

                if( ptr2ctg.mMap.lowerKey( chpt.m_date_begin.m_date ) != null )
                    chpt = ptr2ctg.mMap.lowerEntry( chpt.m_date_begin.m_date ).getValue();
                else
                    chpt = null;
            }

            // REMOVE THE ACTUAL CHAPTER
            ptr2ctg.mMap.remove( chapter.get_date_t() );

            // SHIFT OTHER CHAPTERS
            for( Chapter chpt = chapter_next; chpt != null; ) {
                ptr2ctg.mMap.remove( chpt.get_date_t() );
                chpt.set_date( chpt.get_date_t() - Date.ORDINAL_STEP );
                ptr2ctg.mMap.put( chpt.get_date_t(), chpt );

                if( ptr2ctg.mMap.lowerKey( chpt.m_date_begin.m_date ) != null )
                    chpt = ptr2ctg.mMap.lowerEntry( chpt.m_date_begin.m_date ).getValue();
                else
                    chpt = null;
            }
        }
        else // TEMPORAL CHAPTER
        {
            if( !m_ptr2chapter_ctg_cur.mMap.containsKey( chapter.m_date_begin.m_date ) )
            {
                Log.e( Lifeograph.TAG, "Chapter could not be found in assumed category" );
                return;
            }
            // fix time span
            else if( m_ptr2chapter_ctg_cur.mMap.higherKey( chapter.m_date_begin.m_date ) != null )
            {
                Chapter chapter_earlier =
                        m_ptr2chapter_ctg_cur.mMap.higherEntry( chapter.m_date_begin.m_date )
                                                  .getValue();
                if( chapter.m_time_span > 0 )
                    chapter_earlier.m_time_span += chapter.m_time_span;
                else
                    chapter_earlier.m_time_span = 0;
            }

//            if( flag_dismiss_contained )
//            {
//                for( Entry entry : chapter.mEntries )
//                    dismiss_entry( entry );
//            }

            m_ptr2chapter_ctg_cur.mMap.remove( chapter.get_date_t() );
        }

        update_entries_in_chapters();
    }

    // Date get_free_chapter_order_temporal();

    // Date get_free_chapter_order_ordinal();

    // boolean make_free_entry_order( Date date );

    // FUNCTIONS TO GET NEAREST CHAPTERS / TOPICS
//    public Chapter getPrevChapter( Chapter chapter ) {
//        if( chapter.is_ordinal() ) {
//            long d_chapter_prev = chapter.m_date_begin.m_date - Date.ORDINAL_STEP;
//            return m_topics.mMap.get( d_chapter_prev );
//        }
//        else
//            return m_ptr2chapter_ctg_cur.getChapterEarlier( chapter );
//    }

//    public Chapter getNextChapter( Chapter chapter ) {
//        if( chapter.is_ordinal() ) {
//            long d_chapter_next = chapter.m_date_begin.m_date + Date.ORDINAL_STEP;
//            return m_topics.mMap.get( d_chapter_next );
//        }
//        else
//            return m_ptr2chapter_ctg_cur.getChapterLater( chapter );
//    }

    public void update_entries_in_chapters() {
        Log.d( Lifeograph.TAG, "update_entries_in_chapters()" );

        Chapter.Category[] chapters = new Chapter.Category[] { m_topics, m_groups,
                                                               m_ptr2chapter_ctg_cur };
        long date_last = m_entries.isEmpty() ? 0 : m_entries.firstEntry().getKey();
        boolean entries_finished = false;

        for( int i = 0; i < 3; i++ ) {
            for( Chapter chapter : chapters[ i ].getMap().values() ) {
                chapter.clear();

                if( entries_finished )
                    continue;

                entries_finished = true;
                for( Entry entry : m_entries.tailMap( date_last ).values() ) {
                    date_last = entry.get_date_t();

                    if( entry.get_date_t() > chapter.get_date_t() )
                        chapter.insert( entry );
                    else {
                        entries_finished = false;
                        break;
                    }
                }
            }
        }

        m_orphans.clear();

        if( !entries_finished ) {
            for( Entry entry : m_entries.tailMap( date_last ).values() ) {
                m_orphans.insert( entry );
                m_orphans.set_date( entry.get_date_t() );
            }
        }
        else
            m_orphans.set_date( Date.DATE_MAX );
    }

    public void add_entry_to_related_chapter( Entry entry ) {
        // NOTE: works as per the current listing options needs to be updated when something
        // changes the arrangement such as a change in the current chapter category

        Chapter.Category ptr2ctg;

        if( entry.m_date.is_ordinal() ) // in groups or topics
            ptr2ctg = ( entry.m_date.is_hidden() ? m_groups : m_topics );
        else // in chapters
            ptr2ctg = m_ptr2chapter_ctg_cur;

        for( Chapter chapter : ptr2ctg.getMap().values() )
        {
            if( entry.get_date_t() > chapter.get_date_t() )
            {
                chapter.insert( entry );
                return;
            }
        }

        // if does not belong to any of the defined chapters:
        m_orphans.insert( entry );
        if( entry.m_date.m_date < m_orphans.get_date().m_date )
            m_orphans.set_date( entry.m_date.m_date );
    }

    public void remove_entry_from_chapters( Entry entry ) {
        Chapter.Category ptr2ctg;

        if( entry.m_date.is_ordinal() ) // in groups or topics
            ptr2ctg = ( entry.m_date.is_hidden() ? m_groups : m_topics );
        else // in chapters
            ptr2ctg = m_ptr2chapter_ctg_cur;

        for( Chapter chapter : ptr2ctg.getMap().values() ) {
            if( chapter.find( entry ) ) {
                chapter.erase( entry );
                return;
            }
        }

        // if does not belong to any of the defined chapters:
        m_orphans.erase( entry );
    }

    // DB PARSING HELPER FUNCTIONS =================================================================
    private long get_db_line_date( String line ) {
        long date = 0;

        for( int i = 2; i < line.length() && i < 12 && line.charAt( i ) >= '0'
                        && line.charAt( i ) <= '9'; i++ ) {
            date = ( date * 10 ) + line.charAt( i ) - '0';
        }

        return date;
    }

    private String get_db_line_name( String line ) {
        int begin = line.indexOf( '\t' );
        if( begin == -1 )
            begin = 2;
        else
            begin++;

        return( line.substring( begin ) );
    }

    private long fix_pre_1020_date( long d ) {
        if( Date.is_ordinal( d ) ) {
            if( ( d & Date.VISIBLE_FLAG ) != 0 )
                d -= Date.VISIBLE_FLAG;
            else
                d |= Date.VISIBLE_FLAG;
        }

        return d;
    }

    private void do_standard_checks_after_parse() {
        // every diary must at least have one chapter category:
        if( m_chapter_categories.size() < 1 )
            m_ptr2chapter_ctg_cur = create_chapter_ctg( "default" ); // TODO: i18n

        if( m_startup_elem_id > DiaryElement.DEID_DIARY )
            if( get_element( m_startup_elem_id ) == null )
            {
                Log.w( Lifeograph.TAG, "Startup element cannot be found in db" );
                m_startup_elem_id = DiaryElement.DEID_DIARY;
            }

        if( m_entries.size() < 1 ) {
            add_today();
            Log.i( Lifeograph.TAG, "A dummy entry added to the diary" );
        }
    }

    private void parse_todo_status( DiaryElement elem, char c ) {
        switch( c ) {
            case 't':
                elem.set_todo_status( ES_TODO );
                break;
            case 'T':
                elem.set_todo_status( ES_NOT_TODO | ES_TODO );
                break;
            case 'p':
                elem.set_todo_status( ES_PROGRESSED );
                break;
            case 'P':
                elem.set_todo_status( ES_NOT_TODO | ES_PROGRESSED );
                break;
            case 'd':
                elem.set_todo_status( ES_DONE );
                break;
            case 'D':
                elem.set_todo_status( ES_NOT_TODO | ES_DONE );
                break;
            case 'c':
                elem.set_todo_status( ES_CANCELED );
                break;
            case 'C':
                elem.set_todo_status( ES_NOT_TODO | ES_CANCELED );
                break;
        }
    }

    // DB PARSING MAIN FUNCTIONS ===================================================================
    private Result parse_db_body_text() {
        switch( m_read_version )
        {
            case 1050:
            case 1040:
            case 1030:
            case 1020:
                return parse_db_body_text_1050();
            default: // (1011 & 1010):
                return parse_db_body_text_1010();
        }
    }

    private Result parse_db_body_text_1050() {
        String line;
        Entry entry_new = null;
        Chapter.Category ptr2chapter_ctg = null;
        Chapter ptr2chapter = null;
        Tag.Category ptr2tag_ctg = null;
        Tag ptr2tag = null;
        boolean flag_first_paragraph = false;

        try
        {
            // TAG DEFINITIONS & CHAPTERS
            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 1 ) // end of section
                    break;
                else if( line.length() >= 3 ) {
                    switch( line.charAt( 0 ) ) {
                        case 'I':
                            set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                            break;
                        case 'T': // tag category
                            ptr2tag_ctg = create_tag_ctg( line.substring( 2 ) );
                            ptr2tag_ctg.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 't': // tag
                            switch( line.charAt( 1 ) ) {
                                case ' ':
                                    ptr2tag = create_tag( line.substring( 2 ), ptr2tag_ctg );
                                    break;
                                case 'c':
                                    if( ptr2tag != null )
                                        ptr2tag.set_chart_type(
                                                Integer.parseInt( line.substring( 2 ) ) );
                                    break;
                                case 'u':
                                    if( ptr2tag != null )
                                        ptr2tag.set_unit( line.substring( 2 ) );
                                    break;
                            }
                            break;
                        case 'u': // untagged
                            ptr2tag = m_untagged;
                            if( line.charAt( 1 ) == 'c' ) {
                                ptr2tag.set_chart_type( Integer.parseInt( line.substring( 2 ) ) );
                                break;
                            }
                            // no break
                        case 'm':
                            if( ptr2tag == null ) {
                                Log.e( Lifeograph.TAG, "No tag declared for theme" );
                                break;
                            }
                            switch( line.charAt( 1 ) ) {
                                case 'f': // font
                                    ptr2tag.get_own_theme().font = line.substring( 2 );
                                    break;
                                case 'b': // base color
                                    if( line.charAt( 2 ) != 'n' )
                                        ptr2tag.get_own_theme().color_base =
                                                Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 't': // text color
                                    if( line.charAt( 2 ) != 'n' )
                                        ptr2tag.get_own_theme().color_text =
                                                Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 'h': // heading color
                                    if( line.charAt( 2 ) != 'n' )
                                        ptr2tag.get_own_theme().color_heading =
                                                Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 's': // subheading color
                                    if( line.charAt( 2 ) != 'n' )
                                        ptr2tag.get_own_theme().color_subheading =
                                                Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 'l': // highlight color
                                    // a work-around against a bug in 0.3
                                    if( line.charAt( 2 ) == 'n' )
                                        ptr2tag.create_own_theme_duplicating( Theme.System.get() );
                                    else
                                        ptr2tag.get_own_theme().color_highlight =
                                                Theme.parse_color( line.substring( 2 ) );
                                    break;
                            }
                            break;
                        case 'f':
                            switch( line.charAt( 1 ) ) {
                                case 's':   // status
                                    if( line.length() < 11 ) {
                                        Log.e( Lifeograph.TAG, "Status filter length error" );
                                        continue;
                                    }
                                    m_filter_default.set_trash( line.charAt( 2 ) == 'T',
                                            line.charAt( 3 ) == 't' );
                                    m_filter_default.set_favorites( line.charAt( 4 ) == 'F',
                                            line.charAt( 5 ) == 'f' );
                                    m_filter_default.set_todo( line.charAt( 6 )  == 'N',
                                                               line.charAt( 7 )  == 'T',
                                                               line.charAt( 8 )  == 'P',
                                                               line.charAt( 9 )  == 'D',
                                                               line.charAt( 10 ) == 'C' );
                                    break;
                                case 't':   // tag
                                {
                                    Tag tag = m_tags.get( line.substring( 2 ) );
                                    if( tag != null )
                                        m_filter_default.set_tag( tag );
                                    else
                                        Log.e( Lifeograph.TAG, "Reference to undefined tag: "
                                                + line.substring( 2 ) );
                                    break;
                                }
                                case 'b':   // begin date
                                    m_filter_default.set_date_begin(
                                            Long.parseLong( line.substring( 2 ) ) );
                                    break;
                                case 'e':   // end date
                                    m_filter_default.set_date_end(
                                            Long.parseLong( line.substring( 2 ) ) );
                                    break;
                            }
                            break;
                        case 'C': // chapters...
                            switch( line.charAt( 1 ) ) {
                                case 'C':   // chapter category
                                    ptr2chapter_ctg = create_chapter_ctg( line.substring( 3 ) );
                                    if( line.charAt( 2 ) == 'c' )
                                        m_ptr2chapter_ctg_cur = ptr2chapter_ctg;
                                    break;
                                case 'c':   // chapter color
                                    if( ptr2chapter == null ) {
                                        Log.e( Lifeograph.TAG, "No chapter defined" );
                                        break;
                                    }
                                    ptr2chapter.set_color( Theme.parse_color( line.substring( 2 ) ) );
                                    break;
                                case 'T':   // temporal chapter
                                    if( ptr2chapter_ctg == null ) {
                                        Log.e( Lifeograph.TAG, "No chapter category defined" );
                                        break;
                                    }
                                    ptr2chapter =
                                            ptr2chapter_ctg.create_chapter( get_db_line_name( line ),
                                                    get_db_line_date( line ) );
                                    break;
                                case 'O':   // ordinal chapter (used to be called topic)
                                    ptr2chapter =
                                            m_topics.create_chapter( get_db_line_name( line ),
                                                    get_db_line_date( line ) );
                                    break;
                                case 'G':   // free chapter
                                    ptr2chapter = m_groups.create_chapter(
                                            get_db_line_name( line ), get_db_line_date( line ) );
                                    break;
                                case 'p':   // chapter preferences
                                    ptr2chapter.set_expanded( line.charAt( 2 ) == 'e' );
                                    parse_todo_status( ptr2chapter, line.charAt( 3 ) );
                                    break;
                            }
                            break;
                        case 'O': // options
                            m_option_sorting_criteria = line.charAt( 2 );
                            if( m_read_version >= 1050 ) {
                                m_option_sorting_dir = line.charAt( 3 );
                                if( line.length() > 4 && line.charAt( 4 ) == 'Y' )
                                    set_chart_type( ChartPoints.YEARLY );
                            }
                            else {
                                if( line.length() > 3 && line.charAt( 3 ) == 'Y' )
                                    set_chart_type( ChartPoints.YEARLY );
                            }
                            break;
                        case 'l': // language
                            m_language = line.substring( 2 );
                            break;
                        case 'S': // startup action
                            m_startup_elem_id = Integer.parseInt( line.substring( 2 ) );
                            break;
                        case 'L':
                            m_last_elem_id = Integer.parseInt( line.substring( 2 ) );
                            break;
                        default:
                            Log.e( Lifeograph.TAG, "Unrecognized line:\n" + line );
                            clear();
                            return Result.CORRUPT_FILE;
                    }
                }
            }

            // ENTRIES
            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 2 )
                    continue;
                else if( line.charAt( 0 ) != 'I' && line.charAt( 0 ) != 'E' &&
                         line.charAt( 0 ) != 'e' && entry_new == null ) {
                    Log.e( Lifeograph.TAG, "No entry declared for the attribute" );
                    continue;
                }

                switch( line.charAt( 0 ) ) {
                    case 'I':
                        set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                        break;
                    case 'E':   // new entry
                    case 'e':   // trashed
                        if( line.length() < 5 )
                            continue;

                        long date = Long.parseLong( line.substring( 4 ) );
                        entry_new = new Entry( this, date, line.charAt( 1 ) == 'f' );
                        m_entries.put( date, entry_new );
                        add_entry_to_related_chapter( entry_new );
                        m_untagged.add_entry( entry_new );

                        if( line.charAt( 0 ) == 'e' )
                            entry_new.set_trashed( true );
                        if( line.charAt( 2 ) == 'h' )
                            m_filter_default.add_entry( entry_new );

                        parse_todo_status( entry_new, line.charAt( 3 ) );

                        flag_first_paragraph = true;
                        break;
                    case 'D':   // creation & change dates (optional)
                        switch( line.charAt( 1 ) ) {
                            case 'r':
                                entry_new.m_date_created = Long.parseLong( line.substring( 2 ) );
                                break;
                            case 'h':
                                entry_new.m_date_edited = Long.parseLong( line.substring( 2 ) );
                                break;
                            case 's':
                                entry_new.m_date_status = Long.parseLong( line.substring( 2 ) );
                                break;
                        }
                        break;
                    case 'T':   // tag
                        NameAndValue nav = NameAndValue.parse( line.substring( 2 ) );
                        Tag tag = m_tags.get( nav.name );
                        if( tag != null ) {
                            entry_new.add_tag( tag, nav.value );
                            if( line.charAt( 1 ) == 'T' )
                                entry_new.set_theme_tag( tag );
                        }
                        else
                            Log.e( Lifeograph.TAG, "Reference to undefined tag: " + nav.name );
                        break;
                    case 'L':   // location
                        entry_new.m_location = line.substring( 2 );
                        break;
                    case 'l':   // language
                        entry_new.set_lang( line.substring( 2 ) );
                        break;
                    case 'P':    // paragraph
                        if( flag_first_paragraph ) {
                            if( line.length() > 2 )
                                entry_new.m_text = line.substring( 2 );
                            entry_new.m_name = entry_new.m_text;
                            flag_first_paragraph = false;
                        }
                        else {
                            entry_new.m_text += "\n";
                            entry_new.m_text += line.substring( 2 );
                        }
                        break;
                    default:
                        Log.e( Lifeograph.TAG, "Unrecognized line:\n" + line );
                        clear();
                        return Result.CORRUPT_FILE;
                }
            }
        }
        catch( IOException e )
        {
            return Result.CORRUPT_FILE;
        }

        do_standard_checks_after_parse();

        m_filter_active.set( m_filter_default );

        if( m_read_version == 1020 )
            upgrade_entries_to_1030();

        if( m_read_version < 1050 )
            upgrade_entries_to_1050();

        return Result.SUCCESS;
    }

    private Result parse_db_body_text_1010() {
        String line;
        Entry entry_new = null;
        Chapter.Category ptr2chapter_ctg = null;
        Chapter ptr2chapter = null;
        Tag.Category ptr2tag_ctg = null;
        Tag ptr2tag = null;
        boolean flag_first_paragraph = false;

        try {
            // TAG DEFINITIONS & CHAPTERS
            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 1 ) // end of section
                    break;
                else if( line.length() >= 3 ) {
                    switch( line.charAt( 0 ) ) {
                        case 'I':
                            set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                            break;
                        case 'T': // tag category
                            ptr2tag_ctg = create_tag_ctg( line.substring( 2 ) );
                            ptr2tag_ctg.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 't': // tag
                            ptr2tag = create_tag( line.substring( 2 ), ptr2tag_ctg );
                            break;
                        case 'u': // untagged
                            ptr2tag = m_untagged;
                            // no break
                        case 'm':
                            if( ptr2tag == null ) {
                                Log.e( Lifeograph.TAG, "No tag declared for theme" );
                                break;
                            }
                            switch( line.charAt( 1 ) ) {
                                case 'f': // font
                                    ptr2tag.get_own_theme().font = line.substring( 2 );
                                    break;
                                case 'b': // base color
                                    ptr2tag.get_own_theme().color_base =
                                            Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 't': // text color
                                    ptr2tag.get_own_theme().color_text =
                                            Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 'h': // heading color
                                    ptr2tag.get_own_theme().color_heading =
                                            Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 's': // subheading color
                                    ptr2tag.get_own_theme().color_subheading =
                                            Theme.parse_color( line.substring( 2 ) );
                                    break;
                                case 'l': // highlight color
                                    ptr2tag.get_own_theme().color_highlight =
                                            Theme.parse_color( line.substring( 2 ) );
                                    break;
                            }
                            break;
                        case 'f':
                            switch( line.charAt( 1 ) ) {
                                case 's':   // status
                                    if( line.length() < 9 ) {
                                        Log.e( Lifeograph.TAG, "Status filter length error" );
                                        continue;
                                    }
                                    m_filter_default.set_trash( line.charAt( 2 ) == 'T',
                                                                line.charAt( 3 ) == 't' );
                                    m_filter_default.set_favorites( line.charAt( 4 ) == 'F',
                                                                    line.charAt( 5 ) == 'f' );
                                    // made in-progress entries depend on the preference for open ones
                                    m_filter_default.set_todo( true,
                                                               line.charAt( 6 ) == 'T',
                                                               line.charAt( 6 ) == 'T',
                                                               line.charAt( 7 ) == 'D',
                                                               line.charAt( 8 ) == 'C' );
                                    break;
                                case 't':   // tag
                                {
                                    Tag tag = m_tags.get( line.substring( 2 ) );
                                    if( tag != null )
                                        m_filter_default.set_tag( tag );
                                    else
                                        Log.e( Lifeograph.TAG, "Reference to undefined tag: "
                                                +line.substring( 2 ) );
                                    break;
                                }
                                case 'b':   // begin date
                                    m_filter_default.set_date_begin(
                                            Long.parseLong( line.substring( 2 ) ) );
                                    break;
                                case 'e':   // end date
                                    m_filter_default.set_date_end(
                                            Long.parseLong( line.substring( 2 ) ) );
                                    break;
                            }
                            break;
                        case 'o':   // ordinal chapter (topic)
                            ptr2chapter =
                                    m_topics.create_chapter( get_db_line_name( line ),
                                                             fix_pre_1020_date( get_db_line_date( line ) ) );
                            ptr2chapter.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 'd':   // to-do group
                            if( line.charAt( 1 ) == ':' ) // declaration
                            {
                                ptr2chapter = m_groups.create_chapter(
                                        get_db_line_name( line ),
                                        fix_pre_1020_date( get_db_line_date( line ) ) );
                            }
                            else // options
                            {
                                ptr2chapter.set_expanded( line.charAt( 2 ) == 'e' );
                                if( line.charAt( 3 ) == 'd' )
                                    ptr2chapter.set_todo_status( ES_DONE );
                                else if( line.charAt( 3 ) == 'c' )
                                    ptr2chapter.set_todo_status( ES_CANCELED );
                                else
                                    ptr2chapter.set_todo_status( ES_TODO );
                            }
                            break;
                        case 'C':   // chapter category
                            ptr2chapter_ctg = create_chapter_ctg( line.substring( 2 ) );
                            if( line.charAt( 1 ) == 'c' )
                                m_ptr2chapter_ctg_cur = ptr2chapter_ctg;
                            break;
                        case 'c':   // chapter
                            if( ptr2chapter_ctg == null ) {
                                Log.e( Lifeograph.TAG, "No chapter category defined" );
                                break;
                            }
                            ptr2chapter = ptr2chapter_ctg.create_chapter(
                                    get_db_line_name( line ),
                                    fix_pre_1020_date( get_db_line_date( line ) ) );
                            ptr2chapter.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 'O':   // options
                            m_option_sorting_criteria = line.charAt( 2 );
                            break;
                        case 'l':   // language
                            m_language = line.substring( 2 );
                            break;
                        case 'S':   // startup action
                            m_startup_elem_id = Integer.parseInt( line.substring( 2 ) );
                            break;
                        case 'L':
                            m_last_elem_id = Integer.parseInt( line.substring( 2 ) );
                            break;
                        default:
                            Log.e( Lifeograph.TAG, "Unrecognized line:\n"+line );
                            clear();
                            return Result.CORRUPT_FILE;
                    }
                }
            }

            // ENTRIES
            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 2 )
                    continue;

                switch( line.charAt( 0 ) ) {
                    case 'I':
                        set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                        break;
                    case 'E':   // new entry
                    case 'e':   // trashed
                        if( line.length() < 5 )
                            continue;

                        long date = fix_pre_1020_date( Long.parseLong( line.substring( 4 ) ) );
                        entry_new = new Entry( this,  date, line.charAt( 1 ) == 'f' );
                        m_entries.put( date, entry_new );
                        add_entry_to_related_chapter( entry_new );
                        m_untagged.add_entry( entry_new );

                        if( line.charAt( 0 ) == 'e' )
                            entry_new.set_trashed( true );
                        if( line.charAt( 2 ) == 'h' )
                            m_filter_default.add_entry( entry_new );
                        if( line.charAt( 3 ) == 'd' )
                            entry_new.set_todo_status( ES_DONE );
                        else if( line.charAt( 3 ) == 'c' )
                            entry_new.set_todo_status( ES_CANCELED );
                            // hidden flag used to denote to do items:
                        else if( entry_new.get_date().is_hidden() )
                            entry_new.set_todo_status( ES_TODO );

                        flag_first_paragraph = true;
                        break;

                    case 'D':   // creation & change dates (optional)
                        if( entry_new == null ) {
                            Log.e( Lifeograph.TAG, "No entry declared" );
                            break;
                        }
                        if( line.charAt( 1 ) == 'r' )
                            entry_new.m_date_created = Long.parseLong( line.substring( 2 ) );
                        else    // it should be 'h'
                            entry_new.m_date_edited = Long.parseLong( line.substring( 2 ) );
                        break;
                    case 'T':   // tag
                        if( entry_new == null )
                            Log.e( Lifeograph.TAG, "No entry declared" );
                        else {
                            Tag tag = m_tags.get( line.substring( 2 ) );
                            if( tag != null ) {
                                entry_new.add_tag( tag );
                                if( line.charAt( 1 ) == 'T' )
                                    entry_new.set_theme_tag( tag );
                            }
                            else
                                Log.e( Lifeograph.TAG, "Reference to undefined tag: "+
                                        line.substring( 2 ) );
                        }
                        break;
                    case 'l':   // language
                        if( entry_new == null )
                            Log.e( Lifeograph.TAG, "No entry declared" );
                        else
                            entry_new.set_lang( line.substring( 2 ) );
                        break;
                    case 'P':    // paragraph
                        if( entry_new == null ) {
                            Log.e( Lifeograph.TAG, "No entry declared" );
                            break;
                        }
                        if( flag_first_paragraph ) {
                            if( line.length() > 2 )
                                entry_new.m_text = line.substring( 2 );
                            entry_new.m_name = entry_new.m_text;
                            flag_first_paragraph = false;
                        }
                        else {
                            entry_new.m_text += "\n";
                            entry_new.m_text += line.substring( 2 );
                        }
                        break;
                    default:
                        Log.e( Lifeograph.TAG, "Unrecognized line:\n" + line );
                        clear();
                        return Result.CORRUPT_FILE;
                }
            }
        }
        catch( IOException e )
        {
            return Result.CORRUPT_FILE;
        }

        do_standard_checks_after_parse();

        m_filter_active.set( m_filter_default );   // employ the default filter

        upgrade_entries_to_1030();
        upgrade_entries_to_1050();

        return Result.SUCCESS;
    }

    private void upgrade_entries_to_1030() {
        for( Entry entry : m_entries.values() ) {
            entry.m_date_status = entry.m_date_created; // initialize the status date

            char c;
            char lf = 'n';
            String check = "";
            StringBuilder new_content  = new StringBuilder();

            for( int i = 0; i < entry.m_text.length(); i++ ) {
                switch( c = entry.m_text.charAt( i ) ) {
                    case '\n':
                    case '\r':
                        new_content.append( '\n' );
                    case 0:     // should never be the case
                        lf = 't'; // tab
                        break;
                    case '\t':
                        new_content.append( c );
                        lf = ( lf == 't' || lf == 'c' ) ? 'c' : 'n';
                        break;
                    case '☐':
                        check = "[ ] ";
                        new_content.append( c );
                        lf = lf == 'c' ? 's' : 'n';
                        break;
                    case '☑':
                        check = "[+] ";
                        new_content.append( c );
                        lf = lf == 'c' ? 's' : 'n';
                        break;
                    case '☒':
                        check = "[x] ";
                        new_content.append( c );
                        lf = lf == 'c' ? 's' : 'n';
                        break;
                    case ' ':
                        if( lf == 's' ) {
                            new_content.deleteCharAt( new_content.length() - 1 );
                            new_content.append( check );
                        }
                        else
                            new_content.append( c );
                        lf = 'n';
                        break;
                    default:
                        new_content.append( c );
                        lf = 'n';
                        break;
                }
            }

            entry.m_text = new_content.toString();
        }
    }

    private void upgrade_entries_to_1050() {
        for( Entry entry : m_entries.values() ) {
            if( entry.get_todo_status() == ES_NOT_TODO )
                entry.set_todo_status( entry.calculate_todo_status_internal() );
        }
    }

    // DB CREATING HELPER FUNCTIONS ================================================================
    private void create_db_todo_status_text( DiaryElement elem ) throws IOException {
        switch( elem.get_todo_status() ) {
            case ES_TODO:
                mBufferedWriter.append( 't' );
                break;
            case ES_NOT_TODO | ES_TODO:
                mBufferedWriter.append( 'T' );
                break;
            case ES_PROGRESSED:
                mBufferedWriter.append( 'p' );
                break;
            case ES_NOT_TODO | ES_PROGRESSED:
                mBufferedWriter.append( 'P' );
                break;
            case ES_DONE:
                mBufferedWriter.append( 'd' );
                break;
            case ES_NOT_TODO | ES_DONE:
                mBufferedWriter.append( 'D' );
                break;
            case ES_CANCELED:
                mBufferedWriter.append( 'c' );
                break;
            case ES_NOT_TODO | ES_CANCELED:
                mBufferedWriter.append( 'C' );
                break;
            case ES_NOT_TODO:
            default:
                mBufferedWriter.append( 'n' );
                break;
        }
    }

    private void create_db_tag_text( Tag tag ) throws IOException {
        char type = 'm';
        if( tag.get_type() == Type.UNTAGGED ) {
            mBufferedWriter.append( "uc" );
            type = 'u';
        }
        else
            mBufferedWriter.append( "ID" )
                           .append( Integer.toString( tag.get_id() ) )
                           .append( "\nt " )
                           .append( tag.get_name() )
                           .append( "\ntc" );
        mBufferedWriter.append( Integer.toString( tag.get_chart_type() ) ).append( '\n' );

        if( ! tag.is_boolean() && ! tag.get_unit().isEmpty() )
            mBufferedWriter.append( "tu" ).append( tag.get_unit() ).append( '\n' );

        if( tag.get_has_own_theme() )
        {
            Theme theme = tag.get_theme();

            mBufferedWriter.append( type ).append( "f" ).append( theme.font ).append( '\n' );
            mBufferedWriter.append( type ).append( "b" )
                           .append( Theme.color2string( theme.color_base ) ).append( '\n' )
                           .append( type ).append( "t" )
                           .append( Theme.color2string( theme.color_text ) ).append( '\n' )
                           .append( type ).append( "h" )
                           .append( Theme.color2string( theme.color_heading ) ).append( '\n' )
                           .append( type ).append( "s" )
                           .append( Theme.color2string( theme.color_subheading ) ).append( '\n' )
                           .append( type ).append( "l" )
                           .append( Theme.color2string( theme.color_highlight ) ).append( '\n' );
        }
    }

    private void create_db_chapterctg_text( char type, Chapter.Category ctg ) throws IOException {
        for( Chapter chapter : ctg.mMap.values() ) {
            mBufferedWriter.append( "ID" ).append( Integer.toString( chapter.get_id() ) )
                           .append( "\nC" ).append( type )
                           .append( Long.toString( chapter.m_date_begin.m_date ) )
                           .append( '\t' ).append( chapter.get_name() )
                           .append( "\nCp" ).append( chapter.get_expanded() ? 'e' : '_' );
            create_db_todo_status_text( chapter );
            if( ( chapter.get_chart_type() & ChartPoints.YEARLY ) != 0 )
                mBufferedWriter.append( "Y\n" );
            else
                mBufferedWriter.append( '\n' );

            if( chapter.get_color() != Color.WHITE )
                mBufferedWriter.append( "Cc" )
                               .append( Theme.color2string( chapter.get_color() ) )
                               .append( '\n' );
        }
    }

    // DB CREATING MAIN FUNCTIONS ==================================================================
    private void create_db_header_text( boolean encrypted ) throws IOException {
        mBufferedWriter.write( DB_FILE_HEADER );
        mBufferedWriter.append( "\nV "+DB_FILE_VERSION_INT )
                       .append( encrypted ? "\nE yes" : "\nE no" )
                       .append( "\n\n" ); // end of header
    }

    private void create_db_body_text() throws IOException {
        // OPTIONS
        mBufferedWriter.append( "O " )
                       .append( m_option_sorting_criteria )
                       .append( m_option_sorting_dir )
                       .append( ( m_chart_type & ChartPoints.YEARLY ) != 0 ? "Y\n" : "M\n" );
        if( ! m_language.isEmpty() )
            mBufferedWriter.append( "l " ).append( m_language ).append( '\n' );

        // STARTUP ACTION (HOME ITEM)
        mBufferedWriter.append( "S " ).append( Integer.toString( m_startup_elem_id ) ).append( '\n' );
        mBufferedWriter.append( "L " ).append( Integer.toString( m_last_elem_id ) ).append( '\n' );

        // ROOT TAGS
        for( Tag tag : m_tags.values() ) {
            if( tag.get_category() == null )
                create_db_tag_text( tag );
        }
        // CATEGORIZED TAGS
        for( Tag.Category ctg : m_tag_categories.values() ) {
            // tag category:
            mBufferedWriter.append( "ID" ).append( Integer.toString( ctg.get_id() ) )
                           .append( "\nT" ).append( ctg.get_expanded() ? 'e' : '_' )
                           .append( ctg.get_name() ).append( '\n' );
            // tags in it:
            for( Tag tag : ctg.mTags )
                create_db_tag_text( tag );
        }
        // UNTAGGED THEME
        create_db_tag_text( m_untagged );

        // TOPICS
        create_db_chapterctg_text( 'O', m_topics );

        // FREE CHAPTERS
        create_db_chapterctg_text( 'G', m_groups );

        // CHAPTERS
        for( Chapter.Category ctg : m_chapter_categories.values() )
        {
            // chapter category:
            mBufferedWriter.append( "ID" ).append( Integer.toString( ctg.get_id() ) )
                           .append( ctg == m_ptr2chapter_ctg_cur ? "\nCCc" : "\nCC_" )
                           .append( ctg.get_name() ).append( '\n' );
            // chapters in it:
            create_db_chapterctg_text( 'T', ctg );
        }

        // FILTER
        final int fs = m_filter_default.get_status();
        mBufferedWriter.append( "fs" )
                       .append( ( fs & DiaryElement.ES_SHOW_TRASHED ) != 0 ? 'T' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_NOT_TRASHED ) != 0 ? 't' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_FAVORED ) != 0 ? 'F' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_NOT_FAVORED ) != 0 ? 'f' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_NOT_TODO ) != 0 ? 'N' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_TODO ) != 0 ? 'T' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_PROGRESSED ) != 0 ? 'P' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_DONE ) != 0 ? 'D' : '_' )
                       .append( ( fs & DiaryElement.ES_SHOW_CANCELED ) != 0 ? 'C' : '_' )
                       .append( '\n' );
        if( ( fs & DiaryElement.ES_FILTER_TAG ) != 0 )
            mBufferedWriter.append( "ft" ).append( m_filter_default.get_tag().get_name() )
                           .append( '\n' );
        if( ( fs & DiaryElement.ES_FILTER_DATE_BEGIN ) != 0 )
            mBufferedWriter.append( "fb" ).append( Long.toString( m_filter_default.get_date_begin() ) )
                           .append( '\n' );
        if( ( fs & DiaryElement.ES_FILTER_DATE_END ) != 0 )
            mBufferedWriter.append( "fe" ).append( Long.toString( m_filter_default.get_date_end() ) )
                           .append( '\n' );

        // END OF SECTION
        mBufferedWriter.append( '\n' );

        // ENTRIES
        for( Entry entry : m_entries.values() ) {
            // purge empty entries:
            if( entry.m_text.length() < 1 && entry.m_tags.isEmpty() )
                continue;
            // optionally only save filtered entries: (may not come to Android too soon)
            // else if( entry.get_filtered_out() && m_flag_only_save_filtered )
            // continue;

            // ENTRY DATE
            mBufferedWriter.append( "ID" ).append( Integer.toString( entry.get_id() ) )
                           .append( "\n" );
            mBufferedWriter.append( entry.is_trashed() ? "e" : "E" )
                           .append( entry.is_favored() ? 'f' : '_' )
                           .append( m_filter_default.is_entry_filtered( entry ) ? 'h' : '_' );
            create_db_todo_status_text( entry );
            mBufferedWriter.append( Long.toString( entry.get_date_t() ) ).append( "\n" );

            mBufferedWriter.append( "Dr" ).append( Long.toString( entry.m_date_created ) )
                           .append( '\n' );
            mBufferedWriter.append( "Dh" ).append( Long.toString( entry.m_date_edited ) )
                           .append( '\n' );
            mBufferedWriter.append( "Ds" ).append( Long.toString( entry.m_date_status ) )
                           .append( '\n' );

            // TAGS
            for( Tag tag : entry.m_tags )
                mBufferedWriter.append( "T" ).append( tag == entry.get_theme_tag() ? 'T' : '_' )
                               .append( tag.get_name_and_value( entry, true, false ) )
                               .append( '\n' );

            // LOCATION
            if( ! entry.m_location.isEmpty() )
                mBufferedWriter.append( "L " ).append( entry.m_location ).append( '\n' );

            // LANGUAGE
            if( entry.get_lang().compareTo( Lifeograph.LANG_INHERIT_DIARY ) != 0 )
                mBufferedWriter.append( "l " ).append( entry.get_lang() ).append( '\n' );

            // CONTENT
            if( entry.m_text.isEmpty() )
                mBufferedWriter.append( '\n' );
            else {
                int pt_start = 0, pt_end;
                boolean flag_terminate_loop = false;

                while( true ) {
                    pt_end = entry.m_text.indexOf( '\n', pt_start );
                    if( pt_end == -1 ) {
                        pt_end = entry.m_text.length();
                        flag_terminate_loop = true;
                    }
                    else
                        pt_end++;

                    mBufferedWriter.append( "P " )
                                   .append( entry.m_text.substring( pt_start, pt_end ) );
                    if( flag_terminate_loop )
                        break; // end of while( true )

                    pt_start = pt_end;
                }

                mBufferedWriter.append( "\n\n" );
            }
        }
    }

    // DB READ/WRITE ===============================================================================
    private void close_file() {
        try {
            if( mBufferedReader != null ) {
                mBufferedReader.close();
                mBufferedReader = null;
            }
        }
        catch( IOException e ) {
            Log.e( Lifeograph.TAG, Objects.requireNonNull( e.getMessage() ) );
        }
    }

    private Result read_plain() {
        return parse_db_body_text();
    }

    private Result read_encrypted() {
        close_file();

        try {
            RandomAccessFile file = new RandomAccessFile( m_path, "r" );
            file.readLine(); // LIFEOGRAPHDB
            file.readLine(); // V
            file.readLine(); // E
            file.readLine(); // 0

            // allocate memory for salt and iv
            byte[] salt = new byte[ cSALT_SIZE ];
            byte[] iv = new byte[ cIV_SIZE ];

            // read salt and iv
            file.read( salt );
            file.read( iv );

            // calculate bytes of data in file
            int size = ( int ) ( file.length() - file.getFilePointer() );
            if( size <= 3 ) {
                clear();
                return Result.CORRUPT_FILE;
            }
            byte[] buffer = new byte[ size ];
            file.readFully( buffer );
            file.close();

            String output = decryptBuffer( m_passphrase, salt, buffer, size, iv );

            // passphrase check
            if( output.charAt( 0 ) != m_passphrase.charAt( 0 ) || output.charAt( 1 ) != '\n' ) {
                clear();
                return Result.WRONG_PASSWORD;
            }

            mBufferedReader = new BufferedReader( new StringReader( output ) );
        }
        catch( FileNotFoundException ex ) {
            return Result.FILE_NOT_FOUND;
        }
        catch( IOException ex ) {
            return Result.CORRUPT_FILE;
        }

        return parse_db_body_text();
    }

    private Result write_plain( String path, boolean flag_header_only ) {
        try {
            mBufferedWriter = new BufferedWriter( new FileWriter( path ) );
            create_db_header_text( flag_header_only );
            // header only mode is for encrypted diaries
            if( !flag_header_only ) {
                create_db_body_text();
            }

            mBufferedWriter.close();
            mBufferedWriter = null;

            return Result.SUCCESS;
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Failed to save diary: " + ex.getMessage() );

            mBufferedWriter = null;

            return Result.COULD_NOT_START;
        }
    }

    private Result write_encrypted( String path ) {
        // writing header
        write_plain( path, true );

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mBufferedWriter = new BufferedWriter( new OutputStreamWriter( baos ) );

            // first char of passphrase for validity checking
            mBufferedWriter.append( m_passphrase.charAt( 0 ) ).append( '\n' );
            create_db_body_text();
            mBufferedWriter.close();

            byte[] buffer = baos.toByteArray();

            byte[] output = encryptBuffer( m_passphrase, buffer, buffer.length );

            FileOutputStream file = new FileOutputStream( path, true );

            file.write( output );

            file.close();
            mBufferedWriter = null;
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Failed to save diary: " + ex.getMessage() );

            mBufferedWriter = null;

            return Result.COULD_NOT_START;
        }

        return Result.SUCCESS;
    }

    boolean remove_lock_if_necessary() {
        if( m_login_status != LoginStatus.LOGGED_IN_EDIT || m_path.isEmpty() )
            return false;

        File fp = new File( m_path + LOCK_SUFFIX );
        if( fp.exists() )
            return fp.delete();
        return true;
    }

    public boolean is_locked() {
        File lockFile = new File( m_path + LOCK_SUFFIX );
        return( lockFile.exists() );
    }

    void set_saving_enabled( boolean flag ) {
        m_flag_save_enabled = flag;
    }

    // DISK I/O ====================================================================================
    public Result set_path( String path, SetPathType type ) {
        // ANDROID ONLY:
        if( path.equals( sExampleDiaryPath ) ) {
            m_path = path;
            m_name = sExampleDiaryName;
            m_flag_read_only = true;
            return Result.SUCCESS;
        }

        // CHECK FOR SYSTEM PERMISSIONS
        File fp = new File( path );
        if( !fp.exists() ) {
            if( type != SetPathType.NEW )
            {
                Log.e( Lifeograph.TAG, "File is not found" );
                return Result.FILE_NOT_FOUND;
            }
        }
        else if( !fp.canRead() ) {
            Log.e( Lifeograph.TAG, "File is not readable" );
            return Result.FILE_NOT_READABLE;
        }
        else if( type == SetPathType.NEW && !fp.canWrite() ) {
            Log.w( Lifeograph.TAG, "File is not writable" );
            return Result.FILE_NOT_WRITABLE;
        }

        // ACCEPT PATH
        m_path = path;

        // update m_name
        int i = m_path.lastIndexOf( "/" );
        if( i == -1 )
            m_name = m_path;
        else
            m_name = m_path.substring( i + 1 );

        m_flag_read_only = ( type == SetPathType.READ_ONLY );

        if( !m_flag_read_only && is_locked() )
            return Result.FILE_LOCKED;
        else
            return Result.SUCCESS;
    }

    // ANDROID ONLY:
    public void enable_working_on_lockfile( boolean enable ) {
        if( enable )
            m_path += LOCK_SUFFIX;
        else if( m_path.endsWith( LOCK_SUFFIX ) )
            m_path = m_path.substring( 0, m_path.length() - LOCK_SUFFIX.length() );
    }

    public Result enable_editing() {
        if( m_flag_read_only )
        {
            Log.e( Lifeograph.TAG, "Diary: editing cannot be enabled. Diary is read-only" );
            return Result.FILE_LOCKED;
        }

        File fp = new File( m_path );
        if( !fp.canWrite() ) // check write access
        {
            Log.e( Lifeograph.TAG, "File is not writable" );
            return Result.FILE_NOT_WRITABLE;
        }

        // CREATE THE LOCK FILE
        File lockFile = new File( m_path + LOCK_SUFFIX );
        try {
            Lifeograph.copyFile( fp, lockFile );
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Could not create lock file" );
            return Result.FILE_NOT_WRITABLE;
        }

        m_login_status = LoginStatus.LOGGED_IN_EDIT;

        return Result.SUCCESS;
    }

//    public String get_path() {
//        return m_path;
//    }

//  NOT NEEDED NOW
//    public boolean is_path_set() {
//        return false;
//    }

    public Result read_header( AssetManager assetMan ) {
        String line;

        try {
            if( m_path.equals( sExampleDiaryPath ) ) {
                mBufferedReader = new BufferedReader( new InputStreamReader(
                        assetMan.open( "example.diary" ) ) );
            }
            else {
                mBufferedReader = new BufferedReader( new FileReader( m_path ) );
            }

            line = mBufferedReader.readLine();
            if( line == null ) {
                clear();
                return Result.CORRUPT_FILE;
            }
            else if( !line.equals( DB_FILE_HEADER ) ) {
                clear();
                return Result.CORRUPT_FILE;
            }

            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 1 ) { // end of header
                    return Result.SUCCESS;
                }

                switch( line.charAt( 0 ) ) {
                    case 'V':
                        m_read_version = Integer.parseInt( line.substring( 2 ) );
                        if( m_read_version < DB_FILE_VERSION_INT_MIN ) {
                            clear();
                            return Result.INCOMPATIBLE_FILE_OLD;
                        }
                        else if( m_read_version > DB_FILE_VERSION_INT ) {
                            clear();
                            return Result.INCOMPATIBLE_FILE_NEW;
                        }
                        break;
                    case 'E':
                        if( line.charAt( 2 ) == 'y' )
                            // passphrase is set to a dummy value to indicate that diary
                            // is an encrypted one until user enters the real passphrase
                            m_passphrase = " ";
                        else
                            m_passphrase = "";
                        break;
                    // case 0: // end of header
                    // m_body_position = br.position(); // not easy in java
                    // mFileReader.close();

                    default:
                        Log.e( Lifeograph.TAG, "Unrecognized header line: " + line );
                        break;
                }
            }
        }
        catch( IOException e ) {
            // Unable to create file, likely because external storage is not currently mounted.
            Log.e( Lifeograph.TAG, "Failed to open diary file " + m_path, e );
        }

        clear();
        return Result.CORRUPT_FILE;
    }

    public Result read_body() {
        Result res = m_passphrase.isEmpty() ? read_plain() : read_encrypted();

        close_file();

        enable_working_on_lockfile( false );

        if( res == Result.SUCCESS )
            m_login_status = LoginStatus.LOGGED_IN_RO;

        return res;
    }

    public Result write() {
        // BACKUP THE PREVIOUS VERSION
        File file = new File( m_path );
        if( file.exists() ) {
            File dir_backups = new File( file.getParent() + "/backups" );
            if( dir_backups.exists() || dir_backups.mkdirs() ) {
                File file_backup = new File( dir_backups, file.getName() + ".backup0" );
                if( file_backup.exists() ) {
                    File file_backup1 = new File( dir_backups, file.getName() + ".backup1" );
                    file_backup.renameTo( file_backup1 );
                }
                if( file.renameTo( file_backup ) )
                    Log.d( Lifeograph.TAG, "Backup written to: " + file_backup.toString() );
            }
        }

        // WRITE THE FILE
        return write( m_path );
    }

    public Result write( String path ) {
        // m_flag_only_save_filtered = false;

        if( m_passphrase.isEmpty() )
            return write_plain( path, false );
        else
            return write_encrypted( path );
    }

    public Result write_txt() {
        // contrary to c++ version this version always limits the operation to the filtered

        try {
            File file = new File( m_path );
            File dir_backups = new File( file.getParent() + "/backups" );
            FileWriter fileWriter;
            if( dir_backups.exists() || dir_backups.mkdirs() ) {
                File file_text = new File( dir_backups, file.getName() + ".txt" );
                fileWriter = new FileWriter( file_text.toString() );
            }
            else
                return Result.FILE_NOT_WRITABLE;

            // HELPERS
            Chapter.Category dummy_ctg_orphans = new Chapter.Category( null, "" );
            dummy_ctg_orphans.mMap.put( 0L, m_orphans );
            Chapter.Category[] chapters = new Chapter.Category[]
                    { dummy_ctg_orphans, m_ptr2chapter_ctg_cur, m_topics, m_groups };
            final String separator         = "---------------------------------------------\n";
            final String separator_favored = "+++++++++++++++++++++++++++++++++++++++++++++\n";
            final String separator_thick   = "=============================================\n";
            final String separator_chapter = ":::::::::::::::::::::::::::::::::::::::::::::\n";

            // DIARY TITLE
            fileWriter.write( separator_thick );
            fileWriter.append( file.getName() )
                      .append( '\n' )
                      .append( separator_thick );

            // ENTRIES
            for( int i = 0; i < 4; i++ ) {
                // CHAPTERS
                for( Chapter chapter : chapters[ i ].getMap().descendingMap().values() ) {
                    if( !chapter.mEntries.isEmpty() ) {
                        fileWriter.append( "\n\n" )
                                  .append( separator_chapter )
                                  .append( chapter.get_date().format_string() )
                                  .append( " - " )
                                  .append( chapter.get_name() )
                                  .append( '\n' )
                                  .append( separator_chapter )
                                  .append( "\n\n" );
                    }

                    // ENTRIES
                    for( Entry entry : chapter.mEntries.descendingSet() ) {
                        // PURGE EMPTY ENTRIES
                        if( ( entry.m_text.isEmpty() && entry.m_tags.isEmpty() ) ||
                                entry.get_filtered_out() )
                            continue;

                        if( entry.is_favored() )
                            fileWriter.append( separator_favored );
                        else
                            fileWriter.append( separator );

                        // DATE AND FAVOREDNESS
                        fileWriter.append( entry.get_date().format_string() );
                        if( entry.is_favored() )
                            fileWriter.append( '\n' ).append( separator_favored );
                        else
                            fileWriter.append( '\n' ).append( separator );

                        // CONTENT
                        fileWriter.append( entry.get_text() );

                        // TAGS
                        boolean first_tag = true;
                        for( Tag tag : entry.m_tags ) {
                            if( first_tag ) {
                                fileWriter.append( "\n\n" )
                                          .append( "TAGS" )
                                          .append( ": " );
                                first_tag = false;
                            }
                            else
                                fileWriter.append( ", " );

                            fileWriter.append( tag.get_name() );
                        }

                        fileWriter.append( "\n\n" );
                    }
                }
            }

            fileWriter.append( '\n' );

            fileWriter.close();

            return Result.SUCCESS;
        }
        catch( IOException ex ) {
            Log.e( Lifeograph.TAG, "Failed to save diary: " + ex.getMessage() );

            return Result.FAILURE;
        }
    }

    // Java only
    public void write_at_logout() {
        if( m_flag_save_enabled && is_in_edit_mode() ) {
            if( write() == Result.SUCCESS )
                Log.d( Lifeograph.TAG, "File saved successfully" );
            else
                Lifeograph.showToast( "Cannot save the diary file" );
        }
        else
            Log.d( Lifeograph.TAG, "Diary is not saved" );
    }

    // Java only
    public void write_lock() {
        if( m_flag_save_enabled && is_in_edit_mode() ) {
            if( write( m_path + LOCK_SUFFIX ) == Result.SUCCESS )
                Log.d( Lifeograph.TAG, "LOCK file saved successfully" );
            else
                Lifeograph.showToast( "Cannot save the lock file" );
        }
        else
            Log.d( Lifeograph.TAG, "Diary is not saved" );
    }

    // NATIVE ENCRYPTION METHODS ===================================================================
    private static native boolean initCipher();
    private native String decryptBuffer( String passphrase, byte[] salt,
                                         byte[] buffer, int size, byte[] iv );
    private native byte[] encryptBuffer( String passphrase, byte[] buffer, int size );

    // VARIABLES ===================================================================================
    private String m_path = "";
    private String m_passphrase = "";

    //ids (DEID)
    private int m_startup_elem_id;
    private int m_last_elem_id;
    private int m_current_id;
    private int m_force_id;
    private java.util.TreeMap< Integer, DiaryElement > m_ids = new TreeMap<>();

    java.util.TreeMap< Long, Entry > m_entries = new TreeMap<>( DiaryElement.compare_dates );
    Untagged m_untagged = new Untagged();
    java.util.TreeMap< String, Tag > m_tags = new TreeMap<>( DiaryElement.compare_names );
    java.util.TreeMap< String, Tag.Category > m_tag_categories =
            new TreeMap<>( DiaryElement.compare_names );
    java.util.TreeMap< String, Chapter.Category > m_chapter_categories =
            new TreeMap<>( DiaryElement.compare_names );
    Chapter.Category m_ptr2chapter_ctg_cur = null;
    Chapter.Category m_topics = new Chapter.Category( this, Date.TOPIC_MIN );
    Chapter.Category m_groups = new Chapter.Category( this, Date.GROUP_MIN );
    Chapter m_orphans = new Chapter( null, "<Other Entries>", Date.DATE_MAX );

    private String m_language = "";

    private int m_read_version;

    // options & flags
    private char m_option_sorting_criteria;
    private char m_option_sorting_dir;
    private boolean m_flag_read_only;
    private boolean m_flag_ignore_locks = false;
    private boolean m_flag_skip_old_check = false;
    private boolean m_flag_save_enabled = true;
    //private boolean m_flag_only_save_filtered;
    //private boolean m_flag_changed;

    enum LoginStatus{ LOGGED_OUT, LOGGED_TIME_OUT, LOGGED_IN_RO, LOGGED_IN_EDIT }
    private LoginStatus m_login_status = LoginStatus.LOGGED_OUT;

    // filtering
    private String m_search_text = "";
    Filter m_filter_active = new Filter( null, "Active Filter" );
    Filter m_filter_default = new Filter( null, "Default Filter" );

    // i/o
    // protected int m_body_offset;
    private BufferedReader mBufferedReader = null;
    private BufferedWriter mBufferedWriter = null;

    private static final int cIV_SIZE = 16; // = 128 bits
    private static final int cSALT_SIZE = 16; // = 128 bits
}
