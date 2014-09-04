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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.util.Log;
import android.widget.Toast;

enum Result {
    OK, ABORTED, SUCCESS, FAILURE, COULD_NOT_START, COULD_NOT_FINISH, WRONG_PASSWORD,
    APPARENTLY_ENCRYTED_FILE, APPARENTLY_PLAIN_FILE, INCOMPATIBLE_FILE, CORRUPT_FILE,
    FILE_NOT_FOUND, FILE_NOT_READABLE, FILE_LOCKED
}

public class Diary extends DiaryElement
{
    public static final char SC_DATE = 'd';
    public static final char SC_SIZE = 's';

    protected int m_current_id;
    protected int m_force_id;
    java.util.Map< Integer, DiaryElement > m_ids = new TreeMap< Integer, DiaryElement >();

    public final static String DB_FILE_HEADER = "LIFEOGRAPHDB";
    public final static int DB_FILE_VERSION_INT = 1020;
    public final static int DB_FILE_VERSION_INT_MIN = 110;
    public static final String LOCK_SUFFIX = ".~LOCK~";

    public final static int PASSPHRASE_MIN_SIZE = 4;
    public static Diary diary = null;

    public Diary() {
        super( null, DiaryElement.DEID_DIARY, ES_VOID );
        m_current_id = DiaryElement.DEID_FIRST;
        m_force_id = DiaryElement.DEID_UNSET;
    }

    public Result init_new( String path ) {
        clear();
        Result result = set_path( path, true, false );

        if( result != Result.SUCCESS ) {
            clear();
            return result;
        }

        // every diary must at least have one chapter category:
        m_ptr2chapter_ctg_cur = create_chapter_ctg( "Default" );

        add_today(); // must come after m_ptr2chapter_ctg_cur is set

        return Result.SUCCESS;
    }

    public void clear() {
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
        m_custom_sorteds.mMap.clear();
        m_orphaned_entries.clear();

        m_startup_elem_id = DiaryElement.HOME_CURRENT_ELEM;
        m_last_elem_id = DiaryElement.DEID_DIARY;

        m_passphrase = "";

        m_search_text = "";
        m_filter_active.reset();
        m_filter_default.reset();

        // NOTE: only reset body options here:
        m_language = "";
        m_option_sorting_criteria = SC_DATE;

        m_flag_changed = false;

        // java specific:
        mBufferedReader = null;
    }

    @Override
    public int get_size() {
        return m_entries.size();
    }

    @Override
    public String get_name() {
        int i = m_path.lastIndexOf( "/" );
        if( i == -1 )
            return m_path;
        else
            return m_path.substring( i + 1 );
    }

    @Override
    public DiaryElement.Type get_type() {
        return DiaryElement.Type.DIARY;
    }

    @Override
    public String getSubStr() {
        return m_path;
    }

    @Override
    public int get_icon() {
        return R.drawable.ic_diary;
    }

    // ID HANDLING =================================================================================
    protected int create_new_id( DiaryElement element ) {
        int retval;
        if( m_force_id == DiaryElement.DEID_UNSET )
            retval = m_current_id;
        else {
            retval = m_force_id;
            m_force_id = DiaryElement.DEID_UNSET;
        }
        m_ids.put( retval, element );

        while( m_ids.containsKey( m_current_id ) )
            m_current_id++;

        return retval;
    }

    protected boolean set_force_id( int id ) {
        if( m_ids.get( id ) != null || id <= DiaryElement.DEID_DIARY )
            return false;
        m_force_id = id;
        return true;
    }

    boolean make_free_entry_order( Date date ) {
        date.reset_order_1();
        while( m_entries.get( date.m_date ) != null )
            date.m_date += 1;

        return true; // reserved for bounds checking
    }

    public String create_unique_chapter_ctg_name( String name0 ) {
        String name = name0;
        for( int i = 1; m_chapter_categories.get( name ) != null; i++ ) {
            name = name0 + " " + i;
        }

        return name;
    }
    
    // DISK I/O ====================================================================================
    // protected int m_body_offset;
    protected BufferedReader mBufferedReader = null;
    protected String mStrIO = new String();

    public Result read_header() {
        // File file = new File( m_path );
        String line;

        try {
            FileReader fr = new FileReader( m_path );
            if( mBufferedReader == null )
                mBufferedReader = new BufferedReader( fr );

            line = mBufferedReader.readLine();

            if( line.equals( DB_FILE_HEADER ) == false ) {
                fr.close();
                mBufferedReader = null;
                return Result.CORRUPT_FILE;
            }

            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 1 ) { // end of header
                    return Result.SUCCESS;
                }

                switch( line.charAt( 0 ) ) {
                    case 'V':
                        m_read_version = Integer.parseInt( line.substring( 2 ) );
                        if( m_read_version < DB_FILE_VERSION_INT_MIN
                            || m_read_version > DB_FILE_VERSION_INT ) {
                            fr.close();
                            mBufferedReader = null;
                            return Result.INCOMPATIBLE_FILE;
                        }
                        break;
                    case 'E':
                        if( line.charAt( 2 ) == 'y' )
                            // passphrase is set to a dummy value to indicate that
                            // diary
                            // is an encrypted one until user enters the real
                            // passphrase
                            m_passphrase = " ";
                        else
                            m_passphrase = "";
                        break;
                    // case 0: // end of header
                    // m_body_position = br.position(); // not easy in java
                    // fr.close();

                    default:
                        Log.w( "LFO", "unrecognized header line: " + line );
                        break;
                }
            }

