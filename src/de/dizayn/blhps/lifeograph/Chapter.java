package de.dizayn.blhps.lifeograph;

import java.util.Map;

public class Chapter extends DiaryElement {
    public static class Category extends DiaryElement {

        public Category( Diary diary, String name ) {
            super( diary, name );
            mMap = new java.util.TreeMap< Long, Chapter >( DiaryElement.compare_dates );
        }

        @Override
        public Type get_type() {
            return Type.CHAPTER_CTG;
        }

        @Override
        public int get_size() {
            return mMap.size();
        }

        @Override
        public String getSubStr() {
            return "Size: " + mMap.size();
        }

        @Override
        public int get_icon() {
            return R.drawable.ic_diary;
        }

        public Chapter create_chapter( String name, long date ) {
            Chapter chapter = new Chapter( m_diary, name, date );
            mMap.put( date, chapter );
            return chapter;
        }

        public Chapter add_chapter( String name ) {
            Chapter chapter = new Chapter( m_diary, name, get_free_order_ordinal().m_date );
            chapter.set_expanded( true );
            mMap.put( chapter.m_date_begin.m_date, chapter );
            return chapter;
        }

        public void dismiss_chapter( Chapter chapter ) {
            boolean found = false;

            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date ) {
                    found = true;
                }
                else if( found ) {
                    Chapter chapter_earlier = e.getValue();
                    if( chapter.m_time_span > 0 )
                        chapter_earlier.m_time_span += chapter.m_time_span;
                    else
                        chapter_earlier.m_time_span = 0;
                    break;
                }
            }
            mMap.remove( chapter.m_date_begin.m_date );
        }

        Date get_free_order_ordinal() {
            if( mMap.size() > 0 ) {
                Date d = new Date( ( Long ) mMap.keySet().toArray()[ 0 ] );
                if( d.is_ordinal() ) {
                    d.forward_ordinal_order();
                    return d;
                }
            }

            return( new Date( 0, 0 ) );
        }

        public Chapter getChapterEarlier( Chapter chapter ) {
            boolean found = false;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date ) {
                    found = true;
                }
                else if( found ) {
                    return e.getValue();
                }
            }
            return null;
        }

        public Chapter getChapterLater( Chapter chapter ) {
            Chapter chapter_later = null;
            for( Map.Entry< Long, Chapter > e : mMap.entrySet() ) {
                if( e.getKey() == chapter.m_date_begin.m_date )
                    return chapter_later;
                else if( e.getKey() < chapter.m_date_begin.m_date )
                    break;

                chapter_later = e.getValue();
            }
            return null;
        }

        protected java.util.Map< Long, Chapter > mMap;
    }

    public Chapter( Diary diary, String name ) {
        super( diary, name );
        m_date_begin = new Date();
    }

    public Chapter( Diary diary, String name, long date ) {
        super( diary, name );
        m_date_begin = new Date( date );
    }

    @Override
    public Type get_type() {
        if( m_date_begin != null )
            if( m_date_begin.is_ordinal() )
                return Type.TOPIC;
        return Type.CHAPTER;
    }

    @Override
    public int get_size() {
        return m_size;
    }

    boolean is_ordinal() {
        return m_date_begin.is_ordinal();
    }

    @Override
    public String getListStr() {
        return( m_date_begin.format_string( false ) + STR_SEPARATOR + m_name );
    }

    @Override
    public String getSubStr() {
        return ( m_date_begin.is_ordinal() ? "Topic" : "Chapter" ) + " with " + m_size + " Entries";
    }

    @Override
    public int get_icon() {
        return( m_date_begin.is_ordinal() ? R.drawable.ic_topic : R.drawable.ic_chapter );
    }

    public boolean get_expanded() {
        return m_flag_expanded;
    }

    public void set_expanded( boolean expanded ) {
        m_flag_expanded = expanded;
    }

    public void set_date( long date ) {
        m_date_begin.m_date = date;
    }

    void recalculate_span( Chapter next ) {
        if( next == null )
            m_time_span = 0; // unlimited
        else if( next.m_date_begin.is_ordinal() )
            m_time_span = 0; // last temporal chapter: unlimited
        else
            m_time_span = m_date_begin.calculate_days_between( next.m_date_begin );
    }

    Date get_free_order() {
        Date date = new Date( m_date_begin.m_date );
        Diary.diary.make_free_entry_order( date );
        return date;
    }

    protected Date m_date_begin;
    int m_time_span = 0;
    protected int m_size = 0;
    protected boolean m_flag_expanded = true;
}
