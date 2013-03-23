package de.dizayn.blhps.lifeograph;

import java.util.ArrayList;

public class Tag extends DiaryElement {

    public static class Category extends DiaryElement {
        public Category( Diary diary, String name ) {
            super( diary, name );
            m_name = name;
            m_flag_expanded = true;
        }

        @Override
        public Type get_type() {
            return Type.TAG_CTG;
        }

        @Override
        public int get_size() {
            return mTags.size();
        }

        @Override
        public String getSubStr() {
            return "Size: " + mTags.size();
        }

        @Override
        public int get_icon() {
            return R.drawable.icon_tag;
        }

        public boolean get_expanded() {
            return m_flag_expanded;
        }

        public void set_expanded( boolean expanded ) {
            m_flag_expanded = expanded;
        }

        public void add( Tag tag ) {
            mTags.add( tag );
        }

        public void remove( Tag tag ) {
            mTags.remove( tag );
        }

        protected boolean m_flag_expanded;
        protected java.util.List< Tag > mTags = new ArrayList< Tag >();
    }

    public Tag( Diary diary ) {
        super( diary, "" );
    }

    public Tag( Diary diary, String name, Category ctg ) {
        super( diary, name );
        m_ptr2category = ctg;
        if( ctg != null )
            ctg.add( this );
    }

    @Override
    public Type get_type() {
        return Type.TAG;
    }

    @Override
    public int get_size() {
        return mEntries.size();
    }

    @Override
    public String getSubStr() {
        return( "Tag with " + mEntries.size() + " Entries" );
    }

    @Override
    public int get_icon() {
        return R.drawable.icon_tag;
    }

    public Category get_category() {
        return m_ptr2category;
    }

    public void set_category( Category ctg ) {
        // TODO...
    }

    public void add_entry( Entry entry ) {
        mEntries.add( entry );
    }

    public void remove_entry( Entry entry ) {
        mEntries.remove( entry );
    }

    // MEMBER VARIABLES
    protected Category m_ptr2category;
    protected java.util.List< Entry > mEntries = new ArrayList< Entry >();
}
