/***********************************************************************************

    Copyright (C) 2012-2014 Ahmet Öztürk (aoz_2@yahoo.com)

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

import java.util.ArrayList;

public class Tag extends DiaryElement {

    public static class Category extends DiaryElement
    {
        public Category( Diary diary, String name ) {
            super( diary, name, ES_EXPANDED );
            m_name = name;
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
        public int get_icon() {
            return R.drawable.ic_tag;
        }

        @Override
        public String get_info_str() {
            return( mTags.size() + " entries" );
        }

        public boolean get_expanded() {
            return( ( m_status & ES_EXPANDED ) != 0 );
        }
        public void set_expanded( boolean expanded ) {
            set_status_flag( ES_EXPANDED, expanded );
        }

        public void add( Tag tag ) {
            mTags.add( tag );
        }

        public void remove( Tag tag ) {
            mTags.remove( tag );
        }

        // CONTENTS
        java.util.List< Tag > mTags = new ArrayList< Tag >();
    }

//    public Tag( Diary diary ) {
//        super( diary, ES_VOID, "" );
//    }

    public Tag( Diary diary, String name, Category ctg ) {
        super( diary, name, ES_VOID );
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
    public int get_icon() {
        return( get_has_own_theme() ? R.drawable.ic_theme_tag : R.drawable.ic_tag );
    }

    @Override
    public String get_info_str() {
        return( get_size() + " entries" );
    }

    @Override
    public String getListStrSecondary() {
        return( "Tag with " + get_size() + " entries" );
    }

    public Category get_category() {
        return m_ptr2category;
    }

    public void set_category( Category ctg ) {
        if( m_ptr2category != null )
            m_ptr2category.remove( this );
        if( ctg != null )
            ctg.add( this );
        m_ptr2category = ctg;
    }

    public void add_entry( Entry entry ) {
        mEntries.add( entry );
    }

    public void remove_entry( Entry entry ) {
        mEntries.remove( entry );
    }

    // THEMES
    public Theme get_theme()
    {
        return( m_theme != null ? m_theme : Theme.System.get() );
    }

    public boolean get_has_own_theme()
    {
        return( m_theme != null );
    }

    public Theme get_own_theme() {
        if( m_theme == null ) {
            m_theme = new Theme();

            for( Entry entry : mEntries )
                entry.update_theme();
        }

        return m_theme;
    }

    public Theme create_own_theme_duplicating( Theme theme ) {
        m_theme = new Theme( theme );

        for( Entry entry : mEntries )
            entry.update_theme();

        return m_theme;
    }

    public void reset_theme() {
        if( m_theme != null ) {
            m_theme = null;

            for( Entry entry : mEntries )
                entry.update_theme();
        }
    }

    // MEMBER VARIABLES
    Category m_ptr2category;
    java.util.List< Entry > mEntries = new ArrayList< Entry >();
    private Theme m_theme = null;
}
