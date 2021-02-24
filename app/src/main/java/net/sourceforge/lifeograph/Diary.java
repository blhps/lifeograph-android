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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import kotlin.Pair;

import net.sourceforge.lifeograph.helpers.*;


public class Diary extends DiaryElement
{
    static {
        System.loadLibrary( "gpg-error" );
        System.loadLibrary( "gcrypt" );
        System.loadLibrary( "lifeocrypt" );

        initCipher();
    }

    static final int SoCr_DATE          = 0x1;
    static final int SoCr_SIZE_C        = 0x2;  // size (char count)
    static final int SoCr_CHANGE        = 0x3;  // last change date
    static final int SoCr_NAME          = 0x4;  // name
    static final int SoCr_FILTER_CRTR   = 0xF;
    static final int SoCr_DESCENDING    = 0x10;
    static final int SoCr_ASCENDING     = 0x20;
    static final int SoCr_FILTER_DIR    = 0xF0;
    static final int SoCr_INVERSE       = 0x100; // (<2000) inverse dir for ordinals
    static final int SoCr_DESCENDING_T  = 0x100; // temporal
    static final int SoCr_ASCENDING_T   = 0x200; // temporal
    static final int SoCr_FILTER_DIR_T  = 0xF00; // temporal
    static final int SoCr_DEFAULT       = SoCr_DATE|SoCr_ASCENDING|SoCr_DESCENDING_T;

    static final String DB_FILE_HEADER = "LIFEOGRAPHDB";
    static final int    DB_FILE_VERSION_INT = 2000;
    static final int    DB_FILE_VERSION_INT_MIN = 1010;
    static final String LOCK_SUFFIX = ".~LOCK~";

    static final String sExampleDiaryPath = "*/E/X/A/M/P/L/E/D/I/A/R/Y/*";
    static final String sExampleDiaryName = "*** Example Diary ***";

    static final int PASSPHRASE_MIN_SIZE = 4;

    enum SetPathType { NORMAL, READ_ONLY, NEW }

    public Diary() {
        super( null, DiaryElement.DEID_UNSET, ES_VOID );
    }

    // MAIN FUNCTIONALITY ==========================================================================
    boolean
    is_old() {
        return( m_read_version < DB_FILE_VERSION_INT );
    }

    boolean
    is_encrypted() {
        return( !m_passphrase.isEmpty() );
    }

    boolean
    is_open() {
        return( m_login_status == LoginStatus.LOGGED_IN_RO ||
                m_login_status == LoginStatus.LOGGED_IN_EDIT );
    }

    boolean
    is_in_edit_mode() {
        return( m_login_status == LoginStatus.LOGGED_IN_EDIT );
    }

    boolean
    can_enter_edit_mode() {
        return( !m_flag_read_only );
    }

    // not part of c++
    boolean
    is_virtual() {
        return m_path.equals( sExampleDiaryPath );
    }

    Result
    init_new( String path, String pw ) {
        clear();

        set_id( create_new_id( this ) ); // adds itself to the ID pool with a unique ID

        m_read_version = DB_FILE_VERSION_INT;
        Result result = set_path( path, SetPathType.NEW );

        if( result != Result.SUCCESS ) {
            clear();
            return result;
        }

        // every diary must at least have one chapter category:
        m_p2chapter_ctg_cur = create_chapter_ctg( "Default" );

        m_filter_active = create_filter( "Default", Filter.DEFINITION_DEFAULT );
        m_chart_active = create_chart( "Default", ChartElem.DEFINITION_DEFAULT );
        m_table_active = create_table( "Default", TableElem.DEFINITION_DEFAULT );

        m_theme_default = create_theme( Theme.System.get().get_name() );
        Theme.System.get().copy_to( m_theme_default );
        //-------------NAME-------------FONT----BASE-------TEXT-------HEADING----SUBHEAD----HLIGHT----
        create_theme( "Dark",       "Sans 10", "#111111", "#CCCCCC", "#FF6666", "#DD3366", "#661133" );
        create_theme( "Artemisia", "Serif 10", "#FFEEEE", "#000000", "#CC0000", "#A00000", "#F1BBC4" );
        create_theme( "Urgent",     "Sans 10", "#A00000", "#FFAA33", "#FFFFFF", "#FFEE44", "#000000" );
        create_theme( "Well Noted", "Sans 11", "#BBEEEE", "#000000", "#553366", "#224488", "#90E033" );

        add_today(); // must come after m_ptr2chapter_ctg_cur is set
        set_passphrase( pw );

        m_login_status = LoginStatus.LOGGED_IN_RO;

        return write();
    }

    void
    clear() {
        close_file();

        m_path = "";
        m_read_version = 0;

        set_id( DEID_UNSET );
        m_force_id = DEID_UNSET;
        m_ids.clear();

        m_entries.clear();
        m_entry_names.clear();

        m_chapter_categories.clear();
        m_p2chapter_ctg_cur = null;

        m_search_text = "";
        clear_matches();

        m_filters.clear();
        m_filter_active = null;

        m_charts.clear();
        m_chart_active = null;

        m_tables.clear();
        m_table_active = null;

        clear_themes();
        m_theme_default = null;

        m_startup_entry_id = HOME_CURRENT_ENTRY;
        m_last_entry_id = DEID_UNSET;
        m_completion_tag_id = DEID_UNSET;

        m_passphrase = "";

        // NOTE: only reset body options here:
        m_language = "";
        m_sorting_criteria = SoCr_DEFAULT;
        m_opt_show_all_entry_locations = false;
        m_opt_ext_panel_cur = 1;

        m_flag_read_only = false;
        m_flag_ignore_locks = false;
        m_flag_skip_old_check = false;
        m_login_status = LoginStatus.LOGGED_OUT;

        // java specific:
        mBufferedReader = null;
    }

    // DIARYELEMENT INHERITED FUNCTIONS ============================================================
    @Override public DiaryElement.Type
    get_type() {
        return DiaryElement.Type.DIARY;
    }

    @Override public int
    get_size() {
        return m_entries.size();
    }

    @Override public int
    get_icon() {
        return R.mipmap.ic_diary;
    }

    public String
    get_info_str() {
        return( get_size() + " entries" );
    }

    // PASSPHRASE ==================================================================================
    @SuppressWarnings( "UnusedReturnValue" )
    boolean
    set_passphrase( String passphrase ) {
        if( passphrase.length() >= PASSPHRASE_MIN_SIZE ) {
            m_passphrase = passphrase;
            return true;
        }
        else
            return false;
    }

    boolean
    compare_passphrase( String passphrase ) {
        return m_passphrase.equals( passphrase );
    }

    boolean
    is_passphrase_Set() {
        return( !m_passphrase.isEmpty() );
    }

    // ID HANDLING =================================================================================
    DiaryElement
    get_element( int id ) {
        return m_ids.get( id );
    }