            fr.close();
        }
        catch( IOException e ) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w( "LFO", "Failed to open diary file " + m_path, e );
        }

        m_path = "";
        return Result.CORRUPT_FILE;
    }

    public Result read_body() {
        if( m_passphrase.length() == 0 )
            return read_plain();
        else
            // return read_encrypted();
            return Result.FAILURE;
    }

    public Result write() {
        /*
         * if( !m_flag_changed ) return Result.ABORTED; // XXX ???
         */

        m_flag_only_save_filtered = false;

        // File file_prev = new File( m_path + ".~previousversion~" );
        // File file = new File( m_path );

        // if( file.exists() )
        // file.renameTo( file_prev );

        Result result = write( m_path );

        if( result == Result.SUCCESS )
            m_flag_changed = false;

        return result;
    }

    public Result write( String path ) {
        // TODO: implement encryption
        // if( m_passphrase.length() == 0 )
        return write_plain( path, false );
        // else
        // return write_encrypted( path );
    }

    protected Result write_plain( String path, boolean flag_header_only ) {
        try {
            FileWriter fwr = new FileWriter( path );
            create_db_header_text( flag_header_only );
            // header only mode is for encrypted diaries
            if( !flag_header_only ) {
                create_db_body_text();
            }

            fwr.append( mStrIO );
            fwr.close();

            return Result.SUCCESS;
        }
        catch( IOException ex ) {
            Log.e( "L", "failed to save diary: " + ex.getMessage() );
            return Result.COULD_NOT_START;
        }
    }

    public Result set_path( String path, boolean new_file, boolean read_only ) {
        // CHECK FOR SYSTEM PERMISSIONS
        File fp = new File( path );
        if( new_file == false ) {
            if( fp.exists() == false )
                return Result.FILE_NOT_FOUND;

            if( fp.canRead() == false )
                return Result.FILE_NOT_READABLE;
        }

        if( fp.canWrite() == false ) {
            Toast.makeText( Lifeobase.activityOpenDiary,
                            "File is not writable, opening read-only..", Toast.LENGTH_LONG ).show();
            read_only = true;
        }

        // CHECK LOCK
        File lockFile = new File( path + LOCK_SUFFIX );
        if( !new_file && lockFile.exists() && !read_only ) {
            return Result.FILE_LOCKED;
        }

        // TODO: REMOVE PREVIOUS LOCK IF ANY

        // ACCEPT PATH
        m_path = path;
        m_flag_read_only = read_only;

        // TODO: "TOUCH" THE NEW LOCK

        return Result.SUCCESS;
    }

    public String get_path() {
        return m_path;
    }

    public boolean is_path_set() {
        return false;
    }

    public boolean set_passphrase( String passphrase ) {
        if( passphrase.length() >= PASSPHRASE_MIN_SIZE ) {
            m_passphrase = passphrase;
            return true;
        }
        else
            return false;
    }

    public DiaryElement get_element( int id ) {
        return m_ids.get( id );
    }

    public DiaryElement get_startup_elem() {
        return null;
    }

    public DiaryElement get_most_current_elem() {
        return null;
    }

    public DiaryElement get_prev_session_elem() {
        return null;
    }

    public Entry get_entry( long date ) {
        Entry entry = m_entries.get( date );
        if( entry != null )
            if( entry.get_filtered_out() == false )
                return entry;

        return null;
    }

    public Entry get_entry_today() {
        return m_entries.get( Date.get_today( 1 ) ); // 1 is the order
    }

    public Entry[] get_entries( long date ) {
        return null;
    }

    boolean get_day_has_multiple_entries( Date date_impure ) {
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

    // Entry get_entry_next_in_day( Date date );
    // Entry get_entry_first();
    // void set_entry_date( Entry entry, Date date );

    // boolean is_first( Entry e );
    // boolean is_last( Entry e );
    public char get_sorting_criteria() {
        return m_option_sorting_criteria;
    }

    void set_sorting_criteria( char sc ) {
        m_option_sorting_criteria = sc;
    }

    // FILTERING ===================================================================================
    void set_search_text( String filter ) {
        // TODO stub
    }

    String get_search_text() {
        return m_search_text;
    }

    public Filter get_filter() {
        return m_filter_active;
    }

    // ENTRIES =====================================================================================
    Entry create_entry( Date dateObj, String content, boolean flag_favorite ) {
        // make it the last entry of its day:
        dateObj.reset_order_1();
        long date = dateObj.m_date;
        while( m_entries.get( date ) != null )
            ++date;

        Entry entry = new Entry( this, date, content, flag_favorite );

        m_entries.put( date, entry );
        add_entry_to_related_chapter( entry );

        return( entry );
    }

    // adds a new entry to today even if there is already one or more:
    public Entry add_today() {
        Date date = new Date( Date.get_today( 0 ) );
        return create_entry( date, "", false );
    }

    boolean dismiss_entry( Entry entry ) {
        long date = entry.m_date.m_date;

        // fix startup element
        if( m_startup_elem_id == entry.get_id() )
            m_startup_elem_id = DiaryElement.DEID_DIARY;

        // remove from tags:
        for( Tag tag : entry.m_tags )
            tag.mEntries.remove( entry );

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

        return true;
    }

    // TAGS ========================================================================================
    java.util.Map< String, Tag > get_tags() {
        return m_tags;
    }

    public Untagged get_untagged()
    {
        return m_untagged;
    }

    public Tag.Category create_tag_ctg() {
        String name = create_unique_chapter_ctg_name( "New category" );
        Tag.Category new_category = new Tag.Category( this, name );
        m_tag_categories.put( name, new_category );

        return new_category;
    }

    public Tag.Category create_tag_ctg( String name ) {
        Tag.Category new_category = new Tag.Category( this, name );
        m_tag_categories.put( name, new_category );

        return new_category;
    }

    // public void dismiss_tag_ctg( CategoryTags ctg ) { }
    public Tag create_tag( String name, Tag.Category ctg ) {
        Tag tag = m_tags.get( name );
        if( tag != null ) {
            Log.w( "LFO", "Tag already exists: " + name );
            return( tag );
        }
        tag = new Tag( this, name, ctg );
        m_tags.put( name, tag );
        return tag;
    }

    public void dismiss_tag( Tag tag ) {
        // remove from entries:
        for( Entry e : tag.mEntries )
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
    /*
     * PoolCategoriesChapters get_chapter_ctgs() { return m_chapter_categories; }
     * CategoryChapters get_current_chapter_ctg() { return m_ptr2chapter_ctg_cur; }
     * CategoryChapters create_chapter_ctg();
     */
    Chapter.Category create_chapter_ctg( String name ) {
        Chapter.Category category = new Chapter.Category( this, name );
        m_chapter_categories.put( name, category );
        return category;
    }

    void dismiss_chapter_ctg( Chapter.Category ctg ) {
        // TODO
    }

    boolean rename_chapter_ctg( Chapter.Category ctg, String name ) {
        // TODO
        return false;
    }

    void dismiss_chapter( Chapter chapter ) {
        // TODO: add an option to also delete entries within the chapter
        if( chapter.is_ordinal() ) {
            // ORDER SHIFTING TO PRESERVE CONTINUITY
            if( m_topics.mMap.size() == 1 ) // must be handled separately
            {
                m_topics.mMap.remove( chapter.m_date_begin.m_date );
                return;
            }

            // CALCULATE HELPER VALUES
            Date d_chapter = chapter.m_date_begin;
            long d_chapter_next = d_chapter.m_date + Date.ORDINAL_STEP;
            boolean flag_last_chapter = ( m_topics.mMap.get( d_chapter_next ) == null );

            long last_order_of_prev_chapter = 0;
            if( flag_last_chapter ) {
                Chapter chapter_prev = ( m_topics.mMap.get( d_chapter.m_date - Date.ORDINAL_STEP ) );
                Date d_first_free = chapter_prev.get_free_order();
                last_order_of_prev_chapter = d_first_free.get_order() - 1;
            }
            else {
                Date d_first_free = chapter.get_free_order();
                last_order_of_prev_chapter = d_first_free.get_order() - 1;
            }

            // ACTUALLY DISMISS THE TOPIC
            m_topics.mMap.remove( chapter.m_date_begin.m_date );

            // SHIFT TOPICS
            for( long d = d_chapter.m_date + Date.ORDINAL_STEP;; d += Date.ORDINAL_STEP ) {
                Chapter chpt = m_topics.mMap.get( d );
                if( chpt == null )
                    break;
                m_topics.mMap.remove( d );
                chpt.set_date( d - Date.ORDINAL_STEP );
                m_topics.mMap.put( chpt.m_date_begin.m_date, chpt );
            }

            // SHIFT ENTRIES
            if( m_entries.size() > 0 ) {
                long date_first = ( Long ) m_entries.keySet().toArray()[ 0 ];
                for( long d = d_chapter.m_date + 1; d <= date_first; ) {
                    Entry entry = m_entries.get( d );
                    if( entry == null ) {
                        d += Date.ORDINAL_STEP;
                        d = Date.reset_order_1( d );
                        continue;
                    }

                    int order_diff =
                            ( entry.m_date.get_ordinal_order() - d_chapter.get_ordinal_order() );

                    if( order_diff > 0 || ( order_diff == 0 && flag_last_chapter ) ) {
                        m_entries.remove( d );
                        long d_new = ( d - Date.ORDINAL_STEP );
                        if( order_diff == 1 || ( order_diff == 0 && flag_last_chapter ) )
                            d_new += last_order_of_prev_chapter;
                        entry.m_date.m_date = d_new;
                        m_entries.put( d_new, entry );
                    }
                    d++;
                }
            }
        }
        else
            m_ptr2chapter_ctg_cur.dismiss_chapter( chapter );
    }

    // Date get_free_chapter_order_temporal();

    // Date get_free_chapter_order_ordinal();

    // boolean make_free_entry_order( Date date );

    // FUNCTIONS TO GET NEAREST CHAPTERS / TOPICS
    public Chapter getPrevChapter( Chapter chapter ) {
        if( chapter.is_ordinal() ) {
            long d_chapter_prev = chapter.m_date_begin.m_date - Date.ORDINAL_STEP;
            return m_topics.mMap.get( d_chapter_prev );
        }
        else
            return m_ptr2chapter_ctg_cur.getChapterEarlier( chapter );
    }

    public Chapter getNextChapter( Chapter chapter ) {
        if( chapter.is_ordinal() ) {
            long d_chapter_next = chapter.m_date_begin.m_date + Date.ORDINAL_STEP;
            return m_topics.mMap.get( d_chapter_next );
        }
        else
            return m_ptr2chapter_ctg_cur.getChapterLater( chapter );
    }

    public void update_entries_in_chapters() {
        Log.w( "LFO", "update_entries_in_chapters()" );
        Chapter.Category chapters[] = new Chapter.Category[] { m_topics, m_custom_sorteds,
                m_ptr2chapter_ctg_cur };
        Iterator itr_entry = m_entries.entrySet().iterator();
        Entry entry = null;
        if( itr_entry.hasNext() ) {
            Map.Entry mapEntry = ( Map.Entry ) itr_entry.next();
            entry = ( Entry ) mapEntry.getValue();
        }

        for( int i = 0; i < 3; i++ )
        {
            for( Chapter chapter : chapters[ i ].getMap().values() )
            {
                chapter.clear();

                if( entry == null )
                    continue;

                while( entry.m_date.m_date > chapter.m_date_begin.m_date )
                {
                    chapter.insert( entry );

                    if( !itr_entry.hasNext() )
                    {
                        entry = null;
                        break;
                    }
                    else
                    {
                        Map.Entry mapEntry = ( Map.Entry ) itr_entry.next();
                        entry = ( Entry ) mapEntry.getValue();
                    }
                }
            }
        }

        m_orphaned_entries.clear();

        while( itr_entry.hasNext() ) {
            Map.Entry mapEntry = ( Map.Entry ) itr_entry.next();
            m_orphaned_entries.add( ( Entry ) mapEntry.getValue() );
        }
    }

    public void add_entry_to_related_chapter( Entry entry ) {
        // NOTE: works as per the current listing options needs to be updated when something
        // changes the arrangement such as a change in the current chapter category

        Chapter.Category ptr2ctg;

        if( entry.m_date.is_ordinal() ) // in custom_sorteds or topics
            ptr2ctg = ( entry.m_date.is_hidden() ? m_custom_sorteds : m_topics );
        else // in chapters
            ptr2ctg = m_ptr2chapter_ctg_cur;

        for( Chapter chapter : ptr2ctg.getMap().values() )
        {
            if( entry.m_date.m_date > chapter.m_date_begin.m_date )
            {
                chapter.insert( entry );
                return;
            }
        }

        // if does not belong to any of the defined chapters:
        m_orphaned_entries.add( entry );
    }

    public void remove_entry_from_chapters( Entry entry ) {
        Chapter.Category ptr2ctg;

        if( entry.m_date.is_ordinal() ) // in custom_sorteds or topics
            ptr2ctg = ( entry.m_date.is_hidden() ? m_custom_sorteds : m_topics );
        else // in chapters
            ptr2ctg = m_ptr2chapter_ctg_cur;

        for( Chapter chapter : ptr2ctg.getMap().values() )
        {
            if( chapter.find( entry ) )
            {
                chapter.erase( entry );
                return;
            }
        }

        // if does not belong to any of the defined chapters:
        m_orphaned_entries.remove( entry );
    }

    public String get_lang() {
        return m_language;
    }

    void set_lang( String lang ) {
        m_language = lang;
    }

    boolean is_encrypted() {
        return( m_passphrase.length() > 0 );
    }

    // CONTENT =====================================================================================
    protected String m_path;
    private String m_passphrase;

    protected java.util.Map< Long, Entry > m_entries =
            new TreeMap< Long, Entry >( DiaryElement.compare_dates );
    protected Untagged m_untagged = new Untagged();
    protected java.util.Map< String, Tag > m_tags = new TreeMap< String, Tag >();
    protected java.util.Map< String, Tag.Category > m_tag_categories =
            new TreeMap< String, Tag.Category >( DiaryElement.compare_names );
    protected java.util.Map< String, Chapter.Category > m_chapter_categories =
            new TreeMap< String, Chapter.Category >( DiaryElement.compare_names );
    protected Chapter.Category m_ptr2chapter_ctg_cur = null;
    protected Chapter.Category m_topics = new Chapter.Category( this, Date.TOPIC_MIN );
    protected Chapter.Category m_custom_sorteds = new Chapter.Category( this, Date.SORTED_MIN );
    protected List< Entry > m_orphaned_entries = new ArrayList< Entry >();

    int m_startup_elem_id; // DEID
    int m_last_elem_id; // DEID
    // options & flags
    char m_option_sorting_criteria;
    int m_read_version;
    protected boolean m_flag_only_save_filtered;
    protected boolean m_flag_changed;
    protected boolean m_flag_read_only;
    protected String m_language;
    // filtering
    protected String m_search_text;
    protected Filter m_filter_active = new Filter( null, "Active Filter" );
    protected Filter m_filter_default = new Filter( null, "Default Filter" );

    protected long get_db_line_date( String line ) {
        long date = 0;

        for( int i = 2; i < line.length() && i < 12 && line.charAt( i ) >= '0'
                        && line.charAt( i ) <= '9'; i++ ) {
            date = ( date * 10 ) + line.charAt( i ) - '0';
        }

        return date;
    }

    protected String get_db_line_name( String line ) {
        int begin = line.indexOf( '\t' );
        if( begin == -1 )
            begin = 2;
        else
            begin++;

        return( line.substring( begin ) );
    }

    protected Result parse_db_body_text() {
        if( m_read_version == 1020 )
            return parse_db_body_text_1020();
        else if( m_read_version == 1010 || m_read_version == 1011 )
            return parse_db_body_text_1010();
        else
            return parse_db_body_text_110();
    }

    protected Result parse_db_body_text_1020() {
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
            while( ( line = mBufferedReader.readLine() ) != null )
            {
                if( line.length() < 1 ) // end of section
                    break;
                else if( line.length() < 3 )
                    continue;
                else
                {
                    switch( line.charAt( 0 ) )
                    {
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
                        case 'm':
                            if( ptr2tag == null )
                            {
                                Log.w( "LFO", "No tag declared for theme" );
                                break;
                            }
                            switch( line.charAt( 1 ) )
                            {
                                case 'f': // font
                                    ptr2tag.get_own_theme().font = line.substring( 2 );
                                    break;
                                case 'b': // base color
                                    ptr2tag.get_own_theme().color_base = line.substring( 2 );
                                    break;
                                case 't': // text color
                                    ptr2tag.get_own_theme().color_text = line.substring( 2 );
                                    break;
                                case 'h': // heading color
                                    ptr2tag.get_own_theme().color_heading = line.substring( 2 );
                                    break;
                                case 's': // subheading color
                                    ptr2tag.get_own_theme().color_subheading = line.substring( 2 );
                                    break;
                                case 'l': // highlight color
                                    ptr2tag.get_own_theme().color_highlight = line.substring( 2 );
                                    break;
                            }
                            break;
                        case 'f':
                            switch( line.charAt( 1 ) )
                            {
                                case 's':   // status
                                    if( line.length() < 9 )
                                    {
                                        Log.w( "LFO", "status filter length error" );
                                        continue;
                                    }
                                    m_filter_default.set_trash( line.charAt( 2 ) == 'T',
                                            line.charAt( 3 ) == 't' );
                                    m_filter_default.set_favorites( line.charAt( 4 ) == 'F',
                                            line.charAt( 5 ) == 'f' );
                                    m_filter_default.set_todo( line.charAt( 6 ) == 'N',
                                            line.charAt( 7 ) == 'T', line.charAt( 8 ) == 'D',
                                            line.charAt( 9 ) == 'C' );
                                    break;
                                case 't':   // tag
                                {
                                    Tag tag = m_tags.get( line.substring( 2 ) );
                                    if( tag != null )
                                        m_filter_default.set_tag( tag );
                                    else
                                        Log.w( "LFO", "Reference to undefined tag: "
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
                            switch( line.charAt( 1 ) )
                            {
                                case 'C':   // chapter category
                                    ptr2chapter_ctg = create_chapter_ctg( line.substring( 3 ) );
                                    if( line.charAt( 2 ) == 'c' )
                                        m_ptr2chapter_ctg_cur = ptr2chapter_ctg;
                                    break;
                                case 'T':   // temporal chapter
                                    if( ptr2chapter_ctg == null )
                                    {
                                        Log.w( "LFO", "No chapter category defined" );
                                        break;
                                    }
                                    ptr2chapter =
                                            ptr2chapter_ctg.create_chapter( get_db_line_name( line ),
                                                    get_db_line_date( line ) );
                                    break;
                                case 'O':   // ordinal chapter (topic)
                                    ptr2chapter =
                                            m_topics.create_chapter( get_db_line_name( line ),
                                                    get_db_line_date( line ) );
                                    break;
                                case 'S':   // sorted chapter
                                    ptr2chapter = m_custom_sorteds.create_chapter(
                                            get_db_line_name( line ), get_db_line_date( line ) );
                                    break;
                                case 'p':   // chapter preferences
                                    ptr2chapter.set_expanded( line.charAt( 2 ) == 'e' );
                                    if( line.charAt( 3 ) == 't' )
                                        ptr2chapter.set_todo_status( DiaryElement.ES_TODO );
                                    else if( line.charAt( 3 ) == 'd' )
                                        ptr2chapter.set_todo_status( DiaryElement.ES_DONE );
                                    else if( line.charAt( 3 ) == 'c' )
                                        ptr2chapter.set_todo_status( DiaryElement.ES_CANCELED );
                                    break;
                            }
                            break;
                        case 'O': // options
                            if( line.length() < 4 )
                                break;
                            m_option_sorting_criteria = line.charAt( 2 );
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
                            Log.w( "LFO", "unrecognized line:\n" + line );
                            return Result.CORRUPT_FILE;
                    }
                }
            }

            // ENTRIES
            while( ( line = mBufferedReader.readLine() ) != null )
            {
                if( line.length() < 2 )
                    continue;

                switch( line.charAt( 0 ) )
                {
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

                        if( line.charAt( 0 ) == 'e' )
                            entry_new.set_trashed( true );
                        if( line.charAt( 2 ) == 'h' )
                            m_filter_default.add_entry( entry_new );
                        if( line.charAt( 3 ) == 't' )
                            entry_new.set_todo_status( DiaryElement.ES_TODO );
                        else if( line.charAt( 3 ) == 'd' )
                            entry_new.set_todo_status( DiaryElement.ES_DONE );
                        else if( line.charAt( 3 ) == 'c' )
                            entry_new.set_todo_status( DiaryElement.ES_CANCELED );

                        flag_first_paragraph = true;
                        break;
                    case 'D':   // creation & change dates (optional)
                        if( entry_new == null )
                        {
                            Log.w( "LFO", "No entry declared" );
                            break;
                        }
                        if( line.charAt( 1 ) == 'r' )
                            entry_new.m_date_created = Long.parseLong( line.substring( 2 ) );
                        else    // it should be 'h'
                            entry_new.m_date_changed = Long.parseLong( line.substring( 2 ) );
                        break;
                    case 'T':   // tag
                        if( entry_new == null )
                            Log.w( "LFO", "No entry declared" );
                        else
                        {
                            Tag tag = m_tags.get( line.substring( 2 ) );
                            if( tag != null )
                            {
                                entry_new.add_tag( tag );
                                if( line.charAt( 1 ) == 'T' )
                                    entry_new.set_theme_tag( tag );
                            } else
                                Log.w( "LFO", "Reference to undefined tag: " + line.substring( 2 ) );
                        }
                        break;
                    case 'l':   // language
                        if( entry_new == null )
                            Log.w( "LFO", "No entry declared" );
                        else
                            entry_new.set_lang( line.substring( 2 ) );
                        break;
                    case 'P':    // paragraph
                        if( entry_new == null )
                        {
                            Log.w( "LFO", "No entry declared" );
                            break;
                        }
                        if( flag_first_paragraph )
                        {
                            if( line.length() > 2 )
                                entry_new.m_text = line.substring( 2 );
                            entry_new.m_name = entry_new.m_text;
                            flag_first_paragraph = false;
                        } else
                        {
                            entry_new.m_text += "\n";
                            entry_new.m_text += line.substring( 2 );
                        }
                        break;
                    default:
                        Log.w( "LFO", "Unrecognized line:\n" + line );
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

        return Result.SUCCESS;
    }

    protected long fix_pre_1020_date( long d ) {
        if( Date.is_ordinal( d ) ) {
            if( ( d & Date.VISIBLE_FLAG ) != 0 )
                d -= Date.VISIBLE_FLAG;
            else
                d |= Date.VISIBLE_FLAG;
        }

        return d;
    }

    protected Result parse_db_body_text_1010() {
        // TODO
        return Result.ABORTED;
    }

    protected Result parse_db_body_text_110() {
        String line = "";
        Entry entry_new = null;
        Chapter.Category ptr2chapter_ctg = null;
        Chapter ptr2chapter = null;
        Tag.Category ptr2tag_ctg = null;
        Theme ptr2theme = null;
        Theme ptr2default_theme = null;
        boolean flag_first_paragraph = false;

        // add tag for system theme
        create_tag( "[ - 0 - ]", null ).get_own_theme();

        // TAG DEFINITIONS & CHAPTERS
        try {
            while( ( line = mBufferedReader.readLine() ) != null )
            {
                if( line.length() < 1 ) // end of section
                    break;
                else if( line.length() < 3 )
                    continue;
                else {
                    switch( line.charAt( 0 ) ) {
                        case 'I':
                            set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                            break;
                        case 'T': // tag category
                            ptr2tag_ctg = create_tag_ctg( line.substring( 2 ) );
                            ptr2tag_ctg.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 't': // tag
                            create_tag( line.substring( 2 ), ptr2tag_ctg );
                            break;
                        case 'C': // chapter category
                            ptr2chapter_ctg = create_chapter_ctg( line.substring( 2 ) );
                            if( line.charAt( 1 ) == 'c' )
                                m_ptr2chapter_ctg_cur = ptr2chapter_ctg;
                            break;
                        case 'o': // ordinal chapter (topic)
                            ptr2chapter =
                                    m_topics.create_chapter( get_db_line_name( line ),
                                            fix_pre_1020_date( get_db_line_date( line ) ) );
                            ptr2chapter.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 'c': // chapter
                            if( ptr2chapter_ctg == null ) {
                                Log.w( "LFO", "No chapter category defined" );
                                break;
                            }
                            ptr2chapter =
                                    ptr2chapter_ctg.create_chapter(
                                            get_db_line_name( line ),
                                            fix_pre_1020_date( get_db_line_date( line ) ) );
                            ptr2chapter.set_expanded( line.charAt( 1 ) == 'e' );
                            break;
                        case 'M':
                            // themes with same name as tags are merged into existing tags
                            ptr2theme = create_tag( line.substring( 2 ), null ).get_own_theme();
                            if( line.charAt( 1 ) == 'd' )
                                ptr2default_theme = ptr2theme;
                            break;
                        case 'm':
                            if( ptr2theme == null ) {
                                Log.w( "LFO", "No theme declared" );
                                break;
                            }
                            switch( line.charAt( 1 ) ) {
                                case 'f': // font
                                    ptr2theme.font = line.substring( 2 );
                                    break;
                                case 'b': // base color
                                    ptr2theme.color_base = line.substring( 2 );
                                    break;
                                case 't': // text color
                                    ptr2theme.color_text = line.substring( 2 );
                                    break;
                                case 'h': // heading color
                                    ptr2theme.color_heading = line.substring( 2 );
                                    break;
                                case 's': // subheading color
                                    ptr2theme.color_subheading = line.substring( 2 );
                                    break;
                                case 'l': // highlight color
                                    ptr2theme.color_highlight = line.substring( 2 );
                                    break;
                            }
                            break;
                        case 'O': // options
                            if( line.length() < 4 )
                                break;
                            m_option_sorting_criteria = line.charAt( 3 );
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
                            Log.w( "LFO", "unrecognized line:\n" + line );
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
                    case 'E': // new entry
                    case 'e': // trashed
                        long date = Long.parseLong( line.substring( 2 ) );
                        entry_new = new Entry( this, fix_pre_1020_date( date ),
                                line.charAt( 1 ) == 'f' );
                        m_entries.put( date, entry_new );
                        add_entry_to_related_chapter( entry_new );

                        if( line.charAt( 0 ) == 'e' )
                            entry_new.set_trashed( true );

                        flag_first_paragraph = true;
                        break;
                    case 'D': // creation & change dates (optional)
                        if( entry_new == null ) {
                            Log.w( "LFO", "No entry declared" );
                            break;
                        }
                        if( line.charAt( 1 ) == 'r' )
                            entry_new.m_date_created = Long.parseLong( line.substring( 2 ) );
                        else
                            // it should be 'h'
                            entry_new.m_date_changed = Long.parseLong( line.substring( 2 ) );
                        break;
                    case 'M': // themes are converted into tags
                    case 'T': // tag
                        if( entry_new == null )
                            Log.w( "LFO", "No entry declared" );
                        else
                        {
                            Tag tag = m_tags.get( line.substring( 2 ) );
                            if( tag != null )
                                entry_new.add_tag( tag );
                            else
                                Log.w( "LFO", "Reference to undefined tag: " + line.substring( 2
                                ) );
                        }
                        break;
                    case 'l': // language
                        if( entry_new == null )
                            Log.w( "LFO", "No entry declared" );
                        else
                            entry_new.set_lang( line.substring( 2 ) );
                        break;
                    case 'P': // paragraph
                        if( entry_new == null ) {
                            Log.w( "LFO", "No entry declared" );
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
                        Log.w( "LFO", "Unrecognized line (110):\n" + line );
                        return Result.CORRUPT_FILE;
                }
            }
        }
        catch( IOException e ) {
            return Result.CORRUPT_FILE;
        }

        do_standard_checks_after_parse();

        // if default theme is different than the system theme, set the untagged accordingly
        if( ptr2default_theme != null )
            m_untagged.create_own_theme_duplicating( ptr2default_theme );

        return Result.SUCCESS;
    }

    protected void do_standard_checks_after_parse() {
        // every diary must at least have one chapter category:
        if( m_chapter_categories.size() < 1 )
            m_ptr2chapter_ctg_cur = create_chapter_ctg( "default" ); // TODO: i18n

        if( m_startup_elem_id > DiaryElement.HOME_FIXED_ELEM )
            if( get_element( m_startup_elem_id ) == null )
            {
                Log.w( "LFO", "startup element cannot be found in db" );
                m_startup_elem_id = DiaryElement.DEID_DIARY;
            }

        if( m_entries.size() < 1 )
        {
            add_today();
            Log.w( "LFO", "a dummy entry added to the diary" );
        }
    }
    protected boolean create_db_header_text( boolean encrypted ) {
        mStrIO = DB_FILE_HEADER; // clears string
        mStrIO += ( "\nV " + DB_FILE_VERSION_INT );
        mStrIO += ( encrypted ? "\nE yes" : "\nE no" );
        mStrIO += "\n\n"; // end of header

        return true;
    }

    protected void create_db_tag_text( char type, Tag tag ) {
        if( type == 'm' )
            mStrIO += ( "ID" + tag.get_id() + "\nt " + tag.get_name() + '\n' );

        if( tag.get_has_own_theme() )
        {
            Theme theme = tag.get_theme();

            mStrIO += ( type + "f" + theme.font + '\n' );
            mStrIO += ( type + "b" + theme.color_base + '\n' );
            mStrIO += ( type + "t" + theme.color_text + '\n' );
            mStrIO += ( type + "h" + theme.color_heading + '\n' );
            mStrIO += ( type + "s" + theme.color_subheading + '\n' );
            mStrIO += ( type + "l" + theme.color_highlight + '\n' );
        }
    }

    protected void create_db_chapterctg_text( char type, Chapter.Category ctg ) {
        for( Chapter chapter : ctg.mMap.values() )
        {
            mStrIO += ( "ID" + chapter.get_id()
                    + "\nC" + type + chapter.m_date_begin.m_date // type + date
                    + '\t' + chapter.get_name()   // name
                    + "\nCp" + ( chapter.get_expanded() ? 'e' : '_' ) );

            switch( chapter.get_todo_status() )
            {
                case DiaryElement.ES_NOT_TODO:
                    mStrIO += 'n';
                    break;
                case DiaryElement.ES_TODO:
                    mStrIO += 't';
                    break;
                case DiaryElement.ES_DONE:
                    mStrIO += 'd';
                    break;
                case DiaryElement.ES_CANCELED:
                    mStrIO += 'c';
                    break;
            }
            mStrIO += '\n';
        }
    }

    protected boolean create_db_body_text() {
        String content_std;

        // OPTIONS
        // dashed char used to be used for spell-checking before v110
        mStrIO += ( "O " + m_option_sorting_criteria + '\n' );
        if( m_language.length() > 0 )
            mStrIO += ( "l " + m_language + '\n' );

        // STARTUP ACTION (HOME ITEM)
        mStrIO += ( "S " + m_startup_elem_id + '\n' );
        mStrIO += ( "L " + m_last_elem_id + '\n' );

        // ROOT TAGS
        for( Tag tag : m_tags.values() )
        {
            if( tag.get_category() == null )
                create_db_tag_text( 'm', tag );
        }
        // CATEGORIZED TAGS
        for( Tag.Category ctg : m_tag_categories.values() ) {
            // tag category:
            mStrIO +=
                    ( "ID" + ctg.get_id() + "\nT" + ( ctg.get_expanded() ? 'e' : '_' )
                      + ctg.get_name() + '\n' );
            // tags in it:
            for( Tag tag : ctg.mTags )
            {
                create_db_tag_text( 'm', tag );
            }
        }
        // UNTAGGED THEME
        create_db_tag_text( 'u', m_untagged );

        // TOPICS
        create_db_chapterctg_text( 'O', m_topics );

        // CUSTOM SORTED ENTRY GROUPS
        create_db_chapterctg_text( 'S', m_custom_sorteds );

        // CHAPTERS
        for( Chapter.Category ctg : m_chapter_categories.values() )
        {
            // chapter category:
            mStrIO +=
                    ( "ID" + ctg.get_id() + ( ctg == m_ptr2chapter_ctg_cur ? "\nCCc" : "\nCC_" )
                      + ctg.get_name() + '\n' );
            // chapters in it:
            create_db_chapterctg_text( 'T', ctg );
        }

        // FILTER
        final int fs = m_filter_default.get_status();
        mStrIO += ( "fs"
                + ( ( fs & DiaryElement.ES_SHOW_TRASHED ) != 0 ? 'T' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_NOT_TRASHED ) != 0 ? 't' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_FAVORED ) != 0 ? 'F' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_NOT_FAVORED ) != 0 ? 'f' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_NOT_TODO ) != 0 ? 'N' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_TODO ) != 0 ? 'T' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_DONE ) != 0 ? 'D' : '_' )
                + ( ( fs & DiaryElement.ES_SHOW_CANCELED ) != 0 ? 'C' : '_' )
                + '\n' );
        if( ( fs & DiaryElement.ES_FILTER_TAG ) != 0 )
            mStrIO += ( "ft" + m_filter_default.get_tag().get_name() + '\n' );
        if( ( fs & DiaryElement.ES_FILTER_DATE_BEGIN ) != 0 )
            mStrIO += ( "fb" + m_filter_default.get_date_begin() + '\n' );
        if( ( fs & DiaryElement.ES_FILTER_DATE_END ) != 0 )
            mStrIO += ( "fe" + m_filter_default.get_date_end() + '\n' );

        // END OF SECTION
        mStrIO += '\n';

        // ENTRIES
        for( Entry entry : m_entries.values() ) {
            // purge empty entries:
            if( entry.m_text.length() < 1 && entry.m_tags.isEmpty() )
                continue;
            // optionally only save filtered entries: (may not come to Android too soon)
            // else if( entry.get_filtered_out() && m_flag_only_save_filtered )
            // continue;

            // ENTRY DATE
            mStrIO += ( "ID" + entry.get_id() + "\n" );
            mStrIO += ( ( entry.is_trashed() ? "e" : "E" )  +
                        ( entry.is_favored() ? 'f' : '_' ) +
                        ( m_filter_default.is_entry_filtered( entry ) ? 'h' : '_' ) );
            switch( entry.get_todo_status() )
            {
                case DiaryElement.ES_NOT_TODO:
                    mStrIO += 'n';
                    break;
                case DiaryElement.ES_TODO:
                    mStrIO += 't';
                    break;
                case DiaryElement.ES_DONE:
                    mStrIO += 'd';
                    break;
                case DiaryElement.ES_CANCELED:
                    mStrIO += 'c';
                    break;
            }
            mStrIO += ( entry.m_date.m_date + "\n" );
            mStrIO += ( "Dr" + entry.m_date_created + '\n' );
            mStrIO += ( "Dh" + entry.m_date_changed + '\n' );

            // TAGS
            for( Tag tag : entry.m_tags )
                mStrIO += ( "T" + ( tag == entry.get_theme_tag() ? 'T' : '_' ) + tag.get_name() +
                        '\n' );

            // LANGUAGE
            if( entry.get_lang().compareTo( Lifeobase.LANG_INHERIT_DIARY ) != 0 )
                mStrIO += ( "l " + entry.get_lang() + '\n' );

            // CONTENT
            if( entry.m_text.length() == 0 )
                mStrIO += '\n';
            else {
                content_std = entry.m_text;

                int pt_start = 0, pt_end;
                while( true ) {
                    pt_end = content_std.indexOf( '\n', pt_start );
                    if( pt_end == -1 ) {
                        pt_end = content_std.length();
                        mStrIO += ( "P " + content_std.substring( pt_start, pt_end ) );
                        break; // end of while( true )
                    }
                    else {
                        pt_end++;
                        mStrIO += ( "P " + content_std.substring( pt_start, pt_end ) );
                        pt_start = pt_end;
                    }
                }

                mStrIO += "\n\n";
            }
        }

        return true;
    }

    protected Result read_plain() {
        return parse_db_body_text();
    }
}
