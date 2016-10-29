/***********************************************************************************

 Copyright (C) 2012-2016 Ahmet Öztürk (aoz_2@yahoo.com)

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

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public class AddElemAction extends ActionProvider implements MenuItem.OnMenuItemClickListener
{
    public AddElemAction( Context context ) {
        super( context );
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public void onPrepareSubMenu( SubMenu menu ) {
        super.onPrepareSubMenu( menu );

        menu.clear();

        menu.add( 0, R.id.add_today, 0, R.string.add_today )
            .setIcon( R.mipmap.ic_entry )
            .setOnMenuItemClickListener( this );

        menu.add( 0, R.id.add_topic, 1, R.string.topic )
            .setIcon( R.mipmap.ic_chapter_o )
            .setOnMenuItemClickListener( this );

        menu.add( 0, R.id.add_group, 1, R.string.group )
            .setIcon( R.mipmap.ic_chapter_f )
            .setOnMenuItemClickListener( this );

        if( mParent.mPager.getCurrentItem() == 2 )
            menu.add( 0, R.id.add_tag_ctg, 1, R.string.tag_ctg )
                .setIcon( R.mipmap.ic_tag_ctg )
                .setOnMenuItemClickListener( this );
    }

    //@Override
    public boolean onMenuItemClick( MenuItem item ) {
        switch( item.getItemId() ) {
            case android.R.id.home:
                return true;
            case R.id.add_today:
                mParent.goToToday();
                return true;
            case R.id.add_topic:
                mParent.createTopic();
                return true;
            case R.id.add_group:
                mParent.createGroup();
                return true;
            case R.id.add_tag_ctg:
                mParent.createTagCtg();
                return true;
        }
        return true;
    }

    @Override
    public View onCreateActionView() {
        return null;
    }

    ActivityDiary mParent;
}