    int
    create_new_id( DiaryElement element ) {
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

    void
    erase_id( int id ) {
        m_ids.remove( id );
    }

    @SuppressWarnings( "UnusedReturnValue" )
    boolean
    set_force_id( int id ) {
        if( m_ids.get( id ) != null || id <= DiaryElement.DEID_MIN )
            return false;
        m_force_id = id;
        return true;
    }

    // OPTIONS =====================================================================================
//    public char get_sorting_criteria() {
//        return m_option_sorting_criteria;
//    }

//    public void set_sorting_criteria( char sc ) {
//        m_option_sorting_criteria = sc;
//    }

    String
    get_lang() {
        return m_language;
    }

//    public void set_lang( String lang ) {
//        m_language = lang;
//    }

    // ENTRIES =====================================================================================
    Entry
    get_most_current_entry() {
        long    date = Date.get_today( 0 );
        long    diff1 = Date.FLAG_ORDINAL;
        long    diff2;
        Entry   most_curr = null;
        boolean descending = false;

        for( Entry entry : m_entries.values() ) {
            if( ! entry.get_filtered_out() && ! entry.get_date().is_ordinal() ) {
                diff2 = entry.get_date_t() - date;
                if( diff2 < 0 )
                    diff2 *= -1;
                if( diff2 < diff1 ) {
                    diff1 = diff2;
                    most_curr = entry;
                    descending = true;
                }
            else
                if( descending )
                    break;
            }
        }

        return most_curr;
    }

    Entry
    get_entry_today() {
        return m_entries.get( Date.get_today( 1 ) ); // 1 is the order
    }

    Entry
    get_entry_by_id( int id ) {
        DiaryElement elem = get_element( id );

        if( elem != null && elem.get_type() == Type.ENTRY )
            return( ( Entry ) elem );
        else
            return null;
    }

    Entry
    get_entry_by_date( long date ) {
        return m_entries.get( date ); // unlike the C++ version, does not respect filters
    }

    Entry
    get_entry_by_name( String name ) {
        return m_entry_names.get( name ).get( 0 );
    }

    List< Entry >
    get_entries_by_name( String name ) {
        return m_entry_names.get( name );
    }

    Vector< Entry >
    get_entries_by_filter( String filter_name ) {
        Vector< Entry > ev = new Vector<>();
        Filter          filter = m_filters.get( filter_name );

        if( filter != null ) {
            FiltererContainer fc = filter.get_filterer_stack();

            for( Entry entry : m_entries.values() )
                if( fc.filter( entry ) )
                    ev.add( entry );
        }

        return ev;
    }

    int
    get_entry_count_on_day( Date date_impure ) {
        int count = 0;
        long date = date_impure.get_pure();
        while( m_entries.containsKey( date + count + 1 ) )
            count++;

        return count;
    }

    Entry
    get_entry_next_in_day( Date date ) {
        if( date.is_ordinal() )
            return null;

        for( Map.Entry< Long, Entry > kv_entry : m_entries.descendingMap().entrySet() ) {
            if( date.get_day() == Date.get_day( kv_entry.getKey() ) &&
                date.get_order_3rd() < Date.get_order_3rd( kv_entry.getKey() ) )
                return( kv_entry.getValue() );
        }

        return null;
    }

    Entry
    get_entry_first_untrashed() {
        for( Entry entry : m_entries.values() ) {
            if( ! entry.is_trashed() )
                return entry;
        }
        return null;
    }

    Entry
    get_entry_latest() {
        for( Map.Entry< Long, Entry > kv_entry : m_entries.entrySet() ) {
            if( ! Date.is_ordinal( kv_entry.getKey() ) )
                return kv_entry.getValue();
        }
        return null;
    }

    Entry
    get_entry_closest_to( long source, boolean only_unfiltered ) {
        Entry              entry_after = null;
        Entry              entry_before = null;
        int                i = 0;
        ArrayList< Long >  keys = new ArrayList<>( m_entries.keySet() );
        ArrayList< Entry > entries = new ArrayList<>( m_entries.values() );


        for( ; i < keys.size(); i++ ) {
            if( source < keys.get( i ) ) {
                entry_before = entries.get( i );
                break;
            }
            else if( source > keys.get( i ) )
                entry_after = entries.get( i );
        }

        if( only_unfiltered && entry_after != null && entry_after.get_filtered_out() ) {
            for( ; i >= 0; i-- ) {
                if( !entries.get( i ).get_filtered_out() ) {
                    entry_before = entries.get( i );
                    break;
                }
            }
        }

        if( only_unfiltered && entry_before != null && entry_before.get_filtered_out() ) {
            for( ; i < keys.size(); i++ ) {
                if( !entries.get( i ).get_filtered_out() ) {
                    entry_before = entries.get( i );
                    break;
                }
            }
        }

        if( entry_before == null && entry_after == null )
            return null;
        if( entry_before != null && entry_after == null )
            return entry_before;
        if( entry_before == null && entry_after != null )
            return entry_after;

        return( ( source - entry_before.get_date_t() ) < ( entry_after.get_date_t() - source ) ?
                entry_before : entry_after );
    }

    Entry
    get_first_descendant( long date ) {
        if( ! Date.is_ordinal( date ) )
            return null;

        Entry entry = m_entries.get( date );

        if( entry == null || date == m_entries.firstKey() )
            return null;
        else {
            Map.Entry< Long, Entry > kv_entry = m_entries.ceilingEntry( date + 1 );

            if( kv_entry != null && Date.is_descendant_of( kv_entry.getValue().get_date_t(),
                                                           date ) )
                return kv_entry.getValue();
        }

        return null;
    }

    void
    set_entry_date( @NonNull Entry entry, @NonNull Date date ) throws Exception {
        Vector< Entry > ev = new Vector<>();
        boolean         flag_move_children = false;
        boolean         flag_has_children = ( get_first_descendant( entry.get_date_t() ) != null );
        int             level_diff = ( entry.m_date.get_level() - date.get_level() );

        m_entries.remove( entry.get_date_t() );

        if( entry.is_ordinal() ) {
            // remove and store sub-entries first
            if( date.is_ordinal() && date.get_level() + entry.get_descendant_depth() <= 3 ) {
                flag_move_children = true;

                for( Map.Entry< Long, Entry > kv_entry : m_entries.entrySet() ) {
                    if( !Date.is_ordinal( kv_entry.getKey() ) )
                        break;

                    if( Date.is_descendant_of( kv_entry.getKey(), entry.get_date_t() ) )
                        ev.add( kv_entry.getValue() );
                }

                for( Entry e : ev ) {
                    m_entries.remove( e.get_date_t() );
                }

                // then shift the following siblings and their children backwards
                shift_entry_orders_after( entry.get_date_t(), -1 );

                // update target date per the shift above
                if( date.m_date > entry.get_date_t() &&
                    // below two together mean "is_nephew?"
                    !( Date.is_sibling( date.m_date, entry.get_date_t() ) ) &&
                    Date.is_descendant_of( date.m_date, entry.get_date().get_parent() ) )
                    date.backward_order( entry.get_date().get_level() );
            }
            else if( flag_has_children ) // no depth under the target to hold the descendants
                throw new Exception( "Tried to set an unfit date for the entry" );
        }
        else
        {
            shift_dated_entry_orders_after( entry.get_date_t(), -1 );
        }

        if( date.is_ordinal() ) {
            // then shift the entries after the target forwards to make room (if date is taken)
            if( m_entries.containsKey( date.m_date ) )
                shift_entry_orders_after( date.m_date, 1 );

            // add back removed sub entries
            if( flag_move_children ) {
                for( Entry e : ev ) {
                    Date date_new = new Date(
                            Date.make_ordinal( !date.is_hidden(), 0, 0, 0 ) );
                    int  level = 1;
                    // copy all orders from the new parent
                    for( ; level <= date.get_level(); level++ )
                        date_new.set_order( level, ( int ) date.get_order( level ) );
                    // copy all sub-orders from the previous
                    for( ; level <= 3; level++ )
                        date_new.set_order( level,
                                            ( int ) e.m_date.get_order( level + level_diff ) );

                    e.m_date.m_date = date_new.m_date;
                    m_entries.put( e.get_date_t(), e );
                }
            }
        }
        else
        {
            shift_dated_entry_orders_after( date.m_date, 1 );
        }

        entry.set_date( date.m_date );
        m_entries.put( date.m_date, entry );

        update_entries_in_chapters(); // date changes require full update
    }

    protected void
    addEntryNameToMap( Entry e ) {
        List< Entry > list = m_entry_names.get( e.m_name );
        if( list != null ) {
            list.add( e );
        }
        else {
            list = new ArrayList<>();
            list.add( e );
            m_entry_names.put( e.m_name, list );
        }
    }
    protected void
    removeEntryNameFromMap( Entry e ) {
        List< Entry > list = m_entry_names.get( e.m_name );
        if( list != null ) {
            if( list.contains( e ) ) {
                if( list.size() == 1 )
                    m_entry_names.remove( e.m_name );
                else
                    list.remove( e );
            }
        }
    }

    void
    update_entry_name( Entry entry ) {
        String  old_name = "";
        boolean flag_name_changed = true;

        for( Entry e : m_entries.values() ) {
            if( e.get_id() == entry.get_id() ) {
                if( e.m_name.equals( entry.m_name ) )
                    flag_name_changed = false;
                else
                    removeEntryNameFromMap( e );

                break;
            }
        }

        if( flag_name_changed && entry.has_name() ) {
            addEntryNameToMap( entry );

            // update references in other entries
            if( !old_name.isEmpty() )
                replace_all_matches( ":" + old_name + ":",
                                     ":" + entry.get_name() + ":" );
        }
    }

    Entry
    create_entry( long date, String content, boolean flag_favorite ) {
        // make it the last entry of its day:
        while( m_entries.containsKey( date ) && Date.get_order_3rd( date ) != 0 ) {
            if( Date.get_order_3rd( date ) == Date.ORDER_3RD_MAX ) {
                Log.e( Lifeograph.TAG, "Day is full!" );
                return null;
            }
            ++date;
        }

        Entry entry = new Entry( this, date,
                                 flag_favorite ? ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );

        entry.set_text( content );

        m_entries.put( date, entry );
        add_entry_to_related_chapter( entry );
        if( entry.has_name() )
            addEntryNameToMap( entry );

        return( entry );
    }
    // USED DURING DIARY FILE READ:
    Entry
    create_entry( long date, boolean flag_favorite, boolean flag_trashed, boolean flag_expanded ) {
        int status = ( ES_NOT_TODO |
                     ( flag_favorite ? ES_FAVORED : ES_NOT_FAVORED ) |
                     ( flag_trashed ? ES_TRASHED : ES_NOT_TRASHED ) );
        if( flag_expanded ) status |= ES_EXPANDED;

        Entry entry = new Entry( this, date, status );

        m_entries.put( date, entry );
        add_entry_to_related_chapter( entry );

        return( entry );
    }

    // adds a new entry to today even if there is already one or more:
    Entry
    add_today() {
        return create_entry( Date.get_today( 0 ), "", false );
    }

    boolean
    dismiss_entry( @NonNull Entry entry, boolean flag_update_chapters ) {
        // fix startup element:
        if( m_startup_entry_id == entry.get_id() )
            m_startup_entry_id = HOME_CURRENT_ENTRY;

        // remove from filters:
        remove_entry_from_filters( entry );

        // remove from map:
        m_entries.remove( entry.get_date_t() );

        // remove from name map:
        removeEntryNameFromMap( entry );

        // fix entry order:
        Vector< Entry > ev = new Vector<>();
        if( entry.is_ordinal() ) {
            // remove and store sub-entries first
            for( Map.Entry< Long, Entry > kv_entry : m_entries.entrySet() ) {
                if( !Date.is_ordinal( kv_entry.getKey() ) )
                    break;

                if( Date.is_descendant_of( kv_entry.getKey(), entry.get_date_t() ) )
                    ev.add( kv_entry.getValue() );
            }
            
            for( Entry e : ev )
                m_entries.remove( e.get_date_t() );

            // then, shift the following top items and their sub items backwards
            shift_entry_orders_after( entry.get_date_t(), -1 );

            // add back removed sub entries as top level entries
            Date lowest_top = new Date( get_available_order_1st( entry.is_ordinal_hidden() ) );
            for( int i = ev.size() - 1; i >= 0; i-- ) {
                Entry e = ev.elementAt( i );
                e.m_date.m_date = lowest_top.m_date;
                m_entries.put( lowest_top.m_date, e );
                lowest_top.forward_order_1st();
            }
        }
        else
            shift_dated_entry_orders_after( entry.get_date_t(), -1 );

        if( m_entries.isEmpty() ) // an open diary must always contain at least one entry
            add_today();

        if( flag_update_chapters )
            update_entries_in_chapters(); // date changes require full update

        return true;
    }

    void
    shift_entry_orders_after( long begin, int shift ) {
        if( shift == 0 || !Date.is_ordinal( begin ) )
            return;

        Vector< Entry > ev = new Vector<>();

        if( shift < 0 ) {
            Iterator< Map.Entry< Long, Entry > > iter =
                    m_entries.descendingMap().entrySet().iterator();
            while( iter.hasNext() ) {
                Map.Entry< Long, Entry > kv_entry = iter.next();

                if( ! Date.is_same_kind( begin, kv_entry.getKey() ) ) {
                    continue;
                }

                Entry entry = kv_entry.getValue();
                if( Date.is_descendant_of( entry.get_date_t(), Date.get_parent( begin ) ) &&
                    entry.get_date_t() > begin ) {
                    entry.m_date.backward_order( Date.get_level( begin ) );
                    // TODO check if the following is absolutely necessary:
                    entry.m_date.m_date = get_available_order_next( entry.get_date() );
                    ev.add( entry );
                    iter.remove();
                }
            }
        }
        else {
            Iterator< Map.Entry< Long, Entry > > iter = m_entries.entrySet().iterator();
            while( iter.hasNext() ) {
                Map.Entry< Long, Entry > kv_entry = iter.next();

                if( ! Date.is_same_kind( begin, kv_entry.getKey() ) )
                    continue;

                Entry entry = kv_entry.getValue();
                if( Date.is_descendant_of( entry.get_date_t(), Date.get_parent( begin ) ) &&
                    entry.get_date_t() >= begin ) {
                    entry.m_date.forward_order( Date.get_level( begin ) );
                    ev.add( entry );
                    iter.remove();
                }
            }
        }

        for( Entry e : ev )
            m_entries.put( e.get_date_t(), e );
    }

    void
    shift_dated_entry_orders_after( long begin, int shift ) {
        if( shift == 0 )
            return;

        Vector< Entry > ev = new Vector<>();

        if( shift < 0 ) {
            Iterator< Map.Entry< Long, Entry > > iter =
                    m_entries.descendingMap().entrySet().iterator();
            while( iter.hasNext() ) {
                Map.Entry< Long, Entry > kv_entry = iter.next();

                if( Date.is_ordinal( kv_entry.getKey() ) )
                    break;

                Entry entry = kv_entry.getValue();

                if( entry.get_date().get_pure() == Date.get_pure( begin ) &&
                    entry.get_date().get_order_3rd() > Date.get_order_3rd( begin ) ) {
                    entry.m_date.m_date--;
                    iter.remove();
                }
            }
        }
        else {
            Iterator< Map.Entry< Long, Entry > > iter = m_entries.entrySet().iterator();
            while( iter.hasNext() ) {
                Map.Entry< Long, Entry > kv_entry = iter.next();

                if( Date.is_ordinal( kv_entry.getKey() ) )
                    break;

                Entry entry = kv_entry.getValue();

                if( entry.get_date().get_pure() == Date.get_pure( begin ) &&
                    entry.get_date().get_order_3rd() >= Date.get_order_3rd( begin ) ) {
                    entry.m_date.m_date++;
                    iter.remove();
                }
            }
        }

        for( Entry e : ev )
            m_entries.put( e.get_date_t(), e );
    }

    long
    get_available_order_1st( boolean flag_hidden ) {
        long result = 0;

        for( Entry entry : m_entries.values() ) {
            if( entry.is_ordinal() && entry.is_ordinal_hidden() == flag_hidden )
            {
                result = entry.get_date_t();
                break;
            }
        }

        if( result != 0 )
            result = Date.make_ordinal( !flag_hidden, Date.get_order_1st( result ) + 1, 0, 0 );
        else
            result = Date.get_lowest_ordinal( flag_hidden );

        return result;
    }

    long
    get_available_order_sub( @NonNull Date date ) {
        final int level = ( date.get_level() + 1 );
        if( level > 3 )
            return Date.NOT_SET;

        date.set_order( level, 1 );
        while( m_entries.containsKey( date.m_date ) ) {
            if( date.get_order( level ) >= Date.ORDER_MAX )
                return Date.NOT_SET;

            date.forward_order( level );
        }

        return date.m_date;
    }

    long
    get_available_order_next( @NonNull Date date ) {
        while( m_entries.containsKey( date.m_date ) ) {
            if( date.get_order( date.get_level() ) >= Date.ORDER_MAX )
                return Date.NOT_SET;

            date.forward_order( date.get_level() );
        }

        return date.m_date;
    }

    boolean
    is_first( @NonNull Entry entry ) {
        return( entry.is_equal_to( Objects.requireNonNull( m_entries.firstEntry() ).getValue() ) );
    }

    boolean
    is_last( @NonNull Entry entry ) {
        return( entry.is_equal_to( Objects.requireNonNull( m_entries.lastEntry() ).getValue() ) );
    }

    Entry
    get_completion_tag() {
        return get_entry_by_id( m_completion_tag_id );
    }
    void
    set_completion_tag( int id ) {
        m_completion_tag_id = id;
    }

    // SEARCHING ===================================================================================
    int
    set_search_text( @NonNull String text, boolean flag_only_unfiltered ) {
        m_search_text = text;
        clear_matches();

        if( text.isEmpty() )
            return 0;

        int pos_para;
        int pos_entry;
        int length = m_search_text.length();
        int count = 0;

        for( Entry entry : m_entries.values() ) {
            pos_entry = 0;
            if( flag_only_unfiltered && entry.get_filtered_out() )
                continue;

            for( Paragraph para : entry.m_paragraphs ) {
                final String entrytext = para.m_text.toLowerCase();
                pos_para = 0;

                while( ( pos_para = entrytext.indexOf( m_search_text, pos_para ) ) != -1 ) {
                    m_matches.add( new Match( para, pos_entry + pos_para, pos_para ) );
                    count++;
                    pos_para += length;
                }

                pos_entry += ( entrytext.length() + 1 );
            }
        }

        return count;
    }

    String
    get_search_text() {
        return m_search_text;
    }

    boolean
    is_search_active() {
        return( !m_search_text.isEmpty() );
    }

    void
    replace_all_matches( String newtext ) {
        int       length_s  = m_search_text.length();
        int       delta     = ( newtext.length() - length_s );
        int       delta_cum = 0;
        Paragraph para_cur  = null;

        for( Match m : m_matches ) {
            if( !( m.valid ) ) continue;

            if( m.para != para_cur ) {
                delta_cum = 0;
                para_cur = m.para;
            }
            else
                m.pos_para += delta_cum;

            m.para.erase_text( m.pos_para, length_s );
            m.para.insert_text( m.pos_para, newtext );

            delta_cum += delta;
        }

        m_matches.clear();
    }

    // this version does not disturb the active search and is case-sensitive
    void
    replace_all_matches( String oldtext, String newtext ) {
        if( oldtext.isEmpty() )
            return;

        int       pos_para;
        int       pos_entry;
        int       length_old = oldtext.length();
        int       delta      = ( newtext.length() - length_old );
        int       delta_cum  = 0;
        Paragraph para_cur   = null;
        Vector< Match >
                  matches    = new Vector<>();

        // FIND
        for( Entry entry : m_entries.values() ) {
            pos_entry = 0;

            for( Paragraph para : entry.m_paragraphs ) {
                final String entrytext = para.m_text;
                pos_para = 0;

                while( ( pos_para = entrytext.indexOf( oldtext, pos_para ) ) != -1 ) {
                    matches.add( new Match( para, pos_entry + pos_para, pos_para ) );
                    pos_para += length_old;
                }

                pos_entry += ( entrytext.length() + 1 );
            }
        }

        // REPLACE
        for( Match m : matches ) {
            if( m.para != para_cur ) {
                delta_cum = 0;
                para_cur = m.para;
            }
            else
                m.pos_para += delta_cum;

            m.para.erase_text( m.pos_para, length_old );
            m.para.insert_text( m.pos_para, newtext );

            delta_cum += delta;
        }
    }

    void
    clear_matches() {
        m_matches.clear();
    }

    // FILTERS =====================================================================================
    Filter
    create_filter( String name0, String definition ) {
        String name = create_unique_name_for_map( m_filters, name0 );
        Filter filter = new Filter( this, name, definition );
        m_filters.put( name, filter );

        return filter;
    }

    Filter
    get_filter( String name ) {
        return m_filters.get( name );
    }

    boolean
    set_filter_active( String name ) {
        Filter filter = m_filters.get( name );
        if( filter == null )
            return false;

        m_filter_active = filter;
        return true;
    }

    String
    get_filter_active_name() {
        return m_filter_active.get_name();
    }

    boolean
    rename_filter( String old_name, String new_name ) {
        if( new_name.isEmpty() )
            return false;
        if( m_filters.containsKey( new_name ) )
            return false;

        Filter filter = m_filters.get( old_name );
        if( filter == null )
            return false;

        filter.set_name( new_name );

        m_filters.remove( old_name );
        m_filters.put( new_name, filter );

        return true;
    }

    boolean
    dismiss_filter( String name ) {
        if( m_filters.size() < 2 )
            return false;

        Filter filter_to_delete = m_filters.get( name );

        if( filter_to_delete == null )
            return false;

        m_filters.remove( name );

        if( name.equals( m_filter_active.get_name() ) )
            m_filter_active = m_filters.firstEntry().getValue();

        return true;
    }

    boolean
    dismiss_filter_active() {
        return dismiss_filter( m_filter_active.get_name() );
    }

    void
    remove_entry_from_filters( Entry entry ) {
        String rep_str = String.format( "Fr%d", DEID_UNSET );

        for( Filter filter : m_filters.values() ) {
            String definition = filter.get_definition();
            String ref_str    = String.format( "Fr%d", entry.get_id() );

            filter.set_definition( definition.replace( ref_str, rep_str ) );
        }
    }

    void // Android only
    updateAllEntriesFilterStatus() {
        if( m_filter_active == null ) return;

        //int count = 0;
        FiltererContainer fc = m_filter_active.get_filterer_stack();
        for( Entry entry : m_entries.values() ) {
            final boolean flag_filtered_in = fc.filter( entry );
            entry.set_filtered_out( ! flag_filtered_in );
            //if( flag_filtered_in ) count++;
        }
    }

    // CHARTS ======================================================================================
    ChartElem
    create_chart( String name0, String definition ) {
        String name = create_unique_name_for_map( m_charts, name0 );
        ChartElem chart = new ChartElem( this, name, definition );
        m_charts.put( name, chart );

        return chart;
    }

    ChartElem
    get_chart( String name ) {
        return m_charts.get( name );
    }

    boolean
    set_chart_active( String name ) {
        ChartElem chart = m_charts.get( name );
        if( chart == null )
            return false;

        m_chart_active = chart;
        return true;
    }

    String
    get_chart_active_name() {
        return m_chart_active.get_name();
    }

    boolean
    rename_chart( String old_name, String new_name ) {
        if( new_name.isEmpty() )
            return false;
        if( m_charts.containsKey( new_name ) )
            return false;

        ChartElem chart = m_charts.get( old_name );
        if( chart == null )
            return false;

        chart.set_name( new_name );

        m_charts.remove( old_name );
        m_charts.put( new_name, chart );

        return true;
    }

    boolean
    dismiss_chart( String name ) {
        ChartElem chart_to_delete = m_charts.get( name );

        if( chart_to_delete == null )
            return false;

        m_charts.remove( name );

        if( m_chart_active != null && name.equals( m_chart_active.get_name() ) ) {
            m_chart_active = m_charts.isEmpty() ? null : m_charts.firstEntry().getValue();
        }

        return true;
    }

    void
    fill_up_chart_data( ChartData cd ) {
        if( cd == null )
            return;

        if( cd.get_span() < 1 )
            return;

        for( Map.Entry< Long, Chapter > kv_chapter :
                m_p2chapter_ctg_cur.mMap.descendingMap().entrySet() ) {
            final Date d_chapter = kv_chapter.getValue().get_date();
            double pos = ( double ) cd.calculate_distance( cd.get_start_date(),
                                                           d_chapter.m_date );
            if( cd.get_start_date() > d_chapter.m_date )
                pos = -pos;
            switch( cd.get_period() ) {
                case ChartData.YEARLY:
                    pos += ( double ) ( d_chapter.get_yearday() - 1.0 ) / d_chapter.get_days_in_year();
                    break;
                case ChartData.MONTHLY:
                    pos += ( double ) ( d_chapter.get_day() - 1.0 ) / d_chapter.get_days_in_month();
                    break;
                case ChartData.WEEKLY:
                    pos += ( double ) ( d_chapter.get_weekday() ) / 7.0;
                    break;
            }

            cd.chapters.add(
                    new Pair<>( pos, Theme.midtone( kv_chapter.getValue().m_color,
                                                    Color.parseColor( "#FFFFFF" ),
                                                    0.7 ) ) );
        }

        // dummy entry just to have the last chapter drawn:
        cd.chapters.add( new Pair<>( Double.MAX_VALUE, Color.parseColor( "#000000" ) ) );
    }

    // TABLES ======================================================================================
    TableElem
    create_table( String name0, String definition ) {
        String name = create_unique_name_for_map( m_tables, name0 );
        TableElem table = new TableElem( this, name, definition );
        m_tables.put( name, table );

        return table;
    }

    TableElem
    get_table( String name ) {
        return m_tables.get( name );
    }

    boolean
    set_table_active( String name ) {
        TableElem table = m_tables.get( name );
        if( table == null )
            return false;

        m_table_active = table;
        return true;
    }

    String
    get_table_active_name() {
        return( m_table_active != null ? m_table_active.get_name() : "" );
    }

    boolean
    rename_table( String old_name, String new_name ) {
        if( new_name.isEmpty() )
            return false;
        if( m_tables.containsKey( new_name ) )
            return false;

        TableElem table = m_tables.get( old_name );
        if( table == null )
            return false;

        table.set_name( new_name );

        m_tables.remove( old_name );
        m_tables.put( new_name, table );

        return true;
    }

    boolean
    dismiss_table( String name ) {
        TableElem table_to_delete = m_tables.get( name );

        if( table_to_delete == null )
            return false;

        m_tables.remove( name );

        if( m_table_active != null && name.equals( m_table_active.get_name() ) ) {
            m_table_active = m_tables.isEmpty() ? null : m_tables.firstEntry().getValue();
        }

        return true;
    }

    // THEMES ======================================================================================
    Theme
    create_theme( String name0 ) {
        String name = create_unique_name_for_map( m_themes, name0 );
        Theme theme = new Theme( this, name );
        m_themes.put( name, theme );

        return theme;
    }
    Theme
    create_theme( String name0,
                  String font,
                  String base,  String text,
                  String head,  String subh,
                  String hlgt ) {
        String name = create_unique_name_for_map( m_themes, name0 );
        Theme theme = new Theme( this, name, font, base, text, head, subh, hlgt );
        m_themes.put( name, theme );

        return theme;
    }

    Theme
    get_theme( String name ) {
        return m_themes.get( name );
    }

    Theme
    get_theme_default() {
        return m_theme_default;
    }

    void
    set_theme_default( Theme theme ) {
        m_theme_default = theme;
    }

    void
    clear_themes() {
        m_themes.clear();
        m_theme_default = null;
    }

    void
    dismiss_theme( @NonNull Theme theme ) throws Exception {
        if( theme.is_default() ) throw new Exception( "Trying to delete the default theme" );

        for( Entry entry : m_entries.values() ) {
            if( theme.is_equal_to( entry.get_theme() ) )
                entry.set_theme( null );
        }

        // remove from associated filters
        for( Filter filter : m_filters.values() ) {
            String def = filter.get_definition().replace( "\nFh" + theme.get_name(),
                                                          "" );
            filter.set_definition( def );
        }

        m_themes.remove( theme.get_name() );
    }

    void
    rename_theme( @NonNull Theme theme, String new_name ) {
        m_themes.remove( theme.get_name() );
        theme.set_name( create_unique_name_for_map( m_themes, new_name ) );
        m_themes.put( theme.get_name(), theme );
    }

    // CHAPTERS ====================================================================================
    void
    set_chapter_ctg_cur( Chapter.Category ctg ) {
        m_p2chapter_ctg_cur = ctg;
        update_entries_in_chapters();
    }
    void
    set_chapter_ctg_cur( String ctg_name ) {
        m_p2chapter_ctg_cur = get_chapter_ctg( ctg_name );
        update_entries_in_chapters();
    }

    Chapter.Category
    get_chapter_ctg( String name ) {
        return m_chapter_categories.get( name );
    }

    Chapter.Category
    create_chapter_ctg( String name0 ) {
        String name = create_unique_name_for_map( m_chapter_categories, name0 );
        Chapter.Category category = new Chapter.Category( this, name );
        m_chapter_categories.put( name, category );
        return category;
    }

    boolean
    dismiss_chapter_ctg( @NonNull Chapter.Category ctg ) {
        if( m_chapter_categories.size() < 2 )
            return false;

        if( m_chapter_categories.remove( ctg.m_name ) != null ) {

            if( m_p2chapter_ctg_cur.is_equal_to( ctg ) )
                m_p2chapter_ctg_cur =
                        Objects.requireNonNull( m_chapter_categories.firstEntry() ).getValue();
            return true;
        }
        else
            return false;
    }

    void
    rename_chapter_ctg( @NonNull Chapter.Category ctg, String new_name ) {
        m_chapter_categories.remove( ctg.m_name );
        ctg.m_name = create_unique_name_for_map( m_chapter_categories, new_name );
        m_chapter_categories.put( ctg.m_name, ctg );
    }

    void
    dismiss_chapter( @NonNull Chapter chapter, boolean flag_dismiss_contained ) {
        if( !m_p2chapter_ctg_cur.mMap.containsKey( chapter.get_date_t() ) ) {
            Log.e( Lifeograph.TAG, "Chapter could not be found in assumed category" );
            return;
        }

        // BEWARE: higher means the earlier and lower means the later here!
        Map.Entry< Long, Chapter > kv_chapter_earlier =
                m_p2chapter_ctg_cur.mMap.higherEntry( chapter.get_date_t() );

        if( kv_chapter_earlier != null ) { // fix time span
            Chapter chapter_earlier = kv_chapter_earlier.getValue();
            if( chapter.m_time_span > 0 )
                chapter_earlier.m_time_span =
                        chapter_earlier.m_time_span + chapter.m_time_span;
            else
                chapter_earlier.m_time_span = 0;
        }

        if( flag_dismiss_contained ) {
            for( Entry entry : chapter.mEntries )
                dismiss_entry( entry, false );
        }

        m_p2chapter_ctg_cur.mMap.remove( chapter.get_date_t() );

        update_entries_in_chapters();
    }

    void
    update_entries_in_chapters() {
        Log.d( Lifeograph.TAG, "Diary.update_entries_in_chapters()" );

        Entry entry = Objects.requireNonNull( m_entries.firstEntry() ).getValue();

        while( entry != null && entry.is_ordinal() ) {
            Map.Entry< Long, Entry > kv_entry = m_entries.higherEntry( entry.get_date_t() );
            entry = ( kv_entry != null ? kv_entry.getValue() : null );
        }

        for( Chapter chapter : m_p2chapter_ctg_cur.mMap.values() ) {
            chapter.clear();

            while( entry != null && entry.get_date_t() > chapter.get_date_t() ) {
                chapter.insert( entry );
                Map.Entry< Long, Entry > kv_entry = m_entries.higherEntry( entry.get_date_t() );
                entry = ( kv_entry != null ? kv_entry.getValue() : null );
            }
        }
    }

    void
    add_entry_to_related_chapter( @NonNull Entry entry ) {
        // NOTE: works as per the current listing options needs to be updated when something
        // changes the arrangement such as a change in the current chapter category

        if( !( entry.is_ordinal() ) )
        {
            for( Chapter chapter : m_p2chapter_ctg_cur.mMap.values() ) {
                if( entry.get_date_t() > chapter.get_date_t() ) {
                    chapter.insert( entry );
                    return;
                }
            }
        }
    }

    void
    remove_entry_from_chapters( @NonNull Entry entry ) {
        for( Chapter chapter : m_p2chapter_ctg_cur.mMap.values() ) {
            if( chapter.mEntries.contains( entry ) ) {
                chapter.erase( entry );
                return;
            }
        }
    }

    // DB PARSING HELPER FUNCTIONS =================================================================
    static long
    get_db_line_date( String line ) {
        long date = 0;

        for( int i = 2; i < line.length() && i < 12 && line.charAt( i ) >= '0'
                        && line.charAt( i ) <= '9'; i++ ) {
            date = ( date * 10 ) + line.charAt( i ) - '0';
        }

        return date;
    }

    static String
    get_db_line_name( String line ) {
        int begin = line.indexOf( '\t' );
        if( begin == -1 )
            begin = 2;
        else
            begin++;

        return( line.substring( begin ) );
    }

    protected void
    parse_todo_status( DiaryElement elem, char c ) {
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

    protected void
    parse_theme( Theme ptr2theme, String line ) {
        switch( line.charAt( 1 ) ) {
            case 'f':   // font
                ptr2theme.font = line.substring( 2 );
                break;
            case 'b':   // base color
                ptr2theme.color_base= Theme.parse_color( line.substring( 2 ) );
                break;
            case 't':   // text color
                ptr2theme.color_text = Theme.parse_color( line.substring( 2 ) );
                break;
            case 'h':   // heading color
                ptr2theme.color_heading= Theme.parse_color( line.substring( 2 ) );
                break;
            case 's':   // subheading color
                ptr2theme.color_subheading= Theme.parse_color( line.substring( 2 ) );
                break;
            case 'l':   // highlight color
                ptr2theme.color_highlight= Theme.parse_color( line.substring( 2 ) );
                break;
            case 'i':   // background image
                ptr2theme.image_bg = line.substring( 2 );
                break;
        }
    }

    protected long
    tmp_upgrade_date_to_1020( long d ) {
        if( Date.is_ordinal( d ) ) {
            if( ( d & Date.FLAG_VISIBLE ) != 0 )
                d -= Date.FLAG_VISIBLE;
            else
                d |= Date.FLAG_VISIBLE;
        }

        return d;
    }

    protected long
    tmp_upgrade_ordinal_date_to_2000( long old_date ) {
        if( !Date.is_ordinal( old_date ) )
            return old_date;

        return Date.make_ordinal( !Date.is_hidden( old_date ),
                                  Date.get_order_2nd( old_date ),
                                  Date.get_order_3rd( old_date ),
                                  0 );
    }

    protected void
    tmp_add_tags_as_paragraph( Entry entry_new,
                               Map< Entry, Double > entry_tags,
                               ParserPara parser_para ) {
        if( entry_new != null ) {
            for( Map.Entry< Entry, Double > kv_tag : entry_tags.entrySet() ) {
                entry_new.add_tag( kv_tag.getKey(), kv_tag.getValue() );
                parser_para.parse( entry_new.m_paragraphs.get( entry_new.m_paragraphs.size() - 1 ) );
            }

            entry_tags.clear();
        }
    }

    protected void
    tmp_create_chart_from_tag( Entry tag, long type, Diary diary ) {
        String o2 = ( type & ChartData.UNDERLAY_MASK ) == ChartData.UNDERLAY_PREV_YEAR ? "Y" : "-";
        String o3 = ( type & ChartData.PERIOD_MASK ) == ChartData.MONTHLY ?              "M" : "Y";
        String o4 = ( type & ChartData.VALUE_TYPE_MASK ) == ChartData.AVERAGE ?          "A" : "P";
        // create predefined charts for non-boolean tags
        if( tag != null && ( type & ChartData.VALUE_TYPE_MASK ) != ChartData.BOOLEAN ) {
            diary.create_chart( tag.get_name(),
                                String.format( "Gyt%d\nGoT" + o2 + o3 + o4, tag.get_id() ) );
        }
    }

    protected void
    upgrade_to_1030() {
        // initialize the status dates:
        for( Entry entry : m_entries.values() )
            entry.m_date_status = entry.m_date_created;

        // replace old to-do boxes:
        replace_all_matches( "☐", "[ ]" );
        replace_all_matches( "☑", "[+]" );
        replace_all_matches( "☒", "[x]" );
    }

    protected void
    upgrade_to_1050() {
        for( Entry entry : m_entries.values() ) {
            if( entry.get_todo_status() == ES_NOT_TODO )
                entry.update_todo_status();
        }
    }

    protected void
    do_standard_checks_after_parse() {
        if( m_theme_default == null ) {
            m_theme_default = create_theme( Theme.System.get().m_name );
            Theme.System.get().copy_to( m_theme_default );
        }

        // DEFAULT CHART AND TABLE
        if( m_chart_active == null )
            m_chart_active = create_chart( Lifeograph.getStr( R.string.default_ ),
            ChartElem.DEFINITION_DEFAULT );
        if( m_table_active == null )
            m_table_active = create_table( Lifeograph.getStr( R.string.default_ ),
                                           TableElem.DEFINITION_DEFAULT );

        // initialize derived theme colors
        for( Theme theme : m_themes.values() )
            theme.calculate_derived_colors();

        // every diary must at least have one chapter category:
        if( m_chapter_categories.isEmpty() )
            m_p2chapter_ctg_cur = create_chapter_ctg( Lifeograph.getStr( R.string.default_ ) );

        if( m_startup_entry_id > DEID_MIN )
        {
            if( get_element( m_startup_entry_id ) == null )
            {
                Log.e( Lifeograph.TAG, "Startup element " + m_startup_entry_id
                                                               + " cannot be found in db" );
                m_startup_entry_id = HOME_CURRENT_ENTRY;
            }
        }
        else if( m_startup_entry_id == DEID_MIN ) // this is used when upgrading from <2000
            m_startup_entry_id = HOME_CURRENT_ENTRY;

        if( m_entries.isEmpty() ) {
            Log.i( Lifeograph.TAG, "A dummy entry added to the diary" );
            add_today();
        }
    }

    void
    add_entries_to_name_map() {
        for( Entry entry : m_entries.values() ) {
            // add entries to name map
            if( entry.has_name() )
                addEntryNameToMap( entry );
        }
    }

    // DB PARSING MAIN FUNCTIONS ===================================================================
    protected Result
    parse_db_body_text() {
        switch( m_read_version ) {
            case 2000:
            case 1999:
                return parse_db_body_text_2000();
            case 1050:
            case 1040:
            case 1030:
            case 1020:
            case 1011:
            case 1010:
                return parse_db_body_text_1050();
        }
        return Result.FAILURE; // should never happen
    }

    protected Result
    parse_db_body_text_2000() {
        String            line;
        Theme             ptr2theme = null;
        Filter            ptr2filter = null;
        ChartElem         ptr2chart = null;
        TableElem         ptr2table = null;
        Chapter.Category  ptr2chapter_ctg = null;
        Chapter           ptr2chapter = null;
        Entry             ptr2entry = null;
        Paragraph         ptr2para;
        ParserPara        parser_para = new ParserPara();

        // TAG DEFINITIONS & CHAPTERS
        try {
            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 2 )
                    continue;

                switch( line.charAt( 0 ) ) {
                    // DIARY OPTION
                    case 'D':
                        switch( line.charAt( 1 ) ) {
                            case 'o':   // options
                                m_opt_show_all_entry_locations = ( line.charAt( 2 ) == 'A' );
                                m_sorting_criteria = Integer.parseInt( line.substring( 3, 6 ) );
                                m_opt_ext_panel_cur = Integer.parseInt( line.substring( 6, 7 ) );
                                break;
                            case 's':   // spell checking language
                                m_language = line.substring( 2 );
                                break;
                            case 'f':   // first entry to show
                                m_startup_entry_id = Integer.parseInt( line.substring( 2 ) );
                                break;
                            case 'l':   // last entry shown in the previous session
                                m_last_entry_id = Integer.parseInt( line.substring( 2 ) );
                                break;
                            case 'c':   // completion tag
                                m_completion_tag_id = Integer.parseInt( line.substring( 2 ) );
                                break;
                        }
                        break;
                    // ID (START OF A NEW ELEMENT)
                    case 'I':   // id
                        set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                        break;
                    // THEME
                    case 'T':
                        if( line.charAt( 1 ) == ' ' ) { // declaration
                            ptr2theme = create_theme( line.substring( 3 ) );
                            if( line.charAt( 2 ) == 'D' )
                                m_theme_default = ptr2theme;
                        }
                        else
                            parse_theme( ptr2theme, line );
                        break;
                    // FILTER
                    case 'F':
                        if( line.charAt( 1 ) == ' ' ) { // declaration
                            ptr2filter = create_filter( line.substring( 3 ), "" );
                            if( line.charAt( 2 ) == 'A' )
                                m_filter_active = ptr2filter;
                        }
                        else
                            ptr2filter.add_definition_line( line );
                        break;
                    // CHART
                    case 'G':
                        if( line.charAt( 1 ) == ' ' ) { // declaration
                            ptr2chart = create_chart( line.substring( 3 ), "" );
                            if( line.charAt( 2 ) == 'A' )
                                m_chart_active = ptr2chart;
                        }
                        else {
                            ptr2chart.add_definition_line( line );
                        }
                        break;
                    // TABLE (MATRIX)
                    case 'M':
                        if( line.charAt( 1 ) == ' ' ) { // declaration
                            ptr2table = create_table( line.substring( 3 ), "" );
                            if( line.charAt( 2 ) == 'A' )
                                m_table_active = ptr2table;
                        }
                        else
                            ptr2table.add_definition_line( line );
                        break;
                    // CHAPTER CATEGORY
                    case 'C':
                        ptr2chapter_ctg = create_chapter_ctg( line.substring( 3 ) );
                        if( line.charAt( 2 ) == 'A' )
                            m_p2chapter_ctg_cur = ptr2chapter_ctg;
                        break;
                    // ENTRY / CHAPTER
                    case 'E':
                        switch( line.charAt( 1 ) ) {
                            case ' ':   // declaration
                                ptr2entry = create_entry( Long.parseLong( line.substring( 6 ) ),
                                                          line.charAt( 2 ) == 'F',
                                                          line.charAt( 3 ) == 'T',
                                                          line.charAt( 5 ) == 'E' );

                                parse_todo_status( ptr2entry, line.charAt( 4 ) );
                                break;
                            case '+':   // chapter declaration
                                ptr2entry = ptr2chapter = ptr2chapter_ctg.create_chapter(
                                        Long.parseLong( line.substring( 6 ) ),
                                        line.charAt( 2 ) == 'F',
                                        line.charAt( 3 ) == 'T',
                                        line.charAt( 5 ) == 'E' );

                                parse_todo_status( ptr2chapter, line.charAt( 4 ) );
                                break;
                            case 'c':
                                ptr2entry.m_date_created = Long.parseLong( line.substring( 2 ) );
                                break;
                            case 'e':
                                ptr2entry.m_date_edited = Long.parseLong( line.substring( 2 ) );
                                break;
                            case 't':   // to do status change date
                                ptr2entry.m_date_status = Long.parseLong( line.substring( 2 ) );
                                break;
                            case 'm':
                                ptr2entry.set_theme( m_themes.get( line.substring( 2 ) ) );
                                break;
                            case 's':   // spell checking language
                                ptr2entry.m_option_lang = line.substring( 2 );
                                break;
                            case 'u':   // spell checking language
                                ptr2entry.m_unit = line.substring( 2 );
                                break;
                            case 'l':   // location
                                if( line.charAt( 2 ) == 'a' )
                                    ptr2entry.m_location.latitude =
                                            parseDouble( line.substring( 3 ) );
                                else if( line.charAt( 2 ) == 'o' )
                                    ptr2entry.m_location.longitude =
                                            parseDouble( line.substring( 3 ) );
                                break;
                            case 'r':   // path (route)
                                if( line.charAt( 2 ) == 'a' )
                                    ptr2entry.add_map_path_point(
                                            parseDouble( line.substring( 3 ) ), 0.0 );
                        else if( line.charAt( 2 ) == 'o' )
                                ptr2entry.get_map_path_end().longitude =
                                        parseDouble( line.substring( 3 ) );
                                break;
                            case 'p':   // paragraph
                                ptr2para = ptr2entry.add_paragraph( line.substring( 3 ) );
                                parser_para.parse( ptr2para );
                                ptr2para.m_justification = line.charAt( 2 );
                                break;
                            case 'b':   // chapter bg color
                                ptr2chapter.m_color = Theme.parse_color( line.substring( 2 ) );
                                break;
                        }
                        break;
                    default:
                        Log.e( Lifeograph.TAG, "Unrecognized line: [" + line + "]" );
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
        add_entries_to_name_map();
        updateAllEntriesFilterStatus(); // Android addition

        return Result.SUCCESS;
    }

    protected Result
    parse_db_body_text_1050() {
        StringBuilder    read_buffer       = new StringBuilder();
        Lifeograph.MutableString  line     = new Lifeograph.MutableString();
        Lifeograph.MutableInt     line_offset  = new Lifeograph.MutableInt();
        Entry            entry_new;
        int              tag_o1            = 0;
        int              tag_o2            = 0;
        int              tag_o3            = 0;
        Chapter.Category p2chapter_ctg     = null;
        Chapter          p2chapter         = null;
        Theme            p2theme           = null;
        boolean          flag_in_tag_ctg   = false;
        StringBuilder    filter_def        = new StringBuilder( "F&" );
        Map< Entry, Double > entry_tags    = new TreeMap<>( DiaryElement.compare_elems_by_date );
        ParserPara       parser_para       = new ParserPara();
        String           chart_def_default = ChartElem.DEFINITION_DEFAULT;

        try {
            // PREPROCESSING TO DETERMINE FIRST AVAILABLE ORDER
            while( ( line.v = mBufferedReader.readLine() ) != null ) {
                read_buffer.append( line.v ).append( '\n' );

                if( !line.v.isEmpty() && line.v.charAt( 0 ) == 'C' && line.v.charAt( 1 ) == 'G' )
                    tag_o1++;
            }

            // TAGS TOP LEVEL
            entry_new = new Entry( this, Date.make_ordinal( false, ++tag_o1, 0 ) );
            m_entries.put( entry_new.m_date.m_date, entry_new );
            entry_new.add_paragraph( "[###>" );
            entry_new.set_expanded( true );

            // TAG DEFINITIONS & CHAPTERS
            String read_buffer_str = read_buffer.toString();
            while( Lifeograph.get_line( read_buffer_str, line_offset, line ) ) {
                if( line.v.isEmpty() )    // end of section
                    break;
                else if( line.v.length() >= 3 ) {
                    switch( line.v.charAt( 0 ) ) {
                        case 'I':   // id
                            set_force_id( Integer.parseInt( line.v.substring( 2 ) ) );
                            break;
                        // TAGS
                        case 'T':   // tag category
                            entry_new = new Entry( this,
                                                   Date.make_ordinal( false, tag_o1, ++tag_o2 ) );
                            m_entries.put( entry_new.m_date.m_date, entry_new );
                            entry_new.add_paragraph( line.v.substring( 2 ) );
                            entry_new.set_expanded( line.v.charAt( 1 ) == 'e' );
                            addEntryNameToMap( entry_new );
                            flag_in_tag_ctg = true;
                            tag_o3 = 0;
                            break;
                        case 't':   // tag
                            switch( line.v.charAt( 1 ) ) {
                                case ' ':
                                    entry_new = new Entry(
                                            this,
                                            flag_in_tag_ctg ?
                                            Date.make_ordinal( false, tag_o1, tag_o2, ++tag_o3 ) :
                                    Date.make_ordinal( false, tag_o1, ++tag_o2 ) );
                                    m_entries.put( entry_new.m_date.m_date, entry_new );
                                    entry_new.add_paragraph( line.v.substring( 2 ) );
                                    addEntryNameToMap( entry_new );
                                    p2theme = null;
                                    break;
                                case 'c': // not used in 1010
                                    tmp_create_chart_from_tag(
                                            entry_new, Long.parseLong( line.v.substring( 2 ) ), this );
                                    break;
                                case 'u': // not used in 1010
                                    if( entry_new != null )
                                        entry_new.m_unit = line.v.substring( 2 );
                                    break;
                            }
                            break;
                        case 'u':
                            if( m_theme_default == null && line.v.charAt( 1 ) != 'c' )
                                // chart is ignored
                                m_theme_default = create_theme( Theme.System.get().get_name() );
                            parse_theme( m_theme_default, line.v );
                            break;
                        case 'm':
                            if( p2theme == null )
                                p2theme = create_theme( entry_new.get_name() );
                            parse_theme( p2theme, line.v );
                            break;
                        // DEFAULT FILTER
                        case 'f':
                            switch( line.v.charAt( 1 ) ) {
                                case 's':   // status
                                    if( m_read_version < 1020 ) {
                                        filter_def.append( "\nFsN" );
                                        filter_def.append( ( line.v.charAt( 6 ) == 'T' ) ? 'O' :
                                                                                         'o' );
                                        // made in-progress entries depend on the preference for open ones:
                                        filter_def.append( ( line.v.charAt( 6 ) == 'T' ) ? 'P' :
                                                                                         'p' );
                                        filter_def.append( ( line.v.charAt( 7 ) == 'D' ) ? 'D' :
                                                                                         'd' );
                                        filter_def.append( ( line.v.charAt( 8 ) == 'C' ) ? 'C' :
                                                                                         'c' );
                                    }
                                    else {
                                        filter_def.append( "\nFs" );
                                        filter_def.append( ( line.v.charAt( 6 ) == 'N' ) ? 'N' :
                                                                                         'n' );
                                        filter_def.append( ( line.v.charAt( 7 ) == 'T' ) ? 'O' :
                                                                                         'o' );
                                        filter_def.append( ( line.v.charAt( 8 ) == 'P' ) ? 'P' :
                                                                                         'p' );
                                        filter_def.append( ( line.v.charAt( 9 ) == 'D' ) ? 'D' :
                                                                                         'd' );
                                        filter_def.append( ( line.v.charAt( 10 ) == 'C' ) ? 'C' :
                                                                                          'c' );
                                    }
                                    if( line.v.charAt( 2 ) == 'T' && line.v.charAt( 3 ) != 't' )
                                        filter_def.append( "\nFty" );
                                    else if( line.v.charAt( 2 ) != 'T' && line.v.charAt( 3 ) == 't' )
                                        filter_def.append( "\nFtn" );
                                    if( line.v.charAt( 4 ) == 'F' && line.v.charAt( 5 ) != 'f' )
                                        filter_def.append( "\nFfy" );
                                    else if( line.v.charAt( 4 ) != 'F' && line.v.charAt( 5 ) == 'f' )
                                        filter_def.append( "\nFfn" );
                                    break;
                                case 't':   // tag
                                {
                                    Entry tag = m_entry_names.get( line.v.substring( 2 ) ).get( 0 );
                                    if( tag != null )
                                        filter_def.append( "\nFt" ).append( tag.get_id() );
                                    else
                                        Log.e( Lifeograph.TAG,
                                               "Reference to undefined tag: " +
                                               line.v.substring( 2 ) );
                                    break;
                                }
                                case 'b':   // begin date: in the new system this is an after filter
                                    filter_def.append( "\nFa" ).append( line.v.substring( 2 ) );
                                    break;
                                case 'e':   // end date: in the new system this is a before filter
                                    filter_def.append( "\nFb" ).append( line.v.substring( 2 ) );
                                    break;
                            }
                            break;
                        // CHAPTERS
                        case 'o':   // ordinal chapter (topic) (<1020)
                            entry_new = new Entry( this, get_db_line_date( line.v ) );
                            tmp_upgrade_date_to_1020( entry_new.m_date.m_date );
                            tmp_upgrade_ordinal_date_to_2000( entry_new.m_date.m_date );
                            m_entries.put( entry_new.m_date.m_date, entry_new );
                            entry_new.set_text( get_db_line_name( line.v ) );
                            entry_new.set_expanded( line.v.charAt( 1 ) == 'e' );
                            break;
                        case 'd':   // to-do group (<1020)
                            if( line.v.charAt( 1 ) == ':' ) { // declaration
                                entry_new = new Entry( this, get_db_line_date( line.v ) );
                                tmp_upgrade_date_to_1020( entry_new.m_date.m_date );
                                tmp_upgrade_ordinal_date_to_2000( entry_new.m_date.m_date );
                                m_entries.put( entry_new.m_date.m_date, entry_new );
                                entry_new.set_text( get_db_line_name( line.v ) );
                            }
                            else { // options
                                entry_new.set_expanded( line.v.charAt( 2 ) == 'e' );
                                if( line.v.charAt( 3 ) == 'd' )
                                    entry_new.set_todo_status( ES_DONE );
                                else if( line.v.charAt( 3 ) == 'c' )
                                    entry_new.set_todo_status( ES_CANCELED );
                                else
                                    entry_new.set_todo_status( ES_TODO );
                            }
                            break;
                        case 'c':   // temporal chapter (<1020)
                            if( p2chapter_ctg != null ) {
                                long d_c = get_db_line_date( line.v );
                                tmp_upgrade_date_to_1020( d_c );

                                entry_new = p2chapter =
                                        p2chapter_ctg.create_chapter( d_c, false, false, true );
                                p2chapter.set_text( get_db_line_name( line.v ) );
                            }
                            else
                                Log.e( Lifeograph.TAG, "No chapter category defined" );
                            break;
                        case 'C':
                            if( m_read_version < 1020 ) { // chapter category
                                p2chapter_ctg = create_chapter_ctg( line.v.substring( 2 ) );
                                if( line.v.charAt( 1 ) == 'c' )
                                    m_p2chapter_ctg_cur = p2chapter_ctg;
                                break;
                            }
                            switch( line.v.charAt( 1 ) ) {
                                // any chapter item based on line[1] (>=1020)
                                case 'C':   // chapter category
                                    p2chapter_ctg = create_chapter_ctg( line.v.substring( 3 ) );
                                    if( line.v.charAt( 2 ) == 'c' )
                                        m_p2chapter_ctg_cur = p2chapter_ctg;
                                    break;
                                case 'c':   // chapter color
                                    p2chapter.m_color = Theme.parse_color( line.v.substring( 2 ) );
                                    break;
                                case 'T':   // temporal chapter
                                    entry_new = p2chapter =
                                            p2chapter_ctg.create_chapter(
                                                    get_db_line_date( line.v ),
                                                    false, false, true );
                                    p2chapter.set_text( get_db_line_name( line.v ) );
                                    break;
                                case 'O':   // ordinal chapter (used to be called topic)
                                case 'G':   // free chapter (replaced todo_group in v1020)
                                    entry_new = new Entry( this, get_db_line_date( line.v ) );
                                    tmp_upgrade_ordinal_date_to_2000( entry_new.m_date.m_date );
                                    m_entries.put( entry_new.m_date.m_date, entry_new );
                                    entry_new.set_text( get_db_line_name( line.v ) );
                                    break;
                                case 'p':   // chapter preferences
                                    entry_new.set_expanded( line.v.charAt( 2 ) == 'e' );
                                    parse_todo_status( entry_new, line.v.charAt( 3 ) );
                                    //line.v.charAt( 4 ] (Y) is ignored as we no longer create charts for chapters
                                    break;
                            }
                            break;
                        case 'O':   // options
                            switch( line.v.charAt( 2 ) ) {
                                case 'd': m_sorting_criteria = SoCr_DATE; break;
                                case 's': m_sorting_criteria = SoCr_SIZE_C; break;
                                case 'c': m_sorting_criteria = SoCr_CHANGE; break;
                            }
                            if( m_read_version == 1050 ) {
                                m_sorting_criteria |= ( line.v.charAt( 3 ) == 'd' ?
                                                        SoCr_DESCENDING : SoCr_ASCENDING );

                                if( line.v.length() > 4 && line.v.charAt( 4 ) == 'Y' )
                                    chart_def_default = ChartElem.DEFINITION_DEFAULT_Y;
                            }
                            else if( m_read_version == 1040 ) {
                                if( line.v.length() > 3 && line.v.charAt( 3 ) == 'Y' )
                                    chart_def_default = ChartElem.DEFINITION_DEFAULT_Y;
                            }
                            break;
                        case 'l':   // language
                            m_language = line.v.substring( 2 );
                            break;
                        case 'S':   // startup action
                            m_startup_entry_id = Integer.parseInt( line.v.substring( 2 ) );
                            break;
                        case 'L':
                            m_last_entry_id = Integer.parseInt( line.v.substring( 2 ) );
                            break;
                        default:
                            Log.e( Lifeograph.TAG, "Unrecognized line:\n" + line.v );
                            clear();
                            return Result.CORRUPT_FILE;
                    }
                }
            }

            // ENTRIES
            entry_new = null;
            while( Lifeograph.get_line( read_buffer_str, line_offset, line ) ) {
                if( line.v.length() < 2 )
                    continue;
                else if( line.v.charAt( 0 ) != 'I' && line.v.charAt( 0 ) != 'E' && line.v.charAt( 0 ) != 'e' && entry_new == null ) {
                    Log.e( Lifeograph.TAG, "No entry declared for the attribute" );
                    continue;
                }

                switch( line.v.charAt( 0 ) ) {
                    case 'I':
                        set_force_id( Integer.parseInt( line.v.substring( 2 ) ) );
                        break;
                    case 'E':   // new entry
                    case 'e':   // trashed
                        if( line.v.length() < 5 )
                            continue;

                        // add tags as inline tags
                        tmp_add_tags_as_paragraph( entry_new, entry_tags, parser_para );

                        entry_new = new Entry( this, Long.parseLong( line.v.substring( 4 ) ),
                                               line.v.charAt( 1 ) == 'f' ?
                                                       ES_ENTRY_DEFAULT_FAV : ES_ENTRY_DEFAULT );
                        if( m_read_version < 1020 )
                            tmp_upgrade_date_to_1020( entry_new.m_date.m_date );
                        tmp_upgrade_ordinal_date_to_2000( entry_new.m_date.m_date );
                        m_entries.put( entry_new.m_date.m_date, entry_new );

                        if( line.v.charAt( 0 ) == 'e' )
                            entry_new.set_trashed( true );
                        if( line.v.charAt( 2 ) == 'h' )
                            filter_def.append( "\nFn" ).append( entry_new.get_id() );

                        parse_todo_status( entry_new, line.v.charAt( 3 ) );

                        // all hidden entries were to-do items once:
                        if( m_read_version < 1020 && entry_new.get_date().is_hidden() )
                            entry_new.set_todo_status( ES_TODO );

                        break;
                    case 'D':   // creation & change dates (optional)
                        switch( line.v.charAt( 1 ) ) {
                            case 'r':
                                entry_new.m_date_created = Long.parseLong( line.v.substring( 2 ) );
                                break;
                            case 'h':
                                entry_new.m_date_edited = Long.parseLong( line.v.substring( 2 ) );
                                break;
                            case 's':
                                entry_new.m_date_status = Long.parseLong( line.v.substring( 2 ) );
                                break;
                        }
                        break;
                    case 'T':    // tag
                    {
                        NameAndValue nav = NameAndValue.parse( line.v.substring( 2 ) );
                        Entry        tag = m_entry_names.get( nav.name ).get( 0 );
                        if( tag != null ) {
                            entry_tags.put( tag, nav.value );
                            if( line.v.charAt( 1 ) == 'T' )
                                entry_new.set_theme( m_themes.get( nav.name ) );
                        }
                        else
                            Log.e( Lifeograph.TAG, "Reference to undefined tag: " + nav.name );
                        break;
                    }
                    case 'l':   // language
                        entry_new.m_option_lang = line.v.substring( 2 );
                        break;
                    case 'P':    // paragraph
                        parser_para.parse( entry_new.add_paragraph( line.v.substring( 2 ) ) );
                        break;
                    default:
                        Log.e( Lifeograph.TAG,"Unrecognized line:\n" + line );
                        clear();
                        return Result.CORRUPT_FILE;
                }
            }
        }
        catch( IOException e )
        {
            return Result.CORRUPT_FILE;
        }

        // add tags to the last entry as inline tags
        tmp_add_tags_as_paragraph( entry_new, entry_tags, parser_para );

        update_entries_in_chapters();

        if( m_read_version == 1020 )
            upgrade_to_1030();

        if( m_read_version < 1050 )
            upgrade_to_1050();

        // DEFAULT FILTER AND CHART
        m_filter_active = create_filter( Lifeograph.getStr( R.string.default_ ),
                                         filter_def.toString() );
        m_chart_active = create_chart( Lifeograph.getStr( R.string.default_ ), chart_def_default );

        do_standard_checks_after_parse();
        add_entries_to_name_map();
        updateAllEntriesFilterStatus(); // Android addition

        return Result.SUCCESS;
    }

    // DB CREATING FUNCTIONS =======================================================================
    protected char
    get_elem_todo_status_char( @NonNull DiaryElement elem ) {
        switch( elem.get_todo_status() ) {
            case ES_TODO:                         return 't';
            case ( ES_NOT_TODO | ES_TODO ):       return 'T';
            case ES_PROGRESSED:                   return 'p';
            case ( ES_NOT_TODO | ES_PROGRESSED ): return 'P';
            case ES_DONE:                         return 'd';
            case ( ES_NOT_TODO | ES_DONE ):       return 'D';
            case ES_CANCELED:                     return 'c';
            case ( ES_NOT_TODO | ES_CANCELED ):   return 'C';
            case ES_NOT_TODO:
            default:    /* should never occur */  return 'n';
        }
    }

    void
    create_db_entry_text( @NonNull Entry entry ) throws IOException {
        // ENTRY DATE
        mBufferedWriter.append( "\n\nID" ).append( Integer.toString( entry.get_id() ) )
                       .append( "\nE" ).append( ( entry.get_type() == Type.CHAPTER ) ? '+' : ' ' )
                       .append( entry.is_favored() ? 'F' : '_' )
                       .append( entry.is_trashed() ? 'T' : '_' )
                       .append( get_elem_todo_status_char( entry ) )
                       .append( entry.get_expanded() ? 'E' : '_' )
                       .append( Long.toString( entry.m_date.m_date ) );

        mBufferedWriter.append( "\nEc" ).append( Long.toString( entry.m_date_created ) );
        mBufferedWriter.append( "\nEe" ).append( Long.toString( entry.m_date_edited ) );
        mBufferedWriter.append( "\nEt" ).append( Long.toString( entry.m_date_status ) );

        // THEME
        if( entry.is_theme_set() )
            mBufferedWriter.append( "\nEm" ).append( entry.get_theme().get_name() );

        // SPELLCHECKING LANGUAGE
        if( !entry.m_option_lang.equals( Lifeograph.LANG_INHERIT_DIARY ) )
            mBufferedWriter.append( "\nEs" ).append( entry.m_option_lang );

        // UNIT
        if( !entry.m_unit.isEmpty() )
            mBufferedWriter.append( "\nEu" ).append( entry.m_unit );

        // LOCATION
        if( entry.is_location_set() )
            mBufferedWriter.append( "\nEla" ).append( Double.toString( entry.m_location.latitude ) )
                           .append( "\nElo" )
                           .append( Double.toString( entry.m_location.longitude ) );
        // PATH (ROUTE)
        if( entry.is_map_path_set() )
            for( Entry.Coords point : entry.m_map_path )
        mBufferedWriter.append( "\nEra" ).append( Double.toString( point.latitude ) )
                       .append( "\nEro" ).append( Double.toString( point.longitude ) );

        // PARAGRAPHS
        for( Paragraph para : entry.m_paragraphs )
            mBufferedWriter.append( "\nEp" ).append( para.m_justification )
                           .append( para.m_text );
    }

    protected void
    create_db_header_text( boolean encrypted ) throws IOException {
        mBufferedWriter.write( DB_FILE_HEADER );
        mBufferedWriter.append( "\nV " + DB_FILE_VERSION_INT )
                       .append( "\nE " ).append( encrypted ? "y" : "n" )
                       .append( "\nId" ).append( Integer.toString( get_id() ) ) // diary id
                       .append( "\n\n" ); // end of header
    }

    @SuppressLint( "DefaultLocale" )
    protected boolean
    create_db_body_text() throws IOException {
        // DIARY OPTIONS
        mBufferedWriter.append( "Do" ).append( m_opt_show_all_entry_locations ? 'A' : '_' )
                       .append( String.format( "%03d", m_sorting_criteria ) )
                       .append( Integer.toString( m_opt_ext_panel_cur ) );

        // DEFAULT SPELLCHECKING LANGUAGE
        if( !m_language.isEmpty() )
            mBufferedWriter.append( "\nDs" ).append( m_language );

        // FIRST ENTRY TO SHOW AT STARTUP (HOME ITEM) & LAST ENTRY SHOWN IN PREVIOUS SESSION
        mBufferedWriter.append( "\nDf" ).append( Integer.toString( m_startup_entry_id ) );
        mBufferedWriter.append( "\nDl" ).append( Integer.toString( m_last_entry_id ) );

        // COMPLETION TAG
        if( m_completion_tag_id != DEID_UNSET )
            mBufferedWriter.append( "\nDc" ).append( Integer.toString( m_completion_tag_id ) );

        // THEMES
        for( Theme theme : m_themes.values() ) {
            mBufferedWriter.append( "\n\nID" ).append( Integer.toString( theme.get_id() ) );
            mBufferedWriter.append( "\nT " ).append( ( theme == m_theme_default ? 'D' : '_' ) )
                           .append( theme.m_name );
            mBufferedWriter.append( "\nTf" ).append( theme.font );
            mBufferedWriter.append( "\nTb" ).append( Theme.color2string( theme.color_base  ) );
            mBufferedWriter.append( "\nTt" ).append( Theme.color2string( theme.color_text ) );
            mBufferedWriter.append( "\nTh" ).append( Theme.color2string( theme.color_heading ) );
            mBufferedWriter.append( "\nTs" ).append( Theme.color2string( theme.color_subheading ) );
            mBufferedWriter.append( "\nTl" ).append( Theme.color2string( theme.color_highlight ) );
            if( !theme.image_bg.isEmpty() )
                mBufferedWriter.append( "\nTi" ).append( theme.image_bg );
        }

        // FILTERS
        for( Filter filter : m_filters.values() ) {
            mBufferedWriter.append( "\n\nID" ).append( Integer.toString( filter.get_id() ) );
            mBufferedWriter.append( "\nF " )
                           .append( filter == m_filter_active ? 'A' : '_' )
                           .append( filter.m_name );
            mBufferedWriter.append( '\n' ).append( filter.get_definition() );
        }

        // CHARTS
        for( ChartElem chart : m_charts.values() ) {
            mBufferedWriter.append( "\n\nID" ).append( Integer.toString( chart.get_id() ) );
            mBufferedWriter.append( "\nG " ).append( chart == m_chart_active ? 'A' : '_' )
                           .append( chart.m_name );
            mBufferedWriter.append( '\n' ).append( chart.get_definition() );
        }

        // TABLES
        for( TableElem table : m_tables.values() ) {
            mBufferedWriter.append( "\n\nID" ).append( Integer.toString( table.get_id() ) );
            mBufferedWriter.append( "\nM " ).append( table == m_table_active ? 'A' : '_' )
                           .append( table.m_name );
            mBufferedWriter.append( '\n' ).append( table.get_definition() );
        }

        // CHAPTERS
        for( Chapter.Category ctg : m_chapter_categories.values() ) {
            mBufferedWriter.append( "\n\nID" ).append( Integer.toString( ctg.get_id() ) )
                           .append( "\nC " ).append( ctg == m_p2chapter_ctg_cur ? 'A' : '_' )
                           .append( ctg.m_name );
            // chapters in it:
            for( Chapter chapter : ctg.mMap.values() ) {
                create_db_entry_text( chapter );
                if( chapter.m_color != Color.WHITE )
                    mBufferedWriter.append( "\nEb" )
                                   .append( Theme.color2string( chapter.m_color ) );
            }
        }

        // ENTRIES
        for( Entry entry : m_entries.values() ) {
            // purge empty entries:
            if( entry.is_empty() ) continue;
                // optionally only save filtered entries:
            //else if( entry.get_filtered_out() && m_flag_only_save_filtered ) continue;

            create_db_entry_text( entry );
        }

        return true;
    }

    // READING =====================================================================================
    Result
    set_path( String path, SetPathType type ) {
        clear();

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

        if( !m_flag_read_only && isLocked() )
            return Result.FILE_LOCKED;
        else
            return Result.SUCCESS;
    }

    protected Result
    read_header( AssetManager assetMan ) {
        String line;
        mHeaderLineCount = 0;

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
            mHeaderLineCount++;

            while( ( line = mBufferedReader.readLine() ) != null ) {
                if( line.length() < 1 ) { // end of header
                    set_id( create_new_id( this ) );
                    mHeaderLineCount++;
                    return( m_read_version != 0 ? Result.SUCCESS : Result.CORRUPT_FILE );
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
                        mHeaderLineCount++;
                        break;
                    case 'E':
                        // passphrase is set to a dummy value to indicate that diary
                        // is an encrypted one until user enters the real passphrase
                        m_passphrase = ( line.charAt( 2 ) == 'y' ? " " : "" );
                        mHeaderLineCount++;
                        break;
                    case 'I':
                        set_force_id( Integer.parseInt( line.substring( 2 ) ) );
                        mHeaderLineCount++;
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

    protected Result
    read_body() {
        Result res = ( m_passphrase.isEmpty() ? read_plain() : read_encrypted() );

        close_file();

        enableWorkingOnLockfile( false );

        if( res == Result.SUCCESS )
            m_login_status = LoginStatus.LOGGED_IN_RO;

        return res;
    }

    protected Result
    read_plain() {
        return parse_db_body_text();
    }

    protected Result
    read_encrypted() {
        close_file();

        try {
            RandomAccessFile file = new RandomAccessFile( m_path, "r" );
            for( int i= 0; i < mHeaderLineCount; i++ )
                file.readLine();

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

            String mBufferedWriter = decryptBuffer( m_passphrase, salt, buffer, size, iv );

            // passphrase check
            if( mBufferedWriter.charAt( 0 ) != m_passphrase.charAt( 0 ) || mBufferedWriter.charAt( 1 ) != '\n' ) {
                clear();
                return Result.WRONG_PASSWORD;
            }

            mBufferedReader = new BufferedReader( new StringReader( mBufferedWriter ) );
        }
        catch( FileNotFoundException ex ) {
            return Result.FILE_NOT_FOUND;
        }
        catch( IOException ex ) {
            return Result.CORRUPT_FILE;
        }

        return parse_db_body_text();
    }

    Result
    enable_editing() {
        if( m_flag_read_only ) {
            Log.e( Lifeograph.TAG, "Diary: editing cannot be enabled. Diary is read-only" );
            return Result.FILE_LOCKED;
        }

        File fp = new File( m_path );
        if( !fp.canWrite() ) { // check write access
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

    // WRITING =====================================================================================
    Result
    write() {
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
                    Log.d( Lifeograph.TAG, "Backup written to: " + file_backup );
            }
        }

        // WRITE THE FILE
        return write( m_path );
    }

    Result
    write( String path ) {
        // m_flag_only_save_filtered = false;

        if( m_passphrase.isEmpty() )
            return write_plain( path, false );
        else
            return write_encrypted( path );
    }

    Result
    write_txt() {
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
            final String separator         = "---------------------------------------------\n";
            final String separator_favored = "+++++++++++++++++++++++++++++++++++++++++++++\n";
            final String separator_thick   = "=============================================\n";
            final String separator_chapter = ":::::::::::::::::::::::::::::::::::::::::::::\n";

            Chapter chapter = ( m_p2chapter_ctg_cur.mMap.isEmpty() ? null :
                    Objects.requireNonNull( m_p2chapter_ctg_cur.mMap.firstEntry() ).getValue() );

            // DIARY TITLE
            fileWriter.write( separator_thick );
            fileWriter.append( file.getName() )
                      .append( '\n' )
                      .append( separator_thick );

            // ENTRIES
            for( Entry entry : m_entries.descendingMap().values() ) {
                // CHAPTER
                while( chapter != null && entry.m_date.m_date < chapter.m_date.m_date ) {
                        fileWriter.append( "\n\n" ).append( separator_chapter )
                                  .append( chapter.m_date.format_string() )
                                  .append( " - " ).append( chapter.get_name() ).append( '\n' )
                                  .append( separator_chapter ).append( "\n\n" );

                        Map.Entry< Long, Chapter > kv_chapter =
                                m_p2chapter_ctg_cur.mMap.higherEntry( chapter.m_date.m_date );
                        chapter = ( kv_chapter != null ? kv_chapter.getValue() : null );
                }

                // PURGE EMPTY OR FILTERED OUT ENTRIES
                if( entry.is_empty() || entry.get_filtered_out() )
                    continue;

                // FAVOREDNESS AND DATE
                if( entry.is_favored() )
                    fileWriter.append( '\n' ).append( separator_favored );
                else
                    fileWriter.append( '\n' ).append( separator );
                if( !entry.m_date.is_hidden() )
                    fileWriter.append( entry.m_date.format_string() ).append( '\n' );

                // TO-DO STATUS
                switch( entry.get_status() & ES_FILTER_TODO ) {
                    case ES_TODO:
                        fileWriter.append( "[ ] " );
                        break;
                    case ES_PROGRESSED:
                        fileWriter.append( "[~] " );
                        break;
                    case ES_DONE:
                        fileWriter.append( "[+] " );
                        break;
                    case ES_CANCELED:
                        fileWriter.append( "[X] " );
                        break;
                }

                boolean flag_first = true;

                for( Paragraph para : entry.m_paragraphs ) {
                    fileWriter.append( para.m_text ).append( '\n' );
                    if( flag_first ) {
                        fileWriter.append( entry.is_favored() ? separator_favored : separator );
                        flag_first = false;
                    }
                }


                fileWriter.append( "\n\n" );
            }

            // EMPTY CHAPTERS
            while( chapter != null ) {
                fileWriter.append( "\n\n" ).append( separator_chapter )
                          .append( chapter.m_date.format_string() )
                          .append( " - " ).append( chapter.m_name ).append( '\n' )
                          .append( separator_chapter ).append( "\n\n" );

                Map.Entry< Long, Chapter > kv_chapter =
                        m_p2chapter_ctg_cur.mMap.higherEntry( chapter.m_date.m_date );
                chapter = ( kv_chapter != null ? kv_chapter.getValue() : null );
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

    protected Result
    write_plain( String path, boolean flag_header_only ) {
        try {
            mBufferedWriter = new BufferedWriter( new FileWriter( path ) );
            create_db_header_text( flag_header_only ); // header only mode = encrypted diary
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

    protected Result
    write_encrypted( String path ) {
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

    protected void
    close_file() {
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

    boolean
    remove_lock_if_necessary() {
        if( m_login_status != LoginStatus.LOGGED_IN_EDIT || m_path.isEmpty() )
            return false;

        File fp = new File( m_path + LOCK_SUFFIX );
        if( fp.exists() )
            return fp.delete();
        return true;
    }

    // JAVA ONLY ===================================================================================
    boolean
    isLocked() {
        File lockFile = new File( m_path + LOCK_SUFFIX );
        return( lockFile.exists() );
    }

    void
    setSavingEnabled( boolean flag ) {
        m_flag_save_enabled = flag;
    }

    void
    enableWorkingOnLockfile( boolean enable ) {
        if( enable )
            m_path += LOCK_SUFFIX;
        else if( m_path.endsWith( LOCK_SUFFIX ) )
            m_path = m_path.substring( 0, m_path.length() - LOCK_SUFFIX.length() );
    }

    void
    writeAtLogout() {
        if( m_flag_save_enabled && is_in_edit_mode() ) {
            if( write() == Result.SUCCESS )
                Log.d( Lifeograph.TAG, "File saved successfully" );
            else
                Lifeograph.showToast( "Cannot save the diary file" );
        }
        else
            Log.d( Lifeograph.TAG, "Diary is not saved" );
    }

    void
    writeLock() {
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

    // HELPER FUNCTIONS ============================================================================
    public String
    create_unique_name_for_map( TreeMap map, String name0 ) {
        String name = name0;
        for( int i = 1; map.containsKey( name ); i++ ) {
            name = name0 + " " + i;
        }

        return name;
    }

    double
    parseDouble( String text ) {
        //NOTE: this implementation may be a little bit more forgiving than good for health
        double  value = 0.0;
        int     divider = 0;
        boolean negative = false;
        char    c;

        for( int i = 0; i < text.length(); i++ ) {
            c = text.charAt( i );
            switch( c ) {
                case ',':
                case '.':
                    if( divider == 0 ) // note that if divider
                        divider = 1;
                    break;
                case '-':
                    negative = true;
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    value *= 10;
                    value += ( c - '0' );
                    if( divider != 0 )
                        divider *= 10;
                    break;
                default:
                    break;
            }
        }

        if( divider > 1 )
            value /= divider;
        if( negative )
            value *= -1;

        return value;
    }

    // VARIABLES ===================================================================================
    static Diary d = null;

    private String m_path;
    private String m_passphrase;

    //ids (DEID)
    private final TreeMap< Integer, DiaryElement > m_ids = new TreeMap<>();
    private int m_force_id          = DEID_UNSET;
    private int m_startup_entry_id  = HOME_CURRENT_ENTRY;
    private int m_last_entry_id     = DEID_UNSET;
    private int m_completion_tag_id = DEID_UNSET;

    // CONTENT
    TreeMap< Long, Entry >    m_entries = new TreeMap<>( DiaryElement.compare_dates );
    Map< String, List< Entry > >
                              m_entry_names = new TreeMap<>();

    TreeMap< String, Theme >  m_themes = new TreeMap<>();
    Theme                     m_theme_default = null;

    TreeMap< String, Chapter.Category >
                              m_chapter_categories = new TreeMap<>( DiaryElement.compare_names );
    Chapter.Category          m_p2chapter_ctg_cur = null;

    TreeMap< String, Filter > m_filters = new TreeMap<>();
    Filter                    m_filter_active = null;

    TreeMap< String, ChartElem >
                              m_charts = new TreeMap<>();
    ChartElem                 m_chart_active = null;

    TreeMap< String, TableElem >
                              m_tables = new TreeMap<>();
    TableElem                 m_table_active = null;

    // OPTIONS
    protected String  m_language;

    protected int     m_read_version;

    // options & flags
    protected int     m_sorting_criteria = SoCr_DEFAULT;
    protected boolean m_opt_show_all_entry_locations = false;
    protected int     m_opt_ext_panel_cur = 1;
    protected boolean m_flag_read_only = false;
    protected boolean m_flag_ignore_locks = false;
    protected boolean m_flag_skip_old_check = false;
    protected boolean m_flag_save_enabled = true;
    //private boolean m_flag_only_save_filtered;

    enum LoginStatus{ LOGGED_OUT, LOGGED_TIME_OUT, LOGGED_IN_RO, LOGGED_IN_EDIT }
    protected LoginStatus m_login_status = LoginStatus.LOGGED_OUT;

    // searching
    protected String  m_search_text = "";
    List< Match >     m_matches     = new ArrayList<>();

    // i/o
    protected BufferedReader mBufferedReader  = null;
    protected BufferedWriter mBufferedWriter  = null;
    protected int            mHeaderLineCount = 0;

    protected static final int cIV_SIZE   = 16; // = 128 bits
    protected static final int cSALT_SIZE = 16; // = 128 bits
}
